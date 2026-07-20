package com.tankermanager.service;

import com.tankermanager.dto.AuthDtos.*;
import com.tankermanager.entity.Driver;
import com.tankermanager.entity.Operator;
import com.tankermanager.entity.UserAccount;
import com.tankermanager.enums.Role;
import com.tankermanager.exception.BadRequestException;
import com.tankermanager.exception.ForbiddenException;
import com.tankermanager.exception.ResourceNotFoundException;
import com.tankermanager.repository.DriverRepository;
import com.tankermanager.repository.OperatorRepository;
import com.tankermanager.repository.UserAccountRepository;
import com.tankermanager.security.JwtService;
import com.tankermanager.security.SecurityUtils;
import com.tankermanager.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserAccountRepository userAccountRepository;
    private final OperatorRepository operatorRepository;
    private final DriverRepository driverRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest req) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.getPhone(), req.getPassword()));
        UserAccount user = userAccountRepository.findByPhone(req.getPhone())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (!user.isActive()) {
            throw new ForbiddenException("Account is disabled");
        }
        if (user.getOperator() != null && !user.getOperator().isActive()) {
            throw new ForbiddenException("Operator account is inactive. Contact support.");
        }
        return toAuthResponse(user);
    }

    /**
     * Only OWNER may create MANAGER or DRIVER.
     * Owners themselves are created by Super Admin via /api/admin/operators.
     */
    @Transactional
    public AuthResponse createStaff(CreateStaffRequest req) {
        UserPrincipal current = SecurityUtils.currentUser();
        if (current.getRole() != Role.OWNER) {
            throw new ForbiddenException("Only the owner can create managers and drivers");
        }
        Long operatorId = SecurityUtils.requireOperatorId();

        if (req.getRole() != Role.MANAGER && req.getRole() != Role.DRIVER) {
            throw new BadRequestException("Owner can only create MANAGER or DRIVER");
        }
        if (userAccountRepository.existsByPhone(req.getPhone())) {
            throw new BadRequestException("Phone already registered");
        }

        Operator operator = operatorRepository.findById(operatorId)
                .orElseThrow(() -> new ResourceNotFoundException("Operator not found"));

        UserAccount user = userAccountRepository.save(UserAccount.builder()
                .fullName(req.getFullName())
                .phone(req.getPhone())
                .email(req.getEmail())
                .passwordHash(passwordEncoder.encode(req.getPassword()))
                .role(req.getRole())
                .operator(operator)
                .active(true)
                .build());

        if (req.getRole() == Role.DRIVER) {
            driverRepository.save(Driver.builder()
                    .operator(operator)
                    .user(user)
                    .licenseNumber(req.getLicenseNumber())
                    .licenseExpiry(req.getLicenseExpiry())
                    .monthlySalary(req.getMonthlySalary() != null ? req.getMonthlySalary() : java.math.BigDecimal.ZERO)
                    .available(true)
                    .active(true)
                    .build());
        }

        return toAuthResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse me() {
        UserPrincipal principal = SecurityUtils.currentUser();
        UserAccount user = userAccountRepository.findByIdWithOperator(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return toAuthResponse(user);
    }

    private AuthResponse toAuthResponse(UserAccount user) {
        UserPrincipal principal = new UserPrincipal(user);
        return AuthResponse.builder()
                .token(jwtService.generateToken(principal))
                .userId(user.getId())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .role(user.getRole())
                .operatorId(user.getOperator() != null ? user.getOperator().getId() : null)
                .operatorName(user.getOperator() != null ? user.getOperator().getName() : null)
                .build();
    }
}
