package com.repertorio.hermandad.api.controller;

import com.repertorio.hermandad.api.dto.AddMemberRequest;
import com.repertorio.hermandad.api.dto.CreateHermandadRequest;
import com.repertorio.hermandad.api.dto.HermandadResponse;
import com.repertorio.hermandad.application.service.HermandadService;
import com.repertorio.hermandad.domain.model.HermandadMember;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<HermandadResponse> createHermandad(@Valid @RequestBody CreateHermandadRequest createHermandadRequest) {
        HermandadResponse hermandad = hermandadService.createHermandad(createHermandadRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(hermandad);
    }

    @GetMapping("/{hermandadId}")
    public ResponseEntity<HermandadResponse> getHermandad(@PathVariable UUID hermandadId) {
        log.info("getHermandad {}", hermandadId);
        HermandadResponse hermandad = hermandadService.findHermandadById(hermandadId);
        return ResponseEntity.ok(hermandad);
    }

    @PostMapping("/{hermandadId}/members")
    public ResponseEntity<HermandadMember> createHermandadMember(@PathVariable UUID hermandadId, @Valid @RequestBody AddMemberRequest addMemberRequest) {
        HermandadMember member = hermandadService.addMember(hermandadId, addMemberRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(member);
    }

    @GetMapping("/{hermandadId}/members")
    public ResponseEntity<List<HermandadMember>> getHermandadMembers(@PathVariable UUID hermandadId) {
        return ResponseEntity.ok(hermandadService.getMembers(hermandadId).members());
    }

}
