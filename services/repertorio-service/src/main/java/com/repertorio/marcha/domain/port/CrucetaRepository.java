package com.repertorio.marcha.domain.port;

import com.repertorio.marcha.domain.model.Cruceta;
import com.repertorio.marcha.domain.model.CrucetaProgression;

import java.util.Optional;
import java.util.UUID;

public interface CrucetaRepository {
    Cruceta save(Cruceta cruceta);
    Optional<Cruceta> findByPasoId(UUID pasoId);
    Optional<CrucetaProgression> findProgressionByPasoId(UUID crucetaId, UUID pasoId);
    void saveProgression(CrucetaProgression progression);
    void deleteByPasoId(UUID pasoId);
    boolean existsByPasoId(UUID pasoId);
}
