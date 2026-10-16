package com.docguard.docs;

import com.docguard.model.Endpoint;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class DriftComparator {
    private DriftComparator() {
    }

    public static DriftReport compare(List<Endpoint> expectedEndpoints, List<Endpoint> actualEndpoints) {
        validateUniqueEndpointIdentity(expectedEndpoints);
        validateUniqueEndpointIdentity(actualEndpoints);

        Map<String, Endpoint> expectedByKey = indexEndpoints(expectedEndpoints);
        Map<String, Endpoint> actualByKey = indexEndpoints(actualEndpoints);

        List<Endpoint> added = expectedByKey.entrySet().stream()
                .filter(entry -> !actualByKey.containsKey(entry.getKey()))
                .map(Map.Entry::getValue)
                .sorted(Endpoint.CANONICAL_ORDER)
                .toList();

        List<Endpoint> removed = actualByKey.entrySet().stream()
                .filter(entry -> !expectedByKey.containsKey(entry.getKey()))
                .map(Map.Entry::getValue)
                .sorted(Endpoint.CANONICAL_ORDER)
                .toList();

        List<EndpointChange> changed = expectedByKey.entrySet().stream()
                .filter(entry -> actualByKey.containsKey(entry.getKey()))
                .map(entry -> {
                    Endpoint expected = entry.getValue();
                    Endpoint actual = actualByKey.get(entry.getKey());
                    List<FieldChange> fieldChanges = detectFieldChanges(expected, actual);
                    return fieldChanges.isEmpty() ? null : new EndpointChange(expected, actual, fieldChanges);
                })
                .filter(java.util.Objects::nonNull)
                .sorted(Comparator.comparing(change -> endpointKey(change.expected())))
                .toList();

        return new DriftReport(added, removed, changed);
    }

    public static String formatReport(DriftReport driftReport) {
        if (driftReport == null || !driftReport.hasDrift()) {
            return "Documentation is synchronized.";
        }

        StringBuilder builder = new StringBuilder();
        builder.append("Documentation drift detected.\n");

        if (!driftReport.added().isEmpty()) {
            builder.append("Added endpoints:\n");
            for (Endpoint endpoint : driftReport.added()) {
                builder.append("- ").append(endpoint.getMethod()).append(" ").append(endpoint.getPath()).append("\n");
            }
        }

        if (!driftReport.removed().isEmpty()) {
            builder.append("Removed endpoints:\n");
            for (Endpoint endpoint : driftReport.removed()) {
                builder.append("- ").append(endpoint.getMethod()).append(" ").append(endpoint.getPath()).append("\n");
            }
        }

        if (!driftReport.changed().isEmpty()) {
            builder.append("Changed endpoints:\n");
            for (EndpointChange change : driftReport.changed()) {
                builder.append("- ")
                        .append(change.expected().getMethod())
                        .append(" ")
                        .append(change.expected().getPath())
                        .append("\n");
                for (FieldChange fieldChange : change.fieldChanges()) {
                    builder.append("  - ")
                            .append(fieldChange.field())
                            .append(": ")
                            .append(fieldChange.expectedValue())
                            .append(" -> ")
                            .append(fieldChange.actualValue())
                            .append("\n");
                }
            }
        }

        return builder.toString().trim();
    }

    private static void validateUniqueEndpointIdentity(List<Endpoint> endpoints) {
        Map<String, Endpoint> seen = new LinkedHashMap<>();
        if (endpoints == null) {
            return;
        }
        for (Endpoint endpoint : endpoints) {
            String identity = endpointKey(endpoint);
            if (seen.containsKey(identity)) {
                throw new IllegalStateException(
                        "Duplicate endpoint detected for HTTP method and path: " + identity +
                                ". Ambiguous endpoint definitions are not supported in v1.");
            }
            seen.put(identity, endpoint);
        }
    }

    private static Map<String, Endpoint> indexEndpoints(List<Endpoint> endpoints) {
        Map<String, Endpoint> index = new LinkedHashMap<>();
        if (endpoints == null) {
            return index;
        }
        for (Endpoint endpoint : endpoints) {
            index.put(endpointKey(endpoint), endpoint);
        }
        return index;
    }

    private static List<FieldChange> detectFieldChanges(Endpoint expected, Endpoint actual) {
        List<FieldChange> fieldChanges = new ArrayList<>();
        addFieldChange("method", expected.getMethod(), actual.getMethod(), fieldChanges);
        addFieldChange("path", expected.getPath(), actual.getPath(), fieldChanges);
        addFieldChange("pathVariables", joinList(expected.getPathVariables()), joinList(actual.getPathVariables()), fieldChanges);
        addFieldChange("queryParams", joinList(expected.getQueryParams()), joinList(actual.getQueryParams()), fieldChanges);
        addFieldChange("requestBody", expected.getRequestBody(), actual.getRequestBody(), fieldChanges);
        addFieldChange("responseType", expected.getResponseType(), actual.getResponseType(), fieldChanges);
        return fieldChanges;
    }

    private static void addFieldChange(String fieldName, String expected, String actual, List<FieldChange> fieldChanges) {
        String expectedValue = normalize(expected);
        String actualValue = normalize(actual);
        if (!expectedValue.equals(actualValue)) {
            fieldChanges.add(new FieldChange(fieldName, expectedValue, actualValue));
        }
    }

    private static String normalize(String value) {
        return value == null ? "" : value;
    }

    private static String joinList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "-";
        }
        return String.join("; ", values);
    }

    private static String endpointKey(Endpoint endpoint) {
        return String.join("|",
                normalize(endpoint.getMethod()),
                normalize(endpoint.getPath()));
    }

    public record FieldChange(String field, String expectedValue, String actualValue) {
    }

    public record EndpointChange(Endpoint expected, Endpoint actual, List<FieldChange> fieldChanges) {
    }

    public record DriftReport(List<Endpoint> added, List<Endpoint> removed, List<EndpointChange> changed) {
        public boolean hasDrift() {
            return !(added.isEmpty() && removed.isEmpty() && changed.isEmpty());
        }
    }
}
