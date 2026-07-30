package com.repertorio.procesion.domain.model;

import java.time.Instant;
import java.util.UUID;

public class Paso {

    private UUID id;
    private UUID procesionId;
    private int position;
    private UUID titularId;
    private String notes;
    private Instant createdAt;
    private Instant updatedAt;

    protected Paso() {}

    private Paso(UUID id, UUID procesionId, int position, UUID titularId,
                 String notes, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.procesionId = procesionId;
        this.position = position;
        this.titularId = titularId;
        this.notes = notes;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Paso create(UUID procesionId, int position, UUID titularId, String notes) {
        var now = Instant.now();
        return new Paso(null, procesionId, position, titularId, notes, now, now);
    }

    public void update(int position, UUID titularId, String notes) {
        this.position = position;
        this.titularId = titularId;
        this.notes = notes;
        this.updatedAt = Instant.now();
    }

    public static Paso reconstruct(UUID id, UUID procesionId, int position, UUID titularId,
                                    String notes, Instant createdAt, Instant updatedAt) {
        return new Paso(id, procesionId, position, titularId, notes, createdAt, updatedAt);
    }

    public UUID getId() { return id; }
    public UUID getProcesionId() { return procesionId; }
    public int getPosition() { return position; }
    public UUID getTitularId() { return titularId; }
    public String getNotes() { return notes; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
