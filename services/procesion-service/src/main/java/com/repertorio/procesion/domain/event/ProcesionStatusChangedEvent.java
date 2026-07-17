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
        Instant occurredAt
) implements DomainEvent {
    public ProcesionStatusChangedEvent(UUID id, UUID hermandadId, ProcesionStatus previousStatus, ProcesionStatus newStatus) {
        this(id, hermandadId, previousStatus, newStatus, UUID.randomUUID(), Instant.now());
    }

    @Override
    public String aggregateType() { return "procesion"; }
    @Override
    public UUID aggregateId() { return id; }
    @Override
    public String eventType() { return "PROCESION_STATUS_CHANGED"; }
}
