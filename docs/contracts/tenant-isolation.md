# Tenant Isolation Contract

Tenant-isolation invariants per service, verified live on the dev stack (issue #59). Roadmap Ticket 09 enforces the procesion TARGET state.

## 1. Core invariants

- **Reads** require membership in the owning hermandad.
- **Writes** additionally require the approved role:
  - Procesion and repertorio hermandad-scoped writes: `CAPATAZ` or `HERMANDAD_ADMIN`.
  - Hermandad member management: `HERMANDAD_ADMIN` only.
- **Public endpoints are GET-only and explicitly listed**: `GET /api/hermandades`, `GET /api/hermandades/{id}`. Everything else requires a JWT (`anyRequest().authenticated()`).

## 2. Authorization model

1. A custom `JwtAuthenticationConverter` maps the `hermandad_memberships` JWT claim to Spring authorities of the form `HERMANDAD_{hermandadId}_{role}`.
2. `@PreAuthorize` on endpoints, backed by a per-service security bean:
   - Hermandad: `HermandadSecurityService.isAdmin`
   - Repertorio: `RepertorioSecurityService.isAdmin`
3. Each security bean uses a **JWT fast path** (check the claim-derived authority) with a **DB fallback** (query persisted membership).

## 3. AS-IS status per service (verified)

| Service | Status | Details |
|---------|--------|---------|
| Hermandad | ✅ Enforced | `@PreAuthorize` active on member-management endpoints; public `GET /api/hermandades` and `GET /api/hermandades/{id}`. |
| Procesion | ⚠️ Partial | `@EnableMethodSecurity` is declared but **inactive**. A service-level ownership check returns 403 for **cross-tenant** requests (path hermandad != procesion's hermandad, e.g. admin token + hermandad-B path), but a no-membership user on its **own** path does NOT get 403 (200 for GET, 409 for PUT after finalize). |
| Repertorio | ✅ Enforced (cruceta) | Cruceta endpoints (`GET`/`PUT` cruceta, run-sheet, current) are claim-guarded via `@PreAuthorize(repertorioSecurity.isAdmin)` and active — no-membership user → 403. |
| Repertorio marchas | Authenticated only | `GET/POST/PUT/DELETE /api/marchas*` are a global catalog, not hermandad-scoped; only `authenticated()` applies. |

## 4. TARGET (Ticket 09)

Every procesion endpoint enforces **persisted** tenant ownership:

- Reads = membership in the owning hermandad.
- `POST` / `PATCH` / `DELETE` additionally require `CAPATAZ` or `HERMANDAD_ADMIN`.
- Ownership comes from a DB lookup for the procesion's `hermandadId`; the JWT fast path alone is not sufficient.

See Ticket 09 in `docs/plans/2026-07-28-cruceta-first-high-throughput-roadmap.md`.

## 5. Testable assertion matrix

For each endpoint class below, the following must hold:

- Unauthenticated → **401**
- Cross-tenant (valid token, membership in a different hermandad) → **403**
- Wrong role (e.g. `MUSICIAN` in the owning hermandad) → **403**
- No membership in any hermandad → **403 where enforced** (see section 6)

| Endpoint class | 401 | Cross-tenant 403 | Wrong-role 403 | No-membership 403 |
|----------------|-----|------------------|----------------|-------------------|
| Hermandad member CRUD (`/api/hermandades/{id}/members*`) | ✅ | ✅ | ✅ | ✅ |
| Procesion CRUD/status (`/api/procesiones*`) | ✅ | ✅ | ✅ | ⚠️ TARGET only (Ticket 09) |
| Procesion pasos/route/finalize (`/api/hermandades/{hid}/procesiones/{pid}/...`) | ✅ | ✅ | ✅ | ⚠️ TARGET only (Ticket 09) |
| Repertorio cruceta / run-sheet / current | ✅ | ✅ | ✅ | ✅ |
| Repertorio marchas (global catalog) | ✅ | n/a (not scoped) | n/a | n/a (not scoped) |

## 6. Measurement protocol with harness gotchas

- Harness **403 assertions for procesion must use cross-tenant (hermandad-B) paths**; a no-membership user on its own path does not get 403 today.
- No-membership 403 assertions are only valid on **repertorio cruceta paths**.
- `docs/demo/cruceta-product-flow.sh` is the reference harness: it asserts 403 on cruceta for a no-membership user and 403 on a cross-tenant write.
- Ticket 09 flips the procesion rows in the matrix above from "TARGET only" to enforced.

## 7. Testable assertions (summary)

1. Unauthenticated request to any non-public endpoint → 401.
2. Valid token for hermandad B on hermandad A's resource → 403 on every hermandad-scoped endpoint.
3. `MUSICIAN` role in the owning hermandad → 403 on write endpoints.
4. No-membership user → 403 on hermandad member CRUD and repertorio cruceta endpoints today; on all procesion endpoints after Ticket 09.
5. `GET /api/hermandades` and `GET /api/hermandades/{id}` remain public and GET-only.
6. Public endpoints are exactly the two listed; no new `permitAll()` on write endpoints.
