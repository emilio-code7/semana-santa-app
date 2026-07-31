# Reliability-First High-Throughput Roadmap

> **SUPERSEDED** — the active roadmap is `2026-07-28-cruceta-first-high-throughput-roadmap.md`; all GitHub issues were filed against it. This document is retained for decision history only. Do not create issues, branches, or worktrees from this plan. (Marked 2026-07-31.)

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal**: Within four months, evolve the current single-replica, at-least-once implementation and prove that Repertorio can sustain 1,000 HTTP req/s and 500 events/s at p95 <300 ms with event freshness <5 s on disposable multi-node infrastructure.

**Architecture**: Three existing Spring Boot microservices (hermandad, procesion, repertorio), Kafka outbox eventing, Keycloak JWT auth, PostgreSQL per service, hexagonal + DDD. The first half builds correctness and reliability foundations in the existing Docker Compose environment. The second half measures throughput and failure recovery in a temporary multi-node environment, then adds route-aware Cruceta only after the throughput gate passes.

**Tech Stack**: Java 21, Spring Boot 4.1.0, Kafka, PostgreSQL 16, Flyway, Testcontainers, Docker Compose, JUnit 5, and Gradle. Load testing uses k6 against HTTP commands that create real outbox rows. Observability uses Micrometer/Prometheus endpoints, lightweight hosted visualization, external uptime checks, and structured JSON logs.

---

## Confirmed Decisions

### Reliability-First Ordering

Correctness and reliable event delivery precede all feature expansion. The route-aware Cruceta is gated behind the throughput proof (Gate 20). No GPS, maps, tracking, or notifications until the core event system is proven.

### Regional-Spike Target (Gate 20, End of Month 3)

| Metric | Target |
|--------|--------|
| HTTP requests/s | 1,000 sustained (aggregate across services) |
| Events/s through outbox → Kafka → consumer | 500 sustained |
| HTTP p95 latency | <300 ms (under load) |
| Event freshness (publish → consumer business commit) | <5 s p95 |

Evidence must come from a repeatable, disposable multi-node environment — not the always-on VPS.

### Per-Aggregate Ordering

- Kafka message key = `aggregateId` (UUID string) after Ticket 04 keys the `MessageSender`.
- All events for one aggregate land in the same partition, preserving broker order.
- Ticket 10 serializes outbox claims per aggregate so concurrent pollers cannot publish later aggregate events before earlier ones.
- Consumers process per-aggregate sequentially within a partition.

### SQS FIFO Compatibility Mapping

AWS adapters remain compile-tested but undeployed. The mapping to SQS FIFO queues (if ever used) is:

| Kafka concept | SQS FIFO equivalent |
|---------------|---------------------|
| key = aggregateId | `MessageGroupId = aggregateId` |
| eventId (UUID) | `MessageDeduplicationId = eventId` |
| topic | Queue name |

This is documented for adapter compatibility only. No SQS deployment is part of this plan.

### Kafka Is Primary; AWS Adapters Compile-Tested Only

- All active development targets `@Profile("!aws")` (Kafka).
- AWS adapters (`SqsMessageSender`, `ProcesionSqsConsumer`, `Cognito*Adapter`) remain in the codebase and compile.
- No AWS deployment, no `cdk deploy`, no credential configuration.
- `marcha-events` is approved as an additive CDK delta (not deployed). `cruceta-events` remains explicitly deferred; Cruceta AWS outbox rows remain pending/retried under the AWS profile.

### Fresh Environment with Flyway/Seeds

- Every environment (local dev, CI, VPS, test cluster) starts from `docker compose up` with Flyway migrations + seed data scripts.
- No data migration strategy needed — no production data exists.
- Seed scripts provide reproducible baselines for load tests.

### Provider-Neutral VPS Contract

The always-on VPS must meet these minimums, but is explicitly a demo environment, not a high-availability claim:

| Requirement | Minimum |
|-------------|---------|
| OS | Linux (any distribution with Docker support) |
| Access | Root SSH |
| Runtime | Docker Compose |
| CPU | 4 vCPUs |
| RAM | 8 GB |
| Storage | 80 GB SSD |
| Network | Public ports 80/443 + DNS A record |
| Image registry | GitHub Container Registry (GHCR) |
| Data persistence | Named persistent volumes for live data |
| Backups | pg_dump/pg_dumpall streamed off-host; never archive/rsync live Postgres volumes |
| Configuration | Environment variables only (no files with secrets) |

Deployment is `docker compose pull && docker compose up -d` from a GitHub Actions runner or human SSH session. Rollback is `docker compose up -d` with previous image SHA/tag.

### Throughput Proof Uses Disposable Multi-Node Environments

The load/failure evidence (Month 3) runs on a purpose-built, temporary multi-node environment — not the always-on VPS. The test environment is destroyed after evidence is collected. The deployment automation (Ticket 14) targets the VPS only; the test environment (Ticket 17) is a separate, parallel setup.

---

## Explicit Anti-Goals

These are deliberately excluded. No plan ticket will introduce them.

| Technology | Reason |
|-----------|--------|
| **Kubernetes** | Overkill for this three-service demo and temporary test footprint. Docker Compose + SSH is sufficient and simpler. |
| **Debezium / CDC** | The application-level outbox pattern is working and debuggable. CDC adds a second, opaque data path. |
| **Axon Framework / generic event platform** | No need for saga orchestration or event sourcing. Domain events are explicit Java records. |
| **New microservices** | Tracking and Notification remain stubs. All effort goes into the three existing services. |
| **Blanket caching** | No Redis/everywhere caching. Add caching only where measured bottlenecks prove it. |
| **Speculative pool / thread tuning** | No `-Xms` bumps, thread pool expansions, or connection pool changes without load-test evidence. |

---

## Month Mapping

| Month | Theme | Tickets | Gate |
|-------|-------|---------|------|
| **M1** | Correctness & Event Foundations | 01–08 | Integration Gate 09 |
| **M2** | Reliable Delivery, Operability, VPS | 10–14 | Integration Gate 15 |
| **M3** | Load/Failure Evidence & Tuning | 16–19 | Integration Gate 20 |
| **M4** | Route-Aware Cruceta | 21–23 | — |

Each month is 3–4 calendar weeks. AI capacity is modeled through dependency gates, not human-hour estimates.

---

## Dependency Graph and Parallel Frontiers

```text
                          Month 1 — Correctness
                         ┌──────────────────────┐
                         │ 01 Freeze contracts  │
                         └──────────┬───────────┘
              ┌──────────┬──────────┼──────────┬──────────┐
              ▼          ▼          ▼          ▼          │
     ┌────────────┐┌──────────┐┌──────────┐┌────────┐    │
     │ 02         ││ 03       ││ 04       ││ 08     │    │
     │ Procesion  ││ Cruceta  ││ Event    ││ Opt.   │    │
     │ tenant     ││ tenant   ││ envelope ││ lock   │    │
     │ isolation  ││ isolation││ + keyed  ││ repair │    │
     └─────┬──────┘└────┬─────┘└────┬─────┘└───┬────┘    │
           │            │           │           │         │
           │            │     ┌─────┴─────┐     │         │
           │            │     ▼     ▼     ▼     │         │
           │            │  ┌────┐┌────┐┌────┐  │         │
           │            │  │ 05 ││ 06 ││ 07 │  │         │
           │            │  │Herm││P→R ││Rep │  │         │
           │            │  └────┘└────┘└────┘  │         │
           └────────────┴──────────┬───────────┘         │
                                  │                      │
                          ┌───────┴──────────────────────┘
                          │        09 Gate
                          │   correctness
                          └────────────────────────────────
                                   │
            Month 2 — Reliability & Operability
                                   │
          ┌─────────────┬──────────┼──────────┬─────────────┐
          ▼             ▼          ▼          ▼
     [10 Outbox]  [11 Consumer] [12 Visibility] [13 VPS]
          │             │          └─────┬──────┘
          │             │                ▼
          │             │           [14 Deploy]
          └─────────────┴────────────────┘
                                   ▼
                              [15 Gate]
                                   │
            Month 3 — Load & Failure Evidence
                                           │
                              ┌────────────┼────────────┐
                              ▼            ▼            ▼
                     ┌────────────┐ ┌────────────┐
                     │ 16 Load/   │ │ 17 Multi-  │
                     │ failure    │ │ node test  │
                     │ harness    │ │ env        │
                     └─────┬──────┘ └──────┬─────┘
                           └──────┬────────┘
                                  ▼
                         ┌────────────────┐
                         │ 18 Untuned     │
                         │ baseline       │
                         └───────┬────────┘
                                 ▼
                         ┌────────────────┐
                         │ 19 Measured    │
                         │ tuning only    │
                         └───────┬────────┘
                                 ▼
                         ┌────────────────┐
                         │ 20 Gate: prove │
                         │ spike target   │
                         └───────┬────────┘
                                 │
            Month 4 — Route-Aware Cruceta (gated)
                                 │
                         ┌───────┴────────┐
                         │ 21 Named route │
                         │ points         │
                         └───────┬────────┘
                                 ▼
                         ┌────────────────┐
                         │ 22 Bind items  │
                         │ to moments     │
                         └───────┬────────┘
                                 ▼
                         ┌────────────────┐
                         │ 23 Run-sheet   │
                         │ progression    │
                         └────────────────┘
```

### Parallel Frontiers

| Frontier | Tickets | Description |
|----------|---------|-------------|
| F1 | 02, 03, 04, 08 | Month 1 correctness in parallel. Independent code areas. |
| F2 | 05, 06, 07 | Month 1–2 event migration. Parallel within frontier after Ticket 04 completes. |
| F3 | 10, 11, 12, 13 | Month 2 reliability and operability. Independent sub-trees. Tickets 10–12 are code; 13 is infrastructure. |
| F4 | 16, 17 | Month 3 harness and test environment. Independent (code vs infrastructure). |

Tickets 09, 15, 20 are integration-owner verification gates — single-threaded, no parallel work.

---

## Worktree/Lane Protocol

### Branch and Worktree Convention

Each ticket uses a dedicated Git worktree rooted at `.slim/worktrees/<slug>/`. The slug follows the pattern `{ticket-number}-{kebab-description}`.

```bash
# Create worktree (requires explicit confirmation + preflight)
git worktree add -b omos/02-procesion-tenant-isolation .slim/worktrees/02-procesion-tenant-isolation <approved-base>
# Work is done in that worktree, committed independently
```

### Lane Ownership Rules

| Lane | Slug pattern | Writer limit | File scope |
|------|-------------|-------------|------------|
| Correctness F1 | `0[2-4]-*`, `08-*` | One per ticket | Disjoint |
| Event migration F2 | `0[5-7]-*` | One per ticket | Disjoint across services |
| Reliability F3 | `1[0-3]-*` | One per ticket | Disjoint |
| Infrastructure | `13-*`, `14-*`, `17-*` | One per ticket | Infrastructure files |
| Integration gate | `09`, `15`, `20` | Orchestrator only | Integration tests + docs |
| Route-aware | `21-23-*` | One per ticket (Month 4) | Disjoint |

**Rules:**
- Explicit user confirmation required before any worktree/branch mutation.
- One writer per lane. Never run parallel writers on shared files.
- Central files (`docs/`, `docs/openapi.yaml`, `AGENTS.md`, `docker-compose.yml`) are owned by the integration lane (gates 09/15/20) or the orchestrator reconciliation commit. Ticket 01 updates `docs/openapi.yaml` with contracts needed by 02/03 before their code runs. No ticket writer modifies these without gate coordination.
- A controller change must be preceded by an OpenAPI contract change from the designated central-file owner. Parallel writers implement against that contract and do not edit `docs/openapi.yaml`; sequential Tickets 21–23 own it one at a time.
- Intermediate CI must stay green within each worktree. If a ticket is a cross-codebase expand/contract refactor (e.g., Ticket 04), the worktree may have transient compilation gaps across services but must be mergeable without breaking the build.

---

## Observable Integration Gates

### Gate 09 — Correctness Integrity Gate

**Evidence path:**
1. `./gradlew build` passes on all three services.
2. Tenant-isolation tests: reads and writes for a procession/Cruceta belonging to Hermandad A are 403 for a Hermandad B user.
3. Outbox-envelope tests: outbox rows carry `eventId`, `aggregateId`, `occurredAt`, `schemaVersion`.
4. Optimistic-lock tests: two successive updates succeed and a stale concurrent update fails through the real adapter.
5. Kafka adapter tests prove `aggregateId` is the record key; AWS compile/unit tests prove FIFO group=`aggregateId` and deduplication ID=`eventId` without deploying queues.
6. The temporary unkeyed sender overload and temporary default `schemaVersion` are removed; `docs/openapi.yaml` matches the implemented endpoints.

**Limitations:**
- Tests run on a single-node Testcontainers environment. Multi-replica claim ordering is deferred to Gate 15.
- No proof of correctness under load — that is Gate 20.

### Gate 15 — Reliable Deployment Gate

**Evidence path:**
1. Two concurrent pollers never claim the same row or the same aggregate simultaneously; successful events for one aggregate reach Kafka in creation order. Crash redelivery remains possible.
2. Duplicate Kafka delivery yields at most one committed business effect because the dedup claim and mutation share one transaction.
3. Outbox backlog, consumer lag, and error count visible via `curl localhost:8081/actuator/prometheus`.
4. VPS is reachable at its public DNS, `docker compose up` starts all services, `/health` endpoints return 200 (real readiness, not static nginx 200).
5. Deployment via `docker compose pull && docker compose up -d` succeeds from a clean SSH session.
6. Rollback via previous image SHA/tag restores the previous version.
7. Off-host backup + restore of every application database and Keycloak via pg_dump is documented and tested. Restore into clean databases, then verify migrations and service readiness.

**Limitations:**
- VPS is single-node, no HA. Restart has downtime. This is accepted — see confirmed decisions.
- Backups are scheduled; every restore remains manually approved and observed.
- Outbox is at-least-once: crash after publish before mark produces a duplicate. Acceptable because consumers are idempotent.

### Gate 20 — Regional Spike Throughput Gate

**Evidence path:**
1. k6 HTTP test sustains 1,000 req/s across all service endpoints for 5 minutes with p95 <300 ms.
2. The k6 event-producing HTTP scenario sustains 500 committed outbox events/s through Kafka → consumer for 5 minutes with freshness <5 s p95.
3. Measured baseline (Ticket 18) vs. tuned result (Ticket 19) comparison published in gate ticket.
4. The disposable multi-node environment used for the test is destroyed after evidence is captured.
5. All tests pass in single-node mode.

**Limitations:**
- Multi-node test is ephemeral. The always-on VPS does not reproduce multi-node throughput.
- Load test covers synthetic traffic, not real user patterns.
- Event freshness is measured from the producer-generated event `occurredAt` to consumer business transaction commit. The report also records outbox `created_at` so publisher delay can be diagnosed separately.

---

## Tickets

### Ticket 01: Freeze reliability contracts and evidence path

**Purpose / delivery:** Define the contracts, invariants, and measurement framework that all subsequent tickets verify against. No implementation — only documentation. Integration owner also updates `docs/openapi.yaml` with contracts needed by parallel tickets 02/03 before their code runs.

**Blockers:** None

**Lane / worktree slug:** `01-reliability-contracts`

**File / folder ownership:** Documentation and the central OpenAPI contract only; no production code:
- `docs/contracts/event-envelope.md` — versioned event envelope schema
- `docs/contracts/reliability-metrics.md` — target metrics and measurement protocol
- `docs/contracts/tenant-isolation.md` — tenant isolation invariants per service
- `docs/openapi.yaml` — add the approved tenant responses to the already-valid Path Items

Gate owner also touches `docs/openapi.yaml` to add endpoint contracts needed by 02/03.

**Implementation constraints:**
- Zero production code changes. Documentation only.
- CI already runs pinned Redocly semantic lint. Keep it green while adding the tenant contracts.
- Define the canonical `DomainEvent` shape (already has `eventId`, `occurredAt`; Ticket 04 adds `schemaVersion`).
- Contract for `eventId` generation: producer-generated `UUID.randomUUID()`. Deterministic hashing rejected because identical payloads that represent different events must not collide.
- Tenant isolation invariants cover reads and writes: reads require owning-tenant membership; writes additionally require the approved role.
- This ticket's updates to `docs/openapi.yaml` are for contracts needed by 02/03. Parallel writers do not edit it.

**Acceptance criteria:**
- Three contract documents exist with clear, testable assertions.
- Every subsequent ticket references these contracts.
- A reviewer can read any contract and write a failing test from the description.
- Redocly semantic lint passes locally and in CI; duplicate path keys can no longer pass silently.

**Smallest verification:**
```bash
git diff --check
npx --yes @redocly/cli@2.41.0 lint docs/openapi.yaml
```

**Suggested commit:** `docs: freeze reliability contracts and evidence path`

**Gherkin scenarios:** None (documentation-only ticket).

---

### Ticket 02: Enforce Procesion tenant isolation

**Purpose / delivery:** Every Procesion endpoint must enforce persisted tenant ownership. Authenticated members may read only their own Hermandad's processions; `POST`, `PATCH`, and `DELETE` additionally require `CAPATAZ` or `HERMANDAD_ADMIN` membership.

**Blockers:** 01

**Lane / worktree slug:** `02-procesion-tenant-isolation`

**File / folder ownership:**
- Modify: `services/procesion-service/src/main/java/com/repertorio/procesion/adapter/config/SecurityConfig.java`
- Create: `ProcesionSecurityService` in the Procesion application/security area.
- Modify: `services/procesion-service/src/main/java/com/repertorio/procesion/adapter/inbound/rest/controller/ProcesionController.java`
- Modify: `services/procesion-service/src/main/java/com/repertorio/procesion/application/service/ProcesionService.java`
- Modify: corresponding test files

**Implementation constraints:**
- Implement `@PreAuthorize` with a `ProcesionSecurityService` bean (mirror the Hermandad security pattern).
- The security service loads persisted ownership. Reads require membership in the owning Hermandad; writes require `CAPATAZ` or `HERMANDAD_ADMIN`.
- Use DB lookup for the procession's hermandadId (fast path via JWT is not sufficient without persisted verification).
- Add `@EnableMethodSecurity` if not already active.

**Acceptance criteria:**
- `POST /api/procesiones` with Hermandad A JWT creates a procession for A.
- `PATCH /api/procesiones/{id}/status` with Hermandad B JWT on Hermandad A's procession → 403.
- `DELETE /api/procesiones/{id}` with Hermandad B JWT → 403.
- `GET /api/procesiones/{id}` and `GET /api/procesiones?hermandadId=A` with only Hermandad B membership → 403.
- An unauthenticated request to any write endpoint → 401.
- A user with `MUSICIAN` role for the correct Hermandad → 403 (not authorized).
- At least one failing test existed before the fix (RED).

**Smallest verification:**
```bash
./gradlew :services:procesion-service:test
```

**Suggested commit:** `feat(procesion): enforce tenant isolation on all endpoints`

**Gherkin:**
```gherkin
Feature: Procesion tenant isolation
  Scenario: admin manages own Hermandad's procession
    Given a JWT with HERMANDAD_ADMIN membership for Hermandad A
    When creating a procession for Hermandad A
    Then the response is 201

  Scenario: cross-tenant access denied
    Given a JWT with HERMANDAD_ADMIN membership for Hermandad B
    When changing status of Hermandad A's procession
    Then the response is 403

  Scenario: insufficient role denied
    Given a JWT with MUSICIAN membership for Hermandad A
    When deleting Hermandad A's procession
    Then the response is 403

TDD steps:
  RED: Write security service test with cross-tenant assertion (expects AccessDeniedException).
  GREEN: Implement separate persisted-ownership checks for member reads and CAPATAZ/admin writes.
  REFACTOR: Extract common auth pattern if duplicated.
  OpenAPI-first: Ticket 01 owns the read/write 401/403 contract. Implement against it; do not edit the central file in this parallel lane.
```

---

### Ticket 03: Enforce Cruceta tenant isolation and safe replacement

**Purpose / delivery:** Cruceta `GET` and `PUT` must verify that the target procession belongs to the authorized Hermandad. Replacement must be atomic with deterministic concurrency behavior: requests serialize, or one succeeds and the stale request returns 409; partial state is impossible.

**Blockers:** 01

**Lane / worktree slug:** `03-cruceta-tenant-isolation`

**File / folder ownership:**
- Modify: `services/repertorio-service/src/main/java/com/repertorio/marcha/adapter/inbound/rest/controller/CrucetaController.java`
- Modify: `services/repertorio-service/src/main/java/com/repertorio/marcha/application/service/CrucetaService.java`
- Modify: `services/repertorio-service/src/main/java/com/repertorio/marcha/domain/model/Cruceta.java`
- Modify: corresponding test files

**Implementation constraints:**
- The `CrucetaService.defineCruceta()` must validate `KnownProcesion.hermandadId` against the authorized `{hermandadId}` from the path — not just that the procesion exists.
- Pass `{hermandadId}` into both application operations; never authorize only the path and then load by `procesionId` without comparing persisted ownership.
- Reads require membership in the owning Hermandad. Replacement requires `CAPATAZ` or `HERMANDAD_ADMIN`.
- A transaction alone is insufficient. Update a managed Cruceta with optimistic/pessimistic concurrency control and map a stale/unique race to 409. Ticket 08 supplies shared version-propagation repair before Gate 09.

**Acceptance criteria:**
- `PUT /api/hermandades/{hid}/procesiones/{pid}/cruceta` with JWT for Hermandad A on Hermandad B's procession → 403.
- Cross-tenant `GET` on the same path → 403.
- Concurrent `PUT` requests serialize or one returns 409; the stored Cruceta is one complete submitted version with no duplicate/partial items.
- Existing test suite passes with no regression.

**Smallest verification:**
```bash
./gradlew :services:repertorio-service:test
```

**Suggested commit:** `feat(repertorio): enforce cruceta tenant isolation and safe replacement`

**Gherkin:**
```gherkin
Feature: Cruceta tenant isolation
  Scenario: admin defines Cruceta for own Hermandad
    Given a JWT with HERMANDAD_ADMIN for Hermandad A
    And a procession belonging to Hermandad A
    When defining a Cruceta with valid marchas
    Then the response is 200

  Scenario: admin cannot define Cruceta for another Hermandad
    Given a JWT with HERMANDAD_ADMIN for Hermandad B
    And a procession belonging to Hermandad A
    When defining a Cruceta
    Then the response is 403

TDD steps:
  RED: Write CrucetaServiceTest with cross-tenant 403 assertion.
  GREEN: Pass hermandadId into GET/PUT application operations, compare it to KnownProcesion ownership, and make replacement atomic.
  REFACTOR: Align RepertorioSecurityService with ProcesionSecurityService pattern.
  OpenAPI-first: Ticket 01 owns the GET/PUT 401/403/409 contract. Implement against it; do not edit the central file in this parallel lane.
```

---

### Ticket 04: Expand the versioned event envelope and outbox schema

**Purpose / delivery:** Add explicit `schemaVersion` to the shared `DomainEvent` contract and transport `aggregateId` plus `eventId` through the messaging seam. Update shared outbox persistence. This is a cross-service expand/contract refactor because one shared interface has Kafka and SQS adapters in all three services.

**Blockers:** 01

**Lane / worktree slug:** `04-event-envelope`

**File / folder ownership:**
- Modify: `shared/common/src/main/java/com/repertorio/common/event/DomainEvent.java` — add `schemaVersion()` with default `return 1`.
- Modify: `shared/common/src/main/java/com/repertorio/common/messaging/MessageSender.java` — accept destination, aggregateId, eventId, and payload (or one immutable message record containing those fields). Preserve the old overload only during expand commits; remove it at Gate 09.
- Modify: `shared/common/src/main/java/com/repertorio/common/outbox/OutboxEventEntity.java` — add `eventId UUID`, `occurredAt TIMESTAMPTZ`, `schemaVersion INTEGER` columns.
- Modify: `shared/common/src/main/java/com/repertorio/common/outbox/OutboxEventPublisher.java` — populate new fields from `DomainEvent` interface.
- Modify: `shared/common/src/main/java/com/repertorio/common/outbox/OutboxPoller.java` — pass both aggregateId and eventId to `MessageSender`.
- Adapt every Kafka/SQS sender implementation. Kafka uses `aggregateId` as record key. The compile-tested FIFO SQS path maps group=`aggregateId` and deduplication ID=`eventId`; it remains undeployed until its queue is FIFO.
- Create: Flyway migration in each service — add columns to `outbox_event` table:
  - Hermandad: next available forward version (currently V9; recheck at dispatch time)
  - Procesion: next available forward version (currently V5; recheck at dispatch time)
  - Repertorio: next available forward version (currently V9; recheck at dispatch time)
- Do NOT touch `ProcessedEventEntity`, `ProcessedEventJpaRepository`, or service-specific consumers. Those belong to Tickets 05/06/07/11.
- Tests: verify new fields on all outbox rows.

**Implementation constraints:**
- `eventId` already on every domain event (producer-generated `UUID.randomUUID()`). `occurredAt` already on every event. `schemaVersion` = 1 for all current events.
- A default `schemaVersion=1` is allowed only as a temporary expand step. Tickets 05–07 implement it explicitly and Gate 09 removes the default so missing metadata cannot be hidden.
- This is an expand/contract: (a) expand adds new fields + new interface method, old code still compiles; (b) once all services are updated, old interface methods can be removed. The worktree may have transient compilation gaps across services during the expand phase.
- No unsafe default IDs/timestamps. Missing metadata fails validation. No DOWN migrations.
- Migration versions described as "next available forward version" — recheck at dispatch time because unrelated Hermandad migrations may have been added.

**Acceptance criteria:**
- `DomainEvent` exposes `schemaVersion`; its temporary default is marked for mandatory removal at Gate 09.
- `MessageSender` carries destination, aggregateId, eventId, and payload. No adapter parses payload to recover transport metadata.
- Outbox rows in all three databases contain non-null `event_id`, `occurred_at`, `schema_version`.
- Kafka sender uses key=aggregateId; SQS adapter tests verify FIFO group and deduplication metadata.
- No changes to `ProcessedEventEntity` or consumer processing logic.

**Smallest verification:**
```bash
./gradlew build
```

**Suggested commit:** `feat(shared): add schemaVersion to DomainEvent, keyed MessageSender, expand outbox schema`

**TDD steps:**
  RED: Write contract tests proving schemaVersion is present and both transport adapters receive aggregateId/eventId without parsing payload.
  GREEN: Add `schemaVersion()` default to interface, verify test passes.
  EXPAND: Key the MessageSender, update poller, add Flyway migrations. Verify `./gradlew build`.
  OpenAPI-first: No controller changes — events are internal. No OpenAPI update needed.

---

### Ticket 05: Migrate Hermandad event flow

**Purpose / delivery:** Update Hermandad's event publishing and consumption to use the new versioned event envelope. Consumer dedup uses the producer-generated `eventId` from the envelope (already present — migrate from any remaining payload-hash path).

**Blockers:** 04

**Lane / worktree slug:** `05-hermandad-event-flow`

**File / folder ownership:**
- Modify: `services/hermandad-service/src/main/java/com/repertorio/hermandad/application/event/MemberAddedListener.java`
- Modify: `services/hermandad-service/src/main/java/com/repertorio/hermandad/adapter/inbound/kafka/IdempotentEventConsumer.java` — migrate dedup key if needed
- Tests: update `IdempotentEventConsumerTest` to verify dedup by eventId.

**Implementation constraints:**
- The `IdempotentEventConsumer` must use `eventId` from the consumed event envelope for dedup.
- The `MemberAddedListener` (in-process Spring event consumer) receives events already carrying the new envelope.
- Existing tests must pass without modification except for the dedup key change.
- Keep `processed_event` table structure for now. Ticket 11 owns the atomicity upgrade and `(consumer_name, event_id)` uniqueness.

**Acceptance criteria:**
- `IdempotentEventConsumer` deduplicates by `eventId`.
- Two identical payloads with different `eventId` values are both processed.
- Same `eventId` delivered twice → second is skipped.
- Hermandad event records implement `schemaVersion` explicitly so Gate 09 can remove the temporary default.

**Smallest verification:**
```bash
./gradlew :services:hermandad-service:test
```

**Suggested commit:** `refactor(hermandad): migrate event flow to versioned envelope`

---

### Ticket 06: Migrate Procesion-to-Repertorio flow

**Purpose / delivery:** Complete the ordered Procesion→Repertorio projection flow: created, status-changed, and deleted events use the explicit envelope; failures reach Kafka retry handling; deletion removes the derived KnownProcesion and associated Cruceta; invalid state regressions cannot overwrite newer projection state.

**Blockers:** 04

**Lane / worktree slug:** `06-procesion-repertorio-flow`

**File / folder ownership:**
- Modify: `services/procesion-service/src/main/java/com/repertorio/procesion/adapter/outbound/events/DomainEventPublisherAdapter.java`
- Modify: `services/repertorio-service/src/main/java/com/repertorio/marcha/adapter/inbound/kafka/ProcesionEventConsumer.java`
- Modify: `services/repertorio-service/src/main/java/com/repertorio/marcha/application/event/ProcesionEventProcessor.java`
- Modify: KnownProcesion/Cruceta repository ports and adapters only as required for deletion and monotonic projection handling.
- Tests: update `ProcesionEventConsumerTest` to verify dedup by eventId.

**Implementation constraints:**
- Add `ProcesionDeletedEvent`; all three event records explicitly implement schemaVersion and use the keyed transport from Ticket 04.
- Parse/switch on explicit `eventType`; do not infer event type from payload-field presence.
- Consumer dedup uses producer-generated `eventId`.
- Consumer exceptions propagate so Kafka retries; no catch-and-ack loss.
- Projection updates accept only valid forward status transitions. The same aggregate's outbox rows are serialized by Ticket 10; duplicate redelivery is handled by Ticket 11.

**Acceptance criteria:**
- `ProcesionEventConsumer` deduplicates by producer-generated `eventId`.
- Two `ProcesionCreatedEvent` with different ids and same payload are both processed.
- Same `eventId` delivered twice → consumer skips the second.
- Created → status → deleted leaves no stale KnownProcesion or Cruceta; an invalid status regression is rejected and observable.

**Smallest verification:**
```bash
./gradlew :services:procesion-service:test :services:repertorio-service:test
```

**Suggested commit:** `refactor(procesion,repertorio): migrate cross-service event flow to versioned envelope`

---

### Ticket 07: Migrate Repertorio producer flow

**Purpose / delivery:** Update Repertorio's outbox publishing to use the versioned event envelope for all produced events (`MarchaAddedEvent`, `MarchaRemovedEvent`, `CrucetaDefinedEvent`).

**Blockers:** 04

**Lane / worktree slug:** `07-repertorio-producer-flow`

**File / folder ownership:**
- Modify: `services/repertorio-service/src/main/java/com/repertorio/marcha/adapter/outbound/events/DomainEventPublisherAdapter.java`
- Tests: update `MarchaServiceTest` and `CrucetaServiceTest` to assert eventId presence.

**Implementation constraints:**
- All three Repertorio domain events already implement `DomainEvent` with `eventId`, `occurredAt`, `aggregateId`, `eventType`. Verify `schemaVersion` from Ticket 04 is propagated.
- `OutboxEventPublisher` persists the new fields from the envelope; the poller only claims, sends, and marks rows.
- No consumer exists for `marcha-events` — this ticket only ensures the producer side is correct.

**Acceptance criteria:**
- `MarchaAddedEvent`, `MarchaRemovedEvent`, `CrucetaDefinedEvent` all carry `eventId`, `occurredAt`, `aggregateId`, `eventType`, `schemaVersion`.
- Outbox rows produced by Repertorio contain the new columns.

**Smallest verification:**
```bash
./gradlew :services:repertorio-service:test
```

**Suggested commit:** `refactor(repertorio): migrate producer events to versioned envelope`

---

### Ticket 08: Repair optimistic-lock version propagation

**Purpose / delivery:** All aggregate JPA entities already have `@Version` annotations. The remaining gap is correct version propagation and mapping through repository adapters — especially in Procesion and Repertorio services. Tests must prove two successive updates succeed and stale concurrent updates fail through real adapters.

**Blockers:** 01

**Lane / worktree slug:** `08-optimistic-lock`

**File / folder ownership:**
- Verify: all 6 aggregate JPA entities have properly typed `@Version` fields (already: `HermandadEntity`, `HermandadMemberEntity`, `ProcesionEntity`, `MarchaEntity`, `CrucetaEntity`, `CrucetaItemEntity`).
- Modify: `ProcesionRepositoryAdapter.java` — ensure version is preserved on update (use `findById` + `merge` or load managed entity).
- Modify: `MarchaRepositoryAdapter.java`, `CrucetaRepositoryAdapter.java` — same treatment.
- Create: `ConcurrentWriteTest.java` in each service — Testcontainers integration test that fires two concurrent writes to the same aggregate and asserts one fails.
- The domain model does NOT expose the version field — it is an adapter concern. Do not add `@Version` to entities that already have it.

**Implementation constraints:**
- No duplicate `@Version` annotations. Do not add annotations that source shows already exist.
- For existing rows, adapters load the managed entity and update its fields so JPA retains the adapter-owned version. Do not merge a reconstructed entity that lacks version state.
- Hermandad is the reference but has unrelated in-progress fixes — do not touch Hermandad adapter files.
- Tests must prove (a) two successive updates to the same aggregate succeed, (b) concurrent stale updates fail with `OptimisticLockException`.

**Acceptance criteria:**
- All aggregate JPA entities have `@Version` (verify; add only if source shows one missing).
- Concurrent writes to the same aggregate throw `OptimisticLockException` in the second writer.
- Successive updates (read → modify → save → read → modify → save) succeed without stale state.
- All existing tests pass.

**Smallest verification:**
```bash
./gradlew build
```

**Suggested commit:** `fix: repair @Version propagation through repository adapters`

**Gherkin:**
```gherkin
Feature: Optimistic locking prevents lost updates
  Scenario: concurrent writes to same aggregate
    Given two concurrent requests to update the same Procesion
    When both requests read the current state
    And both requests attempt to save with the same version
    Then one request succeeds
    And the other request fails with OptimisticLockException

TDD steps:
  RED: Write ConcurrentWriteTest that fires two parallel updates and asserts exactly one fails.
  GREEN: Fix ProcesionRepositoryAdapter to preserve version through merge/managed-load.
  VERIFY: All adapters now handle version correctly.
```

---

### Ticket 09: Pass the correctness integration gate

**Purpose / delivery:** Orchestrator-only ticket. Execute the Gate 09 evidence path. Merge all Month 1 worktrees. Run full test suite. Fix any integration issues discovered. Update `docs/openapi.yaml` with any missed endpoint changes. Update `docs/functional-map.md` with tenant isolation status.

**Blockers:** 02, 03, 04, 05, 06, 07, 08 (must all be merged)

**Lane / worktree slug:** `09-correctness-gate`

**File / folder ownership:**
- Verify: `docs/openapi.yaml` (gate audit)
- Modify: `docs/functional-map.md` (update tenant isolation status)
- Tests: orchestrator-level integration tests (full `docker compose` stack)

**Implementation constraints:**
- This ticket adds zero new functionality. It is a verification gate.
- Any test failure in this gate blocks Month 2 work.
- If a test depends on a Month 2 change (e.g., multi-replica outbox), skip it and log the gap — do not expand scope.
- Remove the deprecated unkeyed sender overload and temporary default schemaVersion before the gate can pass.

**Acceptance criteria:** See Gate 09 evidence path above.

**Smallest verification:**
```bash
./gradlew build
```

**Suggested commit:** `gate: pass correctness integration gate 09`

---

### Ticket 10: Make outbox polling multi-replica safe

**Purpose / delivery:** Ensure that running two or more replicas of the same service does not cause duplicate outbox message publication beyond the inherent at-least-once contract (crash after publish before mark can duplicate; consumer idempotency absorbs it). Use `SELECT ... FOR UPDATE SKIP LOCKED` with claim tokens, bounded retries, and proper indexing.

**Blockers:** 09

**Lane / worktree slug:** `10-outbox-multi-replica`

**File / folder ownership:**
- Modify: `shared/common/src/main/java/com/repertorio/common/outbox/OutboxPoller.java` — claim/lease pattern
- Modify: `shared/common/src/main/java/com/repertorio/common/outbox/OutboxEventEntity.java` — add claim + retry columns
- Modify: `shared/common/src/main/java/com/repertorio/common/outbox/OutboxEventJpaRepository.java` — add locking query
- Create: Flyway migration (all three services) — add columns: `claimed_by VARCHAR(100)`, `claimed_at TIMESTAMPTZ`, `retry_count INTEGER DEFAULT 0`, `next_attempt_at TIMESTAMPTZ`, `last_error TEXT`, `terminal BOOLEAN DEFAULT FALSE`. Add an eligible-row index and an aggregate-order index covering `aggregate_id`, `processed`, and `created_at`.
- Tests: `OutboxPollerMultiReplicaTest.java` — integration test with two poller instances verifying disjoint row/aggregate claims, ordering, expiration, retries, and terminal state. Consumer absorption is verified at Gate 15 after Ticket 11 merges.

**Implementation constraints:**
- Short DB transaction selects `FOR UPDATE SKIP LOCKED`, writes claim token + `claimed_at`, commits.
- Publish happens OUTSIDE the claim transaction (no long-held locks).
- On success: mark `processed = true`, clear claim.
- On failure: increment `retry_count`, set `last_error`, compute `next_attempt_at` (exponential backoff), set `terminal = true` after max retries (configurable, default 5).
- Expired claim (configurable timeout, default 30s) is eligible for reprocessing.
- A claim query may select only the oldest eligible row for an aggregate, and no aggregate with an active claim. This serializes publication per aggregate across replicas.
- Duplicate after crash is expected and accepted — consumers must be idempotent.
- No promise of exactly-once delivery. This ticket prevents concurrent claims, not crash redelivery.

**Acceptance criteria:**
- Every non-terminal, publishable row is attempted at least once; crash redelivery may duplicate it.
- No more than one active claimant per row at any time.
- No more than one active claimant per aggregate; successful rows for one aggregate publish in `created_at` order.
- If one instance crashes after locking but before sending, the row is reprocessed after the lock timeout.
- Retry metadata is persisted: `retry_count`, `next_attempt_at`, `last_error`. Terminal rows are not retried.
- Eligible-row and per-aggregate-order indexes exist in each service's `outbox_event` table.

**Smallest verification:**
```bash
./gradlew :services:hermandad-service:test --tests "*OutboxPollerMultiReplicaTest*"
./gradlew :services:procesion-service:test --tests "*OutboxPollerMultiReplicaTest*"
./gradlew :services:repertorio-service:test --tests "*OutboxPollerMultiReplicaTest*"
```

**Suggested commit:** `feat(shared): make outbox polling multi-replica safe with SELECT FOR UPDATE SKIP LOCKED and retry metadata`

**Gherkin:**
```gherkin
Feature: Multi-replica outbox safety
  Scenario: two pollers claim disjoint rows
    Given an outbox with 10 unprocessed rows
    And two poller instances running concurrently
    When both pollers poll
    Then no row is claimed by both pollers
    And each aggregate's rows are published in creation order

  Scenario: crashed poller recovery
    Given an outbox row claimed by a crashed instance
    When the claim timeout expires
    Then another poller picks up the row

  Scenario: terminal failure stops retries
    Given an outbox row that has failed 5 times
    When any poller runs
    Then the row is not retried
    And it remains available for manual inspection
```

---

### Ticket 11: Make consumer idempotency atomic

**Purpose / delivery:** Ensure consumer-side dedup is transactional. The current check-then-insert pattern has a race: two consumer instances could both check, find no record, and both process the same event. Fix with unique upsert in the same DB transaction as the business mutation.

**Blockers:** 09

**Lane / worktree slug:** `11-consumer-idempotency`

**File / folder ownership:**
- Modify: `services/hermandad-service/src/main/java/com/repertorio/hermandad/adapter/outbound/events/ProcessedEventJpaRepository.java`
- Modify: `services/repertorio-service/src/main/java/com/repertorio/marcha/adapter/outbound/events/ProcessedEventJpaRepository.java`
- Modify: consumers in hermandad and repertorio to use atomic insert-on-conflict with the business mutation in the same DB transaction.
- Create: forward Flyway migration in both services — replace the current `event_id`-only primary key with a composite primary/unique key on `(consumer_name, event_id)` while preserving existing rows.
- Tests: `ConsumerIdempotencyAtomicTest.java` — fire two concurrent deliveries with the same `(consumerName,eventId)` and verify at most one business transaction commits.

**Implementation constraints:**
- Use `INSERT ... ON CONFLICT DO NOTHING` (Postgres upsert) as the claim mechanism.
- BOTH the unique `(consumer_name, event_id)` claim AND the business mutation must share one DB transaction. If mutation fails, the claim rolls back and the event is eligible for retry.
- If the claim finds a duplicate (zero rows inserted), skip processing entirely.
- `consumer_name` is a service-specific identifier (e.g., `hermandad-self-consumer`, `repertorio-procesion-consumer`).
- Hermandad's current `IdempotentEventConsumer` is audit-only — update it to the atomic pattern but do NOT call it a business exactly-once processor (it has no downstream mutation).
- Stable eventId migration belongs in Tickets 05/06; this ticket owns atomicity and `(consumer_name, event_id)` uniqueness.

**Acceptance criteria:**
- Two concurrent deliveries of the same `(consumerName,eventId)` → at most one committed business effect; the duplicate is skipped.
- Check-then-insert race is eliminated (prove with concurrent test).
- If the business mutation fails, the claim rolls back and the event retries.
- Duplicate claimant skips (zero rows affected from upsert).

**Smallest verification:**
```bash
./gradlew :services:hermandad-service:test --tests "*ConsumerIdempotencyAtomicTest*"
```

**Suggested commit:** `fix: make consumer idempotency atomic with INSERT ON CONFLICT DO NOTHING and transactional dedup`

---

### Ticket 12: Add minimum operational visibility

**Purpose / delivery:** Expose HTTP rate/latency/errors, outbox backlog/oldest age, publish failures/latency, Kafka lag/retries, and event freshness through separate observability adapters and native framework instrumentation. Add structured JSON logging without editing Ticket 10 or Ticket 11 implementation files.

**Blockers:** 09

**Lane / worktree slug:** `12-operational-visibility`

**File / folder ownership:**
- Create: new observability packages in `shared/common/src/main/java/com/repertorio/common/observability/` — repository-backed `processed = FALSE` backlog and oldest-age gauges that work before Ticket 10 merges, plus separate sender/listener observations. Terminal/retry breakdown is integrated at Gate 15 after the claim schema exists.
- Modify: each service's `build.gradle.kts` — ensure `micrometer-registry-prometheus` is a runtime dependency.
- Modify: each service's `application.yml` — expose health/prometheus and enable Spring Boot's native structured logging after confirming the Spring Boot 4.1 property from official docs.
- Create: separate `MeterBinder`, Kafka listener/record observation, and `MessageSender` decorator/configuration classes; do not edit consumer processors or poller claim code.
- Tests: focused Actuator/Micrometer tests assert metric names, tags, values, and freshness timing.
- Do NOT edit any Ticket 10 (outbox poller internals) or Ticket 11 (consumer processor/repository) implementation files. Observability hooks go into separate wrapper/decoration classes.

**Implementation constraints:**
- Use Micrometer's `Counter`, `Gauge`, and `Timer` directly (not `@Timed`).
- Outbox backlog gauge counts all unprocessed rows, including terminal rows that require operator attention.
- Oldest-age gauge: TIMESTAMPTZ difference for oldest unprocessed eligible row.
- Broker-provided consumer lag exposed via Kafka's built-in Micrometer metrics (added automatically by `spring-kafka`).
- HTTP metrics: Spring Boot actuator provides `/actuator/metrics/http.server.requests` automatically.
- Correlation/event-freshness observation uses separate transport/listener instrumentation and ends after the consumer transaction commits.
- Use Spring Boot native structured logging; do not add Logstash solely for JSON formatting.
- No Grafana dashboard in this ticket — just the raw metrics.
- Metrics must include `service` tag for multi-service disambiguation.

**Acceptance criteria:**
- Automated Actuator tests prove the Prometheus endpoint contains the required metrics with service tags; a curl smoke check is supplementary only.
- Each service has `outbox_backlog`, `outbox_processed_total`, `outbox_failed_total`, `outbox_age_oldest_seconds`.
- Each consumer has `consumer_processed_total`, `consumer_duplicate_skipped_total`.
- Log lines from OutboxPoller include `aggregateType`, `eventType`, `eventId` as structured fields.
- No edits to Ticket 10's OutboxPoller claim/lease logic or Ticket 11's consumer processor/repository files.

**Smallest verification:**
```bash
./gradlew :services:hermandad-service:test :services:procesion-service:test :services:repertorio-service:test --tests "*Observability*"
```

**Suggested commit:** `feat(shared): add outbox and consumer Prometheus metrics with structured logging`

---

### Ticket 13: Build the provider-neutral VPS runtime

**Purpose / delivery:** Set up the single always-on VPS with Docker Compose, database-consistent pg_dump backups off-host, and nginx reverse proxy with TLS. The VPS runs a dedicated reduced compose file, not the full development stack.

**Blockers:** 09

**Lane / worktree slug:** `13-vps-runtime`

**File / folder ownership:**
- Create: `infrastructure/vps/docker-compose.vps.yml` — dedicated VPS compose with nginx/TLS; three services; one PostgreSQL 16 engine with separate databases/users for each service and Keycloak; Kafka KRaft; Keycloak with its own DB; and Redis for the existing Hermandad cache. Omit ZooKeeper, Eureka, Gateway, Kafka UI, stubs, and heavy local observability containers.
- Create: `infrastructure/vps/backup.sh` — database-consistent pg_dump/pg_dumpall for every application DB and Keycloak, streamed/copied off-host. Never archive/rsync live Postgres volumes.
- Create: `infrastructure/vps/restore.sh` — restore into clean databases, then verify migrations and service readiness.
- Create: `infrastructure/vps/verify-deployment.sh` — assert Compose configuration and each service readiness endpoint without static nginx success.
- Create: `infrastructure/vps/.env.template` — all required environment variables.
- This ticket owns compose/runtime/scripts only — never workflow files (those are Ticket 14).

**Implementation constraints:**
- No hardcoded secrets. All configuration is environment variables.
- A restricted deployment user owns the application directory and Docker access; routine deployment does not use root.
- Services use immutable GHCR images built from the same Dockerfiles as local development. Live PostgreSQL, Kafka, Keycloak, and Redis data use named persistent volumes.
- Deployment checks each service's real readiness endpoint (DB/Kafka dependencies as appropriate), not a static nginx 200.
- nginx reverse proxy terminates TLS (Let's Encrypt via certbot or Caddy).
- No Eureka or API Gateway on the VPS — nginx routes by path prefix.
- No workload files, CI workflows, or deploy automation in this ticket.
- Any external VPS, DNS, TLS, backup, restore, or teardown action requires explicit human approval. Local Compose validation does not.

**Acceptance criteria:**
- `docker compose -f infrastructure/vps/docker-compose.vps.yml up -d` starts all services.
- Each service readiness endpoint is reachable through nginx and returns 200 only when its required dependencies are ready.
- `curl https://<vps-domain>/api/hermandades` returns data (requires JWT — test with seeded token).
- `infrastructure/vps/backup.sh` creates a timestamped pg_dump archive on the backup host.
- `infrastructure/vps/restore.sh` restores the latest backup into clean databases and verifies readiness.

**Smallest verification:**
```bash
docker compose -f infrastructure/vps/docker-compose.vps.yml config
./gradlew build
```

**Suggested commit:** `feat(vps): add provider-neutral VPS runtime with nginx, volumes, and backup scripts`

---

### Ticket 14: Automate immutable deployment, rollback, and backup restore

**Purpose / delivery:** Create a GitHub Actions workflow that builds Docker images, pushes to GHCR, deploys to the VPS via SSH, and includes automated rollback on health-check failure. This ticket owns workflow files only. Build/push/deploy uses immutable SHA tags only — no `latest`. Compose requires explicit image SHA/tag; rollback records the prior deployed SHA.

**Blockers:** 12, 13

**Lane / worktree slug:** `14-deploy-automation`

**File / folder ownership:**
- Create: `.github/workflows/deploy-vps.yml` — build → GHCR (SHA tags only) → SSH deploy → health check → rollback on failure. Preserve the existing AWS workflow as manual compatibility history unless its retirement is separately approved.
- Create: `.github/workflows/backup.yml` — scheduled nightly backup via `infrastructure/vps/backup.sh`.
- Create: `.github/workflows/restore.yml` — manual-dispatch workflow to restore from latest backup.
- No external deployment, DNS/TLS mutation, restore, or teardown without human confirmation.
- No actual deployment in this ticket unless explicitly authorized at execution time.

**Implementation constraints:**
- Deploy workflow tags images with `:{sha}` (Git commit SHA). No `:latest` tag.
- No `git push origin main` as verification. Use dry validation (`docker compose config`, `./gradlew build`) plus a human-approved `workflow_dispatch`.
- Compose file references explicit image SHA/tag (not mutable tags).
- Rollback records the prior deployed SHA and uses it explicitly.
- Use the built-in `GITHUB_TOKEN` with `packages: write` for GHCR. External secrets are limited to `VPS_SSH_KEY`, `VPS_HOST`, `VPS_USER`, `BACKUP_HOST`, `BACKUP_PATH`, and `BACKUP_SSH_KEY`.
- Restore uses a protected GitHub environment and typed confirmation because it is destructive.
- No CD to AWS — this targets the provider-neutral VPS only.

**Acceptance criteria:**
- `workflow_dispatch` with approved commit SHA triggers build, GHCR push, SSH deploy, and health check.
- If health check fails, workflow automatically runs rollback and marks the run as failed.
- Nightly backup runs and creates archive on backup host.
- Restore workflow, when triggered, restores the latest backup and all three services start correctly.

**Smallest verification:**
```bash
./gradlew build
docker compose -f infrastructure/vps/docker-compose.vps.yml config
```

**Suggested commit:** `feat(ci): automate immutable VPS deploy, rollback, and backup restore`

---

### Ticket 15: Pass the reliable-deployment gate

**Purpose / delivery:** Orchestrator-only ticket. Execute the Gate 15 evidence path. Merge all Month 2 worktrees. Run full test suite. Verify VPS deployment, rollback, and backup restore end-to-end. Update docs.

**Blockers:** 10, 11, 12, 13, 14 (must all be merged)

**Lane / worktree slug:** `15-reliable-deploy-gate`

**File / folder ownership:**
- Verify: `docs/functional-map.md`, `docs/openapi.yaml`
- Verify: VPS deployment (manual run of deploy workflow or SSH)
- Verify: backup and restore scripts (manual test)

**Implementation constraints:**
- This ticket adds zero new functionality.
- A multi-replica outbox test must pass (two JVM instances poll same DB; disjoint claims verified).
- Consumer idempotency atomicity test must pass.
- Metrics must be visible on the VPS deployment.

**Acceptance criteria:** See Gate 15 evidence path above.

**Smallest verification:**
```bash
./gradlew build
bash infrastructure/vps/verify-deployment.sh --local-compose
```

**Suggested commit:** `gate: pass reliable deployment gate 15`

---

### Ticket 16: Build the repeatable load/failure harness

**Purpose / delivery:** Create a repeatable k6 workload that invokes real HTTP commands which commit business state and outbox rows, then measures the resulting Kafka/consumer path. Direct Kafka production is forbidden because it would bypass the behavior being proved.

**Blockers:** 15

**Lane / worktree slug:** `16-load-failure-harness`

**File / folder ownership:**
- Create: `load-testing/k6/` — directory with k6 scripts
  - `k6/hermandad-api.js` — Hermandad CRUD load test
  - `k6/procesion-api.js` — Procesion CRUD + status transitions
  - `k6/repertorio-api.js` — Marcha + Cruceta load test
  - `k6/mixed-workflow.js` — realistic cross-service workflow mix
  - `k6/helpers.js` — shared token acquisition and base URLs; never sign JWTs locally
- Add a dedicated k6 constant-arrival-rate scenario for 500 event-producing HTTP commands/s.
- Create: `load-testing/README.md` — how to run each test, expected metrics, and report format

**Implementation constraints:**
- k6 scripts must be self-contained (no external dependencies beyond k6 binary).
- Event workloads call application endpoints only; they never write Kafka or outbox tables directly.
- Tests accept base URLs, duration/rate, and either a supplied token or token-endpoint credentials via environment variables. No credentials enter source or reports.
- Raw high-volume results are ignored by Git and retained as CI/external artifacts. The repository stores compact summaries, scenario configuration, checksums, and artifact links.
- The harness must be runnable against both the local Docker Compose environment and the multi-node test environment (Ticket 17).
- Include a failure scenario test: stop Kafka, verify outbox backlog grows, restart Kafka, verify backlog drains.
- Destructive stop/restart scenarios run only in the disposable test environment after explicit approval.

**Acceptance criteria:**
- `k6 run load-testing/k6/mixed-workflow.js` runs without errors against local stack.
- The event scenario sustains its configured arrival rate and correlates event `occurredAt` to committed consumer metrics.
- Failure test: kill Kafka → outbox backlog > 0 → restart Kafka → backlog drains to 0.
- Each run emits machine-readable raw output to the configured artifact location and a compact repository summary.

**Smallest verification:**
```bash
bash load-testing/verify-harness.sh --local --smoke
```

**Suggested commit:** `test: add k6 HTTP and event-flow load harness`

---

### Ticket 17: Build the temporary multi-node test environment

**Purpose / delivery:** Configure a disposable four-role environment: two independent application nodes, one Kafka/PostgreSQL node, and one separate load-generator node. Provider-neutral scripts operate on an approved inventory of pre-provisioned Linux hosts; they do not pretend to provision every cloud provider through one abstraction.

**Blockers:** 15

**Lane / worktree slug:** `17-multi-node-test-env`

**File / folder ownership:**
- Create: `infrastructure/test-env/setup.sh` — bootstrap script
- Create: `infrastructure/test-env/teardown.sh` — destroy script
- Create: `infrastructure/test-env/README.md` — how to provision, use, and destroy

**Implementation constraints:**
- Use plain shell/Compose plus an inventory file for already-provisioned hosts. Provider purchase/provision/deletion is selected and explicitly approved at execution time.
- Application nodes 1 and 2 each run replicas of the three Spring services behind the test load balancer.
- The infrastructure node runs Kafka and PostgreSQL.
- The load-generator node runs k6 and stores raw artifacts; it is not shared with application or data workloads.
- All nodes must be on the same private network (or have low-latency connectivity).
- The environment must support scaling application nodes horizontally for bottleneck identification.
- Teardown removes containers/test data from the inventory. Provider VM deletion is a separate, explicitly approved provider operation.

**Acceptance criteria:**
- `bash infrastructure/test-env/setup.sh --inventory <file>` configures the approved hosts and prints connection details.
- `bash infrastructure/test-env/teardown.sh --inventory <file>` removes the deployed test stack and exits 0.
- Two application nodes are active and receive traffic during the test.
- Application health check succeeds from the load generator node.
- The environment is fully containerized — no application code runs directly on hosts.

**Smallest verification:**
```bash
bash infrastructure/test-env/validate.sh --inventory inventory.example.yml
```

**Suggested commit:** `feat(test): add temporary multi-node test environment infrastructure`

---

### Ticket 18: Capture the untuned baseline

**Purpose / delivery:** Run the load harness against the multi-node test environment with zero tuning. Measure HTTP throughput, event throughput, latency, and freshness. Publish the baseline report. No code changes.

**Blockers:** 16, 17

**Lane / worktree slug:** `18-untuned-baseline`

**File / folder ownership:**
- Retain raw baseline output as an external/CI artifact (Git-ignored).
- Create: `load-testing/reports/baseline-summary.md` — human-readable summary with artifact link/checksum
- No production code changes.

**Implementation constraints:**
- Application runs with default configuration (no tuning parameters changed).
- Multi-node environment from Ticket 17.
- Use a fixed seeded dataset and warm-up period, then run each scenario three times and report the median plus range.
- Record: CPU%, memory%, disk IO, network IO on each node during the test.
- Record: outbox poller cycle time, consumer processing time per event.
- Record: Kafka producer and consumer metrics (throughput, request latency).

**Acceptance criteria:**
- Baseline report exists with all required metrics.
- Any tuning in Ticket 19 is measured against this baseline.
- The summary links to raw artifacts and records their checksums, scenario configuration, and tested commit SHA.

**Smallest verification:**
```bash
test -f load-testing/reports/baseline-summary.md
```

**Suggested commit:** `test: capture untuned load test baseline`

---

### Ticket 19: Apply only measured tuning

**Purpose / delivery:** Select the single highest-impact measured bottleneck from Ticket 18, apply one minimum change, and rerun the same scenario. This keeps the ticket inside one fresh context and one concern per commit.

**Blockers:** 18

**Lane / worktree slug:** `19-measured-tuning`

**File / folder ownership (depends on identified bottlenecks):**
- Candidate: `shared/common/src/main/java/.../outbox/OutboxPoller.java` — poller batch size, poll interval
- Candidate: each service's `application.yml` — connection pool sizes, thread pool sizes
- Candidate: Kafka configuration — partition count, producer batching, listener concurrency
- Candidate: PostgreSQL configuration — `max_connections`, `shared_buffers`, `work_mem`

**Implementation constraints:**
- The change is preceded by a directional, measurable hypothesis naming the metric and cause; no invented percentage is required.
- Re-run the identical baseline scenario and record the delta. Revert regressions.
- No speculative tuning. Every change must be backed by baseline data.
- Acceptable changes: poller batch size, poll interval, connection pool sizes, Kafka producer/consumer config, DB config, JVM heap (but only with measured evidence).
- Unacceptable changes: architectural rewrites, new caching layers, new infrastructure components.

**Acceptance criteria:**
- Exactly one bounded measure → change → measure iteration and its delta are documented.
- If target is reached, publish the final report.
- If target is not reached, publish the remaining bottleneck and next decision; Gate 20 stays blocked rather than expanding this ticket.

**Smallest verification:**
```bash
bash load-testing/rerun-baseline.sh --same-scenario --artifact-dir "$ARTIFACT_DIR"
```

**Suggested commit:** `perf: apply measured tuning based on baseline analysis`

---

### Ticket 20: Prove the regional spike target

**Purpose / delivery:** Orchestrator-only ticket. Execute the Gate 20 evidence path. Gate 20 is the end of Month 3. Route-aware Cruceta starts only if Gate 20 passes. Run final tuned load test. Capture evidence. Destroy the multi-node test environment. Publish the throughput report. If target not met, document the gap and the next bottleneck.

**Blockers:** 19

**Lane / worktree slug:** `20-throughput-gate`

**File / folder ownership:**
- Create: `load-testing/reports/final-throughput-report.md` — publish evidence
- Modify: `docs/functional-map.md` — update throughput capabilities
- Prepare the teardown evidence; execute environment teardown only after explicit approval.

**Implementation constraints:**
- This ticket adds zero new functionality.
- If target is met: mark throughput gate as PASSED, proceed to Month 4 planning. If not met: Month 4 is blocked until a new decision is made — the gap is a blocker, not an automatic unlock.
- Preserve the target. If not met, document the bottleneck and the new decision needed.
- The final evidence must be reproducible from the `load-testing/` directory.

**Acceptance criteria:** See Gate 20 evidence path above.

**Smallest verification:**
```bash
bash load-testing/run-gate-20.sh --http-rate 1000 --event-rate 500 --duration 5m
# The script exits non-zero unless all rate, latency, freshness, and error thresholds pass.
# Teardown runs only after explicit approval.
```

**Suggested commit:** `gate: prove regional spike throughput target 20`

---

### Ticket 21: Add tenant-safe named route points

**Purpose / delivery:** Add an ordered route with named points to a Procesion. The route is owned by Procesion service. Repertorio maintains a local projection. Tenant isolation is enforced (same as Tickets 02/03). No GPS, coordinates, or maps.

**Blockers:** 20

**Lane / worktree slug:** `21-named-route-points`

**File / folder ownership:**
- Modify: `services/procesion-service/` — add `RoutePoint` domain model, route endpoints, outbox events
- Modify: `services/repertorio-service/` — add `KnownRoutePoint` projection
- Create: Flyway migrations — route points table (procesion), known route points table (repertorio)
- Modify: `docs/openapi.yaml` — add route endpoints (sequential Ticket 21–23 own central file one at a time; update OpenAPI first)

**Implementation constraints:**
- Route is defined by `PUT /api/procesiones/{procesionId}/route` (idempotent replace, similar to Cruceta).
- Route points have: `id`, `displayName`, `position` (integer order), optional `notes`.
- Procesion publishes `ProcesionRouteDefinedEvent` through the event envelope.
- Repertorio consumes the event and builds `KnownRoutePoint` projection.
- Tenant isolation: same `ProcesionSecurityService` pattern from Ticket 02.
- If a Cruceta already references route points, route replacement returns 409 Conflict.

**Acceptance criteria:**
- A capataz can define named route points for a procession.
- Cross-tenant route definition is denied (403).
- Repertorio learns route points within event freshness SLO (<5s).
- Invalid route (blank names, non-unique positions) → 400.

**Smallest verification:**
```bash
./gradlew :services:procesion-service:test :services:repertorio-service:test
```

**Suggested commit:** `feat(procesion,repertorio): add tenant-safe named route points`

---

### Ticket 22: Bind Cruceta items to route moments

**Purpose / delivery:** Extend `CrucetaItem` with a required `routePointId`. When defining a Cruceta, every item must reference a route point belonging to the same procession. The Cruceta read response includes the route point name and position.

**Blockers:** 21

**Lane / worktree slug:** `22-cruceta-route-binding`

**File / folder ownership:**
- Modify: `services/repertorio-service/` — `CrucetaItem` gains `routePointId`, validation, response DTO
- Modify: `docs/openapi.yaml` — update Cruceta request/response schemas (update OpenAPI first)

**Implementation constraints:**
- `CrucetaItem` already has order_index, marcha_id, notes. Add `route_point_id UUID NOT NULL`.
- Because no production data exists, document the required local reset or use an expand/backfill/contract migration so existing rows cannot make Flyway fail.
- Validation: referenced `routePointId` must exist in the known route points for that procession.
- Route points are projected from Ticket 21.
- Response DTO includes `routePointName`, `routePointPosition` for display.

**Acceptance criteria:**
- Cruceta items can reference a route point.
- Defining a Cruceta with a route point from a different procession → 400.
- Cruceta read response includes route point details.
- The Cruceta replacement is still atomic (from Ticket 03).

**Smallest verification:**
```bash
./gradlew :services:repertorio-service:test
```

**Suggested commit:** `feat(repertorio): bind cruceta items to route points`

---

### Ticket 23: Add manual current/next run-sheet progression

**Purpose / delivery:** Expose a run-sheet endpoint ordered by route progression and an idempotent command that sets the current item. The response derives the next item. No GPS or automatic progression.

**Blockers:** 22

**Lane / worktree slug:** `23-run-sheet-progression`

**File / folder ownership:**
- Modify: `services/repertorio-service/` — add run-sheet endpoint, progression state, manual advance
- Create: Flyway migration — add nullable `current_item_id` to Cruceta persistence (minimum state; no separate progression table).
- Modify: `docs/openapi.yaml` — add run-sheet endpoint (update OpenAPI first)

**Implementation constraints:**
- `GET /api/hermandades/{hid}/procesiones/{pid}/cruceta/run-sheet` returns items sorted by route position, with the current item marked.
- `PUT /api/hermandades/{hid}/procesiones/{pid}/cruceta/current-item` sets a requested Cruceta item ID and is safe to retry.
- Progression state (`currentItemId`) is persisted in the Cruceta aggregate and protected by optimistic locking.
- Replacing a Cruceta resets `currentItemId` to null so it cannot reference an item from the previous plan.
- Tenant isolation: same pattern as Tickets 02/03.
- No automatic progression, no timing, no GPS.
- Run sheet response format: ordered list with `{routePointName, marchaTitle, isCurrent, isNext}` fields.

**Acceptance criteria:**
- Run sheet returns route-ordered items with current/next indicators.
- Setting a valid item updates current/next; repeating the same command is a no-op success.
- Setting an item outside this Cruceta → 400.
- Cross-tenant run-sheet access → 403.

**Smallest verification:**
```bash
./gradlew :services:repertorio-service:test
```

**Suggested commit:** `feat(repertorio): add manual current/next run-sheet progression`

---

## Dispatch Prompt Template

When dispatching a ticket to a `@fixer`, provide the full ticket body plus the confirmed decisions, lane rules, blocker issue/commit references, and current integration base. Do not add unrelated context.

```markdown
## Dispatch: Ticket XX — {ticket title}

Read `AGENTS.md` and `docs/functional-map.md` first.

### Scope
{ticket purpose/delivery from this plan, verbatim}

### Blockers
{ticket blockers; if not resolved, halt and report}

### Lane
Worktree: `.slim/worktrees/{slug}`
Branch: `omos/{slug}` (or the branch name explicitly approved under the repository workflow)

### Files to change
{ticket file/folder ownership from this plan, verbatim}

### Constraints
{ticket implementation constraints from this plan, verbatim}

### Acceptance criteria
{ticket acceptance criteria from this plan, verbatim}

### TDD (if applicable)
{ticket TDD steps or Gherkin from this plan, verbatim}

### Output contract
1. Implement RED test → verify it fails.
2. Implement GREEN code → verify RED passes.
3. REFACTOR if needed → verify all tests still pass.
4. Run `./gradlew {affected-service}:test` → pass.
5. Run `git status && git diff --check` → clean.
6. If controller/API changed: confirm `docs/openapi.yaml` was updated FIRST.
7. Commit only when the dispatch explicitly authorizes it; use `{suggested commit message}`.
8. Return: diff/branch reference, exact verification output, and uncovered limitations.

### Prohibited
- No sub-delegation. You are the only writer.
- No scope expansion beyond the acceptance criteria.
- No modification of files outside the listed ownership.
- No Kubernetes, Debezium, Axon, new microservices, blanket caching, or speculative tuning.
- No commit amendments. One clean commit per ticket.
```

---

## Tracker Publication

Each ticket becomes one GitHub issue in `github.com/emilio-code7/semana-santa-app`.

### Per-Issue Structure

| Field | Value |
|-------|-------|
| Title | `{ticket-number}: {title}` (e.g., `02: Enforce Procesion tenant isolation`) |
| Label | `ready-for-agent` only when every blocker is closed |
| Body | Stable outcome, acceptance criteria, blockers, and link to this plan section; implementation paths remain in the plan |
| Dependencies | In the issue body, include a `### Depends on` section listing actual blocking issue references |

### Dependency Edges

Because the repository issue tracker guide does not document native dependency operations, blocking edges must be documented in the issue body:

```markdown
### Depends on
- #<actual-issue-number> — Freeze reliability contracts and evidence path
```

The orchestrator must verify that blocking issues are closed before dispatching.

### Creation Order

All 23 approved issues are publishable now in dependency order. Apply `ready-for-agent` only to the current frontier; blocked issues receive the label when their real issue-number blockers close.

1. Create Tickets 01–08 and Gate 09, replacing ticket-number placeholders with actual issue numbers as each issue is created.
2. Create Tickets 10–14 and Gate 15 with actual Month 1/Gate 09 references.
3. Create Tickets 16–19 and Gate 20 with actual Gate 15 references.
4. Create Tickets 21–23 with actual Gate 20 references.

Tickets 09, 15, 20 (gates) are created alongside the preceding batch.

Do not create issues during plan authoring. This section specifies the format for when they are created.
