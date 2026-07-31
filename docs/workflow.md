# Development Workflow

## Core Principle

**Agent-driven delivery, human-owned merge.** The orchestrator plans, delegates, and reconciles. Specialists execute bounded lanes: writers (`@fixer`, `@designer`) implement, advisors (`@explorer`, `@librarian`, `@oracle`, `@observer`) research and review. The user reviews and merges PRs — no agent merges without explicit authorization. See `AGENTS.md` for the full capability boundaries and routing rules.

**TDD is mandatory:** RED (failing test) → GREEN (minimal code) → REFACTOR. Test before code, always. Every non-trivial behavior change ships with a test that would fail without it — enforced by the pre-commit hook and CI (a main-source change without a test change in the same commit is rejected).

---

## Per-Task Loop

```
Spec → RED (failing test) → GREEN (minimal code) → REFACTOR → Verify → Document → Sync API → Commit
```

Each step must complete before the next. No skipping.

---

## Step-by-Step

### 0. Spec

The issue/plan defines the acceptance criteria before any code. If the request is vague, ask a targeted question before proceeding. Never guess at critical details (paths, APIs, architectural decisions).

### 1. Plan

The orchestrator routes work by intent (`AGENTS.md`): backend → backend workflow, frontend → frontend workflow, infrastructure → infrastructure workflow. Simple tasks go directly to one `@fixer`. Complex work gets a short work graph with independent lanes dispatched in parallel and disjoint file ownership.

### Plan Format

Plans (`docs/plans/`) contain only:
- Use cases / BDD scenarios (Given-When-Then)
- Acceptance criteria
- Technical decisions & trade-offs
- Architectural notes

**No implementation code in plans.** Code belongs in files, not planning documents. The plan captures *what* and *why*, not *how*.

### 2. RED — Write the Failing Test

- Write the test that fails without the behavior, at the narrowest seam (domain unit → service → controller slice → integration)
- **One behavior per test.** Test the observable behavior, not implementation
- Commit the failing test alone (`test:` commit) when the change is large enough to warrant it; otherwise keep RED and GREEN in the same commit with the test clearly first in the diff
- The pre-commit hook and CI enforce pairing: a change to `src/main` without a change to `src/test` in the same commit fails the gate

### 3. GREEN — Minimal Implementation

Implement the minimum code that makes the failing test pass. No speculative abstractions, no boilerplate "for later". Simple tasks use one bounded `@fixer`; multi-file changes use bounded fixer delegation per AGENTS.md task sizing.

### 4. REFACTOR

After GREEN, clean up without changing behavior:
- **Duplication** — extract if the same logic appears 3+ times
- **Naming** — does it reflect intent or just describe what it does?
- **Structure** — does it belong where it's placed?
- **Ponytail check** — is this the minimum code that works? Any speculative abstraction?

Re-run the tests after refactoring. If nothing to refactor, note "No refactor needed" and move on.

### 5. Build & Deploy

```bash
export JAVA_HOME="$HOME/.jdks/jdk-21.0.6+7"
./gradlew :services:hermandad-service:build -x test
docker compose --profile core build hermandad-service
docker compose --profile core up -d hermandad-service
```

Verify the container starts healthy:

```bash
docker logs hermandad-service 2>&1 | grep -E "Started|APPLICATION FAILED"
```

### 6. Verify

Run a quick smoke test against the running service:

- Hit the affected endpoints (200/201 expected)
- Check error cases if applicable (400/404 expected)
- Confirm Kafka events if the flow publishes them

Only proceed if the behavior matches expectations.

### 7. Update Documentation

- `docs/openapi.yaml` — update FIRST for any API/controller change (spec is the contract; hook enforces)
- `docs/functional-map.md` — endpoints, topology, DB schemas, test inventory, AS-IS/TARGET status
- `docs/architecture.md` — update patterns, decisions, or configurations that changed
- `docs/workflow.md` — update if the process itself changes
- Any other relevant `docs/` file

### 8. Sync OpenAPI Spec

If the API changed (new/edited endpoints, request/response schemas, status codes):

```bash
curl -s http://localhost:8081/v3/api-docs | python3 -c "
import sys, json
spec = json.load(sys.stdin)
spec['servers'] = [{'url': 'http://localhost:8080', 'description': 'API Gateway (development)'}]
spec['info']['description'] = 'REST API for Semana Santa Hermandad management'
print(json.dumps(spec, indent=2))
" > docs/openapi.yaml
```

### 9. Commit

Before staging, review what changed:

```bash
git status
git diff --stat
```

Stage only intended files (no build artifacts, IDE files, secrets):

```bash
git add <file1> <file2> ...
git commit -m "<type>: <description>"
```

Conventional commit types: `feat:`, `fix:`, `docs:`, `chore:`, `refactor:`, `test:`.

If it's PR-worthy work, create a pull request instead of committing directly:

```bash
gh pr create --title "<type>: <description>" --body "## Summary\n\nWhat changed and why."
```

---

## Reminders

- **No scope creep.** If you spot a problem unrelated to the task, mention it but don't fix it without asking.
- **Build requires JDK 21** — set `JAVA_HOME` before every `./gradlew`.
- **Docker images are stale after JAR changes** — rebuild always.
- **Verify before claiming done.** Evidence over assertions — CI must be green and `gh pr checks` must show pass before claiming completion.
- **Agents never merge or deploy.** Opening PRs and verifying CI is the agent's job; merging is the user's decision.
