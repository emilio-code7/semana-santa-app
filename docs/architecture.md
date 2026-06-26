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
| `procesion-service` | Processions, routes, schedules | procesion-db |
| `repertorio-service` | Musical repertoire, marches | repertorio-db |
| `tracking-service` | Real-time GPS positions of processions | tracking-db |
| `notification-service` | Push notifications / alerts | notification-db |
| `api-gateway` | Spring Cloud Gateway | — |
| `discovery-server` | Eureka | — |

No shared databases. Communication via Kafka events for async flows.

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

All domain events implement the `DomainEvent` interface (`application/port/DomainEvent.java`) with:
- `aggregateType()` — logical aggregate name for routing (e.g., `hermandad-member`)
- `aggregateId()` — the aggregate's UUID
- `eventType()` — event discriminant (e.g., `MEMBER_ADDED`)

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
- JPA annotations stay on domain models (pragmatic Hexagonal — JPA is not an "infrastructure leak", it's a practical choice).

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

The REST API is documented via **springdoc-openapi** (`/v3/api-docs`, Swagger UI at `/swagger-ui.html`). The live spec is generated from controller annotations — no manual sync needed.

For Postman: File → Import → Link → `http://localhost:8080/v3/api-docs`.

Or browse interactively at `http://localhost:8080/swagger-ui.html`.

A hand-written contract-first spec lives in [`docs/openapi.yaml`](./openapi.yaml) for design-stage reference, but the running code is the source of truth.
