package com.repertorio.marcha.adapter.outbound.persistence;

import com.repertorio.marcha.adapter.inbound.kafka.ProcesionEventConsumer;
import com.repertorio.common.outbox.OutboxEventJpaRepository;
import com.repertorio.marcha.domain.model.BandType;
import com.repertorio.common.JdbcIntegrationTestBase;
import com.repertorio.marcha.domain.model.Marcha;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * Integration test proving optimistic locking through the real MarchaRepositoryAdapter
 * against a real PostgreSQL database.
 * <p>
 * Requires a running PostgreSQL (default localhost:5433/repertorio_db, overridable via
 * IT_DATASOURCE_URL/IT_DATASOURCE_USERNAME/IT_DATASOURCE_PASSWORD). Skips if unreachable.
 * <p>
 * The adapter is called inside an explicit transaction (TransactionTemplate) exactly as
 * production does via the @Transactional MarchaService methods — a detached MarchaEntity
 * mutation followed by flush() is otherwise a silent no-op.
 */
@SpringBootTest
class ConcurrentWriteTest extends JdbcIntegrationTestBase {

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () ->
                System.getenv().getOrDefault("IT_DATASOURCE_URL", "jdbc:postgresql://localhost:5433/repertorio_db"));
        registry.add("spring.datasource.username", () ->
                System.getenv().getOrDefault("IT_DATASOURCE_USERNAME", "postgres"));
        registry.add("spring.datasource.password", () ->
                System.getenv().getOrDefault("IT_DATASOURCE_PASSWORD", "postgres"));
    }

    @Autowired
    private MarchaRepositoryAdapter adapter;

    @Autowired
    private TransactionTemplate txTemplate;

    @MockitoBean
    private KafkaTemplate<String, String> kafkaTemplate;

    @MockitoBean
    private OutboxEventJpaRepository outboxEventJpaRepository;

    @MockitoBean
    private ProcesionEventConsumer procesionEventConsumer;

    @Test
    void twoSuccessiveUpdatesSucceed() {
        var saved = adapter.save(Marcha.create("Original", "Composer", BandType.BANDA_PALIO, 300, null, null));
        var id = saved.getId();

        txTemplate.executeWithoutResult(statusTx -> {
            var first = adapter.findById(id).orElseThrow();
            first.update("First edit", "Composer A", BandType.AGRUPACION_MUSICAL, 400, 2000, null);
            adapter.save(first);

            var second = adapter.findById(id).orElseThrow();
            second.update("Second edit", "Composer B", BandType.BANDA_CORNETAS, 500, 2005, null);
            adapter.save(second);
        });

        var finalState = adapter.findById(id).orElseThrow();
        assertThat(finalState.getTitle()).isEqualTo("Second edit");
        assertThat(finalState.getComposer()).isEqualTo("Composer B");
    }

    @Test
    void concurrentWritesToSameMarchaRejectStaleUpdate() throws Exception {
        var id = adapter.save(Marcha.create("Concurrent", "Composer", BandType.BANDA_PALIO, 300, null, null)).getId();

        var barrier = new CyclicBarrier(2);
        var winnerCommitted = new CountDownLatch(1);
        var pool = Executors.newFixedThreadPool(2);
        try {
            var winner = pool.submit(() -> writeAndCommit(id, "First edit", barrier, winnerCommitted, true));
            var loser = pool.submit(() -> writeAndCommit(id, "Second edit", barrier, winnerCommitted, false));

            assertThat(winner.get()).as("first writer wins").isTrue();
            assertThat(loser.get()).as("second writer loses the optimistic-lock race").isFalse();
        } finally {
            pool.shutdownNow();
        }
    }

    private boolean writeAndCommit(UUID id, String title, CyclicBarrier barrier,
                                   CountDownLatch winnerCommitted, boolean isWinner) {
        try {
            txTemplate.executeWithoutResult(statusTx -> {
                var domain = adapter.findById(id).orElseThrow();
                await(barrier);
                domain.update(title, "Composer X", BandType.AGRUPACION_MUSICAL, 400, 2000, null);
                if (isWinner) {
                    adapter.save(domain);
                    sleep(500); // hold the uncommitted row lock until commit
                } else {
                    // stale write: this tx loaded version 0 before the barrier; flush only after
                    // the winner commits so the version pre-check still passes and the flush collides
                    await(winnerCommitted);
                    adapter.save(domain);
                }
            });
            return true;
        } catch (ObjectOptimisticLockingFailureException e) {
            return false;
        } catch (Exception e) {
            fail("unexpected failure", e);
            return false;
        } finally {
            if (isWinner) {
                winnerCommitted.countDown();
            }
        }
    }

    private static void await(CyclicBarrier barrier) {
        try {
            barrier.await();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }
}
