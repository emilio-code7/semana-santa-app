# Cruceta-First High-Throughput Roadmap

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal**: Within four months, build the corrected domain foundation (Titulares, Pasos, Route Sections, per-Paso Crucetas) and route-aware Cruceta, then prove that the same system can sustain 1,000 HTTP req/s and 500 events/s at p95 <300 ms with event freshness <5 s on disposable multi-node infrastructure. Product first; then provable foundations.

**Architecture**: Three existing Spring Boot microservices (hermandad, procesion, repertorio), Kafka outbox eventing, Keycloak JWT auth, PostgreSQL per service, hexagonal + DDD. Month 1 builds Titulares, Pasos, Route Sections, finalization, per-Paso Crucetas with tenant safety inside the feature slice. Months 2–4 establish correctness, reliability, operability, and throughput proof.

**Tech Stack**: Java 21, Spring Boot 4.1.0, Kafka, PostgreSQL 16, Flyway, Testcontainers, Docker Compose, JUnit 5, and Gradle. Load testing uses k6 against HTTP commands that create real outbox rows. Observability uses Micrometer/Prometheus endpoints, lightweight hosted visualization, external uptime checks, and structured JSON logs.

---

## Confirmed Decisions

### Cruceta-First Ordering

Route-aware Cruceta is the immediate first development priority, not gated by throughput. Month 1 builds the corrected domain foundation (Titulares, Pasos, Route Sections, per-Paso Crucetas). Correctness, reliability, and throughput follow as the product slice expands. Tenant safety is part of every Month 1 slice — security is never deferred.

### Regional-Spike Target (Gate 26, End of Month 4)

| Metric | Target |
|--------|--------|
| HTTP requests/s | 1,000 sustained (aggregate across services) |
| Events/s through outbox → Kafka → consumer | 500 sustained |
| HTTP p95 latency | <300 ms (under load) |
| Event freshness (event occurredAt → consumer business transaction commit) | <5 s p95 |

Evidence must come from a repeatable, disposable multi-node environment — not the always-on VPS. Gate 26 verifies the expanded Cruceta workflow (multiple Pasos with independent Crucetas) after it exists.

### Per-Aggregate Ordering

- Kafka message key = `aggregateId` (UUID string) after Ticket 10 keys the `MessageSender`.
- All events for one aggregate land in the same partition, preserving broker order.
- Ticket 16 serializes outbox claims per aggregate so concurrent pollers cannot publish later aggregate events before earlier ones.
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

The load/failure evidence (Month 4) runs on a purpose-built, temporary multi-node environment — not the always-on VPS. The test environment is destroyed after evidence is collected. The deployment automation (Ticket 20) targets the VPS only; the test environment (Ticket 23) is a separate, parallel setup.

### OpenAPI Ownership

`docs/openapi.yaml` reflects AS-IS implemented endpoints only. Each implementation ticket with controller changes must update it first at execution time. No ticket pre-defines every later endpoint — one writer at a time for OpenAPI during the sequential product chain.

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
| **Rain/emergency route amendment** | Deferred to future backlog. The finalized plan is immutable for this MVP. |

---

## Month Mapping

| Month | Theme | Tickets | Gate |
|-------|-------|---------|------|
| **M1** | Correct Domain Foundation and Route-Aware Cruceta | 01–06 | Integration Gate 07 |
| **M2** | Correctness & Event Foundations | 08–14 | Integration Gate 15 |
| **M3** | Reliable Delivery, Operability, VPS | 16–20 | Integration Gate 21 |
| **M4** | Load/Failure Evidence & Tuning | 22–25 | Integration Gate 26 |

Each month is 3–4 calendar weeks. AI capacity is modeled through dependency gates, not human-hour estimates.

---

## Dependency Graph and Parallel Frontiers

```text
                    Month 1 — Domain Foundation & Cruceta
                    ┌──────────────────────────────────────┐
                    │ 01 Titulares + projection             │
                    └──────────────────┬───────────────────┘
                                       │
                    ┌──────────────────┴───────────────────┐
                    ▼                                       │
           ┌──────────────────┐                              │
           │ 02 Ordered Pasos │                              │
           └────────┬─────────┘                              │
                    ▼                                        │
           ┌──────────────────┐                              │
           │ 03 Route Sections│                              │
           │    + finalization │                             │
           └────────┬─────────┘                              │
                    ▼                                        │
           ┌──────────────────────────────┐                   │
           │ 04 Project finalized plan   │                   │
           └────────┬─────────────────────┘                   │
                    ▼                                        │
           ┌──────────────────┐                              │
           │ 05 Cruceta per   │                              │
           │ Paso + Route Sec │                              │
           └────────┬─────────┘                              │
                    ▼                                        │
           ┌──────────────────┐                              │
           │ 06 Run-sheet     │                              │
           │ progression      │                              │
           └────────┬─────────┘                              │
                    ▼                                        │
              ┌───────────┐                                  │
              │ 07 Gate   │                                  │
              │ domain    │◄─────────────────────────────────┘
              └───────────┘
                    │
           ┌────────┴────────────────────────────────┐
           │                                         │
           ▼                                         │
   ┌──────────────────────┐                          │
   │ 08 Freeze contracts  │                          │
   └───────────┬──────────┘                          │
    ┌──────────┼──────────────┐                      │
    ▼          ▼              ▼                      │
┌────────┐┌──────────┐ ┌────────────┐                │
│ 09     ││ 10       │ │ 11         │                │
│Proces. ││ Event    │ │ Optimistic │                │
│tenant  ││ envelope │ │ lock       │                │
│isolat. ││ + keyed  │ │ repair     │                │
└────────┘└─────┬────┘ └────────────┘                │
         ┌──────┼──────┐                             │
         ▼      ▼      ▼                             │
     ┌──────┐┌──────┐┌──────┐                        │
     │ 12   ││ 13   ││ 14   │                        │
     │Herm  ││P→R   ││Rep   │                        │
     │event ││event ││prodr │                        │
     │migr  ││migr  ││migr  │                        │
     └──────┘└──────┘└──────┘                        │
                    │                                 │
             ┌──────┴─────────────────────────────────┘
             │
        ┌────────────┐
        │ 15 Gate    │
        │correctness │
        └────────────┘
                │
    Month 3 — Reliable Delivery & Operability
                │
  ┌───────┬─────┼───────┬───────┐
  ▼       ▼     ▼       ▼       │
[16 Outbox][17][18 Obs.][19 VPS]│
 multi-   Consumer          │   │
 replica  idempot.          ▼   │
                    [20 Deploy] │
  └───────────────────────┼─────┘
                          ▼
                   [21 Gate]
                          │
     Month 4 — Load & Failure Evidence
                          │
             ┌────────────┼────────────┐
             ▼            ▼            ▼
    ┌────────────┐ ┌────────────┐
    │ 22 Load/   │ │ 23 Multi-  │
    │ failure    │ │ node test  │
    │ harness    │ │ env        │
    └─────┬──────┘ └──────┬─────┘
          └───────┬───────┘
                  ▼
         ┌────────────────┐
         │ 24 Untuned     │
         │ baseline       │
         └───────┬────────┘
                 ▼
         ┌────────────────┐
         │ 25 Measured    │
         │ tuning only    │
         └───────┬────────┘
                 ▼
         ┌────────────────┐
         │ 26 Gate: prove │
         │ spike target + │
         │ Cruceta flow   │
         └────────────────┘
```

### Parallel Frontiers

| Frontier | Tickets | Description |
|----------|---------|-------------|
| F1 | 01 → 02 → 03 → 04 → 05 → 06 | Month 1 product chain. Sequential. |
| F2 | 09, 10, 11 | Month 2 foundations parallel after Ticket 08 completes. |
| F3 | 12, 13, 14 | Month 2 event migration parallel after Ticket 10 completes. |
| F4 | 16, 17, 18, 19 | Month 3 reliability and operability. Independent sub-trees after Gate 15. |
| F5 | 22, 23 | Month 4 harness and test environment. Independent (code vs infrastructure) after Gate 21. |
| F6 | 16, 17, 18, 19 → 20 | 16–19 parallel; 20 blocked by 18+19. |

Tickets 07, 15, 21, 26 are integration-owner verification gates — single-threaded, no parallel work.

---

## Worktree/Lane Protocol

### Branch and Worktree Convention

Each ticket uses a dedicated Git worktree rooted at `.slim/worktrees/<slug>/`. The slug follows the pattern `{ticket-number}-{kebab-description}`.

```bash
# Create worktree (requires explicit confirmation + preflight)
git worktree add -b omos/01-titulares-projection .slim/worktrees/01-titulares-projection <approved-base>
# Work is done in that worktree, committed independently
```

### Lane Ownership Rules

| Lane | Slug pattern | Writer limit | File scope |
|------|-------------|-------------|------------|
| Product chain | `01-*` through `06-*` | One per ticket, sequential | Disjoint |
| Foundations F2 | `09-*`, `10-*`, `11-*` | One per ticket | Disjoint |
| Event migration F3 | `12-*`, `13-*`, `14-*` | One per ticket | Disjoint across services |
| Reliability F4 | `16-*`, `17-*`, `18-*`, `19-*` | One per ticket | Disjoint |
| Infrastructure | `19-*`, `20-*`, `23-*` | One per ticket | Infrastructure files |
| Integration gate | `07`, `15`, `21`, `26` | Orchestrator only | Integration tests + docs |
| Load/Evidence | `22-*`, `23-*`, `24-*`, `25-*` | One per ticket | Disjoint |

**Rules:**
- Explicit user confirmation required before any worktree/branch mutation.
- One writer per lane. Never run parallel writers on shared files.
- Central files (`docs/`, `docs/openapi.yaml`, `AGENTS.md`, `docker-compose.yml`) are owned by the integration lane (gates 07/15/21/26) or the orchestrator reconciliation commit. No ticket writer modifies these without gate coordination.
- A controller change must be preceded by an OpenAPI contract change from the designated central-file owner. Sequential Tickets 01–06 own it one at a time for their endpoints.
- Intermediate CI must stay green within each worktree. If a ticket is a cross-codebase expand/contract refactor (e.g., Ticket 10), the worktree may have transient compilation gaps across services but must be mergeable without breaking the build.

---

## Observable Integration Gates

### Gate 07 — Domain/Product Integration Gate

**Evidence path:**
1. `./gradlew build` passes on all three services.
2. Full end-to-end workflow validates: create Titulares → define ordered Pasos → define and finalize shared Route Sections → project finalized plan into Repertorio → create one Cruceta per Paso (Marchas by Route Section) → independent per-Paso run-sheet progression.
3. Tenant isolation enforced: cross-tenant reads/writes return 403 at every step.
4. Finalization is idempotent: repeated finalization command creates no new outbox row. Duplicate broker delivery may occur and must converge to the same projection.
5. Atomic Cruceta replacement: concurrent PUTs serialize or one returns 409; per-Paso Cruceta is independent.
6. Run-sheet progression: two Pasos in same Procesion progress independently; Cruceta replacement resets progression.

**Limitations:**
- Event flow uses existing at-least-once outbox without keyed/versioned/atomic upgrades.
- No multi-replica safety, no throughput proof — these follow in Months 2–4.
- Load tests cover only functional correctness, not performance.

### Gate 15 — Correctness Integrity Gate

**Evidence path:**
1. `./gradlew build` passes on all three services.
2. Tenant-isolation tests: reads and writes for a procession/Cruceta belonging to Hermandad A are 403 for a Hermandad B user. Titular and Paso tenant isolation verified.
3. Outbox-envelope tests: outbox rows carry `eventId`, `aggregateId`, `occurredAt`, `schemaVersion`.
4. Optimistic-lock tests: two successive updates succeed and a stale concurrent update fails through the real adapter.
5. Kafka adapter tests prove `aggregateId` is the record key; AWS compile/unit tests prove FIFO group=`aggregateId` and deduplication ID=`eventId` without deploying queues.
6. The temporary unkeyed sender overload and temporary default `schemaVersion` are removed; `docs/openapi.yaml` matches the implemented endpoints.
7. Month 1 event types (Titular events, ProcesionPlanFinalizedEvent, per-Paso Cruceta events) carry the full envelope.

**Limitations:**
- Tests run on a single-node Testcontainers environment. Multi-replica claim ordering is deferred to Gate 21.
- No proof of correctness under load — that is Gate 26.

### Gate 21 — Reliable Deployment Gate

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

### Gate 26 — Regional Spike Throughput Gate

**Evidence path:**
1. k6 HTTP test sustains 1,000 req/s across all service endpoints for 5 minutes with p95 <300 ms. Includes multiple Pasos with independent Crucetas, route-aware workflows.
2. The k6 event-producing HTTP scenario sustains 500 committed outbox events/s through Kafka → consumer for 5 minutes with freshness <5 s p95.
3. Measured baseline (Ticket 24) vs. tuned result (Ticket 25) comparison published in gate ticket.
4. The disposable multi-node environment used for the test is destroyed after evidence is captured.
5. All tests pass in single-node mode.

**Limitations:**
- Multi-node test is ephemeral. The always-on VPS does not reproduce multi-node throughput.
- Load test covers synthetic traffic, not real user patterns.
- Event freshness is measured from the producer-generated event `occurredAt` to consumer business transaction commit. The report also records outbox `created_at` so publisher delay can be diagnosed separately.

---

## Tickets

### Ticket 01: Add tenant-safe Titulares and project them into Procesion

**Purpose / delivery:** Add a Titular catalogue owned by Hermandad. Procesion maintains a local KnownTitular projection via outbox events. Vertical slice: create, read, list, and update Titulares with tenant isolation. No synchronous REST between services.

**Blockers:** None

**Lane / worktree slug:** `01-titulares-projection`

**File / folder ownership:**
- Modify: `services/hermandad-service/` — add Titular domain model, repository port, JPA adapter, controller, outbox events
- Modify: `services/procesion-service/` — add KnownTitular projection, event consumer
- Create: Flyway migrations — `titular` table (hermandad), `known_titular` table (procesion)
- Modify: `docs/openapi.yaml` first — add Titular create/read/list/update endpoints (deletion is not needed by this roadmap)

**Implementation constraints:**
- Titular domain: id, name, description (nullable), hermandadId, createdAt, updatedAt.
- Hermandad publishes `TitularCreatedEvent`, `TitularUpdatedEvent` through outbox.
- Procesion consumes Titular events → KnownTitular projection (id, hermandadId, name).
- Tenant isolation: `@PreAuthorize` on Hermandad endpoints verifying persisted Hermandad ownership.
- Reads require membership in owning Hermandad; writes require CAPATAZ or HERMANDAD_ADMIN.
- No synchronous REST calls between services — projection is event-driven.

**Acceptance criteria:**
- CAPATAZ or HERMANDAD_ADMIN can create and update Titulares for their own Hermandad; cross-tenant access returns 403.
- Procesion learns about Titulares through the current at-least-once outbox path. Keyed ordering, atomic deduplication, and multi-replica safety remain deferred to later tickets.
- Titular update propagates to KnownTitular projection.
- Titular endpoints enforce tenant isolation; projection data carries hermandadId for downstream authorization.

**Smallest verification:**
```bash
./gradlew :services:hermandad-service:test :services:procesion-service:test
```

**Suggested commit:** `feat(hermandad,procesion): add tenant-safe Titular catalogue and projection`

**OpenAPI-first:** Define Titular contract in `docs/openapi.yaml` before implementing controller.

**TDD checkpoints:**
1. RED: controller/service tests reject cross-tenant reads and writes and accept owning CAPATAZ/admin operations.
2. RED: a projection test shows `TitularCreatedEvent` and `TitularUpdatedEvent` update KnownTitular idempotently.
3. GREEN: implement the minimum domain, persistence, API, outbox, and projection path required by those tests.

---

### Ticket 02: Define ordered Pasos for a Procesion

**Purpose / delivery:** Add ordered Pasos to a Procesion. Each Paso has a stable ID, unique position within the Procesion, and references exactly one KnownTitular belonging to the same Hermandad. A small GET/PUT collection interface defines the draft Paso list atomically.

**Blockers:** 01

**Lane / worktree slug:** `02-ordered-pasos`

**File / folder ownership:**
- Modify: `services/procesion-service/` — add Paso domain model, repository, controller
- Create: Flyway migration — `paso` table (procesion)
- Modify: `docs/openapi.yaml` first — add `GET/PUT /api/hermandades/{hid}/procesiones/{pid}/pasos`

**Implementation constraints:**
- Paso domain: id, procesionId, position (integer order), titularId, notes (nullable).
- `PUT .../pasos` atomically replaces the draft ordered list. Existing Paso IDs in the request remain stable; omitted IDs create new Pasos and are returned in the response.
- Position must be unique within the Procesion.
- TitularId must reference a KnownTitular that belongs to the same Hermandad as the Procesion.
- Tenant isolation via persisted Hermandad ownership (Procesion → Hermandad).
- No cross-context events for individual Paso mutations — the only cross-context product event is the finalized snapshot (Ticket 03).
- Pasos exist before finalization; after finalization (Ticket 03), Pasos are immutable.

**Acceptance criteria:**
- CAPATAZ can create ordered Pasos referencing their own Hermandad's Titulares.
- Repeating the same PUT is idempotent and preserves Paso IDs and order.
- Cross-tenant Paso creation returns 403.
- Paso with a Titular from a different Hermandad returns 403 without exposing that tenant's catalogue.
- Duplicate position within same Procesion returns 400.
- List/read Pasos within Procesion context works with tenant isolation.

**Smallest verification:**
```bash
./gradlew :services:procesion-service:test
```

**Suggested commit:** `feat(procesion): add ordered Pasos with Titular reference and tenant isolation`

**TDD checkpoints:**
1. RED: prove one Paso references exactly one known Titular and positions are unique within a Procesion.
2. RED: prove cross-tenant Titular references and cross-tenant Paso access return 403.
3. GREEN: implement only the Paso model, persistence, and GET/PUT collection endpoints needed by those tests; emit no unused per-Paso events.

---

### Ticket 03: Define and finalize shared Route Sections

**Purpose / delivery:** Add ordered Route Sections to a Procesion. Shared by all Pasos. Implement the idempotent finalization command that makes Pasos and Route immutable. Finalization requires at least one Paso and one Route Section. Pasos/route immutable after finalization for this MVP.

**Blockers:** 02

**Lane / worktree slug:** `03-route-sections-finalization`

**File / folder ownership:**
- Modify: `services/procesion-service/` — add Route Section domain, repository, draft-route GET/PUT endpoints, finalization logic, outbox publication
- Create: Flyway migration — `route_section` table (procesion), add `plan_finalized_at` nullable column to procesion
- Modify: `docs/openapi.yaml` — add Route Section endpoints, finalization command

**Implementation constraints:**
- Route Section: id, procesionId, name, position (integer order), notes (nullable).
- `PUT /api/hermandades/{hid}/procesiones/{pid}/route` atomically replaces the draft Route Sections; `GET` returns them in position order. Existing IDs remain stable and omitted IDs create new Sections.
- Names are not required to be unique (outbound/return may use same street).
- Route is an ordered collection of sections, not a separate entity — sections belong to the Procesion.
- Finalization: idempotent command `POST /api/hermandades/{hid}/procesiones/{pid}/plan/finalize`.
- Guard: must have at least one Paso and one Route Section.
- After finalization: `procesion.planFinalizedAt` is set (non-null), Pasos and Route Sections reject mutation. Existing PLANNED→IN_PROGRESS→COMPLETED/CANCELLED state machine remains independent.
- Finalization publishes `ProcesionPlanFinalizedEvent` (see Ticket 04).
- No rain/emergency amendment — deferred to future backlog.

**Acceptance criteria:**
- CAPATAZ can define ordered Route Sections for a Procesion.
- Repeating the same draft-route PUT is idempotent and preserves Section IDs and order.
- Route Section names may repeat (e.g., two sections both named "Calle Sierpes").
- Finalization succeeds when at least one Paso and one Section exist.
- After finalization: Pasos and Route Sections reject modification (409 Conflict).
- Repeated finalize command is idempotent and creates no new outbox row. Broker redelivery may still duplicate the original event.
- Cross-tenant finalization returns 403.

**Smallest verification:**
```bash
./gradlew :services:procesion-service:test
```

**Suggested commit:** `feat(procesion): add Route Sections, finalization, and immutable plan`

**TDD checkpoints:**
1. RED: finalization fails without at least one Paso and one Route Section.
2. RED: repeated finalization preserves the original `planFinalizedAt` and outbox row count; later Paso/Route mutations return 409.
3. GREEN: add Route Sections and the separate plan-finalization state without changing the existing execution-status state machine.

---

### Ticket 04: Project the finalized Procesion plan into Repertorio

**Purpose / delivery:** `ProcesionPlanFinalizedEvent` is published through the outbox containing full snapshot (Hermandad, Procesion, ordered Pasos with Titular refs, ordered Route Sections). Repertorio consumes and maintains a local transactional projection. No synchronous cross-service REST calls.

**Blockers:** 03

**Lane / worktree slug:** `04-plan-projection`

**File / folder ownership:**
- Modify: `services/procesion-service/` — update `ProcesionPlanFinalizedEvent` payload, verify outbox publication
- Modify: `services/repertorio-service/` — add `KnownPaso` and `KnownRouteSection` projection entities, extend `ProcesionEventProcessor` for `ProcesionPlanFinalizedEvent`
- Create: Flyway migration (repertorio) — `known_paso`, `known_route_section` tables
- No controller changes — internal event flow only.

**Implementation constraints:**
- `ProcesionPlanFinalizedEvent` snapshot: hermandadId, procesionId, date, time, status, planFinalizedAt, pasos[{id, position, titularId}], routeSections[{id, name, position, notes}].
- Extend existing `KnownProcesion` with related `KnownPaso` and `KnownRouteSection` child tables rather than superseding it. Preserve existing created/status/deleted event handling.
- Repertorio consumer stores the plan projection transactionally — if consumer fails, Kafka retries.
- Consumer dedup uses the existing `processed_event` pattern (at-least-once, no atomic upgrade yet — Ticket 17).
- State current event reliability honestly: existing at-least-once, no keyed/versioned/atomic guarantees yet.

**Acceptance criteria:**
- Finalized plan is published as a single outbox event from Procesion.
- Repertorio stores the full plan projection (KnownProcesion extended with KnownPaso, KnownRouteSection).
- Repeated finalization command creates no new outbox row; duplicate broker delivery may occur and must converge to the same projection.
- Projection persists hermandadId; downstream authorization uses that ownership (events themselves are not authenticated requests).

**Smallest verification:**
```bash
./gradlew :services:procesion-service:test :services:repertorio-service:test
```

**Suggested commit:** `feat(procesion,repertorio): project finalized plan into Repertorio via outbox`

**TDD checkpoints:**
1. RED: a finalized-plan event creates the complete KnownProcesion/KnownPaso/KnownRouteSection projection in one transaction.
2. RED: duplicate delivery converges to the same projection; a failed child write rolls back the whole snapshot.
3. GREEN: extend the existing projection rather than introducing a second competing KnownProcesion model.

---

### Ticket 05: Migrate Cruceta to one per Paso and assign Marchas by Route Section

**Purpose / delivery:** Repertorio now owns one Cruceta per Paso (not one per Procesion). Target endpoint: `/api/hermandades/{hid}/procesiones/{pid}/pasos/{pasoId}/cruceta`. Each Cruceta Item assigns a Marcha to a Route Section with sequenceWithinSection. Tenant-safe atomic replacement.

**Blockers:** 04

**Lane / worktree slug:** `05-cruceta-per-paso`

**File / folder ownership:**
- Modify: `services/repertorio-service/` — Cruceta pivots from one-per-Procesion to one-per-Paso. CrucetaItem gains routeSectionId, sequenceWithinSection. Old `cruceta.procesion_id` UNIQUE constraint replaced.
- Create: Flyway migration — forward-only reset migration for incompatible old Cruceta rows (no production data). Never edit old migrations or add DOWN migrations.
- Modify: `docs/openapi.yaml` first — add per-Paso Cruceta endpoints.

**Implementation constraints:**
- Target endpoint: `PUT/GET .../pasos/{pasoId}/cruceta`.
- Validate Paso and Route Section from local plan projection (Ticket 04) and Marcha locally.
- Each item: routeSectionId, marchaId, sequenceWithinSection, notes (nullable).
- Full order: Route Section position then sequenceWithinSection.
- Zero or more items per Route Section; same Marcha may recur in different sections.
- Atomic replacement: requests serialize or one returns 409; partial state impossible.
- Tenant isolation via Hermandad ownership of the plan projection.
- Old unique `procesion_id` constraint on cruceta table must be replaced. Document the forward-only reset migration.
- Keep `CrucetaDefinedEvent` small because it has no current consumer: include crucetaId, procesionId, pasoId, and itemCount. Do not copy the full assignment list into the event without a consumer need.

**Acceptance criteria:**
- CAPATAZ can define one Cruceta per Paso, assigning Marchas to Route Sections.
- Multiple Pasos in same Procesion have independent Crucetas.
- Zero-or-more items per Route Section; same Marcha may recur in different sections.
- Cross-tenant Cruceta definition returns 403.
- Atomic replacement: concurrent PUTs serialize or return 409.
- Old Cruceta rows (per-Procesion) are handled by the documented reset migration.

**Smallest verification:**
```bash
./gradlew :services:repertorio-service:test
```

**Suggested commit:** `feat(repertorio): migrate Cruceta to per-Paso with Route Section assignments`

**TDD checkpoints:**
1. RED: two Pasos in one Procesion can each store a different Cruceta; a second Cruceta for the same Paso is rejected.
2. RED: validate Route Section ownership, cross-tenant access, repeated Marchas, multiple Marchas per Section, and concurrent replacement.
3. GREEN: apply the documented forward-only reset migration, then implement the minimum per-Paso aggregate and endpoint.

---

### Ticket 06: Add independent per-Paso run-sheet progression

**Purpose / delivery:** Persist currentRouteSectionId and nullable currentCrucetaItemId per Cruceta for progression tracking. Idempotent progress command and current/next run sheet. Each Paso tracks independently. Replacing Cruceta clears progression.

**Blockers:** 05

**Lane / worktree slug:** `06-run-sheet-progression`

**File / folder ownership:**
- Modify: `services/repertorio-service/` — add progression state, run-sheet endpoint, manual advance command
- Create: Flyway migration — add `current_route_section_id` and `current_cruceta_item_id` columns to cruceta
- Modify: `docs/openapi.yaml` first — add run-sheet endpoint for per-Paso progression

**Implementation constraints:**
- `GET .../pasos/{pasoId}/cruceta/run-sheet` returns items sorted by route progression, with current/next marked.
- `PUT .../pasos/{pasoId}/cruceta/current` accepts `{routeSectionId, crucetaItemId?}`. `crucetaItemId` is nullable (for silent sections with no marcha) and, when present, must belong to the requested section and Cruceta. Idempotent (safe to retry).
- If `crucetaItemId` is present, `next` is the following item in the same Section or the first item in the next non-empty Section. If it is null, `next` is the first future assigned Marcha after the current silent Section.
- Progression state is per Cruceta (per Paso). Two Pasos in same Procesion advance independently.
- Silent sections (no Marcha assigned) are representable in progression.
- Replacing a Cruceta resets progression (currentRouteSectionId and currentCrucetaItemId to null).
- Tenant isolation: same pattern as Ticket 05.

**Acceptance criteria:**
- Run sheet returns route-ordered items with current/next indicators per Paso.
- Two Pasos in same Procesion have independent progression.
- Setting a valid item updates current/next; repeating the same command is a no-op success.
- Silent sections (no Marchas) are visible in the run sheet.
- Cross-tenant run-sheet access → 403.
- Verifies full end-to-end Cruceta product flow: Titulares → Pasos → Route Sections → finalization → projection → per-Paso Cruceta → independent run sheets.

**Smallest verification:**
```bash
./gradlew :services:repertorio-service:test
```

**Suggested commit:** `feat(repertorio): add independent per-Paso run-sheet progression`

**TDD checkpoints:**
1. RED: two Pasos advance independently and retries of the same progress command are no-ops.
2. RED: silent Sections, multiple Marchas in one Section, next-Section lookup, invalid item/Section combinations, and replacement reset are covered.
3. GREEN: persist only `currentRouteSectionId` and nullable `currentCrucetaItemId`; derive the run sheet from the plan and assignments.

---

### Gate 07: Pass the domain/product integration gate

**Purpose / delivery:** Orchestrator-only ticket. Execute the Gate 07 evidence path. Merge all Month 1 worktrees. Run full test suite. Verify end-to-end Cruceta product flow. Update `docs/openapi.yaml` and `docs/functional-map.md` with implemented endpoints.

**Blockers:** 01, 02, 03, 04, 05, 06 (must all be merged)

**Lane / worktree slug:** `07-domain-product-gate`

**File / folder ownership:**
- Verify: `docs/openapi.yaml` (gate audit)
- Modify: `docs/functional-map.md` (update Titular/Paso/RouteSection/Cruceta-per-Paso status)
- Create: `docs/demo/cruceta-product-flow.sh` as the repeatable Gate 07 verification affordance
- Tests: orchestrator-level integration tests (full `docker compose` stack)

**Implementation constraints:**
- This ticket adds zero new functionality. It is a verification gate.
- Any test failure in this gate blocks Month 2 work.
- Must verify all six product tickets work end-to-end (see Gate 07 evidence path).

**Acceptance criteria:** See Gate 07 evidence path above.

**Smallest verification:**
```bash
./gradlew build
bash docs/demo/cruceta-product-flow.sh  # created by this gate
```

**Suggested commit:** `gate: pass domain/product integration gate 07`

---

### Ticket 08: Freeze reliability contracts and evidence path

**Purpose / delivery:** Define the contracts, invariants, and measurement framework that all subsequent tickets verify against. No implementation — only documentation. Integration owner also updates `docs/openapi.yaml` with tenant-isolation responses needed by Ticket 09.

**Blockers:** 07

**Lane / worktree slug:** `08-reliability-contracts`

**File / folder ownership:** Documentation and the central OpenAPI contract only; no production code:
- `docs/contracts/event-envelope.md` — versioned event envelope schema
- `docs/contracts/reliability-metrics.md` — target metrics and measurement protocol
- `docs/contracts/tenant-isolation.md` — tenant isolation invariants per service
- `docs/openapi.yaml` — add the approved tenant responses to the already-valid Path Items

Gate owner also touches `docs/openapi.yaml` to add endpoint contracts needed by Ticket 09. Tickets 10 and 11 do not change controllers.

**Implementation constraints:**
- Zero production code changes. Documentation only.
- CI already runs pinned Redocly semantic lint. Keep it green while adding the tenant contracts.
- Define the canonical `DomainEvent` shape (already has `eventId`, `occurredAt`; Ticket 10 adds `schemaVersion`).
- Contract for `eventId` generation: producer-generated `UUID.randomUUID()`. Deterministic hashing rejected because identical payloads that represent different events must not collide.
- Tenant isolation invariants cover reads and writes: reads require owning-tenant membership; writes additionally require the approved role.
- This ticket's updates to `docs/openapi.yaml` are for contracts needed by Ticket 09. Parallel writers do not edit it.

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

### Ticket 09: Enforce Procesion tenant isolation

**Purpose / delivery:** Every Procesion endpoint must enforce persisted tenant ownership. Authenticated members may read only their own Hermandad's processions; `POST`, `PATCH`, and `DELETE` additionally require `CAPATAZ` or `HERMANDAD_ADMIN` membership.

**Blockers:** 08

**Lane / worktree slug:** `09-procesion-tenant-isolation`

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
- Tenant isolation covers Pasos, Route Sections, and finalization endpoints added in Tickets 02/03.

**Acceptance criteria:**
- `POST /api/procesiones` with Hermandad A JWT creates a procession for A.
- `PATCH /api/procesiones/{id}/status` with Hermandad B JWT on Hermandad A's procession → 403.
- `DELETE /api/procesiones/{id}` with Hermandad B JWT → 403.
- `GET /api/procesiones/{id}` and `GET /api/procesiones?hermandadId=A` with only Hermandad B membership → 403.
- An unauthenticated request to any write endpoint → 401.
- A user with `MUSICIAN` role for the correct Hermandad → 403 (not authorized).
- At least one failing test existed before the fix (RED).
- Paso, Route Section, and finalization endpoints also enforce tenant isolation.

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
```

---

### Ticket 10: Expand the versioned event envelope and outbox schema

**Purpose / delivery:** Add explicit `schemaVersion` to the shared `DomainEvent` contract and transport `aggregateId` plus `eventId` through the messaging seam. Update shared outbox persistence. This is a cross-service expand/contract refactor because one shared interface has Kafka and SQS adapters in all three services.

**Blockers:** 08

**Lane / worktree slug:** `10-event-envelope`

**File / folder ownership:**
- Modify: `shared/common/src/main/java/com/repertorio/common/event/DomainEvent.java` — add `schemaVersion()` with default `return 1`.
- Modify: `shared/common/src/main/java/com/repertorio/common/messaging/MessageSender.java` — accept destination, aggregateId, eventId, and payload (or one immutable message record containing those fields). Preserve the old overload only during expand commits; remove it at Gate 15.
- Modify: `shared/common/src/main/java/com/repertorio/common/outbox/OutboxEventEntity.java` — add `eventId UUID`, `occurredAt TIMESTAMPTZ`, `schemaVersion INTEGER` columns.
- Modify: `shared/common/src/main/java/com/repertorio/common/outbox/OutboxEventPublisher.java` — populate new fields from `DomainEvent` interface.
- Modify: `shared/common/src/main/java/com/repertorio/common/outbox/OutboxPoller.java` — pass both aggregateId and eventId to `MessageSender`.
- Adapt every Kafka/SQS sender implementation. Kafka uses `aggregateId` as record key. The compile-tested FIFO SQS path maps group=`aggregateId` and deduplication ID=`eventId`; it remains undeployed until its queue is FIFO.
- Create: Flyway migration in each service — add columns to `outbox_event` table:
  - Hermandad: next available forward version (currently V9; recheck at dispatch time)
  - Procesion: next available forward version (currently V5; recheck at dispatch time)
  - Repertorio: next available forward version (currently V9; recheck at dispatch time)
- Do NOT touch `ProcessedEventEntity`, `ProcessedEventJpaRepository`, or service-specific consumers. Those belong to Tickets 12/13/14/17.
- Tests: verify new fields on all outbox rows.

**Implementation constraints:**
- `eventId` already on every domain event (producer-generated `UUID.randomUUID()`). `occurredAt` already on every event. `schemaVersion` = 1 for all current events.
- A default `schemaVersion=1` is allowed only as a temporary expand step. Tickets 12–14 implement it explicitly and Gate 15 removes the default so missing metadata cannot be hidden.
- This is an expand/contract: (a) expand adds new fields + new interface method, old code still compiles; (b) once all services are updated, old interface methods can be removed. The worktree may have transient compilation gaps across services during the expand phase.
- No unsafe default IDs/timestamps. Missing metadata fails validation. No DOWN migrations.
- Migration versions described as "next available forward version" — recheck at dispatch time because unrelated migrations may have been added.

**Acceptance criteria:**
- `DomainEvent` exposes `schemaVersion`; its temporary default is marked for mandatory removal at Gate 15.
- `MessageSender` carries destination, aggregateId, eventId, and payload. No adapter parses payload to recover transport metadata.
- Outbox rows in all three databases contain non-null `event_id`, `occurred_at`, `schema_version`.
- Kafka sender uses key=aggregateId; SQS adapter tests verify FIFO group and deduplication metadata.
- No changes to `ProcessedEventEntity` or consumer processing logic.

**Smallest verification:**
```bash
./gradlew build
```

**Suggested commit:** `feat(shared): add schemaVersion to DomainEvent, keyed MessageSender, expand outbox schema`

---

### Ticket 11: Repair optimistic-lock version propagation

**Purpose / delivery:** All aggregate JPA entities already have `@Version` annotations. The remaining gap is correct version propagation and mapping through repository adapters — especially in Procesion and Repertorio services. Tests must prove two successive updates succeed and stale concurrent updates fail through real adapters.

**Blockers:** 08

**Lane / worktree slug:** `11-optimistic-lock`

**File / folder ownership:**
- Verify: all aggregate JPA entities have properly typed `@Version` fields (already: `HermandadEntity`, `HermandadMemberEntity`, `ProcesionEntity`, `MarchaEntity`, `CrucetaEntity`, `CrucetaItemEntity`; verify new TitularEntity, PasoEntity, RouteSectionEntity).
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

---

### Ticket 12: Migrate Hermandad event flow

**Purpose / delivery:** Update Hermandad's event publishing and consumption to use the new versioned event envelope. Consumer dedup uses the producer-generated `eventId` from the envelope. Includes Titular events published in Ticket 01.

**Blockers:** 10

**Lane / worktree slug:** `12-hermandad-event-flow`

**File / folder ownership:**
- Modify: `services/hermandad-service/src/main/java/com/repertorio/hermandad/application/event/MemberAddedListener.java`
- Modify: `services/hermandad-service/src/main/java/com/repertorio/hermandad/adapter/inbound/kafka/IdempotentEventConsumer.java` — migrate dedup key if needed
- Modify: the Procesion KnownTitular consumer introduced by Ticket 01 so Titular projection consumption uses producer eventId and the explicit envelope
- Modify: Titular domain events to implement `schemaVersion` explicitly.
- Tests: update Hermandad consumer tests and KnownTitular consumer tests to verify dedup by eventId.

**Implementation constraints:**
- The `IdempotentEventConsumer` must use `eventId` from the consumed event envelope for dedup.
- The `MemberAddedListener` (in-process Spring event consumer) receives events already carrying the new envelope.
- Existing tests must pass without modification except for the dedup key change.
- Keep `processed_event` table structure for now. Ticket 17 owns the atomicity upgrade and `(consumer_name, event_id)` uniqueness.

**Acceptance criteria:**
- `IdempotentEventConsumer` deduplicates by `eventId`.
- Two identical payloads with different `eventId` values are both processed.
- Same `eventId` delivered twice → second is skipped.
- KnownTitular projection consumption uses the producer eventId and remains idempotent.
- Hermandad event records (including Titular events) implement `schemaVersion` explicitly so Gate 15 can remove the temporary default.

**Smallest verification:**
```bash
./gradlew :services:hermandad-service:test
```

**Suggested commit:** `refactor(hermandad): migrate event flow to versioned envelope`

---

### Ticket 13: Migrate Procesion-to-Repertorio flow

**Purpose / delivery:** Complete the ordered Procesion→Repertorio projection flow: `ProcesionPlanFinalizedEvent` joins existing created/status-changed/deleted events using the explicit envelope. Failures reach Kafka retry; deletion removes derived projections.

**Blockers:** 10

**Lane / worktree slug:** `13-procesion-repertorio-flow`

**File / folder ownership:**
- Modify: `services/procesion-service/src/main/java/com/repertorio/procesion/adapter/outbound/events/DomainEventPublisherAdapter.java`
- Modify: `services/repertorio-service/src/main/java/com/repertorio/marcha/adapter/inbound/kafka/ProcesionEventConsumer.java`
- Modify: `services/repertorio-service/src/main/java/com/repertorio/marcha/application/event/ProcesionEventProcessor.java`
- Modify: KnownPaso, KnownRouteSection projection entities (from Ticket 04) and event processor to use versioned envelope. Existing `ProcesionCreatedEvent`/`ProcesionStatusChangedEvent`/`ProcesionDeletedEvent` handling extends to include plan-finalization projection via `ProcesionPlanFinalizedEvent`.
- Tests: update `ProcesionEventConsumerTest` to verify dedup by eventId and handle plan projection alongside existing events.

**Implementation constraints:**
- Add `ProcesionDeletedEvent`, `ProcesionPlanFinalizedEvent`; all events explicitly implement schemaVersion and use the keyed transport from Ticket 10.
- Parse/switch on explicit `eventType`; do not infer event type from payload-field presence.
- Consumer dedup uses producer-generated `eventId`.
- Consumer exceptions propagate so Kafka retries; no catch-and-ack loss.
- Projection for Pasos and Route Sections is stored as part of the plan snapshot, with cleanup on deletion.

**Acceptance criteria:**
- `ProcesionPlanFinalizedEvent` is consumed and stored as full plan projection (Pasos, Route Sections).
- `ProcesionEventConsumer` deduplicates by producer-generated `eventId`.
- Two events with different ids and same payload are both processed.
- Same `eventId` delivered twice → consumer skips the second.
- Plan deletion removes all related projections (Pasos, Route Sections, Crucetas).

**Smallest verification:**
```bash
./gradlew :services:procesion-service:test :services:repertorio-service:test
```

**Suggested commit:** `refactor(procesion,repertorio): migrate cross-service event flow to versioned envelope including PlanFinalizedEvent`

---

### Ticket 14: Migrate Repertorio producer flow

**Purpose / delivery:** Update Repertorio's outbox publishing to use the versioned event envelope for all produced events (`MarchaAddedEvent`, `MarchaRemovedEvent`, per-Paso `CrucetaDefinedEvent`).

**Blockers:** 10

**Lane / worktree slug:** `14-repertorio-producer-flow`

**File / folder ownership:**
- Modify: `services/repertorio-service/src/main/java/com/repertorio/marcha/adapter/outbound/events/DomainEventPublisherAdapter.java`
- Tests: update `MarchaServiceTest` and `CrucetaServiceTest` to assert eventId presence and new per-Paso cruceta event shape.

**Implementation constraints:**
- All Repertorio domain events implement `DomainEvent` with `eventId`, `occurredAt`, `aggregateId`, `eventType`. Verify `schemaVersion` from Ticket 10 is propagated.
- Per-Paso `CrucetaDefinedEvent` includes crucetaId, procesionId, pasoId, and itemCount. Full assignment details stay inside the Cruceta aggregate until a real consumer requires them.
- `OutboxEventPublisher` persists the new fields from the envelope; the poller only claims, sends, and marks rows.
- No consumer exists for `marcha-events` — this ticket only ensures the producer side is correct.

**Acceptance criteria:**
- `MarchaAddedEvent`, `MarchaRemovedEvent`, per-Paso `CrucetaDefinedEvent` all carry `eventId`, `occurredAt`, `aggregateId`, `eventType`, `schemaVersion`.
- Outbox rows produced by Repertorio contain the new columns.

**Smallest verification:**
```bash
./gradlew :services:repertorio-service:test
```

**Suggested commit:** `refactor(repertorio): migrate producer events to versioned envelope including per-Paso Cruceta events`

---

### Gate 15: Pass the correctness integration gate

**Purpose / delivery:** Orchestrator-only ticket. Execute the Gate 15 evidence path. Merge all Month 2 worktrees. Run full test suite. Remove deprecated unkeyed sender overload and temporary default schemaVersion. Update docs.

**Blockers:** 09, 10, 11, 12, 13, 14 (must all be merged)

**Lane / worktree slug:** `15-correctness-gate`

**File / folder ownership:**
- Verify: `docs/openapi.yaml` (gate audit)
- Modify: `docs/functional-map.md` (update tenant isolation and envelope status)
- Tests: orchestrator-level integration tests (full `docker compose` stack)

**Implementation constraints:**
- This ticket adds zero new functionality. It is a verification gate.
- Any test failure in this gate blocks Month 3 work.
- If a test depends on a Month 3 change (e.g., multi-replica outbox), skip it and log the gap — do not expand scope.
- Remove the deprecated unkeyed sender overload and temporary default schemaVersion before the gate can pass.

**Acceptance criteria:** See Gate 15 evidence path above.

**Smallest verification:**
```bash
./gradlew build
```

**Suggested commit:** `gate: pass correctness integration gate 15`

---

### Ticket 16: Make outbox polling multi-replica safe

**Purpose / delivery:** Ensure that running two or more replicas of the same service does not cause duplicate outbox message publication beyond the inherent at-least-once contract (crash after publish before mark can duplicate; consumer idempotency absorbs it). Use `SELECT ... FOR UPDATE SKIP LOCKED` with claim tokens, bounded retries, and proper indexing.

**Blockers:** 15

**Lane / worktree slug:** `16-outbox-multi-replica`

**File / folder ownership:**
- Modify: `shared/common/src/main/java/com/repertorio/common/outbox/OutboxPoller.java` — claim/lease pattern
- Modify: `shared/common/src/main/java/com/repertorio/common/outbox/OutboxEventEntity.java` — add claim + retry columns
- Modify: `shared/common/src/main/java/com/repertorio/common/outbox/OutboxEventJpaRepository.java` — add locking query
- Create: Flyway migration (all three services) — add columns: `claimed_by VARCHAR(100)`, `claimed_at TIMESTAMPTZ`, `retry_count INTEGER DEFAULT 0`, `next_attempt_at TIMESTAMPTZ`, `last_error TEXT`, `terminal BOOLEAN DEFAULT FALSE`. Add an eligible-row index and an aggregate-order index covering `aggregate_id`, `processed`, and `created_at`.
- Tests: `OutboxPollerMultiReplicaTest.java` — integration test with two poller instances verifying disjoint row/aggregate claims, ordering, expiration, retries, and terminal state. Consumer absorption is verified at Gate 21 after Ticket 17 merges.

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

---

### Ticket 17: Make consumer idempotency atomic

**Purpose / delivery:** Ensure consumer-side dedup is transactional. The current check-then-insert pattern has a race: two consumer instances could both check, find no record, and both process the same event. Fix with unique upsert in the same DB transaction as the business mutation.

**Blockers:** 15

**Lane / worktree slug:** `17-consumer-idempotency`

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
- Stable eventId migration belongs in Tickets 12/13; this ticket owns atomicity and `(consumer_name, event_id)` uniqueness.

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

### Ticket 18: Add minimum operational visibility

**Purpose / delivery:** Expose HTTP rate/latency/errors, outbox backlog/oldest age, publish failures/latency, Kafka lag/retries, and event freshness through separate observability adapters and native framework instrumentation. Add structured JSON logging without editing Ticket 16 or Ticket 17 implementation files.

**Blockers:** 15

**Lane / worktree slug:** `18-operational-visibility`

**File / folder ownership:**
- Create: new observability packages in `shared/common/src/main/java/com/repertorio/common/observability/` — repository-backed `processed = FALSE` backlog and oldest-age gauges that work before Ticket 16 merges, plus separate sender/listener observations. Terminal/retry breakdown is integrated at Gate 21 after the claim schema exists.
- Modify: each service's `build.gradle.kts` — ensure `micrometer-registry-prometheus` is a runtime dependency.
- Modify: each service's `application.yml` — expose health/prometheus and enable Spring Boot's native structured logging after confirming the Spring Boot 4.1 property from official docs.
- Create: separate `MeterBinder`, Kafka listener/record observation, and `MessageSender` decorator/configuration classes; do not edit consumer processors or poller claim code.
- Tests: focused Actuator/Micrometer tests assert metric names, tags, values, and freshness timing.
- Do NOT edit any Ticket 16 (outbox poller internals) or Ticket 17 (consumer processor/repository) implementation files. Observability hooks go into separate wrapper/decoration classes.

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
- No edits to Ticket 16's OutboxPoller claim/lease logic or Ticket 17's consumer processor/repository files.

**Smallest verification:**
```bash
./gradlew :services:hermandad-service:test :services:procesion-service:test :services:repertorio-service:test --tests "*Observability*"
```

**Suggested commit:** `feat(shared): add outbox and consumer Prometheus metrics with structured logging`

---

### Ticket 19: Build the provider-neutral VPS runtime

**Purpose / delivery:** Set up the single always-on VPS with Docker Compose, database-consistent pg_dump backups off-host, and nginx reverse proxy with TLS. The VPS runs a dedicated reduced compose file, not the full development stack.

**Blockers:** 15

**Lane / worktree slug:** `19-vps-runtime`

**File / folder ownership:**
- Create: `infrastructure/vps/docker-compose.vps.yml` — dedicated VPS compose with nginx/TLS; three services; one PostgreSQL 16 engine with separate databases/users for each service and Keycloak; Kafka KRaft; Keycloak with its own DB; and Redis for the existing Hermandad cache. Omit ZooKeeper, Eureka, Gateway, Kafka UI, stubs, and heavy local observability containers.
- Create: `infrastructure/vps/backup.sh` — database-consistent pg_dump/pg_dumpall for every application DB and Keycloak, streamed/copied off-host. Never archive/rsync live Postgres volumes.
- Create: `infrastructure/vps/restore.sh` — restore into clean databases, then verify migrations and service readiness.
- Create: `infrastructure/vps/verify-deployment.sh` — assert Compose configuration and each service readiness endpoint without static nginx success.
- Create: `infrastructure/vps/.env.template` — all required environment variables.
- This ticket owns compose/runtime/scripts only — never workflow files (those are Ticket 20).

**Implementation constraints:**
- No hardcoded secrets. All configuration is environment variables.
- A restricted deployment user owns the application directory and Docker access; routine deployment does not use root.
- Services use immutable GHCR images built from the same Dockerfiles as local development. Use named persistent volumes.
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

### Ticket 20: Automate immutable deployment, rollback, and backup restore

**Purpose / delivery:** Create a GitHub Actions workflow that builds Docker images, pushes to GHCR, deploys to the VPS via SSH, and includes automated rollback on health-check failure. This ticket owns workflow files only. Build/push/deploy uses immutable SHA tags only — no `latest`. Compose requires explicit image SHA/tag; rollback records the prior deployed SHA.

**Blockers:** 18, 19

**Lane / worktree slug:** `20-deploy-automation`

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

### Gate 21: Pass the reliable-deployment gate

**Purpose / delivery:** Orchestrator-only ticket. Execute the Gate 21 evidence path. Merge all Month 3 worktrees. Run full test suite. Verify VPS deployment, rollback, and backup restore end-to-end. Update docs.

**Blockers:** 16, 17, 18, 19, 20 (must all be merged)

**Lane / worktree slug:** `21-reliable-deploy-gate`

**File / folder ownership:**
- Verify: `docs/functional-map.md`, `docs/openapi.yaml`
- Verify: VPS deployment (manual run of deploy workflow or SSH)
- Verify: backup and restore scripts (manual test)

**Implementation constraints:**
- This ticket adds zero new functionality.
- A multi-replica outbox test must pass (two JVM instances poll same DB; disjoint claims verified).
- Consumer idempotency atomicity test must pass.
- Metrics must be visible on the VPS deployment.

**Acceptance criteria:** See Gate 21 evidence path above.

**Smallest verification:**
```bash
./gradlew build
bash infrastructure/vps/verify-deployment.sh --local-compose
```

**Suggested commit:** `gate: pass reliable deployment gate 21`

---

### Ticket 22: Build the repeatable load/failure harness

**Purpose / delivery:** Create a repeatable k6 workload that invokes real HTTP commands which commit business state and outbox rows, then measures the resulting Kafka/consumer path. Direct Kafka production is forbidden because it would bypass the behavior being proved.

**Blockers:** 21

**Lane / worktree slug:** `22-load-failure-harness`

**File / folder ownership:**
- Create: `load-testing/k6/` — directory with k6 scripts
  - `k6/hermandad-api.js` — Hermandad CRUD load test (including Titulares)
  - `k6/procesion-api.js` — Procesion CRUD + Pasos + Route Sections + finalization
  - `k6/repertorio-api.js` — Marcha + per-Paso Cruceta load test
  - `k6/mixed-workflow.js` — realistic cross-service workflow mix including multiple Pasos with independent Crucetas
  - `k6/helpers.js` — shared token acquisition and base URLs; never sign JWTs locally
- Add a dedicated k6 constant-arrival-rate scenario for 500 event-producing HTTP commands/s.
- Create: `load-testing/README.md` — how to run each test, expected metrics, and report format

**Implementation constraints:**
- k6 scripts must be self-contained (no external dependencies beyond k6 binary).
- Event workloads call application endpoints only; they never write Kafka or outbox tables directly.
- Tests accept base URLs, duration/rate, and either a supplied token or token-endpoint credentials via environment variables. No credentials enter source or reports.
- Raw high-volume results are ignored by Git and retained as CI/external artifacts. The repository stores compact summaries, scenario configuration, checksums, and artifact links.
- The harness must be runnable against both the local Docker Compose environment and the multi-node test environment (Ticket 23).
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

### Ticket 23: Build the temporary multi-node test environment

**Purpose / delivery:** Configure a disposable four-role environment: two independent application nodes, one Kafka/PostgreSQL node, and one separate load-generator node. Provider-neutral scripts operate on an approved inventory of pre-provisioned Linux hosts; they do not pretend to provision every cloud provider through one abstraction.

**Blockers:** 21

**Lane / worktree slug:** `23-multi-node-test-env`

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

### Ticket 24: Capture the untuned baseline

**Purpose / delivery:** Run the load harness against the multi-node test environment with zero tuning. Measure HTTP throughput, event throughput, latency, and freshness. Publish the baseline report. No code changes.

**Blockers:** 22, 23

**Lane / worktree slug:** `24-untuned-baseline`

**File / folder ownership:**
- Retain raw baseline output as an external/CI artifact (Git-ignored).
- Create: `load-testing/reports/baseline-summary.md` — human-readable summary with artifact link/checksum
- No production code changes.

**Implementation constraints:**
- Application runs with default configuration (no tuning parameters changed).
- Multi-node environment from Ticket 23.
- Use a fixed seeded dataset and warm-up period, then run each scenario three times and report the median plus range.
- Record: CPU%, memory%, disk IO, network IO on each node during the test.
- Record: outbox poller cycle time, consumer processing time per event.
- Record: Kafka producer and consumer metrics (throughput, request latency).

**Acceptance criteria:**
- Baseline report exists with all required metrics.
- Any tuning in Ticket 25 is measured against this baseline.
- The summary links to raw artifacts and records their checksums, scenario configuration, and tested commit SHA.

**Smallest verification:**
```bash
test -f load-testing/reports/baseline-summary.md
```

**Suggested commit:** `test: capture untuned load test baseline`

---

### Ticket 25: Apply only measured tuning

**Purpose / delivery:** Select the single highest-impact measured bottleneck from Ticket 24, apply one minimum change, and rerun the same scenario. This keeps the ticket inside one fresh context and one concern per commit.

**Blockers:** 24

**Lane / worktree slug:** `25-measured-tuning`

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
- If target is not reached, publish the remaining bottleneck and next decision; Gate 26 stays blocked rather than expanding this ticket.

**Smallest verification:**
```bash
bash load-testing/rerun-baseline.sh --same-scenario --artifact-dir "$ARTIFACT_DIR"
```

**Suggested commit:** `perf: apply measured tuning based on baseline analysis`

---

### Gate 26: Prove the regional spike target

**Purpose / delivery:** Orchestrator-only ticket. Execute the Gate 26 evidence path. Gate 26 is the end of Month 4. Run final tuned load test including the expanded Cruceta route-aware workflow (multiple Pasos with independent Crucetas). Capture evidence. Destroy the multi-node test environment. Publish the throughput report. If target not met, document the gap and the next bottleneck.

**Blockers:** 25

**Lane / worktree slug:** `26-throughput-gate`

**File / folder ownership:**
- Create: `load-testing/reports/final-throughput-report.md` — publish evidence including multi-Paso Cruceta workflow throughput
- Modify: `docs/functional-map.md` — update throughput capabilities
- Prepare the teardown evidence; execute environment teardown only after explicit approval.

**Implementation constraints:**
- This ticket adds zero new functionality.
- If target is met: mark throughput gate as PASSED. If not met: the gap is documented as a blocker for further scaling — Cruceta already exists and works regardless.
- Preserve the target. If not met, document the bottleneck and the next decision needed.
- The final evidence must be reproducible from the `load-testing/` directory.
- Evidence must demonstrate the expanded Cruceta workflow under load (multiple Pasos with independent Crucetas, Route Section assignments, run-sheet progression).

**Acceptance criteria:** See Gate 26 evidence path above.

**Smallest verification:**
```bash
bash load-testing/run-gate-26.sh --http-rate 1000 --event-rate 500 --duration 5m
# The script exits non-zero unless all rate, latency, freshness, and error thresholds pass.
# Teardown runs only after explicit approval.
```

**Suggested commit:** `gate: prove regional spike throughput target 26`

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
| Title | `{ticket-number}: {title}` (e.g., `02: Define ordered Pasos for a Procesion`) |
| Label | `ready-for-agent` only when every blocker is closed |
| Body | Stable outcome, acceptance criteria, blockers, and link to this plan section; implementation paths remain in the plan |
| Dependencies | In the issue body, include a `### Depends on` section listing actual blocking issue references |

### Dependency Edges

Because the repository issue tracker guide does not document native dependency operations, blocking edges must be documented in the issue body:

```markdown
### Depends on
- #<actual-issue-number> — Add tenant-safe Titulares and project them into Procesion
```

The orchestrator must verify that blocking issues are closed before dispatching.

### Creation Order

All 26 approved issues are publishable now in dependency order. Apply `ready-for-agent` only to the current frontier; blocked issues receive the label when their real issue-number blockers close.

1. Create Tickets 01–06 and Gate 07 (Month 1 product chain) with their sequential blocker references.
2. Create Tickets 08–14 and Gate 15 with actual Month 1/Gate 07 references.
3. Create Tickets 16–20 and Gate 21 with actual Gate 15 references.
4. Create Tickets 22–25 and Gate 26 with actual Gate 21 references.

Gates 07, 15, 21, 26 are created alongside the preceding batch.

Do not create issues during plan authoring. This section specifies the format for when they are created.
