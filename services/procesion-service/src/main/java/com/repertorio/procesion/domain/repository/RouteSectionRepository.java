package com.repertorio.procesion.domain.repository;

import com.repertorio.procesion.domain.model.RouteSection;

import java.util.List;
import java.util.UUID;

public interface RouteSectionRepository {
    RouteSection save(RouteSection section);
    List<RouteSection> findByProcesionIdOrderByPositionAsc(UUID procesionId);
    void deleteByProcesionId(UUID procesionId);
}
