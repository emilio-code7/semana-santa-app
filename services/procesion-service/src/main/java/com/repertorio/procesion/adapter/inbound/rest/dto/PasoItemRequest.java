package com.repertorio.procesion.adapter.inbound.rest.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record PasoItemRequest(
        UUID id,
        @Min(0) int position,
        @NotNull UUID titularId,
        String notes
) {}
