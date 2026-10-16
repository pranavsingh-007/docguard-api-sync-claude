package com.docguard.service;

import com.docguard.discovery.SourceFileDiscovery;
import com.docguard.model.Endpoint;
import com.docguard.parser.JavaParserAdapter;
import com.docguard.resolver.SpringEndpointResolver;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class DocGuardService {
    private final JavaParserAdapter javaParserAdapter;

    public DocGuardService() {
        this(new JavaParserAdapter());
    }

    public DocGuardService(JavaParserAdapter javaParserAdapter) {
        this.javaParserAdapter = javaParserAdapter;
    }

    public List<Endpoint> resolveEndpoints(Path sourceRoot) {
        List<Endpoint> endpoints = new ArrayList<>();
        SpringEndpointResolver resolver = new SpringEndpointResolver(javaParserAdapter);

        for (Path javaFile : SourceFileDiscovery.findJavaFiles(sourceRoot)) {
            endpoints.addAll(resolver.resolve(javaFile));
        }

        validateUniqueEndpointIdentity(endpoints);
        endpoints.sort(Endpoint.CANONICAL_ORDER);
        return endpoints;
    }

    public static void validateUniqueEndpointIdentity(List<Endpoint> endpoints) {
        Map<String, Endpoint> seen = new LinkedHashMap<>();
        for (Endpoint endpoint : endpoints) {
            String identity = endpoint.getMethod() + " " + endpoint.getPath();
            if (seen.containsKey(identity)) {
                throw new IllegalStateException(
                        "Duplicate endpoint detected for HTTP method and path: " + identity +
                                ". Ambiguous endpoint definitions are not supported in v1.");
            }
            seen.put(identity, endpoint);
        }
    }
}
