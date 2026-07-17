package com.repertorio.common.outbox;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxPoller {

    private final OutboxEventJpaRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Scheduled(fixedDelayString = "PT5S")
    public void processPendingOutbox() {
        List<OutboxEventEntity> events = outboxEventRepository
                .findTop100ByProcessedFalseOrderByCreatedAtAsc();
        for (OutboxEventEntity event : events) {
            kafkaTemplate.send(event.getAggregateType() + "-events", event.getPayload())
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Error sending outbox event {} to Kafka: {}", event.getId(), ex.getMessage());
                    } else {
                        event.markAsProcessed();
                        outboxEventRepository.save(event);
                    }
                });
        }
    }
}
