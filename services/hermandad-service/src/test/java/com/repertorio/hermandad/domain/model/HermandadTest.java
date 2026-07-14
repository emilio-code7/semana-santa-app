package com.repertorio.hermandad.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    @Test
    void rejectsNullName() {
        assertThatThrownBy(() -> new Hermandad(null, "Sevilla", 1932, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name");
    }

    @Test
    void rejectsEmptyName() {
        assertThatThrownBy(() -> new Hermandad("", "Sevilla", 1932, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name");
    }

    @Test
    void rejectsNullCity() {
        assertThatThrownBy(() -> new Hermandad("Macarena", null, 1932, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("city");
    }

    @Test
    void rejectsEmptyCity() {
        assertThatThrownBy(() -> new Hermandad("Macarena", "", 1932, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("city");
    }

    @Test
    void rejectsNegativeFoundedYear() {
        assertThatThrownBy(() -> new Hermandad("Macarena", "Sevilla", -1, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("foundedYear");
    }
}
