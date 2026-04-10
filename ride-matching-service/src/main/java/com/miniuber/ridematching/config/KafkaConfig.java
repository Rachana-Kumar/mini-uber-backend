package com.miniuber.ridematching.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    @Bean
    public NewTopic rideRequestsTopic() {
        return TopicBuilder.name("ride-requests")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic rideMatchedTopic() {
        return TopicBuilder.name("ride-matched")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic locationUpdatesTopic() {
        return TopicBuilder.name("location-updates")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic tripEventsTopic() {
        return TopicBuilder.name("trip-events")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic surgeEventsTopic() {
        return TopicBuilder.name("surge-events")
                .partitions(1)
                .replicas(1)
                .build();
    }
}
