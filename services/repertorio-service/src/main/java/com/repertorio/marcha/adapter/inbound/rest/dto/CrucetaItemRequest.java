package com.repertorio.marcha.adapter.inbound.rest.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.UUID;

public record CrucetaItemRequest(
        @NotNull UUID marchaId,
        @PositiveOrZero int orderIndex,
        String notes
) {}
