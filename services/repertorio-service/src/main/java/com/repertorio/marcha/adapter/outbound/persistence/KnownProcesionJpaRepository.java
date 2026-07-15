package com.repertorio.marcha.adapter.outbound.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface KnownProcesionJpaRepository extends JpaRepository<KnownProcesionEntity, UUID> {

    boolean existsByProcesionId(UUID procesionId);
}
