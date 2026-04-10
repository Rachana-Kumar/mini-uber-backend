package com.miniuber.pricing.controller;

import com.miniuber.pricing.service.SurgePricingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pricing")
@RequiredArgsConstructor
public class PricingController {

    private final SurgePricingService surgePricingService;

    @GetMapping("/surge/{zoneId}")
    public ResponseEntity<Double> getSurge(@PathVariable String zoneId) {
        return ResponseEntity.ok(surgePricingService.getSurgeMultiplier(zoneId));
    }

    @PostMapping("/demand/{zoneId}")
    public ResponseEntity<Void> signalDemand(@PathVariable String zoneId) {
        surgePricingService.incrementDemand(zoneId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/supply/{zoneId}")
    public ResponseEntity<Void> signalSupply(@PathVariable String zoneId) {
        surgePricingService.incrementSupply(zoneId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/fare")
    public ResponseEntity<Double> calculateFare(
            @RequestParam double distanceKm,
            @RequestParam String zoneId) {
        double base = 2.50;
        double perKm = 1.20;
        double surge = surgePricingService.getSurgeMultiplier(zoneId);
        double fare  = (base + (distanceKm * perKm)) * surge;
        return ResponseEntity.ok(Math.round(fare * 100.0) / 100.0);
    }
}

