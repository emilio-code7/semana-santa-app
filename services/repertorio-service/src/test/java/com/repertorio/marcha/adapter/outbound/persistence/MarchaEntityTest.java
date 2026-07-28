package com.repertorio.marcha.adapter.outbound.persistence;

import com.repertorio.marcha.domain.model.BandType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MarchaEntityTest {

    @Test
    void freshlyConstructedEntityIsNew() {
        var entity = new MarchaEntity(UUID.randomUUID(), "Test", "Composer", BandType.BANDA_PALIO,
                300, null, null, Instant.now(), Instant.now());
        // @Transient isNew defaults to true for any new instance, even with non-null ID
        assertThat(entity.isNew()).isTrue();
    }

    @Test
    void postLoadMarksEntityNotNew() {
        var entity = new MarchaEntity(UUID.randomUUID(), "Test", "Composer", BandType.BANDA_PALIO,
                300, null, null, Instant.now(), Instant.now());
        entity.markNotNew();  // simulates @PostLoad
        assertThat(entity.isNew()).isFalse();
    }
}
