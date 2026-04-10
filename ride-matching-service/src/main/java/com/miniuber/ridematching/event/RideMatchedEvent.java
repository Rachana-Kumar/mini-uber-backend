package com.miniuber.ridematching.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RideMatchedEvent {
    private String rideRequestId;
    private String passengerId;
    private String driverId;
    private double estimatedArrivalMinutes;
}

