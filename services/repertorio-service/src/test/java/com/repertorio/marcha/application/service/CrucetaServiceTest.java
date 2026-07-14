package com.repertorio.marcha.application.service;

import com.repertorio.marcha.application.port.DomainEventPublisher;
import com.repertorio.marcha.domain.event.CrucetaDefinedEvent;
import com.repertorio.marcha.domain.model.Cruceta;
import com.repertorio.marcha.domain.model.CrucetaItem;
import com.repertorio.marcha.domain.model.CrucetaNotFoundException;
import com.repertorio.marcha.domain.port.CrucetaRepository;
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

@ExtendWith(MockitoExtension.class)
class CrucetaServiceTest {

    @Mock
    CrucetaRepository crucetaRepository;
    @Mock
    DomainEventPublisher eventPublisher;
    @InjectMocks
    CrucetaService crucetaService;

    @Test
    void defineCruceta_createsNew() {
        var procesionId = UUID.randomUUID();
        var items = List.of(new CrucetaItem(UUID.randomUUID(), 1, "Opening"));
        when(crucetaRepository.save(any(Cruceta.class))).thenAnswer(inv -> inv.getArgument(0));

        var cruceta = crucetaService.defineCruceta(procesionId, items);

        assertEquals(procesionId, cruceta.getProcesionId());
        assertEquals(1, cruceta.getItems().size());
        verify(crucetaRepository).save(any(Cruceta.class));
        verify(eventPublisher).publish(any(CrucetaDefinedEvent.class));
    }

    @Test
    void defineCruceta_replacesExisting() {
        var procesionId = UUID.randomUUID();
        var existing = new Cruceta(procesionId, List.of(new CrucetaItem(UUID.randomUUID(), 1, "Old")));
        when(crucetaRepository.findByProcesionId(procesionId)).thenReturn(Optional.of(existing));
        when(crucetaRepository.save(any(Cruceta.class))).thenAnswer(inv -> inv.getArgument(0));

        var newItems = List.of(new CrucetaItem(UUID.randomUUID(), 1, "New"));
        var cruceta = crucetaService.defineCruceta(procesionId, newItems);

        assertEquals(procesionId, cruceta.getProcesionId());
        assertEquals(1, cruceta.getItems().size());
        assertEquals("New", cruceta.getItems().getFirst().getNotes());
        verify(crucetaRepository).save(any(Cruceta.class));
        verify(eventPublisher).publish(any(CrucetaDefinedEvent.class));
    }

    @Test
    void getCruceta_returnsWhenFound() {
        var procesionId = UUID.randomUUID();
        var cruceta = new Cruceta(procesionId, List.of());
        when(crucetaRepository.findByProcesionId(procesionId)).thenReturn(Optional.of(cruceta));

        var result = crucetaService.getCruceta(procesionId);

        assertEquals(procesionId, result.getProcesionId());
    }

    @Test
    void getCruceta_throwsWhenNotFound() {
        var procesionId = UUID.randomUUID();
        when(crucetaRepository.findByProcesionId(procesionId)).thenReturn(Optional.empty());

        assertThrows(CrucetaNotFoundException.class, () -> crucetaService.getCruceta(procesionId));
    }
}
