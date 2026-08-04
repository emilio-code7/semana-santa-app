package com.repertorio.hermandad.adapter.inbound.kafka;

import com.repertorio.hermandad.adapter.outbound.events.ProcessedEventJpaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdempotentEventConsumerTest {

    @Mock
    private ProcessedEventJpaRepository processedEventRepository;

    // real ObjectMapper (spy only so Mockito can constructor-inject it)
    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private IdempotentEventConsumer consumer;

    @Test
    void newEventIsProcessed() {
        UUID eventId = UUID.fromString("123e4567-e89b-42d3-a456-556642440000");
        var payload = "{\"eventId\":\"" + eventId
                + "\",\"id\":\"550e8400-e29b-41d4-a716-446655440000\",\"name\":\"Macarena\"}";

        when(processedEventRepository.tryClaim(eq(eventId), eq("hermandad-service"), any(Instant.class)))
                .thenReturn(1);

        consumer.consume(payload);

        verify(processedEventRepository).tryClaim(eq(eventId), eq("hermandad-service"), any(Instant.class));
    }

    @Test
    void duplicateEventIsSkipped() {
        UUID eventId = UUID.fromString("123e4567-e89b-42d3-a456-556642440000");
        var payload = "{\"eventId\":\"" + eventId
                + "\",\"id\":\"550e8400-e29b-41d4-a716-446655440000\",\"name\":\"Macarena\"}";

        when(processedEventRepository.tryClaim(eq(eventId), eq("hermandad-service"), any(Instant.class)))
                .thenReturn(0);

        consumer.consume(payload);

        verify(processedEventRepository).tryClaim(eq(eventId), eq("hermandad-service"), any(Instant.class));
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

        when(processedEventRepository.tryClaim(eq(eventIdX), eq("hermandad-service"), any(Instant.class)))
                .thenReturn(1);
        when(processedEventRepository.tryClaim(eq(eventIdY), eq("hermandad-service"), any(Instant.class)))
                .thenReturn(1);

        consumer.consume(payloadX);
        consumer.consume(payloadY);

        verify(processedEventRepository).tryClaim(eq(eventIdX), eq("hermandad-service"), any(Instant.class));
        verify(processedEventRepository).tryClaim(eq(eventIdY), eq("hermandad-service"), any(Instant.class));
    }
}
