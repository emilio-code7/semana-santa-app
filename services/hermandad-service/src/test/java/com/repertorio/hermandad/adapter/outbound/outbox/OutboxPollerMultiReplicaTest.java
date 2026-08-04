package com.repertorio.hermandad.adapter.outbound.outbox;

import com.repertorio.common.JdbcIntegrationTestBase;
import com.repertorio.common.messaging.MessageSender;
import com.repertorio.common.outbox.OutboxEventJpaRepository;
import com.repertorio.common.outbox.OutboxPoller;
import com.repertorio.common.outbox.OutboxProperties;
import com.repertorio.hermandad.adapter.inbound.kafka.IdempotentEventConsumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.reset;

/**
 * Acceptance test for multi-replica safe outbox polling (issue #30): two raw poller
 * instances with distinct instance ids run against the same outbox table and must never
 * claim the same row. Runs against a real PostgreSQL (skips if unreachable).
 */
@SpringBootTest
class OutboxPollerMultiReplicaTest extends JdbcIntegrationTestBase {

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () ->
                System.getenv().getOrDefault("IT_DATASOURCE_URL", "jdbc:postgresql://localhost:5432/hermandad_db"));
        registry.add("spring.datasource.username", () ->
                System.getenv().getOrDefault("IT_DATASOURCE_USERNAME", "postgres"));
        registry.add("spring.datasource.password", () ->
                System.getenv().getOrDefault("IT_DATASOURCE_PASSWORD", "postgres"));
    }

    @Autowired
    private OutboxEventJpaRepository repo;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private IdempotentEventConsumer idempotentEventConsumer;

    // Replace the real @Scheduled poller so it cannot interfere with these assertions.
    @MockitoBean
    private OutboxPoller outboxPoller;

    // Capture sends instead of hitting Kafka.
    @MockitoBean
    private MessageSender messageSender;

    private final List<String> sentPayloads = Collections.synchronizedList(new ArrayList<>());

    @BeforeEach
    void cleanOutbox() {
        jdbcTemplate.execute("DELETE FROM outbox_event");
        sentPayloads.clear();
        stubSendCompleted();
    }

    @Test
    void postgresMustBeReachable() {
        // Prove the suite actually executed against PG — a silently skipped suite is not a pass.
        assertThat(isPostgresAvailable()).isTrue();
    }

    @Test
    void twoInstancesNeverClaimTheSameRow() throws InterruptedException {
        UUID[] aggregates = {UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()};
        int[] rowsPerAggregate = {3, 3, 2, 2};
        Instant base = Instant.parse("2026-01-01T00:00:00Z");
        List<String> payloads = new ArrayList<>();
        int seq = 0;
        for (int a = 0; a < aggregates.length; a++) {
            for (int r = 0; r < rowsPerAggregate[a]; r++) {
                String payload = "payload-" + seq;
                payloads.add(payload);
                seed(aggregates[a], payload, base.plusSeconds(seq));
                seq++;
            }
        }

        // Drain concurrently: each cycle claims at most the oldest row per aggregate, so run
        // concurrent cycles until every row has been sent. SKIP LOCKED + claim writes make
        // each row claimable by exactly one instance.
        for (int cycle = 0; cycle < 10 && sentPayloads.size() < payloads.size(); cycle++) {
            runConcurrently(poller("instance-a"), poller("instance-b"));
        }

        assertThat(sentPayloads).hasSize(payloads.size());
        assertThat(sentPayloads.stream().distinct()).hasSize(payloads.size());
        assertThat(sentPayloads).containsExactlyInAnyOrderElementsOf(payloads);
    }

    @Test
    void claimsOnlyOldestEligibleRowPerAggregate() {
        UUID aggregate = UUID.randomUUID();
        Instant base = Instant.parse("2026-02-01T00:00:00Z");
        seed(aggregate, "payload-oldest", base);
        seed(aggregate, "payload-middle", base.plusSeconds(5));
        seed(aggregate, "payload-newest", base.plusSeconds(10));

        OutboxPoller poller = poller("instance-a");
        poller.processPendingOutbox();
        assertThat(sentPayloads).containsExactly("payload-oldest");

        poller.processPendingOutbox();
        assertThat(sentPayloads).containsExactly("payload-oldest", "payload-middle");

        poller.processPendingOutbox();
        assertThat(sentPayloads).containsExactly("payload-oldest", "payload-middle", "payload-newest");
    }

    @Test
    void doesNotJumpAheadWhileAggregateIsClaimed() {
        UUID aggregate = UUID.randomUUID();
        Instant base = Instant.parse("2026-03-01T00:00:00Z");
        seed(aggregate, "payload-oldest", base);
        seed(aggregate, "payload-newest", base.plusSeconds(5));

        stubSendNeverCompleting();
        poller("instance-a").processPendingOutbox();
        assertThat(sentPayloads).containsExactly("payload-oldest");
        assertThat(claimedBy("payload-oldest")).isEqualTo("instance-a");

        // The aggregate has an active claim, so instance-b must not touch it at all.
        poller("instance-b").processPendingOutbox();
        assertThat(sentPayloads).containsExactly("payload-oldest");
        assertThat(claimedBy("payload-newest")).isNull();
        assertThat(processed("payload-newest")).isFalse();
    }

    @Test
    void expiredClaimIsReprocessedAfterTimeout() {
        String payload = "payload-expired";
        seed(UUID.randomUUID(), payload, Instant.parse("2026-04-01T00:00:00Z"));

        stubSendNeverCompleting();
        poller("instance-a").processPendingOutbox();
        assertThat(claimedBy(payload)).isEqualTo("instance-a");

        // Simulate a crashed claimer: its 30s claim timeout has passed.
        jdbcTemplate.update("UPDATE outbox_event SET claimed_at = ?",
                Timestamp.from(Instant.now().minusSeconds(31)));

        // The first send was a hanging attempt by the "crashed" instance — only count real sends.
        sentPayloads.clear();
        stubSendCompleted();
        poller("instance-b").processPendingOutbox();

        assertThat(sentPayloads).containsExactly(payload);
        assertThat(processed(payload)).isTrue();
        assertThat(claimedBy(payload)).isNull();
    }

    @Test
    void failuresBackOffAndBecomeTerminal() {
        String payload = "payload-fails";
        seed(UUID.randomUUID(), payload, Instant.parse("2026-05-01T00:00:00Z"));

        OutboxPoller retryPoller = new OutboxPoller(repo, messageSender,
                new TransactionTemplate(transactionManager),
                new OutboxProperties("instance-retry", Duration.ofSeconds(30), 2, Duration.ofSeconds(1), 100));

        AtomicInteger sendAttempts = new AtomicInteger();
        reset(messageSender);
        doAnswer(inv -> {
            sendAttempts.incrementAndGet();
            return CompletableFuture.failedFuture(new RuntimeException("boom"));
        }).when(messageSender).send(anyString(), any(UUID.class), any(UUID.class), eq(payload));

        Instant before = Instant.now();
        retryPoller.processPendingOutbox();
        Instant after = Instant.now();

        assertThat(sendAttempts.get()).isEqualTo(1);
        assertThat(retryCount(payload)).isEqualTo(1);
        assertThat(lastError(payload)).isEqualTo("boom");
        assertThat(terminal(payload)).isFalse();
        assertThat(processed(payload)).isFalse();
        assertThat(nextAttemptAt(payload)).isBetween(before.plusSeconds(1), after.plusSeconds(2));

        // Immediate re-run: still backed off (next_attempt_at in the future) -> not claimed.
        retryPoller.processPendingOutbox();
        assertThat(sendAttempts.get()).isEqualTo(1);
        assertThat(retryCount(payload)).isEqualTo(1);

        // Fast-forward past the backoff and run again: second failure becomes terminal.
        jdbcTemplate.update("UPDATE outbox_event SET next_attempt_at = now() - interval '1 second'");
        retryPoller.processPendingOutbox();
        assertThat(sendAttempts.get()).isEqualTo(2);
        assertThat(retryCount(payload)).isEqualTo(2);
        assertThat(terminal(payload)).isTrue();
        assertThat(processed(payload)).isFalse();

        // Terminal rows are never retried.
        retryPoller.processPendingOutbox();
        assertThat(sendAttempts.get()).isEqualTo(2);
    }

    private void seed(UUID aggregateId, String payload, Instant createdAt) {
        jdbcTemplate.update("""
                INSERT INTO outbox_event
                    (id, aggregate_type, aggregate_id, event_type, payload, created_at,
                     processed, event_id, occurred_at, schema_version, retry_count, terminal)
                VALUES (?, ?, ?, ?, ?, ?, FALSE, ?, ?, 1, 0, FALSE)
                """, UUID.randomUUID(), "hermandad", aggregateId, "Created", payload,
                Timestamp.from(createdAt), UUID.randomUUID(), Timestamp.from(createdAt));
    }

    private OutboxPoller poller(String instanceId) {
        return new OutboxPoller(repo, messageSender, new TransactionTemplate(transactionManager),
                new OutboxProperties(instanceId, Duration.ofSeconds(30), 5, Duration.ofSeconds(1), 100));
    }

    private void runConcurrently(OutboxPoller pollerA, OutboxPoller pollerB) throws InterruptedException {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(2);
        for (OutboxPoller poller : List.of(pollerA, pollerB)) {
            pool.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                try {
                    poller.processPendingOutbox();
                } finally {
                    done.countDown();
                }
            });
        }
        assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
        start.countDown();
        assertThat(done.await(30, TimeUnit.SECONDS)).isTrue();
        pool.shutdownNow();
    }

    private void stubSendCompleted() {
        reset(messageSender);
        doAnswer(inv -> {
            sentPayloads.add(inv.getArgument(3));
            return CompletableFuture.completedFuture(null);
        }).when(messageSender).send(anyString(), any(UUID.class), any(UUID.class), anyString());
    }

    private void stubSendNeverCompleting() {
        reset(messageSender);
        doAnswer(inv -> {
            sentPayloads.add(inv.getArgument(3));
            return new CompletableFuture<>();
        }).when(messageSender).send(anyString(), any(UUID.class), any(UUID.class), anyString());
    }

    private String claimedBy(String payload) {
        return jdbcTemplate.queryForObject(
                "SELECT claimed_by FROM outbox_event WHERE payload = ?", String.class, payload);
    }

    private Boolean processed(String payload) {
        return jdbcTemplate.queryForObject(
                "SELECT processed FROM outbox_event WHERE payload = ?", Boolean.class, payload);
    }

    private Integer retryCount(String payload) {
        return jdbcTemplate.queryForObject(
                "SELECT retry_count FROM outbox_event WHERE payload = ?", Integer.class, payload);
    }

    private String lastError(String payload) {
        return jdbcTemplate.queryForObject(
                "SELECT last_error FROM outbox_event WHERE payload = ?", String.class, payload);
    }

    private Boolean terminal(String payload) {
        return jdbcTemplate.queryForObject(
                "SELECT terminal FROM outbox_event WHERE payload = ?", Boolean.class, payload);
    }

    private Instant nextAttemptAt(String payload) {
        Timestamp ts = jdbcTemplate.queryForObject(
                "SELECT next_attempt_at FROM outbox_event WHERE payload = ?", Timestamp.class, payload);
        return ts == null ? null : ts.toInstant();
    }
}
