package com.repertorio.procesion.adapter.outbound.messaging;

import com.repertorio.common.messaging.MessageSender;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@Profile("!aws")
@RequiredArgsConstructor
public class KafkaMessageSender implements MessageSender {
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Override
    public CompletableFuture<Void> send(String destination, UUID aggregateId, UUID eventId, String payload) {
        return kafkaTemplate.send(destination, aggregateId.toString(), payload).thenApply(ignored -> null);
    }
}
