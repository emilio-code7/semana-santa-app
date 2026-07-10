# Sprint 6 Retrospective — Hermandad Service Polish

**Dates**: 2026-07-10
**Duration**: Single session
**Velocity**: 3 items delivered + 1 bonus, 7 tests added, 4 commits

---

## What Went Well

- **Decomposition > direct implementation**: Breaking the sprint into 3 independent tasks allowed parallel dispatch. Task 2 (CAPATAZ in OpenAPI) was a 30-second verification, not a feature — caught early.
- **Code review caught a real issue**: In `KeycloakUserExistenceAdapter`, the reviewer flagged `searchByUsername()` (wrong API — returns a search result set, not a single user) and corrected it to `users().get(userId).toRepresentation()`. This would have been a production bug if unchecked.
- **Spec review before merging**: Cross-checking each commit against the spec caught deviations — notably `@Cacheable` left on the paginated query method (would cache the first page forever) and `Pageable` parameter issues. Caught before PR.
- **Integration test delivered end-to-end confidence**: Testcontainers-based `HermandadControllerIntegrationTest` proved pagination works through the full stack (controller → service → adapter → PostgreSQL). Skippable CI-friendly test with `assumeTrue`.
- **Backlog discipline sustained**: Backlog updated immediately after the last commit, Sprint 6 section created, completed items pruned from unordered backlog.

## What Tripped Us Up

### 1. Async flow issues between callback/response handlers

The main ArchGuard spec review ran into issues with `run_in_background` semantics and callback response formatting. The `await_agents` callback was malformed (JSON wrapped in markdown) — ClineJS could not parse it. Asynchronous exploration + spec review + code review flow is powerful but demands strict adherence to callback format.

**Lesson**: Spec review is read-only enough to run in background matching explore agents. When chaining exploration → review → fix, prefer `task_id` continuation over separate background tasks to avoid callback formatting issues.

### 2. `searchByUsername` vs `users().get()` in Keycloak adapter

Initial implementation used `realm.users().searchByUsername(userId).getFirst()` which is incorrect for two reasons: `userId` is a UUID (Keycloak ID), not a username; and `searchByUsername` is a search mechanism, not an identity lookup.

**Lesson**: When integrating with external APIs, use identity lookups (`.get(id)`) over search lookups (`.searchByUsername()`). The existing `KeycloakMembershipAdapter` had the correct pattern — should have been used as reference from the start.

### 3. Parallel agent scheduling overhead

Dispatching spec review + code quality review as separate background agents for every task creates context management overhead. For a 3-task sprint of moderate complexity (no cross-cutting concerns), the review-per-task pattern generated 6 parallel reviews, most of which passed cleanly.

**Mitigation**: For tasks where spec is clear and tests pass, skip or batch reviews. Reserve full spec-review for tasks with architectural decisions or ambiguous requirements.

### 4. Test container integration test not run by default

The integration test requires a local PostgreSQL instance. When writing it, the first attempt failed integration tests because they were assumed to be included in `./gradlew test`. The previous pattern (`HermandadRepositoryIntegrationTest`) already uses `assumeTrue` with a reachability check, so tests skip gracefully — but the author didn't know about it until caught in review.

**Lesson**: When adding integration tests, check existing integration test patterns in the project first. The `assumeTrue` + `@DynamicPropertySource` pattern is standard here.

### 5. Docker test slowness still unmitigated (carry-over)

Sprint 5 identified that each `./gradlew test` run takes 4-5 minutes inside Docker. No progress on running unit tests locally. Still tempts batching changes instead of tight RED-GREEN-REFACTOR cycles.

**Lesson**: Still pending. Worth picking up if iteration speed becomes a bottleneck.

## Action Items

| Action | Priority |
|--------|----------|
| Prefer `task_id` continuation over separate background tasks for review chains | Process |
| Use existing adapter patterns as reference when integrating with same external API | Process |
| Skip or batch spec/code reviews for straightforward tasks; reserve for architecture decisions | Process |
| Check existing integration test patterns when adding new tests | Process |

## Stats

- **Planned**: 3 items + 1 bonus
- **Delivered**: 4 (100%)
- **Tests added**: 7 (3 integration, 2 adapter, 2 service)
- **Test stability**: 55/55 green (was 47 in Sprint 5; integration tests skip gracefully when PostgreSQL unavailable)
- **Files changed**: 11 (4 commits)
- **Production code**: 3 new files + modifications to existing (service, controller, adapter, etc.)
- **Test code**: 2 new files + 1 modified
- **Code review findings**: 1 production bug caught (searchByUsername → users().get()), 1 spec deviation (pagination, fixed before merge)
- **Docs**: 1 file updated (backlog)
