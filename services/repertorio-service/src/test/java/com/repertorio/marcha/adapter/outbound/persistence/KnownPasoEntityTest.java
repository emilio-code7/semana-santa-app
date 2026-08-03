package com.repertorio.marcha.adapter.outbound.persistence;

import com.repertorio.marcha.domain.model.KnownPaso;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class KnownPasoEntityTest {

    @Test
    void freshlyConstructedEntityIsNew() {
        var entity = KnownPasoEntity.from(new KnownPaso(UUID.randomUUID(), UUID.randomUUID(), 0, UUID.randomUUID()));
        assertThat(entity.isNew()).isTrue();
    }

    @Test
    void postLoadMarksEntityNotNew() {
        var entity = KnownPasoEntity.from(new KnownPaso(UUID.randomUUID(), UUID.randomUUID(), 0, UUID.randomUUID()));
        entity.markNotNew();
        assertThat(entity.isNew()).isFalse();
    }
}
