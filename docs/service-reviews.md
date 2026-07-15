# Service Reviews

## Procesion Service

**Files**: 22 Java files | **Tests**: 47 (11 domain + 8 service + 13 controller + 4 repository IT + 8 controller IT + 3 exception handler)
**Hexagonal**: ✅ Full | **DDD**: ✅ | **TDD**: ✅
**Last review**: 2026-07-13

### Positive

| Area | Status | Notes |
|------|--------|-------|
| Hexagonal Architecture | ✅ | Full ports/adapters split. Domain → Application/Port → Adapter |
| DDD | ✅ | State machine in `changeStatus()`, static factory, domain events |
| Security | ✅ | `anyRequest().authenticated()`, custom `JwtAuthenticationConverter` extracting `hermandad_memberships` |
| Testing | ✅ | Domain unit tests (11), service unit (8), controller slice (13), integration (12), exception handler (3) |
| Event reliability | ✅ | Outbox pattern (table + poller → Kafka `procesion-events` topic). Mirror of hermandad-service |
| Build | ✅ | Modular `spring-boot-starter-webmvc`, Flyway, Kafka |

### Issues

| # | Severity | Issue | Location |
|---|----------|-------|----------|
| 1 | Low | ✅ ~~**Flyway index not mirrored on entity** — `idx_procesion_hermandad_id` exists in SQL but not declared via `@Table(indexes = ...)`~~ | `Procesion.java` |
| 2 | Low | **No `@PreAuthorize` on controller** — `@EnableMethodSecurity` declared but unused; all controller methods rely on blanket `anyRequest().authenticated()` | `ProcesionController.java` |
| 3 | Low | ✅ ~~**Dead `@EnableFeignClients`** — annotation present with zero `@FeignClient` interfaces in codebase~~ | `ProcesionServiceApplication.java:12` + `build.gradle.kts:18` |
| 4 | Note | **No Redis caching** — hermandad-service has it, could benefit read-heavy listings | — |

### Recommendations

1. ✅ ~~Add `@Table(indexes = @Index(...))` to `Procesion.java` to match Flyway index~~
2. ✅ ~~Remove `@EnableFeignClients` + `spring-cloud-starter-openfeign` (dead config)~~
3. Optionally add `@PreAuthorize` for consistent method-level security

---

## Hermandad Service (updated review)

**Files**: 59 Java files (was 36) | **Tests**: 55 across 11 classes, all passing
**Previous audit findings**: 9 issues identified — **6 fixed**, 3 remain
**SB4 migration**: Already on 4.1 (`tools.jackson`, `spring-boot-properties-migrator`)

### What Changed (since last audit)

| Change | Commit | Impact |
|--------|--------|--------|
| **Security overhaul** | `644d4cf` | `anyRequest().authenticated()`, dual-path: public GET, admin-only mutations via `@PreAuthorize` + `HermandadSecurityService` |
| **JwtAuthenticationConverter** | `644d4cf` | Custom converter extracts `hermandad_memberships` claim into authorities |
| **Pagination for members** | `e2cb80f` | `Pageable`-backed listing endpoint |
| **Keycloak user validation** | `aa02afc` | Checks user exists before adding member |
| **Idempotent Kafka consumer** | `f9aa454` | `processed_event` table dedup |
| **Member removal (DELETE)** | `2e61162` | New endpoint + service + event |
| **Unique name constraint** | `ecb29c2` | Domain exception + DB unique constraint |
| **Description field** | `554e8b3` | Added to Hermandad entity |
| **Outbox batch limit + ORDER BY** | `f5a763e` | `findTop100ByProcessedFalseOrderByCreatedAtAsc` |
| **Integration tests** | `a453c0d`, `3b6782f` | Repository integration tests + controller integration test with pagination |
| **Spring Boot 4.1 migration** | `f5501a2` | Migrated to `tools.jackson`, added `spring-boot-properties-migrator` |
| **OpenApiConfig** | — | Swagger with bearer auth, gateway server |

### Positive

- **Security gap closed**: Previously `.permitAll()` on everything. Now has proper auth with `@PreAuthorize` + role-based access + public GET for hermandades listing
- **Event reliability**: Outbox pattern with batch processing + ORDER BY (previously unstructured). Idempotent consumer via `processed_event` table
- **Domain events**: `MemberRemovedEvent` now exists. All events include hermandadId
- **Testing**: 11 test classes, 55 tests — covers controllers, services, domain, Kafka consumer, exception handlers, Keycloak adapter, JPA repositories
- **State machine**: `HermandadMember.changeRole()` validates no-op (`role == newRole → throw`)
- **Caching**: Redis caching on hermandad lookups + `@CacheEvict` on member changes

### Remaining Issues

| # | Severity | Issue | Location |
|---|----------|-------|----------|
| 1 | Note | ✅ ~~**`Hermandad` constructor does not validate args** — name/city could be empty strings~~ | `Hermandad.java:34-39` |
| 2 | Note | **Kafka self-consumption** — `IdempotentEventConsumer` listens to hermandad's own topics. Works as dedup audit trail but unusual pattern | `IdempotentEventConsumer.java` |

### Resolved Since Last Review

| Issue | Resolution |
|-------|-----------|
| **Plain text error responses** | ✅ Both services now return `ApiError` JSON record with `status, error, message`. Unit tests per handler. |
| **Hermandad entity missing `updatedAt`** | ✅ Added `updatedAt` field + `@PrePersist`/`@PreUpdate` + Flyway V7 migration. |
| **`changeRole()` missing `@Transactional`** | ✅ Added `@Transactional` to `addMember()`, `changeRole()`, `removeMember()` (all write operations). |
| **Hermandad constructor no validation** | ✅ Added null/blank checks for `name`, `city` and negative check for `foundedYear`. 5 tests added. |
| **`KeycloakUserExistenceAdapter` generic catch** | ✅ Split: `NotFoundException` (debug log, return false) + `Exception` (warn log, return false). `KeycloakMembershipAdapter` now re-throws instead of swallowing. `MemberAddedListener` catches `RuntimeException`. `OutboxEventPublisher` catches `JsonProcessingException`. |

### Recommendations

1. Add `updatedAt` to `Hermandad` entity (inconsistency with Procesion)
2. Add `@Transactional` to `changeRole()` method
3. Both services: migrate from plain text to structured error responses with an `ErrorResponse` DTO
4. `KeycloakUserExistenceAdapter`: catch specific `javax.ws.rs.NotFoundException` for "not found", let other exceptions propagate
5. ✅ ~~Validate constructor params in `Hermandad` (empty strings, negative foundedYear)~~

---

## Repertorio Service

**Files**: 42 Java files | **Tests**: 44 (6 domain + 17 service + 19 controller + 0 integration)
**Hexagonal**: ✅ Full | **DDD**: ✅ | **TDD**: ✅
**Last review**: 2026-07-15

### Positive

| Area | Status | Notes |
|------|--------|-------|
| Hexagonal Architecture | ✅ | Full ports/adapters split. Domain → Application/Port → Adapter |
| DDD | ✅ | State in `Marcha` constructor validation, `Cruceta.redefine()`, domain events |
| Security | ✅ | `anyRequest().authenticated()`, custom `JwtAuthenticationConverter` extracting `hermandad_memberships` |
| Testing | ✅ | Domain unit tests (14), service tests (17), controller slice (19) |
| Event reliability | ✅ | Outbox pattern (table + poller → Kafka `marcha-events` topic) |
| Build | ✅ | Modular `spring-boot-starter-webmvc`, Flyway, Kafka, Dockerfile |
| Seed data | ✅ | V3 seeds 15 iconic Semana Santa marchas with diverse band types and composers |

### Issues

| # | Severity | Issue | Location |
|---|----------|-------|----------|
| 1 | 🔴 High | **No integration tests** — unlike hermandad (2 IT) and procesion (2 IT), repertorio has zero repository or controller integration tests | `src/test/` |
| 2 | 🟡 Medium | **No Kafka consumer** — `procesion-events` topic has no consumer to handle procesion deletion cleanup (cruceta should cascade) | — |
| 3 | 🟡 Medium | **Hibernate 7 UUID issue may affect repertorio too** — Procesion needed `Persistable` interface fix. Repertorio entities use same `@UuidGenerator` pattern | All repertorio entities |
| 4 | 🟠 Low | **No `@PreAuthorize`** — `@EnableMethodSecurity` + `RepertorioSecurityService` declared but unused | `MarchaController.java`, `CrucetaController.java` |
| 5 | 🟠 Low | **No Redis caching** — consistent with procesion but missing compared to hermandad | — |

### Recommendations

1. Write integration tests (repository + controller) to match hermandad/procesion pattern
2. Add Kafka consumer for `procesion-events` to clean up cruceta on procesion deletion
3. Verify repertorio entities don't hit the same Hibernate 7 UUID save issue
4. Optionally add `@PreAuthorize` for consistent method-level security across all services

---

| Aspect | Hermandad | Procesion | Repertorio |
|--------|-----------|-----------|------------|
| Hexagonal | ✅ | ✅ | ✅ |
| DDD | ✅ | ✅ | ✅ |
| Tests | 56 (12 classes, 2 IT) | 47 (6 classes, 2 IT) | 44 (6 classes, 0 IT) |
| Security | `@PreAuthorize` + custom JWT converter + SecurityService | `anyRequest().authenticated()` + custom JWT converter | `anyRequest().authenticated()` + custom JWT converter + RepertorioSecurityService (unused) |
| Event reliability | Outbox + idempotent consumer | Outbox | Outbox |
| Caching | Redis | None | None |
| Spring Boot | 4.1 (tools.jackson) | 4.1 (tools.jackson) | 4.1 (tools.jackson) |
| Error responses | ✅ `ApiError` JSON | ✅ `ApiError` JSON | ✅ `ApiError` JSON |
| Domain tests | ✅ (HermandadTest, HermandadMemberTest) | ✅ (ProcesionTest, 11 state machine tests) | ✅ (MarchaTest, CrucetaTest, 14 total) |
| Integration tests | ✅ Repository + Controller | ✅ Repository + Controller | ❌ None |
| Flyway migrations | V1–V7 (7) | V1–V4 (4) | V1–V4 (4) |
| Docker | ✅ Dockerfile + compose entry | ✅ Dockerfile + compose entry | ✅ Dockerfile + compose entry |
| Gateway routes | ✅ `/api/hermandades/**` | ✅ `/api/procesiones/**` | ✅ `/api/marchas/**` + `/api/hermandades/*/procesiones/*/cruceta/**` |
| Kafka consumer | ✅ Self-consumption (idempotent) | ❌ None | ❌ None |
| Port | 8081 | 8082 | 8083 |
