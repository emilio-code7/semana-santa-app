package com.repertorio.marcha.domain.event;

import com.repertorio.common.event.DomainEvent;
import com.repertorio.marcha.domain.model.BandType;
import java.time.Instant;
import java.util.UUID;

public record MarchaAddedEvent(
        UUID eventId,
        Instant occurredAt,
        UUID marchaId,
        String title,
        String composer,
        BandType bandType,
        Integer compositionYear,
        String youtubeUrl,
        String eventType
) implements DomainEvent {
    public MarchaAddedEvent(UUID marchaId, String title, String composer,
                            BandType bandType, Integer compositionYear, String youtubeUrl) {
        this(UUID.randomUUID(), Instant.now(), marchaId, title, composer, bandType, compositionYear, youtubeUrl,
                "MARCHA_ADDED");
    }

    @Override
    public String aggregateType() { return "marcha"; }

    @Override
    public UUID aggregateId() { return marchaId(); }

    @Override
    public int schemaVersion() { return 1; }
}
