# User Story: Automated REST API Documentation Sync

## User Story

As a software development team,
I want an automated tool that detects differences between Spring Boot REST API source code and API documentation,
so that our documentation remains synchronized whenever API endpoints are changed.

## Background

Developers frequently modify REST API endpoints such as URLs, HTTP methods, request parameters, and request bodies.

However, the corresponding API documentation may not always be updated.

This creates documentation drift where the source code and documentation contain different information.

The system should automatically scan Java Spring REST controller files and compare the discovered endpoints with generated API documentation.

## Expected Capabilities

The tool should:

1. Scan Java source files containing Spring REST controllers.
2. Identify REST endpoints using annotations such as:
   - @GetMapping
   - @PostMapping
   - @PutMapping
   - @PatchMapping
   - @DeleteMapping
   - @RequestMapping
3. Identify endpoint details such as:
   - HTTP method
   - API path
   - Path variables
   - Query parameters
   - Request body
   - Response type
4. Generate API documentation in Markdown format.
5. Detect whether the existing documentation is different from the current source code.
6. Provide a check mode that reports documentation drift without modifying files.
7. Provide a sync mode that updates the generated documentation.
8. Preserve manually written documentation outside the generated documentation section.
9. Return a non-zero exit code when documentation drift is detected so that the tool can be used in CI/CD pipelines.
10. Handle missing files, invalid source files, and empty projects gracefully.

## Out of Scope

For the initial version:

- The tool does not need to run the Spring Boot application.
- The tool does not need to call live REST APIs.
- The tool does not need to generate Swagger/OpenAPI files.
- The tool only needs to analyze Java source code statically.