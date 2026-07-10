package com.repertorio.procesion.application.port;

public interface DomainEventPublisher {
    void publish(DomainEvent domainEvent);
}
