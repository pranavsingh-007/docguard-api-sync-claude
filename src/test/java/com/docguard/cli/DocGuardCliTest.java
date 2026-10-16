package com.docguard.cli;

import com.docguard.docs.GeneratedSectionManager;
import com.docguard.docs.MarkdownGenerator;
import com.docguard.model.Endpoint;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocGuardCliTest {

    @Test
    void checkCommandReturnsZeroWhenDocumentationMatchesSource() throws IOException {
        Path sourceDirectory = Files.createTempDirectory("docguard-check-src");
        Path sourceFile = sourceDirectory.resolve("UserController.java");
        Files.writeString(sourceFile,
                "import org.springframework.web.bind.annotation.*;\n" +
                        "@RestController\n" +
                        "class UserController {\n" +
                        "  @GetMapping(\"/api/users/{userId}\")\n" +
                        "  public String listUsers(@PathVariable String userId, @RequestParam String page) { return \"x\"; }\n" +
                        "}\n");

        Endpoint endpoint = new Endpoint("GET", "/api/users/{userId}", List.of("userId"), List.of("page"),
                "none", "String", "UserController", "listUsers", List.of());
        String expectedGeneratedSection = MarkdownGenerator.generate(List.of(endpoint));
        Path documentationFile = Files.createTempFile(sourceDirectory, "api", ".md");
        Files.writeString(documentationFile,
                "# API docs\n\n" +
                        "<!-- GENERATED:START -->\n" +
                        expectedGeneratedSection +
                        "<!-- GENERATED:END -->\n" +
                        "More manual text\n");

        int exitCode = new CommandLine(new DocGuardCli()).execute(
                "check",
                "--source", sourceDirectory.toString(),
                "--docs", documentationFile.toString());

        assertEquals(0, exitCode);
    }

    @Test
    void checkCommandReturnsNonZeroWhenDriftDetected() throws IOException {
        Path sourceDirectory = Files.createTempDirectory("docguard-check-drift-src");
        Path sourceFile = sourceDirectory.resolve("UserController.java");
        Files.writeString(sourceFile,
                "import org.springframework.web.bind.annotation.*;\n" +
                        "@RestController\n" +
                        "class UserController {\n" +
                        "  @GetMapping(\"/api/users/{userId}\")\n" +
                        "  public String listUsers(@PathVariable String userId, @RequestParam String page) { return \"x\"; }\n" +
                        "}\n");

        Path documentationFile = Files.createTempFile(sourceDirectory, "api", ".md");
        Files.writeString(documentationFile,
                "# API docs\n\n<!-- GENERATED:START -->\nold content\n<!-- GENERATED:END -->\nMore manual text\n");

        int exitCode = new CommandLine(new DocGuardCli()).execute(
                "check",
                "--source", sourceDirectory.toString(),
                "--docs", documentationFile.toString());

        assertEquals(1, exitCode);
    }

    @Test
    void checkCommandDoesNotModifyDocumentationFile() throws IOException {
        Path sourceDirectory = Files.createTempDirectory("docguard-check-no-write");
        Path sourceFile = sourceDirectory.resolve("UserController.java");
        Files.writeString(sourceFile,
                "import org.springframework.web.bind.annotation.*;\n" +
                        "@RestController\n" +
                        "class UserController {\n" +
                        "  @GetMapping(\"/api/users\")\n" +
                        "  public String listUsers() { return \"x\"; }\n" +
                        "}\n");

        Path documentationFile = Files.createTempFile(sourceDirectory, "api", ".md");
        String original = "# API docs\n\n<!-- GENERATED:START -->\nold content\n<!-- GENERATED:END -->\nMore manual text\n";
        Files.writeString(documentationFile, original);

        int exitCode = new CommandLine(new DocGuardCli()).execute(
                "check",
                "--source", sourceDirectory.toString(),
                "--docs", documentationFile.toString());

        assertEquals(1, exitCode);
        assertEquals(original, Files.readString(documentationFile));
    }

    @Test
    void checkCommandFailsForMissingSourceDirectory() throws IOException {
        Path sourceDirectory = sourceDirectoryMissing();
        Path documentationFile = Files.createTempFile("docguard-missing-src-doc", ".md");

        int exitCode = new CommandLine(new DocGuardCli()).execute(
                "check",
                "--source", sourceDirectory.toString(),
                "--docs", documentationFile.toString());

        assertFalse(exitCode == 0);
    }

    @Test
    void checkCommandFailsForMalformedGeneratedMarkers() throws IOException {
        Path sourceDirectory = Files.createTempDirectory("docguard-malformed-markers");
        Path sourceFile = sourceDirectory.resolve("UserController.java");
        Files.writeString(sourceFile,
                "import org.springframework.web.bind.annotation.*;\n" +
                        "@RestController\n" +
                        "class UserController {\n" +
                        "  @GetMapping(\"/api/users\")\n" +
                        "  public String listUsers() { return \"x\"; }\n" +
                        "}\n");

        Path documentationFile = Files.createTempFile(sourceDirectory, "api", ".md");
        Files.writeString(documentationFile, "# API docs\n\n<!-- GENERATED:START -->\ncontent\n<!-- GENERATED:END \nmanual\n");

        int exitCode = new CommandLine(new DocGuardCli()).execute(
                "check",
                "--source", sourceDirectory.toString(),
                "--docs", documentationFile.toString());

        assertFalse(exitCode == 0);
    }

    @Test
    void checkCommandHandlesEmptySourceTree() throws IOException {
        Path sourceDirectory = Files.createTempDirectory("docguard-empty-source");
        Path documentationFile = Files.createTempFile(sourceDirectory, "api", ".md");
        String generated = MarkdownGenerator.generate(List.of());
        Files.writeString(documentationFile,
                "# API docs\n\n<!-- GENERATED:START -->\n" + generated + "<!-- GENERATED:END -->\nManual\n");

        PrintStream originalOut = System.out;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        System.setOut(new PrintStream(baos));
        try {
            int exitCode = new CommandLine(new DocGuardCli()).execute(
                    "check",
                    "--source", sourceDirectory.toString(),
                    "--docs", documentationFile.toString());

            assertEquals(0, exitCode);
            assertTrue(baos.toString().contains("No endpoints discovered."));
        } finally {
            System.setOut(originalOut);
        }
    }

    @Test
    void checkCommandFailsForInvalidJavaSource() throws IOException {
        Path sourceDirectory = Files.createTempDirectory("docguard-invalid-java");
        Path sourceFile = sourceDirectory.resolve("BrokenController.java");
        Files.writeString(sourceFile, "class BrokenController { void broken( { } }");

        Path documentationFile = Files.createTempFile(sourceDirectory, "api", ".md");
        Files.writeString(documentationFile, "# API docs\n\n<!-- GENERATED:START -->\n<!-- GENERATED:END -->\nManual\n");

        int exitCode = new CommandLine(new DocGuardCli()).execute(
                "check",
                "--source", sourceDirectory.toString(),
                "--docs", documentationFile.toString());

        assertFalse(exitCode == 0);
    }

    @Test
    void syncCommandFailsWhenDocumentationFileDoesNotExist() throws IOException {
        Path sourceDirectory = Files.createTempDirectory("docguard-sync-src");
        Path missingFile = sourceDirectory.resolve("missing.md");

        int exitCode = new CommandLine(new DocGuardCli()).execute(
                "sync",
                "--source", sourceDirectory.toString(),
                "--docs", missingFile.toString());

        assertFalse(exitCode == 0);
        assertFalse(Files.exists(missingFile));
    }

    @Test
    void syncCommandPreservesManualContentOutsideGeneratedSection() throws IOException {
        Path sourceDirectory = Files.createTempDirectory("docguard-sync-preserve-src");
        Path sourceFile = sourceDirectory.resolve("UserController.java");
        Files.writeString(sourceFile,
                "import org.springframework.web.bind.annotation.*;\n" +
                        "@RestController\n" +
                        "class UserController {\n" +
                        "  @GetMapping(\"/api/users/{userId}\")\n" +
                        "  public String listUsers(@PathVariable String userId, @RequestParam String page) { return \"x\"; }\n" +
                        "}\n");

        Path documentationFile = Files.createTempFile(sourceDirectory, "api", ".md");
        String original = "# Manual intro\n\nBefore\n"
                + "<!-- GENERATED:START -->\n"
                + "old generated data\n"
                + "<!-- GENERATED:END -->\n"
                + "After\n";
        Files.writeString(documentationFile, original);

        int exitCode = new CommandLine(new DocGuardCli()).execute(
                "sync",
                "--source", sourceDirectory.toString(),
                "--docs", documentationFile.toString());

        assertEquals(0, exitCode);

        String updatedContent = Files.readString(documentationFile);
        assertTrue(updatedContent.startsWith("# Manual intro\n\nBefore\n"));
        assertTrue(updatedContent.contains("<!-- GENERATED:START -->"));
        assertTrue(updatedContent.contains("GET | /api/users/{userId}"));
        assertTrue(updatedContent.endsWith("\nAfter\n"));
    }

    private static Path sourceDirectoryMissing() {
        return Path.of("C:/definitely/does/not/exist/docguard-source");
    }
}
