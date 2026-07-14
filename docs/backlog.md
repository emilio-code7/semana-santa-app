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

### Sprint 8 — Pre-MVP Cleanup ✅

| Pri | Task | Effort | Status |
|-----|------|--------|--------|
| 🔴 1 | **Hermandad constructor validation** — `Hermandad.java`: null/empty checks for name, city; valid foundedYear | ~1 file | ✅ |
| 🔴 2 | **Remove dead `@EnableFeignClients`** — `ProcesionServiceApplication.java:12` + `build.gradle.kts:18` `spring-cloud-starter-openfeign` | 2 files, cleanup | ✅ |
| 🔴 3 | **Flyway index alignment** — add `@Table(indexes = @Index(...))` to `Procesion.java` to match existing `idx_procesion_hermandad_id` | 1 file | ✅ |
| 🔴 4 | **Guard gateway stub routes** — 3 routes (`/api/marchas/**`, `/api/tracking/**`, `/api/notifications/**`) currently 503; fallback or comment out | 1 file | ✅ |
| 🟡 5 | **API Gateway + Discovery Server tests** — zero test coverage for infrastructure | Medium | ✅ |
| 🟡 6 | **Stub service cleanup** — 3 of 5 services still empty skeletons | Medium | ✅

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

- Model `Marcha` aggregate (musical piece): title, composer, genre, duration
- Model `Repertorio` aggregate: year, list of marches selected by a hermandad
- CRUD REST endpoints for marches
- Assign marches to a hermandad's repertorio
- Publish events: `MarchaAdded`, `MarchaRemoved`
- Kafka consumer for hermandad events
- Docker Compose integration

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
