package com.repertorio.marcha.adapter.outbound.persistence;

import com.repertorio.marcha.domain.model.CrucetaProgression;
import jakarta.persistence.*;
import org.springframework.data.domain.Persistable;

import java.util.UUID;

@Entity
@Table(name = "cruceta_progression",
       uniqueConstraints = @UniqueConstraint(columnNames = {"cruceta_id", "paso_id"}))
public class CrucetaProgressionEntity implements Persistable<UUID> {

    @Id
    private UUID id;

    @Transient
    private boolean isNew = true;

    @Column(name = "cruceta_id", nullable = false)
    private UUID crucetaId;

    @Column(name = "paso_id", nullable = false)
    private UUID pasoId;

    @Column(name = "current_route_section_id", nullable = false)
    private UUID currentRouteSectionId;

    @Column(name = "current_cruceta_item_id")
    private UUID currentCrucetaItemId;

    protected CrucetaProgressionEntity() {}

    public CrucetaProgressionEntity(UUID id, UUID crucetaId, UUID pasoId,
                                     UUID currentRouteSectionId, UUID currentCrucetaItemId) {
        this.id = id;
        this.crucetaId = crucetaId;
        this.pasoId = pasoId;
        this.currentRouteSectionId = currentRouteSectionId;
        this.currentCrucetaItemId = currentCrucetaItemId;
    }

    @Override
    public UUID getId() { return id; }

    @Override
    public boolean isNew() { return isNew; }

    @PostLoad
    void markNotNew() { this.isNew = false; }

    public UUID getCrucetaId() { return crucetaId; }
    public UUID getPasoId() { return pasoId; }
    public UUID getCurrentRouteSectionId() { return currentRouteSectionId; }
    public UUID getCurrentCrucetaItemId() { return currentCrucetaItemId; }

    public void setCurrentRouteSectionId(UUID currentRouteSectionId) { this.currentRouteSectionId = currentRouteSectionId; }
    public void setCurrentCrucetaItemId(UUID currentCrucetaItemId) { this.currentCrucetaItemId = currentCrucetaItemId; }

    public static CrucetaProgressionEntity from(CrucetaProgression domain) {
        return new CrucetaProgressionEntity(
                domain.getId(),
                domain.getCrucetaId(),
                domain.getPasoId(),
                domain.getCurrentRouteSectionId(),
                domain.getCurrentCrucetaItemId().orElse(null)
        );
    }

    public CrucetaProgression toDomain() {
        return CrucetaProgression.reconstruct(id, crucetaId, pasoId, currentRouteSectionId, currentCrucetaItemId);
    }
}
