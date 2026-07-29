# ⚠️ SUPERSEDED — Sprint 10 — Route-Aware Cruceta

> **This plan is superseded by the active [`2026-07-28-cruceta-first-high-throughput-roadmap.md`](2026-07-28-cruceta-first-high-throughput-roadmap.md).**
>
> **Why superseded:** It assumes one Cruceta per Procesion and RoutePoint terminology, before the Titular/Paso/Route Section model was clarified. The active plan uses Route Sections, per-Paso Crucetas, and plan finalization.
>
> Retained as historical context only. Do not implement against this document.

## Product Goal

Turn the existing Cruceta from an ordered, procession-specific setlist into a route-aware operational plan: a capataz can see which marcha is intended for each meaningful moment of a procession.

The sprint proves the product's central value without introducing GPS, maps, public tracking, or notifications.

## Sprint Outcome

An authorized Hermandad operator can:

1. Define an ordered route using named points such as *Salida*, *Calle Sierpes*, and *Recogida*.
2. Assign a marcha in the Cruceta to a route point.
3. Read a route-ordered run sheet with the current and next planned marcha.

## Scope and Boundaries

### Included

- Correct authorization for Procesion and Cruceta operations.
- Named, ordered route points owned by a Procesion.
- Kafka replication of route points to Repertorio's local projection.
- Route-point assignment for Cruceta items.
- A read-only run-sheet endpoint.
- Automated tests and an end-to-end demo update.

### Explicitly Deferred

- Coordinates, map rendering, routing-provider integration, or GPS triggers.
- Automatic selection of the current route point or marcha.
- Public sharing, tracking, push notifications, and band-facing collaboration features.
- Event envelope versioning, stable producer-generated event IDs, retries, and dead-letter queues. These remain Phase 3 work.

## Product and Authorization Decisions

### Route ownership

`Procesion` owns its route. A route is not a new service or aggregate in this sprint; it is an ordered collection of route points belonging to one procession.

`Repertorio` owns the Cruceta. It consumes a local, read-only projection of route points so it can validate and display a musical plan without synchronous calls to Procesion.

### Roles

| Capability | HERMANDAD_ADMIN | CAPATAZ | Other authenticated user |
|---|---:|---:|---:|
| Create/list a procession for its Hermandad | Yes | Yes | No |
| Change procession status | Yes | Yes | No |
| Define or change route points | Yes | Yes | No |
| Define or change Cruceta | Yes | Yes | No |
| Read the operational run sheet | Yes | Yes | No |

The authorization decision must use the persisted Hermandad ID of the target procession or route point. A path parameter and a client-provided Hermandad ID are never sufficient proof of ownership.

## Domain Model

### Procesion service

`RoutePoint` contains:

- stable ID
- procession ID
- display name
- ordered position
- optional operator notes

The route is valid only when point names are non-blank and positions are unique. Coordinates and estimated times are deliberately absent.

### Repertorio service

`KnownRoutePoint` is a local projection containing the route point ID, procession ID, Hermandad ID, name, and position.

`CrucetaItem` gains a required `routePointId`. Repertorio must validate that the referenced point belongs to the same known procession as the Cruceta.

### Route-change rule for this MVP

A route cannot be replaced while a Cruceta already references its points. Return a clear conflict and require the operator to revise the musical plan first. This protects the meaning of an existing plan and avoids silent reassignment or deletion of musical cues.

## API Contract to Define First

Update `docs/openapi.yaml` before implementation with these operations and error cases:

| Operation | Intent |
|---|---|
| `PUT /api/procesiones/{procesionId}/route` | Define or replace the ordered named route points before a Cruceta exists. |
| `GET /api/procesiones/{procesionId}/route` | Read the route points in order. |
| `GET /api/hermandades/{hermandadId}/procesiones/{procesionId}/cruceta/run-sheet` | Read route point, assigned marcha, notes, and current/next plan. |

Extend the existing Cruceta request/response schemas so each item carries `routePointId`. The contract must document `401`, `403`, `404`, and `409` responses where relevant.

## Event and Consistency Decision

Procesion publishes `ProcesionRouteDefinedEvent` through the existing transactional outbox. The event contains the procession ID, Hermandad ID, and full ordered route-point list.

Repertorio consumes this event from `procesion-events` and maintains `KnownRoutePoint` records. Therefore, immediately after defining a route there can be a short, intentional delay before Cruceta editing is available. The API must return an intentional response until the local projection is ready; it must not make a synchronous call to Procesion.

## BDD Scenarios

### Feature: secure procession operations

```gherkin
Scenario: a capataz manages its Hermandad's procession
  Given the user has CAPATAZ membership for Hermandad A
  And a procession belongs to Hermandad A
  When the user defines its route or changes its status
  Then the operation succeeds

Scenario: a user cannot manage another Hermandad's procession
  Given the user has ADMIN membership for Hermandad A
  And a procession belongs to Hermandad B
  When the user attempts to read or mutate that procession
  Then the response is 403 Forbidden
  And no state changes
```

### Feature: define a route

```gherkin
Scenario: define a named ordered route
  Given an authorized operator and a planned procession
  When they submit the route points Salida, Calle Sierpes, and Recogida
  Then the route is stored in that order
  And Procesion publishes ProcesionRouteDefinedEvent through its outbox

Scenario: reject an invalid route
  Given an authorized operator
  When they submit blank names or duplicate positions
  Then the response is 400 Bad Request
  And no route event is published

Scenario: preserve existing musical cues
  Given a procession already has a Cruceta that references its route points
  When an operator attempts to replace the route
  Then the response is 409 Conflict
  And the existing route and Cruceta remain unchanged
```

### Feature: route-aware Cruceta

```gherkin
Scenario: repertorio learns a route asynchronously
  Given Procesion has published a valid route-defined event
  When Repertorio consumes the event
  Then it stores the corresponding KnownRoutePoint records
  And every record belongs to the event's procession and Hermandad

Scenario: define a route-aware Cruceta
  Given Repertorio knows a procession and its route points
  And the user is ADMIN or CAPATAZ for that procession's Hermandad
  When the user assigns a marcha to each selected route point
  Then the Cruceta is stored
  And every item references a route point of that procession

Scenario: reject a cross-tenant Cruceta change
  Given the user is ADMIN for Hermandad A
  And the requested procession belongs to Hermandad B
  When the user defines or replaces its Cruceta using Hermandad A in the path
  Then the response is 403 Forbidden
  And no Cruceta is created or changed

Scenario: reject a route point from a different procession
  Given a valid procession and its known route points
  And a route point belonging to another procession
  When the user includes the other procession's route point in a Cruceta
  Then the response is 400 Bad Request
  And no Cruceta is created or changed
```

### Feature: run sheet

```gherkin
Scenario: view an operational music plan
  Given an authorized operator and a route-aware Cruceta
  When they request the run sheet
  Then items are ordered by route progression
  And each item shows its route point, marcha, and notes
  And the response clearly identifies the next planned item
```

## Delivery Sequence

1. Define the OpenAPI contract and add authorization/tenant-denial tests first.
2. Implement persisted-Hermandad authorization in Procesion and Cruceta flows.
3. Add route points, route validation, and Procesion outbox event publication.
4. Add Repertorio's local route-point projection and event-consumer tests.
5. Extend Cruceta validation and persistence with `routePointId`.
6. Expose the run sheet.
7. Add controller, service/domain, Kafka-consumer, and Testcontainers integration coverage.
8. Update `docs/demo/phase-1.sh`, the functional map, service reviews, backlog, and roadmap.

## Definition of Done

- All BDD scenarios are covered by automated tests at the appropriate level.
- The gateway-only demo proves procession creation, route definition, eventual projection, route-aware Cruceta creation, and run-sheet retrieval.
- Cross-Hermandad access is denied for both Procesion and Repertorio paths.
- Route and Cruceta data remain unchanged after invalid or unauthorized requests.
- The documented eventual-consistency delay is visible and understandable to an operator.
- No Tracking or Notification service code is added.
