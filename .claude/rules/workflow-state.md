---
paths:
  - "sdlc/**/*"
---

# Workflow state and continuation

- Before creating or updating any artifact, inspect the active story workspace (`sdlc/<story-slug>/`) and infer the current lifecycle state from existing artifacts, their recorded status, and conversation approvals.
- Never restart a completed phase or blindly overwrite an approved artifact. Resume at the earliest incomplete phase or pending human gate.
- A file's presence alone does not prove approval. Use explicit approval recorded in the conversation or artifact status headers.
- If approval state cannot be established, present the existing artifact at its required gate instead of recreating it.
- If requested changes exist, update only the affected artifact, preserve approved decisions, and return to the same gate.
- Root-level SDLC files (`requirements.md`, `architecture.md`, `design-review.md`, `impl-plan.md`, `code-review.md`, `verification-report.md`, `pull-request.md`) are historical v1 evidence. Read for reference only; do not modify them during a new story cycle.
