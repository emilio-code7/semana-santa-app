package com.repertorio.hermandad.application.event;

import com.repertorio.hermandad.domain.model.HermandadRole;

public record MemberAddedEvent (
        String userId,
        HermandadRole role
) {
}
