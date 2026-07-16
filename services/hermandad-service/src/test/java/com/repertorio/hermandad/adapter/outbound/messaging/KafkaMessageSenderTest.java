package com.repertorio.hermandad.adapter.outbound.messaging;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KafkaMessageSenderTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    private KafkaMessageSender sender;

    @BeforeEach
    void setUp() {
        sender = new KafkaMessageSender(kafkaTemplate);
    }

    @Test
    void sendDelegatesToKafkaTemplate() {
        var future = new CompletableFuture<SendResult<String, String>>();
        when(kafkaTemplate.send(anyString(), anyString())).thenReturn(future);

        var result = sender.send("test-topic", "test-payload");

        verify(kafkaTemplate).send("test-topic", "test-payload");
        assertFalse(result.isDone());
    }

    @SuppressWarnings("unchecked")
    @Test
    void sendReturnsCompletableFuture() {
        var future = new CompletableFuture<SendResult<String, String>>();
        future.complete(mock(SendResult.class));
        when(kafkaTemplate.send(anyString(), anyString())).thenReturn(future);

        var result = sender.send("test-topic", "test-payload");

        assertTrue(result.isDone());
    }
}
