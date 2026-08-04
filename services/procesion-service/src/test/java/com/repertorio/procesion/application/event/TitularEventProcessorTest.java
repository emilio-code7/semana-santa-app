package com.repertorio.procesion.application.event;

import com.repertorio.procesion.application.port.ProcessedEventStore;
import com.repertorio.procesion.domain.model.KnownTitular;
import com.repertorio.procesion.domain.port.KnownTitularRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TitularEventProcessorTest {

    @Mock
    private KnownTitularRepository knownTitularRepository;

    @Mock
    private ProcessedEventStore processedEventStore;

    private TitularEventProcessor processor;

    @Captor
    private ArgumentCaptor<KnownTitular> titularCaptor;

    @Captor
    private ArgumentCaptor<UUID> eventIdCaptor;

    private final UUID titularId = UUID.randomUUID();
    private final UUID hermandadId = UUID.randomUUID();
    private final UUID eventId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        processor = new TitularEventProcessor(knownTitularRepository, processedEventStore, new ObjectMapper());
    }

    @Test
    void createdEventSavesKnownTitular() {
        var payload = """
                {"id":"%s","hermandadId":"%s","name":"Jesus del Gran Poder","eventId":"%s","eventType":"TITULAR_CREATED"}
                """.formatted(titularId, hermandadId, eventId);

        when(processedEventStore.claim(eventId)).thenReturn(true);
        when(knownTitularRepository.findById(titularId)).thenReturn(Optional.empty());
        when(knownTitularRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        processor.process(payload);

        verify(knownTitularRepository).save(titularCaptor.capture());
        assertThat(titularCaptor.getValue().getId()).isEqualTo(titularId);
        assertThat(titularCaptor.getValue().getHermandadId()).isEqualTo(hermandadId);
        assertThat(titularCaptor.getValue().getName()).isEqualTo("Jesus del Gran Poder");
        verify(processedEventStore).claim(eventId);
    }

    @Test
    void usesProducerEventId() {
        var payload = """
                {"id":"%s","hermandadId":"%s","name":"Jesus","eventId":"%s","eventType":"TITULAR_CREATED"}
                """.formatted(titularId, hermandadId, eventId);

        when(processedEventStore.claim(eventId)).thenReturn(true);
        when(knownTitularRepository.findById(titularId)).thenReturn(Optional.empty());
        when(knownTitularRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        processor.process(payload);

        verify(processedEventStore).claim(eventId);
    }

    @Test
    void updatedEventUpdatesKnownTitular() {
        var kt = new KnownTitular(titularId, hermandadId, "Old Name");
        var payload = """
                {"id":"%s","hermandadId":"%s","name":"New Name","eventId":"%s","eventType":"TITULAR_UPDATED"}
                """.formatted(titularId, hermandadId, eventId);

        when(processedEventStore.claim(eventId)).thenReturn(true);
        when(knownTitularRepository.findById(titularId)).thenReturn(Optional.of(kt));
        when(knownTitularRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        processor.process(payload);

        verify(knownTitularRepository).save(titularCaptor.capture());
        assertThat(titularCaptor.getValue().getName()).isEqualTo("New Name");
        verify(processedEventStore).claim(eventId);
    }

    @Test
    void duplicateEventIsSkipped() {
        var payload = """
                {"id":"%s","hermandadId":"%s","name":"Jesus","eventId":"%s","eventType":"TITULAR_CREATED"}
                """.formatted(titularId, hermandadId, eventId);

        when(processedEventStore.claim(eventId)).thenReturn(false);

        processor.process(payload);

        verify(knownTitularRepository, never()).save(any());
        verify(processedEventStore).claim(eventId);
    }

    @Test
    void updateForUnknownTitularThrows() {
        var payload = """
                {"id":"%s","hermandadId":"%s","name":"New","eventId":"%s","eventType":"TITULAR_UPDATED"}
                """.formatted(titularId, hermandadId, eventId);

        when(processedEventStore.claim(eventId)).thenReturn(true);
        when(knownTitularRepository.findById(titularId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> processor.process(payload))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Update for unknown titular");
    }

    @Test
    void malformedPayloadThrows() {
        var payload = "not-json";

        assertThatThrownBy(() -> processor.process(payload))
                .isInstanceOf(Exception.class);
    }

    @Test
    void unknownEventTypeThrows() {
        var payload = """
                {"id":"%s","hermandadId":"%s","name":"Jesus","eventId":"%s","eventType":"TITULAR_UNKNOWN"}
                """.formatted(titularId, hermandadId, eventId);

        when(processedEventStore.claim(eventId)).thenReturn(true);

        assertThatThrownBy(() -> processor.process(payload))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown titular event type");

        verify(knownTitularRepository, never()).save(any());
        verify(processedEventStore).claim(eventId);
    }

    @Test
    void missingEventIdThrows() {
        var payload = """
                {"id":"%s","hermandadId":"%s","name":"Jesus","eventType":"TITULAR_CREATED"}
                """.formatted(titularId, hermandadId);

        assertThatThrownBy(() -> processor.process(payload))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Missing required field: eventId");
    }

    @Test
    void rejectsCreateWithDifferentHermandadId() {
        var otherHermandadId = UUID.randomUUID();
        var existing = new KnownTitular(titularId, otherHermandadId, "Existing");
        var payload = """
                {"id":"%s","hermandadId":"%s","name":"Jesus","eventId":"%s","eventType":"TITULAR_CREATED"}
                """.formatted(titularId, hermandadId, eventId);

        when(processedEventStore.claim(eventId)).thenReturn(true);
        when(knownTitularRepository.findById(titularId)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> processor.process(payload))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("HermandadId mismatch");

        verify(knownTitularRepository, never()).save(any());
        verify(processedEventStore).claim(eventId);
    }

    @Test
    void acceptsCreateWithSameHermandadId() {
        var existing = new KnownTitular(titularId, hermandadId, "Existing Name");
        // force the existing updatedAt to be in the past for deterministic assertion
        var payload = """
                {"id":"%s","hermandadId":"%s","name":"Jesus del Gran Poder","eventId":"%s","eventType":"TITULAR_CREATED"}
                """.formatted(titularId, hermandadId, eventId);

        when(processedEventStore.claim(eventId)).thenReturn(true);
        when(knownTitularRepository.findById(titularId)).thenReturn(Optional.of(existing));
        when(knownTitularRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        processor.process(payload);

        verify(knownTitularRepository).save(titularCaptor.capture());
        assertThat(titularCaptor.getValue().getId()).isEqualTo(titularId);
        assertThat(titularCaptor.getValue().getHermandadId()).isEqualTo(hermandadId);
        assertThat(titularCaptor.getValue().getName()).isEqualTo("Jesus del Gran Poder");
        verify(processedEventStore).claim(eventId);
    }

    @Test
    void rejectsUpdateWithDifferentHermandadId() {
        var otherHermandadId = UUID.randomUUID();
        var existing = new KnownTitular(titularId, otherHermandadId, "Old Name");
        var payload = """
                {"id":"%s","hermandadId":"%s","name":"New Name","eventId":"%s","eventType":"TITULAR_UPDATED"}
                """.formatted(titularId, hermandadId, eventId);

        when(processedEventStore.claim(eventId)).thenReturn(true);
        when(knownTitularRepository.findById(titularId)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> processor.process(payload))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("HermandadId mismatch");

        verify(knownTitularRepository, never()).save(any());
        verify(processedEventStore).claim(eventId);
    }

    @Test
    void rejectsCreateWithMissingHermandadId() {
        var payload = """
                {"id":"%s","name":"Jesus","eventId":"%s","eventType":"TITULAR_CREATED"}
                """.formatted(titularId, eventId);

        when(processedEventStore.claim(eventId)).thenReturn(true);

        assertThatThrownBy(() -> processor.process(payload))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Missing required field: hermandadId");
    }

    @Test
    void rejectsUpdateWithMissingHermandadId() {
        var payload = """
                {"id":"%s","name":"New","eventId":"%s","eventType":"TITULAR_UPDATED"}
                """.formatted(titularId, eventId);

        when(processedEventStore.claim(eventId)).thenReturn(true);

        assertThatThrownBy(() -> processor.process(payload))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Missing required field: hermandadId");
    }
}
