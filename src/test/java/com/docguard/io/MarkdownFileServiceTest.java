package com.docguard.io;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarkdownFileServiceTest {

    @Test
    void readsAndWritesUtf8FileContent() throws IOException {
        Path tempFile = Files.createTempFile("docguard-md-", ".md");
        String content = "# Example\n\nHello, UTF-8 — world\n";

        MarkdownFileService.writeUtf8(tempFile, content);
        String readContent = MarkdownFileService.readUtf8(tempFile);

        assertEquals(content, readContent);
        assertTrue(readContent.contains("UTF-8 — world"));
    }

    @Test
    void writesContentUsingNormalizedUtf8Encoding() throws IOException {
        Path tempFile = Files.createTempFile("docguard-utf8-", ".md");
        String content = "Intro\n<!-- GENERATED:START -->\nGenerated section\n<!-- GENERATED:END -->\nOutro\n";

        MarkdownFileService.writeUtf8(tempFile, content);
        byte[] bytes = Files.readAllBytes(tempFile);

        assertEquals("Intro\n<!-- GENERATED:START -->\nGenerated section\n<!-- GENERATED:END -->\nOutro\n",
                new String(bytes, StandardCharsets.UTF_8));
    }
}
