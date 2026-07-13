package com.repertorio.procesion.adapter.inbound.rest.dto;

import com.repertorio.procesion.domain.model.Procesion;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record ProcesionResponse(
        UUID id,
        UUID hermandadId,
        LocalDate date,
        LocalTime time,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
    public static ProcesionResponse from(Procesion procesion) {
        return new ProcesionResponse(
                procesion.getId(),
                procesion.getHermandadId(),
                procesion.getDate(),
                procesion.getTime(),
                procesion.getStatus().name(),
                procesion.getCreatedAt(),
                procesion.getUpdatedAt()
        );
    }
}
