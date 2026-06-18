# Development Workflow

## Core Principle

**The user implements, AI only guides/scaffolds.** The AI designs, plans, writes specs, documents, and reminds. The user writes the code. Never implement without explicit confirmation.

**TDD is mandatory:** RED (failing test) → GREEN (minimal code) → refactor. Test before code, always.

---

## Per-Task Loop

```
Ask → Guide → User implements → Build & Deploy → Verify → Document → Sync API → Commit
```

Each step must complete before the next. No skipping.

---

## Step-by-Step

### 0. Ask

Before any work, ask the user for confirmation. Do not proceed without a response. The goal is to remind the user what needs to be done, not to do it for them.

### 1. Guide

- AI explains what to build, referencing the plan and backlog
- AI provides the **test first** (RED) — the user writes the implementation (GREEN)
- One atomic change per task
- No scope creep — if something else needs fixing, note it and ask

### Plan Format

Plans (`docs/plans/`) contain only:
- Use cases / BDD scenarios (Given-When-Then)
- Acceptance criteria
- Technical decisions & trade-offs
- Architectural notes

**No implementation code in plans.** Code belongs in files, not planning documents. The plan captures *what* and *why*, not *how*.

### 2. Code Review

After the user implements (GREEN), AI reviews the diff before proceeding:
- **Correctness** — does it meet the acceptance criteria?
- **Pattern fit** — does it follow existing codebase conventions?
- **Trade-offs** — propose alternative approaches with pros/cons if applicable
- **No nitpicking** — only raise meaningful issues (correctness, maintainability, security)

If issues found → flag them, let the user decide. Do not block on style preferences.

### 3. Build & Deploy

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

### 3. Verify

Run a quick smoke test against the running service:

- Hit the affected endpoints (200/201 expected)
- Check error cases if applicable (400/404 expected)
- Confirm Kafka events if the flow publishes them

Only proceed if the behavior matches expectations.

### 4. Update Documentation

- `docs/backlog.md` — mark stories done, add new items if discovered
- `docs/architecture.md` — update patterns, decisions, or configurations that changed
- `docs/workflow.md` — update if the process itself changes
- Any other relevant `docs/` file

### 5. Sync OpenAPI Spec

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

### 6. Commit

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

- **Ask before starting.** Never proceed without confirmation.
- **No scope creep.** If you spot a problem unrelated to the task, mention it but don't fix it without asking.
- **Build requires JDK 21** — set `JAVA_HOME` before every `./gradlew`.
- **Docker images are stale after JAR changes** — rebuild always.
- **Verify before claiming done.** Evidence over assertions.
