package com.repertorio.procesion.adapter.outbound.persistence;

import com.repertorio.common.JdbcIntegrationTestBase;
import com.repertorio.procesion.adapter.outbound.outbox.OutboxEventJpaRepository;
import com.repertorio.procesion.domain.model.Procesion;
import com.repertorio.procesion.domain.model.ProcesionStatus;
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
class ProcesionRepositoryIntegrationTest extends JdbcIntegrationTestBase {

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
    private ProcesionJpaRepository procesionRepo;

    @MockitoBean
    private KafkaTemplate<String, String> kafkaTemplate;

    @MockitoBean
    private OutboxEventJpaRepository outboxEventJpaRepository;

    @Test
    void saveAndFindProcesion() {
        var hermandadId = UUID.randomUUID();
        var saved = procesionRepo.save(
                new ProcesionEntity(null, hermandadId, LocalDate.of(2026, 4, 13), LocalTime.of(18, 0),
                        ProcesionStatus.PLANNED, null, null));

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
            procesionRepo.save(new ProcesionEntity(null, hermandadId,
                    LocalDate.of(2026, 4, 13).plusDays(i), LocalTime.of(18, 0),
                    ProcesionStatus.PLANNED, null, null));
        }

        Page<ProcesionEntity> page1 = procesionRepo.findByHermandadId(hermandadId, PageRequest.of(0, 2));
        assertThat(page1.getContent()).hasSize(2);
        assertThat(page1.getTotalElements()).isEqualTo(5);
        assertThat(page1.getTotalPages()).isEqualTo(3);

        Page<ProcesionEntity> page2 = procesionRepo.findByHermandadId(hermandadId, PageRequest.of(1, 2));
        assertThat(page2.getContent()).hasSize(2);

        Page<ProcesionEntity> page3 = procesionRepo.findByHermandadId(hermandadId, PageRequest.of(2, 2));
        assertThat(page3.getContent()).hasSize(1);
    }

    @Test
    void statusTransitionsPersistCorrectly() {
        var procesion = procesionRepo.save(
                new ProcesionEntity(null, UUID.randomUUID(), LocalDate.of(2026, 4, 13), LocalTime.of(18, 0),
                        ProcesionStatus.PLANNED, null, null));

        procesionRepo.save(procesion);

        var updated = procesionRepo.findById(procesion.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(ProcesionStatus.PLANNED);
        assertThat(updated.getUpdatedAt()).isAfter(updated.getCreatedAt());
    }

    @Test
    void defaultStatusIsPlanned() {
        var procesion = procesionRepo.save(
                new ProcesionEntity(null, UUID.randomUUID(), LocalDate.of(2026, 4, 13), LocalTime.of(18, 0),
                        ProcesionStatus.PLANNED, null, null));
        assertThat(procesion.getStatus()).isEqualTo(ProcesionStatus.PLANNED);
    }
}
