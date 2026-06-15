package com.repertorio.hermandad.domain.model;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class HermandadMemberTest {

    @Test
    void constructorSetsFieldsCorrectly() {
        HermandadMember hermandadMember = new HermandadMember(UUID.randomUUID(), "Juan", HermandadRole.MUSICIAN);

        assertThat(hermandadMember.getUserId()).isEqualTo("Juan");
        assertThat(hermandadMember.getRole()).isEqualTo(HermandadRole.MUSICIAN);
    }

    @Test
    void createdAtIsNullBeforePersist() {
        HermandadMember hermandadMember = new HermandadMember(UUID.randomUUID(), "Juan", HermandadRole.MUSICIAN);

        assertThat(hermandadMember.getJoinedAt()).isNull();
        assertThat(hermandadMember.getUpdatedAt()).isNull();
    }
}
