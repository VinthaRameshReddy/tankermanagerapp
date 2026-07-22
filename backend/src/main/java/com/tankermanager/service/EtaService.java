package com.tankermanager.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * ETA via OpenRouteService (free OSM-based routing).
 * Falls back to haversine ~25 km/h tanker average when key missing or API fails.
 *
 * Get a free key: https://openrouteservice.org/dev/#/signup
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EtaService {

    private final RestClient.Builder restClientBuilder;
    private final ObjectMapper objectMapper;

    @Value("${app.maps.provider:openrouteservice}")
    private String provider;

    @Value("${app.maps.api-key:}")
    private String mapsApiKey;

    @Value("${app.maps.ors-base-url:https://api.openrouteservice.org}")
    private String orsBaseUrl;

    public double distanceKm(BigDecimal fromLat, BigDecimal fromLng, BigDecimal toLat, BigDecimal toLng) {
        if (fromLat == null || fromLng == null || toLat == null || toLng == null) {
            return 0;
        }
        return haversineKm(
                fromLat.doubleValue(), fromLng.doubleValue(),
                toLat.doubleValue(), toLng.doubleValue());
    }

    public int estimateMinutes(BigDecimal fromLat, BigDecimal fromLng, BigDecimal toLat, BigDecimal toLng) {
        if (fromLat == null || fromLng == null || toLat == null || toLng == null) {
            return 45;
        }

        if (mapsApiKey != null && !mapsApiKey.isBlank()
                && "openrouteservice".equalsIgnoreCase(provider)) {
            try {
                Integer ors = callOpenRouteService(
                        fromLat.doubleValue(), fromLng.doubleValue(),
                        toLat.doubleValue(), toLng.doubleValue());
                if (ors != null) {
                    // small buffer for loading / tanker slowdown in city
                    return Math.max(10, ors + 5);
                }
            } catch (Exception ex) {
                log.warn("OpenRouteService ETA failed, using fallback: {}", ex.getMessage());
            }
        }

        return haversineFallback(
                fromLat.doubleValue(), fromLng.doubleValue(),
                toLat.doubleValue(), toLng.doubleValue());
    }

    private Integer callOpenRouteService(double fromLat, double fromLng, double toLat, double toLng) {
        // ORS expects [longitude, latitude]
        Map<String, Object> body = Map.of(
                "coordinates", List.of(
                        List.of(fromLng, fromLat),
                        List.of(toLng, toLat)
                )
        );

        String base = orsBaseUrl.endsWith("/") ? orsBaseUrl.substring(0, orsBaseUrl.length() - 1) : orsBaseUrl;
        RestClient client = restClientBuilder.build();
        String json = client.post()
                .uri(base + "/v2/directions/driving-car")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", mapsApiKey)
                .body(body)
                .retrieve()
                .body(String.class);

        if (json == null || json.isBlank()) {
            return null;
        }

        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode duration = root.path("routes").path(0).path("summary").path("duration");
            if (duration.isMissingNode() || !duration.isNumber()) {
                // geojson style
                duration = root.path("features").path(0).path("properties").path("summary").path("duration");
            }
            if (duration.isNumber()) {
                double seconds = duration.asDouble();
                return (int) Math.ceil(seconds / 60.0);
            }
        } catch (Exception ex) {
            log.warn("Failed parsing ORS response: {}", ex.getMessage());
        }
        return null;
    }

    private int haversineFallback(double lat1, double lon1, double lat2, double lon2) {
        double km = haversineKm(lat1, lon1, lat2, lon2);
        int minutes = (int) Math.ceil((km / 25.0) * 60) + 10;
        return Math.max(15, minutes);
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
