package com.repertorio.common.outbox;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OutboxEventJpaRepository extends JpaRepository<OutboxEventEntity, UUID> {

    /**
     * Multi-replica safe claim: picks the oldest unprocessed, non-terminal row per aggregate
     * whose claim has expired and whose retry backoff has elapsed. The {@code NOT EXISTS}
     * guard excludes any aggregate that has an active (non-expired) claim anywhere, so no
     * instance can jump ahead to a newer row while another instance holds its oldest row.
     * {@code FOR UPDATE SKIP LOCKED} makes concurrent claims from different replicas safe:
     * each row is locked by exactly one claimer.
     */
    @Query(value = """
            SELECT * FROM outbox_event o
            WHERE o.id IN (
                SELECT DISTINCT ON (e.aggregate_id) e.id
                FROM outbox_event e
                WHERE e.processed = FALSE
                  AND e.terminal = FALSE
                  AND (e.claimed_at IS NULL OR e.claimed_at < :claimCutoff)
                  AND (e.next_attempt_at IS NULL OR e.next_attempt_at <= :now)
                  AND NOT EXISTS (
                      SELECT 1 FROM outbox_event active
                      WHERE active.aggregate_id = e.aggregate_id
                        AND active.claimed_at IS NOT NULL
                        AND active.claimed_at >= :claimCutoff
                  )
                ORDER BY e.aggregate_id, e.created_at, e.id
            )
            ORDER BY o.created_at, o.id
            LIMIT :batchSize
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<OutboxEventEntity> claimEligible(@Param("claimCutoff") Instant claimCutoff,
                                          @Param("now") Instant now,
                                          @Param("batchSize") int batchSize);
}
