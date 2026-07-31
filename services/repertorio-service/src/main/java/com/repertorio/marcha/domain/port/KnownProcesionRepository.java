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
     * Persists the full plan projection: procesion, pasos and route sections.
     */
    void saveFullPlan(KnownProcesion knownProcesion, List<KnownPaso> pasos, List<KnownRouteSection> routeSections);

    List<KnownRouteSection> findRouteSectionsByProcesionId(UUID procesionId);

    List<KnownPaso> findPasosByProcesionId(UUID procesionId);

    /** Returns true if the given paso ID exists in the local plan projection. */
    boolean existsPasoById(UUID pasoId);

    /** Returns true if the given route section ID exists in the local plan projection. */
    boolean existsRouteSectionById(UUID routeSectionId);
}
