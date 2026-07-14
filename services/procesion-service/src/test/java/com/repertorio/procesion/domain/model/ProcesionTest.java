package com.repertorio.procesion.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ProcesionTest {

    // Helper to create a PLANNED procesion
    private Procesion aPlannedProcesion() {
        return Procesion.create(UUID.randomUUID(), LocalDate.of(2026, 4, 13), LocalTime.of(18, 0));
    }

    @Test
    void create_shouldSetStatusToPlanned() {
        Procesion procesion = aPlannedProcesion();
        assertEquals(ProcesionStatus.PLANNED, procesion.getStatus());
    }

    @Test
    void plannedToInProgress_shouldSucceed() {
        Procesion procesion = aPlannedProcesion();
        procesion.changeStatus(ProcesionStatus.IN_PROGRESS);
        assertEquals(ProcesionStatus.IN_PROGRESS, procesion.getStatus());
    }

    @Test
    void plannedToCancelled_shouldSucceed() {
        Procesion procesion = aPlannedProcesion();
        procesion.changeStatus(ProcesionStatus.CANCELLED);
        assertEquals(ProcesionStatus.CANCELLED, procesion.getStatus());
    }

    @Test
    void plannedToCompleted_shouldThrow() {
        Procesion procesion = aPlannedProcesion();
        assertThrows(IllegalArgumentException.class,
            () -> procesion.changeStatus(ProcesionStatus.COMPLETED));
        assertEquals(ProcesionStatus.PLANNED, procesion.getStatus()); // status unchanged
    }

    @Test
    void sameStatus_shouldBeNoOp() {
        Procesion procesion = aPlannedProcesion();
        procesion.changeStatus(ProcesionStatus.PLANNED);
        assertEquals(ProcesionStatus.PLANNED, procesion.getStatus());
    }

    @Test
    void inProgressToCompleted_shouldSucceed() {
        Procesion procesion = aPlannedProcesion();
        procesion.changeStatus(ProcesionStatus.IN_PROGRESS);
        procesion.changeStatus(ProcesionStatus.COMPLETED);
        assertEquals(ProcesionStatus.COMPLETED, procesion.getStatus());
    }

    @Test
    void inProgressToCancelled_shouldSucceed() {
        Procesion procesion = aPlannedProcesion();
        procesion.changeStatus(ProcesionStatus.IN_PROGRESS);
        procesion.changeStatus(ProcesionStatus.CANCELLED);
        assertEquals(ProcesionStatus.CANCELLED, procesion.getStatus());
    }

    @Test
    void inProgressToPlanned_shouldThrow() {
        Procesion procesion = aPlannedProcesion();
        procesion.changeStatus(ProcesionStatus.IN_PROGRESS);
        assertThrows(IllegalArgumentException.class,
            () -> procesion.changeStatus(ProcesionStatus.PLANNED));
        assertEquals(ProcesionStatus.IN_PROGRESS, procesion.getStatus()); // unchanged
    }

    @Test
    void completedToAny_shouldThrow() {
        Procesion procesion = aPlannedProcesion();
        procesion.changeStatus(ProcesionStatus.IN_PROGRESS);
        procesion.changeStatus(ProcesionStatus.COMPLETED);
        assertThrows(IllegalArgumentException.class,
            () -> procesion.changeStatus(ProcesionStatus.CANCELLED));
        assertThrows(IllegalArgumentException.class,
            () -> procesion.changeStatus(ProcesionStatus.PLANNED));
        assertThrows(IllegalArgumentException.class,
            () -> procesion.changeStatus(ProcesionStatus.IN_PROGRESS));
        assertEquals(ProcesionStatus.COMPLETED, procesion.getStatus()); // unchanged
    }

    @Test
    void cancelledToAny_shouldThrow() {
        Procesion procesion = aPlannedProcesion();
        procesion.changeStatus(ProcesionStatus.CANCELLED);
        assertThrows(IllegalArgumentException.class,
            () -> procesion.changeStatus(ProcesionStatus.PLANNED));
        assertThrows(IllegalArgumentException.class,
            () -> procesion.changeStatus(ProcesionStatus.IN_PROGRESS));
        assertThrows(IllegalArgumentException.class,
            () -> procesion.changeStatus(ProcesionStatus.COMPLETED));
        assertEquals(ProcesionStatus.CANCELLED, procesion.getStatus()); // unchanged
    }

    @Test
    void changeStatus_shouldUpdateUpdatedAt() {
        Procesion procesion = aPlannedProcesion();
        Instant before = procesion.getUpdatedAt();
        try { Thread.sleep(1); } catch (InterruptedException e) { throw new RuntimeException(e); }
        procesion.changeStatus(ProcesionStatus.IN_PROGRESS);
        assertTrue(procesion.getUpdatedAt().isAfter(before));
    }
}
