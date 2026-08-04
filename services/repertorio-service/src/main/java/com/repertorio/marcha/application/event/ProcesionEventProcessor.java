package com.repertorio.marcha.application.event;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.repertorio.marcha.application.port.ProcessedEventStore;
import com.repertorio.marcha.domain.model.KnownPaso;
import com.repertorio.marcha.domain.model.KnownProcesion;
import com.repertorio.marcha.domain.model.KnownRouteSection;
import com.repertorio.marcha.domain.port.CrucetaRepository;
import com.repertorio.marcha.domain.port.KnownProcesionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProcesionEventProcessor {

    private final KnownProcesionRepository knownProcesionRepository;
    private final CrucetaRepository crucetaRepository;
    private final ProcessedEventStore processedEventStore;
    private final ObjectMapper objectMapper;

    @Transactional
    public void process(String payload) {
        try {
            JsonNode root = parsePayload(payload);
            UUID eventId = extractUuid(root, "eventId");

            if (!processedEventStore.claim(eventId)) {
                log.debug("Duplicate procesion event skipped: {}", eventId);
                return;
            }

            String eventType = extractEventType(root);
            switch (eventType) {
                case "PROCESION_CREATED" -> handleCreated(root);
                case "PROCESION_STATUS_CHANGED" -> handleStatusChanged(root);
                case "PROCESION_PLAN_FINALIZED" -> handlePlanFinalized(root);
                case "PROCESION_DELETED" -> handleDeleted(root);
                default -> throw new IllegalArgumentException("Unknown procesion event type: " + eventType);
            }
        } catch (IllegalArgumentException e) {
            if (e.getMessage() != null && e.getMessage().startsWith("Malformed event payload")) {
                // malformed JSON is a transport-level failure: escalate so Kafka/SQS retries
                throw new RuntimeException("Failed to process procesion event: " + e.getMessage(), e);
            }
            log.warn("Invalid procesion event: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Failed to process procesion event: {}", e.getMessage());
            throw new RuntimeException("Failed to process procesion event: " + e.getMessage(), e);
        }
    }

    private JsonNode parsePayload(String payload) {
        try {
            return objectMapper.readTree(payload);
        } catch (Exception e) {
            throw new IllegalArgumentException("Malformed event payload", e);
        }
    }

    private static String extractEventType(JsonNode root) {
        JsonNode node = root.get("eventType");
        if (node == null || !node.isTextual() || node.asText().isBlank()) {
            throw new IllegalArgumentException("Missing required field: eventType");
        }
        return node.asText();
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

    private void handlePlanFinalized(JsonNode root) {
        // plan-finalized records serialize the procesion id as "procesionId" (renamed in procesion-service)
        UUID procesionId = extractUuid(root, "procesionId");
        UUID hermandadId = extractUuid(root, "hermandadId");
        LocalDate date = LocalDate.parse(extractText(root, "date"));
        LocalTime time = LocalTime.parse(extractText(root, "time"));
        Instant planFinalizedAt = Instant.parse(extractText(root, "planFinalizedAt"));

        KnownProcesion known = knownProcesionRepository.findByProcesionId(procesionId)
                .orElseGet(() -> new KnownProcesion(procesionId, hermandadId, "PLANNED"));

        known.finalizePlan(date, time, planFinalizedAt);

        knownProcesionRepository.saveFullPlan(known, parsePasos(root, procesionId), parseRouteSections(root, procesionId));
        log.info("Projected finalized plan for procesion {} at {}", procesionId, planFinalizedAt);
    }

    private void handleDeleted(JsonNode root) {
        UUID procesionId = extractUuid(root, "id");
        extractUuid(root, "hermandadId");

        List<KnownPaso> pasos = knownProcesionRepository.findPasosByProcesionId(procesionId);
        for (KnownPaso paso : pasos) {
            crucetaRepository.deleteByPasoId(paso.getId());
        }
        // idempotent: no-op when the procesion projection is already unknown
        knownProcesionRepository.deleteByProcesionId(procesionId);
        log.info("Removed plan projections for deleted procesion {} ({} pasos, crucetas removed)",
                procesionId, pasos.size());
    }

    private static List<KnownPaso> parsePasos(JsonNode root, UUID procesionId) {
        JsonNode pasosNode = root.get("pasos");
        if (pasosNode == null || !pasosNode.isArray()) {
            return Collections.emptyList();
        }
        List<KnownPaso> pasos = new ArrayList<>();
        for (JsonNode pasoNode : pasosNode) {
            pasos.add(new KnownPaso(
                    extractUuid(pasoNode, "id"),
                    procesionId,
                    extractInt(pasoNode, "position"),
                    extractUuid(pasoNode, "titularId")));
        }
        return pasos;
    }

    private static List<KnownRouteSection> parseRouteSections(JsonNode root, UUID procesionId) {
        JsonNode sectionsNode = root.get("routeSections");
        if (sectionsNode == null || !sectionsNode.isArray()) {
            return Collections.emptyList();
        }
        List<KnownRouteSection> sections = new ArrayList<>();
        for (JsonNode sectionNode : sectionsNode) {
            sections.add(new KnownRouteSection(
                    extractUuid(sectionNode, "id"),
                    procesionId,
                    extractText(sectionNode, "name"),
                    extractInt(sectionNode, "position"),
                    sectionNode.has("notes") && sectionNode.get("notes").isTextual() ? sectionNode.get("notes").asText() : null));
        }
        return sections;
    }

    private static int extractInt(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || !value.canConvertToInt()) {
            throw new IllegalArgumentException("Missing or non-numeric field: " + field);
        }
        return value.asInt();
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
        if (node == null || !node.isTextual() || node.asText().isBlank()) {
            throw new IllegalArgumentException("Missing or blank required field: " + field);
        }
        return node.asText();
    }
}
