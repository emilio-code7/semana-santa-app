package com.repertorio.marcha.adapter.inbound.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.repertorio.marcha.adapter.outbound.events.ProcessedEventEntity;
import com.repertorio.marcha.adapter.outbound.events.ProcessedEventJpaRepository;
import com.repertorio.marcha.domain.model.KnownProcesion;
import com.repertorio.marcha.domain.port.KnownProcesionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProcesionEventConsumer {

    static final String CONSUMER_NAME = "repertorio-service";
    static final String GROUP_ID = "repertorio-service-group";

    private final KnownProcesionRepository knownProcesionRepository;
    private final ProcessedEventJpaRepository processedEventRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "procesion-events", groupId = GROUP_ID)
    public void consume(String payload) {
        UUID eventId = UUID.nameUUIDFromBytes(payload.getBytes(StandardCharsets.UTF_8));

        if (processedEventRepository.existsById(eventId)) {
            log.debug("Duplicate procesion event skipped: {}", eventId);
            return;
        }

        try {
            JsonNode root = objectMapper.readTree(payload);

            // Event type detection by field presence (eventType not serialized in JSON yet)
            if (root.has("newStatus")) {
                handleStatusChanged(root);
            } else {
                handleCreated(root);
            }

            processedEventRepository.save(new ProcessedEventEntity(eventId, CONSUMER_NAME));
        } catch (Exception e) {
            // ponytail: single retry on next poll — no dead-letter yet (Phase 2)
            log.error("Failed to process procesion event {}: {}", eventId, e.getMessage());
        }
    }

    private void handleCreated(JsonNode root) {
        UUID procesionId = UUID.fromString(root.get("id").asText());
        UUID hermandadId = UUID.fromString(root.get("hermandadId").asText());
        knownProcesionRepository.save(new KnownProcesion(procesionId, hermandadId, "PLANNED"));
        log.info("Registered known procesion {} for hermandad {}", procesionId, hermandadId);
    }

    private void handleStatusChanged(JsonNode root) {
        UUID procesionId = UUID.fromString(root.get("id").asText());
        String newStatus = root.get("newStatus").asText();
        knownProcesionRepository.findByProcesionId(procesionId).ifPresentOrElse(
            kp -> {
                kp.updateStatus(newStatus);
                knownProcesionRepository.save(kp);
                log.info("Updated procesion {} status to {}", procesionId, newStatus);
            },
            () -> log.warn("Received status change for unknown procesion {}", procesionId)
        );
    }
}
