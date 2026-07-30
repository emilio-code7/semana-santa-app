package com.repertorio.marcha.domain.model;

import java.util.UUID;

public class KnownRouteSection {

    private final UUID id;
    private final UUID procesionId;
    private String name;
    private int position;
    private String notes;

    // ponytail: package-private for adapter reconstruction only
    KnownRouteSection() {
        this.id = null;
        this.procesionId = null;
    }

    public KnownRouteSection(UUID id, UUID procesionId, String name, int position, String notes) {
        this.id = java.util.Objects.requireNonNull(id, "id");
        this.procesionId = java.util.Objects.requireNonNull(procesionId, "procesionId");
        this.name = java.util.Objects.requireNonNull(name, "name");
        this.position = position;
        this.notes = notes;
    }

    // ponytail: private all-args for adapter reconstruction
    private KnownRouteSection(UUID id, UUID procesionId, String name, int position, String notes, boolean ignored) {
        this.id = id;
        this.procesionId = procesionId;
        this.name = name;
        this.position = position;
        this.notes = notes;
    }

    public static KnownRouteSection reconstruct(UUID id, UUID procesionId, String name, int position, String notes) {
        return new KnownRouteSection(id, procesionId, name, position, notes, true);
    }

    public UUID getId() { return id; }
    public UUID getProcesionId() { return procesionId; }
    public String getName() { return name; }
    public int getPosition() { return position; }
    public String getNotes() { return notes; }
}
