package com.repertorio.hermandad.adapter.inbound.kafka;

import com.repertorio.hermandad.adapter.outbound.events.ProcessedEventJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.UUID;

/**
 * Consumes events from hermandad topics for idempotency tracking.
 *
 * This consumer deduplicates events via the processed_event table and logs
 * them. Current purpose: operational audit trail — confirms events crossed
 * the outbox → Kafka boundary successfully.
 *
 * ponytail: sink — no downstream processing wired yet. Add business logic
 * here when a consumer for these events is needed (e.g., cross-service
 * notifications, cache invalidation, webhook dispatch).
 */
// ponytail: audit-only sink. Events are consumed, deduplicated, logged,
// and discarded. Add real processing when a concrete consumer requirement
// appears. See CR-11 in docs/plans/2026-07-16-code-review-fixes.md.
@Component
@Profile("!aws")
@RequiredArgsConstructor
@Slf4j
public class IdempotentEventConsumer {

    static final String CONSUMER_NAME = "hermandad-service";
    static final String GROUP_ID = "hermandad-service-group";

    private final ProcessedEventJpaRepository processedEventRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = {"hermandad-events", "hermandad-member-events"}, groupId = GROUP_ID)
    public void consume(String payload) {
        UUID eventId = extractEventId(payload);

        int claimed = processedEventRepository.tryClaim(eventId, CONSUMER_NAME, Instant.now());
        if (claimed == 0) {
            log.info("Duplicate event skipped: {}", eventId);
            return;
        }

        log.info("Event processed: {} payload={}", eventId, truncate(payload, 200));
    }

    private UUID extractEventId(String payload) {
        try {
            JsonNode root = objectMapper.readTree(payload);
            if (root == null || !root.isObject()) {
                throw new IllegalArgumentException("Payload is not a JSON object");
            }
            JsonNode node = root.get("eventId");
            if (node == null || node.asText() == null || node.asText().isBlank()) {
                throw new IllegalArgumentException("Missing required field: eventId");
            }
            return UUID.fromString(node.asText());
        } catch (JacksonException e) {
            throw new IllegalArgumentException("Malformed event payload", e);
        }
    }

    // ponytail: truncate long payloads in logs
    private static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }
}
