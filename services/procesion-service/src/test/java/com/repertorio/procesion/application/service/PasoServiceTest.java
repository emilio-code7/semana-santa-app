package com.repertorio.procesion.application.service;

import com.repertorio.procesion.domain.model.ForbiddenException;
import com.repertorio.procesion.domain.model.KnownTitular;
import com.repertorio.procesion.domain.model.Paso;
import com.repertorio.procesion.domain.model.Procesion;
import com.repertorio.procesion.domain.model.ProcesionNotFoundException;
import com.repertorio.procesion.domain.port.KnownTitularRepository;
import com.repertorio.procesion.domain.repository.PasoRepository;
import com.repertorio.procesion.domain.repository.ProcesionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
class PasoServiceTest {

    @Mock
    private PasoRepository pasoRepository;

    @Mock
    private ProcesionRepository procesionRepository;

    @Mock
    private KnownTitularRepository knownTitularRepository;

    @InjectMocks
    private PasoService pasoService;

    @Captor
    private ArgumentCaptor<Paso> pasoCaptor;

    private final UUID hermandadId = UUID.randomUUID();
    private final UUID procesionId = UUID.randomUUID();
    private final UUID titularId = UUID.randomUUID();

    private Procesion aProcesion() {
        return Procesion.create(hermandadId, LocalDate.of(2026, 4, 13), LocalTime.of(18, 0));
    }

    private KnownTitular aTitular() {
        return new KnownTitular(titularId, hermandadId, "Jesus del Gran Poder");
    }

    // --- replacePasos ---

    @Test
    void replacePasosCreatesNewPasosAndDeletesOldOnes() {
        var procesion = aProcesion();
        when(procesionRepository.findById(procesionId)).thenReturn(Optional.of(procesion));
        when(knownTitularRepository.findById(titularId)).thenReturn(Optional.of(aTitular()));
        when(pasoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var items = List.of(
                new PasoService.PasoItem(null, 1, titularId, "First"),
                new PasoService.PasoItem(null, 2, titularId, "Second")
        );

        var result = pasoService.replacePasos(hermandadId, procesionId, items);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getPosition()).isEqualTo(1);
        assertThat(result.get(1).getPosition()).isEqualTo(2);
        verify(pasoRepository).deleteByProcesionId(procesionId);
        verify(pasoRepository, times(2)).save(any());
    }

    @Test
    void replacePasosPreservesProvidedIds() {
        var procesion = aProcesion();
        var existingId = UUID.randomUUID();
        when(procesionRepository.findById(procesionId)).thenReturn(Optional.of(procesion));
        when(knownTitularRepository.findById(titularId)).thenReturn(Optional.of(aTitular()));
        when(pasoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var items = List.of(
                new PasoService.PasoItem(existingId, 1, titularId, null)
        );

        var result = pasoService.replacePasos(hermandadId, procesionId, items);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(existingId);
    }

    @Test
    void replacePasosRejectsDuplicatePositions() {
        var procesion = aProcesion();
        when(procesionRepository.findById(procesionId)).thenReturn(Optional.of(procesion));

        var items = List.of(
                new PasoService.PasoItem(null, 1, titularId, null),
                new PasoService.PasoItem(null, 1, titularId, null)
        );

        assertThrows(IllegalArgumentException.class,
                () -> pasoService.replacePasos(hermandadId, procesionId, items));
    }

    @Test
    void replacePasosRejectsTitularFromDifferentHermandad() {
        var procesion = aProcesion();
        var otherHermandad = UUID.randomUUID();
        when(procesionRepository.findById(procesionId)).thenReturn(Optional.of(procesion));
        var otherTitular = new KnownTitular(UUID.randomUUID(), otherHermandad, "Foreign");
        when(knownTitularRepository.findById(otherTitular.getId())).thenReturn(Optional.of(otherTitular));

        var items = List.of(
                new PasoService.PasoItem(null, 1, otherTitular.getId(), null)
        );

        assertThrows(ForbiddenException.class,
                () -> pasoService.replacePasos(hermandadId, procesionId, items));
    }

    @Test
    void replacePasosRejectsUnknownTitular() {
        var procesion = aProcesion();
        var unknownTitularId = UUID.randomUUID();
        when(procesionRepository.findById(procesionId)).thenReturn(Optional.of(procesion));
        when(knownTitularRepository.findById(unknownTitularId)).thenReturn(Optional.empty());

        var items = List.of(
                new PasoService.PasoItem(null, 1, unknownTitularId, null)
        );

        assertThrows(ForbiddenException.class,
                () -> pasoService.replacePasos(hermandadId, procesionId, items));
    }

    @Test
    void replacePasosRejectsCrossTenantProcesion() {
        var otherHermandad = UUID.randomUUID();
        var otherProcesion = Procesion.create(otherHermandad, LocalDate.of(2026, 4, 13), LocalTime.of(18, 0));
        when(procesionRepository.findById(procesionId)).thenReturn(Optional.of(otherProcesion));

        var items = List.of(
                new PasoService.PasoItem(null, 1, titularId, null)
        );

        assertThrows(ForbiddenException.class,
                () -> pasoService.replacePasos(hermandadId, procesionId, items));
    }

    @Test
    void replacePasosThrowsWhenProcesionNotFound() {
        when(procesionRepository.findById(procesionId)).thenReturn(Optional.empty());

        assertThrows(ProcesionNotFoundException.class,
                () -> pasoService.replacePasos(hermandadId, procesionId, List.of()));
    }

    @Test
    void replacePasosIsIdempotent() {
        var procesion = aProcesion();
        when(procesionRepository.findById(procesionId)).thenReturn(Optional.of(procesion));
        when(knownTitularRepository.findById(titularId)).thenReturn(Optional.of(aTitular()));
        when(pasoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var existingId = UUID.randomUUID();
        var items = List.of(
                new PasoService.PasoItem(existingId, 1, titularId, null)
        );

        // First call
        var firstResult = pasoService.replacePasos(hermandadId, procesionId, items);
        assertThat(firstResult.get(0).getId()).isEqualTo(existingId);

        // Second call with same items - should preserve IDs
        var secondResult = pasoService.replacePasos(hermandadId, procesionId, items);
        assertThat(secondResult.get(0).getId()).isEqualTo(existingId);
        assertThat(secondResult.get(0).getPosition()).isEqualTo(1);
    }

    // --- getPasos ---

    @Test
    void getPasosReturnsListForOwnHermandad() {
        var procesion = aProcesion();
        var paso1 = Paso.create(procesionId, 1, titularId, null);
        var paso2 = Paso.create(procesionId, 2, titularId, null);

        when(procesionRepository.findById(procesionId)).thenReturn(Optional.of(procesion));
        when(pasoRepository.findByProcesionId(procesionId)).thenReturn(List.of(paso1, paso2));

        var result = pasoService.getPasos(hermandadId, procesionId);

        assertThat(result).hasSize(2);
    }

    @Test
    void getPasosRejectsCrossTenantAccess() {
        var otherHermandad = UUID.randomUUID();
        var otherProcesion = Procesion.create(otherHermandad, LocalDate.of(2026, 4, 13), LocalTime.of(18, 0));
        when(procesionRepository.findById(procesionId)).thenReturn(Optional.of(otherProcesion));

        assertThrows(ForbiddenException.class,
                () -> pasoService.getPasos(hermandadId, procesionId));
    }

    @Test
    void getPasosThrowsWhenProcesionNotFound() {
        when(procesionRepository.findById(procesionId)).thenReturn(Optional.empty());

        assertThrows(ProcesionNotFoundException.class,
                () -> pasoService.getPasos(hermandadId, procesionId));
    }
}
