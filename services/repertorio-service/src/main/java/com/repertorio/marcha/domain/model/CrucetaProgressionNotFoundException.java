package com.repertorio.marcha.domain.model;

import java.util.UUID;

public class CrucetaProgressionNotFoundException extends RuntimeException {
    public CrucetaProgressionNotFoundException(UUID pasoId) {
        super("Progression not found for paso: " + pasoId);
    }
}
