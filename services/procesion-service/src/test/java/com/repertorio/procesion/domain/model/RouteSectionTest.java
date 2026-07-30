package com.repertorio.procesion.domain.model;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RouteSectionTest {

    private final UUID procesionId = UUID.randomUUID();

    @Test
    void createShouldSetFields() {
        var section = RouteSection.create(procesionId, "Calle Sierpes", 0, null);
        assertNull(section.getId());
        assertEquals(procesionId, section.getProcesionId());
        assertEquals("Calle Sierpes", section.getName());
        assertEquals(0, section.getPosition());
        assertNull(section.getNotes());
        assertNotNull(section.getCreatedAt());
        assertNotNull(section.getUpdatedAt());
    }

    @Test
    void createWithNotes() {
        var section = RouteSection.create(procesionId, "Plaza", 1, "Outbound");
        assertEquals("Outbound", section.getNotes());
    }

    @Test
    void namesMayRepeat() {
        var s1 = RouteSection.create(procesionId, "Calle Sierpes", 0, null);
        var s2 = RouteSection.create(procesionId, "Calle Sierpes", 1, null);
        assertEquals(s1.getName(), s2.getName());
        assertNotEquals(s1.getPosition(), s2.getPosition());
    }

    @Test
    void updateShouldMutateFields() {
        var section = RouteSection.create(procesionId, "Old", 0, "old notes");
        section.update("New", 1, "new notes");
        assertEquals("New", section.getName());
        assertEquals(1, section.getPosition());
        assertEquals("new notes", section.getNotes());
        assertTrue(section.getUpdatedAt().isAfter(section.getCreatedAt()));
    }

    @Test
    void reconstructRestoresAllFields() {
        var id = UUID.randomUUID();
        var now = java.time.Instant.now();
        var section = RouteSection.reconstruct(id, procesionId, "Name", 0, "notes", now, now);
        assertEquals(id, section.getId());
        assertEquals(procesionId, section.getProcesionId());
        assertEquals("Name", section.getName());
        assertEquals(0, section.getPosition());
        assertEquals("notes", section.getNotes());
        assertEquals(now, section.getCreatedAt());
    }
}
