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
