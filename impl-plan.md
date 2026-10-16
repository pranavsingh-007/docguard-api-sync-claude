# Implementation Plan: DocGuard

## 1. Objective

Implement the approved v1 DocGuard CLI in Java 17 with Maven. The tool shall:

- scan Java source files statically using JavaParser,
- extract Spring REST controller endpoints,
- generate deterministic Markdown documentation,
- compare the generated section against an existing documentation file,
- run in `check` and `sync` modes,
- fail safely on invalid input, malformed generated markers, parse errors, or missing documentation files.

This plan follows the approved architecture and accepted design-review findings.

## 2. Implementation principles

- Fail fast on invalid input or parse errors.
- Keep the CLI read-only for `check` mode.
- Keep `sync` limited to the generated section only.
- Normalize endpoint metadata before rendering or comparing.
- Preserve manual Markdown outside the generated section.
- Keep the first release narrow and avoid unsupported advanced Spring annotation patterns.

## 3. Dependency-ordered task list

### Task ID: DOC-01
Task name: Project bootstrap and Maven structure
Short description:
Set up the Maven Java 17 project structure, dependencies, plugin configuration, and baseline package layout for the CLI and tests.
Dependencies:
None. This can start immediately.
Expected output:
- Maven project skeleton
- `pom.xml` with Java 17, JUnit 5, JavaParser, Picocli, and logging dependencies
- base package structure
- empty but runnable CLI entry point
Validation or test needed:
- `mvn test` passes on the empty baseline
- `mvn -q -DskipTests package` succeeds

### Task ID: DOC-02
Task name: CLI command and argument validation
Short description:
Build the CLI entry point and define the `check` and `sync` commands with source directory and documentation file arguments, plus required validation rules.
Dependencies:
DOC-01
Expected output:
- `DocGuardCli` with subcommands
- argument parsing and validation layer
- clear usage/help text
- exit code contract for success and failure
Validation or test needed:
- unit tests for missing args, invalid dir, invalid file path, and `--help`
- CLI smoke tests for both commands

### Task ID: DOC-03
Task name: Configuration model and fail-fast validation
Short description:
Create the configuration object and validation logic that enforces v1 safety rules: source directory checks, doc-file existence rules, output mode contracts, and validation of required arguments.
Dependencies:
DOC-02
Expected output:
- `Config`/`ConfigValidator` model and validation rules
- explicit failure messages for missing files and invalid inputs
- clear behavior for `check` vs `sync`
Validation or test needed:
- tests for missing source directory
- tests for missing doc file in `check`
- tests for missing doc file in `sync`
- tests for malformed CLI configuration

### Task ID: DOC-04
Task name: File IO utilities for Markdown and UTF-8 handling
Short description:
Implement file reading and writing utilities for Markdown files, including UTF-8 handling, preserving content outside the generated section, and normalizing line endings for deterministic comparisons.
Dependencies:
DOC-03
Expected output:
- Markdown file reader/writer utilities
- UTF-8 without BOM handling
- normalized `\n` line-ending handling
- safe file write abstraction
Validation or test needed:
- tests for CRLF normalization
- tests for preserving manual content outside the generated block
- tests for non-UTF-8 or unreadable file handling

### Task ID: DOC-05
Task name: Generated-section marker management
Short description:
Implement strict marker validation and section extraction for the generated documentation block. This is required before sync writes are allowed.
Dependencies:
DOC-04
Expected output:
- generated-block detection and validation logic
- exact markers: `<!-- GENERATED:START -->` and `<!-- GENERATED:END -->`
- logic to reject missing, duplicate, nested, or mismatched markers
Validation or test needed:
- tests for valid marker pair
- tests for missing start marker
- tests for missing end marker
- tests for duplicate markers
- tests for nested markers
- tests for sync refusing to proceed on invalid markers

### Task ID: DOC-06
Task name: Java source discovery
Short description:
Implement recursion for the source tree and detect candidate Java files requiring endpoint inspection.
Dependencies:
DOC-03
Expected output:
- source file enumeration
- filtering to `.java` files under the configured root
- simple candidate-file selection logic
Validation or test needed:
- tests for nested directories
- tests for empty directory
- tests for non-Java files ignored correctly
- tests for source-root path edge cases

### Task ID: DOC-07
Task name: JavaParser adapter layer
Short description:
Wrap JavaParser so the rest of the system depends on a stable adapter instead of directly on parser classes.
Dependencies:
DOC-06
Expected output:
- `JavaParserAdapter` that parses Java files and exposes AST access for classes, methods, annotations, params, and return types
- parser exception wrapping for consistent diagnostics
Validation or test needed:
- tests for valid Java class parse
- tests for invalid Java syntax fails with diagnostic
- tests for parser adapter returns expected method and annotation metadata

### Task ID: DOC-08
Task name: Spring endpoint extraction model
Short description:
Define the canonical endpoint schema and domain model for method, path, parameters, request body, response type, source class, and source method.
Dependencies:
DOC-07
Expected output:
- `Endpoint`, `Parameter`, and related domain model classes
- deterministic canonical ordering rules
- diagnostic support for unresolved/incomplete metadata
Validation or test needed:
- unit tests for object equality, ordering, and canonical sort
- tests for model representation stability across runs

### Task ID: DOC-09
Task name: Resolver for controller and mapping annotations
Short description:
Implement Spring controller detection and mapping resolution for class-level and method-level annotations, including the supported v1 mapping set: `@RequestMapping`, `@GetMapping`, `@PostMapping`, `@PutMapping`, `@PatchMapping`, and `@DeleteMapping`.
Dependencies:
DOC-07, DOC-08
Expected output:
- Spring endpoint resolver
- combined path and method resolution for controller + method mappings
- handling for multiple paths and multiple methods
Validation or test needed:
- tests for class-level `@RequestMapping` + method-level `@GetMapping`
- tests for multiple method paths
- tests for multiple HTTP methods
- tests for unsupported composed/inherited annotations fail clearly (v1 safe behavior)

### Task ID: DOC-10
Task name: Parameter and response-type extraction
Short description:
Extract `@PathVariable`, `@RequestParam`, `@RequestBody`, and response type metadata from method signatures and declared types.
Dependencies:
DOC-09
Expected output:
- resolved endpoint parameter metadata
- response type extraction for common generic/container return types
- unresolved-type diagnostics for unsupported cases
Validation or test needed:
- tests for `@PathVariable` extraction
- tests for `@RequestParam` extraction
- tests for `@RequestBody` extraction
- tests for response type extraction
- tests for unresolved/incomplete types handled safely

### Task ID: DOC-11
Task name: Canonical Markdown generation
Short description:
Generate the Markdown fragment that represents the current source-derived endpoint set using the agreed stable ordering and section format.
Dependencies:
DOC-08, DOC-09, DOC-10
Expected output:
- Markdown generator for the generated section
- deterministic rendering of all required fields
- output that can be used for both `check` and `sync`
Validation or test needed:
- golden-file tests for generated Markdown output
- tests for deterministic ordering and formatting across repeated runs
- tests for required fields present in output

### Task ID: DOC-12
Task name: Drift comparator and diff reporting
Short description:
Implement semantic comparison between the generated endpoint set and the existing generated section extracted from the doc file. Produce endpoint-level and field-level changes.
Dependencies:
DOC-05, DOC-11
Expected output:
- data model for added, removed, and changed endpoints
- endpoint diff and field-level diff report
- CLI-friendly string output for drift summary
Validation or test needed:
- tests for no-drift case
- tests for added endpoint
- tests for removed endpoint
- tests for changed method/path/query/body/response field
- tests for exact semantics vs raw text comparison

### Task ID: DOC-13
Task name: Sync write flow and safe file update
Short description:
Implement the safe write path that replaces only the generated section and leaves manual content outside the markers untouched.
Dependencies:
DOC-05, DOC-11, DOC-12
Expected output:
- sync workflow that reads current file, validates markers, builds new generated section, and writes updated file atomically
- no partial-write behavior on failures
Validation or test needed:
- tests for replacing only generated section
- tests for preserving manual content before and after markers
- tests for no partial file writes on failure
- tests for missing marker failure

### Task ID: DOC-14
Task name: Check mode end-to-end pipeline
Short description:
Wire the full `check` command: scan, parse, normalize, generate, compare, and report drift with exit codes.
Dependencies:
DOC-02, DOC-03, DOC-05, DOC-06, DOC-07, DOC-08, DOC-09, DOC-10, DOC-11, DOC-12
Expected output:
- complete `check` command behavior
- correct drift detection and exit codes
Validation or test needed:
- end-to-end tests for synchronized docs returning exit code 0
- end-to-end tests for drift returning non-zero exit code
- end-to-end tests for invalid input returning non-zero

### Task ID: DOC-15
Task name: Sync mode end-to-end pipeline
Short description:
Wire the full `sync` command: scan, parse, normalize, generate, validate markers, write updated generated section, and report status.
Dependencies:
DOC-02, DOC-03, DOC-05, DOC-06, DOC-07, DOC-08, DOC-09, DOC-10, DOC-11, DOC-13
Expected output:
- complete `sync` command behavior
- update of generated section only
- correct exit codes for success and failure
Validation or test needed:
- end-to-end tests for successful sync
- end-to-end tests for missing doc file failure
- end-to-end tests for malformed markers failure
- end-to-end tests for file preservation outside generated section

### Task ID: DOC-16
Task name: Testing infrastructure and smoke coverage
Short description:
Add an organized test suite covering unit, parser, diff, and CLI-level scenarios, keeping tests deterministic and CI-friendly.
Dependencies:
DOC-14, DOC-15
Expected output:
- comprehensive regression suite
- CLI smoke tests and golden-case fixtures
- reproducible test configuration
Validation or test needed:
- `mvn test` passes in CI
- test report generated successfully
- targeted tests for failure scenarios all pass

### Task ID: DOC-17
Task name: CI/CD pipeline configuration
Short description:
Create the Maven-based continuous integration pipeline for the project, including build, test, and artifact packaging checks.
Dependencies:
DOC-16
Expected output:
- CI config for build/test validation
- standard Maven lifecycle usage
- fail-fast CI behavior on broken builds
Validation or test needed:
- pipeline runs successfully on a clean checkout
- failing tests break the build
- packaging step succeeds

### Task ID: DOC-18
Task name: User documentation and usage guide
Short description:
Document the CLI commands, expected behavior, safety constraints, and usage examples for end users and contributors.
Dependencies:
DOC-15, DOC-17
Expected output:
- README or usage guide
- examples for `check` and `sync` modes
- explanation of generated-section markers and safety rules
Validation or test needed:
- verify commands in documentation match actual CLI help output
- check examples remain accurate and executable

## 4. Immediate-start tasks

These tasks can begin immediately because they do not depend on earlier implementation:

- DOC-01: Project bootstrap and Maven structure

## 5. Blocked tasks

These tasks are blocked until dependencies complete:

- DOC-03 depends on DOC-02
- DOC-04 depends on DOC-03
- DOC-05 depends on DOC-04
- DOC-06 depends on DOC-03
- DOC-07 depends on DOC-06
- DOC-08 depends on DOC-07
- DOC-09 depends on DOC-07 and DOC-08
- DOC-10 depends on DOC-09
- DOC-11 depends on DOC-08, DOC-09, and DOC-10
- DOC-12 depends on DOC-05 and DOC-11
- DOC-13 depends on DOC-05, DOC-11, and DOC-12
- DOC-14 depends on DOC-02, DOC-03, DOC-05, DOC-06, DOC-07, DOC-08, DOC-09, DOC-10, DOC-11, and DOC-12
- DOC-15 depends on DOC-02, DOC-03, DOC-05, DOC-06, DOC-07, DOC-08, DOC-09, DOC-10, DOC-11, and DOC-13
- DOC-16 depends on DOC-14 and DOC-15
- DOC-17 depends on DOC-16
- DOC-18 depends on DOC-15 and DOC-17

## 6. Testing tasks

Testing is intentionally distributed across the implementation plan, not left until the end. The following tasks are explicitly testing-focused:

- DOC-02: CLI validation tests
- DOC-03: config validation tests
- DOC-04: file IO and encoding tests
- DOC-05: generated-marker validation tests
- DOC-06: source discovery tests
- DOC-07: parser adapter tests
- DOC-08: canonical model tests
- DOC-09: resolver tests
- DOC-10: parameter and response extraction tests
- DOC-11: markdown generation golden tests
- DOC-12: comparator and diff tests
- DOC-13: sync-write safety tests
- DOC-14: `check` end-to-end tests
- DOC-15: `sync` end-to-end tests
- DOC-16: smoke and regression suite
- DOC-17: CI pipeline validation

## 7. CI/CD task

- DOC-17: CI/CD pipeline configuration

This task is explicitly for the build and validation automation around the Java/Maven project and should run after the core implementation and testing tasks are in place.

## 8. Documentation task

- DOC-18: User documentation and usage guide

This is the documentation deliverable for end users and contributors, and it should be completed after the CLI behavior is stable and validated.

## 9. Execution recommendation

Use a staged execution order:

1. Bootstrap and CLI scaffolding (DOC-01, DOC-02)
2. Validation and safe file primitives (DOC-03, DOC-04, DOC-05)
3. Source scanning and JavaParser integration (DOC-06, DOC-07)
4. Endpoint resolution and model normalization (DOC-08, DOC-09, DOC-10)
5. Markdown rendering and drift logic (DOC-11, DOC-12)
6. Sync/write workflow and end-to-end commands (DOC-13, DOC-14, DOC-15)
7. Regression validation and CI/CD (DOC-16, DOC-17)
8. Final user documentation (DOC-18)

This sequence keeps dependencies ordered and ensures blocked tasks only start after required evidence is available.
