# CI/CD Standardization Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Standardize CI/CD around OIDC-authenticated immutable-image deploys, trunk-based branch protection, SSM-based EC2 access, and a clear separation between CI, application deployment, and infrastructure lifecycle workflows — without introducing staging, preview environments, blue/green deployments, or ECS.

**Architecture:** GitHub Actions controlled via branch protection rules; GitHub OIDC replaces static IAM user keys; SSM Session Manager replaces SSH for runtime access; Docker images tagged with `${{ github.sha }}` for immutable, traceable deploys; EC2 + Docker Compose retained as the deployment target; infrastructure changes managed through a separate manual/protected CDK workflow with synth → diff → Oracle review → deploy gates.

**Tech Stack:** GitHub Actions, GitHub OIDC provider, AWS IAM, AWS SSM, AWS CDK v2 (TypeScript), Docker, Docker Compose, ECR, Java 21/Gradle (application build), PostgreSQL 16 (RDS).

---

## Current State

| Workflow | Trigger | Notes |
|----------|---------|-------|
| `ci.yml` | Every push | Parallel test matrix, PostgreSQL service container, OpenAPI JSON validation, shell check. CI runs on all branches. |
| `deploy.yml` | Push to main | Monolithic job: build all 3 services, push all ECR images (`:latest` + `${{ github.sha }}`), SSH via appleboy/ssh-action, Docker Compose pull+up, health check. Uses static `AWS_ACCESS_KEY_ID`/`AWS_SECRET_ACCESS_KEY` secrets. |

**Known gaps to close:**

| # | Gap | Resolution task |
|---|-----|-----------------|
| G1 | Deploy uses static IAM user keys stored as repo secrets | GitHub OIDC provider + IAM role with `AssumeRoleWithWebIdentity` |
| G2 | Deploy uses SSH (`appleboy/ssh-action`) — requires port 22 open, key pair stored in secrets, no audit trail | SSM Run Command (IAM permission + `aws ssm send-command`) |
| G3 | `deploy.yml` monolithic — failure in one service blocks all three | Decoupled per-service build jobs + single deploy job with `needs:` |
| G4 | `:latest` tag used alongside SHA tag, making rollback ambiguous | Immutable SHA tags only; `:latest` removed from build/push |
| G5 | No branch protection rules on `main` | Settings: require CI passing, dismiss stale reviews, linear history, no direct push |
| G6 | Infra changes (CDK) are applied manually outside any CI/CD pipeline — no review gate or repeatable process | Separate `infra-deploy.yml` — manual trigger (`workflow_dispatch`), requires Oracle-reviewed diff |
| G7 | No deployment environment or OIDC yet configured in GitHub | Create `production` environment, configure OIDC thumbprint, set env protection rules |
| G8 | EC2 port 22 remains open for SSH, SSM not used for run-time access | Close port 22, verify SSM connectivity, move deploy to SSM |
| G9 | No dry-run deploy or deploy verification beyond basic health check | Add deploy plan step, smoke test suite, rollback procedure documentation |

## Preflight: CDK state constraint

> **⚠️ Hard gate: the current synthesized CDK stack already contains the undeployed `marcha-events` additive delta from Task 6 of the AWS consolidation plan. An OIDC/IAM CDK deploy would apply the **entire** stack delta — including `marcha-events` and any other pending change — in a single CloudFormation execution.**
>
> **Therefore this plan MUST NOT deploy any CDK infrastructure while the diff includes `marcha-events`.**
>
> **Allowed:** `cdk synth`, `cdk diff`, design docs, workflow files.
> **Blocked:** `cdk deploy`, any `cloudformation:*` action against the `RepertorioInfraStack`.
>
> **Required before any infra deploy:**
> 1. A separately reviewed and approved `marcha-events` checkpoint deployment (outside this plan).
> 2. Or another Oracle-approved isolation strategy (e.g. stack分割, feature flag in CDK).
> 3. `cruceta-events` remains explicitly deferred per the AWS consolidation plan.
>
> Synth/diff-only workflows are safe. Deploy-capable workflows must fail closed if they would include unapproved resources.

## Deferred (explicitly out of scope)

| Item | Rationale | Future trigger |
|------|-----------|----------------|
| `marcha-events` CDK deploy | Approved additive checkpoint from Task 6 of the AWS consolidation plan; MUST NOT be deployed by this plan (see Preflight above). Requires separate CDK diff review and Oracle approval. | Separate checkpoint deployment outside this plan |
| `cruceta-events` queue + consumer | Deferred by consolidation plan; cruceta outbox rows remain pending/retried under AWS. | Separate reviewed queue/consumer decision |
| Staging / preview environments | Not in scope. Single `production` environment only. | When multi-environment deployment is approved |
| Blue/green deployments | Not in scope. Current EC2 + Docker Compose architecture does not support it. | When infra architecture changes |
| ECS / Fargate / EKS migration | Not in scope. EC2 + Docker Compose is the current and planned target. | When container orchestration is evaluated |
| Dependabot configuration | Listed in previous CI/CD plan but deferred here. | When dependency automation is prioritized |

## Task Dependencies

```
T1 (branch protection + repo settings)      # Git/UI config — can run first
  |
  +---> T2 (CI required status gate)          # Depends on T1 settings; modifies ci.yml
  |
  +---> T3 (OIDC/IAM design + Oracle review)  # Design doc, no code changes
         |
         +---> T4 (OIDC/IAM implementation)   # Depends on T3 approval
         |         |
         |         +---> T5 (SSM deploy path) # Depends on OIDC IAM role having SSM permissions
         |
         +---> T6 (SHA-tag image build/push)  # Depends on T3 approval; modifies deploy.yml
         |
         +---> T7 (separate infra workflow)   # Depends on T3 approval; new file
```

**Task 7 can run in parallel with T4/T5/T6 after T3 approval.** Tasks T4, T5, T6 all modify `deploy.yml` together in practice — plan them as a single merge.

---

### Task 1: Configure branch protection and repository settings

Establish the GitHub-side controls that enforce the trunk-based workflow. Some of these require GitHub UI / Settings access and cannot be achieved by repository files alone.

**Files:**
- No repository files to create (settings are GitHub UI)
- Update `docs/functional-map.md` §0.8 (Commit Discipline) to reference the workflow
- Update `docs/aws-guide.md` (if relevant to deploy procedure)

**Steps:**
1. Navigate to Settings → Branches → Branch protection rules → Add rule for `main`.
2. Enable:
   - Require a pull request before merging
   - Dismiss stale pull request approvals when new commits are pushed
   - Require status checks to pass before merging (add the CI status check name from T2)
   - Require branches to be up to date before merging
   - Do not allow bypassing the above settings
   - Restrict pushes that create matching branches (only admins)
   - Require linear history (no merge commits — enforces squash or rebase)
3. Navigate to Settings → Actions → General → Fork pull request workflows: select "Require approval for first-time contributors"
4. Navigate to Settings → Environments → New environment: `production`
   - Required reviewers (add at least one)
   - Wait timer (optional, e.g. 5 minutes)
   - Deployment branches: `main` only

**Expected evidence:**
- Branch protection rule visible in UI with all checkboxes set
- `production` environment listed in Settings → Environments with deployment branch set to `main`

**Rollback/safety:** All settings are revertible via the same UI. No code change to revert.

**Commit:** Settings-only — no commit for this task itself.

**Acknowledge — cannot achieve by repository files alone:**
- `Branch protection rules` — Settings UI only
- `Environment creation and protection rules` — Settings UI only (`.github/environments/` is not used in this repo)
- `OIDC provider thumbprint` — AWS IAM console or CDK (covered in T3/T4)
- `EC2 security group ingress rules` — CDK or AWS console (covered in T5)

---

### Task 2: Add CI status check gate

Make the CI workflow provide a status check name that branch protection can require. The current CI workflow produces per-matrix-job checks (validate, test-hermandad-service, etc.). Branch protection needs a single check name or all checks.

**Option A (recommended, minimal):** Add a `ci-success` job that `needs:` the validate + test matrix jobs and passes only when all succeed. Branch protection can require `ci-success` as a single check.

**Files:**
- Modify: `.github/workflows/ci.yml`

**Steps:**
1. Add job:
   ```yaml
   ci-success:
     needs: [validate, test]
     if: always()
     runs-on: ubuntu-latest
     steps:
       - name: Check matrix status
         if: contains(join(needs.*.result, ','), 'failure') || contains(join(needs.*.result, ','), 'cancelled')
         run: exit 1
       - run: echo "All CI checks passed"
   ```
2. Configure branch protection rule (Settings UI) to require `ci-success`.
3. Push to non-main branch, create PR, verify the status check appears and blocks merge.

**Expected evidence:**
- `ci-success` job appears in CI runs
- PR on `main` shows required status check `ci-success`, blocks merge when CI fails

**Rollback/safety:** Remove the `ci-success` job from `ci.yml` and uncheck in branch protection. No resource impact.

**Commit example:** `chore(ci): add ci-success aggregation job for branch protection status gate`

---

### Task 3: Design GitHub OIDC IAM role (design doc, no code — requires Oracle review)

Design the IAM trust policy and permissions for GitHub OIDC before writing any infrastructure or workflow code. The OIDC role must replace the static IAM user currently used by `deploy.yml`.

**Deliverable:** Short design decision record (add to `docs/architecture.md` or create `docs/plans/oidc-iam-design.md`).

**Design parameters:**
- GitHub organization: `emilio-code7`
- Repository: `semana-santa-app`
- Allowed branch: `main` (deploy workflow only)
- OIDC provider URL: `token.actions.githubusercontent.com`
- Audience: `sts.amazonaws.com`
- Required IAM permissions:
  - `ecr:GetAuthorizationToken`, `ecr:BatchCheckLayerAvailability`, `ecr:InitiateLayerUpload`, `ecr:UploadLayerPart`, `ecr:CompleteLayerUpload`, `ecr:PutImage` — on all 3 ECR repos (`repertorio/hermandad-service`, `repertorio/procesion-service`, `repertorio/repertorio-service`)
  - `ssm:SendCommand`, `ssm:ListCommands`, `ssm:ListCommandInvocations` — on the EC2 instance (target by instance ID or tag `Stack=RepertorioInfraStack`)
  - `ssm:DescribeInstanceInformation` — discovery
  - `ec2:DescribeInstances` — resolve instance ID from tags if needed

**Role split (requires Oracle review):**

| Role | Trust | Permissions | Used by |
|------|-------|-------------|---------|
| **App Deploy Role** (`RepertorioGitHubAppDeployRole`) | `repo:emilio-code7/semana-santa-app:ref:refs/heads/main` | `ecr:*` on 3 ECR repos + `ssm:SendCommand`/`ListCommands`/`ListCommandInvocations` + `ssm:DescribeInstanceInformation` + `ec2:DescribeInstances` | `deploy.yml` (T4/T5/T6) |
| **Infra Deploy Role** (`RepertorioGitHubInfraDeployRole`) | `repo:emilio-code7/semana-santa-app:ref:refs/heads/main` | `cloudformation:*`, `iam:*` on stack-managed resources, `ecr:*`, `ssm:*`, `ec2:*`, `sqs:*`, cognito read — scoped to the `RepertorioInfraStack` tag or resource ARNs | `infra-deploy.yml` (T7) |

**Explicitly excluded from App Deploy Role:** CloudFormation (`cloudformation:*`), SQS data plane (`sqs:SendMessage`, `sqs:ReceiveMessage`), Cognito admin — these remain on the EC2 instance profile and the separate infra deploy role.

**Oracle review:** Present the design parameters and IAM policy document for **both** roles. Do not proceed to T4/T5/T6/T7 without Oracle approval.

**Commit example:** `docs: add OIDC IAM role design record` (if stored as architecture doc)

---

### Task 4: Implement GitHub OIDC IAM role (CDK)

Add the GitHub OIDC IAM role for the **application** deploy workflow to the CDK stack — a deploy-only role, not attached to EC2. The role trusts the GitHub OIDC provider and carries the ECR push + SSM permissions designed in T3 for the App Deploy Role. The Infra Deploy Role is implemented in T7.

**Files:**
- Modify: `infrastructure/aws/lib/stack.ts`
- Modify: `.github/workflows/deploy.yml` — use `aws-actions/configure-aws-credentials@v4` with `role-to-assume`

**Steps:**
1. Add OIDC provider (if not already present):
   ```typescript
   const githubOidc = new iam.OpenIdConnectProvider(this, 'GitHubOidc', {
     url: 'https://token.actions.githubusercontent.com',
     clientIds: ['sts.amazonaws.com'],
   });
   ```
2. Add the **App Deploy Role** (`RepertorioGitHubAppDeployRole`) with trust policy scoped to `repo:emilio-code7/semana-santa-app:ref:refs/heads/main`.
3. Attach ECR push + SSM send-command permissions (reuse existing ECR repo references).
4. `npm run synth` — verify stack compiles.
5. `cdk diff RepertorioInfraStack --no-change-set` — verify only the OIDC provider + role are added, no existing resource replaced or deleted.
6. Present diff to Oracle for review before deploy.
7. Do not deploy `cdk deploy` — mark as approved but await user approval.
8. In `deploy.yml`, replace static credential steps:
   ```yaml
   - name: Configure AWS credentials (OIDC)
     uses: aws-actions/configure-aws-credentials@v4
     with:
        role-to-assume: arn:aws:iam::${{ secrets.AWS_ACCOUNT_ID }}:role/RepertorioGitHubAppDeployRole
       aws-region: ${{ secrets.AWS_REGION }}
   ```
9. Remove `AWS_ACCESS_KEY_ID` and `AWS_SECRET_ACCESS_KEY` from repo secrets after confirming OIDC works.

**Expected evidence:**
- `cdk synth` succeeds
- `cdk diff` shows only additive changes (OIDC provider + role + policy)
- Oracle review approved
- `deploy.yml` references `role-to-assume` and no longer references static access key secrets

**Rollback/safety:**
- If CDK changes fail: `git checkout main -- infrastructure/aws/lib/stack.ts`
- If IAM misconfigured: deploy workflow fails at `configure-aws-credentials` step — no production impact
- Transition safely: keep both OIDC and static key paths with conditional logic

**Commit example:** `feat(infra): add GitHub OIDC IAM role for deploy workflow`

---

### Task 5: Replace SSH deploy with SSM Run Command

Replace `appleboy/ssh-action` with `aws ssm send-command` so EC2 access is audited, keyless, and managed through IAM.

**Files:**
- Modify: `.github/workflows/deploy.yml` — replace the "Deploy to EC2" step
- Modify: `infrastructure/aws/lib/stack.ts` — close port 22 (remove SSH ingress rule)

**Steps:**
1. Ensure SSM permissions on the OIDC role (designed in T3, implemented in T4).
2. Determine EC2 instance ID — add CDK output `Ec2InstanceId` using `ec2Instance.instanceId`.
3. Replace SSH deploy step:
    ```yaml
    - name: Deploy via SSM
      run: |
        COMMAND_ID=$(aws ssm send-command \
          --instance-ids "${{ vars.EC2_INSTANCE_ID }}" \
          --document-name "AWS-RunShellScript" \
          --parameters 'commands=[
            "aws ecr get-login-password --region ${{ secrets.AWS_REGION }} | docker login --username AWS --password-stdin ${{ vars.ECR_REGISTRY }}",
            "cd /opt/repertorio",
            "DEPLOY_TAG=${{ github.sha }} docker compose -f docker-compose.aws.yml pull",
            "DEPLOY_TAG=${{ github.sha }} docker compose -f docker-compose.aws.yml up -d --remove-orphans"
          ]' \
          --output text \
          --query 'Command.CommandId')
        echo "SSM command $COMMAND_ID submitted"
    ```
4. Add polling loop with timeout (e.g. 30 iterations at 10s intervals, checking `list-command-invocations` status).
5. Remove the `appleboy/ssh-action` step and the `EC2_SSH_KEY` / `EC2_HOST` secrets. Keep old secrets until OIDC+SSM verification is confirmed working.
6. Remove SSH ingress rule from CDK; run `cdk synth` + `cdk diff`. Do not deploy without user approval.
7. Update health check to resolve IP via `aws ec2 describe-instances` or use the instance's private DNS through SSM.

**Expected evidence:**
- SSM command successfully deploys containers to EC2 (production-safe connectivity check only — see note below)
- Health check passes through resolved IP
- Port 22 removed from security group after CDK deploy
- `EC2_SSH_KEY` and `EC2_HOST` secrets removed only after confirmed OIDC+SSM verification

> **Note on production verification:** No staging environment exists. Before the first real deployment, run a separately approved production-safe SSM no-op/read-only command as a connectivity check (e.g. `aws ssm send-command` with `uptime` or `docker ps`). Once connectivity is confirmed, the first real deployment requires explicit approval. Never run `--dry-run` on a real deployment without this prior connectivity verification.

**Rollback/safety:**
- If SSM fails, deploy workflow fails before restart — previous containers remain running
- Port 22 removal is a CDK change requiring user-approved deploy. If SSH debug access is needed, the ingress rule must be added back through the IaC review process (not by skipping IaC)

**Commit example:** `feat(infra): replace SSH deploy with SSM Run Command; close port 22`

---

### Task 6: Immutable SHA image tags — remove `:latest`

Every Docker image tag must be the `${{ github.sha }}` commit hash. Remove the `:latest` dual-tag pattern. The deploy Compose file references a specific SHA tag.

**Files:**
- Modify: `.github/workflows/deploy.yml` — build and push steps
- Modify: `docker-compose.aws.yml` — require `${DEPLOY_TAG:?DEPLOY_TAG is required}`

**Steps:**
1. In `deploy.yml`, change each build step to single-tag:
    ```yaml
    docker build -t $REGISTRY/repertorio/hermandad-service:${{ github.sha }} ./services/hermandad-service
    docker push $REGISTRY/repertorio/hermandad-service:${{ github.sha }}
    ```
2. Modify `docker-compose.aws.yml` image references to require an explicit tag:
    ```yaml
    image: ${ECR_BASE}/repertorio/hermandad-service:${DEPLOY_TAG:?DEPLOY_TAG is required}
    ```
3. Pass `DEPLOY_TAG=${{ github.sha }}` in the SSM command environment (Task 5). The compose `pull` and `up` commands must include:
    ```bash
    DEPLOY_TAG=${{ github.sha }} docker compose -f docker-compose.aws.yml pull
    DEPLOY_TAG=${{ github.sha }} docker compose -f docker-compose.aws.yml up -d --remove-orphans
    ```
4. Verify SHA-tagged pushes appear in ECR after deploy.

**Expected evidence:**
- ECR repositories show images with SHA tags
- `docker-compose.aws.yml` uses `${DEPLOY_TAG:?DEPLOY_TAG is required}` — fails fast if tag is missing
- Deploy workflow logs show SHA-tagged push
- Old `:latest` tags from previous deploys remain in ECR (not deleted by this change); only new workflow runs stop overwriting them

**Rollback/safety:** Redeploy the last known-good SHA through the same approved workflow. Do not fall back to a mutable tag.

**Commit example:** `chore(ci): use immutable SHA image tags; remove latest from deploy`

---

### Task 7: Create separate manual infrastructure deployment workflow

Create a new workflow for CDK deployments that is manually triggered and requires environment approval. Decouples infrastructure changes from application deploys.

**Files:**
- Create: `.github/workflows/infra-deploy.yml`

**Workflow design:**
```yaml
name: Infrastructure Deploy (CDK)
on:
  workflow_dispatch:
    inputs:
      diff-only:
        description: 'Run cdk diff only (no deploy)'
        required: true
        default: 'true'
        type: choice
        options:
          - 'true'
          - 'false'

jobs:
  synth-diff:
    runs-on: ubuntu-latest
    environment: production
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: '20'
      - run: npm ci
        working-directory: infrastructure/aws
      - name: Configure AWS credentials (OIDC) — infra role
        uses: aws-actions/configure-aws-credentials@v4
        with:
          role-to-assume: arn:aws:iam::${{ secrets.AWS_ACCOUNT_ID }}:role/RepertorioGitHubInfraDeployRole
          aws-region: ${{ secrets.AWS_REGION }}
      - run: npx cdk synth
        working-directory: infrastructure/aws
      - run: npx cdk diff RepertorioInfraStack --no-change-set
        working-directory: infrastructure/aws

  deploy:
    needs: synth-diff
    if: inputs.diff-only == 'false'
    runs-on: ubuntu-latest
    environment: production
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
      - run: npm ci
        working-directory: infrastructure/aws
      - name: Configure AWS credentials (OIDC) — infra role
        uses: aws-actions/configure-aws-credentials@v4
        with:
          role-to-assume: arn:aws:iam::${{ secrets.AWS_ACCOUNT_ID }}:role/RepertorioGitHubInfraDeployRole
          aws-region: ${{ secrets.AWS_REGION }}
      - run: npx cdk synth
        working-directory: infrastructure/aws
      - run: npx cdk deploy RepertorioInfraStack --require-approval never
        working-directory: infrastructure/aws
```

**Key rules enforced:**
- Default `diff-only: 'true'` — deploy is opt-in, never accidental
- `environment: production` — environment protection rules pause deploy for manual gate
- CDK diff output visible in workflow log before deploy proceeds
- **Uses `RepertorioGitHubInfraDeployRole`** — a separate role with CloudFormation/IAM/CDK permissions (designed in T3 with Oracle review). Must NOT reuse the application deploy role (`RepertorioGitHubAppDeployRole`). Both roles require Oracle review before implementation.

**Expected evidence:**
- `infra-deploy.yml` exists in `.github/workflows/`
- `cdk synth` succeeds when workflow triggered
- `cdk diff` output visible in workflow logs
- Deploy blocked when `diff-only: 'true'`

**Rollback/safety:** `workflow_dispatch` means no automated run. Bad CDK deploy must be rolled back via the same workflow or AWS console. Consider `cdk rollback` for future iterations.

**Commit example:** `chore(ci): add separate manual CDK infrastructure deploy workflow`

---

### Task 8: Update operational docs

Reflect the new deploy architecture, removed SSH path, and workflow responsibilities.

**Files:**
- Modify: `docs/aws-guide.md`
  - Add section on OIDC deploy role and how to assume it
  - Replace SSH deploy instructions with SSM Run Command
  - Add infra-deploy workflow section (how to trigger, what it does)
  - Remove references to static AWS access keys and `deploy-aws.sh` if still documented
  - Document branch protection rules affecting deploy
- Modify: `docs/functional-map.md` §0.8 if workflow references change

**Expected evidence:**
- `docs/aws-guide.md` updated with OIDC, SSM, infra-workflow sections
- No remaining references to SSH keys, static AWS credentials, or `appleboy/ssh-action`
- Branch protection rules documented

**Commit example:** `docs: update AWS guide for OIDC, SSM deploy, and separate infra workflow`

---

### Task 9: Final verification and dry-run

Run the full verification gate before any production deployment. No deploy is performed without explicit approval.

**Steps:**
1. `cd infrastructure/aws && npm run synth` — CDK template compiles (use `npx cdk synth --strict` for strict validation)
2. `npx cdk diff RepertorioInfraStack --no-change-set --strict` — all changes additive, no replacements. Read-only; **do not deploy**.
3. Workflow syntax validation — options (keep dependency additions deliberate):
   - **If `actionlint` is already adopted/pinned in the repository:** `actionlint .github/workflows/*.yml`
   - **Otherwise:** rely on GitHub Actions parse/run evidence — push the branch and let GitHub Actions parse the workflows; a parse error fails the workflow before any step runs
   - Do not install `actionlint` or Python `pyyaml` solely for this step unless explicitly instructed
4. `git diff --check` — no whitespace errors
5. Search modified files for `FIXME`, `TODO`, `HACK`, static AWS keys, IPs, or credentials
6. Confirm `docs/openapi.yaml` is unchanged (no API changes in this plan)
7. Confirm `docs/functional-map.md` matches current workflow topology
8. Present final status summary including CDK diff, workflow changes, and remaining GitHub Settings steps

**Rollback/safety:** All changes are in documentation and workflow files. No production resources are touched. CDK diff is reviewed but not deployed.

**Commit example:** `docs: finalize CI/CD standardization plan with verification evidence`
