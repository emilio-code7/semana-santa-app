package com.repertorio.marcha.adapter.inbound.rest.dto;

import java.util.List;
import java.util.UUID;

public record RunSheetResponse(
        UUID pasoId,
        List<RunSheetSection> sections
) {
    public record RunSheetSection(
            UUID sectionId,
            String sectionName,
            int position,
            boolean isCurrent,
            boolean isNext,
            List<RunSheetItem> items
    ) {}

    public record RunSheetItem(
            UUID itemId,
            UUID marchaId,
            int orderIndex,
            boolean isCurrent,
            boolean isNext,
            String notes
    ) {}
}
