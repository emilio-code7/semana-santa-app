package com.repertorio.hermandad.domain.model;

import lombok.Getter;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

@Getter
public class HermandadMember implements Serializable {

    public static final String DOMAIN_EVENT_AGGREGATE_TYPE = "hermandad-member";

    private UUID id;
    private UUID hermandadId;
    private String userId;
    private HermandadRole role;
    private Instant joinedAt;
    private Instant updatedAt;

    protected HermandadMember() {}

    public HermandadMember(UUID hermandadId, String userId, HermandadRole role) {
        this.hermandadId = hermandadId;
        this.userId = userId;
        this.role = role;
    }

    public void changeRole(HermandadRole newRole) {
        if (this.role == newRole) {
            throw new IllegalArgumentException("Member already has role " + newRole);
        }
        this.role = newRole;
    }

    // ponytail: private all-args constructor for adapter reconstruction
    private HermandadMember(UUID id, UUID hermandadId, String userId, HermandadRole role,
                            Instant joinedAt, Instant updatedAt) {
        this.id = id;
        this.hermandadId = hermandadId;
        this.userId = userId;
        this.role = role;
        this.joinedAt = joinedAt;
        this.updatedAt = updatedAt;
    }

    public static HermandadMember reconstruct(UUID id, UUID hermandadId, String userId, HermandadRole role,
                                               Instant joinedAt, Instant updatedAt) {
        return new HermandadMember(id, hermandadId, userId, role, joinedAt, updatedAt);
    }

}
