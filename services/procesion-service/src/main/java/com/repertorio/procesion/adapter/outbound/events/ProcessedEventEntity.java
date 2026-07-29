package com.repertorio.procesion.adapter.outbound.events;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "processed_event")
public class ProcessedEventEntity {

    @Id
    private UUID eventId;

    @Column(nullable = false, length = 100)
    private String consumerName;

    @Column(nullable = false)
    private Instant processedAt;

    protected ProcessedEventEntity() {}

    public ProcessedEventEntity(UUID eventId, String consumerName) {
        this.eventId = eventId;
        this.consumerName = consumerName;
        this.processedAt = Instant.now();
    }

    public UUID getEventId() { return eventId; }
    public String getConsumerName() { return consumerName; }
    public Instant getProcessedAt() { return processedAt; }
}
