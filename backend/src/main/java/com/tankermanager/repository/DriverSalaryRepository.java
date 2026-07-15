package com.tankermanager.repository;

import com.tankermanager.entity.DriverSalary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DriverSalaryRepository extends JpaRepository<DriverSalary, Long> {
    List<DriverSalary> findByOperatorIdOrderBySalaryMonthDesc(Long operatorId);
    List<DriverSalary> findByDriverIdOrderBySalaryMonthDesc(Long driverId);
    Optional<DriverSalary> findByDriverIdAndSalaryMonth(Long driverId, String salaryMonth);
}
