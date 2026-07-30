package com.repertorio.marcha.adapter.outbound.persistence;

import com.repertorio.marcha.domain.model.KnownPaso;
import com.repertorio.marcha.domain.model.KnownProcesion;
import com.repertorio.marcha.domain.model.KnownRouteSection;
import com.repertorio.marcha.domain.port.KnownProcesionRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class KnownProcesionRepositoryAdapter implements KnownProcesionRepository {

    private final KnownProcesionJpaRepository jpa;
    private final KnownPasoJpaRepository pasoJpa;
    private final KnownRouteSectionJpaRepository routeSectionJpa;

    public KnownProcesionRepositoryAdapter(KnownProcesionJpaRepository jpa,
                                           KnownPasoJpaRepository pasoJpa,
                                           KnownRouteSectionJpaRepository routeSectionJpa) {
        this.jpa = jpa;
        this.pasoJpa = pasoJpa;
        this.routeSectionJpa = routeSectionJpa;
    }

    @Override
    public KnownProcesion save(KnownProcesion knownProcesion) {
        var entity = KnownProcesionEntity.from(knownProcesion);
        var saved = jpa.save(entity);
        return saved.toDomain();
    }

    @Override
    public Optional<KnownProcesion> findByProcesionId(UUID procesionId) {
        return jpa.findById(procesionId).map(KnownProcesionEntity::toDomain);
    }

    @Override
    public boolean existsByProcesionId(UUID procesionId) {
        return jpa.existsByProcesionId(procesionId);
    }

    @Override
    @Transactional
    public void saveFullPlan(KnownProcesion knownProcesion, List<KnownPaso> pasos, List<KnownRouteSection> routeSections) {
        jpa.save(KnownProcesionEntity.from(knownProcesion));

        // ponytail: delete-then-insert for child replace; fine for small datasets
        pasoJpa.deleteByProcesionId(knownProcesion.getProcesionId());
        routeSectionJpa.deleteByProcesionId(knownProcesion.getProcesionId());

        pasoJpa.saveAll(pasos.stream().map(KnownPasoEntity::from).toList());
        routeSectionJpa.saveAll(routeSections.stream().map(KnownRouteSectionEntity::from).toList());
    }

    @Override
    public boolean existsPasoById(UUID pasoId) {
        return pasoJpa.existsById(pasoId);
    }

    @Override
    public boolean existsRouteSectionById(UUID routeSectionId) {
        return routeSectionJpa.existsById(routeSectionId);
    }
}
