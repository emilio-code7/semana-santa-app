package com.repertorio.marcha.adapter.outbound.messaging;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    @Test
    void delegatesAndMapsSuccessfulResult() {
        when(template.send("events", "payload")).thenReturn(CompletableFuture.completedFuture(null));
        assertNull(new KafkaMessageSender(template).send("events", "payload").join());
        verify(template).send("events", "payload");
    }

    @Test
    void propagatesSendFailure() {
        when(template.send("events", "payload")).thenReturn(CompletableFuture.failedFuture(new IllegalStateException()));
        assertThrows(java.util.concurrent.CompletionException.class, () -> new KafkaMessageSender(template).send("events", "payload").join());
    }
}
