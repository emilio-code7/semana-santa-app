package com.repertorio.procesion.domain.event;

import com.repertorio.common.event.DomainEvent;
import com.repertorio.procesion.domain.model.ProcesionStatus;

import java.time.Instant;
import java.util.UUID;

public record ProcesionStatusChangedEvent(
        UUID id,
        UUID hermandadId,
        ProcesionStatus previousStatus,
        ProcesionStatus newStatus,
        UUID eventId,
        Instant occurredAt,
        String eventType
) implements DomainEvent {
    public ProcesionStatusChangedEvent(UUID id, UUID hermandadId, ProcesionStatus previousStatus, ProcesionStatus newStatus) {
        this(id, hermandadId, previousStatus, newStatus, UUID.randomUUID(), Instant.now(), "PROCESION_STATUS_CHANGED");
    }

    @Override
    public String aggregateType() { return "procesion"; }
    @Override
    public UUID aggregateId() { return id; }
    @Override
    public int schemaVersion() { return 1; }
}
