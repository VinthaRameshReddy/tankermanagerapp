package com.tankermanager.util;

import com.tankermanager.exception.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-256-GCM for sensitive auth payloads.
 * Wire format (Base64): iv(12) || ciphertext+tag
 */
@Component
public class AesGcmPayloadCodec {

    private static final int IV_LEN = 12;
    private static final int TAG_BITS = 128;

    private final byte[] keyBytes;
    private final SecureRandom random = new SecureRandom();

    public AesGcmPayloadCodec(@Value("${app.secure.payload-key:}") String payloadKey) {
        if (payloadKey == null || payloadKey.isBlank()) {
            // Deterministic fallback for local/dev only — override in production via env.
            payloadKey = "Tank3rFlowLocalPayloadKeyChangeMe!!";
        }
        this.keyBytes = deriveKey(payloadKey);
    }

    public String encryptJson(String json) {
        try {
            byte[] iv = new byte[IV_LEN];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(keyBytes, "AES"), new GCMParameterSpec(TAG_BITS, iv));
            byte[] cipherText = cipher.doFinal(json.getBytes(StandardCharsets.UTF_8));
            ByteBuffer buf = ByteBuffer.allocate(iv.length + cipherText.length);
            buf.put(iv);
            buf.put(cipherText);
            return Base64.getEncoder().encodeToString(buf.array());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to encrypt payload", e);
        }
    }

    public String decryptToJson(String payloadBase64) {
        if (payloadBase64 == null || payloadBase64.isBlank()) {
            throw new BadRequestException("Encrypted payload required");
        }
        try {
            byte[] all = Base64.getDecoder().decode(payloadBase64.trim());
            if (all.length <= IV_LEN + 16) {
                throw new BadRequestException("Invalid encrypted payload");
            }
            byte[] iv = new byte[IV_LEN];
            System.arraycopy(all, 0, iv, 0, IV_LEN);
            byte[] cipherText = new byte[all.length - IV_LEN];
            System.arraycopy(all, IV_LEN, cipherText, 0, cipherText.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(keyBytes, "AES"), new GCMParameterSpec(TAG_BITS, iv));
            byte[] plain = cipher.doFinal(cipherText);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new BadRequestException("Could not decrypt secure payload");
        }
    }

    private static byte[] deriveKey(String secret) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return md.digest(secret.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
