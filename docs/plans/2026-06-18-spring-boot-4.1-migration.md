# Spring Boot 4.1 Migration

**For me:** Execute tasks in order, verify between each.

**Goal:** `spring-boot 3.5.15` → `4.1.0` with zero regressions.

**Approach:** Bump version → compile → surface & fix errors → verify.

---

### Task 1: libs.versions.toml — version bumps + deps

**Changes:**
- `spring-boot = "4.1.0"`
- `spring-cloud` — likely `2025.0.3` doesn't work with Boot 4.1, may need `2026.0.x`
- `springdoc-starter` — may need bump to `3.0.3`
- Replace `flyway-core` + `flyway-database-postgresql` with `spring-boot-starter-flyway`
- Add `spring-boot-starter-jackson` library entry
- Remove `jackson-datatype-jsr310` (built into Jackson 3 core, no separate dep needed)
- Add `spring-boot-properties-migrator` (runtimeOnly, to catch renamed props)

### Task 2: Module build.gradle.kts — modular starter renames

Boot 4.1 drops the classic `spring-boot-starter-web` uber-starter. Need modular starters:

| File | Change |
|------|--------|
| Each module's `build.gradle.kts` | `spring-boot-starter-web` → `spring-boot-starter-webmvc` |
| Modules using Jackson | Add `spring-boot-starter-jackson` |
| hermandad-service | swap flyway deps for `spring-boot-starter-flyway`, add test starters |

---

### Task 3: First compile — `./gradlew clean build`

Collect ALL failures. Fix categories in order of likelihood:

**3a. Jackson 3 package renames (highest impact)**
`com.fasterxml.jackson` → `tools.jackson` in Java imports.
Exception: `com.fasterxml.jackson.annotation.*` still works with Jackson 3.

**Specific files to check:**
- ObjectMapper usage: `JwtMembershipExtractor.java`, `JwtTestFactory.java`, `OutboxEventPublisher.java`, `RedisConfig.java`
- Entity/DTO annotations: `ValueObject.java`, `Entity.java`, `ErrorResponse.java`, plus DTOs across all services

**3b. Redis serializer renames**
Spring Data Redis 4.x renames:
- `GenericJackson2JsonRedisSerializer` → `GenericJacksonJsonRedisSerializer`
- `Jackson2JsonRedisSerializer` → `JacksonJsonRedisSerializer`
- Remove `JavaTimeModule` registration (built into Jackson 3)

**3c. Hibernate 7 API removals** — low risk, check on first compile
**3d. Flyway version change** — low risk
**3e. Spring Security Lambda DSL** — low risk, verify compiles

---

### Task 4: Verify no runtime surprises

**Clean (no changes needed):**
- Kafka ✅ — `KafkaTemplate.send()` returns `CompletableFuture` since 3.0, broker compatible
- Java 21 ✅ — already set in `gradle.properties`
- Testcontainers ✅ — managed by Boot BOM
- Keycloak ✅ — JWT format unchanged
- Docker images ✅ — all use `eclipse-temurin:21-jre-alpine`
- Postgres/Redis infrastructure ✅ — unchanged

**Check:**
- `spring.cloud.*` config properties still valid
- Flyway migration scripts run with new version
- `spring-boot-properties-migrator` catches any renamed properties — remove it before final commit
- Embedded H2 dialect compatibility (Hibernate 7)

**Known risks:**
| Risk | Level | Notes |
|------|-------|-------|
| Redis serializer API removed | High | Mechanical rename, caught at compile time |
| Jackson 3 serialization defaults change | Medium | `JavaTimeModule` gone, date format may shift in Redis. Add `WRITE_DATES_AS_TIMESTAMPS` if tests fail |
| Springdoc incompatible with Boot 4.1 | Medium | Detect on first build, bump if needed |
| Hibernate 7 JPA breakage | Low | We only use standard JPA annotations |

---

### Task 4: Final verification

- `./gradlew clean build` — green
- `./gradlew test` — all pass
- Spotless/checkstyle — clean
- Docker Compose up — app starts, smoke test endpoints
