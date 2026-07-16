package com.repertorio.hermandad.adapter.inbound.sqs;

import com.repertorio.hermandad.adapter.outbound.events.ProcessedEventEntity;
import com.repertorio.hermandad.adapter.outbound.events.ProcessedEventJpaRepository;
import io.awspring.cloud.sqs.listener.acknowledgement.Acknowledgement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SqsEventConsumerTest {

    @Mock
    private ProcessedEventJpaRepository processedEventRepository;

    @Mock
    private Acknowledgement ack;

    private SqsEventConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new SqsEventConsumer(processedEventRepository);
    }

    @Test
    void processesNewEvent() {
        when(processedEventRepository.findById(any())).thenReturn(Optional.empty());

        consumer.consumeHermandadEvent("{\"event\":\"test\"}", ack);

        verify(processedEventRepository).save(any(ProcessedEventEntity.class));
        verify(ack).acknowledge();
    }

    @Test
    void skipsDuplicateEvent() {
        var eventId = UUID.nameUUIDFromBytes("{\"event\":\"test\"}".getBytes());
        when(processedEventRepository.findById(eventId)).thenReturn(Optional.of(mock(ProcessedEventEntity.class)));

        consumer.consumeHermandadEvent("{\"event\":\"test\"}", ack);

        verify(processedEventRepository, never()).save(any());
        verify(ack).acknowledge();
    }
}
