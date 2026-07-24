package com.repertorio.marcha.adapter.inbound.sqs;

import com.repertorio.marcha.application.event.ProcesionEventProcessor;
import io.awspring.cloud.sqs.annotation.SqsListener;
import io.awspring.cloud.sqs.listener.acknowledgement.Acknowledgement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("aws")
@RequiredArgsConstructor
@Slf4j
public class ProcesionSqsConsumer {

    private final ProcesionEventProcessor processor;

    @SqsListener(value = "${spring.cloud.aws.sqs.queue.procesion-events:procesion-events}", acknowledgementMode = "MANUAL")
    public void consume(String payload, Acknowledgement ack) {
        try {
            processor.process(payload);
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Failed to process SQS message, not acknowledging: {}", e.getMessage());
            throw e;
        }
    }
}
