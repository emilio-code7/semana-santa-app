package com.repertorio.marcha.adapter.inbound.rest.controller;

import com.repertorio.marcha.adapter.inbound.rest.dto.AdvanceCurrentRequest;
import com.repertorio.marcha.adapter.inbound.rest.dto.CrucetaRequest;
import com.repertorio.marcha.adapter.inbound.rest.dto.CrucetaResponse;
import com.repertorio.marcha.adapter.inbound.rest.dto.RunSheetResponse;
import com.repertorio.marcha.application.service.CrucetaService;
import com.repertorio.marcha.domain.model.CrucetaItem;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/hermandades/{hermandadId}/procesiones/{procesionId}")
@RequiredArgsConstructor
@Slf4j
public class CrucetaController {

    private final CrucetaService crucetaService;

    @GetMapping("/cruceta")
    @Operation(summary = "Get cruceta for a procesion")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cruceta found"),
            @ApiResponse(responseCode = "404", description = "Cruceta not found")
    })
    public ResponseEntity<CrucetaResponse> getCruceta(@PathVariable UUID procesionId) {
        log.info("Getting cruceta for procesion {}", procesionId);
        var cruceta = crucetaService.getCruceta(procesionId);
        return ResponseEntity.ok(CrucetaResponse.from(cruceta));
    }

    @PutMapping("/cruceta")
    @Operation(summary = "Define or replace cruceta for a procesion")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cruceta defined"),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "403", description = "Not authorized")
    })
    @PreAuthorize("@repertorioSecurity.isAdmin(#hermandadId)")
    public ResponseEntity<CrucetaResponse> defineCruceta(
            @PathVariable UUID hermandadId,
            @PathVariable UUID procesionId,
            @Valid @RequestBody CrucetaRequest request) {
        log.info("Defining cruceta for procesion {} (hermandad {})", procesionId, hermandadId);
        var items = request.items().stream()
                .map(i -> new CrucetaItem(i.marchaId(), i.routeSectionId(), i.orderIndex(), i.notes()))
                .toList();
        var cruceta = crucetaService.defineCruceta(procesionId, items);
        return ResponseEntity.ok(CrucetaResponse.from(cruceta));
    }

    @GetMapping("/pasos/{pasoId}/cruceta/run-sheet")
    @Operation(summary = "Get run sheet for a paso with current/next indicators")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Run sheet returned"),
            @ApiResponse(responseCode = "403", description = "Cross-tenant access denied"),
            @ApiResponse(responseCode = "404", description = "Cruceta not found")
    })
    @PreAuthorize("@repertorioSecurity.isAdmin(#hermandadId)")
    public ResponseEntity<RunSheetResponse> getRunSheet(
            @PathVariable UUID hermandadId,
            @PathVariable UUID procesionId,
            @PathVariable UUID pasoId) {
        log.info("Getting run sheet for paso {} in procesion {} (hermandad {})", pasoId, procesionId, hermandadId);
        var runSheet = crucetaService.getRunSheet(procesionId, pasoId);
        return ResponseEntity.ok(runSheet);
    }

    @PutMapping("/pasos/{pasoId}/cruceta/current")
    @Operation(summary = "Advance current progression item for a paso (idempotent)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Current item advanced"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "403", description = "Cross-tenant access denied"),
            @ApiResponse(responseCode = "404", description = "Cruceta not found")
    })
    @PreAuthorize("@repertorioSecurity.isAdmin(#hermandadId)")
    public ResponseEntity<RunSheetResponse> advanceCurrent(
            @PathVariable UUID hermandadId,
            @PathVariable UUID procesionId,
            @PathVariable UUID pasoId,
            @Valid @RequestBody AdvanceCurrentRequest request) {
        log.info("Advancing current for paso {} in procesion {} (hermandad {})", pasoId, procesionId, hermandadId);
        var runSheet = crucetaService.advanceCurrent(procesionId, pasoId,
                request.routeSectionId(), request.crucetaItemId());
        return ResponseEntity.ok(runSheet);
    }
}
