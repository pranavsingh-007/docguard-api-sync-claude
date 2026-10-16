---
paths:
  - "sdlc/**/verification-report.md"
---

# Verification

- Run relevant JUnit 5 unit and integration tests, Maven build/package validation, and applicable CLI smoke checks. Prefer targeted Maven tests for changed behavior, then complete relevant final verification.
- Verify the approved acceptance criteria and generated documentation quality with evidence. Cover happy paths and important failures; confirm `check` does not mutate files, drift is detected, `sync` preserves manual Markdown, and repeated generation is stable.
- Record commands, test counts, build/package and CLI results, criterion-level PASS/FAIL/PARTIAL status, defects, limitations, and final status in `verification-report.md`.
- Report failures before changing code. Rerun verification after approved defect fixes; proceed to PR preparation only after PASS or explicit human acceptance of a documented limitation.
