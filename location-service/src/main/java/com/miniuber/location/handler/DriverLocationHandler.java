package com.miniuber.location.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.miniuber.location.event.LocationUpdateEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class DriverLocationHandler extends TextWebSocketHandler {

    // Keep track of all active driver WebSocket sessions
    private final Map<String, WebSocketSession> activeSessions = new ConcurrentHashMap<>();

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String driverId = extractDriverId(session);
        activeSessions.put(driverId, session);
        log.info("Driver {} connected via WebSocket. Active sessions: {}",
                driverId, activeSessions.size());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String driverId = extractDriverId(session);
        LocationUpdateEvent event = objectMapper.readValue(message.getPayload(),
                LocationUpdateEvent.class);
        event.setDriverId(driverId);
        event.setTimestamp(System.currentTimeMillis());

        // Publish to Kafka so ride-matching-service can update Redis geo-index
        kafkaTemplate.send("location-updates", driverId, event);
        log.debug("Location update from driver {}: ({}, {})",
                driverId, event.getLat(), event.getLon());

        // Echo back an ACK so the driver app knows we got it
        session.sendMessage(new TextMessage("{\"status\":\"ok\"}"));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String driverId = extractDriverId(session);
        activeSessions.remove(driverId);
        log.info("Driver {} disconnected. Active sessions: {}",
                driverId, activeSessions.size());
    }

    // Push a message TO a specific driver (e.g. "you've been matched")
    public void sendMessageToDriver(String driverId, Object message) throws Exception {
        WebSocketSession session = activeSessions.get(driverId);
        if (session != null && session.isOpen()) {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(message)));
        }
    }

    public int getActiveDriverCount() {
        return activeSessions.size();
    }

    private String extractDriverId(WebSocketSession session) {
        // The URL is /ws/driver/{driverId}/location
        String path = session.getUri().getPath();
        String[] parts = path.split("/");
        return parts[3]; // index 3 = driverId
    }
}
