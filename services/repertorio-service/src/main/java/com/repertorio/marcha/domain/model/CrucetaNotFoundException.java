package com.repertorio.marcha.domain.model;

import java.util.UUID;

public class CrucetaNotFoundException extends RuntimeException {
    public CrucetaNotFoundException(UUID procesionId) {
        super("Cruceta not found for procesion: " + procesionId);
    }
}
