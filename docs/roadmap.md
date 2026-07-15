# Product & Architecture Roadmap

## Purpose

This roadmap turns Repertorio from a collection of well-structured services into a demonstrable distributed system. It prioritizes depth over adding more services: one coherent business flow, reliable events, observable failure behaviour, and a portfolio-ready developer experience.

## Guiding Principles

- Finish and harden the existing bounded contexts before expanding the system.
- Every phase must produce a demoable outcome and an interview story.
- Preserve at-least-once delivery; make business processing idempotent.
- Build vertical slices and tests first, following the development workflow.

## Roadmap at a Glance

| Phase | Outcome | Primary interview theme |
|---|---|---|
| 1. Operational procession | ✅ One end-to-end business workflow | Service boundaries and eventual consistency |
| 2. Reliable event delivery | Explicit, recoverable event processing | Outbox, idempotency, failure recovery |
| 3. Observable system | A failure is visible from request to consumer | Logs, metrics, traces, SLO thinking |
| 4. Contract and tenancy maturity | Services evolve safely and enforce consistent access | API/event contracts and multi-tenancy |
| 5. Portfolio readiness | A reviewer can run and understand the project quickly | Technical communication and trade-offs |

---

## Phase 1 — Operational Procession

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
- ✅ Authorization is enforced using the relevant hermandad membership.
- ✅ The system handles an unavailable referenced resource with a clear, intentional response or documented eventual-consistency rule.
- ✅ Unit, controller, and at least one integration test cover the happy path and a meaningful failure path.

### Key decisions to document

- ✅ **Cross-service validation**: Kafka-based eventual consistency (repertorio consumes `procesion-events` to maintain local `KnownProcesion` cache). Synchronous REST lookup was rejected because it couples repertorio to procesion availability.
- What consistency users see when a marcha or cruceta changes.
- Why this workflow is the smallest useful MVP.

---

## Phase 2 — Reliable Event Delivery

### Goal

Make the event contract explicit and show how the system recovers from publish and consumer failures.

### Work items

- Introduce a versioned event envelope with `eventId`, `eventType`, `aggregateId`, `occurredAt`, `schemaVersion`, and `payload`.
- Generate `eventId` when the domain event is created and persist the same ID in the outbox.
- Update consumers to deduplicate on the producer-generated `eventId`, rather than a payload hash.
- Track outbox attempts, error information, and terminal failure state.
- Add bounded retry with backoff and a dead-letter topic for messages that cannot be processed.
- Define a replay procedure for dead-letter messages.

### Acceptance criteria

- Publishing the same logical event twice is safe for every consumer.
- Two legitimate events with identical payloads are not incorrectly treated as duplicates.
- A failed publication is retried and is visible to an operator.
- A permanently failing consumer message reaches a dead-letter topic with enough context for diagnosis.
- An integration test proves the outbox-to-Kafka path; a consumer test proves duplicate handling.

### Interview story

“We chose at-least-once delivery because it is practical with Kafka and database transactions. We avoided duplicate business effects through a stable event ID and consumer-side idempotency.”

---

## Phase 3 — Observable System

### Goal

Make normal operation and failure behaviour visible across HTTP, Kafka, and the outbox.

### Work items

- Propagate a correlation/trace ID from the API gateway through HTTP and Kafka headers.
- Emit structured JSON logs with service, trace ID, event ID, aggregate ID, and error details.
- Add OpenTelemetry tracing for HTTP requests, database calls, and Kafka publish/consume operations.
- Expose Prometheus metrics for request failures/latency, Kafka consumer lag, outbox backlog, retries, and dead-letter count.
- Create a minimal Grafana dashboard and one alert-like dashboard panel for a growing outbox backlog.

### Acceptance criteria

- A user request can be followed from the gateway to the resulting Kafka consumer action.
- Turning Kafka off produces a visible outbox backlog and recovery once Kafka returns.
- A short runbook explains how to diagnose a delayed event.
- The dashboard and sample trace are linked from the README.

---

## Phase 4 — Contract and Tenancy Maturity

### Goal

Allow services to evolve independently while applying the same tenancy and authorization rules everywhere.

### Work items

- Establish compatibility rules for REST and Kafka events, including `schemaVersion` handling.
- Add contract tests for the workflow's important REST and event interactions.
- Ensure gateway tenant context propagation supports all domain route patterns, not only Hermandad paths.
- State each service's tenant ownership and authorization policy in the architecture documentation.
- Keep `shared:common` restricted to cross-cutting concerns; do not introduce shared domain models.

### Acceptance criteria

- A compatible event change can be consumed by an older consumer version.
- Contract tests fail when a producer breaks a documented consumer expectation.
- Tenant and authorization behaviour is consistent for Hermandad, Procesion, and Repertorio routes.

---

## Phase 5 — Portfolio Readiness

### Goal

Make the repository easy for an interviewer or reviewer to run, understand, and discuss.

### Work items

- ✅ ~~Add a root `README.md` with project purpose, architecture diagram, prerequisites, startup, seeded users, URLs, and the main demo workflow.~~ (done in Phase 1)
- Refresh `docs/audit.md` and `docs/backlog.md` so completed Repertorio work and current risks are accurate.
- Create concise architecture decision records for the outbox approach, event choreography, and authorization design.
- ✅ ~~Add a repeatable demo script: successful workflow, intentional Kafka failure, recovery, and trace/metric inspection.~~ (done: `docs/demo/phase-1.sh`)
- Record three or four interview stories based on real decisions and incidents encountered while building the project.

### Acceptance criteria

- ✅ A new developer can start the stack and run the main demo from the README. (README + demo script exist)
- ✅ The README links to the dashboard, API documentation, event topics, and architecture decisions.
- Every major architectural choice includes its rationale and trade-off.

---

## Suggested Sequence

Complete the phases in order. Phase 1 creates the business value; Phase 2 makes it reliable; Phase 3 proves that reliability operationally. Phases 4 and 5 turn the implementation into a maintainable, convincing portfolio artifact.

Tracking and Notification should remain deferred until Phase 3 is complete. They become useful then as consumers of the established event contract, rather than additional services that dilute focus.
