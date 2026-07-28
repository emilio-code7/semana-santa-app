package com.repertorio.marcha.adapter.outbound.persistence;

import jakarta.persistence.*;
import org.springframework.data.domain.Persistable;
import java.util.UUID;

@Entity
@Table(name = "cruceta_item",
       uniqueConstraints = @UniqueConstraint(columnNames = {"cruceta_id", "order_index"}))
public class CrucetaItemEntity implements Persistable<UUID> {

    @Id
    private UUID id;

    @Transient
    private boolean isNew = true;

    @Version
    private int version;

    @Column(name = "cruceta_id", nullable = false)
    private UUID crucetaId;

    @Column(name = "marcha_id", nullable = false)
    private UUID marchaId;

    @Column(name = "order_index", nullable = false)
    private int orderIndex;

    @Column(name = "notes", length = 1000)
    private String notes;

    protected CrucetaItemEntity() {}

    public CrucetaItemEntity(UUID id, UUID crucetaId, UUID marchaId, int orderIndex, String notes) {
        this.id = id;
        this.crucetaId = crucetaId;
        this.marchaId = marchaId;
        this.orderIndex = orderIndex;
        this.notes = notes;
    }

    @Override
    public UUID getId() { return id; }

    @Override
    public boolean isNew() { return isNew; }

    @PostLoad
    void markNotNew() { this.isNew = false; }

    public int getVersion() { return version; }
    public UUID getCrucetaId() { return crucetaId; }
    public UUID getMarchaId() { return marchaId; }
    public int getOrderIndex() { return orderIndex; }
    public String getNotes() { return notes; }

    public void setCrucetaId(UUID crucetaId) { this.crucetaId = crucetaId; }
}
