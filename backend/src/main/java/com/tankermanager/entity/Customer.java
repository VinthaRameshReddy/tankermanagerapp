package com.tankermanager.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "customers", indexes = {
        @Index(name = "idx_customers_operator_phone", columnList = "operator_id,phone", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "operator_id", nullable = false)
    private Operator operator;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(length = 500)
    private String defaultAddress;

    private BigDecimal defaultLat;
    private BigDecimal defaultLng;

    @Builder.Default
    private boolean active = true;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;
}
