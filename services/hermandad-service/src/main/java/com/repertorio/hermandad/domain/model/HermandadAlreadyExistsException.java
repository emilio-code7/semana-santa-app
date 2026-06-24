package com.repertorio.hermandad.domain.model;

public class HermandadAlreadyExistsException extends RuntimeException {
    public HermandadAlreadyExistsException(String name) {
        super("Hermandad with name '" + name + "' already exists");
    }
}
