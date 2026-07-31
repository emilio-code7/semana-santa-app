package com.repertorio.marcha.domain.model;

import java.util.UUID;

public class CrucetaNotFoundException extends RuntimeException {
    public CrucetaNotFoundException(UUID pasoId) {
        super("Cruceta not found for paso: " + pasoId);
    }
}
