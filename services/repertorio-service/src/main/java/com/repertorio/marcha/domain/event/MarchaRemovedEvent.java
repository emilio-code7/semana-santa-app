package com.repertorio.marcha.domain.event;

import com.repertorio.common.event.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public record MarchaRemovedEvent(
        UUID eventId,
        Instant occurredAt,
        UUID marchaId,
        String title,
        String eventType
) implements DomainEvent {
    public MarchaRemovedEvent(UUID marchaId, String title) {
        this(UUID.randomUUID(), Instant.now(), marchaId, title, "MARCHA_REMOVED");
    }

    @Override
    public String aggregateType() { return "marcha"; }

    @Override
    public UUID aggregateId() { return marchaId(); }

    @Override
    public int schemaVersion() { return 1; }
}
