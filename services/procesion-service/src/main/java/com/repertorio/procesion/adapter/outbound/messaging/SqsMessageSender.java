package com.repertorio.procesion.adapter.outbound.messaging;

import com.repertorio.common.messaging.MessageSender;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("aws")
@RequiredArgsConstructor
public class SqsMessageSender implements MessageSender {
    private final SqsTemplate sqsTemplate;

    @Override
    public CompletableFuture<Void> send(String destination, UUID aggregateId, UUID eventId, String payload) {
        return sqsTemplate.sendAsync(options -> options
                        .queue(destination)
                        .payload(payload)
                        .messageGroupId(aggregateId.toString())
                        .messageDeduplicationId(eventId.toString()))
                .thenApply(ignored -> null);
    }
}
