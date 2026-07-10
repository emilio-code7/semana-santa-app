package com.repertorio.procesion.domain.model;

import java.util.UUID;

public class ProcesionNotFoundException extends RuntimeException {

    public ProcesionNotFoundException(UUID id) {
        super("Procesión no encontrada: " + id);
    }
}
