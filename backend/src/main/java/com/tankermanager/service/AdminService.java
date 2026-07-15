package com.tankermanager.service;

import com.tankermanager.dto.ApiDtos.*;
import com.tankermanager.entity.Operator;
import com.tankermanager.entity.UserAccount;
import com.tankermanager.enums.Role;
import com.tankermanager.enums.SubscriptionPlan;
import com.tankermanager.exception.BadRequestException;
import com.tankermanager.exception.ResourceNotFoundException;
import com.tankermanager.repository.OperatorRepository;
import com.tankermanager.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final OperatorRepository operatorRepository;
    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public OperatorResponse createOperator(CreateOperatorRequest req) {
        if (operatorRepository.existsByCode(req.getCode().toUpperCase())) {
            throw new BadRequestException("Operator code already exists");
        }
        if (userAccountRepository.existsByPhone(req.getOwnerPhone())) {
            throw new BadRequestException("Owner phone already registered");
        }

        Operator operator = operatorRepository.save(Operator.builder()
                .name(req.getName())
                .code(req.getCode().toUpperCase())
                .phone(req.getPhone())
                .email(req.getEmail())
                .address(req.getAddress())
                .plan(req.getPlan() != null ? req.getPlan() : SubscriptionPlan.BASIC)
                .active(true)
                .build());

        userAccountRepository.save(UserAccount.builder()
                .fullName(req.getOwnerName() != null ? req.getOwnerName() : req.getName() + " Owner")
                .phone(req.getOwnerPhone())
                .email(req.getEmail())
                .passwordHash(passwordEncoder.encode(req.getOwnerPassword()))
                .role(Role.OWNER)
                .operator(operator)
                .active(true)
                .build());

        return toOperator(operator);
    }

    public List<OperatorResponse> listOperators() {
        return operatorRepository.findAll().stream().map(this::toOperator).collect(Collectors.toList());
    }

    @Transactional
    public OperatorResponse setActive(Long id, boolean active) {
        Operator op = operatorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Operator not found"));
        op.setActive(active);
        return toOperator(operatorRepository.save(op));
    }

    private OperatorResponse toOperator(Operator o) {
        return OperatorResponse.builder()
                .id(o.getId()).name(o.getName()).code(o.getCode())
                .phone(o.getPhone()).email(o.getEmail()).plan(o.getPlan())
                .active(o.isActive()).subscriptionExpiresOn(o.getSubscriptionExpiresOn())
                .build();
    }
}
