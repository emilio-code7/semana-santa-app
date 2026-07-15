package com.repertorio.marcha.adapter.outbound.persistence;

import com.repertorio.marcha.domain.model.KnownProcesion;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "known_procesion")
public class KnownProcesionEntity {

    @Id
    private UUID procesionId;

    @Column(nullable = false)
    private UUID hermandadId;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected KnownProcesionEntity() {}

    private KnownProcesionEntity(UUID procesionId, UUID hermandadId, String status, Instant updatedAt) {
        this.procesionId = procesionId;
        this.hermandadId = hermandadId;
        this.status = status;
        this.updatedAt = updatedAt;
    }

    public static KnownProcesionEntity from(KnownProcesion domain) {
        return new KnownProcesionEntity(
                domain.getProcesionId(),
                domain.getHermandadId(),
                domain.getStatus(),
                domain.getUpdatedAt()
        );
    }

    public KnownProcesion toDomain() {
        return KnownProcesion.reconstruct(procesionId, hermandadId, status, updatedAt);
    }

    public UUID getProcesionId() { return procesionId; }
    public UUID getHermandadId() { return hermandadId; }
    public String getStatus() { return status; }
    public Instant getUpdatedAt() { return updatedAt; }
}
