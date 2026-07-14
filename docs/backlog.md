# Backlog — Semana Santa App

Process:
1. **Sprint planning** — pick backlog items, define acceptance criteria, break into tasks
2. **Implementation** — TDD (RED → GREEN → refactor), domain-first, vertical slices. Every feature needs a test before code.
3. **Commit** — one commit per completed story (acceptance criteria met, all green). Descriptive conventional commit messages in English.
4. **Sprint review** — mark done, decide next sprint

---

## Current Sprint

### Sprint 3 — Spring Boot 4.1 Migration ✅

Upgrade platform to Boot 4.1. Bump versions, migrate modular starters, Jackson 3 packages, Redis serializers.

- ~~Version catalog: Spring Boot 4.1.0, Spring Cloud 2025.0.0, tools.jackson~~ ✅
- ~~Build files: all services migrated to Boot 4.1 / tools.jackson~~ ✅
- ~~Jackson imports: RedisConfig, shared/common, test factories migrated~~ ✅
- ~~Test packages: autoconfigure paths, @MockitoBean, jakarta.persistence~~ ✅
- ~~TestCacheConfig: fix slice test CacheManager failures~~ ✅
- ~~Flyway V4 + service-level unique name constraint on Hermandad~~ ✅
- ~~HermandadAlreadyExistsException → 409 CONFLICT~~ ✅

---

### Sprint 4 — Hermandad Service Hardening ✅

Complete the hermandad service: auth, tests, missing fields, and outbox quality.

1. **Auth enforcement** ✅
   - ~~**`JwtAuthenticationConverter`**: new class. Extracts `hermandad_memberships` claim from JWT, creates `GrantedAuthority` in format `HERMANDAD_{hermandadId}_{role}`. Also captures JWT `sub` as the authenticated user ID for auto-assign.~~ ✅
   - ~~**`SecurityConfig`**: wire the converter. `POST /api/hermandades` → any authenticated user (bootstrap). `GET /api/hermandades/{id}` → public. Everything else → authenticated + `@PreAuthorize`.~~ ✅
   - ~~**`HermandadController`**: `@PreAuthorize` on member management. Bootstrap endpoint stays open.~~ ✅
   - ~~**`HermandadService.createHermandad()`**: accept creator userId (from JWT `sub`) for auto-assign (ties into item 4).~~ ✅
2. **Integration tests** — JPA repositories against real PostgreSQL ✅
   - ~~`HermandadRepositoryIntegrationTest` — saves/finds/constraints via running Postgres~~ ✅
   - ~~Skips automatically if no Postgres available (graceful dev/CI fallback)~~ ✅
3. **Missing entity fields** — `description` (added, nullable TEXT) ✅
   - ~~`V5__add_description_to_hermandad.sql`~~ ✅
   - ~~`CreateHermandadRequest`, `HermandadResponse`, constructor, service, tests~~ ✅
4. **Auto-assign creator as HERMANDAD_ADMIN** on `POST /api/hermandades` ✅
   - ~~Creator saved as `HERMANDAD_ADMIN` in `hermandad_member` on create~~ ✅
   - ~~`HermandadSecurityService` dual-path auth: JWT authorities (fast) → DB membership (fallback)~~ ✅
5. **Outbox quality** — `ORDER BY created_at` + batch size limit (100) ✅
   - ~~`findTop100ByProcessedFalseOrderByCreatedAtAsc()` query~~ ✅
    - ~~Poller uses `ORDER BY created_at ASC`, capped at 100~~ ✅

---

### Sprint 5 — Hermandad Polish + Idempotent Consumer ✅

Complete remaining hermandad-service gaps and build the idempotent Kafka consumer reference pattern.

1. **`MemberAddedEvent` add `hermandadId`** ✅
   - ~~Add `UUID hermandadId` field to `MemberAddedEvent` record~~ ✅
   - ~~Populate it when publishing from `HermandadService.addMember()`~~ ✅
   - ~~Update `HermandadServiceTest`~~ ✅
   - **AC**: Event payload includes hermandadId; Kafka consumers get tenant context without cross-service lookup

2. **MockMvc + Exception handler tests** ✅
   - ~~Fill gaps in `HermandadControllerTest` (validation 400, conflict 409, response body checks for success paths)~~ ✅
   - ~~Add `GlobalExceptionHandlerTest` for each handler (HermandadNotFoundException → 404, validation → 400, conflict → 409, AccessDenied → 403, generic → 500)~~ ✅
   - **AC**: All exception handlers have a test proving correct status code + body format

3. **Idempotent Kafka consumer (reference implementation)** ✅
   - ~~`V6__create_processed_event_table.sql`: `processed_event` table with `(event_id UUID PK, consumer_name VARCHAR(100), processed_at TIMESTAMP)`~~ ✅
   - ~~Consumer bean listens to `hermandad-events` and `hermandad-member-events`~~ ✅
   - ~~Checks `processed_event` table before processing; skips if already processed; stores event_id + consumer_name + timestamp if new~~ ✅
   - ~~Register consumer group for offset tracking~~ ✅
   - **AC**: Duplicate Kafka messages are silently skipped; each unique event is processed exactly once; pattern is copy-paste ready for other services

---

### Sprint 2 — Audit Fixes & Member Removal ✅

Clean technical debt, add missing error handlers, and complete member CRUD.

---

#### ~~1. Audit fixes~~ ✅

- ~~Fix `HermandadMember.updatedAt`~~ ✅
- ~~Add `DataIntegrityViolationException` handler → 409~~ ✅
- ~~Add `MethodArgumentNotValidException` handler → 400~~ ✅
- ~~Add generic `Exception` fallback → 500~~ ✅
- ~~Bump outbox `payload` column from `VARCHAR(255)` to `TEXT`~~ ✅

---

#### 2. Member removal (🔄 in progress)

**As a** API client, **I want** to remove a member via `DELETE /api/hermandades/{hermandadId}/members/{userId}`, **so that** I can undo member additions.

**Done:**
- ~~`MemberRemovedEvent` domain event~~ ✅
- ~~`HermandadMemberRepository.delete()` port + adapter~~ ✅
- ~~`HermandadService.removeMember()` + service test~~ ✅

**All done:**
- ~~`DELETE` endpoint in controller~~ ✅
- ~~MockMvc test~~ ✅
- ~~Sync OpenAPI spec~~ ✅

---

### Sprint 1 — Complete ✅

All stories implemented. E2E verified except duplicate member (handled in Sprint 2 audit fixes).

---

### Sprint 6 — Hermandad Service Polish ✅

Complete remaining hermandad-service gaps: pagination, CAPATAZ role in OpenAPI, Keycloak validation.

1. **Members list pagination** ✅
   - ~~Port, JPA repo, adapter, service, controller updated with `Pageable`/`Page`~~ ✅
   - ~~`@PageableDefault(size = 20)` on controller, `@Cacheable` removed from paginated method~~ ✅
   - ~~Service test + integration test fix~~ ✅
   - **AC**: `GET /api/hermandades/{id}/members?page=0&size=20` returns paginated response with Spring Data `Page` structure

2. **CAPATAZ role in OpenAPI spec** ✅
   - ~~Already present in `AddMemberRequest.role`, `HermandadMember.role`, `ChangeRoleRequest.role` enums~~ ✅
   - **AC**: OpenAPI spec reflects all domain roles including CAPATAZ

3. **Keycloak user existence validation** ✅
   - ~~`UserExistencePort` interface with `exists(String userId)`~~ ✅
   - ~~`KeycloakUserExistenceAdapter` calling `users().get(userId).toRepresentation()`~~ ✅
   - ~~Check in `HermandadService.addMember()` → `IllegalArgumentException` → 400~~ ✅
   - **AC**: Adding a member with a non-existent Keycloak user returns 400; existing users pass through

4. **Integration test for pagination (bonus)** ✅
   - ~~`HermandadControllerIntegrationTest` with Testcontainers PostgreSQL pattern~~ ✅
   - ~~3 scenarios: paginated with size=2, size=5, default size=20~~ ✅
   - **AC**: Integration test verifies pagination structure end-to-end

---

### Sprint 7 — MVP Foundation: Member Removal + Procesión Service ✅

Build the remaining hermandad-service feature (member removal), bootstrap the Procesión service, and apply polish across both services.

**Implementation plan:** `docs/plans/2026-07-10-sprint-07-mvp-foundation.md`

**Completed:**
1. **Member Removal (Hermandad Service)** ✅
2. **Procesión Service — Project Skeleton** ✅
3. **Procesión Service — Domain Aggregate + Repository Port** ✅
4. **Procesión Service — JPA Adapter + Flyway** ✅
5. **Procesión Service — Service Layer + Outbox** ✅
6. **Procesión Service — REST Controller + Auth** ✅
7. **Docker Compose — Procesión Service + DB** ✅
8. **API Gateway — Procesión Routes** ✅
9. **Spanish→English refactor** — Procesión internal API anglicized ✅
10. **Procesión outbox pattern** — events now reach Kafka via outbox table + poller ✅
11. **Structured error responses (both services)** — `ApiError` JSON replacing plain text ✅
12. **Catch block narrowing** — Keycloak adapters, listeners, outbox ✅
13. **Hermandad entity `updatedAt`** — added + Flyway V7 ✅
14. **`@Transactional` on Hermandad write methods** — `addMember()`, `changeRole()`, `removeMember()` ✅
15. **Procesión domain unit tests** — 11 state machine tests ✅
16. **Procesión integration tests** — 4 repository + 8 controller integration tests ✅
17. **Spring Boot 4.1 compilation** — both services compile on 4.1 with tools.jackson ✅

**Deferred:**
- Audit rename (`keycloak_group_id` → `keycloak_group_id_refs`)
- Shared lib unit tests
- Outbox→Kafka integration test (EmbeddedKafka)
- Repertorio Service, Tracking Service, Notification Service

---

### Sprint 9 — Repertorio Service

> Build the final MVP service: global marcha catalog + cruceta (ordered marcha list per procession).
> **Package**: `com.repertorio.marcha` · **Port**: 8083 · **DB**: `postgres-repertorio:5433/repertorio_db`

---

#### TASK-1: Project Scaffold

**Description:** Create the repertorio-service project skeleton — Gradle build with Spring Boot 4.1, app main class with `@EnableDiscoveryClient` + `@EnableScheduling` + `@EnableMethodSecurity`, config files (application.yml, bootstrap.yml), and module registration.

**Acceptance Criteria:**
- [ ] `build.gradle.kts` has correct plugins, dependencies, and version (Spring Boot 4.1, Spring Cloud 2025.1.2, Java 21)
- [ ] `RepertorioServiceApplication.java` compiles with `@EnableDiscoveryClient`, `@EnableScheduling`, `@EnableMethodSecurity`
- [ ] `application.yml` configures Postgres (port 5433), Flyway (`ddl-auto: validate`), Kafka (`localhost:9092`), server port 8083, Eureka
- [ ] `bootstrap.yml` sets `spring.application.name: repertorio-service`
- [ ] `./gradlew :services:repertorio-service:compileJava` passes

**Technical Notes:** 
- Copy dep list from procesion-service (same stack: webmvc, data-jpa, flyway, kafka, security, oauth2, eureka, actuator, lombok, springdoc, test deps)
- Do NOT include `spring-boot-starter-web` (use modular `spring-boot-starter-webmvc`)
- Bring `spring-cloud-starter-netflix-eureka-client` for service discovery registration
- Version catalog already has all needed versions from root BOM — no new entries

**Effort:** 4 files · **Dependencies:** None

---

#### TASK-2: Domain Model — Marcha Aggregate

**Description:** Implement the Marcha aggregate — the core domain entity representing a Semana Santa musical march (marcha procesional). Includes the `BandType` enum (BANDA_PALIO, AGRUPACION_MUSICAL, BANDA_CORNETAS), domain events, and validation.

**Acceptance Criteria:**
- [ ] `BandType` enum with 3 values matching Semana Santa band types
- [ ] `Marcha` entity with: id, title, composer, bandType, durationSeconds, compositionYear, youtubeUrl, createdAt, updatedAt
- [ ] Constructor validates: blank title/composer throws, negative duration throws
- [ ] Static factory or constructor-based creation (follow Procesion pattern)
- [ ] `MarchaNotFoundException` — runtime exception with message including ID
- [ ] `MarchaAddedEvent` / `MarchaRemovedEvent` — domain event records implementing `DomainEvent` interface
- [ ] `DomainEvent` marker interface: eventId, occurredAt, aggregateType, aggregateId
- [ ] `./gradlew :services:repertorio-service:compileJava` passes

**Technical Notes:**
- Domain must be pure Java — no JPA, no Spring annotations
- `compositionYear` is `Integer` (nullable, not all marchas have known year)
- `youtubeUrl` is `String` (nullable)
- Event topic naming: `aggregateType()` returns `"marcha"` for Kafka topic routing
- Match the package structure: `com.repertorio.marcha.domain.model/` and `com.repertorio.marcha.domain.event/`

**Effort:** 6 files · **Dependencies:** TASK-1

---

#### TASK-3: Domain Model — Cruceta Aggregate

**Description:** Implement the Cruceta aggregate — an ordered list of marchas assigned to a specific procesion. A cruceta represents what a band will play during a procession and in what order. Includes CrucetaItem value object and validation rules.

**Acceptance Criteria:**
- [ ] `Cruceta` entity with: id, procesionId, items list, createdAt, updatedAt
- [ ] `CrucetaItem` value object: id, marchaId, orderIndex, notes (nullable)
- [ ] Constructor validates: null items throws, duplicate orderIndex values throws
- [ ] `redefine(List<CrucetaItem>)` method replaces all items and increments updatedAt
- [ ] `containsMarcha(UUID marchaId)` — returns true if any item references that marcha
- [ ] `CrucetaNotFoundException` — runtime exception with message including procesionId
- [ ] `CrucetaDefinedEvent` — domain event record implementing `DomainEvent` interface
- [ ] `./gradlew :services:repertorio-service:compileJava` passes

**Technical Notes:**
- Cruceta is 1:1 with procesion — one procesion has exactly one cruceta
- `redefine()` replaces the entire marcha list (not incremental add/remove)
- `orderIndex` is 0-based: 0 = first marcha to play
- Use `List.copyOf()` in getter to maintain immutability
- Event topic: `aggregateType()` returns `"cruceta"`

**Effort:** 4 files · **Dependencies:** TASK-1

---

#### TASK-4: Database Migrations (Flyway)

**Description:** Create 4 Flyway migrations: V1 (marcha table), V2 (cruceta + cruceta_item tables with FK + unique constraints), V3 (seed 15 iconic Semana Santa marchas), and the outbox table V4. All migrations must be idempotent and match the domain model.

**Acceptance Criteria:**
- [ ] `V1__create_marcha_table.sql`: marcha table with UUID PK, title, composer, band_type (VARCHAR), duration_seconds, composition_year (nullable), youtube_url (nullable), created_at, updated_at. Indexes on band_type and composer.
- [ ] `V2__create_cruceta_tables.sql`: cruceta table (id, procesion_id UNIQUE, timestamps) + cruceta_item table (id, cruceta_id FK CASCADE, marcha_id, order_index, UNIQUE(cruceta_id, order_index)). Indexes on cruceta_id and marcha_id.
- [ ] `V3__seed_global_marchas.sql`: INSERT 15 iconic marchas with deterministic UUIDs (a0000001-...-01 through 15), diverse band types, real composers and durations
- [ ] `V4__create_outbox_table.sql`: outbox_event table matching procesion-service pattern
- [ ] `./gradlew :services:repertorio-service:flywayMigrate` passes (or migrations classpath is valid)

**Technical Notes:**
- Use hardcoded UUIDs for seed marchas so they're referenceable across environments
- All 4 migrations are independent of each other (V1-V4 can run in sequence)
- Use `TIMESTAMP WITH TIME ZONE` for all timestamps, `VARCHAR(30)` for band_type
- `ON DELETE CASCADE` on cruceta_item FK — deleting a cruceta removes all its items
- Match the exact same outbox table schema from procesion-service V3

**Effort:** 4 files · **Dependencies:** None (can run parallel to TASK-1)

---

#### TASK-5: JPA Entities + Repository Adapters

**Description:** Implement the persistence layer — JPA entity classes mirroring the DB tables, Spring Data repositories, domain port interfaces, and adapter classes that map between domain entities and JPA entities. This bridges hexagonal architecture's domain and infrastructure.

**Acceptance Criteria:**
- [ ] `MarchaEntity` — JPA entity with `@Table(name = "marcha")`, `@Enumerated(STRING)` for bandType
- [ ] `CrucetaEntity` + `CrucetaItemEntity` — JPA entities with `@OneToMany(cascade = ALL)` for items
- [ ] `MarchaJpaRepository` — extends `JpaRepository<MarchaEntity, UUID>`
- [ ] `CrucetaJpaRepository` — extends `JpaRepository<CrucetaEntity, UUID>`, has `findByProcesionId(UUID)`
- [ ] `MarchaRepository` (domain port interface) — `findAll()`, `findById()`, `save()`, `deleteById()`, `existsById()`
- [ ] `CrucetaRepository` (domain port interface) — `findByProcesionId()`, `save()`, `deleteByProcesionId()`
- [ ] `MarchaRepositoryAdapter` — implements `MarchaRepository`, maps between `Marcha` ↔ `MarchaEntity`
- [ ] `CrucetaRepositoryAdapter` — implements `CrucetaRepository`, maps between `Cruceta` ↔ `CrucetaEntity` (including items)
- [ ] `./gradlew :services:repertorio-service:compileJava` passes

**Technical Notes:**
- Domain entities (`Marcha`, `Cruceta`) are NOT JPA-annotated — keep domain pure
- JPA entities are package-private (adapter layer only)
- Mapping logic in adapters: `toDomain()`, `toEntity()` methods
- `CrucetaItemEntity` uses `@ManyToOne` back-reference to `CrucetaEntity` with `@JsonIgnore`
- Use `@UuidGenerator` on JPA entity IDs (NOT `@GeneratedValue` — matches procesion fix)
- No `@DynamicUpdate` or Hibernate-specific optimizations for MVP

**Effort:** 8 files · **Dependencies:** TASK-1, TASK-4

---

#### TASK-6: Application Services

**Description:** Implement the application service layer — `MarchaService` for CRUD operations on the global marcha catalog (with delete validation checking cruceta references), and `CrucetaService` for managing per-procession marcha lists. Both services publish domain events.

**Acceptance Criteria:**
- [ ] `MarchaService.listMarchas(bandType?, composer?, query?)` — filter by optional params, no pagination for MVP
- [ ] `MarchaService.getMarcha(id)` — returns marcha or throws MarchaNotFoundException
- [ ] `MarchaService.createMarcha(fields)` — creates, saves, publishes MarchaAddedEvent
- [ ] `MarchaService.updateMarcha(id, fields)` — updates, saves, does NOT publish event (no update event for MVP)
- [ ] `MarchaService.deleteMarcha(id)` — checks no cruceta references it, deletes, publishes MarchaRemovedEvent
- [ ] `CrucetaService.getCruceta(procesionId)` — returns cruceta or throws CrucetaNotFoundException
- [ ] `CrucetaService.defineCruceta(procesionId, items)` — creates or replaces cruceta, publishes CrucetaDefinedEvent
- [ ] Integration: `./gradlew :services:repertorio-service:compileJava` passes

**Technical Notes:**
- Delete validation: inject `CrucetaRepository`, call `findByProcesionId(procesionId)` for all processions... wait — for MVP, simpler: CrucetaRepository has `existsByMarchaId()` or check all crucetas. Actually simplest approach: `crucetaRepository.listAll().stream().anyMatch(c -> c.containsMarcha(marchaId))`. `ponytail`: full-scan, add DB index if perf matters.
- Use `@Transactional` on write methods
- Both services use `DomainEventPublisher` port (not direct Kafka) — events go through outbox
- `createMarcha` uses the static factory or constructor from TASK-2

**Effort:** 2 files · **Dependencies:** TASK-5

---

#### TASK-7: REST Controllers + Security + DTOs

**Description:** Implement the HTTP layer — REST controllers for Marcha (global catalog) and Cruceta (per-procession), request/response DTOs, security configuration (JWT auth with `@PreAuthorize`), custom `JwtAuthenticationConverter`, `RepertorioSecurityService`, `GlobalExceptionHandler`, and `OpenApiConfig` for Swagger.

**Acceptance Criteria:**
- [ ] `MarchaController` — `GET /api/marchas` (public), `GET /api/marchas/{id}` (public), `POST /api/marchas` (auth'd), `PUT /api/marchas/{id}` (auth'd), `DELETE /api/marchas/{id}` (auth'd)
- [ ] `CrucetaController` — `GET /api/hermandades/{hid}/procesiones/{pid}/cruceta` (public), `PUT /api/hermandades/{hid}/procesiones/{pid}/cruceta` (HERMANDAD_ADMIN only)
- [ ] `MarchaRequest`/`MarchaResponse` DTOs — match Marcha fields, validation annotations on request
- [ ] `CrucetaRequest`/`CrucetaResponse` DTOs — items with marchaId, orderIndex, notes
- [ ] `SecurityConfig` — `anyRequest().authenticated()`, public GET for `/api/marchas/**, /swagger-ui/**, /v3/api-docs/**, /actuator/**`
- [ ] `JwtAuthenticationConverter` — extracts `hermandad_memberships` claim into authorities (copy from hermandad-service)
- [ ] `RepertorioSecurityService` — `@Component("repertorioSecurity")` checks JWT authorities for `HERMANDAD_{id}_HERMANDAD_ADMIN` (no DB fallback for MVP)
- [ ] `GlobalExceptionHandler` — returns `ApiError` JSON, same pattern as hermandad/procesion
- [ ] `OpenApiConfig` — bearer JWT security scheme pointing at gateway (localhost:8080)
- [ ] `ApiError` record — same as other services
- [ ] `./gradlew :services:repertorio-service:compileJava` passes

**Technical Notes:**
- Copy `JwtAuthenticationConverter` from hermandad-service, update package to `com.repertorio.marcha.adapter.config.security`
- `RepertorioSecurityService` is JWT-only (fast path) — no DB fallback call for MVP
- `@PreAuthorize("@repertorioSecurity.isAdmin(#hermandadId)")` on cruceta mutation endpoints
- Cruceta controller path includes `hermandadId` for auth scope, even though cruceta lives in repertorio-service
- DTO validation: `@NotBlank`, `@NotNull`, `@Positive`, `@Size` where applicable

**Effort:** 12 files · **Dependencies:** TASK-6

---

#### TASK-8: Outbox + Event Publishing

**Description:** Implement the outbox pattern for reliable event publishing — mirror the exact same pattern from procesion-service. Domain events flow: entity → `DomainEventPublisherAdapter` → `ApplicationEventPublisher` (in-process) + `OutboxEventEntity` (DB) → `OutboxPoller` (@Scheduled 5s) → Kafka topic.

**Acceptance Criteria:**
- [ ] `OutboxPublisher` (port interface) — `publish(DomainEvent)`
- [ ] `OutboxEventEntity` — JPA entity for `outbox_event` table with `@UuidGenerator` (no `@GeneratedValue`)
- [ ] `OutboxEventJpaRepository` — `findTop100ByProcessedFalseOrderByCreatedAtAsc`
- [ ] `OutboxEventPublisher` — serializes domain event to JSON via `ObjectMapper`, saves entity
- [ ] `OutboxPoller` — `@Scheduled(fixedDelayString = "PT5S")`, sends to Kafka topic `{aggregateType}-events`
- [ ] `DomainEventPublisherAdapter` — `@Component` that calls `ApplicationEventPublisher` + `OutboxPublisher`
- [ ] `V4__create_outbox_table.sql` already exists (from TASK-4) — verify it matches
- [ ] `./gradlew :services:repertorio-service:compileJava` passes
- [ ] Confirm `@EnableScheduling` is on `RepertorioServiceApplication` (from TASK-1)

**Technical Notes:**
- Copy-paste exact implementation from `procesion-service/.../adapter/outbound/outbox/`, update package to `com.repertorio.marcha.adapter.outbound.outbox`
- Spring Boot 4.1 uses `tools.jackson.databind.ObjectMapper` (not `com.fasterxml.jackson`)
- Poller sends to topic name derived from `domainEvent.aggregateType()` + `"-events"` suffix (e.g., `marcha-events`, `cruceta-events`)
- No idempotent consumer needed for MVP — repertorio only produces, doesn't consume its own events
- Catch `JacksonException` (not `JsonProcessingException`) — matches Spring Boot 4.1 Jackson 3 API

**Effort:** 6 files · **Dependencies:** TASK-4, TASK-6

---

#### TASK-9: Docker + Gateway Integration

**Description:** Containerize repertorio-service and integrate it into the existing Docker Compose stack. Add Dockerfile, docker-compose service definition (both full and dev profiles), cruceta gateway route, and create the `marcha-events` Kafka topic in the init script.

**Acceptance Criteria:**
- [ ] `Dockerfile` — `eclipse-temurin:21-jre-alpine`, copies JAR, `ENTRYPOINT ["java", "-jar", "app.jar"]`
- [ ] `docker-compose.yml` — repertorio-service container: port 8083, depends_on postgres-repertorio + kafka + redis + discovery-server, env vars for DB + Kafka + Eureka
- [ ] `docker-compose.dev.yml` — same service with memory limits (256m)
- [ ] Gateway route for cruceta: `/api/hermandades/{hid}/procesiones/{pid}/cruceta/**` → `lb://repertorio-service` (existing `/api/marchas/**` route already exists)
- [ ] `marcha-events` topic (3 partitions) added to kafka-init topic creation in both compose files
- [ ] `docker compose build repertorio-service` passes
- [ ] `docker compose up -d repertorio-service` starts without errors

**Technical Notes:**
- Dockerfile same pattern as procesion-service (multi-stage not needed for MVP)
- Env vars override application.yml for container environment: `SPRING_DATASOURCE_URL` uses `postgres-repertorio:5432`, `SPRING_KAFKA_PRODUCER_BOOTSTRAP_SERVERS` uses `kafka:29092`
- Gateway already has `/api/marchas/**` route from initial setup — just verify it still exists
- Add cruceta route as a new route ID `repertorio-cruceta` before `repertorio-service` in the route list
- Kafka init: add `marcha-events` before the final `echo 'All topics created.'` in both `docker-compose.yml` and `docker-compose.dev.yml`

**Effort:** 5 files (1 create, 4 modify) · **Dependencies:** TASK-1

---

#### TASK-10: Domain Unit Tests

**Description:** Write pure JUnit 5 unit tests (no Spring context) for the Marcha and Cruceta domain entities — covering construction validation, mutation behavior, edge cases.

**Acceptance Criteria:**
- [ ] `MarchaTest`:
  - `createWithValidFields` — succeeds
  - `createWithBlankTitle` — throws IllegalArgumentException
  - `createWithBlankComposer` — throws
  - `createWithNegativeDuration` — throws
  - `updateChangesFields` — title, composer, bandType updated
  - `updateWithBlankTitle` — throws
- [ ] `CrucetaTest`:
  - `createWithValidItems` — succeeds
  - `createWithNullItems` — throws
  - `createWithDuplicateOrderIndex` — throws
  - `redefineReplacesItems` — items replaced, updatedAt ticks
  - `containsMarchaReturnsTrue` — when marchaId present in items
  - `containsMarchaReturnsFalse` — when marchaId absent
- [ ] All tests pass: `./gradlew :services:repertorio-service:test --tests "*MarchaTest" --tests "*CrucetaTest"`

**Technical Notes:**
- Pure JUnit 5 — no `@SpringBootTest`, no Mockito
- `assertThrows` for validation, `assertEquals`/`assertTrue`/`assertFalse` for behavior
- Write tests first (TDD), then implement domain to make them pass

**Effort:** 2 files · **Dependencies:** TASK-2, TASK-3

---

#### TASK-11: Service Layer Unit Tests

**Description:** Write Mockito-based unit tests for `MarchaService` and `CrucetaService` — mocking repository and event publisher dependencies, verifying business logic and event publication.

**Acceptance Criteria:**
- [ ] `MarchaServiceTest`:
  - `listMarchas` — returns filtered results
  - `getMarcha_found` — returns marcha
  - `getMarcha_notFound` — throws
  - `createMarcha` — saves and publishes `MarchaAddedEvent`
  - `deleteMarcha_noReferences` — deletes and publishes `MarchaRemovedEvent`
  - `deleteMarcha_referencedInCruceta` — throws (delete validation)
- [ ] `CrucetaServiceTest`:
  - `getCruceta_found` — returns cruceta
  - `getCruceta_notFound` — throws
  - `defineCruceta_new` — saves new and publishes `CrucetaDefinedEvent`
  - `defineCruceta_replace` — replaces existing and publishes event
- [ ] All tests pass: `./gradlew :services:repertorio-service:test --tests "*ServiceTest"`

**Technical Notes:**
- Use `@ExtendWith(MockitoExtension.class)` + `@Mock` + `@InjectMocks`
- Use `ArgumentCaptor` to verify domain events were published with correct fields
- Mock `DomainEventPublisher` (the port interface), not the adapter

**Effort:** 2 files · **Dependencies:** TASK-6

---

#### TASK-12: Controller Slice Tests

**Description:** Write `@WebMvcTest` controller tests for `MarchaController` and `CrucetaController` — verifying HTTP status codes, response bodies, auth enforcement (public GET works, mutations require JWT).

**Acceptance Criteria:**
- [ ] `MarchaControllerTest`:
  - `GET /api/marchas` (no auth) → 200
  - `GET /api/marchas/{id}` (no auth) → 200
  - `POST /api/marchas` (no auth) → 401
  - `POST /api/marchas` (with JWT) → 201
  - `DELETE /api/marchas/{id}` (with JWT) → 204
  - `DELETE /api/marchas/{id}` (no auth) → 401
- [ ] `CrucetaControllerTest`:
  - `GET .../cruceta` (no auth) → 200
  - `PUT .../cruceta` (no auth) → 401
  - `PUT .../cruceta` (with non-admin JWT) → 403
  - `PUT .../cruceta` (with admin JWT) → 200
- [ ] All tests pass: `./gradlew :services:repertorio-service:test --tests "*ControllerTest"`

**Technical Notes:**
- Use `@WebMvcTest(controllers = {MarchaController.class, CrucetaController.class})`
- `@MockitoBean` for services and security service
- Use `with(jwt())` from `spring-security-test` for authenticated requests
- Use `@WithMockUser(authorities = "HERMANDAD_{id}_HERMANDAD_ADMIN")` or `MockMvcRequestPostProcessors.jwt()` for admin tests
- Test JSON response body structure with `jsonPath` for key fields

**Effort:** 2 files · **Dependencies:** TASK-7

---

## Backlog (ordered)

### Technical Debt / Audit Findings

Items from `docs/audit.md` — small-effort fixes that should be picked up early.

- Rename `keycloak_group_id` to `keycloak_group_id_refs` (audit finding — naming)

### Hermandad Tests

- Add Testcontainers integration test for outbox → Kafka flow (EmbeddedKafka)

### Shared Library

- Add unit tests for `JwtMembershipExtractor` (valid JSON claim, null claim, malformed JSON)
- Add unit tests for `TenantContextFilter` (header present, header absent, cleanup on exit)

### Procesión Service

- ✅ ~~Model `Procesion` aggregate with DDD + TDD~~
- `Recorrido` value object: ordered list of waypoints with timestamps
- ✅ ~~CRUD REST endpoints for processions~~
- ✅ ~~Assign hermandad to procession~~
- ✅ ~~Publish events: `ProcesionCreated`, `ProcesionStatusChanged`~~
- Kafka consumer for hermandad events (react when hermandad is modified)
- ✅ ~~Docker Compose integration (procesion-db, service registration)~~
- ✅ ~~Outbox pattern for event publishing~~
- `@PreAuthorize` method-level security guards

### Repertorio Service

*(Items below tracked in Sprint 9 above. Kept here for visibility.)*

- ✅ Model `Marcha` aggregate: title, composer, `BandType` enum, durationSeconds, compositionYear, youtubeUrl
- ✅ Model `Cruceta` + `CrucetaItem`: ordered marcha list per procesionId
- CRUD REST endpoints for global marcha catalog
- Cruceta management by procesionId (get + replace)
- Outbox pattern: `MarchaAdded`, `MarchaRemoved`, `CrucetaDefined` → Kafka
- Docker Compose + Gateway route integration

### Tracking Service

- GPS position model (hermandadId, lat, lon, timestamp)
- Ingestion endpoint for GPS data
- Last known position endpoint
- Kafka consumer for procesion events (start/stop tracking)
- Docker Compose integration

### Notification Service

- Kafka consumer for all domain events
- Notification dispatch (placeholder — could be email, push, or just logs)
- Docker Compose integration

### Cross-cutting / Infrastructure

- Idempotent Kafka consumer pattern (one service as reference, then apply everywhere)
- Structured logging with correlation IDs across services
- Circuit breaker with Resilience4j
- Distributed tracing with Zipkin
- API Gateway route configuration for all services (currently points to non-existent repertorio/procesion/tracking/notification services)
- Integration test suite (end-to-end flows across services)
- Rate limiting on API Gateway
- DB migration strategy (Flyway already in use, extend to all services)
- Extend `TenantIdInjectionFilter` to handle all service URL patterns, not just `/api/hermandades/{id}/...`
- Replace `kafka-init` no-op healthcheck with a real readiness check (wait until topics are listed)
- Add Docker Compose services for repertorio-service, procesion-service, tracking-service, notification-service (currently missing from docker-compose.yml)
- Upgrade to Spring Boot 3.5.x (completed)
- Migrate to Spring Boot 4.x (cross-cutting)

### Learning / Study

- SOLID principles applied to the codebase (review each service)
- Design pattern identification (Strategy, Factory, Observer — map to concrete usage)
- System design practice: talk through one service's architecture out loud

---

## Done

- **Sprint 5 — Hermandad Polish + Idempotent Consumer** ✅
  - `MemberAddedEvent.hermandadId` field + test assertion
  - MockMvc tests (409/404/400 for controller, all handlers for GlobalExceptionHandler)
  - Idempotent Kafka consumer reference pattern (`processed_event` table, entity, consumer, tests)
- **Sprint 4 — Hermandad Service Hardening** ✅
- Project skeleton: Gradle multi-project, Docker Compose, shared library
- Infrastructure: API Gateway, Discovery Server, Keycloak realm
- Hermandad model: `Hermandad`, `HermandadMember`, `HermandadRole`
- Hermandad CRUD: create, find by id, list members
- Hexagonal Architecture refactor (ports & adapters)
- Outbox pattern implementation (table, publisher, poller to Kafka)
- Redis caching for hermandad and members
- Keycloak integration: group membership sync via events
- Architecture decision: `HermandadMember` as separate aggregate (not child of `Hermandad`)
- DDD enrichment: `HermandadMember.changeRole()` with no-op invariant
- Architecture document: `docs/architecture.md`
- DomainEvent interface + DomainEventPublisher port + adapter wrapping OutboxPublisher and ApplicationEventPublisher
- All domain events implement `DomainEvent` (aggregateType, aggregateId, eventType)
- `HermandadService` refactored to depend solely on `DomainEventPublisher`
- `OutboxEventPublisher` updated to match new `OutboxPublisher` interface using `DomainEvent`
- Story 4 (change role): full endpoint + exception handlers + domain events + tests
- Story 5 (MemberRoleChangedEvent): published via DomainEventPublisher on role change
- `ChangeRoleRequest` DTO, `GlobalExceptionHandler` updated for 404/400
- `DomainEventPublisherAdapter` wiring `OutboxPublisher` + `ApplicationEventPublisher`
- Spring Boot upgraded from 3.3.5 → 3.5.15, Spring Cloud 2023.0.3 → 2024.0.1
- springdoc-openapi `2.7.0` + Swagger UI integrated, gateway routes + security permits for `/v3/api-docs`, `/swagger-ui/**`
- Missing `hermandad-member-events` Kafka topic added to `kafka-init`
