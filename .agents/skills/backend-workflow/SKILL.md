---
name: backend-workflow
description: Backend-only workflow for Spring Boot services, API, persistence, domain, Kafka, and outbox changes.
---

# Backend Workflow

Use this skill when implementing backend changes — services, API, persistence, domain, Kafka, outbox.

## Reference Docs

- `docs/functional-map.md` — topology, profiles, endpoints, DB schemas, test inventory, operating principles
- `docs/openapi.yaml` — API contract (update FIRST for any API/controller change)
- `docs/architecture.md` — Hexagonal + DDD design decisions
- `docs/plans/` — Sprint plans with acceptance criteria

## Workflow

1. **Explore** — Use graph MCP tools first (`semantic_search_nodes_tool`, `query_graph_tool`, `get_architecture_overview_tool`). For external/version-specific research, delegate to `@librarian`. Fall back to `@explorer` for 4+ files or complex flows, or to targeted Grep/Glob/Read when graph coverage is insufficient.
2. **OpenAPI first** — For API/controller changes, update `docs/openapi.yaml` with new endpoints, request/response schemas, and operationIds before writing code. The spec is the contract. Pre-commit hook enforces this.
3. **Gherkin** — For non-trivial behavior changes, write `Feature:`/`Scenario:`/`Given/When/Then` blocks covering happy path, error/edge cases, and auth/permission boundaries. Present for approval before implementing.
4. **Design review** — Consult `@oracle` only for: architecture trade-offs, hex layer placement, security decisions, hard bugs, or required non-trivial diff review. Oracle is an escalation, not a default.
5. **Implement TDD (RED → GREEN → REFACTOR)** — This is not optional.
   - **RED**: Write the failing test FIRST, before any implementation code.
   - **GREEN**: Simple tasks follow AGENTS.md and use exactly one fixer; multi-file changes use bounded fixer delegation.
   - **REFACTOR**: Clean up without changing behavior.
   - One behavior change per commit. Small commits.
6. **Code review** — Oracle review is risk-triggered: architecture/boundary decisions, security changes, multi-service or data-integrity changes, hard bugs, or when non-trivial review is explicitly required. Routine localized changes rely on tests/build. When review is needed, include diff hash, files reviewed, issues categorized, and verdict. Fix issues and re-verify until clean.
7. **Validate against spec** — Check OpenAPI spec matches implementation (response codes, field types, endpoint paths).
8. **Update docs** — Per functional-map §0.9. Regenerate Bruno scripts if API changed.
9. **Verify gate** — `./gradlew :services:<affected-service>:test` all pass, `./gradlew :services:<affected-service>:compileJava` succeeds, no FIXME/TODO/HACK markers, pre-commit hook passes.
10. **Store decisions** — Use `memory` tool for architectural decisions, patterns discovered, gotchas. Scope: `project`.
11. **Commit** — Conventional commit (`feat:`, `fix:`, `refactor:`, `test:`, `docs:`). Check `git diff` before committing.

## Simple Tasks

Simple operational tasks (commit, status check, single command, single known-file edit) are governed by AGENTS.md — delegate directly to `@fixer` without this workflow.
