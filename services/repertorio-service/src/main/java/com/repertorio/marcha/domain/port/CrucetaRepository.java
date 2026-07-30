package com.repertorio.marcha.domain.port;

import com.repertorio.marcha.domain.model.Cruceta;
import com.repertorio.marcha.domain.model.CrucetaProgression;

import java.util.Optional;
import java.util.UUID;

public interface CrucetaRepository {
    Cruceta save(Cruceta cruceta);
    Optional<Cruceta> findByProcesionId(UUID procesionId);
    Optional<CrucetaProgression> findProgressionByPasoId(UUID crucetaId, UUID pasoId);
    void saveProgression(CrucetaProgression progression);
    void deleteByProcesionId(UUID procesionId);
    boolean existsByProcesionId(UUID procesionId);
}
