package com.repertorio.procesion.adapter.outbound.persistence;

import com.repertorio.procesion.domain.model.Procesion;
import com.repertorio.procesion.domain.model.ProcesionStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.domain.Persistable;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "procesion", indexes = @Index(name = "idx_procesion_hermandad_id", columnList = "hermandad_id"))
public class ProcesionEntity implements Persistable<UUID> {

    @Id
    @UuidGenerator
    private UUID id;

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

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected ProcesionEntity() {}

    public ProcesionEntity(UUID id, UUID hermandadId, LocalDate date, LocalTime time,
                           ProcesionStatus status, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.hermandadId = hermandadId;
        this.date = date;
        this.time = time;
        this.status = status;
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

    public static ProcesionEntity from(Procesion domain) {
        return new ProcesionEntity(
                domain.getId(),
                domain.getHermandadId(),
                domain.getDate(),
                domain.getTime(),
                domain.getStatus(),
                domain.getCreatedAt(),
                domain.getUpdatedAt()
        );
    }

    public Procesion toDomain() {
        return Procesion.reconstruct(
                id, hermandadId, date, time, status, createdAt, updatedAt
        );
    }

    @Override
    public UUID getId() { return id; }
    public UUID getHermandadId() { return hermandadId; }
    public LocalDate getDate() { return date; }
    public LocalTime getTime() { return time; }
    public ProcesionStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
