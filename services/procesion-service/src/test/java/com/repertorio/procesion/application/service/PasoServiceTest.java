package com.repertorio.procesion.application.service;

import com.repertorio.procesion.domain.model.ForbiddenException;
import com.repertorio.procesion.domain.model.KnownTitular;
import com.repertorio.procesion.domain.model.Paso;
import com.repertorio.procesion.domain.model.Procesion;
import com.repertorio.procesion.domain.model.ProcesionStatus;
import com.repertorio.procesion.domain.model.ProcesionNotFoundException;
import com.repertorio.procesion.domain.port.KnownTitularRepository;
import com.repertorio.procesion.domain.repository.PasoRepository;
import com.repertorio.procesion.domain.repository.ProcesionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

    private final UUID hermandadId = UUID.randomUUID();
    private final UUID procesionId = UUID.randomUUID();
    private final UUID titularId = UUID.randomUUID();

    private Procesion aProcesion() {
        return Procesion.reconstruct(procesionId, hermandadId,
                LocalDate.of(2026, 4, 13), LocalTime.of(18, 0),
                ProcesionStatus.PLANNED, null,
                java.time.Instant.now(), java.time.Instant.now());
    }

    @Test
    void getPasosReturnsList() {
        when(procesionRepository.findById(procesionId)).thenReturn(Optional.of(aProcesion()));
        when(pasoRepository.findByProcesionId(procesionId))
                .thenReturn(List.of(Paso.create(procesionId, 0, titularId, null)));

        var result = pasoService.getPasos(hermandadId, procesionId);

        assertThat(result).hasSize(1);
    }

    @Test
    void getPasosThrowsForbiddenOnCrossTenant() {
        var otherHermandad = UUID.randomUUID();
        var otherProcesion = Procesion.reconstruct(procesionId, otherHermandad,
                LocalDate.of(2026, 4, 13), LocalTime.of(18, 0),
                ProcesionStatus.PLANNED, null,
                java.time.Instant.now(), java.time.Instant.now());
        when(procesionRepository.findById(procesionId)).thenReturn(Optional.of(otherProcesion));

        assertThrows(ForbiddenException.class,
                () -> pasoService.getPasos(hermandadId, procesionId));
    }

    @Test
    void replacePasosSavesNewPasos() {
        var itemId = UUID.randomUUID();
        var items = List.of(
                new PasoService.PasoItem(itemId, 0, titularId, null),
                new PasoService.PasoItem(null, 1, titularId, "notes"));

        when(procesionRepository.findById(procesionId)).thenReturn(Optional.of(aProcesion()));
        when(knownTitularRepository.findById(titularId))
                .thenReturn(Optional.of(new KnownTitular(titularId, hermandadId, "Titular")));
        when(pasoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = pasoService.replacePasos(hermandadId, procesionId, items);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(itemId);
        assertThat(result.get(0).getPosition()).isEqualTo(0);
        assertThat(result.get(1).getNotes()).isEqualTo("notes");
        verify(pasoRepository).deleteByProcesionId(procesionId);
    }

    @Test
    void replacePasosRejectsDuplicatePositions() {
        var items = List.of(
                new PasoService.PasoItem(null, 0, titularId, null),
                new PasoService.PasoItem(null, 0, titularId, null));

        when(procesionRepository.findById(procesionId)).thenReturn(Optional.of(aProcesion()));
        // ponytail: no titular stub needed — duplicate check fails before titular validation

        assertThrows(IllegalArgumentException.class,
                () -> pasoService.replacePasos(hermandadId, procesionId, items));
    }

    @Test
    void replacePasosRejectsTitularFromOtherHermandad() {
        var otherTitularId = UUID.randomUUID();
        var items = List.of(new PasoService.PasoItem(null, 0, otherTitularId, null));

        when(procesionRepository.findById(procesionId)).thenReturn(Optional.of(aProcesion()));
        when(knownTitularRepository.findById(otherTitularId))
                .thenReturn(Optional.of(new KnownTitular(otherTitularId, UUID.randomUUID(), "Other")));

        assertThrows(ForbiddenException.class,
                () -> pasoService.replacePasos(hermandadId, procesionId, items));
    }

    @Test
    void replacePasosRejectsWhenPlanFinalized() {
        var finalizedProcesion = Procesion.reconstruct(procesionId, hermandadId,
                LocalDate.of(2026, 4, 13), LocalTime.of(18, 0),
                ProcesionStatus.PLANNED, java.time.Instant.now(),
                java.time.Instant.now(), java.time.Instant.now());

        when(procesionRepository.findById(procesionId)).thenReturn(Optional.of(finalizedProcesion));

        assertThrows(IllegalStateException.class,
                () -> pasoService.replacePasos(hermandadId, procesionId, List.of()));
    }
}
