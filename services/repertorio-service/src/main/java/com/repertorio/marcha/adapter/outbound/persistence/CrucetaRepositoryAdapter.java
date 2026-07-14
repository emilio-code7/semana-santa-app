package com.repertorio.marcha.adapter.outbound.persistence;

import com.repertorio.marcha.domain.model.Cruceta;
import com.repertorio.marcha.domain.model.CrucetaItem;
import com.repertorio.marcha.domain.port.CrucetaRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class CrucetaRepositoryAdapter implements CrucetaRepository {

    private final CrucetaJpaRepository jpa;

    public CrucetaRepositoryAdapter(CrucetaJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Cruceta save(Cruceta cruceta) {
        var entity = toEntity(cruceta);
        var saved = jpa.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<Cruceta> findByProcesionId(UUID procesionId) {
        return jpa.findByProcesionId(procesionId).map(this::toDomain);
    }

    @Override
    public void deleteByProcesionId(UUID procesionId) {
        jpa.deleteByProcesionId(procesionId);
    }

    @Override
    public boolean existsByProcesionId(UUID procesionId) {
        return jpa.findByProcesionId(procesionId).isPresent();
    }

    private CrucetaEntity toEntity(Cruceta c) {
        var entity = new CrucetaEntity(c.getId(), c.getProcesionId(), c.getCreatedAt(), c.getUpdatedAt());
        var itemEntities = c.getItems().stream()
                .map(item -> new CrucetaItemEntity(item.getId(), c.getId(),
                        item.getMarchaId(), item.getOrderIndex(), item.getNotes()))
                .toList();
        entity.setItems(itemEntities);
        return entity;
    }

    private Cruceta toDomain(CrucetaEntity e) {
        var items = e.getItems().stream()
                .map(i -> CrucetaItem.reconstruct(i.getId(), i.getMarchaId(), i.getOrderIndex(), i.getNotes()))
                .toList();
        return Cruceta.reconstruct(e.getId(), e.getProcesionId(), items, e.getCreatedAt(), e.getUpdatedAt());
    }
}
