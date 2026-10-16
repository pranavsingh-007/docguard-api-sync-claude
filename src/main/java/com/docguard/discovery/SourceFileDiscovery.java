package com.docguard.discovery;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class SourceFileDiscovery {
    private SourceFileDiscovery() {
    }

    public static List<Path> findJavaFiles(Path rootDirectory) {
        if (rootDirectory == null || rootDirectory.toString().isBlank()) {
            throw new IllegalArgumentException("Source root must not be null or blank.");
        }

        Path normalizedRoot = rootDirectory.toAbsolutePath().normalize();
        if (!Files.exists(normalizedRoot)) {
            throw new IllegalArgumentException("Source root does not exist: " + normalizedRoot);
        }
        if (!Files.isDirectory(normalizedRoot)) {
            throw new IllegalArgumentException("Source root is not a directory: " + normalizedRoot);
        }

        try (Stream<Path> files = Files.walk(normalizedRoot)) {
            return files
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName() != null && path.getFileName().toString().endsWith(".java"))
                    .sorted()
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to scan Java source files from: " + normalizedRoot, e);
        }
    }
}
