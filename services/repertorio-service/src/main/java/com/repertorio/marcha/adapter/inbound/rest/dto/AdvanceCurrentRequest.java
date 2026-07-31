package com.repertorio.marcha.adapter.inbound.rest.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AdvanceCurrentRequest(
        @NotNull UUID routeSectionId,
        UUID crucetaItemId
) {}
