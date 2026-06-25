package com.repertorio.hermandad.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HermandadTest {

    @Test
    void constructorSetsFieldsCorrectly() {
        Hermandad hermandad = new Hermandad("Macarena", "Sevilla", 1932, null);

        assertThat(hermandad.getName()).isEqualTo("Macarena");
        assertThat(hermandad.getCity()).isEqualTo("Sevilla");
        assertThat(hermandad.getFoundedYear()).isEqualTo(1932);
        assertThat(hermandad.getDescription()).isNull();
    }

    @Test
    void createdAtIsNullBeforePersist() {
        Hermandad hermandad = new Hermandad("Macarena", "Sevilla", 1932, null);

        assertThat(hermandad.getCreatedAt()).isNull();
    }
}
