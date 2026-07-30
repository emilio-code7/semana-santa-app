package com.repertorio.procesion.domain.repository;

import com.repertorio.procesion.domain.model.Paso;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PasoRepository {
    Paso save(Paso paso);
    List<Paso> findByProcesionId(UUID procesionId);
    Optional<Paso> findById(UUID id);
    void deleteByProcesionId(UUID procesionId);
    void delete(Paso paso);
}
