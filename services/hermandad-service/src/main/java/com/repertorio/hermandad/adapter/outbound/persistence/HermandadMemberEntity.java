package com.repertorio.hermandad.adapter.outbound.persistence;

import com.repertorio.hermandad.domain.model.HermandadMember;
import com.repertorio.hermandad.domain.model.HermandadRole;
import jakarta.persistence.*;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.domain.Persistable;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "hermandad_member")
public class HermandadMemberEntity implements Persistable<UUID>, Serializable {

    @Id
    @UuidGenerator
    private UUID id;

    @Version
    private int version;

    @Column(nullable = false, updatable = false)
    private UUID hermandadId;

    @Column(nullable = false, updatable = false)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private HermandadRole role;

    @Column(nullable = false, updatable = false)
    private Instant joinedAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected HermandadMemberEntity() {}

    public HermandadMemberEntity(UUID id, UUID hermandadId, String userId, HermandadRole role,
                                 Instant joinedAt, Instant updatedAt) {
        this.id = id;
        this.hermandadId = hermandadId;
        this.userId = userId;
        this.role = role;
        this.joinedAt = joinedAt;
        this.updatedAt = updatedAt;
    }

    @PrePersist
    public void prePersist() {
        this.joinedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }

    public static HermandadMemberEntity from(HermandadMember domain) {
        return new HermandadMemberEntity(
                domain.getId(),
                domain.getHermandadId(),
                domain.getUserId(),
                domain.getRole(),
                domain.getJoinedAt(),
                domain.getUpdatedAt()
        );
    }

    public HermandadMember toDomain() {
        return HermandadMember.reconstruct(
                id, hermandadId, userId, role, joinedAt, updatedAt
        );
    }

    @Override
    public UUID getId() { return id; }
    public UUID getHermandadId() { return hermandadId; }
    public String getUserId() { return userId; }
    public HermandadRole getRole() { return role; }
    public Instant getJoinedAt() { return joinedAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    @Override
    public boolean isNew() {
        return false; // IDs are always provided by the domain
    }
}
