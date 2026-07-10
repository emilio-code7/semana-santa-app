package com.repertorio.procesion.application.port;

import java.util.UUID;

public interface DomainEvent {
    String aggregateType();
    UUID aggregateId();
    String eventType();
}
