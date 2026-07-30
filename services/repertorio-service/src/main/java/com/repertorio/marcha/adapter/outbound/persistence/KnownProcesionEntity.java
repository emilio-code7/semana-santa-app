package com.repertorio.marcha.adapter.outbound.persistence;

import com.repertorio.marcha.domain.model.KnownProcesion;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "known_procesion")
public class KnownProcesionEntity {

    @Id
    private UUID procesionId;

    @Column(nullable = false)
    private UUID hermandadId;

    @Column
    private LocalDate date;

    @Column
    private LocalTime time;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "plan_finalized_at")
    private Instant planFinalizedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected KnownProcesionEntity() {}

    private KnownProcesionEntity(UUID procesionId, UUID hermandadId, LocalDate date, LocalTime time,
                                 String status, Instant planFinalizedAt, Instant updatedAt) {
        this.procesionId = procesionId;
        this.hermandadId = hermandadId;
        this.date = date;
        this.time = time;
        this.status = status;
        this.planFinalizedAt = planFinalizedAt;
        this.updatedAt = updatedAt;
    }

    public static KnownProcesionEntity from(KnownProcesion domain) {
        return new KnownProcesionEntity(
                domain.getProcesionId(),
                domain.getHermandadId(),
                domain.getDate(),
                domain.getTime(),
                domain.getStatus(),
                domain.getPlanFinalizedAt(),
                domain.getUpdatedAt()
        );
    }

    public KnownProcesion toDomain() {
        return KnownProcesion.reconstruct(procesionId, hermandadId, date, time, status, planFinalizedAt, updatedAt);
    }

    public UUID getProcesionId() { return procesionId; }
    public UUID getHermandadId() { return hermandadId; }
    public LocalDate getDate() { return date; }
    public LocalTime getTime() { return time; }
    public String getStatus() { return status; }
    public Instant getPlanFinalizedAt() { return planFinalizedAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
