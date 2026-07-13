package com.repertorio.hermandad.domain.repository;

import com.repertorio.hermandad.domain.model.Hermandad;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HermandadRepository {
    Hermandad save(Hermandad hermandad);
    Optional<Hermandad> findById(UUID id);
    List<Hermandad> findAll();
    boolean existsById(UUID id);
    boolean existsByName(String name);
}
