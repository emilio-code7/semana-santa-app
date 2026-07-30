package com.repertorio.procesion.domain.model;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PasoTest {

    private final UUID procesionId = UUID.randomUUID();
    private final UUID titularId = UUID.randomUUID();

    @Test
    void createShouldSetFields() {
        var paso = Paso.create(procesionId, 0, titularId, null);
        assertNull(paso.getId());
        assertEquals(procesionId, paso.getProcesionId());
        assertEquals(0, paso.getPosition());
        assertEquals(titularId, paso.getTitularId());
        assertNull(paso.getNotes());
        assertNotNull(paso.getCreatedAt());
    }

    @Test
    void createWithNotes() {
        var paso = Paso.create(procesionId, 1, titularId, "Primer paso");
        assertEquals("Primer paso", paso.getNotes());
    }

    @Test
    void updateShouldMutateFields() {
        var paso = Paso.create(procesionId, 0, titularId, "old");
        paso.update(1, UUID.randomUUID(), "new");
        assertEquals(1, paso.getPosition());
        assertEquals("new", paso.getNotes());
        assertTrue(paso.getUpdatedAt().isAfter(paso.getCreatedAt()));
    }

    @Test
    void reconstructRestoresAllFields() {
        var id = UUID.randomUUID();
        var now = java.time.Instant.now();
        var paso = Paso.reconstruct(id, procesionId, 0, titularId, "notes", now, now);
        assertEquals(id, paso.getId());
        assertEquals(procesionId, paso.getProcesionId());
        assertEquals(0, paso.getPosition());
        assertEquals(titularId, paso.getTitularId());
        assertEquals("notes", paso.getNotes());
    }
}
