package com.repertorio.hermandad.adapter.outbound.persistence;

import com.repertorio.hermandad.domain.model.Titular;
import jakarta.persistence.*;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.domain.Persistable;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "titular", indexes = @Index(name = "idx_titular_hermandad_id", columnList = "hermandad_id"))
public class TitularEntity implements Persistable<UUID> {

    @Id
    @UuidGenerator
    private UUID id;

    @Version
    private int version;

    @Column(nullable = false)
    private UUID hermandadId;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected TitularEntity() {}

    public TitularEntity(UUID id, UUID hermandadId, String name, String description,
                         Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.hermandadId = hermandadId;
        this.name = name;
        this.description = description;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }

    public static TitularEntity from(Titular domain) {
        return new TitularEntity(
                domain.getId(),
                domain.getHermandadId(),
                domain.getName(),
                domain.getDescription(),
                domain.getCreatedAt(),
                domain.getUpdatedAt()
        );
    }

    public Titular toDomain() {
        return Titular.reconstruct(id, name, description, hermandadId, createdAt, updatedAt);
    }

    @Override
    public UUID getId() { return id; }
    public UUID getHermandadId() { return hermandadId; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    // ponytail: package-private setters for adapter update path
    void setName(String name) { this.name = name; }
    void setDescription(String description) { this.description = description; }

    @Override
    public boolean isNew() { return id == null; }
}
