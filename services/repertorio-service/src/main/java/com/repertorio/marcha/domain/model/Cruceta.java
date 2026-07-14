package com.repertorio.marcha.domain.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Cruceta {

    private final UUID id;
    private final UUID procesionId;
    private List<CrucetaItem> items;
    private Instant createdAt;
    private Instant updatedAt;

    // JPA reconstruction
    protected Cruceta() {
        this.id = null;
        this.procesionId = null;
    }

    public Cruceta(UUID procesionId, List<CrucetaItem> items) {
        this.id = UUID.randomUUID();
        this.procesionId = procesionId;
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
        var orderIndexes = items.stream().map(CrucetaItem::getOrderIndex).toList();
        if (orderIndexes.stream().distinct().count() != orderIndexes.size()) {
            throw new IllegalArgumentException("duplicate orderIndex values");
        }
        this.items = new ArrayList<>(items);
    }

    public boolean containsMarcha(UUID marchaId) {
        return items.stream().anyMatch(item -> item.getMarchaId().equals(marchaId));
    }

    // ponytail: reconstruct for adapter mapping
    private Cruceta(UUID id, UUID procesionId, List<CrucetaItem> items,
                    Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.procesionId = procesionId;
        this.items = new ArrayList<>(items);
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Cruceta reconstruct(UUID id, UUID procesionId, List<CrucetaItem> items,
                                       Instant createdAt, Instant updatedAt) {
        return new Cruceta(id, procesionId, items, createdAt, updatedAt);
    }

    public UUID getId() { return id; }
    public UUID getProcesionId() { return procesionId; }
    public List<CrucetaItem> getItems() { return List.copyOf(items); }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
