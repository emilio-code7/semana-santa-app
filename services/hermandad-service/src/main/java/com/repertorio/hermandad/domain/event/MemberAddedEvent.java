package com.repertorio.hermandad.domain.event;

import com.repertorio.common.event.DomainEvent;
import com.repertorio.hermandad.domain.model.HermandadMember;
import com.repertorio.hermandad.domain.model.HermandadRole;

import java.time.Instant;
import java.util.UUID;

public record MemberAddedEvent(
        UUID memberId,
        UUID hermandadId,
        String userId,
        HermandadRole role,
        UUID eventId,
        Instant occurredAt
) implements DomainEvent {
    public MemberAddedEvent(UUID memberId, UUID hermandadId, String userId, HermandadRole role) {
        this(memberId, hermandadId, userId, role, UUID.randomUUID(), Instant.now());
    }

    @Override
    public String aggregateType() { return HermandadMember.DOMAIN_EVENT_AGGREGATE_TYPE; }

    @Override
    public UUID aggregateId() { return memberId; }

    @Override
    public String eventType() { return "MEMBER_ADDED"; }

    @Override
    public int schemaVersion() { return 1; }
}
