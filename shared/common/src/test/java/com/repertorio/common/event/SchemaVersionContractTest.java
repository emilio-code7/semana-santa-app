package com.repertorio.common.event;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SchemaVersionContractTest {

    @Test
    void anonymousEventDefaultsToSchemaVersionOne() {
        // TEMPORARY expand-step default (Ticket 10): every concrete event must implement
        // schemaVersion() explicitly and the interface default is removed MANDATORILY at
        // Gate 15 (tickets 12-14) so missing metadata cannot be hidden.
        DomainEvent event = new DomainEvent() {
            @Override
            public UUID eventId() {
                return UUID.randomUUID();
            }

            @Override
            public Instant occurredAt() {
                return Instant.now();
            }

            @Override
            public String aggregateType() {
                return "test";
            }

            @Override
            public UUID aggregateId() {
                return UUID.randomUUID();
            }

            @Override
            public String eventType() {
                return "TEST_EVENT";
            }
        };

        assertEquals(1, event.schemaVersion());
    }
}
