package com.repertorio.procesion.domain.model;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public class Procesion {

    private UUID id;
    private UUID hermandadId;
    private LocalDate date;
    private LocalTime time;
    private ProcesionStatus status = ProcesionStatus.PLANNED;
    private Instant planFinalizedAt;
    private Instant createdAt;
    private Instant updatedAt;

    protected Procesion() {}

    private Procesion(UUID id, UUID hermandadId, LocalDate date, LocalTime time,
                       ProcesionStatus status, Instant planFinalizedAt, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.hermandadId = hermandadId;
        this.date = date;
        this.time = time;
        this.status = status;
        this.planFinalizedAt = planFinalizedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Procesion create(UUID hermandadId, LocalDate date, LocalTime time) {
        var now = Instant.now();
        return new Procesion(
                null,
                hermandadId,
                date,
                time,
                ProcesionStatus.PLANNED,
                null,
                now,
                now
        );
    }

    public void changeStatus(ProcesionStatus newStatus) {
        if (newStatus == this.status) return;

        switch (this.status) {
            case PLANNED -> {
                if (newStatus != ProcesionStatus.IN_PROGRESS && newStatus != ProcesionStatus.CANCELLED) {
                    throw new IllegalArgumentException(
                            "Cannot transition from PLANNED to " + newStatus);
                }
            }
            case IN_PROGRESS -> {
                if (newStatus != ProcesionStatus.COMPLETED && newStatus != ProcesionStatus.CANCELLED) {
                    throw new IllegalArgumentException(
                            "Cannot transition from IN_PROGRESS to " + newStatus);
                }
            }
            case COMPLETED, CANCELLED -> throw new IllegalArgumentException(
                    "Cannot transition from terminal status " + this.status);
        }

        this.status = newStatus;
        this.updatedAt = Instant.now();
    }

    /**
     * Idempotently marks the plan as finalized. Returns {@code true} if newly finalized,
     * {@code false} if already finalized.
     */
    public boolean finalizePlan() {
        if (planFinalizedAt != null) return false;
        this.planFinalizedAt = Instant.now();
        this.updatedAt = Instant.now();
        return true;
    }

    public boolean isPlanFinalized() {
        return planFinalizedAt != null;
    }

    public static Procesion reconstruct(UUID id, UUID hermandadId, LocalDate date, LocalTime time,
                                         ProcesionStatus status, Instant planFinalizedAt, Instant createdAt, Instant updatedAt) {
        return new Procesion(id, hermandadId, date, time, status, planFinalizedAt, createdAt, updatedAt);
    }

    public UUID getId() { return id; }
    public UUID getHermandadId() { return hermandadId; }
    public LocalDate getDate() { return date; }
    public LocalTime getTime() { return time; }
    public ProcesionStatus getStatus() { return status; }
    public Instant getPlanFinalizedAt() { return planFinalizedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
