package com.repertorio.procesion.application.service;

import com.repertorio.common.event.DomainEvent;
import com.repertorio.procesion.application.port.DomainEventPublisher;
import com.repertorio.procesion.domain.event.ProcesionCreatedEvent;
import com.repertorio.procesion.domain.event.ProcesionPlanFinalizedEvent;
import com.repertorio.procesion.domain.event.ProcesionStatusChangedEvent;
import com.repertorio.procesion.domain.model.Procesion;
import com.repertorio.procesion.domain.model.ProcesionStatus;
import com.repertorio.procesion.domain.model.ProcesionNotFoundException;
import com.repertorio.procesion.domain.repository.ProcesionRepository;
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
        assertThat(eventCaptor.getValue()).isInstanceOf(ProcesionCreatedEvent.class);
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
    }

    @Test
    void changeStatusRejectsInvalidTransition() {
        var id = UUID.randomUUID();
        var procesion = Procesion.create(UUID.randomUUID(), LocalDate.now(), LocalTime.now());

        when(procesionRepository.findById(id)).thenReturn(Optional.of(procesion));

        // PLANNED -> COMPLETED is invalid
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
    void finalizePlanFinalizesAndPublishesEvent() {
        var id = UUID.randomUUID();
        var procesion = Procesion.create(UUID.randomUUID(), LocalDate.now(), LocalTime.now());

        when(procesionRepository.findById(id)).thenReturn(Optional.of(procesion));
        when(procesionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = procesionService.finalizePlan(id);

        assertThat(result.getPlanFinalizedAt()).isNotNull();
        assertThat(result.isPlanFinalized()).isTrue();
        verify(eventPublisher).publish(eventCaptor.capture());
        var event = (ProcesionPlanFinalizedEvent) eventCaptor.getValue();
        assertThat(event.id()).isEqualTo(id);
        assertThat(event.planFinalizedAt()).isNotNull();
    }

    @Test
    void finalizePlanIsIdempotent() {
        var id = UUID.randomUUID();
        var procesion = Procesion.create(UUID.randomUUID(), LocalDate.now(), LocalTime.now());
        procesion.finalizePlan();
        Instant firstFinalizedAt = procesion.getPlanFinalizedAt();

        when(procesionRepository.findById(id)).thenReturn(Optional.of(procesion));
        when(procesionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = procesionService.finalizePlan(id);

        assertThat(result.getPlanFinalizedAt()).isEqualTo(firstFinalizedAt);
        // still publishes the event — the caller decides idempotency semantics
        verify(eventPublisher).publish(eventCaptor.capture());
        var event = (ProcesionPlanFinalizedEvent) eventCaptor.getValue();
        assertThat(event.planFinalizedAt()).isEqualTo(firstFinalizedAt);
    }

    @Test
    void deleteProcesionThrowsWhenNotFound() {
        var id = UUID.randomUUID();

        when(procesionRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ProcesionNotFoundException.class, () -> procesionService.deleteProcesion(id));
    }
}
