package com.repertorio.hermandad.adapter.inbound.rest.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateTitularRequest(
        @NotBlank
        String name,

        String description
) {}
