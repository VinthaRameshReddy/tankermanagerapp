package com.tankermanager.repository;

import com.tankermanager.entity.BiometricDevice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BiometricDeviceRepository extends JpaRepository<BiometricDevice, Long> {
    Optional<BiometricDevice> findByDeviceIdAndActiveTrue(String deviceId);

    Optional<BiometricDevice> findByDeviceIdAndUserIdAndActiveTrue(String deviceId, Long userId);

    List<BiometricDevice> findByUserIdAndActiveTrue(Long userId);

    boolean existsByUserIdAndActiveTrue(Long userId);

    long countByUserIdAndActiveTrue(Long userId);
}
