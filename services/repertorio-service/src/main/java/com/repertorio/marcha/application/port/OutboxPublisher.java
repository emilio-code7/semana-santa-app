package com.repertorio.marcha.application.port;

import com.repertorio.marcha.domain.event.DomainEvent;

public interface OutboxPublisher {
    void publish(DomainEvent domainEvent);
}
