---
name: using-repository-git-workflow
description: Use when creating branches, opening PRs, merging changes, organizing sprint work, handling hotfixes, or tagging releases in this repository.
---

# Git Workflow — Repertorio

## Overview

Trunk-based development with a single long-lived branch (`main`), short-lived feature branches, and squash-merge PRs. GitHub Issues + Milestones organize sprint work. Releases are annotated SemVer tags from `main`.

## Workflow & Rules

- **`main` is the only long-lived branch.** Never push directly to it.
- **One issue/concern per branch.** Start from an updated `main`.
- **Branch format:** `<type>/<issue>-<kebab-summary>`
  - Types: `feat`, `fix`, `infra`, `refactor`, `test`, `docs`, `chore`
  - Example: `feat/42-add-marcha-search` or `fix/17-fix-cruceta-ordering`
- **GitHub Issues + Milestones** organize sprint work. Never use long-lived sprint branches.
- **PR required.** CI must pass. PR description links to or closes the issue.
- **Squash merge only.** Final PR title/commit follows `type(scope): description` format (e.g. `feat(repertorio): add marcha search endpoint`). Delete branch after merge.
- **Releases** are annotated SemVer tags from `main`: `git tag -a v1.2.3 -m "v1.2.3"`. Tagging does not imply deployment unless a reviewed workflow says so.
- **Hotfixes** still use a short-lived branch + PR. No undocumented bypass.

## Quick Reference

| Action | Command |
|--------|---------|
| Start from main | `git checkout main && git pull` |
| Create branch | `git checkout -b feat/42-add-marcha-search` |
| Commit | `git commit -m "feat(repertorio): implement marcha search"` |
| Push | `git push -u origin feat/42-add-marcha-search` |
| Open PR | `gh pr create --fill` |
| Merge PR | Squash via GitHub UI |
| Tag release | `git tag -a v1.2.3 -m "v1.2.3" && git push --tags` |

## Common Mistakes

| Mistake | Correction |
|---------|------------|
| Pushing directly to main | Create a branch + PR |
| Long-lived branches (sprint branches) | Use issues + milestones |
| Merge commits in main | Squash merge only |
| `feat/` for docs-only changes | Use `docs/` type |
| Tagging without annotating | Use `-a` flag |
| Committing without authorization | Wait for explicit user approval per platform instructions |
