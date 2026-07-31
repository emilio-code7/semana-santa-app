package com.repertorio.marcha.domain.model;

import java.util.UUID;

public class PasoNotFoundException extends RuntimeException {
    public PasoNotFoundException(UUID pasoId) {
        super("Paso not found: " + pasoId);
    }
}
