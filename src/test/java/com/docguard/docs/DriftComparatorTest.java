package com.docguard.docs;

import com.docguard.model.Endpoint;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DriftComparatorTest {

    @Test
    void reportsAddedEndpoint() {
        Endpoint expected = new Endpoint(
                "GET",
                "/api/users",
                List.of(),
                List.of(),
                "none",
                "List<String>",
                "UserController",
                "listUsers",
                List.of()
        );

        DriftComparator.DriftReport report = DriftComparator.compare(List.of(expected), List.of());

        assertTrue(report.hasDrift());
        assertEquals(1, report.added().size());
        assertEquals(0, report.removed().size());
    }

    @Test
    void reportsRemovedEndpoint() {
        Endpoint actual = new Endpoint(
                "DELETE",
                "/api/users/{id}",
                List.of("id"),
                List.of(),
                "none",
                "void",
                "UserController",
                "deleteUser",
                List.of()
        );

        DriftComparator.DriftReport report = DriftComparator.compare(List.of(), List.of(actual));

        assertTrue(report.hasDrift());
        assertEquals(0, report.added().size());
        assertEquals(1, report.removed().size());
    }

    @Test
    void reportsChangedEndpointField() {
        Endpoint expected = new Endpoint(
                "GET",
                "/api/users/{userId}",
                List.of("userId"),
                List.of("page"),
                "UserDto",
                "String",
                "UserController",
                "listUsers",
                List.of()
        );
        Endpoint actual = new Endpoint(
                "GET",
                "/api/users/{userId}",
                List.of("userId"),
                List.of("page", "size"),
                "UserDto",
                "String",
                "UserController",
                "listUsers",
                List.of()
        );

        DriftComparator.DriftReport report = DriftComparator.compare(
                List.of(expected),
                List.of(actual)
        );

        assertTrue(report.hasDrift());
        assertEquals(1, report.changed().size());
        assertEquals("queryParams", report.changed().get(0).fieldChanges().get(0).field());
    }

    @Test
    void formatsFieldLevelChangesClearly() {
        Endpoint expected = new Endpoint(
                "GET",
                "/api/users",
                List.of(),
                List.of("page"),
                "UserDto",
                "String",
                "UserController",
                "listUsers",
                List.of()
        );
        Endpoint actual = new Endpoint(
                "GET",
                "/api/users",
                List.of(),
                List.of("page", "size"),
                "UserDto",
                "String",
                "UserController",
                "listUsers",
                List.of()
        );

        DriftComparator.DriftReport report = DriftComparator.compare(List.of(expected), List.of(actual));
        String formatted = DriftComparator.formatReport(report);

        assertTrue(formatted.contains("Changed endpoints:"));
        assertTrue(formatted.contains("queryParams"));
        assertTrue(formatted.contains("page -> page; size"));
    }

    @Test
    void rejectsDuplicateMethodAndPathAsAmbiguous() {
        Endpoint first = new Endpoint(
                "GET",
                "/api/users",
                List.of(),
                List.of(),
                "none",
                "String",
                "UserController",
                "listUsers",
                List.of()
        );
        Endpoint second = new Endpoint(
                "GET",
                "/api/users",
                List.of(),
                List.of(),
                "none",
                "String",
                "UserController",
                "findUsers",
                List.of()
        );

        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class,
                () -> DriftComparator.compare(List.of(first, second), List.of()));
    }
}
