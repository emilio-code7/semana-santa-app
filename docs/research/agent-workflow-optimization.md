# Agent Workflow Optimization Research

> Saved: 2026-07-15. Source: librarian research + oracle analysis.
> Topic: Loop engineering, reducing hallucinations, OpenCode ecosystem, Spec-Driven Development.

---

## Spec-Driven Development (SDD) for AI Agents

### The Three Levels

| Level | Name | What it means | When |
|-------|------|---------------|------|
| 1 | Spec-first | Write spec before code, but spec is disposable | AI-assisted initial development |
| 2 | **Spec-anchored** | Spec is kept after implementation, maintained alongside code | **Long-lived production systems ← we are here** |
| 3 | Spec-as-source | Only the spec is edited; code is always regenerated from it | Mature tooling, high trust in generation |

**Golden rule:** "Use the minimum level of specification rigor that removes ambiguity for your context."

### Our Current SDD Mapping

| SDD artifact | Our equivalent | Status |
|-------------|----------------|--------|
| Spec (what/why) | `docs/functional-map.md` + OpenAPI spec | ✅ |
| Plan (how) | `docs/architecture.md` + `docs/plans/` | ✅ |
| Tasks | Sprint plans with acceptance criteria | ✅ |
| Constitution | `AGENTS.md` hard rules | ✅ New |
| Contract validation | OpenAPI spec | ⚠️ Passive — not executable |
| Gherkin scenarios | Plan acceptance criteria | ⚠️ Gherkin-like, not formal |
| **Contract tests** | Specmatic | ❌ Missing — the gap |

### The Gap

We're **spec-first but not spec-enforced.** The spec drives initial implementation but there's no automated check that code still matches the spec after changes. This is exactly what Specmatic solves.

### Specmatic Overview

- Runs contract tests from OpenAPI spec against running service
- Validates every endpoint request/response against spec
- Generates boundary/resiliency tests automatically
- Native Spring Boot integration (JUnit 5, Testcontainers support)
- AsyncAPI support for Kafka events (our outbox pattern)
- Backward compatibility checks
- Has MCP server mode for agent workflows
- CI gate: `specmatic test` must pass before merge

### Agent Workflow with SDD (proposed)

```
Functional Map → OpenAPI Spec → Specmatic Contract Tests
Functional Map → Feature Spec → Gherkin Scenarios → Agent Implementation → Contract Tests Pass? → Human Review → Merge
```

### Key Insight

SDD's value isn't in the commands — it's in the **checkpoints**. The review gates are where the human catches mistakes before they compound into code. We already have these checkpoints. Encoding them explicitly as a step the agent can't skip is the upgrade.

### Proven vs Experimental

| Adopt now | Watch |
|-----------|-------|
| OpenAPI as executable contract via Specmatic | Full spec-as-source (Tessl, CodeMySpec) |
| Gherkin scenarios as agent acceptance criteria | Multi-agent orchestrators from spec |
| Contract testing in CI as drift detection | Living specs that auto-update (Intent) |
| Spec → Plan → Tasks workflow | Formal verification of spec-compliance |
| Three-tier boundaries (Always/Ask/Never) | MCP server generation from spec |

---

## Loop Engineering
...

| Layer | What it does | When it matters |
|-------|-------------|-----------------|
| Agent loop | Model calls tools until done | Basic automation |
| Verification loop | Output scored against rubric, retried with feedback | Quality/correctness |
| Event-driven loop | Triggers + cron + webhooks → runs at scale | Production systems |
| Hill-climbing loop | Traces fed to analysis agent that rewrites harness config | Continuous improvement |

### Verification Is the Most Important Lever

- **Deterministic verifiers** (tests, lint, compile) beat LLM-as-judge
- LLM self-critique suffers from agreement bias (50% failure detection), echoing (32.8% convergence past 7 turns), latent entanglement (ρ=0.64–0.71 within model families)
- **Fix:** Cross-model review for critical changes, evidence-first review (reviewer sees code before agent's explanation — 61%→87.8% accuracy)

### Anti-Patterns to Watch

| Anti-pattern | The smell | The fix |
|-------------|----------|---------|
| Prompted Architecture | 150+ line prompt doing control flow | Move sequencing into code, leave intent in prompt |
| Compaction-Vulnerable State | Goals only in user messages | Use AGENTS.md, checkpoint files, session.metadata |
| Ungated Background Work | Cron regardless of power/CPU | Read machine state before firing |
| Tool-Result Flooding | Every intermediate result appended | Script deterministic pipelines, don't round-trip through LLM |
| Premature Distribution | docker-compose with Kafka + Redis for single-user | Start monolithic, distribute when you have >1 consumer |

### Context Management Best Practices

- **CAT (2026):** Context compression as a callable tool, not passive heuristic — 57.6% on SWE-Bench
- **CodeDelegator (2026):** Separate planning from implementation via ephemeral agents with clean context — 10.5% lift
- **COMPASS (2026, Google):** Main Agent + Meta-Thinker (monitors anomalies) + Context Manager (structured briefs) — 20% accuracy gain
- **Practical takeaway:** Don't let context grow unbounded. Compress at subtask boundaries. Isolate implementation traces from planning.

### Terminal Failure Study (20,574 sessions, 1,639 repos)

- 91.49% of misalignments need explicit user correction — agents rarely self-correct
- 51.9% misalignment persistence across adjacent sessions
- CLI sessions show more project-state damage than IDE sessions
- Over time: code errors decline, but constraint violations increase

---

## OpenCode Ecosystem

| Plugin | What it does | Key tools |
|--------|-------------|-----------|
| **oh-my-opencode** (839★) | 10 specialized agents + built-in skills | async subagents, LSP/AST tools, task delegation |
| **superpowers** (14 skills) | Structured workflow: brainstorming, TDD, plans, verification | hooks, skills, checkpoint workflows |
| **opencode-mcp** (samuelgudi, 79 tools) | MCP bridge Claude/Cursor → OpenCode | `opencode_run`, `opencode_fire`, `opencode_review_changes` |
| **legate** (momidala, 40 tools) | Workflow orchestrator with session management | create/fork/revert, checkpointing |
| **codebase-memory-mcp** | Graph-based code exploration | search_graph, trace_path, get_architecture |
| **opensearch** (kagan-sh) | Evidence-backed web + code search | structured JSON, SearXNG integration |

### Compatibility Note

oh-my-opencode's skill loader can shadow superpowers skills. Workaround: symlink superpowers skills into `~/.config/opencode/skills/`.

---

## Recommendations for This Project

### High Impact, Low Effort
1. **AGENTS.md** — project invariants auto-loaded every session (see root AGENTS.md)
2. **Verification gate before every commit** — run tests/lint/compile, don't trust agent's "all green"
3. **Review step after implementer agents** — cross-model review catches context-blindness bugs

### Medium Impact, Medium Effort
4. **Checkpoint files** — agents write `CHECKPOINT.md` at milestones for crash recovery
5. **Hard budgets** — max steps, max tokens per session (configured in opencode.json)
6. **Log agent trajectories** — review failure patterns periodically

### Low Priority
7. **MCP servers** (codebase-memory, opencode-mcp) — valuable but more setup
8. **Legate** workflow orchestrator — only if we do multi-session pipelines regularly
