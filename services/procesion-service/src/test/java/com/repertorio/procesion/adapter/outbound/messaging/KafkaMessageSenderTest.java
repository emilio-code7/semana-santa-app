package com.repertorio.procesion.adapter.outbound.messaging;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

@ExtendWith(MockitoExtension.class)
class KafkaMessageSenderTest {
    @Mock
    private KafkaTemplate<String, String> template;

    private final UUID aggregateId = UUID.randomUUID();
    private final UUID eventId = UUID.randomUUID();

    @Test
    void delegatesAndMapsSuccessfulResult() {
        when(template.send("events", aggregateId.toString(), "payload"))
                .thenReturn(CompletableFuture.completedFuture(null));

        assertNull(new KafkaMessageSender(template).send("events", aggregateId, eventId, "payload").join());
        verify(template).send("events", aggregateId.toString(), "payload");
    }

    @Test
    void propagatesSendFailure() {
        when(template.send("events", aggregateId.toString(), "payload"))
                .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("failed")));

        assertThrows(java.util.concurrent.CompletionException.class,
                () -> new KafkaMessageSender(template).send("events", aggregateId, eventId, "payload").join());
    }
}
