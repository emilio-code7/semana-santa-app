package com.repertorio.marcha.domain.model;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class KnownPasoTest {

    private final UUID id = UUID.randomUUID();
    private final UUID procesionId = UUID.randomUUID();
    private final UUID titularId = UUID.randomUUID();

    @Test
    void createWithValidFields() {
        var paso = new KnownPaso(id, procesionId, 1, titularId);
        assertEquals(id, paso.getId());
        assertEquals(procesionId, paso.getProcesionId());
        assertEquals(1, paso.getPosition());
        assertEquals(titularId, paso.getTitularId());
    }

    @Test
    void rejectNullId() {
        assertThrows(NullPointerException.class,
                () -> new KnownPaso(null, procesionId, 1, titularId));
    }

    @Test
    void rejectNullProcesionId() {
        assertThrows(NullPointerException.class,
                () -> new KnownPaso(id, null, 1, titularId));
    }

    @Test
    void rejectNullTitularId() {
        assertThrows(NullPointerException.class,
                () -> new KnownPaso(id, procesionId, 1, null));
    }

    @Test
    void reconstructRestoresAllFields() {
        var paso = KnownPaso.reconstruct(id, procesionId, 3, titularId);
        assertEquals(id, paso.getId());
        assertEquals(procesionId, paso.getProcesionId());
        assertEquals(3, paso.getPosition());
        assertEquals(titularId, paso.getTitularId());
    }

    @Test
    void positionCanBeZero() {
        var paso = new KnownPaso(id, procesionId, 0, titularId);
        assertEquals(0, paso.getPosition());
    }
}
