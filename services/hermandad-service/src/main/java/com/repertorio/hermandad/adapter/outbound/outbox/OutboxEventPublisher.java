package com.repertorio.hermandad.adapter.outbound.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.repertorio.hermandad.application.port.EventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OutboxEventPublisher implements EventPublisher {

    private final OutboxEventJpaRepository repository;
    private final ObjectMapper objectMapper;

    @Override
    public void publish(String aggregateType, UUID aggregateId, String eventType, Object payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            repository.save(new OutboxEventEntity(aggregateType, aggregateId, eventType, json));
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize outbox payload", e);
        }
    }

}
