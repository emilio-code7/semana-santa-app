package com.repertorio.marcha.domain.model;

import java.util.UUID;

public class VersionMismatchException extends RuntimeException {
    public VersionMismatchException(String entityName, UUID id, int expectedVersion, int actualVersion) {
        super(entityName + "[" + id + "] version mismatch: expected " + expectedVersion + " but DB has " + actualVersion);
    }
}
