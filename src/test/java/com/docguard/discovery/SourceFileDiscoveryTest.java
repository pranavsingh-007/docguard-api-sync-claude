package com.docguard.discovery;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SourceFileDiscoveryTest {

    @Test
    void findsJavaFilesRecursively() throws IOException {
        Path root = Files.createTempDirectory("docguard-source-root");
        Path nestedDir = Files.createDirectory(root.resolve("nested"));
        Path javaFile1 = Files.createFile(root.resolve("Alpha.java"));
        Path javaFile2 = Files.createFile(nestedDir.resolve("Beta.java"));
        Files.createFile(root.resolve("notes.txt"));

        List<Path> files = SourceFileDiscovery.findJavaFiles(root);

        assertEquals(2, files.size());
        assertTrue(files.contains(javaFile1));
        assertTrue(files.contains(javaFile2));
    }

    @Test
    void rejectsMissingSourceRoot() {
        Path missing = rootThatDoesNotExist();
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> SourceFileDiscovery.findJavaFiles(missing));
    }

    private Path rootThatDoesNotExist() {
        return Path.of("C:/definitely/does/not/exist/docguard-root");
    }
}
