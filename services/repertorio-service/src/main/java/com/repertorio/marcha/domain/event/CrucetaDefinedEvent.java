package com.repertorio.marcha.domain.event;

import java.time.Instant;
import java.util.UUID;

public record CrucetaDefinedEvent(
        UUID eventId,
        Instant occurredAt,
        UUID crucetaId,
        UUID procesionId,
        int itemCount
) implements DomainEvent {
    public CrucetaDefinedEvent(UUID crucetaId, UUID procesionId, int itemCount) {
        this(UUID.randomUUID(), Instant.now(), crucetaId, procesionId, itemCount);
    }

    @Override
    public String aggregateType() { return "cruceta"; }

    @Override
    public UUID aggregateId() { return crucetaId(); }
}
