package com.ecommerce.project.scheduler;

import com.ecommerce.project.kafka.producer.ReconciliationEventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Every 5 minutes: publishes a ReconciliationTriggerEvent to Kafka.
 * The actual DB work (PENDING → PROCESSING) is done by ReconciliationEventConsumer.
 *
 * Flow:
 *   Scheduler (every 5 min)
 *     → publishes to topic: order-reconciliation-trigger
 *       → ReconciliationEventConsumer
 *           → OrderService.promotePendingToProcessing()
 *               → UPDATE orders SET order_status='PROCESSING' WHERE order_status='PENDING'
 *
 * Requires @EnableScheduling on OrderEcomApplication.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderReconciliationScheduler {

    private final ReconciliationEventProducer producer;

    @Scheduled(fixedRate = 300_000)
    public void triggerReconciliation() {
        log.info("[OrderReconciliationScheduler] Firing reconciliation trigger");
        try {
            producer.publishTrigger("SCHEDULED");
        } catch (Exception e) {
            log.error("[OrderReconciliationScheduler] Failed to publish trigger: {}", e.getMessage(), e);
        }
    }
}