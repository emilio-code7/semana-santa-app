package com.repertorio.hermandad.adapter.outbound.persistence;

import com.repertorio.hermandad.domain.model.HermandadRole;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class HermandadMemberEntityTest {

    @Test
    void entityWithNullIdIsNew() {
        var entity = new HermandadMemberEntity(null, UUID.randomUUID(), "user-1", HermandadRole.MUSICIAN, null, null);
        assertThat(entity.isNew()).isTrue();
    }

    @Test
    void entityWithNonNullIdIsNotNew() {
        var id = UUID.randomUUID();
        var entity = new HermandadMemberEntity(id, UUID.randomUUID(), "user-1", HermandadRole.MUSICIAN, null, null);
        assertThat(entity.isNew()).isFalse();
    }
}
