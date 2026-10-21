# Verify Release

Verify the approved DocGuard implementation against the active story requirements and acceptance criteria.

Required behavior:

1. Use the active story workspace supplied by the main agent; when used directly, derive the story slug from either a local story filename or a supported external issue identifier such as a Jira key.
2. Run automated tests and Maven package validation.
3. Validate CLI behavior, including direct standalone execution when relevant.
4. Validate check mode, sync mode, drift, and error handling scenarios.
5. Check generated documentation quality for correctness, stability, and manual Markdown preservation.
6. If validation fails, report defects before modifying code.
7. Re-run verification after approved defect fixes.
8. Update `sdlc/<story-slug>/verification-report.md` with final evidence.
9. If verification fails, ask which defect fixes are approved; after approved fixes, rerun verification automatically.
10. Proceed only after PASS or explicit acceptance of a documented limitation.
11. Re-run the final quality validation from `.claude/rules/git-workflow.md` (`mvn test`, `mvn package`, `java -jar target/docguard-0.1.0-SNAPSHOT.jar --help`), then create `sdlc/<story-slug>/pull-request.md` from the actual diff and that run's results, and prepare the PR title/body.
12. Follow Step 7 of the `docguard-sdlc` skill: ask for commit approval, push approval, and final PR approval, separately and in that order. Create the PR with `gh pr create --draft --base main` after final PR approval; if `gh` is unavailable, provide the complete title/body and required integration guidance.
13. Never merge automatically, and leave the PR in Draft.

Repository rules:

- Java 17 + Maven
- acceptance criteria traceability required
- deterministic output expected
- safe file update behavior required
- fail-fast diagnostics required for invalid input and malformed markers
- root-level SDLC files are historical evidence only and are not the active story verification target
