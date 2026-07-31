package com.repertorio.marcha.domain.model;

import java.util.UUID;

public class CrucetaItem {
    private final UUID id;
    private final int version;
    private final UUID marchaId;
    private final UUID routeSectionId;
    private final int sequenceWithinSection;
    private final String notes;

    public CrucetaItem(UUID marchaId, UUID routeSectionId, int sequenceWithinSection, String notes) {
        this.id = UUID.randomUUID();
        this.version = 0;
        this.marchaId = marchaId;
        this.routeSectionId = routeSectionId;
        this.sequenceWithinSection = sequenceWithinSection;
        this.notes = notes;
    }

    // ponytail: reconstruct for adapter mapping
    private CrucetaItem(UUID id, int version, UUID marchaId, UUID routeSectionId,
                        int sequenceWithinSection, String notes) {
        this.id = id;
        this.version = version;
        this.marchaId = marchaId;
        this.routeSectionId = routeSectionId;
        this.sequenceWithinSection = sequenceWithinSection;
        this.notes = notes;
    }

    public static CrucetaItem reconstruct(UUID id, int version, UUID marchaId, UUID routeSectionId,
                                           int sequenceWithinSection, String notes) {
        return new CrucetaItem(id, version, marchaId, routeSectionId, sequenceWithinSection, notes);
    }

    public UUID getId() { return id; }
    public int getVersion() { return version; }
    public UUID getMarchaId() { return marchaId; }
    public UUID getRouteSectionId() { return routeSectionId; }
    public int getSequenceWithinSection() { return sequenceWithinSection; }
    public String getNotes() { return notes; }
}
