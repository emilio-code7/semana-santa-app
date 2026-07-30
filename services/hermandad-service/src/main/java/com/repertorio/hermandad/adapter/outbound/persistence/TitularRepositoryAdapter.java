package com.repertorio.hermandad.adapter.outbound.persistence;

import com.repertorio.hermandad.domain.model.Titular;
import com.repertorio.hermandad.domain.repository.TitularRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TitularRepositoryAdapter implements TitularRepository {

    private final TitularJpaRepository jpaRepository;

    @Override
    public Titular save(Titular titular) {
        if (titular.getId() == null) {
            var entity = TitularEntity.from(titular);
            var saved = jpaRepository.save(entity);
            jpaRepository.flush();
            return saved.toDomain();
        }
        // Existing entity: load managed instance, copy mutable fields, preserve version
        var managed = jpaRepository.findById(titular.getId())
                .orElseThrow(() -> new IllegalArgumentException("Titular not found: " + titular.getId()));
        managed.setName(titular.getName());
        managed.setDescription(titular.getDescription());
        jpaRepository.flush();
        return managed.toDomain();
    }

    @Override
    public Optional<Titular> findById(UUID id) {
        return jpaRepository.findById(id).map(TitularEntity::toDomain);
    }

    @Override
    public List<Titular> findByHermandadId(UUID hermandadId) {
        return jpaRepository.findByHermandadId(hermandadId).stream()
                .map(TitularEntity::toDomain)
                .toList();
    }

    @Override
    public boolean existsById(UUID id) {
        return jpaRepository.existsById(id);
    }
}
