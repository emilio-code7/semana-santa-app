package com.repertorio.hermandad.domain.event;

import com.repertorio.common.event.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record HermandadCreatedEvent(
        UUID id,
        String name,
        String city,
        Integer foundedYear,
        UUID eventId,
        Instant occurredAt
) implements DomainEvent {
    public HermandadCreatedEvent(UUID id, String name, String city, Integer foundedYear) {
        this(id, name, city, foundedYear, UUID.randomUUID(), Instant.now());
    }

    @Override
    public String aggregateType() { return "hermandad"; }
    @Override
    public UUID aggregateId() { return id; }
    @Override
    public String eventType() { return "HERMANDAD_CREATED"; }
}
