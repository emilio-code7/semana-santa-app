# Sprint 2 — Audit Fixes & Member Removal

**Goal:** Clean technical debt and complete member CRUD with removal.

**Scope:** hermandad-service only. No cross-service changes.

---

## Item 1 — Audit Fixes

Six small fixes from `docs/audit.md` that fix broken behavior or add missing guards.

### Use Cases / Acceptance Criteria

**1.1 `updatedAt` persists after entity changes**
- `HermandadMember.updatedAt` must change when the entity is updated via `changeRole()`
- Proved by `@DataJpaTest` that saves a member, changes role, and asserts `updatedAt` differs after reading from the database

**1.2 Duplicate member → 409 Conflict**
- `POST /api/hermandades/{id}/members` with existing user → 409 with descriptive error
- Handled by `DataIntegrityViolationException` → `@ExceptionHandler` in `GlobalExceptionHandler`

**1.3 Validation error → 400 with field-level messages**
- `POST /api/hermandades/` or `/members` with invalid body → 400 with field error details
- Handled by `MethodArgumentNotValidException` → `@ExceptionHandler`

**1.4 Unexpected error → 500 without leaking stack traces**
- Any unhandled exception → 500 with generic message (no stack trace in response)
- Handled by `Exception` fallback → `@ExceptionHandler`

**1.5 Outbox payload column can hold large events**
- `outbox_event.payload` changed from `VARCHAR(255)` to `TEXT`
- Flyway V3 migration

**1.6 Existing handlers keep working**
- HermandadNotFound → 404, IllegalArgumentException → 400 still return the exception message as plain text

### Technical Notes & Decisions

- Error responses stay as `ResponseEntity<String>` — HTTP status + message string is sufficient for this project. No structured JSON (`{code, message}`) needed.
- Tests: `@DataJpaTest` for the entity test, unit test for `GlobalExceptionHandler` (plain JUnit, no Spring context needed). H2 added as test dependency for `@DataJpaTest`.

---

## Item 2 — Member Removal

Complete member CRUD by adding the deletion flow.

### Use Cases / Acceptance Criteria

**2.1 Remove a member from a hermandad**
- `DELETE /api/hermandades/{hermandadId}/members/{userId}` → 204 No Content
- Member is deleted from `hermandad_member` table
- `MemberRemovedEvent` published via `DomainEventPublisher` → outbox → Kafka topic `hermandad-member-events`
- No Keycloak sync on removal (user might belong to other hermandads)

**2.2 Remove non-existent member → 404**
- `DELETE` with a `userId` that doesn't belong to the hermandad → 404
- Reuses existing `HermandadMemberNotFoundException`

**2.3 Service method follows existing patterns**
- `HermandadService.removeMember(hermandadId, userId)` finds member, deletes, publishes event
- Same pattern as `addMember()` and `changeRole()`

### Technical Notes & Decisions

- Keycloak roles are NOT cleaned up on removal — roles are per-user across hermandads. If needed later, add a `MemberRemovedListener` following the same async pattern as `MemberAddedListener`.
- Tests: unit test for service (Mockito), MockMvc test for the endpoint.
- OpenAPI spec synced from live `/v3/api-docs` after implementation.
