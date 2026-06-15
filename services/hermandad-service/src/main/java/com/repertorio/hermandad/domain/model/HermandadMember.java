package com.repertorio.hermandad.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "hermandad_member")
@Getter
@Setter
public class HermandadMember implements Serializable {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(nullable = false, updatable = false)
    private UUID hermandadId;

    @Column(nullable = false, updatable = false)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private HermandadRole role;

    @Column(nullable = false, updatable = false)
    private Instant joinedAt;

    @Column(nullable = false, updatable = false)
    private Instant updatedAt;

    protected HermandadMember() {}

    public HermandadMember(UUID hermandadId, String userId, HermandadRole role) {
        this.hermandadId = hermandadId;
        this.userId = userId;
        this.role = role;
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
}
