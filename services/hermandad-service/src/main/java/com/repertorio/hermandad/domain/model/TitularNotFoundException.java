package com.repertorio.hermandad.domain.model;

import java.util.UUID;

public class TitularNotFoundException extends RuntimeException {
    public TitularNotFoundException(UUID id) {
        super("Titular not found: " + id);
    }
}
