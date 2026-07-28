package com.repertorio.marcha.adapter.outbound.persistence;

import com.repertorio.marcha.domain.model.Cruceta;
import com.repertorio.marcha.domain.model.CrucetaItem;
import com.repertorio.marcha.domain.model.VersionMismatchException;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * DataJpaTest for CrucetaRepositoryAdapter lifecycle.
 * Uses H2 embedded database (auto-configured by @DataJpaTest).
 */
@DataJpaTest
@Import(CrucetaRepositoryAdapter.class)
class CrucetaRepositoryLifecycleTest {

    @Autowired
    private CrucetaRepositoryAdapter adapter;

    @Autowired
    private CrucetaJpaRepository jpaRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void saveNewCrucetaPreservesIdsOfCrucetaAndItems() {
        var now = Instant.now();
        var item1 = new CrucetaItem(UUID.randomUUID(), 1, "Opening");
        var item2 = new CrucetaItem(UUID.randomUUID(), 2, "Closing");
        var beforeCrucetaId = UUID.randomUUID();  // simulate domain-assigned ID
        var cruceta = Cruceta.reconstruct(beforeCrucetaId, 0, UUID.randomUUID(),
                List.of(item1, item2), now, now);

        var saved = adapter.save(cruceta);
        entityManager.flush();

        assertThat(saved.getId()).isEqualTo(beforeCrucetaId);
        assertThat(saved.getItems()).hasSize(2);
        assertThat(saved.getItems().get(0).getId()).isEqualTo(item1.getId());
        assertThat(saved.getItems().get(1).getId()).isEqualTo(item2.getId());
    }

    @Test
    void redefineCrucetaReplacesItems() {
        var now = Instant.now();
        var procesionId = UUID.randomUUID();
        var crucetaId = UUID.randomUUID();
        var oldItems = List.of(
                new CrucetaItem(UUID.randomUUID(), 1, "Old Item")
        );
        var cruceta = Cruceta.reconstruct(crucetaId, 0, procesionId, oldItems, now, now);

        // Save initial version
        var saved = adapter.save(cruceta);
        entityManager.flush();
        entityManager.clear();

        // Replace with new items
        var newItem1 = new CrucetaItem(UUID.randomUUID(), 1, "New Item 1");
        var newItem2 = new CrucetaItem(UUID.randomUUID(), 2, "New Item 2");
        var reloaded = adapter.findByProcesionId(procesionId).orElseThrow();
        reloaded.redefine(List.of(newItem1, newItem2));

        var replaced = adapter.save(reloaded);
        entityManager.flush();
        entityManager.clear();

        // Then: cruceta has new items
        var result = adapter.findByProcesionId(procesionId).orElseThrow();
        assertThat(result.getId()).isEqualTo(crucetaId);
        assertThat(result.getItems()).hasSize(2);
        assertThat(result.getItems().get(0).getNotes()).isEqualTo("New Item 1");
        // Old item should not exist
        assertThat(result.getItems().stream().noneMatch(i -> i.getNotes().equals("Old Item"))).isTrue();
    }

    @Test
    void redefineInPlacePreservesAggregateId() {
        var procesionId = UUID.randomUUID();
        var item = new CrucetaItem(UUID.randomUUID(), 1, "First");
        var cruceta = new Cruceta(procesionId, List.of(item));
        var originalId = cruceta.getId();

        // Save initial
        var saved = adapter.save(cruceta);
        entityManager.flush();
        entityManager.clear();

        // Replace via redefine (simulating defineCruceta service pattern)
        var reloaded = adapter.findByProcesionId(procesionId).orElseThrow();
        var newItem = new CrucetaItem(UUID.randomUUID(), 1, "Second");
        reloaded.redefine(List.of(newItem));
        adapter.save(reloaded);
        entityManager.flush();
        entityManager.clear();

        var result = adapter.findByProcesionId(procesionId).orElseThrow();
        assertThat(result.getId()).isEqualTo(originalId);
        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().get(0).getNotes()).isEqualTo("Second");
    }

    @Test
    void newCrucetaStartsWithVersionZero() {
        var item = new CrucetaItem(UUID.randomUUID(), 1, "Item");
        var cruceta = new Cruceta(UUID.randomUUID(), List.of(item));
        assertThat(cruceta.getVersion()).isZero();
    }

    @Test
    void saveCrucetaRoundTripPreservesVersion() {
        var now = Instant.now();
        var item1 = new CrucetaItem(UUID.randomUUID(), 1, "Opening");
        var item2 = new CrucetaItem(UUID.randomUUID(), 2, "Closing");
        var cruceta = Cruceta.reconstruct(UUID.randomUUID(), 0, UUID.randomUUID(),
                List.of(item1, item2), now, now);
        var saved = adapter.save(cruceta);
        entityManager.flush();
        entityManager.clear();

        var reloaded = adapter.findByProcesionId(saved.getProcesionId()).orElseThrow();
        assertThat(reloaded.getVersion()).isZero();

        // Update: redefine with new items
        var newItem = new CrucetaItem(UUID.randomUUID(), 1, "Replaced");
        reloaded.redefine(List.of(newItem));
        adapter.save(reloaded);
        entityManager.flush();
        entityManager.clear();

        var updated = adapter.findByProcesionId(saved.getProcesionId()).orElseThrow();
        assertThat(updated.getVersion()).isEqualTo(1);
    }

    @Test
    void redefineCrucetaReturnsDomainAssignedItemIds() {
        var procesionId = UUID.randomUUID();
        var item1 = new CrucetaItem(UUID.randomUUID(), 1, "First");
        var item2 = new CrucetaItem(UUID.randomUUID(), 2, "Second");
        var item1Id = item1.getId();
        var item2Id = item2.getId();
        var cruceta = new Cruceta(procesionId, List.of(item1, item2));

        var saved = adapter.save(cruceta);
        entityManager.flush();
        entityManager.clear();

        // Reload, redefine with partially overlapping items
        var reloaded = adapter.findByProcesionId(procesionId).orElseThrow();
        var newItem = new CrucetaItem(UUID.randomUUID(), 1, "Replacement");
        var newItemId = newItem.getId();
        reloaded.redefine(List.of(newItem));
        adapter.save(reloaded);
        entityManager.flush();
        entityManager.clear();

        var result = adapter.findByProcesionId(procesionId).orElseThrow();
        assertThat(result.getItems()).hasSize(1);
        // The surviving item ID should match the domain-assigned value
        assertThat(result.getItems().get(0).getId()).isEqualTo(newItemId);
        assertThat(result.getItems().get(0).getNotes()).isEqualTo("Replacement");
    }

    @Test
    void consecutiveReplacementsPreserveAggregateWithoutStaleChildren() {
        var procesionId = UUID.randomUUID();
        var crucetaId = UUID.randomUUID();
        var now = Instant.now();

        // First: create new cruceta with item A (version 0)
        var itemA = new CrucetaItem(UUID.randomUUID(), 1, "A");
        var cruceta = Cruceta.reconstruct(crucetaId, 0, procesionId, List.of(itemA), now, now);
        var saved = adapter.save(cruceta);
        entityManager.flush();
        // PC NOT cleared — retained between consecutive saves

        // Second: replace item A with item B (first replacement via save on existing aggregate)
        var itemB = new CrucetaItem(UUID.randomUUID(), 1, "B");
        saved.redefine(List.of(itemB));
        var afterFirst = adapter.save(saved);
        entityManager.flush();
        // PC still NOT cleared — deleted children may persist in context

        // Third: replace item B with item C (second replacement on same saturated PC)
        var itemC = new CrucetaItem(UUID.randomUUID(), 1, "C");
        afterFirst.redefine(List.of(itemC));
        var afterSecond = adapter.save(afterFirst);
        entityManager.flush();

        // Verify root aggregate preserved, old items gone, new items present
        assertThat(afterSecond.getId()).isEqualTo(crucetaId);
        assertThat(afterSecond.getItems()).hasSize(1);
        assertThat(afterSecond.getItems().get(0).getNotes()).isEqualTo("C");

        // Verify persistence: fresh load from DB
        entityManager.clear();
        var finalLoad = adapter.findByProcesionId(procesionId).orElseThrow();
        assertThat(finalLoad.getId()).isEqualTo(crucetaId);
        assertThat(finalLoad.getItems()).hasSize(1);
        assertThat(finalLoad.getItems().get(0).getNotes()).isEqualTo("C");
    }

    @Test
    void staleCrucetaWriteThrowsVersionMismatch() {
        var now = Instant.now();
        var procesionId = UUID.randomUUID();
        var crucetaId = UUID.randomUUID();
        var item = new CrucetaItem(UUID.randomUUID(), 1, "Original");
        var cruceta = Cruceta.reconstruct(crucetaId, 0, procesionId, List.of(item), now, now);
        var saved = adapter.save(cruceta);
        entityManager.flush();
        entityManager.clear();

        // Load domain object (version 0)
        var loaded = adapter.findByProcesionId(procesionId).orElseThrow();
        // Sneakily update DB to bump version
        var managed = entityManager.find(CrucetaEntity.class, crucetaId);
        managed.setUpdatedAt(Instant.now());
        entityManager.merge(managed);
        entityManager.flush();
        entityManager.clear();
        // DB version is now 1, domain version is still 0

        assertThatThrownBy(() -> {
            adapter.save(loaded);
            entityManager.flush();
        }).isInstanceOf(VersionMismatchException.class);
    }
}
