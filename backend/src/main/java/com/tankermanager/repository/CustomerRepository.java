package com.tankermanager.repository;

import com.tankermanager.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
    List<Customer> findByOperatorIdAndActiveTrue(Long operatorId);
    Optional<Customer> findByOperatorIdAndPhone(Long operatorId, String phone);
    Optional<Customer> findByIdAndOperatorId(Long id, Long operatorId);
}
