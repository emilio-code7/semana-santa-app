package com.repertorio.hermandad.domain.event;

import com.repertorio.hermandad.application.port.DomainEvent;
import com.repertorio.hermandad.domain.model.HermandadRole;

import java.util.UUID;

public record MemberRoleChangedEvent(
        UUID memberId, UUID hermandadId, String userId, HermandadRole oldRole, HermandadRole newRole
) implements DomainEvent {
    @Override
    public String aggregateType() {
        return "hermandad-member";
    }
    @Override
    public UUID aggregateId() {
        return memberId;
    }
    @Override
    public String eventType() {
        return "MEMBER_ROLE_CHANGED";
    }
}
