package com.repertorio.marcha.adapter.outbound.persistence;

import jakarta.persistence.*;
import org.springframework.data.domain.Persistable;
import java.util.UUID;

@Entity
@Table(name = "cruceta_item")
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

    @Column(name = "route_section_id", nullable = false)
    private UUID routeSectionId;

    @Column(name = "sequence_within_section", nullable = false)
    private int sequenceWithinSection;

    @Column(name = "notes", length = 1000)
    private String notes;

    protected CrucetaItemEntity() {}

    public CrucetaItemEntity(UUID id, UUID crucetaId, UUID marchaId, UUID routeSectionId,
                             int sequenceWithinSection, String notes) {
        this.id = id;
        this.crucetaId = crucetaId;
        this.marchaId = marchaId;
        this.routeSectionId = routeSectionId;
        this.sequenceWithinSection = sequenceWithinSection;
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
    public UUID getRouteSectionId() { return routeSectionId; }
    public int getSequenceWithinSection() { return sequenceWithinSection; }
    public String getNotes() { return notes; }

    public void setCrucetaId(UUID crucetaId) { this.crucetaId = crucetaId; }
}
