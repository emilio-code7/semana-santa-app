package com.repertorio.marcha.adapter.inbound.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.repertorio.marcha.adapter.outbound.events.ProcessedEventJpaRepository;
import com.repertorio.marcha.domain.model.KnownProcesion;
import com.repertorio.marcha.domain.port.KnownProcesionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProcesionEventConsumerTest {

    @Mock
    private KnownProcesionRepository knownProcesionRepository;

    @Mock
    private ProcessedEventJpaRepository processedEventRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private ProcesionEventConsumer consumer;

    @Captor
    private ArgumentCaptor<KnownProcesion> knownProcesionCaptor;

    private final ObjectMapper realMapper = new ObjectMapper();

    @Test
    void processesProcesionCreatedEvent() throws Exception {
        var payload = "{\"id\":\"11111111-1111-1111-1111-111111111111\",\"hermandadId\":\"22222222-2222-2222-2222-222222222222\",\"date\":\"2026-04-09\",\"time\":\"18:00:00\"}";
        JsonNode node = realMapper.readTree(payload);
        when(objectMapper.readTree(anyString())).thenReturn(node);
        when(processedEventRepository.existsById(any())).thenReturn(false);

        consumer.consume(payload);

        verify(knownProcesionRepository).save(knownProcesionCaptor.capture());
        KnownProcesion saved = knownProcesionCaptor.getValue();
        assertThat(saved.getProcesionId()).hasToString("11111111-1111-1111-1111-111111111111");
        assertThat(saved.getHermandadId()).hasToString("22222222-2222-2222-2222-222222222222");
        assertThat(saved.getStatus()).isEqualTo("PLANNED");
        assertThat(saved.getUpdatedAt()).isNotNull();
        verify(processedEventRepository).save(any());
    }

    @Test
    void processesProcesionStatusChangedEvent() throws Exception {
        var payload = "{\"id\":\"11111111-1111-1111-1111-111111111111\",\"hermandadId\":\"22222222-2222-2222-2222-222222222222\",\"previousStatus\":\"PLANNED\",\"newStatus\":\"IN_PROGRESS\"}";
        JsonNode node = realMapper.readTree(payload);
        when(objectMapper.readTree(anyString())).thenReturn(node);
        when(processedEventRepository.existsById(any())).thenReturn(false);

        UUID procesionId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID hermandadId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        KnownProcesion existing = new KnownProcesion(procesionId, hermandadId, "PLANNED");
        when(knownProcesionRepository.findByProcesionId(procesionId)).thenReturn(Optional.of(existing));

        consumer.consume(payload);

        assertThat(existing.getStatus()).isEqualTo("IN_PROGRESS");
        verify(knownProcesionRepository).save(existing);
        verify(processedEventRepository).save(any());
    }

    @Test
    void skipsDuplicateEvent() throws Exception {
        var payload = "{\"id\":\"11111111-1111-1111-1111-111111111111\",\"hermandadId\":\"22222222-2222-2222-2222-222222222222\",\"date\":\"2026-04-09\"}";
        UUID eventId = UUID.nameUUIDFromBytes(payload.getBytes());
        when(processedEventRepository.existsById(eventId)).thenReturn(true);

        consumer.consume(payload);

        verifyNoInteractions(knownProcesionRepository);
        verify(processedEventRepository, never()).save(any());
    }

    @Test
    void handlesInvalidJsonGracefully() throws Exception {
        var payload = "not-valid-json";
        when(objectMapper.readTree(anyString())).thenThrow(new JsonProcessingException("bad json") {});
        when(processedEventRepository.existsById(any())).thenReturn(false);

        consumer.consume(payload);

        verifyNoInteractions(knownProcesionRepository);
        verify(processedEventRepository, never()).save(any());
    }

    @Test
    void statusChangedForUnknownProcesion() throws Exception {
        var payload = "{\"id\":\"11111111-1111-1111-1111-111111111111\",\"hermandadId\":\"22222222-2222-2222-2222-222222222222\",\"previousStatus\":\"PLANNED\",\"newStatus\":\"IN_PROGRESS\"}";
        JsonNode node = realMapper.readTree(payload);
        when(objectMapper.readTree(anyString())).thenReturn(node);
        when(processedEventRepository.existsById(any())).thenReturn(false);
        when(knownProcesionRepository.findByProcesionId(any())).thenReturn(Optional.empty());

        consumer.consume(payload);

        verify(knownProcesionRepository, never()).save(any(KnownProcesion.class));
        verify(processedEventRepository).save(any());
    }
}
