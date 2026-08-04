package com.repertorio.marcha.adapter.outbound.events;

import com.repertorio.marcha.application.port.ProcessedEventStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ProcessedEventStoreAdapter implements ProcessedEventStore {

    static final String CONSUMER_NAME = "repertorio-service";

    private final ProcessedEventJpaRepository jpaRepository;

    @Override
    public boolean claim(UUID eventId) {
        return jpaRepository.tryClaim(eventId, CONSUMER_NAME, Instant.now()) > 0;
    }
}
