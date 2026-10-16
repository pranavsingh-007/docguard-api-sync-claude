# Review Code

Review the current DocGuard implementation against the approved story requirements and architecture in the active story workspace.

Required behavior:

1. Use the active story workspace supplied by the orchestrator; when used directly, derive the slug from either a local story filename or a supported external issue identifier such as a Jira key.
2. Review correctness, security and safety, error handling, tests, code clarity, DRY, and dependency safety.
3. Identify findings before fixing them.
4. Record findings in `sdlc/<story-slug>/code-review.md` with severity, recommendation, and status.
5. Apply only findings explicitly approved by the human.
6. Keep the review aligned with the approved scope and architecture.
7. If findings exist, ask which findings are approved for fixing and wait.
8. If no findings exist, ask for approval to proceed to verification.
9. After approved fixes or approval to proceed, begin verification automatically.

Repository rules:

- no feature expansion during review
- no silent implementation of unapproved fixes
- fail-fast and safe behavior must be reviewed explicitly
- preserve the approved v1 boundaries
- root-level SDLC files remain historical evidence and are not the active story review target
