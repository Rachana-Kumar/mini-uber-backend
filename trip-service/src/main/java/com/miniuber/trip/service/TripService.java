package com.miniuber.trip.service;

import com.miniuber.trip.event.RideMatchedEvent;
import com.miniuber.trip.event.TripStateChangedEvent;
import com.miniuber.trip.model.Trip;
import com.miniuber.trip.model.TripState;
import com.miniuber.trip.repository.TripRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class TripService {

    private final TripRepository tripRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    // React when ride-matching-service matches a driver
    @KafkaListener(topics = "ride-matched", groupId = "trip-service-group")
    @Transactional
    public void handleRideMatched(RideMatchedEvent event) {
        Trip trip = new Trip();
        trip.setId(event.getRideRequestId());
        trip.setPassengerId(event.getPassengerId());
        trip.setDriverId(event.getDriverId());
        trip.setState(TripState.MATCHED);
        trip.setMatchedAt(LocalDateTime.now());
        trip.setSurgeMultiplier(1.0);
        tripRepository.save(trip);

        publishStateChange(trip, "Driver assigned, heading to pickup");
        log.info("Trip {} created in MATCHED state", trip.getId());
    }

    public Trip getTrip(String tripId) {
        return tripRepository.findById(tripId)
                .orElseThrow(() -> new RuntimeException("Trip not found: " + tripId));
    }

    @Transactional
    public Trip advanceState(String tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new RuntimeException("Trip not found: " + tripId));

        TripState next = nextState(trip.getState());
        if (next == null) {
            throw new IllegalStateException("Trip " + tripId + " is already in terminal state");
        }

        trip.setState(next);
        applyTimestamp(trip, next);
        tripRepository.save(trip);

        publishStateChange(trip, "State advanced to " + next);
        log.info("Trip {} advanced to {}", tripId, next);
        return trip;
    }

    @Transactional
    public Trip cancelTrip(String tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new RuntimeException("Trip not found: " + tripId));
        trip.setState(TripState.CANCELLED);
        tripRepository.save(trip);
        publishStateChange(trip, "Trip cancelled");
        return trip;
    }

    private TripState nextState(TripState current) {
        return switch (current) {
            case MATCHED      -> TripState.EN_ROUTE;
            case EN_ROUTE     -> TripState.ARRIVED;
            case ARRIVED      -> TripState.IN_PROGRESS;
            case IN_PROGRESS  -> TripState.COMPLETED;
            default           -> null; // COMPLETED / CANCELLED are terminal
        };
    }

    private void applyTimestamp(Trip trip, TripState state) {
        if (state == TripState.IN_PROGRESS) trip.setStartedAt(LocalDateTime.now());
        if (state == TripState.COMPLETED)   trip.setCompletedAt(LocalDateTime.now());
    }

    private void publishStateChange(Trip trip, String reason) {
        TripStateChangedEvent event = new TripStateChangedEvent(
                trip.getId(), trip.getPassengerId(), trip.getDriverId(),
                trip.getState().name(), reason, System.currentTimeMillis()
        );
        kafkaTemplate.send("trip-events", trip.getId(), event);
    }
}

