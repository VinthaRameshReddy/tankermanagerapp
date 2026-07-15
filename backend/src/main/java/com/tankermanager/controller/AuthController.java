package com.tankermanager.controller;

import com.tankermanager.dto.AuthDtos.*;
import com.tankermanager.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /** Public login only. Owners are created by Super Admin. */
    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    public AuthResponse me() {
        return authService.me();
    }

    /**
     * Owner creates MANAGER or DRIVER for their operator.
     * Managers cannot create staff — only customers/trips.
     */
    @PostMapping("/staff")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse createStaff(@Valid @RequestBody CreateStaffRequest request) {
        return authService.createStaff(request);
    }
}
