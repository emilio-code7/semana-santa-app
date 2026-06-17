package com.repertorio.hermandad.application.port;

public interface DomainEventPublisher {
    void publish(DomainEvent domainEvent);
}
