package com.repertorio.procesion.application.port;

public interface OutboxPublisher {
    void publish(DomainEvent domainEvent);
}
