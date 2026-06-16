# Architecture — Semana Santa App

## Stack

- Java 21, Spring Boot 3.x
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

### Event-Driven Sync

Domain events (e.g., `MemberAddedEvent`) are published via Spring's `ApplicationEventPublisher` and handled asynchronously (`@Async`) by listeners. Example: `KeycloakMembershipAdapter` assigns Keycloak group membership when a member is added.

### Caching

- Redis via `@Cacheable` / `@CacheEvict`.
- Cache names: `hermandad` (single), `hermandad-members` (list).
- Cache-aside pattern (populate on read, evict on write).

### Auth

OAuth2 / JWT via `spring-boot-starter-oauth2-resource-server`. Keycloak as the issuer. JWK Set URI for token validation.

### Style Conventions

- No abstract base entities. No generic CRUD services.
- No MapStruct (for now). Manual DTO mapping.
- No Axon Framework, no Debezium CDC, no Saga pattern.
- JPA annotations stay on domain models (pragmatic Hexagonal — JPA is not an "infrastructure leak", it's a practical choice).
