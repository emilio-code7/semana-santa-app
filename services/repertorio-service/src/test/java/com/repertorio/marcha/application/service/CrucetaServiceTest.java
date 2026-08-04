package com.repertorio.marcha.application.service;

import com.repertorio.marcha.application.port.DomainEventPublisher;
import com.repertorio.marcha.domain.event.CrucetaDefinedEvent;
import com.repertorio.marcha.domain.model.*;
import com.repertorio.marcha.domain.port.CrucetaRepository;
import com.repertorio.marcha.domain.port.KnownProcesionRepository;
import com.repertorio.marcha.domain.port.MarchaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.mockito.ArgumentCaptor;

@ExtendWith(MockitoExtension.class)
class CrucetaServiceTest {

    @Mock
    CrucetaRepository crucetaRepository;
    @Mock
    DomainEventPublisher eventPublisher;
    @Mock
    KnownProcesionRepository knownProcesionRepository;
    @Mock
    MarchaRepository marchaRepository;
    @InjectMocks
    CrucetaService crucetaService;

    private final UUID routeSectionId = UUID.randomUUID();

    @Test
    void defineCruceta_createsNew() {
        var pasoId = UUID.randomUUID();
        var procesionId = UUID.randomUUID();
        var marchaId = UUID.randomUUID();
        var items = List.of(new CrucetaItem(marchaId, routeSectionId, 1, "Opening"));
        when(knownProcesionRepository.findPasoById(pasoId))
                .thenReturn(Optional.of(new KnownPaso(pasoId, procesionId, 1, UUID.randomUUID())));
        when(marchaRepository.existsById(marchaId)).thenReturn(true);
        when(knownProcesionRepository.existsRouteSectionById(routeSectionId)).thenReturn(true);
        when(crucetaRepository.findByPasoId(pasoId)).thenReturn(Optional.empty());
        when(crucetaRepository.save(any(Cruceta.class))).thenAnswer(inv -> inv.getArgument(0));

        var cruceta = crucetaService.defineCruceta(pasoId, items);

        assertEquals(pasoId, cruceta.getPasoId());
        assertEquals(1, cruceta.getItems().size());
        verify(crucetaRepository).save(any(Cruceta.class));
        var eventCaptor = ArgumentCaptor.forClass(CrucetaDefinedEvent.class);
        verify(eventPublisher).publish(eventCaptor.capture());
        var event = eventCaptor.getValue();
        assertEquals(procesionId, event.procesionId());
        assertEquals(pasoId, event.pasoId());
        assertEquals(items.size(), event.itemCount());
        assertNotNull(event.eventId());
        assertEquals("CRUCETA_DEFINED", event.eventType());
        assertEquals(1, event.schemaVersion());
    }

    @Test
    void defineCruceta_replacesExisting() {
        var pasoId = UUID.randomUUID();
        var procesionId = UUID.randomUUID();
        var marchaId = UUID.randomUUID();
        var existing = new Cruceta(pasoId, List.of(new CrucetaItem(marchaId, routeSectionId, 1, "Old")));
        var existingId = existing.getId();
        when(knownProcesionRepository.findPasoById(pasoId))
                .thenReturn(Optional.of(new KnownPaso(pasoId, procesionId, 1, UUID.randomUUID())));
        when(marchaRepository.existsById(marchaId)).thenReturn(true);
        when(knownProcesionRepository.existsRouteSectionById(routeSectionId)).thenReturn(true);
        when(crucetaRepository.findByPasoId(pasoId)).thenReturn(Optional.of(existing));
        when(crucetaRepository.save(any(Cruceta.class))).thenAnswer(inv -> inv.getArgument(0));

        var newItems = List.of(new CrucetaItem(marchaId, routeSectionId, 1, "New"));
        var cruceta = crucetaService.defineCruceta(pasoId, newItems);

        assertEquals(pasoId, cruceta.getPasoId());
        assertEquals(existingId, cruceta.getId(), "aggregate ID must be preserved on replacement");
        assertEquals(1, cruceta.getItems().size());
        assertEquals("New", cruceta.getItems().getFirst().getNotes());
        verify(crucetaRepository).save(any(Cruceta.class));
        var eventCaptor = ArgumentCaptor.forClass(CrucetaDefinedEvent.class);
        verify(eventPublisher).publish(eventCaptor.capture());
        assertEquals(existingId, eventCaptor.getValue().crucetaId(), "event must use preserved aggregate ID");
        var event = eventCaptor.getValue();
        assertEquals(procesionId, event.procesionId());
        assertEquals(pasoId, event.pasoId());
        assertEquals(newItems.size(), event.itemCount());
        assertNotNull(event.eventId());
        assertEquals("CRUCETA_DEFINED", event.eventType());
        assertEquals(1, event.schemaVersion());
    }

    @Test
    void getCruceta_returnsWhenFound() {
        var pasoId = UUID.randomUUID();
        var cruceta = new Cruceta(pasoId, List.of());
        when(crucetaRepository.findByPasoId(pasoId)).thenReturn(Optional.of(cruceta));

        var result = crucetaService.getCruceta(pasoId);

        assertEquals(pasoId, result.getPasoId());
    }

    @Test
    void getCruceta_throwsWhenNotFound() {
        var pasoId = UUID.randomUUID();
        when(crucetaRepository.findByPasoId(pasoId)).thenReturn(Optional.empty());

        assertThrows(CrucetaNotFoundException.class, () -> crucetaService.getCruceta(pasoId));
    }

    @Test
    void defineCrucetaThrowsWhenPasoNotKnown() {
        var pasoId = UUID.randomUUID();
        when(knownProcesionRepository.findPasoById(pasoId)).thenReturn(Optional.empty());

        assertThrows(PasoNotFoundException.class,
                () -> crucetaService.defineCruceta(pasoId, List.of()));
    }

    @Test
    void defineCrucetaThrowsWhenMarchaNotFound() {
        var pasoId = UUID.randomUUID();
        var procesionId = UUID.randomUUID();
        var marchaId = UUID.randomUUID();
        var items = List.of(new CrucetaItem(marchaId, routeSectionId, 0, null));

        when(knownProcesionRepository.findPasoById(pasoId))
                .thenReturn(Optional.of(new KnownPaso(pasoId, procesionId, 1, UUID.randomUUID())));
        when(marchaRepository.existsById(marchaId)).thenReturn(false);

        assertThrows(MarchaNotFoundException.class,
                () -> crucetaService.defineCruceta(pasoId, items));
    }

    @Test
    void defineCrucetaThrowsWhenRouteSectionNotFound() {
        var pasoId = UUID.randomUUID();
        var procesionId = UUID.randomUUID();
        var marchaId = UUID.randomUUID();
        var items = List.of(new CrucetaItem(marchaId, routeSectionId, 0, null));

        when(knownProcesionRepository.findPasoById(pasoId))
                .thenReturn(Optional.of(new KnownPaso(pasoId, procesionId, 1, UUID.randomUUID())));
        when(marchaRepository.existsById(marchaId)).thenReturn(true);
        when(knownProcesionRepository.existsRouteSectionById(routeSectionId)).thenReturn(false);

        assertThrows(IllegalArgumentException.class,
                () -> crucetaService.defineCruceta(pasoId, items));
    }

    @Test
    void defineCrucetaSucceedsWhenPasoIsKnown() {
        var pasoId = UUID.randomUUID();
        var procesionId = UUID.randomUUID();
        var marchaId = UUID.randomUUID();
        var items = List.of(new CrucetaItem(marchaId, routeSectionId, 0, null));

        when(knownProcesionRepository.findPasoById(pasoId))
                .thenReturn(Optional.of(new KnownPaso(pasoId, procesionId, 1, UUID.randomUUID())));
        when(marchaRepository.existsById(marchaId)).thenReturn(true);
        when(knownProcesionRepository.existsRouteSectionById(routeSectionId)).thenReturn(true);
        when(crucetaRepository.findByPasoId(pasoId)).thenReturn(Optional.empty());
        when(crucetaRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        var result = crucetaService.defineCruceta(pasoId, items);

        assertNotNull(result);
        assertEquals(pasoId, result.getPasoId());
        assertEquals(1, result.getItems().size());
        verify(knownProcesionRepository).findPasoById(pasoId);
    }
}
