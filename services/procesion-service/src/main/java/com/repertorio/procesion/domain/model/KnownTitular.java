package com.repertorio.procesion.domain.model;

import java.time.Instant;
import java.util.UUID;

public class KnownTitular {

    private final UUID id;
    private final UUID hermandadId;
    private String name;
    private Instant updatedAt;

    // ponytail: package-private for adapter reconstruction only
    KnownTitular() {
        this.id = null;
        this.hermandadId = null;
    }

    public KnownTitular(UUID id, UUID hermandadId, String name) {
        this.id = requireNonNull(id, "id");
        this.hermandadId = requireNonNull(hermandadId, "hermandadId");
        this.name = requireNonBlank(name, "name");
        this.updatedAt = Instant.now();
    }

    public void updateName(String name) {
        this.name = requireNonBlank(name, "name");
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
    private KnownTitular(UUID id, UUID hermandadId, String name, Instant updatedAt) {
        this.id = id;
        this.hermandadId = hermandadId;
        this.name = name;
        this.updatedAt = updatedAt;
    }

    public static KnownTitular reconstruct(UUID id, UUID hermandadId, String name, Instant updatedAt) {
        return new KnownTitular(id, hermandadId, name, updatedAt);
    }

    // Getters
    public UUID getId() { return id; }
    public UUID getHermandadId() { return hermandadId; }
    public String getName() { return name; }
    public Instant getUpdatedAt() { return updatedAt; }
}
