package com.repertorio.hermandad.domain.model;

import com.repertorio.hermandad.adapter.config.TestCacheConfig;
import com.repertorio.hermandad.adapter.outbound.persistence.HermandadEntity;
import com.repertorio.hermandad.adapter.outbound.persistence.HermandadJpaRepository;
import com.repertorio.hermandad.adapter.outbound.persistence.HermandadMemberEntity;
import com.repertorio.hermandad.adapter.outbound.persistence.HermandadMemberJpaRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import org.springframework.context.annotation.Import;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(TestCacheConfig.class)
class HermandadMemberDataJpaTest {

    @Autowired
    private HermandadMemberJpaRepository memberRepository;

    @Autowired
    private HermandadJpaRepository hermandadRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void updatedAtChangesAfterRoleUpdate() {
        // ponytail: need a real hermandad to satisfy FK constraint
        var hermandad = hermandadRepository.save(
                new HermandadEntity(null, "Macarena", "Sevilla", 1932, null, null, null, null));

        var member = new HermandadMemberEntity(null, hermandad.getId(), "user-1", HermandadRole.MUSICIAN, null, null);
        var saved = memberRepository.save(member);
        var initialUpdatedAt = saved.getUpdatedAt();

        // mutate the entity directly
        memberRepository.flush();

        // clear persistence context so findById reads from DB, not 1st-level cache
        entityManager.clear();

        var afterUpdate = memberRepository.findById(saved.getId()).orElseThrow();
        assertThat(afterUpdate.getUpdatedAt()).isNotEqualTo(initialUpdatedAt);
    }
}
