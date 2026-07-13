# Service Reviews

## Procesion Service

**Files**: 22 Java files | **Tests**: 21 (8 unit + 13 controller), all passing
**Hexagonal**: ✅ Full | **DDD**: ✅ | **TDD**: ✅

### Positive

| Area | Status | Notes |
|------|--------|-------|
| Hexagonal Architecture | ✅ | Full ports/adapters split. Domain → Application/Port → Adapter |
| DDD | ✅ | State machine in `cambiarEstado()`, static factory, domain events |
| Security | ✅ | `anyRequest().authenticated()`, custom `JwtAuthenticationConverter` extracting `hermandad_memberships` |
| Testing | ✅ | Service unit (Mockito) + controller slice (MockMvc + JWT). Auth path tested |
| Build | ✅ | Modular `spring-boot-starter-webmvc`, Flyway, Kafka, OpenFeign |

### Issues

| # | Severity | Issue | Location |
|---|----------|-------|----------|
| 1 | Medium | **No outbox pattern** — events published via `ApplicationEventPublisher` (sync, in-process). If listener fails or app crashes after DB save, event is lost. Hermandad-service has an outbox | `DomainEventPublisherAdapter.java` |
| 2 | Medium | **`UUID.randomUUID()` in factory + `@GeneratedValue @UuidGenerator` on entity** — redundant ID generation. Pick one | `Procesion.java:16-17, 55` |
| 3 | Low | **Duplicate Kafka deps** — both `spring-kafka` and `spring-boot-starter-kafka` declared (starter includes the former) | `build.gradle.kts:20-21` |
| 4 | Low | **Double lookup in `eliminarProcesion()`** — `findById` then `deleteById`. Use `existsById()` or let delete throw | `ProcesionService.java:58-62` |
| 5 | Low | **No domain unit tests** — `Procesion.cambiarEstado()` state machine (5 transition rules, 4 states) tested only through service | `/domain/model/` |
| 6 | Low | **Plain text error responses** — same pattern as hermandad, should return structured `{code, message}` JSON | `GlobalExceptionHandler.java` |
| 7 | Low | **Flyway index not mirrored on entity** — `idx_procesion_hermandad_id` exists in SQL but not declared via `@Table(indexes = ...)` | `Procesion.java` |
| 8 | Note | **No Redis caching** — hermandad-service has it, could benefit read-heavy listings | — |

### Recommendations

1. **Critical before production**: Add outbox pattern (copy from hermandad-service: `OutboxEventEntity` + `OutboxPoller` + `OutboxEventPublisher`)
2. Remove `@GeneratedValue` from `Procesion.id` (let `@UuidGenerator` handle it)
3. Drop `spring-kafka` from `build.gradle.kts` (starter covers it)
4. Write direct domain tests for `Procesion.cambiarEstado()` — all valid/invalid transitions
5. Add `@Table(indexes = @Index(...))` to match Flyway index

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
| 1 | Low | **Plain text error responses** — `ResponseEntity<String>` instead of structured JSON `{code, message}`. Same across both services | `GlobalExceptionHandler.java` |
| 2 | Low | **Hermandad entity has no `updatedAt`** — only `createdAt`. Unlike Procesion which tracks both | `Hermandad.java:29-30` |
| 3 | Low | **`HermandadService.changeRole()` lacks `@Transactional`** — all other mutation methods have it | `HermandadService.java:109` |
| 4 | Note | **`Hermandad` constructor does not validate args** — name/city could be empty strings | `Hermandad.java:34-39` |
| 5 | Note | **`KeycloakUserExistenceAdapter.exists()` catches generic `Exception`** — could mask connectivity issues vs actual "not found" | `KeycloakUserExistenceAdapter.java:24` |

### Recommendations

1. Add `updatedAt` to `Hermandad` entity (inconsistency with Procesion)
2. Add `@Transactional` to `changeRole()` method
3. Both services: migrate from plain text to structured error responses with an `ErrorResponse` DTO
4. `KeycloakUserExistenceAdapter`: catch specific `javax.ws.rs.NotFoundException` for "not found", let other exceptions propagate
5. Validate constructor params in `Hermandad` (empty strings, negative foundedYear)

---

## Cross-Service Comparison

| Aspect | Hermandad | Procesion |
|--------|-----------|-----------|
| Hexagonal | ✅ | ✅ |
| DDD | ✅ | ✅ |
| Tests | 55 (11 classes) | 21 (2 classes) |
| Security | `anyRequest().authenticated()` + `@PreAuthorize` + custom JWT converter | `anyRequest().authenticated()` + custom JWT converter |
| Event reliability | Outbox + idempotent consumer | `ApplicationEventPublisher` only |
| Caching | Redis | None |
| Spring Boot | 4.1 (tools.jackson) | 3.5.x |
| Error responses | Plain text `String` | Plain text `String` |
| Domain tests | ✅ (HermandadTest, HermandadMemberTest) | ❌ |
