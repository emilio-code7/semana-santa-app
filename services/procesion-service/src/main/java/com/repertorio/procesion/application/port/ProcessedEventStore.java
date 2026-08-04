package com.repertorio.procesion.application.port;

import java.util.UUID;

public interface ProcessedEventStore {
    /** Atomically claims (consumer_name, event_id). True = this caller owns the claim and must process. */
    boolean claim(UUID eventId);
}
