package com.repertorio.marcha.adapter.outbound.events;

import com.repertorio.common.outbox.OutboxPublisher;
import com.repertorio.marcha.application.port.DomainEventPublisher;
import com.repertorio.common.event.DomainEvent;
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
