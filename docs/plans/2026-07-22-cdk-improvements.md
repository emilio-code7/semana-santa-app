# CDK Improvements Plan (historical/superseded)

> **Goal:** Harden the AWS CDK infrastructure after initial migration — security, reliability, maintainability.
> **Status:** Historical and superseded by the 2026-07-23 consolidation plan. Any `us-east-1` reference below is stale and must not be used for deployment; the deployed region is `eu-south-2`.

---

## Current Architecture

```
RepertorioInfraStack (one monolithic stack)
├── VPC (default, imported)
├── Security Groups (EC2 + RDS)
├── SQS (3 queues + 3 DLQs)
├── ECR (3 repositories)
├── IAM (EC2 role)
├── Cognito (User Pool + Client + Domain + Lambda trigger)
├── RDS (PostgreSQL 16, t3.micro, 20GB)
├── KeyPair (repertorio-deploy)
└── EC2 (t3.small, Amazon Linux 2023, 20GB)
```

---

## Proposed Improvements

Grouped by priority and impact.

### 🔴 Critical

#### C1: Separate stateful vs stateless stacks

**Problem:** RDS, SQS, Cognito, and ECR live in the same stack as EC2. A change that replaces EC2 (e.g., instance type, AMI update) risks touching stateful resources. CloudFormation stack operations are all-or-nothing — a failed EC2 update could lock the whole stack including the database.

**Solution:** Split into two stacks with cross-stack references:

```
RepertorioStatefulStack (deletionProtection: true)
├── RDS (database)
├── SQS (3 queues + 3 DLQs)
├── Cognito (User Pool + Client + Domain + Lambda)
└── ECR (3 repositories)

RepertorioComputeStack (can destroy/recreate freely)
├── Security Groups
├── IAM Role
├── KeyPair
└── EC2 instance
```

The compute stack receives resource ARNs from the stateful stack via stack props (CDK auto-generates CloudFormation exports/imports).

**Effort:** ~2h. Requires `cdk deploy` twice (stateful first, then compute).

**Risk:** Cross-stack references must not create circular dependencies. Read-only references (EC2 consumes SQS queue ARNs, RDS endpoint) are safe because the consumer doesn't block the producer.

---

#### C2: Enable RDS backup retention

**Problem:** `backupRetention: Duration.days(0)` — no automated backups. A DB crash, accidental delete, or data corruption means total data loss.

**Solution:**
```typescript
backupRetention: cdk.Duration.days(7),
preferredBackupWindow: '02:00-03:00',
```

**Cost:** ~$0.095/GB/month for snapshot storage (20GB → ~$2/month). Negligible for the safety.

**Effort:** 10 min. Requires a single `cdk deploy` — but the first backup takes time.

---

#### C3: Restrict SSH to known IPs

**Problem:** `sgEc2.addIngressRule(ec2.Peer.anyIpv4(), ec2.Port.tcp(22))` — SSH open to the entire internet. Bots probe on port 22 constantly.

**Solution:** Replace with the deployer's IP:
```typescript
sgEc2.addIngressRule(
  ec2.Peer.ipv4('XX.XX.XX.XX/32'),  // deployer's home/office IP
  ec2.Port.tcp(22),
  'SSH from trusted IP'
);
```

If multiple people deploy, create an `allowedSshIps` parameter and use `ec2.Peer.ipv4()` for each.

**Effort:** 10 min.

**Note:** Only do this *after* confirming SSM Session Manager works (it doesn't need SSH at all). The IAM role already has `AmazonSSMManagedInstanceCore` — you can connect via AWS Console → EC2 → Instance → Connect → Session Manager. SSH via port 22 is redundant once that's tested.

---

#### C4: Add stack termination protection

**Problem:** A single `cdk destroy RepertorioInfraStack` destroys everything — database, queues, users — with no confirmation beyond the CLI prompt.

**Solution:**
```typescript
new RepertorioInfraStack(app, 'RepertorioInfraStack', {
  terminationProtection: true,
  // ...
});
```

After this, `cdk destroy` is rejected. To destroy, you must first disable termination protection via AWS Console or CLI — a deliberate two-step process.

**Effort:** 5 min.

---

### 🟡 Medium Priority

#### M1: Remove orphan CfnInstanceProfile

**Problem:** The stack creates a `iam.CfnInstanceProfile` explicitly but `ec2.Instance` with a `role` prop creates an instance profile automatically. Results in two instance profiles, one unused.

**Solution:** Delete the `CfnInstanceProfile` block. The `ec2.Instance` handles it.

**Effort:** 5 min.

---

#### M2: Remove explicit queue names (or keep — trade-off documented)

**Problem:** `queueName: 'hermandad-events'` means if the stack is deleted and re-created, CloudFormation fails because the queue name already exists (AWS queues have a global name uniqueness constraint within a region). Explicit names also prevent parallel stacks (dev/staging/prod) in the same account.

**Options:**
- **Remove queueName** → CDK generates names like `RepertorioInfraStack-Queuehermandadevents-1A2B3C4D`. More portable, but the deploy script must discover them dynamically.
- **Keep queueName but document the constraint** — acceptable for a single dev environment.

**Decision needed:** Do we ever want dev/staging stacks alongside production?

**Effort:** 15 min if removing + updating deploy script.

---

#### M3: Commit cdk.context.json

**Problem:** CDK queries AWS at synth time for VPC lookups and caches results in `cdk.context.json`. If it's not committed, every `cdk synth` on a new machine re-queries — and could get different results (e.g., different default VPC CIDR).

**Solution:** `git add infrastructure/aws/cdk.context.json && git commit`.

**Effort:** 1 min.

---

#### M4: Add cdk-nag for compliance scanning

**Problem:** No automated security checks on the CDK code. Security anti-patterns (open SSH, no backups, public RDS) are caught only by human review.

**Solution:**
```bash
npm install --save-dev cdk-nag
```

```typescript
import { AwsSolutionsChecks } from 'cdk-nag';
cdk.Aspects.of(app).add(new AwsSolutionsChecks());
```

Run: `cdk synth` — cdk-nag emits warnings for rule violations. Add `NagSuppressions` with documented reasons where violations are intentional (e.g., SSH open to internet is a suppressions with reason "dev only").

**Effort:** 30 min.

---

#### M5: Make region required, not defaulted to a stale region

**Problem:** A stale default region could deploy resources far from their users.

**Solution:**
```typescript
region: process.env.CDK_DEFAULT_REGION  // no fallback
```

If the env var isn't set, `cdk synth` fails with a clear error. The deployer must explicitly configure their region.

**Effort:** 5 min.

---

#### M6: Register application in AWS AppRegistry ("My Applications" tab)

**Problem:** The stack deploys all infrastructure (EC2, RDS, SQS, Cognito, ECR) but nothing appears in the AWS Console's **"My Applications"** tab. That tab shows **AppRegistry** applications, which require explicit registration.

**Solution:** Add `ApplicationAssociator` in `bin/app.ts`:

```bash
npm install @aws-cdk/aws-servicecatalogappregistry-alpha
```

```typescript
// bin/app.ts
import { ApplicationAssociator, TargetApplication }
  from '@aws-cdk/aws-servicecatalogappregistry-alpha';

const app = new cdk.App();

// Register app FIRST — it auto-associates all stacks in this scope
new ApplicationAssociator(app, 'RepertorioApplication', {
  applications: [TargetApplication.createApplicationStack({
    applicationName: 'Repertorio',
    applicationDescription: 'Semana Santa management system — 3 microservices',
  })],
});

new RepertorioInfraStack(app, 'RepertorioInfraStack', { ... });
```

After `cdk deploy`, the application and all its resources appear under **My Applications** with topology view, cost tracking, and operations dashboards.

**Effort:** 15 min.

---

### 🟢 Nice-to-have

#### N1: EC2 health check alarm

Add a CloudWatch alarm that triggers if the EC2 instance stops responding:
```typescript
new cloudwatch.Alarm(this, 'Ec2HealthAlarm', {
  metric: ec2Instance.metricStatusCheckFailed(),
  threshold: 1,
  evaluationPeriods: 2,
  alarmDescription: 'EC2 instance health check failed',
});
```

**Effort:** 15 min.

---

#### N2: Parameterize environment

Instead of hardcoding region and account, use an `env` parameter so the same CDK code can deploy to dev/staging/prod:
```typescript
interface StackProps extends cdk.StackProps {
  environment: 'dev' | 'staging' | 'prod';
}
```

Then conditionally set instance sizes, backup retention, etc. based on environment.

**Effort:** 1h.

---

## Decision Matrix

| Change | Risk | Effort | Value | Do first? |
|--------|------|--------|-------|-----------|
| C1: Split stacks | Medium (requires re-deploy) | 2h | High (safety) | ✅ After region decision |
| C2: RDS backup | Low | 10min | High (data safety) | ✅ |
| C3: Restrict SSH | Low | 10min | Medium (security) | ✅ After SSM verified |
| C4: Termination protection | Low | 5min | High (accident prevention) | ✅ |
| M1: Remove orphan CfnProfile | Low | 5min | Low (tidiness) | ✅ Anytime |
| M2: Queue names | Low | 15min | Low | 🟡 Defer |
| M3: Commit cdk.context.json | Low | 1min | Medium (reproducibility) | ✅ |
| M4: cdk-nag | Low | 30min | Medium (automated checks) | 🟡 After critical |
| M5: Region env var | Low | 5min | Low (documentation) | 🟡 Tied to region decision |
| M6: AppRegistry registration | Low | 15min | Medium (visibility) | ✅ Quick win |
| N1: Health alarm | Low | 15min | Low | 🟡 Defer |
| N2: Environment parameterization | Medium | 1h | Medium | ❌ Not yet needed |

---

## Prerequisites

Before implementing any changes:

1. **Decide on the target region** — this historical decision is superseded; use the deployed `eu-south-2` region.
   - If switching: deploy new stack in new region, test, then destroy old stack
   - If staying: implement changes in-place on the current stack
2. **Test SSM Session Manager** — if it works, SSH port 22 becomes optional (C3 becomes lower priority)
3. **Confirm no production data yet** — if this is still dev, we can be more aggressive with changes
