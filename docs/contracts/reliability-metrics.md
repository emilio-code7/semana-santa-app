# Reliability Metrics Contract

Target metrics and measurement protocol that subsequent roadmap tickets verify against. Every metric below is measurable with the commands in section 3; no metric is defined that cannot be measured.

## 1. Delivery contract summary

- **At-least-once** delivery. Duplicate delivery after crash is expected and absorbed by consumer idempotency.
- **Producer-generated `eventId`** (`UUID.randomUUID()`) is the envelope identity; consumer dedup migrates to it in Tickets 12-14 (see `event-envelope.md`).
- **Idempotent consumers** (dedup via the `processed_event` table today).
- **No exactly-once claims** anywhere in the documentation.

## 2. Target metrics

| Metric | Definition | Current nominal value | Measured by |
|--------|------------|-----------------------|-------------|
| Outbox backlog | Count of `outbox_event` rows with `processed = false` | 0 at steady state | SQL (section 3.1) |
| Oldest-unprocessed age | Age of the oldest `processed = false` row, from `created_at` | < 5 s + processing time | SQL (section 3.1) |
| Poll interval | Outbox poller cadence | 5 s (`@Scheduled`) | Poller log lines |
| Publish failures / retries | Sends that fail and are retried on the next poll | 0 sustained; a failed row stays `processed = false` and is retried | Poller logs; `processed_at` gap |
| Consumer dedup skips | Rows skipped by the idempotency check (`processed_event` exists) | Expected on redelivery only | Consumer `duplicate skipped` logs |
| Terminal / DLQ rows | Rows that exhausted retries and reached a terminal state | n/a today; Ticket 16 adds retry/terminal columns | Ticket 16 schema |
| Event end-to-end freshness | Time from `occurredAt` to consumer-side `processed_at` | n/a (not yet instrumented) | End-to-end harness logs |

## 3. Measurement protocol

### 3.1 Outbox backlog and oldest age

Copy-pasteable SQL against any service database (`hermandad_db`, `procesion_db`, `repertorio_db`):

```sql
-- Backlog: how many rows are still unprocessed?
SELECT count(*) FROM outbox_event WHERE processed = false;

-- Oldest unprocessed age (seconds), from created_at
SELECT extract(epoch FROM (now() - created_at)) AS age_seconds, aggregate_type, event_type
FROM outbox_event
WHERE processed = false
ORDER BY created_at ASC
LIMIT 5;
```

The poller's bounded query is `findTop100ByProcessedFalseOrderByCreatedAtAsc` — a backlog above ~100 rows indicates the poller is falling behind or sends are failing.

### 3.2 Kafka consumer lag

Monitor consumer lag for `procesion-events` (repertorio consumer) and the hermandad self-consumption topics. In the dev stack, Kafka UI (port 8086) exposes per-partition lag; `kafka-consumer-groups.sh --describe --group <groupId>` is the CLI equivalent.

### 3.3 End-to-end evidence harness

The repeatable end-to-end evidence path is:

- `docs/demo/cruceta-product-flow.sh` — Month-1 cruceta e2e with real tokens and full flow
- `docs/demo/phase-1.sh` — cross-service workflow demo

These scripts are the harness for freshness and delivery assertions: after each flow, outbox backlog must return to 0 and consumers must have recorded the events.

## 4. Concurrency contract (verified by Ticket 11)

Optimistic-lock / `@Version` propagation is an adapter concern. The contract:

- A stale or concurrent write must surface as **409 Conflict**, never a 5xx.
- Repertorio's `GlobalExceptionHandler` maps `VersionMismatchException`, `DataIntegrityViolationException`, and `ObjectOptimisticLockingFailureException` → 409.
- Race assertions must follow the established pattern: **"never 5xx, any loser is 409, state consistent"** — NOT "exactly one 409". The legal last-write-wins interleaving allows both writers to succeed; only "no 5xx and state consistent" is deterministic.

## 5. Spec-integrity guard (OpenAPI)

The OpenAPI spec is the contract and is guarded on every push:

- Redocly CLI is pinned: CI runs `npx --yes @redocly/cli@2.41.0 lint docs/openapi.yaml`.
- Current baseline on `main`: **exit 0 with exactly 3 warnings** — `info-license`, `no-server-example.com`, and `operation-4xx-response` on `GET /api/marchas`. New warnings are not allowed.
- **Duplicate path keys cannot pass silently**: Redocly's parser hard-fails at parse time with `duplicated mapping key` when any mapping key (including a path key) appears twice.
- `git diff --check` must be clean.

## 6. Testable assertions

1. After any end-to-end demo run, `SELECT count(*) FROM outbox_event WHERE processed = false` returns 0 within one poll interval.
2. The oldest-unprocessed age never exceeds poll interval + processing time at steady state.
3. A failed publish leaves the row `processed = false`; the next poll retries it (no silent drop).
4. A duplicate delivery is skipped by the consumer and logged as a dedup skip, never re-processed.
5. A concurrent/stale write to the same aggregate returns 409; no 5xx is ever observed; final state is consistent (loser's write not partially applied).
6. `npx --yes @redocly/cli@2.41.0 lint docs/openapi.yaml` exits 0 with no more than the 3 baseline warnings.
7. `git diff --check` is clean.
