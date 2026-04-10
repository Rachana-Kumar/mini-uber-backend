package com.miniuber.ridematching.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "ride_requests")
public class RideRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String passengerId;
    private double pickupLat;
    private double pickupLon;
    private double dropoffLat;
    private double dropoffLon;

    @Enumerated(EnumType.STRING)
    private RideStatus status;

    private String assignedDriverId;
    private LocalDateTime requestedAt;
}
