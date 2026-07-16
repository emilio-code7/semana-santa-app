package com.repertorio.procesion.adapter.outbound.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ProcesionJpaRepository extends JpaRepository<ProcesionEntity, UUID> {
    Page<ProcesionEntity> findByHermandadId(UUID hermandadId, Pageable pageable);
}
