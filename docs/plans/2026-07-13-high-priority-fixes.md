# High Priority Fixes Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Fix the 3 high-priority issues found in project analysis: procesion outbox (events never reach Kafka), missing domain unit tests, and missing integration tests.

**Architecture:** Procesion service follows hexagonal architecture matching hermandad's outbox pattern. The outbox table + poller enables reliable event publishing to Kafka without 2PC. Domain unit tests validate the Procesion state machine independently. Integration tests validate DB + controller wiring.

**Tech Stack:** Java 21, Spring Boot 4.1, JPA/Hibernate, Flyway, Kafka, Testcontainers

---

### Task 1: Procesion Outbox — Events Now Reach Kafka

**Goal:** Procesion domain events (ProcesionCreatedEvent, ProcesionStatusChangedEvent) currently only fire Spring in-process ApplicationEvents. They must also persist to an outbox table and be polled + sent to Kafka.

**Pattern:** Mirror hermandad-service's outbox exactly:
- `OutboxPublisher` port interface
- `OutboxEventEntity` JPA entity with `@Table(name = "outbox_event")`
- `OutboxEventJpaRepository` — `findTop100ByProcessedFalseOrderByCreatedAtAsc()`
- `OutboxEventPublisher` — serializes event to JSON via ObjectMapper, saves entity
- `OutboxPoller` — `@Scheduled(fixedDelay = 5s)`, polls unprocessed, sends to `{aggregateType}-events` topic via `KafkaTemplate<String, String>`
- `V3__create_outbox_table.sql` — Flyway migration
- Update `DomainEventPublisherAdapter` — inject and call `OutboxPublisher` alongside `ApplicationEventPublisher`

**Files:**
- Create: `services/procesion-service/src/main/java/com/repertorio/procesion/application/port/OutboxPublisher.java`
- Create: `services/procesion-service/src/main/java/com/repertorio/procesion/adapter/outbound/outbox/OutboxEventEntity.java`
- Create: `services/procesion-service/src/main/java/com/repertorio/procesion/adapter/outbound/outbox/OutboxEventJpaRepository.java`
- Create: `services/procesion-service/src/main/java/com/repertorio/procesion/adapter/outbound/outbox/OutboxEventPublisher.java`
- Create: `services/procesion-service/src/main/java/com/repertorio/procesion/adapter/outbound/outbox/OutboxPoller.java`
- Create: `services/procesion-service/src/main/resources/db/migration/V3__create_outbox_table.sql`
- Modify: `services/procesion-service/src/main/java/com/repertorio/procesion/adapter/outbound/events/DomainEventPublisherAdapter.java`

**Step 1: Create OutboxPublisher port interface**

File: `application/port/OutboxPublisher.java`
```java
package com.repertorio.procesion.application.port;

public interface OutboxPublisher {
    void publish(DomainEvent domainEvent);
}
```

**Step 2: Create OutboxEventEntity**

File: `adapter/outbound/outbox/OutboxEventEntity.java`
- Package: `com.repertorio.procesion.adapter.outbound.outbox`
- `@Entity @Table(name = "outbox_event")` with fields:
  - `UUID id` — `@Id @UuidGenerator`
  - `String aggregateType` — not null
  - `UUID aggregateId` — not null
  - `String eventType` — not null
  - `String payload` — not null
  - `Instant createdAt` — `@PrePersist`, not null, updatable=false
  - `Instant processedAt` — nullable
  - `Boolean processed` — default false
- `markAsProcessed()` sets processed=true + processedAt=Instant.now()
- `@PrePersist` sets createdAt

**Step 3: Create OutboxEventJpaRepository**

File: `adapter/outbound/outbox/OutboxEventJpaRepository.java`
- Extends `JpaRepository<OutboxEventEntity, UUID>`
- Method: `List<OutboxEventEntity> findTop100ByProcessedFalseOrderByCreatedAtAsc()`

**Step 4: Create OutboxEventPublisher**

File: `adapter/outbound/outbox/OutboxEventPublisher.java`
- `@Component @RequiredArgsConstructor`
- Implements `OutboxPublisher`
- Dependencies: `OutboxEventJpaRepository`, `ObjectMapper`
- `publish()`: serializes event with ObjectMapper → saves `new OutboxEventEntity(...)`
- Catch `JsonProcessingException` → throw RuntimeException

**Step 5: Create OutboxPoller**

File: `adapter/outbound/outbox/OutboxPoller.java`
- `@Component @RequiredArgsConstructor @Slf4j`
- Dependencies: `OutboxEventJpaRepository`, `KafkaTemplate<String, String>`
- `@Scheduled(fixedDelayString = "PT5S")` on `processPendingOutbox()`
- Fetches unprocessed events → sends each via `kafkaTemplate.send(aggregateType + "-events", payload)` → on success: `markAsProcessed() + save()`

**Step 6: Create Flyway migration V3**

File: `db/migration/V3__create_outbox_table.sql`
```sql
CREATE TABLE outbox_event
(
    id            UUID                     NOT NULL PRIMARY KEY,
    aggregate_type VARCHAR(50)              NOT NULL,
    aggregate_id   UUID                     NOT NULL,
    event_type     VARCHAR(50)              NOT NULL,
    payload        TEXT                     NOT NULL,
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    processed_at   TIMESTAMP WITH TIME ZONE,
    processed     BOOLEAN                  NOT NULL DEFAULT FALSE
);
```

**Step 7: Update DomainEventPublisherAdapter**

Change from only publishing Spring `ApplicationEvent` to also publishing via `OutboxPublisher`:
```java
@Component
@RequiredArgsConstructor
public class DomainEventPublisherAdapter implements DomainEventPublisher {
    private final OutboxPublisher outboxPublisher;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    public void publish(DomainEvent domainEvent) {
        applicationEventPublisher.publishEvent(domainEvent);
        outboxPublisher.publish(domainEvent);
    }
}
```

**Verification:**
- All 5 new files and 1 modified file compile correctly
- Kafka auto-config creates `KafkaTemplate<String, String>` (producer config already in application.yml)
- `@EnableScheduling` already on ProcesionServiceApplication
- `spring-boot-starter-kafka` already in build.gradle.kts

---

### Task 2: Procesion Domain Unit Tests

**Goal:** Add standalone unit tests for the `Procesion` entity state machine (Procesion.changeStatus() transitions). These should test all valid and invalid state transitions without Spring context.

**Files:**
- Create: `services/procesion-service/src/test/java/com/repertorio/procesion/domain/model/ProcesionTest.java`

**Test cases:**
```
Given a new Procesion (status = PLANNED):
→ create() returns entity with PLANNED

Status transitions verified:
PLANNED → IN_PROGRESS ✅ allowed
PLANNED → CANCELLED ✅ allowed
PLANNED → COMPLETED ❌ throws
PLANNED → PLANNED ✅ no-op (no exception, status unchanged)
IN_PROGRESS → COMPLETED ✅ allowed
IN_PROGRESS → CANCELLED ✅ allowed
IN_PROGRESS → PLANNED ❌ throws
COMPLETED → any ❌ throws
CANCELLED → any ❌ throws
```

---

### Task 3: Procesion Integration Tests

**Goal:** Add integration tests using Testcontainers (PostgreSQL) — following the pattern from hermandad-service's `HermandadRepositoryIntegrationTest` and `HermandadControllerIntegrationTest`.

**Files:**
- Create: `services/procesion-service/src/test/java/com/repertorio/procesion/adapter/outbound/persistence/ProcesionRepositoryIntegrationTest.java`
- Create: `services/procesion-service/src/test/java/com/repertorio/procesion/adapter/inbound/rest/controller/ProcesionControllerIntegrationTest.java`

**Integration test strategy:**
- Use shared `IntegrationTestBase` from `:shared:common` (starts Postgres, Kafka, Redis via Testcontainers)
- Repository test: test save, findById, findByHermandadId pagination
- Controller test: test full HTTP cycle with authenticated requests (use `JwtTestFactory` from shared)
