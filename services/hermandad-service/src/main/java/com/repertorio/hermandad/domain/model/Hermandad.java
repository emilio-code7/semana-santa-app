package com.repertorio.hermandad.domain.model;

import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
public class Hermandad {

    private UUID id;
    private String name;
    private String city;
    private int foundedYear;
    private String keycloakGroupId;
    private String description;
    private Instant createdAt;
    private Instant updatedAt;

    protected Hermandad() {}

    public Hermandad(String name, String city, int foundedYear, String description) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be null or blank");
        }
        if (city == null || city.isBlank()) {
            throw new IllegalArgumentException("city must not be null or blank");
        }
        if (foundedYear < 0) {
            throw new IllegalArgumentException("foundedYear must not be negative");
        }
        this.name = name;
        this.city = city;
        this.foundedYear = foundedYear;
        this.description = description;
    }

    // ponytail: private all-args constructor for adapter reconstruction
    private Hermandad(UUID id, String name, String city, int foundedYear,
                      String keycloakGroupId, String description,
                      Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.name = name;
        this.city = city;
        this.foundedYear = foundedYear;
        this.keycloakGroupId = keycloakGroupId;
        this.description = description;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Hermandad reconstruct(UUID id, String name, String city, int foundedYear,
                                         String keycloakGroupId, String description,
                                         Instant createdAt, Instant updatedAt) {
        return new Hermandad(id, name, city, foundedYear, keycloakGroupId, description, createdAt, updatedAt);
    }

}
