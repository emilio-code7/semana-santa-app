package com.repertorio.procesion.adapter.inbound.rest.dto;

import com.repertorio.procesion.domain.model.ProcesionEstado;
import jakarta.validation.constraints.NotNull;

public record EstadoChangeRequest(
        @NotNull ProcesionEstado nuevoEstado
) {}
