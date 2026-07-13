package com.repertorio.procesion.adapter.inbound.rest.dto;

import com.repertorio.procesion.domain.model.ProcesionStatus;
import jakarta.validation.constraints.NotNull;

public record StatusChangeRequest(
        @NotNull ProcesionStatus newStatus
) {}
