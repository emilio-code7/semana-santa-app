package com.repertorio.procesion.domain.event;

import com.repertorio.common.event.DomainEvent;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record ProcesionPlanFinalizedEvent(
        UUID id,
        UUID hermandadId,
        LocalDate date,
        LocalTime time,
        Instant planFinalizedAt,
        UUID eventId,
        Instant occurredAt
) implements DomainEvent {
    public ProcesionPlanFinalizedEvent(UUID id, UUID hermandadId, LocalDate date, LocalTime time, Instant planFinalizedAt) {
        this(id, hermandadId, date, time, planFinalizedAt, UUID.randomUUID(), Instant.now());
    }

    @Override
    public String aggregateType() { return "procesion"; }
    @Override
    public UUID aggregateId() { return id; }
    @Override
    public String eventType() { return "PROCESION_PLAN_FINALIZED"; }
}
