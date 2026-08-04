package com.repertorio.hermandad.adapter.inbound.kafka;

import com.repertorio.hermandad.adapter.outbound.events.ProcessedEventEntity;
import com.repertorio.hermandad.adapter.outbound.events.ProcessedEventJpaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IdempotentEventConsumerTest {

    @Mock
    private ProcessedEventJpaRepository processedEventRepository;

    // real ObjectMapper (spy only so Mockito can constructor-inject it)
    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private IdempotentEventConsumer consumer;

    @Captor
    private ArgumentCaptor<ProcessedEventEntity> entityCaptor;

    @Test
    void newEventIsProcessed() {
        UUID eventId = UUID.fromString("123e4567-e89b-42d3-a456-556642440000");
        var payload = "{\"eventId\":\"" + eventId
                + "\",\"id\":\"550e8400-e29b-41d4-a716-446655440000\",\"name\":\"Macarena\"}";

        when(processedEventRepository.existsById(eventId)).thenReturn(false);

        consumer.consume(payload);

        verify(processedEventRepository).save(entityCaptor.capture());
        assertThat(entityCaptor.getValue().getEventId()).isEqualTo(eventId);
        assertThat(entityCaptor.getValue().getConsumerName()).isEqualTo("hermandad-service");
        assertThat(entityCaptor.getValue().getProcessedAt()).isNotNull();
    }

    @Test
    void duplicateEventIsSkipped() {
        UUID eventId = UUID.fromString("123e4567-e89b-42d3-a456-556642440000");
        var payload = "{\"eventId\":\"" + eventId
                + "\",\"id\":\"550e8400-e29b-41d4-a716-446655440000\",\"name\":\"Macarena\"}";

        when(processedEventRepository.existsById(eventId)).thenReturn(true);

        consumer.consume(payload);

        verify(processedEventRepository, never()).save(any());
    }

    @Test
    void identicalPayloadDifferentEventIdAreBothProcessed() {
        UUID eventIdX = UUID.fromString("123e4567-e89b-42d3-a456-556642440000");
        UUID eventIdY = UUID.fromString("123e4567-e89b-42d3-a456-556642440001");
        var payloadX = "{\"eventId\":\"" + eventIdX
                + "\",\"id\":\"550e8400-e29b-41d4-a716-446655440000\",\"name\":\"Macarena\"}";
        var payloadY = "{\"eventId\":\"" + eventIdY
                + "\",\"id\":\"550e8400-e29b-41d4-a716-446655440000\",\"name\":\"Macarena\"}";

        when(processedEventRepository.existsById(eventIdX)).thenReturn(false);
        when(processedEventRepository.existsById(eventIdY)).thenReturn(false);

        consumer.consume(payloadX);
        consumer.consume(payloadY);

        verify(processedEventRepository, times(2)).save(entityCaptor.capture());
        List<ProcessedEventEntity> saved = entityCaptor.getAllValues();
        assertThat(saved).extracting(ProcessedEventEntity::getEventId)
                .containsExactly(eventIdX, eventIdY);
        assertThat(saved).extracting(ProcessedEventEntity::getConsumerName)
                .containsExactly("hermandad-service", "hermandad-service");
    }
}
