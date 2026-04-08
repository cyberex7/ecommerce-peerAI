package com.ecommerce.project.kafka.event;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Message published to the order-reconciliation-trigger topic.
 *
 * Intentionally a trigger-only event — no order IDs embedded.
 * The consumer queries the DB itself so it always works on the
 * freshest data, not IDs that may have already changed status
 * between publish time and consume time.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class ReconciliationTriggerEvent {

    /** Unique ID — lets the consumer log/deduplicate if needed */
    @Builder.Default
    private String eventId = UUID.randomUUID().toString();

    /** "SCHEDULED" or "MANUAL" */
    private String triggerSource;

    @Builder.Default
    private LocalDateTime triggeredAt = LocalDateTime.now();
}