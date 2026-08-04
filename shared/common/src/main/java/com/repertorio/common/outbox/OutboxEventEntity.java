package com.repertorio.common.outbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.domain.Persistable;

@Entity
@Table(name = "outbox_event")
@Getter
public class OutboxEventEntity implements Persistable<UUID> {

    @Id
    @UuidGenerator
    private UUID id;

    @Column(nullable = false)
    private String aggregateType;

    @Column(nullable = false)
    private UUID aggregateId;

    @Column(nullable = false)
    private String eventType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column
    private Instant processedAt;

    @Column
    private Boolean processed;

    @Column(nullable = false, updatable = false)
    private UUID eventId;

    @Column(nullable = false, updatable = false)
    private Instant occurredAt;

    @Column(nullable = false, updatable = false)
    private int schemaVersion;

    @Column
    private String claimedBy;

    @Column
    private Instant claimedAt;

    @Column(nullable = false)
    private int retryCount;

    @Column
    private Instant nextAttemptAt;

    @Column(columnDefinition = "TEXT")
    private String lastError;

    @Column(nullable = false)
    private boolean terminal;

    protected OutboxEventEntity() {}

    public OutboxEventEntity(String aggregateType, UUID aggregateId, String eventType, String payload,
                             UUID eventId, Instant occurredAt, int schemaVersion) {
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.payload = payload;
        this.eventId = eventId;
        this.occurredAt = occurredAt;
        this.schemaVersion = schemaVersion;
        this.processed = false;
        this.retryCount = 0;
        this.terminal = false;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public void markAsProcessed() {
        this.processed = true;
        this.processedAt = Instant.now();
        clearClaim();
    }

    public void claim(String instanceId, Instant now) {
        this.claimedBy = instanceId;
        this.claimedAt = now;
    }

    public void clearClaim() {
        this.claimedBy = null;
        this.claimedAt = null;
    }

    public void recordFailure(String error, Instant now, Duration backoff, int maxRetries) {
        this.retryCount++;
        this.lastError = error;
        clearClaim();
        if (retryCount >= maxRetries) {
            this.terminal = true;
            this.nextAttemptAt = null;
        } else {
            this.nextAttemptAt = now.plus(backoff.multipliedBy(1L << (retryCount - 1)));
        }
    }

    @Override
    public boolean isNew() {
        return id == null;
    }
}
