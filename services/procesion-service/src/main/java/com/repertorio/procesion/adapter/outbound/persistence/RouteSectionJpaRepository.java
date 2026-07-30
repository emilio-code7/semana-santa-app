package com.repertorio.procesion.adapter.outbound.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RouteSectionJpaRepository extends JpaRepository<RouteSectionEntity, UUID> {
    List<RouteSectionEntity> findByProcesionIdOrderByPositionAsc(UUID procesionId);
    void deleteByProcesionId(UUID procesionId);
}
