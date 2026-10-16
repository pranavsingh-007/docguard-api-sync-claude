# DocGuard SDLC Skill

## Purpose

This skill defines a reusable, stateful, human-governed lifecycle for DocGuard stories. The SDLC Orchestrator owns the workflow after a developer supplies one story reference; the developer does not manually select phases or specialist agents.

## Minimal entry point

Use:

`Start the SDLC flow for <story-reference>.`

The story reference may be a local story file or, when configured tooling supports retrieval, an external issue identifier such as a Jira issue key.

## Story intake and workspace resolution

- Resolve and read the supplied story automatically.
- Derive the story slug from the local filename without its final extension, or normalize an external issue identifier to lowercase.
- Replace spaces and unsupported slug characters with single hyphens and trim leading/trailing hyphens.
- Use `sdlc/<story-slug>/` as the active workspace without asking the human to specify it.
- Inspect existing artifacts before acting. Resume from the earliest incomplete phase or pending approval gate.
- File presence alone is not approval. If approval is uncertain, present the existing artifact for approval rather than overwriting it.
- Root-level SDLC files are historical evidence only and must not be modified.

## Lifecycle

1. **Requirements**
   - Automatically read and analyze the story.
   - Ask only necessary clarification questions.
   - Create or update `requirements.md` after clarification.
   - Ask: **Approve and continue** or **Request changes**.

2. **Architecture and design review**
   - Begin automatically after requirements approval.
   - Create `architecture.md` and `design-review.md`.
   - Present findings and ask for design approval or changes.

3. **Implementation planning**
   - Begin automatically after design approval.
   - Create `impl-plan.md` with dependency-ordered, immediate, and blocked tasks.
   - Ask for implementation approval or revisions.
   - Do not change production code before approval.

4. **Implementation**
   - Begin automatically after plan approval.
   - Change production code and tests in normal repository locations.
   - Run relevant tests automatically.
   - Continue directly to code review when implementation completes.

5. **Code review and approved fixes**
   - Review against the active requirements, architecture, design review, and plan.
   - Create `code-review.md` before fixes.
   - If findings exist, ask which findings may be fixed; apply only approved fixes.
   - If no findings exist, ask for approval to proceed to verification.

6. **Verification and approved defect fixes**
   - Run relevant unit tests, integration tests, Maven build/package, acceptance checks, and story-specific functional checks.
   - Create `verification-report.md`.
   - Report failures before changes and request fix approval.
   - After approved fixes, rerun verification automatically.
   - Continue only after PASS or explicit acceptance of a documented limitation.

7. **Pull request preparation**
   - Create `pull-request.md` with Summary, Changes Made, Test Evidence, Known Limitations, Reviewer Checklist, Agentic SDLC Evidence, and Changelog.
   - Prepare the PR title and body.
   - Obtain final human approval.
   - Create/open the PR when supported; otherwise provide the complete title/body and required integration guidance.
   - Never merge automatically.

## Automatic transition rule

Approval at a lifecycle gate authorizes the orchestrator to start the next phase immediately. The human must not be asked to issue workflow-management prompts such as "continue to architecture," "start implementation planning," "perform code review," or "run verification."

## Artifact contract

- `sdlc/<story-slug>/requirements.md`
- `sdlc/<story-slug>/architecture.md`
- `sdlc/<story-slug>/design-review.md`
- `sdlc/<story-slug>/impl-plan.md`
- `sdlc/<story-slug>/code-review.md`
- `sdlc/<story-slug>/verification-report.md`
- `sdlc/<story-slug>/pull-request.md`

The story workspace contains SDLC evidence only. Production source, tests, and user documentation remain in their normal repository locations.

## Mandatory human gates

- requirements clarifications and approval
- design approval
- implementation-plan approval
- code-review finding approval, or approval to verify when there are no findings
- verification defect-fix approval or limitation acceptance
- final pull request approval

## Quality rules

- No production implementation before plan approval.
- No review or verification fixes without explicit approval.
- No silent feature expansion.
- No blind overwrite of approved artifacts.
- Deterministic output and safe file updates are mandatory.
- Relevant tests and Maven package validation must pass before final signoff unless a limitation is explicitly accepted.
