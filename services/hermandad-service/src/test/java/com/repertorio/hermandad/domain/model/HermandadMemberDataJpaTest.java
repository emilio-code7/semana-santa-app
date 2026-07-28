package com.repertorio.hermandad.domain.model;

import com.repertorio.hermandad.adapter.config.TestCacheConfig;
import com.repertorio.hermandad.adapter.outbound.persistence.HermandadEntity;
import com.repertorio.hermandad.adapter.outbound.persistence.HermandadJpaRepository;
import com.repertorio.hermandad.adapter.outbound.persistence.HermandadMemberEntity;
import com.repertorio.hermandad.adapter.outbound.persistence.HermandadMemberJpaRepository;
import com.repertorio.hermandad.adapter.outbound.persistence.HermandadMemberRepositoryAdapter;
import com.repertorio.hermandad.adapter.outbound.persistence.HermandadRepositoryAdapter;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.time.Instant;
import java.util.UUID;

import org.springframework.context.annotation.Import;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import({TestCacheConfig.class, HermandadRepositoryAdapter.class, HermandadMemberRepositoryAdapter.class})
class HermandadMemberDataJpaTest {

    @Autowired
    private HermandadMemberJpaRepository memberRepository;

    @Autowired
    private HermandadJpaRepository hermandadRepository;

    @Autowired
    private HermandadRepositoryAdapter hermandadAdapter;

    @Autowired
    private HermandadMemberRepositoryAdapter memberAdapter;

    @Autowired
    private EntityManager entityManager;

    @Test
    void updatedAtChangesAfterRoleUpdate() {
        // ponytail: need a real hermandad to satisfy FK constraint
        var hermandad = hermandadRepository.save(
                new HermandadEntity(null, "Macarena", "Sevilla", 1932, null, null, null, null));

        var member = new HermandadMemberEntity(null, hermandad.getId(), "user-1", HermandadRole.MUSICIAN, null, null);
        memberRepository.save(member);
        entityManager.flush();
        entityManager.clear();

        var loaded = memberRepository.findById(member.getId()).orElseThrow();
        var initialUpdatedAt = loaded.getUpdatedAt();

        // change role through the adapter — triggers @PreUpdate → updatedAt changes
        var domain = loaded.toDomain();
        domain.changeRole(HermandadRole.CAPATAZ);
        memberAdapter.save(domain);
        entityManager.flush();
        entityManager.clear();

        var afterUpdate = memberRepository.findById(member.getId()).orElseThrow();
        assertThat(afterUpdate.getUpdatedAt()).isNotEqualTo(initialUpdatedAt);
        assertThat(afterUpdate.getRole()).isEqualTo(HermandadRole.CAPATAZ);
    }

    @Test
    void twoConsecutiveHermandadUpdatesPreservesVersionViaAdapter() {
        // Given: a hermandad saved in DB
        var saved = hermandadRepository.save(
                new HermandadEntity(null, "OptLockTest", "Sevilla", 2024, null, "initial", null, null));
        entityManager.flush();
        entityManager.clear();

        // When: first update through the adapter
        var domain1 = hermandadRepository.findById(saved.getId()).orElseThrow().toDomain();
        var modified1 = Hermandad.reconstruct(
                domain1.getId(), domain1.getName(), domain1.getCity(), domain1.getFoundedYear(),
                domain1.getKeycloakGroupId(), "updated desc", domain1.getCreatedAt(), domain1.getUpdatedAt());
        hermandadAdapter.save(modified1);
        entityManager.flush();
        entityManager.clear();

        // When: second update through the adapter — should NOT throw optimistic-lock
        var domain2 = hermandadRepository.findById(saved.getId()).orElseThrow().toDomain();
        var modified2 = Hermandad.reconstruct(
                domain2.getId(), domain2.getName(), domain2.getCity(), domain2.getFoundedYear(),
                domain2.getKeycloakGroupId(), "final desc", domain2.getCreatedAt(), domain2.getUpdatedAt());
        hermandadAdapter.save(modified2);
        entityManager.flush();
        entityManager.clear();

        // Then: the final state reflects the last update
        var result = hermandadRepository.findById(saved.getId()).orElseThrow();
        assertThat(result.getDescription()).isEqualTo("final desc");
    }

    @Test
    void deleteAfterUpdateSucceeds() {
        // Given: a hermandad and member saved in DB
        var hermandad = hermandadRepository.save(
                new HermandadEntity(null, "DeleteTest", "Sevilla", 2024, null, null, null, null));
        entityManager.flush();
        entityManager.clear();

        var member = memberRepository.save(
                new HermandadMemberEntity(null, hermandad.getId(), "user-1", HermandadRole.MUSICIAN, null, null));
        entityManager.flush();
        entityManager.clear();

        // When: member is updated through the adapter (version gets bumped in DB)
        var domain1 = memberRepository.findById(member.getId()).orElseThrow().toDomain();
        domain1.changeRole(HermandadRole.CAPATAZ);
        memberAdapter.save(domain1);
        entityManager.flush();
        entityManager.clear();

        // Then: deleting the updated member should succeed
        // (delete() must load the managed entity by ID, not reconstruct from domain)
        var domain2 = memberRepository.findById(member.getId()).orElseThrow().toDomain();
        memberAdapter.delete(domain2);
        entityManager.flush();

        var deleted = memberRepository.findById(member.getId());
        assertThat(deleted).isEmpty();
    }

    @Test
    void twoConsecutiveMemberUpdatesPreservesVersionViaAdapter() {
        // Given: a hermandad and member saved in DB
        var hermandad = hermandadRepository.save(
                new HermandadEntity(null, "MemberOptLock", "Sevilla", 2024, null, null, null, null));
        entityManager.flush();
        entityManager.clear();

        var member = memberRepository.save(
                new HermandadMemberEntity(null, hermandad.getId(), "user-1", HermandadRole.MUSICIAN, null, null));
        entityManager.flush();
        entityManager.clear();

        // When: first update through the adapter
        var domain1 = memberRepository.findById(member.getId()).orElseThrow().toDomain();
        domain1.changeRole(HermandadRole.CAPATAZ);
        memberAdapter.save(domain1);
        entityManager.flush();
        entityManager.clear();

        // When: second update through the adapter — should NOT throw optimistic-lock
        var domain2 = memberRepository.findById(member.getId()).orElseThrow().toDomain();
        domain2.changeRole(HermandadRole.HERMANDAD_ADMIN);
        memberAdapter.save(domain2);
        entityManager.flush();
        entityManager.clear();

        // Then: the final state reflects the last role change
        var result = memberRepository.findById(member.getId()).orElseThrow();
        assertThat(result.getRole()).isEqualTo(HermandadRole.HERMANDAD_ADMIN);
    }

    @Test
    void returnedDomainReflectsUpdatedAtAfterHermandadUpdate() {
        // Given: a hermandad saved in DB
        var saved = hermandadRepository.save(
                new HermandadEntity(null, "UpdatedAtReflect", "Sevilla", 2024, null, null, null, null));
        entityManager.flush();
        entityManager.clear();

        var domain = hermandadAdapter.findById(saved.getId()).orElseThrow();
        var previousUpdatedAt = domain.getUpdatedAt();

        // When: updating through the adapter
        var updated = Hermandad.reconstruct(
                domain.getId(), domain.getName(), domain.getCity(), domain.getFoundedYear(),
                domain.getKeycloakGroupId(), "modified", domain.getCreatedAt(), domain.getUpdatedAt());
        var result = hermandadAdapter.save(updated);

        // Then: the returned domain should have a newer updatedAt
        assertThat(result.getUpdatedAt()).isAfter(previousUpdatedAt);
    }

    @Test
    void preservesSuppliedCreatedAtOnInsert() {
        var fixedInstant = Instant.parse("2024-01-15T10:00:00Z");
        var domain = Hermandad.reconstruct(null, "PreserveCreated", "Sevilla", 2024, null, null, fixedInstant, null);

        var result = hermandadAdapter.save(domain);

        assertThat(result.getCreatedAt()).isEqualTo(fixedInstant);
    }

    @Test
    void preservesSuppliedJoinedAtOnInsert() {
        // Given: a hermandad for FK constraint
        var hermandad = hermandadRepository.save(
                new HermandadEntity(null, "JoinedTest", "Sevilla", 2024, null, null, null, null));
        entityManager.flush();
        entityManager.clear();

        var fixedJoinedAt = Instant.parse("2024-06-01T12:00:00Z");
        var member = HermandadMember.reconstruct(null, hermandad.getId(), "user-joined", HermandadRole.MUSICIAN,
                fixedJoinedAt, null);

        // When: saving through the adapter
        var result = memberAdapter.save(member);

        // Then: joinedAt should be preserved, not overwritten
        assertThat(result.getJoinedAt()).isEqualTo(fixedJoinedAt);
    }

    @Test
    void deleteMemberWithNullIdThrowsIllegalArgumentException() {
        var member = new HermandadMember(UUID.randomUUID(), "no-id", HermandadRole.MUSICIAN);

        assertThatThrownBy(() -> memberAdapter.delete(member))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("id");
    }

    @Test
    void hermandadUpdatePreservesCreatedAt() {
        var fixedCreatedAt = Instant.parse("2024-01-15T10:00:00Z");
        var saved = hermandadRepository.save(
                new HermandadEntity(null, "PreserveCreatedAt", "Sevilla", 2024, null, null, fixedCreatedAt, null));
        entityManager.flush();
        entityManager.clear();

        var domain = hermandadAdapter.findById(saved.getId()).orElseThrow();
        var updated = Hermandad.reconstruct(
                domain.getId(), domain.getName(), domain.getCity(), domain.getFoundedYear(),
                domain.getKeycloakGroupId(), "updated desc", domain.getCreatedAt(), domain.getUpdatedAt());
        var result = hermandadAdapter.save(updated);

        assertThat(result.getCreatedAt()).isEqualTo(fixedCreatedAt);
    }

    @Test
    void memberUpdatePreservesImmutableFields() {
        var hermandad = hermandadRepository.save(
                new HermandadEntity(null, "MemberImmutableTest", "Sevilla", 2024, null, null, null, null));
        entityManager.flush();
        entityManager.clear();

        var fixedJoinedAt = Instant.parse("2024-06-01T12:00:00Z");
        var member = memberRepository.save(
                new HermandadMemberEntity(null, hermandad.getId(), "user-immutable", HermandadRole.MUSICIAN,
                        fixedJoinedAt, null));
        entityManager.flush();
        entityManager.clear();

        var domain = memberAdapter.findByUserIdAndHermandadId("user-immutable", hermandad.getId()).orElseThrow();
        domain.changeRole(HermandadRole.CAPATAZ);
        var result = memberAdapter.save(domain);

        assertThat(result.getHermandadId()).isEqualTo(hermandad.getId());
        assertThat(result.getUserId()).isEqualTo("user-immutable");
        assertThat(result.getJoinedAt()).isEqualTo(fixedJoinedAt);
        assertThat(result.getRole()).isEqualTo(HermandadRole.CAPATAZ);
    }

    @Test
    void hermandadAdapterSaveWithNonExistentIdThrowsException() {
        var nonExistentId = UUID.randomUUID();
        var domain = Hermandad.reconstruct(nonExistentId, "Ghost", "Sevilla", 2024, null, null, null, null);

        assertThatThrownBy(() -> hermandadAdapter.save(domain))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(nonExistentId.toString());
    }

    @Test
    void memberAdapterDeleteWithNonExistentIdThrowsException() {
        var nonExistentId = UUID.randomUUID();
        var member = HermandadMember.reconstruct(nonExistentId, UUID.randomUUID(), "ghost-user",
                HermandadRole.MUSICIAN, null, null);

        assertThatThrownBy(() -> memberAdapter.delete(member))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(nonExistentId.toString());
    }
}
