# Repertorio — Semana Santa Management API

[![CI](https://github.com/emilio-code7/semana-santa-app/actions/workflows/ci.yml/badge.svg)](https://github.com/emilio-code7/semana-santa-app/actions/workflows/ci.yml)

REST API for managing Semana Santa (Holy Week) brotherhoods, processions, and musical repertoire. Built on Spring Boot 4.1 with hexagonal architecture, event-driven communication, and containerized deployment.

**Portfolio goals:** demonstrable distributed system — event-driven microservices, outbox pattern, Kafka-based eventual consistency, hexagonal + DDD, CI/CD-ready.

---

## Architecture

```
hermandad-service :8081    Procesion CRUD + member management, Keycloak admin sync
procesion-service :8082    Procession CRUD + state machine (PLANNED → IN_PROGRESS → COMPLETED)
repertorio-service :8083   Global marcha catalog + cruceta (ordered playlist per procession)
api-gateway         :8080   Spring Cloud Gateway — routes by path prefix
discovery-server    :8761   Eureka service registry
```

**Communication:** Services communicate asynchronously via Apache Kafka using the outbox pattern (DB-level reliability + poller → Kafka). No synchronous REST calls between services.

**Auth:** JWT-based via Keycloak (OAuth2 resource server). Custom `hermandad_memberships` claim encodes per-hermandad role assignments.

**Data:** Each service has its own PostgreSQL database with Flyway migrations. Cache via Redis (hermandad-service). Full stack defined in Docker Compose.

---

## Quick Start

```bash
docker compose up -d        # Starts full stack (core profile)
```

Wait ~30 seconds for services to register. Then:

```bash
# Run the end-to-end demo
./docs/demo/phase-1.sh
```

The demo creates a hermandad → procesion → cruceta → status change, validated against Kafka events.

### Seeded Credentials

| User | Role |
|------|------|
| `qa-admin-user` / `test` | Pre-configured admin on the seeded hermandad |

Get a token: `curl -s -X POST http://localhost:8180/realms/semana-santa/protocol/openid-connect/token -d "grant_type=password&client_id=semana-santa-client&client_secret=secret&username=qa-admin-user&password=test"`

### APIs

| Base URL | Docs |
|----------|------|
| `http://localhost:8080/api/hermandades` | `/v3/api-docs/hermandad` |
| `http://localhost:8080/api/procesiones` | `/v3/api-docs/procesion` |
| `http://localhost:8080/api/marchas` | `/v3/api-docs/repertorio` |

Swagger UI at `http://localhost:8080/swagger-ui.html`

---

## Project Map

| Path | Contents |
|------|----------|
| `docs/` | Architecture, audit, backlog, functional map, service reviews |
| `docs/plans/` | Implementation plans by sprint |
| `docs/demo/` | Runnable demo scripts |
| `docs/roadmap.md` | Development phases and portfolio milestones |
| `docs/openapi.yaml` | Complete API spec (OpenAPI 3.0) |
| `docs/functional-map.md` | Full topology, endpoints, DB schemas, test inventory — 960 lines of agent-ready context |
| `docs/architecture.md` | Hexagonal + DDD design decisions |
| `infrastructure/` | API Gateway, Discovery Server, Keycloak realm export, nginx config |
| `shared/common/` | Cross-cutting: `IntegrationTestBase`, `JwtMembershipExtractor`, `TenantContext` |
| `.github/workflows/` | CI/CD pipeline (build → test → ECR → EC2 deploy) |

---

## Testing

```bash
./gradlew test                       # All tests (~185)
./gradlew :services:hermandad-service:test   # 50 tests
./gradlew :services:procesion-service:test   # 47 tests
./gradlew :services:repertorio-service:test  # 76 tests
```

Integration tests use Testcontainers (PostgreSQL), skip gracefully if unavailable.

---

## Architecture Decisions (key ones)

| Decision | Rationale |
|----------|-----------|
| **Hexagonal + DDD** | Domain purity, testability, framework independence — domain layer has zero Spring imports |
| **Outbox pattern** | Guarantees at-least-once delivery to Kafka without distributed transactions |
| **Kafka-based cross-service validation** | Repertorio validates cruceta references against a local `KnownProcesion` cache populated from `procesion-events` topic. Eventual consistency, no synchronous coupling |
| **Per-service database** | Independent Flyway histories, no migration conflicts, clean service boundaries |
| **Spring Cloud Gateway** | Centralized routing, auth filtering, API docs aggregation — replaces per-service URL management |

---

## Deployment

Current: Docker Compose on single host.

Planned (Sprint 10): AWS-native — SQS replaces Kafka, Cognito replaces Keycloak, RDS replaces container Postgres. Infra-as-code via CDK at `infrastructure/aws/`.

Future: K8s with Helm charts ($5/mo Civo cluster).

---

## Roadmap

See `docs/roadmap.md` for the full 5-phase plan:

1. ✅ **Operational Procesion** — cross-service workflow (done)
2. 🔜 **Reliable Event Delivery** — event envelope, retry, dead-letter topics
3. **Observable System** — traces, metrics, Grafana
4. **Contract Maturity** — evolvable APIs, contract tests
5. **Portfolio Readiness** — README, demo script, interview stories
