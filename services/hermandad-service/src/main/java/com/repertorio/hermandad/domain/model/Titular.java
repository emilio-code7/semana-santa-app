package com.repertorio.hermandad.domain.model;

import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
public class Titular {

    private UUID id;
    private String name;
    private String description;
    private UUID hermandadId;
    private Instant createdAt;
    private Instant updatedAt;

    protected Titular() {}

    public Titular(String name, String description, UUID hermandadId) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be null or blank");
        }
        if (hermandadId == null) {
            throw new IllegalArgumentException("hermandadId must not be null");
        }
        this.name = name;
        this.description = description;
        this.hermandadId = hermandadId;
    }

    // ponytail: private all-args constructor for adapter reconstruction
    private Titular(UUID id, String name, String description, UUID hermandadId,
                    Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.hermandadId = hermandadId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Titular reconstruct(UUID id, String name, String description, UUID hermandadId,
                                       Instant createdAt, Instant updatedAt) {
        return new Titular(id, name, description, hermandadId, createdAt, updatedAt);
    }

    public void update(String name, String description) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be null or blank");
        }
        this.name = name;
        this.description = description;
    }
}
