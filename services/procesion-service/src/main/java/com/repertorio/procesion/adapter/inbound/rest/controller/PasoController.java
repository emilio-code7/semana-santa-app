package com.repertorio.procesion.adapter.inbound.rest.controller;

import com.repertorio.procesion.adapter.inbound.rest.dto.PasoItemRequest;
import com.repertorio.procesion.adapter.inbound.rest.dto.ReplacePasosRequest;
import com.repertorio.procesion.adapter.inbound.rest.dto.ReplacePasosResponse;
import com.repertorio.procesion.application.service.PasoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/hermandades/{hermandadId}/procesiones/{procesionId}/pasos")
@RequiredArgsConstructor
@Slf4j
public class PasoController {

    private final PasoService pasoService;

    @GetMapping
    @Operation(summary = "Get ordered list of pasos for a procesion")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ordered list of pasos"),
            @ApiResponse(responseCode = "403", description = "Forbidden — cross-tenant access"),
            @ApiResponse(responseCode = "404", description = "Procesion not found")
    })
    public ResponseEntity<ReplacePasosResponse> getPasos(
            @PathVariable UUID hermandadId,
            @PathVariable UUID procesionId
    ) {
        log.info("Getting pasos for procesion {} in hermandad {}", procesionId, hermandadId);
        var pasos = pasoService.getPasos(hermandadId, procesionId);
        return ResponseEntity.ok(ReplacePasosResponse.from(pasos));
    }

    @PutMapping
    @Operation(summary = "Atomically replace the ordered list of pasos")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pasos replaced"),
            @ApiResponse(responseCode = "400", description = "Invalid input (validation error or duplicate position)"),
            @ApiResponse(responseCode = "403", description = "Forbidden — cross-tenant or titular from another hermandad"),
            @ApiResponse(responseCode = "404", description = "Procesion not found")
    })
    public ResponseEntity<ReplacePasosResponse> replacePasos(
            @PathVariable UUID hermandadId,
            @PathVariable UUID procesionId,
            @Valid @RequestBody ReplacePasosRequest request
    ) {
        log.info("Replacing pasos for procesion {} in hermandad {} ({} items)",
                procesionId, hermandadId, request.pasos().size());
        var items = request.pasos().stream()
                .map(this::toServiceItem)
                .toList();
        var pasos = pasoService.replacePasos(hermandadId, procesionId, items);
        return ResponseEntity.ok(ReplacePasosResponse.from(pasos));
    }

    private PasoService.PasoItem toServiceItem(PasoItemRequest req) {
        return new PasoService.PasoItem(req.id(), req.position(), req.titularId(), req.notes());
    }
}
