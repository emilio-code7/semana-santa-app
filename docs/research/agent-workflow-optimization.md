# Agent Workflow Optimization Research

> Saved: 2026-07-15. Source: librarian research + oracle analysis.
> Topic: Loop engineering, reducing hallucinations, OpenCode ecosystem.

---

## Key Findings

### Loop Engineering — The Loop Is the Unit of Work

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
| Compaction-Vulnerable State | Goals only in user messages | Use CLAUDE.md, checkpoint files, session.metadata |
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
1. **CLAUDE.md** — project invariants auto-loaded every session (see root CLAUDE.md)
2. **Verification gate before every commit** — run tests/lint/compile, don't trust agent's "all green"
3. **Review step after implementer agents** — cross-model review catches context-blindness bugs

### Medium Impact, Medium Effort
4. **Checkpoint files** — agents write `CHECKPOINT.md` at milestones for crash recovery
5. **Hard budgets** — max steps, max tokens per session (configured in opencode.json)
6. **Log agent trajectories** — review failure patterns periodically

### Low Priority
7. **MCP servers** (codebase-memory, opencode-mcp) — valuable but more setup
8. **Legate** workflow orchestrator — only if we do multi-session pipelines regularly
