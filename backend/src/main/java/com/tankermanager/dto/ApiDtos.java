package com.tankermanager.dto;

import com.tankermanager.enums.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class ApiDtos {
    private ApiDtos() {}

    @Data
    public static class TankerRequest {
        @NotBlank
        private String vehicleNumber;
        private String model;
        private Integer capacityLitres;
    }

    @Data
    @Builder
    public static class TankerResponse {
        private Long id;
        private String vehicleNumber;
        private String model;
        private Integer capacityLitres;
        private TankerStatus status;
        private BigDecimal lastKnownLat;
        private BigDecimal lastKnownLng;
        private boolean active;
    }

    @Data
    public static class CustomerRequest {
        @NotBlank
        private String name;
        @NotBlank
        private String phone;
        private String defaultAddress;
        private BigDecimal defaultLat;
        private BigDecimal defaultLng;
    }

    @Data
    @Builder
    public static class CustomerResponse {
        private Long id;
        private String name;
        private String phone;
        private String defaultAddress;
        private BigDecimal defaultLat;
        private BigDecimal defaultLng;
    }

    @Data
    public static class BoreRequest {
        @NotBlank
        private String name;
        @NotBlank
        private String address;
        @NotNull
        private BigDecimal latitude;
        @NotNull
        private BigDecimal longitude;
        private boolean primaryBore;
    }

    @Data
    @Builder
    public static class BoreResponse {
        private Long id;
        private String name;
        private String address;
        private BigDecimal latitude;
        private BigDecimal longitude;
        private boolean primaryBore;
    }

    @Data
    public static class BookTripRequest {
        @NotBlank
        private String customerPhone;
        private String customerName;
        @NotNull
        private Long tankerId;
        @NotNull
        private Long driverId;
        private Long boreId;
        @NotBlank
        private String dropAddress;
        @NotNull
        private BigDecimal dropLat;
        @NotNull
        private BigDecimal dropLng;
        private String notes;
    }

    @Data
    public static class UpdateTripStatusRequest {
        @NotNull
        private TripStatus status;
        private String note;
    }

    @Data
    public static class LocationUpdateRequest {
        @NotNull
        private BigDecimal latitude;
        @NotNull
        private BigDecimal longitude;
        private Float speedKmh;
        private Float heading;
    }

    @Data
    @Builder
    public static class TripResponse {
        private Long id;
        private String tripCode;
        private TripStatus status;
        private String customerName;
        private String customerPhone;
        private String tankerNumber;
        private Long driverId;
        private String driverName;
        private String driverPhone;
        private String boreName;
        private String dropAddress;
        private BigDecimal dropLat;
        private BigDecimal dropLng;
        private BigDecimal boreLat;
        private BigDecimal boreLng;
        private Integer etaMinutes;
        private String trackingToken;
        private boolean trackingEnabled;
        private Instant assignedAt;
        private Instant startedAt;
        private Instant completedAt;
        private String notes;
        private List<StatusHistoryItem> history;
    }

    @Data
    @Builder
    public static class StatusHistoryItem {
        private TripStatus fromStatus;
        private TripStatus toStatus;
        private String note;
        private boolean smsSent;
        private Instant changedAt;
    }

    @Data
    @Builder
    public static class TrackingResponse {
        private String tripCode;
        private TripStatus status;
        private boolean trackingEnabled;
        private Integer etaMinutes;
        private BigDecimal vehicleLat;
        private BigDecimal vehicleLng;
        private BigDecimal dropLat;
        private BigDecimal dropLng;
        private String dropAddress;
        private String message;
    }

    @Data
    public static class ExpenseRequest {
        @NotNull
        private Long tankerId;
        @NotNull
        private ExpenseType type;
        @NotNull
        private BigDecimal amount;
        private LocalDate expenseDate;
        private String description;
    }

    @Data
    @Builder
    public static class ExpenseResponse {
        private Long id;
        private Long tankerId;
        private String vehicleNumber;
        private ExpenseType type;
        private BigDecimal amount;
        private LocalDate expenseDate;
        private String description;
    }

    @Data
    public static class BoreExpenseRequest {
        @NotNull
        private Long boreId;
        @NotNull
        private BoreExpenseType type;
        @NotNull
        private BigDecimal amount;
        private LocalDate expenseDate;
        private String description;
    }

    @Data
    @Builder
    public static class BoreExpenseResponse {
        private Long id;
        private Long boreId;
        private String boreName;
        private BoreExpenseType type;
        private BigDecimal amount;
        private LocalDate expenseDate;
        private String description;
    }

    @Data
    public static class SalaryRequest {
        @NotNull
        private Long driverId;
        @NotBlank
        private String salaryMonth; // yyyy-MM
        @NotNull
        private BigDecimal baseAmount;
        private BigDecimal bonus;
        private BigDecimal deductions;
        private String notes;
        private boolean markPaid;
    }

    @Data
    @Builder
    public static class SalaryResponse {
        private Long id;
        private Long driverId;
        private String driverName;
        private String salaryMonth;
        private BigDecimal baseAmount;
        private BigDecimal bonus;
        private BigDecimal deductions;
        private BigDecimal netAmount;
        private boolean paid;
    }

    @Data
    @Builder
    public static class DriverResponse {
        private Long id;
        private Long userId;
        private String fullName;
        private String phone;
        private String licenseNumber;
        private BigDecimal monthlySalary;
        private int totalTripsCompleted;
        private double performanceScore;
        private boolean available;
        private boolean active;
    }

    @Data
    @Builder
    public static class DashboardResponse {
        private long totalTankers;
        private long availableTankers;
        private long onTripTankers;
        private long totalDrivers;
        private long activeTrips;
        private long completedTrips;
        private BigDecimal totalExpenses;
        private BigDecimal totalBoreExpenses;
    }

    @Data
    @Builder
    public static class VehicleReportResponse {
        private Long tankerId;
        private String vehicleNumber;
        private long completedTrips;
        private BigDecimal totalExpenses;
        private TankerStatus status;
    }

    @Data
    @Builder
    public static class StaffResponse {
        private Long id;
        private String fullName;
        private String phone;
        private String role;
        private boolean active;
    }

    @Data
    public static class CreateOperatorRequest {
        @NotBlank
        private String name;
        @NotBlank
        private String code;
        private String phone;
        private String email;
        private String address;
        private SubscriptionPlan plan;
        private String ownerName;
        @NotBlank
        private String ownerPhone;
        @NotBlank
        private String ownerPassword;
    }

    @Data
    @Builder
    public static class OperatorResponse {
        private Long id;
        private String name;
        private String code;
        private String phone;
        private String email;
        private SubscriptionPlan plan;
        private boolean active;
        private LocalDate subscriptionExpiresOn;
    }
}
