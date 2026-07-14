# Repertorio — Application Functional Map

> **Purpose**: Single-source-of-truth for AI agents to understand the full application topology, architecture, workflows, and current state. Self-contained — no cross-referencing needed.
>
> **Legend**: `✅` active/complete · `⚠️` partial/stub · `❌` missing · `🔴` high severity · `🟡` medium · `🟠` low

---

## 0. Operating Principles

*Rules for AI agents modifying this codebase. Follow these before any implementation work.*

### 0.1 Hexagonal Architecture (Ports & Adapters)

| Layer | Contains | Dependencies | Can import from |
|-------|----------|-------------|-----------------|
| **domain/** | Entities, value objects, events, repository interfaces (ports) | None | JDK only |
| **application/** | Use-cases, services, input/output ports | domain/ | domain/ |
| **adapter/inbound/** | REST controllers, Kafka consumers, scheduled tasks | application/ + domain/ | application/ |
| **adapter/outbound/** | JPA repositories, Kafka producers, REST clients | application/ + domain/ | application/ |

**Dependency rule**: outer layers depend on inner, never the reverse. Domain must not import Spring, JPA, or any framework annotation. Adapters wire everything via Spring config.

### 0.2 DDD Rules

- **Aggregates** are the consistency boundary — `Hermandad` and `HermandadMember` are separate aggregates
- **State machines** belong in entities (`Procesion.changeStatus()`, `HermandadMember.changeRole()`) — not in services
- **Domain events** are recorded inside entities, published by application services — never construct events in adapters
- **No anemic domain** — entities have behavior, not just getters/setters
- **Static factory methods** over constructors for meaningful creation (`Procesion.create()`)

### 0.3 Test-Driven Development

- **Always** write the failing test first before implementation code
- **Unit tests** (JUnit 5, no Spring context) for domain entities and application services
- **`@WebMvcTest`** for controller slice tests (mocked services)
- **`@DataJpaTest`** for repository tests
- **Integration tests** use `IntegrationTestBase` from `shared/common` (Testcontainers for Postgres + Kafka + Redis), `@MockitoBean` for outbox/Kafka to prevent side effects
- **No test = incomplete** — every behavior change must have a test that would fail without it

### 0.4 Security Defaults

- **`anyRequest().authenticated()`** as the base rule in `SecurityConfig` — never `permitAll()` on write endpoints
- **`@PreAuthorize("@hermandadSecurity.isAdmin(#hermandadId)")`** for admin-only mutations (hermandad-service pattern)
- **Public GET only** for listing endpoints that are intentionally open
- Custom `JwtAuthenticationConverter` extracts `hermandad_memberships` JWT claim into Spring Security authorities
- Method-level security (`@EnableMethodSecurity`) over raw request matchers for fine-grained access

### 0.5 Event Reliability (Outbox Pattern)

Writing to Kafka directly is forbidden. All events must:
1. Be published to **both** `ApplicationEventPublisher` (in-process) and `OutboxEventEntity` (DB)
2. Outbox poller (`@Scheduled 5s`) picks up unprocessed rows and sends to `{aggregateType}-events` Kafka topic
3. Consumers must use **idempotency** (`processed_event` dedup table) before processing
4. Outbox queries must have `ORDER BY` and `LIMIT` (currently `findTop100ByProcessedFalseOrderByCreatedAtAsc`)

### 0.6 Database Migrations (Flyway)

- Every schema change is a new migration file — never edit existing migrations
- Naming: `V{n}__description.sql` — increment the version number
- Entity annotations (`@Table`, `@Column`, `@Index`) must mirror the migration — if the SQL has an index, the entity must declare it
- Column type must match: `TEXT` for JSON strings, `TIMESTAMP WITH TIME ZONE` for instants

### 0.7 YAGNI & Code Discipline

- **No unrequested abstractions** — one implementation doesn't need an interface with a single impl
- **Shortest working diff wins** — prefer editing existing files over creating new ones
- **Stdlib/native first** — built-in language features over libraries, already-installed deps over new ones
- **Deletion > addition** — remove dead code when you find it
- Mark deliberate shortcuts with `ponytail:` comment naming the ceiling and upgrade path
- Complex request → ship the lazy version first, question it same sentence

### 0.8 Commit Discipline

- **Commit after each logical change** — not at end of session, not once per day
- Message format: `type: short description` — `feat:`, `fix:`, `refactor:`, `test:`, `docs:`
- Check `git status` + `git diff` before committing — only stage intended files, no secrets
- One concern per commit — don't mix refactors with feature work

### 0.9 Documentation Protocol

Every change that touches any of these **must** update the corresponding document:

| Change affects | Document to update |
|----------------|-------------------|
| API endpoints, request/response shapes | `docs/openapi.yaml` **(first!)** |
| Topology, services, architecture decisions | `docs/architecture.md` |
| Service internals, new issues, resolved issues | `docs/service-reviews.md` |
| Technical debt, backlog, completed sprints | `docs/backlog.md` |
| Feature implementation | `docs/plans/<plan-file>` |
| Functional map (this document) | `docs/functional-map.md` |

**Order matters**: OpenAPI spec first (it's the contract), then docs, then code if the change was already implemented.

---

## 1. Project Topology

```
repertorio/
├── build.gradle.kts                          # Root: Spring Boot 4.1.0 + Spring Cloud 2025.1.2 BOM
├── settings.gradle.kts                       # 7 modules: 2 infra, 5 services, 1 shared
├── gradle/
│   ├── libs.versions.toml                    # Version catalog (Java 21, SB 4.1.0, Testcontainers 1.20.1)
│   └── properties                            # -Xmx2g, auto-download JDK
├── docker-compose.yml                        # Full stack: Keycloak, Kafka, Redis, 5×Postgres, Eureka, Gateway, 2 services, observability
├── docker-compose.dev.yml                    # Dev variant: single Postgres, lighter mem limits, no observability
│
├── infrastructure/
│   ├── api-gateway/          ✅ Active       # Spring Cloud Gateway, port 8080
│   ├── discovery-server/     ✅ Active       # Eureka standalone, port 8761
│   └── keycloak/                             # Realm export + seed scripts
│
├── services/
│   ├── hermandad-service/    ✅ Active       # Brotherhoods + members, port 8081 (59 Java files, 50 tests)
│   ├── procesion-service/    ✅ Active       # Processions + state machine, port 8082 (22 Java files, 47 tests)
│   ├── repertorio-service/   ⚠️ Stub         # Musical repertoire — build.gradle.kts only, no src/
│   ├── tracking-service/     ⚠️ Stub         # GPS tracking — build.gradle.kts only, no src/
│   └── notification-service/ ⚠️ Stub         # Push notifications — build.gradle.kts only, no src/
│
├── shared/
│   └── common/               ✅ Active       # Shared domain: TenantContext, JwtMembershipExtractor, IntegrationTestBase
│
└── docs/
    ├── architecture.md                       # Hexagonal + DDD design doc
    ├── audit.md                              # Historical audit trail
    ├── backlog.md                            # Sprint backlog
    ├── service-reviews.md                    # Per-service reviews + cross-service comparison
    ├── functional-map.md                     # ← THIS FILE
    └── plans/                                # Sprint plans
```

### Module Dependency Graph

```
shared/common  ←────── hermandad-service ←────── api-gateway ←──→ discovery-server
                          ↓ Kafka ↑ (self)              ↓
                      procesion-service (→ Kafka, no consumer yet)
                          ↓
                      (3 stub services: repertorio, tracking, notification)
```

---

## 2. Service Profiles

### 2.1 Active Services

#### hermandad-service (`✅ Active`)

| Attribute | Value |
|-----------|-------|
| **Port** | 8081 |
| **Package** | `com.repertorio.hermandad` |
| **DB** | PostgreSQL `hermandad_db` (port 5432) |
| **Flyway** | 7 migrations (V1–V7) |
| **Java files** | 31 main + 11 test = 59 total |
| **Tests** | 50 (2 integration, 9 unit/slice) |
| **Security** | `@PreAuthorize` + custom JWT converter + `HermandadSecurityService` |
| **Caching** | Redis (`@Cacheable`, `@CacheEvict`) |
| **Messaging** | Kafka producer (outbox) + consumer (idempotent, self-consumption) |
| **Events** | `HermandadCreatedEvent`, `MemberAddedEvent`, `MemberRoleChangedEvent`, `MemberRemovedEvent` |
| **SB4** | Migrated (`tools.jackson`, `spring-boot-properties-migrator`) |

**Key directories:**
```
adapter/
  config/         HermandadSecurityService, JwtAuthenticationConverter, OpenApiConfig, RedisConfig, SecurityConfig, AsyncConfig, KeycloakConfig
  inbound/
    kafka/        IdempotentEventConsumer
    rest/
      controller/ HermandadController
      dto/        CreateHermandadRequest, HermandadResponse, AddMemberRequest, ChangeRoleRequest, MembersCache, ApiError
      GlobalExceptionHandler.java
  outbound/
    events/       DomainEventPublisherAdapter, ProcessedEventEntity, ProcessedEventJpaRepository
    keycloak/     KeycloakMembershipAdapter, KeycloakUserExistenceAdapter
    outbox/       OutboxEventEntity, OutboxEventJpaRepository, OutboxEventPublisher, OutboxPoller
    persistence/  HermandadJpaRepository, HermandadMemberJpaRepository, HermandadRepositoryAdapter, HermandadMemberRepositoryAdapter
application/
  event/          MemberAddedListener
  port/           DomainEvent, DomainEventPublisher, OutboxPublisher, UserExistencePort
  service/        HermandadService
domain/
  event/          HermandadCreatedEvent, MemberAddedEvent, MemberRemovedEvent, MemberRoleChangedEvent
  model/          Hermandad, HermandadMember, HermandadRole, HermandadNotFoundException, HermandadAlreadyExistsException, HermandadMemberNotFoundException
  repository/     HermandadRepository, HermandadMemberRepository
```

#### procesion-service (`✅ Active`)

| Attribute | Value |
|-----------|-------|
| **Port** | 8082 |
| **Package** | `com.repertorio.procesion` |
| **DB** | PostgreSQL `procesion_db` (port 5434) |
| **Flyway** | 3 migrations (V1–V3) |
| **Java files** | 16 main + 6 test = 22 total |
| **Tests** | 47 (12 integration, 11 domain unit, 8 service, 13 controller slice, 3 exception handler) |
| **Security** | `anyRequest().authenticated()` + custom JWT converter — no `@PreAuthorize` |
| **Caching** | None |
| **Messaging** | Kafka producer (outbox) — no consumer |
| **Events** | `ProcesionCreatedEvent`, `ProcesionStatusChangedEvent` |
| **SB4** | Migrated (`tools.jackson`) |

**Key directories:**
```
adapter/
  config/         JwtAuthenticationConverter, OpenApiConfig, SecurityConfig
  inbound/
    rest/
      controller/ ProcesionController
      dto/        CreateProcesionRequest, ProcesionResponse, StatusChangeRequest, ApiError
      GlobalExceptionHandler.java
  outbound/
    events/       DomainEventPublisherAdapter
    outbox/       OutboxEventEntity, OutboxEventJpaRepository, OutboxEventPublisher, OutboxPoller
    persistence/  ProcesionJpaRepository, ProcesionRepositoryAdapter
application/
  port/           DomainEvent, DomainEventPublisher, OutboxPublisher
  service/        ProcesionService
domain/
  event/          ProcesionCreatedEvent, ProcesionStatusChangedEvent
  model/          Procesion, ProcesionStatus, ProcesionNotFoundException
  repository/     ProcesionRepository
```

### 2.2 Stub Services (build.gradle.kts only — no src/)

| Service | Port (Docker) | Purpose | Status |
|---------|--------------|---------|--------|
| **repertorio-service** | 8083 (5433 DB) | Musical marches CRUD | ⚠️ Stub |
| **tracking-service** | 8084 (5435 DB) | GPS tracking during processions | ⚠️ Stub |
| **notification-service** | 8085 (5436 DB) | Push/email notifications | ⚠️ Stub |

### 2.3 Infrastructure Modules

| Module | Tech | Port | Purpose |
|--------|------|------|---------|
| **api-gateway** | Spring Cloud Gateway (WebFlux) | 8080 | Route traffic to services via Eureka `lb://` |
| **discovery-server** | Eureka standalone | 8761 | Service registration + discovery |

---

## 3. Workflow Flows

### 3.1 Request Lifecycle (External → Gateway → Service)

```
Client ──HTTP──► api-gateway:8080
                  │
                  │ JWT validation at gateway level
                  │ (oauth2ResourceServer.jwt)
                  │
                  ├── /api/hermandades/** ──lb://──► hermandad-service:8081
                  ├── /api/procesiones/** ──lb://──► procesion-service:8082
                  ├── /api/marchas/**     ──lb://──► ⚠️ 503 (stub)
                  ├── /api/tracking/**    ──lb://──► ⚠️ 503 (stub)
                  ├── /api/notifications/**─lb://──► ⚠️ 503 (stub)
                  │
                  └── /v3/api-docs/* ────lb://──► target service (Swagger aggregator)
```

Gateway public routes: `GET /api/hermandades`, `GET /api/hermandades/{id}`, Swagger, actuator. All other routes require JWT.

### 3.2 Event Publishing Flow (Hermandad and Procesion — Outbox Pattern)

```
Domain Event (e.g. HermandadCreatedEvent)
    │
    ├──► DomainEventPublisherAdapter
    │       │
    │       ├──► ApplicationEventPublisher (in-process Spring events)
    │       │       │
    │       │       └──► @EventListener / @TransactionalEventListener
    │       │               e.g., MemberAddedListener → KeycloakMembershipAdapter
    │       │
    │       └──► OutboxPublisher
    │               │
    │               └──► OutboxEventEntity saved to outbox_event table
    │
    └──► OutboxPoller (@Scheduled every 5s)
            │
            ├──► SELECT TOP 100 WHERE processed = FALSE ORDER BY created_at ASC
            │
            └──► KafkaTemplate.send(topic = "{aggregateType}-events", payload = JSON)
                    │
                    └──► Kafka topic (e.g. hermandad-events, procesion-events)
```

### 3.3 Event Consumption Flow (Hermandad only)

```
Kafka topic: hermandad-events / hermandad-member-events
    │
    └──► IdempotentEventConsumer (groupId = hermandad-service-group)
            │
            ├──► Check processed_event table by deterministic UUID
            │       ├── Existing → log "duplicate skipped"
            │       └── New → save processed_event row
            │
            └──► ⚠️ No downstream processing — currently a sink (audit/self-consumption)
```

**Note**: `procesion-events` topic exists but has **no consumer** — procesion-service only produces to it.

### 3.4 Kafka Topology

| Topic | Partitions | Producer | Consumer | Status |
|-------|-----------|----------|----------|--------|
| `hermandad-events` | 3 | hermandad-service (outbox) | hermandad-service (self) | ✅ |
| `hermandad-member-events` | 3 | hermandad-service (outbox) | hermandad-service (self) | ✅ |
| `procesion-events` | 3 | procesion-service (outbox) | none | ⚠️ Orphan topic |
| `notification-commands` | 3 | none (planned) | none | ⚠️ No producer/consumer |
| `tracking-events` | 6 | none (planned) | none | ⚠️ No producer/consumer |

### 3.5 State Machine: Procesion Status Transitions

```
          ┌──────────┐
          │  PLANNED │
          └────┬─────┘
         ┌─────┴──────┐
         ▼             ▼
   ┌──────────┐  ┌───────────┐
   │ IN_PROGRESS│  │ CANCELLED │ (terminal)
   └─────┬────┘  └───────────┘
         │
    ┌────┴─────┐
    ▼          ▼
┌─────────┐ ┌───────────┐
│ COMPLETED│ │ CANCELLED │ (terminal)
└─────────┘ └───────────┘
```

Rules enforced in `Procesion.changeStatus()`: PLANNED → IN_PROGRESS|CANCELLED, IN_PROGRESS → COMPLETED|CANCELLED, terminal → error.

---

## 4. API Endpoints

### 4.1 Hermandad Service (`/api/hermandades`)

| Method | Path | Auth | Endpoint | File |
|--------|------|------|----------|------|
| `POST` | `/api/hermandades` | authenticated (any user) | `createHermandad()` | `HermandadController.java:45` |
| `GET` | `/api/hermandades` | public | `getAllHermandades()` | `HermandadController.java:56` |
| `GET` | `/api/hermandades/{hermandadId}` | public | `getHermandad()` | `HermandadController.java:67` |
| `POST` | `/api/hermandades/{hermandadId}/members` | `@PreAuthorize(isAdmin)` | `createHermandadMember()` | `HermandadController.java:82` |
| `GET` | `/api/hermandades/{hermandadId}/members` | `@PreAuthorize(isAdmin)` | `getHermandadMembers()` | `HermandadController.java:95` |
| `PATCH` | `/api/hermandades/{hermandadId}/members/{userId}/role` | `@PreAuthorize(isAdmin)` | `changeHermandadMemberRole()` | `HermandadController.java:110` |
| `DELETE` | `/api/hermandades/{hermandadId}/members/{userId}` | `@PreAuthorize(isAdmin)` | `deleteHermandadMember()` | `HermandadController.java:126` |

**`@PreAuthorize` guard**: `@hermandadSecurity.isAdmin(#hermandadId)` — checks JWT `hermandad_memberships` claim for `HERMANDAD_ADMIN` role on the target hermandad. Falls back to DB query.

### 4.2 Procesion Service (`/api/procesiones`)

| Method | Path | Auth | Endpoint | File |
|--------|------|------|----------|------|
| `POST` | `/api/procesiones` | authenticated | `createProcesion()` | `ProcesionController.java:37` |
| `GET` | `/api/procesiones/{id}` | authenticated | `getProcesion()` | `ProcesionController.java:49` |
| `GET` | `/api/procesiones?hermandadId={id}` | authenticated | `listByHermandad()` | `ProcesionController.java:60` |
| `PATCH` | `/api/procesiones/{id}/status` | authenticated | `changeStatus()` | `ProcesionController.java:77` |
| `DELETE` | `/api/procesiones/{id}` | authenticated | `deleteProcesion()` | `ProcesionController.java:93` |

**Note**: Procesion service has no `@PreAuthorize` — only `anyRequest().authenticated()`. `@EnableMethodSecurity` is declared but unused.

### 4.3 Public Routes (Gateway-level)

| Path | Service |
|------|---------|
| `GET /api/hermandades` | hermandad-service |
| `GET /api/hermandades/{id}` | hermandad-service |
| `GET /v3/api-docs/hermandad` | hermandad-service |
| `GET /v3/api-docs/procesion` | procesion-service |
| `GET /swagger-ui/**` | — |
| `GET /actuator/**` | — |

---

## 5. Data Model

### 5.1 Hermandad DB (`hermandad_db`)

#### `hermandad` table

| Column | Type | Constraints |
|--------|------|-------------|
| `id` | UUID | PK |
| `name` | VARCHAR(255) | NOT NULL, UNIQUE |
| `city` | VARCHAR(255) | NOT NULL |
| `founded_year` | INTEGER | NOT NULL |
| `keycloak_group_id` | VARCHAR(255) | nullable |
| `description` | VARCHAR | nullable |
| `created_at` | TIMESTAMPTZ | NOT NULL |
| `updated_at` | TIMESTAMPTZ | nullable (added V7) |

#### `hermandad_member` table

| Column | Type | Constraints |
|--------|------|-------------|
| `id` | UUID | PK |
| `hermandad_id` | UUID | FK → hermandad(id), NOT NULL |
| `user_id` | VARCHAR(255) | NOT NULL |
| `role` | VARCHAR(50) | NOT NULL (CAPATAZ, HERMANDAD_ADMIN, BAND_DIRECTOR, MUSICIAN) |
| `joined_at` | TIMESTAMPTZ | NOT NULL |
| `updated_at` | TIMESTAMPTZ | NOT NULL |
| | | UNIQUE(hermandad_id, user_id) |

#### `outbox_event` table

| Column | Type | Constraints |
|--------|------|-------------|
| `id` | UUID | PK |
| `aggregate_type` | VARCHAR(50) | NOT NULL |
| `aggregate_id` | UUID | NOT NULL |
| `event_type` | VARCHAR(50) | NOT NULL |
| `payload` | VARCHAR(255) | NOT NULL |
| `created_at` | TIMESTAMPTZ | NOT NULL |
| `processed_at` | TIMESTAMPTZ | nullable |
| `processed` | BOOLEAN | DEFAULT FALSE |

#### `processed_event` table (idempotency)

| Column | Type | Constraints |
|--------|------|-------------|
| `event_id` | UUID | PK |
| `consumer_name` | VARCHAR(100) | NOT NULL |
| `processed_at` | TIMESTAMPTZ | NOT NULL |

**Flyway migrations**: V1 (create tables) → V2 (outbox) → V3 (alter payload column) → V4 (unique name) → V5 (description) → V6 (processed_event) → V7 (updated_at)

### 5.2 Procesion DB (`procesion_db`)

#### `procesion` table

| Column | Type | Constraints |
|--------|------|-------------|
| `id` | UUID | PK |
| `hermandad_id` | UUID | NOT NULL |
| `date` | DATE | NOT NULL (renamed from `fecha` in V2) |
| `time` | TIME | NOT NULL (renamed from `hora` in V2) |
| `status` | VARCHAR(20) | NOT NULL, DEFAULT 'PLANNED' (renamed from `estado` in V2) |
| `created_at` | TIMESTAMP | NOT NULL |
| `updated_at` | TIMESTAMP | NOT NULL |
| | | INDEX idx_procesion_hermandad_id (hermandad_id) |

#### `procesion` status enum → DB mapping

| Java enum | DB value |
|-----------|----------|
| `PLANNED` | `PLANNED` |
| `IN_PROGRESS` | `IN_PROGRESS` |
| `COMPLETED` | `COMPLETED` |
| `CANCELLED` | `CANCELLED` |

#### `outbox_event` table (same schema as hermandad, but `TEXT` payload)

| Column | Type |
|--------|------|
| `id` | UUID PK |
| `aggregate_type` | VARCHAR(50) NOT NULL |
| `aggregate_id` | UUID NOT NULL |
| `event_type` | VARCHAR(50) NOT NULL |
| `payload` | **TEXT** NOT NULL (vs VARCHAR(255) in hermandad) |
| `created_at` | TIMESTAMPTZ NOT NULL |
| `processed_at` | TIMESTAMPTZ nullable |
| `processed` | BOOLEAN DEFAULT FALSE |

**Flyway migrations**: V1 (create procesion + index) → V2 (rename columns to English) → V3 (outbox table with TEXT payload)

### 5.3 Domain Entity ↔ DB Mapping

| Entity | Table | Key Generator |
|--------|-------|---------------|
| `Hermandad` | `hermandad` | `@UuidGenerator` (Hibernate) |
| `HermandadMember` | `hermandad_member` | `@UuidGenerator` |
| `Procesion` | `procesion` | `@UuidGenerator` |
| `OutboxEventEntity` (both) | `outbox_event` | `@UuidGenerator` |
| `ProcessedEventEntity` | `processed_event` | manual UUID |

---

## 6. Security Architecture

### 6.1 Authentication Flow

```
Request with Bearer JWT
    │
    ├──► API Gateway validates JWT (oauth2ResourceServer)
    │       └── Forwards JWT to downstream service via headers
    │
    ├──► Per-service SecurityConfig validates JWT
    │       └── JwtAuthenticationConverter extracts `hermandad_memberships`
    │               claim → List<HermandadMembership>
    │               └── Maps each membership to GrantedAuthority:
    │                   "HERMANDAD_{hermandadId}_{role}"
    │
    └──► Method-level auth (hermandad only):
            @PreAuthorize("@hermandadSecurity.isAdmin(#hermandadId)")
            ├── Fast path: check HERMANDAD_{id}_ADMIN authority from JWT
            └── Fallback path: query DB (HermandadMemberRepository)
```

### 6.2 Endpoint Protection by Service

| Level | Hermandad | Procesion |
|-------|-----------|-----------|
| **Public** | `GET /api/hermandades` + `GET /api/hermandades/{id}` | none |
| **Authenticated** | `POST /api/hermandades` | all endpoints |
| **Admin-only** (`@PreAuthorize`) | member CRUD endpoints | none |

### 6.3 Key Classes

| Class | Service | Role |
|-------|---------|------|
| `SecurityConfig` | both | HTTP security filter chain |
| `JwtAuthenticationConverter` | both | Extracts JWT claims → Spring authorities |
| `JwtMembershipExtractor` | shared/common | Parses `hermandad_memberships` JSON claim |
| `HermandadMembership` | shared/common | `record(hermandadId, role, pasoId)` |
| `HermandadSecurityService` | hermandad | `isAdmin(hermandadId)` — JWT fast path → DB fallback |
| `KeycloakConfig` | hermandad | Admin client for Keycloak REST API |

### 6.4 Roles (Hermandad)

| Enum | Meaning |
|------|---------|
| `HERMANDAD_ADMIN` | Admin — can manage members |
| `CAPATAZ` | Foreman — leads processions |
| `BAND_DIRECTOR` | Band director |
| `MUSICIAN` | Band musician |

---

## 7. Configuration Index

### 7.1 Docker Compose Ports

| Service | Internal Port | External Port |
|---------|--------------|---------------|
| API Gateway | 8080 | 8080 |
| Hermandad Service | 8081 | 8081 |
| Procesion Service | 8082 | 8082 |
| Eureka | 8761 | 8761 |
| Keycloak | 8080 (container) | 8180 |
| Kafka | 29092 (internal) / 9092 (external) | 9092 |
| Redis | 6379 | 6379 |
| Postgres Hermandad | 5432 | 5432 |
| Postgres Repertorio | 5433 | 5433 |
| Postgres Procesion | 5434 | 5434 |
| Postgres Tracking | 5435 | 5435 |
| Postgres Notification | 5436 | 5436 |
| Kafka UI | 8080 (container) | 8086 |

### 7.2 Key Configuration Files

| File | Purpose |
|------|---------|
| `gradle/libs.versions.toml` | Central version catalog (Spring Boot 4.1.0, Spring Cloud 2025.1.2) |
| `build.gradle.kts` (root) | BOM overrides, Java 21 toolchain, common plugins |
| `hermandad-service/build.gradle.kts` | Dependencies: persistence, security, Kafka, Redis, Flyway, caching, OpenAPI |
| `procesion-service/build.gradle.kts` | Dependencies: persistence, security, Kafka, Flyway, OpenAPI, Feign (dead) |
| `hermandad-service/src/main/resources/application.yml` | Port 8081, postgres, redis, kafka:29092, eureka, keycloak admin |
| `procesion-service/src/main/resources/application.yml` | Port 8082, postgres:5434, kafka producer localhost:9092, eureka, no Redis |
| `infrastructure/api-gateway/src/main/resources/application.yml` | Port 8080, routes, JWT validation, Eureka client |
| `infrastructure/discovery-server/src/main/resources/application.yml` | Standalone Eureka, port 8761 |

### 7.3 Docker Profiles

| Profile | Mode | Includes |
|---------|------|----------|
| `core` | docker-compose.yml | Keycloak, Kafka+init+UI, Redis, 5×Postgres, Eureka, Gateway, hermandad, procesion |
| `full` | docker-compose.yml | `core` + Zipkin, ELK (ES+Logstash+Kibana), Prometheus, Grafana |
| `dev` | docker-compose.dev.yml | Single Postgres + init, all services with 256m mem limits, no observability |

---

## 8. Testing Inventory

### 8.1 Test Counts by Module

| Module | Test Files | `@Test` Count | Integration Tests | Infrastructure |
|--------|:----------:|:-------------:|:-----------------:|:--------------:|
| **hermandad-service** | 11 | **50** | 2 (Repository + Controller) | Testcontainers (PG + Kafka + Redis) |
| **procesion-service** | 6 | **47** | 2 (Repository + Controller) | Testcontainers (PG + Kafka), @MockitoBean for Kafka/Outbox |
| **shared/common** | 2 | 7 | 0 | — |
| **api-gateway** | 0 | 0 | 0 | ❌ |
| **discovery-server** | 0 | 0 | 0 | ❌ |
| **3 stub services** | 0 | 0 | 0 | ❌ |
| **Total** | **19** | **104** | **4** | |

### 8.2 Hermandad Tests

| Test File | Type | Tests | What it covers |
|-----------|------|:-----:|----------------|
| `HermandadServiceTest.java` | Unit (mock service) | 8 | CRUD, events, validation, member ops |
| `HermandadTest.java` | Domain unit | 2 | Entity creation |
| `HermandadMemberTest.java` | Domain unit | 4 | Role transitions, no-op validation |
| `HermandadMemberDataJpaTest.java` | JPA slice | 1 | Persistence mapping |
| `HermandadRepositoryIntegrationTest.java` | **IT** (Testcontainers) | 7 | CRUD round-trip, pagination, members |
| `HermandadControllerTest.java` | Web slice (MockMvc) | 16 | All endpoints, auth scenarios |
| `HermandadControllerIntegrationTest.java` | **IT** (Testcontainers + MockMvc) | 3 | HTTP lifecycle with real DB |
| `IdempotentEventConsumerTest.java` | Unit | 3 | Dedup logic |
| `GlobalExceptionHandlerTest.java` | Unit | 3 | Error response format |
| `JwtAuthenticationConverterTest.java` | Unit | 1 | Authority extraction |
| `KeycloakUserExistenceAdapterTest.java` | Unit (mock) | 2 | User existence, not-found |

### 8.3 Procesion Tests

| Test File | Type | Tests | What it covers |
|-----------|------|:-----:|----------------|
| `ProcesionTest.java` | Domain unit | 11 | State machine (all transitions) |
| `ProcesionServiceTest.java` | Unit (mock service) | 8 | CRUD, status transitions, exceptions |
| `ProcesionControllerTest.java` | Web slice (MockMvc) | 13 | All endpoints, 401 scenarios |
| `GlobalExceptionHandlerTest.java` | Unit | 3 | Error response format |
| `ProcesionRepositoryIntegrationTest.java` | **IT** (Testcontainers) | 4 | CRUD, pagination, status persistence |
| `ProcesionControllerIntegrationTest.java` | **IT** (Testcontainers + MockMvc) | 8 | HTTP lifecycle, status transitions, 401 |

### 8.4 Shared Tests

| Test File | Tests | What it covers |
|-----------|:-----:|----------------|
| `TenantContextTest.java` | 3 | ThreadLocal isolation |
| `JwtMembershipExtractorTest.java` | 4 | Claim parsing, null/malformed handling |

### 8.5 Test Infrastructure

| File | Purpose |
|------|---------|
| `shared/common/src/test/.../IntegrationTestBase.java` | Base class — starts PG + Kafka + Redis Testcontainers, sets `@DynamicPropertySource` |
| `shared/common/src/test/.../JwtTestFactory.java` | Builds mock JWT tokens for tests |
| `hermandad-service/src/test/.../TestCacheConfig.java` | Swaps Redis for `ConcurrentMapCacheManager` in tests |
| Test deps: | `spring-boot-starter-test`, `spring-security-test`, `spring-boot-starter-webmvc-test`, `spring-boot-starter-data-jpa-test`, `h2database`, `testcontainers`, `testcontainers-junit-jupiter`, `testcontainers-postgresql` |

---

## 9. Known Technical Debt

| # | Severity | Issue | File(s) | Status |
|---|----------|-------|---------|--------|
| 1 | 🟠 Low | ✅ ~~**Flyway index not mirrored on entity** — `idx_procesion_hermandad_id` exists in SQL but `@Table(indexes = ...)` missing on entity~~ | `Procesion.java`, `V1__create_procesion_table.sql` | Done |
| 2 | 🟠 Low | **No `@PreAuthorize` on procesion controller** — `@EnableMethodSecurity` declared but unused | `ProcesionController.java`, `SecurityConfig.java` | Open |
| 3 | 🟠 Low | ✅ ~~**Dead `@EnableFeignClients`** — annotation + `spring-cloud-starter-openfeign` with zero `@FeignClient`~~ | `ProcesionServiceApplication.java:12`, `build.gradle.kts:18` | Done |
| 4 | 🟠 Low | **No Redis caching on procesion** — hermandad has it, procesion doesn't. Read-heavy listings would benefit | — | Open |
| 5 | 🟡 Medium | ✅ ~~**3 ghost gateway routes** — repertorio, tracking, notification routes point to stub services → 503~~ | `api-gateway/application.yml:33-48` | Done (commented out) |
| 6 | 🟡 Medium | **No consumers for 3 Kafka topics** — `procesion-events`, `notification-commands`, `tracking-events` have no handlers | Kafka init | Open |
| 7 | 🟠 Low | **Hermandad self-consumption** — `IdempotentEventConsumer` consumes hermandad's own topics, no downstream logic | `IdempotentEventConsumer.java` | Open (maybe intentional) |
| 8 | 🟡 Medium | ✅ ~~**Infrastructure zero tests** — `api-gateway` + `discovery-server` have no test coverage at all~~ | — | Done |
| 9 | 🟠 Low | ✅ ~~**Hermandad constructor no validation** — `name`/`city` could be empty strings~~ | `Hermandad.java:34-39` | Done |
| 10 | 🟠 Low | **Gateway routes for `/api/hermandades/{id}/procesiones`** defined in gateway but don't exist in hermandad (legacy from initial design) | `api-gateway/SecurityConfig.java:21-23` | Open (stale routes) |

---

## File Inventory (All Source Files, by Module)

### hermandad-service (31 main files)

```
HermandadServiceApplication.java
adapter/
  config/
    AsyncConfig.java
    HermandadSecurityService.java
    JwtAuthenticationConverter.java
    KeycloakConfig.java
    OpenApiConfig.java
    RedisConfig.java
    SecurityConfig.java
  inbound/
    kafka/IdempotentEventConsumer.java
    rest/
      GlobalExceptionHandler.java
      controller/HermandadController.java
      dto/
        AddMemberRequest.java
        ApiError.java
        ChangeRoleRequest.java
        CreateHermandadRequest.java
        HermandadResponse.java
        MembersCache.java
  outbound/
    events/
      DomainEventPublisherAdapter.java
      ProcessedEventEntity.java
      ProcessedEventJpaRepository.java
    keycloak/
      KeycloakMembershipAdapter.java
      KeycloakUserExistenceAdapter.java
    outbox/
      OutboxEventEntity.java
      OutboxEventJpaRepository.java
      OutboxEventPublisher.java
      OutboxPoller.java
    persistence/
      HermandadJpaRepository.java
      HermandadMemberJpaRepository.java
      HermandadMemberRepositoryAdapter.java
      HermandadRepositoryAdapter.java
application/
  event/MemberAddedListener.java
  port/
    DomainEvent.java
    DomainEventPublisher.java
    OutboxPublisher.java
    UserExistencePort.java
  service/HermandadService.java
domain/
  event/
    HermandadCreatedEvent.java
    MemberAddedEvent.java
    MemberRemovedEvent.java
    MemberRoleChangedEvent.java
  model/
    Hermandad.java
    HermandadAlreadyExistsException.java
    HermandadMember.java
    HermandadMemberNotFoundException.java
    HermandadNotFoundException.java
    HermandadRole.java
  repository/
    HermandadMemberRepository.java
    HermandadRepository.java
```

### procesion-service (16 main files)

```
ProcesionServiceApplication.java
adapter/
  config/
    JwtAuthenticationConverter.java
    OpenApiConfig.java
    SecurityConfig.java
  inbound/
    rest/
      GlobalExceptionHandler.java
      controller/ProcesionController.java
      dto/
        ApiError.java
        CreateProcesionRequest.java
        ProcesionResponse.java
        StatusChangeRequest.java
  outbound/
    events/DomainEventPublisherAdapter.java
    outbox/
      OutboxEventEntity.java
      OutboxEventJpaRepository.java
      OutboxEventPublisher.java
      OutboxPoller.java
    persistence/
      ProcesionJpaRepository.java
      ProcesionRepositoryAdapter.java
application/
  port/
    DomainEvent.java
    DomainEventPublisher.java
    OutboxPublisher.java
  service/ProcesionService.java
domain/
  event/
    ProcesionCreatedEvent.java
    ProcesionStatusChangedEvent.java
  model/
    Procesion.java
    ProcesionNotFoundException.java
    ProcesionStatus.java
  repository/ProcesionRepository.java
```

### infrastructure (4 main files)

```
api-gateway/
  ApiGatewayApplication.java
  SecurityConfig.java
  filter/
    LoggingFilter.java
    TenantIdInjectionFilter.java
discovery-server/
  DiscoveryServerApplication.java
```

### shared/common (4 main files)

```
tenant/
  HermandadMembership.java
  JwtMembershipExtractor.java
  TenantContext.java
  TenantContextFilter.java
```

---

*Generated 2026-07-14. Keep in sync with codebase after significant changes.*
