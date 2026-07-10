package com.repertorio.procesion.adapter.outbound.persistence;

import com.repertorio.procesion.domain.model.Procesion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ProcesionJpaRepository extends JpaRepository<Procesion, UUID> {
    Page<Procesion> findByHermandadId(UUID hermandadId, Pageable pageable);
}
