package com.repertorio.procesion.adapter.outbound.persistence;

import com.repertorio.procesion.domain.model.RouteSection;
import com.repertorio.procesion.domain.repository.RouteSectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RouteSectionRepositoryAdapter implements RouteSectionRepository {

    private final RouteSectionJpaRepository jpaRepository;

    @Override
    public RouteSection save(RouteSection section) {
        var entity = RouteSectionEntity.from(section);
        var saved = jpaRepository.save(entity);
        return saved.toDomain();
    }

    @Override
    public List<RouteSection> findByProcesionIdOrderByPositionAsc(UUID procesionId) {
        return jpaRepository.findByProcesionIdOrderByPositionAsc(procesionId)
                .stream()
                .map(RouteSectionEntity::toDomain)
                .toList();
    }

    @Override
    public void deleteByProcesionId(UUID procesionId) {
        jpaRepository.deleteByProcesionId(procesionId);
    }
}
