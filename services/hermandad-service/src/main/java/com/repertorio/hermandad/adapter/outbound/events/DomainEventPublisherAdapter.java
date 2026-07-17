package com.repertorio.hermandad.adapter.outbound.events;


import com.repertorio.common.event.DomainEvent;
import com.repertorio.common.outbox.OutboxPublisher;
import com.repertorio.hermandad.application.port.DomainEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DomainEventPublisherAdapter implements DomainEventPublisher {

    private final OutboxPublisher outboxPublisher;
    private final ApplicationEventPublisher applicationEventPublisher;


    @Override
    public void publish(DomainEvent domainEvent) {
        applicationEventPublisher.publishEvent(domainEvent);
        outboxPublisher.publish(domainEvent);
    }
}
