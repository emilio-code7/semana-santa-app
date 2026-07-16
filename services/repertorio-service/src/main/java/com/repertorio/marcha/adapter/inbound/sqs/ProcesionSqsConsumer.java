package com.repertorio.marcha.adapter.inbound.sqs;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.repertorio.marcha.adapter.outbound.events.ProcessedEventEntity;
import com.repertorio.marcha.adapter.outbound.events.ProcessedEventJpaRepository;
import com.repertorio.marcha.domain.model.KnownProcesion;
import com.repertorio.marcha.domain.port.KnownProcesionRepository;
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
public class ProcesionSqsConsumer {

    private final KnownProcesionRepository knownProcesionRepository;
    private final ProcessedEventJpaRepository processedEventRepository;
    private final ObjectMapper objectMapper;

    @SqsListener(value = "${spring.cloud.aws.sqs.queue.procesion-events}", acknowledgementMode = "MANUAL")
    public void consume(String payload, Acknowledgement ack) {
        var eventId = UUID.nameUUIDFromBytes(payload.getBytes());

        if (processedEventRepository.findById(eventId).isPresent()) {
            log.debug("Duplicate event skipped: {}", eventId);
            ack.acknowledge();
            return;
        }

        try {
            JsonNode root = objectMapper.readTree(payload);
            String eventType = root.has("eventType") ? root.get("eventType").asText() : "";
            UUID procesionId = root.has("aggregateId") ? UUID.fromString(root.get("aggregateId").asText()) : null;
            UUID hermandadId = root.has("hermandadId") ? UUID.fromString(root.get("hermandadId").asText()) : null;

            if ("PROCESION_CREATED".equals(eventType) && procesionId != null && hermandadId != null) {
                knownProcesionRepository.save(new KnownProcesion(procesionId, hermandadId, "PLANNED"));
                log.info("Registered known procesion {} for hermandad {}", procesionId, hermandadId);
            }

            processedEventRepository.save(new ProcessedEventEntity(eventId, "sqs-consumer"));
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Failed to process SQS event {}: {}", eventId, e.getMessage());
            // Do not ack — message returns to queue for retry
        }
    }
}
