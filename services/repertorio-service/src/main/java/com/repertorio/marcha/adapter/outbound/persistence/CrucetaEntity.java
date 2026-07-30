package com.repertorio.marcha.adapter.outbound.persistence;

import jakarta.persistence.*;
import org.springframework.data.domain.Persistable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "cruceta", indexes = @Index(name = "idx_cruceta_paso_id", columnList = "paso_id", unique = true))
public class CrucetaEntity implements Persistable<UUID> {

    @Id
    private UUID id;

    @Transient
    private boolean isNew = true;

    @Version
    private int version;

    @Column(name = "paso_id", nullable = false)
    private UUID pasoId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // ponytail: unidirectional @JoinColumn since crucetaItemEntity has plain UUID FK, no @ManyToOne
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "cruceta_id")
    private List<CrucetaItemEntity> items = new ArrayList<>();

    protected CrucetaEntity() {}

    public CrucetaEntity(UUID id, UUID pasoId, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.pasoId = pasoId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    @Override
    public UUID getId() { return id; }

    @Override
    public boolean isNew() { return isNew; }

    @PostLoad
    void markNotNew() { this.isNew = false; }

    public int getVersion() { return version; }
    public UUID getPasoId() { return pasoId; }
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

    // ponytail: package-private setter for adapter use only
    void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
