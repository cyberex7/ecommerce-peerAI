package com.ecommerce.project.kafka.consumer;

import com.ecommerce.project.kafka.event.ReconciliationTriggerEvent;
import com.ecommerce.project.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReconciliationEventConsumer {

    private final OrderService orderService;

    /**
     * Idempotency guard — tracks eventIds already processed in this JVM session.
     *
     * Kafka guarantees at-least-once delivery, meaning the same event can
     * arrive more than once after a broker restart or consumer rebalance.
     * We deduplicate within the current session using the eventId from the payload.
     *
     * For production use, replace this with a Redis SET or a DB-backed
     * processed_events table that survives restarts.
     */
    private final Set<String> processedEventIds = new HashSet<>();

    /**
     * Listens to the order.reconciliation.trigger topic.
     *
     * groupId = "order-reconciliation-group":
     *   - Only one instance in the group processes each message.
     *   - If you deploy multiple app instances, Kafka will assign partitions
     *     across them — no duplicate execution.
     *
     * concurrency is set in application.properties or KafkaListenerContainerFactory.
     */
    @KafkaListener(
            topics = "${kafka.topic.order-reconciliation}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onReconciliationTrigger(
            @Payload ReconciliationTriggerEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        log.info("[ReconciliationConsumer] Received eventId={}, source={}, partition={}, offset={}",
                event.getEventId(), event.getTriggerSource(), partition, offset);

        // Idempotency check
        if (processedEventIds.contains(event.getEventId())) {
            log.warn("[ReconciliationConsumer] Duplicate eventId={} — skipping", event.getEventId());
            return;
        }

        try {
            int promoted = orderService.promotePendingToProcessing();

            if (promoted > 0) {
                log.info("[ReconciliationConsumer] Promoted {} PENDING order(s) to PROCESSING " +
                        "(eventId={}, triggeredAt={})", promoted, event.getEventId(), event.getTriggeredAt());
            } else {
                log.debug("[ReconciliationConsumer] No PENDING orders found (eventId={})",
                        event.getEventId());
            }

            processedEventIds.add(event.getEventId());

        } catch (Exception e) {
            // Re-throw so Spring Kafka's error handler can decide whether to retry
            // (configured via DefaultErrorHandler in KafkaConsumerConfig).
            log.error("[ReconciliationConsumer] Failed processing eventId={}: {}",
                    event.getEventId(), e.getMessage(), e);
            throw e;
        }
    }
}