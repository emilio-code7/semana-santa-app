package com.repertorio.hermandad.adapter.inbound.rest.controller;

import com.repertorio.hermandad.adapter.inbound.rest.dto.CreateTitularRequest;
import com.repertorio.hermandad.adapter.inbound.rest.dto.TitularResponse;
import com.repertorio.hermandad.adapter.inbound.rest.dto.UpdateTitularRequest;
import com.repertorio.hermandad.application.service.TitularService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/hermandades/{hermandadId}/titulares")
@RequiredArgsConstructor
public class TitularController {

    private final TitularService titularService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@hermandadSecurity.canManageTitulares(#hermandadId)")
    @Operation(summary = "Create a titular for a hermandad")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Titular created"),
            @ApiResponse(responseCode = "400", description = "Invalid input (validation error)"),
            @ApiResponse(responseCode = "403", description = "Forbidden — not admin or capataz of this hermandad")
    })
    public ResponseEntity<TitularResponse> createTitular(
            @PathVariable UUID hermandadId,
            @Valid @RequestBody CreateTitularRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(titularService.createTitular(hermandadId, request));
    }

    @GetMapping
    @PreAuthorize("@hermandadSecurity.isMember(#hermandadId)")
    @Operation(summary = "List titulares of a hermandad")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of titulares"),
            @ApiResponse(responseCode = "403", description = "Forbidden — not a member of this hermandad")
    })
    public ResponseEntity<List<TitularResponse>> listTitulares(@PathVariable UUID hermandadId) {
        return ResponseEntity.ok(titularService.listTitulares(hermandadId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@hermandadSecurity.isMember(#hermandadId)")
    @Operation(summary = "Get a titular by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Titular found"),
            @ApiResponse(responseCode = "403", description = "Forbidden — not a member of this hermandad"),
            @ApiResponse(responseCode = "404", description = "Titular not found")
    })
    public ResponseEntity<TitularResponse> getTitular(
            @PathVariable UUID hermandadId,
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(titularService.getTitular(hermandadId, id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@hermandadSecurity.canManageTitulares(#hermandadId)")
    @Operation(summary = "Update a titular")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Titular updated"),
            @ApiResponse(responseCode = "400", description = "Invalid input (validation error)"),
            @ApiResponse(responseCode = "403", description = "Forbidden — not admin or capataz of this hermandad"),
            @ApiResponse(responseCode = "404", description = "Titular not found")
    })
    public ResponseEntity<TitularResponse> updateTitular(
            @PathVariable UUID hermandadId,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTitularRequest request
    ) {
        return ResponseEntity.ok(titularService.updateTitular(hermandadId, id, request));
    }
}
