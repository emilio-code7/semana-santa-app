package com.repertorio.marcha.domain.event;

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
        String youtubeUrl
) implements DomainEvent {
    public MarchaAddedEvent(UUID marchaId, String title, String composer,
                            BandType bandType, Integer compositionYear, String youtubeUrl) {
        this(UUID.randomUUID(), Instant.now(), marchaId, title, composer, bandType, compositionYear, youtubeUrl);
    }

    @Override
    public String aggregateType() { return "marcha"; }

    @Override
    public UUID aggregateId() { return marchaId(); }
}
