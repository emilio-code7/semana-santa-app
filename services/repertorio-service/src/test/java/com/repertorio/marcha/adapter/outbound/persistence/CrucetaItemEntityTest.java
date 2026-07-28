package com.repertorio.marcha.adapter.outbound.persistence;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CrucetaItemEntityTest {

    @Test
    void freshlyConstructedEntityIsNew() {
        var entity = new CrucetaItemEntity(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 1, null);
        // @Transient isNew defaults to true for any new instance, even with non-null ID
        assertThat(entity.isNew()).isTrue();
    }

    @Test
    void postLoadMarksEntityNotNew() {
        var entity = new CrucetaItemEntity(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 1, null);
        entity.markNotNew();  // simulates @PostLoad
        assertThat(entity.isNew()).isFalse();
    }
}
