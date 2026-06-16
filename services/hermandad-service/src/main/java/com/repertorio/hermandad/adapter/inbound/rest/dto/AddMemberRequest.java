package com.repertorio.hermandad.adapter.inbound.rest.dto;

import com.repertorio.hermandad.domain.model.HermandadRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AddMemberRequest(
        @NotBlank
        String userId,
        @NotNull
        HermandadRole role
) {
}
