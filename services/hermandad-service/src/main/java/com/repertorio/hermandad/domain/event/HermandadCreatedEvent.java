package com.repertorio.hermandad.domain.event;

import com.repertorio.hermandad.application.port.DomainEvent;

import java.util.UUID;

public record HermandadCreatedEvent(
        UUID id,
        String name,
        String city,
        Integer foundedYear
) implements DomainEvent {
    @Override
    public String aggregateType() { return "hermandad"; }
    @Override
    public UUID aggregateId() { return id; }
    @Override
    public String eventType() { return "HERMANDAD_CREATED"; }
}