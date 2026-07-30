package com.repertorio.marcha.domain.model;

import java.util.Objects;
import java.util.UUID;

public class CrucetaProgression {

    private final UUID id;
    private final UUID crucetaId;
    private final UUID pasoId;
    private UUID currentRouteSectionId;
    private UUID currentCrucetaItemId; // nullable

    // ponytail: full constructor for adapter reconstruction
    private CrucetaProgression(UUID id, UUID crucetaId, UUID pasoId,
                               UUID currentRouteSectionId, UUID currentCrucetaItemId) {
        this.id = Objects.requireNonNull(id, "id");
        this.crucetaId = Objects.requireNonNull(crucetaId, "crucetaId");
        this.pasoId = Objects.requireNonNull(pasoId, "pasoId");
        this.currentRouteSectionId = Objects.requireNonNull(currentRouteSectionId, "currentRouteSectionId");
        this.currentCrucetaItemId = currentCrucetaItemId;
    }

    public CrucetaProgression(UUID crucetaId, UUID pasoId, UUID currentRouteSectionId) {
        this(UUID.randomUUID(), crucetaId, pasoId, currentRouteSectionId, null);
    }

    public static CrucetaProgression reconstruct(UUID id, UUID crucetaId, UUID pasoId,
                                                  UUID currentRouteSectionId, UUID currentCrucetaItemId) {
        return new CrucetaProgression(id, crucetaId, pasoId, currentRouteSectionId, currentCrucetaItemId);
    }

    public UUID getId() { return id; }
    public UUID getCrucetaId() { return crucetaId; }
    public UUID getPasoId() { return pasoId; }
    public UUID getCurrentRouteSectionId() { return currentRouteSectionId; }
    public java.util.Optional<UUID> getCurrentCrucetaItemId() { return java.util.Optional.ofNullable(currentCrucetaItemId); }

    public void advance(UUID routeSectionId, UUID crucetaItemId) {
        this.currentRouteSectionId = Objects.requireNonNull(routeSectionId, "routeSectionId");
        this.currentCrucetaItemId = crucetaItemId;
    }

    public void clear() {
        this.currentRouteSectionId = null;
        this.currentCrucetaItemId = null;
    }
}
