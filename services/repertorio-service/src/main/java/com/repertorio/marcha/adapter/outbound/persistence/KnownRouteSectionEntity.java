package com.repertorio.marcha.adapter.outbound.persistence;

import com.repertorio.marcha.domain.model.KnownRouteSection;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

@Entity
@Table(name = "known_route_section")
public class KnownRouteSectionEntity {

    @Id
    @UuidGenerator
    private UUID id;

    @Column(name = "procesion_id", nullable = false)
    private UUID procesionId;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false)
    private int position;

    @Column(length = 1000)
    private String notes;

    protected KnownRouteSectionEntity() {}

    private KnownRouteSectionEntity(UUID id, UUID procesionId, String name, int position, String notes) {
        this.id = id;
        this.procesionId = procesionId;
        this.name = name;
        this.position = position;
        this.notes = notes;
    }

    public static KnownRouteSectionEntity from(KnownRouteSection domain) {
        return new KnownRouteSectionEntity(
                domain.getId(),
                domain.getProcesionId(),
                domain.getName(),
                domain.getPosition(),
                domain.getNotes()
        );
    }

    public KnownRouteSection toDomain() {
        return KnownRouteSection.reconstruct(id, procesionId, name, position, notes);
    }

    public UUID getId() { return id; }
    public UUID getProcesionId() { return procesionId; }
    public String getName() { return name; }
    public int getPosition() { return position; }
    public String getNotes() { return notes; }
}
