package com.repertorio.hermandad.adapter.outbound.persistence;

import com.repertorio.hermandad.domain.model.Hermandad;
import jakarta.persistence.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "hermandad")
public class HermandadEntity {

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

    protected HermandadEntity() {}

    public HermandadEntity(UUID id, String name, String city, int foundedYear,
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

    @PrePersist
    public void prePersist() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }

    public static HermandadEntity from(Hermandad domain) {
        return new HermandadEntity(
                domain.getId(),
                domain.getName(),
                domain.getCity(),
                domain.getFoundedYear(),
                domain.getKeycloakGroupId(),
                domain.getDescription(),
                domain.getCreatedAt(),
                domain.getUpdatedAt()
        );
    }

    public Hermandad toDomain() {
        return Hermandad.reconstruct(
                id, name, city, foundedYear, keycloakGroupId, description, createdAt, updatedAt
        );
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getCity() { return city; }
    public int getFoundedYear() { return foundedYear; }
    public String getKeycloakGroupId() { return keycloakGroupId; }
    public String getDescription() { return description; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
