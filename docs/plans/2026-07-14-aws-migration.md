# AWS Migration Plan

> **Goal**: Adapt the full-stack Spring Boot microservices project to deploy on AWS free tier — replacing self-managed Kafka, Keycloak, and container Postgres with SQS, Cognito, and RDS.

**Architecture**: Single EC2 t2.micro for Spring Boot services + nginx routing. Managed AWS services for everything else: SQS (events), Cognito (auth), RDS (Postgres), ElastiCache (Redis). No Eureka or Gateway on cloud — nginx handles path-based routing.

**Tech Stack**: Java 21, Spring Boot 4.1.0, Spring Cloud AWS 4.0.2, nginx, GitHub Actions, AWS SDK v2 (CognitoIdentityProviderClient)

**Cost**: $0/mo for 12 months under AWS free tier

---

### Task 1: Migration Research & Planning

**Files:**
- Create: `docs/plans/2026-07-14-aws-migration.md` (this file)

**Step 1: Review the migration analysis**

Review the following decisions:

**SQS:**
- Use `spring-cloud-aws-starter-sqs` 4.0.2 (4.1.0 pending Maven Central release)
- `SqsTemplate` replaces `KafkaTemplate` in OutboxPoller (synchronous send, no callback)
- `@SqsListener` replaces `@KafkaListener` on IdempotentEventConsumer
- Keep `processed_event` table for dedup (SQS Standard queues don't have built-in dedup)
- Standard SQS queues (not FIFO) — matches current Kafka ordering semantics
- Queues must be created manually (no auto-creation): `hermandad-events`, `hermandad-member-events`, `procesion-events`
- DLQ configured at queue level via RedrivePolicy

**Cognito:**
- Pre-token generation Lambda V2.0 injects custom `hermandad_memberships` claim into access token
- Cognito group naming convention: `HERMANDAD_{id}_{role}` → Lambda parses them into JSON claim
- **JwtAuthenticationConverter needs ZERO changes** — same claim structure
- CognitoIdentityProviderClient replaces Keycloak Admin Client for user management
- Cognito Essentials plan ($0.0055/MAU) required for Lambda triggers, but 50k MAU free tier covers it

**RDS:**
- Multiple databases on one RDS instance (not schemas) — each service gets its own DB
- Connection URL format: `jdbc:postgresql://{rds-endpoint}:5432/{db_name}`
- Flyway works unchanged with separate databases

**Networking:**
- nginx path-based routing replaces API Gateway + Eureka
- Services listen on localhost:8081/8082/8083, nginx on :80 proxies to them

**Step 2: Commit**

```bash
git add docs/plans/2026-07-14-aws-migration.md
git commit -m "docs: add AWS migration plan"
```

---

### Task 2: nginx Reverse Proxy Config

**Files:**
- Create: `infrastructure/nginx/nginx.conf`
- Create: `infrastructure/nginx/Dockerfile` (optional, for local testing)

**Step 1: Write nginx config**

Path-based routing mapping:

| Path | Target |
|------|--------|
| `/api/hermandades/*` | `localhost:8081` |
| `/api/procesiones/*` | `localhost:8082` |
| `/api/marchas/*` | `localhost:8083` |

```nginx
events {
    worker_connections 1024;
}

http {
    upstream hermandad { server 127.0.0.1:8081; }
    upstream procesion { server 127.0.0.1:8082; }
    upstream repertorio { server 127.0.0.1:8083; }

    # hermandad-ui frontend (future)
    # upstream ui { server 127.0.0.1:3000; }

    server {
        listen 80;
        server_name _;
        client_max_body_size 1M;

        # Procesion
        location /api/procesiones/ {
            proxy_pass http://procesion;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
        }

        # Repertorio
        location /api/marchas/ {
            proxy_pass http://repertorio;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
        }

        # Hermandad
        location /api/hermandades/ {
            proxy_pass http://hermandad;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
        }

        # Health
        location /health {
            return 200 "OK";
            add_header Content-Type text/plain;
        }
    }
}
```

**Step 2: Commit**

```bash
git add infrastructure/nginx/nginx.conf
git commit -m "infra: add nginx config for path-based routing"
```

---

### Task 3: AWS Infrastructure Setup (CDK)

**Files:**
- Create: `infrastructure/aws/stack.ts` or `infrastructure/aws/main.tf`
- Create: `infrastructure/aws/seed-queues.sh`

**Step 1: Define AWS resources**

Using AWS CDK (TypeScript) or CloudFormation. Resources:

- **EC2**: t2.micro, Amazon Linux 2023, security group (HTTP:80, SSH:22)
- **RDS**: db.t2.micro, 20GB, Postgres 16, security group (Postgres:5432, EC2-only)
- **ElastiCache**: t2.micro, Redis 7, security group (Redis:6379, EC2-only)
- **SQS**: 3 standard queues (hermandad-events, hermandad-member-events, procesion-events) + DLQs
- **Cognito**: User pool + app client + pre-token generation Lambda
- **ECR**: Repository for Docker images
- **IAM**: Instance profile with SQS + ECR + ElastiCache permissions
- **VPC**: Default VPC (simplest for free tier), or new VPC with public subnet

**Step 2: Write queue creation script**

```bash
#!/bin/bash
# infrastructure/aws/seed-queues.sh
# Run after CDK/CloudFormation deployment

QUEUES=("hermandad-events" "hermandad-member-events" "procesion-events")
REGION="eu-west-1"  # change as needed

for queue in "${QUEUES[@]}"; do
  # Create DLQ
  DLQ_URL=$(aws sqs create-queue --queue-name "$queue-dlq" --region "$REGION" --output text)
  DLQ_ARN=$(aws sqs get-queue-attributes \
    --queue-url "$DLQ_URL" \
    --attribute-names QueueArn \
    --region "$REGION" \
    --output text | awk '{print $2}')

  # Create main queue with DLQ redrive
  aws sqs create-queue --queue-name "$queue" \
    --region "$REGION" \
    --attributes '{"RedrivePolicy":"{\"deadLetterTargetArn\":\"'$DLQ_ARN'\",\"maxReceiveCount\":\"3\"}"}'
done
```

**Step 3: Commit**

```bash
git add infrastructure/aws/
git commit -m "infra: add AWS CDK stack with EC2, RDS, SQS, Cognito, ElastiCache"
```

---

### Task 4: SQS Outbox Adapter — Hermandad Service

**Files:**
- Modify: `services/hermandad-service/build.gradle.kts` — swap `spring-kafka` + `spring-boot-starter-kafka` → `spring-cloud-aws-starter-sqs`
- Modify: `services/hermandad-service/src/main/resources/application.yml` — replace Kafka config with SQS config
- Modify: `services/hermandad-service/src/main/java/com/repertorio/hermandad/adapter/outbound/outbox/OutboxEventPublisher.java` — inject SqsTemplate instead of KafkaTemplate
- Modify: `services/hermandad-service/src/main/java/com/repertorio/hermandad/adapter/outbound/outbox/OutboxPoller.java` — use SqsTemplate.send() instead of KafkaTemplate.send()
- Modify: `services/hermandad-service/src/main/java/com/repertorio/hermandad/adapter/inbound/kafka/IdempotentEventConsumer.java` — rename file, change @KafkaListener to @SqsListener, keep dedup logic
- Delete or keep-inactive: Kafka-related config

**Step 1: Update build.gradle.kts**

Replace:
```kotlin
implementation("org.springframework.kafka:spring-kafka")
implementation("org.springframework.boot:spring-boot-starter-kafka")
```

With:
```kotlin
implementation("io.awspring.cloud:spring-cloud-aws-starter-sqs")
```

**Step 2: Update application.yml**

Replace Kafka bootstrap-servers with SQS region:

```yaml
spring:
  cloud:
    aws:
      region:
        static: eu-west-1
      credentials:
        # Use instance profile on EC2, or env vars locally
  # Remove: spring.kafka.*
```

**Step 3: Rewrite OutboxPoller.sendToQueue**

```java
// Before: kafkaTemplate.send(topic, payload).whenComplete(...)
// After:
try {
    sqsTemplate.send(outboxEvent.getAggregateType() + "-events", outboxEvent.getPayload());
    outboxEvent.markAsProcessed();
    outboxEventRepository.save(outboxEvent);
} catch (SqsException e) {
    log.error("Failed to send outbox event to SQS: {}", outboxEvent.getId(), e);
}
```

**Step 4: Rewrite IdempotentEventConsumer**

Rename class to `IdempotentEventSqsConsumer` (optional). Replace `@KafkaListener` with `@SqsListener`. Use SQS `@Header(SqsHeaders.SQS_MESSAGE_ID_HEADER)` for dedup key instead of UUID-from-payload.

**Step 5: Run tests**

```bash
./gradlew :services:hermandad-service:test
```

**Step 6: Commit**

```bash
git commit -m "feat(hermandad): migrate outbox from Kafka to SQS"
```

---

### Task 5: SQS Outbox Adapter — Procesion Service

**Files:**
- Modify: `services/procesion-service/build.gradle.kts` — swap `spring-boot-starter-kafka` → `spring-cloud-aws-starter-sqs`
- Modify: `services/procesion-service/src/main/resources/application.yml` — SQS config
- Modify: `services/procesion-service/src/main/java/com/repertorio/procesion/adapter/outbound/outbox/OutboxPoller.java` — SqsTemplate
- Modify: `services/procesion-service/src/main/java/com/repertorio/procesion/adapter/outbound/events/DomainEventPublisherAdapter.java` — no KafkaTemplate to change, but verify
- Delete: Kafka consumer config if any

**Step 1: Build + config changes**

Same pattern as Task 4. Remove `spring-boot-starter-kafka`, add `spring-cloud-aws-starter-sqs`.

**Step 2: OutboxPoller rewrite**

Same SqsTemplate pattern as hermandad. Note: procesion-service has no consumer — it only produces to the `procesion-events` queue.

**Step 3: Compile + tests**

```bash
./gradlew :services:procesion-service:compileJava :services:procesion-service:test
```

**Step 4: Commit**

```bash
git commit -m "feat(procesion): migrate outbox from Kafka to SQS"
```

---

### Task 6: SQS Outbox Adapter — Repertorio Service

(This task may be part of Sprint 9 if repertorio hasn't shipped yet. If it already shipped with Kafka, apply same pattern as Tasks 4-5.)

**Changes:**
- `build.gradle.kts` — use `spring-cloud-aws-starter-sqs` instead of Kafka starter
- OutboxPoller uses SqsTemplate
- No consumer (repertorio only produces events)

---

### Task 7: Cognito JWT — Spring Security Config

**Files:**
- Modify: `services/hermandad-service/src/main/resources/application.yml` — issuer-uri
- Modify: `services/procesion-service/src/main/resources/application.yml` — issuer-uri
- Modify: `services/repertorio-service/src/main/resources/application.yml` — issuer-uri
- No changes to: `JwtAuthenticationConverter.java` (same claim structure)

**Step 1: Update issuer-uri**

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: https://cognito-idp.eu-west-1.amazonaws.com/eu-west-1_XXXXXXXXX
```

**Step 2: Verify Spring Security works with Cognito**

```bash
# Get token
aws cognito-idp admin-initiate-auth --user-pool-id <pool-id> --client-id <client-id> --auth-flow ADMIN_USER_PASSWORD_AUTH --auth-parameters USERNAME=admin,PASSWORD=xxxx

# Test with curl
curl -H "Authorization: Bearer <token>" http://localhost:8081/api/hermandades
```

**Step 3: Commit**

```bash
git commit -m "feat: switch JWT issuer from Keycloak to Cognito"
```

---

### Task 8: Cognito User Adapter — Replace Keycloak Admin Client

**Files:**
- Create: `services/hermandad-service/.../adapter/outbound/cognito/CognitoUserAdapter.java` — replaces `KeycloakUserExistenceAdapter`
- Create: `services/hermandad-service/.../adapter/outbound/cognito/CognitoMembershipAdapter.java` — replaces `KeycloakMembershipAdapter`
- Delete or deprecate: Keycloak adapter classes
- Modify: `services/hermandad-service/build.gradle.kts` — remove `keycloak-admin-client`, add `cognito-idp` SDK
- Modify: `services/hermandad-service/src/main/resources/application.yml` — add Cognito pool ID config

**Step 1: Add dependency**

```kotlin
implementation(platform("software.amazon.awssdk:bom:2.28.0"))
implementation("software.amazon.awssdk:cognitoidentityprovider")
```

**Step 2: Write CognitoUserAdapter**

```java
@Component
public class CognitoUserAdapter implements UserExistencePort {
    private final CognitoIdentityProviderClient cognitoClient;
    @Value("${cognito.user-pool-id}")
    private String userPoolId;

    @Override
    public boolean exists(String userId) {
        try {
            cognitoClient.adminGetUser(req -> req.userPoolId(userPoolId).username(userId));
            return true;
        } catch (UserNotFoundException e) {
            return false;
        }
    }
}
```

**Step 3: Write CognitoMembershipAdapter**

```java
@Component
public class CognitoMembershipAdapter {
    private final CognitoIdentityProviderClient cognitoClient;
    @Value("${cognito.user-pool-id}")
    private String userPoolId;

    public void addUserToHermandad(String userId, UUID hermandadId, HermandadRole role) {
        String groupName = "HERMANDAD_" + hermandadId + "_" + role.name();
        cognitoClient.adminAddUserToGroup(req -> req
            .userPoolId(userPoolId)
            .username(userId)
            .groupName(groupName));
    }

    public void removeUserFromHermandad(String userId, UUID hermandadId) {
        // List groups, remove those matching the hermandad
        var groups = cognitoClient.adminListGroupsForUser(req -> req
            .userPoolId(userPoolId)
            .username(userId));
        groups.groups().stream()
            .filter(g -> g.groupName().startsWith("HERMANDAD_" + hermandadId))
            .forEach(g -> cognitoClient.adminRemoveUserFromGroup(req -> req
                .userPoolId(userPoolId)
                .username(userId)
                .groupName(g.groupName())));
    }
}
```

**Step 4: Update tests**

Replace Keycloak mocks with Cognito mocks in `KeycloakUserExistenceAdapterTest` → rename to `CognitoUserAdapterTest`.

**Step 5: Commit**

```bash
git commit -m "feat(hermandad): replace Keycloak admin client with Cognito SDK"
```

---

### Task 9: RDS Connection Config

**Files:**
- Modify: `services/hermandad-service/src/main/resources/application.yml` — datasource URL
- Modify: `services/procesion-service/src/main/resources/application.yml` — datasource URL
- Modify: `services/repertorio-service/src/main/resources/application.yml` — datasource URL

**Step 1: Update datasource URLs**

```yaml
spring:
  datasource:
    url: jdbc:postgresql://${RDS_ENDPOINT}:5432/hermandad_db
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
```

**Step 2: Create databases on RDS**

```bash
PGPASSWORD=$DB_PASSWORD psql -h $RDS_ENDPOINT -U postgres -c "CREATE DATABASE hermandad_db;"
PGPASSWORD=$DB_PASSWORD psql -h $RDS_ENDPOINT -U postgres -c "CREATE DATABASE procesion_db;"
PGPASSWORD=$DB_PASSWORD psql -h $RDS_ENDPOINT -U postgres -c "CREATE DATABASE repertorio_db;"
```

**Step 3: Run Flyway migrations**

```bash
./gradlew :services:hermandad-service:flywayMigrate
./gradlew :services:procesion-service:flywayMigrate
# repertorio migrations run on first startup
```

**Step 4: Commit**

```bash
git commit -m "feat: switch datasources from container Postgres to RDS"
```

---

### Task 10: CI/CD Pipeline

**Files:**
- Create: `.github/workflows/deploy.yml`

**Step 1: Write GitHub Actions workflow**

```yaml
name: Deploy

on:
  push:
    branches: [main]

env:
  AWS_REGION: eu-west-1
  ECR_REGISTRY: ${{ secrets.AWS_ACCOUNT_ID }}.dkr.ecr.eu-west-1.amazonaws.com

jobs:
  build-and-deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - uses: actions/setup-java@v4
        with:
          java-version: 21
          distribution: temurin

      - name: Build
        run: ./gradlew build -x test

      - name: Build Docker images
        run: |
          docker build -t hermandad-service ./services/hermandad-service
          docker build -t procesion-service ./services/procesion-service
          docker build -t repertorio-service ./services/repertorio-service

      - name: Push to ECR
        run: |
          aws ecr get-login-password | docker login --username AWS --password-stdin $ECR_REGISTRY
          for service in hermandad-service procesion-service repertorio-service; do
            docker tag $service $ECR_REGISTRY/$service:latest
            docker push $ECR_REGISTRY/$service:latest
          done

      - name: Deploy to EC2
        uses: appleboy/ssh-action@v1.0.3
        with:
          host: ${{ secrets.EC2_HOST }}
          username: ec2-user
          key: ${{ secrets.EC2_SSH_KEY }}
          script: |
            cd /home/ec2-user/repertorio
            aws ecr get-login-password | docker login --username AWS --password-stdin $ECR_REGISTRY
            docker compose pull
            docker compose up -d --remove-orphans
```

**Step 2: Add docker-compose.aws.yml**

A version of docker-compose that uses ECR images and RDS instead of local Postgres containers.

```yaml
name: repertorio-aws
services:
  nginx:
    image: nginx:alpine
    volumes:
      - ./infrastructure/nginx/nginx.conf:/etc/nginx/nginx.conf:ro
    ports:
      - "80:80"
    depends_on:
      - hermandad-service
      - procesion-service
      - repertorio-service

  hermandad-service:
    image: ${ECR_REGISTRY}/hermandad-service:latest
    environment:
      RDS_ENDPOINT: ${RDS_ENDPOINT}
      DB_USERNAME: ${DB_USERNAME}
      DB_PASSWORD: ${DB_PASSWORD}
      AWS_REGION: eu-west-1
      COGNITO_POOL_ID: ${COGNITO_POOL_ID}

  procesion-service:
    image: ${ECR_REGISTRY}/procesion-service:latest
    environment:
      RDS_ENDPOINT: ${RDS_ENDPOINT}
      DB_USERNAME: ${DB_USERNAME}
      DB_PASSWORD: ${DB_PASSWORD}
      AWS_REGION: eu-west-1
      COGNITO_POOL_ID: ${COGNITO_POOL_ID}

  repertorio-service:
    image: ${ECR_REGISTRY}/repertorio-service:latest
    environment:
      RDS_ENDPOINT: ${RDS_ENDPOINT}
      DB_USERNAME: ${DB_USERNAME}
      DB_PASSWORD: ${DB_PASSWORD}
      AWS_REGION: eu-west-1
      COGNITO_POOL_ID: ${COGNITO_POOL_ID}
```

**Step 3: Commit**

```bash
git add .github/workflows/deploy.yml docker-compose.aws.yml
git commit -m "ci: add GitHub Actions deploy pipeline to AWS"
```

---

### Task 11: LocalStack Dev Profile (Optional)

**Files:**
- Create: `docker-compose.localstack.yml` — LocalStack for local SQS testing
- Modify: `application-dev.yml` — LocalStack endpoint

For local development without actual AWS, use LocalStack:

```yaml
spring:
  cloud:
    aws:
      endpoint-override: http://localhost:4566
      region:
        static: eu-west-1
      credentials:
        access-key: test
        secret-key: test
```

```bash
docker run --name localstack -p 4566:4566 localstack/localstack
aws --endpoint-url=http://localhost:4566 sqs create-queue --queue-name hermandad-events
```

---

### Task 12: Documentation & Cleanup

**Step 1: Update functional-map.md**

- Update "Inter-service Communication" section — Kafka → SQS
- Update "Auth" section — Keycloak → Cognito
- Update "Infrastructure" section — add AWS resources

**Step 2: Update architecture.md**

- Document the AWS deployment topology
- Document SQS queue names and their purpose
- Document Cognito user pool configuration

**Step 3: Update backlog.md**

- Mark migration tasks as completed
- Update any pending items affected by the migration

**Step 4: Clean up Kafka dependencies**

- Remove `kafka-init` from docker-compose (no longer needed)
- Remove `kafka-ui` if not used
- Mark `docker-compose.yml` as dev-only (new docker-compose.aws.yml for cloud)

**Step 5: Commit**

```bash
git commit -m "docs: update architecture docs for AWS migration"
```
