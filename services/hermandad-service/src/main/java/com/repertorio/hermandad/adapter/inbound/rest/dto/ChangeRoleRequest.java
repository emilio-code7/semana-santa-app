package com.repertorio.hermandad.adapter.inbound.rest.dto;

import com.repertorio.hermandad.domain.model.HermandadRole;
import jakarta.validation.constraints.NotNull;

public record ChangeRoleRequest(
        @NotNull
        HermandadRole role
) {
}
