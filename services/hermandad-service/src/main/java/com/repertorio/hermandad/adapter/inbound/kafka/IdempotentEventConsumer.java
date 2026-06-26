package com.repertorio.hermandad.adapter.inbound.kafka;

import com.repertorio.hermandad.adapter.outbound.events.ProcessedEventEntity;
import com.repertorio.hermandad.adapter.outbound.events.ProcessedEventJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "kafka.consumer.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class IdempotentEventConsumer {

    static final String CONSUMER_NAME = "hermandad-service";
    static final String GROUP_ID = "hermandad-service-group";

    private final ProcessedEventJpaRepository processedEventRepository;

    @KafkaListener(topics = {"hermandad-events", "hermandad-member-events"}, groupId = GROUP_ID)
    public void consume(String payload) {
        UUID eventId = UUID.nameUUIDFromBytes(payload.getBytes(StandardCharsets.UTF_8));

        if (processedEventRepository.existsById(eventId)) {
            log.info("Duplicate event skipped: {}", eventId);
            return;
        }

        processedEventRepository.save(new ProcessedEventEntity(eventId, CONSUMER_NAME));
        log.info("Event processed: {} payload={}", eventId, truncate(payload, 200));
    }

    // ponytail: truncate long payloads in logs
    private static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }
}
