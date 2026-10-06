package com.tankermanager.dto;

import com.tankermanager.enums.Role;
import com.tankermanager.enums.SubscriptionPlan;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public final class AuthDtos {
    private AuthDtos() {}

    @Data
    public static class LoginRequest {
        @NotBlank
        private String phone;
        @NotBlank
        private String password;
    }

    @Data
    public static class RegisterOwnerRequest {
        @NotBlank
        private String operatorName;
        @NotBlank
        @Size(min = 3, max = 80)
        private String operatorCode;
        @NotBlank
        private String ownerName;
        @NotBlank
        private String phone;
        private String email;
        @NotBlank
        @Size(min = 6)
        private String password;
        private String address;
    }

    @Data
    @Builder
    public static class AuthResponse {
        private String token;
        private Long userId;
        private String fullName;
        private String phone;
        private Role role;
        private Long operatorId;
        private String operatorName;
        private boolean mpinEnabled;
        private boolean biometricEnabled;
        /** Encrypted JSON of the same fields (AES-GCM Base64) for secure clients. */
        private String securePayload;
    }

    @Data
    public static class EncryptedPayloadRequest {
        /** Base64 AES-256-GCM: iv(12) || ciphertext+tag */
        @NotBlank
        private String payload;
    }

    @Data
    public static class SetMpinPlain {
        /** Raw 4–6 digit MPIN OR client SHA-256 hex of MPIN. */
        @NotBlank
        private String mpin;
        private String currentMpin;
    }

    @Data
    public static class MpinLoginPlain {
        @NotBlank
        private String phone;
        /** Raw MPIN or SHA-256 hex of MPIN. */
        @NotBlank
        private String mpin;
    }

    @Data
    public static class RegisterBiometricPlain {
        @NotBlank
        private String deviceId;
        /** Base64 X.509 EC public key (P-256). */
        @NotBlank
        private String publicKeyBase64;
        private String deviceLabel;
    }

    @Data
    public static class BiometricChallengeRequest {
        @NotBlank
        private String phone;
        @NotBlank
        private String deviceId;
    }

    @Data
    @Builder
    public static class BiometricChallengeResponse {
        private String challengeId;
        private String nonce;
        private Instant expiresAt;
        private String securePayload;
    }

    @Data
    public static class BiometricVerifyPlain {
        @NotBlank
        private String phone;
        @NotBlank
        private String deviceId;
        @NotBlank
        private String challengeId;
        /** Base64 DER ECDSA signature over SHA-256(challenge payload). */
        @NotBlank
        private String signatureBase64;
    }

    @Data
    public static class CreateStaffRequest {
        @NotBlank
        private String fullName;
        @NotBlank
        private String phone;
        private String email;
        @NotBlank
        @Size(min = 6)
        private String password;
        @NotNull
        private Role role; // MANAGER or DRIVER only (OWNER is created by Super Admin)
        private String licenseNumber;
        private LocalDate licenseExpiry;
        private BigDecimal monthlySalary;
    }
}
