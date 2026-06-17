package com.repertorio.hermandad.domain.model;

import com.repertorio.hermandad.adapter.outbound.persistence.HermandadJpaRepository;
import com.repertorio.hermandad.adapter.outbound.persistence.HermandadMemberJpaRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
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
        var hermandad = hermandadRepository.save(new Hermandad("Macarena", "Sevilla", 1932));

        var member = new HermandadMember(hermandad.getId(), "user-1", HermandadRole.MUSICIAN);
        var saved = memberRepository.save(member);
        var initialUpdatedAt = saved.getUpdatedAt();

        saved.changeRole(HermandadRole.CAPATAZ);
        memberRepository.flush();

        // clear persistence context so findById reads from DB, not 1st-level cache
        entityManager.clear();

        var afterUpdate = memberRepository.findById(saved.getId()).orElseThrow();
        assertThat(afterUpdate.getUpdatedAt()).isNotEqualTo(initialUpdatedAt);
    }
}
