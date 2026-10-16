# Design Review: DocGuard

## Executive summary

The proposed architecture is directionally sound and aligns with the stated requirements: it uses Java 17, Maven, a JavaParser-based static analysis pipeline, a CLI with `check` and `sync` modes, and a marker-based generated section in Markdown.

The main concerns are not with the overall approach, but with a few important gaps in the design and a small number of inconsistencies between the requirements and the architecture. The architectural design should be tightened before implementation to improve safety, determinism, testability, and CI/CD reliability.

## Findings

### DR-01
Severity: High
Finding:
The architecture introduces a broad end-to-end pipeline and a large number of responsibilities, but the requirements do not define the exact contract for endpoint normalization, ordering, or Markdown representation. The architecture states that the tool should produce deterministic output, but it does not specify canonical ordering rules such as sort by controller class, path, and HTTP method. Without a canonical sort and stable rendering contract, two equivalent source trees may produce different output ordering, undermining automation and CI/CD reproducibility.

Recommendation:
Define a single canonical endpoint model and explicit ordering rules before implementation. Example ordering: controller class name, full path, HTTP method, then parameter names. Also define the exact Markdown output schema for each endpoint so that `check` and `sync` compare equivalent semantics instead of text variations.

### DR-02
Severity: High
Finding:
There is a requirement/architecture inconsistency around the treatment of a missing documentation file. The requirements explicitly say that `sync` should not create or replace a missing documentation file unless initialization is explicitly added later. However, the architecture states that the configuration validation layer should “ensure the documentation file exists in sync mode when required by policy.” This leaves the product behavior unclear and allows for implementation drift between requirement and design.

Recommendation:
Adopt a strict rule for v1: `sync` fails with a clear error if the target documentation file is missing; no auto-create behavior unless an explicit future feature is approved. The architecture should state this unambiguously.

### DR-03
Severity: High
Finding:
The architecture does not specify the precise failure behavior when a subset of Java files cannot be parsed. The requirements state that invalid Java source should be reported as an error and should return non-zero exit code, but they do not say whether the tool should stop the whole run or continue processing other files. This is important for CI/CD reliability because partial results can create false positives or hidden drift.

Recommendation:
Explicitly adopt fail-fast behavior for v1: if any Java file in the source tree cannot be parsed, the tool halts with a non-zero exit code and reports the file path and parse error. Do not silently ignore parse errors or partially process the project.

### DR-04
Severity: High
Finding:
The architecture describes a generated-section manager, diff comparator, and writer, but it does not define the precise rules for handling malformed generated markers. The requirements require malformed marker situations to be reported without modification, yet the architecture does not define whether a single marker, duplicate markers, or nested markers should be treated as fatal errors and how they are surfaced to the user.

Recommendation:
Add a dedicated marker-validation contract: exact pair validation, duplicate-check enforcement, and clear diagnostics for missing start/end markers, mismatched pairs, or nested/overlapping blocks. Fail before writing anything in sync mode.

### DR-05
Severity: Medium
Finding:
The architecture says the tool filters “candidate controller files by scanning class declarations and annotation usage” before parsing, but the requirements do not define how this determination is made for classes with composed annotations, inherited controller annotations, or annotation aliases. This can lead to inconsistent results and false negatives when endpoint definitions are expressed through Spring meta-annotations or inheritance patterns.

Recommendation:
Document the supported annotation model explicitly. For v1, support direct Spring MVC mapping annotations on methods and controller classes, and fail with a clear diagnostic when the tool encounters unsupported composed or inherited annotation patterns rather than silently skipping them.

### DR-06
Severity: Medium
Finding:
The architecture details file writing and atomic replace behavior, but it does not include a file-locking or concurrency strategy. In CI/CD or multi-process environments, two DocGuard runs could race on the same documentation file. This is a real operational risk even though the tool is a CLI and likely runs in isolated build jobs.

Recommendation:
Specify whether DocGuard should guard against concurrent writes by file locking or by failing when a target file is already locked. At minimum, document that the tool is intended for single-writer execution and should fail cleanly if a lock is detected.

### DR-07
Severity: Medium
Finding:
The architecture recommends a relatively elaborate component model for a small CLI tool, including separate “Documentation Model Builder,” “Markdown Section Manager,” and “Drift Comparator.” This is not inherently wrong, but the design may be more complex than the problem requires. The risk is unnecessary maintenance and over-engineering for a first release with a narrow requirement set.

Recommendation:
Keep the modular separation but avoid adding additional abstraction layers beyond the current need. For v1, a simple domain model plus a markdown renderer plus a diff engine is sufficient. Avoid building a framework around the tool before the core parser and diff logic are validated.

### DR-08
Severity: Medium
Finding:
The current design does not specify how endpoint metadata is represented when mapping values are not statically resolvable. Requirements say unsupported or unresolvable values should be represented as unknown or unresolved rather than silently omitted, but architecture does not define data type or representation for unresolved values.

Recommendation:
Add a nullable/enum-based representation in the endpoint model, for example: `status = RESOLVED | UNRESOLVED`, plus a `rawValue` or `diagnostic` field. This will preserve required visibility in check mode and avoid ambiguous diagnostics.

### DR-09
Severity: Medium
Finding:
The requirements state that the tool should support path variables, query parameters, request body, and response type as output fields, but the architecture does not define the full schema for each field or the specific representation of arrays, generics, and nested types. This can create inconsistent markdown output and diff behavior across endpoints.

Recommendation:
Document a concrete endpoint schema before implementation. For example: `method`, `path`, `pathVariables[]`, `queryParams[]`, `requestBody`, `responseType`, `sourceClass`, `sourceMethod`, `status`, and `diagnostics`. This makes the generated Markdown and diff logic deterministic.

### DR-10
Severity: Medium
Finding:
The architecture does not explicitly cover cross-platform file handling and encoding issues in a way that is testable. The requirements mention documentation files are Markdown and UTF-8, but the architecture does not define the encoding handling strategy, BOM behavior, or newline normalization during compare/write operations.

Recommendation:
Specify a strict policy: read and write UTF-8 without BOM, normalize line endings to `\n` when comparing generated content, and document how the tool behaves when the documentation file is in a different encoding or contains CRLF variants.

### DR-11
Severity: Medium
Finding:
The architecture describes user-facing output and exit codes, but it does not define what should be printed to stdout versus stderr for success, drift, and failures. In CI/CD, this can matter when build logs are parsed or when errors must be distinguished from warnings.

Recommendation:
Define a clear output contract: stdout for normal status and generated summaries, stderr for actual errors, warnings, and diagnostics. Keep exit-code semantics explicit and stable.

### DR-12
Severity: Low
Finding:
The architecture’s “Source Discovery” layer suggests scanning for likely controller files using annotation detection, but it does not specify how the project will be treated if the source directory contains generated or vendor files, or if there are duplicates across modules. This can increase parsing time and create noise in results.

Recommendation:
Add explicit directory filtering rules for v1, such as ignoring `target/`, `build/`, `out/`, generated code folders, and vendor libraries where appropriate. If mult-module builds are supported later, document how modules are handled.

### DR-13
Severity: Low
Finding:
The architecture document includes a proposed package structure that may be too detailed for a design review and could prematurely lock in implementation choices. This is not a defect in itself, but it reduces flexibility if the project later adds plugin support or changes the CLI packaging approach.

Recommendation:
Keep the package layout as a suggestion, not a firm design commitment. The architecture should focus more on responsibilities and contracts than on a fixed directory structure unless the project is already beyond the initial phase.

## Additional review observations

1. Requirements and architecture are broadly aligned on scope, static analysis, JavaParser usage, and marker-based generated sections.
2. The architecture is stronger than the requirements on safety and atomic writes, which is a positive sign.
3. The main design weakness is the lack of explicit contracts for canonical data representation, diff semantics, and strict error behavior.
4. The architecture should be tighter about v1 boundaries. The tool is intentionally narrow, and the design should resist feature creep into generic parser frameworks or advanced annotation resolution.

## Final agreed design decisions

### Accepted for v1

The following findings were accepted and must be reflected in the implementation design:

- DR-01: Define a canonical endpoint model and deterministic ordering rules before implementation.
- DR-02: `sync` will fail if the target documentation file is missing; no auto-create behavior in v1.
- DR-03: The tool will fail-fast on parse errors in any Java file encountered during a run.
- DR-04: Generated markers will be strictly validated; malformed or duplicated markers will fail before write operations.
- DR-07: Keep the design modular but avoid introducing unnecessary abstraction layers beyond v1 needs.
- DR-09: The endpoint schema will be explicitly defined (`method`, `path`, `pathVariables[]`, `queryParams[]`, `requestBody`, `responseType`, `sourceClass`, `sourceMethod`, diagnostics as needed).
- DR-10: Markdown files will be handled as UTF-8 without BOM; compare and generate with normalized `\n` line endings.
- DR-11: stdout will carry normal status output; stderr will carry errors, warnings, and diagnostics; exit codes will be explicit and CI-friendly.

### Deferred for v1

The following findings were reviewed and intentionally deferred to a later release:

- DR-05: composed/inherited Spring annotation support
- DR-06: file locking/concurrency strategy
- DR-08: dedicated `RESOLVED/UNRESOLVED` status model
- DR-12: generated/vendor directory filtering
- DR-13: package structure concern

### Additional v1 design commitments

1. Java 17 and Maven will be the implementation baseline.
2. JavaParser will be used for static source parsing; regex-only parsing will not be used.
3. The tool will be a single Java CLI with exactly `check` and `sync` commands.
4. The generated section will be bounded by `<!-- GENERATED:START -->` and `<!-- GENERATED:END -->` markers and will preserve all content outside those markers.
5. Drift comparison will be semantic and field-based rather than a raw text comparison, with explicit endpoint-level and field-level differences.
6. File writes will be atomic and limited to the generated section only.

## Conclusion

The architecture is viable and appropriate for the stated requirements, but it should be tightened around strict contracts for validation, parse failure handling, canonical endpoint modeling, and safe file update behavior. These recommended changes are not large reroutes; they are focused clarifications that will make the implementation more reliable, testable, and production-safe.
