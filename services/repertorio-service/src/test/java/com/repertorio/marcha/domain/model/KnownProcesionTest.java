package com.repertorio.marcha.domain.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
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
        assertNull(kp.getDate());
        assertNull(kp.getTime());
        assertNull(kp.getPlanFinalizedAt());
        assertNotNull(kp.getUpdatedAt());
    }

    @Test
    void createWithFullPlanFields() {
        var date = LocalDate.of(2026, 4, 13);
        var time = LocalTime.of(18, 0);
        var finalizedAt = Instant.now();
        var kp = new KnownProcesion(procesionId, hermandadId, date, time, "PLANNED", finalizedAt);
        assertEquals(procesionId, kp.getProcesionId());
        assertEquals(hermandadId, kp.getHermandadId());
        assertEquals(date, kp.getDate());
        assertEquals(time, kp.getTime());
        assertEquals("PLANNED", kp.getStatus());
        assertEquals(finalizedAt, kp.getPlanFinalizedAt());
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
    void finalizePlanSetsDateTimeAndPlanFinalizedAt() {
        var kp = new KnownProcesion(procesionId, hermandadId, "PLANNED");
        var date = LocalDate.of(2026, 4, 13);
        var time = LocalTime.of(18, 0);
        var finalizedAt = Instant.now();

        kp.finalizePlan(date, time, finalizedAt);

        assertEquals(date, kp.getDate());
        assertEquals(time, kp.getTime());
        assertEquals(finalizedAt, kp.getPlanFinalizedAt());
    }

    @Test
    void finalizePlanUpdatesTimestamp() throws InterruptedException {
        var kp = new KnownProcesion(procesionId, hermandadId, "PLANNED");
        var before = kp.getUpdatedAt();

        Thread.sleep(1);
        kp.finalizePlan(LocalDate.now(), LocalTime.now(), Instant.now());

        assertTrue(kp.getUpdatedAt().isAfter(before));
    }

    @Test
    void reconstructRestoresAllFields() {
        var now = Instant.now();
        var date = LocalDate.of(2026, 4, 13);
        var time = LocalTime.of(18, 0);
        var kp = KnownProcesion.reconstruct(procesionId, hermandadId, date, time, "CANCELLED", now, now);
        assertEquals(procesionId, kp.getProcesionId());
        assertEquals(hermandadId, kp.getHermandadId());
        assertEquals(date, kp.getDate());
        assertEquals(time, kp.getTime());
        assertEquals("CANCELLED", kp.getStatus());
        assertEquals(now, kp.getPlanFinalizedAt());
        assertEquals(now, kp.getUpdatedAt());
    }
}
