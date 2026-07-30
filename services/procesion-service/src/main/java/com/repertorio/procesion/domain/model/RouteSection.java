package com.repertorio.procesion.domain.model;

import java.time.Instant;
import java.util.UUID;

public class RouteSection {

    private UUID id;
    private UUID procesionId;
    private String name;
    private int position;
    private String notes;
    private Instant createdAt;
    private Instant updatedAt;

    protected RouteSection() {}

    private RouteSection(UUID id, UUID procesionId, String name, int position,
                         String notes, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.procesionId = procesionId;
        this.name = name;
        this.position = position;
        this.notes = notes;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static RouteSection create(UUID procesionId, String name, int position, String notes) {
        var now = Instant.now();
        return new RouteSection(null, procesionId, name, position, notes, now, now);
    }

    public void update(String name, int position, String notes) {
        this.name = name;
        this.position = position;
        this.notes = notes;
        this.updatedAt = Instant.now();
    }

    public static RouteSection reconstruct(UUID id, UUID procesionId, String name, int position,
                                           String notes, Instant createdAt, Instant updatedAt) {
        return new RouteSection(id, procesionId, name, position, notes, createdAt, updatedAt);
    }

    public UUID getId() { return id; }
    public UUID getProcesionId() { return procesionId; }
    public String getName() { return name; }
    public int getPosition() { return position; }
    public String getNotes() { return notes; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
