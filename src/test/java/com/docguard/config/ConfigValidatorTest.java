package com.docguard.config;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConfigValidatorTest {

    @Test
    void validatesRequiredValuesForCheckMode() throws IOException {
        Path sourceDirectory = Files.createTempDirectory("docguard-src");
        Path documentationFile = Files.createTempFile(sourceDirectory, "api", ".md");

        Config config = ConfigValidator.validate(sourceDirectory, documentationFile, Config.Mode.CHECK);

        assertNotNull(config);
        assertEquals(sourceDirectory.toAbsolutePath().normalize(), config.getSourceDirectory());
        assertEquals(documentationFile.toAbsolutePath().normalize(), config.getDocumentationFile());
    }

    @Test
    void throwsWhenDocumentationFileMissingInCheckMode() throws IOException {
        Path sourceDirectory = Files.createTempDirectory("docguard-src");
        Path missingFile = sourceDirectory.resolve("missing.md");

        assertThrows(IllegalArgumentException.class,
                () -> ConfigValidator.validate(sourceDirectory, missingFile, Config.Mode.CHECK));
    }

    @Test
    void throwsWhenDocumentationFileMissingInSyncMode() throws IOException {
        Path sourceDirectory = Files.createTempDirectory("docguard-src");
        Path missingFile = sourceDirectory.resolve("missing.md");

        assertThrows(IllegalArgumentException.class,
                () -> ConfigValidator.validate(sourceDirectory, missingFile, Config.Mode.SYNC));
    }

    @Test
    void throwsWhenSourceDirectoryIsMissing() {
        Path missingDirectory = Path.of("C:/does/not/exist/docguard-src");
        Path documentationFile = Path.of("C:/does/not/exist/api.md");

        assertThrows(IllegalArgumentException.class,
                () -> ConfigValidator.validate(missingDirectory, documentationFile, Config.Mode.CHECK));
    }
}
