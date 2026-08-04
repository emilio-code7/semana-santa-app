package com.repertorio.procesion.adapter.outbound.events;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

public interface ProcessedEventJpaRepository extends JpaRepository<ProcessedEventEntity, UUID> {

    /**
     * Atomically claims (consumer_name, event_id): inserts the row, or does nothing if
     * another replica already claimed it. Returns the number of rows inserted — 1 means
     * this caller owns the claim, 0 means duplicate. Unlike check-then-insert there is
     * no race window between the existence check and the write.
     */
    @Modifying
    @Transactional
    @Query(value = """
            INSERT INTO processed_event (event_id, consumer_name, processed_at)
            VALUES (:eventId, :consumerName, :processedAt)
            ON CONFLICT (consumer_name, event_id) DO NOTHING
            """, nativeQuery = true)
    int tryClaim(@Param("eventId") UUID eventId,
                 @Param("consumerName") String consumerName,
                 @Param("processedAt") Instant processedAt);
}
