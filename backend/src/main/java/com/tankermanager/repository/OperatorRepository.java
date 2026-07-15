package com.tankermanager.repository;

import com.tankermanager.entity.Operator;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OperatorRepository extends JpaRepository<Operator, Long> {
    Optional<Operator> findByCode(String code);
    boolean existsByCode(String code);
}
