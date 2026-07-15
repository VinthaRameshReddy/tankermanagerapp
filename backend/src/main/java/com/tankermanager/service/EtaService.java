package com.tankermanager.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * ETA estimation. When GOOGLE_MAPS_API_KEY is set, integrate Directions API with traffic.
 * Fallback uses haversine distance at ~25 km/h tanker average.
 */
@Service
public class EtaService {

    @Value("${app.maps.api-key:}")
    private String mapsApiKey;

    public int estimateMinutes(BigDecimal fromLat, BigDecimal fromLng, BigDecimal toLat, BigDecimal toLng) {
        if (fromLat == null || fromLng == null || toLat == null || toLng == null) {
            return 45;
        }
        double km = haversineKm(
                fromLat.doubleValue(), fromLng.doubleValue(),
                toLat.doubleValue(), toLng.doubleValue());
        // Average urban tanker speed ~25 km/h + 10 min buffer
        int minutes = (int) Math.ceil((km / 25.0) * 60) + 10;
        return Math.max(15, minutes);
        // TODO: if mapsApiKey present, call Google Directions with departure_time=now for traffic ETA
    }

    private double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}
