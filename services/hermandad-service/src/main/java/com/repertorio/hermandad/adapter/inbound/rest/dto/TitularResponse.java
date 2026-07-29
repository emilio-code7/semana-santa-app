package com.repertorio.hermandad.adapter.inbound.rest.dto;

import com.repertorio.hermandad.domain.model.Titular;

import java.time.Instant;
import java.util.UUID;

public record TitularResponse(
        UUID id,
        UUID hermandadId,
        String name,
        String description,
        Instant createdAt,
        Instant updatedAt
) {
    public static TitularResponse from(Titular titular) {
        return new TitularResponse(
                titular.getId(),
                titular.getHermandadId(),
                titular.getName(),
                titular.getDescription(),
                titular.getCreatedAt(),
                titular.getUpdatedAt()
        );
    }
}
