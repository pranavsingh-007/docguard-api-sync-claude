package com.docguard.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ConfigValidator {
    private ConfigValidator() {
    }

    public static Config validate(Path sourceDirectory, Path documentationFile, Config.Mode mode) {
        if (sourceDirectory == null || sourceDirectory.toString().isBlank()) {
            throw new IllegalArgumentException("Source directory must be provided.");
        }

        Path normalizedSource = sourceDirectory.toAbsolutePath().normalize();
        if (!Files.exists(normalizedSource)) {
            throw new IllegalArgumentException("Source directory does not exist: " + normalizedSource);
        }
        if (!Files.isDirectory(normalizedSource)) {
            throw new IllegalArgumentException("Source directory is not a directory: " + normalizedSource);
        }

        if (documentationFile == null || documentationFile.toString().isBlank()) {
            throw new IllegalArgumentException("Documentation file must be provided.");
        }

        Path normalizedDoc = documentationFile.toAbsolutePath().normalize();
        if (mode == Config.Mode.CHECK) {
            if (!Files.exists(normalizedDoc)) {
                throw new IllegalArgumentException("Documentation file does not exist for check mode: " + normalizedDoc);
            }
        } else if (mode == Config.Mode.SYNC) {
            if (!Files.exists(normalizedDoc)) {
                throw new IllegalArgumentException("Documentation file does not exist for sync mode: " + normalizedDoc);
            }
        }

        try {
            if (Files.isDirectory(normalizedDoc)) {
                throw new IllegalArgumentException("Documentation path points to a directory, not a file: " + normalizedDoc);
            }
        } catch (SecurityException e) {
            throw new IllegalArgumentException("Unable to access documentation file: " + normalizedDoc, e);
        }

        return new Config(normalizedSource, normalizedDoc, mode);
    }

    public static Config validate(String sourceDirectory, String documentationFile, Config.Mode mode) {
        if (sourceDirectory == null || sourceDirectory.isBlank()) {
            throw new IllegalArgumentException("Source directory must be provided.");
        }
        if (documentationFile == null || documentationFile.isBlank()) {
            throw new IllegalArgumentException("Documentation file must be provided.");
        }

        return validate(Path.of(sourceDirectory), Path.of(documentationFile), mode);
    }
}
