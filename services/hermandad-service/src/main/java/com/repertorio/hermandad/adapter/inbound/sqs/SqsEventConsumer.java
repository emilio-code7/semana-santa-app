package com.repertorio.hermandad.adapter.inbound.sqs;

import com.repertorio.hermandad.adapter.outbound.events.ProcessedEventEntity;
import com.repertorio.hermandad.adapter.outbound.events.ProcessedEventJpaRepository;
import io.awspring.cloud.sqs.annotation.SqsListener;
import io.awspring.cloud.sqs.listener.acknowledgement.Acknowledgement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Profile("aws")
@RequiredArgsConstructor
@Slf4j
public class SqsEventConsumer {

    private final ProcessedEventJpaRepository processedEventRepository;

    @SqsListener(value = "${spring.cloud.aws.sqs.queue.hermandad-events}", acknowledgementMode = "MANUAL")
    public void consumeHermandadEvent(String payload, Acknowledgement ack) {
        processEvent(payload, ack);
    }

    @SqsListener(value = "${spring.cloud.aws.sqs.queue.hermandad-member-events}", acknowledgementMode = "MANUAL")
    public void consumeHermandadMemberEvent(String payload, Acknowledgement ack) {
        processEvent(payload, ack);
    }

    private void processEvent(String payload, Acknowledgement ack) {
        var eventId = UUID.nameUUIDFromBytes(payload.getBytes());
        if (processedEventRepository.findById(eventId).isPresent()) {
            log.debug("Duplicate event skipped: {}", eventId);
            ack.acknowledge();
            return;
        }
        processedEventRepository.save(new ProcessedEventEntity(eventId, "sqs-consumer"));
        ack.acknowledge();
        log.info("Processed event {} from SQS", eventId);
    }
}
