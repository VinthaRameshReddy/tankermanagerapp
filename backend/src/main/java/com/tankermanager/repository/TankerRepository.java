package com.tankermanager.repository;

import com.tankermanager.entity.Tanker;
import com.tankermanager.enums.TankerStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TankerRepository extends JpaRepository<Tanker, Long> {
    List<Tanker> findByOperatorIdAndActiveTrue(Long operatorId);
    Optional<Tanker> findByIdAndOperatorId(Long id, Long operatorId);
    List<Tanker> findByOperatorIdAndStatus(Long operatorId, TankerStatus status);
    boolean existsByOperatorIdAndVehicleNumber(Long operatorId, String vehicleNumber);
}
