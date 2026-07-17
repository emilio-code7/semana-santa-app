package com.repertorio.common.outbox;

import com.repertorio.common.event.DomainEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class OutboxEventPublisher implements OutboxPublisher {

    private final OutboxEventJpaRepository repository;
    private final ObjectMapper objectMapper;

    @Override
    public void publish(DomainEvent domainEvent) {
        try {
            String json = objectMapper.writeValueAsString(domainEvent);
            repository.save(new OutboxEventEntity(
                    domainEvent.aggregateType(),
                    domainEvent.aggregateId(),
                    domainEvent.eventType(),
                    json));
        } catch (JacksonException e) {
            throw new RuntimeException("Failed to serialize outbox payload", e);
        }
    }
}
