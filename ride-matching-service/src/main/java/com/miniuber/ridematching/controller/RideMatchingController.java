package com.miniuber.ridematching.controller;

import com.miniuber.ridematching.event.RideRequestedEvent;
import com.miniuber.ridematching.model.Driver;
import com.miniuber.ridematching.model.DriverStatus;
import com.miniuber.ridematching.repository.DriverRepository;
import com.miniuber.ridematching.service.DriverLocationGeoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/rides")
@RequiredArgsConstructor
public class RideMatchingController {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final DriverRepository driverRepository;
    private final DriverLocationGeoService geoService;

    // Passenger requests a ride
    @PostMapping("/request")
    public ResponseEntity<String> requestRide(@RequestBody RideRequestedEvent request) {
        String rideId = UUID.randomUUID().toString();
        request.setRideRequestId(rideId);
        request.setSurgePriceMultiplier(1.0); // pricing service will update this
        kafkaTemplate.send("ride-requests", request);
        return ResponseEntity.ok("Ride requested: " + rideId);
    }

    // Register a new driver
    @PostMapping("/drivers/register")
    public ResponseEntity<Driver> registerDriver(@RequestBody Driver driver) {
        driver.setAvailable(true);
        driver.setStatus(DriverStatus.ONLINE);
        return ResponseEntity.ok(driverRepository.save(driver));
    }

    // Driver goes online and sets their location
    @PostMapping("/drivers/{driverId}/location")
    public ResponseEntity<Void> updateDriverLocation(
            @PathVariable String driverId,
            @RequestParam double lat,
            @RequestParam double lon) {
        geoService.updateDriverLocation(driverId, lat, lon);
        return ResponseEntity.ok().build();
    }

    // See nearby drivers (useful for testing)
    @GetMapping("/drivers/nearby")
    public ResponseEntity<List<String>> getNearbyDrivers(
            @RequestParam double lat,
            @RequestParam double lon,
            @RequestParam(defaultValue = "5.0") double radiusKm) {
        return ResponseEntity.ok(geoService.findNearbyDrivers(lat, lon, radiusKm));
    }
}
