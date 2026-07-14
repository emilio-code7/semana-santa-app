package com.repertorio.marcha.adapter.outbound.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.repertorio.marcha.application.port.OutboxPublisher;
import com.repertorio.marcha.domain.event.DomainEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OutboxEventPublisher implements OutboxPublisher {

    private final OutboxEventJpaRepository repository;
    private final ObjectMapper objectMapper;

    @Override
    public void publish(DomainEvent domainEvent) {
        try {
            String json = objectMapper.writeValueAsString(domainEvent);
            String eventType = domainEvent.getClass().getSimpleName();
            repository.save(new OutboxEventEntity(
                    domainEvent.aggregateType(),
                    domainEvent.aggregateId(),
                    eventType,
                    json));
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize outbox payload", e);
        }
    }
}
