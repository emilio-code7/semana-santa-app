package com.repertorio.hermandad.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "hermandad")
@Getter
public class Hermandad {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;
    @Column(nullable = false, unique = true)
    private String name;
    @Column(nullable = false)
    private String city;
    @Column(nullable = false)
    private int foundedYear;
    @Column
    private String keycloakGroupId;
    @Column
    private String description;
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
    @Column
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

    @PrePersist
    public void prePersist() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }

}
