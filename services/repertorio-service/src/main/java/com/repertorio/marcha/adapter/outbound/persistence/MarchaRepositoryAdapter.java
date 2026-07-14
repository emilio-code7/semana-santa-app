package com.repertorio.marcha.adapter.outbound.persistence;

import com.repertorio.marcha.domain.model.Marcha;
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
        var entity = toEntity(marcha);
        var saved = jpa.save(entity);
        return toDomain(saved);
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
        jpa.deleteById(id);
    }

    @Override
    public boolean existsById(UUID id) {
        return jpa.existsById(id);
    }

    private MarchaEntity toEntity(Marcha m) {
        return new MarchaEntity(m.getId(), m.getTitle(), m.getComposer(), m.getBandType(),
                m.getDurationSeconds(), m.getCompositionYear(), m.getYoutubeUrl(),
                m.getCreatedAt(), m.getUpdatedAt());
    }

    private Marcha toDomain(MarchaEntity e) {
        return Marcha.reconstruct(e.getId(), e.getTitle(), e.getComposer(), e.getBandType(),
                e.getDurationSeconds(), e.getCompositionYear(), e.getYoutubeUrl(),
                e.getCreatedAt(), e.getUpdatedAt());
    }
}
