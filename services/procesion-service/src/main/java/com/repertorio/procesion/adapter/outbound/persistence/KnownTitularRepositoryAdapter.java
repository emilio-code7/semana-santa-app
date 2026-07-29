package com.repertorio.procesion.adapter.outbound.persistence;

import com.repertorio.procesion.domain.model.KnownTitular;
import com.repertorio.procesion.domain.port.KnownTitularRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class KnownTitularRepositoryAdapter implements KnownTitularRepository {

    private final KnownTitularJpaRepository jpa;

    public KnownTitularRepositoryAdapter(KnownTitularJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public KnownTitular save(KnownTitular knownTitular) {
        var entity = KnownTitularEntity.from(knownTitular);
        var saved = jpa.save(entity);
        return saved.toDomain();
    }

    @Override
    public Optional<KnownTitular> findById(UUID id) {
        return jpa.findById(id).map(KnownTitularEntity::toDomain);
    }

    @Override
    public boolean existsById(UUID id) {
        return jpa.existsById(id);
    }
}
