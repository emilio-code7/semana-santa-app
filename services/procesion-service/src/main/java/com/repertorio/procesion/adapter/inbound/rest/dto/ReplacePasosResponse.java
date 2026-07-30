package com.repertorio.procesion.adapter.inbound.rest.dto;

import com.repertorio.procesion.domain.model.Paso;

import java.util.List;

public record ReplacePasosResponse(
        List<PasoItemResponse> pasos
) {
    public static ReplacePasosResponse from(List<Paso> domain) {
        return new ReplacePasosResponse(
                domain.stream().map(PasoItemResponse::from).toList()
        );
    }
}
