package com.repertorio.procesion.adapter.outbound.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.awspring.cloud.sqs.operations.SqsSendOptions;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class SqsMessageSenderTest {

    private final UUID aggregateId = UUID.randomUUID();
    private final UUID eventId = UUID.randomUUID();

    @Test
    void delegatesAndMapsSuccessfulResult() {
        SqsTemplate template = mock(SqsTemplate.class);
        when(template.sendAsync(any(Consumer.class))).thenReturn(CompletableFuture.completedFuture(null));

        assertNull(new SqsMessageSender(template).send("events", aggregateId, eventId, "payload").join());
    }

    @Test
    void keysMessageWithAggregateIdGroupAndEventIdDeduplication() {
        SqsTemplate template = mock(SqsTemplate.class);
        when(template.sendAsync(any(Consumer.class))).thenReturn(CompletableFuture.completedFuture(null));

        new SqsMessageSender(template).send("events", aggregateId, eventId, "payload").join();

        RecordingSendOptions options = new RecordingSendOptions();
        captureConfigurator(template).accept(options);
        assertEquals("events", options.queue);
        assertEquals("payload", options.payload);
        assertEquals(aggregateId.toString(), options.messageGroupId);
        assertEquals(eventId.toString(), options.messageDeduplicationId);
    }

    @Test
    void propagatesSendFailure() {
        SqsTemplate template = mock(SqsTemplate.class);
        when(template.sendAsync(any(Consumer.class)))
                .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("failed")));

        assertThrows(java.util.concurrent.CompletionException.class,
                () -> new SqsMessageSender(template).send("events", aggregateId, eventId, "payload").join());
    }

    @SuppressWarnings("unchecked")
    private Consumer<SqsSendOptions<String>> captureConfigurator(SqsTemplate template) {
        ArgumentCaptor<Consumer<SqsSendOptions<String>>> captor = ArgumentCaptor.forClass(Consumer.class);
        verify(template).sendAsync(captor.capture());
        return captor.getValue();
    }

    /** Records queue/payload/group/dedup so tests can assert what the sender configures. */
    private static class RecordingSendOptions implements SqsSendOptions<String> {
        String queue;
        String payload;
        String messageGroupId;
        String messageDeduplicationId;

        @Override
        public SqsSendOptions<String> queue(String queue) {
            this.queue = queue;
            return this;
        }

        @Override
        public SqsSendOptions<String> payload(String payload) {
            this.payload = payload;
            return this;
        }

        @Override
        public SqsSendOptions<String> header(String name, Object value) {
            return this;
        }

        @Override
        public SqsSendOptions<String> headers(Map<String, Object> headers) {
            return this;
        }

        @Override
        public SqsSendOptions<String> delaySeconds(Integer delaySeconds) {
            return this;
        }

        @Override
        public SqsSendOptions<String> messageGroupId(String messageGroupId) {
            this.messageGroupId = messageGroupId;
            return this;
        }

        @Override
        public SqsSendOptions<String> messageDeduplicationId(String messageDeduplicationId) {
            this.messageDeduplicationId = messageDeduplicationId;
            return this;
        }
    }
}
