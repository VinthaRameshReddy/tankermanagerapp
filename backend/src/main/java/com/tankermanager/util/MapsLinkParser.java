package com.tankermanager.util;

import com.tankermanager.exception.BadRequestException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts lat/lng from common Google Maps share links.
 * Short links (maps.app.goo.gl) without embedded coords cannot be resolved without HTTP follow —
 * ask customer to share the full link or pin coordinates.
 */
public final class MapsLinkParser {

    private static final Pattern AT_COORDS = Pattern.compile("@(-?\\d+\\.\\d+),(-?\\d+\\.\\d+)");
    private static final Pattern Q_COORDS = Pattern.compile("[?&]q=(-?\\d+\\.\\d+),(-?\\d+\\.\\d+)");
    private static final Pattern QUERY_COORDS = Pattern.compile("[?&]query=(-?\\d+\\.\\d+),(-?\\d+\\.\\d+)");
    private static final Pattern BANG_3D4D = Pattern.compile("!3d(-?\\d+\\.\\d+)!4d(-?\\d+\\.\\d+)");
    private static final Pattern LL = Pattern.compile("[?&]ll=(-?\\d+\\.\\d+),(-?\\d+\\.\\d+)");
    private static final Pattern DESTINATION = Pattern.compile("destination=(-?\\d+\\.\\d+)%2C(-?\\d+\\.\\d+)|destination=(-?\\d+\\.\\d+),(-?\\d+\\.\\d+)");
    private static final Pattern PLAIN_PAIR = Pattern.compile("(-?\\d{1,2}\\.\\d{3,}),\\s*(-?\\d{1,3}\\.\\d{3,})");

    private MapsLinkParser() {
    }

    public static Coords parse(String mapsLink) {
        if (mapsLink == null || mapsLink.isBlank()) {
            throw new BadRequestException("Google Maps link is required");
        }
        String raw = mapsLink.trim();
        String decoded;
        try {
            decoded = URLDecoder.decode(raw, StandardCharsets.UTF_8);
        } catch (Exception e) {
            decoded = raw;
        }

        Coords c = tryPatterns(decoded);
        if (c == null) {
            c = tryPatterns(raw);
        }
        if (c == null) {
            throw new BadRequestException(
                    "Could not read coordinates from this Maps link. "
                            + "Ask the customer to open Google Maps → Share → Copy link "
                            + "(full link with @lat,lng), or paste latitude and longitude.");
        }
        return c;
    }

    public static boolean looksLikeMapsLink(String value) {
        if (value == null) {
            return false;
        }
        String v = value.toLowerCase();
        return v.contains("maps.google") || v.contains("google.com/maps")
                || v.contains("goo.gl/maps") || v.contains("maps.app.goo.gl")
                || v.contains("maps.apple.com");
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
        // last resort: first plausible lat,lng pair in the string
        Matcher plain = PLAIN_PAIR.matcher(text);
        if (plain.find()) {
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
