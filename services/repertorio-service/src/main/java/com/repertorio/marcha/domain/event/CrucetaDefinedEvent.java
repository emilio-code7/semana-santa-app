package com.repertorio.marcha.domain.event;

import com.repertorio.common.event.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public record CrucetaDefinedEvent(
        UUID eventId,
        Instant occurredAt,
        UUID crucetaId,
        UUID procesionId,
        UUID pasoId,
        int itemCount,
        String eventType
) implements DomainEvent {
    public CrucetaDefinedEvent(UUID crucetaId, UUID procesionId, UUID pasoId, int itemCount) {
        this(UUID.randomUUID(), Instant.now(), crucetaId, procesionId, pasoId, itemCount, "CRUCETA_DEFINED");
    }

    @Override
    public String aggregateType() { return "cruceta"; }

    @Override
    public UUID aggregateId() { return crucetaId(); }

    @Override
    public int schemaVersion() { return 1; }
}
