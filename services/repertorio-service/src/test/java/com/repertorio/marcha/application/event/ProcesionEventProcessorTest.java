package com.repertorio.marcha.application.event;

import com.repertorio.marcha.application.port.ProcessedEventStore;
import com.repertorio.marcha.domain.model.KnownProcesion;
import com.repertorio.marcha.domain.port.KnownProcesionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProcesionEventProcessorTest {

    @Mock
    private KnownProcesionRepository knownProcesionRepository;

    @Mock
    private ProcessedEventStore processedEventStore;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private ProcesionEventProcessor processor;

    @Captor
    private ArgumentCaptor<KnownProcesion> knownProcesionCaptor;

    private final ObjectMapper realMapper = new ObjectMapper();

    @Test
    void processesCreatedEvent() throws Exception {
        var payload = "{\"id\":\"11111111-1111-1111-1111-111111111111\",\"hermandadId\":\"22222222-2222-2222-2222-222222222222\",\"date\":\"2026-04-09\",\"time\":\"18:00:00\"}";
        var node = realMapper.readTree(payload);
        when(objectMapper.readTree(payload)).thenReturn(node);
        UUID eventId = UUID.nameUUIDFromBytes(payload.getBytes(StandardCharsets.UTF_8));
        when(processedEventStore.exists(eventId)).thenReturn(false);

        processor.process(payload);

        verify(knownProcesionRepository).save(knownProcesionCaptor.capture());
        var saved = knownProcesionCaptor.getValue();
        assertThat(saved.getProcesionId()).hasToString("11111111-1111-1111-1111-111111111111");
        assertThat(saved.getHermandadId()).hasToString("22222222-2222-2222-2222-222222222222");
        assertThat(saved.getStatus()).isEqualTo("PLANNED");
        verify(processedEventStore).record(eventId);
    }

    @Test
    void processesStatusChangedEvent() throws Exception {
        var payload = "{\"id\":\"11111111-1111-1111-1111-111111111111\",\"hermandadId\":\"22222222-2222-2222-2222-222222222222\",\"previousStatus\":\"PLANNED\",\"newStatus\":\"IN_PROGRESS\"}";
        var node = realMapper.readTree(payload);
        when(objectMapper.readTree(payload)).thenReturn(node);
        UUID eventId = UUID.nameUUIDFromBytes(payload.getBytes(StandardCharsets.UTF_8));
        when(processedEventStore.exists(eventId)).thenReturn(false);

        var procesionId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        var hermandadId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        var existing = new KnownProcesion(procesionId, hermandadId, "PLANNED");
        when(knownProcesionRepository.findByProcesionId(procesionId)).thenReturn(Optional.of(existing));

        processor.process(payload);

        assertThat(existing.getStatus()).isEqualTo("IN_PROGRESS");
        verify(knownProcesionRepository).save(existing);
        verify(processedEventStore).record(eventId);
    }

    @Test
    void skipsDuplicateEvent() throws Exception {
        var payload = "{\"id\":\"11111111-1111-1111-1111-111111111111\",\"hermandadId\":\"22222222-2222-2222-2222-222222222222\",\"date\":\"2026-04-09\"}";
        UUID eventId = UUID.nameUUIDFromBytes(payload.getBytes(StandardCharsets.UTF_8));
        when(processedEventStore.exists(eventId)).thenReturn(true);

        processor.process(payload);

        verifyNoInteractions(knownProcesionRepository);
        verify(objectMapper, never()).readTree(anyString());
        verify(processedEventStore, never()).record(any());
    }

    @Test
    void throwsOnMalformedJson() throws Exception {
        var payload = "not-valid-json";
        UUID eventId = UUID.nameUUIDFromBytes(payload.getBytes(StandardCharsets.UTF_8));
        when(processedEventStore.exists(eventId)).thenReturn(false);
        when(objectMapper.readTree(payload)).thenThrow(new RuntimeException("Invalid JSON"));

        assertThatThrownBy(() -> processor.process(payload))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to process procesion event");

        verifyNoInteractions(knownProcesionRepository);
        verify(processedEventStore, never()).record(any());
    }

    @Test
    void throwsOnMissingId() throws Exception {
        var payload = "{\"hermandadId\":\"22222222-2222-2222-2222-222222222222\",\"date\":\"2026-04-09\"}";
        var node = realMapper.readTree(payload);
        when(objectMapper.readTree(payload)).thenReturn(node);
        UUID eventId = UUID.nameUUIDFromBytes(payload.getBytes(StandardCharsets.UTF_8));
        when(processedEventStore.exists(eventId)).thenReturn(false);

        assertThatThrownBy(() -> processor.process(payload))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(knownProcesionRepository);
        verify(processedEventStore, never()).record(any());
    }

    @Test
    void throwsOnMissingHermandadIdForCreated() throws Exception {
        var payload = "{\"id\":\"11111111-1111-1111-1111-111111111111\",\"date\":\"2026-04-09\"}";
        var node = realMapper.readTree(payload);
        when(objectMapper.readTree(payload)).thenReturn(node);
        UUID eventId = UUID.nameUUIDFromBytes(payload.getBytes(StandardCharsets.UTF_8));
        when(processedEventStore.exists(eventId)).thenReturn(false);

        assertThatThrownBy(() -> processor.process(payload))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(knownProcesionRepository);
        verify(processedEventStore, never()).record(any());
    }

    @Test
    void throwsOnStatusChangeForUnknownProcesion() throws Exception {
        var payload = "{\"id\":\"11111111-1111-1111-1111-111111111111\",\"hermandadId\":\"22222222-2222-2222-2222-222222222222\",\"previousStatus\":\"PLANNED\",\"newStatus\":\"IN_PROGRESS\"}";
        var node = realMapper.readTree(payload);
        when(objectMapper.readTree(payload)).thenReturn(node);
        UUID eventId = UUID.nameUUIDFromBytes(payload.getBytes(StandardCharsets.UTF_8));
        when(processedEventStore.exists(eventId)).thenReturn(false);
        when(knownProcesionRepository.findByProcesionId(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> processor.process(payload))
                .isInstanceOf(IllegalArgumentException.class);

        verify(knownProcesionRepository, never()).save(any(KnownProcesion.class));
        verify(processedEventStore, never()).record(any());
    }

    @Test
    void throwsOnMissingNewStatusWhenFieldPresentButBlank() throws Exception {
        var payload = "{\"id\":\"11111111-1111-1111-1111-111111111111\",\"hermandadId\":\"22222222-2222-2222-2222-222222222222\",\"newStatus\":\"\"}";
        var node = realMapper.readTree(payload);
        when(objectMapper.readTree(payload)).thenReturn(node);
        UUID eventId = UUID.nameUUIDFromBytes(payload.getBytes(StandardCharsets.UTF_8));
        when(processedEventStore.exists(eventId)).thenReturn(false);

        assertThatThrownBy(() -> processor.process(payload))
                .isInstanceOf(IllegalArgumentException.class);

        verify(knownProcesionRepository, never()).save(any(KnownProcesion.class));
        verify(processedEventStore, never()).record(any());
    }

    @Test
    void rejectsNewStatusNullValue() throws Exception {
        var payload = "{\"id\":\"11111111-1111-1111-1111-111111111111\",\"hermandadId\":\"22222222-2222-2222-2222-222222222222\",\"newStatus\":null}";
        var node = realMapper.readTree(payload);
        when(objectMapper.readTree(payload)).thenReturn(node);
        UUID eventId = UUID.nameUUIDFromBytes(payload.getBytes(StandardCharsets.UTF_8));
        when(processedEventStore.exists(eventId)).thenReturn(false);

        assertThatThrownBy(() -> processor.process(payload))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("newStatus");

        verify(knownProcesionRepository, never()).save(any(KnownProcesion.class));
        verify(processedEventStore, never()).record(any());
    }

    @Test
    void rejectsNonTextualNewStatus() throws Exception {
        var payload = "{\"id\":\"11111111-1111-1111-1111-111111111111\",\"hermandadId\":\"22222222-2222-2222-2222-222222222222\",\"newStatus\":123}";
        var node = realMapper.readTree(payload);
        when(objectMapper.readTree(payload)).thenReturn(node);
        UUID eventId = UUID.nameUUIDFromBytes(payload.getBytes(StandardCharsets.UTF_8));
        when(processedEventStore.exists(eventId)).thenReturn(false);

        assertThatThrownBy(() -> processor.process(payload))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("newStatus");

        verify(knownProcesionRepository, never()).save(any(KnownProcesion.class));
        verify(processedEventStore, never()).record(any());
    }

    @Test
    void throwsOnMissingNewStatusInStatusPayload() throws Exception {
        var payload = "{\"id\":\"11111111-1111-1111-1111-111111111111\",\"hermandadId\":\"22222222-2222-2222-2222-222222222222\",\"previousStatus\":\"PLANNED\"}";
        var node = realMapper.readTree(payload);
        when(objectMapper.readTree(payload)).thenReturn(node);
        UUID eventId = UUID.nameUUIDFromBytes(payload.getBytes(StandardCharsets.UTF_8));
        when(processedEventStore.exists(eventId)).thenReturn(false);

        assertThatThrownBy(() -> processor.process(payload))
                .isInstanceOf(IllegalArgumentException.class);

        verify(knownProcesionRepository, never()).save(any(KnownProcesion.class));
        verify(processedEventStore, never()).record(any());
    }

    @Test
    void throwsOnMissingHermandadIdInStatusPayload() throws Exception {
        var payload = "{\"id\":\"11111111-1111-1111-1111-111111111111\",\"previousStatus\":\"PLANNED\",\"newStatus\":\"IN_PROGRESS\"}";
        var node = realMapper.readTree(payload);
        when(objectMapper.readTree(payload)).thenReturn(node);
        UUID eventId = UUID.nameUUIDFromBytes(payload.getBytes(StandardCharsets.UTF_8));
        when(processedEventStore.exists(eventId)).thenReturn(false);

        assertThatThrownBy(() -> processor.process(payload))
                .isInstanceOf(IllegalArgumentException.class);

        verify(knownProcesionRepository, never()).save(any(KnownProcesion.class));
        verify(processedEventStore, never()).record(any());
    }

    @Test
    void deduplicatesWithUtf8Encoding() {
        var payload = "{\"id\":\"11111111-1111-1111-1111-111111111111\",\"hermandadId\":\"22222222-2222-2222-2222-222222222222\",\"titulo\":\"Música\"}";
        UUID eventId = UUID.nameUUIDFromBytes(payload.getBytes(StandardCharsets.UTF_8));
        when(processedEventStore.exists(eventId)).thenReturn(true);

        processor.process(payload);

        verify(processedEventStore).exists(eventId);
        verifyNoInteractions(knownProcesionRepository);
        verify(objectMapper, never()).readTree(anyString());
        verify(processedEventStore, never()).record(any());
    }
}
