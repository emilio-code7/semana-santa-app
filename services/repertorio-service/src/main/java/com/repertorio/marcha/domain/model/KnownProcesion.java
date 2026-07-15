package com.repertorio.marcha.domain.model;

import java.time.Instant;
import java.util.UUID;

public class KnownProcesion {

    private final UUID procesionId;
    private final UUID hermandadId;
    private String status;
    private Instant updatedAt;

    // ponytail: package-private for adapter reconstruction only
    KnownProcesion() {
        this.procesionId = null;
        this.hermandadId = null;
    }

    public KnownProcesion(UUID procesionId, UUID hermandadId, String status) {
        this.procesionId = requireNonNull(procesionId, "procesionId");
        this.hermandadId = requireNonNull(hermandadId, "hermandadId");
        this.status = requireNonBlank(status, "status");
        this.updatedAt = Instant.now();
    }

    public void updateStatus(String newStatus) {
        this.status = requireNonBlank(newStatus, "status");
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
    private KnownProcesion(UUID procesionId, UUID hermandadId, String status, Instant updatedAt) {
        this.procesionId = procesionId;
        this.hermandadId = hermandadId;
        this.status = status;
        this.updatedAt = updatedAt;
    }

    public static KnownProcesion reconstruct(UUID procesionId, UUID hermandadId,
                                              String status, Instant updatedAt) {
        return new KnownProcesion(procesionId, hermandadId, status, updatedAt);
    }

    // Getters
    public UUID getProcesionId() { return procesionId; }
    public UUID getHermandadId() { return hermandadId; }
    public String getStatus() { return status; }
    public Instant getUpdatedAt() { return updatedAt; }
}
