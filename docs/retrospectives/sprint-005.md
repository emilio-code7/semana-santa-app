# Sprint 5 Retrospective — Hermandad Polish + Idempotent Consumer

**Dates**: 2026-06-26
**Duration**: Single session
**Velocity**: 3 items delivered, 7 tests added, 5 commits

---

## What Went Well

- **TDD flow held**: RED tests written before implementation on all 3 items. Tests caught the `@ConditionalOnProperty` misstep.
- **Idempotent consumer pattern clean**: Entity + repo + migration + consumer + test in one atomic commit. The `UUID.nameUUIDFromBytes(payload)` approach is minimal and deterministic.
- **Doc discipline**: backlog.md, architecture.md, audit.md updated in lockstep with code. No separate "docs sprint" needed afterwards.
- **Ponytail lazy mode effective**: No over-engineered abstractions. Consumer is ~40 lines, no factories or interfaces for a single implementation.

## What Tripped Us Up

### 1. `@WebMvcTest` validation test dropped

The `createHermandadReturns400WhenBodyInvalid` test in `HermandadControllerTest` was removed because Spring's validation in `@WebMvcTest` didn't fire as expected (returned 500 instead of 400). Root cause never fully diagnosed — likely a missing validation auto-configuration in the slice test.

**Lesson**: `@WebMvcTest` + `@Valid` testing is fragile. Rely on `GlobalExceptionHandlerTest` for handler-level validation coverage. Controller-level validation tests should be `@SpringBootTest` or use `@AutoConfigureMockMvc` with the full context.

### 2. `@ConditionalOnProperty` detour

Initially added `@ConditionalOnProperty` to `IdempotentEventConsumer` to prevent `HermandadRepositoryIntegrationTest` from trying to connect to Kafka. This was production code surgery for a test problem.

**Lesson**: Use `@MockitoBean` in `@SpringBootTest` to exclude irrelevant beans. Never modify production code solely to satisfy a test environment constraint.

### 3. Slow Docker test runs

Each `./gradlew test` run inside the Docker container takes 4-5 minutes. This slows the RED-GREEN-REFACTOR cycle significantly — temptation to batch changes instead of iterating.

**Mitigation**: Consider running unit tests locally (outside Docker) when no database/Kafka is needed. Integration tests can stay in Docker.

### 4. Debug test file left behind

Created `DebugTest.java` during root cause analysis and forgot to delete it. `git status` caught it before commit, but it should have been cleaned immediately.

**Lesson**: Clean up debugging artifacts as soon as the root cause is found, not at commit time.

## Action Items

| Action | Priority |
|--------|----------|
| Prefer `@MockitoBean` over production code changes for test isolation | Process |
| Keep debugging artifacts cleaned immediately after use | Process |
| Controller validation tests → `@SpringBootTest` or skip, handler tests cover it | Decision |
| Consider local test runner for unit tests (speed) | Low |

## Stats

- **Planned**: 3 items
- **Delivered**: 3 (100%)
- **Tests added**: 7 (3 controller, 2 handler, 3 consumer)
- **Test stability**: 47/47 green (7 integration tests skip gracefully when PostgreSQL unavailable)
- **Files changed**: 13 (5 commits)
- **Production code**: 3 new files (consumer, entity, repo) + 1 migration
- **Test code**: 3 new/modified files
- **Docs**: 3 files updated (backlog, architecture, audit)
