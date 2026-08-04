# Architecture — Semana Santa App

## Stack

- Java 21, Spring Boot 4.x
- Gradle multi-project
- PostgreSQL, Redis, Kafka
- Keycloak (auth)
- Docker Compose (dev)

## Architecture Style

Hexagonal (ports & adapters) + DDD.

**Layers (inside a service):**

```
domain/       — Pure model, domain services, repository interfaces (ports)
application/  — Application services, port interfaces (EventPublisher, etc.)
adapter/      — Infrastructure:
  inbound/rest/     — Controllers, DTOs, exception handlers
  outbound/persistence/ — JPA repos implementing domain repository ports
  outbound/outbox/  — Outbox table + publisher + scheduler
  outbound/keycloak/ — Keycloak REST client
  config/           — @Configuration classes
```

## Services

| Service | Responsibility | DB |
|---|---|---|
| `hermandad-service` | Hermandades (brotherhoods) and their members | hermandad-db |
| `procesion-service` | Processions (MVP: CRUD + state machine) | procesion-db |
| `repertorio-service` | Marcha catalogue and Cruceta (per Paso, with run-sheet progression) | repertorio-db |
| `tracking-service` | Real-time GPS positions of processions | tracking-db |
| `notification-service` | Push notifications / alerts | notification-db |
| `api-gateway` | Spring Cloud Gateway | — |
| `discovery-server` | Eureka | — |

No shared databases. Communication via Kafka events for async flows.

## AS-IS vs TARGET Domain Context Ownership

### AS-IS (current implementation)

| Context | Owns | Notes |
|---------|------|-------|
| Hermandad | Hermandad, HermandadMember | No Titular entity |
| Procesion | Procesion (flat: id, hermandadId, date, time, status) | No Pasos, no Route Sections, no finalized plan |
| Repertorio | Marcha, Cruceta (one per Paso), KnownProcesion/KnownPaso/KnownRouteSection projection, run-sheet progression | CrucetaItem has marchaId/routeSectionId/sequenceWithinSection |

### TARGET (active roadmap)

Procesion context is expanded to own the full finalized plan. Repertorio shifts to per-Paso Crucetas.

| Context | Owns | Projected locally |
|---------|------|-------------------|
| Hermandad | Hermandad, HermandadMember, **Titular** | — |
| Procesion | **Procesion plan**, **Pasos**, **shared Route Sections**, **finalization** | KnownTitular (from Hermandad) |
| Repertorio | Marcha, **Cruceta per Paso**, **local finalized-plan projection** | KnownProcesion plan (Pasos, Route Sections, Titular refs) |

**Key rules:**
- No synchronous cross-service REST calls. Procesion publishes finalized plan via outbox; Repertorio maintains a local projection.
- The finalized Route is immutable for this MVP — rain/emergency amendment is deferred.
- Tenant isolation is enforced in every context: reads require owning-tenant membership; writes additionally require the approved role.

---

## Aggregate Design

### Hermandad and HermandadMember — Separate Aggregates

`HermandadMember` is NOT a child entity inside the `Hermandad` aggregate.

**Rationale:**
- A hermandad can have thousands of members. Loading all of them to add one is expensive.
- Role changes, membership queries, and member listing don't need the parent aggregate's state.
- The "no duplicate members" invariant is enforced at the database level via a unique constraint on `(hermandad_id, user_id)`, not by loading all members into memory.

**Implications:**
- `Hermandad` keeps its own repository. `HermandadMember` keeps its own repository.
- Operations on members go to the member repository directly, not through `Hermandad`.
- Application services coordinate cross-aggregate flows (e.g., when adding a member, validate the hermandad exists, check uniqueness via DB constraint, publish event).

### Aggregate Boundaries (General Principle)

Keep aggregates small. An aggregate should load entirely in one transaction without performance concern. When in doubt, err on the side of separate aggregates and enforce cross-aggregate invariants via:
1. Database constraints (unique, foreign keys)
2. Application service coordination
3. Eventually consistent event handlers (when strong consistency isn't required)

## Key Patterns

### Outbox Pattern (Manual)

Kafka messages are published via an outbox table (`outbox_event`) and a `@Scheduled` poller every 5s. This avoids distributed transactions. Topic naming: `{aggregate-type}-events`.

The poller processes events in `ORDER BY created_at ASC`, capped at 100 rows per cycle to limit memory pressure. See `OutboxPoller` and `OutboxEventJpaRepository`.

### Domain Events

All domain events implement the `DomainEvent` interface (in `shared/common` or per-service `domain/event` package) with:
- `eventId()` — producer-generated event identity
- `occurredAt()` — producer occurrence timestamp
- `aggregateType()` — logical aggregate name for routing (e.g., `hermandad-member`)
- `aggregateId()` — the aggregate's UUID
- `eventType()` — event discriminant (e.g., `MEMBER_ADDED`)

### Idempotent Kafka Consumer (AS-IS)

Kafka consumers use check-before-process idempotency via the `processed_event` table, keyed by the **producer-generated envelope `eventId`** (payload-hash dedup is retired — hermandad migrated in #26, repertorio's procesion consumer in #27):

1. **Envelope event ID** — the `eventId` produced by the producer at event construction, parsed from the consumed payload.
2. **Check-before-process** — consumer checks `processed_event` before handling; skips if event_id exists.
3. **Register on first process** — stores event_id + consumer_name + timestamp after successful processing.

**TARGET** (active roadmap): Transactional `INSERT ... ON CONFLICT DO NOTHING` with `(consumer_name, event_id)` composite key eliminates the check-then-insert race. See atomic-idempotency Ticket 17 in the active plan.

Topics follow `{aggregate-type}-events` naming (e.g., `hermandad-events`, `hermandad-member-events`). Consumer group: `hermandad-service-group`.

### Event Publishing

Event publishing goes through a single `DomainEventPublisher` port (`application/port/DomainEventPublisher.java`).
The adapter (`adapter/outbound/events/DomainEventPublisherAdapter.java`) wraps two downstream publishers:

1. **Spring `ApplicationEventPublisher`** — synchronous in-process events for listeners (e.g., Keycloak group sync)
2. **OutboxPublisher** — persists to the outbox table for async Kafka delivery

Services never publish to multiple channels manually. One `domainEventPublisher.publish(event)` call routes to both.

### Caching

- Redis via `@Cacheable` / `@CacheEvict`.
- Cache names: `hermandad` (single), `hermandad-members` (list).
- Cache-aside pattern (populate on read, evict on write).

### Auth

OAuth2 / JWT via `spring-boot-starter-oauth2-resource-server`. Keycloak as the issuer. JWK Set URI for token validation.

**Authorization** uses a dual-path approach:
1. **Fast path** — `JwtAuthenticationConverter` extracts `hermandad_memberships` from the JWT claim and creates `HERMANDAD_{id}_{role}` Spring Security authorities. `@PreAuthorize` on admin endpoints checks these authorities first.
2. **Fallback path** — `HermandadSecurityService` queries the `hermandad_member` table directly for users whose membership isn't in the JWT (e.g., newly assigned admins whose token hasn't refreshed).

`@EnableMethodSecurity` is configured in `SecurityConfig`. Unauthenticated access is allowed for `GET /api/hermandades/{id}` and `/actuator/health`. Everything else requires authentication + role check.

### Style Conventions

- No abstract base entities. No generic CRUD services.
- No MapStruct (for now). Manual DTO mapping.
- No Axon Framework, no Debezium CDC, no Saga pattern.
- No JPA annotations in domain models (`@Entity`, `@Table`, `@Id` go in adapter JPA entities under `adapter/outbound/persistence/`). Domain classes are pure Java — see functional-map §0.1.

### Version Compatibility

Versions live in `gradle/libs.versions.toml`. The Spring Cloud release train must match Spring Boot:

| Spring Boot | Spring Cloud Train |
|---|---|---|
| 3.4.x | 2024.0.x (Moorgate) |
| 3.5.x | 2025.0.x (Northfields) |
| 4.0.x | 2025.1.x (Oakwood) |
| 4.1.x | 2026.0.x (Pinehurst) |

When upgrading either, update both. The compatibility verifier (`spring.cloud.compatibility-verifier.enabled=false`) can be disabled temporarily but will break at runtime if versions are mismatched.

**Build prerequisites:**

- Java 21+ in `$JAVA_HOME` (Spring Boot 4.1 requires Java 21)
- `export JAVA_HOME=~/.jdks/jdk-21.0.6+7` before any `./gradlew build`
- Docker builds copy `build/libs/*.jar` — the `.dockerignore` must **not** exclude `build/libs/`

### API Specification

[`docs/openapi.yaml`](./openapi.yaml) is the contract and source of truth — updated first before any controller change. The pre-commit hook enforces this.

The generated springdoc-openAPI spec (`/v3/api-docs`, Swagger UI at `/swagger-ui.html`) reflects running code and is used for validation and interactive exploration, not as the primary contract.

For Postman: File → Import → Link → `http://localhost:8080/v3/api-docs`.

Or browse interactively at `http://localhost:8080/swagger-ui.html`.
