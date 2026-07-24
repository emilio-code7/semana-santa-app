# AWS Migration Guide

> A practical guide to the AWS infrastructure powering this project — what we built, why, and how to test it.
>
> **Region:** eu-south-2 (Madrid) — chosen for latency from Spain.

---

## Table of Contents

1. [What did we build?](#1-what-did-we-build)
2. [Architecture overview](#2-architecture-overview)
3. [AWS services explained](#3-aws-services-explained)
4. [How to test locally](#4-how-to-test-locally)
5. [How to deploy for real](#5-how-to-deploy-for-real)
6. [FAQ](#6-faq)

---

## 1. What did we build?

We replaced the **self-managed local infrastructure** (Kafka, Keycloak, container Postgres) with **AWS managed services** so the project can run in the cloud without requiring Docker containers for everything.

### Before (local dev)

```
Your laptop
├── Docker Compose with 15 containers
├── Keycloak (auth — self-managed)
├── Kafka + Zookeeper (messaging — self-managed)
├── 5x PostgreSQL (databases — self-managed)
├── Redis (caching — self-managed)
├── Eureka (service discovery — self-managed)
└── API Gateway (routing — self-managed)
```

### After (AWS)

```
AWS Cloud
├── EC2 (1 virtual server — runs our 3 services + nginx)
├── RDS (managed PostgreSQL — no container DB)
├── SQS (managed messaging — no Kafka)
├── Cognito (managed auth — no Keycloak)
└── ECR (stores our Docker images)
```

---

## 2. Architecture overview

### Infrastructure diagram

```
Internet ──► EC2 (t3.small, single server)
                 │
            ┌──── nginx (port 80)
            │       │
            │  ┌────┴──────────┐────┐
            │  │               │    │
            ▼  ▼               ▼    ▼
       Hermandad  Procesion  Repertorio  (3 Spring Boot services)
       :8081      :8082      :8083
            │
             ├──► RDS (PostgreSQL — single instance, 3 databases)
            │
             ├──► SQS (4 queues — replaces Kafka topics)
            │
            └──► Cognito (user pool — replaces Keycloak)
```

### Before vs After — service by service

| Component | Local dev | AWS (profile=aws) |
|-----------|-----------|-------------------|
| **Database** | Container PostgreSQL | RDS PostgreSQL (managed) |
| **Messaging** | Kafka + Zookeeper | SQS (4 queues: hermandad-events, hermandad-member-events, procesion-events, marcha-events) |
| **Auth (JWT)** | Keycloak | Cognito User Pool |
| **Admin Auth** | Keycloak admin client | Cognito SDK |
| **Service Discovery** | Eureka | Removed (nginx static routes) |
| **API Gateway** | Spring Cloud Gateway | nginx (on EC2) |
| **Caching** | Redis | Removed (not in free tier) |

### Code architecture (hexagonal)

We didn't replace Kafka. We added SQS **alongside** it using a port/adapter pattern:

```
MessageSender (port — shared/common)
    ├── KafkaMessageSender (@Profile("!aws"))   ← default, local dev
    └── SqsMessageSender  (@Profile("aws"))     ← deployed, cloud
```

Same pattern for consumers:

```
Event Consumers
    ├── @KafkaListener classes (@Profile("!aws"))   ← default
    └── @SqsListener classes   (@Profile("aws"))     ← cloud
```

This means:
- `./gradlew bootRun` → uses Kafka (works offline)
- Deployed on EC2 with `SPRING_PROFILES_ACTIVE=aws` → uses SQS
- No code changes — just a profile switch

---

## 3. AWS services explained

### EC2 (Elastic Compute Cloud)

**What it is:** A virtual server in the cloud. Like renting a computer that runs 24/7.

**What we use:** A `t3.small` instance (2 vCPU, 2GB RAM) running Amazon Linux 2023. It hosts our 3 Spring Boot services + nginx inside Docker containers.

**Cost note:** t3.small is the deployed size. It may incur charges depending on the account's credits and free-tier eligibility; check current pricing and budget before making changes.

**Key concept: Security Groups.** These are virtual firewalls. We have:
- Port 80 open to anyone (HTTP)
- Port 22 open to anyone (SSH — **should be locked to your IP**)

### RDS (Relational Database Service)

**What it is:** Managed PostgreSQL. AWS handles backups, patching, and replication.

**What we use:** A single `db.t3.micro` instance with 20GB storage. It hosts 3 databases (hermandad_db, procesion_db, repertorio_db). Backup retention is 1 day in the deployed stack.

**Database creation:**
- `hermandad_db` — created by CDK's `databaseName` prop at stack creation time
- `procesion_db`, `repertorio_db` — created through the approved database bootstrap procedure

**Why not container Postgres?** RDS is more reliable, automatically backs up, and if it fails AWS replaces it. A container Postgres loses data when the container dies.

**Cost:** Eligibility depends on the AWS account and current offers. Check pricing, credits, and the budget before changing the instance or storage.

### SQS (Simple Queue Service)

**What it is:** Managed message queues. Services send/receive messages without needing Kafka.

**Target architecture:** 4 queues (hermandad-events, hermandad-member-events, procesion-events, marcha-events) plus 4 dead-letter queues for failed messages.

| Queue | Producer | Consumer | Purpose |
|-------|----------|----------|---------|
| `hermandad-events` | hermandad-service | producer-only (no SQS consumer exists) | Created/modified brotherhoods |
| `hermandad-member-events` | hermandad-service | producer-only (no SQS consumer exists) | Member role changes |
| `procesion-events` | procesion-service | repertorio-service (SqsListener, local cache) | Procesion lifecycle events |
| `marcha-events` | repertorio-service | producer-only (future consumer planned) | Marcha catalog events |

**Currently deployed:** 3 queues (`hermandad-events`, `hermandad-member-events`, `procesion-events`); `marcha-events` will be provisioned in the next CDK deployment.

> **`cruceta-events` not provisioned**: Cruceta outbox rows (aggregate type `cruceta`) resolve to destination `cruceta-events`, but no SQS queue exists for this name. Under the AWS profile, the outbox poller's `SqsMessageSender.send("cruceta-events", payload)` will fail, leaving those rows `processed = false` (pending/retried each poll cycle). A separately reviewed queue/consumer decision is required before provisioning this queue.

**SQS vs Kafka:**
| Kafka | SQS |
|-------|-----|
| You manage the servers | AWS manages everything |
| Messages have order | No ordering guarantee |
| You pay for servers | You pay per request (1M free/mo) |
| Harder to set up | 5 lines of code |

### Cognito (Amazon Cognito)

**What it is:** Managed user authentication. Like Keycloak but AWS-native.

**What we use:** A user pool that issues JWT tokens. A pre-token generation Lambda function injects our custom `hermandad_memberships` claim into tokens (same format as Keycloak).

**Key concept: Pre-token generation Lambda.** Cognito doesn't include custom claims in JWT access tokens by default. We have a Lambda function that runs right before a token is issued and injects our custom claim by reading the user's group memberships.

### ECR (Elastic Container Registry)

**What it is:** Docker image storage. Like Docker Hub but in AWS.

**What we use:** 3 repositories (one per service). We build Docker images locally, push to ECR, and the EC2 pulls them on startup.

---

## AWS credentials for local setup

Use short-term credentials from your existing IAM Identity Center workflow, or use
`aws login` where supported (AWS CLI >=2.32). Deploy with a scoped role and
permissions approved for this stack; use ephemeral credentials rather than
persistent credentials for local setup.

---

## 4. How to test locally

### Before deploying to AWS, you can test each piece locally:

**Test the SQS code path (without AWS):**

The SQS adapters are behind `@Profile("aws")`. To verify they compile and don't break anything:

```bash
# Default profile (uses Kafka) — should work normally
./gradlew :services:hermandad-service:test
```

To test the SQS adapters specifically, you'd need either:
1. **LocalStack** — a mock AWS that runs in Docker
2. **Real AWS SQS** — using the actual queues (free tier)

**Test Cognito auth locally:**

You can't fully test Cognito locally without AWS, but you can verify the code compiles:
```bash
# Compile with aws profile to verify all configs load
SPRING_PROFILES_ACTIVE=aws ./gradlew :services:hermandad-service:compileJava
```

**Test the full api-test.sh:**

This script uses Keycloak on localhost, so it tests the default (non-AWS) path:
```bash
./api-tests/test-api.sh
```

### What you CAN test

| What | How | Status |
|------|-----|--------|
| Outbox poller with MessageSender | `OutboxPollerTest` | ✅ Unit tests (success marks processed, failure leaves pending) |
| ProcesionEventProcessor | `ProcesionEventProcessorTest` | ✅ Unit tests (created, status change, duplicate, malformed) |
| AWS YAML property bindings | `AwsYamlConfigTest`, `ProcesionAwsYamlConfigTest`, `RepertorioAwsYamlConfigTest` | ✅ Focused YAML-loading tests (no Spring context) |
| Kafka consumer delegates to processor | `ProcesionEventConsumerTest` | ✅ Unit test (delegates + propagates exceptions) |
| Existing tests still pass | `./gradlew test` | ⚠️ Baseline JPA/WebMvc context failures; focused test tasks pass |
| SQS adapters compile | `./gradlew compileJava` | ✅ Works |
| MessageSender interface | Unit tests | ✅ 2 shared tests (`OutboxPollerTest`) |
| Bruno collection | Bruno app (local) | ✅ Works |
| Full smoke test | `test-api.sh` | ✅ 20/20 |

### What needs AWS to test

| What | Why |
|------|-----|
| SQS send/receive | Needs real SQS queues — or LocalStack |
| Cognito token generation | Needs Cognito user pool |
| RDS connection | Needs RDS instance |
| Full deployed stack | All services running on EC2 |

### Testing locally with LocalStack (future work)

No LocalStack-based integration test exists yet. You can test manually with LocalStack:

```bash
docker run -d --name localstack \
  -p 4566:4566 \
  -e SERVICES=sqs \
  localstack/localstack

# Create queues
aws --endpoint-url=http://localhost:4566 sqs create-queue --queue-name hermandad-events

# Then start service with
SPRING_PROFILES_ACTIVE=aws \
SPRING_CLOUD_AWS_ENDPOINT=http://localhost:4566 \
SPRING_CLOUD_AWS_REGION_STATIC=eu-south-2 \
  ./gradlew :services:hermandad-service:bootRun
```

---

## 5. How to deploy for real

Canonical deployment is currently paused until an approved SSM Session Manager or
Secrets-Manager-safe path exists. Do not use the excluded `scripts/deploy-aws.sh`:
it retrieves secrets and SSH material unsafely. Deployment must be resumed only
after the replacement path has been reviewed and documented.

### Troubleshooting

**SSH key rejected?** Amazon Linux 2023 disables `ssh-rsa`. Use SSM Session
Manager while the approved deployment path is being prepared; do not retrieve
private keys or secret values directly from the command line.

### Prerequisites for deploy

- [ ] CDK stack deployed in `eu-south-2` and all resources exist
- [ ] RDS password is available through the approved runtime secret reference
- [ ] SSM Session Manager or another approved deployment path is available
- [ ] Docker images built and pushed to ECR

---

## Live resources (eu-south-2)

Live identifiers are intentionally omitted from this documentation. Obtain them
from approved runtime configuration or the AWS Console with appropriate access.

| Resource | Sanitized reference |
|---|---|
| **Region** | eu-south-2 (Madrid) |
| **EC2** | `${EC2_PUBLIC_HOST}` (t3.small, Amazon Linux 2023) |
| **EC2 Key Pair** | `${EC2_KEY_PAIR_NAME}` |
| **RDS** | `${RDS_ENDPOINT}:5432` |
| **Cognito Pool** | `${COGNITO_POOL_ID}` |
| **Cognito Client** | `${COGNITO_CLIENT_ID}` |
| **Cognito Domain** | `${COGNITO_DOMAIN}` |
| **SQS queues** | `${SQS_QUEUE_URL_*}` |
| **ECR repositories** | `${ECR_REGISTRY}/repertorio/<service>` |

---

## 6. Known issues & gotchas

### Region: CDK_DEFAULT_REGION always overrides

`CDK_DEFAULT_REGION` is set by the CDK CLI from your AWS CLI default region, so `|| "eu-south-2"` fallbacks in `bin/app.ts` are never reached. Instead, we use `REPERTORIO_AWS_REGION` env var.

When deployment is approved, use the reviewed CDK workflow with
`REPERTORIO_AWS_REGION=eu-south-2`; do not deploy while the canonical path is
paused.

### SSH: Amazon Linux 2023 disables ssh-rsa

Amazon Linux 2023 ships with a system-wide crypto policy that removes `ssh-rsa` from `PubkeyAcceptedAlgorithms`. Only `rsa-sha2-*` and `ed25519` signatures are accepted. The CDK `ec2.KeyPair` generates an RSA key — its private key works with `ssh -o PubkeyAcceptedAlgorithms=+ssh-rsa` or you can deploy an ed25519 key instead.

Use SSM Session Manager rather than retrieving private keys while the approved
deployment path is paused.

### EC2 key pair not associated with instance

The CDK stack creates a `KeyPair` resource but does **not** pass it to the `ec2.Instance` construct (`keyPair:` prop missing). If the instance is replaced (e.g., AMI update, stack delete+recreate), it launches without any SSH key. Fix: add `keyPair: keyPair` to the Instance props in `stack.ts`.

### RDS backup retention: deployed value

The deployed stack uses one day. Changing retention affects recovery capability and potentially cost, so review current RDS pricing and requirements first.

### Security group: SSH open to 0.0.0.0/0

The SG allows SSH from any IP (`0.0.0.0/0`) — should be restricted to specific IPs for production.

### No Cognito users seeded

The user pool and client exist, but there are no users. You need to create users via Cognito console or AWS CLI before any authenticated API calls work.

---

## 7. FAQ

**Q: Will this cost me money?**

A: Charges depend on the account's credits and free-tier eligibility. Check current pricing and budget before deploying or changing EC2 or RDS resources.

**Q: Do I lose my local development setup?**

A: No. `@Profile("!aws")` is the default. Your local Docker Compose still works exactly as before. The AWS code only activates when you set `SPRING_PROFILES_ACTIVE=aws`.

**Q: Why not use ECS Fargate instead of EC2?**

A: Fargate and EC2 have different pricing models. EC2 is the deployed hosting option; compare current pricing and budget for similar capacity before changing it.

**Q: Why keep Kafka if we have SQS?**

A: Dual-transport lets us switch back by changing the profile. Kafka remains the default for local development where SQS isn't available. Once SQS is validated in production, we can remove Kafka.

**Q: What is a CDK? Do I need to learn it?**

A: CDK (Cloud Development Kit) lets you define AWS infrastructure in TypeScript code. It creates CloudFormation templates that AWS deploys. It's like `docker compose` but for AWS resources. We used it to create EC2, RDS, SQS, Cognito, and ECR with one command.

**Q: Can I see the resources in the AWS Console?**

A: Yes. Go to https://console.aws.amazon.com → **CloudFormation → Stacks → RepertorioInfraStack → Resources** to see everything. Or check individual services: EC2, RDS, SQS, Cognito, ECR.
