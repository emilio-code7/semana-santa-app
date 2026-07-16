package com.repertorio.marcha.application.service;

import com.repertorio.marcha.application.port.DomainEventPublisher;
import com.repertorio.marcha.domain.event.MarchaAddedEvent;
import com.repertorio.marcha.domain.event.MarchaRemovedEvent;
import com.repertorio.marcha.domain.model.BandType;
import com.repertorio.marcha.domain.model.Marcha;
import com.repertorio.marcha.domain.model.MarchaNotFoundException;
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

@ExtendWith(MockitoExtension.class)
class MarchaServiceTest {

    @Mock
    MarchaRepository marchaRepository;
    @Mock
    DomainEventPublisher eventPublisher;
    @InjectMocks
    MarchaService marchaService;

    @Test
    void createMarcha_savesAndPublishesEvent() {
        when(marchaRepository.save(any(Marcha.class))).thenAnswer(inv -> inv.getArgument(0));
        var marcha = marchaService.createMarcha("Amarguras", "Manuel López Farfán",
                BandType.BANDA_PALIO, 420, 1919, null);

        assertNotNull(marcha.getId());
        assertEquals("Amarguras", marcha.getTitle());
        verify(marchaRepository).save(any(Marcha.class));
        verify(eventPublisher).publish(any(MarchaAddedEvent.class));
    }

    @Test
    void getMarcha_returnsWhenFound() {
        var id = UUID.randomUUID();
        var marcha = Marcha.create("Test", "Composer", BandType.AGRUPACION_MUSICAL, 120, null, null);
        when(marchaRepository.findById(id)).thenReturn(Optional.of(marcha));

        var result = marchaService.getMarcha(id);

        assertTrue(result.isPresent());
        assertEquals(marcha.getTitle(), result.get().getTitle());
    }

    @Test
    void getMarcha_returnsEmptyWhenNotFound() {
        var id = UUID.randomUUID();
        when(marchaRepository.findById(id)).thenReturn(Optional.empty());

        var result = marchaService.getMarcha(id);

        assertTrue(result.isEmpty());
    }

    @Test
    void listMarchas_returnsAll() {
        var marchas = List.of(
                Marcha.create("A", "C1", BandType.BANDA_PALIO, 120, null, null),
                Marcha.create("B", "C2", BandType.BANDA_CORNETAS, 180, null, null)
        );
        when(marchaRepository.findAll()).thenReturn(marchas);

        var result = marchaService.listMarchas();

        assertEquals(2, result.size());
        verify(marchaRepository).findAll();
    }

    @Test
    void updateMarcha_updatesAndSaves() {
        var id = UUID.randomUUID();
        var original = Marcha.create("Original", "Composer", BandType.BANDA_PALIO, 120, null, null);
        when(marchaRepository.findById(id)).thenReturn(Optional.of(original));
        when(marchaRepository.save(any(Marcha.class))).thenAnswer(inv -> inv.getArgument(0));

        var updated = marchaService.updateMarcha(id, "Updated", "NewComposer",
                BandType.AGRUPACION_MUSICAL, 240, 2000, "https://youtu.be/test");

        assertEquals("Updated", updated.getTitle());
        assertEquals("NewComposer", updated.getComposer());
        verify(marchaRepository).save(original);
    }

    @Test
    void updateMarcha_throwsWhenNotFound() {
        var id = UUID.randomUUID();
        when(marchaRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(MarchaNotFoundException.class, () ->
                marchaService.updateMarcha(id, "X", "Y", BandType.BANDA_PALIO, 120, null, null));
    }

    @Test
    void deleteMarcha_deletesAndPublishesEvent() {
        var id = UUID.randomUUID();
        var marcha = Marcha.create("Test", "Composer", BandType.BANDA_CORNETAS, 180, null, null);
        when(marchaRepository.findById(id)).thenReturn(Optional.of(marcha));

        marchaService.deleteMarcha(id);

        verify(marchaRepository).deleteById(id);
        verify(eventPublisher).publish(any(MarchaRemovedEvent.class));
    }

    @Test
    void deleteMarcha_throwsWhenNotFound() {
        var id = UUID.randomUUID();
        when(marchaRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(MarchaNotFoundException.class, () -> marchaService.deleteMarcha(id));
    }

    @Test
    void search_returnsMatchesByTitleOrComposer() {
        var match = Marcha.create("El Amor de Dios", "Manuel López Farfán", BandType.BANDA_PALIO, 420, null, null);
        when(marchaRepository.findByTitleContainingIgnoreCaseOrComposerContainingIgnoreCase("amor", "amor"))
                .thenReturn(List.of(match));

        var result = marchaService.search("amor");

        assertEquals(1, result.size());
        assertEquals("El Amor de Dios", result.get(0).getTitle());
        verify(marchaRepository).findByTitleContainingIgnoreCaseOrComposerContainingIgnoreCase("amor", "amor");
    }

    @Test
    void search_returnsEmptyWhenNoMatches() {
        when(marchaRepository.findByTitleContainingIgnoreCaseOrComposerContainingIgnoreCase("zzzzz", "zzzzz"))
                .thenReturn(List.of());

        var result = marchaService.search("zzzzz");

        assertTrue(result.isEmpty());
    }
}
