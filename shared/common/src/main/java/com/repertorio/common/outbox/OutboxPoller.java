package com.repertorio.common.outbox;

import com.repertorio.common.messaging.MessageSender;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxPoller {

    private final OutboxEventJpaRepository outboxEventRepository;
    private final MessageSender messageSender;

    @Scheduled(fixedDelayString = "PT5S")
    public void processPendingOutbox() {
        List<OutboxEventEntity> events = outboxEventRepository
                .findTop100ByProcessedFalseOrderByCreatedAtAsc();
        for (OutboxEventEntity event : events) {
            messageSender.send(event.getAggregateType() + "-events", event.getPayload())
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Error sending outbox event {}: {}", event.getId(), ex.getMessage());
                    } else {
                        event.markAsProcessed();
                        outboxEventRepository.save(event);
                    }
                });
        }
    }
}
