# AWS SQS Migration — Hexagonal Adapter Addition

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add SQS as a second messaging transport alongside Kafka, selectable via Spring profile. Default profile stays Kafka (local dev), `aws` profile uses SQS (deployed).

**Architecture:** Extract a `MessageSender` port from the existing `OutboxPoller` so it depends on an interface, not on `KafkaTemplate` directly. Add `@Profile("aws")` SQS implementations for both the outbox sender and the event consumers. Keep all existing Kafka code unchanged.

**Design decision — dual-transport over replacement:** Kafka stays as default profile for local development. SQS is additive behind `@Profile("aws")`. After SQS is validated in production, a follow-up task can remove Kafka. This avoids a cut-over risk and allows easy rollback.

**Tech Stack:** Spring Cloud AWS 4.0.2 (SQS), AWS SDK v2, Spring profiles

---

### Pre-Task: Update backlog to reflect dual-transport decision

**Files:**
- Modify: `docs/backlog.md` lines 290-318

Change "replaces" language to "adds alongside", add deferred Kafka removal note.

---

### Task 1: Add `MessageSender` port + per-service adapters

**Files:**
- Create: `shared/common/src/main/java/com/repertorio/common/messaging/MessageSender.java`
- Create: `services/hermandad-service/.../adapter/outbound/messaging/KafkaMessageSender.java`
- Create: `services/procesion-service/.../adapter/outbound/messaging/KafkaMessageSender.java`
- Create: `services/repertorio-service/.../adapter/outbound/messaging/KafkaMessageSender.java`
- Create: `services/hermandad-service/.../adapter/outbound/messaging/SqsMessageSender.java`
- Create: `services/procesion-service/.../adapter/outbound/messaging/SqsMessageSender.java`
- Create: `services/repertorio-service/.../adapter/outbound/messaging/SqsMessageSender.java`
- Modify: All 3 `OutboxPoller.java` files

**Design:** `MessageSender.send()` returns `CompletableFuture<Void>` so the OutboxPoller can keep its callback-based `markAsProcessed` pattern, preserving at-least-once delivery.

**Step 1: Create the MessageSender port**

```java
package com.repertorio.common.messaging;

import java.util.concurrent.CompletableFuture;

public interface MessageSender {
    CompletableFuture<Void> send(String topic, String payload);
}
```

**Step 2: Create KafkaMessageSender in each service**

One per service (identical, ~15 lines each), in their own `adapter/outbound/messaging/` package:

```java
package com.repertorio.hermandad.adapter.outbound.messaging;

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
```

Copy to all 3 services with package adjusted. `@Profile("!aws")` ensures this bean is not created when `aws` profile is active, preventing Kafka bootstrap failures.

**Step 3: Create SqsMessageSender in each service**

```java
package com.repertorio.hermandad.adapter.outbound.messaging;

import com.repertorio.common.messaging.MessageSender;
import io.awspring.cloud.sqs.core.SqsTemplate;
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
        CompletableFuture<Void> future = new CompletableFuture<>();
        sqsTemplate.send(queueName, payload)
            .whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Failed to send SQS message to {}: {}", queueName, ex.getMessage());
                    future.completeExceptionally(ex);
                } else {
                    future.complete(null);
                }
            });
        return future;
    }
}
```

Copy to all 3 services. `@Profile("aws")` ensures this bean is only created on deployed EC2.

**Step 4: Update OutboxPoller (all 3 services)**

Change constructor to accept `MessageSender` instead of `KafkaTemplate`. Keep the callback pattern for `markAsProcessed`:

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxPoller {

    private final OutboxEventJpaRepository outboxEventRepository;
    private final MessageSender messageSender;

    @Scheduled(fixedDelayString = "PT5S")
    public void processPendingOutbox() {
        List<OutboxEventEntity> list = outboxEventRepository
            .findTop100ByProcessedFalseOrderByCreatedAtAsc();
        for (OutboxEventEntity evt : list) {
            String topic = evt.getAggregateType() + "-events";
            messageSender.send(topic, evt.getPayload())
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to send outbox event {} to {}: {}", evt.getId(), topic, ex.getMessage());
                    } else {
                        evt.markAsProcessed();
                        outboxEventRepository.save(evt);
                    }
                });
        }
    }
}
```

This preserves **at-least-once delivery**: `markAsProcessed` only happens after the broker confirms receipt. If the app crashes between `send()` and confirmation, the event stays unprocessed and will be retried on next poll.

The `KafkaMessageSender` wraps `KafkaTemplate.send()`'s existing `CompletableFuture`. The `SqsMessageSender` wraps `SqsTemplate.send()`'s `CompletableFuture`. Both return `CompletableFuture<Void>`.

**Step 5: Compile and test**

```bash
./gradlew :services:hermandad-service:compileJava
./gradlew :services:procesion-service:compileJava
./gradlew :services:repertorio-service:compileJava
./gradlew :services:hermandad-service:test
```

**Step 6: Commit**

```bash
git add shared/common/src/main/java/com/repertorio/common/messaging/
git add services/*/src/main/java/**/messaging/
git add services/*/src/main/java/**/OutboxPoller.java
git commit -m "feat: extract MessageSender port from OutboxPoller with Kafka+SQS adapters"
```

---

### Task 2: Add SQS dependency + profile configs

**Files:**
- Modify: `gradle/libs.versions.toml` — add Spring Cloud AWS BOM version and SQS starter
- Modify: All 3 service `build.gradle.kts` — add SQS dependency
- Create: `services/hermandad-service/src/main/resources/application-aws.yml`
- Create: `services/procesion-service/src/main/resources/application-aws.yml`
- Create: `services/repertorio-service/src/main/resources/application-aws.yml`

**Step 1: Update version catalog**

In `gradle/libs.versions.toml`:
```toml
[versions]
spring-cloud-aws = "4.0.2"

[libraries]
spring-cloud-aws-starter-sqs = { group = "io.awspring.cloud", name = "spring-cloud-aws-starter-sqs", version.ref = "spring-cloud-aws" }
```

**Step 2: Add SQS deps to all 3 service build.gradle.kts**

```kotlin
implementation(libs.spring.cloud.aws.starter.sqs)
```

**Step 3: Create AWS profile configs**

`application-aws.yml` (same for all 3 services):
```yaml
spring:
  autoconfigure:
    exclude: org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration
  cloud:
    aws:
      region:
        static: us-east-1
```

Note: `spring.autoconfigure.exclude` prevents Kafka auto-configuration failures when `aws` profile is active and no Kafka broker is available. No `credentials.type` is set — the SDK uses the default credential chain (env vars → `~/.aws/credentials` → instance profile), which works both on EC2 and for local testing.

**Step 4: Commit**

```bash
git add gradle/libs.versions.toml
git add services/*/build.gradle.kts
git add services/*/src/main/resources/application-aws.yml
git commit -m "feat: add SQS starter dependency and aws profile configs"
```

---

### Task 3: Add SQS consumer adapters (@Profile("aws"))

**Files:**
- Create: `services/hermandad-service/.../adapter/inbound/sqs/SqsEventConsumer.java`
- Create: `services/repertorio-service/.../adapter/inbound/sqs/ProcesionSqsConsumer.java`
- Modify: `services/hermandad-service/.../adapter/inbound/kafka/IdempotentEventConsumer.java` — add `@Profile("!aws")`
- Modify: `services/repertorio-service/.../adapter/inbound/kafka/ProcesionEventConsumer.java` — add `@Profile("!aws")`
- Modify: `services/repertorio-service/src/main/java/.../RepertorioServiceApplication.java` — guard `@EnableKafka`

**Step 1: Add @Profile("!aws") to existing Kafka consumers**

On `IdempotentEventConsumer`:
```java
@Component
@Profile("!aws")
```

On `ProcesionEventConsumer`:
```java
@Component
@Profile("!aws")
```

Without this, when `aws` profile is active and Kafka auto-config is excluded, the `@KafkaListener` container factory won't be available and the app will fail to start.

**Step 2: Guard @EnableKafka on repertorio-service**

```java
@SpringBootApplication
@EnableDiscoveryClient
@EnableScheduling
@ConditionalOnProperty(name = "spring.profiles.active", havingValue = "aws", matchIfMissing = true)
@EnableKafka
```

Actually, simpler: move `@EnableKafka` to a separate `@Configuration` class that imports it conditionally, or just don't set it since the Kafka listener containers will be created lazily if the KafkaTemplate bean exists.

Simplest fix: leave `@EnableKafka` as-is. With `spring.autoconfigure.exclude` of `KafkaAutoConfiguration`, the Kafka listener containers won't have a `ConsumerFactory` and `@KafkaListener` beans won't be processable. But since the `@KafkaListener` beans are `@Profile("!aws")`, they won't be registered. Spring's `KafkaListenerAnnotationBeanPostProcessor` will be registered but won't find any `@KafkaListener` beans to process. This should be safe.

**Step 3: Create Hermandad SQS consumer**

```java
package com.repertorio.hermandad.adapter.inbound.sqs;

import com.repertorio.hermandad.adapter.outbound.events.ProcessedEventEntity;
import com.repertorio.hermandad.adapter.outbound.events.ProcessedEventJpaRepository;
import io.awspring.cloud.sqs.annotation.SqsListener;
import io.awspring.cloud.sqs.listener.acknowledgement.Acknowledgement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import java.util.UUID;

@Component
@Profile("aws")
@RequiredArgsConstructor
@Slf4j
public class SqsEventConsumer {

    private final ProcessedEventJpaRepository processedEventRepository;

    @SqsListener(value = "${spring.cloud.aws.sqs.queue.hermandad-events}", acknowledgementMode = AcknowledgementMode.MANUAL)
    public void consumeHermandadEvent(String payload, Acknowledgement ack) {
        processEvent(payload, ack);
    }

    @SqsListener(value = "${spring.cloud.aws.sqs.queue.hermandad-member-events}", acknowledgementMode = AcknowledgementMode.MANUAL)
    public void consumeHermandadMemberEvent(String payload, Acknowledgement ack) {
        processEvent(payload, ack);
    }

    private void processEvent(String payload, Acknowledgement ack) {
        var eventId = UUID.nameUUIDFromBytes(payload.getBytes());
        if (processedEventRepository.findById(eventId).isPresent()) {
            log.debug("Duplicate event skipped: {}", eventId);
            ack.acknowledge();
            return;
        }
        processedEventRepository.save(new ProcessedEventEntity(eventId, "sqs-consumer"));
        ack.acknowledge();
        log.info("Processed event {} from SQS", eventId);
    }
}
```

Key design: `AcknowledgementMode.MANUAL` prevents the TOCTOU race that can cause infinite retry loops. The message is only deleted from the queue after the idempotency record is persisted. If the save fails, the ack is not called, and the message becomes visible for retry — at which point the idempotency check catches it.

**Step 4: Create Repertorio SQS consumer**

```java
package com.repertorio.marcha.adapter.inbound.sqs;

import com.repertorio.marcha.adapter.outbound.events.ProcessedEventEntity;
import com.repertorio.marcha.adapter.outbound.events.ProcessedEventJpaRepository;
import com.repertorio.marcha.domain.model.KnownProcesion;
import com.repertorio.marcha.domain.port.KnownProcesionRepository;
import io.awspring.cloud.sqs.annotation.SqsListener;
import io.awspring.cloud.sqs.listener.acknowledgement.Acknowledgement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import java.util.UUID;

@Component
@Profile("aws")
@RequiredArgsConstructor
@Slf4j
public class ProcesionSqsConsumer {

    private final KnownProcesionRepository knownProcesionRepository;
    private final ProcessedEventJpaRepository processedEventRepository;
    private final ObjectMapper objectMapper;

    @SqsListener(value = "${spring.cloud.aws.sqs.queue.procesion-events}", acknowledgementMode = AcknowledgementMode.MANUAL)
    public void consume(String payload, Acknowledgement ack) {
        var eventId = UUID.nameUUIDFromBytes(payload.getBytes());

        if (processedEventRepository.findById(eventId).isPresent()) {
            log.debug("Duplicate event skipped: {}", eventId);
            ack.acknowledge();
            return;
        }

        try {
            JsonNode root = objectMapper.readTree(payload);
            String eventType = root.has("eventType") ? root.get("eventType").asText() : "";
            UUID procesionId = root.has("aggregateId") ? UUID.fromString(root.get("aggregateId").asText()) : null;
            UUID hermandadId = root.has("hermandadId") ? UUID.fromString(root.get("hermandadId").asText()) : null;

            if ("PROCESION_CREATED".equals(eventType) && procesionId != null && hermandadId != null) {
                knownProcesionRepository.save(new KnownProcesion(procesionId, hermandadId));
                log.info("Registered known procesion {} for hermandad {}", procesionId, hermandadId);
            }

            processedEventRepository.save(new ProcessedEventEntity(eventId, "sqs-consumer"));
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Failed to process SQS event {}: {}", eventId, e.getMessage());
            // Do not ack — message returns to queue for retry
        }
    }
}
```

**Step 5: Commit**

```bash
git add services/hermandad-service/src/main/java/.../sqs/
git add services/repertorio-service/src/main/java/.../sqs/
git add services/hermandad-service/src/main/java/.../kafka/
git add services/repertorio-service/src/main/java/.../kafka/
git commit -m "feat: add @Profile('aws') SQS consumers with manual ack for idempotent processing"
```

---

### Task 4: Deploy config + tests

**Files:**
- Modify: `.worktrees/aws-infra/docker-compose.aws.yml` — add `SPRING_PROFILES_ACTIVE: aws`
- Modify: `.worktrees/aws-infra/scripts/deploy-aws.sh` — verify
- Create: `services/hermandad-service/src/test/java/.../adapter/outbound/messaging/KafkaMessageSenderTest.java`
- Create: `services/hermandad-service/src/test/java/.../adapter/outbound/messaging/SqsMessageSenderTest.java`
- Create: `services/hermandad-service/src/test/java/.../adapter/inbound/sqs/SqsEventConsumerTest.java`
- Modify: `docs/backlog.md`

**Step 1-3: Deploy config**

Add to all 3 services in docker-compose.aws.yml:
```yaml
SPRING_PROFILES_ACTIVE: aws
```

**Step 4: Update backlog**

Mark AWS-TASK-3 progress, add note about dual-transport decision, add deferred task for Kafka removal.

**Step 5: Commit**

```bash
git add .worktrees/aws-infra/docker-compose.aws.yml
git add services/*/src/test/java/**/messaging/
git add services/*/src/test/java/**/sqs/
git add docs/backlog.md
git commit -m "chore: add deploy config, SQS tests, update backlog"
```

---

## Verification

1. **Local compile**: All 3 services compile with new SQS dependency
2. **Existing tests**: All pass (Kafka profile is still default, no change)
3. **SQS sender test**: Verify `SqsMessageSender.send()` delegates to `SqsTemplate`
4. **SQS consumer test**: Verify idempotent processing with `AcknowledgementMode.MANUAL`
5. **Profile switch**: Starting with `SPRING_PROFILES_ACTIVE=aws` should not break locally (using default credential chain)
6. **Integration test**: Optional — LocalStack Testcontainers for end-to-end SQS outbox flow

## Rollout Order

1. Task 1 (MessageSender port) — safe refactor, no behavior change
2. Task 2 (SQS deps) — safe, adds dependency without enabling
3. Task 3 (SQS consumers) — safe behind @Profile("aws")
4. Task 4 (deploy) — activates aws profile on EC2

## Future: Kafka Removal

After SQS has been running in production for N days without issues:
- Remove `spring-boot-starter-kafka` dependency
- Delete `KafkaMessageSender` adapters
- Delete `@KafkaListener` consumer classes
- Remove `@EnableKafka` annotation
- Remove `spring.autoconfigure.exclude` for Kafka
- Remove Kafka from docker-compose.dev.yml
