package com.repertorio.marcha.adapter.inbound.kafka;

import com.repertorio.marcha.application.event.ProcesionEventProcessor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProcesionEventConsumerTest {

    @Mock
    private ProcesionEventProcessor processor;

    @InjectMocks
    private ProcesionEventConsumer consumer;

    @Test
    void delegatesToProcessor() {
        var payload = "{\"id\":\"11111111-1111-1111-1111-111111111111\",\"hermandadId\":\"22222222-2222-2222-2222-222222222222\",\"date\":\"2026-04-09\",\"time\":\"18:00:00\"}";

        consumer.consume(payload);

        verify(processor).process(payload);
    }

    @Test
    void propagatesProcessorException() {
        var payload = "bad-json";
        doThrow(new RuntimeException("processing failed"))
                .when(processor).process(payload);

        assertThatThrownBy(() -> consumer.consume(payload))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("processing failed");
    }
}
