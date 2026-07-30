package com.repertorio.procesion.adapter.inbound.rest.dto;

import com.repertorio.procesion.domain.model.RouteSection;

import java.time.Instant;
import java.util.UUID;

public record RouteSectionResponse(
        UUID id,
        UUID procesionId,
        String name,
        int position,
        String notes,
        Instant createdAt,
        Instant updatedAt
) {
    public static RouteSectionResponse from(RouteSection section) {
        return new RouteSectionResponse(
                section.getId(),
                section.getProcesionId(),
                section.getName(),
                section.getPosition(),
                section.getNotes(),
                section.getCreatedAt(),
                section.getUpdatedAt()
        );
    }
}
