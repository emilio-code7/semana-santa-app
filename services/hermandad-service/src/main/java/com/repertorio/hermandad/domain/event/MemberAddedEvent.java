package com.repertorio.hermandad.domain.event;

import com.repertorio.hermandad.application.port.DomainEvent;
import com.repertorio.hermandad.domain.model.HermandadMember;
import com.repertorio.hermandad.domain.model.HermandadRole;

import java.util.UUID;

public record MemberAddedEvent (
        UUID memberId,
        UUID hermandadId,
        String userId,
        HermandadRole role
) implements DomainEvent {

    @Override
    public String aggregateType() { return HermandadMember.DOMAIN_EVENT_AGGREGATE_TYPE; }

    @Override
    public UUID aggregateId() { return memberId; }

    @Override
    public String eventType() { return "MEMBER_ADDED"; }
}
