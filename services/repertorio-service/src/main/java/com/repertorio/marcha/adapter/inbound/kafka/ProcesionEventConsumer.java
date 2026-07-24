package com.repertorio.marcha.adapter.inbound.kafka;

import com.repertorio.marcha.application.event.ProcesionEventProcessor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Profile("!aws")
@RequiredArgsConstructor
public class ProcesionEventConsumer {

    static final String GROUP_ID = "repertorio-service-group";

    private final ProcesionEventProcessor processor;

    @KafkaListener(topics = "procesion-events", groupId = GROUP_ID)
    public void consume(String payload) {
        processor.process(payload);
    }
}
