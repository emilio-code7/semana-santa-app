package com.repertorio.procesion.application.service;

import com.repertorio.procesion.application.port.DomainEventPublisher;
import com.repertorio.procesion.domain.event.ProcesionCreatedEvent;
import com.repertorio.procesion.domain.event.ProcesionPlanFinalizedEvent;
import com.repertorio.procesion.domain.event.ProcesionStatusChangedEvent;
import com.repertorio.procesion.domain.model.ForbiddenException;
import com.repertorio.procesion.domain.model.Paso;
import com.repertorio.procesion.domain.model.Procesion;
import com.repertorio.procesion.domain.model.ProcesionNotFoundException;
import com.repertorio.procesion.domain.model.ProcesionStatus;
import com.repertorio.procesion.domain.model.RouteSection;
import com.repertorio.procesion.domain.repository.PasoRepository;
import com.repertorio.procesion.domain.repository.ProcesionRepository;
import com.repertorio.procesion.domain.repository.RouteSectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProcesionService {

    private final ProcesionRepository procesionRepository;
    private final RouteSectionRepository routeSectionRepository;
    private final PasoRepository pasoRepository;
    private final DomainEventPublisher eventPublisher;

    @Transactional
    public Procesion createProcesion(UUID hermandadId, LocalDate date, LocalTime time) {
        var procesion = Procesion.create(hermandadId, date, time);
        procesion = procesionRepository.save(procesion);
        eventPublisher.publish(new ProcesionCreatedEvent(procesion.getId(), hermandadId, date, time));
        return procesion;
    }

    @Transactional(readOnly = true)
    public Procesion getProcesion(UUID id) {
        return procesionRepository.findById(id)
                .orElseThrow(() -> new ProcesionNotFoundException(id));
    }

    @Transactional
    public Procesion changeStatus(UUID id, ProcesionStatus newStatus) {
        var procesion = getProcesion(id);
        var previousStatus = procesion.getStatus();
        procesion.changeStatus(newStatus);
        procesion = procesionRepository.save(procesion);
        eventPublisher.publish(new ProcesionStatusChangedEvent(id, procesion.getHermandadId(), previousStatus, newStatus));
        return procesion;
    }

    // --- Route Section operations ---

    @Transactional
    public List<RouteSection> replaceRouteSections(UUID hermandadId, UUID procesionId, List<RouteSectionItem> items) {
        var procesion = procesionRepository.findById(procesionId)
                .orElseThrow(() -> new ProcesionNotFoundException(procesionId));
        if (!procesion.getHermandadId().equals(hermandadId)) {
            throw new ForbiddenException("Procesion does not belong to this hermandad");
        }
        if (procesion.isPlanFinalized()) {
            throw new IllegalStateException("Plan is already finalized — route sections are immutable");
        }

        routeSectionRepository.deleteByProcesionId(procesionId);

        var result = new ArrayList<RouteSection>();
        for (var item : items) {
            var id = item.id() != null ? item.id() : UUID.randomUUID();
            var section = RouteSection.reconstruct(id, procesionId, item.name(),
                    item.position(), item.notes(), java.time.Instant.now(), java.time.Instant.now());
            result.add(routeSectionRepository.save(section));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<RouteSection> getRouteSections(UUID hermandadId, UUID procesionId) {
        var procesion = procesionRepository.findById(procesionId)
                .orElseThrow(() -> new ProcesionNotFoundException(procesionId));
        if (!procesion.getHermandadId().equals(hermandadId)) {
            throw new ForbiddenException("Procesion does not belong to this hermandad");
        }
        return routeSectionRepository.findByProcesionIdOrderByPositionAsc(procesionId);
    }

    // --- Plan finalization ---

    @Transactional
    public Procesion finalizePlan(UUID hermandadId, UUID procesionId) {
        var procesion = procesionRepository.findById(procesionId)
                .orElseThrow(() -> new ProcesionNotFoundException(procesionId));
        if (!procesion.getHermandadId().equals(hermandadId)) {
            throw new ForbiddenException("Procesion does not belong to this hermandad");
        }

        // Guard: at least one Paso and one Route Section must exist
        var pasos = pasoRepository.findByProcesionId(procesionId);
        var routeSections = routeSectionRepository.findByProcesionIdOrderByPositionAsc(procesionId);

        if (pasos.isEmpty() || routeSections.isEmpty()) {
            throw new IllegalStateException(
                    "Cannot finalize plan: at least one paso and one route section are required");
        }

        var actuallyFinalized = procesion.finalizePlan();
        procesion = procesionRepository.save(procesion);

        if (actuallyFinalized) {
            var pasoSnapshots = pasos.stream()
                    .map(p -> new ProcesionPlanFinalizedEvent.PasoSnapshot(p.getId(), p.getPosition(), p.getTitularId()))
                    .toList();
            var routeSnapshots = routeSections.stream()
                    .map(rs -> new ProcesionPlanFinalizedEvent.RouteSectionSnapshot(rs.getId(), rs.getName(), rs.getPosition(), rs.getNotes()))
                    .toList();

            eventPublisher.publish(new ProcesionPlanFinalizedEvent(
                    procesionId, hermandadId, procesion.getPlanFinalizedAt(),
                    pasoSnapshots, routeSnapshots));
        }

        return procesion;
    }

    @Transactional(readOnly = true)
    public Page<Procesion> listByHermandad(UUID hermandadId, Pageable pageable) {
        return procesionRepository.findByHermandadId(hermandadId, pageable);
    }

    @Transactional
    public Procesion finalizePlan(UUID id) {
        var procesion = getProcesion(id);
        procesion.finalizePlan();
        procesion = procesionRepository.save(procesion);
        eventPublisher.publish(new ProcesionPlanFinalizedEvent(
                id, procesion.getHermandadId(), procesion.getDate(), procesion.getTime(), procesion.getPlanFinalizedAt()));
        return procesion;
    }

    @Transactional
    public void deleteProcesion(UUID id) {
        var procesion = procesionRepository.findById(id)
                .orElseThrow(() -> new ProcesionNotFoundException(id));
        procesionRepository.delete(procesion);
    }

    public record RouteSectionItem(UUID id, String name, int position, String notes) {}
}
