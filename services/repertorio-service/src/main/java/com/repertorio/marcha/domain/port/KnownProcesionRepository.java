package com.repertorio.marcha.domain.port;

import com.repertorio.marcha.domain.model.KnownPaso;
import com.repertorio.marcha.domain.model.KnownProcesion;
import com.repertorio.marcha.domain.model.KnownRouteSection;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface KnownProcesionRepository {
    KnownProcesion save(KnownProcesion knownProcesion);
    Optional<KnownProcesion> findByProcesionId(UUID procesionId);
    boolean existsByProcesionId(UUID procesionId);

    /**
     * Saves the full plan projection: updates the KnownProcesion and replaces
     * its child KnownPaso and KnownRouteSection entries atomically.
     */
    void saveFullPlan(KnownProcesion knownProcesion, List<KnownPaso> pasos, List<KnownRouteSection> routeSections);

    List<KnownRouteSection> findRouteSectionsByProcesionId(UUID procesionId);

    List<KnownPaso> findPasosByProcesionId(UUID procesionId);
}
