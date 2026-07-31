package com.repertorio.marcha.adapter.inbound.rest.controller;

import com.repertorio.marcha.adapter.inbound.rest.dto.CrucetaRequest;
import com.repertorio.marcha.adapter.inbound.rest.dto.CrucetaResponse;
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
@RequestMapping("/api/hermandades/{hermandadId}/procesiones/{procesionId}/pasos/{pasoId}/cruceta")
@RequiredArgsConstructor
@Slf4j
public class CrucetaController {

    private final CrucetaService crucetaService;

    @GetMapping
    @Operation(summary = "Get cruceta for a paso")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cruceta found"),
            @ApiResponse(responseCode = "404", description = "Cruceta not found")
    })
    public ResponseEntity<CrucetaResponse> getCruceta(@PathVariable UUID pasoId) {
        log.info("Getting cruceta for paso {}", pasoId);
        var cruceta = crucetaService.getCruceta(pasoId);
        return ResponseEntity.ok(CrucetaResponse.from(cruceta));
    }

    @PutMapping
    @Operation(summary = "Define or replace cruceta for a paso")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cruceta defined"),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "403", description = "Not authorized")
    })
    @PreAuthorize("@repertorioSecurity.isAdmin(#hermandadId)")
    public ResponseEntity<CrucetaResponse> defineCruceta(
            @PathVariable UUID hermandadId,
            @PathVariable UUID pasoId,
            @Valid @RequestBody CrucetaRequest request) {
        log.info("Defining cruceta for paso {} (hermandad {})", pasoId, hermandadId);
        var items = request.items().stream()
                .map(i -> new CrucetaItem(i.marchaId(), i.routeSectionId(), i.sequenceWithinSection(), i.notes()))
                .toList();
        var cruceta = crucetaService.defineCruceta(pasoId, items);
        return ResponseEntity.ok(CrucetaResponse.from(cruceta));
    }
}
