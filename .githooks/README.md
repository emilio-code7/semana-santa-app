# Git Hooks

These hooks enforce the spec-first development workflow.

## Setup

```bash
git config core.hooksPath .githooks
```

This tells git to use project hooks instead of the default `.git/hooks/`.
Run once per clone.

## What's checked

- **OpenAPI spec must change when controllers change** (blocks commit)
- **Docs should be updated when source changes** (warns)
- **No FIXME/TODO/HACK markers** (warns)
- **Java compile check** (best-effort, runs only if Gradle daemon is alive)
