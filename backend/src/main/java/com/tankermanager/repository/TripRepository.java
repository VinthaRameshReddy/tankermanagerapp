package com.tankermanager.repository;

import com.tankermanager.entity.Trip;
import com.tankermanager.enums.TripStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TripRepository extends JpaRepository<Trip, Long> {
    List<Trip> findByOperatorIdOrderByCreatedAtDesc(Long operatorId);
    List<Trip> findByOperatorIdAndStatus(Long operatorId, TripStatus status);
    List<Trip> findByDriverIdOrderByCreatedAtDesc(Long driverId);
    List<Trip> findByDriverIdAndStatusNotIn(Long driverId, List<TripStatus> statuses);
    Optional<Trip> findByIdAndOperatorId(Long id, Long operatorId);
    Optional<Trip> findByTrackingToken(String trackingToken);
    Optional<Trip> findByTripCode(String tripCode);
    long countByOperatorIdAndStatus(Long operatorId, TripStatus status);
    long countByTankerIdAndStatus(Long tankerId, TripStatus status);
    long countByDriverIdAndStatus(Long driverId, TripStatus status);
}
