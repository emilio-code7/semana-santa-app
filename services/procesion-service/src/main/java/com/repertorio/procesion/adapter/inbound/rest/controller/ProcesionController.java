package com.repertorio.procesion.adapter.inbound.rest.controller;

import com.repertorio.procesion.adapter.inbound.rest.dto.CreateProcesionRequest;
import com.repertorio.procesion.adapter.inbound.rest.dto.StatusChangeRequest;
import com.repertorio.procesion.adapter.inbound.rest.dto.ProcesionResponse;
import com.repertorio.procesion.application.service.ProcesionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/procesiones")
@RequiredArgsConstructor
@Slf4j
public class ProcesionController {

    private final ProcesionService procesionService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new procesion")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Procesion created"),
            @ApiResponse(responseCode = "400", description = "Invalid input (validation error)")
    })
    public ResponseEntity<ProcesionResponse> createProcesion(@Valid @RequestBody CreateProcesionRequest request) {
        log.info("Creating procesion for hermandad {} on {}", request.hermandadId(), request.date());
        var procesion = procesionService.createProcesion(request.hermandadId(), request.date(), request.time());
        return ResponseEntity.status(HttpStatus.CREATED).body(ProcesionResponse.from(procesion));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a procesion by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Procesion found"),
            @ApiResponse(responseCode = "404", description = "Procesion not found")
    })
    public ResponseEntity<ProcesionResponse> getProcesion(@PathVariable UUID id) {
        log.info("Getting procesion {}", id);
        var procesion = procesionService.getProcesion(id);
        return ResponseEntity.ok(ProcesionResponse.from(procesion));
    }

    @GetMapping
    @Operation(summary = "List procesiones by hermandad")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paginated list of procesiones")
    })
    public ResponseEntity<Page<ProcesionResponse>> listByHermandad(
            @RequestParam UUID hermandadId,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        log.info("Listing procesiones for hermandad {}", hermandadId);
        return ResponseEntity.ok(
                procesionService.listByHermandad(hermandadId, pageable)
                        .map(ProcesionResponse::from));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Change procesion status")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status changed"),
            @ApiResponse(responseCode = "400", description = "Invalid status transition"),
            @ApiResponse(responseCode = "404", description = "Procesion not found")
    })
    public ResponseEntity<ProcesionResponse> changeStatus(
            @PathVariable UUID id,
            @Valid @RequestBody StatusChangeRequest request
    ) {
        log.info("Changing status of procesion {} to {}", id, request.newStatus());
        var procesion = procesionService.changeStatus(id, request.newStatus());
        return ResponseEntity.ok(ProcesionResponse.from(procesion));
    }

    @PostMapping("/{id}/finalize-plan")
    @Operation(summary = "Finalize the plan for a procesion (idempotent)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Plan finalized"),
            @ApiResponse(responseCode = "404", description = "Procesion not found")
    })
    public ResponseEntity<ProcesionResponse> finalizePlan(
            @PathVariable UUID id,
            @RequestParam UUID hermandadId
    ) {
        log.info("Finalizing plan for procesion {}", id);
        var procesion = procesionService.finalizePlan(hermandadId, id);
        return ResponseEntity.ok(ProcesionResponse.from(procesion));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a procesion")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Procesion deleted"),
            @ApiResponse(responseCode = "404", description = "Procesion not found")
    })
    public ResponseEntity<Void> deleteProcesion(@PathVariable UUID id) {
        log.info("Deleting procesion {}", id);
        procesionService.deleteProcesion(id);
        return ResponseEntity.noContent().build();
    }
}
