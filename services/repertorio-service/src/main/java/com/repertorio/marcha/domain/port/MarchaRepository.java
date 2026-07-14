package com.repertorio.marcha.domain.port;

import com.repertorio.marcha.domain.model.Marcha;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MarchaRepository {
    Marcha save(Marcha marcha);
    Optional<Marcha> findById(UUID id);
    List<Marcha> findAll();
    void deleteById(UUID id);
    boolean existsById(UUID id);
}
