package com.repertorio.procesion.adapter.outbound.persistence;

import com.repertorio.procesion.domain.model.RouteSection;
import jakarta.persistence.*;
import org.springframework.data.domain.Persistable;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "route_section",
        indexes = @Index(name = "idx_route_section_procesion_id", columnList = "procesion_id"))
public class RouteSectionEntity implements Persistable<UUID> {

    @Id
    private UUID id;

    @Transient
    private boolean isNew = true;

    @Version
    private long version;

    @Column(nullable = false)
    private UUID procesionId;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false)
    private int position;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected RouteSectionEntity() {}

    public RouteSectionEntity(UUID id, UUID procesionId, String name, int position,
                              String notes, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.procesionId = procesionId;
        this.name = name;
        this.position = position;
        this.notes = notes;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    @Override
    public boolean isNew() { return isNew; }

    @PostLoad
    void markNotNew() { this.isNew = false; }

    @PrePersist
    protected void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
        if (updatedAt == null) updatedAt = Instant.now();
    }

    @PreUpdate
    protected void preUpdate() {
        updatedAt = Instant.now();
    }

    public static RouteSectionEntity from(RouteSection domain) {
        return new RouteSectionEntity(
                domain.getId(),
                domain.getProcesionId(),
                domain.getName(),
                domain.getPosition(),
                domain.getNotes(),
                domain.getCreatedAt(),
                domain.getUpdatedAt()
        );
    }

    public RouteSection toDomain() {
        return RouteSection.reconstruct(id, procesionId, name, position, notes, createdAt, updatedAt);
    }

    @Override
    public UUID getId() { return id; }
    public UUID getProcesionId() { return procesionId; }
    public String getName() { return name; }
    public int getPosition() { return position; }
    public String getNotes() { return notes; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
