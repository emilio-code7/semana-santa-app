package com.repertorio.procesion.domain.event;

import com.repertorio.procesion.application.port.DomainEvent;
import com.repertorio.procesion.domain.model.ProcesionEstado;

import java.util.UUID;

public record ProcesionEstadoChangedEvent(
        UUID id,
        UUID hermandadId,
        ProcesionEstado estadoAnterior,
        ProcesionEstado nuevoEstado
) implements DomainEvent {
    @Override
    public String aggregateType() { return "procesion"; }
    @Override
    public UUID aggregateId() { return id; }
    @Override
    public String eventType() { return "PROCESION_ESTADO_CHANGED"; }
}
