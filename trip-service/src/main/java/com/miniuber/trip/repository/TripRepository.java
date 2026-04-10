package com.miniuber.trip.repository;

import com.miniuber.trip.model.Trip;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TripRepository extends JpaRepository<Trip, String> {
    List<Trip> findByPassengerId(String passengerId);
    List<Trip> findByDriverId(String driverId);
    List<Trip> findByState(com.miniuber.trip.model.TripState state);
}

