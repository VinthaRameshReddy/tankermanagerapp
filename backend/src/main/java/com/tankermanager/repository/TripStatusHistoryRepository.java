package com.tankermanager.repository;

import com.tankermanager.entity.TripStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TripStatusHistoryRepository extends JpaRepository<TripStatusHistory, Long> {
    List<TripStatusHistory> findByTripIdOrderByChangedAtAsc(Long tripId);
}
