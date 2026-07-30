package com.repertorio.procesion.adapter.inbound.rest.dto;

import jakarta.validation.Valid;

import java.util.List;

public record ReplaceRouteSectionsRequest(
        @Valid List<RouteSectionRequest> sections
) {}
