package com.tankermanager.repository;

import com.tankermanager.entity.UserAccount;
import com.tankermanager.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {

    @Query("SELECT u FROM UserAccount u LEFT JOIN FETCH u.operator WHERE u.phone = :phone")
    Optional<UserAccount> findByPhone(@Param("phone") String phone);

    boolean existsByPhone(String phone);

    @Query("SELECT u FROM UserAccount u LEFT JOIN FETCH u.operator WHERE u.id = :id")
    Optional<UserAccount> findByIdWithOperator(@Param("id") Long id);

    List<UserAccount> findByOperatorIdAndRole(Long operatorId, Role role);

    List<UserAccount> findByOperatorIdAndRoleIn(Long operatorId, List<Role> roles);

    List<UserAccount> findByOperatorId(Long operatorId);
}
