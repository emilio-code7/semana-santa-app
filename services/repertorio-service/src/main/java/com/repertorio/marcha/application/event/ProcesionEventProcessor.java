package com.repertorio.marcha.application.event;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.repertorio.marcha.application.port.ProcessedEventStore;
import com.repertorio.marcha.domain.model.KnownProcesion;
import com.repertorio.marcha.domain.port.KnownProcesionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProcesionEventProcessor {

    private final KnownProcesionRepository knownProcesionRepository;
    private final ProcessedEventStore processedEventStore;
    private final ObjectMapper objectMapper;

    @Transactional
    public void process(String payload) {
        UUID eventId = UUID.nameUUIDFromBytes(payload.getBytes(StandardCharsets.UTF_8));

        if (processedEventStore.exists(eventId)) {
            log.debug("Duplicate procesion event skipped: {}", eventId);
            return;
        }

        try {
            JsonNode root = objectMapper.readTree(payload);

            if (root.has("previousStatus") || root.has("newStatus")) {
                handleStatusChanged(root);
            } else {
                handleCreated(root);
            }

            processedEventStore.record(eventId);
        } catch (IllegalArgumentException e) {
            // domain validation failure — throw so transport can retry/error
            log.warn("Invalid procesion event {}: {}", eventId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Failed to process procesion event {}: {}", eventId, e.getMessage());
            throw new RuntimeException("Failed to process procesion event: " + eventId, e);
        }
    }

    private void handleCreated(JsonNode root) {
        UUID procesionId = extractUuid(root, "id");
        UUID hermandadId = extractUuid(root, "hermandadId");
        knownProcesionRepository.save(new KnownProcesion(procesionId, hermandadId, "PLANNED"));
        log.info("Registered known procesion {} for hermandad {}", procesionId, hermandadId);
    }

    private void handleStatusChanged(JsonNode root) {
        UUID procesionId = extractUuid(root, "id");
        extractUuid(root, "hermandadId");
        JsonNode statusNode = root.get("newStatus");
        if (statusNode == null || !statusNode.isTextual() || statusNode.asText().isBlank()) {
            throw new IllegalArgumentException("newStatus must not be blank");
        }
        String newStatus = statusNode.asText();

        KnownProcesion known = knownProcesionRepository.findByProcesionId(procesionId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Status change for unknown procesion: " + procesionId));

        known.updateStatus(newStatus);
        knownProcesionRepository.save(known);
        log.info("Updated procesion {} status to {}", procesionId, newStatus);
    }

    private static UUID extractUuid(JsonNode root, String field) {
        JsonNode node = root.get(field);
        if (node == null || node.asText() == null || node.asText().isBlank()) {
            throw new IllegalArgumentException("Missing required field: " + field);
        }
        return UUID.fromString(node.asText());
    }
}
