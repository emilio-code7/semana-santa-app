# Repertorio — Agent Guide

## Identity

Semana Santa management system: 3 Spring Boot microservices (hermandad, procesion, repertorio), event-driven via Kafka outbox, JWT auth via Keycloak, hexagonal + DDD.

## Operating Principle

**Orchestrator** reasons, decides, and delegates.
**Sub-agents** receive one specific task, start with a clean context, produce an artifact, and do nothing else — no sub-orchestration, no scope creep, no second-guessing.

This is not negotiable. A sub-agent that drifts into analysis, re-planning, or further delegation has violated its contract.

## Agent Capability Boundaries

| Agent | Capability |
|-------|-----------|
| `@explorer`, `@librarian`, `@oracle`, `@observer` | **Read-only advisory** — inspect, research, analyze, report only. Never edit/create/delete files, implement fixes, commit, or push. |
| `@council` | **Review synthesis only** — may coordinate councillors internally but must not modify project files. |
| `@fixer`, `@designer` | **Writers** — the only agents authorized to edit, create, or delete project files. |

If advisory work reveals an implementation need, return a handoff to `@fixer` or `@designer` instead of implementing.

## Task Sizing and Delegation Budget

- Preserve parallel execution for genuinely independent, non-overlapping workstreams and whenever it materially reduces elapsed time. Parallel lanes require disjoint file ownership; use one fixer per lane; never run parallel writers on shared files; the orchestrator reconciles artifacts.
- For simple operational tasks such as committing, checking status, formatting, running one command, or making a known one-file edit, delegate exactly one bounded task to `@fixer` so the orchestrator context is preserved.
- For those simple tasks, do not create a plan or call `@explorer`, `@oracle`, `@librarian`, `@designer`, or `@council`.
- The fixer executes the request directly, verifies proportionately, and must not delegate further.
- The exception above applies before the mode-specific workflows; complex work may still use parallel specialist lanes.

## Intent-Based Routing

Route by intent, not keywords. If multiple apply, choose primary by changed behavior/files.

| Intent | When | Workflow |
|--------|------|----------|
| **Operations** | Status/format/tests/git/bounded command | Direct `@fixer` per simple-task rule |
| **Backend** (default) | Services/API/persistence/domain/Kafka/outbox | `.agents/skills/backend-workflow/SKILL.md` |
| **Frontend** | Browser/UI/components/pages | `.agents/skills/frontend-workflow/SKILL.md` |
| **Infrastructure** | AWS/cloud/deployment/CI/CD/Docker | `.agents/skills/infrastructure-workflow/SKILL.md` |

## Specialists

Route work to the right specialist — each handles one concern and returns control:

| Specialist | When |
|-----------|------|
| `@explorer` | 4+ files, unfamiliar flows, or multi-service understanding |
| `@librarian` | External research, version-specific docs, dependency compatibility |
| `@fixer` | Bounded multi-file implementation (simple tasks per Task Sizing above) |
| `@designer` | Visual/UX work: layout, styling, responsive behavior, animation |
| `@oracle` | Strategic, security, or data-integrity decisions — risk-triggered per workflow skill |

## First Thing (all modes)

Read `docs/functional-map.md` before any implementation work — it's your complete reference: topology, profiles, endpoints, DB schemas, test inventory, operating principles. This file is the pointer. That file is the truth.

## Non-Negotiable Rules

1. **No JPA annotations in domain layer.** `@Entity`, `@Table`, `@Id` go in adapter JPA entities. Domain classes are pure Java.
2. **No REST calls between services.** Cross-service communication is async via Kafka (outbox pattern). No `RestTemplate`, `WebClient`, or Feign across service boundaries.
3. **All events go through the outbox.** Kafka direct produce is forbidden. `DomainEventPublisherAdapter` publishes to both `ApplicationEventPublisher` (in-process) and outbox table → poller → Kafka.
4. **Every schema change is a new Flyway migration.** Never edit existing migrations. Increment the version number.
5. **No `permitAll()` on write endpoints.** `anyRequest().authenticated()` is the base rule. Public endpoints are explicitly listed and must be GET-only.
6. **Every non-trivial behavior change needs a test that would fail without it.** No test = incomplete.
7. **OpenAPI first for API/controller changes.** Update `docs/openapi.yaml` before writing code. The spec is the contract. Pre-commit hook enforces this.
8. **No secrets or credentials in commits.** Never hardcode AWS keys, IPs, or passwords. Use environment variables.

## Source-of-Truth Pointers

| What | Where |
|------|-------|
| App topology, profiles, endpoints, DB schemas, test inventory | `docs/functional-map.md` |
| API contract (endpoints, request/response schemas) | `docs/openapi.yaml` |
| Hexagonal + DDD architecture decisions | `docs/architecture.md` |
| Domain glossary, ubiquitous language, aggregate roots | `docs/agents/domain.md` |
| AWS migration rationale, architecture, deploy instructions | `docs/aws-guide.md` |
| Live AWS resource IDs (CDK outputs) | `infrastructure/aws/deploy-outputs.json` |
| Sprint plans with acceptance criteria | `docs/plans/` |

## Verification & Commit

Before declaring any task complete:
- Check `git status` + `git diff` — only stage intended files
- Run affected tests/build — refer to the workflow skill for mode-specific gates
- No unfinished-work markers in new/modified files
- Conventional commit: `type(scope): description` (`feat:`, `fix:`, `refactor:`, `test:`, `docs:`, `chore:`)
- One concern per commit

### Completion Gate (applies before any "done" or "ready" claim)

**Never claim an issue or PR is complete until all of these pass:**

1. **No merge conflicts.** Merge `origin/main` into the branch and verify clean auto-merge before reporting success. If conflicts exist, resolve them and re-verify. Do not report "done" with unresolved conflicts.

2. **All CI checks green.** After pushing, poll `gh pr checks` until all jobs complete with `pass`. If any job fails:
   - Read the failure log
   - Fix the root cause in the branch
   - Push the fix
   - Poll again until all green
   Do not move to the next ticket while a PR has failing checks.

3. **OpenAPI validation passes.** `npx @redocly/cli lint docs/openapi.yaml` must exit 0 with no errors (warnings from pre-existing code are acceptable but must not be new). Duplicate operationIds, unresolved $refs, and duplicate YAML keys are hard errors.

4. **Flyway migration numbering is contiguous.** Check that migrations in each service follow the same version sequence as `origin/main`. If a migration number already exists on main, the branch's migration must be renumbered to the next available version. Never ship two migrations with the same version number. Check this **before** opening the PR, not after.

This gate is non-negotiable. A PR with conflicts or failing checks is not complete, and claiming otherwise wastes review cycles.

### Documentation Protocol (applies before any "done" or "ready" claim)

Every behavior change must keep the narrative docs truthful — a merged feature that the README or functional map still describes as "not existing" is an incomplete change. Before closing out work, verify the docs that describe the changed behavior are updated:

| Change affects | Document to update |
|----------------|-------------------|
| API endpoints, request/response shapes | `docs/openapi.yaml` **(first!)** |
| Endpoints, topology, DB schemas, test inventory, AS-IS/TARGET status | `docs/functional-map.md` |
| Services, architecture, cross-service flow | `docs/architecture.md` |
| Domain terms, aggregates, ubiquitous language | `docs/agents/domain.md` |
| Capabilities, architecture diagram, run instructions | `README.md` |
| AWS resources, deployment, migration rationale | `docs/aws-guide.md` |
| Completed work vs roadmap | active roadmap doc in `docs/plans/` |

- The **orchestrator/integration owner** owns documentation reconciliation — not the feature fixer. After merging implementation lanes, check `README.md` and `docs/functional-map.md` against the merged behavior and update them (directly, or via one bounded `docs:` fixer lane).
- If a fixer lane's scope includes docs, the orchestrator still verifies the narrative docs at merge time; a fixer that finishes implementation and silently skips doc updates has not completed the issue.
- `docs/` and `README.md` are central files: parallel implementation lanes do not edit them; the integration lane owns them.

### Merging and Closing

**The implementation agent must not merge or deploy PRs without explicit authorization from the user.** Opening a PR and verifying CI is the agent's job. Merging, deploying, and closing issues are the user's decision. Use `Closes #XX` in the PR body so issues auto-close on merge, but never merge the PR yourself unless explicitly told to.

## Secret Safety

MUST load the `aws-secrets-manager` skill first for any secret/credential/API key/token/password task. MUST NOT call `secretsmanager get-secret-value` or `batch-get-secret-value`, and MUST NOT hit the Secrets Manager Agent daemon directly. MUST use `{{resolve:secretsmanager:secret-id:SecretString:json-key}}` with `asm-exec` so the secret resolves at runtime without entering context.

## Code Review Graph

**IMPORTANT: Use code-review-graph MCP tools BEFORE file scanning for exploration, impact analysis, and review.** The graph is faster, cheaper (fewer tokens), and gives structural context that file scanning cannot.

| Tool | Use |
|------|-----|
| `semantic_search_nodes_tool` / `query_graph_tool` | Exploring code instead of Grep |
| `get_impact_radius_tool` | Understanding blast radius instead of manual import tracing |
| `detect_changes_tool` + `get_review_context_tool` | Code review instead of reading entire files |
| `query_graph_tool` | Finding callers, callees, imports, tests, dependencies |
| `get_architecture_overview_tool` + `list_communities_tool` | Architecture understanding |

Fall back to Grep/Glob/Read only when the graph doesn't cover what you need.

## Agent Skills & Tools

- **Issue tracker**: GitHub `github.com/emilio-code7/semana-santa-app`. See `docs/agents/issue-tracker.md`.
- **Triage labels**: needs-triage, needs-info, ready-for-agent, ready-for-human, wontfix. See `docs/agents/triage-labels.md`.
- **Git workflow**: Load `using-repository-git-workflow` skill for branch naming, PRs, merges, sprint organization, hotfixes, release tags.
- **Memory**: Use `memory` tool for architectural decisions, patterns, gotchas. Scope: `project`.

## GitHub Issue Delivery Workflow

GitHub Issues at `github.com/emilio-code7/semana-santa-app` are the canonical executable work queue. Plans and documentation provide context, but an issue is the implementation unit.

### Coordination level is set by task size

Match orchestration to task complexity — small tasks do not need multi-lane plans:

- **Small task** (one file, <20 lines, no API/doc/schema change): delegate exactly one bounded task to `@fixer` per the Task Sizing rule. No plan, no doc lane, no extra agents.
- **Medium task** (one service, one concern, API or schema change): one fixer lane + the orchestrator verifies OpenAPI/docs per the Documentation Protocol at merge time. Optionally run `@oracle` review for security/data-integrity risk.
- **Large task** (multi-service, cross-cutting, or a roadmap ticket): build a short work graph before dispatching — implementation lanes (one per disjoint file group), a docs/verification lane owned by the orchestrator, and the Completion Gate. Delegate each lane to a fresh bounded specialist; reconcile artifacts yourself.

For large tasks the orchestrator must **plan lanes before dispatching** (per the workflow rule): identify which parts are independent, dispatch them in parallel, then reconcile the terminal results and own the documentation reconciliation at merge time. Do not hand the whole large task to a single fixer and let it run end-to-end; the orchestrator stays the integration owner.

### Discovery and creation

- Before any implementation, search open and closed issues for the concern. Never duplicate. If no issue exists, create one before coding.
- An independently dispatchable issue must contain: **Source**, **Current state**, **What to build** (or gate evidence), **Acceptance criteria/pass rule**, **Constraints**, **Verification/TDD evidence**, **Blocked by**, and **Agent handoff**. Add the appropriate milestone when the work belongs to a roadmap or sprint.
- See `docs/agents/issue-tracker.md` for issue metadata conventions and `docs/agents/triage-labels.md` for label semantics.

### Selecting and executing work

- Only implement frontier issues whose blockers are all **closed** and which carry the `ready-for-agent` label. Gates carry `ready-for-human`. Do not bypass blockers or self-promote blocked issues.
- **Dispatch checklist (mandatory before labeling `ready-for-agent` or starting work):**
  1. Read the issue body's **Blocked by** section: `gh issue view <n> --json body,state`
  2. Verify every blocker is **closed**: `gh issue view <blocker> --json state` — a blocker with an open PR is NOT closed. Do not label or start the issue while any blocker is open.
  3. Check for overlapping open PRs: `gh pr list --state open --json headRefName,files` — if any open PR touches the same files, do not dispatch until it merges (or stack per the rule below).
  4. Check Flyway versions on `origin/main` (see Completion Gate #4) so the migration number is correct from the first commit.
- Before coding, read the issue, its comments, `AGENTS.md`, `docs/functional-map.md`, and any linked plan or roadmap document.
- Follow OpenAPI-first (update the spec before writing controller code) and TDD (failing test first, then implementation, then refactor).
- Stay strictly inside the issue scope. When new or related work is discovered, create a follow-up issue instead of expanding the current branch or PR.

**Stacking rule:** if issue B is blocked by A and A's PR is still open, do **not** start B by default. Wait for A to merge. The only exception is explicit user authorization to stack: then branch B is created **off A's branch** and B's PR base is A's branch (stacked PR). Never build a blocked issue on `main` while its blocker is in review.

### Branching, PRs, and closing

- One issue or concern per short-lived branch from an updated `main`. Branch names include the **GitHub issue number** (not the plan ticket number) as documented in the repository Git workflow: `<type>/<issue>-<kebab-summary>`. Plan docs keep ticket numbers only for the dependency graph — never use them in branch or worktree names.
- Complete work through a PR. The PR body must include `Closes #<issue-number>` and the verification evidence.
- **Do not open a PR while CI on the head commit is failing.** If checks fail after opening, fix before requesting review or claiming completion. A claim of "tests pass" requires `gh pr checks` output showing pass.
- CI must pass. The implementation agent must not merge or deploy without explicit authorization.
- Keep the issue open during review. A squash merge closes it automatically.
- **After merge: delete the branch and worktree** — `git push origin --delete <branch>` and `git worktree remove <path>`. Never leave merged branches or `check-*` review branches behind.
- After merge, the integration owner verifies completion and promotes newly unblocked implementation issues to `ready-for-agent`. Gates remain `ready-for-human`.

## AWS Guidance

- Prefer the AWS MCP Server for AWS interactions (sandboxed execution, observability, audit logging). Fall back to AWS CLI.
- Check for relevant AWS skills before starting; prefer skill guidance over general knowledge.
- Prefer infrastructure-as-code (CDK/CloudFormation) over direct CLI commands.
- Follow AWS Well-Architected Framework principles.
- Do not use em dashes in AWS resource names or descriptions — use hyphens.
