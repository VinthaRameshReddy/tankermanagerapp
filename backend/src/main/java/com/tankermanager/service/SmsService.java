package com.tankermanager.service;

import com.tankermanager.entity.SmsLog;
import com.tankermanager.entity.Trip;
import com.tankermanager.enums.TripStatus;
import com.tankermanager.repository.SmsLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * SMS providers (India):
 * - fast2sms (recommended for India — cheap transactional SMS)
 * - msg91
 * - console (default for local)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SmsService {

    private final SmsLogRepository smsLogRepository;
    private final RestClient.Builder restClientBuilder;

    @Value("${app.sms.enabled:false}")
    private boolean smsEnabled;

    @Value("${app.sms.provider:console}")
    private String provider;

    @Value("${app.sms.api-key:}")
    private String apiKey;

    @Value("${app.sms.sender-id:TANKER}")
    private String senderId;

    @Value("${app.public-base-url:}")
    private String publicBaseUrl;

    @Transactional
    public boolean sendTripSms(Trip trip, TripStatus status) {
        String phone = trip.getCustomer().getPhone();
        String message = buildMessage(trip, status);
        String providerResponse;
        boolean success;
        try {
            providerResponse = dispatch(phone, message);
            success = true;
        } catch (Exception ex) {
            log.error("SMS failed to {}: {}", phone, ex.getMessage());
            providerResponse = ex.getMessage();
            success = false;
        }

        smsLogRepository.save(SmsLog.builder()
                .operator(trip.getOperator())
                .trip(trip)
                .phone(phone)
                .message(message)
                .success(success)
                .providerResponse(providerResponse)
                .build());
        return success;
    }

    private String buildMessage(Trip trip, TripStatus status) {
        String vehicle = trip.getTanker().getVehicleNumber();
        String eta = trip.getEtaMinutes() != null ? trip.getEtaMinutes() + " mins" : "soon";
        String track = trackUrl(trip.getTrackingToken());
        return switch (status) {
            case ASSIGNED -> "Trip " + trip.getTripCode() + " assigned. Vehicle " + vehicle
                    + " going to load at bore. Track: " + track;
            case GOING_FOR_LOADING -> "Vehicle " + vehicle + " is going for loading at bore.";
            case LOADING -> "Vehicle " + vehicle + " is loading water at bore.";
            case LOADING_COMPLETED -> "Loading completed for " + vehicle + ". Coming for delivery.";
            case EN_ROUTE -> "Trip started! " + vehicle + " on the way. ETA ~ " + eta
                    + ". Live track: " + track;
            case ARRIVED -> "Vehicle " + vehicle + " has arrived at your location.";
            case UNLOADING -> "Unloading in progress at your location.";
            case COMPLETED -> "Trip " + trip.getTripCode() + " completed. Thank you!";
            case CANCELLED -> "Trip " + trip.getTripCode() + " has been cancelled.";
        };
    }

    private String trackUrl(String token) {
        if (publicBaseUrl != null && !publicBaseUrl.isBlank()) {
            String base = publicBaseUrl.endsWith("/") ? publicBaseUrl.substring(0, publicBaseUrl.length() - 1) : publicBaseUrl;
            return base + "/api/public/track/" + token;
        }
        return "tankermanager://track/" + token;
    }

    private String dispatch(String phone, String message) {
        if (!smsEnabled || "console".equalsIgnoreCase(provider)) {
            log.info("[SMS-CONSOLE] to={} msg={}", phone, message);
            return "console";
        }
        String digits = phone.replaceAll("[^0-9]", "");
        if (digits.length() > 10) {
            digits = digits.substring(digits.length() - 10);
        }

        return switch (provider.toLowerCase()) {
            case "fast2sms" -> sendFast2Sms(digits, message);
            case "msg91" -> sendMsg91(digits, message);
            default -> {
                log.info("[SMS-{}] to={} msg={}", provider, digits, message);
                yield provider + "-logged";
            }
        };
    }

    private String sendFast2Sms(String phone, String message) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("SMS_API_KEY missing for Fast2SMS");
        }
        // Fast2SMS quick SMS API — https://www.fast2sms.com/docs
        RestClient client = restClientBuilder.build();
        String response = client.get()
                .uri(uriBuilder -> uriBuilder
                        .scheme("https")
                        .host("www.fast2sms.com")
                        .path("/dev/bulkV2")
                        .queryParam("authorization", apiKey)
                        .queryParam("route", "q")
                        .queryParam("message", message)
                        .queryParam("language", "english")
                        .queryParam("flash", "0")
                        .queryParam("numbers", phone)
                        .build())
                .retrieve()
                .body(String.class);
        log.info("[SMS-FAST2SMS] to={} response={}", phone, response);
        return response;
    }

    private String sendMsg91(String phone, String message) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("SMS_API_KEY missing for MSG91");
        }
        RestClient client = restClientBuilder.build();
        String encoded = URLEncoder.encode(message, StandardCharsets.UTF_8);
        String response = client.get()
                .uri("https://api.msg91.com/api/sendhttp.php?authkey={auth}&mobiles=91{mobile}&message={msg}&sender={sender}&route=4",
                        apiKey, phone, encoded, senderId)
                .retrieve()
                .body(String.class);
        log.info("[SMS-MSG91] to={} response={}", phone, response);
        return response;
    }
}
