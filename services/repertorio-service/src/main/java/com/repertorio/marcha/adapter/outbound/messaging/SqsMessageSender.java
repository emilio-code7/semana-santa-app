package com.repertorio.marcha.adapter.outbound.messaging;

import com.repertorio.common.messaging.MessageSender;
import io.awspring.cloud.sqs.operations.SqsTemplate;
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
    public CompletableFuture<Void> send(String destination, String payload) {
        return sqsTemplate.sendAsync(destination, payload).thenApply(ignored -> null);
    }
}
