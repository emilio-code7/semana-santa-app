package com.repertorio.hermandad.domain.model;

import java.util.UUID;

public class HermandadMemberNotFoundException extends RuntimeException {
    public HermandadMemberNotFoundException(UUID hermandadId, String userId) {
        super("Hermandad member with hermandadId " + hermandadId + " not found for userId " + userId);
    }
}
