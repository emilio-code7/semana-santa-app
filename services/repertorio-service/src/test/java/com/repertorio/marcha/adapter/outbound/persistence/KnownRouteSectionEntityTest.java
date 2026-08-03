package com.repertorio.marcha.adapter.outbound.persistence;

import com.repertorio.marcha.domain.model.KnownRouteSection;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class KnownRouteSectionEntityTest {

    @Test
    void freshlyConstructedEntityIsNew() {
        var entity = KnownRouteSectionEntity.from(new KnownRouteSection(UUID.randomUUID(), UUID.randomUUID(), "name", 0, null));
        assertThat(entity.isNew()).isTrue();
    }

    @Test
    void postLoadMarksEntityNotNew() {
        var entity = KnownRouteSectionEntity.from(new KnownRouteSection(UUID.randomUUID(), UUID.randomUUID(), "name", 0, null));
        entity.markNotNew();
        assertThat(entity.isNew()).isFalse();
    }
}
