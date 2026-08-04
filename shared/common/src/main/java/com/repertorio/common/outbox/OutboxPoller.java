package com.repertorio.common.outbox;

import com.repertorio.common.messaging.MessageSender;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Component
@RequiredArgsConstructor
@Slf4j
@EnableConfigurationProperties(OutboxProperties.class)
public class OutboxPoller {

    private final OutboxEventJpaRepository outboxEventRepository;
    private final MessageSender messageSender;
    private final TransactionTemplate transactionTemplate;
    private final OutboxProperties properties;

    @Scheduled(fixedDelayString = "PT5S")
    public void processPendingOutbox() {
        for (OutboxEventEntity event : claimBatch()) {
            send(event);
        }
    }

    // Short claim transaction: SELECT ... FOR UPDATE SKIP LOCKED, write claim, COMMIT.
    List<OutboxEventEntity> claimBatch() {
        return transactionTemplate.execute(status -> {
            Instant now = Instant.now();
            List<OutboxEventEntity> events = outboxEventRepository.claimEligible(
                    now.minus(properties.claimTimeout()), now, properties.batchSize());
            events.forEach(e -> e.claim(properties.instanceId(), now));
            return outboxEventRepository.saveAll(events);
        });
    }

    // Publish OUTSIDE the claim transaction.
    void send(OutboxEventEntity event) {
        messageSender.send(event.getAggregateType() + "-events",
                event.getAggregateId(), event.getEventId(), event.getPayload())
            .whenComplete((result, ex) -> {
                if (ex != null) {
                    recordFailure(event, ex);
                } else {
                    markProcessed(event);
                }
            });
    }

    void markProcessed(OutboxEventEntity event) {
        transactionTemplate.executeWithoutResult(status -> {
            event.markAsProcessed();
            outboxEventRepository.save(event);
        });
    }

    void recordFailure(OutboxEventEntity event, Throwable ex) {
        transactionTemplate.executeWithoutResult(status -> {
            event.recordFailure(ex.getMessage(), Instant.now(),
                    properties.backoffInitial(), properties.maxRetries());
            outboxEventRepository.save(event);
        });
    }
}
