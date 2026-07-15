package com.repertorio.marcha.domain.port;

import com.repertorio.marcha.domain.model.KnownProcesion;

import java.util.Optional;
import java.util.UUID;

public interface KnownProcesionRepository {
    KnownProcesion save(KnownProcesion knownProcesion);
    Optional<KnownProcesion> findByProcesionId(UUID procesionId);
    boolean existsByProcesionId(UUID procesionId);
}
