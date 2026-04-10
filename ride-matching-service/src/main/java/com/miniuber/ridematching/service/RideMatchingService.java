package com.miniuber.ridematching.service;

import com.miniuber.ridematching.event.RideMatchedEvent;
import com.miniuber.ridematching.event.RideRequestedEvent;
import com.miniuber.ridematching.model.*;
import com.miniuber.ridematching.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RideMatchingService {

    private final DriverLocationGeoService geoService;
    private final DriverRepository driverRepository;
    private final RideRequestRepository rideRequestRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    // Listens for passenger ride requests from Kafka
    @KafkaListener(topics = "ride-requests", groupId = "ride-matching-group")
    @Transactional
    public void handleRideRequest(RideRequestedEvent event) {
        log.info("Received ride request {} from passenger {}",
                event.getRideRequestId(), event.getPassengerId());

        // 1. Persist the request
        RideRequest request = new RideRequest();
        request.setId(event.getRideRequestId());
        request.setPassengerId(event.getPassengerId());
        request.setPickupLat(event.getPickupLat());
        request.setPickupLon(event.getPickupLon());
        request.setDropoffLat(event.getDropoffLat());
        request.setDropoffLon(event.getDropoffLon());
        request.setStatus(RideStatus.REQUESTED);
        request.setRequestedAt(LocalDateTime.now());
        rideRequestRepository.save(request);

        // 2. Find nearby drivers via Redis geo-search (5km radius)
        List<String> nearbyDriverIds = geoService.findNearbyDrivers(
                event.getPickupLat(), event.getPickupLon(), 5.0
        );

        if (nearbyDriverIds.isEmpty()) {
            log.warn("No drivers found near request {}", event.getRideRequestId());
            request.setStatus(RideStatus.CANCELLED);
            rideRequestRepository.save(request);
            return;
        }

        // 3. Pick the closest available driver
        String assignedDriverId = nearbyDriverIds.stream()
                .filter(driverId -> driverRepository.findById(driverId)
                        .map(Driver::isAvailable).orElse(false))
                .findFirst()
                .orElse(null);

        if (assignedDriverId == null) {
            log.warn("All nearby drivers are busy for request {}", event.getRideRequestId());
            request.setStatus(RideStatus.CANCELLED);
            rideRequestRepository.save(request);
            return;
        }

        // 4. Mark driver unavailable, update request
        driverRepository.findById(assignedDriverId).ifPresent(driver -> {
            driver.setAvailable(false);
            driver.setStatus(DriverStatus.ON_TRIP);
            driverRepository.save(driver);
        });

        request.setAssignedDriverId(assignedDriverId);
        request.setStatus(RideStatus.MATCHED);
        rideRequestRepository.save(request);

        // 5. Publish match event so other services react
        RideMatchedEvent matchedEvent = new RideMatchedEvent(
                event.getRideRequestId(),
                event.getPassengerId(),
                assignedDriverId,
                3.5  // placeholder ETA in minutes — Phase 3 will calculate this properly
        );

        kafkaTemplate.send("ride-matched", matchedEvent);
        log.info("Matched ride {} to driver {}", event.getRideRequestId(), assignedDriverId);
    }
}
