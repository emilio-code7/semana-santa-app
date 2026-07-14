package com.repertorio.marcha.domain.model;

import java.util.UUID;

public class MarchaNotFoundException extends RuntimeException {
    public MarchaNotFoundException(UUID id) {
        super("Marcha not found: " + id);
    }
}
