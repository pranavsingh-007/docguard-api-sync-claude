# Code Review: DocGuard

## Review scope

Reviewed against the approved requirements and architecture for the current DocGuard implementation, with emphasis on correctness, safety, error handling, test coverage, clarity, DRY principles, and dependency safety.

## Findings

### CR-01
- Area: Code Clarity / DRY Principle
- Severity: Medium
- Finding: The endpoint resolution pipeline is duplicated in both `CheckCommand` and `SyncCommand`. The same source discovery + JavaParser + SpringEndpointResolver flow appears in two separate classes, increasing the risk of drift between check and sync behavior over time. This is not currently a functional bug, but it weakens maintainability and makes future changes harder to keep consistent.
- Recommendation: Extract the shared source-processing pipeline into a single service or orchestration component (for example, a `DocGuardService` or `SourceDocumentService`) and let both commands call the same implementation. This keeps execution logic centralized and reduces the chance of inconsistent behavior between modes.
- Status: Resolved

### CR-02
- Area: Correctness / Output Stability
- Severity: Medium
- Finding: `GeneratedSectionManager.replaceGeneratedSection` normalizes the generated replacement using `strip()`, and then reinserts it with a fixed blank-line layout. This means the generated section is not always preserved exactly as emitted by the generator; it is rewritten with a normalized whitespace structure. The requirement calls for deterministic output and for preserving manual Markdown outside the generated section exactly, but the current implementation is also normalizing the replacement body itself. This may create byte-level differences where the output should remain stable and predictable.
- Recommendation: Keep the generated Markdown rendering contract consistent at a single stage: generate the final section body once, then insert it without additional `.strip()` normalization. If whitespace normalization is required, enforce it in the generator itself and keep the section replacement logic as a pure structural operation.
- Status: Resolved

### CR-03
- Area: Error Handling / CLI UX / CI/CD
- Severity: Medium
- Finding: Validation and operational failures are allowed to bubble up as raw `IllegalArgumentException`/runtime exceptions from the command methods. In a CLI tool, this produces stack traces and inconsistent human-facing messages rather than a stable, command-oriented error contract. The requirements and architecture expect non-zero exit codes and clear diagnostics, especially for missing files, malformed markers, and invalid Java input.
- Recommendation: Add a thin CLI exception-handling boundary that catches validation and processing exceptions, writes a clean, user-facing message to stderr, and exits with a consistent non-zero status. This ensures operational clarity in local use and CI/CD pipelines.
- Status: Resolved

### CR-04
- Area: Correctness / Endpoint Identity
- Severity: Medium
- Finding: `DriftComparator.compare` keys endpoints by method + path only. This is a simplified identity model. If two endpoints share the same method/path but differ in another meaningful way (for example, same route but different source method or different request semantics), the comparator can collapse them into a single identity and mask real drift. This is especially risky when the tool is expected to compare endpoints semantically and report field-level differences.
- Recommendation: Use a stronger canonical key that includes enough information to distinguish endpoints uniquely for the current v1 model, or validate and fail on duplicate identity collisions before diffing. A safer approach is to key on the full normalized endpoint tuple (method, path, path variables, query params, request body, response type) or to treat duplicate method/path combinations as ambiguous and report them explicitly.
- Status: Resolved

### CR-05
- Area: Security and Safety / File Operations
- Severity: Low
- Finding: The application is intentionally narrow and does not expose network or shell access, which is positive. However, the CLI does not currently enforce any explicit guardrails beyond path validation and marker checks. That is acceptable for the approved v1 scope, but it is worth noting that writes are performed to the exact documentation file path supplied by the user. If a caller passes a path outside the intended project area, the tool will still overwrite it when `sync` is used. This is not an intrinsic vulnerability in the code, but the safety posture depends entirely on caller discipline.
- Recommendation: Document the expected safety contract clearly in the CLI help and README: `sync` writes only to the explicitly supplied documentation file and does not traverse or rewrite unrelated files. If the project later adds broader automation, add path allowlisting or project-root confinement.
- Status: Deferred for v1

### CR-06
- Area: Dependency Safety / Maintainability
- Severity: Low
- Finding: The current Maven dependency set is small and conventional; this is a good sign. There are no obvious risky or unnecessary dependencies in the current build. However, because the tool is intended to be static-analysis-first and intentionally narrow, the project should avoid future dependency growth without stronger justification. The code is currently maintainable because the dependency surface stays small, but that discipline must be preserved as the project evolves.
- Recommendation: Keep the dependency surface minimal. Favor internal abstractions over new libraries unless a requirement absolutely requires them. This is especially important for static analysis and CLI-only tooling where complexity can increase quickly.
- Status: Accepted as observation / no action required

## Overall assessment

The current implementation aligns well with the approved v1 scope and generally satisfies the agreed architecture: it is static, Java-based, uses JavaParser and Picocli, handles generated-section replacement carefully, and includes meaningful validation and tests. The largest remaining concerns are maintainability and operational UX rather than fundamental correctness: the check/sync logic is duplicated, raw exceptions leak from the CLI surface, and the drift identity model is simplified in a way that can hide ambiguous collisions.

These are manageable issues and are appropriate for the next iteration, but they should be addressed before broadening the tool’s scope beyond the approved v1 requirements.
