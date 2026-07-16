package com.repertorio.marcha.adapter.outbound.persistence;

import jakarta.persistence.*;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.domain.Persistable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "cruceta", indexes = @Index(name = "idx_cruceta_procesion_id", columnList = "procesion_id", unique = true))
public class CrucetaEntity implements Persistable<UUID> {

    @Id
    @UuidGenerator
    private UUID id;

    @Version
    private int version;

    @Column(name = "procesion_id", nullable = false)
    private UUID procesionId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // ponytail: unidirectional @JoinColumn since crucetaItemEntity has plain UUID FK, no @ManyToOne
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "cruceta_id")
    private List<CrucetaItemEntity> items = new ArrayList<>();

    protected CrucetaEntity() {}

    public CrucetaEntity(UUID id, UUID procesionId, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.procesionId = procesionId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    @Override
    public UUID getId() { return id; }

    @Override
    public boolean isNew() { return id == null; }

    public UUID getProcesionId() { return procesionId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public List<CrucetaItemEntity> getItems() { return items; }

    public void setItems(List<CrucetaItemEntity> items) {
        this.items.clear();
        if (items != null) {
            items.forEach(item -> item.setCrucetaId(this.id));
            this.items.addAll(items);
        }
    }
}
