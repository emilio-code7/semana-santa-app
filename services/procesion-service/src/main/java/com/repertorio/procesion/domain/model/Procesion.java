package com.repertorio.procesion.domain.model;

import jakarta.persistence.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "procesion")
public class Procesion {

    @Id
    @UuidGenerator
    private UUID id;

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

    protected Procesion() {}

    private Procesion(UUID id, UUID hermandadId, LocalDate date, LocalTime time,
                      ProcesionStatus status, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.hermandadId = hermandadId;
        this.date = date;
        this.time = time;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Procesion create(UUID hermandadId, LocalDate date, LocalTime time) {
        var now = Instant.now();
        return new Procesion(
                UUID.randomUUID(),
                hermandadId,
                date,
                time,
                ProcesionStatus.PLANNED,
                now,
                now
        );
    }

    public void changeStatus(ProcesionStatus newStatus) {
        if (newStatus == this.status) return;

        switch (this.status) {
            case PLANNED -> {
                if (newStatus != ProcesionStatus.IN_PROGRESS && newStatus != ProcesionStatus.CANCELLED) {
                    throw new IllegalArgumentException(
                            "Cannot transition from PLANNED to " + newStatus);
                }
            }
            case IN_PROGRESS -> {
                if (newStatus != ProcesionStatus.COMPLETED && newStatus != ProcesionStatus.CANCELLED) {
                    throw new IllegalArgumentException(
                            "Cannot transition from IN_PROGRESS to " + newStatus);
                }
            }
            case COMPLETED, CANCELLED -> throw new IllegalArgumentException(
                    "Cannot transition from terminal status " + this.status);
        }

        this.status = newStatus;
        this.updatedAt = Instant.now();
    }

    @PrePersist
    protected void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
        if (updatedAt == null) updatedAt = Instant.now();
    }

    @PreUpdate
    protected void preUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getHermandadId() { return hermandadId; }
    public LocalDate getDate() { return date; }
    public LocalTime getTime() { return time; }
    public ProcesionStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
