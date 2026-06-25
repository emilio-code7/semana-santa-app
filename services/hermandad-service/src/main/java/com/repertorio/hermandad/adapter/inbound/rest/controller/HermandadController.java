package com.repertorio.hermandad.adapter.inbound.rest.controller;

import com.repertorio.hermandad.adapter.inbound.rest.dto.AddMemberRequest;
import com.repertorio.hermandad.adapter.inbound.rest.dto.ChangeRoleRequest;
import com.repertorio.hermandad.adapter.inbound.rest.dto.CreateHermandadRequest;
import com.repertorio.hermandad.adapter.inbound.rest.dto.HermandadResponse;
import com.repertorio.hermandad.application.service.HermandadService;
import com.repertorio.hermandad.domain.model.HermandadMember;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/hermandades")
@RequiredArgsConstructor
@Slf4j
public class HermandadController {

    private final HermandadService hermandadService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new hermandad")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Hermandad created"),
            @ApiResponse(responseCode = "400", description = "Invalid input (validation error)")
    })
    public ResponseEntity<HermandadResponse> createHermandad(@Valid @RequestBody CreateHermandadRequest createHermandadRequest) {
        var auth = (JwtAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        HermandadResponse hermandad = hermandadService.createHermandad(createHermandadRequest, auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(hermandad);
    }

    @GetMapping("/{hermandadId}")
    @Operation(summary = "Get a hermandad by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Hermandad found"),
            @ApiResponse(responseCode = "400", description = "Invalid ID format"),
            @ApiResponse(responseCode = "404", description = "Hermandad not found")
    })
    public ResponseEntity<HermandadResponse> getHermandad(@PathVariable UUID hermandadId) {
        log.info("getHermandad {}", hermandadId);
        HermandadResponse hermandad = hermandadService.findHermandadById(hermandadId);
        return ResponseEntity.ok(hermandad);
    }

    @PostMapping("/{hermandadId}/members")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@hermandadSecurity.isAdmin(#hermandadId)")
    @Operation(summary = "Add a member to a hermandad")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Member added"),
            @ApiResponse(responseCode = "400", description = "Invalid input (validation error or illegal argument)"),
            @ApiResponse(responseCode = "404", description = "Hermandad not found")
    })
    public ResponseEntity<HermandadMember> createHermandadMember(@PathVariable UUID hermandadId, @Valid @RequestBody AddMemberRequest addMemberRequest) {
        HermandadMember member = hermandadService.addMember(hermandadId, addMemberRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(member);
    }

    @GetMapping("/{hermandadId}/members")
    @PreAuthorize("@hermandadSecurity.isAdmin(#hermandadId)")
    @Operation(summary = "List members of a hermandad")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of members",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = HermandadMember.class)))),
            @ApiResponse(responseCode = "404", description = "Hermandad not found")
    })
    public ResponseEntity<List<HermandadMember>> getHermandadMembers(@PathVariable UUID hermandadId) {
        return ResponseEntity.ok(hermandadService.getMembers(hermandadId).members());
    }

    @PatchMapping("/{hermandadId}/members/{userId}/role")
    @PreAuthorize("@hermandadSecurity.isAdmin(#hermandadId)")
    @Operation(summary = "Change a member's role")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Role updated"),
            @ApiResponse(responseCode = "400", description = "Invalid input (validation error)"),
            @ApiResponse(responseCode = "404", description = "Member not found in this hermandad")
    })
    public ResponseEntity<HermandadMember> changeHermandadMemberRole(
            @PathVariable UUID hermandadId,
            @PathVariable String userId,
            @Valid @RequestBody ChangeRoleRequest request
    ) {
        HermandadMember member = hermandadService.changeRole(hermandadId, userId, request.role());
        return ResponseEntity.ok(member);
    }

    @DeleteMapping("/{hermandadId}/members/{userId}")
    @PreAuthorize("@hermandadSecurity.isAdmin(#hermandadId)")
    @Operation(summary = "Delete a member from hermandad")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Member deleted"),
            @ApiResponse(responseCode = "404", description = "Member not found in this hermandad")
    })
    public ResponseEntity<Void> deleteHermandadMember(@PathVariable UUID hermandadId, @PathVariable String userId) {
        hermandadService.removeMember(hermandadId, userId);
        return ResponseEntity.noContent().build();
    }

}
