package com.repertorio.marcha.adapter.outbound.persistence;

import com.repertorio.marcha.domain.model.Marcha;
import com.repertorio.marcha.domain.model.VersionMismatchException;
import com.repertorio.marcha.domain.port.MarchaRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class MarchaRepositoryAdapter implements MarchaRepository {

    private final MarchaJpaRepository jpa;

    public MarchaRepositoryAdapter(MarchaJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Marcha save(Marcha marcha) {
        // Check whether this is a new or existing entity by looking it up in the DB
        var managed = jpa.findById(marcha.getId());
        if (managed.isEmpty()) {
            // New entity: construct entity with domain-assigned ID; isNew=true via @Transient → persist
            var entity = toEntity(marcha);
            var saved = jpa.save(entity);
            jpa.flush();
            return toDomain(saved);
        }
        // Existing entity: check version before applying changes to detect stale writes
        var existing = managed.get();
        if (marcha.getVersion() != existing.getVersion()) {
            throw new VersionMismatchException("Marcha", marcha.getId(), marcha.getVersion(), existing.getVersion());
        }
        existing.setTitle(marcha.getTitle());
        existing.setComposer(marcha.getComposer());
        existing.setBandType(marcha.getBandType());
        existing.setDurationSeconds(marcha.getDurationSeconds());
        existing.setCompositionYear(marcha.getCompositionYear());
        existing.setYoutubeUrl(marcha.getYoutubeUrl());
        existing.setUpdatedAt(marcha.getUpdatedAt());
        jpa.flush();
        return toDomain(existing);
    }

    @Override
    public Optional<Marcha> findById(UUID id) {
        return jpa.findById(id).map(this::toDomain);
    }

    @Override
    public List<Marcha> findAll() {
        return jpa.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    public void deleteById(UUID id) {
        var managed = jpa.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Marcha not found: " + id));
        jpa.delete(managed);
    }

    @Override
    public boolean existsById(UUID id) {
        return jpa.existsById(id);
    }

    @Override
    public List<Marcha> findByTitleContainingIgnoreCaseOrComposerContainingIgnoreCase(String title, String composer) {
        return jpa.findByTitleContainingIgnoreCaseOrComposerContainingIgnoreCase(title, composer)
                .stream().map(this::toDomain).toList();
    }

    private MarchaEntity toEntity(Marcha m) {
        return new MarchaEntity(m.getId(), m.getTitle(), m.getComposer(), m.getBandType(),
                m.getDurationSeconds(), m.getCompositionYear(), m.getYoutubeUrl(),
                m.getCreatedAt(), m.getUpdatedAt());
    }

    private Marcha toDomain(MarchaEntity e) {
        return Marcha.reconstruct(e.getId(), e.getVersion(), e.getTitle(), e.getComposer(), e.getBandType(),
                e.getDurationSeconds(), e.getCompositionYear(), e.getYoutubeUrl(),
                e.getCreatedAt(), e.getUpdatedAt());
    }
}
