package com.miniuber.location.consumer;

import com.miniuber.location.event.LocationUpdateEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.core.GeoOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LocationUpdateConsumer {

    private static final String DRIVER_GEO_KEY = "drivers:locations";
    private final RedisTemplate<String, String> redisTemplate;

    @KafkaListener(topics = "location-updates", groupId = "location-service-group")
    public void handleLocationUpdate(LocationUpdateEvent event) {
        GeoOperations<String, String> geoOps = redisTemplate.opsForGeo();
        geoOps.add(DRIVER_GEO_KEY, new Point(event.getLon(), event.getLat()), event.getDriverId());
        log.debug("Geo-index updated for driver {} → ({}, {})",
                event.getDriverId(), event.getLat(), event.getLon());
    }
}
