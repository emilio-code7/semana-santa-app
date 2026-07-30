package com.repertorio.procesion.adapter.inbound.rest.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record RouteSectionRequest(
        UUID id,
        @NotBlank String name,
        @Min(0) int position,
        String notes
) {}
