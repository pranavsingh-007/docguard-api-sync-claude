# Verify Release

Verify the approved DocGuard implementation against the active story requirements and acceptance criteria.

Required behavior:

1. Use the active story workspace supplied by the orchestrator; when used directly, derive the story slug from either a local story filename or a supported external issue identifier such as a Jira key.
2. Run automated tests and Maven package validation.
3. Validate CLI behavior, including direct standalone execution when relevant.
4. Validate check mode, sync mode, drift, and error handling scenarios.
5. Check generated documentation quality for correctness, stability, and manual Markdown preservation.
6. If validation fails, report defects before modifying code.
7. Re-run verification after approved defect fixes.
8. Update `sdlc/<story-slug>/verification-report.md` with final evidence.
9. If verification fails, ask which defect fixes are approved; after approved fixes, rerun verification automatically.
10. Proceed only after PASS or explicit acceptance of a documented limitation.
11. Create `sdlc/<story-slug>/pull-request.md`, prepare the PR title/body, and ask for final approval before PR creation.
12. Create/open the PR after approval when supported; otherwise provide the complete title/body and required integration guidance.
13. Never merge automatically.

Repository rules:

- Java 17 + Maven
- acceptance criteria traceability required
- deterministic output expected
- safe file update behavior required
- fail-fast diagnostics required for invalid input and malformed markers
- root-level SDLC files are historical evidence only and are not the active story verification target
