package com.repertorio.hermandad.domain.event;

import com.repertorio.common.event.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record TitularUpdatedEvent(
        UUID id,
        UUID hermandadId,
        String name,
        String description,
        UUID eventId,
        Instant occurredAt,
        String eventType
) implements DomainEvent {
    public TitularUpdatedEvent(UUID id, UUID hermandadId, String name, String description) {
        this(id, hermandadId, name, description, UUID.randomUUID(), Instant.now(), "TITULAR_UPDATED");
    }

    @Override
    public String aggregateType() { return "titular"; }
    @Override
    public UUID aggregateId() { return id; }

    @Override
    public int schemaVersion() { return 1; }
}
