// TripCompletionConsumer.java
package com.miniuber.ridematching.consumer;

import com.miniuber.ridematching.model.DriverStatus;
import com.miniuber.ridematching.repository.DriverRepository;
import com.miniuber.ridematching.service.DriverLocationGeoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class TripCompletionConsumer {

    private final DriverRepository driverRepository;
    private final DriverLocationGeoService geoService;

    @KafkaListener(topics = "trip-events", groupId = "ride-matching-group")
    public void handleTripEvent(Map<String, Object> event) {
        String newState = (String) event.get("newState");
        String driverId = (String) event.get("driverId");

        if ("COMPLETED".equals(newState) || "CANCELLED".equals(newState)) {
            driverRepository.findById(driverId).ifPresent(driver -> {
                driver.setAvailable(true);
                driver.setStatus(DriverStatus.ONLINE);
                driverRepository.save(driver);
                log.info("Driver {} is now available again after trip {}",
                        driverId, newState.toLowerCase());
            });
        }
    }
}