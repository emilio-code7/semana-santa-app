package com.repertorio.procesion.adapter.inbound.rest.dto;

import com.repertorio.procesion.domain.model.Procesion;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record FinalizePlanResponse(
        UUID id,
        UUID hermandadId,
        LocalDate date,
        LocalTime time,
        String status,
        Instant planFinalizedAt,
        int pasoCount,
        int routeSectionCount,
        Instant createdAt,
        Instant updatedAt
) {
    public static FinalizePlanResponse from(Procesion procesion, int pasoCount, int routeSectionCount) {
        return new FinalizePlanResponse(
                procesion.getId(),
                procesion.getHermandadId(),
                procesion.getDate(),
                procesion.getTime(),
                procesion.getStatus().name(),
                procesion.getPlanFinalizedAt(),
                pasoCount,
                routeSectionCount,
                procesion.getCreatedAt(),
                procesion.getUpdatedAt()
        );
    }
}
