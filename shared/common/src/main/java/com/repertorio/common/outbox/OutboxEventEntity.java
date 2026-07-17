package com.repertorio.common.outbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
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

    protected OutboxEventEntity() {}

    public OutboxEventEntity(String aggregateType, UUID aggregateId, String eventType, String payload) {
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.payload = payload;
        this.processed = false;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public void markAsProcessed() {
        this.processed = true;
        this.processedAt = Instant.now();
    }

    @Override
    public boolean isNew() {
        return id == null;
    }
}
