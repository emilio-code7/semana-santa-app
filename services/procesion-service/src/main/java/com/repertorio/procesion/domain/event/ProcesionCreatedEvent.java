package com.repertorio.procesion.domain.event;

import com.repertorio.common.event.DomainEvent;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record ProcesionCreatedEvent(
        UUID id,
        UUID hermandadId,
        LocalDate date,
        LocalTime time,
        UUID eventId,
        Instant occurredAt
) implements DomainEvent {
    public ProcesionCreatedEvent(UUID id, UUID hermandadId, LocalDate date, LocalTime time) {
        this(id, hermandadId, date, time, UUID.randomUUID(), Instant.now());
    }

    @Override
    public String aggregateType() { return "procesion"; }
    @Override
    public UUID aggregateId() { return id; }
    @Override
    public String eventType() { return "PROCESION_CREATED"; }
}
