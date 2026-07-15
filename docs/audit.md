# Audit Report — Semana Santa App

> Generated: 2026-06-16
> Scope: Codebase health, documentation alignment, known flaws, missing pieces

This document captures findings from a comprehensive codebase audit. It serves as a reference for iteration planning — each section links to backlog items where applicable.

---

## 1. Security & Access Control

### 1.1 No Auth Enforcement in Hermandad Service ✅ RESOLVED

**File**: `services/hermandad-service/.../adapter/config/SecurityConfig.java`

**Resolution (Sprint 4):** `JwtAuthenticationConverter` extracts `hermandad_memberships` → `HERMANDAD_{id}_{role}` authorities. `@EnableMethodSecurity` + `@PreAuthorize` on admin endpoints. Dual-path fallback via `HermandadSecurityService` (JWT fast, DB fallback). `AccessDeniedException` → 403.

### 1.2 Missing Role in OpenAPI Spec

`CAPATAZ` role exists in `HermandadRole` enum but is missing from `docs/openapi.yaml` and the role-permission matrix in the plan. The role's permissions are undefined.

---

## 2. Architecture Contradictions

### 2.1 Hexagonal vs Layered (RESOLVED)

The `.sisyphus/plans/semana-santa-app.md` originally said *"NO Hexagonal Architecture — use simple layered architecture"* while `docs/architecture.md` and the hermandad-service implementation both use Hexagonal (ports & adapters). This was a contradiction.

**Resolution**: Hexagonal + DDD is now the chosen approach. The plan file has been updated to match.

---

## 3. Data & Domain Issues

### 3.1 `HermandadMember.updatedAt` Not Persisting ✅ RESOLVED

**File**: `services/hermandad-service/.../domain/model/HermandadMember.java`

~~The column is marked `updatable = false`, but `@PreUpdate` sets `updatedAt` expecting it to persist. Hibernate omits it from UPDATE SQL — changes to `updatedAt` are silently lost.~~

**Resolution**: `updatable = false` was removed from `updatedAt` (it remains only on `joinedAt`, which is correct). Role changes now persist the new `updatedAt` timestamp.

### 3.2 `MemberAddedEvent` Missing `hermandadId` ✅ RESOLVED

**File**: `services/hermandad-service/.../domain/event/MemberAddedEvent.java`

The event carried `memberId`, `userId`, and `role` but not `hermandadId`. Kafka consumers could not determine which hermandad the member belongs to without a cross-service lookup.

**Resolution (Sprint 5):** `UUID hermandadId` added to the record and populated when publishing. Verified in `HermandadServiceTest.addMemberPublishesDomainEvent()`.

### 3.3 Hermandad Entity Missing Fields ✅ PARTIALLY RESOLVED

The `Hermandad` entity has `name, city, foundedYear, keycloakGroupId, createdAt` but the plan and OpenAPI spec define additional fields: `country`, `description`, `visibility` (PUBLIC/PRIVATE), `showSongs` (boolean). Visibility checks (return 404 for private hermandads) cannot work without these fields.

**Resolution (Sprint 4):** `description` (nullable TEXT) was added. `country`, `visibility`, and `showSongs` were explicitly deferred as speculative (YAGNI review).

---

## 4. Error Handling

### 4.1 Incomplete Exception Handling ✅ RESOLVED

**File**: `services/hermandad-service/.../adapter/inbound/rest/GlobalExceptionHandler.java`

Only `HermandadNotFoundException` was handled. Missing handlers:
- `HermandadMemberNotFoundException` — used by `changeRole()`, would return 500 instead of 404 → ✅ Added
- `MethodArgumentNotValidException` — validation errors from `@Valid` return 400 with Tomcat's default HTML → ✅ Added
- `DataIntegrityViolationException` — duplicate member unique constraint returns 500 → ✅ Added
- `IllegalArgumentException` — same-role change from `changeRole()` returns 500 → ✅ Added
- Generic fallback — any unhandled exception returns 500 with no useful body → ✅ Added
- `AccessDeniedException` — added in Sprint 4 → 403

**Resolution (Sprint 2/4/5):** All handlers implemented. `GlobalExceptionHandlerTest` provides per-handler test coverage for each status code + body format.

**Resolution (Sprint 7):** Both services now return `ResponseEntity<ApiError>` with structured JSON. `ApiError` record in `adapter/inbound/rest/dto/` with `status`, `error`, `message`. Unit tests assert structured body for each handler.

### 4.2 Non-Structured Error Responses ✅ RESOLVED

Error responses previously returned `ResponseEntity<String>` with plain text.

**Resolution (Sprint 7):** Both services migrated to `ApiError` record. Processing: `ErrorResponse` is the consistent format across all services.

---

## 5. Infrastructure Gaps

### 5.1 Two of Five Services Are Still Empty Skeletons

| Service | Status |
|---------|--------|
| hermandad-service | ✅ Implemented (48 Java files, 56 tests) |
| procesion-service | ✅ Implemented (27 Java files, 47 tests) |
| repertorio-service | ✅ Implemented (42 Java files, 44 tests) |
| tracking-service | ❌ `// placeholder` build.gradle.kts, no source |
| notification-service | ❌ `// placeholder` build.gradle.kts, no source |

**Achieved (Sprint 9):** repertorio-service went from stub to full implementation — Marcha catalog + Cruceta with hexagonal architecture, outbox pattern, 4 Flyway migrations. Gateway routes for `/api/marchas/**` and `/api/hermandades/*/procesiones/*/cruceta/**` are active.

The API Gateway still has routes for stub services — `lb://tracking-service`, `lb://notification-service` — these will 503 until implemented.

### 5.2 `kafka-init` Healthcheck Is a No-Op

The `kafka-init` container's healthcheck immediately exits 0 regardless of topic creation status. It doesn't actually block dependent services from starting before topics exist.

### 5.3 Missing Seed QA Users Script Format

The `infrastructure/keycloak/seed-qa-users.sh` file exists but was not verified for correctness in this audit.

---

## 6. Testing Coverage

### 6.1 No Integration Tests ✅ RESOLVED

**Resolution (Sprint 4/7):** Hermandad: `HermandadRepositoryIntegrationTest` covers JPA repository CRUD + constraints against real PostgreSQL. ✅ Procesion: `ProcesionRepositoryIntegrationTest` (4 tests) + `ProcesionControllerIntegrationTest` (8 tests) added. Both follow the same pattern: connects to running Docker Postgres, skips gracefully if unavailable.

### 6.2 Shared Library Has No Tests

The `shared/common` module has zero tests despite having logic (`JwtMembershipExtractor`, `TenantContextFilter`) that would benefit from them.

### 6.3 No Controller Tests ✅ RESOLVED

Hermandad: `HermandadControllerTest` covers error paths (409 conflict, 404 not found, 400 same-role) + `GlobalExceptionHandlerTest`. ✅ Procesion: `ProcesionControllerTest` (13 tests) + `ProcesionControllerIntegrationTest` (8 tests) + `GlobalExceptionHandlerTest` cover all endpoints. Both services have structured JSON error assertions.

---

## 7. Missing Endpoints

The following endpoints from the backlog and OpenAPI spec are not implemented:

| Endpoint | Status |
|----------|--------|
| `PATCH /api/hermandades/{id}/members/{userId}/role` | ✅ Implemented (`changeRole()`) |
| `DELETE /api/hermandades/{id}/members/{userId}` | ✅ Implemented (`removeMember()`) |
| `PUT /api/hermandades/{id}` | ❌ Missing |
| `GET /api/hermandades` (list all public) | ✅ Implemented (`findAllHermandades()`) |
| `GET /api/hermandades/{id}/with-members` | ❌ Missing |
| Member removal (soft/hard delete) | ✅ Implemented (hard delete) |
| Pagination for members list | ✅ Implemented (Pageable) |

---

## 8. Configuration & Dependencies

### 8.1 Keycloak Admin Client Misconfigured

**File**: `services/hermandad-service/src/main/resources/application.yml`

Uses `admin-cli` (master realm admin user) instead of `semana-santa-admin-client` (service account client defined in realm-export.json). The plan specifies using the admin client with client credentials grant, but the implementation uses admin username/password.

### 8.2 Missing `springdoc-openapi`

The plan mentions auto-generating OpenAPI specs, but `springdoc-openapi-starter-webmvc-ui` is absent from both the version catalog and build files.

### 8.3 Tenant Header Injection Is Hermandad-Only

**File**: `infrastructure/api-gateway/.../filter/TenantIdInjectionFilter.java`

The filter only injects `X-Tenant-Id` for paths under `/api/hermandades/{id}/...`. Other services (procesion, repertorio, tracking) with their own URL patterns won't get the tenant header injected.

---

## 9. Outbox Pattern Concerns

### 9.1 No Batch Processing Limit ✅ RESOLVED

**Resolution (Sprint 4):** Query changed to `findTop100ByProcessedFalseOrderByCreatedAtAsc()`, capped at 100 rows per poll cycle.

### 9.2 No Ordering Guarantee ✅ RESOLVED

**Resolution (Sprint 4):** `ORDER BY created_at ASC` added to the outbox query. Oldest events are processed first.

### 9.3 Payload Size Limit ✅ RESOLVED

The `payload` column is `VARCHAR(255)` — too small for complex events. 

**Resolution (Sprint 2):** `V3__alter_outbox_payload_column.sql` changed the column to `TEXT`.

---

## 10. Spring Boot 4 Migration Impact Analysis ✅ MIGRATED

> **Status: Complete** — migrated to Spring Boot 4.1.x (Sprint 3).
> This section is preserved as historical reference for future migrations.

### 10.1 Current Project Baseline

| Component | Current Version | SB4 Requirement | Status |
|-----------|----------------|-----------------|--------|
| Java | 21 | 21+ | ✅ OK |
| Gradle | 8.10 | 8.14+ (9.x recommended) | ❌ Must upgrade |
| Spring Boot | 3.3.5 (root BOM) / 3.5.15 (catalog) | 4.0.x | ⚠️ Drift detected |
| Spring Cloud | 2023.0.3 | 2025.1 (Oakwood) | ❌ Must upgrade |
| Spring Framework | 6.1.x | 7.0 | ❌ |
| Spring Security | 6.3.x | 7.0 | ❌ |
| Hibernate | 6.x (via SB BOM) | 7.1 | ❌ |
| Jakarta EE | 10 | 11 | ✅ Additive, no namespace rename |
| Jackson | 2.x | 3.0 | ❌ Package rename + API changes |
| Tomcat | 10.x | 11.0 | ❌ |

**⚠️ Version Catalog Drift**: `gradle/libs.versions.toml` declares `spring-boot = "3.5.15"` and `spring-cloud = "2024.0.1"`, but `build.gradle.kts` overrides BOMs to `"3.3.5"` and `"2023.0.3"` — the plugin (3.5.15) and resolved deps (3.3.5) are misaligned. Must reconcile before migration.

### 10.2 Migration Path (Phased)

**Phase 0 — Reconcile → Upgrade to SB 3.5.x** (prerequisite):
1. Fix the version catalog vs root BOM drift: pick one source of truth
2. Bump to Spring Boot 3.5.x, fix all deprecation warnings (SB 3.5 is the bridge that marks everything removed in 4.0 as deprecated)
3. Upgrade Gradle to 8.14+ (9.x recommended)

**Phase 1 — Spring Boot 4.0**:
4. Bump versions: SB 4.0, SC 2025.1, Gradle 9.x
5. Modular auto-configuration: adopt explicit starters
6. Jackson 2 → 3 migration
7. Spring Security 7 migration
8. Testing annotation updates
9. Hibernate 7.1 migration
10. Verification (compile, test, integration)

### 10.3 Affected Files — Detailed Breakdown

#### 10.3.1 Build Configuration (6 files)

**File**: `build.gradle.kts` (root)
- Replace `val springBootVersion = "3.3.5"` → `"4.0.x"`
- Replace `val springCloudVersion = "2023.0.3"` → `"2025.1.0"`
- BOM imports will transitively pull Framework 7, Security 7, Hibernate 7.1

**File**: `gradle/libs.versions.toml`
- Update version entries: `spring-boot = "4.0.x"`, `spring-cloud = "2025.1.0"`
- Jackson 3 coordinates: `tools.jackson.core:jackson-databind:3.0.x` (groupId changed!)
- `jackson-datatype-jsr310` → no longer needed (built into `JsonMapper`)
- Potentially remove `flyway-core` / `flyway-postgresql` if internal, or verify Hibernate 7.1 compatibility
- SpringDoc: check if `2.7.0` supports SB4 (may need `3.x`)

**File**: `gradle/wrapper/gradle-wrapper.properties`
- `gradle-8.10-bin.zip` → `gradle-9.0-bin.zip` (or at least 8.14)

**File**: `services/hermandad-service/build.gradle.kts`
- Starters may need modular equivalents:
  - `spring-boot-starter-web` → could use `spring-boot-starter-webmvc` (or keep classic bridge)
  - `spring-boot-starter-oauth2-resource-server` → may move to `spring-boot-starter-security-oauth2-resource-server`
- `com.fasterxml.jackson.datatype:jackson-datatype-jsr310` → remove (built into SB4 Jackson 3 auto-config)
- `springdoc-openapi-starter-webmvc-ui:2.7.0` → verify SB4 compatibility

**File**: `infrastructure/api-gateway/build.gradle.kts` (likely exists, check)
- Spring Cloud Gateway dependencies — verify compatibility with SC 2025.1

**File**: `services/hermandad-service/src/main/resources/application.yml`
- Some SB4 config properties renamed (check migration guide for `spring.*` → `spring.security.*` moves)

#### 10.3.2 Jackson 3 Migration — 4 source files + 1 dependency

Jackson 3 breaking changes for this project:
- Package: `com.fasterxml.jackson.databind` → `tools.jackson.databind`
- `ObjectMapper` → `JsonMapper` (immutable builder API, `JsonMapper.builder().build()`)
- `JsonProcessingException` (checked) → `JacksonException` (unchecked) — catch blocks change
- `jackson-datatype-jsr310` → built into SB4 auto-config, remove dependency
- `Jackson2JsonRedisSerializer` / `GenericJackson2JsonRedisSerializer` → need SB4-compatible equivalents
- Annotations stay at `com.fasterxml.jackson.annotation` (shared between v2 and v3)

**File**: `shared/common/src/main/java/.../JwtMembershipExtractor.java`
```java
// Current (Jackson 2)
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
// → Jackson 3: import tools.jackson.databind.json.JsonMapper;
//   JsonProcessingException → JacksonException (unchecked)
//   objectMapper.readValue() still works, but JsonMapper is preferred
```

**File**: `shared/common/src/test/java/.../JwtTestFactory.java`
```java
// Same pattern as above
// OBJECT_MAPPER.writeValueAsString() → JsonMapper.builder().build().writeValueAsString()
```

**File**: `services/hermandad-service/.../adapter/config/RedisConfig.java`
```java
// ObjectMapper → Spring Boot 4 auto-configures JsonMapper
// GenericJackson2JsonRedisSerializer → may need Jackson2JsonRedisSerializer overload updates
// Jackson2JsonRedisSerializer → constructor signature may change with Jackson 3
// JavaTimeModule → no longer needed
// SerializationFeature → still works (stays at com.fasterxml.jackson)
```
- `@Bean RedisCacheManagerBuilderCustomizer redisBuilderCustomizer(ObjectMapper objectMapper)` → parameter may auto-wire `JsonMapper` instead
- Wait for Spring Boot 4 Redis auto-configuration updates before changing

**File**: `services/hermandad-service/.../adapter/outbound/outbox/OutboxEventPublisher.java`
- `ObjectMapper` → `JsonMapper`, same pattern

#### 10.3.3 Spring Security 7 — 2 files

**File**: `services/hermandad-service/.../adapter/config/SecurityConfig.java`
✅ Already uses lambda DSL (`http.csrf(AbstractHttpConfigurer::disable)`) — this is good, method chaining removed in SS7
✅ Does NOT extend `WebSecurityConfigurerAdapter` (removed in SB3, not an issue)
⚠️ CSRF disabled explicitly — SS7 enables CSRF by default, but `.disable()` will still work if called
⚠️ OAuth2 resource server config syntax — check if `jwt(jwt -> {})` empty lambda still works or requires `.jwt(Customizer.withDefaults())`
⚠️ `AbstractHttpConfigurer::disable` method reference syntax — verify still compiles on SS7

**File**: `infrastructure/api-gateway/.../SecurityConfig.java`
- `ServerHttpSecurity` lambda DSL — likely OK
- CSRF disabled — verify SS7 Gateway compatibility

#### 10.3.4 Testing — 1 file (low impact)

**File**: `services/hermandad-service/.../HermandadServiceTest.java`
- Uses `@Mock` (Mockito), not `@MockBean` — ✅ no migration needed
- No `@SpringBootTest` with `@MockBean` — ✅ fine
- If integration tests are added later with `@MockBean`, that will need `@MockitoBean` replacement

#### 10.3.5 Hibernate 7.1 — domain entities

**Domain entities** (Hermandad, HermandadMember):
- `jakarta.persistence` annotations — ✅ no namespace change (EE 10→11 is additive)
- Hibernate 7.1 breaking changes may affect:
  - `@PreUpdate` / `@PrePersist` behavior (currently used in HermandadMember)
  - Sequence generation defaults
- Verify: `HibernateException` hierarchy may have changes

#### 10.3.6 Infrastructure — no code changes expected

- Prometheus, Grafana, ELK configs — no changes
- Docker Compose — no changes
- Keycloak realm export — no changes (OIDC protocol unaffected)
- Kafka — `spring-kafka` dependency updates via BOM

### 10.4 Dependency Compatibility Check

| Dependency | Current Version | SB4 Compatible? | Risk |
|------------|----------------|-----------------|------|
| springdoc-openapi 2.7.0 | 2.7.0 | ⚠️ Check — may need 3.x | Medium |
| keycloak-admin-client 24.0.3 | 24.0.3 | ⚠️ Jakarta EE 11 compat | Medium |
| testcontainers 1.20.1 | 1.20.1 | ✅ Likely fine (upgrade anyway) | Low |
| logstash-logback-encoder 7.4 | 7.4 | ⚠️ Check Logback compat | Low |
| flyway-core / flyway-postgresql | (via BOM) | ✅ Should follow SB BOM | Low |
| lombok | (via BOM) | ✅ Annotation processor, unaffected | None |

### 10.5 Migration Effort Estimate

| Area | Files Changed | Effort | Risk |
|------|--------------|--------|------|
| Build config (Gradle, version catalog) | 4 | Low | Medium |
| Jackson 3 package rename + API | 4 | Medium | High |
| Spring Security 7 | 2 | Low | Medium |
| Hibernate 7.1 | ~4 entities | Low | Medium |
| Testing annotations | 0 (for now) | None | Low |
| SpringDoc compatibility | 1 (version bump) | Low | Low |
| Keycloak client | 0 (version bump) | Low | Low |
| Verification & integration testing | — | High | High |

**Total estimated effort**: 2-3 days for a single service (hermandad-service), more if other services have code by then.

### 10.6 Key Recommendations

1. **Do not start yet** — migrate to SB 3.5.x first, fix all deprecation warnings, reconcile the version drift. SB 3.5 support runs through November 2026, so there's time.
2. **Use OpenRewrite** — the SB4 migration recipe automates 80%+ of the changes (package renames, property migrations, build file updates). Run `./gradlew rewriteRun` with the SB4 recipe.
3. **Jackson 3 is the highest-risk change** — especially `RedisConfig.java`. Test Redis serialization/deserialization thoroughly with integration tests before deploying.
4. **Add integration tests before migrating** — the project has zero integration tests. SB4 changes will break things at runtime that unit tests won't catch. Adding Testcontainers tests for the Redis + Jackson + JPA stack should be a prerequisite.
5. **Gradle 9 has its own breaking changes** (configuration cache, Kotlin DSL changes). Budget separate time for that upgrade, or go to 8.14 first.

---

## Appendix: Quick Fixes (Small Effort)

- Add `DataIntegrityViolationException` handler → 409 Conflict (Section 4.1)
- Add basic `@ExceptionHandler(Exception.class)` fallback (Section 4.1)
- Bump `payload` column to `TEXT` or `VARCHAR(4000)` (Section 9.3)
