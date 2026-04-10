package com.miniuber.trip.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TripStateChangedEvent {
    private String tripId;
    private String passengerId;
    private String driverId;
    private String newState;
    private String reason;
    private long timestamp;
}

