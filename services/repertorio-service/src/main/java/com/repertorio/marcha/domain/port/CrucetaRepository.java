package com.repertorio.marcha.domain.port;

import com.repertorio.marcha.domain.model.Cruceta;

import java.util.Optional;
import java.util.UUID;

public interface CrucetaRepository {
    Cruceta save(Cruceta cruceta);
    Optional<Cruceta> findByPasoId(UUID pasoId);
    void deleteByPasoId(UUID pasoId);
    boolean existsByPasoId(UUID pasoId);
}
