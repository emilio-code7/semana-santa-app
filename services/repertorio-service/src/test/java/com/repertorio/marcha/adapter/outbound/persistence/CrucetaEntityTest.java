package com.repertorio.marcha.adapter.outbound.persistence;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CrucetaEntityTest {

    @Test
    void freshlyConstructedEntityIsNew() {
        var entity = new CrucetaEntity(UUID.randomUUID(), UUID.randomUUID(), Instant.now(), Instant.now());
        assertThat(entity.isNew()).isTrue();
    }

    @Test
    void postLoadMarksEntityNotNew() {
        var entity = new CrucetaEntity(UUID.randomUUID(), UUID.randomUUID(), Instant.now(), Instant.now());
        entity.markNotNew();
        assertThat(entity.isNew()).isFalse();
    }
}
