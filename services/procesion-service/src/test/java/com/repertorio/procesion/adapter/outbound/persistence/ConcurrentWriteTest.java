package com.repertorio.procesion.adapter.outbound.persistence;

import com.repertorio.common.JdbcIntegrationTestBase;
import com.repertorio.common.outbox.OutboxEventJpaRepository;
import com.repertorio.procesion.domain.model.Procesion;
import com.repertorio.procesion.domain.model.ProcesionStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

@SpringBootTest
class ConcurrentWriteTest extends JdbcIntegrationTestBase {

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () ->
                System.getenv().getOrDefault("IT_DATASOURCE_URL", "jdbc:postgresql://localhost:5434/procesion_db"));
        registry.add("spring.datasource.username", () ->
                System.getenv().getOrDefault("IT_DATASOURCE_USERNAME", "postgres"));
        registry.add("spring.datasource.password", () ->
                System.getenv().getOrDefault("IT_DATASOURCE_PASSWORD", "postgres"));
    }

    @Autowired
    private ProcesionRepositoryAdapter adapter;

    @Autowired
    private TransactionTemplate txTemplate;

    @MockitoBean
    private KafkaTemplate<String, String> kafkaTemplate;

    @MockitoBean
    private OutboxEventJpaRepository outboxEventJpaRepository;

    @Test
    void twoSuccessiveUpdatesSucceed() {
        var saved = adapter.save(Procesion.create(UUID.randomUUID(), LocalDate.of(2026, 4, 13), LocalTime.of(18, 0)));
        var id = saved.getId();

        var first = adapter.findById(id).orElseThrow();
        first.changeStatus(ProcesionStatus.IN_PROGRESS);
        adapter.save(first);

        var second = adapter.findById(id).orElseThrow();
        second.changeStatus(ProcesionStatus.COMPLETED);
        adapter.save(second);

        var finalState = adapter.findById(id).orElseThrow();
        assertThat(finalState.getStatus()).isEqualTo(ProcesionStatus.COMPLETED);
    }

    @Test
    void concurrentWritesToSameProcesionRejectStaleUpdate() throws Exception {
        var id = adapter.save(Procesion.create(UUID.randomUUID(), LocalDate.of(2026, 4, 13), LocalTime.of(18, 0))).getId();

        var start = new CyclicBarrier(2); // both load the aggregate (version 0) before anyone writes
        var go = new CyclicBarrier(2);    // the loser only saves once the winner holds the uncommitted row lock
        var pool = Executors.newFixedThreadPool(2);
        try {
            var first = pool.submit(() -> writeAndCommit(id, ProcesionStatus.IN_PROGRESS, start, go, 500));
            var second = pool.submit(() -> writeAndCommit(id, ProcesionStatus.CANCELLED, start, go, 0));

            assertThat(first.get()).as("first writer wins").isTrue();
            assertThat(second.get()).as("second writer loses the optimistic-lock race").isFalse();
        } finally {
            pool.shutdownNow();
        }
    }

    private boolean writeAndCommit(UUID id, ProcesionStatus status, CyclicBarrier start, CyclicBarrier go, long sleepMillis) {
        try {
            txTemplate.executeWithoutResult(statusTx -> {
                var domain = adapter.findById(id).orElseThrow();
                awaitBarrier(start);
                domain.changeStatus(status);
                if (sleepMillis > 0) {
                    adapter.save(domain); // winner: eager flush acquires the row lock (version 0 -> 1, uncommitted)
                    awaitBarrier(go);     // then release the loser so its flush collides on the held lock
                    sleep(sleepMillis);   // keep the lock held until the loser's flush is blocked on it
                } else {
                    awaitBarrier(go);     // loser: wait until the winner holds the row lock
                    adapter.save(domain); // flush collides after the winner commits
                }
            });
            return true;
        } catch (ObjectOptimisticLockingFailureException e) {
            return false;
        } catch (Exception e) {
            fail("unexpected failure", e);
            return false;
        }
    }

    private static void awaitBarrier(CyclicBarrier barrier) {
        try {
            barrier.await();
        } catch (InterruptedException | BrokenBarrierException e) {
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
