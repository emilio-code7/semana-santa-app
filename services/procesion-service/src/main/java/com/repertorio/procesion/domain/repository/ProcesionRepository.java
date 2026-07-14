package com.repertorio.procesion.domain.repository;

import com.repertorio.procesion.domain.model.Procesion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface ProcesionRepository {
    Procesion save(Procesion procesion);
    Optional<Procesion> findById(UUID id);
    Page<Procesion> findByHermandadId(UUID hermandadId, Pageable pageable);
    void deleteById(UUID id);
    void delete(Procesion procesion);
}
