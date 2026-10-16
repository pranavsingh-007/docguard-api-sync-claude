package com.docguard.docs;

import com.docguard.model.Endpoint;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarkdownGeneratorTest {

    @Test
    void generatesDeterministicMarkdownWithEndpointFields() {
        Endpoint endpoint = new Endpoint(
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

        String markdown = MarkdownGenerator.generate(List.of(endpoint));

        assertTrue(markdown.contains("| HTTP Method | Path | Path Variables | Query Parameters | Request Body | Response Type |"));
        assertTrue(markdown.contains("| GET | /api/users/{userId} | userId | page; size | UserDto | String |"));
    }

    @Test
    void parsesGeneratedMarkdownBackToEndpointData() {
        String markdown = "## Generated API Endpoints\n\n"
                + "| HTTP Method | Path | Path Variables | Query Parameters | Request Body | Response Type |\n"
                + "| --- | --- | --- | --- | --- | --- |\n"
                + "| GET | /api/users/{userId} | userId | page; size | UserDto | String |\n";

        List<Endpoint> endpoints = MarkdownGenerator.parseGeneratedContent(markdown);

        assertEquals(1, endpoints.size());
        assertEquals("GET", endpoints.get(0).getMethod());
        assertEquals("/api/users/{userId}", endpoints.get(0).getPath());
        assertEquals(List.of("userId"), endpoints.get(0).getPathVariables());
        assertEquals(List.of("page", "size"), endpoints.get(0).getQueryParams());
        assertEquals("UserDto", endpoints.get(0).getRequestBody());
        assertEquals("String", endpoints.get(0).getResponseType());
    }

    @Test
    void generatesDeterministicOutputForEquivalentInput() {
        Endpoint endpoint = new Endpoint(
                "POST",
                "/api/users",
                List.of(),
                List.of("debug"),
                "CreateUserRequest",
                "Long",
                "UserController",
                "createUser",
                List.of()
        );

        String first = MarkdownGenerator.generate(List.of(endpoint));
        String second = MarkdownGenerator.generate(List.of(endpoint));

        assertEquals(first, second);
    }
}
