package com.repertorio.marcha.domain.model;

import java.util.UUID;

public class KnownPaso {

    private final UUID id;
    private final UUID procesionId;
    private int position;
    private UUID titularId;

    // ponytail: package-private for adapter reconstruction only
    KnownPaso() {
        this.id = null;
        this.procesionId = null;
    }

    public KnownPaso(UUID id, UUID procesionId, int position, UUID titularId) {
        this.id = java.util.Objects.requireNonNull(id, "id");
        this.procesionId = java.util.Objects.requireNonNull(procesionId, "procesionId");
        this.position = position;
        this.titularId = java.util.Objects.requireNonNull(titularId, "titularId");
    }

    // ponytail: private all-args for adapter reconstruction
    private KnownPaso(UUID id, UUID procesionId, int position, UUID titularId, boolean ignored) {
        this.id = id;
        this.procesionId = procesionId;
        this.position = position;
        this.titularId = titularId;
    }

    public static KnownPaso reconstruct(UUID id, UUID procesionId, int position, UUID titularId) {
        return new KnownPaso(id, procesionId, position, titularId, true);
    }

    public UUID getId() { return id; }
    public UUID getProcesionId() { return procesionId; }
    public int getPosition() { return position; }
    public UUID getTitularId() { return titularId; }
}
