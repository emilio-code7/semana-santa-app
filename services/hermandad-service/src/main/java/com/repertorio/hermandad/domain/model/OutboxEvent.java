package com.repertorio.hermandad.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox_event")
@Getter
public class OutboxEvent {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(nullable = false)
    private String aggregateType;

    @Column(nullable = false)
    private UUID aggregateId;

    @Column(nullable = false)
    private String eventType;

    @Column(nullable = false)
    private String payload;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column
    private Instant processedAt;

    @Column
    private Boolean processed;

    protected OutboxEvent() {}

    public OutboxEvent(String aggregateType, UUID aggregateId, String eventType, String payload) {
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.payload = payload;
        this.processed = false;
    }

    @PrePersist
    public void prePersist() {
        this.createdAt = Instant.now();
    }

    public void markAsProcessed() {
        this.processed = true;
        this.processedAt = Instant.now();
    }

}
