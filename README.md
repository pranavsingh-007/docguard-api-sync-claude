# DocGuard

DocGuard is a Java 17 Maven CLI that statically analyzes Spring REST controller source code, generates a deterministic Markdown API section, and detects drift between the generated section and the current Java source model.

## Problem statement

Spring REST APIs evolve as controllers change, but the documentation in Markdown files often drifts from the actual code. Manual API documentation updates are easy to miss, especially when multiple controllers, endpoints, and parameter combinations exist.

DocGuard addresses that problem by:

- scanning Java source files statically
- extracting Spring MVC endpoint metadata
- generating a canonical Markdown section
- comparing the generated section to the existing documentation
- supporting read-only `check` mode and safe `sync` mode

## Key features

- Java 17 Maven CLI built with Picocli
- Static source analysis using JavaParser
- Spring mapping support for direct annotation-based REST endpoints
- Canonical endpoint model with deterministic ordering
- Generated-section marker management for safe Markdown updates
- Drift detection with endpoint- and field-level reporting
- Atomic Markdown writes to avoid partial updates
- Fail-fast validation for missing files, invalid Java, and malformed markers

## Architecture summary

The project is organized around a small, deterministic processing pipeline:

1. Discover Java files under the provided source root.
2. Parse the source using JavaParser.
3. Resolve Spring mappings and parameters into a canonical `Endpoint` model.
4. Generate a Markdown table for the generated section.
5. Compare generated output with the current generated-doc section.
6. Either report drift in `check` mode or replace the generated section in `sync` mode.

The design intentionally favors static, explicit behavior over runtime or reflective inspection.

## Prerequisites

- Java 17+
- Maven 3.9+
- Git for repository checkout

## Build and test

```bash
mvn test
mvn package
```

## Usage

### Check mode

```bash
mvn exec:java -Dexec.mainClass=com.docguard.cli.DocGuardCli -Dexec.args="check --source src/main/java --docs docs/api.md"
```

Example:

```bash
java -jar target/docguard-0.1.0-SNAPSHOT.jar check --source ./src/main/java --docs ./docs/api.md
```

`check` mode is read-only. It returns exit code `0` when the documentation is synchronized and a non-zero exit code when drift is detected.

### Sync mode

```bash
mvn exec:java -Dexec.mainClass=com.docguard.cli.DocGuardCli -Dexec.args="sync --source src/main/java --docs docs/api.md"
```

Example:

```bash
java -jar target/docguard-0.1.0-SNAPSHOT.jar sync --source ./src/main/java --docs ./docs/api.md
```

`s`ync` mode replaces only the content between the generated markers and leaves manual Markdown outside the section untouched.

## Generated section markers

DocGuard expects generated content to be wrapped in these markers:

```markdown
<!-- GENERATED:START -->
<!-- GENERATED:END -->
```

The tool preserves everything before and after the markers exactly, replacing only the section content during sync.

## Example drift output

```text
Documentation drift detected.
Added endpoints:
- GET /api/users/{userId}

Changed endpoints:
- GET /api/users/{userId}
  - queryParams: page -> page; size
```

## Safety behavior

- `check` mode never writes files.
- `sync` mode only updates the content inside the generated section.
- Missing or malformed generated markers fail safely.
- Missing source directories or missing doc files fail fast.
- File writes are atomic and do not leave partial updates in place.

## Limitations

This v1 implementation intentionally stays narrow:

- supports direct Spring annotation mappings only
- does not support composed or inherited meta-annotation resolution
- does not auto-create missing documentation files
- does not scan vendor or generated directories by default
- does not infer runtime-only or reflection-based endpoint behavior

## Agentic SDLC overview

This project was developed using an agentic software delivery lifecycle:

1. Requirements capture and clarification
2. Architecture proposal and design review
3. Implementation planning with dependency ordering
4. Staged implementation batches
5. Validation using Maven tests and package verification
6. Documentation and CI automation for sustainable delivery

This keeps scope explicit, ensures safe incremental delivery, and makes the v1 implementation predictable and reviewable.
