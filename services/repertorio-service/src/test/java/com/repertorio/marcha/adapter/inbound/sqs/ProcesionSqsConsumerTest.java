package com.repertorio.marcha.adapter.inbound.sqs;

import com.repertorio.marcha.application.event.ProcesionEventProcessor;
import com.repertorio.marcha.application.port.ProcessedEventStore;
import com.repertorio.marcha.domain.port.KnownProcesionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProcesionSqsConsumerTest {

    @Mock
    private ProcesionEventProcessor processor;

    @Mock
    private ProcessedEventStore processedEventStore;

    @InjectMocks
    private ProcesionSqsConsumer consumer;

    private final ObjectMapper realMapper = new ObjectMapper();

    @Test
    void acknowledgesOnSuccess() {
        var payload = "{\"id\":\"11111111-1111-1111-1111-111111111111\",\"hermandadId\":\"22222222-2222-2222-2222-222222222222\",\"date\":\"2026-04-09\"}";
        var ack = mock(io.awspring.cloud.sqs.listener.acknowledgement.Acknowledgement.class);

        consumer.consume(payload, ack);

        verify(processor).process(payload);
        verify(ack).acknowledge();
    }

    @Test
    void acknowledgesDuplicateViaRealProcessor() throws Exception {
        var payload = "{\"id\":\"00000000-0000-0000-0000-000000000001\",\"hermandadId\":\"00000000-0000-0000-0000-000000000002\"}";
        var ack = mock(io.awspring.cloud.sqs.listener.acknowledgement.Acknowledgement.class);
        var knownRepo = mock(KnownProcesionRepository.class);

        UUID eventId = UUID.nameUUIDFromBytes(payload.getBytes(StandardCharsets.UTF_8));
        when(processedEventStore.exists(eventId)).thenReturn(true);

        var realProcessor = new ProcesionEventProcessor(knownRepo, processedEventStore, realMapper);
        var consumerWithRealProcessor = new ProcesionSqsConsumer(realProcessor);

        consumerWithRealProcessor.consume(payload, ack);

        verify(processedEventStore).exists(eventId);
        verifyNoInteractions(knownRepo);
        verify(ack).acknowledge();
    }

    @Test
    void doesNotAcknowledgeAndRethrowsOnProcessorFailure() {
        var payload = "bad-json";
        doThrow(new RuntimeException("processing failed"))
                .when(processor).process(payload);
        var ack = mock(io.awspring.cloud.sqs.listener.acknowledgement.Acknowledgement.class);

        assertThatThrownBy(() -> consumer.consume(payload, ack))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("processing failed");

        verify(processor).process(payload);
        verify(ack, never()).acknowledge();
    }
}
