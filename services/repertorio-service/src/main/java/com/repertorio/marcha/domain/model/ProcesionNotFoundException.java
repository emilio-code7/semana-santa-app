package com.repertorio.marcha.domain.model;

import java.util.UUID;

public class ProcesionNotFoundException extends RuntimeException {
    public ProcesionNotFoundException(UUID procesionId) {
        super("Procesion not found: " + procesionId);
    }
}
