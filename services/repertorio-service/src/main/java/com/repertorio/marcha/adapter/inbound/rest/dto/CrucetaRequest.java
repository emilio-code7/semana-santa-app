package com.repertorio.marcha.adapter.inbound.rest.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record CrucetaRequest(
        @NotEmpty @Valid List<CrucetaItemRequest> items
) {}
