# Repertorio — Agent Guide

## Identity

Semana Santa management system: 3 Spring Boot microservices (hermandad, procesion, repertorio), event-driven via Kafka outbox, JWT auth via Keycloak, hexagonal + DDD.

## Mode Detection (read first)

If the request mentions any of these keywords → activate **Infrastructure Mode**:
`aws`, `cdk`, `ec2`, `rds`, `sqs`, `cognito`, `ecr`, `ecs`, `iam`, `vpc`, `deploy`, `deployment`, `ci/cd`, `pipeline`, `github actions`, `github workflows`, `docker`, `container`, `infrastructure`, `terraform`, `cloudformation`, `nginx`, `load balancer`, `monitoring`, `alerts`, `secrets`, `ssl`, `domain`, `dns`

If the request mentions any of these keywords → activate **Frontend Mode**:
`frontend`, `next.js`, `next`, `react`, `component`, `page`, `ui`, `ux`, `styling`, `tailwind`, `shadcn`, `layout`, `responsive`, `animation`, `design`, `css`, `rsc`, `server component`, `client component`, `keycloak`, `next-auth`, `login`, `session`, `navbar`, `header`, `footer`, `sidebar`, `card`, `grid`, `form`, `dialog`, `modal`, `toast`, `table`, `list`

Everything else → activate **Backend Mode** (default).

## First Thing (all modes)

Read `docs/functional-map.md` before any implementation work — it's your complete reference: topology, profiles, endpoints, DB schemas, test inventory, operating principles. This file is the pointer. That file is the truth.

---

# Infrastructure Mode

```
Explore → Assess → Design → Implement → Diff → Review → Verify → Store → Commit
```

## Key Context Files

- **`docs/aws-guide.md`** — 317-line comprehensive reference: AWS migration rationale, architecture, service-by-service mapping, before/after comparison, deploy instructions, FAQ
- **`infrastructure/aws/`** — AWS CDK stack (TypeScript): `bin/app.ts` entry, `lib/stack.ts` main stack, `deploy-outputs.json` live resources
- **`.github/workflows/ci.yml`** — CI pipeline (build + test + OpenAPI check)
- **`.github/workflows/deploy.yml`** — CD pipeline (build → ECR push → SSH deploy → health check)
- **`docker-compose.aws.yml`** — EC2 deployment compose (nginx + 3 services, no Kafka/Keycloak — uses AWS managed services)
- **`scripts/deploy-aws.sh`** — Manual deploy alternative

## Current AWS State (live)

| Resource | Details |
|---|---|
| **EC2** | t3.micro, Amazon Linux 2023, IP: `98.81.108.60` |
| **RDS** | PostgreSQL 16, db.t3.micro, 20GB gp2, 5 databases |
| **SQS** | 3 queues (`hermandad-events`, `hermandad-member-events`, `procesion-events`) + 3 DLQs |
| **Cognito** | User Pool `us-east-1_V6Uwds4fO`, Client `7jlsvkd6jrajupfkcoaqsn6dd` |
| **ECR** | 3 repos: `repertorio/hermandad-service`, `repertorio/procesion-service`, `repertorio/repertorio-service` |
| **Lambda** | Pre-token generation (Node.js 20, Cognito group → claim injection) |
| **IAM** | EC2 role: SSM + SQS + ECR pull |

## AWS Profile Architecture

Spring Boot services use `@Profile("aws")` to swap adapters:
- `@Profile("aws")` → SQS messaging, Cognito auth
- `@Profile("!aws")` → Kafka messaging, Keycloak auth (local dev)
- Nginx reverse proxy routes `/api/hermandades/` → `:8081`, `/api/procesiones/` → `:8082`, `/api/marchas/` → `:8083`
- Eureka disabled on EC2 (`EUREKA_CLIENT_ENABLED=false`)

## Infrastructure Workflow (step-by-step)

1. **Explore** — Delegate to `@explorer`: recon the affected CDK modules, CI workflows, existing AWS resources. Read `docs/aws-guide.md` §relevant section.

2. **Assess** — Read `infrastructure/aws/deploy-outputs.json` for live resource state. Run `cdk diff` to see what would change. Never touch AWS without knowing the current state.

3. **Design** — `@oracle` is **mandatory** for: IAM policy changes, new AWS services, security group modifications, cost-impacting changes (instance types, RDS sizing). Oracle must approve before any CDK code is written.

4. **Implement** — Delegate to `@fixer` for bounded CDK/CI changes. Handle single-line config edits directly. CDK is TypeScript — same tooling as the rest of the project.

5. **Diff** — Run `cdk diff` and present the changes before deploying. Agent must show what resources will be created/modified/destroyed.

6. **Review** — `@oracle` review against the CDK diff. Same evidence standard as Development Mode (diff hash, files, issues, verdict).

7. **Verify** — `cdk synth` succeeds, `docker build` works for affected services, GitHub Actions syntax valid, no secrets in committed files.

8. **Store** — Memory tool: why this infra decision was made. Future sessions need to know "we chose SQS over MSK because free tier."

9. **Commit** — Conventional commit: `feat(infra):`, `fix(infra):`, `chore(infra):`. Check `git diff` before committing.

## Infrastructure Hard Rules

1. **Never edit live AWS resources in the console.** Everything through CDK. Manual changes cause drift.
2. **Never deploy without `cdk diff` first.** Blind deploys destroy resources.
3. **Never change IAM policies without Oracle review.** Least privilege is hard.
4. **Never commit AWS credentials, IPs, or secrets.** Already in `.env` — don't leak to git.
5. **Never change instance types without cost analysis.** t3.micro is free tier. Anything else costs money.
6. **Always update `docs/aws-guide.md`** when adding services, changing architecture, or modifying the deploy process.
7. **Always run `cdk synth` before `cdk deploy`.** Catch synthesis errors locally, not in CloudFormation.

## Infrastructure Common Mistakes

| Mistake | Correction |
|---|---|
| Editing resources in AWS console then syncing CDK | CDK is source of truth. Console-first causes drift conflicts. |
| Forgetting to update `deploy-outputs.json` | CDK outputs track live resource IDs. Stale outputs cause misconfigured deploys. |
| Hardcoding region/account in CDK code | Use `cdk.Stack.of(this).region` and `cdk.Stack.of(this).account`. |
| Deploying CDK changes without reading `cdk diff` | Always read the diff. Replacement of stateful resources = data loss. |
| Mixing `deploy-aws.sh` and GitHub Actions | Pick one deploy path. Mixed deploys cause version conflicts. |
| Changing security groups without testing connectivity | Use `docker-compose.aws.yml` locally with LocalStack to validate networking. |

## Infrastructure Verification Gate

Before declaring any infra task complete:
1. `cdk synth` — CloudFormation template generates without errors
2. `cdk diff` — reviewed and approved by Oracle for non-trivial changes
3. No hardcoded secrets, IPs, or credentials in committed files
4. `docs/aws-guide.md` updated if architecture changed
5. `deploy-outputs.json` committed if resources changed

---

# Frontend Mode

```
Explore → Spec → Design → Implement → Review → Verify → Store → Commit
```

## Key Context Files

- **`frontend/package.json`** — Next.js 16.2, React 19.2, TypeScript 5 strict, Tailwind v4, next-auth v5
- **`frontend/src/lib/auth.ts`** — NextAuth config: Keycloak provider, JWT session, accessToken injection
- **`frontend/src/middleware.ts`** — Route protection: `/hermandades/*/admin/*` requires auth
- **`frontend/src/types/`** — Type definitions: hermandad, procesion, marcha, cruceta
- **`frontend/src/components/ui/`** — shadcn/ui components (base-nova style, 13 components)
- **`docs/openapi.yaml`** — API contract for the 3 backend services

## Frontend Tech Stack

| Layer | Technology |
|---|---|
| Framework | Next.js 16.2.10 (App Router) |
| React | 19.2.4 (RSC-first, 'use client' only when needed) |
| Language | TypeScript 5 (strict) |
| Styling | Tailwind CSS v4 (CSS-first config, no `tailwind.config.js`) |
| Components | shadcn/ui (base-nova), lucide-react icons |
| Server state | TanStack React Query v5 |
| Auth | next-auth v5 (beta) + Keycloak |
| Toasts | sonner |
| Fonts | Geist Sans, Geist Mono, Cormorant Garamond |
| Backend API | Spring Cloud Gateway at port 8080 |

## Frontend Workflow (step-by-step)

1. **Explore** — Delegate to `@explorer`: recon affected components, pages, existing patterns, backend endpoints used.

2. **Read the spec** — `docs/openapi.yaml` for the endpoint shapes, `docs/functional-map.md` for service topology.

3. **Design** — For non-trivial UI changes, delegate to `@designer`. Designer owns: layout, spacing, hierarchy, motion, color, affordances, responsive behavior. Orchestrator may only edit copy after design work, must preserve visual structure.

4. **Implement** — For mechanical changes (API wiring, data fetching, types): delegate to `@fixer`. For visual/UX changes: always `@designer`. Follow these patterns:
   - **Server Components by default** — only add `'use client'` for interactivity (state, effects, event handlers)
   - **Data fetching**: Server Components use `fetch()` directly. Client Components use `useQuery`/`useMutation` from TanStack React Query
   - **Loading states**: Every page/component with async data must handle loading (via Next.js `loading.tsx` or Suspense)
   - **Error states**: Every data fetch must handle errors (via `error.tsx` or ErrorBoundary)
   - **Empty states**: Empty lists/tables must show a meaningful empty state, not just nothing
   - **Auth**: Use `auth()` in server components, `useSession()` or `session.accessToken` in client components
   - **API URL**: Use `NEXT_PUBLIC_API_URL` env variable, never hardcode `localhost:8080`

5. **Review** — After implementation, run user-facing checks:
   - Does it work at mobile (375px) and desktop (1440px)?
   - Are loading/error/empty states handled?
   - Are there hardcoded API URLs?
   - Does auth work (logged in vs logged out behavior)?

6. **Verify** — `npm run build` succeeds, no TypeScript errors, no hardcoded `localhost:8080`.

7. **Store** — Memory tool: frontend patterns discovered, gotchas.

8. **Commit** — Conventional commit: `feat(frontend):`, `fix(frontend):`, `style(frontend):`.

## Frontend Hard Rules

1. **No hardcoded API URLs.** Use `NEXT_PUBLIC_API_URL` environment variable.
2. **Server Components by default.** Only add `'use client'` when you need `useState`, `useEffect`, `onClick`, or browser APIs.
3. **Always handle three states.** Every data display must handle: loading, error, empty.
4. **Use shadcn/ui, don't build custom.** Buttons, inputs, dialogs, tables — use the existing `src/components/ui/` components.
5. **Responsive-first.** Desktop layout is the default, but every component must collapse gracefully at mobile sizes.
6. **Types over `any`.** Every API response must have a type in `src/types/`. No `any` in production code.
7. **Delegate design to @designer.** Never implement styling/visual changes yourself — that's the Designer's lane.

## Frontend Common Mistakes

| Mistake | Correction |
|---|---|
| Hardcoding `localhost:8080` | Use `process.env.NEXT_PUBLIC_API_URL` |
| Missing loading state | Add `loading.tsx` or Suspense boundary |
| Client component when server would work | Remove `'use client'`, use server fetch |
| Raw `fetch` everywhere | Centralize in `src/lib/api.ts` (create if missing) |
| No error boundary | Add `error.tsx` at the route segment level |
| Building custom UI from scratch | Use existing shadcn/ui components, extend only if needed |
| Implementing design yourself | Delegate to `@designer` for any visual/polish work |

## Frontend Verification Gate

Before declaring any frontend task complete:
1. `cd frontend && npm run build` — builds without errors
2. No hardcoded `localhost:8080` in new code
3. All new pages/components handle loading, error, and empty states
4. Responsive at 375px width (use Chrome DevTools mobile view)

---

# Backend Mode

```
Explore → Spec → Plan → Gherkin → Design → Implement → Review → Verify → Store → Commit
```

## Key Context Files

- **`docs/functional-map.md`** — Complete reference: topology, profiles, endpoints, DB schemas, test inventory
- **`docs/openapi.yaml`** — API contract (the spec is the contract, code implements the spec)
- **`docs/architecture.md`** — Hexagonal + DDD design decisions
- **`docs/plans/`** — Sprint plans with acceptance criteria

## Development Workflow (step-by-step)

1. **Explore** — Delegate to `@explorer`. Recon the affected code area: locate related files, existing tests, patterns in use. Return a compressed summary. Do not read every file yourself.

2. **Read the spec** — `docs/functional-map.md` for project context, `docs/openapi.yaml` for API contracts, the relevant plan in `docs/plans/`.

3. **Update OpenAPI spec FIRST** — Before writing any code, update `docs/openapi.yaml` with the new endpoints, request/response schemas, and operationIds. The spec is the contract. Code implements the spec, not the other way around.

4. **Extract Gherkin scenarios** — Write `Feature:`, `Scenario:`, `Given/When/Then` blocks covering:
   - Happy path
   - Error/edge cases (null, not found, invalid state)
   - Auth/permission boundaries

5. **Present scenarios for review** — Show the scenarios. Wait for approval before implementing.

6. **Design review** — For non-trivial changes, consult `@oracle` on architecture: trade-offs, hex layer placement, data flow, potential risks. Oracle is an escalation, not a default — use it when the change is complex or risky.

7. **Implement TDD (RED → GREEN → REFACTOR)** — This is not optional.
   - **RED**: Write the failing test FIRST, before any implementation code.
   - **GREEN**: Delegate bounded implementation to `@fixer` for multi-file changes. Handle trivial single-file edits directly.
   - **REFACTOR**: Clean up without changing behavior.
   - If you wrote implementation before tests, you skipped TDD. Delete the implementation and start over.
   - One behavior change per commit. Small commits.

8. **Code review** — After implementation, request `@oracle` review against the diff. The review must include:
   - Git diff hash (sha256 of the diff being reviewed)
   - Files reviewed (explicit list)
   - Issues found (categorized: bug / spec-violation / code-smell / scope-creep)
   - Verdict: approved / changes-requested
   - If the review finds issues, fix them and re-run until clean. **Fixes must also follow TDD** — write the failing test that reproduces the issue first, then fix the code, then verify the test passes.

9. **Validate against spec** — Check the OpenAPI spec matches the implementation (response codes, field types, endpoint paths).

10. **Update docs** — See functional-map §0.9 for which docs to update per change type. Regenerate Bruno scripts if API changed.

11. **Verify gate** — Tests pass, build succeeds, no FIXME/TODO/HACK markers, pre-commit hook passes.

12. **Store decisions** — Use the `memory` tool to store key architectural decisions, patterns discovered, or gotchas. This persists across sessions so future agents don't re-learn the same lessons.

13. **Commit** — Conventional commit message. Check `git diff` before committing.

## Delegation Rules

These are hard thresholds. Do not do manually what a specialist should handle.

| Trigger | Action |
|---|---|
| Reading 4+ files to understand a flow | Delegate exploration to `@explorer` |
| Touching 2+ non-trivial files for implementation | Delegate to `@fixer` |
| Architecture decision, trade-off, or hard bug | Consult `@oracle` |
| UI/UX work (layout, styling, animation, component feel) | Delegate to `@designer` |
| External library research or current docs lookup | Delegate to `@librarian` |
| Single trivial change (<20 lines, one file) | Handle directly |

### Why

The pre-commit hook blocks commits that change controllers without updating OpenAPI. The auto-run code review (step 8) catches spec violations, code smells, and scope creep before the human sees the work — reducing feedback loops. The CI pipeline validates the spec on every push. Specialist delegation keeps the orchestrator's context lean and routes each job to the best model.

## Hard Rules (never violate)

1. **No JPA annotations in domain layer.** `@Entity`, `@Table`, `@Id` go in adapter JPA entities. Domain classes are pure Java.
2. **No REST calls between services.** Cross-service communication is async via Kafka (outbox pattern). No `RestTemplate`, `WebClient`, or Feign calls across service boundaries.
3. **All events go through the outbox.** Kafka direct produce is forbidden. `DomainEventPublisherAdapter` publishes to both `ApplicationEventPublisher` (in-process) and outbox table → poller → Kafka.
4. **Every schema change is a new Flyway migration.** Never edit existing migrations. Increment the version number.
5. **No `permitAll()` on write endpoints.** `anyRequest().authenticated()` is the base rule. Public endpoints are explicitly listed and must be GET-only.
6. **Every non-trivial behavior change needs a test that would fail without it.** No test = incomplete.

## Before You Start Working

- Read the plan file in `docs/plans/` for the current sprint first
- Read `docs/functional-map.md` §0 (Operating Principles) — covers hexagonal layering, DDD, security, outbox, Flyway, YAGNI, commit discipline
- For implementation: use the plan's acceptance criteria as your test
- For architecture questions: read `docs/architecture.md`

## Common Mistakes (previous agents made these)

| Mistake | Correction |
|---------|-----------|
| Adding `@Entity` on domain/model classes | JPA entities go in `adapter/outbound/persistence/` |
| Forgetting `@EnableKafka` on the app class | Every Kafka consumer needs it |
| Using `tools.jackson` imports | Yes, Spring Boot 4.1 uses `tools.jackson` — NOT `com.fasterxml.jackson` |
| Writing code without reading the plan first | The plan has acceptance criteria. Read it. |
| Claiming "all tests pass" without running them | Run `./gradlew test` first. Verify output. |
| Adding dead abstractions (interface for single impl, factory for one product) | YAGNI. One implementation doesn't need an interface. |
| Implementing before delegating exploration | Multi-file changes: `@explorer` first, then `@fixer`. Not read-all-files-yourself. |
| Skipping Oracle on architectural changes | Complex domain logic or new service boundaries → `@oracle` before code. |
| Forgetting to store decisions to memory | After commit, store architectural decisions via `memory` tool or they're lost next session. |

## Verification Gate

Before declaring any task complete:
1. `./gradlew :services:<affected-service>:test` — all tests pass
2. `./gradlew :services:<affected-service>:compileJava` — build succeeds
3. No `FIXME`, `TODO`, or `HACK` markers in new/modified files
4. Acceptance criteria from the plan are met
5. OpenAPI spec matches implementation (response codes, field types, endpoints)
6. Oracle review approved (for non-trivial changes)

---

## Commit Messages

Format: `type(scope): description`
Types: `feat:`, `fix:`, `refactor:`, `test:`, `docs:`, `chore:`
For infrastructure: `feat(infra):`, `fix(infra):`, `chore(infra):`
One concern per commit. Check `git diff` before committing.

## Agent skills

### Issue tracker

Issues live on GitHub at `github.com/emilio-code7/semana-santa-app`. See `docs/agents/issue-tracker.md`.

### Triage labels

Default labels (needs-triage, needs-info, ready-for-agent, ready-for-human, wontfix). See `docs/agents/triage-labels.md`.

### Domain docs

Single-context — one AGENTS.md + docs/. See `docs/agents/domain.md`.

### Memory

Use the `memory` tool to persist architectural decisions, discovered patterns, and hard-won fixes. Future sessions (and other agents) will retrieve them automatically. Scope: `project`.
