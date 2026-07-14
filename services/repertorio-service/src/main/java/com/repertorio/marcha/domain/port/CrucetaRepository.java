package com.repertorio.marcha.domain.port;

import com.repertorio.marcha.domain.model.Cruceta;

import java.util.Optional;
import java.util.UUID;

public interface CrucetaRepository {
    Cruceta save(Cruceta cruceta);
    Optional<Cruceta> findByProcesionId(UUID procesionId);
    void deleteByProcesionId(UUID procesionId);
    boolean existsByProcesionId(UUID procesionId);
}
