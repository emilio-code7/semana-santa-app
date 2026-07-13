package com.repertorio.procesion.adapter.inbound.rest.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record CreateProcesionRequest(
        @NotNull UUID hermandadId,
        @NotNull LocalDate fecha,
        @NotNull LocalTime hora
) {}
