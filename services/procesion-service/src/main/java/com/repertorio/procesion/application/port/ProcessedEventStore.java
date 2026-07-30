package com.repertorio.procesion.application.port;

import java.util.UUID;

public interface ProcessedEventStore {
    boolean exists(UUID eventId);
    void record(UUID eventId);
}
