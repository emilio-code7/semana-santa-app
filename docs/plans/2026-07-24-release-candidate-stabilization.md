# Release Candidate Stabilization Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Produce a trustworthy local release candidate by making deployment explicitly manual, closing the remaining agent-guide review gaps, fixing all backend suite failures, and patching reachable frontend vulnerabilities.

**Architecture:** Complete the small governance delta before application work so subsequent branches inherit the corrected workflow and cannot deploy automatically. After that gate, execute backend JPA/test-slice repair and frontend dependency remediation in isolated, parallel worktrees. Merge those lanes independently, then run release-candidate verification without deploying AWS. CI/CD redesign remains owned by the existing CI/CD standardization plan.

**Tech Stack:** Markdown agent guidance, GitHub Actions, Spring Boot 4.1, Spring Data JPA, Gradle, Next.js 16, Auth.js v5, npm, Docker Compose.

---

## Scope and evidence claims

This plan establishes these claims:

1. Merging to `main` cannot automatically deploy through the current SSH workflow.
2. The agent guide preserves project invariants while intentionally using risk-triggered review and specialist routing.
3. All three backend service test tasks pass without skips added to hide failures.
4. The production-reachable frontend advisories are patched and the frontend still builds.
5. The local application stack can complete its existing smoke path.

This plan does **not** establish production AWS readiness. It must not run `cdk deploy`, update live outputs, provision `cruceta-events`, close port 22, or replace SSH/credentials. Those remain in `docs/plans/2026-07-24-ci-cd-standardization.md` after this plan is green.

## Execution graph

```text
Wave 0 — governance (parallel, separate worktrees)

G1 Correct agent-guide refactor ───────────────┐
                                               ├─> G3 merge deploy gate first,
G2 Disable automatic deployment ──────────────┘   then merge agent-guide PR

Wave 1 — stabilization (parallel, from updated main)

S1 Backend JPA/test-slice repair ──────────────┐
                                               ├─> S3 integrate both PRs
S2 Frontend security patch ────────────────────┘

Wave 2 — release verification (sequential)

S3 integrate -> V1 local release-candidate verification

Wave 3 — explicitly later

Secure AWS activation -> Route-aware Cruceta
```

### Parallel ownership

| Lane | Owner | Files | May run with |
|------|-------|-------|--------------|
| G1 agent guide | one `@fixer` | `AGENTS.md`, three workflow skills, agent-guide plan | G2 |
| G2 deploy gate | one `@fixer` in separate worktree | `.github/workflows/deploy.yml` | G1 |
| S1 backend | one `@fixer` | shared outbox auto-config + three application classes | S2 |
| S2 frontend | one `@fixer` | `frontend/package.json`, `frontend/package-lock.json` | S1 |

Do not split S1 into three service writers. The failures share one persistence configuration seam, and the four production files should remain one coherent fix.

---

### Task 1: Close the remaining agent-guide review gaps

**Depends on:** None

**Files:**
- Modify: `AGENTS.md`
- Modify: `.agents/skills/backend-workflow/SKILL.md`
- Modify: `.agents/skills/frontend-workflow/SKILL.md`
- Modify: `.agents/skills/infrastructure-workflow/SKILL.md`
- Modify: `docs/plans/2026-07-24-agents-guide-refactor.md`

**Step 1: Record intentional policy changes in the refactor plan**

Add a short section stating that these are deliberate changes, not accidental omissions:

- routine Oracle review is risk-triggered;
- simple operational work is delegated to exactly one fixer;
- live `deploy-outputs.json` changes only after a verified deployment;
- keyword routing is replaced by intent routing.

**Step 2: Restore concise cross-cutting routing rules**

Keep `AGENTS.md` below 200 lines. Add only the missing durable rules:

- graph tools are first for exploration/review; file scans are fallback;
- 4+ files or unfamiliar flows use `@explorer`;
- external/version-specific research uses `@librarian`;
- multi-file non-visual implementation uses bounded `@fixer` work;
- visual work uses `@designer`;
- strategic/security/data-integrity decisions use `@oracle`;
- reference `docs/agents/domain.md` as the single-context domain guide.

**Step 3: Remove workflow contradictions**

In all three workflow skills, replace direct-scan wording with:

```markdown
Use code-review-graph first. Delegate broad or unfamiliar discovery to `@explorer`; fall back to targeted Grep/Glob/Read only when the graph does not cover the need.
```

**Step 4: Restore dropped mode-specific invariants**

- Backend: explicitly route current external-library research to `@librarian`.
- Infrastructure: update `docs/aws-guide.md` when the deployment process changes, not only when architecture changes; reference `.github/workflows/deploy.yml`.
- Keep review risk-triggered as documented in Step 1; do not restore unconditional Oracle review.

**Step 5: Verify**

Run:

```bash
wc -l AGENTS.md
git diff --check
git status --short
git diff --name-only
```

Expected:

- `AGENTS.md` is at most 200 lines;
- no whitespace errors;
- only agent/tooling/docs files changed;
- no application, OpenAPI, infrastructure, or workflow file changed in this lane.

**Step 6: Commit**

```bash
git add AGENTS.md .agents/skills docs/plans/2026-07-24-agents-guide-refactor.md
git commit -m "fix(agents): preserve workflow guidance after refactor"
```

Do not merge this PR until Task 2 is merged to `main`.

---

### Task 2: Disable automatic production deployment

**Depends on:** None

**Files:**
- Modify: `.github/workflows/deploy.yml`

**Step 1: Create a dedicated issue and worktree**

Create a short-lived branch from `origin/main` using the repository convention:

```text
infra/<issue>-pause-automatic-deploy
```

Do not base this lane on `docs/agent-guide-refactor`.

**Step 2: Replace the push trigger**

Change only the trigger:

```yaml
on:
  workflow_dispatch:
```

Do not add path filters. A path filter still permits unreviewed automatic production deployment for service changes. Keep `.github/workflows/ci.yml` unchanged so CI continues on pushes and pull requests.

**Step 3: Validate the workflow**

Run:

```bash
actionlint .github/workflows/deploy.yml
git diff --check
git diff -- .github/workflows/ci.yml
```

Expected:

- actionlint exits 0;
- `deploy.yml` has no `push:` trigger;
- `ci.yml` has no diff.

If `actionlint` is unavailable, use the repository-approved validation mechanism without adding a permanent dependency.

**Step 4: Commit and open the first PR**

```bash
git add .github/workflows/deploy.yml
git commit -m "fix(ci): require manual production deployment"
git push -u origin infra/<issue>-pause-automatic-deploy
gh pr create --fill
```

Squash-merge this PR before the agent-guide PR. No workflow should be manually dispatched.

---

### Task 3: Merge governance in the safe order

**Depends on:** Tasks 1 and 2

**Files:** None beyond merge metadata

**Step 1: Merge the deploy-gate PR**

Require CI to pass, then squash-merge Task 2.

**Step 2: Verify the live default branch file**

Run:

```bash
gh api repos/emilio-code7/semana-santa-app/contents/.github/workflows/deploy.yml --jq '.content' | base64 -d
```

Expected: `workflow_dispatch` is present and `push` is absent.

**Step 3: Update and merge the agent-guide PR**

The existing `docs/agent-guide-refactor` branch contains both tooling standardization and the guide refactor. Open one PR, describe both commits, and squash-merge after the deploy gate exists on `main`.

Do not push local `main` directly. Do not rename or force-push the existing remote branch without explicit approval.

**Step 4: Start stabilization from fresh main**

```bash
git checkout main
git pull --ff-only
```

Tasks 4 and 5 must branch from this updated commit.

---

### Task 4: Repair shared outbox JPA configuration and backend test slices

**Depends on:** Task 3

**Files:**
- Modify: `shared/common/src/main/java/com/repertorio/common/outbox/OutboxJpaAutoConfiguration.java`
- Modify: `services/hermandad-service/src/main/java/com/repertorio/hermandad/HermandadServiceApplication.java`
- Modify: `services/procesion-service/src/main/java/com/repertorio/procesion/ProcesionServiceApplication.java`
- Modify: `services/repertorio-service/src/main/java/com/repertorio/marcha/RepertorioServiceApplication.java`

**Claim:** Shared outbox JPA wiring is active in full JPA contexts but does not force repositories into `@WebMvcTest` slices.

**Step 1: Create the RED evidence**

Run:

```bash
./gradlew :services:hermandad-service:test --tests '*HermandadControllerTest' --tests '*HermandadMemberDataJpaTest'
./gradlew :services:procesion-service:test --tests '*ProcesionControllerTest'
./gradlew :services:repertorio-service:test --tests '*MarchaControllerTest' --tests '*CrucetaControllerTest'
```

Expected RED:

- web slices fail because `OutboxEventJpaRepository` requests a missing `entityManagerFactory`;
- the JPA slice reports `OutboxEventEntity` is not a managed type.

Save the first complete root-cause chain from each failure group in the task notes.

**Step 2: Move shared repository registration to shared auto-configuration**

Add repository registration next to the existing entity scan:

```java
@EnableJpaRepositories(basePackageClasses = OutboxEventJpaRepository.class)
```

Keep the existing `@AutoConfiguration(after = HibernateJpaAutoConfiguration.class)` and `@ConditionalOnBean(EntityManagerFactory.class)` guard.

**Step 3: Remove direct shared repository scanning from each application**

For each service application:

- keep service-local repositories in `@EnableJpaRepositories`;
- remove `com.repertorio.common.outbox` from that annotation;
- include both the service package and `com.repertorio.common.outbox` in `@EntityScan` so full JPA and `@DataJpaTest` contexts manage the shared entity;
- keep shared outbox component scanning unchanged.

Do not modify controller tests, add mocks for `EntityManagerFactory`, disable repositories, or skip tests.

**Step 4: Run focused GREEN checks**

Repeat the three commands from Step 1.

Expected: all selected tests pass.

**Step 5: Run the full backend suites**

```bash
./gradlew :shared:common:test \
  :services:hermandad-service:test \
  :services:procesion-service:test \
  :services:repertorio-service:test
```

Expected: build succeeds with zero failed tests. Existing environment-dependent integration tests may skip only where their existing contract explicitly allows it; do not introduce new skips.

**Step 6: Commit and open PR**

```bash
git add shared/common/src/main/java/com/repertorio/common/outbox/OutboxJpaAutoConfiguration.java \
  services/hermandad-service/src/main/java/com/repertorio/hermandad/HermandadServiceApplication.java \
  services/procesion-service/src/main/java/com/repertorio/procesion/ProcesionServiceApplication.java \
  services/repertorio-service/src/main/java/com/repertorio/marcha/RepertorioServiceApplication.java
git commit -m "fix(outbox): isolate shared JPA auto-configuration"
```

Use branch `fix/<issue>-outbox-jpa-test-slices`; open a PR and attach RED/GREEN evidence.

---

### Task 5: Patch frontend authentication and framework advisories

**Depends on:** Task 3

**Runs in parallel with:** Task 4

**Files:**
- Modify: `frontend/package.json`
- Modify: `frontend/package-lock.json`
- Update: `docs/plans/2026-07-24-release-candidate-stabilization.md` when audit evidence identifies an unfixable transitive advisory

**Claim:** The reachable Auth.js fail-open advisory is patched, the frontend builds, and any remaining transitive advisories are explicitly bounded and documented.

**Step 1: Capture RED security evidence**

Run:

```bash
cd frontend
npm ci
npm audit --audit-level=high
```

Expected RED: audit exits non-zero and reports vulnerable `next@16.2.10` and `next-auth@5.0.0-beta.31`/`@auth/core@0.41.2`.

**Step 2: Apply only patched dependency versions**

Run:

```bash
npm install next@16.2.11 next-auth@5.0.0-beta.32
```

Do not rename `middleware.ts` in this task. The `proxy.ts` migration is a separate deprecation cleanup, not part of the security patch.

**Step 3: Verify resolved versions and audit**

```bash
npm ls next next-auth @auth/core postcss sharp
npm audit --audit-level=critical
npm run build
```

Expected:

- `next` resolves to at least 16.2.11;
- `next-auth` resolves to at least 5.0.0-beta.32;
- `@auth/core` resolves to at least 0.41.3;
- the critical audit gate exits 0 and the Auth.js fail-open advisory is absent;
- production build succeeds.

The current latest Next release still bundles `postcss@8.4.31` and `sharp@0.34.5`, so full `npm audit` may report transitive high advisories. Do not force Sharp or PostCSS overrides: Next pins those dependencies and Sharp 0.35 crosses a breaking 0.x boundary. Record the residual risk as accepted only because this app has repository-controlled CSS, no user CSS processing, no image uploads, and no `next/image` usage. Reassess on every Next release and before enabling those features. Never run `npm audit fix --force`.

**Step 4: Commit and open PR**

```bash
git add frontend/package.json frontend/package-lock.json docs/plans/2026-07-24-release-candidate-stabilization.md
git commit -m "fix(frontend): patch authentication dependencies"
```

Use branch `fix/<issue>-frontend-security-advisories`; open a PR with audit/build evidence.

---

### Task 6: Integrate stabilization lanes

**Depends on:** Tasks 4 and 5

**Step 1: Review both PRs independently**

- Backend PR: confirm only four production configuration files changed and full suites pass.
- Frontend PR: confirm package files plus the residual-risk plan note changed and no forced dependency upgrade occurred.

**Step 2: Squash-merge each PR**

Because ownership is disjoint, either PR may merge first after CI passes. Delete both branches after merge.

**Step 3: Refresh main and run the combined gate**

```bash
git checkout main
git pull --ff-only
./gradlew :shared:common:test \
  :services:hermandad-service:test \
  :services:procesion-service:test \
  :services:repertorio-service:test
(cd frontend && npm ci && npm audit --audit-level=critical && npm run build)
```

Expected: all commands exit 0.

---

### Task 7: Run local release-candidate verification

**Depends on:** Task 6

**Files:** No source changes expected

**Step 1: Build production artifacts**

```bash
./gradlew :services:hermandad-service:bootJar \
  :services:procesion-service:bootJar \
  :services:repertorio-service:bootJar
docker build -t repertorio/hermandad:rc services/hermandad-service
docker build -t repertorio/procesion:rc services/procesion-service
docker build -t repertorio/repertorio:rc services/repertorio-service
```

Expected: all JAR and image builds succeed.

**Step 2: Start the local stack from a known state**

```bash
docker compose -f docker-compose.yml config
docker compose -f docker-compose.yml up -d --build
docker compose -f docker-compose.yml ps
```

Expected: compose config is valid and required services become healthy.

**Step 3: Run the existing API smoke path**

```bash
./api-tests/test-api.sh
```

Expected: the script exits 0. Capture failed endpoint, response code, and service logs if it does not.

**Step 4: Verify frontend release behavior**

```bash
cd frontend
npm ci
npm run build
```

Manually check the existing primary flow at desktop and mobile widths:

1. authenticate through Keycloak;
2. list and create a hermandad;
3. create/list a procession;
4. list/create marchas;
5. open the Cruceta editor.

Record that this is manual evidence; this plan does not add Playwright or another E2E dependency.

**Step 5: Stop the local stack**

```bash
docker compose -f docker-compose.yml down
```

**Step 6: Final review**

Request one scope-aware Oracle review with:

- exact merged commit range;
- files reviewed;
- full backend, frontend audit/build, Docker, API smoke, and CI evidence;
- categorized issues and verdict.

Do not tag a release or deploy AWS in this task.

---

## Exit criteria

- `deploy.yml` is manual-only.
- Agent-guide refactor reviews are approved.
- All shared and service Gradle tests pass.
- Frontend critical audit gate and build pass; any remaining nested Next advisories are documented as bounded residual risk.
- All three service images build.
- Existing API smoke script passes locally.
- Current backend CI commands pass; CI aggregation, frontend CI, and branch protection remain owned by `docs/plans/2026-07-24-ci-cd-standardization.md`.
- No AWS deployment occurred.
- Remaining uncertainties are explicit: manual frontend flow, real Cognito/SQS/RDS behavior, SSM/OIDC migration, `cruceta-events`, and route-aware Cruceta.

## Follow-up order after exit

1. Execute `docs/plans/2026-07-24-ci-cd-standardization.md` for authoritative CI, branch protection, OIDC, SSM, immutable image deployment, and protected AWS activation.
2. Review and deploy the already-approved additive AWS delta separately; do not bundle unrelated CDK changes.
3. Start route-aware Cruceta only after fixing the cross-hermandad ownership test described in `docs/backlog.md:39-43`.
4. Keep GPS, maps, tracking, notifications, and `cruceta-events` deferred until their consumers and product value are defined.
