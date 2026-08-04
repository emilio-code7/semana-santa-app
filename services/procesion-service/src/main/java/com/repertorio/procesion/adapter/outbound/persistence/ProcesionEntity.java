package com.repertorio.procesion.adapter.outbound.persistence;

import com.repertorio.procesion.domain.model.Procesion;
import com.repertorio.procesion.domain.model.ProcesionStatus;
import jakarta.persistence.*;
import org.springframework.data.domain.Persistable;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "procesion", indexes = @Index(name = "idx_procesion_hermandad_id", columnList = "hermandad_id"))
public class ProcesionEntity implements Persistable<UUID> {

    @Id
    private UUID id;

    @Transient
    private boolean isNew = true;

    @Version
    private long version;

    @Column(nullable = false)
    private UUID hermandadId;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private LocalTime time;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProcesionStatus status = ProcesionStatus.PLANNED;

    @Column(name = "plan_finalized_at")
    private Instant planFinalizedAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected ProcesionEntity() {}

    public ProcesionEntity(UUID id, UUID hermandadId, LocalDate date, LocalTime time,
                           ProcesionStatus status, Instant planFinalizedAt, Instant createdAt, Instant updatedAt) {
        this.id = id != null ? id : UUID.randomUUID();
        this.hermandadId = hermandadId;
        this.date = date;
        this.time = time;
        this.status = status;
        this.planFinalizedAt = planFinalizedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    @Override
    public boolean isNew() { return isNew; }

    @PostLoad
    void markNotNew() { this.isNew = false; }

    @PostPersist
    void markNotNewAfterPersist() { this.isNew = false; }

    void setVersion(long version) { this.version = version; }
    long getVersion() { return version; }

    void setHermandadId(UUID hermandadId) { this.hermandadId = hermandadId; }
    void setDate(LocalDate date) { this.date = date; }
    void setTime(LocalTime time) { this.time = time; }
    void setStatus(ProcesionStatus status) { this.status = status; }
    void setPlanFinalizedAt(Instant planFinalizedAt) { this.planFinalizedAt = planFinalizedAt; }
    void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    @PrePersist
    protected void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
        if (updatedAt == null) updatedAt = Instant.now();
    }

    @PreUpdate
    protected void preUpdate() {
        updatedAt = Instant.now();
    }

    public static ProcesionEntity from(Procesion domain) {
        var entity = new ProcesionEntity(
                domain.getId(),
                domain.getHermandadId(),
                domain.getDate(),
                domain.getTime(),
                domain.getStatus(),
                domain.getPlanFinalizedAt(),
                domain.getCreatedAt(),
                domain.getUpdatedAt()
        );
        entity.isNew = domain.getId() == null;
        return entity;
    }

    public Procesion toDomain() {
        return Procesion.reconstruct(
                id, hermandadId, date, time, status, planFinalizedAt, createdAt, updatedAt
        );
    }

    @Override
    public UUID getId() { return id; }
    public UUID getHermandadId() { return hermandadId; }
    public LocalDate getDate() { return date; }
    public LocalTime getTime() { return time; }
    public ProcesionStatus getStatus() { return status; }
    public Instant getPlanFinalizedAt() { return planFinalizedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
