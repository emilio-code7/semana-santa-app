package com.repertorio.procesion.adapter.outbound.messaging;

import com.repertorio.common.messaging.MessageSender;
import io.awspring.cloud.sqs.operations.SqsSendOptions;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

@Component
@Profile("aws")
@RequiredArgsConstructor
@Slf4j
public class SqsMessageSender implements MessageSender {

    private final SqsTemplate sqsTemplate;

    @Override
    public CompletableFuture<Void> send(String queueName, String payload) {
        return sqsTemplate.sendAsync((Consumer<SqsSendOptions<String>>)
                opts -> opts.queue(queueName).payload(payload))
            .thenApply(result -> null);
    }
}
