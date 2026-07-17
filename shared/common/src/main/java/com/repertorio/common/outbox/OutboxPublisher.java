package com.repertorio.common.outbox;

import com.repertorio.common.event.DomainEvent;

public interface OutboxPublisher {
    void publish(DomainEvent domainEvent);
}
