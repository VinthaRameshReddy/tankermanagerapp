package com.tankermanager.entity;

import com.tankermanager.enums.TankerStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "tanker_fleet", indexes = {
        @Index(name = "idx_tanker_fleet_operator", columnList = "operator_id"),
        @Index(name = "idx_tanker_fleet_number", columnList = "vehicle_number")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tanker {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "operator_id", nullable = false)
    private Operator operator;

    @Column(nullable = false, length = 30)
    private String vehicleNumber;

    @Column(length = 80)
    private String model;

    /** Capacity in litres */
    private Integer capacityLitres;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private TankerStatus status = TankerStatus.AVAILABLE;

    private BigDecimal lastKnownLat;
    private BigDecimal lastKnownLng;

    @Builder.Default
    private boolean active = true;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;
}
