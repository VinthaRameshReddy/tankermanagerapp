package com.tankermanager.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/**
 * Public key for a device-bound biometric login.
 * Private key never leaves the phone (Android Keystore + biometric unlock).
 */
@Entity
@Table(name = "tanker_biometric_devices", indexes = {
        @Index(name = "idx_bio_user", columnList = "user_id"),
        @Index(name = "idx_bio_device", columnList = "device_id", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BiometricDevice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;

    @Column(name = "device_id", nullable = false, unique = true, length = 64)
    private String deviceId;

    /** Base64-encoded X.509 SubjectPublicKeyInfo (EC P-256). */
    @Column(nullable = false, length = 1000)
    private String publicKeyBase64;

    @Column(length = 120)
    private String deviceLabel;

    @Builder.Default
    private boolean active = true;

    private Instant lastUsedAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;
}
