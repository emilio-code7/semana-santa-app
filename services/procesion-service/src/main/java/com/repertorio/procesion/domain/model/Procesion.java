package com.repertorio.procesion.domain.model;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public class Procesion {

    private final UUID id;
    private final UUID hermandadId;
    private final LocalDate fecha;
    private final LocalTime hora;
    private ProcesionEstado estado;
    private final Instant createdAt;
    private Instant updatedAt;

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

    public UUID getId() { return id; }
    public UUID getHermandadId() { return hermandadId; }
    public LocalDate getFecha() { return fecha; }
    public LocalTime getHora() { return hora; }
    public ProcesionEstado getEstado() { return estado; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
