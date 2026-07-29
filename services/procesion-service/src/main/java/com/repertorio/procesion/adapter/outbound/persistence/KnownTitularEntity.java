package com.repertorio.procesion.adapter.outbound.persistence;

import com.repertorio.procesion.domain.model.KnownTitular;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "known_titular", indexes = @Index(name = "idx_known_titular_hermandad_id", columnList = "hermandad_id"))
public class KnownTitularEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID hermandadId;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected KnownTitularEntity() {}

    private KnownTitularEntity(UUID id, UUID hermandadId, String name, Instant updatedAt) {
        this.id = id;
        this.hermandadId = hermandadId;
        this.name = name;
        this.updatedAt = updatedAt;
    }

    public static KnownTitularEntity from(KnownTitular domain) {
        return new KnownTitularEntity(
                domain.getId(),
                domain.getHermandadId(),
                domain.getName(),
                domain.getUpdatedAt()
        );
    }

    public KnownTitular toDomain() {
        return KnownTitular.reconstruct(id, hermandadId, name, updatedAt);
    }

    public UUID getId() { return id; }
    public UUID getHermandadId() { return hermandadId; }
    public String getName() { return name; }
    public Instant getUpdatedAt() { return updatedAt; }
}
