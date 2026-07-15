package com.tankermanager.controller;

import com.tankermanager.dto.ApiDtos.*;
import com.tankermanager.service.TripService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/driver")
@RequiredArgsConstructor
public class DriverController {

    private final TripService tripService;

    @GetMapping("/trips")
    public List<TripResponse> myTrips() {
        return tripService.listForDriver();
    }

    @GetMapping("/trips/active")
    public List<TripResponse> activeTrips() {
        return tripService.listActiveForDriver();
    }

    @PatchMapping("/trips/{id}/status")
    public TripResponse updateStatus(@PathVariable Long id, @Valid @RequestBody UpdateTripStatusRequest request) {
        return tripService.updateStatus(id, request, true);
    }

    @PostMapping("/trips/{id}/location")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateLocation(@PathVariable Long id, @Valid @RequestBody LocationUpdateRequest request) {
        tripService.updateLocation(id, request);
    }
}
