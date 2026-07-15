package com.repertorio.marcha.adapter.outbound.persistence;

import com.repertorio.marcha.domain.model.KnownProcesion;
import com.repertorio.marcha.domain.port.KnownProcesionRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class KnownProcesionRepositoryAdapter implements KnownProcesionRepository {

    private final KnownProcesionJpaRepository jpa;

    public KnownProcesionRepositoryAdapter(KnownProcesionJpaRepository jpa) {
        this.jpa = jpa;
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
}
