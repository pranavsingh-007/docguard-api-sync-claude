package com.docguard.docs;

import java.util.Objects;

public final class GeneratedSectionManager {
    public static final String START_MARKER = "<!-- GENERATED:START -->";
    public static final String END_MARKER = "<!-- GENERATED:END -->";

    private GeneratedSectionManager() {
    }

    public static ParsedGeneratedSection parse(String documentContent) {
        Objects.requireNonNull(documentContent, "Document content must not be null.");

        int startIndex = documentContent.indexOf(START_MARKER);
        int endIndex = documentContent.indexOf(END_MARKER);

        if (startIndex < 0 && endIndex < 0) {
            throw new IllegalStateException("Missing generated markers: expected both start and end markers.");
        }
        if (startIndex < 0) {
            throw new IllegalStateException("Missing generated start marker: " + START_MARKER);
        }
        if (endIndex < 0) {
            throw new IllegalStateException("Missing generated end marker: " + END_MARKER);
        }
        if (startIndex > endIndex) {
            throw new IllegalStateException("Generated markers are out of order: start marker appears after end marker.");
        }

        int secondStart = documentContent.indexOf(START_MARKER, startIndex + START_MARKER.length());
        int secondEnd = documentContent.indexOf(END_MARKER, endIndex + END_MARKER.length());
        if (secondStart >= 0 || secondEnd >= 0) {
            throw new IllegalStateException("Duplicate generated markers detected.");
        }

        String before = documentContent.substring(0, startIndex);
        String generated = documentContent.substring(startIndex + START_MARKER.length(), endIndex);
        String after = documentContent.substring(endIndex + END_MARKER.length());

        return new ParsedGeneratedSection(before, generated, after);
    }

    public static void validateStructure(String documentContent) {
        parse(documentContent);
    }

    public static String replaceGeneratedSection(String documentContent, String replacementContent) {
        ParsedGeneratedSection parsed = parse(documentContent);
        String replacement = replacementContent == null ? "" : replacementContent;
        StringBuilder builder = new StringBuilder();
        builder.append(parsed.before)
                .append(START_MARKER)
                .append(replacement)
                .append(END_MARKER)
                .append(parsed.after);
        return builder.toString();
    }

    public static final class ParsedGeneratedSection {
        private final String before;
        private final String generated;
        private final String after;

        private ParsedGeneratedSection(String before, String generated, String after) {
            this.before = before;
            this.generated = generated;
            this.after = after;
        }

        public String getBefore() {
            return before;
        }

        public String getGenerated() {
            return generated;
        }

        public String getAfter() {
            return after;
        }
    }
}
