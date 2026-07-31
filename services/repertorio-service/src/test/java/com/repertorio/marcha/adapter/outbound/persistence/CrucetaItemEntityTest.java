package com.repertorio.marcha.adapter.outbound.persistence;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CrucetaItemEntityTest {

    @Test
    void freshlyConstructedEntityIsNew() {
        var entity = new CrucetaItemEntity(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), 0, null);
        assertThat(entity.isNew()).isTrue();
    }

    @Test
    void postLoadMarksEntityNotNew() {
        var entity = new CrucetaItemEntity(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), 0, null);
        entity.markNotNew();
        assertThat(entity.isNew()).isFalse();
    }
}
