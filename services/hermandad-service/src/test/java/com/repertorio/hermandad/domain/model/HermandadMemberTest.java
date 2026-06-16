package com.repertorio.hermandad.domain.model;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    @Test
    void changeRoleUpdatesRole() {
        HermandadMember member = new HermandadMember(UUID.randomUUID(), "Juan", HermandadRole.MUSICIAN);

        member.changeRole(HermandadRole.CAPATAZ);

        assertThat(member.getRole()).isEqualTo(HermandadRole.CAPATAZ);
    }

    @Test
    void changeRoleToSameRoleThrowsException() {
        HermandadMember member = new HermandadMember(UUID.randomUUID(), "Juan", HermandadRole.MUSICIAN);

        assertThatThrownBy(() -> member.changeRole(HermandadRole.MUSICIAN))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already has role");
    }
}
