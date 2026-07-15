package com.repertorio.marcha.domain.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import java.util.UUID;

class KnownProcesionTest {

    private final UUID procesionId = UUID.randomUUID();
    private final UUID hermandadId = UUID.randomUUID();

    @Test
    void createWithValidFields() {
        var kp = new KnownProcesion(procesionId, hermandadId, "PLANNED");
        assertEquals(procesionId, kp.getProcesionId());
        assertEquals(hermandadId, kp.getHermandadId());
        assertEquals("PLANNED", kp.getStatus());
        assertNotNull(kp.getUpdatedAt());
    }

    @Test
    void rejectNullProcesionId() {
        assertThrows(IllegalArgumentException.class,
                () -> new KnownProcesion(null, hermandadId, "PLANNED"));
    }

    @Test
    void rejectNullHermandadId() {
        assertThrows(IllegalArgumentException.class,
                () -> new KnownProcesion(procesionId, null, "PLANNED"));
    }

    @Test
    void rejectNullStatus() {
        assertThrows(IllegalArgumentException.class,
                () -> new KnownProcesion(procesionId, hermandadId, null));
    }

    @Test
    void rejectBlankStatus() {
        assertThrows(IllegalArgumentException.class,
                () -> new KnownProcesion(procesionId, hermandadId, ""));
        assertThrows(IllegalArgumentException.class,
                () -> new KnownProcesion(procesionId, hermandadId, "   "));
    }

    @Test
    void updateStatusChangesStatusAndTicksTimestamp() throws InterruptedException {
        var kp = new KnownProcesion(procesionId, hermandadId, "PLANNED");
        var beforeUpdate = kp.getUpdatedAt();

        Thread.sleep(1);
        kp.updateStatus("IN_PROGRESS");

        assertEquals("IN_PROGRESS", kp.getStatus());
        assertTrue(kp.getUpdatedAt().isAfter(beforeUpdate));
    }

    @Test
    void updateStatusRejectsBlank() {
        var kp = new KnownProcesion(procesionId, hermandadId, "PLANNED");
        assertThrows(IllegalArgumentException.class, () -> kp.updateStatus(""));
        assertThrows(IllegalArgumentException.class, () -> kp.updateStatus("   "));
        assertThrows(IllegalArgumentException.class, () -> kp.updateStatus(null));
    }

    @Test
    void reconstructRestoresAllFields() {
        var now = java.time.Instant.now();
        var kp = KnownProcesion.reconstruct(procesionId, hermandadId, "CANCELLED", now);
        assertEquals(procesionId, kp.getProcesionId());
        assertEquals(hermandadId, kp.getHermandadId());
        assertEquals("CANCELLED", kp.getStatus());
        assertEquals(now, kp.getUpdatedAt());
    }
}
