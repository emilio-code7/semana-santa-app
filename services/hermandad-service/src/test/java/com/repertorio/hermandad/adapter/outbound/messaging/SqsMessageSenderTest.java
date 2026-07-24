package com.repertorio.hermandad.adapter.outbound.messaging;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.awspring.cloud.sqs.operations.SqsTemplate;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;

class SqsMessageSenderTest {
    @Test
    void delegatesAndMapsSuccessfulResult() {
        SqsTemplate template = org.mockito.Mockito.mock(SqsTemplate.class);
        when(template.sendAsync("events", "payload")).thenReturn(CompletableFuture.completedFuture(null));

        assertNull(new SqsMessageSender(template).send("events", "payload").join());
        verify(template).sendAsync("events", "payload");
    }

    @Test
    void propagatesSendFailure() {
        SqsTemplate template = org.mockito.Mockito.mock(SqsTemplate.class);
        when(template.sendAsync("events", "payload"))
                .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("failed")));

        assertThrows(java.util.concurrent.CompletionException.class,
                () -> new SqsMessageSender(template).send("events", "payload").join());
    }
}
