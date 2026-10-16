package com.docguard.config;

import java.nio.file.Path;

public class Config {
    public enum Mode {
        CHECK,
        SYNC
    }

    private final Path sourceDirectory;
    private final Path documentationFile;
    private final Mode mode;

    public Config(Path sourceDirectory, Path documentationFile, Mode mode) {
        this.sourceDirectory = sourceDirectory;
        this.documentationFile = documentationFile;
        this.mode = mode;
    }

    public Path getSourceDirectory() {
        return sourceDirectory;
    }

    public Path getDocumentationFile() {
        return documentationFile;
    }

    public Mode getMode() {
        return mode;
    }
}
