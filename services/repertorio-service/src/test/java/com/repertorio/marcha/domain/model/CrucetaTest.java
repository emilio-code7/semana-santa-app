package com.repertorio.marcha.domain.model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CrucetaTest {

    private UUID rs() { return UUID.randomUUID(); }

    @Test
    void createWithValidItems() {
        var procesionId = UUID.randomUUID();
        var items = List.of(
                new CrucetaItem(UUID.randomUUID(), rs(), 0, null),
                new CrucetaItem(UUID.randomUUID(), rs(), 1, "second marcha")
        );
        var cruceta = new Cruceta(procesionId, items);

        assertNotNull(cruceta.getId());
        assertEquals(procesionId, cruceta.getProcesionId());
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
    void createWithDuplicateOrderIndex() {
        var marchaId = UUID.randomUUID();
        var sectionId = rs();
        var items = List.of(
                new CrucetaItem(marchaId, sectionId, 0, null),
                new CrucetaItem(UUID.randomUUID(), sectionId, 0, "duplicate index")
        );
        assertThrows(IllegalArgumentException.class, () ->
                new Cruceta(UUID.randomUUID(), items));
    }

    @Test
    void redefineReplacesItems() {
        var procesionId = UUID.randomUUID();
        var initialItems = List.of(
                new CrucetaItem(UUID.randomUUID(), rs(), 0, null)
        );
        var cruceta = new Cruceta(procesionId, initialItems);

        var newItems = List.of(
                new CrucetaItem(UUID.randomUUID(), rs(), 0, "replaced"),
                new CrucetaItem(UUID.randomUUID(), rs(), 1, "new item")
        );
        cruceta.redefine(newItems);

        assertEquals(2, cruceta.getItems().size());
        assertEquals("replaced", cruceta.getItems().get(0).getNotes());
    }

    @Test
    void redefineWithDuplicateOrderIndexThrows() {
        var cruceta = new Cruceta(UUID.randomUUID(), List.of(
                new CrucetaItem(UUID.randomUUID(), rs(), 0, null)
        ));
        assertThrows(IllegalArgumentException.class, () ->
                cruceta.redefine(List.of(
                        new CrucetaItem(UUID.randomUUID(), rs(), 0, "a"),
                        new CrucetaItem(UUID.randomUUID(), rs(), 0, "b")
                )));
    }

    @Test
    void containsMarchaReturnsTrue() {
        var marchaId = UUID.randomUUID();
        var items = List.of(
                new CrucetaItem(marchaId, rs(), 0, null),
                new CrucetaItem(UUID.randomUUID(), rs(), 1, null)
        );
        var cruceta = new Cruceta(UUID.randomUUID(), items);

        assertTrue(cruceta.containsMarcha(marchaId));
    }

    @Test
    void containsMarchaReturnsFalse() {
        var items = List.of(
                new CrucetaItem(UUID.randomUUID(), rs(), 0, null)
        );
        var cruceta = new Cruceta(UUID.randomUUID(), items);

        assertFalse(cruceta.containsMarcha(UUID.randomUUID()));
    }

    @Test
    void getItemsReturnsImmutableCopy() {
        var items = List.of(
                new CrucetaItem(UUID.randomUUID(), rs(), 0, null)
        );
        var cruceta = new Cruceta(UUID.randomUUID(), items);

        assertThrows(UnsupportedOperationException.class, () ->
                cruceta.getItems().add(new CrucetaItem(UUID.randomUUID(), rs(), 1, null)));
    }

    @Test
    void redefineTicksUpdatedAt() throws InterruptedException {
        var cruceta = new Cruceta(UUID.randomUUID(), List.of(
                new CrucetaItem(UUID.randomUUID(), rs(), 0, null)
        ));
        var beforeUpdate = cruceta.getUpdatedAt();
        Thread.sleep(1);
        cruceta.redefine(List.of(
                new CrucetaItem(UUID.randomUUID(), rs(), 0, "updated")
        ));
        assertTrue(cruceta.getUpdatedAt().isAfter(beforeUpdate));
    }

    @Test
    void redefineClearsProgressions() {
        var cruceta = new Cruceta(UUID.randomUUID(), List.of(
                new CrucetaItem(UUID.randomUUID(), rs(), 0, null)
        ));
        var p = new CrucetaProgression(cruceta.getId(), UUID.randomUUID(), rs());
        cruceta.setProgressions(List.of(p));
        assertEquals(1, cruceta.getProgressions().size());

        cruceta.redefine(List.of(
                new CrucetaItem(UUID.randomUUID(), rs(), 0, "new")
        ));
        assertTrue(cruceta.getProgressions().isEmpty());
    }
}
