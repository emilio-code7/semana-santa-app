package com.repertorio.hermandad.domain.repository;

import com.repertorio.hermandad.domain.model.Titular;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TitularRepository {
    Titular save(Titular titular);
    Optional<Titular> findById(UUID id);
    List<Titular> findByHermandadId(UUID hermandadId);
    boolean existsById(UUID id);
}
