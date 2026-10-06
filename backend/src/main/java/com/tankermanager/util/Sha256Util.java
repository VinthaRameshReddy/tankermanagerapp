package com.tankermanager.util;

import com.tankermanager.exception.BadRequestException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

public final class Sha256Util {
    private Sha256Util() {}

    public static String hex(String input) {
        if (input == null) {
            throw new BadRequestException("Value required for hashing");
        }
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] dig = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(dig);
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    /** Accept client-provided SHA-256 hex (64 chars) or hash raw input. */
    public static String normalizeClientShaOrHash(String value) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException("MPIN digest required");
        }
        String v = value.trim().toLowerCase();
        if (v.matches("^[0-9a-f]{64}$")) {
            return v;
        }
        return hex(v);
    }
}
