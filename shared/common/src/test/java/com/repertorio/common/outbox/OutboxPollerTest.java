package com.repertorio.common.outbox;

import com.repertorio.common.messaging.MessageSender;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OutboxPollerTest {

    @Mock
    private OutboxEventJpaRepository repository;

    @Mock
    private MessageSender messageSender;

    @Test
    void marksAndSavesEventAfterSuccessfulSend() {
        OutboxEventEntity event = event();
        when(repository.findTop100ByProcessedFalseOrderByCreatedAtAsc()).thenReturn(List.of(event));
        when(messageSender.send("procesion-events", event.getAggregateId(), event.getEventId(), "payload"))
                .thenReturn(CompletableFuture.completedFuture(null));

        new OutboxPoller(repository, messageSender).processPendingOutbox();

        assertTrue(event.getProcessed());
        verify(repository).save(event);
    }

    @Test
    void forwardsRowMetadataAsTransportKeysWithoutParsingPayload() {
        OutboxEventEntity event = event();
        when(repository.findTop100ByProcessedFalseOrderByCreatedAtAsc()).thenReturn(List.of(event));
        when(messageSender.send("procesion-events", event.getAggregateId(), event.getEventId(), "payload"))
                .thenReturn(CompletableFuture.completedFuture(null));

        new OutboxPoller(repository, messageSender).processPendingOutbox();

        // The poller forwards the row's aggregateId and eventId as transport metadata
        // (Kafka message key / SQS group + dedup) — it never parses the payload to recover them.
        verify(messageSender).send("procesion-events", event.getAggregateId(), event.getEventId(), "payload");
    }

    @Test
    void leavesEventPendingWhenSendFails() {
        OutboxEventEntity event = event();
        when(repository.findTop100ByProcessedFalseOrderByCreatedAtAsc()).thenReturn(List.of(event));
        when(messageSender.send("procesion-events", event.getAggregateId(), event.getEventId(), "payload"))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("send failed")));

        new OutboxPoller(repository, messageSender).processPendingOutbox();

        assertFalse(event.getProcessed());
        verify(repository, never()).save(event);
    }

    private OutboxEventEntity event() {
        return new OutboxEventEntity("procesion", UUID.randomUUID(), "Created", "payload",
                UUID.randomUUID(), Instant.now(), 1);
    }
}
