package com.repertorio.hermandad.adapter.inbound.kafka;

import com.repertorio.hermandad.adapter.outbound.events.ProcessedEventEntity;
import com.repertorio.hermandad.adapter.outbound.events.ProcessedEventJpaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IdempotentEventConsumerTest {

    @Mock
    private ProcessedEventJpaRepository processedEventRepository;

    @InjectMocks
    private IdempotentEventConsumer consumer;

    @Captor
    private ArgumentCaptor<ProcessedEventEntity> entityCaptor;

    @Test
    void newEventIsProcessed() {
        var payload = "{\"id\":\"550e8400-e29b-41d4-a716-446655440000\",\"name\":\"Macarena\"}";
        UUID expectedEventId = UUID.nameUUIDFromBytes(payload.getBytes());

        when(processedEventRepository.existsById(expectedEventId)).thenReturn(false);

        consumer.consume(payload);

        verify(processedEventRepository).save(entityCaptor.capture());
        assertThat(entityCaptor.getValue().getEventId()).isEqualTo(expectedEventId);
        assertThat(entityCaptor.getValue().getConsumerName()).isEqualTo("hermandad-service");
        assertThat(entityCaptor.getValue().getProcessedAt()).isNotNull();
    }

    @Test
    void duplicateEventIsSkipped() {
        var payload = "{\"id\":\"550e8400-e29b-41d4-a716-446655440000\",\"name\":\"Macarena\"}";
        UUID expectedEventId = UUID.nameUUIDFromBytes(payload.getBytes());

        when(processedEventRepository.existsById(expectedEventId)).thenReturn(true);

        consumer.consume(payload);

        verify(processedEventRepository, never()).save(any());
    }

    @Test
    void samePayloadProducesSameEventId() {
        var payload = "{\"id\":\"550e8400-e29b-41d4-a716-446655440000\",\"name\":\"Macarena\"}";
        UUID id1 = UUID.nameUUIDFromBytes(payload.getBytes());
        UUID id2 = UUID.nameUUIDFromBytes(payload.getBytes());
        assertThat(id1).isEqualTo(id2);
    }
}
