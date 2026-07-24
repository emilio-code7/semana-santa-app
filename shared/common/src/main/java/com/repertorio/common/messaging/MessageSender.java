package com.repertorio.common.messaging;

import java.util.concurrent.CompletableFuture;

public interface MessageSender {
    CompletableFuture<Void> send(String destination, String payload);
}
