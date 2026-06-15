package com.repertorio.hermandad.application;

import com.repertorio.hermandad.domain.model.OutboxEvent;
import com.repertorio.hermandad.domain.repository.OutboxEventRepository;
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

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;


    @Scheduled(fixedDelayString = "PT5S")
    public void processPendingOutbox() {
        List<OutboxEvent> outboxEventList = outboxEventRepository.findByProcessedFalse();
        log.info("Outbox events found after PT5S: {}", outboxEventList);
        for (OutboxEvent outboxEvent : outboxEventList) {
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
