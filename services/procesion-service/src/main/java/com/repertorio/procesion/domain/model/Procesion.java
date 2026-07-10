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
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(nullable = false)
    private UUID hermandadId;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(nullable = false)
    private LocalTime hora;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProcesionEstado estado = ProcesionEstado.PLANIFICADA;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected Procesion() {}

    private Procesion(UUID id, UUID hermandadId, LocalDate fecha, LocalTime hora,
                      ProcesionEstado estado, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.hermandadId = hermandadId;
        this.fecha = fecha;
        this.hora = hora;
        this.estado = estado;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Procesion crear(UUID hermandadId, LocalDate fecha, LocalTime hora) {
        var now = Instant.now();
        return new Procesion(
                UUID.randomUUID(),
                hermandadId,
                fecha,
                hora,
                ProcesionEstado.PLANIFICADA,
                now,
                now
        );
    }

    public void cambiarEstado(ProcesionEstado nuevoEstado) {
        if (nuevoEstado == this.estado) return;

        switch (this.estado) {
            case PLANIFICADA -> {
                if (nuevoEstado != ProcesionEstado.EN_CURSO && nuevoEstado != ProcesionEstado.CANCELADA) {
                    throw new IllegalArgumentException(
                            "No se puede cambiar estado de PLANIFICADA a " + nuevoEstado);
                }
            }
            case EN_CURSO -> {
                if (nuevoEstado != ProcesionEstado.FINALIZADA && nuevoEstado != ProcesionEstado.CANCELADA) {
                    throw new IllegalArgumentException(
                            "No se puede cambiar estado de EN_CURSO a " + nuevoEstado);
                }
            }
            case FINALIZADA, CANCELADA -> throw new IllegalArgumentException(
                    "No se puede cambiar estado desde " + this.estado);
        }

        this.estado = nuevoEstado;
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
    public LocalDate getFecha() { return fecha; }
    public LocalTime getHora() { return hora; }
    public ProcesionEstado getEstado() { return estado; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
