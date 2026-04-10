# 🚕 Mini-Uber Backend Architecture

![Java](https://img.shields.io/badge/Java-21-orange?style=for-the-badge&logo=java)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.2-brightgreen?style=for-the-badge&logo=spring)
![Apache Kafka](https://img.shields.io/badge/Kafka-Event_Driven-black?style=for-the-badge&logo=apachekafka)
![Redis](https://img.shields.io/badge/Redis-Geo_Index-red?style=for-the-badge&logo=redis)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-Relational-blue?style=for-the-badge&logo=postgresql)
![Docker](https://img.shields.io/badge/Docker-Containerized-2496ED?style=for-the-badge&logo=docker)

## 📌 Overview
A highly scalable, event-driven microservices architecture replicating the core backend functionality of a ride-sharing platform (like Uber or Lyft).

Designed to handle real-time geospatial queries, bidirectional driver tracking, and asynchronous trip state management, this project demonstrates modern system design principles tailored for high throughput and low latency.

## ✨ Key Features & Engineering Highlights
* **Sub-Millisecond Driver Matching:** Utilizes **Redis Geospatial Indexes** (`GEOADD`, `GEORADIUS`) to instantly query and match the nearest available drivers within a 5km radius of a passenger's pickup location.
* **Event-Driven Decoupling:** Implements **Apache Kafka** as a central message broker. Services interact asynchronously via Pub/Sub topics (`ride-requested`, `ride-matched`, `location-updates`), ensuring fault tolerance and preventing cascading failures.
* **Real-Time GPS Tracking:** Uses **Spring WebSockets** to maintain persistent, bidirectional connections with drivers, streaming coordinate updates every 3 seconds with minimal overhead.
* **Dynamic Surge Pricing:** A localized supply-and-demand algorithm using Redis sliding-window counters to dynamically calculate fare multipliers based on active requests vs. available drivers in specific geographic zones.
* **Auditable State Machine:** Manages the complex lifecycle of a ride (Requested → Matched → En Route → Arrived → In Progress → Completed) using centralized, event-sourced transitions stored securely in **PostgreSQL**.

---

## 🏗️ Microservices Architecture

```text
 📱 PASSENGER APP                                         🚗 DRIVER APP
        │                                                       │
        │ 1. GET /estimate                                      │ 3. WebSocket stream
        │ 2. POST /request                                      │    every 3 seconds
        ▼                                                       ▼
 🚪 API GATEWAY (Port 8080)                              🚪 API GATEWAY (Port 8080)
        │                                                       │
        ├─────────────────────────┐                             │
        │                         │                             │
        ▼                         ▼                             ▼
 💰 PRICING SERVICE        🚕 TRIP SERVICE               📍 LOCATION SERVICE
    (Port 8083)               (Port 8084)                   (Port 8082)
        │                         │                             │
        │ Reads Active            │ Writes Trip                 │ Writes Long/Lat
        │ Trip Count              │ Status                      │ to Geo-Index
        ▼                         ▼                             ▼
   [ REDIS ]                [ POSTGRESQL ]                  [ REDIS ]
        ▲                         │                             ▲
        │                         │ 4. Publishes                │ 5. Queries 5km
        │                         ▼    Event                    │    Radius
        │                [ KAFKA MESSAGE BUS ]                  │
        │                Topic: ride-requested                  │
        │                         │                             │
        │                         │                             │
        │                         ▼                             │
        └──────────────── 🤝 RIDE MATCHING SERVICE ─────────────┘
                               (Port 8081)
                                  │
                                  │ 6. Publishes Match
                                  ▼
                         [ KAFKA MESSAGE BUS ]
                         Topic: ride-matched
                                  │
                                  │ 7. Consumes Match
                                  ▼
                             🚕 TRIP SERVICE
                            (Updates DB to MATCHED)