package com.docguard.io;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

public final class MarkdownFileService {
    private MarkdownFileService() {
    }

    public static String readUtf8(Path file) throws IOException {
        if (file == null) {
            throw new IllegalArgumentException("File path must not be null.");
        }
        return Files.readString(file, StandardCharsets.UTF_8);
    }

    public static void writeUtf8(Path file, String content) throws IOException {
        if (file == null) {
            throw new IllegalArgumentException("File path must not be null.");
        }
        if (content == null) {
            throw new IllegalArgumentException("Content must not be null.");
        }

        Path parent = file.getParent() == null ? Path.of(".") : file.getParent();
        Path tempFile = Files.createTempFile(parent, "docguard-", ".tmp");
        try {
            Files.writeString(tempFile, content, StandardCharsets.UTF_8,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE);
            Files.move(tempFile, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            Files.deleteIfExists(tempFile);
            throw e;
        }
    }
}
