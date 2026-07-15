package com.tankermanager.repository;

import com.tankermanager.entity.SmsLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SmsLogRepository extends JpaRepository<SmsLog, Long> {
    List<SmsLog> findByTripIdOrderBySentAtDesc(Long tripId);
}
