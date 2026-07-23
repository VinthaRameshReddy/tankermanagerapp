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
 * Extracts lat/lng from Google Maps share links, including short links
 * (maps.app.goo.gl / goo.gl/maps) by following redirects to the full URL.
 */
public final class MapsLinkParser {

    private static final Logger log = LoggerFactory.getLogger(MapsLinkParser.class);

    private static final Pattern AT_COORDS = Pattern.compile("@(-?\\d+\\.\\d+),(-?\\d+\\.\\d+)");
    private static final Pattern Q_COORDS = Pattern.compile("[?&]q=(-?\\d+\\.\\d+),(-?\\d+\\.\\d+)");
    private static final Pattern QUERY_COORDS = Pattern.compile("[?&]query=(-?\\d+\\.\\d+),(-?\\d+\\.\\d+)");
    private static final Pattern BANG_3D4D = Pattern.compile("!3d(-?\\d+\\.\\d+)!4d(-?\\d+\\.\\d+)");
    private static final Pattern LL = Pattern.compile("[?&]ll=(-?\\d+\\.\\d+),(-?\\d+\\.\\d+)");
    private static final Pattern DESTINATION = Pattern.compile(
            "destination=(-?\\d+\\.\\d+)%2C(-?\\d+\\.\\d+)|destination=(-?\\d+\\.\\d+),(-?\\d+\\.\\d+)");
    private static final Pattern PLAIN_PAIR = Pattern.compile("(-?\\d{1,2}\\.\\d{3,}),\\s*(-?\\d{1,3}\\.\\d{3,})");

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    private MapsLinkParser() {
    }

    public static Coords parse(String mapsLink) {
        if (mapsLink == null || mapsLink.isBlank()) {
            throw new BadRequestException("Google Maps link is required");
        }
        String raw = mapsLink.trim();
        String decoded = decode(raw);

        Coords c = tryPatterns(decoded);
        if (c == null) {
            c = tryPatterns(raw);
        }
        if (c == null && looksLikeShortOrMapsLink(raw)) {
            String resolved = resolveRedirectChain(raw);
            if (resolved != null && !resolved.equalsIgnoreCase(raw)) {
                String resolvedDecoded = decode(resolved);
                c = tryPatterns(resolvedDecoded);
                if (c == null) {
                    c = tryPatterns(resolved);
                }
            }
        }
        if (c == null) {
            throw new BadRequestException(
                    "Could not read coordinates from this Maps link. "
                            + "Short links (maps.app.goo.gl) are supported — check the link is valid, "
                            + "or paste the full Maps link / latitude,longitude.");
        }
        return c;
    }

    public static boolean looksLikeMapsLink(String value) {
        return looksLikeShortOrMapsLink(value);
    }

    private static boolean looksLikeShortOrMapsLink(String value) {
        if (value == null) {
            return false;
        }
        String v = value.toLowerCase(Locale.ROOT);
        return v.contains("maps.google") || v.contains("google.com/maps")
                || v.contains("goo.gl/maps") || v.contains("maps.app.goo.gl")
                || v.contains("maps.apple.com") || v.contains("g.co/maps");
    }

    private static boolean isShortLink(String value) {
        String v = value.toLowerCase(Locale.ROOT);
        return v.contains("maps.app.goo.gl") || v.contains("goo.gl/maps") || v.contains("g.co/maps");
    }

    /**
     * Follows Location redirects manually (max 10 hops), only for Google Maps hosts (SSRF-safe).
     * Tries to parse coords from each hop URL.
     */
    private static String resolveRedirectChain(String startUrl) {
        String current = normalizeHttpUrl(startUrl);
        if (current == null || !isAllowedMapsHost(hostOf(current))) {
            return null;
        }
        String lastUseful = current;
        try {
            for (int hop = 0; hop < 10; hop++) {
                Coords already = tryPatterns(decode(current));
                if (already == null) {
                    already = tryPatterns(current);
                }
                if (already != null && !isShortLink(current)) {
                    return current;
                }

                HttpRequest request = HttpRequest.newBuilder(URI.create(current))
                        .timeout(Duration.ofSeconds(12))
                        .header("User-Agent", "TankerManager/1.0 (MapsLinkResolver)")
                        .header("Accept", "text/html,application/xhtml+xml")
                        .GET()
                        .build();
                HttpResponse<Void> response = HTTP.send(request, HttpResponse.BodyHandlers.discarding());
                int status = response.statusCode();
                if (status >= 300 && status < 400) {
                    String location = response.headers().firstValue("Location").orElse(null);
                    if (location == null || location.isBlank()) {
                        break;
                    }
                    String next = resolveRelative(current, location.trim());
                    if (next == null || !isAllowedMapsHost(hostOf(next))) {
                        log.warn("Refusing Maps redirect to non-Maps host: {}", location);
                        break;
                    }
                    lastUseful = next;
                    current = next;
                    continue;
                }
                // Some short-link endpoints return 200 with HTML that still embeds the target URL
                if (status >= 200 && status < 300) {
                    return current;
                }
                break;
            }
        } catch (Exception e) {
            log.warn("Failed to resolve Maps short link {}: {}", startUrl, e.toString());
            return lastUseful;
        }
        return lastUseful;
    }

    private static String normalizeHttpUrl(String raw) {
        String u = raw.trim();
        if (u.startsWith("//")) {
            u = "https:" + u;
        } else if (!u.startsWith("http://") && !u.startsWith("https://")) {
            u = "https://" + u;
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
            URI resolved = URI.create(base).resolve(location);
            return resolved.toString();
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
        Coords c = match(AT_COORDS, text, 1, 2);
        if (c != null) return c;
        c = match(Q_COORDS, text, 1, 2);
        if (c != null) return c;
        c = match(QUERY_COORDS, text, 1, 2);
        if (c != null) return c;
        c = match(BANG_3D4D, text, 1, 2);
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
        // Avoid matching short-link path garbage as coords
        if (!isShortLink(text)) {
            Matcher plain = PLAIN_PAIR.matcher(text);
            if (plain.find()) {
                return coords(plain.group(1), plain.group(2));
            }
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
