package com.tankermanager.entity;

import com.tankermanager.enums.ExpenseType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "expenses", indexes = {
        @Index(name = "idx_expenses_operator", columnList = "operator_id"),
        @Index(name = "idx_expenses_tanker", columnList = "tanker_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "operator_id", nullable = false)
    private Operator operator;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tanker_id", nullable = false)
    private Tanker tanker;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ExpenseType type;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    private LocalDate expenseDate;

    @Column(length = 500)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recorded_by_id")
    private UserAccount recordedBy;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
}
