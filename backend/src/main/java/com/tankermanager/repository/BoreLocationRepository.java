package com.tankermanager.repository;

import com.tankermanager.entity.BoreLocation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BoreLocationRepository extends JpaRepository<BoreLocation, Long> {
    List<BoreLocation> findByOperatorIdAndActiveTrue(Long operatorId);
    Optional<BoreLocation> findByIdAndOperatorId(Long id, Long operatorId);
    Optional<BoreLocation> findFirstByOperatorIdAndPrimaryBoreTrueAndActiveTrue(Long operatorId);
}
