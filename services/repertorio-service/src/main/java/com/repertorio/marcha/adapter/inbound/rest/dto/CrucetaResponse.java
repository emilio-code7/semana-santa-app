package com.repertorio.marcha.adapter.inbound.rest.dto;

import com.repertorio.marcha.domain.model.Cruceta;
import com.repertorio.marcha.domain.model.CrucetaItem;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CrucetaResponse(
        UUID id,
        UUID pasoId,
        List<CrucetaItemResponse> items,
        Instant createdAt,
        Instant updatedAt
) {
    public static CrucetaResponse from(Cruceta cruceta) {
        return new CrucetaResponse(
                cruceta.getId(),
                cruceta.getPasoId(),
                cruceta.getItems().stream().map(CrucetaItemResponse::from).toList(),
                cruceta.getCreatedAt(),
                cruceta.getUpdatedAt()
        );
    }

    public record CrucetaItemResponse(UUID id, UUID marchaId, UUID routeSectionId,
                                      int sequenceWithinSection, String notes) {
        static CrucetaItemResponse from(CrucetaItem item) {
            return new CrucetaItemResponse(item.getId(), item.getMarchaId(), item.getRouteSectionId(),
                    item.getSequenceWithinSection(), item.getNotes());
        }
    }
}
