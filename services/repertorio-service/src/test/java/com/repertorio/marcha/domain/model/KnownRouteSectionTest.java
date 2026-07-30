package com.repertorio.marcha.domain.model;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class KnownRouteSectionTest {

    private final UUID id = UUID.randomUUID();
    private final UUID procesionId = UUID.randomUUID();

    @Test
    void createWithValidFields() {
        var section = new KnownRouteSection(id, procesionId, "Salida", 1, "Inicio del recorrido");
        assertEquals(id, section.getId());
        assertEquals(procesionId, section.getProcesionId());
        assertEquals("Salida", section.getName());
        assertEquals(1, section.getPosition());
        assertEquals("Inicio del recorrido", section.getNotes());
    }

    @Test
    void createWithNullNotes() {
        var section = new KnownRouteSection(id, procesionId, "Salida", 1, null);
        assertNull(section.getNotes());
    }

    @Test
    void rejectNullId() {
        assertThrows(NullPointerException.class,
                () -> new KnownRouteSection(null, procesionId, "Salida", 1, null));
    }

    @Test
    void rejectNullProcesionId() {
        assertThrows(NullPointerException.class,
                () -> new KnownRouteSection(id, null, "Salida", 1, null));
    }

    @Test
    void rejectNullName() {
        assertThrows(NullPointerException.class,
                () -> new KnownRouteSection(id, procesionId, null, 1, null));
    }

    @Test
    void reconstructRestoresAllFields() {
        var section = KnownRouteSection.reconstruct(id, procesionId, "Recogida", 2, "Fin del recorrido");
        assertEquals(id, section.getId());
        assertEquals(procesionId, section.getProcesionId());
        assertEquals("Recogida", section.getName());
        assertEquals(2, section.getPosition());
        assertEquals("Fin del recorrido", section.getNotes());
    }
}
