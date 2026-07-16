# Awesome OpenCode Plugins — Relevant to Our Workflow

> Source: [awesome-opencode](https://github.com/awesome-opencode/awesome-opencode/tree/main)
> Evaluated for: spec-driven development, workflow enforcement, agent quality

---

## High Relevance

### FlowDeck
**What:** 25 specialist agents (architect, planner, coder, reviewer, tester, debugger, risk-analyst, policy-enforcer) coordinated through a 4-phase cycle: discuss → plan → execute → review. Has 24 reusable workflow skills including **SDD (Spec-Driven Development)**, 15 pre-built orchestration flows, persistent state via `.planning/STATE.md`, wave-based parallel execution, AI safety layer (patch trust scoring, edit gates, phase gating, arch constraint enforcement).

**Why relevant:** Directly matches our CLAUDE.md workflow. The SDD flow is exactly what we're trying to enforce. Persistent state means agents resume after crash. Safety layer catches the "agent cheated" problem.

**Cost:** Heavy — 25 agents, many tokens. Best for large features, not small fixes.

### GoopSpec
**What:** 5-phase spec-driven development: Plan → Research → Specify → Execute → Accept. Contract gates for user confirmation, 12 specialized subagents, persistent memory, wave-based execution with atomic commits, deviation rules for handling unexpected situations.

**Why relevant:** Lighter than FlowDeck, focused on SDD. The "contract gates" pattern matches our "present scenarios for review" step.

### CrewBee
**What:** Task-specific agent teams. Define reusable teams (e.g., "Coding Team" with coder + reviewer + tester). Switch between single-agent and multi-agent based on complexity.

**Why relevant:** Could define a "Repertorio feature team" that matches our workflow: spec-writer → implementer → reviewer → tester.

### BRHP
**What:** Persistent planning state with `/brhp` commands, bounded planner history, TUI sidebar. Structured planning that survives context wipes.

**Why relevant:** Solves "agent forgets the plan after 30 turns." Forces structured planning with commands.

---

## Medium Relevance

### Harness Memory
**What:** Auto-captures evidence from tool interactions, materializes memories through multi-gate pipeline. Claims 73% fewer tokens than CLAUDE.md. Local-first (WASM SQLite), zero cloud.

**Why relevant:** Our CLAUDE.md + functional-map is already good. This could replace the manual doc updates with automated memory capture.

### Agent Identity
**What:** Two plugins that give agents self-identity. AgentSelfIdentityPlugin injects which agent is running. AgentAttributionToolPlugin queries per-message attribution.

**Why relevant:** Useful when dispatching multiple agents — each knows its role (planner vs implementer vs reviewer).

### Dynamic Context Pruning
**What:** Prunes obsolete tool outputs from conversation context to save tokens.

**Why relevant:** We hit context issues regularly. This would prevent the "I forgot the plan" problem.

### Magic Context
**What:** Cache-aware context management with background compression. Keeps long sessions productive. Cross-session project memory.

**Why relevant:** Similar to Dynamic Context Pruning but more comprehensive.

---

## Low Relevance (interesting but not now)

- **Autotitle** — Auto-names sessions. Nice to have.
- **Command Inject** — Auto-discovers Makefile/npm scripts. We don't have those.
- **GitHub Release** — Automated releases. We're not releasing.
- **Dodo Payments** — Payments integration. Not applicable.
- **Model Announcer** — Injects model name. Debugging aid.
- **Envsitter Guard** — Protects .env files. Security good practice.
- **CC Safety Net** — Catches destructive commands. Useful safety net.
- **Handoff** — Creates session handoff prompts. Useful for multi-session.

---

## Recommendations

1. **Try BRHP first** — Lightweight, solves "agent forgets plan" problem directly. Just adds `/brhp` commands and persistent state.
2. **Try CrewBee after** — Define a 3-agent "feature team" matching our CLAUDE.md workflow. Requires config, not code.
3. **Try FlowDeck later** — When we need heavy orchestration for complex cross-service features.

BRHP and CrewBee are both plugins that layer on top of OpenCode without changing our CLAUDE.md or docs.
