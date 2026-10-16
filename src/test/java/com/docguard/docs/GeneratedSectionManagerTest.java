package com.docguard.docs;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeneratedSectionManagerTest {

    @Test
    void replacesGeneratedSectionWhilePreservingManualContent() {
        String original = "# Manual docs\n\nBefore\n<!-- GENERATED:START -->\nold generated section\n<!-- GENERATED:END -->\nAfter\n";

        String updated = GeneratedSectionManager.replaceGeneratedSection(original, "\nNew generated section\n");

        assertTrue(updated.startsWith("# Manual docs\n\nBefore\n"));
        assertTrue(updated.contains("<!-- GENERATED:START -->\nNew generated section\n<!-- GENERATED:END -->"));
        assertTrue(updated.endsWith("\nAfter\n"));
    }

    @Test
    void validatesCorrectMarkerStructure() {
        String validDoc = "Intro\n<!-- GENERATED:START -->\nold\n<!-- GENERATED:END -->\nOutro\n";

        GeneratedSectionManager.validateStructure(validDoc);
    }

    @Test
    void throwsWhenStartMarkerIsMissing() {
        String invalidDoc = "Intro\n<!-- GENERATED:END -->\nOutro\n";

        assertThrows(IllegalStateException.class, () -> GeneratedSectionManager.validateStructure(invalidDoc));
    }

    @Test
    void throwsWhenEndMarkerIsMissing() {
        String invalidDoc = "Intro\n<!-- GENERATED:START -->\nold\nOutro\n";

        assertThrows(IllegalStateException.class, () -> GeneratedSectionManager.validateStructure(invalidDoc));
    }

    @Test
    void throwsWhenDuplicateMarkersArePresent() {
        String invalidDoc = "Intro\n<!-- GENERATED:START -->\nold\n<!-- GENERATED:START -->\nmore\n<!-- GENERATED:END -->\nOutro\n";

        assertThrows(IllegalStateException.class, () -> GeneratedSectionManager.validateStructure(invalidDoc));
    }
}
