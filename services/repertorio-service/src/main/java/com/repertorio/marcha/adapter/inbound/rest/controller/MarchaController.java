package com.repertorio.marcha.adapter.inbound.rest.controller;

import com.repertorio.marcha.adapter.inbound.rest.dto.MarchaRequest;
import com.repertorio.marcha.adapter.inbound.rest.dto.MarchaResponse;
import com.repertorio.marcha.application.service.MarchaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/marchas")
@RequiredArgsConstructor
@Slf4j
public class MarchaController {

    private final MarchaService marchaService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new marcha")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Marcha created"),
            @ApiResponse(responseCode = "400", description = "Invalid input")
    })
    public ResponseEntity<MarchaResponse> createMarcha(@Valid @RequestBody MarchaRequest request) {
        log.info("Creating marcha: {}", request.title());
        var marcha = marchaService.createMarcha(
                request.title(), request.composer(), request.bandType(),
                request.durationSeconds(), request.compositionYear(), request.youtubeUrl());
        return ResponseEntity.status(HttpStatus.CREATED).body(MarchaResponse.from(marcha));
    }

    @GetMapping("/search")
    @Operation(summary = "Search marchas by title or composer")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Matching marchas (empty array if none)")
    })
    public ResponseEntity<List<MarchaResponse>> searchMarchas(@RequestParam(required = true) String q) {
        log.info("Searching marchas with query: {}", q);
        var marchas = marchaService.search(q).stream()
                .map(MarchaResponse::from)
                .toList();
        return ResponseEntity.ok(marchas);
    }

    @GetMapping
    @Operation(summary = "List all marchas")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of marchas")
    })
    public ResponseEntity<List<MarchaResponse>> listMarchas() {
        log.info("Listing marchas");
        var marchas = marchaService.listMarchas().stream()
                .map(MarchaResponse::from)
                .toList();
        return ResponseEntity.ok(marchas);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a marcha by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Marcha found"),
            @ApiResponse(responseCode = "404", description = "Marcha not found")
    })
    public ResponseEntity<MarchaResponse> getMarcha(@PathVariable UUID id) {
        log.info("Getting marcha {}", id);
        return marchaService.getMarcha(id)
                .map(m -> ResponseEntity.ok(MarchaResponse.from(m)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a marcha")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Marcha updated"),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "404", description = "Marcha not found")
    })
    public ResponseEntity<MarchaResponse> updateMarcha(
            @PathVariable UUID id,
            @Valid @RequestBody MarchaRequest request) {
        log.info("Updating marcha {}", id);
        var marcha = marchaService.updateMarcha(
                id, request.title(), request.composer(), request.bandType(),
                request.durationSeconds(), request.compositionYear(), request.youtubeUrl());
        return ResponseEntity.ok(MarchaResponse.from(marcha));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a marcha")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Marcha deleted"),
            @ApiResponse(responseCode = "404", description = "Marcha not found")
    })
    public ResponseEntity<Void> deleteMarcha(@PathVariable UUID id) {
        log.info("Deleting marcha {}", id);
        marchaService.deleteMarcha(id);
        return ResponseEntity.noContent().build();
    }
}
