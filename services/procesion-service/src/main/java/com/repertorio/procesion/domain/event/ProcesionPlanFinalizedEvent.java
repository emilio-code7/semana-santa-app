package com.repertorio.procesion.domain.event;

import com.repertorio.common.event.DomainEvent;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ProcesionPlanFinalizedEvent(
        UUID id,
        UUID hermandadId,
        Instant planFinalizedAt,
        List<PasoSnapshot> pasos,
        List<RouteSectionSnapshot> routeSections,
        UUID eventId,
        Instant occurredAt
) implements DomainEvent {

    public ProcesionPlanFinalizedEvent(UUID id, UUID hermandadId, Instant planFinalizedAt,
                                       List<PasoSnapshot> pasos, List<RouteSectionSnapshot> routeSections) {
        this(id, hermandadId, planFinalizedAt, pasos, routeSections, UUID.randomUUID(), Instant.now());
    }

    @Override
    public String aggregateType() { return "procesion"; }
    @Override
    public UUID aggregateId() { return id; }
    @Override
    public String eventType() { return "PROCESION_PLAN_FINALIZED"; }

    public record PasoSnapshot(UUID id, int position, UUID titularId) {}
    public record RouteSectionSnapshot(UUID id, String name, int position, String notes) {}
}
