package com.repertorio.common.event;

import java.time.Instant;
import java.util.UUID;

public interface DomainEvent {
    UUID eventId();
    Instant occurredAt();
    String aggregateType();
    UUID aggregateId();
    String eventType();

    /**
     * Version of the event envelope schema this event adheres to.
     *
     * <p>TEMPORARY expand-step default (Ticket 10): every concrete event must implement
     * {@code schemaVersion()} explicitly and this default is removed MANDATORILY at Gate 15
     * (tickets 12-14) so missing metadata cannot be hidden.
     */
    default int schemaVersion() {
        return 1;
    }
}
