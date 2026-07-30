package com.repertorio.marcha.domain.model;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public class KnownProcesion {

    private final UUID procesionId;
    private final UUID hermandadId;
    private LocalDate date;
    private LocalTime time;
    private String status;
    private Instant planFinalizedAt;
    private Instant updatedAt;

    // ponytail: package-private for adapter reconstruction only
    KnownProcesion() {
        this.procesionId = null;
        this.hermandadId = null;
    }

    public KnownProcesion(UUID procesionId, UUID hermandadId, String status) {
        this(procesionId, hermandadId, null, null, status, null);
    }

    public KnownProcesion(UUID procesionId, UUID hermandadId, LocalDate date, LocalTime time,
                          String status, Instant planFinalizedAt) {
        this.procesionId = requireNonNull(procesionId, "procesionId");
        this.hermandadId = requireNonNull(hermandadId, "hermandadId");
        this.date = date;
        this.time = time;
        this.status = requireNonBlank(status, "status");
        this.planFinalizedAt = planFinalizedAt;
        this.updatedAt = Instant.now();
    }

    public void updateStatus(String newStatus) {
        this.status = requireNonBlank(newStatus, "status");
        this.updatedAt = Instant.now();
    }

    public void finalizePlan(LocalDate date, LocalTime time, Instant planFinalizedAt) {
        this.date = date;
        this.time = time;
        this.planFinalizedAt = planFinalizedAt;
        this.updatedAt = Instant.now();
    }

    private static String requireNonBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }

    private static <T> T requireNonNull(T value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " must not be null");
        }
        return value;
    }

    // ponytail: private all-args for adapter reconstruction
    private KnownProcesion(UUID procesionId, UUID hermandadId, LocalDate date, LocalTime time,
                           String status, Instant planFinalizedAt, Instant updatedAt) {
        this.procesionId = procesionId;
        this.hermandadId = hermandadId;
        this.date = date;
        this.time = time;
        this.status = status;
        this.planFinalizedAt = planFinalizedAt;
        this.updatedAt = updatedAt;
    }

    public static KnownProcesion reconstruct(UUID procesionId, UUID hermandadId,
                                              LocalDate date, LocalTime time,
                                              String status, Instant planFinalizedAt,
                                              Instant updatedAt) {
        return new KnownProcesion(procesionId, hermandadId, date, time, status, planFinalizedAt, updatedAt);
    }

    // Getters
    public UUID getProcesionId() { return procesionId; }
    public UUID getHermandadId() { return hermandadId; }
    public LocalDate getDate() { return date; }
    public LocalTime getTime() { return time; }
    public String getStatus() { return status; }
    public Instant getPlanFinalizedAt() { return planFinalizedAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
