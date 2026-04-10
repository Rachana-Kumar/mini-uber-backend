package com.miniuber.trip.model;

public enum TripState {
    REQUESTED,
    MATCHED,       // driver assigned
    EN_ROUTE,      // driver heading to pickup
    ARRIVED,       // driver at pickup location
    IN_PROGRESS,   // passenger in car
    COMPLETED,
    CANCELLED
}
