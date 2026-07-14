package com.repertorio.procesion.adapter.outbound.persistence;

import com.repertorio.procesion.adapter.outbound.outbox.OutboxEventJpaRepository;
import com.repertorio.procesion.domain.model.Procesion;
import com.repertorio.procesion.domain.model.ProcesionStatus;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ProcesionRepositoryIntegrationTest {

    private static final String POSTGRES_JDBC = System.getenv().getOrDefault(
            "IT_DATASOURCE_URL", "jdbc:postgresql://localhost:5434/procesion_db");
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
    private ProcesionJpaRepository procesionRepo;

    @MockitoBean
    private KafkaTemplate<String, String> kafkaTemplate;

    @MockitoBean
    private OutboxEventJpaRepository outboxEventJpaRepository;

    @Test
    void saveAndFindProcesion() {
        var hermandadId = UUID.randomUUID();
        var procesion = Procesion.create(hermandadId, LocalDate.of(2026, 4, 13), LocalTime.of(18, 0));
        var saved = procesionRepo.save(procesion);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getHermandadId()).isEqualTo(hermandadId);
        assertThat(saved.getStatus()).isEqualTo(ProcesionStatus.PLANNED);
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();

        var found = procesionRepo.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getDate()).isEqualTo(LocalDate.of(2026, 4, 13));
        assertThat(found.get().getTime()).isEqualTo(LocalTime.of(18, 0));
    }

    @Test
    void findByHermandadIdReturnsPagedResults() {
        var hermandadId = UUID.randomUUID();
        for (int i = 0; i < 5; i++) {
            procesionRepo.save(Procesion.create(hermandadId,
                    LocalDate.of(2026, 4, 13).plusDays(i), LocalTime.of(18, 0)));
        }

        Page<Procesion> page1 = procesionRepo.findByHermandadId(hermandadId, PageRequest.of(0, 2));
        assertThat(page1.getContent()).hasSize(2);
        assertThat(page1.getTotalElements()).isEqualTo(5);
        assertThat(page1.getTotalPages()).isEqualTo(3);

        Page<Procesion> page2 = procesionRepo.findByHermandadId(hermandadId, PageRequest.of(1, 2));
        assertThat(page2.getContent()).hasSize(2);

        Page<Procesion> page3 = procesionRepo.findByHermandadId(hermandadId, PageRequest.of(2, 2));
        assertThat(page3.getContent()).hasSize(1);
    }

    @Test
    void statusTransitionsPersistCorrectly() {
        var procesion = procesionRepo.save(
                Procesion.create(UUID.randomUUID(), LocalDate.of(2026, 4, 13), LocalTime.of(18, 0)));

        procesion.changeStatus(ProcesionStatus.IN_PROGRESS);
        procesionRepo.save(procesion);

        var updated = procesionRepo.findById(procesion.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(ProcesionStatus.IN_PROGRESS);
        assertThat(updated.getUpdatedAt()).isAfter(updated.getCreatedAt());
    }

    @Test
    void defaultStatusIsPlanned() {
        var procesion = procesionRepo.save(
                Procesion.create(UUID.randomUUID(), LocalDate.of(2026, 4, 13), LocalTime.of(18, 0)));
        assertThat(procesion.getStatus()).isEqualTo(ProcesionStatus.PLANNED);
    }
}
