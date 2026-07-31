# ⚠️ STARTUP CHECKLIST — READ BEFORE ANY ACTION

Before responding to any message, I MUST:
1. [ ] Read `docs/workflow.md` to capture the current per-task loop step
2. [ ] Read `docs/backlog.md` to understand current sprint context
3. [ ] Have I already read these files this session? If yes, I still re-read them unless the task is a single obvious action within the same loop step.

Failure to read these = repeating mistakes.

---

# Project Instructions

## Division of labor

- **Agent-driven delivery, human-owned merge.** The orchestrator plans, delegates, and reconciles. Specialists execute bounded lanes (`@fixer`, `@designer` write; `@explorer`, `@librarian`, `@oracle`, `@observer` advise). The user reviews and merges PRs — no agent merges without explicit authorization.
- See `AGENTS.md` for capability boundaries and routing rules.

## Our flow

We follow **TDD** (RED → GREEN → refactor), **DDD** (domain-driven design with hexagonal architecture), **BDD** (Given-When-Then scenarios), and **agile** (sprint-based with backlog).

Per-task loop: **Spec → RED (failing test) → GREEN (minimal code) → REFACTOR → Verify → Document → Sync API → Commit**

See `docs/workflow.md` for the full process.

## Documents

| File | Purpose |
|------|---------|
| `docs/backlog.md` | Sprint backlog, current sprint, done items |
| `docs/architecture.md` | System architecture, patterns, decisions |
| `docs/workflow.md` | Development workflow and per-task loop |
| `docs/audit.md` | Codebase audit findings and technical debt |
| `docs/plans/` | Implementation plans for each sprint/feature |
| `docs/functional-map.md` | Source of truth: topology, endpoints, DB schemas, test inventory |
| `.opencode/instructions.md` | This file — startup checklist, rules |

## Git discipline

- Commit after every completed task. Not at end of day — after each logical unit of work.
- The workflow doc's commit step has the exact flow (`git status` → `git diff --stat` → `git add` → `git commit`). Follow it.
