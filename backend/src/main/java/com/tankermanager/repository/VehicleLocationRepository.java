package com.tankermanager.repository;

import com.tankermanager.entity.VehicleLocation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VehicleLocationRepository extends JpaRepository<VehicleLocation, Long> {
    List<VehicleLocation> findByTripIdOrderByRecordedAtAsc(Long tripId);
    Optional<VehicleLocation> findFirstByTripIdOrderByRecordedAtDesc(Long tripId);
}
