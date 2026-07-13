package com.repertorio.procesion.adapter.inbound.rest.controller;

import com.repertorio.procesion.adapter.inbound.rest.dto.CreateProcesionRequest;
import com.repertorio.procesion.adapter.inbound.rest.dto.EstadoChangeRequest;
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
    @Operation(summary = "Create a new procesión")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Procesión created"),
            @ApiResponse(responseCode = "400", description = "Invalid input (validation error)")
    })
    public ResponseEntity<ProcesionResponse> crearProcesion(@Valid @RequestBody CreateProcesionRequest request) {
        log.info("Creating procesión for hermandad {} on {}", request.hermandadId(), request.fecha());
        var procesion = procesionService.crearProcesion(request.hermandadId(), request.fecha(), request.hora());
        return ResponseEntity.status(HttpStatus.CREATED).body(ProcesionResponse.from(procesion));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a procesión by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Procesión found"),
            @ApiResponse(responseCode = "404", description = "Procesión not found")
    })
    public ResponseEntity<ProcesionResponse> obtenerProcesion(@PathVariable UUID id) {
        log.info("Obtaining procesión {}", id);
        var procesion = procesionService.obtenerProcesion(id);
        return ResponseEntity.ok(ProcesionResponse.from(procesion));
    }

    @GetMapping
    @Operation(summary = "List procesiones by hermandad")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paginated list of procesiones")
    })
    public ResponseEntity<Page<ProcesionResponse>> listarPorHermandad(
            @RequestParam UUID hermandadId,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        log.info("Listing procesiones for hermandad {}", hermandadId);
        return ResponseEntity.ok(
                procesionService.listarPorHermandad(hermandadId, pageable)
                        .map(ProcesionResponse::from));
    }

    @PatchMapping("/{id}/estado")
    @Operation(summary = "Change procesión estado")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estado changed"),
            @ApiResponse(responseCode = "400", description = "Invalid estado transition"),
            @ApiResponse(responseCode = "404", description = "Procesión not found")
    })
    public ResponseEntity<ProcesionResponse> cambiarEstado(
            @PathVariable UUID id,
            @Valid @RequestBody EstadoChangeRequest request
    ) {
        log.info("Changing estado of procesión {} to {}", id, request.nuevoEstado());
        var procesion = procesionService.cambiarEstado(id, request.nuevoEstado());
        return ResponseEntity.ok(ProcesionResponse.from(procesion));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a procesión")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Procesión deleted"),
            @ApiResponse(responseCode = "404", description = "Procesión not found")
    })
    public ResponseEntity<Void> eliminarProcesion(@PathVariable UUID id) {
        log.info("Deleting procesión {}", id);
        procesionService.eliminarProcesion(id);
        return ResponseEntity.noContent().build();
    }
}
