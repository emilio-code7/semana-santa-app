package com.repertorio.procesion.application.event;

import com.repertorio.procesion.application.port.ProcessedEventStore;
import com.repertorio.procesion.domain.model.KnownTitular;
import com.repertorio.procesion.domain.port.KnownTitularRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class TitularEventProcessor {

    private final KnownTitularRepository knownTitularRepository;
    private final ProcessedEventStore processedEventStore;
    private final ObjectMapper objectMapper;

    @Transactional
    public void process(String payload) {
        try {
            JsonNode root = objectMapper.readTree(payload);
            UUID eventId = extractUuid(root, "eventId");

            if (!processedEventStore.claim(eventId)) {
                log.debug("Duplicate titular event skipped: {}", eventId);
                return;
            }

            String eventType = root.has("eventType") ? root.get("eventType").asText() : "";

            if ("TITULAR_CREATED".equals(eventType)) {
                handleCreated(root);
            } else if ("TITULAR_UPDATED".equals(eventType)) {
                handleUpdated(root);
            } else {
                throw new IllegalArgumentException("Unknown titular event type: " + eventType);
            }
        } catch (IllegalArgumentException e) {
            log.warn("Invalid titular event: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Failed to process titular event: {}", e.getMessage());
            throw new RuntimeException("Failed to process titular event", e);
        }
    }

    private void handleCreated(JsonNode root) {
        UUID id = extractUuid(root, "id");
        UUID hermandadId = extractUuid(root, "hermandadId");
        String name = extractText(root, "name");

        // tenant integrity: if a projection exists with a different hermandadId, reject
        knownTitularRepository.findById(id).ifPresent(existing -> {
            if (!existing.getHermandadId().equals(hermandadId)) {
                throw new IllegalArgumentException(
                        "HermandadId mismatch for titular " + id + ": event says " + hermandadId
                                + " but existing projection has " + existing.getHermandadId());
            }
        });

        knownTitularRepository.save(new KnownTitular(id, hermandadId, name));
        log.info("Registered known titular {} for hermandad {}", id, hermandadId);
    }

    private void handleUpdated(JsonNode root) {
        UUID id = extractUuid(root, "id");
        UUID hermandadId = extractUuid(root, "hermandadId");
        String name = extractText(root, "name");

        KnownTitular known = knownTitularRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Update for unknown titular: " + id));

        // tenant integrity: validate hermandadId matches existing projection
        if (!known.getHermandadId().equals(hermandadId)) {
            throw new IllegalArgumentException(
                    "HermandadId mismatch for titular " + id + ": event says " + hermandadId
                            + " but existing projection has " + known.getHermandadId());
        }

        known.updateName(name);
        knownTitularRepository.save(known);
        log.info("Updated known titular {} name to {}", id, name);
    }

    private static UUID extractUuid(JsonNode root, String field) {
        JsonNode node = root.get(field);
        if (node == null || node.asText() == null || node.asText().isBlank()) {
            throw new IllegalArgumentException("Missing required field: " + field);
        }
        return UUID.fromString(node.asText());
    }

    private static String extractText(JsonNode root, String field) {
        JsonNode node = root.get(field);
        if (node == null || node.asText() == null || node.asText().isBlank()) {
            throw new IllegalArgumentException("Missing required field: " + field);
        }
        return node.asText();
    }
}
