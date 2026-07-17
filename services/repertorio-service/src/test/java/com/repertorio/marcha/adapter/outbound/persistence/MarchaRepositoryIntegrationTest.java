package com.repertorio.marcha.adapter.outbound.persistence;

import com.repertorio.marcha.adapter.inbound.kafka.ProcesionEventConsumer;
import com.repertorio.common.outbox.OutboxEventJpaRepository;
import com.repertorio.marcha.domain.model.BandType;
import com.repertorio.common.JdbcIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for MarchaJpaRepository against a real PostgreSQL database.
 * <p>
 * Requires a running PostgreSQL on localhost:5433 with database "repertorio_db"
 * and user "postgres". The app's docker-compose provides this.
 * Skips automatically if PostgreSQL is unreachable.
 */
@SpringBootTest
class MarchaRepositoryIntegrationTest extends JdbcIntegrationTestBase {

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
    private MarchaJpaRepository marchaRepo;

    @MockitoBean
    private KafkaTemplate<String, String> kafkaTemplate;

    @MockitoBean
    private OutboxEventJpaRepository outboxEventJpaRepository;

    @MockitoBean
    private ProcesionEventConsumer procesionEventConsumer;

    private MarchaEntity createMarcha(String title) {
        return new MarchaEntity(UUID.randomUUID(), title, "Test Composer",
                BandType.BANDA_PALIO, 300, null, null,
                Instant.now(), Instant.now());
    }

    @Test
    void saveAndFindById() {
        var entity = createMarcha("IT-Marcha-" + System.nanoTime());
        var saved = marchaRepo.save(entity);

        assertThat(saved.getId()).isNotNull();

        var found = marchaRepo.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getTitle()).isEqualTo(saved.getTitle());
    }

    @Test
    void findAllReturnsMultiple() {
        marchaRepo.save(createMarcha("IT-List-A-" + System.nanoTime()));
        marchaRepo.save(createMarcha("IT-List-B-" + System.nanoTime()));

        var all = marchaRepo.findAll();
        assertThat(all).hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    void deleteRemovesEntity() {
        var entity = createMarcha("IT-Delete-" + System.nanoTime());
        var saved = marchaRepo.save(entity);

        marchaRepo.deleteById(saved.getId());

        assertThat(marchaRepo.findById(saved.getId())).isEmpty();
    }

    @Test
    void persistAndRetrieveAllFields() {
        var id = UUID.randomUUID();
        var now = Instant.now();
        var entity = new MarchaEntity(id, "IT-Full-" + System.nanoTime(), "Composer Name",
                BandType.AGRUPACION_MUSICAL, 420, 2000, "https://youtube.com/test",
                now, now);
        marchaRepo.save(entity);

        var found = marchaRepo.findById(id).orElseThrow();
        assertThat(found.getTitle()).isEqualTo(entity.getTitle());
        assertThat(found.getComposer()).isEqualTo("Composer Name");
        assertThat(found.getBandType()).isEqualTo(BandType.AGRUPACION_MUSICAL);
        assertThat(found.getDurationSeconds()).isEqualTo(420);
        assertThat(found.getCompositionYear()).isEqualTo(2000);
        assertThat(found.getYoutubeUrl()).isEqualTo("https://youtube.com/test");
    }
}
