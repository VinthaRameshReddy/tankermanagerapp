package com.tankermanager.entity;

import com.tankermanager.enums.TripStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "trip_status_history", indexes = {
        @Index(name = "idx_trip_status_trip", columnList = "trip_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TripStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private TripStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private TripStatus toStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by_id")
    private UserAccount changedBy;

    @Column(length = 500)
    private String note;

    private boolean smsSent;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant changedAt;
}
