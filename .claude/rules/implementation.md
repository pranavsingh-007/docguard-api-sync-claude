---
paths:
  - "sdlc/**/impl-plan.md"
  - "src/main/**/*.java"
  - "src/test/**/*.java"
---

# Implementation and planning

- In `impl-plan.md`, create a dependency-ordered plan identifying blocked tasks, their dependencies, immediate work, expected outputs, and validation. Ask for implementation approval or revisions before changing production code.
- Implement only human-approved tasks, in small batches, against the approved requirements, architecture, and plan. Run the smallest relevant Maven tests after each batch and proceed to code review after implementation and relevant tests.
- Follow existing Java 17/Maven patterns; favor clear, maintainable code and avoid duplication. Keep production and test code in their normal repository locations.
- Handle invalid input and errors explicitly. Preserve the shared safety rules for `check`, `sync`, manual Markdown, atomic writes, and deterministic output.
