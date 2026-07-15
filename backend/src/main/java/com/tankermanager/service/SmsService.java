package com.tankermanager.service;

import com.tankermanager.entity.SmsLog;
import com.tankermanager.entity.Trip;
import com.tankermanager.enums.TripStatus;
import com.tankermanager.repository.SmsLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SmsService {

    private final SmsLogRepository smsLogRepository;

    @Value("${app.sms.enabled:false}")
    private boolean smsEnabled;

    @Value("${app.sms.provider:console}")
    private String provider;

    @Transactional
    public boolean sendTripSms(Trip trip, TripStatus status) {
        String phone = trip.getCustomer().getPhone();
        String message = buildMessage(trip, status);
        boolean success = dispatch(phone, message);

        smsLogRepository.save(SmsLog.builder()
                .operator(trip.getOperator())
                .trip(trip)
                .phone(phone)
                .message(message)
                .success(success)
                .providerResponse(provider)
                .build());
        return success;
    }

    private String buildMessage(Trip trip, TripStatus status) {
        String vehicle = trip.getTanker().getVehicleNumber();
        String eta = trip.getEtaMinutes() != null ? trip.getEtaMinutes() + " mins" : "soon";
        return switch (status) {
            case ASSIGNED -> "Trip " + trip.getTripCode() + " assigned. Vehicle " + vehicle
                    + " is going to load at bore. Track: /track/" + trip.getTrackingToken();
            case GOING_FOR_LOADING -> "Vehicle " + vehicle + " is going for loading at bore.";
            case LOADING -> "Vehicle " + vehicle + " is loading water at bore.";
            case LOADING_COMPLETED -> "Loading completed for vehicle " + vehicle + ". Coming for delivery.";
            case EN_ROUTE -> "Trip started! Vehicle " + vehicle + " is on the way. ETA ~ " + eta
                    + ". Track live until delivery completes.";
            case ARRIVED -> "Vehicle " + vehicle + " has arrived at your location.";
            case UNLOADING -> "Unloading in progress at your location.";
            case COMPLETED -> "Trip " + trip.getTripCode() + " completed. Thank you!";
            case CANCELLED -> "Trip " + trip.getTripCode() + " has been cancelled.";
        };
    }

    private boolean dispatch(String phone, String message) {
        if (!smsEnabled) {
            log.info("[SMS-CONSOLE] to={} msg={}", phone, message);
            return true;
        }
        // Plug Fast2SMS / MSG91 / Twilio here using app.sms.api-key
        log.info("[SMS-{}] to={} msg={}", provider, phone, message);
        return true;
    }
}
