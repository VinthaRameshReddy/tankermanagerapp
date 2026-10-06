package com.tankermanager.controller;

import com.tankermanager.dto.AuthDtos.*;
import com.tankermanager.service.AuthService;
import com.tankermanager.service.SecureAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final SecureAuthService secureAuthService;

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

    /** Set / change MPIN — body is AES-GCM encrypted JSON (SHA-256 of MPIN inside). */
    @PostMapping("/mpin/set")
    public AuthResponse setMpin(@Valid @RequestBody EncryptedPayloadRequest request) {
        return secureAuthService.setMpinEncrypted(request);
    }

    /** Login with MPIN — encrypted payload; validated only on backend. */
    @PostMapping("/mpin/login")
    public AuthResponse mpinLogin(@Valid @RequestBody EncryptedPayloadRequest request) {
        return secureAuthService.loginWithMpinEncrypted(request);
    }

    /** Register this device's biometric public key (authenticated). */
    @PostMapping("/biometric/register")
    public Map<String, Object> registerBiometric(@Valid @RequestBody EncryptedPayloadRequest request) {
        return secureAuthService.registerBiometricEncrypted(request);
    }

    /** Start biometric login — returns one-time challenge to sign. */
    @PostMapping("/biometric/challenge")
    public BiometricChallengeResponse biometricChallenge(@Valid @RequestBody BiometricChallengeRequest request) {
        return secureAuthService.createBiometricChallenge(request);
    }

    /** Complete biometric login — encrypted signature payload verified on backend. */
    @PostMapping("/biometric/verify")
    public AuthResponse biometricVerify(@Valid @RequestBody EncryptedPayloadRequest request) {
        return secureAuthService.verifyBiometricEncrypted(request);
    }

    @DeleteMapping("/biometric/{deviceId}")
    public Map<String, Object> revokeBiometric(@PathVariable String deviceId) {
        return secureAuthService.revokeBiometric(deviceId);
    }
}
