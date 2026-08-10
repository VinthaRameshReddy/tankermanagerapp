package com.tankermanager.app.util

import android.content.Context
import android.location.Geocoder
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLDecoder
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.math.abs

/**
 * Resolves Google Maps share / short links to coordinates on the device.
 * Place-only links (no lat/lng in URL) are geocoded via Android [Geocoder].
 */
object MapsLinkResolver {

    private val urlInText = Regex(
        """((?:https?://)?(?:maps\.app\.goo\.gl|goo\.gl|g\.co|www\.google\.com/maps|maps\.google\.com)[^\s"'<>]+)""",
        RegexOption.IGNORE_CASE
    )

    private val placePath = Regex("/maps/place/([^/@?]+)", RegexOption.IGNORE_CASE)

    private val atCoords = Regex("@(-?\\d+\\.?\\d*),(-?\\d+\\.?\\d*)")
    private val bang3d4d = Regex("!3d(-?\\d+\\.?\\d*)!4d(-?\\d+\\.?\\d*)")
    private val bang4d3d = Regex("!4d(-?\\d+\\.?\\d*)!3d(-?\\d+\\.?\\d*)")
    private val qCoords = Regex("[?&]q=(-?\\d+\\.?\\d*),(-?\\d+\\.?\\d*)")
    private val queryCoords = Regex("[?&]query=(-?\\d+\\.?\\d*),(-?\\d+\\.?\\d*)")
    private val centerCoords = Regex("[?&]center=(-?\\d+\\.?\\d*),(-?\\d+\\.?\\d*)")
    private val llCoords = Regex("[?&]ll=(-?\\d+\\.?\\d*),(-?\\d+\\.?\\d*)")
    private val metaRefresh = Regex("""url=(https?://[^"'>\s]+)""", RegexOption.IGNORE_CASE)

    private val http: OkHttpClient = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .build()

    private val httpNoRedirect: OkHttpClient = OkHttpClient.Builder()
        .followRedirects(false)
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    data class Resolved(val mapsLink: String, val latitude: Double, val longitude: Double)

    fun extractUrl(pasted: String): String? {
        val trimmed = pasted.trim()
        if (trimmed.isBlank()) return null
        urlInText.find(trimmed)?.let {
            return normalizeUrl(trimTrailingPunctuation(it.groupValues[1]))
        }
        if (looksLikeMaps(trimmed)) return normalizeUrl(trimmed)
        return null
    }

    fun extractPlaceQuery(url: String): String? {
        val decoded = decode(url)
        val raw = placePath.find(decoded)?.groupValues?.get(1) ?: return null
        val segment = raw.substringBefore("/data").substringBefore("?")
        return segment.replace('+', ' ').trim().takeIf { it.isNotBlank() }
    }

    suspend fun resolveForApi(
        context: Context?,
        mapsPaste: String?,
        gpsLat: Double? = null,
        gpsLng: Double? = null
    ): Triple<String?, Double?, Double?> = withContext(Dispatchers.IO) {
        val paste = mapsPaste?.trim().orEmpty()
        val url = when {
            paste.isBlank() -> null
            else -> extractUrl(paste) ?: normalizeUrl(paste)
        }

        if (gpsLat != null && gpsLng != null) {
            return@withContext Triple(url ?: paste.ifBlank { null }, gpsLat, gpsLng)
        }

        if (url != null) {
            val (coords, finalUrl) = resolveUrlWithFinal(url)
            var latLng = coords
            if (latLng == null && context != null) {
                latLng = geocodePlace(context, finalUrl)
            }
            if (latLng != null) {
                return@withContext Triple(url, latLng.first, latLng.second)
            }
            return@withContext Triple(url, null, null)
        }

        Triple(null, null, null)
    }

    private suspend fun geocodePlace(context: Context, mapsUrl: String): Pair<Double, Double>? {
        val queries = buildList {
            extractPlaceQuery(mapsUrl)?.let { add(it) }
            extractPlaceQuery(decode(mapsUrl))?.let { if (it !in this) add(it) }
        }
        for (query in queries) {
            geocodeQuery(context, query)?.let { return it }
            val shorter = query.split(",").takeLast(3).joinToString(",").trim()
            if (shorter.length >= 8 && shorter != query) {
                geocodeQuery(context, shorter)?.let { return it }
            }
        }
        return null
    }

    private suspend fun geocodeQuery(context: Context, query: String): Pair<Double, Double>? {
        if (!Geocoder.isPresent()) return null
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                suspendCancellableCoroutine { cont ->
                    Geocoder(context, Locale.getDefault()).getFromLocationName(query, 1) { list ->
                        val loc = list.firstOrNull()
                        cont.resume(
                            if (loc != null) loc.latitude to loc.longitude else null
                        )
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                val list = Geocoder(context, Locale.getDefault()).getFromLocationName(query, 1)
                list?.firstOrNull()?.let { it.latitude to it.longitude }
            }
        } catch (_: Exception) {
            null
        }
    }

    private data class UrlResolve(val coords: Pair<Double, Double>?, val finalUrl: String)

    private fun resolveUrlWithFinal(startUrl: String): UrlResolve {
        val normalized = normalizeUrl(startUrl) ?: return UrlResolve(null, startUrl)
        tryPatterns(normalized)?.let { return UrlResolve(it, normalized) }

        var current = normalized
        hopLoop@ for (hop in 0 until 15) {
            tryPatterns(current)?.let { return UrlResolve(it, current) }
            tryPatterns(decode(current))?.let { return UrlResolve(it, current) }

            val request = Request.Builder()
                .url(current)
                .header(
                    "User-Agent",
                    "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 Chrome/120.0.0.0 Mobile Safari/537.36"
                )
                .header("Accept", "text/html,application/xhtml+xml,*/*")
                .get()
                .build()

            try {
                httpNoRedirect.newCall(request).execute().use { response ->
                    when (response.code) {
                        in 300..399 -> {
                            val loc = response.header("Location") ?: return UrlResolve(null, current)
                            current = response.request.url.resolve(loc).toString()
                        }
                        in 200..299 -> {
                            val final = response.request.url.toString()
                            tryPatterns(final)?.let { return UrlResolve(it, final) }
                            val body = response.body?.string().orEmpty()
                            tryPatterns(body)?.let { return UrlResolve(it, final) }
                            metaRefresh.find(body)?.let { m ->
                                val target = normalizeUrl(decode(m.groupValues[1])) ?: return UrlResolve(null, final)
                                tryPatterns(target)?.let { return UrlResolve(it, target) }
                                current = target
                                return@use
                            }
                            return UrlResolve(null, final)
                        }
                        else -> return UrlResolve(null, current)
                    }
                }
            } catch (_: Exception) {
                return UrlResolve(null, current)
            }
        }

        try {
            http.newCall(
                Request.Builder()
                    .url(normalized)
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) Chrome/120.0.0.0 Mobile Safari/537.36")
                    .get()
                    .build()
            ).execute().use { response ->
                val final = response.request.url.toString()
                tryPatterns(final)?.let { return UrlResolve(it, final) }
                tryPatterns(response.body?.string().orEmpty())?.let { return UrlResolve(it, final) }
                return UrlResolve(null, final)
            }
        } catch (_: Exception) {
            return UrlResolve(null, current)
        }
    }

    private fun tryPatterns(text: String): Pair<Double, Double>? {
        if (text.isBlank()) return null
        listOf(atCoords, bang3d4d, bang4d3d, qCoords, queryCoords, centerCoords, llCoords).forEach { regex ->
            regex.find(text)?.let { m ->
                return if (regex === bang4d3d) {
                    pair(m.groupValues[2], m.groupValues[1])
                } else {
                    pair(m.groupValues[1], m.groupValues[2])
                }
            }
        }
        return null
    }

    private fun pair(latStr: String, lngStr: String): Pair<Double, Double>? {
        val lat = latStr.toDoubleOrNull() ?: return null
        val lng = lngStr.toDoubleOrNull() ?: return null
        if (abs(lat) > 90 || abs(lng) > 180) return null
        return lat to lng
    }

    private fun looksLikeMaps(value: String): Boolean {
        val v = value.lowercase().trim()
        return v.contains("maps.google") || v.contains("google.com/maps")
            || v.contains("goo.gl") || v.contains("maps.app.goo.gl")
            || v.contains("g.co")
    }

    private fun normalizeUrl(raw: String): String? {
        var u = raw.trim()
        if (u.startsWith("//")) u = "https:$u"
        else if (!u.startsWith("http://") && !u.startsWith("https://")) {
            if (!looksLikeMaps(u)) return null
            u = "https://$u"
        }
        return u
    }

    private fun trimTrailingPunctuation(url: String): String {
        var u = url
        while (u.isNotEmpty() && u.last() in ".),]>\"'") {
            u = u.dropLast(1)
        }
        return u
    }

    private fun decode(raw: String): String = try {
        URLDecoder.decode(raw, Charsets.UTF_8.name())
    } catch (_: Exception) {
        raw
    }
}
