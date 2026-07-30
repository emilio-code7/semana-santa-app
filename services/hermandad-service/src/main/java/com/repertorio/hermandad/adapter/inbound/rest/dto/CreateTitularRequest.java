package com.repertorio.hermandad.adapter.inbound.rest.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateTitularRequest(
        @NotBlank
        String name,

        String description
) {}
