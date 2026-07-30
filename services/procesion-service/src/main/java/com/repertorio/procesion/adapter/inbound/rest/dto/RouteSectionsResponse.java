package com.repertorio.procesion.adapter.inbound.rest.dto;

import com.repertorio.procesion.domain.model.RouteSection;

import java.util.List;

public record RouteSectionsResponse(
        List<RouteSectionResponse> sections
) {
    public static RouteSectionsResponse from(List<RouteSection> domain) {
        return new RouteSectionsResponse(
                domain.stream().map(RouteSectionResponse::from).toList()
        );
    }
}
