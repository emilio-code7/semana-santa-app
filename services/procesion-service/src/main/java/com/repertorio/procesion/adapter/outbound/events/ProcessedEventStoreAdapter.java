package com.repertorio.procesion.adapter.outbound.events;

import com.repertorio.procesion.application.port.ProcessedEventStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ProcessedEventStoreAdapter implements ProcessedEventStore {

    static final String CONSUMER_NAME = "procesion-service";

    private final ProcessedEventJpaRepository jpaRepository;

    @Override
    public boolean claim(UUID eventId) {
        return jpaRepository.tryClaim(eventId, CONSUMER_NAME, Instant.now()) > 0;
    }
}
