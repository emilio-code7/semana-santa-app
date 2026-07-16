package com.repertorio.hermandad.adapter.outbound.messaging;

import io.awspring.cloud.sqs.operations.SendResult;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SqsMessageSenderTest {

    @Mock
    private SqsTemplate sqsTemplate;

    private SqsMessageSender sender;

    @BeforeEach
    void setUp() {
        sender = new SqsMessageSender(sqsTemplate);
    }

    @Test
    void sendDelegatesToSqsTemplateSendAsync() {
        @SuppressWarnings("unchecked")
        var future = CompletableFuture.completedFuture(mock(SendResult.class));
        when(sqsTemplate.sendAsync(any(Consumer.class))).thenReturn(future);

        var result = sender.send("test-queue", "test-payload");

        verify(sqsTemplate).sendAsync(any(Consumer.class));
        assertTrue(result.isDone());
    }

    @Test
    void sendPropagatesFailure() {
        @SuppressWarnings("unchecked")
        var future = new CompletableFuture<SendResult<String>>();
        future.completeExceptionally(new RuntimeException("SQS error"));
        when(sqsTemplate.sendAsync(any(Consumer.class))).thenReturn(future);

        var result = sender.send("test-queue", "test-payload");

        assertTrue(result.isCompletedExceptionally());
    }
}
