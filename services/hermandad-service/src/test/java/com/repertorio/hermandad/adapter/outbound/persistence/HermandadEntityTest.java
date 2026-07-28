package com.repertorio.hermandad.adapter.outbound.persistence;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class HermandadEntityTest {

    @Test
    void entityWithNullIdIsNew() {
        var entity = new HermandadEntity(null, "Test", "Sevilla", 2024, null, null, null, null);
        assertThat(entity.isNew()).isTrue();
    }

    @Test
    void entityWithNonNullIdIsNotNew() {
        var id = UUID.randomUUID();
        var entity = new HermandadEntity(id, "Test", "Sevilla", 2024, null, null, null, null);
        assertThat(entity.isNew()).isFalse();
    }
}
