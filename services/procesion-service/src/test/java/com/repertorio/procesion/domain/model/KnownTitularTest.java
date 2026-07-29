package com.repertorio.procesion.domain.model;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class KnownTitularTest {

    private final UUID id = UUID.randomUUID();
    private final UUID hermandadId = UUID.randomUUID();

    @Test
    void createsWithValidFields() {
        var kt = new KnownTitular(id, hermandadId, "Jesus del Gran Poder");
        assertThat(kt.getId()).isEqualTo(id);
        assertThat(kt.getHermandadId()).isEqualTo(hermandadId);
        assertThat(kt.getName()).isEqualTo("Jesus del Gran Poder");
    }

    @Test
    void rejectsNullId() {
        assertThrows(IllegalArgumentException.class,
                () -> new KnownTitular(null, hermandadId, "Jesus"));
    }

    @Test
    void rejectsNullHermandadId() {
        assertThrows(IllegalArgumentException.class,
                () -> new KnownTitular(id, null, "Jesus"));
    }

    @Test
    void rejectsBlankName() {
        assertThrows(IllegalArgumentException.class,
                () -> new KnownTitular(id, hermandadId, ""));
    }

    @Test
    void updateNameChangesName() {
        var kt = new KnownTitular(id, hermandadId, "Old");
        kt.updateName("New");
        assertThat(kt.getName()).isEqualTo("New");
    }

    @Test
    void updateNameRejectsBlank() {
        var kt = new KnownTitular(id, hermandadId, "Jesus");
        assertThrows(IllegalArgumentException.class, () -> kt.updateName(""));
    }

    @Test
    void reconstructRestoresAllFields() {
        var now = java.time.Instant.now();
        var kt = KnownTitular.reconstruct(id, hermandadId, "Jesus", now);
        assertThat(kt.getId()).isEqualTo(id);
        assertThat(kt.getName()).isEqualTo("Jesus");
        assertThat(kt.getHermandadId()).isEqualTo(hermandadId);
    }
}
