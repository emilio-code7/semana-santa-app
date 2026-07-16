package com.repertorio.marcha.adapter.outbound.outbox;

import com.repertorio.common.messaging.MessageSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxPoller {

    private final OutboxEventJpaRepository outboxEventRepository;
    private final MessageSender messageSender;

    @Scheduled(fixedDelayString = "PT5S")
    public void processPendingOutbox() {
        List<OutboxEventEntity> list = outboxEventRepository.findTop100ByProcessedFalseOrderByCreatedAtAsc();
        for (OutboxEventEntity evt : list) {
            String topic = evt.getAggregateType() + "-events";
            messageSender.send(topic, evt.getPayload())
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to send outbox event {} to {}: {}", evt.getId(), topic, ex.getMessage());
                    } else {
                        evt.markAsProcessed();
                        outboxEventRepository.save(evt);
                    }
                });
        }
    }
}
