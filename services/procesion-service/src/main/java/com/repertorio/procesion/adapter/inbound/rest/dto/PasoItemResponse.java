package com.repertorio.procesion.adapter.inbound.rest.dto;

import com.repertorio.procesion.domain.model.Paso;

import java.time.Instant;
import java.util.UUID;

public record PasoItemResponse(
        UUID id,
        UUID procesionId,
        int position,
        UUID titularId,
        String notes,
        Instant createdAt,
        Instant updatedAt
) {
    public static PasoItemResponse from(Paso paso) {
        return new PasoItemResponse(
                paso.getId(),
                paso.getProcesionId(),
                paso.getPosition(),
                paso.getTitularId(),
                paso.getNotes(),
                paso.getCreatedAt(),
                paso.getUpdatedAt()
        );
    }
}
