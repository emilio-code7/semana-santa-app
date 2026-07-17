package com.repertorio.marcha.application.port;

import com.repertorio.common.event.DomainEvent;

public interface DomainEventPublisher {
    void publish(DomainEvent domainEvent);
}
