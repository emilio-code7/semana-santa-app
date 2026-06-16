package com.repertorio.hermandad.adapter.inbound.rest.dto;

import com.repertorio.hermandad.domain.model.Hermandad;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

public record HermandadResponse (
        UUID id,
        String name,
        String city,
        int foundedYear,
        Instant createdAt
) implements Serializable {
    public static HermandadResponse from(Hermandad hermandad) {
        return new HermandadResponse(
                hermandad.getId(),
                hermandad.getName(),
                hermandad.getCity(),
                hermandad.getFoundedYear(),
                hermandad.getCreatedAt()
        );
    }
}
