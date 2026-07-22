package com.tankermanager.repository;

import com.tankermanager.entity.CustomerLocation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CustomerLocationRepository extends JpaRepository<CustomerLocation, Long> {
    List<CustomerLocation> findByCustomerIdAndActiveTrueOrderByCreatedAtDesc(Long customerId);

    Optional<CustomerLocation> findByIdAndCustomerOperatorId(Long id, Long operatorId);
}
