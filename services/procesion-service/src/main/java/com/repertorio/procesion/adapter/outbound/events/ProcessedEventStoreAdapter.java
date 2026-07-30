package com.repertorio.procesion.adapter.outbound.events;

import com.repertorio.procesion.application.port.ProcessedEventStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ProcessedEventStoreAdapter implements ProcessedEventStore {

    static final String CONSUMER_NAME = "procesion-service";

    private final ProcessedEventJpaRepository jpaRepository;

    @Override
    public boolean exists(UUID eventId) {
        return jpaRepository.existsById(eventId);
    }

    @Override
    public void record(UUID eventId) {
        jpaRepository.save(new ProcessedEventEntity(eventId, CONSUMER_NAME));
    }
}
