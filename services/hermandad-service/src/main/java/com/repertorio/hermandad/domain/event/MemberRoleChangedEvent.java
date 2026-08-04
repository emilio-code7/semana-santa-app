package com.repertorio.hermandad.domain.event;

import com.repertorio.common.event.DomainEvent;
import com.repertorio.hermandad.domain.model.HermandadMember;
import com.repertorio.hermandad.domain.model.HermandadRole;

import java.time.Instant;
import java.util.UUID;

public record MemberRoleChangedEvent(
        UUID memberId, UUID hermandadId, String userId, HermandadRole oldRole, HermandadRole newRole,
        UUID eventId, Instant occurredAt
) implements DomainEvent {
    public MemberRoleChangedEvent(UUID memberId, UUID hermandadId, String userId, HermandadRole oldRole, HermandadRole newRole) {
        this(memberId, hermandadId, userId, oldRole, newRole, UUID.randomUUID(), Instant.now());
    }

    @Override
    public String aggregateType() { return HermandadMember.DOMAIN_EVENT_AGGREGATE_TYPE; }

    @Override
    public UUID aggregateId() { return memberId; }

    @Override
    public String eventType() { return "MEMBER_ROLE_CHANGED"; }

    @Override
    public int schemaVersion() { return 1; }
}
