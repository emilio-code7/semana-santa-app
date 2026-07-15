# Backlog — Semana Santa App

Process:
1. **Sprint planning** — pick backlog items, define acceptance criteria, break into tasks
2. **Implementation** — TDD (RED → GREEN → refactor), domain-first, vertical slices. Every feature needs a test before code.
3. **Commit** — one commit per completed story (acceptance criteria met, all green). Descriptive conventional commit messages in English.
4. **Sprint review** — mark done, decide next sprint

---

## Current Sprint

### Sprint 3 — Spring Boot 4.1 Migration ✅

Upgrade platform to Boot 4.1. Bump versions, migrate modular starters, Jackson 3 packages, Redis serializers.

- ~~Version catalog: Spring Boot 4.1.0, Spring Cloud 2025.0.0, tools.jackson~~ ✅
- ~~Build files: all services migrated to Boot 4.1 / tools.jackson~~ ✅
- ~~Jackson imports: RedisConfig, shared/common, test factories migrated~~ ✅
- ~~Test packages: autoconfigure paths, @MockitoBean, jakarta.persistence~~ ✅
- ~~TestCacheConfig: fix slice test CacheManager failures~~ ✅
- ~~Flyway V4 + service-level unique name constraint on Hermandad~~ ✅
- ~~HermandadAlreadyExistsException → 409 CONFLICT~~ ✅

---

### Sprint 4 — Hermandad Service Hardening ✅

Complete the hermandad service: auth, tests, missing fields, and outbox quality.

1. **Auth enforcement** ✅
   - ~~**`JwtAuthenticationConverter`**: new class. Extracts `hermandad_memberships` claim from JWT, creates `GrantedAuthority` in format `HERMANDAD_{hermandadId}_{role}`. Also captures JWT `sub` as the authenticated user ID for auto-assign.~~ ✅
   - ~~**`SecurityConfig`**: wire the converter. `POST /api/hermandades` → any authenticated user (bootstrap). `GET /api/hermandades/{id}` → public. Everything else → authenticated + `@PreAuthorize`.~~ ✅
   - ~~**`HermandadController`**: `@PreAuthorize` on member management. Bootstrap endpoint stays open.~~ ✅
   - ~~**`HermandadService.createHermandad()`**: accept creator userId (from JWT `sub`) for auto-assign (ties into item 4).~~ ✅
2. **Integration tests** — JPA repositories against real PostgreSQL ✅
   - ~~`HermandadRepositoryIntegrationTest` — saves/finds/constraints via running Postgres~~ ✅
   - ~~Skips automatically if no Postgres available (graceful dev/CI fallback)~~ ✅
3. **Missing entity fields** — `description` (added, nullable TEXT) ✅
   - ~~`V5__add_description_to_hermandad.sql`~~ ✅
   - ~~`CreateHermandadRequest`, `HermandadResponse`, constructor, service, tests~~ ✅
4. **Auto-assign creator as HERMANDAD_ADMIN** on `POST /api/hermandades` ✅
   - ~~Creator saved as `HERMANDAD_ADMIN` in `hermandad_member` on create~~ ✅
   - ~~`HermandadSecurityService` dual-path auth: JWT authorities (fast) → DB membership (fallback)~~ ✅
5. **Outbox quality** — `ORDER BY created_at` + batch size limit (100) ✅
   - ~~`findTop100ByProcessedFalseOrderByCreatedAtAsc()` query~~ ✅
    - ~~Poller uses `ORDER BY created_at ASC`, capped at 100~~ ✅

---

### Sprint 5 — Hermandad Polish + Idempotent Consumer ✅

Complete remaining hermandad-service gaps and build the idempotent Kafka consumer reference pattern.

1. **`MemberAddedEvent` add `hermandadId`** ✅
   - ~~Add `UUID hermandadId` field to `MemberAddedEvent` record~~ ✅
   - ~~Populate it when publishing from `HermandadService.addMember()`~~ ✅
   - ~~Update `HermandadServiceTest`~~ ✅
   - **AC**: Event payload includes hermandadId; Kafka consumers get tenant context without cross-service lookup

2. **MockMvc + Exception handler tests** ✅
   - ~~Fill gaps in `HermandadControllerTest` (validation 400, conflict 409, response body checks for success paths)~~ ✅
   - ~~Add `GlobalExceptionHandlerTest` for each handler (HermandadNotFoundException → 404, validation → 400, conflict → 409, AccessDenied → 403, generic → 500)~~ ✅
   - **AC**: All exception handlers have a test proving correct status code + body format

3. **Idempotent Kafka consumer (reference implementation)** ✅
   - ~~`V6__create_processed_event_table.sql`: `processed_event` table with `(event_id UUID PK, consumer_name VARCHAR(100), processed_at TIMESTAMP)`~~ ✅
   - ~~Consumer bean listens to `hermandad-events` and `hermandad-member-events`~~ ✅
   - ~~Checks `processed_event` table before processing; skips if already processed; stores event_id + consumer_name + timestamp if new~~ ✅
   - ~~Register consumer group for offset tracking~~ ✅
   - **AC**: Duplicate Kafka messages are silently skipped; each unique event is processed exactly once; pattern is copy-paste ready for other services

---

### Sprint 2 — Audit Fixes & Member Removal ✅

Clean technical debt, add missing error handlers, and complete member CRUD.

---

#### ~~1. Audit fixes~~ ✅

- ~~Fix `HermandadMember.updatedAt`~~ ✅
- ~~Add `DataIntegrityViolationException` handler → 409~~ ✅
- ~~Add `MethodArgumentNotValidException` handler → 400~~ ✅
- ~~Add generic `Exception` fallback → 500~~ ✅
- ~~Bump outbox `payload` column from `VARCHAR(255)` to `TEXT`~~ ✅

---

#### 2. Member removal (🔄 in progress)

**As a** API client, **I want** to remove a member via `DELETE /api/hermandades/{hermandadId}/members/{userId}`, **so that** I can undo member additions.

**Done:**
- ~~`MemberRemovedEvent` domain event~~ ✅
- ~~`HermandadMemberRepository.delete()` port + adapter~~ ✅
- ~~`HermandadService.removeMember()` + service test~~ ✅

**All done:**
- ~~`DELETE` endpoint in controller~~ ✅
- ~~MockMvc test~~ ✅
- ~~Sync OpenAPI spec~~ ✅

---

### Sprint 1 — Complete ✅

All stories implemented. E2E verified except duplicate member (handled in Sprint 2 audit fixes).

---

### Sprint 6 — Hermandad Service Polish ✅

Complete remaining hermandad-service gaps: pagination, CAPATAZ role in OpenAPI, Keycloak validation.

1. **Members list pagination** ✅
   - ~~Port, JPA repo, adapter, service, controller updated with `Pageable`/`Page`~~ ✅
   - ~~`@PageableDefault(size = 20)` on controller, `@Cacheable` removed from paginated method~~ ✅
   - ~~Service test + integration test fix~~ ✅
   - **AC**: `GET /api/hermandades/{id}/members?page=0&size=20` returns paginated response with Spring Data `Page` structure

2. **CAPATAZ role in OpenAPI spec** ✅
   - ~~Already present in `AddMemberRequest.role`, `HermandadMember.role`, `ChangeRoleRequest.role` enums~~ ✅
   - **AC**: OpenAPI spec reflects all domain roles including CAPATAZ

3. **Keycloak user existence validation** ✅
   - ~~`UserExistencePort` interface with `exists(String userId)`~~ ✅
   - ~~`KeycloakUserExistenceAdapter` calling `users().get(userId).toRepresentation()`~~ ✅
   - ~~Check in `HermandadService.addMember()` → `IllegalArgumentException` → 400~~ ✅
   - **AC**: Adding a member with a non-existent Keycloak user returns 400; existing users pass through

4. **Integration test for pagination (bonus)** ✅
   - ~~`HermandadControllerIntegrationTest` with Testcontainers PostgreSQL pattern~~ ✅
   - ~~3 scenarios: paginated with size=2, size=5, default size=20~~ ✅
   - **AC**: Integration test verifies pagination structure end-to-end

---

### Sprint 7 — MVP Foundation: Member Removal + Procesión Service ✅

Build the remaining hermandad-service feature (member removal), bootstrap the Procesión service, and apply polish across both services.

**Implementation plan:** `docs/plans/2026-07-10-sprint-07-mvp-foundation.md`

**Completed:**
1. **Member Removal (Hermandad Service)** ✅
2. **Procesión Service — Project Skeleton** ✅
3. **Procesión Service — Domain Aggregate + Repository Port** ✅
4. **Procesión Service — JPA Adapter + Flyway** ✅
5. **Procesión Service — Service Layer + Outbox** ✅
6. **Procesión Service — REST Controller + Auth** ✅
7. **Docker Compose — Procesión Service + DB** ✅
8. **API Gateway — Procesión Routes** ✅
9. **Spanish→English refactor** — Procesión internal API anglicized ✅
10. **Procesión outbox pattern** — events now reach Kafka via outbox table + poller ✅
11. **Structured error responses (both services)** — `ApiError` JSON replacing plain text ✅
12. **Catch block narrowing** — Keycloak adapters, listeners, outbox ✅
13. **Hermandad entity `updatedAt`** — added + Flyway V7 ✅
14. **`@Transactional` on Hermandad write methods** — `addMember()`, `changeRole()`, `removeMember()` ✅
15. **Procesión domain unit tests** — 11 state machine tests ✅
16. **Procesión integration tests** — 4 repository + 8 controller integration tests ✅
17. **Spring Boot 4.1 compilation** — both services compile on 4.1 with tools.jackson ✅

**Deferred:**
- Audit rename (`keycloak_group_id` → `keycloak_group_id_refs`)
- Shared lib unit tests
- Outbox→Kafka integration test (EmbeddedKafka)
- Repertorio Service, Tracking Service, Notification Service

---

### Sprint 9 — Repertorio Service ✅

> Build the final MVP service: global marcha catalog + cruceta (ordered marcha list per procession).
> **Package**: `com.repertorio.marcha` · **Port**: 8083 · **DB**: `postgres-repertorio:5433/repertorio_db`

**Completed:**
- ✅ TASK-1: Project scaffold (build, app class, configs, Flyway V1-V4)
- ✅ TASK-2: Domain model — Marcha aggregate (BandType, Marcha, events, validation)
- ✅ TASK-3: Domain model — Cruceta aggregate (Cruceta, CrucetaItem, events, validation)
- ✅ TASK-4: DB migrations — V1 (marcha), V2 (cruceta + cruceta_item), V3 (seed 15 marchas), V4 (outbox)
- ✅ TASK-5: JPA persistence adapters (MarchaEntity, CrucetaEntity, repositories + adapters)
- ✅ TASK-6: Application services (MarchaService, CrucetaService)
- ✅ TASK-7: REST controllers + DTOs + Security (MarchaController, CrucetaController)
- ✅ TASK-8: Outbox pattern (mirror of hermandad/procesion — publisher, entity, repo, poller)
- ✅ TASK-9: Dockerfile + docker-compose.yml entry + Gateway routes
- ✅ TASK-10: Controller tests (MarchaControllerTest 13 tests, CrucetaControllerTest 6 tests)
- ✅ TASK-11: Service + domain unit tests (44 total — 6 domain + 17 service + 19 controller)

**Deferred:**
- ❌ TASK-12: Kafka consumer for Procesion events (cruceta cleanup on procesion deletion)
- ❌ Integration tests (repository + controller) to match hermandad/procesion pattern


---


### Sprint 10 — AWS Migration

> Migrate from self-managed Kafka/Keycloak/Postgres containers to AWS-managed services (SQS, Cognito, RDS, ElastiCache). Target: free tier ($0/mo).
> **Architecture**: Single EC2 t2.micro → 3 Spring Boot services + nginx. SQS replaces Kafka, Cognito replaces Keycloak, RDS replaces container Postgres, ElastiCache replaces container Redis.
> **Stack**: Spring Cloud AWS 4.0.2, AWS CDK, Cognito Lambda triggers

---

#### AWS-TASK-1: AWS Infrastructure Stack

**Description:** Deploy the base infrastructure — EC2 instance, RDS Postgres, ElastiCache Redis, SQS queues, Cognito user pool, IAM roles, and security groups. Use AWS CDK for infrastructure-as-code.

**Acceptance Criteria:**
- [ ] EC2 t2.micro with Amazon Linux 2023, security group (HTTP:80, SSH:22 from trusted IPs)
- [ ] RDS db.t2.micro, 20GB gp2, Postgres 16 — single instance with 5 databases (hermandad_db, procesion_db, repertorio_db, tracking_db, notification_db)
- [ ] ElastiCache t2.micro, Redis 7 — security group restricted to EC2 security group
- [ ] 3 SQS Standard queues created: `hermandad-events`, `hermandad-member-events`, `procesion-events` — each with DLQ + RedrivePolicy (maxReceiveCount=3)
- [ ] Cognito user pool + app client + pre-token generation Lambda V2.0 configured
- [ ] IAM instance profile with permissions for SQS (SendMessage, ReceiveMessage, DeleteMessage), ECR (pull), ElastiCache (connect)
- [ ] ECR repository for each service's Docker images
- [ ] Default VPC or new VPC with public subnet
- [ ] `infrastructure/aws/stack.ts` — CDK app compiling and deployable

**Technical Notes:**
- Use Cognito group naming convention `HERMANDAD_{id}_{role}` — Lambda parses these into the `hermandad_memberships` JSON claim (same format as current Keycloak)
- Pre-token generation Lambda uses Lambda V2.0 runtime (ensures access token customization)
- Queue creation: `aws sqs create-queue` for each queue + DLQ
- Use AWS-managed policies where possible, scope down to least-privilege after initial setup
- RDS initial database creation via `psql` after stack deploy

**Effort:** 2-3 files (CDK stack + Lambda + seed script) · **Dependencies:** None

---

#### AWS-TASK-2: nginx Reverse Proxy + EC2 Bootstrap

**Description:** Replace API Gateway + Eureka with nginx path-based routing on the EC2 instance. Services run on localhost and nginx proxies external requests. Includes EC2 bootstrap/user-data script, Docker Compose for AWS, and nginx config.

**Acceptance Criteria:**
- [ ] `infrastructure/nginx/nginx.conf` — proxies `/api/hermandades/*` → `localhost:8081`, `/api/procesiones/*` → `localhost:8082`, `/api/marchas/*` → `localhost:8083`
- [ ] nginx container (or direct install) runs on EC2, binds port 80
- [ ] `docker-compose.aws.yml` — uses ECR images + env vars for RDS/SQS/Cognito config, no Eureka/Gateway/Keycloak/Kafka containers
- [ ] EC2 user-data script: installs Docker + Compose, pulls images, runs `docker compose up`
- [ ] `curl http://<ec2-public-ip>/health` returns `200 OK`
- [ ] `curl http://<ec2-public-ip>/api/hermandades` → proxied to hermandad-service (requires JWT)
- [ ] No Gateway no Eureka dependencies on the deployed stack

**Technical Notes:**
- nginx config is static (not generated), included in the repo
- EC2 user-data mounts ECR auth via `aws ecr get-login-password`
- Services don't discover each other — direct localhost communication only
- No `kafka-init`, no `kafka-ui`, no observability containers on the minimal deploy

**Effort:** 3 files (nginx.conf, docker-compose.aws.yml, user-data.sh) · **Dependencies:** AWS-TASK-1 (EC2 running)

---

#### AWS-TASK-3: SQS Outbox Migration (All Services)

**Description:** Replace Kafka with SQS in the outbox pattern across all 3 active services. OutboxPoller sends to SQS instead of Kafka. Hermandad's IdempotentEventConsumer switches from `@KafkaListener` to `@SqsListener`. Keep the `processed_event` dedup table unchanged.

**Acceptance Criteria:**
- [ ] `spring-cloud-aws-starter-sqs` 4.0.2 replaces `spring-boot-starter-kafka` in all 3 services
- [ ] OutboxPoller (hermandad + procesion + repertorio): `SqsTemplate.send(queueName, payload)` replaces `KafkaTemplate.send(topic, payload)`
- [ ] Hermandad `IdempotentEventConsumer`: `@SqsListener("hermandad-events")` + `@SqsListener("hermandad-member-events")` replace `@KafkaListener`
- [ ] Dedup still uses `processed_event` table (SQS Standard doesn't have built-in dedup)
- [ ] `@SqsListener` uses `@Header(SqsHeaders.SQS_MESSAGE_ID_HEADER)` for dedup key
- [ ] Kafka config removed from `application.yml` (bootstrap-servers, consumer/producer props)
- [ ] SQS region config in `application.yml`: `spring.cloud.aws.region.static`
- [ ] Services use instance profile credentials on EC2, env vars locally
- [ ] `./gradlew :services:hermandad-service:compileJava` passes
- [ ] `./gradlew :services:procesion-service:compileJava` passes
- [ ] `./gradlew :services:repertorio-service:compileJava` passes
- [ ] All existing unit/integration tests pass (adapters mocked)

**Technical Notes:**
- No code change to domain or application layers — only adapter layer changes
- SQS Standard queues (not FIFO) — matches current Kafka ordering (best-effort)
- SQS `@SqsListener` auto-deletes messages on successful return; thrown exception returns to queue after visibility timeout
- Keep Kafka starter removed from all build.gradle.kts — no dual-transport
- Repertorio (if built with Kafka in Sprint 9) applies same migration pattern
- DLQ handled at queue level via RedrivePolicy — no Spring-side config needed

**Effort:** ~8 files across 3 services · **Dependencies:** AWS-TASK-1 (queues exist)

---

#### AWS-TASK-4: Cognito JWT Integration

**Description:** Switch JWT authentication from Keycloak to Cognito. Update issuer-uri in all 3 services. Deploy the pre-token generation Lambda that injects `hermandad_memberships` claim into access tokens. No changes to JwtAuthenticationConverter — same claim structure.

**Acceptance Criteria:**
- [ ] Pre-token generation Lambda deployed (Node.js or Java) — reads Cognito groups `HERMANDAD_{id}_{role}`, injects `hermandad_memberships` JSON array into access token
- [ ] `application.yml` in all 3 services: `spring.security.oauth2.resourceserver.jwt.issuer-uri` points to Cognito URL
- [ ] `JwtAuthenticationConverter` unchanged — reads same `hermandad_memberships` claim
- [ ] `JwtMembershipExtractor` unchanged — parses same JSON format
- [ ] Cognito groups created per user: `HERMANDAD_{hermandadId}_{role}` format
- [ ] `curl -H "Authorization: Bearer <cognito-token>" http://localhost:8081/api/hermandades` → 200
- [ ] `curl` without token → 401
- [ ] `spring-boot-starter-oauth2-resource-server` still works with Cognito OIDC config

**Technical Notes:**
- Cognito access tokens don't include custom claims by default — Lambda trigger V2.0 is required
- The Lambda must use `event.response.claimsAndScopeOverrideDetails.accessTokenGeneration.claimsToAddOrOverride`
- Cognito does NOT accept `/.well-known/openid-configuration` appended to issuer-uri — Spring adds it automatically
- Keycloak issuer format: `http://keycloak:8080/realms/semana-santa`
- Cognito issuer format: `https://cognito-idp.{region}.amazonaws.com/{poolId}`
- Token validation (expiration, signature, issuer) handled by Spring Security's Nimbus library — identical behavior

**Effort:** ~5 files (Lambda + 3 application.yml + deploy script) · **Dependencies:** AWS-TASK-1 (Cognito pool exists)

---

#### AWS-TASK-5: Cognito Admin Adapter (Replace Keycloak Admin Client)

**Description:** Replace `KeycloakUserExistenceAdapter` and `KeycloakMembershipAdapter` with Cognito SDK equivalents. The new adapters use `CognitoIdentityProviderClient` for user existence checks and group management.

**Acceptance Criteria:**
- [ ] `CognitoUserAdapter` implements `UserExistencePort` — calls `cognitoClient.adminGetUser()`, returns false on `UserNotFoundException`
- [ ] `CognitoMembershipAdapter` replaces `KeycloakMembershipAdapter` — `addUserToGroup()` for role assignment, group cleanup for member removal
- [ ] `build.gradle.kts` in hermandad-service: add `software.amazon.awssdk:cognitoidentityprovider`, remove `keycloak-admin-client`
- [ ] `application.yml`: add cognito user-pool-id config
- [ ] All tests pass: `KeycloakUserExistenceAdapterTest` → renamed to `CognitoUserAdapterTest`, mocks updated to Cognito SDK
- [ ] `hermandad-service:compileJava` passes

**Technical Notes:**
- SDK v2 `CognitoIdentityProviderClient` is auto-closeable — use constructor injection (Spring creates one bean)
- Group names match Cognito convention: `HERMANDAD_{hermandadId}_{role}`
- For user removal: list user's groups, filter by hermandadId prefix, remove each
- The `processed_event` consumer and outbox stay unchanged — this only affects Keycloak admin operations
- SDK dependency uses Bill of Materials (BOM): `software.amazon.awssdk:bom:2.28.0`

**Effort:** 4 files (2 adapters + build.gradle + test) · **Dependencies:** AWS-TASK-4 (Cognito pool configured)

---

#### AWS-TASK-6: RDS Connection + Docker Compose AWS

**Description:** Point all services from container Postgres to the new RDS instance. Create the docker-compose.aws.yml that runs the full stack on EC2 with managed services. No local Postgres/Kafka/Keycloak containers needed.

**Acceptance Criteria:**
- [ ] 3 `application.yml` files: datasource URL points to RDS endpoint, credentials from env vars
- [ ] Docker Compose AWS profile: services + nginx only (no Postgres, no Kafka, no Keycloak, no ZK, no Redis container — everything managed)
- [ ] Flyway migrations run correctly against RDS databases
- [ ] `docker compose -f docker-compose.aws.yml up` starts all 3 services + nginx
- [ ] All services register health check endpoints accessible through nginx
- [ ] Redis config switches from container host to ElastiCache endpoint

**Technical Notes:**
- One RDS instance, separate databases (not schemas): `hermandad_db`, `procesion_db`, `repertorio_db`
- Each service's `flyway_schema_history` table lives in its own database — no migration conflicts
- `application.yml` uses `jdbc:postgresql://${RDS_ENDPOINT}:5432/{db_name}`
- RDS initial setup script: `CREATE DATABASE hermandad_db;` etc.
- RDS is on the free tier — db.t2.micro, 20GB gp2 storage, automated backups enabled
- If RDS not available locally, services fall back to H2 or skip via `@ConditionalOnProperty`

**Effort:** 5 files (3 application.yml + docker-compose.aws.yml + init script) · **Dependencies:** AWS-TASK-1 (RDS running)

---

#### AWS-TASK-7: CI/CD Pipeline

**Description:** Set up GitHub Actions workflow that builds Docker images, pushes to ECR, and deploys to EC2 via SSH. Zero-touch deployment from push to production.

**Acceptance Criteria:**
- [ ] `.github/workflows/deploy.yml`: triggers on push to `main` branch
- [ ] Build step: `./gradlew build -x test` (tests run in a separate CI step)
- [ ] Docker build: builds all 3 service images using their Dockerfiles
- [ ] ECR push: authenticates, tags `:latest` and `:{sha}`, pushes
- [ ] EC2 deploy: SSH into EC2, login to ECR, `docker compose pull && docker compose up -d`
- [ ] Secrets configured: `AWS_ACCOUNT_ID`, `EC2_HOST`, `EC2_SSH_KEY`, `DB_USERNAME`, `DB_PASSWORD`
- [ ] Health check step: `curl http://$EC2_HOST/health` returns 200 after deploy

**Technical Notes:**
- Use `appleboy/ssh-action` for EC2 SSH — simple, proven
- ECR authentication: `aws ecr get-login-password | docker login ...`
- Docker Compose file (docker-compose.aws.yml) references ECR images via `${ECR_REGISTRY}/service:latest`
- Rollback: if health check fails, SSH in and run `docker compose up -d` with previous images
- GitHub Actions free tier: 2,000 min/mo — plenty for this

**Effort:** 2 files (workflow + env template) · **Dependencies:** AWS-TASK-1 through AWS-TASK-6

---

#### AWS-TASK-8: LocalStack Dev Profile + Documentation

**Description:** Add LocalStack profile for local SQS development (no real AWS needed). Update architecture docs to reflect the new AWS-native architecture.

**Acceptance Criteria:**
- [ ] `docker-compose.localstack.yml` — runs LocalStack with SQS, creates queues on startup
- [ ] `application-dev.yml` or `application-localstack.yml` — `spring.cloud.aws.endpoint-override: http://localhost:4566`
- [ ] `docs/architecture.md` updated: AWS deployment topology diagram, SQS replaces Kafka, Cognito replaces Keycloak, RDS replaces container Postgres
- [ ] `docs/functional-map.md` updated: update communication section, auth section, infrastructure section
- [ ] `docs/backlog.md` — Sprint 10 marked complete after verification

**Technical Notes:**
- LocalStack free tier covers SQS — no license needed
- Queue order in compose: wait for LocalStack healthy, then create queues via `aws sqs create-queue`
- Use `test` credentials in LocalStack profile (no real AWS auth)
- Docker compose: `services: localstack: { image: localstack/localstack, ports: ["4566:4566"] }`

**Effort:** 3 files · **Dependencies:** AWS-TASK-3 (SQS code deployed)

---

## Backlog (ordered)

### Technical Debt / Audit Findings

Items from `docs/audit.md` — small-effort fixes that should be picked up early.

- Rename `keycloak_group_id` to `keycloak_group_id_refs` (audit finding — naming)

### Hermandad Tests

- Add Testcontainers integration test for outbox → Kafka flow (EmbeddedKafka)

### Shared Library

- Add unit tests for `JwtMembershipExtractor` (valid JSON claim, null claim, malformed JSON)
- Add unit tests for `TenantContextFilter` (header present, header absent, cleanup on exit)

### Procesión Service

- ✅ ~~Model `Procesion` aggregate with DDD + TDD~~
- `Recorrido` value object: ordered list of waypoints with timestamps
- ✅ ~~CRUD REST endpoints for processions~~
- ✅ ~~Assign hermandad to procession~~
- ✅ ~~Publish events: `ProcesionCreated`, `ProcesionStatusChanged`~~
- Kafka consumer for hermandad events (react when hermandad is modified)
- ✅ ~~Docker Compose integration (procesion-db, service registration)~~
- ✅ ~~Outbox pattern for event publishing~~
- `@PreAuthorize` method-level security guards

### Repertorio Service

*(Items below tracked in Sprint 9 above. Kept here for visibility.)*

- ✅ Model `Marcha` aggregate: title, composer, `BandType` enum, durationSeconds, compositionYear, youtubeUrl
- ✅ Model `Cruceta` + `CrucetaItem`: ordered marcha list per procesionId
- CRUD REST endpoints for global marcha catalog
- Cruceta management by procesionId (get + replace)
- Outbox pattern: `MarchaAdded`, `MarchaRemoved`, `CrucetaDefined` → Kafka
- Docker Compose + Gateway route integration

### Tracking Service

- GPS position model (hermandadId, lat, lon, timestamp)
- Ingestion endpoint for GPS data
- Last known position endpoint
- Kafka consumer for procesion events (start/stop tracking)
- Docker Compose integration

### Notification Service

- Kafka consumer for all domain events
- Notification dispatch (placeholder — could be email, push, or just logs)
- Docker Compose integration

### Cross-cutting / Infrastructure

- Idempotent Kafka consumer pattern (one service as reference, then apply everywhere)
- Structured logging with correlation IDs across services
- Circuit breaker with Resilience4j
- Distributed tracing with Zipkin
- API Gateway route configuration for all services (currently points to non-existent repertorio/procesion/tracking/notification services)
- Integration test suite (end-to-end flows across services)
- Rate limiting on API Gateway
- DB migration strategy (Flyway already in use, extend to all services)
- Extend `TenantIdInjectionFilter` to handle all service URL patterns, not just `/api/hermandades/{id}/...`
- Replace `kafka-init` no-op healthcheck with a real readiness check (wait until topics are listed)
- Add Docker Compose services for repertorio-service, procesion-service, tracking-service, notification-service (currently missing from docker-compose.yml)
- Upgrade to Spring Boot 3.5.x (completed)
- Migrate to Spring Boot 4.x (cross-cutting)

### Learning / Study

- SOLID principles applied to the codebase (review each service)
- Design pattern identification (Strategy, Factory, Observer — map to concrete usage)
- System design practice: talk through one service's architecture out loud

---

## Done

- **Sprint 5 — Hermandad Polish + Idempotent Consumer** ✅
  - `MemberAddedEvent.hermandadId` field + test assertion
  - MockMvc tests (409/404/400 for controller, all handlers for GlobalExceptionHandler)
  - Idempotent Kafka consumer reference pattern (`processed_event` table, entity, consumer, tests)
- **Sprint 4 — Hermandad Service Hardening** ✅
- Project skeleton: Gradle multi-project, Docker Compose, shared library
- Infrastructure: API Gateway, Discovery Server, Keycloak realm
- Hermandad model: `Hermandad`, `HermandadMember`, `HermandadRole`
- Hermandad CRUD: create, find by id, list members
- Hexagonal Architecture refactor (ports & adapters)
- Outbox pattern implementation (table, publisher, poller to Kafka)
- Redis caching for hermandad and members
- Keycloak integration: group membership sync via events
- Architecture decision: `HermandadMember` as separate aggregate (not child of `Hermandad`)
- DDD enrichment: `HermandadMember.changeRole()` with no-op invariant
- Architecture document: `docs/architecture.md`
- DomainEvent interface + DomainEventPublisher port + adapter wrapping OutboxPublisher and ApplicationEventPublisher
- All domain events implement `DomainEvent` (aggregateType, aggregateId, eventType)
- `HermandadService` refactored to depend solely on `DomainEventPublisher`
- `OutboxEventPublisher` updated to match new `OutboxPublisher` interface using `DomainEvent`
- Story 4 (change role): full endpoint + exception handlers + domain events + tests
- Story 5 (MemberRoleChangedEvent): published via DomainEventPublisher on role change
- `ChangeRoleRequest` DTO, `GlobalExceptionHandler` updated for 404/400
- `DomainEventPublisherAdapter` wiring `OutboxPublisher` + `ApplicationEventPublisher`
- Spring Boot upgraded from 3.3.5 → 3.5.15, Spring Cloud 2023.0.3 → 2024.0.1
- springdoc-openapi `2.7.0` + Swagger UI integrated, gateway routes + security permits for `/v3/api-docs`, `/swagger-ui/**`
- Missing `hermandad-member-events` Kafka topic added to `kafka-init`
