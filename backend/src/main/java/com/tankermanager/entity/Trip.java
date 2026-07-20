package com.tankermanager.entity;

import com.tankermanager.enums.TripStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "tanker_trips", indexes = {
        @Index(name = "idx_tanker_trips_operator", columnList = "operator_id"),
        @Index(name = "idx_tanker_trips_status", columnList = "status"),
        @Index(name = "idx_tanker_trips_customer", columnList = "customer_id"),
        @Index(name = "idx_tanker_trips_driver", columnList = "driver_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Trip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String tripCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "operator_id", nullable = false)
    private Operator operator;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tanker_id", nullable = false)
    private Tanker tanker;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "driver_id", nullable = false)
    private Driver driver;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bore_id", nullable = false)
    private BoreLocation bore;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booked_by_id")
    private UserAccount bookedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    @Builder.Default
    private TripStatus status = TripStatus.ASSIGNED;

    @Column(nullable = false, length = 500)
    private String dropAddress;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal dropLat;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal dropLng;

    /** Estimated minutes to arrival based on maps/traffic */
    private Integer etaMinutes;

    private Instant assignedAt;
    private Instant startedAt;
    private Instant completedAt;

    /** Public tracking token for customer (tracking ends when COMPLETED) */
    @Column(nullable = false, unique = true, length = 64)
    private String trackingToken;

    @Builder.Default
    private boolean trackingEnabled = true;

    @Column(length = 1000)
    private String notes;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;
}
