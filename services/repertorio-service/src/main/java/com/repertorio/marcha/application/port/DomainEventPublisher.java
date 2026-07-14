package com.repertorio.marcha.application.port;

import com.repertorio.marcha.domain.event.DomainEvent;

public interface DomainEventPublisher {
    void publish(DomainEvent domainEvent);
}
