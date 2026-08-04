package com.repertorio.common.outbox;

import com.repertorio.common.messaging.MessageSender;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

@ExtendWith(MockitoExtension.class)
class OutboxPollerTest {

    @Mock
    private OutboxEventJpaRepository repository;

    @Mock
    private MessageSender messageSender;

    @Mock
    private TransactionTemplate transactionTemplate;

    private final OutboxProperties properties = new OutboxProperties(
            "test-instance", Duration.ofSeconds(30), 5, Duration.ofSeconds(1), 100);

    @Test
    void claimsBatchThenMarksAndSavesEventAfterSuccessfulSend() {
        OutboxEventEntity event = event();
        stubTransactions();
        when(repository.claimEligible(any(Instant.class), any(Instant.class), any(Integer.class)))
                .thenReturn(List.of(event));
        // The claim must be persisted on the batch before the send happens.
        when(repository.saveAll(List.of(event))).thenAnswer(inv -> {
            assertEquals("test-instance", event.getClaimedBy());
            assertNotNull(event.getClaimedAt());
            return inv.getArgument(0);
        });
        when(messageSender.send("procesion-events", event.getAggregateId(), event.getEventId(), "payload"))
                .thenReturn(CompletableFuture.completedFuture(null));

        new OutboxPoller(repository, messageSender, transactionTemplate, properties).processPendingOutbox();

        assertTrue(event.getProcessed());
        // Claim is cleared by markAsProcessed once the event is sent.
        assertNull(event.getClaimedBy());
        assertNull(event.getClaimedAt());
        verify(repository).save(event);
    }

    @Test
    void forwardsRowMetadataAsTransportKeysWithoutParsingPayload() {
        OutboxEventEntity event = event();
        stubTransactions();
        when(repository.claimEligible(any(Instant.class), any(Instant.class), any(Integer.class)))
                .thenReturn(List.of(event));
        when(repository.saveAll(List.of(event))).thenReturn(List.of(event));
        when(messageSender.send("procesion-events", event.getAggregateId(), event.getEventId(), "payload"))
                .thenReturn(CompletableFuture.completedFuture(null));

        new OutboxPoller(repository, messageSender, transactionTemplate, properties).processPendingOutbox();

        // The poller forwards the row's aggregateId and eventId as transport metadata
        // (Kafka message key / SQS group + dedup) — it never parses the payload to recover them.
        verify(messageSender).send("procesion-events", event.getAggregateId(), event.getEventId(), "payload");
    }

    @Test
    void leavesEventUnprocessedAndRecordsRetryWhenSendFails() {
        OutboxEventEntity event = event();
        stubTransactions();
        when(repository.claimEligible(any(Instant.class), any(Instant.class), any(Integer.class)))
                .thenReturn(List.of(event));
        when(repository.saveAll(List.of(event))).thenReturn(List.of(event));
        when(messageSender.send("procesion-events", event.getAggregateId(), event.getEventId(), "payload"))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("send failed")));

        new OutboxPoller(repository, messageSender, transactionTemplate, properties).processPendingOutbox();

        assertFalse(event.getProcessed());
        assertEquals(1, event.getRetryCount());
        assertEquals("send failed", event.getLastError());
        assertNull(event.getClaimedBy());
        assertNull(event.getClaimedAt());
        verify(repository).save(event);
    }

    private void stubTransactions() {
        when(transactionTemplate.execute(any(TransactionCallback.class)))
                .thenAnswer(inv -> ((TransactionCallback<Object>) inv.getArgument(0)).doInTransaction(null));
        doAnswer(inv -> {
            ((Consumer<TransactionStatus>) inv.getArgument(0)).accept(null);
            return null;
        }).when(transactionTemplate).executeWithoutResult(any(Consumer.class));
    }

    private OutboxEventEntity event() {
        return new OutboxEventEntity("procesion", UUID.randomUUID(), "Created", "payload",
                UUID.randomUUID(), Instant.now(), 1);
    }
}
