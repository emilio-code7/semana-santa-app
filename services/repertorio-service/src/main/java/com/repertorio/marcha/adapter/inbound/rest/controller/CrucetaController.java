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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/hermandades/{hermandadId}/procesiones/{procesionId}/cruceta")
@RequiredArgsConstructor
@Slf4j
public class CrucetaController {

    private final CrucetaService crucetaService;

    @GetMapping
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

    @PutMapping
    @Operation(summary = "Define or replace cruceta for a procesion")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cruceta defined"),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "403", description = "Not authorized")
    })
    public ResponseEntity<CrucetaResponse> defineCruceta(
            @PathVariable UUID hermandadId,
            @PathVariable UUID procesionId,
            @Valid @RequestBody CrucetaRequest request) {
        // ponytail: inline admin check
        var auth = SecurityContextHolder.getContext().getAuthentication();
        var adminAuthority = "HERMANDAD_" + hermandadId + "_HERMANDAD_ADMIN";
        if (auth == null || auth.getAuthorities().stream().noneMatch(a -> a.getAuthority().equals(adminAuthority))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        log.info("Defining cruceta for procesion {} (hermandad {})", procesionId, hermandadId);
        var items = request.items().stream()
                .map(i -> new CrucetaItem(i.marchaId(), i.orderIndex(), i.notes()))
                .toList();
        var cruceta = crucetaService.defineCruceta(procesionId, items);
        return ResponseEntity.status(HttpStatus.OK).body(CrucetaResponse.from(cruceta));
    }
}
