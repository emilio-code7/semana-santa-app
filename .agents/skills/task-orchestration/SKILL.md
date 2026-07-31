---
name: task-orchestration
description: Two-tier agent orchestration — Lead (conversation owner) spawns Task Orchestrators per task via a Task Brief and receives Terminal Results. Use when dispatching a task to a task-orchestrator agent, writing a task brief, receiving a terminal result, or answering the lead's question about how to run a task.
---

# Task Orchestration

Two-tier orchestration with a hard context boundary: the lead (the agent the user talks to) stays clean; each task runs in its own task-orchestrator session.

```
User ⇄ LEAD (conversation owner — permanent session)
          │  spawns with a TASK BRIEF, gets back a TERMINAL RESULT
          ▼
      TASK ORCHESTRATOR (per task — fresh context, disposable)
          │  plans lanes, spawns in parallel
          ▼
      Specialists (fixer · explorer · oracle · librarian · designer)
```

## Tier rules

- **Lead** holds only: user intent, priorities, issue queue (IDs + status), briefs sent, results received. Never task internals. Compresses aggressively; references work by path, not content.
- **Task Orchestrator**: spawned per task from a Task Brief. Owns exploration → lane planning → parallel dispatch → reconciliation → verification → report. May spawn specialists and run commands. Does not own the conversation.
- **Specialists (leaf)**: single-shot, bounded, no further delegation.
- **Recursion cap**: task orchestrators spawn leaf specialists only. A task that forks into sub-tasks becomes a new brief.

## When to use the tier

- **Small task** (<20 lines, one file, no API/schema/doc change): skip tier 2 — direct `@fixer` from the lead.
- **Medium/large task**: spawn a task orchestrator with a Task Brief.

## The two contracts

### Task Brief (in) — self-contained; the ONLY context the task orchestrator gets

```markdown
## Task
<one-line summary>

## Source
<issue/plan reference — link or path>

## Current state
<what exists today — 2-4 lines, pointer to relevant code paths, not full content>

## What to build
<the deliverable, in observable terms>

## Acceptance criteria / pass rule
- [ ] <measurable outcome>
- [ ] <measurable outcome>

## Constraints
<repo rules that apply: TDD pairing, OpenAPI-first, Flyway, no JPA in domain,
no REST cross-service, outbox-only events, tenant isolation…>

## Verification / TDD evidence
<exact commands that prove it: gradle test, redocly lint, gh pr checks…>

## Blocked by
<closed blockers only>

## Agent handoff
<pointers: functional-map §, openapi §, relevant skill, relevant agent session if resuming>
```

No brief = no spawn. Pointer-based, not content dumps — the task orchestrator reads the files.

### Terminal Result (out) — the ONLY thing that returns to the lead

```markdown
## Summary
<2-3 lines>

## What changed
- path — one line each

## Verification
- command → result (tests, lint, checks)

## Docs reconciled
- openapi/functional-map/README — what was updated

## PR / CI state
- branch, PR link, checks status

## Decisions made (need user awareness)
- <assumptions or calls the user may veto>

## Follow-ups discovered (out of scope)
- <new issues to file>
```

## Interactivity

- **Blocking decision**: task orchestrator stops, returns a `DECISION REQUIRED` result with bounded options (recommended first) → lead relays → user picks → lead resumes the SAME task-orchestrator session (`task_id`) with the answer.
- **Non-blocking**: task orchestrator assumes, flags in the terminal result, user vetoes on review.
- **Question ladder** before interrupting: grep codebase → repo docs → lead's cached knowledge → ask user. One batched question per interruption.

## Session reuse

- Lead session: permanent.
- Task-orchestrator session: reused only for follow-ups on the SAME task (PR fixes). Never across tasks.
- Specialists: never reused across tasks.
