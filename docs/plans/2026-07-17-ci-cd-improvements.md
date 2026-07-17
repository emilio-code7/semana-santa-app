# CI/CD Improvements Plan

> **Goal:** Professional CI/CD pipeline for a portfolio-ready microservices project.
> **Current:** 2 GitHub Actions workflows (ci.yml + deploy.yml) — basic but functional.

---

## Current State

| Workflow | Trigger | What it does | Time |
|----------|---------|-------------|------|
| `ci.yml` | Every push | `./gradlew test`, OpenAPI validation, shell check | ~3 min |
| `deploy.yml` | Push to main | Build 3 Docker images → ECR → SSH deploy to EC2 | ~10 min |

**Gaps:**
- Tests run sequentially (1 service at a time)
- Integration tests skip (no PostgreSQL in CI)
- Deploy fails entirely if one service breaks
- No PR status checks configured
- No CDK deployment for AWS infra
- No security scanning, linting, or dependency checks

---

## Proposed Improvements

Grouped by value-to-effort ratio.

### Group A: Fast Wins (1-2 hours each)

#### A1: Parallel test matrix

Split CI into 3 parallel jobs, one per service. Runs in ~1 min instead of ~3.

**Config:**
```yaml
strategy:
  matrix:
    service: [hermandad-service, procesion-service, repertorio-service]
steps:
  - run: ./gradlew :services:${{ matrix.service }}:test
```

**Trade-off:** Uses 3× the runner minutes but 3× faster wall clock. Still within free tier.

#### A2: PostgreSQL service container

Add a PostgreSQL service container so integration tests run in CI instead of skipping.

```yaml
services:
  postgres:
    image: postgres:16
    env:
      POSTGRES_PASSWORD: postgres
    options: --health-cmd pg_isready --health-interval 10s --health-timeout 5s --health-retries 5
```

With env: `JDBC_URL: jdbc:postgresql://postgres:5432/`

**Trade-off:** Adds ~30s to CI. Catches DB-related bugs before merge (like the JPA scanning issue we found during docker-compose testing). Probably the single highest-value improvement.

#### A3: Test report artifacts

Upload test reports on failure so you can see what broke without running locally.

```yaml
- uses: actions/upload-artifact@v4
  if: failure()
  with:
    name: test-reports
    path: services/*/build/reports/tests/
```

**Trade-off:** Minimal cost. Invaluable for debugging CI failures.

#### A4: README badges

Add a badge showing CI status:
```markdown
[![CI](https://github.com/emilio-code7/semana-santa-app/actions/workflows/ci.yml/badge.svg)](https://github.com/emilio-code7/semana-santa-app/actions/workflows/ci.yml)
```

**Trade-off:** 1 line. Makes the repo look professional.

---

### Group B: Medium Value (half day each)

#### B1: Decoupled per-service deploy

Current `deploy.yml` is monolithic — one job that builds all 3 services, pushes all, deploys all. If repertorio fails, nothing ships.

Split into 3 separate build jobs + 1 deploy job that waits for all builds:

```yaml
jobs:
  build-hermandad:
  build-procesion:
  build-repertorio:
  deploy:
    needs: [build-hermandad, build-procesion, build-repertorio]
```

**Trade-off:** More YAML but safer deploys. If one service breaks, the other two still deploy.

#### B2: PR checks + status gates

Configure GitHub to require CI passing before merge. Add a `status-check` job that blocks merge unless all matrix jobs pass.

**Trade-off:** Prevents broken code from reaching main. Requires branch protection rules (Settings → Branches).

#### B3: CDK deploy in CI

Add a job that runs `cdk deploy` to manage AWS infrastructure alongside service deploys.

```yaml
jobs:
  infra:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/setup-node@v4
      - run: npm ci && npx cdk deploy --require-approval never
```

**Trade-off:** Requires AWS credentials with CloudFormation permissions. The CDK code is in `infrastructure/aws/`. Should run before service builds since ECR repos need to exist.

#### B4: Dependabot + Dependency review

GitHub-native dependency scanning. Zero config — just enable in repo settings.

```yaml
# .github/dependabot.yml
version: 2
updates:
  - package-ecosystem: "gradle"
    directory: "/"
    schedule:
      interval: "weekly"
```

**Trade-off:** May generate noisy PRs. Worth it for security.

---

### Group C: Polish (when you have time)

| Improvement | What | Effort |
|------------|------|--------|
| **Linting** | Spotless (Gradle plugin) for Java format checks | 1 hour setup, ongoing maintenance |
| **Secret scanning** | Gitleaks + `actions/secret-scan` | 30 min |
| **SBOM generation** | Dependency snapshot via `cyclonedx-gradle-plugin` | 1 hour |
| **Deploy preview** | Spin up ephemeral stack on PR | 2-3 days (new feature) |
| **GitHub Pages** | Auto-publish OpenAPI spec to `docs/` page | 1 hour |

---

## Recommended Execution Order

```
Phase 1 (this sprint):
  A1 — Parallel test matrix
  A2 — PostgreSQL service container for ITs
  A3 — Test report artifacts
  A4 — README badge

Phase 2 (next sprint):
  B1 — Decoupled per-service deploy
  B2 — PR status checks
  B3 — CDK deploy step

Phase 3 (backlog):
  B4 — Dependabot
  C — Linting, secret scan, SBOM, docs
```

---

## Free Tier Budget

| Feature | Monthly minutes |
|---------|----------------|
| Current CI (3 min × 50 pushes) | 150 min |
| Parallel matrix (1 min × 50 pushes) | 50 min |
| Add PG service container | +50 min |
| Deploy (10 min × 10 merges) | 100 min |
| **Total estimated** | **~200 min / month** |
| **Free tier limit** | **2,000 min / month** |

Well within limits. No cost risk.
