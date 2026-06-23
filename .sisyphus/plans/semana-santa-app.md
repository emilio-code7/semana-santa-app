# Semana Santa Procesión App — Microservices Learning Project

## TL;DR

> **Quick Summary**: A multi-tenant SaaS backend platform where hermandades register, plan their Holy Week procession routes (with GPS-defined song segments called crucetas), and broadcast live GPS tracking of their pasos during Semana Santa. Musicians and band directors get live song guidance via WebSockets; the public gets a live procession map.
>
> **Deliverables**:
> - 5 microservices: Hermandad, Repertorio, Procesión, Tracking, Notification
> - Full infrastructure: Kafka, Redis, Keycloak, PostgreSQL ×5, ELK, Zipkin, Prometheus/Grafana
> - New patterns learned: Outbox, CQRS, WebSockets/STOMP, Redis (4 strategies), RBAC, ELK, Multi-tenancy
>
> **Estimated Effort**: XL (6 phases, weeks of learning-paced work)
> **Parallel Execution**: YES — within each phase, infrastructure and domain tasks can overlap
> **Critical Path**: Phase 0 → Phase 1 → Phase 2 → Phase 3 (heaviest) → Phase 4 → Phase 5 → Phase 6

---

## Context

### Original Request
A 7+ year Java/Spring backend engineer, between projects and losing motivation after failed interviews, wants a passion-driven learning project. He loves Spanish Holy Week and plays clarinet in a band. The project must be authentic to his domain knowledge and fill real gaps in his microservices experience.

### Interview Summary
**Key Discussions**:
- **Multi-tenant SaaS**: Any hermandad worldwide can self-register and manage their own procesiones
- **Domain richness**: Hermandad → Procesión (multiple/year) → Paso (with optional band) + Cruz de Guía → Cruceta (GPS segment + ordered marchas)
- **Live tracking**: One GPS broadcaster per tracked element (Cruz de Guía + each Paso); all others consume
- **Plan vs Reality**: App shows planned marcha per GPS position; band director overrides live (skip/repeat/override)
- **Public/Private**: Hermandad controls visibility of route and songs to public users
- **Roles**: HERMANDAD_ADMIN, BAND_DIRECTOR (per Paso), MUSICIAN (per Paso), PUBLIC
- **Backend only**: Pure API focus; APIs designed for future frontend/mobile consumption
- **Scope**: Global generic platform — any hermandad from anywhere can use it

**Research Findings**:
- Developer already mastered: Kafka, Saga, Hexagonal Architecture, Resilience4j, API Gateway, Eureka, Zipkin, Prometheus, Keycloak, JWT, Testcontainers
- Gaps to fill: Outbox Pattern, CQRS, WebSockets, Redis, RBAC applied, ELK Stack
- Recommended: Manual CQRS + manual Outbox poller (NOT Axon, NOT Debezium) — learn the patterns directly

### Metis Review
**Identified Gaps (addressed below)**:
- Procesión state machine not defined → defined in Phase 3
- GPS matching algorithm not specified → nearest-cruceta-on-route with 30m radius + speed validation
- Multi-tenancy propagation mechanism → hermandadId in JWT claims, X-Tenant-Id header, DB-scoped queries
- Docker Compose resource consumption → two profiles: `core` and `full`
- User belonging to multiple hermandades → JWT carries list of {hermandadId, role} pairs
- Post-procesión data retention → GPS breadcrumbs persisted in Tracking Service DB for plan-vs-reality report
- Private hermandad visibility → return 404 (not 403) to avoid leaking existence

---

## Work Objectives

### Core Objective
Build a production-quality microservices backend for Semana Santa procession management, using a domain the developer is passionate about, that naturally forces him to implement the patterns missing from his previous project.

### Concrete Deliverables
- `hermandad-service`: Tenant registration, membership, RBAC, Keycloak user-attribute sync (no Keycloak groups — user attributes store memberships as JSON)
- `repertorio-service`: Global + hermandad song library with Redis cache-aside
- `procesion-service`: Route planning, crucetas, agenda, song assignments — CQRS + Outbox Pattern
- `tracking-service`: Live GPS broadcasting, WebSockets/STOMP, Redis write-through + Pub/Sub
- `notification-service`: Kafka event consumer, musician alerts, ELK centralized logging
- `infrastructure/`: Docker Compose (core + full profiles), API Gateway, Eureka, Keycloak realm export
- `shared/`: JWT extraction library, tenant context propagation, Testcontainers base config

### Definition of Done
- [ ] `docker-compose --profile core up` starts cleanly with 0 errors
- [ ] `./gradlew test` passes across all services (0 failures)
- [ ] Full procesión lifecycle works: register → plan route → go live → broadcast GPS → receive song via WebSocket
- [ ] Tenant isolation verified: User from Hermandad A gets 404 on Hermandad B's private data
- [ ] Outbox end-to-end verified: write command → outbox row → Kafka publish → read model updated
- [ ] WebSocket end-to-end verified: GPS broadcast → Redis Pub/Sub → STOMP subscriber receives position

### Must Have
- Multi-tenant isolation (hermandadId scopes ALL data)
- Procesión state machine (DRAFT → PLANNED → LIVE → COMPLETED)
- Outbox Pattern in Procesión Service (manual @Scheduled poller)
- CQRS in Procesión Service (separate write/read models, eventual consistency)
- WebSockets/STOMP in Tracking Service with JWT auth on handshake
- Redis: cache-aside (Repertorio), write-through (Tracking), TTL (Procesión read), Pub/Sub (Tracking fan-out)
- RBAC via Keycloak: all 4 roles enforced with @PreAuthorize
- ELK structured logging in Notification Service
- GPS → cruceta matching algorithm (nearest on route, 30m radius, speed validation)
- Plan-vs-reality report (what was planned vs what was actually played)
- Docker Compose profiles: `core` (dev) and `full` (observability)

### Must NOT Have (Guardrails)
- NO Axon Framework — implement CQRS manually to learn it
- NO Debezium CDC — use manual @Scheduled outbox poller
- Hexagonal Architecture + DDD — domain/application/adapter layers with explicit port interfaces (see docs/architecture.md for the reference pattern)
- NO Saga Pattern — developer already knows this, don't add it
- NO Resilience4j circuit breakers — developer already knows this
- NO Spring Cloud Config Server — use application.yml + Docker env vars
- NO API versioning infrastructure — premature for learning project
- NO abstract base entities (BaseEntity, AuditableEntity) — keep entities concrete
- NO generic CRUD services (GenericCrudService<T>) — explicit domain operations only
- NO MapStruct until manual mapping becomes a clear pain point (3+ identical mappings)
- NO frontend code, Kubernetes manifests, Helm charts, push notifications (mobile)
- NO "smart" song suggestions — app shows planned marchas only; band director is the intelligence
- NO shared databases between services — physical DB isolation from day 1
- NO deprecated Keycloak Spring adapter — use spring-boot-starter-oauth2-resource-server
- NO vague acceptance criteria — every criterion must be a runnable command

### API Spec Practices
- **Location**: `docs/openapi.yaml` — kept close to code, versioned with the project
- **Auto-generation**: Once a service stabilises, add `springdoc-openapi-starter-webmvc-ui` to auto-generate spec at `/v3/api-docs` and serve Swagger UI at `/swagger-ui.html`
- **Consumer-first**: Spec is the contract with frontend/mobile teams — import into Postman or generate client SDKs with `openapi-generator`
- **Schema validation**: Validate all responses against the spec in integration tests
- **Versioning**: When breaking changes land, increment the version prefix in URLs (e.g. `/v1/`, `/v2/`)
- **Don't over-document early**: Specs drift before the API stabilises — start manual, generate later
- **Error responses**: Consistent shape with `code` (machine-readable) and `message` (human-readable)
- **Pagination**: All list endpoints support `page`, `size`, `sort` query params, return `{ content, totalElements, totalPages, page, size }`
- **URL conventions**: Nouns, plurals, nested resources (e.g. `/hermandades/{id}/members`)

### Concurrency & Multithreading Practices
- **Async for I/O, not CPU** — use `@Async` or `CompletableFuture` for external calls (Keycloak, Kafka, Redis), not for computation
- **Event-driven over blocking chains** — publish events (`ApplicationEventPublisher`) instead of calling services directly; listeners run async and don't block the caller
- **Thread pool sizing** — configure `ThreadPoolTaskExecutor` explicitly; don't use defaults in production:
  - `corePoolSize`: based on concurrent I/O tasks expected
  - `maxPoolSize`: 2× `corePoolSize` for burst handling
  - `queueCapacity`: limit to prevent unbounded growth
  - `threadNamePrefix`: for log traceability
- **Never block on async** — never call `.get()` on a `CompletableFuture` in a request thread without a timeout
- **@Async pitfall** — `@Async` methods must be called from outside the class that defines them (Spring proxy limitation); self-invocation bypasses the proxy and runs synchronously
- **Virtual threads (Java 21)** — use `Executors.newVirtualThreadPerTaskExecutor()` for I/O-bound workloads in Tracking Service WebSocket handling — lower memory, simpler debugging than platform threads
- **Concurrency in Kafka consumers** — use `@KafkaListener` with `concurrency` property to partition consumer threads across partitions
- **Thread safety in shared state** — prefer immutable objects (`record`, final fields); minimise shared mutable state; use `ConcurrentHashMap` over synchronised blocks
- **Test concurrency** — use `CountDownLatch`, `Awaitility`, or `ConcurrentTesting` library to verify async behaviour in unit tests

---

## Verification Strategy

> **ZERO HUMAN INTERVENTION** — ALL verification is agent-executed via commands.
> Acceptance criteria requiring "manually verify" are FORBIDDEN.

### Test Decision
- **Infrastructure exists**: NO (new project)
- **Automated tests**: YES (TDD — RED → GREEN → REFACTOR per feature)
- **Framework**: JUnit 5 + Testcontainers + AssertJ + Mockito + EmbeddedKafka + Spring Test STOMP client
- **If TDD**: Each task follows RED (failing test) → GREEN (minimal impl) → REFACTOR (clean up)

### Testing Pyramid Per Service
```
Unit Tests (70%): Domain logic, state machines, GPS matching algorithm, cache behavior
Integration Tests (25%): Repository+DB, Kafka consumer/producer, Redis operations, WebSocket STOMP
E2E Tests (5%): Full lifecycle via API calls (Testcontainers full stack)
```

### QA Policy
Every task includes agent-executed verification scenarios. Evidence saved to `.sisyphus/evidence/`.

- **API/Backend**: Bash (curl) — send requests, assert status + response body fields
- **Database**: Bash (docker exec + psql) — verify DB state directly
- **Kafka**: Bash (kafka-console-consumer) — verify events published
- **Redis**: Bash (docker exec + redis-cli) — verify cache state
- **WebSocket**: Spring Test STOMP client in integration tests
- **Application start**: Bash (./gradlew bootRun + health check curl)

---

## Domain Reference

### Core Concepts Glossary
| Term | Definition |
|------|-----------|
| **Hermandad** | Religious brotherhood. The tenant unit. Self-registers in the app. |
| **Procesión** | A procession event. One hermandad can have multiple per year (Lunes Santo, Martes Santo...). |
| **Paso** | A float in the procession. Can be silent (no band) or have one band. Has its own GPS tracker. |
| **Cruz de Guía** | The cross that leads the procession. Can have its own band. Has its own GPS tracker. |
| **Cruceta** | A street segment defined by GPS start+end coordinates. Contains an ordered list of marchas to play. |
| **Marcha** | A song (marcha procesional). Can be from the global library or hermandad-specific. |
| **Agenda** | The expected schedule: what time each tracked element (Cruz de Guía, each Paso) arrives at each checkpoint. |
| **Ruta** | The route: ordered list of checkpoints (GPS coordinates) shared by all Pasos in a Procesión. |
| **Plan Mode** | Pre-procesión: admin defines route, crucetas, song assignments, agenda. |
| **Live Mode** | During procesión: GPS tracking active, suggested marchas shown, band director can override. |

### Domain Model
```
Hermandad
├── id, name, city, visibility (PUBLIC/PRIVATE), showSongs (boolean)
├── Members: [{userId, role: HERMANDAD_ADMIN|BAND_DIRECTOR|MUSICIAN, pasoId?}]
└── Procesión[] (multiple per year)
    ├── id, name, date, year, state (DRAFT→PLANNED→LIVE→COMPLETED)
    ├── Ruta
    │   └── Checkpoint[] (ordered: {id, name, lat, lng, orderIndex})
    ├── Agenda
    │   └── ScheduleEntry[]: {trackedElementId, checkpointId, expectedTime}
    ├── Cruz de Guía
    │   ├── optional Band {name, type}
    │   ├── GPS broadcaster (designated userId)
    │   └── Cruceta[] (ordered)
    │       ├── startLat, startLng, endLat, endLng
    │       └── Marcha[] (ordered: {marchaId, orderIndex})
    └── Paso[]
        ├── name, orderInProcesion, isSilent (boolean)
        ├── optional Band {name, type}
        ├── GPS broadcaster (designated userId)
        └── Cruceta[] (ordered — same structure as Cruz de Guía)
```

### Procesión State Machine
```
DRAFT ──────→ PLANNED ──────→ LIVE ──────→ COMPLETED
  │              │               │
  └── (edit)     └── (validate   └── (GPS active,
      freely         route,          marchas suggested,
                     crucetas)       band director
                                     can override)
```

**DRAFT → PLANNED transition prerequisites** (validated by `ProcesionService.transitionToPlanned()`):
- At least 1 route checkpoint defined
- At least 1 tracked element (Paso or Cruz de Guía) defined
- At least 1 cruceta assigned to at least 1 tracked element
- **Agenda is NOT required** — hermandades often don't know exact departure times until the day before

**PLANNED → LIVE transition prerequisites**:
- Date matches today (or override flag set by HERMANDAD_ADMIN for testing)

**LIVE → COMPLETED**:
- HERMANDAD_ADMIN explicitly marks complete

### Self-Registration Bootstrap Rule
Any authenticated Keycloak user (with a valid JWT but NO hermandad membership) can call `POST /api/hermandades`.
On creation:
1. Hermandad is saved in DB
2. The calling user (JWT `sub`) is automatically added as `HERMANDAD_ADMIN` of the new hermandad in `HermandadMember` table
3. `HermandadCreated` event published (including the first admin's userId)
4. Subsequent API calls for this hermandad use the hermandad_memberships JWT claim (which the client refreshes from Keycloak after registration)

This means `POST /api/hermandades` is the ONLY endpoint accessible to a user with no hermandad membership. All other hermandad-scoped endpoints require a valid `{hermandadId, role}` pair in JWT claims.

### Role Permissions Matrix
| Action | ANY_AUTHENTICATED | HERMANDAD_ADMIN | BAND_DIRECTOR | MUSICIAN | PUBLIC |
|--------|:-:|:-:|:-:|:-:|:-:|
| Register hermandad (bootstrap) | ✅ | | | | |
| Manage members | | ✅ | | | |
| Create procesión | | ✅ | | | |
| Define route/crucetas | | ✅ | | | |
| Manage repertoire | | ✅ | | | |
| View own paso plan | | ✅ | ✅ | ✅ | |
| Broadcast GPS | | ✅ | ✅ | | |
| Override marcha live | | ✅ | ✅ | | |
| View public procesión | | ✅ | ✅ | ✅ | ✅ (if public) |
| View songs (public) | | ✅ | ✅ | ✅ | ✅ (if showSongs) |

### Event Catalog
```
hermandad-events (Hermandad Service publishes):
  HermandadCreated    { hermandadId, name, city, visibility, showSongs, firstAdminUserId }
  HermandadUpdated    { hermandadId, name, city, visibility, showSongs }  ← includes showSongs changes
  MemberAdded         { hermandadId, userId, role, pasoId? }
  MemberRoleChanged   { hermandadId, userId, oldRole, newRole }
  VisibilityChanged   { hermandadId, newVisibility, showSongs }  ← always carry both fields together

procesion-events (Procesión Service publishes via Outbox):
  ProcesionCreated    { procesionId, hermandadId, date, year, name }
  ProcesionStateChanged {
    procesionId, hermandadId, oldState, newState,
    -- Snapshot payload when state=LIVE (used by Tracking Service to bootstrap sessions):
    trackedElements: [{
      trackedElementId, type (CRUZ_DE_GUIA|PASO), name, isSilent, orderInProcesion,
      broadcasterUserId,
      crucetas: [{crucetaId, startLat, startLng, endLat, endLng, orderIndex,
                  marchas: [{marchaId, title, orderIndex}]}]
    }],
    ruta: { checkpoints: [{checkpointId, name, lat, lng, orderIndex}] }
    -- Note: snapshot only included when newState=LIVE; omitted for other transitions (too large)
  }
  CrucetaAssigned     { procesionId, hermandadId, trackedElementId, crucetaId,
                        startLat, startLng, endLat, endLng, orderIndex,
                        marchas: [{marchaId, title, orderIndex}] }
  MarchaOverridden    { procesionId, hermandadId, trackedElementId, crucetaId,
                        plannedMarchaId, actualMarchaId, actualMarchaTitle, timestamp }

tracking-events (Tracking Service publishes):
  PositionUpdated     { procesionId, hermandadId, trackedElementId, lat, lng, timestamp }
  CrucetaEntered      { procesionId, hermandadId, trackedElementId, crucetaId,
                        firstMarchaId, firstMarchaTitle, timestamp }
  MusicianAlert       { procesionId, hermandadId, trackedElementId, message, currentMarchaTitle,
                        nextMarchaTitle? }

Notification Service consumes: hermandad-events, procesion-events, tracking-events
Procesión Service read projector consumes: hermandad-events (for hermandadName, visibility, showSongs),
                                           procesion-events (for state, crucetas, marchas)
Tracking Service consumes: procesion-events (ProcesionStateChanged with LIVE snapshot for route bootstrap),
                           tracking-events (MusicianAlert events from Notification Service for STOMP delivery)
```

### Read Model Data Sources
```
ProcesionReadModel fields and their sources:
  hermandadName    ← from HermandadCreated / HermandadUpdated (hermandad-events consumer)
  visibility       ← from HermandadCreated / HermandadUpdated (hermandad-events consumer)
  showSongs        ← from HermandadCreated / HermandadUpdated (hermandad-events consumer)
  procesionId      ← from ProcesionCreated (procesion-events)
  state            ← from ProcesionStateChanged (procesion-events)
  trackedElements  ← from CrucetaAssigned events (procesion-events) accumulated over time
  ruta             ← from ProcesionCreated extended with checkpoint add events

Tracking Service session bootstrap:
  On ProcesionStateChanged (newState=LIVE): extract trackedElements snapshot from event payload
  → create TrackedElementSession records (no REST call to Procesión Service needed)
  → cache route in memory for GPS matching during LIVE mode
```

### Redis Key Convention
```
repertorio:{hermandadId}:marcha:{marchaId}          → Marcha details (cache-aside, TTL 1h)
repertorio:global:marcha:{marchaId}                  → Global marcha (cache-aside, TTL 24h)
tracking:{hermandadId}:{trackedElementId}:position   → Current GPS position (write-through, TTL 60s)
tracking:{hermandadId}:{trackedElementId}:cruceta     → Current cruceta index (write-through, TTL 60s)
procesion:{hermandadId}:{procesionId}:public          → Public route read model (TTL 5min)
```

### Kafka Topic Strategy
```
Topic                  Partition Key        Partitions
hermandad-events       hermandadId          3
procesion-events       procesionId          3
tracking-events        trackedElementId     6  (higher volume)
notification-commands  hermandadId          3
```

---

## Tech Stack Reference

| Layer | Technology | Notes |
|-------|-----------|-------|
| Language | Java 21 | Records, sealed classes, pattern matching |
| Framework | Spring Boot 3.x | NOT 4.x (stick to stable for learning) |
| Build | Gradle Kotlin DSL | build.gradle.kts, multi-project |
| Messaging | Apache Kafka 7.4+ | Confluent images |
| Primary DB | PostgreSQL 15 | One instance per service in Docker |
| Cache | Redis 7 | 4 strategies across services |
| Real-time | Spring WebSocket + STOMP | JWT auth on handshake |
| Auth | Keycloak + spring-boot-starter-oauth2-resource-server | NOT deprecated adapter |
| API Gateway | Spring Cloud Gateway (WebFlux) | |
| Service Discovery | Eureka (Spring Cloud Netflix) | |
| Tracing | Zipkin | |
| Metrics | Prometheus + Grafana | |
| Logging | ELK Stack (Elasticsearch + Logstash + Kibana) | Phase 5 |
| Containerization | Docker Compose | Profiles: core, full |
| Testing | JUnit 5, AssertJ, Mockito, Testcontainers, EmbeddedKafka | |

### Service Port Map (authoritative — use these in all QA scenarios)

| Service | container_name | Host Port | Notes |
|---------|---------------|-----------|-------|
| API Gateway | `api-gateway` | 8080 | All client requests via this |
| Eureka Discovery | `discovery-server` | 8761 | Dashboard at http://localhost:8761 |
| Hermandad Service | `hermandad-service` | 8081 | Direct (bypass Gateway in unit QA) |
| Repertorio Service | `repertorio-service` | 8082 | Direct |
| Procesión Service | `procesion-service` | 8083 | Direct |
| Tracking Service | `tracking-service` | 8084 | Direct (also 8084 for WebSocket) |
| Notification Service | `notification-service` | 8085 | Direct |
| Keycloak | `keycloak` | 8180 | Admin UI at http://localhost:8180 |
| Kafka UI | `kafka-ui` | 8086 | |
| Zipkin | `zipkin` | 9411 | Full profile only |
| Prometheus | `prometheus` | 9090 | Full profile only |
| Grafana | `grafana` | 3000 | Full profile only |
| Kibana | `kibana` | 5601 | Full profile only |
| Elasticsearch | `elasticsearch` | 9200 | Full profile only |
| Logstash | `logstash` | 5044 | Full profile only |
| Redis | `redis` | 6379 | |
| PostgreSQL (hermandad) | `postgres-hermandad` | 5432 | |
| PostgreSQL (repertorio) | `postgres-repertorio` | 5433 | |
| PostgreSQL (procesion) | `postgres-procesion` | 5434 | |
| PostgreSQL (tracking) | `postgres-tracking` | 5435 | |
| PostgreSQL (notification) | `postgres-notification` | 5436 | |

### Multi-Tenancy Mechanism
```
1. User authenticates with Keycloak → JWT contains:
   - sub: userId
   - hermandad_memberships: [{hermandadId, role, pasoId?}, ...]

2. API Gateway validates JWT, forwards X-Tenant-Id header (extracted from path param)

3. Each service:
   - Extracts hermandadId from URL path parameter
   - Validates user has required role for THAT hermandadId from JWT claims
   - Scopes ALL DB queries with WHERE hermandad_id = :hermandadId
   - Private hermandades: return 404 (not 403) for unauthorized access
```

---

## Execution Strategy

### Build Order (Sequential Phases)

```
PHASE 0: Foundation
  Infrastructure (Docker Compose) + Shared Library + Build setup
  Duration: ~2-3 days learning pace

PHASE 1: Hermandad Service
  Tenant + membership + RBAC anchor for entire system
  Duration: ~3-4 days

PHASE 2: Repertorio Service
  Song library + Redis cache-aside (first Redis learning)
  Duration: ~2-3 days

PHASE 3: Procesión Service ← CENTERPIECE
  Route + crucetas + agenda + CQRS + Outbox Pattern
  Duration: ~5-7 days (heaviest phase)

PHASE 4: Tracking Service
  Live GPS + WebSockets/STOMP + Redis write-through + Pub/Sub
  Duration: ~4-5 days

PHASE 5: Notification Service
  Kafka consumer + musician alerts + ELK logging
  Duration: ~2-3 days

PHASE 6: Integration & Polish
  E2E tests + plan-vs-reality report + observability
  Duration: ~2-3 days
```

### Within-Phase Parallelism
Within each phase, infrastructure containers and domain implementation can overlap.
Across phases, strictly sequential (each service depends on the previous being done).

---

## TODOs

---

### PHASE 0 — Foundation

- [ ] 0.1. Initialize multi-project Gradle build

  **What to do**:
  - Create root `settings.gradle.kts` declaring all subprojects: `infrastructure/api-gateway`, `infrastructure/discovery-server`, `services/hermandad-service`, `services/repertorio-service`, `services/procesion-service`, `services/tracking-service`, `services/notification-service`, `shared/common`
  - Create root `build.gradle.kts` with shared dependency management (Spring Boot BOM, Java 21 toolchain)
  - Create `gradle/libs.versions.toml` version catalog with all shared dependency versions
  - Verify `./gradlew projects` shows all subprojects

  **Must NOT do**:
  - No code implementation yet — structure only
  - No Spring Boot 4.x — use 3.x

  **Recommended Agent Profile**:
  - **Category**: `quick`
  - **Skills**: []

  **Parallelization**:
  - **Can Run In Parallel**: NO
  - **Parallel Group**: Sequential — must be first
  - **Blocks**: All other tasks
  - **Blocked By**: None

  **References**:
  - Existing project: `/mnt/c/Proyectos/learning/sample-microservices/task-management-saas/settings.gradle` — follow same multi-module pattern but use Kotlin DSL
  - Existing project: `/mnt/c/Proyectos/learning/sample-microservices/task-management-saas/build.gradle` — adapt to Kotlin DSL

  **QA Scenarios**:
  ```
  Scenario: Multi-project build compiles successfully
    Tool: Bash
    Steps:
      1. ./gradlew projects → expect 8 subprojects listed, exit code 0
      2. ./gradlew build -x test → expect BUILD SUCCESSFUL, 0 errors
    Expected Result: All subprojects compile with no errors
    Evidence: .sisyphus/evidence/task-0.1-gradle-build.txt
  ```

  **Acceptance Criteria**:

---

- [ ] 0.2. Docker Compose infrastructure (core profile)

  **What to do**:
  - Create `docker-compose.yml` with `core` profile containing (all containers must define `container_name` matching the service key so that `docker exec <name>` works predictably):
    - Keycloak (`container_name: keycloak`, port 8180) with volume for data persistence
    - Kafka + Zookeeper (`container_name: kafka`, `container_name: zookeeper`) (Confluent 7.4+) with Kafka UI (`container_name: kafka-ui`, port **8086**)
    - Redis 7 (`container_name: redis`, port 6379)
    - 5× PostgreSQL instances, each with explicit `container_name` and named databases:
      - `container_name: postgres-hermandad` (port 5432) → `hermandad_db`
      - `container_name: postgres-repertorio` (port 5433) → `repertorio_db`
      - `container_name: postgres-procesion` (port 5434) → `procesion_db`
      - `container_name: postgres-tracking` (port 5435) → `tracking_db`
      - `container_name: postgres-notification` (port 5436) → `notification_db`
  - Configure Kafka auto-create topics: `hermandad-events`, `procesion-events`, `tracking-events`, `notification-commands` (3 partitions each, tracking-events: 6)
  - Add healthchecks for all containers
  - Export Keycloak realm config to `infrastructure/keycloak/realm-export.json` with:
    - Realm: `semana-santa`
    - Client: `semana-santa-client` (confidential, Direct Access Grants enabled)
    - Roles: `HERMANDAD_ADMIN`, `BAND_DIRECTOR`, `MUSICIAN`
    - Custom claim mapper: `hermandad_memberships` (maps user attribute `hermandad_memberships` to JWT claim as JSON string — type: User Attribute, token claim name: `hermandad_memberships`)
    - **Admin service account client**: `semana-santa-admin-client` (confidential, Service Account Roles enabled, NOT Direct Access Grants). Grant it the `realm-management` → `manage-users` role so `KeycloakAdminService` can update user attributes. Include in realm export. Configure in `hermandad-service` application.yml as `keycloak.admin.client-id` / `keycloak.admin.client-secret`.
  - Seed QA test users via Keycloak Admin REST API (add to `infrastructure/keycloak/seed-qa-users.sh`):
    - `qa-user-no-hermandad` / password: `test` — no memberships (base user)
    - `qa-admin-user` / password: `test` — will become HERMANDAD_ADMIN via self-registration
    - `qa-musician-user` / password: `test` — will be added as MUSICIAN
    - `qa-band-director-user` / password: `test` — will be added as BAND_DIRECTOR
  - Document token acquisition pattern in `infrastructure/keycloak/README.md`:
    ```bash
    # Acquire token (reusable across all QA scenarios):
    TOKEN_ADMIN=$(curl -s -X POST http://localhost:8180/realms/semana-santa/protocol/openid-connect/token \
      -d "grant_type=password&client_id=semana-santa-client&client_secret=secret&username=qa-admin-user&password=test" \
      | jq -r '.access_token')

    TOKEN_MUSICIAN=$(curl -s -X POST http://localhost:8180/realms/semana-santa/protocol/openid-connect/token \
      -d "grant_type=password&client_id=semana-santa-client&client_secret=secret&username=qa-musician-user&password=test" \
      | jq -r '.access_token')

    TOKEN_BAND_DIRECTOR=$(curl -s -X POST http://localhost:8180/realms/semana-santa/protocol/openid-connect/token \
      -d "grant_type=password&client_id=semana-santa-client&client_secret=secret&username=qa-band-director-user&password=test" \
      | jq -r '.access_token')

    TOKEN_PUBLIC=$(curl -s -X POST http://localhost:8180/realms/semana-santa/protocol/openid-connect/token \
      -d "grant_type=password&client_id=semana-santa-client&client_secret=secret&username=qa-user-no-hermandad&password=test" \
      | jq -r '.access_token')

    # Keycloak Admin CLI token (for attribute inspection in 1.6):
    ADMIN_CLI_TOKEN=$(curl -s -X POST http://localhost:8180/realms/master/protocol/openid-connect/token \
      -d "grant_type=password&client_id=admin-cli&username=admin&password=admin" \
      | jq -r '.access_token')
    ```

  **Must NOT do**:
  - No ELK, Prometheus, Grafana, Zipkin yet (those are `full` profile — Task 0.3)
  - No hardcoded passwords in committed files — use environment variables (QA user passwords are dev-only, seed script is not committed with production secrets)

  **Recommended Agent Profile**:
  - **Category**: `unspecified-high`
  - **Skills**: []

  **Parallelization**:
  - **Can Run In Parallel**: YES (with 0.1 — no code dependency)
  - **Parallel Group**: Wave 0 (with Task 0.1)
  - **Blocks**: All service tasks
  - **Blocked By**: None

  **References**:
  - Existing docker-compose: `/mnt/c/Proyectos/learning/sample-microservices/task-management-saas/docker-compose.yml` — follow same pattern, extend with Redis and separate PG instances
  - Keycloak Docker: https://www.keycloak.org/getting-started/getting-started-docker

  **QA Scenarios**:
  ```
  Scenario: All core infrastructure containers start healthy
    Tool: Bash
    Steps:
      1. docker-compose --profile core up -d → exit code 0
      2. docker ps --filter "health=unhealthy" → expect 0 rows
      3. docker exec redis redis-cli ping → expect PONG
      4. curl http://localhost:8180 → expect 200
            5. curl http://localhost:8086 → expect 200 (Kafka UI)
      6. docker exec postgres-hermandad psql -U postgres -c "\l" | grep hermandad_db → expect match
    Expected Result: All containers healthy, all endpoints reachable
    Evidence: .sisyphus/evidence/task-0.2-core-infra.txt

  Scenario: QA test users seeded and tokens can be acquired
    Tool: Bash (curl + jq)
    Preconditions: docker-compose core up, Keycloak healthy, seed-qa-users.sh executed
    Steps:
      1. bash infrastructure/keycloak/seed-qa-users.sh → expect exit 0
      2. TOKEN_ADMIN=$(curl -s -X POST http://localhost:8180/realms/semana-santa/protocol/openid-connect/token \
           -d "grant_type=password&client_id=semana-santa-client&client_secret=secret&username=qa-admin-user&password=test" \
           | jq -r '.access_token')
      3. echo $TOKEN_ADMIN | cut -d'.' -f2 | base64 -d 2>/dev/null | jq '.sub' → expect non-null UUID string
      4. TOKEN_MUSICIAN=$(curl -s -X POST ... -d "...username=qa-musician-user..." | jq -r '.access_token')
      5. echo $TOKEN_MUSICIAN | cut -d'.' -f2 | base64 -d 2>/dev/null | jq '.sub' → expect non-null UUID string
      6. TOKEN_PUBLIC=$(curl -s -X POST ... -d "...username=qa-user-no-hermandad..." | jq -r '.access_token')
      7. echo $TOKEN_PUBLIC | cut -d'.' -f2 | base64 -d 2>/dev/null | jq '.hermandad_memberships' → expect null or empty (no memberships yet)
    Expected Result: All 4 QA users exist, tokens acquired, JWT sub fields are valid UUIDs
    Failure Indicators: '.access_token' is null, jq parse fails, sub field is null
    Evidence: .sisyphus/evidence/task-0.2-qa-users.txt
  ```

  **Acceptance Criteria**:

---

- [ ] 0.3. Docker Compose full profile (observability stack)

  **What to do**:
  - Add `full` profile to `docker-compose.yml` containing:
    - Zipkin (port 9411)
    - Prometheus (port 9090) with `prometheus.yml` scrape config for all 5 services
    - Grafana (port 3000) with pre-configured datasource pointing to Prometheus
    - Elasticsearch (port 9200)
    - Logstash (port 5044) with pipeline config accepting JSON logs from services
    - Kibana (port 5601)
  - Document in README: "Use `--profile core` for development, `--profile full` for full observability"

  **Must NOT do**:
  - Full profile is NOT required for any service to function — keep it truly optional

  **Recommended Agent Profile**:
  - **Category**: `unspecified-high`
  - **Skills**: []

  **Parallelization**:
  - **Can Run In Parallel**: YES (with 0.1, 0.2)
  - **Parallel Group**: Wave 0
  - **Blocks**: Task 5.3 (ELK logging)
  - **Blocked By**: None

  **References**:
  - Existing prometheus.yml: `/mnt/c/Proyectos/learning/sample-microservices/task-management-saas/prometheus.yml`

  **QA Scenarios**:
  ```
  Scenario: Full observability stack starts and accepts data
    Tool: Bash
    Steps:
      1. docker-compose --profile core --profile full up -d → exit code 0
      2. curl http://localhost:9411 → expect 200 (Zipkin)
      3. curl http://localhost:9090/-/healthy → expect "Prometheus Server is Healthy"
      4. curl http://localhost:9200/_cluster/health → expect {"status":"green"} or "yellow"
      5. curl http://localhost:5601/api/status → expect {"status":{"overall":{"state":"green"}}}
    Expected Result: All observability endpoints reachable and healthy
    Evidence: .sisyphus/evidence/task-0.3-full-infra.txt
  ```

  **Acceptance Criteria**:

---

- [ ] 0.4. Shared library and Testcontainers base

  **What to do**:
  - In `shared/common`, create:
    - `TenantContext.java`: ThreadLocal holding current `hermandadId`, populated from HTTP request path parameter
    - `JwtMembershipExtractor.java`: extracts `hermandad_memberships` list from JWT claims
    - `HermandadMembership.java`: record `{hermandadId, role, pasoId?}`
    - `TenantContextFilter.java`: Spring filter that populates TenantContext from `X-Tenant-Id` header
  - In `shared/common/test`, create:
    - `IntegrationTestBase.java`: abstract base with `@Testcontainers`, shared PostgreSQL + Kafka + Redis containers using `@Container static` (started once per test class)
    - Test utilities: `JwtTestFactory.java` — builds mock JWT tokens with hermandad_memberships for test cases

  **Must NOT do**:
  - No business logic in shared — only cross-cutting infrastructure concerns
  - TenantContext is NOT a God object — keep it minimal (hermandadId only + membership list)

  **Recommended Agent Profile**:
  - **Category**: `quick`
  - **Skills**: []

  **Parallelization**:
  - **Can Run In Parallel**: YES (with 0.2, 0.3)
  - **Parallel Group**: Wave 0
  - **Blocks**: All service tasks (they depend on shared test infrastructure)
  - **Blocked By**: Task 0.1 (Gradle structure must exist)

  **References**:
  - Existing JWT handling: `/mnt/c/Proyectos/learning/sample-microservices/task-management-saas/services/user-service/src/main/java/` — adapt JWT extraction pattern
  - Testcontainers docs: https://testcontainers.com/guides/testing-spring-boot-rest-api-using-testcontainers/

  **QA Scenarios**:
  ```
  Scenario: JWT extraction works with hermandad_memberships claim
    Tool: Bash (unit test via ./gradlew)
    Steps:
      1. ./gradlew :shared:common:test → expect BUILD SUCCESSFUL, 0 failures
      2. Verify test output shows JwtMembershipExtractorTest PASSED
      3. Verify IntegrationTestBaseTest PASSED (Testcontainers starts correctly)
    Expected Result: Shared library compiles and all unit tests pass
    Evidence: .sisyphus/evidence/task-0.4-shared-tests.txt
  ```

  **Acceptance Criteria**:

---

- [ ] 0.5. API Gateway and Discovery Server

  **What to do**:
  - In `infrastructure/discovery-server`: standard Eureka Server setup (same as existing project)
  - In `infrastructure/api-gateway`: Spring Cloud Gateway (WebFlux) with:
    - JWT validation (validates against Keycloak `semana-santa` realm) — applied globally EXCEPT for routes listed below
    - Public (JWT-exempt) routes — configured with `.permitAll()` in Gateway SecurityConfig:
      - `GET /api/hermandades` — list public hermandades
      - `GET /api/hermandades/{hermandadId}` — view hermandad (service decides 404 for private)
      - `GET /api/hermandades/{hermandadId}/procesiones` — list procesiones for a hermandad (service filters by visibility)
      - `GET /api/hermandades/{hermandadId}/procesiones/{procesionId}` — view single procesion (service decides 404 for private)
      - `GET /api/hermandades/{hermandadId}/procesiones/{procesionId}/current-marcha` — live marcha for public procesiones (service respects showSongs flag)
      - `GET /api/marchas/**` — global marcha catalog
      - `GET /api/hermandades/{hermandadId}/marchas/**` — hermandad marcha catalog (service enforces member-only auth)
      - `GET /api/procesiones/live` — global live map across all public hermandades (as defined in Task 3.5)
    - All other routes require valid JWT
    - Routes for all 5 services using `lb://` prefix
    - `X-Tenant-Id` header injection: extract hermandadId from URL path (e.g., `/api/hermandades/{hermandadId}/...`) and forward as header
    - Global logging filter

  **Must NOT do**:
  - Gateway does NOT enforce RBAC — that is each service's responsibility
  - No circuit breakers in Gateway

  **Recommended Agent Profile**:
  - **Category**: `quick`
  - **Skills**: []

  **Parallelization**:
  - **Can Run In Parallel**: YES (with 0.2, 0.3, 0.4)
  - **Parallel Group**: Wave 0
  - **Blocks**: Integration testing of all services
  - **Blocked By**: Task 0.1

  **References**:
  - Existing Gateway: `/mnt/c/Proyectos/learning/sample-microservices/task-management-saas/infrastructure/api-gateway/` — replicate, update realm name and routes
  - Existing Discovery: `/mnt/c/Proyectos/learning/sample-microservices/task-management-saas/infrastructure/discovery-server/`

  **QA Scenarios**:
  ```
  Scenario: Gateway enforces JWT on protected routes, allows public routes through
    Tool: Bash (curl)
    Preconditions: docker-compose core up, discovery-server and api-gateway running (hermandad-service may not be up — 502/503 from service is acceptable; 401 must come from Gateway before reaching service)
    Steps:
      1. curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/api/hermandades/{hermandadId}/members
         → expect 401 (protected route, no JWT)
      2. curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/api/hermandades/{hermandadId}/members \
           -H "Authorization: Bearer invalid-token"
         → expect 401 (protected route, invalid JWT)
      3. curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/api/hermandades
         → expect NOT 401 (public route — may return 200, 503, or 502 if service is down, but NOT 401)
      4. curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/api/hermandades/some-id
         → expect NOT 401 (public route — Gateway forwards, service decides response)
      5. curl http://localhost:8761 → expect 200 (Eureka dashboard)
    Expected Result: Protected routes blocked at Gateway (401); public routes forwarded regardless of JWT presence; Eureka reachable
    Failure Indicators: Step 3 returns 401 (public route incorrectly blocked), Steps 1-2 return 200 (protected routes not enforced)
    Evidence: .sisyphus/evidence/task-0.5-gateway-auth.txt
  ```

  **Acceptance Criteria**:

---

### PHASE 1 — Hermandad Service

- [ ] 1.1. Hermandad domain model and repository

  **What to do**:
  - Create Spring Boot project in `services/hermandad-service`
  - Domain entities:
    - `Hermandad`: id (UUID), name, city, country, description, visibility (PUBLIC/PRIVATE), showSongs (boolean), createdAt
    - `HermandadMember`: id, hermandadId, userId (Keycloak sub), role (enum: HERMANDAD_ADMIN/BAND_DIRECTOR/MUSICIAN), pasoReference (nullable String — references a pasoId in Procesión Service), joinedAt
  - Spring Data JPA repositories: `HermandadRepository`, `HermandadMemberRepository`
  - Flyway migration: `V1__create_hermandad_tables.sql`
  - Service layer: `HermandadService` (concrete class, no interface)
  - Write unit tests for domain validation (name required, visibility enum, etc.)

  **Must NOT do**:
  - No abstract base entity, no AuditableEntity
  - No MapStruct yet
  - HermandadMember does NOT store user profile data — only userId (Keycloak sub) + role

  **Recommended Agent Profile**:
  - **Category**: `unspecified-high`
  - **Skills**: []

  **Parallelization**:
  - **Can Run In Parallel**: NO
  - **Parallel Group**: Sequential (Phase 1 start)
  - **Blocks**: Tasks 1.2, 1.3, 1.4, 1.5
  - **Blocked By**: Task 0.4 (shared library)

  **References**:
  - Domain model defined in plan above (Hermandad, HermandadMember entities)
  - Existing entity pattern: `/mnt/c/Proyectos/learning/sample-microservices/task-management-saas/services/user-service/src/main/java/`

  **QA Scenarios**:
  ```
  Scenario: Domain entities persist and validate correctly
    Tool: Bash (./gradlew test)
    Steps:
      1. ./gradlew :services:hermandad-service:test → expect BUILD SUCCESSFUL, 0 failures
      2. Verify test output shows HermandadRepositoryTest PASSED
      3. Verify test output shows HermandadValidationTest PASSED (null name fails)
    Expected Result: Domain model persists to DB, validation catches null name
    Evidence: .sisyphus/evidence/task-1.1-domain-tests.txt
  ```

  **Acceptance Criteria**:

---

- [ ] 1.2. Hermandad REST API (CRUD)

  **What to do**:
  - `HermandadController`: REST endpoints:
    - `POST /api/hermandades` — register new hermandad (HERMANDAD_ADMIN creates)
    - `GET /api/hermandades/{hermandadId}` — get hermandad details (PUBLIC if visibility=PUBLIC, else 404)
    - `PUT /api/hermandades/{hermandadId}` — update hermandad (HERMANDAD_ADMIN only)
    - `GET /api/hermandades` — list all public hermandades (PUBLIC)
  - Request/Response DTOs: `CreateHermandadRequest`, `HermandadResponse`
  - Manual mapping in controller/service (no MapStruct)
  - MockMvc tests for all endpoints

  **Must NOT do**:
  - No DELETE endpoint (hermandades are permanent once created in scope)
  - Private hermandad accessed by unauthorized user → 404 (NOT 403)

  **Recommended Agent Profile**:
  - **Category**: `unspecified-high`
  - **Skills**: []

  **Parallelization**:
  - **Can Run In Parallel**: NO
  - **Parallel Group**: Sequential
  - **Blocks**: Tasks 1.3, 1.4
  - **Blocked By**: Task 1.1

  **References**:
  - Pattern: `/mnt/c/Proyectos/learning/sample-microservices/task-management-saas/services/user-service/src/main/java/.../controller/`
  - Role model in plan above (Role Permissions Matrix)

  **QA Scenarios**:
  ```
  Scenario: Bootstrap — any authenticated user can create hermandad
    Tool: Bash (curl with Keycloak token)
    Preconditions: Keycloak running, hermandad-service running, Task 1.6 (Keycloak sync) deployed
    Steps:
      1. TOKEN_ADMIN=$(curl -s -X POST http://localhost:8180/realms/semana-santa/protocol/openid-connect/token \
           -d "grant_type=password&client_id=semana-santa-client&client_secret=secret&username=qa-admin-user&password=test" \
           | jq -r '.access_token')
         (Note: at this point the token has no hermandad_memberships claim)
      2. HERMANDAD_ID=$(curl -s -X POST http://localhost:8080/api/hermandades \
           -H "Authorization: Bearer $TOKEN_ADMIN" \
           -H "Content-Type: application/json" \
           -d '{"name":"Hermandad de la Macarena","city":"Sevilla","visibility":"PUBLIC"}' \
           | jq -r '.id')
         → expect 201 Created
      3. docker exec postgres-hermandad psql -U postgres -d hermandad_db \
           -c "SELECT name FROM hermandad WHERE name='Hermandad de la Macarena'"
         → expect 1 row
      4. docker exec postgres-hermandad psql -U postgres -d hermandad_db \
           -c "SELECT role FROM hermandad_member WHERE hermandad_id='$HERMANDAD_ID'"
         → expect HERMANDAD_ADMIN (creator auto-added by 1.6 sync)
      5. # MANDATORY: Reacquire token — previous token lacks the new hermandad_memberships claim
         TOKEN_ADMIN=$(curl -s -X POST http://localhost:8180/realms/semana-santa/protocol/openid-connect/token \
           -d "grant_type=password&client_id=semana-santa-client&client_secret=secret&username=qa-admin-user&password=test" \
           | jq -r '.access_token')
      6. echo $TOKEN_ADMIN | cut -d'.' -f2 | base64 -d 2>/dev/null | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('hermandad_memberships'))"
         → expect non-null (contains hermandadId with role HERMANDAD_ADMIN)
    Expected Result: Registration succeeds, creator added as HERMANDAD_ADMIN in DB, fresh token includes hermandad_memberships claim
    Failure Indicators: Step 4 returns 0 rows (sync failed), Step 6 shows null memberships (claim mapper misconfigured)
    Evidence: .sisyphus/evidence/task-1.2-hermandad-api.txt

  Scenario: Private hermandad returns 404 to anonymous
    Tool: Bash (curl)
    Steps:
      1. PRIVATE_ID=$(curl -s -X POST http://localhost:8080/api/hermandades \
           -H "Authorization: Bearer $TOKEN_ADMIN" \
           -H "Content-Type: application/json" \
           -d '{"name":"Secretos de Sevilla","city":"Sevilla","visibility":"PRIVATE"}' | jq -r '.id')
      2. curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/api/hermandades/$PRIVATE_ID
         (no JWT) → expect 404
    Expected Result: Private hermandad existence not revealed to anonymous callers
    Failure Indicators: Returns 403 (existence revealed), 200 (private data leaked)
    Evidence: .sisyphus/evidence/task-1.2-private-hermandad.txt
  ```

  **Acceptance Criteria**:

---

- [ ] 1.3. Hermandad membership and RBAC

  **What to do**:
  - `MembershipController`: endpoints:
    - `POST /api/hermandades/{hermandadId}/members` — add member (HERMANDAD_ADMIN only)
    - `GET /api/hermandades/{hermandadId}/members` — list members (HERMANDAD_ADMIN only)
    - `PUT /api/hermandades/{hermandadId}/members/{userId}/role` — change role (HERMANDAD_ADMIN only)
    - `DELETE /api/hermandades/{hermandadId}/members/{userId}` — remove member (HERMANDAD_ADMIN only)
  - Spring Security config using `spring-boot-starter-oauth2-resource-server`
  - Custom `JwtAuthenticationConverter`: extracts `hermandad_memberships` from JWT, maps to Spring Security authorities in format `ROLE_{hermandadId}_{ROLE}` (e.g., `ROLE_abc123_HERMANDAD_ADMIN`)
  - `@PreAuthorize` expressions checking tenant-scoped roles
  - Security tests using `jwt()` MockMvc post-processor with `hermandad_memberships` claim

  **Must NOT do**:
  - NOT the deprecated Keycloak Spring adapter
  - No global roles — all roles are scoped to a hermandadId
  - No Hexagonal ports/adapters — just Controller → Service → Repository

  **Recommended Agent Profile**:
  - **Category**: `deep`
  - **Skills**: []

  **Parallelization**:
  - **Can Run In Parallel**: NO
  - **Parallel Group**: Sequential
  - **Blocks**: Task 1.4, and all other services (security pattern is established here)
  - **Blocked By**: Task 1.2

  **References**:
  - Existing security config: `/mnt/c/Proyectos/learning/sample-microservices/task-management-saas/services/user-service/src/main/java/.../config/SecurityConfig.java`
  - JWT test pattern: `/mnt/c/Proyectos/learning/sample-microservices/task-management-saas/services/user-service/src/test/` — adapt `jwt()` post-processor usage
  - Spring Security OAuth2 Resource Server: https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html

  **QA Scenarios**:
  ```
  Scenario: RBAC enforces tenant-scoped roles — cross-tenant 404, wrong-role 403
    Tool: Bash (curl)
    Preconditions:
      - hermandad-service running
      - TOKEN_ADMIN acquired and refreshed (has HERMANDAD_ADMIN claim for $HERMANDAD_A_ID — see Task 1.2 step 5)
      - $HERMANDAD_B_ID exists (second hermandad registered by a different user)
      - qa-musician-user exists in Keycloak (seeded in Task 0.2) and NOT yet a member of hermandad A
    Steps:
      1. # Add qa-musician-user to Hermandad A as MUSICIAN (use their Keycloak UUID)
         MUSICIAN_KEYCLOAK_ID=$(curl -s "http://localhost:8180/admin/realms/semana-santa/users?username=qa-musician-user" \
           -H "Authorization: Bearer $ADMIN_CLI_TOKEN" | jq -r '.[0].id')
         curl -s -o /dev/null -w "%{http_code}" \
           -X POST http://localhost:8080/api/hermandades/$HERMANDAD_A_ID/members \
           -H "Authorization: Bearer $TOKEN_ADMIN" \
           -H "Content-Type: application/json" \
           -d "{\"userId\":\"$MUSICIAN_KEYCLOAK_ID\",\"role\":\"MUSICIAN\"}"
         → expect 201
      2. # Reacquire musician token (now has hermandad_memberships claim for hermandad A with MUSICIAN role)
         TOKEN_MUSICIAN=$(curl -s -X POST http://localhost:8180/realms/semana-santa/protocol/openid-connect/token \
           -d "grant_type=password&client_id=semana-santa-client&client_secret=secret&username=qa-musician-user&password=test" \
           | jq -r '.access_token')
      3. # Cross-tenant: admin of A attempts to manage hermandad B → must return 404
         curl -s -o /dev/null -w "%{http_code}" \
           -X POST http://localhost:8080/api/hermandades/$HERMANDAD_B_ID/members \
           -H "Authorization: Bearer $TOKEN_ADMIN" \
           -H "Content-Type: application/json" \
           -d '{"userId":"00000000-0000-0000-0000-000000000099","role":"MUSICIAN"}'
         → expect 404 (B is invisible to A's admin — no existence leakage)
      4. # Wrong role: TOKEN_MUSICIAN (role=MUSICIAN in A) attempts HERMANDAD_ADMIN action on A → 403
         curl -s -o /dev/null -w "%{http_code}" \
           -X POST http://localhost:8080/api/hermandades/$HERMANDAD_A_ID/members \
           -H "Authorization: Bearer $TOKEN_MUSICIAN" \
           -H "Content-Type: application/json" \
           -d '{"userId":"someoneelse","role":"MUSICIAN"}'
         → expect 403 (membership recognized, role insufficient)
    Expected Result: Cross-tenant → 404; wrong-role within own hermandad → 403; member add succeeds for HERMANDAD_ADMIN
    Failure Indicators: Step 3 returns 403 (existence revealed), Step 4 returns 404 (own hermandad unrecognized), Step 1 returns 409 (musician already a member — check preconditions)
    Evidence: .sisyphus/evidence/task-1.3-rbac.txt
  ```

  **Acceptance Criteria**:

---

- [ ] 1.4. Hermandad Kafka event publishing

  **What to do**:
  - Add Kafka producer to Hermandad Service
  - Publish events to `hermandad-events` topic:
    - `HermandadCreated` on `POST /api/hermandades`
    - `MemberAdded` on `POST /api/hermandades/{id}/members`
    - `MemberRoleChanged` on `PUT /api/hermandades/{id}/members/{userId}/role`
    - `VisibilityChanged` on `PUT /api/hermandades/{id}` when visibility changes
  - Event classes in `shared/common` (Event Catalog in plan above)
  - EmbeddedKafka tests verifying events are published with correct payload

  **Must NOT do**:
  - NO Outbox here — Hermandad Service uses simple direct Kafka publishing (Outbox is the Procesión Service lesson)
  - NO circuit breaker on Kafka producer

  **Recommended Agent Profile**:
  - **Category**: `unspecified-high`
  - **Skills**: []

  **Parallelization**:
  - **Can Run In Parallel**: NO
  - **Parallel Group**: Sequential
  - **Blocks**: Task 5.1 (Notification Service consumes hermandad-events)
  - **Blocked By**: Task 1.3

  **References**:
  - Existing Kafka producer: `/mnt/c/Proyectos/learning/sample-microservices/task-management-saas/services/user-service/src/main/java/` — follow event publishing pattern
  - Event Catalog defined in plan above

  **QA Scenarios**:
  ```
  Scenario: Kafka event published on hermandad creation
    Tool: Bash (kafka-console-consumer)
    Preconditions: hermandad-service and Kafka running
    Steps:
      1. POST /api/hermandades → 201 (creates hermandad)
      2. docker exec kafka kafka-console-consumer \
           --bootstrap-server localhost:9092 \
           --topic hermandad-events \
           --from-beginning \
           --max-messages 1 \
           --timeout-ms 5000
         → expect JSON containing eventType=HermandadCreated, hermandadId, showSongs field
    Expected Result: Event published to Kafka with correct payload including showSongs
    Evidence: .sisyphus/evidence/task-1.4-kafka-events.txt
  ```

  **Acceptance Criteria**:

---

- [ ] 1.5. Hermandad Service integration tests

  **What to do**:
  - Full integration test using `IntegrationTestBase` (Testcontainers):
    - Register hermandad → verify in DB → verify Kafka event published
    - Add member → verify membership in DB
    - Tenant isolation: user from hermandadA cannot access hermandadB data → verify 404
    - Visibility: PRIVATE hermandad returns 404 to unauthenticated request
  - Eureka registration: add `spring-cloud-starter-netflix-eureka-client` dependency
  - Health check endpoint via Spring Actuator

  **Recommended Agent Profile**:
  - **Category**: `unspecified-high`
  - **Skills**: []

  **Parallelization**:
  - **Can Run In Parallel**: NO
  - **Parallel Group**: Sequential (Phase 1 completion)
  - **Blocks**: Phase 2 start
  - **Blocked By**: Task 1.4

  **QA Scenarios**:
  ```
  Scenario: Full hermandad service integration test passes
    Tool: Bash (./gradlew test)
    Steps:
      1. ./gradlew :services:hermandad-service:test → expect BUILD SUCCESSFUL, 0 failures
      2. Verify HermandadIntegrationTest PASSED (Testcontainers lifecycle)
      3. Verify TenantIsolationTest PASSED (cross-hermandad 404)
    Expected Result: All integration tests pass including tenant isolation
    Evidence: .sisyphus/evidence/task-1.5-integration-tests.txt

  Scenario: Service registers with Eureka
    Tool: Bash (curl)
    Preconditions: docker-compose core up, hermandad-service running
    Steps:
      1. curl http://localhost:8761/eureka/apps/HERMANDAD-SERVICE → expect 200 with service metadata
    Evidence: .sisyphus/evidence/task-1.5-eureka.txt
  ```

  **Acceptance Criteria**:

---

- [ ] 1.6. Keycloak membership sync

  **What to do**:
  - Add `org.keycloak:keycloak-admin-client` dependency to `hermandad-service/build.gradle`
  - Configure `keycloak.admin.server-url`, `keycloak.admin.realm`, `keycloak.admin.client-id`, `keycloak.admin.client-secret` in `application.yml`
  - Create `KeycloakAdminService` — thin Spring bean wrapping `Keycloak` client:
    ```
    Keycloak keycloak = KeycloakBuilder.builder()
        .serverUrl(adminServerUrl)
        .realm("semana-santa")   // admin client lives in the semana-santa realm (NOT master)
        .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
        .clientId(adminClientId)    // = "semana-santa-admin-client" from Task 0.2 realm export
        .clientSecret(adminClientSecret)
        .build();
    ```
    - `keycloak.admin.realm` in `application.yml` must be `semana-santa` (same realm where `semana-santa-admin-client` is defined)
  - JWT claim structure for `hermandad_memberships` (stored as user attribute, flattened to JSON string):
    ```json
    "[{\"hermandadId\":\"uuid\",\"role\":\"HERMANDAD_ADMIN\",\"pasoId\":null}]"
    ```
  - Define `updateUserHermandadMemberships(String userId)` in `KeycloakAdminService`:
    - Fetch ALL memberships for `userId` from `HermandadMemberRepository`
    - Serialize as JSON array string
    - Call `keycloak.realm(realm).users().get(userId).update(userRepresentation)` with updated attribute
  - Hook `updateUserHermandadMemberships` in `HermandadMemberService` after:
    - `POST /hermandades/{id}/members` (add member)
    - `PUT /hermandades/{id}/members/{userId}/role` (change role)
    - `DELETE /hermandades/{id}/members/{userId}` (remove member)
    - `POST /hermandades` self-registration (creator added as HERMANDAD_ADMIN)
  - Unit test `KeycloakAdminServiceTest`: mock `Keycloak` client, verify attribute JSON matches memberships
  - Integration test `MembershipSyncIntegrationTest`: add member → call sync → assert user attribute updated in Keycloak

  **Must NOT do**:
  - Do NOT use Keycloak Groups for role storage — user attributes only (simpler, no group management needed)
  - Keycloak sync is **synchronous and best-effort**: call `updateUserHermandadMemberships` in the same thread right after the DB transaction commits (keeping it simple for learning). Wrap the Keycloak call in a try/catch — if Keycloak is unavailable, log the error at WARN level and still return HTTP 200/201 (membership is already saved in DB, which is the source of truth)
  - Do NOT share `Keycloak` admin client instance across threads without careful pooling — use `@Bean` singleton with thread-safe builder

  **Recommended Agent Profile**:
  - **Category**: `unspecified-high`
    - Reason: Involves Keycloak Admin Client API, user attribute management, and Spring integration — needs careful implementation
  - **Skills**: []
  - **Skills Evaluated but Omitted**:
    - `playwright`: No UI involved

  **Parallelization**:
  - **Can Run In Parallel**: NO
  - **Parallel Group**: Sequential (end of Phase 1)
  - **Blocks**: Phase 2 start (JWT claims must include memberships before downstream services depend on them)
  - **Blocked By**: Task 1.3 (membership endpoints must exist before sync can hook into them)

  **References**:

  **Pattern References**:
  - `services/user-service/src/main/java/.../config/SecurityConfig.java` — Keycloak JWT config pattern (resource server setup); note: sync is admin-side, not resource-server-side
  - `services/hermandad-service/.../service/HermandadMemberService.java` — call `updateUserHermandadMemberships` immediately after each mutation

  **API/Type References**:
  - Keycloak Admin Client: `UserRepresentation.getAttributes()` / `setAttributes(Map<String, List<String>>)` — attribute key: `hermandad_memberships`, value: singleton list with JSON string
  - `HermandadMemberRepository.findAllByUserId(String userId)` — must return all memberships across all hermandades for this user

  **External References**:
  - Keycloak Admin Client Maven coords: `org.keycloak:keycloak-admin-client:24.0.3` (match your Keycloak server version)
  - Keycloak Admin REST API docs: `https://www.keycloak.org/docs-api/latest/rest-api/index.html#_users` — PUT /admin/realms/{realm}/users/{id}

  **Acceptance Criteria**:
  - [ ] `POST /hermandades/{id}/members` → Keycloak Admin API called in the same request, user attribute `hermandad_memberships` updated synchronously (best-effort: if Keycloak unreachable, HTTP 200 still returned and error logged at WARN)
  - [ ] `DELETE /hermandades/{id}/members/{userId}` → membership entry removed from Keycloak attribute in same request
  - [ ] `./gradlew :services:hermandad-service:test` → `KeycloakAdminServiceTest` PASSES (mock Keycloak unavailability → verify WARN log emitted, no exception propagated)
  - [ ] Keycloak sync failure path: stub Keycloak Admin Client to throw `RuntimeException` → endpoint still returns 200, error appears in logs

  **QA Scenarios**:
  ```
  Scenario: Add member updates Keycloak user attribute
    Tool: Bash (curl + Keycloak Admin API)
    Preconditions: docker-compose core up, Keycloak running at localhost:8180, test user "qa-user-no-hermandad" exists (created in Task 0.2)
    Steps:
      1. TOKEN_ADMIN=$(curl -s -X POST http://localhost:8180/realms/semana-santa/protocol/openid-connect/token \
           -d "grant_type=password&client_id=semana-santa-client&client_secret=secret&username=qa-admin-user&password=test" \
           | jq -r '.access_token')
      2. HERMANDAD_ID=$(curl -s -X POST http://localhost:8080/api/hermandades \
           -H "Authorization: Bearer $TOKEN_ADMIN" \
           -H "Content-Type: application/json" \
           -d '{"name":"Test Hermandad","city":"Sevilla","visibility":"PUBLIC"}' \
           | jq -r '.id')
      3. NEW_USER_ID=$(curl -s http://localhost:8180/admin/realms/semana-santa/users?username=qa-user-no-hermandad \
           -H "Authorization: Bearer $ADMIN_CLI_TOKEN" | jq -r '.[0].id')
      4. curl -s -X POST http://localhost:8080/api/hermandades/$HERMANDAD_ID/members \
           -H "Authorization: Bearer $TOKEN_ADMIN" \
           -H "Content-Type: application/json" \
           -d "{\"userId\":\"$NEW_USER_ID\",\"role\":\"MUSICIAN\"}" → expect 200
      5. ATTRS=$(curl -s http://localhost:8180/admin/realms/semana-santa/users/$NEW_USER_ID \
           -H "Authorization: Bearer $ADMIN_CLI_TOKEN" | jq -r '.attributes.hermandad_memberships[0]')
      6. echo $ATTRS | jq '.[] | select(.hermandadId == "'$HERMANDAD_ID'")' → expect object with role "MUSICIAN"
    Expected Result: Keycloak user attribute contains entry with hermandadId and role MUSICIAN
    Failure Indicators: Empty attribute, attribute missing hermandadId, jq select returns null
    Evidence: .sisyphus/evidence/task-1.6-keycloak-sync.txt

  Scenario: Remove member removes hermandad from Keycloak attribute
    Tool: Bash (curl + Keycloak Admin API)
    Preconditions: Previous scenario completed (qa-user-no-hermandad is a MUSICIAN in $HERMANDAD_ID)
    Steps:
      1. curl -s -X DELETE http://localhost:8080/api/hermandades/$HERMANDAD_ID/members/$NEW_USER_ID \
           -H "Authorization: Bearer $TOKEN_ADMIN" → expect 204
      2. ATTRS=$(curl -s http://localhost:8180/admin/realms/semana-santa/users/$NEW_USER_ID \
           -H "Authorization: Bearer $ADMIN_CLI_TOKEN" | jq -r '.attributes.hermandad_memberships[0]')
      3. echo $ATTRS | jq '.[] | select(.hermandadId == "'$HERMANDAD_ID'")' → expect empty/null (no entry)
    Expected Result: Keycloak attribute no longer contains the hermandad entry
    Failure Indicators: Entry still present after deletion
    Evidence: .sisyphus/evidence/task-1.6-keycloak-sync-remove.txt
  ```

  **Commit**: YES (completes Phase 1)
  - Message: `feat(hermandad): sync membership changes to Keycloak user attributes`
  - Files: `services/hermandad-service/`
  - Pre-commit: `./gradlew :services:hermandad-service:test`

---

### PHASE 2 — Repertorio Service

- [ ] 2.1. Repertorio domain model and global library

  **What to do**:
  - Create Spring Boot project in `services/repertorio-service`
  - Domain entities:
    - `Marcha`: id (UUID), title, composer (nullable), year (nullable), durationSeconds (nullable), type (enum: MARCHA_PROCESIONAL/MARCHA_FÚNEBRE/HIMNO/OTHER), isGlobal (boolean), hermandadId (nullable — null means global)
    - `HermandadRepertoire`: id, hermandadId, marchaId — join table for hermandad-specific additions
  - Flyway migration: `V1__create_repertorio_tables.sql`
  - Seed initial global marchas (10-15 well-known marchas procesionales)
  - `MarchaService`: concrete class with CRUD operations
  - Unit tests for domain validation

  **Must NOT do**:
  - No audio file storage — Marcha is metadata only
  - hermandadId on Marcha is nullable: null = global, non-null = hermandad-specific

  **Recommended Agent Profile**:
  - **Category**: `unspecified-high`
  - **Skills**: []

  **Parallelization**:
  - **Can Run In Parallel**: NO
  - **Parallel Group**: Sequential (Phase 2 start, after Phase 1 complete)
  - **Blocks**: Tasks 2.2, 2.3
  - **Blocked By**: Task 1.5

  **QA Scenarios**:
  ```
  Scenario: Global marchas seeded and queryable
    Tool: Bash (curl + psql)
    Preconditions: repertorio-service running
    Steps:
      1. docker exec postgres-repertorio psql -U postgres -d repertorio_db \
           -c "SELECT count(*) FROM marcha WHERE is_global=true"
         → expect count >= 10 (seeded global marchas)
      2. ./gradlew :services:repertorio-service:test → expect BUILD SUCCESSFUL, 0 failures
    Expected Result: Seed data loaded, domain model persists correctly
    Evidence: .sisyphus/evidence/task-2.1-domain-tests.txt
  ```

  **Acceptance Criteria**:

---

- [ ] 2.2. Repertorio REST API

  **What to do**:
  - `MarchaController`: endpoints:
    - `GET /api/marchas` — list global marchas (PUBLIC, no auth needed)
    - `GET /api/marchas/{marchaId}` — get single global marcha by ID (PUBLIC, no auth needed)
    - `GET /api/hermandades/{hermandadId}/marchas` — list hermandad repertoire (members only)
    - `GET /api/hermandades/{hermandadId}/marchas/{marchaId}` — get single hermandad marcha by ID (members only)
    - `POST /api/hermandades/{hermandadId}/marchas` — add custom marcha to hermandad (HERMANDAD_ADMIN only)
    - `PUT /api/hermandades/{hermandadId}/marchas/{marchaId}` — update hermandad-specific marcha (HERMANDAD_ADMIN only)
    - `DELETE /api/hermandades/{hermandadId}/marchas/{marchaId}` — delete hermandad-specific marcha (HERMANDAD_ADMIN only, only if not referenced in any cruceta)
      - **Cross-service usage check mechanism**: `repertorio-service` calls `procesion-service` synchronously via `RestTemplate`/`WebClient` at `GET http://procesion-service/api/internal/marchas/{marchaId}/usage?hermandadId={hermandadId}` → returns `{"referenced": true/false}`. If `referenced=true`, return 409 Conflict. If `procesion-service` is down, return 503 with error message (fail-safe: never delete if usage cannot be confirmed).
      - Add internal endpoint `GET /api/internal/marchas/{marchaId}/usage` to **procesion-service** (in Phase 3, Task 3.2) — accessible only from within Docker Compose network (no JWT required for internal traffic via direct service URL; Gateway only exposes `/api/...` routes publicly)
    - `GET /api/hermandades/{hermandadId}/marchas/all` — list global + hermandad marchas combined (members only)
  - Copy security config pattern from Hermandad Service
  - MockMvc tests for all endpoints

  **Must NOT do**:
  - No DELETE for global marchas (they are curated)
  - HERMANDAD_ADMIN can delete their hermandad-specific custom marchas only

  **Recommended Agent Profile**:
  - **Category**: `unspecified-high`
  - **Skills**: []

  **Parallelization**:
  - **Can Run In Parallel**: NO
  - **Parallel Group**: Sequential
  - **Blocks**: Task 2.3
  - **Blocked By**: Task 2.1

  **QA Scenarios**:
  ```
  Scenario: Global marchas publicly accessible, hermandad marchas require auth
    Tool: Bash (curl)
    Preconditions: repertorio-service running
    Steps:
      1. curl http://localhost:8080/api/marchas (no JWT) → expect 200 with list of global marchas
      2. curl http://localhost:8080/api/hermandades/{id}/marchas (no JWT) → expect 401
      3. Get MUSICIAN JWT, curl /api/hermandades/{id}/marchas → expect 200 (musicians can view)
      4. curl -X POST /api/hermandades/{id}/marchas with MUSICIAN JWT → expect 403
      5. curl -X POST /api/hermandades/{id}/marchas with HERMANDAD_ADMIN JWT
           -d '{"title":"El Amor en los Tiempos del Cólera","composer":"Abel Moreno"}'
         → expect 201
    Expected Result: Public/private endpoints correctly separated by role
    Evidence: .sisyphus/evidence/task-2.2-repertorio-api.txt
  ```

  **Acceptance Criteria**:

---

- [ ] 2.3. Repertorio Redis cache-aside

  **What to do**:
  - Add `spring-boot-starter-data-redis` dependency
  - Implement cache-aside pattern MANUALLY (NOT `@Cacheable` annotation — implement it by hand to understand the pattern):
    - `MarchaCache`: component with `get(marchaId)`, `put(marchaId, marcha)`, `evict(marchaId)` methods using `RedisTemplate<String, Marcha>`
    - `MarchaService.findById()`: check Redis first → if miss, query DB → store in Redis → return
    - `MarchaService.update()`: update DB → evict from Redis (cache invalidation)
    - TTL: global marchas = 24 hours, hermandad-specific = 1 hour
    - Key pattern: `repertorio:global:marcha:{id}` and `repertorio:{hermandadId}:marcha:{id}`
  - Integration tests verifying cache hit/miss behavior using Testcontainers Redis

  **Must NOT do**:
  - NO `@Cacheable`/`@CacheEvict` Spring annotations — implement cache-aside manually to learn it
  - No cache for list queries (only individual marcha by ID)

  **Recommended Agent Profile**:
  - **Category**: `deep`
  - **Skills**: []

  **Parallelization**:
  - **Can Run In Parallel**: NO
  - **Parallel Group**: Sequential
  - **Blocks**: Phase 3 start
  - **Blocked By**: Task 2.2

  **References**:
  - Redis key convention defined in plan above
  - Spring Data Redis: https://docs.spring.io/spring-data/redis/docs/current/reference/html/

  **QA Scenarios**:
  ```
  Scenario: Cache miss then hit for global marcha
    Tool: Bash (redis-cli + curl)
    Preconditions: Redis empty, global marcha with id=$MARCHA_ID exists in DB (seeded in Task 2.1)
    Steps:
      1. docker exec redis redis-cli GET "repertorio:global:marcha:$MARCHA_ID" → expect (nil)
      2. curl -s http://localhost:8080/api/marchas/$MARCHA_ID → expect 200 with marcha JSON body
      3. docker exec redis redis-cli GET "repertorio:global:marcha:$MARCHA_ID" → expect non-empty JSON string (cache populated)
      4. curl -s http://localhost:8080/api/marchas/$MARCHA_ID again → expect 200 (same data, served from cache)
    Expected Result: First call populates Redis; subsequent calls served from cache
    Failure Indicators: Step 3 still returns (nil) after fetch
    Evidence: .sisyphus/evidence/task-2.3-cache-hit-miss.txt

  Scenario: Cache invalidation on hermandad-specific marcha update
    Tool: Bash (redis-cli + curl)
    Preconditions: HERMANDAD_ID and TOKEN_ADMIN from Task 1.2; custom marcha created via POST /api/hermandades/$HERMANDAD_ID/marchas → yields $CUSTOM_MARCHA_ID
    Steps:
      1. curl -s http://localhost:8080/api/hermandades/$HERMANDAD_ID/marchas/$CUSTOM_MARCHA_ID \
           -H "Authorization: Bearer $TOKEN_ADMIN"
         → expect 200 (populates hermandad-scoped cache key)
      2. docker exec redis redis-cli GET "repertorio:$HERMANDAD_ID:marcha:$CUSTOM_MARCHA_ID"
         → expect non-empty JSON (cache populated)
      3. curl -s -X PUT http://localhost:8080/api/hermandades/$HERMANDAD_ID/marchas/$CUSTOM_MARCHA_ID \
           -H "Authorization: Bearer $TOKEN_ADMIN" \
           -H "Content-Type: application/json" \
           -d '{"title":"El Amor en los Tiempos del Cólera (updated)","composer":"Abel Moreno"}'
         → expect 200
      4. docker exec redis redis-cli GET "repertorio:$HERMANDAD_ID:marcha:$CUSTOM_MARCHA_ID"
         → expect (nil) (cache evicted on update)
    Expected Result: Hermandad-scoped cache key evicted after PUT update; subsequent GET will re-populate from DB
    Failure Indicators: Step 4 still returns JSON (cache not evicted); cache key uses wrong scope
    Evidence: .sisyphus/evidence/task-2.3-cache-invalidation.txt
  ```

  **Acceptance Criteria**:
  - [ ] Cache integration test: first call hits DB, second call hits Redis (verified via mock/spy on repository)
  - [ ] Update test: after update, `redis-cli GET {key}` returns nil
  - [ ] `./gradlew :services:repertorio-service:test` passes

  **Commit**: `feat(repertorio): add manual Redis cache-aside with TTL and cache invalidation`

---

### PHASE 3 — Procesión Service (CENTERPIECE)

- [ ] 3.1. Procesión write-side domain model

  **What to do**:
  - Create Spring Boot project in `services/procesion-service`
  - Domain entities (write side):
    - `Procesion`: id, hermandadId, name, date, year, state (enum: DRAFT/PLANNED/LIVE/COMPLETED), createdAt
    - `Ruta`: id, procesionId — container for checkpoints
    - `Checkpoint`: id, rutaId, name, lat, lng, orderIndex
    - `TrackedElement`: id, procesionId, type (enum: CRUZ_DE_GUIA/PASO), name, isSilent, bandName (nullable), broadcasterUserId (nullable), orderInProcesion
    - `Cruceta`: id, trackedElementId, procesionId, startLat, startLng, endLat, endLng, orderIndex
    - `CrucetaMarcha`: id, crucetaId, marchaId, orderIndex
    - `AgendaEntry`: id, procesionId, trackedElementId, checkpointId, expectedTime
  - State machine: `ProcesionStateMachine.java` — validates transitions, throws `IllegalStateTransitionException`
    - DRAFT → PLANNED: validates at least 1 checkpoint + 1 TrackedElement + at least 1 Cruceta (agenda NOT required)
    - PLANNED → LIVE: validates date matches today OR forceStart=true flag (for testing)
    - LIVE → COMPLETED: unconditional (manual mark by HERMANDAD_ADMIN)
    - All backwards transitions (COMPLETED→any, LIVE→PLANNED, PLANNED→DRAFT): throw `IllegalStateTransitionException`
  - Flyway migrations
  - Unit tests for ALL state machine transitions (valid and invalid)

  **Must NOT do**:
  - No Hexagonal Architecture — plain JPA entities + repositories + service
  - No abstract base entity

  **Recommended Agent Profile**:
  - **Category**: `deep`
  - **Skills**: []

  **Parallelization**:
  - **Can Run In Parallel**: NO
  - **Parallel Group**: Sequential (Phase 3 start, after Phase 2 complete)
  - **Blocks**: Tasks 3.2 through 3.8
  - **Blocked By**: Task 2.3

  **References**:
  - Domain model defined in plan above
  - State machine pattern: `/mnt/c/Proyectos/learning/sample-microservices/task-management-saas/services/project-service/src/main/java/.../domain/SagaStatus.java` — same approach for state machine

  **QA Scenarios**:
  ```
  Scenario: State machine rejects invalid transitions
    Tool: Bash (./gradlew test)
    Steps:
      1. ./gradlew :services:procesion-service:test → expect BUILD SUCCESSFUL, 0 failures
      2. Verify ProcesionStateMachineTest PASSED:
         - DRAFT→PLANNED: passes
         - DRAFT→LIVE: throws IllegalStateTransitionException
         - COMPLETED→DRAFT: throws IllegalStateTransitionException
         - PLANNED→COMPLETED (skipping LIVE): throws IllegalStateTransitionException
    Expected Result: All valid transitions succeed, all invalid transitions throw exception
    Evidence: .sisyphus/evidence/task-3.1-state-machine.txt
  ```

  **Acceptance Criteria**:

---

- [ ] 3.2. Procesión REST API (write side)

  **What to do**:
  - `ProcesionController`: write-side endpoints:
    - `POST /api/hermandades/{hermandadId}/procesiones` — create procesión (HERMANDAD_ADMIN)
    - `PUT /api/hermandades/{hermandadId}/procesiones/{procesionId}/state` — transition state (HERMANDAD_ADMIN)
    - `POST /api/hermandades/{hermandadId}/procesiones/{procesionId}/ruta/checkpoints` — add checkpoint (HERMANDAD_ADMIN)
    - `POST /api/hermandades/{hermandadId}/procesiones/{procesionId}/pasos` — add Paso or Cruz de Guía (HERMANDAD_ADMIN)
    - `POST /api/hermandades/{hermandadId}/procesiones/{procesionId}/pasos/{pasoId}/crucetas` — add cruceta with marchas (HERMANDAD_ADMIN)
    - `PUT /api/hermandades/{hermandadId}/procesiones/{procesionId}/pasos/{pasoId}/crucetas/{crucetaId}/override` — band director live marcha override (BAND_DIRECTOR, state=LIVE only)
    - `POST /api/hermandades/{hermandadId}/procesiones/{procesionId}/agenda` — set schedule entry (HERMANDAD_ADMIN)
    - `GET /api/internal/marchas/{marchaId}/usage?hermandadId={hermandadId}` — **internal-only** endpoint for Repertorio Service to check if a marcha is referenced in any cruceta; no JWT required (only accessible within Docker Compose network); returns `{"referenced": true/false, "crucetaCount": N}`
  - Security config: copy pattern from Hermandad Service (internal endpoint is secured by being non-routable through Gateway)
  - MockMvc tests for all write endpoints (including internal usage endpoint)

  **Must NOT do**:
  - Override endpoint only valid when procesión state = LIVE (validate in service)
  - No write operations on procesión in COMPLETED state

  **Recommended Agent Profile**:
  - **Category**: `unspecified-high`
  - **Skills**: []

  **Parallelization**:
  - **Can Run In Parallel**: NO
  - **Parallel Group**: Sequential
  - **Blocks**: Tasks 3.3, 3.4
  - **Blocked By**: Task 3.1

  **QA Scenarios**:
  ```
  Scenario: Full procesion planning lifecycle via API (DRAFT → PLANNED)
    Tool: Bash (curl)
    Preconditions: procesion-service running, TOKEN_ADMIN acquired (see Task 0.2), hermandadId known
    Steps:
      1. PROCESION_ID=$(curl -s -X POST http://localhost:8080/api/hermandades/$HERMANDAD_ID/procesiones \
           -H "Authorization: Bearer $TOKEN_ADMIN" \
           -H "Content-Type: application/json" \
           -d '{"name":"Lunes Santo 2025","date":"2025-04-14","year":2025}' \
           | jq -r '.id')
         → expect 201 with state=DRAFT
      2. curl -s -X POST http://localhost:8080/api/hermandades/$HERMANDAD_ID/procesiones/$PROCESION_ID/ruta/checkpoints \
           -H "Authorization: Bearer $TOKEN_ADMIN" \
           -H "Content-Type: application/json" \
           -d '{"name":"Iglesia de La O","lat":37.386,"lng":-5.993,"orderIndex":0}' → expect 201
      3. PASO_ID=$(curl -s -X POST http://localhost:8080/api/hermandades/$HERMANDAD_ID/procesiones/$PROCESION_ID/pasos \
           -H "Authorization: Bearer $TOKEN_ADMIN" \
           -H "Content-Type: application/json" \
           -d '{"name":"Paso de Misterio","type":"PASO","isSilent":false,"orderInProcesion":1}' \
           | jq -r '.id')
         → expect 201 with trackedElementId
       4. # Get a real marcha UUID from Repertorio Service (seeded global marchas from Task 2.1)
          MARCHA_ID=$(curl -s http://localhost:8080/api/marchas | jq -r '.[0].id')
          curl -s -X POST http://localhost:8080/api/hermandades/$HERMANDAD_ID/procesiones/$PROCESION_ID/pasos/$PASO_ID/crucetas \
            -H "Authorization: Bearer $TOKEN_ADMIN" \
            -H "Content-Type: application/json" \
            -d "{\"startLat\":37.386,\"startLng\":-5.993,\"endLat\":37.388,\"endLng\":-5.991,\"orderIndex\":0,\"marchas\":[{\"marchaId\":\"$MARCHA_ID\",\"orderIndex\":0}]}" \
            → expect 201 (cruceta created with real marcha reference)
      5. curl -s -X PUT http://localhost:8080/api/hermandades/$HERMANDAD_ID/procesiones/$PROCESION_ID/state \
           -H "Authorization: Bearer $TOKEN_ADMIN" \
           -H "Content-Type: application/json" \
           -d '{"newState":"PLANNED"}' → expect 200 with state=PLANNED
      6. curl -s -X PUT http://localhost:8080/api/hermandades/$HERMANDAD_ID/procesiones/$PROCESION_ID/state \
           -H "Authorization: Bearer $TOKEN_ADMIN" \
           -H "Content-Type: application/json" \
           -d '{"newState":"DRAFT"}' → expect 409 Conflict (backwards transition forbidden)
    Expected Result: DRAFT → PLANNED succeeds when checkpoint+paso+cruceta exist; backward to DRAFT rejected with 409
    Failure Indicators: Step 5 returns 422 (prerequisites missing), Step 6 returns 200 (backwards transition allowed)
    Evidence: .sisyphus/evidence/task-3.2-write-api.txt

  Scenario: PLANNED transition rejected when no cruceta defined
    Tool: Bash (curl)
    Preconditions: Fresh procesion in DRAFT with 1 checkpoint and 1 paso but NO crucetas
    Steps:
      1. Create procesion, add checkpoint, add paso (as above, steps 1-3)
      2. curl -s -X PUT .../state -d '{"newState":"PLANNED"}' -H "Authorization: Bearer $TOKEN_ADMIN"
         → expect 422 Unprocessable Entity with message containing "cruceta" or "route incomplete"
    Expected Result: Transition rejected with 422 when cruceta prerequisite not met
    Failure Indicators: Returns 200 (transition allowed without crucetas)
    Evidence: .sisyphus/evidence/task-3.2-planned-rejection.txt
  ```

  **Acceptance Criteria**:

---

- [ ] 3.3. Outbox Pattern implementation

  **What to do**:
  - Create `OutboxMessage` entity: id (UUID), aggregateType (String), aggregateId (UUID), eventType (String), payload (String/JSON), status (enum: PENDING/PUBLISHED/FAILED), retryCount (int), topic (String), partitionKey (String), createdAt, publishedAt
  - Flyway migration: `V2__create_outbox_table.sql`
  - `OutboxRepository`: Spring Data JPA
  - `OutboxPublisher`: component that saves OutboxMessage in the SAME transaction as the domain operation
  - `OutboxPoller`: `@Scheduled(fixedDelay = 5000)` component that:
    - Queries `status=PENDING` messages (batch of 100)
    - Publishes each to Kafka using `KafkaTemplate`
    - Marks as `PUBLISHED` on success, increments `retryCount` on failure
    - After 3 failures, marks as `FAILED`
  - Modify ALL write-side operations in `ProcesionService` to save outbox message in same `@Transactional` block
  - Integration test: write command → verify OutboxMessage in DB → verify poller publishes to Kafka topic → verify message consumed
  - Events published: `ProcesionCreated`, `ProcesionStateChanged`, `CrucetaAssigned`, `MarchaOverridden`

  **Must NOT do**:
  - NO Debezium CDC — manual @Scheduled poller only
  - NO separate transaction for outbox save — MUST be same transaction as domain write
  - No outbox for READ operations — only write commands

  **Recommended Agent Profile**:
  - **Category**: `deep`
  - **Skills**: []

  **Parallelization**:
  - **Can Run In Parallel**: NO
  - **Parallel Group**: Sequential (this is THE key learning task)
  - **Blocks**: Task 3.4 (CQRS read model needs events), Task 3.6 (E2E test)
  - **Blocked By**: Task 3.2

  **References**:
  - Outbox schema defined in Metis analysis above
  - Existing Kafka producer: `/mnt/c/Proyectos/learning/sample-microservices/task-management-saas/services/project-service/src/main/java/.../messaging/`

  **QA Scenarios**:
  ```
  Scenario: Outbox guarantees event delivery
    Tool: Bash (psql + kafka-console-consumer)
    Preconditions: Kafka running, procesion-service running
    Steps:
      1. POST /api/hermandades/{id}/procesiones → 201
      2. docker exec postgres-procesion psql -c "SELECT status FROM outbox_message ORDER BY created_at DESC LIMIT 1"
         → expect status=PENDING initially, then PUBLISHED after poller runs
      3. Wait 6 seconds (poller runs every 5s)
      4. docker exec kafka kafka-console-consumer --topic procesion-events --from-beginning --max-messages 1
         → expect JSON with eventType=ProcesionCreated
      5. docker exec postgres-procesion psql -c "SELECT status FROM outbox_message ORDER BY created_at DESC LIMIT 1"
         → expect status=PUBLISHED
    Expected Result: Event published to Kafka via outbox, not via direct Kafka call
    Evidence: .sisyphus/evidence/task-3.3-outbox-delivery.txt

  Scenario: Transaction atomicity (DB write + outbox in same transaction)
    Tool: Bash (psql)
    Preconditions: Simulate DB error mid-transaction
    Steps:
      1. Trigger a write that fails AFTER domain save but BEFORE outbox save (mock test)
      2. Verify neither the domain entity NOR the outbox message is persisted
    Expected Result: No partial state — atomic transaction
    Evidence: Test assertion in OutboxIntegrationTest.java
  ```

  **Acceptance Criteria**:
  - [ ] Integration test: `ProcesionCreated` event appears in `procesion-events` Kafka topic after `POST /procesiones`
  - [ ] Integration test: If Kafka is unavailable, outbox message stays `PENDING`, retried on next poll cycle
  - [ ] `SELECT count(*) FROM outbox_message WHERE status='FAILED'` = 0 in normal operation
  - [ ] `./gradlew :services:procesion-service:test` passes

  **Commit**: `feat(procesion): implement Outbox Pattern with @Scheduled poller and Kafka publishing`

---

- [ ] 3.4. CQRS read model and projection

  **What to do**:
  - Create read-side entities (separate from write-side — can be in same DB but clearly separated):
    - `ProcesionReadModel`: denormalized view of procesión for public consumption. Fields: procesionId, hermandadId, hermandadName, date, year, state, visibility, showSongs, rutaCheckpoints (JSON), trackedElements (JSON with crucetas + marchas if showSongs)
    - `TrackedElementReadModel`: current state per tracked element. Fields: id, procesionId, hermandadId, type, name, currentCrucetaIndex, currentMarchaId, currentMarchaTitle, lastUpdated
  - `ProcesionEventProjector`: Kafka consumer on `procesion-events` topic that updates read models:
    - `ProcesionCreated` → create `ProcesionReadModel` with state=DRAFT
    - `ProcesionStateChanged` → update state field
    - `CrucetaAssigned` → update trackedElements JSON
    - `MarchaOverridden` → update `TrackedElementReadModel.currentMarchaId`
  - Flyway migration: `V3__create_read_models.sql`
  - Integration test: write command → outbox publishes event → projector consumes → read model updated (eventual consistency test with `Awaitility`)

  **Must NOT do**:
  - NO Axon Framework — manual projection only
  - Read model is NOT normalized — it's intentionally denormalized for fast queries
  - Read model consumer is idempotent (same event processed twice = same result)

  **Recommended Agent Profile**:
  - **Category**: `deep`
  - **Skills**: []

  **Parallelization**:
  - **Can Run In Parallel**: NO
  - **Parallel Group**: Sequential
  - **Blocks**: Task 3.5 (read API)
  - **Blocked By**: Task 3.3

  **References**:
  - Existing Kafka consumer: `/mnt/c/Proyectos/learning/sample-microservices/task-management-saas/services/notification-service/src/main/java/.../listener/`
  - CQRS read model strategy defined in plan (denormalized JSON fields for fast public queries)

  **QA Scenarios**:
  ```
  Scenario: Read model updated after write
    Tool: Bash (psql + Awaitility in test)
    Preconditions: procesion-service running with Kafka
    Steps:
      1. POST /procesiones → 201 (write side creates procesión)
      2. Wait up to 10 seconds (Awaitility)
      3. SELECT state FROM procesion_read_model WHERE procesion_id = {id}
         → expect state=DRAFT
      4. PUT /procesiones/{id}/state → body: {newState: PLANNED} → 200
      5. Wait up to 10 seconds (Awaitility)
      6. SELECT state FROM procesion_read_model WHERE procesion_id = {id}
         → expect state=PLANNED
    Expected Result: Read model reflects write-side changes via event projection
    Evidence: .sisyphus/evidence/task-3.4-cqrs-projection.txt
  ```

  **Acceptance Criteria**:
  - [ ] Integration test with Awaitility: write → event → read model updated within 10s
  - [ ] Idempotency test: same event processed twice → read model unchanged (no duplicate data)
  - [ ] `./gradlew :services:procesion-service:test` passes

  **Commit**: `feat(procesion): add CQRS read model with Kafka event projection`

---

- [ ] 3.5. Procesión read API and Redis TTL cache

  **What to do**:
  - `ProcesionQueryController`: read-side endpoints:
    - `GET /api/hermandades/{hermandadId}/procesiones` — list procesiones (visibility-filtered)
    - `GET /api/hermandades/{hermandadId}/procesiones/{procesionId}` — full procesión detail with route + agenda (visibility-filtered)
    - `GET /api/procesiones/live` — all currently LIVE procesiones across all public hermandades (PUBLIC)
    - `GET /api/hermandades/{hermandadId}/procesiones/{procesionId}/pasos/{pasoId}/current-marcha` — current marcha for a Paso (members + public if showSongs)
  - Redis TTL cache for read model queries:
    - Cache key: `procesion:{hermandadId}:{procesionId}:public` → TTL 5 minutes
    - Implement manually (same pattern as Repertorio cache-aside): check Redis → miss → query read model DB → cache with TTL → return
    - Evict cache when `ProcesionStateChanged` event is consumed by projector

  **Must NOT do**:
  - Read endpoints serve from READ MODEL (not write-side DB)
  - Private hermandad → 404 for public users (NOT 403)
  - Songs hidden (showSongs=false) → return crucetas without marcha details

  **Recommended Agent Profile**:
  - **Category**: `unspecified-high`
  - **Skills**: []

  **Parallelization**:
  - **Can Run In Parallel**: NO
  - **Parallel Group**: Sequential
  - **Blocks**: Task 3.6, Phase 4 (Tracking needs to read procesión routes)
  - **Blocked By**: Task 3.4

  **QA Scenarios**:
  ```
  Scenario: Public procesión visible to anonymous
    Tool: Bash (curl)
    Steps:
      1. curl GET /api/hermandades/{publicHermandadId}/procesiones — no JWT
         → expect 200 with procesiones list
      2. curl GET /api/hermandades/{privateHermandadId}/procesiones — no JWT
         → expect 404
    Expected Result: Public hermandad returns 200, private returns 404 (not 403)
    Evidence: .sisyphus/evidence/task-3.5-visibility.txt

  Scenario: Redis TTL cache for public route
    Tool: Bash (redis-cli + curl)
    Steps:
      1. curl GET /api/hermandades/{id}/procesiones/{pId} → 200
      2. docker exec redis redis-cli GET "procesion:{hermandadId}:{procesionId}:public"
         → expect JSON (cached)
      3. docker exec redis redis-cli TTL "procesion:{hermandadId}:{procesionId}:public"
         → expect value between 1 and 300 (5 min TTL)
    Expected Result: Response cached in Redis with correct TTL after first query
    Evidence: .sisyphus/evidence/task-3.5-redis-ttl.txt
  ```

  **Acceptance Criteria**:
  - [ ] `GET /api/procesiones/live` without JWT → 200 with live procesiones from public hermandades
  - [ ] Redis TTL integration test: cache populated after first query, TTL set to ~300s
  - [ ] `./gradlew :services:procesion-service:test` passes

  **Commit**: `feat(procesion): add read-side query API with Redis TTL cache`

---

- [ ] 3.6. Procesión Service end-to-end integration test

  **What to do**:
  - Full lifecycle integration test using Testcontainers (PostgreSQL + Kafka + Redis):
    - Create procesión (DRAFT) → verify write model in DB
    - Add checkpoints, pasos, crucetas, marchas → verify persisted
    - Transition to PLANNED → verify outbox publishes `ProcesionStateChanged`
    - Verify read model updated via event projection (Awaitility, max 10s)
    - Transition to LIVE → verify state
    - Post live marcha override → verify `MarchaOverridden` event in outbox + Kafka
    - Verify `TrackedElementReadModel.currentMarchaId` updated
  - Eureka registration + health check

  **Recommended Agent Profile**:
  - **Category**: `deep`
  - **Skills**: []

  **Parallelization**:
  - **Can Run In Parallel**: NO
  - **Parallel Group**: Sequential (Phase 3 completion)
  - **Blocks**: Phase 4 start
  - **Blocked By**: Task 3.5

  **QA Scenarios**:
  ```
  Scenario: Full integration E2E lifecycle of Procesion Service
    Tool: Bash (./gradlew test with Testcontainers)
    Steps:
      1. ./gradlew :services:procesion-service:test → expect BUILD SUCCESSFUL, 0 failures
      2. Verify ProcesionLifecycleIntegrationTest PASSED (full DRAFT→PLANNED→LIVE→COMPLETED)
      3. Verify OutboxEndToEndTest PASSED (write→outbox→Kafka→read model)
      4. Verify TenantIsolationTest PASSED (cross-hermandad 404)
    Expected Result: All integration tests pass, outbox E2E verified
    Evidence: .sisyphus/evidence/task-3.6-e2e.txt
  ```

  **Acceptance Criteria**:

---

### PHASE 4 — Tracking Service

- [ ] 4.1. Tracking domain model and GPS position storage

  **What to do**:
  - Create Spring Boot project in `services/tracking-service`
  - Domain entities:
    - `TrackedElementSession`: id, procesionId, hermandadId, trackedElementId (references Paso/CruzDeGuia in Procesión Service), broadcasterUserId, isActive, startedAt, endedAt
    - `GpsBreadcrumb`: id, sessionId, **trackedElementId**, lat, lng, accuracy, timestamp — persisted for plan-vs-reality; `trackedElementId` column added for direct QA queries without session join
  - Flyway migrations (`V1__create_tracking_tables.sql` — must include `tracked_element_id` column on `gps_breadcrumb`)
  - `TrackingService`: concrete class
  - `TrackingController` (REST): temporary ingestion endpoint `POST /api/tracking/{procesionId}/{trackedElementId}/position` (HERMANDAD_ADMIN or BAND_DIRECTOR) — accepts `{lat, lng, accuracy}`, delegates to `TrackingService.recordPosition()`. This endpoint is superseded by the STOMP path in Task 4.2, but both can coexist; Task 4.2 adds STOMP as the production channel.
  - On startup, consumes `procesion-events` → `ProcesionStateChanged` (state=LIVE) → creates `TrackedElementSession` records
  - On `ProcesionStateChanged` (state=COMPLETED) → marks sessions inactive, sets endedAt
  - Redis write-through for current position:
    - On GPS update: save `GpsBreadcrumb` to DB AND write to Redis `tracking:{hermandadId}:{trackedElementId}:position`
    - Key TTL: 60 seconds (stale position detection)

  **Must NOT do**:
  - Tracking Service does NOT call Procesión Service via REST — it consumes events from Kafka
  - No complex GPS algorithms yet (that's Task 4.3)

  **Recommended Agent Profile**:
  - **Category**: `unspecified-high`
  - **Skills**: []

  **Parallelization**:
  - **Can Run In Parallel**: NO
  - **Parallel Group**: Sequential (Phase 4 start, after Phase 3 complete)
  - **Blocks**: Tasks 4.2, 4.3, 4.4
  - **Blocked By**: Task 3.6

  **QA Scenarios**:
  ```
  Scenario: GPS update persists to Redis and DB via REST endpoint
    Tool: Bash (redis-cli + psql + curl)
    Preconditions: tracking-service running, $PROCESION_ID and $TRACKED_ELEMENT_ID known (from Task 3.2 QA), $HERMANDAD_ID known, procesion in LIVE state (ProcesionStateChanged event consumed from Kafka — may need to manually publish a test event or use a running procesion-service)
    Steps:
      1. curl -s -X POST http://localhost:8084/api/tracking/$PROCESION_ID/$TRACKED_ELEMENT_ID/position \
           -H "Authorization: Bearer $TOKEN_BAND_DIRECTOR" \
           -H "Content-Type: application/json" \
           -d '{"lat":37.386,"lng":-5.993,"accuracy":5.0}'
         → expect 200
      2. docker exec redis redis-cli GET "tracking:$HERMANDAD_ID:$TRACKED_ELEMENT_ID:position"
         → expect JSON containing "lat":37.386 and "lng":-5.993
      3. docker exec postgres-tracking psql -U postgres -d tracking_db \
           -c "SELECT count(*) FROM gps_breadcrumb WHERE tracked_element_id='$TRACKED_ELEMENT_ID'"
         → expect count >= 1 (breadcrumb persisted; tracked_element_id column populated)
      4. Wait 65 seconds (TTL expiry)
      5. docker exec redis redis-cli GET "tracking:$HERMANDAD_ID:$TRACKED_ELEMENT_ID:position"
         → expect (nil) (TTL expired)
    Expected Result: Position in Redis (TTL 60s) AND persisted in DB for plan-vs-reality with tracked_element_id column queryable
    Failure Indicators: Step 2 returns nil immediately (write-through failed), Step 3 returns count=0 (not persisted), Step 3 fails with "column not found" (migration missing tracked_element_id)
    Evidence: .sisyphus/evidence/task-4.1-gps-redis.txt
  ```

  **Acceptance Criteria**:

---

- [ ] 4.2. WebSocket STOMP server and authentication

  **What to do**:
  - Add `spring-boot-starter-websocket` dependency
  - `WebSocketConfig`: configure STOMP over WebSocket at `/ws` endpoint with SockJS fallback
  - STOMP topic structure:
    - `/topic/procesion/{procesionId}/tracking` — all tracked elements positions (PUBLIC subscribers)
    - `/topic/procesion/{procesionId}/paso/{pasoId}/marcha` — current marcha for specific Paso (MUSICIAN subscribers)
  - JWT authentication on WebSocket handshake: `HandshakeInterceptor` extracts JWT from query param or `Authorization` header, validates with Keycloak public key
  - `TrackingController` (STOMP `@MessageMapping`):
    - `/app/tracking/{procesionId}/{trackedElementId}/position` — broadcaster sends GPS update
  - Spring Security WebSocket config (restrict `@MessageMapping` to authenticated users with BAND_DIRECTOR role)
  - Integration test: connect STOMP client, send message, verify received

  **Must NOT do**:
  - Do NOT use HTTP polling as fallback for real-time — STOMP only
  - No authentication bypass for any STOMP endpoint

  **Recommended Agent Profile**:
  - **Category**: `deep`
  - **Skills**: []

  **Parallelization**:
  - **Can Run In Parallel**: NO
  - **Parallel Group**: Sequential
  - **Blocks**: Task 4.3
  - **Blocked By**: Task 4.1

  **References**:
  - Spring WebSocket STOMP: https://docs.spring.io/spring-framework/docs/current/reference/html/web.html#websocket-stomp

  **QA Scenarios**:
  ```
  Scenario: STOMP connection with valid JWT
    Tool: Integration test (Spring STOMP test client)
    Steps:
      1. Connect STOMP client to ws://localhost:{port}/ws?token={validJwt}
      2. Subscribe to /topic/procesion/{id}/tracking
         → expect connection established (no error)
      3. Disconnect
    Expected Result: Valid JWT grants connection, subscription created without error
    Evidence: .sisyphus/evidence/task-4.2-stomp-connect.txt

  Scenario: STOMP GPS broadcast rejected without BAND_DIRECTOR role
    Tool: Integration test
    Steps:
      1. Connect with MUSICIAN JWT
      2. Send to /app/tracking/{id}/{eId}/position with {lat, lng}
         → expect error frame (ACCESS_DENIED or disconnect)
    Expected Result: MUSICIAN role cannot broadcast GPS — message rejected
    Evidence: .sisyphus/evidence/task-4.2-stomp-auth.txt
  ```

  **Acceptance Criteria**:
  - [ ] STOMP client connects with valid JWT → no error
  - [ ] STOMP client connects without JWT → connection refused
  - [ ] BAND_DIRECTOR sends GPS update → message received by subscriber
  - [ ] MUSICIAN sends GPS update → rejected

  **Commit**: `feat(tracking): add WebSocket STOMP server with JWT authentication`

---

- [ ] 4.3. Redis Pub/Sub fan-out and GPS cruceta matching

  **What to do**:
  - Redis Pub/Sub for WebSocket fan-out (multi-instance ready):
    - On GPS update: publish to Redis channel `tracking:{hermandadId}:{trackedElementId}`
    - `RedisMessageListener`: subscribes to all tracking channels, receives position updates, broadcasts to STOMP topic `/topic/procesion/{procesionId}/tracking`
    - This ensures WebSocket push works even with multiple Tracking Service instances
  - GPS → Cruceta matching algorithm:
    - Tracking Service caches the procesión route (from `ProcesionStateChanged` LIVE event payload)
    - `CrucetaMatcher.findCurrentCruceta(lat, lng, trackedElement)`:
      - Iterate ordered crucetas for this tracked element
      - For each cruceta, compute distance from current GPS point to the cruceta's start-end segment
      - Match to nearest cruceta within 30m radius
      - Speed validation: reject GPS updates implying speed > 5 km/h (procesiones walk 1-3 km/h)
      - Never go backwards: current cruceta index can only increase (unless reset by band director)
    - On cruceta change: push `currentMarcha` (first unplayed marcha in new cruceta) to `/topic/procesion/{procesionId}/paso/{pasoId}/marcha`
    - Publish `CrucetaEntered` event to `tracking-events` Kafka topic
  - **MusicianAlert consumer in Tracking Service** (completes the alert delivery path):
    - Tracking Service also consumes `tracking-events` topic (its own events + Notification Service's `MusicianAlert` events)
    - `MusicianAlertListener`: on `MusicianAlert` event → broadcast to STOMP topic `/topic/procesion/{procesionId}/paso/{pasoId}/marcha` with alert message
    - This way: GPS → CrucetaEntered → Notification Service → MusicianAlert → Tracking Service STOMP fan-out → musician's WebSocket client
    - Note: Tracking Service consuming its own `tracking-events` topic is intentional — it separates the Notification Service's enrichment logic from the delivery mechanism

  **Must NOT do**:
  - No complex ML or prediction — simple nearest-segment matching only
  - No backwards cruceta progression without explicit band director reset

  **Recommended Agent Profile**:
  - **Category**: `deep`
  - **Skills**: []

  **Parallelization**:
  - **Can Run In Parallel**: NO
  - **Parallel Group**: Sequential
  - **Blocks**: Task 4.4
  - **Blocked By**: Task 4.2

  **QA Scenarios**:
  ```
  Scenario: GPS position triggers correct cruceta and marcha
    Tool: Integration test (STOMP client + GPS simulator)
    Steps:
      1. Create procesión with 3 crucetas, each with 2 marchas
      2. Start LIVE procesión, connect STOMP subscriber to /topic/.../paso/{pasoId}/marcha
      3. Broadcast GPS coordinates at cruceta 1 start point
         → expect STOMP message: {currentMarcha: "marcha 1 of cruceta 1"}
      4. Broadcast GPS at cruceta 2 start point
         → expect STOMP message: {currentMarcha: "marcha 1 of cruceta 2"}
      5. Broadcast GPS going backwards (back to cruceta 1 coordinates)
         → expect: no change (backwards movement ignored)
    Expected Result: Cruceta transitions trigger correct marcha STOMP messages; backwards GPS ignored
    Evidence: .sisyphus/evidence/task-4.3-gps-matching.txt

  Scenario: Speed validation rejects GPS teleport
    Tool: Integration test
    Steps:
      1. Send GPS at position A
      2. Send GPS at position B 500m away within 1 second (implies 1800 km/h)
         → expect: GPS update ignored (logged as invalid)
    Expected Result: GPS update implying speed > 5 km/h is silently discarded, no cruceta change
    Evidence: .sisyphus/evidence/task-4.3-speed-validation.txt
  ```

  **Acceptance Criteria**:
  - [ ] `CrucetaMatcher` unit test: GPS at cruceta 1 start → returns crucetaIndex=0
  - [ ] `CrucetaMatcher` unit test: GPS 35m off route → returns no match (null)
  - [ ] `CrucetaMatcher` unit test: GPS implies speed > 5 km/h → returns null (rejected)
  - [ ] Integration test: GPS broadcast → STOMP subscriber receives marcha update within 1s
  - [ ] Integration test: `MusicianAlert` consumed from `tracking-events` → pushed to STOMP topic `/topic/procesion/{id}/paso/{pasoId}/marcha` within 1s

  **Commit**: `feat(tracking): add Redis Pub/Sub fan-out and GPS cruceta matching algorithm`

---

- [ ] 4.4. Tracking Service plan-vs-reality report

  **What to do**:
  - `PlanVsRealityService`: generates comparison report after procesión COMPLETED
  - Report structure per Paso:
    - Per cruceta: planned marchas (from `procesion-events` CrucetaAssigned) vs actual marchas played (from `tracking-events` + `MarchaOverridden` events stored during live)
    - Skipped marchas, added marchas, overridden marchas
  - `ReportController`:
    - `GET /api/hermandades/{hermandadId}/procesiones/{procesionId}/report` — plan vs reality (HERMANDAD_ADMIN + BAND_DIRECTOR only, state must be COMPLETED)
  - Tracking Service stores `ActualMarchaPlayed` records during LIVE mode (when marcha changes via cruceta matching or band director override)
  - Integration test verifying report accuracy

  **Recommended Agent Profile**:
  - **Category**: `unspecified-high`
  - **Skills**: []

  **Parallelization**:
  - **Can Run In Parallel**: NO
  - **Parallel Group**: Sequential (Phase 4 completion)
  - **Blocks**: Phase 5 start
  - **Blocked By**: Task 4.3

  **QA Scenarios**:
  ```
  Scenario: Plan-vs-reality report shows overrides correctly
    Tool: Bash (curl)
    Preconditions: tracking-service running, procesion COMPLETED with override recorded
    Steps:
      1. Simulate live mode: band director overrides marcha in cruceta 1
      2. Transition procesion to COMPLETED
      3. curl -H "Authorization: Bearer $ADMIN_JWT" \
           GET /api/hermandades/{id}/procesiones/{pId}/report
         → expect 200 with JSON report containing:
           - "planned": [{crucetaId, marchas: [marchaId1, marchaId2]}]
           - "actual": [{crucetaId, marchas: [marchaId1, overriddenMarchaId]}]
           - "overrides": [{crucetaId, plannedMarchaId, actualMarchaId, timestamp}]
      4. curl same endpoint with MUSICIAN JWT → expect 403
      5. curl on DRAFT procesion → expect 409 (not completed)
    Expected Result: Report correctly identifies planned vs actual with overrides
    Evidence: .sisyphus/evidence/task-4.4-plan-vs-reality.txt
  ```

  **Acceptance Criteria**:

---

### PHASE 5 — Notification Service

- [ ] 5.1. Notification Kafka consumers and musician alerts

  **What to do**:
  - Create Spring Boot project in `services/notification-service`
  - Kafka consumers:
    - `HermandadEventListener`: consumes `hermandad-events`
    - `ProcesionEventListener`: consumes `procesion-events`
    - `TrackingEventListener`: consumes `tracking-events`
  - DLQ configuration (same pattern as existing project): exponential backoff (3 retries, 2x backoff), failed messages → `{topic}.DLT`
  - Musician alerts via WebSocket (piggyback on Tracking Service's WebSocket — NOT a separate WebSocket server):
    - On `CrucetaEntered` event: publish notification to `tracking-events` topic with type `MusicianAlert` containing: `procesionId`, `pasoId`, `message` ("Próxima marcha: {title} en cruceta {name}")
    - Tracking Service's WebSocket fan-out handles delivery to `/topic/procesion/{id}/paso/{pasoId}/marcha`
  - EmbeddedKafka integration tests for all consumers

  **Must NOT do**:
  - No separate WebSocket server in Notification Service — reuse Tracking Service
  - No email/SMS/push notifications — WebSocket alerts only
  - No user preference management

  **Recommended Agent Profile**:
  - **Category**: `unspecified-high`
  - **Skills**: []

  **Parallelization**:
  - **Can Run In Parallel**: NO
  - **Parallel Group**: Sequential (Phase 5 start, after Phase 4 complete)
  - **Blocks**: Task 5.2
  - **Blocked By**: Task 4.4

  **References**:
  - DLQ pattern: `/mnt/c/Proyectos/learning/sample-microservices/task-management-saas/services/notification-service/src/main/java/.../KafkaConsumerConfig.java`

  **QA Scenarios**:
  ```
  Scenario: CrucetaEntered triggers MusicianAlert event
    Tool: Bash (EmbeddedKafka test via ./gradlew)
    Steps:
      1. ./gradlew :services:notification-service:test → expect BUILD SUCCESSFUL, 0 failures
      2. Verify CrucetaEnteredListenerTest PASSED:
         - CrucetaEntered event consumed → MusicianAlert published to tracking-events
         - Alert message contains "Próxima marcha: {title}"
      3. Verify DlqTest PASSED (poison message goes to .DLT after 3 retries)
    Expected Result: CrucetaEntered → MusicianAlert flow works, DLQ catches failures
    Evidence: .sisyphus/evidence/task-5.1-notification-tests.txt
  ```

  **Acceptance Criteria**:

---

- [ ] 5.2. ELK Stack structured logging

  **What to do**:
  - Add `logstash-logback-encoder` dependency to ALL 5 services
  - Configure `logback-spring.xml` in each service to output JSON to Logstash (via TCP appender to `logstash:5044` — the Docker Compose service name, NOT `localhost` — when `full` Docker profile active) and console (always). Use Spring profile-based configuration: `<springProfile name="docker-full">` block targets `logstash:5044`; `<springProfile name="!docker-full">` outputs to console only
  - Add structured log fields to all services: `hermandadId`, `procesionId`, `pasoId`, `service`, `traceId` (Zipkin correlation)
  - Add meaningful log events:
    - Hermandad Service: hermandad registered, member added
    - Repertorio Service: cache hit/miss, marcha created
    - Procesión Service: state transition, outbox published, projection updated
    - Tracking Service: GPS received, cruceta matched, STOMP pushed
    - Notification Service: event consumed, alert sent, DLT message
  - Create Kibana index pattern `semana-santa-*` and basic dashboard (3-4 log search panels)

  **Must NOT do**:
  - No logging of JWT tokens or sensitive data
  - No logging of GPS coordinates in plain text (privacy) — log `{pasoId, crucetaIndex}` instead

  **Recommended Agent Profile**:
  - **Category**: `unspecified-high`
  - **Skills**: []

  **Parallelization**:
  - **Can Run In Parallel**: NO
  - **Parallel Group**: Sequential
  - **Blocks**: Phase 6
  - **Blocked By**: Task 5.1

  **QA Scenarios**:
  ```
  Scenario: Structured logs appear in Kibana
    Tool: Bash (curl Elasticsearch + docker-compose full profile)
    Preconditions: docker-compose --profile core --profile full up
    Steps:
      1. POST /api/hermandades → register hermandad
      2. Wait 10 seconds (Logstash ingestion)
      3. curl http://localhost:9200/semana-santa-*/_search?q=eventType:HermandadRegistered
         → expect hits > 0
      4. curl http://localhost:5601 → Kibana UI loads (200)
    Expected Result: Structured JSON log from hermandad-service ingested into Elasticsearch within 15s; searchable by eventType field
    Evidence: .sisyphus/evidence/task-5.2-elk-logs.txt
  ```

  **Acceptance Criteria**:
  - [ ] `docker-compose --profile full up` + POST /api/hermandades → log appears in Elasticsearch within 15s
  - [ ] Kibana index pattern `semana-santa-*` shows logs from all 5 services
  - [ ] Zipkin traceId correlates across services in Kibana

  **Commit**: `feat(notification): add ELK structured logging across all services`

---

### PHASE 6 — Integration & Polish

- [ ] 6.1. Full procesión lifecycle end-to-end test

  **What to do**:
  - Write a full E2E integration test (Testcontainers full stack: PostgreSQL ×5 + Kafka + Redis):
    - Register hermandad → add members (admin, band director, musician)
    - Add marchas to repertoire (hermandad-specific)
    - Create procesión → add Cruz de Guía + 2 Pasos → add checkpoints → add crucetas with marchas → set agenda
    - Transition: DRAFT → PLANNED → LIVE
    - Connect STOMP subscriber (musician role) to Paso 1 marcha topic
    - Simulate GPS broadcast for Paso 1 (3 positions across 2 crucetas)
    - Verify STOMP subscriber receives marcha updates for each cruceta transition
    - Band director overrides marcha in cruceta 2
    - Transition: LIVE → COMPLETED
    - GET plan-vs-reality report → verify override recorded
    - Verify tenant isolation throughout (hermandad B user cannot interfere)

  **Recommended Agent Profile**:
  - **Category**: `deep`
  - **Skills**: []

  **Parallelization**:
  - **Can Run In Parallel**: NO (final integration phase)
  - **Parallel Group**: Sequential
  - **Blocks**: Final Verification Wave
  - **Blocked By**: Task 5.2

  **QA Scenarios**:
  ```
  Scenario: Full procesion lifecycle end-to-end test passes
    Tool: Bash (./gradlew test with Testcontainers full stack)
    Steps:
      1. ./gradlew :services:tracking-service:test -Dtest=FullProcesionLifecycleE2ETest
         → expect BUILD SUCCESSFUL, 0 failures
      2. Verify test covers: register → plan → LIVE → GPS broadcast → STOMP receives marcha → override → COMPLETED → report
      3. Verify cross-tenant isolation test PASSED
      4. Verify STOMP subscriber received correct marcha transitions for GPS positions
    Expected Result: Complete E2E lifecycle passes with real containers (no mocks)
    Evidence: .sisyphus/evidence/task-6.1-e2e-lifecycle.txt
  ```

  **Acceptance Criteria**:

---

- [ ] 6.2. Observability and README

  **What to do**:
  - Configure Prometheus scrape targets for all 5 services (Spring Actuator `/actuator/prometheus` endpoint)
  - Create Grafana dashboard with:
    - Request rate per service (from Prometheus)
    - Outbox pending messages count (custom metric via `MeterRegistry`)
    - Active WebSocket sessions count (custom metric)
    - Redis cache hit ratio (custom metric)
  - Add custom Spring Actuator metrics:
    - `hermandad.registrations.total` counter (increment in `HermandadService.register()`) — verifies Hermandad service metrics work
    - `outbox.messages.pending` gauge
    - `tracking.websocket.sessions.active` gauge
    - `repertorio.cache.hits` / `repertorio.cache.misses` counters
  - Write `README.md` documenting:
    - Architecture diagram (ASCII, like existing project)
    - Domain glossary (Spanish terms)
    - How to run (core vs full profiles)
    - Learning goals achieved and patterns implemented

  **Recommended Agent Profile**:
  - **Category**: `unspecified-high`
  - **Skills**: []

  **Parallelization**:
  - **Can Run In Parallel**: YES (with 6.1 — no code dependency)
  - **Parallel Group**: Wave 6
  - **Blocks**: Final Verification Wave
  - **Blocked By**: Task 5.2

  **QA Scenarios**:
  ```
  Scenario: Custom metrics appear in Prometheus and Grafana
    Tool: Bash (curl)
    Preconditions: docker-compose full profile running (includes Prometheus at localhost:9090), all 5 services running (see Service Port Map)
    Steps:
      1. curl http://localhost:8081/actuator/prometheus | grep hermandad_registrations_total
         → expect metric line present (hermandad-service on port 8081; this counter increments on each POST /api/hermandades)
      2. curl http://localhost:8083/actuator/prometheus | grep outbox_messages_pending
         → expect gauge metric present (procesion-service on port 8083)
      3. curl http://localhost:8084/actuator/prometheus | grep websocket_sessions
         → expect gauge metric present (tracking-service on port 8084)
      4. curl "http://localhost:9090/api/v1/query?query=outbox_messages_pending" → expect JSON with non-empty result.data.result array
    Expected Result: Custom metrics scraped by Prometheus, queryable via API
    Failure Indicators: Empty grep output, 404 from actuator, empty result array in Prometheus query
    Evidence: .sisyphus/evidence/task-6.2-observability.txt
  ```

  **Acceptance Criteria**:

---

## Final Verification Wave

> 4 review agents run in PARALLEL after ALL implementation tasks complete.
> ALL must APPROVE. Present consolidated results and get explicit user okay before marking complete.

- [ ] F1. **Plan Compliance Audit** — `oracle`
  Read the plan end-to-end. For each "Must Have": verify implementation exists (read file, curl endpoint, run command). For each "Must NOT Have": search codebase for forbidden patterns (Axon, Debezium, Hexagonal layers, shared DBs) — reject with file:line if found. Check evidence files exist in .sisyphus/evidence/. Compare deliverables against plan.
  Output: `Must Have [N/N] | Must NOT Have [N/N] | Tasks [N/N] | VERDICT: APPROVE/REJECT`

- [ ] F2. **Code Quality Review** — `unspecified-high`
  Run `./gradlew test` (all services). Review all service source for: `as Object`/unchecked casts, empty catches swallowing exceptions, System.out.println in production code, commented-out code blocks, unused imports. Check for AI slop: excessive Javadoc on trivial methods, over-abstraction (interfaces for single implementations), generic names (data/result/item/temp/util).
  Output: `Build [PASS/FAIL] | Tests [N pass/N fail] | Services clean [N/5] | Issues found | VERDICT`

- [ ] F3. **Real Manual QA** — `unspecified-high`
  Start from clean Docker state (`docker-compose --profile core down -v && up`). Execute EVERY QA scenario from EVERY task — follow exact steps, capture evidence. Test the full procesión lifecycle end-to-end (register → plan → live → GPS broadcast → receive marcha via WebSocket). Test tenant isolation (cross-tenant 404). Test band director live override. Save evidence to `.sisyphus/evidence/final-qa/`.
  Output: `Scenarios [N/N pass] | Lifecycle E2E [PASS/FAIL] | Tenant isolation [PASS/FAIL] | VERDICT`

- [ ] F4. **Scope Fidelity Check** — `deep`
  For each task: read "What to do", compare against actual implementation (git log/diff). Verify 1:1 — nothing missing, nothing beyond scope. Explicitly check: no Axon imports, no Hexagonal packages (port/adapter), no shared DB connections, no frontend code. Flag any unplanned files or dependencies added.
  Output: `Tasks [N/N compliant] | Forbidden patterns [CLEAN/N found] | Unaccounted files [CLEAN/N] | VERDICT`

---

## Commit Strategy

### Convention
```
<type>(<scope>): <description>

Types:  feat | fix | refactor | test | infra | docs
Scopes: infra | shared | hermandad | repertorio | procesion | tracking | notification
```

### Atomic Commit Rule
Every commit must be GREEN: compiles, tests pass, application starts.
Never commit red tests. Never commit code that doesn't compile.

### Commit Sequence Per Phase (see individual tasks for granular commits)

**PHASE 0**: infra → shared (~8 commits)
**PHASE 1**: hermandad domain → API → security → Kafka → tests (~10 commits)
**PHASE 2**: repertorio domain → API → Redis → tests (~8 commits)
**PHASE 3**: procesion domain → state machine → outbox → CQRS read model → API → tests (~16 commits)
**PHASE 4**: tracking domain → WebSocket → Redis → GPS matching → tests (~12 commits)
**PHASE 5**: notification consumer → alerts → ELK → tests (~7 commits)
**PHASE 6**: E2E tests → report → observability → docs (~6 commits)

---

## Success Criteria

### Verification Commands
```bash
# Full stack starts
docker-compose --profile core up -d
sleep 30
curl http://localhost:8761  # Eureka dashboard → 200

# All tests pass
./gradlew test
# Expected: BUILD SUCCESSFUL, 0 failures

# Hermandad registration
curl -X POST http://localhost:8080/api/hermandades \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"Hermandad de la Macarena","city":"Sevilla","visibility":"PUBLIC"}'
# Expected: 201 Created with hermandadId

# Procesión created in DRAFT state
curl http://localhost:8080/api/hermandades/{id}/procesiones \
  -H "Authorization: Bearer $TOKEN"
# Expected: 200 with procesiones array, state=DRAFT

# GPS broadcast received and in Redis
docker exec redis redis-cli GET "tracking:{hermandadId}:{pasoId}:position"
# Expected: JSON with lat, lng, timestamp

# Outbox processed
docker exec postgres psql -U postgres -c \
  "SELECT count(*) FROM outbox_message WHERE status='PUBLISHED'"
# Expected: count > 0

# Tenant isolation
curl http://localhost:8080/api/hermandades/{otherHermandadId}/procesiones \
  -H "Authorization: Bearer $TOKEN_HERMANDAD_A"
# Expected: 404 Not Found
```

### Final Checklist
- [ ] All 5 services start and register with Eureka
- [ ] All "Must Have" patterns implemented and verifiable
- [ ] All "Must NOT Have" patterns absent (verified by code search)
- [ ] All integration tests pass with Testcontainers
- [ ] Outbox: write → Kafka publish end-to-end verified
- [ ] CQRS: write model → event → read model projection verified
- [ ] WebSocket: GPS broadcast → STOMP subscriber receives marcha update verified
- [ ] Redis: cache-aside hit/miss verified, write-through freshness verified, TTL expiry verified, Pub/Sub fan-out verified
- [ ] RBAC: all 4 roles enforced — unauthorized actions return 403/404
- [ ] Multi-tenancy: cross-tenant access returns 404
- [ ] ELK: structured logs appear in Kibana
