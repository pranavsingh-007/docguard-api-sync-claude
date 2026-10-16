package com.docguard.resolver;

import com.docguard.model.Endpoint;
import com.docguard.model.Parameter;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpringEndpointResolverTest {

    @Test
    void resolvesClassLevelAndMethodLevelMappings() throws IOException {
        Path sourceFile = Files.createTempFile("docguard-endpoints-", ".java");
        Files.writeString(sourceFile, "import org.springframework.web.bind.annotation.*;\n" +
                "@RestController\n" +
                "@RequestMapping(path = \"/api\")\n" +
                "class UserController {\n" +
                "  @GetMapping(path = \"/users\")\n" +
                "  public String listUsers() { return \"x\"; }\n" +
                "}\n");

        SpringEndpointResolver resolver = new SpringEndpointResolver();
        List<Endpoint> endpoints = resolver.resolve(sourceFile);

        assertEquals(1, endpoints.size());
        Endpoint endpoint = endpoints.get(0);
        assertEquals("GET", endpoint.getMethod());
        assertEquals("/api/users", endpoint.getPath());
        assertEquals("UserController", endpoint.getSourceClass());
        assertEquals("listUsers", endpoint.getSourceMethod());
        assertEquals("String", endpoint.getResponseType());
    }

    @Test
    void resolvesClassLevelPrefixWithoutCreatingSyntheticEndpoints() throws IOException {
        Path sourceFile = Files.createTempFile("docguard-prefix-regression-", ".java");
        Files.writeString(sourceFile, "import org.springframework.web.bind.annotation.*;\n" +
                "@RestController\n" +
                "@RequestMapping(\"/api\")\n" +
                "class UserController {\n" +
                "  @GetMapping(\"/users/{userId}\")\n" +
                "  public UserDto getUser(@PathVariable String userId, @RequestParam(required = false) String expand) { return new UserDto(); }\n" +
                "}\n" +
                "class UserDto {}\n");

        SpringEndpointResolver resolver = new SpringEndpointResolver();
        List<Endpoint> endpoints = resolver.resolve(sourceFile);

        assertEquals(1, endpoints.size());
        Endpoint endpoint = endpoints.get(0);
        assertEquals("GET", endpoint.getMethod());
        assertEquals("/api/users/{userId}", endpoint.getPath());
        assertEquals("UserController", endpoint.getSourceClass());
        assertEquals("getUser", endpoint.getSourceMethod());
        assertTrue(endpoints.stream().noneMatch(e -> "/api".equals(e.getMethod())));
    }

    @Test
    void resolvesMultipleMethodsAndPaths() throws IOException {
        Path sourceFile = Files.createTempFile("docguard-multi-", ".java");
        Files.writeString(sourceFile, "import org.springframework.web.bind.annotation.*;\n" +
                "@RestController\n" +
                "class MultiController {\n" +
                "  @RequestMapping(path = {\"/one\", \"/two\"}, method = {RequestMethod.GET, RequestMethod.POST})\n" +
                "  public String route() { return \"x\"; }\n" +
                "}\n");

        SpringEndpointResolver resolver = new SpringEndpointResolver();
        List<Endpoint> endpoints = resolver.resolve(sourceFile);

        assertEquals(4, endpoints.size());
        assertTrue(endpoints.stream().anyMatch(e -> "/one".equals(e.getPath()) && "GET".equals(e.getMethod())));
        assertTrue(endpoints.stream().anyMatch(e -> "/two".equals(e.getPath()) && "POST".equals(e.getMethod())));
    }

    @Test
    void extractsPathVariableQueryParamAndRequestBody() throws IOException {
        Path sourceFile = Files.createTempFile("docguard-params-", ".java");
        Files.writeString(sourceFile, "import org.springframework.web.bind.annotation.*;\n" +
                "@RestController\n" +
                "class ParamController {\n" +
                "  @GetMapping(\"/users/{userId}\")\n" +
                "  public String getUser(@PathVariable String userId, @RequestParam String page, @RequestBody UserDto dto) { return \"x\"; }\n" +
                "}\n" +
                "class UserDto {}\n");

        SpringEndpointResolver resolver = new SpringEndpointResolver();
        List<Endpoint> endpoints = resolver.resolve(sourceFile);

        assertEquals(1, endpoints.size());
        Endpoint endpoint = endpoints.get(0);
        assertEquals(List.of("userId"), endpoint.getPathVariables());
        assertEquals(List.of("page"), endpoint.getQueryParams());
        assertEquals("UserDto", endpoint.getRequestBody());
    }

    @Test
    void rejectsUnsupportedMappingStyle() throws IOException {
        Path sourceFile = Files.createTempFile("docguard-unsupported-", ".java");
        Files.writeString(sourceFile, "import org.springframework.web.bind.annotation.*;\n" +
                "@RestController\n" +
                "class UnsupportedController {\n" +
                "  @CustomMapping(\"/x\")\n" +
                "  public String route() { return \"x\"; }\n" +
                "}\n");

        SpringEndpointResolver resolver = new SpringEndpointResolver();
        List<Endpoint> endpoints = resolver.resolve(sourceFile);

        assertTrue(endpoints.isEmpty());
    }
}
