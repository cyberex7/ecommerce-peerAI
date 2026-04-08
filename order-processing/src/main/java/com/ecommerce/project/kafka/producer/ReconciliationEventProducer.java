package com.ecommerce.project.kafka.producer;

import com.ecommerce.project.kafka.event.ReconciliationTriggerEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReconciliationEventProducer {

    private final KafkaTemplate<String, ReconciliationTriggerEvent> kafkaTemplate;

    @Value("${kafka.topic.order-reconciliation}")
    private String topic;

    /**
     * Publishes a reconciliation trigger event.
     *
     * Key = eventId — ensures events with the same key land on the same
     * partition (useful for ordering guarantees if you add more event types later).
     */
    public void publishTrigger(@org.checkerframework.checker.nullness.qual.MonotonicNonNull String event) {
        log.info("[ReconciliationProducer] Publishing trigger: eventId={}, source={}",
                event.getEventId(), event.getTriggerSource());

        CompletableFuture<SendResult<String, ReconciliationTriggerEvent>> future =
                kafkaTemplate.send(topic, event.getEventId(), event);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("[ReconciliationProducer] Failed to publish eventId={}: {}",
                        event.getEventId(), ex.getMessage(), ex);
            } else {
                log.debug("[ReconciliationProducer] Delivered eventId={} → partition={}, offset={}",
                        event.getEventId(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            }
        });
    }
}