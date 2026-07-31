package com.repertorio.marcha.domain.model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CrucetaTest {

    @Test
    void createWithValidItems() {
        var pasoId = UUID.randomUUID();
        var routeSectionId = UUID.randomUUID();
        var items = List.of(
                new CrucetaItem(UUID.randomUUID(), routeSectionId, 0, null),
                new CrucetaItem(UUID.randomUUID(), routeSectionId, 1, "second marcha")
        );
        var cruceta = new Cruceta(pasoId, items);

        assertNotNull(cruceta.getId());
        assertEquals(pasoId, cruceta.getPasoId());
        assertEquals(2, cruceta.getItems().size());
        assertNotNull(cruceta.getCreatedAt());
        assertNotNull(cruceta.getUpdatedAt());
    }

    @Test
    void createWithNullItems() {
        assertThrows(IllegalArgumentException.class, () ->
                new Cruceta(UUID.randomUUID(), null));
    }

    @Test
    void redefineReplacesItems() {
        var pasoId = UUID.randomUUID();
        var routeSectionId = UUID.randomUUID();
        var initialItems = List.of(
                new CrucetaItem(UUID.randomUUID(), routeSectionId, 0, null)
        );
        var cruceta = new Cruceta(pasoId, initialItems);

        var newItems = List.of(
                new CrucetaItem(UUID.randomUUID(), routeSectionId, 0, "replaced"),
                new CrucetaItem(UUID.randomUUID(), routeSectionId, 1, "new item")
        );
        cruceta.redefine(newItems);

        assertEquals(2, cruceta.getItems().size());
        assertEquals("replaced", cruceta.getItems().get(0).getNotes());
    }

    @Test
    void containsMarchaReturnsTrue() {
        var marchaId = UUID.randomUUID();
        var routeSectionId = UUID.randomUUID();
        var items = List.of(
                new CrucetaItem(marchaId, routeSectionId, 0, null),
                new CrucetaItem(UUID.randomUUID(), routeSectionId, 1, null)
        );
        var cruceta = new Cruceta(UUID.randomUUID(), items);

        assertTrue(cruceta.containsMarcha(marchaId));
    }

    @Test
    void containsMarchaReturnsFalse() {
        var items = List.of(
                new CrucetaItem(UUID.randomUUID(), UUID.randomUUID(), 0, null)
        );
        var cruceta = new Cruceta(UUID.randomUUID(), items);

        assertFalse(cruceta.containsMarcha(UUID.randomUUID()));
    }

    @Test
    void getItemsReturnsImmutableCopy() {
        var items = List.of(
                new CrucetaItem(UUID.randomUUID(), UUID.randomUUID(), 0, null)
        );
        var cruceta = new Cruceta(UUID.randomUUID(), items);

        assertThrows(UnsupportedOperationException.class, () ->
                cruceta.getItems().add(new CrucetaItem(UUID.randomUUID(), UUID.randomUUID(), 1, null)));
    }

    @Test
    void redefineTicksUpdatedAt() throws InterruptedException {
        var cruceta = new Cruceta(UUID.randomUUID(), List.of(
                new CrucetaItem(UUID.randomUUID(), UUID.randomUUID(), 0, null)
        ));
        var beforeUpdate = cruceta.getUpdatedAt();
        Thread.sleep(1);
        cruceta.redefine(List.of(
                new CrucetaItem(UUID.randomUUID(), UUID.randomUUID(), 0, "updated")
        ));
        assertTrue(cruceta.getUpdatedAt().isAfter(beforeUpdate));
    }

    @Test
    void redefineClearsProgressions() {
        var cruceta = new Cruceta(UUID.randomUUID(), List.of(
                new CrucetaItem(UUID.randomUUID(), UUID.randomUUID(), 0, null)
        ));
        var p = new CrucetaProgression(cruceta.getId(), UUID.randomUUID(), UUID.randomUUID());
        cruceta.setProgressions(List.of(p));
        assertEquals(1, cruceta.getProgressions().size());

        cruceta.redefine(List.of(
                new CrucetaItem(UUID.randomUUID(), UUID.randomUUID(), 0, "new")
        ));
        assertTrue(cruceta.getProgressions().isEmpty());
    }
}
