package com.repertorio.hermandad.adapter.outbound.persistence;

import com.repertorio.hermandad.domain.model.Hermandad;
import com.repertorio.hermandad.domain.repository.HermandadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class HermandadRepositoryAdapter implements HermandadRepository {
    private final HermandadJpaRepository jpaRepository;

    @Override
    public Hermandad save(Hermandad hermandad) {
        if (hermandad.getId() == null) {
            // New entity: construct fresh
            var entity = HermandadEntity.from(hermandad);
            var saved = jpaRepository.save(entity);
            jpaRepository.flush();
            return saved.toDomain();
        }
        // Existing entity: load managed instance, copy mutable fields, preserve version
        var managed = jpaRepository.findById(hermandad.getId())
                .orElseThrow(() -> new IllegalArgumentException("Hermandad not found: " + hermandad.getId()));
        managed.setName(hermandad.getName());
        managed.setCity(hermandad.getCity());
        managed.setFoundedYear(hermandad.getFoundedYear());
        managed.setKeycloakGroupId(hermandad.getKeycloakGroupId());
        managed.setDescription(hermandad.getDescription());
        jpaRepository.flush();
        return managed.toDomain();
    }

    @Override
    public Optional<Hermandad> findById(UUID id) {
        return jpaRepository.findById(id).map(HermandadEntity::toDomain);
    }

    @Override
    public boolean existsById(UUID id) {
        return jpaRepository.existsById(id);
    }

    @Override
    public boolean existsByName(String name) {
        return jpaRepository.existsByName(name);
    }

    @Override
    public List<Hermandad> findAll() {
        return jpaRepository.findAll().stream().map(HermandadEntity::toDomain).toList();
    }
}
