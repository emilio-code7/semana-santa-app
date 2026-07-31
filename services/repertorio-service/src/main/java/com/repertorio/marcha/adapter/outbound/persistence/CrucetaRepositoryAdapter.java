package com.repertorio.marcha.adapter.outbound.persistence;

import com.repertorio.marcha.domain.model.Cruceta;
import com.repertorio.marcha.domain.model.CrucetaItem;
import com.repertorio.marcha.domain.model.CrucetaProgression;
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
    private final CrucetaProgressionJpaRepository progressionJpa;

    @PersistenceContext
    private EntityManager entityManager;

    public CrucetaRepositoryAdapter(CrucetaJpaRepository jpa, CrucetaProgressionJpaRepository progressionJpa) {
        this.jpa = jpa;
        this.progressionJpa = progressionJpa;
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
        // Replace progressions on managed instance
        entityManager.createQuery("DELETE FROM CrucetaProgressionEntity p WHERE p.crucetaId = :id")
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
                        item.getMarchaId(), item.getRouteSectionId(),
                        item.getSequenceWithinSection(), item.getNotes()))
                .toList();
        fresh.setItems(itemEntities);
        jpa.flush();
        return toDomain(fresh);
    }

    @Override
    public Optional<Cruceta> findByPasoId(UUID pasoId) {
        return jpa.findByPasoId(pasoId).map(this::toDomain);
    }

    @Override
    public Optional<CrucetaProgression> findProgressionByPasoId(UUID crucetaId, UUID pasoId) {
        return progressionJpa.findByCrucetaIdAndPasoId(crucetaId, pasoId)
                .map(CrucetaProgressionEntity::toDomain);
    }

    @Override
    public void saveProgression(CrucetaProgression progression) {
        var managed = progressionJpa.findById(progression.getId());
        if (managed.isPresent()) {
            var entity = managed.get();
            entity.setCurrentRouteSectionId(progression.getCurrentRouteSectionId());
            entity.setCurrentCrucetaItemId(progression.getCurrentCrucetaItemId().orElse(null));
            progressionJpa.flush();
        } else {
            progressionJpa.save(CrucetaProgressionEntity.from(progression));
            progressionJpa.flush();
        }
    }

    @Override
    public void deleteByPasoId(UUID pasoId) {
        var existing = jpa.findByPasoId(pasoId);
        existing.ifPresent(entity -> {
            // Delete items and progressions first via JPQL to avoid Hibernate's UPDATE FK=null
            entityManager.createQuery("DELETE FROM CrucetaItemEntity i WHERE i.crucetaId = :id")
                    .setParameter("id", entity.getId())
                    .executeUpdate();
            entityManager.createQuery("DELETE FROM CrucetaProgressionEntity p WHERE p.crucetaId = :id")
                    .setParameter("id", entity.getId())
                    .executeUpdate();

            // Flush + clear to avoid stale children in PC before parent deletion.
            // deleteById reloads from DB (no children after JPQL delete) → safe cascade.
            jpa.flush();
            entityManager.clear();
            jpa.deleteById(entity.getId());
        });
    }

    @Override
    public boolean existsByPasoId(UUID pasoId) {
        return jpa.findByPasoId(pasoId).isPresent();
    }

    private CrucetaEntity toEntity(Cruceta c) {
        var entity = new CrucetaEntity(c.getId(), c.getPasoId(), c.getCreatedAt(), c.getUpdatedAt());
        var itemEntities = c.getItems().stream()
                .map(item -> new CrucetaItemEntity(item.getId(), c.getId(),
                        item.getMarchaId(), item.getRouteSectionId(),
                        item.getSequenceWithinSection(), item.getNotes()))
                .toList();
        entity.setItems(itemEntities);
        entity.setProgressions(c.getProgressions().stream()
                .map(CrucetaProgressionEntity::from)
                .toList());
        return entity;
    }

    private Cruceta toDomain(CrucetaEntity e) {
        var items = e.getItems().stream()
                .map(i -> CrucetaItem.reconstruct(i.getId(), i.getVersion(), i.getMarchaId(),
                        i.getRouteSectionId(), i.getSequenceWithinSection(), i.getNotes()))
                .toList();
        var progressions = e.getProgressions().stream()
                .map(CrucetaProgressionEntity::toDomain)
                .toList();
        return Cruceta.reconstruct(e.getId(), e.getVersion(), e.getPasoId(), items, progressions,
                e.getCreatedAt(), e.getUpdatedAt());
    }
}
