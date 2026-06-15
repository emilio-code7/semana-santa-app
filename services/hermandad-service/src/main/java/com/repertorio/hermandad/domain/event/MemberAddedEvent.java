package com.repertorio.hermandad.domain.event;

import com.repertorio.hermandad.domain.model.HermandadRole;

public record MemberAddedEvent (
        String userId,
        HermandadRole role
) {
}
