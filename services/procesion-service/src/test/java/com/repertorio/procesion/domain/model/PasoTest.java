package com.repertorio.procesion.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class PasoTest {

    private final UUID procesionId = UUID.randomUUID();
    private final UUID titularId = UUID.randomUUID();

    @Test
    void createShouldSetFields() {
        var paso = Paso.create(procesionId, 1, titularId, "Some notes");
        assertThat(paso.getProcesionId()).isEqualTo(procesionId);
        assertThat(paso.getPosition()).isEqualTo(1);
        assertThat(paso.getTitularId()).isEqualTo(titularId);
        assertThat(paso.getNotes()).isEqualTo("Some notes");
        assertThat(paso.getId()).isNull();
    }

    @Test
    void createWithNullNotes() {
        var paso = Paso.create(procesionId, 2, titularId, null);
        assertThat(paso.getNotes()).isNull();
    }

    @Test
    void updateShouldChangeFields() {
        var paso = Paso.create(procesionId, 1, titularId, "Old notes");
        var newTitular = UUID.randomUUID();
        paso.update(2, newTitular, "Updated notes");
        assertThat(paso.getPosition()).isEqualTo(2);
        assertThat(paso.getTitularId()).isEqualTo(newTitular);
        assertThat(paso.getNotes()).isEqualTo("Updated notes");
    }

    @Test
    void updateShouldUpdateTimestamp() {
        var paso = Paso.create(procesionId, 1, titularId, null);
        Instant before = paso.getUpdatedAt();
        try { Thread.sleep(1); } catch (InterruptedException e) { throw new RuntimeException(e); }
        paso.update(1, titularId, null);
        assertThat(paso.getUpdatedAt()).isAfter(before);
    }

    @Test
    void reconstructRestoresAllFields() {
        var id = UUID.randomUUID();
        var now = Instant.now();
        var paso = Paso.reconstruct(id, procesionId, 3, titularId, "Reconstructed", now, now);
        assertThat(paso.getId()).isEqualTo(id);
        assertThat(paso.getProcesionId()).isEqualTo(procesionId);
        assertThat(paso.getPosition()).isEqualTo(3);
        assertThat(paso.getTitularId()).isEqualTo(titularId);
        assertThat(paso.getNotes()).isEqualTo("Reconstructed");
        assertThat(paso.getCreatedAt()).isEqualTo(now);
        assertThat(paso.getUpdatedAt()).isEqualTo(now);
    }
}
