package com.repertorio.hermandad.adapter.outbound.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxPoller {

    private final OutboxEventJpaRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;


    @Scheduled(fixedDelayString = "PT5S")
    public void processPendingOutbox() {
        List<OutboxEventEntity> outboxEventList = outboxEventRepository.findTop100ByProcessedFalseOrderByCreatedAtAsc();
        log.info("Outbox events found after PT5S: {}", outboxEventList);
        for (OutboxEventEntity outboxEvent : outboxEventList) {
            kafkaTemplate.send(outboxEvent.getAggregateType() + "-events", outboxEvent.getPayload())
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Error while sending outbox event to Kafka", ex);
                    } else {
                        outboxEvent.markAsProcessed();
                        outboxEventRepository.save(outboxEvent);
                    }
                });
        }

    }
}
