package com.tankermanager.repository;

import com.tankermanager.entity.TripPayment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TripPaymentRepository extends JpaRepository<TripPayment, Long> {
    List<TripPayment> findByTripIdOrderByCreatedAtDesc(Long tripId);
}
