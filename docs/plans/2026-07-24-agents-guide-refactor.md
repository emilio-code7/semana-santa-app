# Agent Guide Refactor Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Reduce AGENTS.md from ~425 lines to ≤200 lines. Extract detailed mode workflows into `.agents/skills/` skill files. Preserve hard invariants, delegation rules, and source-of-truth pointers.

**Architecture:** Keep AGENTS.md as the always-loaded constitution and intent router. Load backend, frontend, and infrastructure procedures only when the task matches their mode.

**Tech Stack:** Markdown agent instructions and project-local skills.

---

**Date:** 2026-07-24

## Architecture

- **AGENTS.md** remains the always-on contract: identity, operating principle, task sizing, intent routing, non-negotiable rules, source-of-truth pointers, verification/commit rules, secret safety, code review graph.
- **`.agents/skills/backend-workflow/SKILL.md`** — backend-only Spring Boot workflow (OpenAPI-first, Gherkin, TDD, hexagonal/DDD, oracle escalation, test/compile verification)
- **`.agents/skills/frontend-workflow/SKILL.md`** — frontend-only workflow (server components, client interactivity, three states, designer delegation, npm build)
- **`.agents/skills/infrastructure-workflow/SKILL.md`** — AWS/CDK workflow (cdk diff, oracle for IAM/new services, cdk synth, no console edits, no secrets)
- **`docs/functional-map.md`** — update §1 topology pointer to reference `.agents/skills/`
- **`docs/plans/2026-07-24-agents-guide-refactor.md`** — this file

## Migration Steps

1. Write the three skill files under `.agents/skills/`
2. Rewrite AGENTS.md (target ≤200 lines) with intent-based routing replacing keyword-based mode detection
3. Update `docs/functional-map.md` line 104 pointer
4. Write this plan file
5. Verify: `wc -l AGENTS.md` (target ≤200), `git diff --check`, inspect git diff

## Files Changed

| File | Action |
|------|--------|
| `AGENTS.md` | Rewrite (425 → ~180 lines) |
| `.agents/skills/backend-workflow/SKILL.md` | Create |
| `.agents/skills/frontend-workflow/SKILL.md` | Create |
| `.agents/skills/infrastructure-workflow/SKILL.md` | Create |
| `docs/functional-map.md` | Edit line 104 pointer |
| `docs/plans/2026-07-24-agents-guide-refactor.md` | Create |

## Verification

- `wc -l AGENTS.md` — should be ≤200
- `git diff --check` — no whitespace errors
- Visual inspection of `git diff` — no unintended changes to application code
- All new skill files have valid YAML frontmatter

## Intentional Policy Changes

This refactor made the following deliberate policy changes:

- **Intent routing replaced keyword mode detection**: Routing decisions are now based on the nature of the change (intent), not on detecting mode keywords. This avoids brittle pattern matching and lets the orchestrator apply judgment.
- **Risk-triggered Oracle review**: Oracle is no longer a required step for every change. Review is triggered only by risk: architecture/boundary decisions, security changes, multi-service or data-integrity changes, hard bugs, or explicit request.
- **Single-fixer operational tasks**: Simple operational tasks (commit, status, single command, one-file edit) delegate directly to `@fixer` without loading the full workflow skill. This preserves orchestrator context and reduces latency.
- **`deploy-outputs.json` only after real deployment**: The file is no longer updated speculatively during implementation. It is updated and committed only after an actual deployment confirms live outputs changed.
