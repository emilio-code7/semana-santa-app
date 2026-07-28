package com.repertorio.marcha.adapter.outbound.persistence;

import com.repertorio.marcha.domain.model.Cruceta;
import com.repertorio.marcha.domain.model.CrucetaItem;
import com.repertorio.marcha.domain.model.VersionMismatchException;
import com.repertorio.marcha.domain.port.CrucetaRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class CrucetaRepositoryAdapter implements CrucetaRepository {

    private final CrucetaJpaRepository jpa;

    @PersistenceContext
    private EntityManager entityManager;

    public CrucetaRepositoryAdapter(CrucetaJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Cruceta save(Cruceta cruceta) {
        var managed = jpa.findById(cruceta.getId());
        if (managed.isEmpty()) {
            // New entity: persist with domain-assigned IDs; isNew=true via @Transient
            var entity = toEntity(cruceta);
            var saved = jpa.save(entity);
            jpa.flush();
            return toDomain(saved);
        }
        // Existing entity: check version for stale-write detection
        var existing = managed.get();
        if (cruceta.getVersion() != existing.getVersion()) {
            throw new VersionMismatchException("Cruceta", cruceta.getId(), cruceta.getVersion(), existing.getVersion());
        }
        // Replace items on managed instance
        // JPQL bulk delete bypasses Hibernate's unidirectional @OneToMany UPDATE FK=null behavior
        entityManager.createQuery("DELETE FROM CrucetaItemEntity i WHERE i.crucetaId = :id")
                .setParameter("id", existing.getId())
                .executeUpdate();

        // Flush and clear PC to ensure no stale children remain in persistence context.
        // flush()/clear()/reload() replaces refresh() because cascade REFRESH could
        // interact unsafely with JPQL-deleted child entities.
        jpa.flush();
        entityManager.clear();

        var fresh = jpa.findById(cruceta.getId()).orElseThrow();
        fresh.setUpdatedAt(cruceta.getUpdatedAt());

        var itemEntities = cruceta.getItems().stream()
                .map(item -> new CrucetaItemEntity(item.getId(), cruceta.getId(),
                        item.getMarchaId(), item.getOrderIndex(), item.getNotes()))
                .toList();
        fresh.setItems(itemEntities);
        jpa.flush();
        return toDomain(fresh);
    }

    @Override
    public Optional<Cruceta> findByProcesionId(UUID procesionId) {
        return jpa.findByProcesionId(procesionId).map(this::toDomain);
    }

    @Override
    public void deleteByProcesionId(UUID procesionId) {
        var existing = jpa.findByProcesionId(procesionId);
        existing.ifPresent(entity -> {
            // Delete items first via JPQL to avoid Hibernate's UPDATE FK=null
            entityManager.createQuery("DELETE FROM CrucetaItemEntity i WHERE i.crucetaId = :id")
                    .setParameter("id", entity.getId())
                    .executeUpdate();

            // Flush + clear to avoid stale children in PC before parent deletion.
            // deleteById reloads from DB (no items after JPQL delete) → safe cascade.
            jpa.flush();
            entityManager.clear();
            jpa.deleteById(entity.getId());
        });
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
                .map(i -> CrucetaItem.reconstruct(i.getId(), i.getVersion(), i.getMarchaId(), i.getOrderIndex(), i.getNotes()))
                .toList();
        return Cruceta.reconstruct(e.getId(), e.getVersion(), e.getProcesionId(), items, e.getCreatedAt(), e.getUpdatedAt());
    }
}
