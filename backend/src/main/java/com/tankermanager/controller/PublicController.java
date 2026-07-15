package com.tankermanager.controller;

import com.tankermanager.dto.ApiDtos.TrackingResponse;
import com.tankermanager.service.TripService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicController {

    private final TripService tripService;

    @GetMapping("/track/{token}")
    public TrackingResponse track(@PathVariable String token) {
        return tripService.publicTrack(token);
    }

    @GetMapping("/health")
    public String health() {
        return "OK";
    }
}
