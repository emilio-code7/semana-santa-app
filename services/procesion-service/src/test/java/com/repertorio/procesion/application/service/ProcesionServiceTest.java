package com.repertorio.procesion.application.service;

import com.repertorio.common.event.DomainEvent;
import com.repertorio.procesion.application.port.DomainEventPublisher;
import com.repertorio.procesion.domain.event.ProcesionCreatedEvent;
import com.repertorio.procesion.domain.event.ProcesionDeletedEvent;
import com.repertorio.procesion.domain.event.ProcesionPlanFinalizedEvent;
import com.repertorio.procesion.domain.event.ProcesionStatusChangedEvent;
import com.repertorio.procesion.domain.model.ForbiddenException;
import com.repertorio.procesion.domain.model.Paso;
import com.repertorio.procesion.domain.model.Procesion;
import com.repertorio.procesion.domain.model.ProcesionStatus;
import com.repertorio.procesion.domain.model.ProcesionNotFoundException;
import com.repertorio.procesion.domain.model.RouteSection;
import com.repertorio.procesion.domain.repository.PasoRepository;
import com.repertorio.procesion.domain.repository.ProcesionRepository;
import com.repertorio.procesion.domain.repository.RouteSectionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProcesionServiceTest {

    @Mock
    private ProcesionRepository procesionRepository;

    @Mock
    private RouteSectionRepository routeSectionRepository;

    @Mock
    private PasoRepository pasoRepository;

    @Mock
    private DomainEventPublisher eventPublisher;

    @InjectMocks
    private ProcesionService procesionService;

    @Captor
    private ArgumentCaptor<DomainEvent> eventCaptor;

    @Test
    void createProcesionPersistsAndPublishesEvent() {
        var hermandadId = UUID.randomUUID();
        var date = LocalDate.of(2026, 4, 5);
        var time = LocalTime.of(18, 0);
        var saved = Procesion.create(hermandadId, date, time);

        when(procesionRepository.save(any())).thenReturn(saved);

        var result = procesionService.createProcesion(hermandadId, date, time);

        assertThat(result).isNotNull();
        assertThat(result.getHermandadId()).isEqualTo(hermandadId);
        assertThat(result.getStatus()).isEqualTo(ProcesionStatus.PLANNED);
        verify(procesionRepository).save(any());
        verify(eventPublisher).publish(eventCaptor.capture());
        var event = eventCaptor.getValue();
        assertThat(event).isInstanceOf(ProcesionCreatedEvent.class);
        assertThat(event.eventType()).isEqualTo("PROCESION_CREATED");
        assertThat(event.schemaVersion()).isEqualTo(1);
    }

    @Test
    void getProcesionReturnsWhenFound() {
        var id = UUID.randomUUID();
        var procesion = Procesion.create(UUID.randomUUID(), LocalDate.now(), LocalTime.now());

        when(procesionRepository.findById(id)).thenReturn(Optional.of(procesion));

        var result = procesionService.getProcesion(id);

        assertThat(result).isEqualTo(procesion);
    }

    @Test
    void getProcesionThrowsWhenNotFound() {
        var id = UUID.randomUUID();

        when(procesionRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ProcesionNotFoundException.class, () -> procesionService.getProcesion(id));
    }

    @Test
    void changeStatusTransitionsCorrectly() {
        var id = UUID.randomUUID();
        var procesion = Procesion.create(UUID.randomUUID(), LocalDate.now(), LocalTime.now());

        when(procesionRepository.findById(id)).thenReturn(Optional.of(procesion));
        when(procesionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = procesionService.changeStatus(id, ProcesionStatus.IN_PROGRESS);

        assertThat(result.getStatus()).isEqualTo(ProcesionStatus.IN_PROGRESS);
        verify(eventPublisher).publish(eventCaptor.capture());
        var event = (ProcesionStatusChangedEvent) eventCaptor.getValue();
        assertThat(event.previousStatus()).isEqualTo(ProcesionStatus.PLANNED);
        assertThat(event.newStatus()).isEqualTo(ProcesionStatus.IN_PROGRESS);
        assertThat(event.eventType()).isEqualTo("PROCESION_STATUS_CHANGED");
        assertThat(event.schemaVersion()).isEqualTo(1);
    }

    @Test
    void changeStatusRejectsInvalidTransition() {
        var id = UUID.randomUUID();
        var procesion = Procesion.create(UUID.randomUUID(), LocalDate.now(), LocalTime.now());

        when(procesionRepository.findById(id)).thenReturn(Optional.of(procesion));

        assertThrows(IllegalArgumentException.class,
                () -> procesionService.changeStatus(id, ProcesionStatus.COMPLETED));
    }

    @Test
    void listByHermandadReturnsPage() {
        var hermandadId = UUID.randomUUID();
        var pageRequest = PageRequest.of(0, 10);
        var page = new PageImpl<>(List.of(Procesion.create(hermandadId, LocalDate.now(), LocalTime.now())));

        when(procesionRepository.findByHermandadId(hermandadId, pageRequest)).thenReturn(page);

        var result = procesionService.listByHermandad(hermandadId, pageRequest);

        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void deleteProcesionDeletesWhenFound() {
        var id = UUID.randomUUID();
        var procesion = Procesion.create(UUID.randomUUID(), LocalDate.now(), LocalTime.now());

        when(procesionRepository.findById(id)).thenReturn(Optional.of(procesion));

        procesionService.deleteProcesion(id);

        verify(procesionRepository).delete(procesion);
    }

    @Test
    void deleteProcesionPublishesDeletedEvent() {
        var id = UUID.randomUUID();
        var hermandadId = UUID.randomUUID();
        var procesion = Procesion.create(hermandadId, LocalDate.now(), LocalTime.now());

        when(procesionRepository.findById(id)).thenReturn(Optional.of(procesion));

        procesionService.deleteProcesion(id);

        verify(procesionRepository).delete(procesion);
        verify(eventPublisher).publish(eventCaptor.capture());
        var event = eventCaptor.getValue();
        assertThat(event).isInstanceOf(ProcesionDeletedEvent.class);
        assertThat(((ProcesionDeletedEvent) event).id()).isEqualTo(id);
        assertThat(((ProcesionDeletedEvent) event).hermandadId()).isEqualTo(hermandadId);
        assertThat(event.eventType()).isEqualTo("PROCESION_DELETED");
        assertThat(event.schemaVersion()).isEqualTo(1);
    }

    @Test
    void deleteProcesionThrowsWhenNotFound() {
        var id = UUID.randomUUID();

        when(procesionRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ProcesionNotFoundException.class, () -> procesionService.deleteProcesion(id));
    }

    // --- Route Section tests ---

    @Test
    void getRouteSectionsReturnsOrderedList() {
        var hermandadId = UUID.randomUUID();
        var procesionId = UUID.randomUUID();
        var procesion = Procesion.create(hermandadId, LocalDate.now(), LocalTime.now());
        procesion = Procesion.reconstruct(procesionId, hermandadId, procesion.getDate(), procesion.getTime(),
                procesion.getStatus(), procesion.getPlanFinalizedAt(),
                procesion.getCreatedAt(), procesion.getUpdatedAt());

        when(procesionRepository.findById(procesionId)).thenReturn(Optional.of(procesion));
        when(routeSectionRepository.findByProcesionIdOrderByPositionAsc(procesionId))
                .thenReturn(List.of(
                        RouteSection.create(procesionId, "First", 0, null),
                        RouteSection.create(procesionId, "Second", 1, "notes")));

        var result = procesionService.getRouteSections(hermandadId, procesionId);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("First");
    }

    @Test
    void getRouteSectionsThrowsForbiddenOnCrossTenant() {
        var hermandadId = UUID.randomUUID();
        var otherHermandad = UUID.randomUUID();
        var procesionId = UUID.randomUUID();
        var procesion = Procesion.create(otherHermandad, LocalDate.now(), LocalTime.now());
        procesion = Procesion.reconstruct(procesionId, otherHermandad, procesion.getDate(), procesion.getTime(),
                procesion.getStatus(), procesion.getPlanFinalizedAt(),
                procesion.getCreatedAt(), procesion.getUpdatedAt());

        when(procesionRepository.findById(procesionId)).thenReturn(Optional.of(procesion));

        assertThrows(ForbiddenException.class,
                () -> procesionService.getRouteSections(hermandadId, procesionId));
    }

    @Test
    void replaceRouteSectionsSavesWithStableIds() {
        var hermandadId = UUID.randomUUID();
        var procesionId = UUID.randomUUID();
        var procesion = Procesion.create(hermandadId, LocalDate.now(), LocalTime.now());
        procesion = Procesion.reconstruct(procesionId, hermandadId, procesion.getDate(), procesion.getTime(),
                procesion.getStatus(), procesion.getPlanFinalizedAt(),
                procesion.getCreatedAt(), procesion.getUpdatedAt());
        var itemId = UUID.randomUUID();
        var items = List.of(
                new ProcesionService.RouteSectionItem(itemId, "Section A", 0, null),
                new ProcesionService.RouteSectionItem(null, "Section B", 1, "notes"));

        when(procesionRepository.findById(procesionId)).thenReturn(Optional.of(procesion));
        when(routeSectionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = procesionService.replaceRouteSections(hermandadId, procesionId, items);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(itemId); // stable ID preserved
        assertThat(result.get(0).getName()).isEqualTo("Section A");
        assertThat(result.get(1).getName()).isEqualTo("Section B");
        assertThat(result.get(1).getNotes()).isEqualTo("notes");
        verify(routeSectionRepository).deleteByProcesionId(procesionId);
    }

    @Test
    void replaceRouteSectionsRejectsWhenFinalized() {
        var hermandadId = UUID.randomUUID();
        var procesionId = UUID.randomUUID();
        var procesion = Procesion.create(hermandadId, LocalDate.now(), LocalTime.now());
        procesion.finalizePlan();
        procesion = Procesion.reconstruct(procesionId, hermandadId, procesion.getDate(), procesion.getTime(),
                procesion.getStatus(), procesion.getPlanFinalizedAt(),
                procesion.getCreatedAt(), procesion.getUpdatedAt());

        when(procesionRepository.findById(procesionId)).thenReturn(Optional.of(procesion));

        assertThrows(IllegalStateException.class,
                () -> procesionService.replaceRouteSections(hermandadId, procesionId, List.of()));
    }

    // --- Finalization tests ---

    @Test
    void finalizePlanSucceedsWhenPasosAndSectionsExist() {
        var hermandadId = UUID.randomUUID();
        var procesionId = UUID.randomUUID();
        var procesion = Procesion.create(hermandadId, LocalDate.now(), LocalTime.now());
        procesion = Procesion.reconstruct(procesionId, hermandadId, procesion.getDate(), procesion.getTime(),
                procesion.getStatus(), procesion.getPlanFinalizedAt(),
                procesion.getCreatedAt(), procesion.getUpdatedAt());

        when(procesionRepository.findById(procesionId)).thenReturn(Optional.of(procesion));
        when(pasoRepository.findByProcesionId(procesionId))
                .thenReturn(List.of(Paso.create(procesionId, 0, UUID.randomUUID(), null)));
        when(routeSectionRepository.findByProcesionIdOrderByPositionAsc(procesionId))
                .thenReturn(List.of(RouteSection.create(procesionId, "Section", 0, null)));
        when(procesionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = procesionService.finalizePlan(hermandadId, procesionId);

        assertThat(result.isPlanFinalized()).isTrue();
        assertThat(result.getPlanFinalizedAt()).isNotNull();
        verify(eventPublisher).publish(eventCaptor.capture());
        assertThat(eventCaptor.getValue()).isInstanceOf(ProcesionPlanFinalizedEvent.class);
    }

    @Test
    void finalizePlanFailsWhenNoPasos() {
        var hermandadId = UUID.randomUUID();
        var procesionId = UUID.randomUUID();
        var procesion = Procesion.create(hermandadId, LocalDate.now(), LocalTime.now());
        procesion = Procesion.reconstruct(procesionId, hermandadId, procesion.getDate(), procesion.getTime(),
                procesion.getStatus(), procesion.getPlanFinalizedAt(),
                procesion.getCreatedAt(), procesion.getUpdatedAt());

        when(procesionRepository.findById(procesionId)).thenReturn(Optional.of(procesion));
        when(pasoRepository.findByProcesionId(procesionId)).thenReturn(List.of());
        when(routeSectionRepository.findByProcesionIdOrderByPositionAsc(procesionId))
                .thenReturn(List.of(RouteSection.create(procesionId, "Section", 0, null)));

        assertThrows(IllegalStateException.class,
                () -> procesionService.finalizePlan(hermandadId, procesionId));
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    void finalizePlanFailsWhenNoRouteSections() {
        var hermandadId = UUID.randomUUID();
        var procesionId = UUID.randomUUID();
        var procesion = Procesion.create(hermandadId, LocalDate.now(), LocalTime.now());
        procesion = Procesion.reconstruct(procesionId, hermandadId, procesion.getDate(), procesion.getTime(),
                procesion.getStatus(), procesion.getPlanFinalizedAt(),
                procesion.getCreatedAt(), procesion.getUpdatedAt());

        when(procesionRepository.findById(procesionId)).thenReturn(Optional.of(procesion));
        when(pasoRepository.findByProcesionId(procesionId))
                .thenReturn(List.of(Paso.create(procesionId, 0, UUID.randomUUID(), null)));
        when(routeSectionRepository.findByProcesionIdOrderByPositionAsc(procesionId)).thenReturn(List.of());

        assertThrows(IllegalStateException.class,
                () -> procesionService.finalizePlan(hermandadId, procesionId));
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    void finalizePlanIsIdempotent() {
        var hermandadId = UUID.randomUUID();
        var procesionId = UUID.randomUUID();
        var procesion = Procesion.create(hermandadId, LocalDate.now(), LocalTime.now());
        procesion.finalizePlan();
        procesion = Procesion.reconstruct(procesionId, hermandadId, procesion.getDate(), procesion.getTime(),
                procesion.getStatus(), procesion.getPlanFinalizedAt(),
                procesion.getCreatedAt(), procesion.getUpdatedAt());

        when(procesionRepository.findById(procesionId)).thenReturn(Optional.of(procesion));
        when(procesionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(pasoRepository.findByProcesionId(procesionId))
                .thenReturn(List.of(Paso.create(procesionId, 0, UUID.randomUUID(), null)));
        when(routeSectionRepository.findByProcesionIdOrderByPositionAsc(procesionId))
                .thenReturn(List.of(RouteSection.create(procesionId, "Section", 0, null)));

        var result = procesionService.finalizePlan(hermandadId, procesionId);

        assertThat(result.isPlanFinalized()).isTrue();
        // No new event published on idempotent call
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    void finalizePlanThrowsForbiddenOnCrossTenant() {
        var hermandadId = UUID.randomUUID();
        var otherHermandad = UUID.randomUUID();
        var procesionId = UUID.randomUUID();
        var procesion = Procesion.create(otherHermandad, LocalDate.now(), LocalTime.now());
        procesion = Procesion.reconstruct(procesionId, otherHermandad, procesion.getDate(), procesion.getTime(),
                procesion.getStatus(), procesion.getPlanFinalizedAt(),
                procesion.getCreatedAt(), procesion.getUpdatedAt());

        when(procesionRepository.findById(procesionId)).thenReturn(Optional.of(procesion));

        assertThrows(ForbiddenException.class,
                () -> procesionService.finalizePlan(hermandadId, procesionId));
    }

    @Test
    void finalizePlanEventContainsSnapshots() {
        var hermandadId = UUID.randomUUID();
        var procesionId = UUID.randomUUID();
        var titularId = UUID.randomUUID();
        var procesion = Procesion.create(hermandadId, LocalDate.now(), LocalTime.now());
        procesion = Procesion.reconstruct(procesionId, hermandadId, procesion.getDate(), procesion.getTime(),
                procesion.getStatus(), procesion.getPlanFinalizedAt(),
                procesion.getCreatedAt(), procesion.getUpdatedAt());

        when(procesionRepository.findById(procesionId)).thenReturn(Optional.of(procesion));
        when(pasoRepository.findByProcesionId(procesionId))
                .thenReturn(List.of(Paso.create(procesionId, 0, titularId, null)));
        when(routeSectionRepository.findByProcesionIdOrderByPositionAsc(procesionId))
                .thenReturn(List.of(RouteSection.create(procesionId, "Section", 0, "notes")));
        when(procesionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        procesionService.finalizePlan(hermandadId, procesionId);

        verify(eventPublisher).publish(eventCaptor.capture());
        var event = (ProcesionPlanFinalizedEvent) eventCaptor.getValue();
        assertThat(event.pasos()).hasSize(1);
        assertThat(event.pasos().get(0).position()).isEqualTo(0);
        assertThat(event.pasos().get(0).titularId()).isEqualTo(titularId);
        assertThat(event.routeSections()).hasSize(1);
        assertThat(event.routeSections().get(0).name()).isEqualTo("Section");
        assertThat(event.routeSections().get(0).notes()).isEqualTo("notes");
        assertThat(event.eventType()).isEqualTo("PROCESION_PLAN_FINALIZED");
        assertThat(event.schemaVersion()).isEqualTo(1);
    }
}
