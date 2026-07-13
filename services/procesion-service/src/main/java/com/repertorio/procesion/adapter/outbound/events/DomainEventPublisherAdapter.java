package com.repertorio.procesion.adapter.outbound.events;

import com.repertorio.procesion.application.port.DomainEvent;
import com.repertorio.procesion.application.port.DomainEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DomainEventPublisherAdapter implements DomainEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    public void publish(DomainEvent domainEvent) {
        log.info("Publishing domain event: aggregateType={}, aggregateId={}, eventType={}",
                domainEvent.aggregateType(), domainEvent.aggregateId(), domainEvent.eventType());
        applicationEventPublisher.publishEvent(domainEvent);
    }
}
