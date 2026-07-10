package com.repertorio.procesion.domain.event;

import com.repertorio.procesion.application.port.DomainEvent;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record ProcesionCreatedEvent(
        UUID id,
        UUID hermandadId,
        LocalDate fecha,
        LocalTime hora
) implements DomainEvent {
    @Override
    public String aggregateType() { return "procesion"; }
    @Override
    public UUID aggregateId() { return id; }
    @Override
    public String eventType() { return "PROCESION_CREATED"; }
}
