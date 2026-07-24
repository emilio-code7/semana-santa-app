---
name: infrastructure-workflow
description: Infrastructure workflow for AWS, CDK, deployment, CI/CD, Docker, and cloud resource changes.
---

# Infrastructure Workflow

Use this skill for infrastructure changes — AWS, CDK, deployment, CI/CD, Docker, cloud resources.

## Prerequisites

1. Load relevant AWS/cloud skills before starting.
2. Read `docs/aws-guide.md` for architecture context, migration rationale, service mapping, deploy instructions.
3. Read `.github/workflows/deploy.yml` for current CI/CD deploy pipeline definition.
4. Read `infrastructure/aws/deploy-outputs.json` for current live resource state.

## Workflow

1. **Explore** — Use graph MCP tools first (`semantic_search_nodes_tool`, `query_graph_tool`, `get_architecture_overview_tool`). Fall back to `@explorer` for complex multi-resource changes or unfamiliar areas, or to targeted Grep/Glob/Read when graph coverage is insufficient.
2. **Assess** — Read `infrastructure/aws/deploy-outputs.json` for live resource state. Run `cdk diff` to see what would change. Never touch AWS without knowing the current state.
3. **Design** — `@oracle` is **mandatory** for: IAM policy changes, new AWS services, security group modifications, cost-impacting changes (instance types, RDS sizing). Oracle must approve before any CDK code is written.
4. **Implement** — Delegate to `@fixer` for bounded CDK/CI changes. Simple tasks follow AGENTS.md and use exactly one fixer. CDK is TypeScript — same tooling as the rest of the project.
5. **Diff** — Run `cdk diff` and present the changes before deploying. Show what resources will be created/modified/destroyed.
6. **Review** — Oracle review is risk-triggered for non-trivial CDK diffs, especially IAM, new services, security groups, cost-impacting, or data-integrity changes. Routine localized changes rely on diff/synth/tests. When review is needed, include diff hash, files reviewed, issues categorized, and verdict.
7. **Verify** — `cdk synth` succeeds, `docker build` works for affected services, GitHub Actions syntax valid, no secrets in committed files.
8. **Store** — Memory tool recording why infrastructure decisions were made (e.g., "we chose SQS over MSK because free tier").
9. **Deploy** — Only when explicitly requested. Never deploy without `cdk diff` + Oracle review.

## Hard Rules

1. **Never edit live AWS resources in the console.** Everything through CDK. Manual changes cause drift.
2. **Never deploy without `cdk diff` first.** Blind deploys destroy resources.
3. **Never change IAM policies without Oracle review.** Least privilege is hard.
4. **Never commit AWS credentials, IPs, or secrets.**
5. **Never change instance types without cost analysis.**
6. **Always update `docs/aws-guide.md`** when adding services, changing architecture, or changing deployment procedures.
7. **Always run `cdk synth` before `cdk deploy`.** Catch synthesis errors locally, not in CloudFormation.

## Verification Gate

Before declaring any infra task complete:
1. `cdk synth` — CloudFormation template generates without errors
2. `cdk diff` — reviewed and approved by Oracle for non-trivial changes
3. No hardcoded secrets, IPs, or credentials in committed files
4. `docs/aws-guide.md` updated if architecture or deployment procedure changed
5. `deploy-outputs.json` updated and committed only after an actual deployment and verification confirms live outputs changed

## Simple Tasks

Simple operational tasks (status check, single command, single known-file edit) are governed by AGENTS.md — delegate directly to `@fixer` without this workflow.
