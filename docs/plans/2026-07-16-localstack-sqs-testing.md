# LocalStack SQS Integration Test

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add a LocalStack-based integration test for the SQS message sender, so AWS engineers can validate SQS code locally without an AWS account.

**Architecture:** Use Testcontainers LocalStack module to spin up a local SQS mock. Create SQS queues dynamically via the AWS SDK. Send a test message through `SqsMessageSender`, then verify it arrives via `SqsTemplate.receive()`. The test is `@Profile("aws")`-aware and runs alongside existing unit tests.

**Tech Stack:** Testcontainers 1.20.1 (localstack module), LocalStack 3.x, AWS SDK SQS v2, Spring Cloud AWS SQS

---

### Task 1: Add Testcontainers LocalStack dependency

**Files:**
- Modify: `gradle/libs.versions.toml` — add localstack module
- Modify: `services/hermandad-service/build.gradle.kts` — add localstack test dep

**Step 1: Update version catalog**

In `gradle/libs.versions.toml`, add under `[libraries]`:
```toml
testcontainers-localstack = { group = "org.testcontainers", name = "localstack", version.ref = "testcontainers" }
```

**Step 2: Add dependency to hermandad-service**

In `services/hermandad-service/build.gradle.kts`, in the `testImplementation` section, add:
```kotlin
testImplementation(libs.testcontainers.localstack)
```

**Step 3: Commit**

```bash
git add gradle/libs.versions.toml
git add services/hermandad-service/build.gradle.kts
git commit -m "chore: add Testcontainers LocalStack dependency for SQS integration tests"
```

---

### Task 2: Create LocalStack SQS integration test

**Files:**
- Create: `services/hermandad-service/src/test/java/com/repertorio/hermandad/adapter/outbound/messaging/SqsMessageSenderIntegrationTest.java`

**Step 1: Write the integration test**

```java
package com.repertorio.hermandad.adapter.outbound.messaging;

import io.awspring.cloud.sqs.operations.SqsTemplate;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.SQS;

@Testcontainers
class SqsMessageSenderIntegrationTest {

    private static final String QUEUE_NAME = "test-queue";
    private static final DockerImageName LOCALSTACK_IMAGE = DockerImageName.parse("localstack/localstack:3.8");

    @Container
    static LocalStackContainer localstack = new LocalStackContainer(LOCALSTACK_IMAGE)
            .withServices(SQS)
            .withStartupTimeout(Duration.ofSeconds(60));

    private static SqsTemplate sqsTemplate;
    private static SqsMessageSender sender;
    private static String queueUrl;

    @BeforeAll
    static void setUp() {
        // Create SQS client pointing to LocalStack
        var credentials = StaticCredentialsProvider.create(
                AwsBasicCredentials.create("test", "test"));
        var region = Region.US_EAST_1;

        try (var sqsClient = SqsClient.builder()
                .endpointOverride(localstack.getEndpointOverride(SQS))
                .credentialsProvider(credentials)
                .region(region)
                .build()) {

            // Create queue
            queueUrl = sqsClient.createQueue(r -> r.queueName(QUEUE_NAME)).queueUrl();
        }

        // Build SqsTemplate for LocalStack
        var template = SqsTemplate.builder()
                .sqsClient(SqsClient.builder()
                        .endpointOverride(localstack.getEndpointOverride(SQS))
                        .credentialsProvider(credentials)
                        .region(region)
                        .build())
                .build();

        sqsTemplate = template;
        sender = new SqsMessageSender(template);
    }

    @Test
    void sendsMessageToLocalStackSqs() {
        var payload = "{\"event\":\"test-event\",\"aggregateId\":\"00000000-0000-0000-0000-000000000001\"}";

        // Send via the adapter
        var sendFuture = sender.send(QUEUE_NAME, payload);
        assertThat(sendFuture).succeedsWithin(Duration.ofSeconds(5));

        // Receive and verify
        var received = sqsTemplate.receive(QUEUE_NAME, String.class);
        assertThat(received).isPresent();
        assertThat(received.get().getPayload()).isEqualTo(payload);
    }

    @Test
    void sendReturnsFailedFutureWhenBrokerUnreachable() {
        // This test verifies error handling by checking the behaviour
        // with a non-existent queue (LocalStack will reject it)
        var future = sender.send("non-existent-queue", "{}");
        assertThat(future).failsWithin(Duration.ofSeconds(10));
    }
}
```

**Step 2: Run the test**

```bash
JAVA_HOME="$HOME/.local/opt/jdk-21" ./gradlew :services:hermandad-service:test --tests "*SqsMessageSenderIntegrationTest" --no-daemon
```

Expected: BUILD SUCCESSFUL, tests pass.

**Step 3: Add test to CI filter (optional)**

If the project has a CI file, add integration test exclusions or markers.

**Step 4: Commit**

```bash
git add services/hermandad-service/src/test/java/com/repertorio/hermandad/adapter/outbound/messaging/SqsMessageSenderIntegrationTest.java
git commit -m "test: add LocalStack SQS integration test for SqsMessageSender"
```

---

### Task 3: Document how to run SQS tests locally

**Files:**
- Modify: `docs/aws-guide.md` — add LocalStack testing section

Add a section after "How to test locally" explaining:
1. Running the unit tests (no Docker needed, mocks SQS)
2. Running the integration test (starts LocalStack via Testcontainers, needs Docker)
3. Running a full manual test with LocalStack and Spring profile

---

## Verification

1. Unit tests pass: `./gradlew :services:hermandad-service:test --tests "*KafkaMessageSenderTest" --tests "*SqsMessageSenderTest"`
2. Integration test passes: `./gradlew :services:hermandad-service:test --tests "*SqsMessageSenderIntegrationTest"`
3. All existing tests still pass: `./gradlew :services:hermandad-service:test`

## Execution

**Plan is saved. Want me to implement it?**

1. **Subagent-driven** — I dispatch Task 1 (deps) + Task 2 (test) in parallel, then Task 3 (docs)
2. **Sequential** — I implement one task at a time with you reviewing
