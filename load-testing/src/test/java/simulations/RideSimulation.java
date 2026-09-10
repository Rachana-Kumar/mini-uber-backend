package simulations;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

public class RideSimulation extends Simulation {

    // Base URLs for each service
    private static final String RIDE_MATCHING_URL = "http://localhost:8081";
    private static final String PRICING_URL       = "http://localhost:8083";
    private static final String TRIP_URL          = "http://localhost:8084";

    // Sample NYC coordinates — randomized per user to simulate real spread
    private static final double BASE_LAT = 40.7128;
    private static final double BASE_LON = -74.0060;

    // ── HTTP Protocols ──────────────────────────────────────────────
    HttpProtocolBuilder rideMatchingProtocol = http
            .baseUrl(RIDE_MATCHING_URL)
            .acceptHeader("application/json")
            .contentTypeHeader("application/json");

    HttpProtocolBuilder pricingProtocol = http
            .baseUrl(PRICING_URL)
            .acceptHeader("application/json");

    // ── Driver Scenario (30% of users) ──────────────────────────────
    // Simulates drivers going online and sending location updates
    ScenarioBuilder driverScenario = scenario("Driver — Go Online & Update Location")
            .exec(session -> {
                // Give each virtual driver a unique ID and random starting position
                double lat = BASE_LAT + (ThreadLocalRandom.current().nextDouble() - 0.5) * 0.1;
                double lon = BASE_LON + (ThreadLocalRandom.current().nextDouble() - 0.5) * 0.1;
                return session
                        .set("lat", lat)
                        .set("lon", lon)
                        .set("driverName", "Driver-" + UUID.randomUUID().toString().substring(0, 8));
            })
            // Step 1: Register driver
            .exec(http("Register Driver")
                    .post("/api/rides/drivers/register")
                    .body(StringBody(session ->
                            String.format(
                                    "{\"name\":\"%s\",\"phone\":\"555-%04d\"}",
                                    session.getString("driverName"),
                                    ThreadLocalRandom.current().nextInt(9999)
                            )
                    ))
                    .check(status().is(200))
                    .check(jsonPath("$.id").saveAs("driverId"))
            )
            .exitHereIfFailed()
            .pause(1)
            // Step 2: Send location update (simulates GPS ping)
            .exec(http("Update Driver Location")
                    .post("/api/rides/drivers/#{driverId}/location")
                    .queryParam("lat", "#{lat}")
                    .queryParam("lon", "#{lon}")
                    .check(status().is(200))
            )
            // Step 3: Keep sending location updates every 3 seconds (like a real driver app)
            .repeat(5).on(
                    pause(3)
                            .exec(session -> {
                                double newLat = session.getDouble("lat") + 0.001;
                                double newLon = session.getDouble("lon") + 0.001;
                                return session.set("lat", newLat).set("lon", newLon);
                            })
                            .exec(http("Driver Location Ping")
                                    .post("/api/rides/drivers/#{driverId}/location")
                                    .queryParam("lat", "#{lat}")
                                    .queryParam("lon", "#{lon}")
                                    .check(status().is(200))
                            )
            );

    // ── Passenger Scenario (70% of users) ───────────────────────────
    // Simulates passengers requesting rides
    ScenarioBuilder passengerScenario = scenario("Passenger — Request a Ride")
            .exec(session -> {
                double pickupLat = BASE_LAT + (ThreadLocalRandom.current().nextDouble() - 0.5) * 0.1;
                double pickupLon = BASE_LON + (ThreadLocalRandom.current().nextDouble() - 0.5) * 0.1;
                double dropoffLat = BASE_LAT + (ThreadLocalRandom.current().nextDouble() - 0.5) * 0.2;
                double dropoffLon = BASE_LON + (ThreadLocalRandom.current().nextDouble() - 0.5) * 0.2;
                return session
                        .set("pickupLat",  pickupLat)
                        .set("pickupLon",  pickupLon)
                        .set("dropoffLat", dropoffLat)
                        .set("dropoffLon", dropoffLon)
                        .set("passengerId", UUID.randomUUID().toString());
            })
            // Step 1: Check surge before requesting (realistic — app shows surge first)
            .exec(http("Check Surge Price")
                    .get(PRICING_URL + "/api/pricing/surge/zone-center")
                    .check(status().is(200))
            )
            .pause(1)
            // Step 2: Signal demand in pricing service
            .exec(http("Signal Demand")
                    .post(PRICING_URL + "/api/pricing/demand/zone-center")
                    .check(status().is(200))
            )
            // Step 3: Request the actual ride
            .exec(http("Request Ride")
                    .post("/api/rides/request")
                    .body(StringBody(session ->
                            String.format(
                                    "{\"passengerId\":\"%s\",\"pickupLat\":%f,\"pickupLon\":%f," +
                                            "\"dropoffLat\":%f,\"dropoffLon\":%f}",
                                    session.getString("passengerId"),
                                    session.getDouble("pickupLat"),
                                    session.getDouble("pickupLon"),
                                    session.getDouble("dropoffLat"),
                                    session.getDouble("dropoffLon")
                            )
                    ))
                    .check(status().is(200))
            )
            .pause(2)
            // Step 4: Check nearby drivers (passenger app polling)
            .exec(http("Check Nearby Drivers")
                    .get("/api/rides/drivers/nearby")
                    .queryParam("lat", "#{pickupLat}")
                    .queryParam("lon", "#{pickupLon}")
                    .queryParam("radiusKm", "5.0")
                    .check(status().is(200))
            );

    // ── Load Profile ─────────────────────────────────────────────────
    // This is what you put on your resume: "handled 10K simulated users"
    {
        setUp(
                // Ramp 3000 drivers over 60 seconds
                driverScenario.injectOpen(
                        rampUsers(3000).during(60)
                ),
                // Ramp 7000 passengers over 90 seconds
                passengerScenario.injectOpen(
                        nothingFor(15),           // wait 15s for drivers to register first
                        rampUsers(7000).during(60)
                )
        )
                .protocols(rideMatchingProtocol)
                .assertions(
                        // These assertions make the test pass/fail — great for CI/CD later
                        global().responseTime().percentile(95).lt(2000),  // 95th percentile under 2s
                        global().successfulRequests().percent().gt(95.0)  // at least 95% success rate
                );
    }
}