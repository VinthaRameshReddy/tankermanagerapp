package com.tankermanager.repository;

import com.tankermanager.entity.Driver;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DriverRepository extends JpaRepository<Driver, Long> {

    @Query("SELECT d FROM Driver d JOIN FETCH d.user WHERE d.operator.id = :operatorId AND d.active = true")
    List<Driver> findByOperatorIdAndActiveTrue(@Param("operatorId") Long operatorId);

    @Query("SELECT d FROM Driver d JOIN FETCH d.user WHERE d.operator.id = :operatorId AND d.available = true AND d.active = true")
    List<Driver> findByOperatorIdAndAvailableTrueAndActiveTrue(@Param("operatorId") Long operatorId);

    @Query("SELECT d FROM Driver d JOIN FETCH d.user WHERE d.id = :id AND d.operator.id = :operatorId")
    Optional<Driver> findByIdAndOperatorId(@Param("id") Long id, @Param("operatorId") Long operatorId);

    @Query("SELECT d FROM Driver d JOIN FETCH d.user WHERE d.user.id = :userId")
    Optional<Driver> findByUserId(@Param("userId") Long userId);
}
