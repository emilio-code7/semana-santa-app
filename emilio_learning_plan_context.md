# Context: Backend Engineer Interview Prep Plan

## Who I am

I am a full stack backend-focused engineer with 7 years of professional experience. My primary stack is **Java 21, Spring Boot, and Apache Kafka**. I am currently between projects at my company, which gives me 4–6 hours of free time per day for self-study and project work. I also have daily company course tests that take some time each morning.

I am a native Spanish speaker working to improve my English proficiency for professional use in English-speaking tech environments. My comprehension is strong (B2 level), but real-time spoken production is my main weak point.

---

## My goal

Pass senior backend engineering interviews, with a focus on **microservices architecture**. My main challenge is that I lack direct production microservices experience, so I am building a personal project to compensate and generate real talking points for interviews.

---

## Daily time structure

| Block | Duration | Activity |
|---|---|---|
| English practice | 30 min | Shadowing, self-talk on technical topics, weekly recording |
| Concept study | 1 hour | One focused topic per week |
| Project work | 2.5–3 hours | Implementation tied to that week's concept |
| System design practice | 30–45 min | One problem, talked through out loud |
| Buffer | ~1 hour | Overflow, company tests, rest |

---

## 8-week learning plan

Each week has one concept theme that connects study and project work together.

### Week 1–2 — Foundation
- **Study:** Bounded contexts, service decomposition principles
- **Build:** Wire up all services end-to-end with Kafka, basic REST APIs, Docker Compose

### Week 3–4 — Reliability patterns
- **Study:** Outbox pattern, idempotency, Saga (choreography vs orchestration), dual-write problem
- **Build:** Implement the outbox pattern, add idempotent Kafka consumer

### Week 5 — Resilience
- **Study:** Circuit breaker, retry strategies, bulkhead pattern, graceful degradation
- **Build:** Add Resilience4j, simulate a service going down and observe behavior

### Week 6 — Observability
- **Study:** Logs, metrics, traces (the three pillars), correlation IDs, Zipkin/Jaeger
- **Build:** Structured logging with correlation IDs across services, distributed tracing with Zipkin

### Week 7 — System design interview prep
- **Study:** Non-functional requirements (scalability, availability, latency, consistency, durability), capacity estimation (back-of-envelope math)
- **Build:** Stop coding — design systems on paper daily (URL shortener, notification service, rate limiter)

### Week 8 — Interview simulation
- **Study:** Behavioral stories, "decisions I made and why" narrative from the project
- **Build:** Mock interviews, record yourself, polish the "no production experience but here's what I built" narrative

---

## The project

A personal microservices project built with Java 21 and Spring Boot, services communicating via Apache Kafka. Currently has a basic skeleton in place.

### Project goals
- Serve as a portfolio piece and source of interview war stories
- Each feature added must be motivated by the concept studied that week
- Treat it as a production system: think about failure, recovery, observability

### Target architecture (3–4 services)
To be defined, but must include:
- At least one producer and one consumer service communicating via Kafka
- A shared-nothing database approach (one DB per service)
- REST APIs exposed by at least one service
- Docker Compose for local orchestration

### Patterns to implement (in order)
1. Basic inter-service communication via Kafka
2. Outbox pattern
3. Idempotent consumer
4. Circuit breaker with Resilience4j
5. Structured logging + correlation IDs
6. Distributed tracing with Zipkin

---

## Key concepts I already know well

- Kafka internals: partitions, consumer groups, offset management, at-least-once vs exactly-once semantics
- Outbox pattern and dual-write problem (theory)
- Saga pattern: choreography vs orchestration (theory)
- Spring WebFlux: non-blocking I/O via event loop model vs Project Loom virtual threads in Java 21
- Observability tooling: Zipkin, Jaeger (conceptual)

---

## Concepts to deepen

- Non-functional requirements and how to ask for them in system design interviews
- Capacity estimation and back-of-envelope math
- CAP theorem in practice, eventual consistency trade-offs
- Resilience patterns: circuit breaker, bulkhead, retry with backoff
- Idempotency implementation patterns

---

## SOLID principles

To be studied as a bridge between weeks 2 and 3. Priority order for backend interviews:

1. **Single Responsibility** — most important; maps directly to microservices decomposition
2. **Dependency Inversion** — foundational to Spring DI; must be articulable, not just practiced
3. **Open/Closed** — extensibility without modification; relevant in Kafka consumer design
4. **Interface Segregation** — keep interfaces focused; lower priority but good to know
5. **Liskov Substitution** — least likely to come up directly, but should be explainable

---

## Design patterns

### Must know (high interview relevance for backend)
- **Strategy** — swapping behaviors at runtime; common in Spring service layers
- **Factory / Factory Method** — object creation abstraction; frequently asked
- **Builder** — used daily in Spring fluent APIs; must be articulable
- **Observer** — conceptual foundation of event-driven systems and Kafka
- **Decorator** — how Spring AOP, filters, and interceptors work under the hood

### Good to know
- **Singleton** — and specifically *why it can be problematic* (shared mutable state, testing)
- **Template Method** — how JdbcTemplate, KafkaTemplate etc. work internally
- **Circuit Breaker** — distributed systems pattern, already in the project plan

### Lower priority
- Visitor, Memento, Flyweight, Prototype — rarely come up in backend interviews

---

## Interview preparation principles

- **Always clarify requirements before designing** — ask about scale, consistency needs, latency targets, read/write ratio
- **Talk in trade-offs** — "I'd use X because... but the downside is..."
- **Have 3–4 stories ready** from the project: a decision made, a problem hit, a pattern implemented and why
- **Capacity estimation** must be practiced: 1M users × 10 req/day = ~115 req/sec style math

---

## Instructions for the AI reading this

Use this document to:

1. **Understand the project context** — help me design, extend, or debug the microservices project in a way that aligns with the weekly learning themes above.
2. **Adapt advice to my stack** — Java 21, Spring Boot, Apache Kafka, Resilience4j, Docker Compose, Zipkin.
3. **Connect implementation to concepts** — when I ask to build something, also explain the "why" and the trade-offs, since I need to be able to talk about these in interviews.
4. **Challenge me on trade-offs** — don't just give me the solution, ask me what I think first or present alternatives.
5. **Respect the weekly theme** — if I'm in week 3, keep suggestions focused on outbox/idempotency/Saga, not jumping ahead to observability.
6. **Keep English in mind** — I am a non-native English speaker improving my professional English. Feel free to correct technical vocabulary or phrasing naturally when relevant, without making it the focus.
7. **Apply design patterns only when they solve a real problem** — do NOT force patterns into the code just because they fit loosely. Only suggest a pattern when there is a concrete problem in the existing code that the pattern genuinely solves. When you do suggest one, always explain: what problem exists, what pattern solves it, and what the trade-off is. The goal is to build judgment, not pattern count. An interviewer will ask "why did you use this pattern?" — the answer must be a real reason, not "because it's good practice."
