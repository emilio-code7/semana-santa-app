package com.repertorio.procesion.adapter.inbound.rest.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record ReplacePasosRequest(
        @NotEmpty @Valid List<PasoItemRequest> pasos
) {}
