package com.repertorio.procesion.adapter.inbound.rest.controller;

import com.repertorio.procesion.adapter.inbound.rest.dto.FinalizePlanResponse;
import com.repertorio.procesion.adapter.inbound.rest.dto.ReplaceRouteSectionsRequest;
import com.repertorio.procesion.adapter.inbound.rest.dto.RouteSectionRequest;
import com.repertorio.procesion.adapter.inbound.rest.dto.RouteSectionsResponse;
import com.repertorio.procesion.application.service.PasoService;
import com.repertorio.procesion.application.service.ProcesionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/hermandades/{hermandadId}/procesiones/{procesionId}")
@RequiredArgsConstructor
@Slf4j
public class ProcesionPlanController {

    private final ProcesionService procesionService;
    private final PasoService pasoService;

    @GetMapping("/route")
    @Operation(summary = "Get ordered route sections for a procesion")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ordered list of route sections"),
            @ApiResponse(responseCode = "403", description = "Forbidden — cross-tenant access"),
            @ApiResponse(responseCode = "404", description = "Procesion not found")
    })
    public ResponseEntity<RouteSectionsResponse> getRouteSections(
            @PathVariable UUID hermandadId,
            @PathVariable UUID procesionId
    ) {
        log.info("Getting route sections for procesion {} in hermandad {}", procesionId, hermandadId);
        var sections = procesionService.getRouteSections(hermandadId, procesionId);
        return ResponseEntity.ok(RouteSectionsResponse.from(sections));
    }

    @PutMapping("/route")
    @Operation(summary = "Atomically replace draft route sections for a procesion")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Route sections replaced"),
            @ApiResponse(responseCode = "400", description = "Invalid input (validation error)"),
            @ApiResponse(responseCode = "403", description = "Forbidden — cross-tenant access"),
            @ApiResponse(responseCode = "404", description = "Procesion not found"),
            @ApiResponse(responseCode = "409", description = "Conflict — plan already finalized, route is immutable")
    })
    public ResponseEntity<RouteSectionsResponse> replaceRouteSections(
            @PathVariable UUID hermandadId,
            @PathVariable UUID procesionId,
            @Valid @RequestBody ReplaceRouteSectionsRequest request
    ) {
        log.info("Replacing route sections for procesion {} in hermandad {} ({} items)",
                procesionId, hermandadId, request.sections().size());
        var items = request.sections().stream()
                .map(this::toServiceItem)
                .toList();
        var sections = procesionService.replaceRouteSections(hermandadId, procesionId, items);
        return ResponseEntity.ok(RouteSectionsResponse.from(sections));
    }

    @PostMapping("/plan/finalize")
    @Operation(summary = "Finalize the procesion plan, making pasos and route immutable")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Plan finalized"),
            @ApiResponse(responseCode = "400", description = "Bad request — no pasos or no route sections defined"),
            @ApiResponse(responseCode = "403", description = "Forbidden — cross-tenant access"),
            @ApiResponse(responseCode = "404", description = "Procesion not found")
    })
    public ResponseEntity<FinalizePlanResponse> finalizePlan(
            @PathVariable UUID hermandadId,
            @PathVariable UUID procesionId
    ) {
        log.info("Finalizing plan for procesion {} in hermandad {}", procesionId, hermandadId);
        var procesion = procesionService.finalizePlan(hermandadId, procesionId);
        // Fetch counts for response (idempotent re-fetch is fine since finalized is immutable)
        var pasos = pasoService.getPasos(hermandadId, procesionId);
        var sections = procesionService.getRouteSections(hermandadId, procesionId);
        return ResponseEntity.ok(FinalizePlanResponse.from(procesion, pasos.size(), sections.size()));
    }

    private ProcesionService.RouteSectionItem toServiceItem(RouteSectionRequest req) {
        return new ProcesionService.RouteSectionItem(req.id(), req.name(), req.position(), req.notes());
    }
}
