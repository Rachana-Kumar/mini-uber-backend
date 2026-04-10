package com.miniuber.trip.controller;

import com.miniuber.trip.model.Trip;
import com.miniuber.trip.service.TripService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/trips")
@RequiredArgsConstructor
public class TripController {

    private final TripService tripService;

    @GetMapping("/{tripId}")
    public ResponseEntity<Trip> getTrip(@PathVariable String tripId) {
        return ResponseEntity.ok(tripService.getTrip(tripId));
    }

    // Driver calls this to advance: EN_ROUTE → ARRIVED → IN_PROGRESS → COMPLETED
    @PostMapping("/{tripId}/advance")
    public ResponseEntity<Trip> advanceState(@PathVariable String tripId) {
        return ResponseEntity.ok(tripService.advanceState(tripId));
    }

    @PostMapping("/{tripId}/cancel")
    public ResponseEntity<Trip> cancel(@PathVariable String tripId) {
        return ResponseEntity.ok(tripService.cancelTrip(tripId));
    }
}
