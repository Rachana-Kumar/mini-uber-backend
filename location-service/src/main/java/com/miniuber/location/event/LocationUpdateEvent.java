package com.miniuber.location.event;

import lombok.Data;

@Data
public class LocationUpdateEvent {
    private String driverId;
    private double lat;
    private double lon;
    private double heading;   // direction in degrees
    private double speed;     // km/h
    private long timestamp;
}

