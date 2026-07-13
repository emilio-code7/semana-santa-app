package com.repertorio.procesion.adapter.inbound.rest.dto;

import com.repertorio.procesion.domain.model.Procesion;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record ProcesionResponse(
        UUID id,
        UUID hermandadId,
        LocalDate fecha,
        LocalTime hora,
        String estado,
        Instant createdAt,
        Instant updatedAt
) {
    public static ProcesionResponse from(Procesion procesion) {
        return new ProcesionResponse(
                procesion.getId(),
                procesion.getHermandadId(),
                procesion.getFecha(),
                procesion.getHora(),
                procesion.getEstado().name(),
                procesion.getCreatedAt(),
                procesion.getUpdatedAt()
        );
    }
}
