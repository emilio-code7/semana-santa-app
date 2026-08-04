package com.repertorio.procesion.domain.event;

import com.repertorio.procesion.domain.model.ProcesionStatus;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The outbox payload is the full JSON serialization of the event record. The eventType
 * discriminator must be a serialized record component (envelope contract section 4);
 * consumers dispatch on the payload's "eventType" field (ticket 13).
 */
class ProcesionEventSerializationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void createdEventSerializesEventType() throws Exception {
        var event = new ProcesionCreatedEvent(
                UUID.randomUUID(), UUID.randomUUID(), LocalDate.of(2026, 4, 9), LocalTime.of(18, 0));

        var json = objectMapper.writeValueAsString(event);

        assertThat(objectMapper.readTree(json).get("eventType").asText()).isEqualTo("PROCESION_CREATED");
    }

    @Test
    void statusChangedEventSerializesEventType() throws Exception {
        var event = new ProcesionStatusChangedEvent(
                UUID.randomUUID(), UUID.randomUUID(), ProcesionStatus.PLANNED, ProcesionStatus.IN_PROGRESS);

        var json = objectMapper.writeValueAsString(event);

        assertThat(objectMapper.readTree(json).get("eventType").asText()).isEqualTo("PROCESION_STATUS_CHANGED");
    }

    @Test
    void planFinalizedEventSerializesEventType() throws Exception {
        var event = new ProcesionPlanFinalizedEvent(
                UUID.randomUUID(), UUID.randomUUID(), LocalDate.of(2026, 4, 13), LocalTime.of(18, 0),
                ProcesionStatus.PLANNED, Instant.parse("2026-04-10T10:00:00Z"),
                List.<ProcesionPlanFinalizedEvent.PasoSnapshot>of(),
                List.<ProcesionPlanFinalizedEvent.RouteSectionSnapshot>of());

        var json = objectMapper.writeValueAsString(event);

        assertThat(objectMapper.readTree(json).get("eventType").asText()).isEqualTo("PROCESION_PLAN_FINALIZED");
    }

    @Test
    void deletedEventSerializesEventType() throws Exception {
        var event = new ProcesionDeletedEvent(UUID.randomUUID(), UUID.randomUUID());

        var json = objectMapper.writeValueAsString(event);

        assertThat(objectMapper.readTree(json).get("eventType").asText()).isEqualTo("PROCESION_DELETED");
    }
}
