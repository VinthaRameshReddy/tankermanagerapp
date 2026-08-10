package com.tankermanager.util;

import com.tankermanager.exception.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts lat/lng from Google Maps share links (including short links),
 * plain "lat,lng" text, or HTML from Maps redirect pages.
 */
public final class MapsLinkParser {

    private static final Logger log = LoggerFactory.getLogger(MapsLinkParser.class);

    private static final Pattern AT_COORDS = Pattern.compile("@(-?\\d+\\.\\d+),(-?\\d+\\.\\d+)");
    private static final Pattern Q_COORDS = Pattern.compile("[?&]q=(-?\\d+\\.\\d+),(-?\\d+\\.\\d+)");
    private static final Pattern QUERY_COORDS = Pattern.compile("[?&]query=(-?\\d+\\.\\d+),(-?\\d+\\.\\d+)");
    private static final Pattern CENTER_COORDS = Pattern.compile("[?&]center=(-?\\d+\\.\\d+),(-?\\d+\\.\\d+)");
    private static final Pattern SLL_COORDS = Pattern.compile("[?&]sll=(-?\\d+\\.\\d+),(-?\\d+\\.\\d+)");
    private static final Pattern BANG_3D4D = Pattern.compile("!3d(-?\\d+\\.\\d+)!4d(-?\\d+\\.\\d+)");
    private static final Pattern BANG_4D3D = Pattern.compile("!4d(-?\\d+\\.\\d+)!3d(-?\\d+\\.\\d+)");
    private static final Pattern LL = Pattern.compile("[?&]ll=(-?\\d+\\.\\d+),(-?\\d+\\.\\d+)");
    private static final Pattern DESTINATION = Pattern.compile(
            "destination=(-?\\d+\\.\\d+)%2C(-?\\d+\\.\\d+)|destination=(-?\\d+\\.\\d+),(-?\\d+\\.\\d+)");
    private static final Pattern PLAIN_PAIR = Pattern.compile("^\\s*(-?\\d{1,2}\\.\\d+)\\s*,\\s*(-?\\d{1,3}\\.\\d+)\\s*$");
    private static final Pattern URL_IN_HTML = Pattern.compile(
            "(?:https?://)?(?:maps\\.google\\.com|www\\.google\\.com/maps|maps\\.app\\.goo\\.gl|goo\\.gl|g\\.co)[^\"'\\s<>]+",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern META_REFRESH = Pattern.compile(
            "url=(https?://[^\"'>\\s]+)", Pattern.CASE_INSENSITIVE);
    /** WhatsApp / share text — with or without https (e.g. goo.gl/AbCdEf). */
    private static final Pattern URL_IN_SHARE_TEXT = Pattern.compile(
            "((?:https?://)?(?:maps\\.app\\.goo\\.gl|goo\\.gl|g\\.co|www\\.google\\.com/maps|maps\\.google\\.com)[^\\s\"'<>]+)",
            Pattern.CASE_INSENSITIVE);

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    private MapsLinkParser() {
    }

    public static Coords parse(String mapsLink) {
        if (mapsLink == null || mapsLink.isBlank()) {
            throw new BadRequestException("Google Maps link or latitude,longitude is required");
        }
        String raw = extractMapsUrl(mapsLink.trim());
        Coords c = tryPlainCoordsOnly(raw);
        if (c != null) {
            return c;
        }
        String decoded = decode(raw);
        c = tryPatterns(decoded);
        if (c == null) {
            c = tryPatterns(raw);
        }
        if (c == null && looksLikeShortOrMapsLink(raw)) {
            c = resolveViaHttp(raw);
        }
        if (c == null) {
            throw new BadRequestException(
                    "Could not read coordinates from this Maps link. "
                            + "Share location from Google Maps again, or use current GPS in the app.");
        }
        return c;
    }

    /**
     * Prefer explicit coordinates; otherwise parse maps link / plain text.
     */
    public static Coords resolve(BigDecimal latitude, BigDecimal longitude, String mapsLink) {
        if (latitude != null && longitude != null) {
            return validate(latitude, longitude);
        }
        if (mapsLink != null && !mapsLink.isBlank()) {
            return parse(mapsLink);
        }
        throw new BadRequestException("Provide a Google Maps link or latitude & longitude");
    }

    public static boolean looksLikeMapsLink(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        if (tryPlainCoordsOnly(value.trim()) != null) {
            return false;
        }
        return looksLikeShortOrMapsLink(value);
    }

    private static Coords validate(BigDecimal lat, BigDecimal lng) {
        if (lat.abs().compareTo(BigDecimal.valueOf(90)) > 0
                || lng.abs().compareTo(BigDecimal.valueOf(180)) > 0) {
            throw new BadRequestException("Invalid latitude or longitude");
        }
        return new Coords(lat.setScale(7, RoundingMode.HALF_UP), lng.setScale(7, RoundingMode.HALF_UP));
    }

    private static Coords tryPlainCoordsOnly(String text) {
        if (looksLikeShortOrMapsLink(text)) {
            return null;
        }
        Matcher m = PLAIN_PAIR.matcher(text);
        if (m.matches()) {
            return coords(m.group(1), m.group(2));
        }
        return null;
    }

    private static String extractMapsUrl(String input) {
        Matcher m = URL_IN_SHARE_TEXT.matcher(input);
        if (m.find()) {
            return trimTrailingUrlPunctuation(normalizeHttpUrl(m.group(1)));
        }
        String t = input.trim();
        if (looksLikeShortOrMapsLink(t)) {
            String n = normalizeHttpUrl(t);
            return n != null ? n : t;
        }
        return t;
    }

    private static String trimTrailingUrlPunctuation(String url) {
        if (url == null) {
            return null;
        }
        String u = url;
        while (!u.isEmpty() && ".,)>\"'".indexOf(u.charAt(u.length() - 1)) >= 0) {
            u = u.substring(0, u.length() - 1);
        }
        return u;
    }

    private static boolean looksLikeShortOrMapsLink(String value) {
        String v = value.toLowerCase(Locale.ROOT);
        return v.contains("maps.google") || v.contains("google.com/maps")
                || v.contains("goo.gl") || v.contains("maps.app.goo.gl")
                || v.contains("maps.apple.com") || v.contains("g.co")
                || v.startsWith("http://") || v.startsWith("https://");
    }

    private static Coords resolveViaHttp(String startUrl) {
        String current = normalizeHttpUrl(startUrl);
        if (current == null) {
            return null;
        }
        try {
            hopLoop:
            for (int hop = 0; hop < 15; hop++) {
                Coords inUrl = tryPatterns(decode(current));
                if (inUrl == null) {
                    inUrl = tryPatterns(current);
                }
                if (inUrl != null) {
                    return inUrl;
                }

                if (!isAllowedMapsHost(hostOf(current))) {
                    break;
                }

                HttpRequest request = HttpRequest.newBuilder(URI.create(current))
                        .timeout(Duration.ofSeconds(15))
                        .header("User-Agent", "Mozilla/5.0 (compatible; TankerManager/1.0; +maps-resolve)")
                        .header("Accept", "text/html,application/xhtml+xml,*/*")
                        .GET()
                        .build();
                HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
                int status = response.statusCode();

                if (status >= 300 && status < 400) {
                    String location = response.headers().firstValue("Location").orElse(null);
                    if (location == null || location.isBlank()) {
                        break;
                    }
                    String next = resolveRelative(current, location.trim());
                    if (next == null) {
                        break;
                    }
                    Coords fromPlaceUrl = geocodePlaceQuery(extractPlaceQuery(next));
                    if (fromPlaceUrl != null) {
                        return fromPlaceUrl;
                    }
                    current = next;
                    continue;
                }

                if (status >= 200 && status < 300) {
                    String body = response.body();
                    if (body != null && !body.isBlank()) {
                        Coords fromHtml = tryPatterns(body);
                        if (fromHtml != null) {
                            return fromHtml;
                        }
                        Matcher refresh = META_REFRESH.matcher(body);
                        if (refresh.find()) {
                            String target = decode(refresh.group(1));
                            Coords c = tryPatterns(target);
                            if (c != null) {
                                return c;
                            }
                            current = normalizeHttpUrl(target);
                            continue;
                        }
                        Matcher urlM = URL_IN_HTML.matcher(body);
                        while (urlM.find()) {
                            String found = decode(urlM.group());
                            Coords c = tryPatterns(found);
                            if (c != null) {
                                return c;
                            }
                            String next = normalizeHttpUrl(found);
                            if (next != null && !next.equals(current)) {
                                current = next;
                                continue hopLoop;
                            }
                        }
                    }
                    break;
                }
                break;
            }
        } catch (Exception e) {
            log.warn("Maps link HTTP resolve failed for {}: {}", startUrl, e.toString());
        }
        Coords fromPlace = geocodePlaceQuery(extractPlaceQuery(current));
        if (fromPlace != null) {
            return fromPlace;
        }
        return null;
    }

    private static final Pattern PLACE_PATH = Pattern.compile("/maps/place/([^/@?]+)", Pattern.CASE_INSENSITIVE);

    private static String extractPlaceQuery(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        Matcher m = PLACE_PATH.matcher(decode(url));
        if (!m.find()) {
            return null;
        }
        String segment = m.group(1);
        int dataIdx = segment.indexOf("/data");
        if (dataIdx >= 0) {
            segment = segment.substring(0, dataIdx);
        }
        return segment.replace('+', ' ').trim();
    }

    private static Coords geocodePlaceQuery(String query) {
        if (query == null || query.isBlank()) {
            return null;
        }
        try {
            String encoded = java.net.URLEncoder.encode(query.trim(), StandardCharsets.UTF_8);
            HttpRequest req = HttpRequest.newBuilder(URI.create(
                            "https://nominatim.openstreetmap.org/search?q=" + encoded + "&format=json&limit=1"))
                    .timeout(Duration.ofSeconds(12))
                    .header("User-Agent", "TankerManager/1.0")
                    .GET()
                    .build();
            HttpResponse<String> res = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() < 200 || res.statusCode() >= 300) {
                return null;
            }
            String body = res.body();
            Matcher latM = Pattern.compile("\"lat\":\"([^\"]+)\"").matcher(body);
            Matcher lonM = Pattern.compile("\"lon\":\"([^\"]+)\"").matcher(body);
            if (latM.find() && lonM.find()) {
                return coords(latM.group(1), lonM.group(1));
            }
        } catch (Exception e) {
            log.debug("Place geocode failed for {}: {}", query, e.toString());
        }
        return null;
    }

    private static String normalizeHttpUrl(String raw) {
        String u = raw.trim();
        if (u.startsWith("//")) {
            u = "https:" + u;
        } else if (!u.startsWith("http://") && !u.startsWith("https://")) {
            if (looksLikeShortOrMapsLink(u)) {
                u = "https://" + u;
            } else {
                return null;
            }
        }
        try {
            URI.create(u);
            return u;
        } catch (Exception e) {
            return null;
        }
    }

    private static String resolveRelative(String base, String location) {
        try {
            return URI.create(base).resolve(location).toString();
        } catch (Exception e) {
            return normalizeHttpUrl(location);
        }
    }

    private static String hostOf(String url) {
        try {
            return URI.create(url).getHost();
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean isAllowedMapsHost(String host) {
        if (host == null || host.isBlank()) {
            return false;
        }
        String h = host.toLowerCase(Locale.ROOT);
        return h.equals("maps.app.goo.gl")
                || h.equals("goo.gl")
                || h.equals("g.co")
                || h.equals("google.com")
                || h.equals("www.google.com")
                || h.equals("maps.google.com")
                || h.equals("www.maps.google.com")
                || h.endsWith(".google.com")
                || h.endsWith(".google.co.in")
                || h.equals("maps.apple.com");
    }

    private static String decode(String raw) {
        try {
            return URLDecoder.decode(raw, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return raw;
        }
    }

    private static Coords tryPatterns(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        Coords c = match(AT_COORDS, text, 1, 2);
        if (c != null) return c;
        c = match(BANG_3D4D, text, 1, 2);
        if (c != null) return c;
        Matcher b43 = BANG_4D3D.matcher(text);
        if (b43.find()) {
            return coords(b43.group(2), b43.group(1));
        }
        c = match(Q_COORDS, text, 1, 2);
        if (c != null) return c;
        c = match(QUERY_COORDS, text, 1, 2);
        if (c != null) return c;
        c = match(CENTER_COORDS, text, 1, 2);
        if (c != null) return c;
        c = match(SLL_COORDS, text, 1, 2);
        if (c != null) return c;
        c = match(LL, text, 1, 2);
        if (c != null) return c;
        Matcher dest = DESTINATION.matcher(text);
        if (dest.find()) {
            if (dest.group(1) != null) {
                return coords(dest.group(1), dest.group(2));
            }
            return coords(dest.group(3), dest.group(4));
        }
        Matcher plain = PLAIN_PAIR.matcher(text.trim());
        if (plain.matches()) {
            return coords(plain.group(1), plain.group(2));
        }
        return null;
    }

    private static Coords match(Pattern p, String text, int latGroup, int lngGroup) {
        Matcher m = p.matcher(text);
        if (m.find()) {
            return coords(m.group(latGroup), m.group(lngGroup));
        }
        return null;
    }

    private static Coords coords(String latStr, String lngStr) {
        try {
            BigDecimal lat = new BigDecimal(latStr).setScale(7, RoundingMode.HALF_UP);
            BigDecimal lng = new BigDecimal(lngStr).setScale(7, RoundingMode.HALF_UP);
            if (lat.abs().compareTo(BigDecimal.valueOf(90)) > 0
                    || lng.abs().compareTo(BigDecimal.valueOf(180)) > 0) {
                return null;
            }
            return new Coords(lat, lng);
        } catch (Exception e) {
            return null;
        }
    }

    public record Coords(BigDecimal latitude, BigDecimal longitude) {
    }
}
