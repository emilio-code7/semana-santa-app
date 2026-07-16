package com.repertorio.hermandad.adapter.outbound.persistence;

import com.repertorio.common.JdbcIntegrationTestBase;
import com.repertorio.hermandad.adapter.inbound.kafka.IdempotentEventConsumer;
import com.repertorio.hermandad.domain.model.HermandadRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

import org.springframework.data.domain.Pageable;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Integration test for JPA repositories against a real PostgreSQL database.
 * <p>
 * Requires a running PostgreSQL on localhost:5432 with database "hermandad_db"
 * and user "postgres". The app's docker-compose provides this.
 * Skips automatically if PostgreSQL is unreachable.
 */
@SpringBootTest
class HermandadRepositoryIntegrationTest extends JdbcIntegrationTestBase {

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
    private HermandadJpaRepository hermandadRepo;

    @Autowired
    private HermandadMemberJpaRepository memberRepo;

    @MockitoBean
    private IdempotentEventConsumer idempotentEventConsumer;

    @Test
    void saveAndFindHermandad() {
        var hermandad = new HermandadEntity(null, "IT-Test-" + System.nanoTime(), "Sevilla", 2024, null, null, null, null);
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
        hermandadRepo.save(new HermandadEntity(null, name, "Sevilla", 2020, null, null, null, null));

        assertThat(hermandadRepo.existsByName(name)).isTrue();
        assertThat(hermandadRepo.existsByName("IT-NonExistent-" + System.nanoTime())).isFalse();
    }

    @Test
    void uniqueNameConstraint() {
        var name = "IT-SameName-" + System.nanoTime();
        hermandadRepo.save(new HermandadEntity(null, name, "Sevilla", 2010, null, null, null, null));

        assertThrows(DataIntegrityViolationException.class,
                () -> hermandadRepo.save(new HermandadEntity(null, name, "Malaga", 2010, null, null, null, null)));
    }

    @Test
    void saveAndFindMember() {
        var hermandad = hermandadRepo.save(
                new HermandadEntity(null, "IT-Members-" + System.nanoTime(), "Sevilla", 2024, null, "desc", null, null));
        var member = memberRepo.save(
                new HermandadMemberEntity(null, hermandad.getId(), "user-" + System.nanoTime(), HermandadRole.HERMANDAD_ADMIN, null, null));

        assertThat(member.getId()).isNotNull();
        assertThat(member.getRole()).isEqualTo(HermandadRole.HERMANDAD_ADMIN);

        var members = memberRepo.findByHermandadId(hermandad.getId(), Pageable.unpaged());
        assertThat(members.getContent()).hasSize(1);
    }

    @Test
    void findMemberByUserIdAndHermandadId() {
        var hermandad = hermandadRepo.save(
                new HermandadEntity(null, "IT-FindMember-" + System.nanoTime(), "Sevilla", 2024, null, null, null, null));
        memberRepo.save(new HermandadMemberEntity(null, hermandad.getId(), "user-a", HermandadRole.MUSICIAN, null, null));

        Optional<HermandadMemberEntity> found = memberRepo.findByUserIdAndHermandadId("user-a", hermandad.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getRole()).isEqualTo(HermandadRole.MUSICIAN);

        assertThat(memberRepo.findByUserIdAndHermandadId("user-b", hermandad.getId())).isEmpty();
    }

    @Test
    void changeMemberRoleUpdatesTimestamps() {
        var hermandad = hermandadRepo.save(
                new HermandadEntity(null, "IT-Roles-" + System.nanoTime(), "Sevilla", 2024, null, null, null, null));
        var member = memberRepo.save(
                new HermandadMemberEntity(null, hermandad.getId(), "user-1", HermandadRole.MUSICIAN, null, null));

        memberRepo.save(member);

        var updated = memberRepo.findById(member.getId()).orElseThrow();
        assertThat(updated.getRole()).isEqualTo(HermandadRole.MUSICIAN);
        assertThat(updated.getUpdatedAt()).isAfter(updated.getJoinedAt());
    }

    @Test
    void descriptionCanBeNullAndSet() {
        var h1 = hermandadRepo.save(
                new HermandadEntity(null, "IT-Desc-" + System.nanoTime(), "Sevilla", 2024, null, null, null, null));
        assertThat(h1.getDescription()).isNull();

        var h2 = hermandadRepo.save(
                new HermandadEntity(null, "IT-Desc2-" + System.nanoTime(), "Sevilla", 2024, null, "some description", null, null));
        assertThat(h2.getDescription()).isEqualTo("some description");
    }
}
