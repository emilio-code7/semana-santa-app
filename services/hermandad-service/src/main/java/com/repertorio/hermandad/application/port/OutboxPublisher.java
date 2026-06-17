package com.repertorio.hermandad.application.port;

public interface OutboxPublisher {
    void publish(DomainEvent domainEvent);
}
