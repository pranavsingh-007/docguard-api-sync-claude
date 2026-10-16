package com.docguard.docs;

import com.docguard.model.Endpoint;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public final class MarkdownGenerator {
    public static final String EMPTY_SECTION_MESSAGE = "No source-derived endpoints available.";

    private MarkdownGenerator() {
    }

    public static String generate(List<Endpoint> endpoints) {
        List<Endpoint> orderedEndpoints = endpoints == null ? List.of() : endpoints.stream()
                .sorted(Endpoint.CANONICAL_ORDER)
                .toList();

        StringBuilder builder = new StringBuilder();
        builder.append("## Generated API Endpoints\n\n");

        if (orderedEndpoints.isEmpty()) {
            builder.append(EMPTY_SECTION_MESSAGE)
                    .append("\n");
            return builder.toString();
        }

        builder.append("| HTTP Method | Path | Path Variables | Query Parameters | Request Body | Response Type |\n")
                .append("| --- | --- | --- | --- | --- | --- |\n");

        for (Endpoint endpoint : orderedEndpoints) {
            builder.append("| ")
                    .append(escapeCell(endpoint.getMethod()))
                    .append(" | ")
                    .append(escapeCell(endpoint.getPath()))
                    .append(" | ")
                    .append(escapeList(endpoint.getPathVariables()))
                    .append(" | ")
                    .append(escapeList(endpoint.getQueryParams()))
                    .append(" | ")
                    .append(escapeCell(endpoint.getRequestBody()))
                    .append(" | ")
                    .append(escapeCell(endpoint.getResponseType()))
                    .append(" |\n");
        }

        return builder.toString();
    }

    public static List<Endpoint> parseGeneratedContent(String generatedContent) {
        if (generatedContent == null || generatedContent.isBlank()) {
            return List.of();
        }

        String trimmed = generatedContent.trim();
        if (trimmed.contains(EMPTY_SECTION_MESSAGE)) {
            return List.of();
        }

        List<String> lines = Arrays.stream(trimmed.split("\\R"))
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .toList();

        List<Endpoint> endpoints = new ArrayList<>();
        for (String line : lines) {
            if (!line.startsWith("|")) {
                continue;
            }
            if (line.startsWith("| HTTP Method |") || line.startsWith("| ---")) {
                continue;
            }

            String row = line.substring(1, line.length() - 1);
            String[] cells = row.split("\\|", -1);
            if (cells.length < 6) {
                continue;
            }

            String method = unescapeCell(cells[0]).trim();
            String path = unescapeCell(cells[1]).trim();
            List<String> pathVariables = parseListValue(cells[2]);
            List<String> queryParams = parseListValue(cells[3]);
            String requestBody = unescapeCell(cells[4]).trim();
            String responseType = unescapeCell(cells[5]).trim();

            endpoints.add(new Endpoint(method, path, pathVariables, queryParams,
                    requestBody, responseType, "", "", List.of()));
        }
        return endpoints;
    }

    private static String escapeCell(String value) {
        String normalized = value == null ? "unknown" : value;
        return normalized.replace("|", "\\|");
    }

    private static String escapeList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "-";
        }
        return String.join("; ", values);
    }

    private static List<String> parseListValue(String rawValue) {
        String value = rawValue == null ? "" : rawValue.trim();
        if (value.isEmpty() || "-".equals(value)) {
            return List.of();
        }
        return Arrays.stream(value.split(";"))
                .map(part -> part.trim())
                .filter(part -> !part.isEmpty())
                .toList();
    }

    private static String unescapeCell(String value) {
        return Objects.toString(value, "").replace("\\|", "|").trim();
    }
}
