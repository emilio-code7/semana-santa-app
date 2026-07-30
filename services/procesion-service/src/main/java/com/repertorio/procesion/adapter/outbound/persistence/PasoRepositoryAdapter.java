package com.repertorio.procesion.adapter.outbound.persistence;

import com.repertorio.procesion.domain.model.Paso;
import com.repertorio.procesion.domain.repository.PasoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PasoRepositoryAdapter implements PasoRepository {

    private final PasoJpaRepository jpaRepository;

    @Override
    public Paso save(Paso paso) {
        var entity = PasoEntity.from(paso);
        var saved = jpaRepository.save(entity);
        return saved.toDomain();
    }

    @Override
    public List<Paso> findByProcesionId(UUID procesionId) {
        return jpaRepository.findByProcesionIdOrderByPositionAsc(procesionId)
                .stream()
                .map(PasoEntity::toDomain)
                .toList();
    }

    @Override
    public Optional<Paso> findById(UUID id) {
        return jpaRepository.findById(id).map(PasoEntity::toDomain);
    }

    @Override
    public void deleteByProcesionId(UUID procesionId) {
        jpaRepository.deleteByProcesionId(procesionId);
    }

    @Override
    public void delete(Paso paso) {
        jpaRepository.delete(PasoEntity.from(paso));
    }
}
