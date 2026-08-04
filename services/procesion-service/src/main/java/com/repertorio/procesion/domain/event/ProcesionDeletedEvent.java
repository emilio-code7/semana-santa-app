package com.repertorio.procesion.domain.event;

import com.repertorio.common.event.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record ProcesionDeletedEvent(
        UUID id,
        UUID hermandadId,
        UUID eventId,
        Instant occurredAt,
        String eventType
) implements DomainEvent {
    public ProcesionDeletedEvent(UUID id, UUID hermandadId) {
        this(id, hermandadId, UUID.randomUUID(), Instant.now(), "PROCESION_DELETED");
    }

    @Override
    public String aggregateType() { return "procesion"; }
    @Override
    public UUID aggregateId() { return id; }
    @Override
    public int schemaVersion() { return 1; }
}
