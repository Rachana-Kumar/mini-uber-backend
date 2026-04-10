package com.miniuber.ridematching.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.geo.*;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.GeoOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DriverLocationGeoService {

    // Redis key that holds ALL driver geo-coordinates
    private static final String DRIVER_GEO_KEY = "drivers:locations";

    private final RedisTemplate<String, String> redisTemplate;

    // Called every time a driver sends a GPS update
    public void updateDriverLocation(String driverId, double lat, double lon) {
        GeoOperations<String, String> geoOps = redisTemplate.opsForGeo();
        geoOps.add(DRIVER_GEO_KEY, new Point(lon, lat), driverId);
        log.debug("Updated location for driver {} → ({}, {})", driverId, lat, lon);
    }

    // Find nearest available drivers within radiusKm kilometres
    public List<String> findNearbyDrivers(double lat, double lon, double radiusKm) {
        GeoOperations<String, String> geoOps = redisTemplate.opsForGeo();

        Circle searchArea = new Circle(
                new Point(lon, lat),
                new Distance(radiusKm, Metrics.KILOMETERS)
        );

        RedisGeoCommands.GeoRadiusCommandArgs args = RedisGeoCommands
                .GeoRadiusCommandArgs.newGeoRadiusArgs()
                .includeDistance()
                .sortAscending()   // closest first
                .limit(10);        // cap at 10 candidates

        GeoResults<RedisGeoCommands.GeoLocation<String>> results =
                geoOps.radius(DRIVER_GEO_KEY, searchArea, args);

        if (results == null) return Collections.emptyList();

        return results.getContent().stream()
                .map(r -> r.getContent().getName())
                .collect(Collectors.toList());
    }

    public void removeDriver(String driverId) {
        redisTemplate.opsForGeo().remove(DRIVER_GEO_KEY, driverId);
    }
}
