package com.repertorio.procesion.adapter.outbound.persistence;

import com.repertorio.procesion.domain.model.Paso;
import jakarta.persistence.*;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.domain.Persistable;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "paso",
        indexes = {
                @Index(name = "idx_paso_procesion_id", columnList = "procesion_id"),
                @Index(name = "idx_paso_titular_id", columnList = "titular_id")
        })
public class PasoEntity implements Persistable<UUID> {

    @Id
    @UuidGenerator
    private UUID id;

    @Version
    private long version;

    @Column(nullable = false)
    private UUID procesionId;

    @Column(nullable = false)
    private int position;

    @Column(nullable = false)
    private UUID titularId;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected PasoEntity() {}

    public PasoEntity(UUID id, UUID procesionId, int position, UUID titularId,
                      String notes, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.procesionId = procesionId;
        this.position = position;
        this.titularId = titularId;
        this.notes = notes;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    @Override
    public boolean isNew() { return id == null; }

    @PrePersist
    protected void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
        if (updatedAt == null) updatedAt = Instant.now();
    }

    @PreUpdate
    protected void preUpdate() {
        updatedAt = Instant.now();
    }

    public static PasoEntity from(Paso domain) {
        return new PasoEntity(
                domain.getId(),
                domain.getProcesionId(),
                domain.getPosition(),
                domain.getTitularId(),
                domain.getNotes(),
                domain.getCreatedAt(),
                domain.getUpdatedAt()
        );
    }

    public Paso toDomain() {
        return Paso.reconstruct(id, procesionId, position, titularId, notes, createdAt, updatedAt);
    }

    @Override
    public UUID getId() { return id; }
    public UUID getProcesionId() { return procesionId; }
    public int getPosition() { return position; }
    public UUID getTitularId() { return titularId; }
    public String getNotes() { return notes; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
