# Sprint 6 — Hermandad Service Polish Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Close remaining hermandad-service gaps: members pagination, CAPATAZ role in OpenAPI, Keycloak user validation.

**Architecture:** All items are vertical slices within the existing hexagonal structure. Pagination uses Spring Data `Pageable`. Keycloak validation adds a new outbound port (`UserExistencePort`) with a Keycloak admin REST adapter.

**Tech Stack:** Spring Boot 4.1, Spring Data JPA, Keycloak Admin Client 24, springdoc-openapi

---

### Task 1: Members List Pagination

**Files:**
- Modify: `services/hermandad-service/src/main/java/.../adapter/inbound/rest/controller/HermandadController.java` (add `Pageable` param to `getHermandadMembers`)
- Modify: `services/hermandad-service/src/main/java/.../domain/repository/HermandadMemberRepository.java` (port: `findByHermandadId(UUID, Pageable)`)
- Modify: `services/hermandad-service/src/main/java/.../adapter/outbound/persistence/HermandadMemberJpaRepository.java` (Spring Data `Page<HermandadMemberEntity>` query)
- Modify: `services/hermandad-service/src/main/java/.../adapter/outbound/persistence/HermandadMemberRepositoryAdapter.java` (adapt the `Page` response)
- Modify: `services/hermandad-service/src/main/java/.../application/service/HermandadService.java` (accept `Pageable`, return page)
- Test: `services/hermandad-service/src/test/java/.../application/service/HermandadServiceTest.java` (existing, add pagination assertions)
- Test: `services/hermandad-service/src/test/java/.../adapter/inbound/rest/controller/HermandadControllerTest.java` (existing, add paginated response assertion)

**Step 1: Write failing test for service pagination**

In `HermandadServiceTest`:
```java
@Test
void listMembersReturnsPagedResults() {
    UUID hermandadId = UUID.randomUUID();
    var pageRequest = PageRequest.of(0, 10);
    var memberPage = new PageImpl<>(List.of(new HermandadMember(hermandadId, "user-1", HermandadRole.MUSICIAN)));

    when(hermandadRepository.existsById(hermandadId)).thenReturn(true);
    when(memberRepository.findByHermandadId(hermandadId, pageRequest)).thenReturn(memberPage);

    var result = hermandadService.getHermandadMembers(hermandadId, pageRequest);

    assertThat(result.getTotalElements()).isEqualTo(1);
    assertThat(result.getContent().get(0).getUserId()).isEqualTo("user-1");
}
```

**Step 2: Run test to verify it fails**

Run: `./gradlew :services:hermandad-service:test --tests "*HermandadServiceTest.listMembersReturnsPagedResults" --no-daemon`
Expected: Compilation error or test failure

**Step 3: Update port interface**

`HermandadMemberRepository.java`: Change `findByHermandadId(UUID)` to `findByHermandadId(UUID, Pageable)` returning `Page<HermandadMember>`.

**Step 4: Update JPA repository**

`HermandadMemberJpaRepository.java`: Add `Page<HermandadMemberEntity> findByHermandadId(UUID hermandadId, Pageable pageable)`.

**Step 5: Update repository adapter**

`HermandadMemberRepositoryAdapter.java`: Adapt the `Page<HermandadMemberEntity>` to `Page<HermandadMember>` using `map()`.

**Step 6: Update service**

`HermandadService.getHermandadMembers()`: Accept `Pageable` parameter, pass through to repository.

**Step 7: Update controller**

`HermandadController.getHermandadMembers()`: Add `@PageableDefault(size = 20)` `Pageable pageable` parameter. Return `Page<HermandadMember>`.

**Step 8: Run all tests to verify pass**

Run: `./gradlew :services:hermandad-service:test --no-daemon`
Expected: All tests pass

**Step 9: Commit**

```bash
git add -A
git commit -m "feat: add pagination to members list endpoint"
```

---

### Task 2: CAPATAZ Role in OpenAPI Spec

**Files:**
- Modify: `docs/openapi.yaml` — add `CAPATAZ` to `AddMemberRequest.role` and `HermandadMember.role` enums, add role-permission matrix comment

**Step 1: Read current OpenAPI spec**

Check `docs/openapi.yaml` for the role enum locations.

**Step 2: Update the spec**

Add `CAPATAZ` to:
- `AddMemberRequest.role` enum
- `HermandadMember.role` enum
- `ChangeRoleRequest.role` enum

**Step 3: Commit**

```bash
git add docs/openapi.yaml
git commit -m "docs: add CAPATAZ role to OpenAPI spec"
```

---

### Task 3: Keycloak User Existence Validation

**Files:**
- Create: `services/hermandad-service/src/main/java/.../application/port/UserExistencePort.java`
- Create: `services/hermandad-service/src/main/java/.../adapter/outbound/keycloak/KeycloakUserExistenceAdapter.java`
- Modify: `services/hermandad-service/src/main/java/.../application/service/HermandadService.java` (inject `UserExistencePort`, check before adding member)
- Test: `services/hermandad-service/src/test/java/.../application/service/HermandadServiceTest.java` (add test for user-not-found → 400)
- Test: `services/hermandad-service/src/test/java/.../adapter/outbound/keycloak/KeycloakUserExistenceAdapterTest.java` (unit test with mocked admin client)

**Step 1: Write port interface**

```java
package com.repertorio.hermandad.application.port;

public interface UserExistencePort {
    boolean exists(String userId);
}
```

**Step 2: Write failing service test**

```java
@Test
void addMemberThrowsWhenUserNotFound() {
    UUID hermandadId = UUID.randomUUID();
    AddMemberRequest request = new AddMemberRequest("nonexistent-user", HermandadRole.MUSICIAN);

    when(hermandadRepository.existsById(hermandadId)).thenReturn(true);
    when(userExistencePort.exists("nonexistent-user")).thenReturn(false);

    assertThrows(IllegalArgumentException.class,
        () -> hermandadService.addMember(hermandadId, request));
}
```

**Step 3: Update service to check user existence**

```java
if (!userExistencePort.exists(request.userId())) {
    throw new IllegalArgumentException("User does not exist in Keycloak: " + request.userId());
}
```

**Step 4: Write Keycloak adapter**

`KeycloakUserExistenceAdapter`:
```java
@Component
@RequiredArgsConstructor
public class KeycloakUserExistenceAdapter implements UserExistencePort {
    private final Keycloak keycloak;
    private static final String REALM = "semana-santa";

    @Override
    public boolean exists(String userId) {
        try {
            var users = keycloak.realm(REALM).users().searchByUsername(userId, true);
            return !users.isEmpty();
        } catch (NotFoundException e) {
            return false;
        }
    }
}
```

**Step 5: Write adapter unit test**

Mock the Keycloak admin client, verify `exists()` returns true/false correctly.

**Step 6: Wire in security context**

The `HermandadService.addMember()` endpoint is called from the controller. No change to the controller needed — the service throws `IllegalArgumentException` which `GlobalExceptionHandler` maps to 400.

**Step 7: Run all tests**

Run: `./gradlew :services:hermandad-service:test --no-daemon`
Expected: All tests pass

**Step 8: Commit**

```bash
git add -A
git commit -m "feat: validate Keycloak user existence before adding member"
```

---

### Task 4: Integration Test for Pagination (Bonus)

**Files:**
- Create: `services/hermandad-service/src/test/java/.../adapter/inbound/rest/controller/HermandadControllerIntegrationTest.java`

**Step 1: Write `@SpringBootTest` + Testcontainers test**

Use Testcontainers PostgreSQL (reuse `HermandadRepositoryIntegrationTest` pattern). Test that:
- `GET /api/hermandades/{id}/members?page=0&size=5` returns a paginated response with correct structure
- Uses `@MockitoBean` for `IdempotentEventConsumer` and Keycloak dependencies

**Step 2: Run test**

Expected: Test passes with running PostgreSQL

**Step 3: Commit**

```bash
git add -A
git commit -m "test: add integration test for members pagination"
```

---

## Summary

| Task | Effort | Dependencies |
|------|--------|-------------|
| 1. Pagination | Medium | None |
| 2. CAPATAZ in OpenAPI | Trivial | None |
| 3. Keycloak user validation | Medium | Task 1 (same files) |
| 4. Integration test | Low | Task 1 |
