package com.tankermanager.repository;

import com.tankermanager.entity.DriverSalary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DriverSalaryRepository extends JpaRepository<DriverSalary, Long> {

    @Query("SELECT s FROM DriverSalary s JOIN FETCH s.driver d JOIN FETCH d.user WHERE s.operator.id = :operatorId ORDER BY s.salaryMonth DESC")
    List<DriverSalary> findByOperatorIdOrderBySalaryMonthDesc(@Param("operatorId") Long operatorId);

    List<DriverSalary> findByDriverIdOrderBySalaryMonthDesc(Long driverId);

    Optional<DriverSalary> findByDriverIdAndSalaryMonth(Long driverId, String salaryMonth);
}
