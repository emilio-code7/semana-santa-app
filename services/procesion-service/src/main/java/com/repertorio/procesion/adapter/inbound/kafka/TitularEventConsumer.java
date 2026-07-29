package com.repertorio.procesion.adapter.inbound.kafka;

import com.repertorio.procesion.application.event.TitularEventProcessor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Profile("!aws")
@RequiredArgsConstructor
public class TitularEventConsumer {

    static final String GROUP_ID = "procesion-service-group";

    private final TitularEventProcessor processor;

    @KafkaListener(topics = "titular-events", groupId = GROUP_ID)
    public void consume(String payload) {
        processor.process(payload);
    }
}
