package com.repertorio.procesion.domain.event;

import com.repertorio.procesion.application.port.DomainEvent;
import com.repertorio.procesion.domain.model.ProcesionStatus;

import java.util.UUID;

public record ProcesionStatusChangedEvent(
        UUID id,
        UUID hermandadId,
        ProcesionStatus previousStatus,
        ProcesionStatus newStatus
) implements DomainEvent {
    @Override
    public String aggregateType() { return "procesion"; }
    @Override
    public UUID aggregateId() { return id; }
    @Override
    public String eventType() { return "PROCESION_STATUS_CHANGED"; }
}
