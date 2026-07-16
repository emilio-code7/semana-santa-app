# Code Review Fixes — Implementation Plan

**Goal:** Address the 12 findings from the full-project code review. Fix architecture violations, spec drift, and technical debt.

**Scope:** All 3 services + docs. Tasks independent unless noted.
**Tech Stack:** Spring Boot 4.1, Java 21, JUnit 5

---

## Task CR-1: Remove JPA from Domain Layer (Hermandad + Procesion)

**Depends on:** Nothing (but check CR-2 first — the rule needs to be clear)

**What:** Move `@Entity`, `@Table`, `@Id`, `@Column`, `@PrePersist`, `@PreUpdate`, `@Enumerated`, `@UuidGenerator` out of domain classes into adapter JPA entities. Create separate `Entity` classes in `adapter/outbound/persistence/`, add `from()`/`toDomain()` conversion methods. Repertorio already does this correctly — follow its pattern.

**Files affected (hermandad-service):**
- `domain/model/Hermandad.java` — strip JPA annotations, keep pure Java
- `domain/model/HermandadMember.java` — strip JPA annotations
- New: `adapter/outbound/persistence/HermandadEntity.java`
- New: `adapter/outbound/persistence/HermandadMemberEntity.java`
- Modify: `adapter/outbound/persistence/HermandadJpaRepository.java` — switch to `HermandadEntity`
- Modify: `adapter/outbound/persistence/HermandadMemberJpaRepository.java` — switch to `HermandadMemberEntity`
- Modify: both repository adapters — add from/toDomain conversions

**Files affected (procesion-service):**
- `domain/model/Procesion.java` — strip JPA annotations, keep pure Java
- `domain/model/ProcesionStatus.java` — already pure Java (enum), clean
- New: `adapter/outbound/persistence/ProcesionEntity.java`
- Modify: `adapter/outbound/persistence/ProcesionJpaRepository.java` — switch to `ProcesionEntity`
- Modify: `adapter/outbound/persistence/ProcesionRepositoryAdapter.java` — add conversions

**Gherkin:**

```gherkin
Feature: Domain layer has zero framework dependencies

  Scenario: Domain model is pure Java
    Given the domain class in domain/model/
    When I inspect its imports
    Then there is no jakarta.persistence.* import
    And there is no org.springframework.* import

  Scenario: JPA entity lives in adapter layer
    Given a JPA entity exists for the aggregate
    When I check its package
    Then it is under adapter/outbound/persistence/
    And it has @Entity, @Table, @Id annotations
    And it has from(Domain) and toDomain() methods

  Scenario: Round-trip conversion preserves all fields
    Given a domain object with all fields set (including optionals)
    When I convert to JPA entity via from() and back via toDomain()
    Then every field matches the original

  Scenario: Relationship handling works correctly
    Given an aggregate with child entities (e.g. Hermandad with Members)
    When I convert from domain to JPA entities
    Then all children are included in the conversion
    And the bidirectional mapping is consistent

  Scenario: PrePersist/PreUpdate timestamps are preserved
    Given the entity has createdAt/updatedAt fields
    When saving a new entity
    Then createdAt is set on first persist
    And updatedAt advances on subsequent updates
```

**Acceptance:**
- All 3 domain classes compile with zero framework imports
- Round-trip field preservation tested (including nullables: description, youtubeUrl, compositionYear)
- Relationship handling verified (Hermandad → HermandadMember list)
- All existing tests pass unchanged (same behavior, different structure)

**Effort:** ~8 files hermandad, ~4 files procesion

---

## Task CR-2: Fix Spec Contradiction — JPA Rule

**Depends on:** Nothing (doc-only change, can run before or alongside CR-1)

**What:** `docs/functional-map.md §0.1` says "Domain must not import JPA." `docs/architecture.md:124` says "JPA annotations stay on domain models (pragmatic Hexagonal — JPA is not an 'infrastructure leak', it's a practical choice)." These contradict. Pick one and update the other. Recommendation: keep functional-map's rule (pure domain) since repertorio already follows it correctly, and update architecture.md:124 to match.

**File:**
- `docs/architecture.md:124` — remove or rewrite the "JPA annotations stay on domain models" paragraph to align with functional-map §0.1

**Acceptance:**
- Both documents agree: domain layer is pure Java, JPA goes in adapter
- No conflicts between functional-map §0.1 and architecture.md

**Effort:** 1 file

---

## Task CR-3: Implement searchMarchas Endpoint

**Depends on:** Nothing

**What:** functional-map §4.3 declares `GET /api/marchas/search?q={query}` → `searchMarchas()`. No code exists. Add it to repertorio-service.

**Gherkin:**

```gherkin
Feature: Search marchas by query

  Scenario: Search by title returns matching marchas
    Given marchas exist with titles "Amarguras" and "Saeta" and "El Amor de Dios"
    When a user sends GET /api/marchas/search?q=amor
    Then the response is 200
    And the response contains only "El Amor de Dios"

  Scenario: Search by composer returns matches
    Given marchas exist by "Manuel López Farfán" and "Abel Moreno"
    When a user sends GET /api/marchas/search?q=moreno
    Then the response contains marchas by "Abel Moreno"

  Scenario: No matches returns empty list
    When a user sends GET /api/marchas/search?q=zzzzznotfound
    Then the response is 200
    And the response is an empty array

  Scenario: Search without query returns 400
    Given the @RequestParam q is required (required=true)
    When a user sends GET /api/marchas/search (missing q)
    Then the response is 400

  Scenario: Unauthenticated user gets 401
    When an unauthenticated user sends GET /api/marchas/search?q=test
    Then the response is 401
```

**Files:**
- Modify: `adapter/inbound/rest/controller/MarchaController.java` — add `searchMarchas(@RequestParam String q)`
- Modify: `application/service/MarchaService.java` — add `search(String query)` method
- Modify: `domain/port/MarchaRepository.java` — add `findByTitleContainingIgnoreCaseOrComposerContainingIgnoreCase(String title, String composer)`
- Modify: `adapter/outbound/persistence/MarchaJpaRepository.java` — add the query method
- Modify: `adapter/outbound/persistence/MarchaRepositoryAdapter.java` — delegate
- Modify: `docs/openapi.yaml` — add the search endpoint (functional-map §4.3 already lists it)
- Note: functional-map §4.3 already declares searchMarchas — verify it's still correct, no update needed

**Acceptance:**
- Search by title fragment returns matches
- Search by composer fragment returns matches
- No matches returns `[]` (not 404)
- Missing `q` param returns 400
- Unauthenticated returns 401
- All existing tests pass

**Effort:** 5 files + tests

---

## Task CR-4: Sync functional-map with Code

**Depends on:** Nothing

**What:** functional-map §4.3 is missing the `updateMarcha` endpoint. Also §9.2 says repertorio has no `@PreAuthorize` — wrong, cruceta has it. Fix both.

**Files:**
- `docs/functional-map.md` — add `updateMarcha` row to §4.3, update §9.2

**Acceptance:**
- `updateMarcha` listed in API endpoints table
- Repertorio security section correctly states: marcha CRUD has no `@PreAuthorize`, cruceta has it
- `./gradlew` not needed (doc-only)

**Effort:** 1 file

---

## Task CR-5: Clean Up Gateway Public Routes

**Depends on:** Nothing

**What:** `api-gateway/SecurityConfig.java` has 5+ `permitAll()` GET routes that don't exist in any service: `/api/hermandades/{id}/procesiones`, `/api/marchas/**`, etc. `GET /api/marchas/**` being public contradicts functional-map §4.3 (marchas require auth). Remove the routes that don't exist; keep only what functional-map §4.4 lists as public.

**Gherkin:**

```gherkin
Feature: Gateway only exposes documented public routes

  Scenario: Public routes match the spec
    Given the functional-map §4.4 lists public routes
    When I check api-gateway SecurityConfig
    Then every permitAll() route is documented in the spec
    And no permitAll() route points to a non-existent service endpoint

  Scenario: Marcha GETs require auth at gateway
    When an unauthenticated user sends GET /api/marchas
    Then the gateway does NOT permitAll — passes through to service auth
```

**Files:**
- Modify: `infrastructure/api-gateway/src/main/java/.../SecurityConfig.java` — remove extra `permitAll()` for non-existent routes. **NOT** `application.yml` — the route predicates in `application.yml` are fine (they define routing, not auth).

**Acceptance:**
- Gateway `.permitAll()` routes match functional-map §4.4 exactly
- No routes point to services/endpoints that don't exist
- Gateway still routes authenticated requests correctly

**Effort:** 2 files

---

## Task CR-6: Add Persistable to Repertorio JPA Entities

**Depends on:** Nothing

**What:** `MarchaEntity`, `CrucetaEntity`, `CrucetaItemEntity` set UUIDs manually with no `@GeneratedValue` or `@UuidGenerator`. Procesion had the same issue — fix was `implements Persistable<UUID>` with `isNew()` returning `id == null`. Follow that pattern.

**Reference:** See `services/procesion-service/.../domain/model/Procesion.java` — the `Persistable<UUID>` + `@UuidGenerator` + `@Version` fix in commit `0577a09`. Also references: `V4__add_version_to_procesion.sql` (adds `version INTEGER DEFAULT 0`).

**Note:** CR-12 also modifies `CrucetaEntity.java` (`@Table(indexes=...)`). Apply CR-12 first (Phase A), then CR-6 (Phase B) to avoid conflicts.

**Gherkin:**

```gherkin
Feature: Repertorio JPA entities handle Hibernate 7 UUID persistence

  Scenario: New entity is detected as new
    Given a new MarchaEntity with null id
    When isNew() is called
    Then it returns true

  Scenario: Existing entity is detected as existing
    Given a MarchaEntity with a non-null id
    When isNew() is called
    Then it returns false

  Scenario: Entity saves and retrieves correctly
    Given a new MarchaEntity is saved via JpaRepository
    When it is retrieved by its generated ID
    Then all fields match
```

**Files:**
- Modify: `adapter/outbound/persistence/MarchaEntity.java` — implement `Persistable<UUID>`, add `@UuidGenerator`, add `@Version`
- Modify: `adapter/outbound/persistence/CrucetaEntity.java` — same (note: CR-12 also changes this file)
- Modify: `adapter/outbound/persistence/CrucetaItemEntity.java` — same
- New: `db/migration/V7__add_version_columns.sql` — add `version INTEGER DEFAULT 0` to `marcha`, `cruceta`, `cruceta_item` tables

**Acceptance:**
- All 3 entities implement `Persistable<UUID>` with correct `isNew()`
- `@UuidGenerator` on id field (not `@GeneratedValue`)
- `@Version` column added via Flyway V7 migration
- All existing tests pass

**Effort:** 3 files + possibly 1 Flyway migration

---

## Task CR-7: Extract Shared Infrastructure to common Module

**Depends on:** CR-1 (reduces duplication surface)

**What:** ~1500 lines of copy-paste infrastructure across 3 services: `OutboxEventEntity`, `OutboxEventJpaRepository`, `OutboxEventPublisher`, `OutboxPoller`, `DomainEventPublisherAdapter`, `ProcessedEventEntity`, `ProcessedEventJpaRepository`, `GlobalExceptionHandler`, `ApiError`, `JwtAuthenticationConverter`, `OpenApiConfig`, `SecurityConfig`. Move shared patterns to `shared/common`.

**Note:** This is a large refactor. Best done after CR-1 since that changes similar files.

**Acceptance:**
- `shared/common` contains shared base classes for: outbox entity, outbox repo, outbox publisher, outbox poller, domain event publisher, exception handler, API error, JWT converter, security config
- Each service extends/wires instead of copying
- All tests pass, zero behavioral change

**Effort:** Major — 30+ files, 2-3 sessions

---

## Task CR-8: Unify DomainEvent Interface

**Depends on:** Nothing

**What:** `DomainEvent` is defined in different packages with different shapes. Hermandad/procesion: `application/port/DomainEvent.java` with 3 methods (`aggregateType`, `aggregateId`, `eventType`). Repertorio: `domain/event/DomainEvent.java` with 4 methods (`eventId`, `occurredAt`, `aggregateType`, `aggregateId` — missing `eventType`). 

**Unified interface:** Move to `shared/common` with 5 methods: `eventId()`, `occurredAt()`, `aggregateType()`, `aggregateId()`, `eventType()`. Hermandad/procesion events need `+eventId`, `+occurredAt`. Repertorio events need `+eventType`.

**Gherkin:**

```gherkin
Feature: All services share one DomainEvent interface

  Scenario: Every event implements the shared interface
    Given a common DomainEvent interface in shared/common
    When I check each service's events
    Then they all implement the same interface
    And they all provide eventId, occurredAt, aggregateType, aggregateId, eventType
```

**Acceptance:**
- Single `DomainEvent` in `shared/common` with 5 methods
- All 3 services' events implement it
- `eventId` and `occurredAt` added to hermandad/procesion events (9 event records)
- `eventType()` added to repertorio events (4 event records)
- All tests pass

**Effort:** 4-6 files

---

## Task CR-9: Remove Unused Code

**Depends on:** Nothing

**What:** Delete dead code: `RepertorioSecurityService` (unused, 23 lines). Verify `@EnableMethodSecurity` redundancy — procesion and repertorio have it on **both** the `Application.java` and `SecurityConfig.java` classes; hermandad only has it on SecurityConfig. Remove the redundant annotation from both Application classes.

**Files:**
- Delete: `repertorio-service/.../adapter/config/security/RepertorioSecurityService.java`
- Modify: `procesion-service/.../ProcesionServiceApplication.java` — remove `@EnableMethodSecurity` (already on SecurityConfig)
- Modify: `repertorio-service/.../RepertorioServiceApplication.java` — remove `@EnableMethodSecurity` (already on SecurityConfig)

**NOT deleting:** `MembersCache` — it is used by `RedisConfig.java` as a `JacksonJsonRedisSerializer<MembersCache>` type parameter. Not dead code.

**Acceptance:**
- Files deleted, no compilation errors
- All tests pass
- `grep` confirms no references to `RepertorioSecurityService`
- Redis caching still works (MembersCache kept)

**Effort:** 2-3 files

---

## Task CR-10: Standardize Integration Test Base Class

**Depends on:** Nothing

**What:** All 3 services use the same manual JDBC reachability check pattern for integration tests (none extends `IntegrationTestBase` from `shared/common`). The current approach works but isn't shared. Two options:

- **Option A (pragmatic):** Create a lightweight `JdbcReachabilityTestBase` in `shared/common` that mirrors the current manual pattern (JDBC check, no Testcontainers, no Docker overhead). All 3 services extend it. ~2 files + 3 test refactors.
- **Option B (document):** Add a `ponytail:` comment explaining the deliberate divergence (Tests connect to external PG via env-configurable JDBC URL, not embedded Testcontainers — faster local dev, no Docker requirement) and close the task. 

**Reference:** See any integration test in the project — they all follow the same pattern:
```java
@BeforeAll
static void checkPostgres() { 
    // try to reach env-configured JDBC URL, skip if unavailable
}
```

**NOT using:** `IntegrationTestBase` from `shared/common` — it uses Testcontainers which requires Docker-in-Docker, spins up unnecessary Kafka+Redis containers, and doesn't match the current external-PG pattern.

**Effort:** 2 test files

---

## Task CR-11: Wire or Remove IdempotentEventConsumer

**Depends on:** Nothing

**What:** `IdempotentEventConsumer` consumes hermandad events, deduplicates via `processed_event` table, then does nothing. Either wire actual downstream processing or remove. If the audit-trail use case is intentional, document it.

**Files:**
- Option A (keep): Add javadoc explaining the audit-trail purpose and a `ponytail:` comment
- Option B (remove): Delete `IdempotentEventConsumer.java`, `ProcessedEventEntity.java`, `ProcessedEventJpaRepository.java`, Flyway V6

**Acceptance:**
- Either the consumer has documented purpose, or it's removed cleanly
- All tests pass

**Effort:** 1 file (doc) or 5 files (removal)

---

## Task CR-12: Mirror Cruceta Unique Index on Entity

**Depends on:** Nothing

**What:** `V2__create_cruceta_tables.sql:8` defines `CREATE UNIQUE INDEX idx_cruceta_procesion_id ON cruceta(procesion_id)`. `CrucetaEntity.java` has no `@Table(indexes = ...)` declaration. Add it.

**File:**
- Modify: `adapter/outbound/persistence/CrucetaEntity.java` — add `@Table(name = "cruceta", indexes = @Index(name = "idx_cruceta_procesion_id", columnList = "procesion_id", unique = true))`

**Note:** CR-6 also modifies `CrucetaEntity.java` (adds `Persistable` + `@Version`). Apply CR-12 first (Phase A), then CR-6 (Phase B) — the annotations are additive and don't conflict.`

**Acceptance:**
- Entity annotation matches the migration SQL
- `./gradlew :services:repertorio-service:compileJava` passes

**Effort:** 1 file, 1 line change

---

## Execution Order (Recommended)

```
Phase A (fast wins):
  CR-2  → Fix spec contradiction (1 doc file) — independent, doc-only
  CR-4  → Sync functional-map (1 doc file)
  CR-5  → Clean gateway routes (1 file)
  CR-9  → Remove dead code (3 files)
  CR-12 → Add index annotation (1 line)

Phase B (behavioral):
  CR-3  → searchMarchas endpoint
  CR-6  → Persistable on repertorio entities (apply after CR-12)

Phase C (architecture):
  CR-1  → Remove JPA from domain (biggest impact)
  CR-10 → Standardize integration test base class
  CR-8  → Unify DomainEvent

Phase D (debt):
  CR-7  → Extract shared infrastructure (major refactor)
  CR-11 → Wire or remove consumer
```
