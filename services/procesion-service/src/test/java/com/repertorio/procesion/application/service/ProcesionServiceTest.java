package com.repertorio.procesion.application.service;

import com.repertorio.procesion.application.port.DomainEvent;
import com.repertorio.procesion.application.port.DomainEventPublisher;
import com.repertorio.procesion.domain.event.ProcesionCreatedEvent;
import com.repertorio.procesion.domain.event.ProcesionEstadoChangedEvent;
import com.repertorio.procesion.domain.model.Procesion;
import com.repertorio.procesion.domain.model.ProcesionEstado;
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
    void crearProcesionPersistsAndPublishesEvent() {
        var hermandadId = UUID.randomUUID();
        var fecha = LocalDate.of(2026, 4, 5);
        var hora = LocalTime.of(18, 0);
        var saved = Procesion.crear(hermandadId, fecha, hora);

        when(procesionRepository.save(any())).thenReturn(saved);

        var result = procesionService.crearProcesion(hermandadId, fecha, hora);

        assertThat(result).isNotNull();
        assertThat(result.getHermandadId()).isEqualTo(hermandadId);
        assertThat(result.getEstado()).isEqualTo(ProcesionEstado.PLANIFICADA);
        verify(procesionRepository).save(any());
        verify(eventPublisher).publish(eventCaptor.capture());
        assertThat(eventCaptor.getValue()).isInstanceOf(ProcesionCreatedEvent.class);
    }

    @Test
    void obtenerProcesionReturnsWhenFound() {
        var id = UUID.randomUUID();
        var procesion = Procesion.crear(UUID.randomUUID(), LocalDate.now(), LocalTime.now());

        when(procesionRepository.findById(id)).thenReturn(Optional.of(procesion));

        var result = procesionService.obtenerProcesion(id);

        assertThat(result).isEqualTo(procesion);
    }

    @Test
    void obtenerProcesionThrowsWhenNotFound() {
        var id = UUID.randomUUID();

        when(procesionRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ProcesionNotFoundException.class, () -> procesionService.obtenerProcesion(id));
    }

    @Test
    void cambiarEstadoTransitionsCorrectly() {
        var id = UUID.randomUUID();
        var procesion = Procesion.crear(UUID.randomUUID(), LocalDate.now(), LocalTime.now());

        when(procesionRepository.findById(id)).thenReturn(Optional.of(procesion));
        when(procesionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = procesionService.cambiarEstado(id, ProcesionEstado.EN_CURSO);

        assertThat(result.getEstado()).isEqualTo(ProcesionEstado.EN_CURSO);
        verify(eventPublisher).publish(eventCaptor.capture());
        var event = (ProcesionEstadoChangedEvent) eventCaptor.getValue();
        assertThat(event.estadoAnterior()).isEqualTo(ProcesionEstado.PLANIFICADA);
        assertThat(event.nuevoEstado()).isEqualTo(ProcesionEstado.EN_CURSO);
    }

    @Test
    void cambiarEstadoRejectsInvalidTransition() {
        var id = UUID.randomUUID();
        var procesion = Procesion.crear(UUID.randomUUID(), LocalDate.now(), LocalTime.now());

        when(procesionRepository.findById(id)).thenReturn(Optional.of(procesion));

        // PLANIFICADA -> FINALIZADA is invalid
        assertThrows(IllegalArgumentException.class,
                () -> procesionService.cambiarEstado(id, ProcesionEstado.FINALIZADA));
    }

    @Test
    void listarPorHermandadReturnsPage() {
        var hermandadId = UUID.randomUUID();
        var pageRequest = PageRequest.of(0, 10);
        var page = new PageImpl<>(List.of(Procesion.crear(hermandadId, LocalDate.now(), LocalTime.now())));

        when(procesionRepository.findByHermandadId(hermandadId, pageRequest)).thenReturn(page);

        var result = procesionService.listarPorHermandad(hermandadId, pageRequest);

        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void eliminarProcesionDeletesWhenFound() {
        var id = UUID.randomUUID();
        var procesion = Procesion.crear(UUID.randomUUID(), LocalDate.now(), LocalTime.now());

        when(procesionRepository.findById(id)).thenReturn(Optional.of(procesion));

        procesionService.eliminarProcesion(id);

        verify(procesionRepository).deleteById(id);
    }

    @Test
    void eliminarProcesionThrowsWhenNotFound() {
        var id = UUID.randomUUID();

        when(procesionRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ProcesionNotFoundException.class, () -> procesionService.eliminarProcesion(id));
    }
}
