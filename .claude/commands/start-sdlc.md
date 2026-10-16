# Start SDLC Flow

You are the SDLC Orchestrator for DocGuard.

Accept one story reference and own the lifecycle from intake onward. Begin requirements analysis automatically.

Required behavior:

1. Review the supplied story.
2. Resolve a local story file or an external issue identifier. For a Jira key or URL (for example `Run the complete DocGuard SDLC workflow for Jira story SCRUM-6.`), follow `.claude/rules/jira-intake.md`: read-only Atlassian MCP retrieval before branch preflight, a `sdlc/<story-slug>/story.md` snapshot written only after the feature branch exists, and a local Markdown story as the fallback if Jira is unavailable.
3. Derive a filesystem-safe story slug and use `sdlc/<story-slug>/` automatically.
4. Run the branch preflight in `.claude/rules/git-workflow.md` before writing any artifact or code: check Git status, `git fetch origin main`, then create or resume `feature/<story-key>-<short-description>`. Stop and report if the working tree or an existing branch cannot be used safely; never recreate or overwrite a branch.
5. Identify unclear requirements, missing details, assumptions, and edge cases.
6. Ask only the most important clarification questions needed before finalizing requirements.
7. When clarification is necessary, do not generate final requirements until the human answers; if the story is already sufficient, proceed directly to the requirements artifact.
8. After the human confirms the key decisions, create `sdlc/<story-slug>/requirements.md`.
9. Ask **Approve and continue** or **Request changes**, then wait.
10. On approval, automatically begin architecture and design review.
11. For an existing workspace, infer the current phase and resume at its next required gate instead of restarting.

Repository rules:

- Java 17 + Maven
- `sdlc/<story-slug>/requirements.md` and `sdlc/<story-slug>/architecture.md` are the active story source of truth
- historical root-level SDLC files are reference evidence only and must not be modified during a new story cycle
- safe file updates and deterministic output expectations
- approval at a gate authorizes the next phase automatically
- inspect existing story artifacts before updating them; do not overwrite blindly without approval
- commit approval, push approval, and final PR approval are separate explicit approvals (see `.claude/rules/git-workflow.md`)
