---
name: frontend-workflow
description: Frontend-only workflow for browser, UI, component, and page changes.
---

# Frontend Workflow

Use this skill when implementing frontend changes — browser, UI, components, pages.

## Reference

- `frontend/package.json` — current dependencies and versions
- `docs/openapi.yaml` — API contract for backend endpoints
- `frontend/src/components/ui/` — existing shadcn/ui components (extend, don't rebuild)

## Patterns

- **Server Components by default** — only add `'use client'` for interactivity (state, effects, event handlers)
- **Data fetching**: Server Components use `fetch()` directly. Client Components use `useQuery`/`useMutation` from TanStack React Query
- **API URL**: Use `NEXT_PUBLIC_API_URL` env variable — never hardcode `localhost:8080`
- **Types**: Every API response needs a type in `src/types/`. No `any` in production code.
- **Three states**: Every data display must handle loading (`loading.tsx`/Suspense), error (`error.tsx`/ErrorBoundary), and empty (meaningful empty state)
- **Auth**: Use `auth()` in server components, `useSession()`/`session.accessToken` in client components
- **UI components**: Use existing shadcn/ui components — don't build custom unless necessary

## Workflow

1. **Explore** — Use graph MCP tools first (`semantic_search_nodes_tool`, `query_graph_tool`, `get_architecture_overview_tool`). Fall back to `@explorer` for 4+ files, complex flows, or to targeted Grep/Glob/Read when graph coverage is insufficient.
2. **Read the spec** — `docs/openapi.yaml` for endpoint shapes, `docs/functional-map.md` for service topology.
3. **Design** — For non-trivial visual/UX changes, delegate to `@designer`. Designer owns: layout, spacing, hierarchy, motion, color, affordances, responsive behavior. Orchestrator may only edit copy after design.
4. **Implement** — Mechanical changes (API wiring, data fetching, types) → `@fixer`. Visual/UX changes → always `@designer`.
5. **Review** — Check mobile (375px) and desktop (1440px), loading/error/empty states handled, no hardcoded API URLs, auth works (logged in vs logged out).
6. **Verify** — `npm run build` succeeds, no TypeScript errors, no hardcoded `localhost:8080`.
7. **Store** — Memory tool for frontend patterns discovered and gotchas.
8. **Commit** — Conventional commit: `feat(frontend):`, `fix(frontend):`, `style(frontend):`.

## Simple Tasks

Simple operational tasks (commit, status check, single command, single known-file edit) are governed by AGENTS.md — delegate directly to `@fixer` without this workflow.
