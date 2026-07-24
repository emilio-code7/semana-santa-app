# AWS Consolidation Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Make `main` the canonical source for the deployed eu-south-2 AWS architecture without regressing the newer shared outbox, persistence, API, or test work already on `main`.

**Architecture:** Recover the deployed CDK source without merging `feature/aws-infra`, then add profile-selected messaging adapters around a shared `MessageSender` port. Kafka and SQS listeners delegate to the same application processor so transport does not change domain behavior. Preserve all deployed CDK logical IDs; infrastructure changes may add resources but must not replace or delete existing stateful resources.

**Tech Stack:** Java 21, Spring Boot 4.1, Spring Kafka, Spring Cloud AWS SQS, AWS SDK v2, AWS CDK v2 TypeScript, JUnit 5, Mockito, Testcontainers/LocalStack.

---

## Baseline and constraints

- Integration branch: `omos/aws-consolidation`
- Integration worktree: `.slim/worktrees/aws-consolidation`
- Deployed-state reference: `feature/aws-infra`
- Live stack: `RepertorioInfraStack`, eu-south-2
- Baseline evidence on 2026-07-23:
  - CloudFormation status `CREATE_COMPLETE`
  - termination protection enabled
  - 29 resources
  - drift status `IN_SYNC`
  - `feature/aws-infra` synth succeeds
  - `cdk diff RepertorioInfraStack --no-change-set` reports zero differences
- Never merge `feature/aws-infra` directly.
- Never deploy while executing this plan. Present `cdk diff` for approval first.
- Do not port `scripts/deploy-aws.sh`; it retrieves secrets and SSH keys unsafely.
- Do not commit `deploy-outputs.json`, account IDs, public IPs, credentials, private keys, generated IDE files, `node_modules`, or `cdk.out`.
- No OpenAPI change is required because HTTP contracts do not change.
- Commit checkpoints require explicit user approval before running `git commit`.
- Task 1 includes required security sanitization of copied scripts and documentation, plus normalization of deployed description values to the literal `?` character; these files need not match the source byte-for-byte.
- Manual QA credential rotation is deferred and was not performed in this task. SSM and secret-handling work remain deferred.
- The brace-expansion audit finding has a package-only follow-up; no application or infrastructure change is included here.

## Acceptance scenarios

```gherkin
Feature: Canonical AWS infrastructure source
  Scenario: Recovered CDK source matches the deployed baseline
    Given the deployed stack is IN_SYNC
    When the recovered source is synthesized and diffed against RepertorioInfraStack
    Then CDK reports no resource differences
    And every deployed logical resource ID remains unchanged

Feature: Profile-selected outbox transport
  Scenario: Local profile publishes through Kafka
    Given a pending outbox event and the aws profile is not active
    When the outbox poller runs
    Then the event is sent to its Kafka topic
    And the row is marked processed only after the send succeeds

  Scenario: AWS profile publishes through SQS
    Given a pending outbox event and the aws profile is active
    When the outbox poller runs
    Then the event is sent to the queue named from its aggregate type
    And the row is marked processed only after the send succeeds

  Scenario: Transport failure preserves the event
    Given the transport send fails
    When completion is observed
    Then the outbox row remains pending

Feature: Transport-neutral procession event processing
  Scenario: Procession creation is consumed from Kafka or SQS
    Given a valid ProcesionCreatedEvent payload
    When either transport listener receives it
    Then the same processor records the known procession
    And the event is recorded as processed once

  Scenario: Procession status change is consumed from Kafka or SQS
    Given a valid ProcesionStatusChangedEvent payload
    When either transport listener receives it
    Then the same processor updates the known procession status

  Scenario: Duplicate event is harmless
    Given the payload has already been processed
    When either listener receives it again
    Then no domain state is written again
    And SQS acknowledges the duplicate

  Scenario: Invalid SQS payload retries
    Given a malformed or incomplete payload
    When the SQS listener receives it
    Then it does not acknowledge the message
    And no processed-event row is saved

Feature: AWS Cognito profile
  Scenario: Membership assignment uses the canonical group name
    Given a member is added to a hermandad under the aws profile
    When membership synchronization runs
    Then the user is assigned to HERMANDAD_<hermandadId>_<role>

  Scenario: AWS region configuration is respected
    Given spring.cloud.aws.region.static is eu-south-2
    When the Cognito client is created
    Then it targets eu-south-2

  Scenario: AWS profile starts without local infrastructure
    Given the aws profile is active
    When each service application context starts
    Then Kafka, Redis, and Keycloak beans are not required

Feature: Missing marcha event queue
  Scenario: CDK adds the missing queue safely
    Given the deployed stack has no marcha-events queue
    When the updated CDK source is diffed
    Then only marcha-events, its DLQ, required grants, and outputs are added
    And no existing resource is replaced or deleted
```

### Task 1: Recover the exact deployed infrastructure baseline

Task 1 acceptance is logical-resource and behavior compatibility, not byte-for-byte
copying: required security sanitization is permitted in scripts and documentation,
and deployed description values use the literal `?` compatibility normalization.

**Files:**
- Copy: `infrastructure/aws/bin/app.ts`
- Copy: `infrastructure/aws/lib/stack.ts`
- Copy: `infrastructure/aws/pre-token-lambda/index.js`
- Copy: `infrastructure/aws/pre-token-lambda/package.json`
- Copy: `infrastructure/aws/cdk.json`
- Copy: `infrastructure/aws/tsconfig.json`
- Copy and edit: `infrastructure/aws/package.json`
- Create: `infrastructure/aws/package-lock.json`
- Copy: `infrastructure/aws/.gitignore`
- Copy: `infrastructure/aws/user-data.sh`
- Copy: `docker-compose.aws.yml`
- Copy: `infrastructure/nginx/nginx.conf`
- Copy: `scripts/smoke-test-aws.sh`
- Copy: `docs/aws-guide.md`
- Copy: `docs/plans/2026-07-22-cdk-improvements.md`

**Steps:**
1. Copy the listed files from `feature/aws-infra`; do not copy service Java code, `deploy-outputs.json`, `scripts/deploy-aws.sh`, or QA credential scripts. Sanitize all copied scripts and documentation as required, without changing infrastructure behavior.
2. Preserve every construct ID, stack ID, stack name, and resource property in `stack.ts` unchanged. Normalize only the deployed description encoding to literal `?` where required for strict diff compatibility.
3. Pin exact versions in `package.json`; add local `aws-cdk` and `ts-node` dev dependencies.
4. Stop ignoring `package-lock.json`; keep `node_modules`, `cdk.out`, generated JavaScript, and local context ignored.
5. Run `npm ci` in `infrastructure/aws`.
6. Run `REPERTORIO_AWS_REGION=eu-south-2 npm run synth`.
7. Run `REPERTORIO_AWS_REGION=eu-south-2 npx cdk diff RepertorioInfraStack --no-change-set --region eu-south-2`.
8. Expected: zero stack differences. If not zero, stop before further work.
9. Run `git diff --check` and inspect `git status --short`.
10. Prepare checkpoint `chore(infra): recover deployed AWS source`; do not commit without approval.

### Task 2: Introduce a tested transport port for the shared outbox

**Files:**
- Create: `shared/common/src/main/java/com/repertorio/common/messaging/MessageSender.java`
- Modify: `shared/common/src/main/java/com/repertorio/common/outbox/OutboxPoller.java`
- Create: `shared/common/src/test/java/com/repertorio/common/outbox/OutboxPollerTest.java`
- Create per service: `adapter/outbound/messaging/KafkaMessageSender.java`
- Create per service: `adapter/outbound/messaging/SqsMessageSender.java`
- Modify: each active service `build.gradle.kts`
- Modify: `gradle/libs.versions.toml`

**Steps:**
1. Write `OutboxPollerTest` proving success marks processed and failure leaves pending.
2. Run `./gradlew :shared:common:test --tests '*OutboxPollerTest'`; expected RED because `MessageSender` wiring does not exist.
3. Add the minimal port:
   ```java
   public interface MessageSender {
       CompletableFuture<Void> send(String destination, String payload);
   }
   ```
4. Replace direct `KafkaTemplate` use in `OutboxPoller` with `MessageSender`; preserve ordering, batch limit, schedule, and asynchronous success/failure semantics.
5. Add profile-selected adapters: Kafka under `!aws`, SQS under `aws`.
6. Run the focused shared and adapter tests; expected GREEN.
7. Run all three service tests to catch bean/profile regressions.
8. Prepare checkpoint `feat(infra): select outbox transport by profile`; do not commit without approval.

### Task 3: Share procession event behavior between Kafka and SQS

**Files:**
- Create: `services/repertorio-service/src/main/java/com/repertorio/marcha/application/event/ProcesionEventProcessor.java`
- Modify: `services/repertorio-service/src/main/java/com/repertorio/marcha/adapter/inbound/kafka/ProcesionEventConsumer.java`
- Create: `services/repertorio-service/src/main/java/com/repertorio/marcha/adapter/inbound/sqs/ProcesionSqsConsumer.java`
- Modify: `services/repertorio-service/src/test/java/com/repertorio/marcha/adapter/inbound/kafka/ProcesionEventConsumerTest.java`
- Create: `services/repertorio-service/src/test/java/com/repertorio/marcha/application/event/ProcesionEventProcessorTest.java`
- Create: `services/repertorio-service/src/test/java/com/repertorio/marcha/adapter/inbound/sqs/ProcesionSqsConsumerTest.java`

**Steps:**
1. Write processor tests for created, status changed, duplicate, malformed, and missing-field payloads.
2. Run the focused tests; expected RED because the processor does not exist.
3. Move parsing, deduplication, domain writes, and processed-event persistence into the processor without changing payload field names (`id`, `hermandadId`, `newStatus`).
4. Make Kafka a thin `!aws` listener that delegates to the processor.
5. Write SQS tests proving success/duplicate acknowledgement and failure no-ack.
6. Add the minimal `aws` listener delegating to the same processor.
7. Run focused tests, then `./gradlew :services:repertorio-service:test`.
8. Prepare checkpoint `feat(repertorio): share procession event processing across transports`; do not commit without approval.

### Task 4: Add and test the AWS Cognito adapters

**Files:**
- Create: `services/hermandad-service/src/main/java/com/repertorio/hermandad/adapter/config/CognitoConfig.java`
- Create: `services/hermandad-service/src/main/java/com/repertorio/hermandad/adapter/outbound/cognito/CognitoMembershipAdapter.java`
- Create: `services/hermandad-service/src/main/java/com/repertorio/hermandad/adapter/outbound/cognito/CognitoUserExistenceAdapter.java`
- Create or modify: membership synchronization output port in `services/hermandad-service/src/main/java/com/repertorio/hermandad/application/port/`
- Modify: `services/hermandad-service/src/main/java/com/repertorio/hermandad/application/event/MemberAddedListener.java`
- Modify: Keycloak adapters/config to apply `!aws`
- Create: Cognito adapter unit tests
- Create: AWS-profile context test

**Steps:**
1. Write failing tests for `HERMANDAD_<hermandadId>_<role>`, eu-south-2 client region, user-not-found behavior, and listener delegation.
2. Run focused tests; expected RED.
3. Introduce one application output port used by the listener; implement it with Keycloak under `!aws` and Cognito under `aws`.
4. Keep the listener active under both profiles; do not disable membership synchronization under AWS.
5. Bind Cognito client region from `spring.cloud.aws.region.static`.
6. Run focused tests and `./gradlew :services:hermandad-service:test`.
7. Prepare checkpoint `feat(hermandad): synchronize membership through Cognito on AWS`; do not commit without approval.

**Deferred follow-up:** The existing asynchronous listener logs membership-provider failures. Durable retry/reconciliation requires a separate design and is intentionally outside this adapter task; do not treat a rethrow from `@Async` as a retry mechanism.

### Task 5: Add AWS profile configuration and startup checks

**Files:**
- Create: each active service `src/main/resources/application-aws.yml`
- Modify: Kafka consumers/config with `!aws`
- Modify: Redis and Keycloak configuration with `!aws`
- Create: one AWS-profile context test per active service
- Modify: `docker-compose.aws.yml` only where required by verified property names

**Steps:**
1. Write failing context tests proving the AWS profile does not require Kafka, Redis, or Keycloak.
2. Port only the required profile exclusions and queue/Cognito properties.
3. Use `eu-south-2`, never the branch's stale `us-east-1` default.
4. Run all AWS-profile context tests, then all three service test tasks.
5. Build all three service Docker images.
6. Prepare checkpoint `feat(infra): add AWS runtime profiles`; do not commit without approval.

### Task 6: Add the missing marcha queue without replacement

**Files:**
- Modify: `infrastructure/aws/lib/stack.ts`
- Modify: `docker-compose.aws.yml`
- Modify: `docs/aws-guide.md`
- Modify: `docs/functional-map.md`

**Steps:**
1. Add `marcha-events` to the existing queue loop. Do not rename or move existing constructs.
2. Add its environment mapping to compose and documentation.
3. Run `npm run synth`.
4. Run `cdk diff --no-change-set`.
5. Expected: one queue, one DLQ, their grants, and output are added; no replacement or deletion.
6. Request Oracle review of the exact diff and SHA-256 hash.
7. Do not deploy. Present the reviewed diff to the user.
8. Prepare checkpoint `fix(infra): provision marcha event queue`; do not commit without approval.

### Task 7: Final verification and handoff

**Steps:**
1. Run `./gradlew :shared:common:test`.
2. Run tests and `compileJava` for all three active services.
3. Run `npm ci`, `npm run synth`, and `cdk diff --no-change-set` in `infrastructure/aws`.
4. Run all affected Docker builds.
5. Search modified files for `FIXME`, `TODO`, `HACK`, credentials, private keys, account IDs, public IPs, `localhost:8080`, and direct Secrets Manager value retrieval.
6. Confirm `docs/openapi.yaml` is unchanged.
7. Request final Oracle review with diff hash, explicit file list, categorized issues, and verdict.
8. Present status, reviewed CDK diff, remaining SSM deployment blocker, and checkpoint commit options to the user.

## Deferred work

- Replace SSH deployment with SSM Session Manager/Run Command only after the required Secrets Manager-safe runtime workflow is available.
- Close port 22 and remove the unused key pair in a separately reviewed CDK change.
- Change destructive retention policies only as a separate decision with cost and recovery analysis.
- Do not clean old branches or worktrees until this branch is verified and integrated.
- **`cruceta-events` queue and consumer** — not provisioned in this consolidation. Cruceta outbox rows (aggregate type `cruceta`) are produced as `cruceta-events` by the outbox poller, but no SQS queue exists for this destination under AWS. Outbox rows remain `processed = false` and are retried each poll cycle. A separately reviewed decision is needed to provision the queue and define its consumer before enabling this flow.
