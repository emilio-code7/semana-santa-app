# Product & Architecture Roadmap

## Purpose

This roadmap turns Repertorio from a collection of well-structured services into a demonstrable distributed system. It prioritizes depth over adding more services: one coherent business flow, reliable events, observable failure behaviour, and a portfolio-ready developer experience.

## Guiding Principles

- Finish and harden the existing bounded contexts before expanding the system.
- Every phase must produce a demoable outcome and an interview story.
- Preserve at-least-once delivery; make business processing idempotent.
- Build vertical slices and tests first, following the development workflow.
- **Correctness and reliability precede all feature expansion.** The route-aware Cruceta is gated behind proven throughput and failure recovery.

## Product North Star

Repertorio is a procession music-planning product, not a general-purpose Hermandad-management system. Its core value is helping a Hermandad decide and execute **which marcha is played at which meaningful moment of a procession route**.

The current `Cruceta` is the intentional first step: an ordered, procession-specific musical plan. Before making it route-aware, the event system must be provably correct and reliable under load. Route awareness is added in Phase 5, after the throughput and failure-recovery gate passes.

## Roadmap at a Glance

| Phase | Outcome | Primary interview theme |
|-------|---------|------------------------|
| 1. Operational procession | ✅ One end-to-end business workflow | Service boundaries and eventual consistency |
| 2. Correctness & Event Foundations | Tenant isolation, versioned events, optimistic locking | Contract-driven correctness and multi-tenancy |
| 3. Reliable Event Delivery & Operability | Multi-replica-safe outbox, atomic dedup, Prometheus metrics | Operational maturity without orchestration |
| 4. Load/Failure Evidence | Measured 1,000 req/s and 500 events/s throughput proof | Data-driven performance engineering |
| 5. Route-Aware Cruceta | Music tied to meaningful route moments | Domain modelling and user-centred MVP scope |
| 6. Portfolio Readiness | A reviewer can run and understand the project quickly | Technical communication and trade-offs |

**Detailed four-month roadmap:** [`docs/plans/2026-07-28-reliability-first-high-throughput-roadmap.md`](plans/2026-07-28-reliability-first-high-throughput-roadmap.md)

---

## Phase 1 — Operational Procession ✅

### Goal

Create one complete, meaningful workflow across the existing Hermandad, Procesion, and Repertorio services. Do not add Tracking or Notification as prerequisites.

### Proposed workflow

1. A capataz creates a procession for a hermandad.
2. A cruceta is defined for that procession using marchas from the repertoire catalogue.
3. The procession transitions through its valid state machine to `ACTIVE`.
4. Each state change is published through the outbox and can be observed in Kafka UI.

### Acceptance criteria

- ✅ A documented demo uses only the API gateway and seeded Keycloak users.
- ✅ The demo crosses at least two services and one Kafka topic.
- ⚠️ Authorization is present, but the Hermandad in the path is not yet verified against the persisted procession; Phase 2 closes this tenant-binding gap.
- ✅ The system handles an unavailable referenced resource with a clear, intentional response or documented eventual-consistency rule.
- ✅ Unit, controller, and at least one integration test cover the happy path and a meaningful failure path.

### Key decisions to document

- ✅ **Cross-service validation**: Kafka-based eventual consistency (repertorio consumes `procesion-events` to maintain local `KnownProcesion` cache). Synchronous REST lookup was rejected because it couples repertorio to procesion availability.
- What consistency users see when a marcha or cruceta changes.
- Why this workflow is the smallest useful MVP.

---

## Phase 2 — Correctness & Event Foundations

### Goal

Establish the correctness invariants that all subsequent phases depend on: tenant isolation, a versioned event envelope, and optimistic-lock safety. Reads require owning-tenant membership; writes additionally require the approved role and persisted ownership verification.

### Detailed plan

See the Month 1 section of [`docs/plans/2026-07-28-reliability-first-high-throughput-roadmap.md`](plans/2026-07-28-reliability-first-high-throughput-roadmap.md) (Tickets 01–09).

### Work items

- Freeze reliability contracts: event envelope schema, reliability metrics protocol, tenant isolation invariants.
- Enforce Procesion tenant isolation: `@PreAuthorize` guard verifying persisted Hermandad ownership.
- Enforce Cruceta tenant isolation and safe (atomic) replacement.
- Expand the event envelope with `schemaVersion` (shared `DomainEvent` already has `eventId` and `occurredAt`), key the `MessageSender` interface, update outbox schema across all services.
- Migrate Hermandad, Procesion→Repertorio, and Repertorio producer flows to the extended envelope.
- Repair `@Version` propagation through repository adapters (all aggregate JPA entities already have the annotation). Add concurrent-write tests.
- Pass the correctness integration gate (Gate 09).

### Acceptance criteria

- A user of Hermandad A cannot read or write another Hermandad's procession or Cruceta.
- All domain events carry a producer-generated `eventId`, `occurredAt`, and `schemaVersion`.
- Consumers deduplicate on `eventId` (not payload hash).
- Concurrent writes to the same aggregate fail with an optimistic-lock exception through real adapters.
- All existing tests pass; new tenant-isolation, envelope, and optimistic-lock tests exist.
- `docs/openapi.yaml` reflects all endpoint changes.

### Interview story

"We fixed tenant isolation before adding features because a user-facing security bug is the hardest to recover from. We chose producer-generated event IDs over payload hashing so two legitimate events with identical payloads would not be incorrectly treated as duplicates."

---

## Phase 3 — Reliable Event Delivery & Operability

### Goal

Make the outbox pattern safe for multi-replica operation, make consumer idempotency atomic (eliminating the check-then-insert race), and expose operational metrics. Deploy to a provider-neutral VPS with automated immutable deployment and backup/restore procedures.

### Detailed plan

See the Month 2 section of [`docs/plans/2026-07-28-reliability-first-high-throughput-roadmap.md`](plans/2026-07-28-reliability-first-high-throughput-roadmap.md) (Tickets 10–15).

### Work items

- Make outbox polling multi-replica safe: `SELECT ... FOR UPDATE SKIP LOCKED`, leases, bounded retries, and one ordered active claim per aggregate.
- Make consumer idempotency atomic: `INSERT ON CONFLICT DO NOTHING` instead of check-then-insert.
- Add minimum operational visibility: Prometheus metrics for outbox backlog, consumer lag, error counts; structured JSON logging.
- Build the provider-neutral VPS runtime: Docker Compose, volumes, nginx reverse proxy with TLS, off-host backup.
- Automate immutable deployment, rollback, and backup restore via GitHub Actions.
- Pass the reliable-deployment gate (Gate 15).

### Acceptance criteria

- Two concurrent outbox poller instances never claim the same row simultaneously.
- Crash after publish before mark can produce a duplicate; consumer idempotency (transactional upsert) absorbs it.
- Two concurrent deliveries of the same `(consumerName, eventId)` result in at most one committed business effect; the duplicate is skipped.
- `curl /actuator/prometheus` on any service shows outbox backlog, processed count, and error count.
- VPS is reachable at public DNS; `docker compose up` starts all services; `/health` endpoints return 200 (real readiness).
- Deployment uses immutable SHA tags only; rollback via previous SHA restores the previous version.
- Off-host backup + restore (pg_dump/pg_dumpall) is documented and testable.

### Interview story

"We made the outbox poller safe for multiple replicas using PostgreSQL's `SELECT ... FOR UPDATE SKIP LOCKED`, one active claim per aggregate, and ordered claims. Delivery remains at-least-once because a crash after publish can duplicate a message. A transactional `(consumerName, eventId)` claim ensures duplicate deliveries create at most one committed business effect."

---

## Phase 4 — Load/Failure Evidence

### Goal

Prove the system can sustain 1,000 HTTP req/s and 500 events/s at p95 <300 ms with event freshness <5 s in a temporary multi-node environment. Use measured baselines and targeted tuning only — no speculative optimization.

### Detailed plan

See the Month 3 section of [`docs/plans/2026-07-28-reliability-first-high-throughput-roadmap.md`](plans/2026-07-28-reliability-first-high-throughput-roadmap.md) (Tickets 16–20).

### Work items

- Build the repeatable load/failure harness: k6 HTTP scenarios that create real outbox events without bypassing application endpoints.
- Build a temporary multi-node test environment (destroyed after evidence is collected).
- Capture the untuned baseline with zero configuration changes.
- Apply only measured tuning based on baseline bottleneck analysis (poller batch size, connection pools, Kafka producer config — no architectural rewrites).
- Prove the regional spike target or document the exact bottleneck preventing it.
- Pass the throughput gate (Gate 20).

### Acceptance criteria

- k6 HTTP test sustains 1,000 req/s across all service endpoints for 5 minutes with p95 <300 ms.
- The k6 event-producing HTTP scenario sustains 500 committed outbox events/s through Kafka → consumer for 5 minutes with freshness <5 s p95.
- Baseline and tuned results are published as a comparison report.
- The multi-node test environment is destroyed after evidence is captured.
- If target is not met: clear documentation of the bottleneck and the architectural change needed.

### Interview story

"We built a repeatable load testing harness and a disposable multi-node environment to measure throughput scientifically. Every tuning change started with a hypothesis from the baseline. We proved 1,000 req/s and 500 events/s before adding the next feature — the Cruceta route-awareness. This data-driven approach meant we never speculated about performance."

---

## Phase 5 — Route-Aware Cruceta

### Goal

Turn the ordered Cruceta into the product's core promise: a route-aware musical plan that tells the team what should play at each meaningful point in the procession. **This phase starts only after Gate 20 passes. A documented gap blocks this phase until a new decision is approved.**

### Detailed plan

See the Month 4 section of [`docs/plans/2026-07-28-reliability-first-high-throughput-roadmap.md`](plans/2026-07-28-reliability-first-high-throughput-roadmap.md) (Tickets 21–23).

### Work items

- Model an ordered route for a procession using named route points; avoid maps and GPS automation.
- Allow a `CrucetaItem` to reference a route point, while retaining its marcha and operational notes.
- Provide a read-only run sheet ordered by route progression, including the current/next marcha.
- Add a manual live-progress control for the active route point and marcha (no GPS or automatic progression).
- Tenant isolation is inherited from Phase 2 (Tickets 02/03).

### Acceptance criteria

- A capataz can define named route points for a procession.
- A Cruceta can associate each marcha with a route point.
- The application can show a clear "now / next" musical plan without GPS.
- Cross-tenant Cruceta operations are denied (inherited from Phase 2).
- All existing reliability and throughput guarantees are maintained.

### Explicitly deferred

- Coordinates, map rendering, routing-provider integration, or GPS triggers.
- Automatic selection of the current route point or marcha.
- Public sharing, tracking, push notifications, and band-facing collaboration features.

---

## Phase 6 — Portfolio Readiness

### Goal

Make the repository easy for an interviewer or reviewer to run, understand, and discuss.

### Work items

- ✅ ~~Add a root `README.md` with project purpose, architecture diagram, prerequisites, startup, seeded users, URLs, and the main demo workflow.~~ (done in Phase 1)
- Refresh `docs/audit.md` and `docs/backlog.md` so completed work and current risks are accurate.
- Create concise architecture decision records for the outbox approach, event choreography, and authorization design.
- ✅ ~~Add a repeatable demo script: successful workflow, intentional Kafka failure, recovery, and trace/metric inspection.~~ (done: `docs/demo/phase-1.sh`)
- Record three or four interview stories based on real decisions and incidents encountered while building the project.

### Acceptance criteria

- ✅ A new developer can start the stack and run the main demo from the README. (README + demo script exist)
- ✅ The README links to the dashboard, API documentation, event topics, and architecture decisions.
- Every major architectural choice includes its rationale and trade-off.

---

## Suggested Sequence

Complete the phases in order. Phase 1 establishes the musical plan. Phase 2 adds tenant isolation and a versioned event contract. Phase 3 makes event delivery reliable and the system operable. Phase 4 proves throughput and failure recovery under load. Phase 5 makes the musical plan route-aware — and is gated behind Phase 4. Phase 6 turns the implementation into a maintainable, convincing portfolio artifact.

Tracking and Notification remain deferred until Phase 4 is complete. They become useful then as consumers of an established, observable event contract, rather than additional services that dilute focus.
