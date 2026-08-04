package com.repertorio.common.outbox;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.UUID;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Outbox poller configuration. {@code instanceId} is unique per JVM (hostname + random
 * suffix, or a plain random UUID when the hostname cannot be resolved) and identifies the
 * claimer of each row, which is what makes multi-replica polling safe.
 */
@ConfigurationProperties(prefix = "repertorio.outbox")
public record OutboxProperties(
        String instanceId,
        Duration claimTimeout,
        int maxRetries,
        Duration backoffInitial,
        int batchSize) {

    public OutboxProperties {
        if (instanceId == null || instanceId.isBlank()) {
            instanceId = defaultInstanceId();
        }
        if (claimTimeout == null) {
            claimTimeout = Duration.ofSeconds(30);
        }
        if (maxRetries == 0) {
            maxRetries = 5;
        }
        if (backoffInitial == null) {
            backoffInitial = Duration.ofSeconds(1);
        }
        if (batchSize == 0) {
            batchSize = 100;
        }
    }

    private static String defaultInstanceId() {
        try {
            return InetAddress.getLocalHost().getHostName() + "-" + UUID.randomUUID();
        } catch (UnknownHostException e) {
            return UUID.randomUUID().toString();
        }
    }
}
