package com.repertorio.hermandad.domain.model;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TitularTest {

    private final UUID hermandadId = UUID.randomUUID();

    @Test
    void createsTitularWithValidName() {
        var titular = new Titular("Jesus del Gran Poder", null, hermandadId);
        assertThat(titular.getName()).isEqualTo("Jesus del Gran Poder");
        assertThat(titular.getDescription()).isNull();
        assertThat(titular.getHermandadId()).isEqualTo(hermandadId);
    }

    @Test
    void createsTitularWithDescription() {
        var titular = new Titular("Maria Santisima", "Description", hermandadId);
        assertThat(titular.getDescription()).isEqualTo("Description");
    }

    @Test
    void rejectsNullName() {
        assertThrows(IllegalArgumentException.class,
                () -> new Titular(null, "desc", hermandadId));
    }

    @Test
    void rejectsBlankName() {
        assertThrows(IllegalArgumentException.class,
                () -> new Titular("  ", "desc", hermandadId));
    }

    @Test
    void rejectsNullHermandadId() {
        assertThrows(IllegalArgumentException.class,
                () -> new Titular("Jesus", null, null));
    }

    @Test
    void updateChangesNameAndDescription() {
        var titular = new Titular("Old", null, hermandadId);
        titular.update("New", "New desc");
        assertThat(titular.getName()).isEqualTo("New");
        assertThat(titular.getDescription()).isEqualTo("New desc");
    }

    @Test
    void updateRejectsBlankName() {
        var titular = new Titular("Jesus", null, hermandadId);
        assertThrows(IllegalArgumentException.class, () -> titular.update("  ", null));
    }

    @Test
    void reconstructRestoresAllFields() {
        var id = UUID.randomUUID();
        var now = java.time.Instant.now();
        var titular = Titular.reconstruct(id, "Jesus", null, hermandadId, now, now);
        assertThat(titular.getId()).isEqualTo(id);
        assertThat(titular.getName()).isEqualTo("Jesus");
        assertThat(titular.getCreatedAt()).isEqualTo(now);
    }
}
