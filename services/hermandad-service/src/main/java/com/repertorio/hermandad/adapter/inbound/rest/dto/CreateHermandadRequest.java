package com.repertorio.hermandad.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record CreateHermandadRequest(
        @NotBlank
        String name,

        @NotBlank
        String city,

        @Min(1)
        int foundedYear
) {
}
