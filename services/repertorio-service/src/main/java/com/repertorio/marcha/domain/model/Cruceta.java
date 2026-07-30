package com.repertorio.marcha.domain.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Cruceta {

    private final UUID id;
    private final UUID pasoId;
    private int version;
    private List<CrucetaItem> items;
    private Instant createdAt;
    private Instant updatedAt;

    // JPA reconstruction
    protected Cruceta() {
        this.id = null;
        this.pasoId = null;
    }

    public Cruceta(UUID pasoId, List<CrucetaItem> items) {
        this.id = UUID.randomUUID();
        this.pasoId = pasoId;
        this.version = 0;
        setItems(items);
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void redefine(List<CrucetaItem> newItems) {
        setItems(newItems);
        this.updatedAt = Instant.now();
    }

    private void setItems(List<CrucetaItem> items) {
        if (items == null) throw new IllegalArgumentException("items must not be null");
        // ponytail: no duplicate (routeSectionId, sequenceWithinSection) check — items are replaced atomically
        this.items = new ArrayList<>(items);
    }

    public boolean containsMarcha(UUID marchaId) {
        return items.stream().anyMatch(item -> item.getMarchaId().equals(marchaId));
    }

    // ponytail: reconstruct for adapter mapping
    private Cruceta(UUID id, int version, UUID pasoId, List<CrucetaItem> items,
                    Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.version = version;
        this.pasoId = pasoId;
        this.items = new ArrayList<>(items);
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Cruceta reconstruct(UUID id, int version, UUID pasoId, List<CrucetaItem> items,
                                       Instant createdAt, Instant updatedAt) {
        return new Cruceta(id, version, pasoId, items, createdAt, updatedAt);
    }

    public UUID getId() { return id; }
    public int getVersion() { return version; }
    public UUID getPasoId() { return pasoId; }
    public List<CrucetaItem> getItems() { return List.copyOf(items); }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
