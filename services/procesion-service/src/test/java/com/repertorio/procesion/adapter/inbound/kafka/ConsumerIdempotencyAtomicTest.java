package com.repertorio.procesion.adapter.inbound.kafka;

import com.repertorio.common.JdbcIntegrationTestBase;
import com.repertorio.common.messaging.MessageSender;
import com.repertorio.common.outbox.OutboxPoller;
import com.repertorio.procesion.adapter.outbound.events.ProcessedEventJpaRepository;
import com.repertorio.procesion.application.event.TitularEventProcessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Acceptance test for issue #31: consumer dedup is an atomic claim
 * (INSERT ... ON CONFLICT DO NOTHING on (consumer_name, event_id)) shared with the
 * business mutation in one transaction. Runs against a real PostgreSQL (skips if unreachable).
 */
@SpringBootTest
class ConsumerIdempotencyAtomicTest extends JdbcIntegrationTestBase {

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () ->
                System.getenv().getOrDefault("IT_DATASOURCE_URL", "jdbc:postgresql://localhost:5432/procesion_test"));
        registry.add("spring.datasource.username", () ->
                System.getenv().getOrDefault("IT_DATASOURCE_USERNAME", "postgres"));
        registry.add("spring.datasource.password", () ->
                System.getenv().getOrDefault("IT_DATASOURCE_PASSWORD", "postgres"));
    }

    @Autowired
    private ProcessedEventJpaRepository repo;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TitularEventProcessor processor;

    @MockitoBean
    private TitularEventConsumer titularEventConsumer;

    @MockitoBean
    private KafkaTemplate<String, String> kafkaTemplate;

    // Replace the real @Scheduled poller so it cannot interfere with these assertions.
    @MockitoBean
    private OutboxPoller outboxPoller;

    // Capture sends instead of hitting Kafka.
    @MockitoBean
    private MessageSender messageSender;

    private TransactionTemplate transactionTemplate;

    @BeforeEach
    void clean() {
        transactionTemplate = new TransactionTemplate(transactionManager);
        jdbcTemplate.execute("DELETE FROM processed_event");
        jdbcTemplate.execute("DELETE FROM known_titular");
    }

    @Test
    void postgresMustBeReachable() {
        // Prove the suite actually executed against PG — a silently skipped suite is not a pass.
        assertThat(isPostgresAvailable()).isTrue();
    }

    @Test
    void concurrentClaimsOnlyOneWins() throws InterruptedException {
        UUID eventId = UUID.randomUUID();
        String consumerName = "procesion-service";
        AtomicInteger winners = new AtomicInteger();
        List<Throwable> failures = Collections.synchronizedList(new ArrayList<>());

        runConcurrently(8, () -> {
            try {
                Integer inserted = transactionTemplate.execute(status ->
                        repo.tryClaim(eventId, consumerName, Instant.now()));
                if (inserted != null && inserted == 1) {
                    winners.incrementAndGet();
                }
            } catch (Throwable t) {
                failures.add(t);
            }
        });

        assertThat(failures).isEmpty();
        assertThat(winners.get()).isEqualTo(1);
        assertThat(rowCount(eventId, consumerName)).isEqualTo(1);
    }

    @Test
    void duplicateClaimSkipsAfterFirstWins() {
        UUID eventId = UUID.randomUUID();
        String consumerName = "procesion-service";

        assertThat(repo.tryClaim(eventId, consumerName, Instant.now())).isEqualTo(1);
        assertThat(repo.tryClaim(eventId, consumerName, Instant.now())).isEqualTo(0);
        assertThat(rowCount(eventId, consumerName)).isEqualTo(1);
    }

    @Test
    void failedBusinessMutationRollsBackClaimAndRetries() {
        UUID eventId = UUID.randomUUID();
        UUID titularId = UUID.randomUUID();
        UUID hermandadId = UUID.randomUUID();

        // update for an unknown titular fails against the empty projection: the whole
        // transaction (claim + mutation) must roll back, leaving the event claimable
        var badPayload = """
                {"id":"%s","hermandadId":"%s","name":"New","eventId":"%s","eventType":"TITULAR_UPDATED"}
                """.formatted(titularId, hermandadId, eventId);

        assertThatThrownBy(() -> processor.process(badPayload))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Update for unknown titular");

        assertThat(rowCount(eventId, "procesion-service")).isZero();
        assertThat(titularCount(titularId)).isZero();

        // same eventId redelivered with a valid event: the retry claims and succeeds
        var goodPayload = """
                {"id":"%s","hermandadId":"%s","name":"Jesus del Gran Poder","eventId":"%s","eventType":"TITULAR_CREATED"}
                """.formatted(titularId, hermandadId, eventId);

        processor.process(goodPayload);

        assertThat(rowCount(eventId, "procesion-service")).isEqualTo(1);
        assertThat(titularCount(titularId)).isEqualTo(1);
    }

    private Long rowCount(UUID eventId, String consumerName) {
        return jdbcTemplate.queryForObject(
                "SELECT count(*) FROM processed_event WHERE event_id = ? AND consumer_name = ?",
                Long.class, eventId, consumerName);
    }

    private Long titularCount(UUID titularId) {
        return jdbcTemplate.queryForObject(
                "SELECT count(*) FROM known_titular WHERE id = ?", Long.class, titularId);
    }

    private void runConcurrently(int threads, Runnable task) throws InterruptedException {
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                try {
                    task.run();
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
}
