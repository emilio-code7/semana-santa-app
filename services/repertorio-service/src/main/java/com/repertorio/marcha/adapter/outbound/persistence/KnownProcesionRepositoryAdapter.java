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
        var managed = jpa.findById(knownProcesion.getProcesionId());
        if (managed.isEmpty()) {
            return jpa.save(KnownProcesionEntity.from(knownProcesion)).toDomain();
        }
        var entity = managed.get();
        entity.setHermandadId(knownProcesion.getHermandadId());
        entity.setDate(knownProcesion.getDate());
        entity.setTime(knownProcesion.getTime());
        entity.setStatus(knownProcesion.getStatus());
        entity.setPlanFinalizedAt(knownProcesion.getPlanFinalizedAt());
        entity.setUpdatedAt(knownProcesion.getUpdatedAt());
        jpa.flush();
        return entity.toDomain();
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
        var managed = jpa.findById(knownProcesion.getProcesionId());
        if (managed.isEmpty()) {
            jpa.save(KnownProcesionEntity.from(knownProcesion));
        } else {
            var entity = managed.get();
            entity.setHermandadId(knownProcesion.getHermandadId());
            entity.setDate(knownProcesion.getDate());
            entity.setTime(knownProcesion.getTime());
            entity.setStatus(knownProcesion.getStatus());
            entity.setPlanFinalizedAt(knownProcesion.getPlanFinalizedAt());
            entity.setUpdatedAt(knownProcesion.getUpdatedAt());
            jpa.flush();
        }

        // ponytail: delete-then-insert for child replace; fine for small datasets
        pasoJpa.deleteByProcesionId(knownProcesion.getProcesionId());
        routeSectionJpa.deleteByProcesionId(knownProcesion.getProcesionId());

        pasoJpa.saveAll(pasos.stream().map(KnownPasoEntity::from).toList());
        routeSectionJpa.saveAll(routeSections.stream().map(KnownRouteSectionEntity::from).toList());
    }

    @Override
    public List<KnownRouteSection> findRouteSectionsByProcesionId(UUID procesionId) {
        return routeSectionJpa.findByProcesionId(procesionId).stream()
                .map(KnownRouteSectionEntity::toDomain)
                .toList();
    }

    @Override
    public List<KnownPaso> findPasosByProcesionId(UUID procesionId) {
        return pasoJpa.findByProcesionId(procesionId).stream()
                .map(KnownPasoEntity::toDomain)
                .toList();
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
