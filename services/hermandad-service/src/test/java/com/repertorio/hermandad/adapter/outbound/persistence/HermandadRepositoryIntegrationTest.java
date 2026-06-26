package com.repertorio.hermandad.adapter.outbound.persistence;

import com.repertorio.hermandad.adapter.inbound.kafka.IdempotentEventConsumer;
import com.repertorio.hermandad.domain.model.Hermandad;
import com.repertorio.hermandad.domain.model.HermandadMember;
import com.repertorio.hermandad.domain.model.HermandadRole;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Integration test for JPA repositories against a real PostgreSQL database.
 * <p>
 * Requires a running PostgreSQL on localhost:5432 with database "hermandad_db"
 * and user "postgres". The app's docker-compose provides this.
 * Skips automatically if PostgreSQL is unreachable.
 */
@SpringBootTest
class HermandadRepositoryIntegrationTest {

    private static final String POSTGRES_JDBC = System.getenv().getOrDefault(
            "IT_DATASOURCE_URL", "jdbc:postgresql://localhost:5432/hermandad_db");
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
    private HermandadJpaRepository hermandadRepo;

    @Autowired
    private HermandadMemberJpaRepository memberRepo;

    @MockitoBean
    private IdempotentEventConsumer idempotentEventConsumer;

    @Test
    void saveAndFindHermandad() {
        var hermandad = new Hermandad("IT-Test-" + System.nanoTime(), "Sevilla", 2024, null);
        var saved = hermandadRepo.save(hermandad);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).startsWith("IT-Test");
        assertThat(saved.getDescription()).isNull();

        var found = hermandadRepo.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getCity()).isEqualTo("Sevilla");
    }

    @Test
    void existsByName() {
        var name = "IT-Unique-" + System.nanoTime();
        hermandadRepo.save(new Hermandad(name, "Sevilla", 2020, null));

        assertThat(hermandadRepo.existsByName(name)).isTrue();
        assertThat(hermandadRepo.existsByName("IT-NonExistent-" + System.nanoTime())).isFalse();
    }

    @Test
    void uniqueNameConstraint() {
        var name = "IT-SameName-" + System.nanoTime();
        hermandadRepo.save(new Hermandad(name, "Sevilla", 2010, null));

        assertThrows(DataIntegrityViolationException.class,
                () -> hermandadRepo.save(new Hermandad(name, "Malaga", 2010, null)));
    }

    @Test
    void saveAndFindMember() {
        var hermandad = hermandadRepo.save(
                new Hermandad("IT-Members-" + System.nanoTime(), "Sevilla", 2024, "desc"));
        var member = memberRepo.save(
                new HermandadMember(hermandad.getId(), "user-" + System.nanoTime(), HermandadRole.HERMANDAD_ADMIN));

        assertThat(member.getId()).isNotNull();
        assertThat(member.getRole()).isEqualTo(HermandadRole.HERMANDAD_ADMIN);

        var members = memberRepo.findByHermandadId(hermandad.getId());
        assertThat(members).hasSize(1);
    }

    @Test
    void findMemberByUserIdAndHermandadId() {
        var hermandad = hermandadRepo.save(
                new Hermandad("IT-FindMember-" + System.nanoTime(), "Sevilla", 2024, null));
        memberRepo.save(new HermandadMember(hermandad.getId(), "user-a", HermandadRole.MUSICIAN));

        Optional<HermandadMember> found = memberRepo.findByUserIdAndHermandadId("user-a", hermandad.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getRole()).isEqualTo(HermandadRole.MUSICIAN);

        assertThat(memberRepo.findByUserIdAndHermandadId("user-b", hermandad.getId())).isEmpty();
    }

    @Test
    void changeMemberRoleUpdatesTimestamps() {
        var hermandad = hermandadRepo.save(
                new Hermandad("IT-Roles-" + System.nanoTime(), "Sevilla", 2024, null));
        var member = memberRepo.save(
                new HermandadMember(hermandad.getId(), "user-1", HermandadRole.MUSICIAN));

        member.changeRole(HermandadRole.HERMANDAD_ADMIN);
        memberRepo.save(member);

        var updated = memberRepo.findById(member.getId()).orElseThrow();
        assertThat(updated.getRole()).isEqualTo(HermandadRole.HERMANDAD_ADMIN);
        assertThat(updated.getUpdatedAt()).isAfter(updated.getJoinedAt());
    }

    @Test
    void descriptionCanBeNullAndSet() {
        var h1 = hermandadRepo.save(
                new Hermandad("IT-Desc-" + System.nanoTime(), "Sevilla", 2024, null));
        assertThat(h1.getDescription()).isNull();

        var h2 = hermandadRepo.save(
                new Hermandad("IT-Desc2-" + System.nanoTime(), "Sevilla", 2024, "some description"));
        assertThat(h2.getDescription()).isEqualTo("some description");
    }
}
