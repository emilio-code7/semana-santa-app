package com.repertorio.procesion.domain.event;

import com.repertorio.common.event.DomainEvent;
import com.repertorio.procesion.domain.model.ProcesionStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record ProcesionPlanFinalizedEvent(
        UUID procesionId,
        UUID hermandadId,
        LocalDate date,
        LocalTime time,
        ProcesionStatus status,
        Instant planFinalizedAt,
        List<PasoSnapshot> pasos,
        List<RouteSectionSnapshot> routeSections,
        UUID eventId,
        Instant occurredAt
) implements DomainEvent {

    public ProcesionPlanFinalizedEvent(UUID procesionId, UUID hermandadId, LocalDate date, LocalTime time,
                                        ProcesionStatus status, Instant planFinalizedAt,
                                        List<PasoSnapshot> pasos, List<RouteSectionSnapshot> routeSections) {
        this(procesionId, hermandadId, date, time, status, planFinalizedAt, pasos, routeSections,
                UUID.randomUUID(), Instant.now());
    }

    @Override
    public String aggregateType() { return "procesion"; }
    @Override
    public UUID aggregateId() { return procesionId; }
    @Override
    public String eventType() { return "PROCESION_PLAN_FINALIZED"; }

    public record PasoSnapshot(UUID id, int position, UUID titularId) {}
    public record RouteSectionSnapshot(UUID id, String name, int position, String notes) {}
}
