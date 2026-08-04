package com.repertorio.common.messaging;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface MessageSender {
    CompletableFuture<Void> send(String destination, UUID aggregateId, UUID eventId, String payload);
}
