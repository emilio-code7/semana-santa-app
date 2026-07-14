package com.repertorio.hermandad.adapter.outbound.outbox;

import tools.jackson.databind.ObjectMapper;
import com.repertorio.hermandad.application.port.DomainEvent;
import com.repertorio.hermandad.application.port.OutboxPublisher;
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
            repository.save(new OutboxEventEntity(domainEvent.aggregateType(), domainEvent.aggregateId(), domainEvent.eventType(), json));
        } catch (tools.jackson.core.JacksonException e) {
            throw new RuntimeException("Failed to serialize outbox payload", e);
        }
    }

}
