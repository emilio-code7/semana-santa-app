# Repertorio — Agent Guide

## Identity

Semana Santa management system: 3 Spring Boot microservices (hermandad, procesion, repertorio), event-driven via Kafka outbox, JWT auth via Keycloak, hexagonal + DDD.

## First Thing

Read `docs/functional-map.md` before any implementation work — it's your complete reference: topology, profiles, endpoints, DB schemas, test inventory, operating principles. This file is the pointer. That file is the truth.

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

## Verification Gate

Before declaring any task complete:
1. `./gradlew :services:<affected-service>:test` — all tests pass
2. `./gradlew :services:<affected-service>:compileJava` — build succeeds
3. No `FIXME`, `TODO`, or `HACK` markers in new/modified files
4. Acceptance criteria from the plan are met

## Commit Messages

Format: `type(scope): description`
Types: `feat:`, `fix:`, `refactor:`, `test:`, `docs:`, `chore:`
One concern per commit. Check `git diff` before committing.
