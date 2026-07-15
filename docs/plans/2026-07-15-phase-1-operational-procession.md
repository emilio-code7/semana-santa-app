# Phase 1 — Operational Procession Plan

> BDD-style plan with Gherkin scenarios. Implementation details deferred.

**Goal:** Create one complete, observable workflow across Hermandad, Procesion, and Repertorio services.

**Architecture decision:** Cross-service reference validation is **Kafka-based eventual consistency**. Repertorio listens to `procesion-events`, maintains a local cache of known procesions, and validates cruceta creation against it. No synchronous REST calls between services.

---

## Feature: Cruceta References a Known Procesion

Repertorio needs to know which procesions exist before a cruceta can reference one. This knowledge comes asynchronously from the `procesion-events` Kafka topic.

### Scenario: Procesion is created and repertorio learns about it

```
Given the procesion-service publishes a ProcesionCreatedEvent to the
  "procesion-events" Kafka topic
When the repertorio-service consumer processes that event
Then a new KnownProcesion record is stored locally
  And it contains: procesionId, hermandadId, status=PLANNED, updatedAt
```

### Scenario: Cruceta can be defined referencing a known procesion

```
Given a KnownProcesion record exists for procesion X
When a user sends PUT /api/hermandades/{hid}/procesiones/{x}/cruceta
  with valid cruceta items
Then the response is 200 OK
  And the cruceta is persisted with the given marcha items
```

### Scenario: Cruceta cannot be defined for an unknown procesion

```
Given no KnownProcesion record exists for procesion X
When a user sends PUT /api/hermandades/{hid}/procesiones/{x}/cruceta
Then the response is 404 Not Found
  And the body explains the procesion is not known yet
```

### Scenario: Duplicate procesion events are safe

```
Given a ProcesionCreatedEvent has already been consumed
When the same event arrives again (duplicate Kafka delivery)
Then the consumer skips it silently
  And only one KnownProcesion record exists
```

### Scenario: Procesion status change is reflected locally

```
Given a KnownProcesion record exists for procesion X with status=PLANNED
When the procesion-service publishes a ProcesionStatusChangedEvent
  (status IN_PROGRESS) to the "procesion-events" topic
And the repertorio consumer processes it
Then the KnownProcesion status changes to IN_PROGRESS
  And updatedAt advances
```

---

## Feature: End-to-End Workflow

The main demo flow a reviewer would run.

### Scenario: Full process from hermandad to cruceta

```
Given the stack is running (docker-compose core profile)
  And a seeded Keycloak user exists with role on a hermandad
When the user creates a hermandad via POST /api/hermandades
  And creates a procesion via POST /api/procesiones
  And waits for outbox processing (~5s)
  And defines a cruceta via PUT /api/hermandades/{hid}/procesiones/{pid}/cruceta
  And changes the procesion status to IN_PROGRESS via PATCH /api/procesiones/{pid}/status
Then every step returns a successful HTTP status
  And the cruceta references existent seeded marchas
  And the status transition is valid per the procesion state machine
  And the outbox consumer events are observable in Kafka UI at port 9000
```

---

## Non-Goals (out of scope for Phase 1)

- Event envelope versioning, schema registry (Phase 2)
- Dead-letter queues and retry policies (Phase 2)
- OpenTelemetry tracing (Phase 3)
- Grafana dashboards (Phase 3)
- Cross-service REST contracts (deferred by architectural decision)

---

### Deferred Design Question: Demo Approach

Two alternatives for the demo:

**A) Documented API script (bash)** — curl-based, calls the gateway with a JWT token. Verifiable by running `./docs/demo/phase-1.sh`. Predictable, transparent, but requires stack up.

**B) Automated integration test** — Testcontainers with full stack (Postgres, Kafka, Gateway). Runs in CI. More robust but slower to write and maintain.

**Recommendation:** Start with (A) for a quick demoable artifact, add (B) as a supplement if the workflow is stable.

---

## Agent Preparation

**Before starting, an agent should read these functional-map sections:**
- §0 Operating Principles (hexagonal rules, layered deps, DDD, event reliability)
- §2.1 Repertorio profile (existing directory structure, 42 files pattern)
- §3.4 Kafka topology (topic names, partition counts)
- §4.3 Repertorio API endpoints (existing endpoint patterns)
- §5 DB schemas (column naming, TYPE syntax, index patterns)
- §8 Test inventory (existing test patterns, IntegrationTestBase)

**Reference files to use as patterns:**
- Domain entity: `services/hermandad-service/.../domain/model/Hermandad.java`
- Domain exceptions: `services/hermandad-service/.../domain/model/HermandadNotFoundException.java`
- Event consumer (idempotent): `services/hermandad-service/.../adapter/inbound/kafka/IdempotentEventConsumer.java`
- ProcessedEventEntity: `services/hermandad-service/.../adapter/outbound/events/ProcessedEventEntity.java`
- Integration test base: `shared/common/src/test/.../IntegrationTestBase.java`
- IT pattern: `services/hermandad-service/.../adapter/outbound/persistence/HermandadRepositoryIntegrationTest.java`
- Controller IT pattern: `services/hermandad-service/.../HermandadControllerIntegrationTest.java`

---

## Tasks

### Task 1: KnownProcesion Domain Model
**Depends on:** nothing

**What:** Create the domain entity and repository port. Pure Java — NO JPA annotations (@Entity, @Table, @Id) in domain layer. Fields: procesionId, hermandadId, status, updatedAt. Constructor validates non-null. updateStatus() ticks timestamp.

**Follow:** Hermandad.java domain pattern (functional-map §0.1 for layering rules)

**Acceptance:**
- Unit tests cover: creation, null rejection, status update, timestamp advancement
- Repository port interface defined in domain layer
- `./gradlew :services:repertorio-service:compileJava` passes

### Task 2: Flyway Tables + JPA Adapters
**Depends on:** Task 1 (KnownProcesion domain model)

**What:** Two new tables in repertorio_db. JPA entities live in adapter layer (NOT domain). Conversion methods `from(KnownProcesion)` → JPA entity and `toDomain()` on each JPA entity.

Tables:
- `known_procesion` (procesion_id UUID PK, hermandad_id UUID, status VARCHAR(20), updated_at TIMESTAMP WITH TIME ZONE) + index on hermandad_id
- `processed_event` (event_id UUID PK, consumer_name VARCHAR(100), processed_at TIMESTAMP WITH TIME ZONE DEFAULT NOW())

**Reference patterns:**
- JPA entity pattern: `procesion-service/.../adapter/outbound/persistence/OutboxEventEntity.java`
- Repository adapter pattern: `procesion-service/.../adapter/outbound/persistence/ProcesionRepositoryAdapter.java`
- ProcessedEventEntity: `hermandad-service/.../adapter/outbound/events/ProcessedEventEntity.java`
- ProcessedEventJpaRepository: `hermandad-service/.../adapter/outbound/events/ProcessedEventJpaRepository.java`

**Acceptance:**
- Flyway V5 (`known_procesion` table), V6 (`processed_event` table) created
- JPA entities + repositories + adapters for both tables
- `KnownProcesionRepositoryAdapter` implements `KnownProcesionRepository` from domain
- `ProcessedEventJpaRepository` extends `JpaRepository<ProcessedEventEntity, UUID>` (no adapter needed — consumed directly)
- Compilation passes

### Task 3: ProcesionEventConsumer
**Depends on:** Task 2 (processed_event table + JPA repository)

**What:** Kafka `@KafkaListener` on `procesion-events` topic. Detects event type by JSON field presence (Phase 2 adds proper envelope). Saves KnownProcesion on creation, updates status on change. Uses `processed_event` table for idempotency (deterministic UUID from payload bytes). Handles malformed payloads without crashing.

**Event field detection rules (important — the serialized JSON does NOT include `eventType`):**
- **ProcesionCreatedEvent** has fields: `id`, `hermandadId`, `date`, `time` → detected by presence of `date`
- **ProcesionStatusChangedEvent** has fields: `id`, `hermandadId`, `previousStatus`, `newStatus` → detected by presence of `newStatus`

**Reference pattern:** `hermandad-service/.../adapter/inbound/kafka/IdempotentEventConsumer.java`

**Acceptance:**
- Consumer registered with group `repertorio-service-group` on topic `procesion-events`
- Event with `date` field → `knownProcesionRepository.save(new KnownProcesion(procesionId, hermandadId, "PLANNED"))`
- Event with `newStatus` field → `knownProcesionRepository.findByProcesionId()` then `.updateStatus()` then save
- Duplicate payloads are silently skipped (checked via `processedEventRepository.existsById()`)
- Invalid JSON is logged but consumer continues (no crash)
- Unit tests cover: 2 event types, dedup, bad payload
- `@EnableKafka` added to `RepertorioServiceApplication.java` (currently missing — it has `@EnableDiscoveryClient`, `@EnableScheduling`, `@EnableMethodSecurity` but not `@EnableKafka`)

### Task 4: CrucetaService Validation
**Depends on:** Task 1 (KnownProcesionRepository port), Task 3 (procesions populated)

**What:** `CrucetaService.defineCruceta()` gets `KnownProcesionRepository` injected and calls `existsByProcesionId()` before creating cruceta. New domain exception `ProcesionNotFoundException` → caught by GlobalExceptionHandler → 404.

Existing CrucetaService already has `@PreAuthorize("@repertorioSecurity.isAdmin(#hermandadId)")` — the new validation is additive, not replacing the auth check.

**Acceptance:**
- Known procesion (exists in local table) → cruceta created successfully (200)
- Unknown procesion → 404 with message "Procesion not found: {id}"
- `ProcesionNotFoundException` added in domain layer
- GlobalExceptionHandler maps it to 404 `ApiError`
- Existing CrucetaControllerTest extended with 2 new tests (known + unknown procesion)

### Task 5: Integration Tests (repertorio)
**Depends on:** Tasks 1-4 (all implementation complete)

**What:** Testcontainers-based integration tests for repertorio-service, following the established pattern in hermandad-service.

**Key details for the agent:**
- DB port: 5433 (repertorio_db, same as docker-compose)
- Base class: `IntegrationTestBase` from `shared/common` — provides Postgres container + `@DynamicPropertySource`
- Annotation: `@SpringBootTest` + `@AutoConfigureMockMvc` for controller ITs
- Mock outbox + Kafka: `@MockitoBean` on `OutboxEventJpaRepository` and `KafkaTemplate`
- Skip silently: `Assumptions.assumeTrue(postgres.isRunning())` or use `@EnabledIf` on container condition

**Reference files:**
- Repository IT: `hermandad-service/.../adapter/outbound/persistence/HermandadRepositoryIntegrationTest.java`
- Controller IT: `hermandad-service/.../adapter/inbound/rest/controller/HermandadControllerIntegrationTest.java`

**Acceptance:**
- `KnownProcesionRepositoryIntegrationTest` (3 tests): save/find, exists(true), exists(false)
- `MarchaRepositoryIntegrationTest` (4 tests): save/find, findAll, delete, constraints
- `MarchaControllerIntegrationTest` (6 tests): POST 201, GET 200, GET all 200, DELETE 204/404, validation 400, unauth 401
- `CrucetaControllerIntegrationTest` (4 tests): define 200, get 200, not-found 404, unauth 401
- All skip gracefully if no PostgreSQL available

### Task 6: End-to-End Demo
**Depends on:** Tasks 1-4 (functional code), Task 5 (tests confirm correctness)

**What:** A runnable bash script that exercises the full workflow and confirms observable Kafka events.

**Acceptance:**
- Script at `docs/demo/phase-1.sh` that:
  - Gets a JWT from Keycloak (using seeded qa-admin-user)
  - Creates a hermandad via gateway
  - Creates a procesion via gateway
  - Sleeps 6s (outbox poller cycle + Kafka consumer)
  - Defines a cruceta referencing seeded marchas and the new procesion
  - Changes procesion status to IN_PROGRESS
  - Prints success at each step, exits non-zero on any failure
- Script uses `set -euo pipefail` and `jq` for JSON extraction
- Seeded marcha UUID: `a0000001-0000-0000-0000-000000000001` (from V3 migration)
- Keycloak URL: `http://localhost:8180`, Gateway URL: `http://localhost:8080`

### Task 7: Documentation Updates
**Depends on:** all prior tasks verified

**What:** Update 4 documents to reflect Phase 1 completion.

**Acceptance:**
- `docs/roadmap.md`: Phase 1 AC marked ✅, validation decision documented (Kafka-based, eventual consistency)
- `docs/functional-map.md`: KnownProcesion + ProcesionEventConsumer + processed_event table + consumer in Kafka topology + repertorio dependency link ← procesion
- `docs/service-reviews.md`: repertorio Issue #4 (no Kafka consumer) → ✅, add cross-service flow note, update test count
- `docs/backlog.md`: Phase 1 sprint section with all tasks

### Verification Gate (Final)

Run before considering Phase 1 complete:
1. `./gradlew :services:repertorio-service:test` — all 50+ existing + new tests pass
2. Integration tests run against Postgres (set `TEST_DB=postgres` or just verify they skip gracefully)
3. `grep -r "FIXME\|TODO\|HACK" services/repertorio-service/src/` — no unresolved markers
4. Demo script documented; optionally executed once against running stack

## Constraints

| Constraint | Reason |
|-----------|--------|
| No new services | Tracking and Notification deferred to Phase 3+ |
| No REST calls between services | Kafka-based validation chosen — eventual consistency |
| Consumer uses processed_event for dedup | Matches existing hermandad pattern |
| Event type detection by field presence | Phase 1 expediency — Phase 2 adds versioned envelope |
| Demo script in bash | Quickest path to a verifiable artifact |
