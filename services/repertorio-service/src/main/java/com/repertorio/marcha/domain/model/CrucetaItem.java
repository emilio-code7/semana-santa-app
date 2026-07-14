package com.repertorio.marcha.domain.model;

import java.util.UUID;

public class CrucetaItem {
    private final UUID id;
    private final UUID marchaId;
    private final int orderIndex;
    private final String notes;

    public CrucetaItem(UUID marchaId, int orderIndex, String notes) {
        this.id = UUID.randomUUID();
        this.marchaId = marchaId;
        this.orderIndex = orderIndex;
        this.notes = notes;
    }

    // ponytail: reconstruct for adapter mapping
    private CrucetaItem(UUID id, UUID marchaId, int orderIndex, String notes) {
        this.id = id;
        this.marchaId = marchaId;
        this.orderIndex = orderIndex;
        this.notes = notes;
    }

    public static CrucetaItem reconstruct(UUID id, UUID marchaId, int orderIndex, String notes) {
        return new CrucetaItem(id, marchaId, orderIndex, notes);
    }

    public UUID getId() { return id; }
    public UUID getMarchaId() { return marchaId; }
    public int getOrderIndex() { return orderIndex; }
    public String getNotes() { return notes; }
}
