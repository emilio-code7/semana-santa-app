package com.repertorio.common.outbox;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OutboxEventEntityTest {

    private final Instant fixedNow = Instant.parse("2026-08-04T10:00:00Z");

    private OutboxEventEntity entity() {
        return new OutboxEventEntity("procesion", UUID.randomUUID(), "Created", "payload",
                UUID.randomUUID(), fixedNow, 1);
    }

    @Test
    void constructorDefaultsRetryAndTerminalMetadata() {
        OutboxEventEntity e = entity();

        assertEquals(0, e.getRetryCount());
        assertFalse(e.isTerminal());
        assertNull(e.getClaimedBy());
        assertNull(e.getClaimedAt());
        assertNull(e.getNextAttemptAt());
        assertNull(e.getLastError());
    }

    @Test
    void claimSetsInstanceIdAndTimestamp() {
        OutboxEventEntity e = entity();

        e.claim("instance-a", fixedNow);

        assertEquals("instance-a", e.getClaimedBy());
        assertEquals(fixedNow, e.getClaimedAt());
    }

    @Test
    void clearClaimReleasesBothFields() {
        OutboxEventEntity e = entity();
        e.claim("instance-a", fixedNow);

        e.clearClaim();

        assertNull(e.getClaimedBy());
        assertNull(e.getClaimedAt());
    }

    @Test
    void markAsProcessedSetsProcessedAtAndClearsClaim() {
        OutboxEventEntity e = entity();
        e.claim("instance-a", fixedNow);

        e.markAsProcessed();

        assertTrue(e.getProcessed());
        assertNotNull(e.getProcessedAt());
        assertNull(e.getClaimedBy());
        assertNull(e.getClaimedAt());
    }

    @Test
    void recordFailureComputesExponentialBackoffForFirstRetries() {
        OutboxEventEntity e = entity();
        e.claim("instance-a", fixedNow);

        e.recordFailure("boom", fixedNow, Duration.ofSeconds(1), 5);
        assertEquals(1, e.getRetryCount());
        assertEquals("boom", e.getLastError());
        assertNull(e.getClaimedBy());
        assertNull(e.getClaimedAt());
        assertEquals(fixedNow.plusSeconds(1), e.getNextAttemptAt());

        e.recordFailure("boom", fixedNow, Duration.ofSeconds(1), 5);
        assertEquals(2, e.getRetryCount());
        assertEquals(fixedNow.plusSeconds(2), e.getNextAttemptAt());

        e.recordFailure("boom", fixedNow, Duration.ofSeconds(1), 5);
        assertEquals(3, e.getRetryCount());
        assertEquals(fixedNow.plusSeconds(4), e.getNextAttemptAt());
    }

    @Test
    void maxRetryFailureMarksTerminalWithoutNextAttempt() {
        OutboxEventEntity e = entity();

        for (int i = 1; i <= 5; i++) {
            e.recordFailure("boom", fixedNow, Duration.ofSeconds(1), 5);
        }

        assertEquals(5, e.getRetryCount());
        assertTrue(e.isTerminal());
        assertNull(e.getNextAttemptAt());
    }
}
