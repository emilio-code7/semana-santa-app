package com.repertorio.procesion.adapter.outbound.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PasoJpaRepository extends JpaRepository<PasoEntity, UUID> {
    List<PasoEntity> findByProcesionIdOrderByPositionAsc(UUID procesionId);
    void deleteByProcesionId(UUID procesionId);
}
