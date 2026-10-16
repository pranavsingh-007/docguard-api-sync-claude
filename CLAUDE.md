# DocGuard Claude Instructions

## Repository context and conventions

DocGuard is a Java 17 Maven CLI that statically analyzes Spring REST controller source code and synchronizes generated Markdown documentation. Use Java 17 and Maven conventions; keep the v1 scope narrow and static-analysis based.

## Story isolation and artifact safety

- Accept a single local story reference or supported external issue identifier. Derive a filesystem-safe slug automatically and use `sdlc/<story-slug>/` without asking the human for a workspace.
- The standard entry point is `Start the SDLC flow for <story-reference>.` The SDLC Orchestrator owns phase transitions and specialist handoffs. Follow `.claude/agents/sdlc-orchestrator.md` and the lifecycle rules below; do not require the human to name the next phase or specialist.
- All specialists and reusable commands must use the active story workspace. Treat its approved `requirements.md` and `architecture.md` as the source of truth for scope and design. Each phase consumes the approved artifacts from that same workspace.
- Inspect existing story artifacts and recorded approvals before acting. Resume at the earliest incomplete phase or pending gate; do not restart or blindly overwrite approved artifacts.
- Root-level `requirements.md`, `architecture.md`, `design-review.md`, `impl-plan.md`, `code-review.md`, `verification-report.md`, and `pull-request.md` are historical v1 evidence. Read for reference only; do not modify them during a new story cycle unless explicitly asked. Do not rewrite historical verification records or other existing SDLC evidence.
- Keep lifecycle evidence in the story workspace; production source and tests remain in their normal repository locations. `README.md` remains the user-facing documentation for build, usage, safety behavior, and limitations.

## Human governance and scope

- Human approval is mandatory at the defined requirements, design, implementation-plan, review, verification-defect, and final PR gates. Approval at a gate authorizes the orchestrator to begin the next phase automatically; do not ask for separate workflow-management prompts or routine-task approval already covered by an approved plan.
- Do not implement before requirements, architecture, and the implementation plan are approved. Report review findings and verification defects before changes; apply only explicitly approved fixes.
- Do not add features beyond approved scope. Document deferred items explicitly rather than silently implementing them.
- Never merge a pull request automatically. Obtain final human approval before PR creation.
- Git and GitHub actions follow `.claude/rules/git-workflow.md`: work only on a story feature branch (created or resumed before any artifact or code is written), and obtain separate explicit approvals for commit, push, and the final PR approval. Commit approval and push approval are Git-action approvals, not lifecycle gates; the six lifecycle gates are unchanged.

## Safety and deterministic behavior

- Do not commit or disclose secrets.
- Preserve deterministic endpoint ordering, formatting, and Markdown generation across repeated runs.
- Keep `check` read-only and `sync` limited to the generated section. Preserve manual Markdown outside the generated markers exactly.
- Fail fast with explicit diagnostics for missing files, invalid Java source, malformed generated markers, duplicate endpoints, and invalid configuration; do not use silent workarounds.
- Prefer atomic file updates; never partially rewrite the documentation file.

## SDLC Lifecycle

The complete lifecycle from story to pull request:

0. **Branch preflight**: Check Git status, fetch `origin main`, create or resume `feature/<story-key>-<short-description>` (no approval gate)
1. **Requirements**: Read story, ask clarification questions, create `requirements.md`, obtain approval
2. **Architecture and Design Review**: Create `architecture.md` and `design-review.md`, obtain design approval
3. **Implementation Planning**: Create `impl-plan.md`, obtain plan approval
4. **Implementation**: Implement approved plan, run tests
5. **Code Review**: Create `code-review.md`, obtain fix approval, apply approved fixes
6. **Verification**: Run tests and validation, create `verification-report.md`, obtain defect-fix approval if needed
7. **Pull Request**: Re-run final Maven checks, create `pull-request.md`, prepare PR, obtain commit approval, push approval, and final PR approval separately, then create the Draft PR with `gh` targeting `main`

Approval at each gate authorizes automatic transition to the next phase. Commit approval, push approval, and final PR approval in step 7 are separate: each authorizes only its own action.
