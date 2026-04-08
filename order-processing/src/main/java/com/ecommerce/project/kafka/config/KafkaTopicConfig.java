package com.ecommerce.project.kafka.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Value("${reconciliation.kafka.topic:order-reconciliation-trigger}")
    private String reconciliationTopic;

    /**
     * Spring will auto-create this topic on startup if it doesn't already exist.
     *
     * partitions=1  — single partition is correct here because reconciliation
     *                 is a singleton job; multiple partitions would cause
     *                 multiple consumers to run the DB update concurrently.
     * replicas=1    — increase to 3 in production with a multi-broker cluster.
     */
    @Bean
    public NewTopic reconciliationTopic() {
        return TopicBuilder.name(reconciliationTopic)
                .partitions(1)
                .replicas(1)
                .build();
    }
}