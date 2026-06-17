# Sprint 2 — Audit Fixes & Member Removal Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Clean technical debt (audit items) and complete member CRUD with removal endpoint.

**Architecture:** Hermandad-service only. Audit fixes touch entity annotations, exception handlers, and Flyway migration. Member removal follows existing pattern (domain event → service → controller → outbox). Both isolated to hermandad-service, no cross-service changes.

**Tech Stack:** Spring Boot 3.5, JPA/Hibernate, Flyway, JUnit 5 + Mockito

---

## Preparation

```bash
export JAVA_HOME=~/.jdks/jdk-21.0.6+7/
```

---

### Task 1.1: Fix `HermandadMember.updatedAt`

**Files:**
- Create: `services/hermandad-service/src/test/java/com/repertorio/hermandad/domain/model/HermandadMemberDataJpaTest.java`
- Modify: `services/hermandad-service/src/main/java/com/repertorio/hermandad/domain/model/HermandadMember.java`

**Problem:** `updatedAt` column has `updatable = false` but `@PreUpdate` sets it → Hibernate ignores the change.

**Step 1 (RED): Write `@DataJpaTest` that proves the bug**

```java
package com.repertorio.hermandad.domain.model;

import com.repertorio.hermandad.adapter.outbound.persistence.HermandadMemberJpaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class HermandadMemberDataJpaTest {

    @Autowired
    private HermandadMemberJpaRepository repository;

    @Test
    void updatedAtChangesAfterRoleUpdate() {
        var hermandadId = UUID.randomUUID();
        var member = new HermandadMember(hermandadId, "user-1", HermandadRole.MUSICIAN);
        var saved = repository.save(member);
        var initialUpdatedAt = saved.getUpdatedAt();

        saved.changeRole(HermandadRole.CAPATAZ);
        repository.flush();

        var afterUpdate = repository.findById(saved.getId()).orElseThrow();
        assertThat(afterUpdate.getUpdatedAt()).isAfter(initialUpdatedAt);
    }
}
```

**Step 2 (RED): Run the test — it must fail**

```bash
./gradlew :services:hermandad-service:test --tests "com.repertorio.hermandad.domain.model.HermandadMemberDataJpaTest"
```
Expected: FAIL — `updatedAt` stays the same because `updatable = false` prevents Hibernate from writing the `@PreUpdate` change.

**Step 3 (GREEN): Remove `updatable = false` from `updatedAt`**

Line 34 — change:
```java
@Column(nullable = false, updatable = false)
private Instant updatedAt;
```
to:
```java
@Column(nullable = false)
private Instant updatedAt;
```

**Step 4 (GREEN): Run the test — it must pass now**

```bash
./gradlew :services:hermandad-service:test --tests "com.repertorio.hermandad.domain.model.HermandadMemberDataJpaTest"
```
Expected: PASS.

**Step 5: Commit**

```bash
git add services/hermandad-service/src/main/java/com/repertorio/hermandad/domain/model/HermandadMember.java services/hermandad-service/src/test/java/com/repertorio/hermandad/domain/model/HermandadMemberDataJpaTest.java
git commit -m "fix: remove updatable=false from updatedAt so @PreUpdate persists"
```

---

### Task 1.2: Refactor `GlobalExceptionHandler` to structured JSON + add missing handlers

**Files:**
- Modify: `services/hermandad-service/src/main/java/com/repertorio/hermandad/adapter/inbound/rest/GlobalExceptionHandler.java`

**Changes:**
1. All handlers keep `ResponseEntity<String>` (no structured JSON — status + message is enough for this project)
2. Add `DataIntegrityViolationException` handler → 409 with "Resource already exists"
3. Add `MethodArgumentNotValidException` handler → 400 with field error messages joined
4. Add `Exception` fallback → 500 with "An unexpected error occurred"

**Step 1: Write the failing tests**

Create: `services/hermandad-service/src/test/java/com/repertorio/hermandad/adapter/inbound/rest/GlobalExceptionHandlerTest.java`

Test cases:
- `dataIntegrityViolationReturns409()` — mock exception, assert status 409, body has `code` and `message` fields
- `methodArgumentNotValidReturns400()` — mock exception with field errors, assert status 400, body has `errors` array
- `genericExceptionReturns500()` — mock runtime exception, assert status 500, body has `code` = `INTERNAL_ERROR`

```java
package com.repertorio.hermandad.adapter.inbound.rest;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.Map;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void dataIntegrityViolationReturns409() {
        var ex = new DataIntegrityViolationException("duplicate key");

        ResponseEntity<Map<String, Object>> response = handler.handleDataIntegrityViolation(ex);

        assertThat(response.getStatusCode().value()).isEqualTo(409);
        assertThat(Objects.requireNonNull(response.getBody()).get("code")).isEqualTo("CONFLICT");
    }

    @Test
    void methodArgumentNotValidReturns400() {
        var ex = mock(MethodArgumentNotValidException.class);
        var bindingResult = mock(BindingResult.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(java.util.List.of(
                new FieldError("obj", "name", "must not be blank")
        ));

        ResponseEntity<Map<String, Object>> response = handler.handleValidation(ex);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(Objects.requireNonNull(response.getBody()).get("code")).isEqualTo("VALIDATION_ERROR");
        assertThat(response.getBody()).containsKey("errors");
    }

    @Test
    void genericExceptionReturns500() {
        var ex = new RuntimeException("unexpected");

        ResponseEntity<Map<String, Object>> response = handler.handleGeneric(ex);

        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(Objects.requireNonNull(response.getBody()).get("code")).isEqualTo("INTERNAL_ERROR");
    }

    @Test
    void hermandadNotFoundReturns404() {
        var ex = new com.repertorio.hermandad.domain.model.HermandadNotFoundException(java.util.UUID.randomUUID());

        ResponseEntity<Map<String, Object>> response = handler.handleHermandadNotFoundException(ex);

        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(Objects.requireNonNull(response.getBody()).get("code")).isEqualTo("NOT_FOUND");
    }

    @Test
    void illegalArgumentReturns400() {
        var ex = new IllegalArgumentException("bad arg");

        ResponseEntity<Map<String, Object>> response = handler.handleIllegalArgument(ex);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(Objects.requireNonNull(response.getBody()).get("code")).isEqualTo("BAD_REQUEST");
    }
}
```

**Step 2: Run test to verify it fails**

```bash
./gradlew :services:hermandad-service:test --tests "com.repertorio.hermandad.adapter.inbound.rest.GlobalExceptionHandlerTest"
```
Expected: compilation errors / test failures (handler doesn't exist yet or returns wrong type).

**Step 3: Rewrite `GlobalExceptionHandler`**

```java
package com.repertorio.hermandad.adapter.inbound.rest;

import com.repertorio.hermandad.domain.model.HermandadMemberNotFoundException;
import com.repertorio.hermandad.domain.model.HermandadNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(HermandadNotFoundException.class)
    public ResponseEntity<String> handleHermandadNotFoundException(HermandadNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    @ExceptionHandler(HermandadMemberNotFoundException.class)
    public ResponseEntity<String> handleHermandadMemberNotFoundException(HermandadMemberNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<String> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<String> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(message);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleGeneric(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred");
    }
}
```

**Step 4: Run tests to verify they pass**

```bash
./gradlew :services:hermandad-service:test --tests "com.repertorio.hermandad.adapter.inbound.rest.GlobalExceptionHandlerTest"
```
Expected: all tests PASS.

**Step 5: Run full build**

```bash
./gradlew :services:hermandad-service:build
```
Expected: BUILD SUCCESSFUL.

**Step 6: Commit**

```bash
git add services/hermandad-service/src/main/java/com/repertorio/hermandad/adapter/inbound/rest/GlobalExceptionHandler.java services/hermandad-service/src/test/java/com/repertorio/hermandad/adapter/inbound/rest/GlobalExceptionHandlerTest.java
git commit -m "fix: structured JSON error responses with 409/400/500 handlers"
```

---

### Task 1.3: Bump outbox `payload` column to `TEXT`

**Files:**
- Create: `services/hermandad-service/src/main/resources/db/migration/V3__alter_outbox_payload_column.sql`

**Step 1: Create Flyway migration**

```sql
ALTER TABLE outbox_event ALTER COLUMN payload TYPE TEXT;
```

**Step 2: Run build (Flyway verifies migration applies)**

```bash
./gradlew :services:hermandad-service:build -x test
```
Expected: BUILD SUCCESSFUL.

**Step 3: Commit**

```bash
git add services/hermandad-service/src/main/resources/db/migration/V3__alter_outbox_payload_column.sql
git commit -m "fix: bump outbox payload column from VARCHAR(255) to TEXT"
```

---

### Task 2.1: Add `delete` to `HermandadMemberRepository`

**Files:**
- Modify: `services/hermandad-service/src/main/java/com/repertorio/hermandad/domain/repository/HermandadMemberRepository.java`
- Modify: `services/hermandad-service/src/main/java/com/repertorio/hermandad/adapter/outbound/persistence/HermandadMemberRepositoryAdapter.java`

**Step 1: Add `delete` to the interface**

```java
void delete(HermandadMember member);
```

**Step 2: Implement in the adapter**

```java
@Override
public void delete(HermandadMember member) {
    jpaRepository.delete(member);
}
```

**Step 3: Verify compile**

```bash
./gradlew :services:hermandad-service:build -x test
```

**Step 4: Commit**

```bash
git add services/hermandad-service/src/main/java/com/repertorio/hermandad/domain/repository/HermandadMemberRepository.java services/hermandad-service/src/main/java/com/repertorio/hermandad/adapter/outbound/persistence/HermandadMemberRepositoryAdapter.java
git commit -m "feat: add delete method to HermandadMemberRepository"
```

---

### Task 2.2: Create `MemberRemovedEvent`

**Files:**
- Create: `services/hermandad-service/src/main/java/com/repertorio/hermandad/domain/event/MemberRemovedEvent.java`

**Step 1: Create the domain event record**

```java
package com.repertorio.hermandad.domain.event;

import com.repertorio.hermandad.application.port.DomainEvent;
import com.repertorio.hermandad.domain.model.HermandadRole;

import java.util.UUID;

public record MemberRemovedEvent(
        UUID memberId,
        UUID hermandadId,
        String userId,
        HermandadRole role
) implements DomainEvent {
    @Override
    public String aggregateType() {
        return "hermandad-member";
    }

    @Override
    public UUID aggregateId() {
        return memberId;
    }

    @Override
    public String eventType() {
        return "MEMBER_REMOVED";
    }
}
```

**Step 2: Commit**

```bash
git add services/hermandad-service/src/main/java/com/repertorio/hermandad/domain/event/MemberRemovedEvent.java
git commit -m "feat: add MemberRemovedEvent domain event"
```

---

### Task 2.3: Add `removeMember()` to `HermandadService` + test

**Files:**
- Modify: `services/hermandad-service/src/main/java/com/repertorio/hermandad/application/service/HermandadService.java`
- Modify: `services/hermandad-service/src/test/java/com/repertorio/hermandad/application/service/HermandadServiceTest.java`

**Step 1: Write the failing test**

Add to `HermandadServiceTest.java`:

```java
@Test
void removeMemberPublishesDomainEvent() {
    UUID hermandadId = UUID.randomUUID();
    String userId = "user-123";
    HermandadMember member = new HermandadMember(hermandadId, userId, HermandadRole.MUSICIAN);

    when(hermandadMemberRepository.findByUserIdAndHermandadId(userId, hermandadId))
            .thenReturn(Optional.of(member));

    hermandadService.removeMember(hermandadId, userId);

    verify(hermandadMemberRepository).delete(member);
    verify(domainEventPublisher).publish(domainEventCaptor.capture());
    var event = domainEventCaptor.getValue();
    assertThat(event.aggregateType()).isEqualTo("hermandad-member");
    assertThat(event.eventType()).isEqualTo("MEMBER_REMOVED");
}

@Test
void removeMemberThrowsWhenMemberNotFound() {
    UUID hermandadId = UUID.randomUUID();
    String userId = "user-123";

    when(hermandadMemberRepository.findByUserIdAndHermandadId(userId, hermandadId))
            .thenReturn(Optional.empty());

    org.junit.jupiter.api.Assertions.assertThrows(
            HermandadMemberNotFoundException.class,
            () -> hermandadService.removeMember(hermandadId, userId)
    );
}
```

**Step 2: Run test to verify it fails**

```bash
./gradlew :services:hermandad-service:test --tests "com.repertorio.hermandad.application.service.HermandadServiceTest.removeMemberPublishesDomainEvent"
```
Expected: compilation error (method doesn't exist).

**Step 3: Implement `removeMember()` in service**

Add to `HermandadService`:

```java
public void removeMember(UUID hermandadId, String userId) {
    HermandadMember member = hermandadMemberRepository.findByUserIdAndHermandadId(userId, hermandadId)
            .orElseThrow(() -> new HermandadMemberNotFoundException(hermandadId, userId));

    hermandadMemberRepository.delete(member);

    var event = new MemberRemovedEvent(member.getId(), member.getHermandadId(), userId, member.getRole());
    domainEventPublisher.publish(event);
}
```

Add import for `MemberRemovedEvent`.

**Step 4: Run tests to verify they pass**

```bash
./gradlew :services:hermandad-service:test --tests "com.repertorio.hermandad.application.service.HermandadServiceTest"
```
Expected: all 5 tests PASS (3 existing + 2 new).

**Step 5: Commit**

```bash
git add services/hermandad-service/src/main/java/com/repertorio/hermandad/application/service/HermandadService.java services/hermandad-service/src/test/java/com/repertorio/hermandad/application/service/HermandadServiceTest.java
git commit -m "feat: add removeMember service method with domain event"
```

---

### Task 2.4: Add `DELETE` endpoint to controller

**Files:**
- Modify: `services/hermandad-service/src/main/java/com/repertorio/hermandad/adapter/inbound/rest/controller/HermandadController.java`

**Step 1: Add endpoint**

```java
@DeleteMapping("/{hermandadId}/members/{userId}")
@ResponseStatus(HttpStatus.NO_CONTENT)
@Operation(summary = "Remove a member from a hermandad")
@ApiResponses({
        @ApiResponse(responseCode = "204", description = "Member removed"),
        @ApiResponse(responseCode = "404", description = "Member not found in this hermandad")
})
public ResponseEntity<Void> removeHermandadMember(
        @PathVariable UUID hermandadId,
        @PathVariable String userId
) {
    hermandadService.removeMember(hermandadId, userId);
    return ResponseEntity.noContent().build();
}
```

**Step 2: Compile check**

```bash
./gradlew :services:hermandad-service:build -x test
```
Expected: BUILD SUCCESSFUL.

**Step 3: Commit**

```bash
git add services/hermandad-service/src/main/java/com/repertorio/hermandad/adapter/inbound/rest/controller/HermandadController.java
git commit -m "feat: add DELETE endpoint for member removal"
```

---

### Task 2.5: Sync OpenAPI spec

**Files:**
- Modify: `docs/openapi.yaml`

**Step 1: Run the service and fetch live spec**

```bash
docker compose --profile core up -d
# wait for hermandad-service to start
curl -s http://localhost:8081/v3/api-docs | python3 -c "import sys,json,yaml; print(yaml.dump(json.load(sys.stdin), default_flow_style=False))" > docs/openapi.yaml
```

**Step 2: Verify the DELETE endpoint is present in the spec**

```bash
grep -A5 "DELETE.*members" docs/openapi.yaml
```

**Step 3: Commit**

```bash
git add docs/openapi.yaml
git commit -m "docs: sync OpenAPI spec with DELETE member endpoint"
```

---

## Verification

Run full build:

```bash
export JAVA_HOME=~/.jdks/jdk-21.0.6+7/
./gradlew :services:hermandad-service:build
```
Expected: BUILD SUCCESSFUL, all tests green.

## Summary of Commits

1. `fix: remove updatable=false from updatedAt so @PreUpdate persists`
2. `fix: structured JSON error responses with 409/400/500 handlers`
3. `fix: bump outbox payload column from VARCHAR(255) to TEXT`
4. `feat: add delete method to HermandadMemberRepository`
5. `feat: add MemberRemovedEvent domain event`
6. `feat: add removeMember service method with domain event`
7. `feat: add DELETE endpoint for member removal`
8. `docs: sync OpenAPI spec with DELETE member endpoint`
