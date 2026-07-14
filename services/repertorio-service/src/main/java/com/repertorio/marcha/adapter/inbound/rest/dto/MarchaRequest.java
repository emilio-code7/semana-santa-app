package com.repertorio.marcha.adapter.inbound.rest.dto;

import com.repertorio.marcha.domain.model.BandType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record MarchaRequest(
        @NotBlank String title,
        @NotBlank String composer,
        @NotNull BandType bandType,
        @Positive int durationSeconds,
        Integer compositionYear,
        String youtubeUrl
) {}
