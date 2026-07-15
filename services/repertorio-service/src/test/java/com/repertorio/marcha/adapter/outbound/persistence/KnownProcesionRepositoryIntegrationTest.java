package com.repertorio.marcha.adapter.outbound.persistence;

import com.repertorio.marcha.adapter.inbound.kafka.ProcesionEventConsumer;
import com.repertorio.marcha.adapter.outbound.outbox.OutboxEventJpaRepository;
import com.repertorio.marcha.domain.model.KnownProcesion;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for KnownProcesionJpaRepository against a real PostgreSQL database.
 * <p>
 * Requires a running PostgreSQL on localhost:5433 with database "repertorio_db"
 * and user "postgres". The app's docker-compose provides this.
 * Skips automatically if PostgreSQL is unreachable.
 */
@SpringBootTest
class KnownProcesionRepositoryIntegrationTest {

    private static final String POSTGRES_JDBC = System.getenv().getOrDefault(
            "IT_DATASOURCE_URL", "jdbc:postgresql://localhost:5433/repertorio_db");
    private static final String POSTGRES_USER = System.getenv().getOrDefault(
            "IT_DATASOURCE_USERNAME", "postgres");
    private static final String POSTGRES_PASS = System.getenv().getOrDefault(
            "IT_DATASOURCE_PASSWORD", "postgres");

    @BeforeAll
    static void checkPostgresRunning() {
        try (var c = java.sql.DriverManager.getConnection(POSTGRES_JDBC, POSTGRES_USER, POSTGRES_PASS)) {
            // connection OK
        } catch (Exception e) {
            org.junit.jupiter.api.Assumptions.assumeTrue(false,
                    "PostgreSQL not reachable at " + POSTGRES_JDBC + " — skipping integration test");
        }
    }

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> POSTGRES_JDBC);
        registry.add("spring.datasource.username", () -> POSTGRES_USER);
        registry.add("spring.datasource.password", () -> POSTGRES_PASS);
    }

    @Autowired
    private KnownProcesionJpaRepository repo;

    @MockitoBean
    private KafkaTemplate<String, String> kafkaTemplate;

    @MockitoBean
    private OutboxEventJpaRepository outboxEventJpaRepository;

    @MockitoBean
    private ProcesionEventConsumer procesionEventConsumer;

    @Test
    void saveAndFindByProcesionId() {
        var procesionId = UUID.randomUUID();
        var hermandadId = UUID.randomUUID();
        var entity = KnownProcesionEntity.from(new KnownProcesion(procesionId, hermandadId, "PLANNED"));
        repo.save(entity);

        var found = repo.findById(procesionId);
        assertThat(found).isPresent();
        assertThat(found.get().getHermandadId()).isEqualTo(hermandadId);
        assertThat(found.get().getStatus()).isEqualTo("PLANNED");
    }

    @Test
    void existsByProcesionIdReturnsTrueAfterSave() {
        var procesionId = UUID.randomUUID();
        var entity = KnownProcesionEntity.from(new KnownProcesion(procesionId, UUID.randomUUID(), "PLANNED"));
        repo.save(entity);

        assertThat(repo.existsByProcesionId(procesionId)).isTrue();
    }

    @Test
    void existsByProcesionIdReturnsFalseForUnknown() {
        assertThat(repo.existsByProcesionId(UUID.randomUUID())).isFalse();
    }
}
