package com.repertorio.hermandad.domain.event;

import com.repertorio.common.event.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record TitularCreatedEvent(
        UUID id,
        UUID hermandadId,
        String name,
        String description,
        UUID eventId,
        Instant occurredAt,
        String eventType
) implements DomainEvent {
    public TitularCreatedEvent(UUID id, UUID hermandadId, String name, String description) {
        this(id, hermandadId, name, description, UUID.randomUUID(), Instant.now(), "TITULAR_CREATED");
    }

    @Override
    public String aggregateType() { return "titular"; }
    @Override
    public UUID aggregateId() { return id; }
}
