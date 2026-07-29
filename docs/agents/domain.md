# Domain Glossary — Canonical Target Model

> **Purpose:** Definitions in domain language only — no implementation details, no adapter concerns, no endpoint paths, no projections.
> **Status:** TARGET design. See `docs/functional-map.md` §0.10 for each concept's AS-IS implementation status.

---

## Core Concepts

### Hermandad
A brotherhood that owns processions, titulares, and members. The top-level organizational boundary for tenant isolation. A Hermandad creates and manages its own processions, titulares, and members. Cross-Hermandad access is always denied.

### Titular
A religious image or icon owned by a Hermandad. A Hermandad may have multiple titulares. A Titular is referenced by exactly one Paso in a given Procesion.

**Invariant:** A Titular belongs to exactly one Hermandad.

---

## Procesion

### Procesion
A planned event for a Hermandad on a specific date and time. A Procesion owns ordered Pasos and a shared ordered Route. The plan is finalized together with its Pasos and Route before Cruceta preparation begins.

**Invariant:** A Procesion belongs to exactly one Hermandad.

### Paso
A float or platform within a Procesion. Each Paso references exactly one Titular. A Procesion contains one or more ordered Pasos. Each Paso tracks its Cruceta progression independently because Pasos are physically separated during the procession.

**Invariant:** Each Paso has a unique position within its Procesion.
**Invariant:** Each Paso references exactly one Titular that belongs to the same Hermandad as the Procesion.

### Route
A shared ordered sequence of Route Sections for a Procesion. All Pasos in the same Procesion follow the same Route. The Route is finalized alongside the Pasos and is immutable after finalization for this MVP.

### Route Section
An ordered occurrence within a Route — usually a street, intersection, or meaningful stretch. Route Section names may repeat because outbound and return may use the same street.

**Invariant:** A Route Section has a unique position within its Route.
**Invariant:** Names are not required to be unique — outbound "Calle Sierpes" and return "Calle Sierpes" are distinct sections.
**Invariant:** Route Sections are shared by all Pasos in the Procesion.

### Plan Finalization
The explicit, idempotent command that makes a Procesion's Pasos and Route immutable. After finalization:
- Pasos and Route cannot be modified.
- Cruceta preparation begins using the finalized plan snapshot.

**Invariant:** A Procesion must have at least one Paso and at least one Route Section to be finalized.
**Invariant:** Finalization is idempotent — the same command repeated produces the same state.
**Invariant:** Rain/emergency route amendment after finalization is deferred to the future backlog.

---

## Musical Plan

### Marcha
A musical piece in the Repertorio catalogue. Marchas are owned by Repertorio and belong to no Hermandad — they are a global, shared catalogue.

### Cruceta
A musical plan for exactly one Paso within a Procesion. There is exactly one Cruceta per Paso (not one per Procesion). A Cruceta assigns zero or more ordered Marchas to each Route Section.

**Invariant:** A Cruceta belongs to exactly one Paso.
**Invariant:** A Cruceta references the Procesion's finalized Route Sections.

### Cruceta Item
An assignment of one Marcha to one Route Section within a Cruceta. A Route Section may have no Marchas; one Section may have several Marchas, ordered by `sequenceWithinSection`. A Marcha may recur in different Sections.

**Invariant:** Each Cruceta Item has a Route Section ID, a Marcha ID, and a `sequenceWithinSection`.
**Invariant:** The full display order derives from Route Section position then `sequenceWithinSection`.
**Invariant:** Zero or more items per Route Section.

### Run Sheet
The operational view of a Cruceta ordered by route progression. It shows the current (and next) item for a given Paso. Each Paso tracks its progression independently because Pasos are physically separated.

**Invariant:** The run sheet is read-only.
**Invariant:** Current/next progression is independent per Paso.
**Invariant:** A Cruceta replacement resets progression.

---

## References

- **TARGET domain detail (AS-IS vs TARGET status table):** `docs/functional-map.md` §0.10
- **Architecture (context ownership):** `docs/architecture.md` §Target Domain
- **Active implementation plan:** `docs/plans/2026-07-28-cruceta-first-high-throughput-roadmap.md`
- **Product roadmap:** `docs/roadmap.md`
