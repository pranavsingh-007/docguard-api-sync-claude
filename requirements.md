# Requirements: Automated REST API Documentation Sync

## 1. Purpose

DocGuard is a Java command-line tool that statically analyzes Spring Boot REST controller source files, generates Markdown API documentation, detects documentation drift, and optionally synchronizes the generated documentation.

The tool must not run the Spring Boot application or call live APIs.

## 2. Terminology

- **Source endpoint**: An endpoint discovered from Spring REST controller source code.
- **Generated section**: The Markdown region enclosed by the configured generated-section markers.
- **Documentation drift**: A difference between the documentation generated from the current source code and the generated section in the existing documentation.
- **Check mode**: Reports drift without modifying files.
- **Sync mode**: Replaces the generated section with documentation generated from the current source code.

## 3. Functional Requirements

### FR-1: CLI invocation

The tool shall be invoked as a CLI and shall provide at least:

- `check` command for drift detection.
- `sync` command for documentation synchronization.
- A source directory argument identifying the Java source tree.
- A documentation file argument identifying the Markdown input/output file.

The CLI shall provide useful help text for commands, arguments, and options.

### FR-2: Source discovery

The tool shall recursively scan the specified source directory for Java source files.

It shall analyze Spring REST controllers and ignore Java files that do not contain relevant REST mapping annotations.

### FR-3: Static Java parsing

The tool shall parse Java source code statically using an AST-based Java parser library.

It shall not require compiled classes, a running application, a classpath containing the application, or network access to discover endpoints.

### FR-4: Endpoint annotation discovery

The tool shall recognize these annotations:

- `@GetMapping`
- `@PostMapping`
- `@PutMapping`
- `@PatchMapping`
- `@DeleteMapping`
- `@RequestMapping`

The tool shall support annotations using their common `value` or `path` attributes where applicable.

### FR-5: Endpoint resolution

For each endpoint, the tool shall identify:

- HTTP method.
- Full API path.
- Path variables.
- Query parameters.
- Request body.
- Response type.

The tool shall combine class-level `@RequestMapping` values with method-level mappings. Class-level path values shall be prefixed to method-level path values, and applicable class-level HTTP methods shall be combined with method-level HTTP methods.

The tool shall support mapping declarations containing multiple paths and, where applicable, multiple HTTP methods, producing a distinct documented endpoint for each resolved combination.

### FR-6: Parameter and type extraction

The tool shall statically identify request information from relevant Spring parameter annotations, including:

- `@PathVariable`
- `@RequestParam`
- `@RequestBody`

It shall identify the declared response type from the endpoint method signature, including common generic/container return types where syntactically available.

If a parameter or return type cannot be resolved statically, the generated documentation shall represent it as unknown or unresolved rather than silently omitting the endpoint.

### FR-7: Deterministic Markdown generation

The tool shall generate API documentation in Markdown.

Generated output shall contain, at minimum, each endpoint's HTTP method, full path, path variables, query parameters, request body, and response type.

For identical source input, generation shall be deterministic: endpoint ordering and formatting shall not vary between runs.

### FR-8: Generated-section markers

The generated section shall be enclosed by these markers:

```text
<!-- GENERATED:START -->
<!-- GENERATED:END -->
```

The tool shall preserve all content before and after the generated section exactly, except for the replacement of the content between the markers during sync.

### FR-9: Check mode

`check` shall:

1. Parse and scan the source directory.
2. Generate the expected Markdown generated section.
3. Compare it with the existing generated section in the documentation file.
4. Report whether the documentation is synchronized.
5. Avoid modifying any file.

When drift exists, the report shall identify:

- Added endpoints.
- Removed endpoints.
- Changed endpoints.
- Field-level changes, including method, path, path variables, query parameters, request body, and response type where applicable.

### FR-10: Sync mode

`sync` shall:

1. Parse and scan the source directory.
2. Generate the expected Markdown generated section.
3. Replace the existing generated section with the generated content.
4. Preserve manual content outside the markers.

If no generated section exists, the tool shall report the condition clearly and shall not overwrite the documentation without an explicitly defined initialization option or equivalent safe behavior.

### FR-11: Exit codes

The CLI shall return:

- Exit code `0` when the operation succeeds and, for `check`, no drift is detected.
- A non-zero exit code when `check` detects drift.
- A distinct non-zero failure code, or another clearly documented non-zero result, for invalid input, parsing failures, missing required files, or other execution errors.

### FR-12: Safe file updates

Sync mode shall write the documentation only after source scanning and generation complete successfully.

The tool shall avoid leaving a partially written documentation file when an update fails.

## 4. Non-Functional Requirements

### NFR-1: Accuracy

Endpoint extraction shall be based on parsed Java syntax and annotations rather than regex-only matching, so that formatting, comments, and normal Java syntax variations do not change results.

### NFR-2: Repeatability

Repeated runs against unchanged source and configuration shall produce byte-equivalent generated sections.

### NFR-3: Performance

The tool should complete scanning and generation within a practical CI/CD timeframe for typical Spring Boot projects and should avoid unnecessary repeated parsing or file writes.

### NFR-4: CI/CD usability

The tool shall write machine-detectable results through exit codes and human-readable diagnostics through standard output and/or standard error.

### NFR-5: Safety

Check mode shall be read-only. Sync mode shall modify only the specified documentation file and shall preserve content outside the generated markers.

### NFR-6: Maintainability

The implementation shall use clear separation between source parsing, endpoint modeling, Markdown generation, drift comparison, CLI handling, and file updates.

### NFR-7: Portability

The CLI shall operate consistently across supported environments without depending on a running Spring Boot application or environment-specific path behavior.

## 5. Assumptions

- The tool is implemented in Java.
- Input source files use Java syntax and Spring MVC/WebFlux-style mapping annotations.
- The source directory and documentation file are supplied by the CLI user.
- The documentation file is Markdown and uses UTF-8 text.
- Generated content is identified by the exact generated-section markers.
- A project may contain multiple controllers and multiple endpoints with the same path but different HTTP methods.
- Java types are documented from source declarations; runtime generic resolution and reflection are not required.
- The initial version targets statically expressible annotation values and method signatures.
- The user is responsible for ensuring the tool has permission to read source files and write the documentation file in sync mode.

## 6. Constraints

- The tool shall analyze Java source statically only.
- It shall not start the Spring Boot application.
- It shall not call live REST APIs.
- It shall not generate Swagger or OpenAPI files.
- It shall not modify manual Markdown outside the generated markers.
- It shall not rely solely on regular expressions for Java endpoint discovery.
- The initial version does not require support for Kotlin or other implementation languages.
- The initial version does not require evaluation of arbitrary Java expressions, constants imported from external classes, runtime profiles, or conditional bean registration.

## 7. Error Handling Requirements

### EH-1: Missing source directory

If the source directory does not exist, is not a directory, or cannot be read, the tool shall report the path and reason and return a non-zero failure exit code.

### EH-2: Missing documentation file

If the documentation file does not exist:

- `check` shall report that the required baseline documentation is missing and return a non-zero failure exit code.
- `sync` shall report the missing file and shall not create or replace it unless an explicitly defined initialization behavior is later added.

### EH-3: Invalid Java source

If a Java file cannot be parsed, the tool shall report the file and parse error. It shall not silently treat the file as having no endpoints.

The command shall fail with a non-zero exit code unless a future, explicitly specified partial-scan mode is enabled.

### EH-4: Invalid or ambiguous annotations

Unsupported or statically unresolvable mapping values shall produce clear diagnostics. The tool shall not invent paths, methods, parameters, or types.

### EH-5: Missing or malformed markers

If the documentation contains only one marker, duplicate/nested marker pairs, or otherwise malformed generated-section boundaries, the tool shall report the problem and shall not modify the file.

### EH-6: No discovered endpoints

An empty project or source tree with no recognized endpoints shall be handled gracefully. The tool shall produce deterministic empty generated content and clearly report that no endpoints were found, rather than crashing.

### EH-7: File read/write failures

Permission errors, encoding errors, and write failures shall identify the affected file and return a non-zero failure exit code. Sync mode shall avoid partial updates.

### EH-8: Diagnostics

Errors shall be written to standard error where appropriate. Diagnostics shall include enough context to locate the source file, documentation file, command, or endpoint involved.

## 8. Acceptance Criteria

1. Given a valid source directory containing controller methods annotated with each supported mapping annotation, the tool discovers and documents the expected HTTP methods and paths.
2. Given class-level and method-level `@RequestMapping` declarations, the generated documentation contains the correctly combined full paths and applicable methods.
3. Given path variables, query parameters, and request bodies, the generated Markdown identifies their names and declared types where available.
4. Given declared response types, the generated Markdown includes the response type, including supported generic/container syntax.
5. Given the same source input on two runs, the generated sections are byte-equivalent and use a stable endpoint order.
6. Given a Markdown file with valid generated markers and manual text outside them, sync mode updates only the content between the markers and preserves the manual text exactly.
7. Given synchronized documentation, check mode returns exit code `0` and does not modify the documentation file.
8. Given added, removed, or changed endpoints, check mode returns a non-zero drift exit code and reports endpoint-level and field-level differences.
9. Given invalid Java source, missing required paths, malformed markers, or file access failures, the tool reports a useful diagnostic and returns a non-zero failure exit code without a partial sync.
10. Given an empty project or source tree with no recognized endpoints, the tool completes gracefully with deterministic output and an explicit no-endpoints diagnostic.
11. The tool operates without starting the application, calling live endpoints, or requiring generated Swagger/OpenAPI files.

## 9. Out of Scope

- Running the Spring Boot application.
- Calling live REST APIs.
- Generating Swagger or OpenAPI files.
- Runtime reflection or runtime endpoint discovery.
- Kotlin or non-Java source analysis.
- Full semantic evaluation of arbitrary Java expressions or external constants.
- Inferring behavior that is not statically represented in controller annotations or method signatures.
- Generating prose descriptions, examples, authentication documentation, error schemas, or business semantics unless explicitly present in a future requirement.
- Automatically resolving or repairing malformed documentation markers.
- Modifying manual documentation outside the generated section.
- Architecture design, implementation details, and technology selection beyond the clarified Java CLI and AST-parser requirements.
