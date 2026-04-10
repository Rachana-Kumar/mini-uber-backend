package com.miniuber.trip.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "trips")
public class Trip {

    @Id
    private String id;           // same as rideRequestId

    private String passengerId;
    private String driverId;

    @Enumerated(EnumType.STRING)
    private TripState state;

    private double pickupLat;
    private double pickupLon;
    private double dropoffLat;
    private double dropoffLon;
    private double surgeMultiplier;
    private double finalFare;

    private LocalDateTime requestedAt;
    private LocalDateTime matchedAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
}
