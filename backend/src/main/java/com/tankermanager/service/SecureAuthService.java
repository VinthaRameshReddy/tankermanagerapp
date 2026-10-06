package com.tankermanager.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tankermanager.dto.AuthDtos.*;
import com.tankermanager.entity.AuthChallenge;
import com.tankermanager.entity.BiometricDevice;
import com.tankermanager.entity.UserAccount;
import com.tankermanager.exception.BadRequestException;
import com.tankermanager.exception.ForbiddenException;
import com.tankermanager.exception.ResourceNotFoundException;
import com.tankermanager.repository.AuthChallengeRepository;
import com.tankermanager.repository.BiometricDeviceRepository;
import com.tankermanager.repository.UserAccountRepository;
import com.tankermanager.security.JwtService;
import com.tankermanager.security.SecurityUtils;
import com.tankermanager.security.UserPrincipal;
import com.tankermanager.util.AesGcmPayloadCodec;
import com.tankermanager.util.Sha256Util;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SecureAuthService {

    private static final int CHALLENGE_TTL_SECONDS = 120;

    private final UserAccountRepository userAccountRepository;
    private final BiometricDeviceRepository biometricDeviceRepository;
    private final AuthChallengeRepository authChallengeRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AesGcmPayloadCodec payloadCodec;
    private final ObjectMapper objectMapper;

    @Transactional
    public AuthResponse setMpinEncrypted(EncryptedPayloadRequest req) {
        SetMpinPlain plain = decrypt(req.getPayload(), SetMpinPlain.class);
        return setMpin(plain);
    }

    @Transactional
    public AuthResponse setMpin(SetMpinPlain req) {
        UserPrincipal principal = SecurityUtils.currentUser();
        UserAccount user = userAccountRepository.findByIdWithOperator(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String dig = Sha256Util.normalizeClientShaOrHash(req.getMpin());
        validateMpinDigestRepresentsPin(req.getMpin());

        if (user.isMpinEnabled() && user.getMpinHash() != null) {
            if (req.getCurrentMpin() == null || req.getCurrentMpin().isBlank()) {
                throw new BadRequestException("Current MPIN required to change MPIN");
            }
            String currentDig = Sha256Util.normalizeClientShaOrHash(req.getCurrentMpin());
            if (!passwordEncoder.matches(currentDig, user.getMpinHash())) {
                throw new ForbiddenException("Current MPIN is incorrect");
            }
        }

        user.setMpinHash(passwordEncoder.encode(dig));
        user.setMpinEnabled(true);
        userAccountRepository.save(user);
        return toAuthResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse loginWithMpinEncrypted(EncryptedPayloadRequest req) {
        MpinLoginPlain plain = decrypt(req.getPayload(), MpinLoginPlain.class);
        return loginWithMpin(plain);
    }

    @Transactional(readOnly = true)
    public AuthResponse loginWithMpin(MpinLoginPlain req) {
        UserAccount user = userAccountRepository.findByPhone(req.getPhone().trim())
                .orElseThrow(() -> new ForbiddenException("Invalid phone or MPIN"));
        assertUserActive(user);
        if (!user.isMpinEnabled() || user.getMpinHash() == null) {
            throw new BadRequestException("MPIN is not set for this account");
        }
        String dig = Sha256Util.normalizeClientShaOrHash(req.getMpin());
        if (!passwordEncoder.matches(dig, user.getMpinHash())) {
            throw new ForbiddenException("Invalid phone or MPIN");
        }
        return toAuthResponse(user);
    }

    @Transactional
    public Map<String, Object> registerBiometricEncrypted(EncryptedPayloadRequest req) {
        RegisterBiometricPlain plain = decrypt(req.getPayload(), RegisterBiometricPlain.class);
        return registerBiometric(plain);
    }

    @Transactional
    public Map<String, Object> registerBiometric(RegisterBiometricPlain req) {
        UserPrincipal principal = SecurityUtils.currentUser();
        UserAccount user = userAccountRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (!user.isMpinEnabled()) {
            throw new BadRequestException("Set MPIN before enabling biometric login");
        }
        validatePublicKey(req.getPublicKeyBase64());

        BiometricDevice device = biometricDeviceRepository.findByDeviceIdAndActiveTrue(req.getDeviceId())
                .orElse(null);
        if (device != null && !device.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("Device already registered to another account");
        }
        if (device == null) {
            device = BiometricDevice.builder()
                    .user(user)
                    .deviceId(req.getDeviceId().trim())
                    .build();
        }
        device.setPublicKeyBase64(req.getPublicKeyBase64().trim());
        device.setDeviceLabel(req.getDeviceLabel());
        device.setActive(true);
        biometricDeviceRepository.save(device);
        return Map.of(
                "registered", true,
                "deviceId", device.getDeviceId(),
                "biometricEnabled", true
        );
    }

    @Transactional
    public BiometricChallengeResponse createBiometricChallenge(BiometricChallengeRequest req) {
        UserAccount user = userAccountRepository.findByPhone(req.getPhone().trim())
                .orElseThrow(() -> new ForbiddenException("Biometric login not available"));
        assertUserActive(user);
        BiometricDevice device = biometricDeviceRepository
                .findByDeviceIdAndUserIdAndActiveTrue(req.getDeviceId().trim(), user.getId())
                .orElseThrow(() -> new ForbiddenException("Biometric not registered for this device"));

        String challengeId = UUID.randomUUID().toString().replace("-", "");
        String nonce = UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
        Instant expires = Instant.now().plusSeconds(CHALLENGE_TTL_SECONDS);
        authChallengeRepository.save(AuthChallenge.builder()
                .challengeId(challengeId)
                .user(user)
                .deviceId(device.getDeviceId())
                .nonce(nonce)
                .expiresAt(expires)
                .used(false)
                .build());

        BiometricChallengeResponse res = BiometricChallengeResponse.builder()
                .challengeId(challengeId)
                .nonce(nonce)
                .expiresAt(expires)
                .build();
        try {
            res.setSecurePayload(payloadCodec.encryptJson(objectMapper.writeValueAsString(Map.of(
                    "challengeId", challengeId,
                    "nonce", nonce,
                    "expiresAt", expires.toString(),
                    "signPayload", signPayload(challengeId, nonce, user.getPhone(), device.getDeviceId())
            ))));
        } catch (Exception ignored) {
            // optional secure envelope
        }
        return res;
    }

    @Transactional
    public AuthResponse verifyBiometricEncrypted(EncryptedPayloadRequest req) {
        BiometricVerifyPlain plain = decrypt(req.getPayload(), BiometricVerifyPlain.class);
        return verifyBiometric(plain);
    }

    @Transactional
    public AuthResponse verifyBiometric(BiometricVerifyPlain req) {
        AuthChallenge challenge = authChallengeRepository.findByChallengeIdAndUsedFalse(req.getChallengeId())
                .orElseThrow(() -> new BadRequestException("Invalid or expired challenge"));
        if (challenge.getExpiresAt().isBefore(Instant.now())) {
            throw new BadRequestException("Challenge expired — try again");
        }
        if (!challenge.getDeviceId().equals(req.getDeviceId().trim())) {
            throw new ForbiddenException("Device mismatch");
        }
        UserAccount user = challenge.getUser();
        if (!user.getPhone().equals(req.getPhone().trim())) {
            throw new ForbiddenException("Phone mismatch");
        }
        assertUserActive(user);

        BiometricDevice device = biometricDeviceRepository
                .findByDeviceIdAndUserIdAndActiveTrue(req.getDeviceId().trim(), user.getId())
                .orElseThrow(() -> new ForbiddenException("Biometric device not found"));

        String payload = signPayload(challenge.getChallengeId(), challenge.getNonce(), user.getPhone(), device.getDeviceId());
        if (!verifyEcdsaSignature(device.getPublicKeyBase64(), payload, req.getSignatureBase64())) {
            throw new ForbiddenException("Biometric signature invalid");
        }

        challenge.setUsed(true);
        authChallengeRepository.save(challenge);
        device.setLastUsedAt(Instant.now());
        biometricDeviceRepository.save(device);
        return toAuthResponse(user);
    }

    @Transactional
    public Map<String, Object> revokeBiometric(String deviceId) {
        UserPrincipal principal = SecurityUtils.currentUser();
        BiometricDevice device = biometricDeviceRepository
                .findByDeviceIdAndUserIdAndActiveTrue(deviceId.trim(), principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Biometric device not found"));
        device.setActive(false);
        biometricDeviceRepository.save(device);
        return Map.of("revoked", true, "biometricEnabled",
                biometricDeviceRepository.existsByUserIdAndActiveTrue(principal.getId()));
    }

    public static String signPayload(String challengeId, String nonce, String phone, String deviceId) {
        return challengeId + "|" + nonce + "|" + phone + "|" + deviceId;
    }

    private AuthResponse toAuthResponse(UserAccount user) {
        UserPrincipal principal = new UserPrincipal(user);
        boolean bio = biometricDeviceRepository.existsByUserIdAndActiveTrue(user.getId());
        AuthResponse res = AuthResponse.builder()
                .token(jwtService.generateToken(principal))
                .userId(user.getId())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .role(user.getRole())
                .operatorId(user.getOperator() != null ? user.getOperator().getId() : null)
                .operatorName(user.getOperator() != null ? user.getOperator().getName() : null)
                .mpinEnabled(user.isMpinEnabled())
                .biometricEnabled(bio)
                .build();
        try {
            res.setSecurePayload(payloadCodec.encryptJson(objectMapper.writeValueAsString(Map.of(
                    "token", res.getToken(),
                    "userId", res.getUserId(),
                    "phone", res.getPhone(),
                    "role", res.getRole().name(),
                    "mpinEnabled", res.isMpinEnabled(),
                    "biometricEnabled", res.isBiometricEnabled()
            ))));
        } catch (Exception ignored) {
        }
        return res;
    }

    private void assertUserActive(UserAccount user) {
        if (!user.isActive()) {
            throw new ForbiddenException("Account is disabled");
        }
        if (user.getOperator() != null && !user.getOperator().isActive()) {
            throw new ForbiddenException("Operator account is inactive. Contact support.");
        }
    }

    private void validateMpinDigestRepresentsPin(String mpinOrSha) {
        if (mpinOrSha == null) {
            throw new BadRequestException("MPIN required");
        }
        String v = mpinOrSha.trim();
        if (v.matches("^[0-9a-fA-F]{64}$")) {
            return; // client already hashed a validated pin
        }
        if (!v.matches("^\\d{4,6}$")) {
            throw new BadRequestException("MPIN must be 4–6 digits");
        }
    }

    private void validatePublicKey(String publicKeyBase64) {
        try {
            byte[] der = Base64.getDecoder().decode(publicKeyBase64.trim());
            KeyFactory.getInstance("EC").generatePublic(new X509EncodedKeySpec(der));
        } catch (Exception e) {
            throw new BadRequestException("Invalid biometric public key");
        }
    }

    private boolean verifyEcdsaSignature(String publicKeyBase64, String payload, String signatureBase64) {
        try {
            byte[] der = Base64.getDecoder().decode(publicKeyBase64.trim());
            PublicKey key = KeyFactory.getInstance("EC").generatePublic(new X509EncodedKeySpec(der));
            Signature sig = Signature.getInstance("SHA256withECDSA");
            sig.initVerify(key);
            sig.update(payload.getBytes(StandardCharsets.UTF_8));
            return sig.verify(Base64.getDecoder().decode(signatureBase64.trim()));
        } catch (Exception e) {
            return false;
        }
    }

    private <T> T decrypt(String payload, Class<T> type) {
        try {
            String json = payloadCodec.decryptToJson(payload);
            return objectMapper.readValue(json, type);
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new BadRequestException("Invalid secure payload");
        }
    }
}
