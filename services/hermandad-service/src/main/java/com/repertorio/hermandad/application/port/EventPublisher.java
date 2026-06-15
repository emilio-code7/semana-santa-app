package com.repertorio.hermandad.application.port;

import java.util.UUID;

public interface EventPublisher {
    void publish(String aggregateType, UUID aggregateId, String eventType, Object payload);
}
