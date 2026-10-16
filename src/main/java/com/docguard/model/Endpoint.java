package com.docguard.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public final class Endpoint {
    private final String method;
    private final String path;
    private final List<String> pathVariables;
    private final List<String> queryParams;
    private final String requestBody;
    private final String responseType;
    private final String sourceClass;
    private final String sourceMethod;
    private final List<String> diagnostics;

    public Endpoint(String method, String path, List<String> pathVariables, List<String> queryParams,
                    String requestBody, String responseType, String sourceClass, String sourceMethod,
                    List<String> diagnostics) {
        this.method = method;
        this.path = path;
        this.pathVariables = pathVariables == null ? List.of() : List.copyOf(pathVariables);
        this.queryParams = queryParams == null ? List.of() : List.copyOf(queryParams);
        this.requestBody = requestBody;
        this.responseType = responseType;
        this.sourceClass = sourceClass;
        this.sourceMethod = sourceMethod;
        this.diagnostics = diagnostics == null ? List.of() : List.copyOf(diagnostics);
    }

    public String getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public List<String> getPathVariables() {
        return pathVariables;
    }

    public List<String> getQueryParams() {
        return queryParams;
    }

    public String getRequestBody() {
        return requestBody;
    }

    public String getResponseType() {
        return responseType;
    }

    public String getSourceClass() {
        return sourceClass;
    }

    public String getSourceMethod() {
        return sourceMethod;
    }

    public List<String> getDiagnostics() {
        return diagnostics;
    }

    public static final Comparator<Endpoint> CANONICAL_ORDER = Comparator
            .comparing(Endpoint::getSourceClass, Comparator.nullsFirst(String::compareTo))
            .thenComparing(Endpoint::getPath, Comparator.nullsFirst(String::compareTo))
            .thenComparing(Endpoint::getMethod, Comparator.nullsFirst(String::compareTo))
            .thenComparing(Endpoint::getSourceMethod, Comparator.nullsFirst(String::compareTo));

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Endpoint endpoint = (Endpoint) o;
        return Objects.equals(method, endpoint.method)
                && Objects.equals(path, endpoint.path)
                && Objects.equals(pathVariables, endpoint.pathVariables)
                && Objects.equals(queryParams, endpoint.queryParams)
                && Objects.equals(requestBody, endpoint.requestBody)
                && Objects.equals(responseType, endpoint.responseType)
                && Objects.equals(sourceClass, endpoint.sourceClass)
                && Objects.equals(sourceMethod, endpoint.sourceMethod)
                && Objects.equals(diagnostics, endpoint.diagnostics);
    }

    @Override
    public int hashCode() {
        return Objects.hash(method, path, pathVariables, queryParams, requestBody, responseType,
                sourceClass, sourceMethod, diagnostics);
    }
}
