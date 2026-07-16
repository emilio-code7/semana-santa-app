package com.repertorio.marcha.adapter.outbound.messaging;

import com.repertorio.common.messaging.MessageSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import java.util.concurrent.CompletableFuture;

@Component
@Profile("!aws")
@RequiredArgsConstructor
@Slf4j
public class KafkaMessageSender implements MessageSender {

    private final KafkaTemplate<String, String> kafkaTemplate;

    @Override
    public CompletableFuture<Void> send(String topic, String payload) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        kafkaTemplate.send(topic, payload)
            .whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Failed to send Kafka message to {}: {}", topic, ex.getMessage());
                    future.completeExceptionally(ex);
                } else {
                    future.complete(null);
                }
            });
        return future;
    }
}
