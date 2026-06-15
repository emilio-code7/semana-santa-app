package com.repertorio.hermandad.domain.model;

import java.util.UUID;

public class HermandadNotFoundException extends RuntimeException {
    public HermandadNotFoundException(UUID hermandadId) {
        super("Hermandad with id: " + hermandadId + " not found");
    }
}
