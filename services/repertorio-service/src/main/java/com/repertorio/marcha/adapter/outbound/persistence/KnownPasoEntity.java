package com.repertorio.marcha.adapter.outbound.persistence;

import com.repertorio.marcha.domain.model.KnownPaso;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

@Entity
@Table(name = "known_paso")
public class KnownPasoEntity {

    @Id
    @UuidGenerator
    private UUID id;

    @Column(name = "procesion_id", nullable = false)
    private UUID procesionId;

    @Column(nullable = false)
    private int position;

    @Column(name = "titular_id", nullable = false)
    private UUID titularId;

    protected KnownPasoEntity() {}

    private KnownPasoEntity(UUID id, UUID procesionId, int position, UUID titularId) {
        this.id = id;
        this.procesionId = procesionId;
        this.position = position;
        this.titularId = titularId;
    }

    public static KnownPasoEntity from(KnownPaso domain) {
        return new KnownPasoEntity(
                domain.getId(),
                domain.getProcesionId(),
                domain.getPosition(),
                domain.getTitularId()
        );
    }

    public KnownPaso toDomain() {
        return KnownPaso.reconstruct(id, procesionId, position, titularId);
    }

    public UUID getId() { return id; }
    public UUID getProcesionId() { return procesionId; }
    public int getPosition() { return position; }
    public UUID getTitularId() { return titularId; }
}
