package com.tankermanager.repository;

import com.tankermanager.entity.UserAccount;
import com.tankermanager.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {
    Optional<UserAccount> findByPhone(String phone);
    boolean existsByPhone(String phone);
    List<UserAccount> findByOperatorIdAndRole(Long operatorId, Role role);
    List<UserAccount> findByOperatorIdAndRoleIn(Long operatorId, List<Role> roles);
    List<UserAccount> findByOperatorId(Long operatorId);
}
