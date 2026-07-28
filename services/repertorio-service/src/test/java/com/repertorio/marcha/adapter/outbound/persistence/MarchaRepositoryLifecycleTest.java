package com.repertorio.marcha.adapter.outbound.persistence;

import com.repertorio.marcha.domain.model.BandType;
import com.repertorio.marcha.domain.model.Marcha;
import com.repertorio.marcha.domain.model.VersionMismatchException;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * DataJpaTest for MarchaRepositoryAdapter lifecycle.
 * Uses H2 embedded database (auto-configured by @DataJpaTest).
 */
@DataJpaTest
@Import(MarchaRepositoryAdapter.class)
class MarchaRepositoryLifecycleTest {

    @Autowired
    private MarchaRepositoryAdapter adapter;

    @Autowired
    private MarchaJpaRepository jpaRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void saveNewMarchaPreservesDomainAssignedId() {
        var marcha = Marcha.create("Amarguras", "Manuel López Farfán",
                BandType.BANDA_PALIO, 420, 1919, null);
        var beforeId = marcha.getId();

        var saved = adapter.save(marcha);
        entityManager.flush();

        assertThat(saved.getId()).isEqualTo(beforeId);
    }

    @Test
    void saveNewMarchaReturnsAllFields() {
        var marcha = Marcha.create("Pasa la Macarena", "Pedro Morales",
                BandType.AGRUPACION_MUSICAL, 360, 1988, "https://youtube.com/test");

        var saved = adapter.save(marcha);
        entityManager.flush();

        assertThat(saved.getTitle()).isEqualTo("Pasa la Macarena");
        assertThat(saved.getComposer()).isEqualTo("Pedro Morales");
        assertThat(saved.getBandType()).isEqualTo(BandType.AGRUPACION_MUSICAL);
        assertThat(saved.getDurationSeconds()).isEqualTo(360);
        assertThat(saved.getCompositionYear()).isEqualTo(1988);
        assertThat(saved.getYoutubeUrl()).isEqualTo("https://youtube.com/test");
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void twoConsecutiveUpdatesPreserveVersion() {
        // Given: a saved marcha
        var marcha = Marcha.create("Original", "Composer A",
                BandType.BANDA_PALIO, 300, null, null);
        var saved = adapter.save(marcha);
        entityManager.flush();
        entityManager.clear();  // clear persistence context

        // When: first update
        var existing1 = adapter.findById(saved.getId()).orElseThrow();
        existing1.update("Updated A", "Composer B", BandType.AGRUPACION_MUSICAL, 400, 2000, "https://a.com");
        adapter.save(existing1);
        entityManager.flush();
        entityManager.clear();

        // When: second update — should NOT throw optimistic-lock exception
        var existing2 = adapter.findById(saved.getId()).orElseThrow();
        existing2.update("Updated B", "Composer C", BandType.BANDA_CORNETAS, 500, 2005, "https://b.com");
        var result = adapter.save(existing2);
        entityManager.flush();
        entityManager.clear();

        // Then: final state reflects last update
        var finalState = adapter.findById(saved.getId()).orElseThrow();
        assertThat(finalState.getTitle()).isEqualTo("Updated B");
        assertThat(finalState.getComposer()).isEqualTo("Composer C");
        assertThat(finalState.getBandType()).isEqualTo(BandType.BANDA_CORNETAS);
    }

    @Test
    void deleteRemovesMarcha() {
        var marcha = Marcha.create("To Delete", "Composer",
                BandType.BANDA_PALIO, 300, null, null);
        var saved = adapter.save(marcha);
        entityManager.flush();
        entityManager.clear();

        adapter.deleteById(saved.getId());
        entityManager.flush();
        entityManager.clear();  // clear persistence context to force DB read on findById

        assertThat(adapter.findById(saved.getId())).isEmpty();
    }

    @Test
    void updateModifiesReturnedUpdatedAt() {
        var marcha = Marcha.create("Timing Test", "Composer",
                BandType.BANDA_PALIO, 300, null, null);
        var saved = adapter.save(marcha);
        entityManager.flush();
        entityManager.clear();

        var previousUpdatedAt = adapter.findById(saved.getId()).orElseThrow().getUpdatedAt();

        // Small delay to ensure different timestamp
        var loaded = adapter.findById(saved.getId()).orElseThrow();
        loaded.update("Updated", "New Composer", BandType.AGRUPACION_MUSICAL, 400, 2000, null);
        var result = adapter.save(loaded);
        entityManager.flush();

        assertThat(result.getUpdatedAt()).isAfter(previousUpdatedAt);
    }

    @Test
    void saveFullyConstructedEntityPreservesCreatedAt() {
        var fixedInstant = Instant.parse("2024-01-15T10:00:00Z");
        var marcha = Marcha.reconstruct(UUID.randomUUID(), 0, "Preserved Created",
                "Composer", BandType.BANDA_PALIO, 300, null, null,
                fixedInstant, fixedInstant);

        var saved = adapter.save(marcha);
        entityManager.flush();

        assertThat(saved.getCreatedAt()).isEqualTo(fixedInstant);
    }

    @Test
    void newMarchaStartsWithVersionZero() {
        var marcha = Marcha.create("Version Test", "Composer", BandType.BANDA_PALIO, 300, null, null);
        assertThat(marcha.getVersion()).isZero();
    }

    @Test
    void saveAndRoundTripPreservesVersion() {
        var marcha = Marcha.create("Roundtrip", "Composer", BandType.BANDA_PALIO, 300, null, null);
        var saved = adapter.save(marcha);
        entityManager.flush();
        entityManager.clear();

        var reloaded = adapter.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getVersion()).isZero();

        reloaded.update("Updated", "Composer B", BandType.AGRUPACION_MUSICAL, 400, 2000, "https://a.com");
        adapter.save(reloaded);
        entityManager.flush();
        entityManager.clear();

        var updated = adapter.findById(saved.getId()).orElseThrow();
        assertThat(updated.getVersion()).isEqualTo(1);
    }

    @Test
    void staleMarchaWriteThrowsVersionMismatch() {
        // Given: saved marcha
        var marcha = Marcha.create("Original", "Composer", BandType.BANDA_PALIO, 300, null, null);
        var saved = adapter.save(marcha);
        entityManager.flush();
        entityManager.clear();

        // Reconstruct a stale domain object with version=0 (while DB has version 0 at this point)
        var loaded = adapter.findById(saved.getId()).orElseThrow();
        // Sneakily update DB through a direct JPA path to bump the version
        var managed = jpaRepository.findById(saved.getId()).orElseThrow();
        managed.setTitle("Sneaky update");
        jpaRepository.save(managed);
        jpaRepository.flush();
        entityManager.clear();
        // Now DB version is 1, but our domain object still has version 0

        // When: saving stale object → should throw
        assertThatThrownBy(() -> {
            adapter.save(loaded);
            entityManager.flush();
        }).isInstanceOf(VersionMismatchException.class);
    }

    @Test
    void concurrentUpdateDetected() {
        // Simulate two concurrent readers both loading the same entity
        var marcha = Marcha.create("Concurrent", "Composer", BandType.BANDA_PALIO, 300, null, null);
        var saved = adapter.save(marcha);
        entityManager.flush();
        entityManager.clear();

        // Reader 1 loads, updates, saves (succeeds)
        var reader1 = adapter.findById(saved.getId()).orElseThrow();
        reader1.update("Reader 1 Edit", "Composer1", BandType.AGRUPACION_MUSICAL, 400, 2000, null);
        adapter.save(reader1);
        entityManager.flush();
        entityManager.clear();

        // Reader 2 loads before Reader 1 saved (stale version 0), now tries to save
        // Reader 2's domain object has version 0, but DB has version 1
        var reader2 = adapter.findById(saved.getId()).orElseThrow();
        // reader2 has version 1 now (fresh load), so we need to simulate a stale one
        var stale = Marcha.reconstruct(saved.getId(), 0, "Reader 2 Edit", "Composer2",
                BandType.BANDA_CORNETAS, 500, 2005, null,
                reader2.getCreatedAt(), reader2.getUpdatedAt());

        assertThatThrownBy(() -> {
            adapter.save(stale);
            entityManager.flush();
        }).isInstanceOf(VersionMismatchException.class);
    }
}
