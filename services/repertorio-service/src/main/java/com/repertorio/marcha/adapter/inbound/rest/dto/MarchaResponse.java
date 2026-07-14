package com.repertorio.marcha.adapter.inbound.rest.dto;

import com.repertorio.marcha.domain.model.BandType;
import com.repertorio.marcha.domain.model.Marcha;

import java.time.Instant;
import java.util.UUID;

public record MarchaResponse(
        UUID id,
        String title,
        String composer,
        BandType bandType,
        int durationSeconds,
        Integer compositionYear,
        String youtubeUrl,
        Instant createdAt,
        Instant updatedAt
) {
    public static MarchaResponse from(Marcha marcha) {
        return new MarchaResponse(
                marcha.getId(),
                marcha.getTitle(),
                marcha.getComposer(),
                marcha.getBandType(),
                marcha.getDurationSeconds(),
                marcha.getCompositionYear(),
                marcha.getYoutubeUrl(),
                marcha.getCreatedAt(),
                marcha.getUpdatedAt()
        );
    }
}
