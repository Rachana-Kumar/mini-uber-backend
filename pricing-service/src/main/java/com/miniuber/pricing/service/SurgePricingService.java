package com.miniuber.pricing.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class SurgePricingService {

    private final RedisTemplate<String, String> redisTemplate;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    // Redis key pattern: surge:zone:{zoneId}
    private static final String SURGE_KEY_PREFIX   = "surge:zone:";
    private static final String DEMAND_KEY_PREFIX  = "demand:zone:";
    private static final String SUPPLY_KEY_PREFIX  = "supply:zone:";

    // Called when a passenger requests a ride in a zone
    public void incrementDemand(String zoneId) {
        String key = DEMAND_KEY_PREFIX + zoneId;
        redisTemplate.opsForValue().increment(key);
        redisTemplate.expire(key, 5, TimeUnit.MINUTES); // sliding window
    }

    // Called when a driver goes online in a zone
    public void incrementSupply(String zoneId) {
        String key = SUPPLY_KEY_PREFIX + zoneId;
        redisTemplate.opsForValue().increment(key);
        redisTemplate.expire(key, 5, TimeUnit.MINUTES);
    }

    public double getSurgeMultiplier(String zoneId) {
        String raw = redisTemplate.opsForValue().get(SURGE_KEY_PREFIX + zoneId);
        return raw != null ? Double.parseDouble(raw) : 1.0;
    }

    // Recalculate surge every 30 seconds for all active zones
    @Scheduled(fixedRate = 30_000)
    public void recalculateSurge() {
        // In a real system you'd iterate known zones; here we demo with a few
        String[] zones = {"zone-north", "zone-south", "zone-east", "zone-west", "zone-center"};
        for (String zoneId : zones) {
            recalculateZone(zoneId);
        }
    }

    private void recalculateZone(String zoneId) {
        String demandStr = redisTemplate.opsForValue().get(DEMAND_KEY_PREFIX + zoneId);
        String supplyStr = redisTemplate.opsForValue().get(SUPPLY_KEY_PREFIX + zoneId);

        double demand = demandStr != null ? Double.parseDouble(demandStr) : 0;
        double supply = supplyStr != null ? Double.parseDouble(supplyStr) : 1; // avoid div/0

        double ratio = demand / supply;
        double multiplier = calculateMultiplier(ratio);

        // Store surge multiplier with 5 min TTL
        redisTemplate.opsForValue().set(
                SURGE_KEY_PREFIX + zoneId,
                String.valueOf(multiplier),
                5, TimeUnit.MINUTES
        );

        if (multiplier > 1.0) {
            log.info("Surge active in {} — demand={}, supply={}, multiplier={}x",
                    zoneId, demand, supply, multiplier);
            kafkaTemplate.send("surge-events", zoneId + ":" + multiplier);
        }
    }

    private double calculateMultiplier(double ratio) {
        // Tiered surge model — easy to explain in interviews
        if (ratio < 0.5) return 1.0;   // more drivers than demand
        if (ratio < 1.0) return 1.2;   // mild demand
        if (ratio < 1.5) return 1.5;   // moderate surge
        if (ratio < 2.0) return 1.8;   // high surge
        return 2.5;                     // extreme surge (capped — Uber caps too)
    }
}

