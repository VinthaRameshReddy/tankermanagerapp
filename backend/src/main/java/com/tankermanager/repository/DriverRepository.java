package com.tankermanager.repository;

import com.tankermanager.entity.Driver;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DriverRepository extends JpaRepository<Driver, Long> {
    List<Driver> findByOperatorIdAndActiveTrue(Long operatorId);
    Optional<Driver> findByIdAndOperatorId(Long id, Long operatorId);
    Optional<Driver> findByUserId(Long userId);
    List<Driver> findByOperatorIdAndAvailableTrueAndActiveTrue(Long operatorId);
}
