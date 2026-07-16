package com.repertorio.procesion.adapter.outbound.messaging;

import com.repertorio.common.messaging.MessageSender;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import java.util.concurrent.CompletableFuture;

@Component
@Profile("aws")
@RequiredArgsConstructor
@Slf4j
public class SqsMessageSender implements MessageSender {

    private final SqsTemplate sqsTemplate;

    @Override
    public CompletableFuture<Void> send(String queueName, String payload) {
        try {
            sqsTemplate.send(queueName, payload);
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            log.error("Failed to send SQS message to {}: {}", queueName, e.getMessage());
            return CompletableFuture.failedFuture(e);
        }
    }
}
