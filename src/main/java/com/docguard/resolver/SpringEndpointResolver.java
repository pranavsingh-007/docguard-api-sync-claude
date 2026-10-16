package com.docguard.resolver;

import com.docguard.model.Endpoint;
import com.docguard.parser.JavaParserAdapter;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.ArrayInitializerExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public final class SpringEndpointResolver {
    private static final Set<String> SUPPORTED_MAPPING_ANNOTATIONS = Set.of(
            "RequestMapping",
            "GetMapping",
            "PostMapping",
            "PutMapping",
            "PatchMapping",
            "DeleteMapping"
    );

    private final JavaParserAdapter javaParserAdapter;

    public SpringEndpointResolver() {
        this(new JavaParserAdapter());
    }

    public SpringEndpointResolver(JavaParserAdapter javaParserAdapter) {
        this.javaParserAdapter = Objects.requireNonNull(javaParserAdapter, "JavaParserAdapter must not be null.");
    }

    public List<Endpoint> resolve(Path javaFile) {
        if (javaFile == null) {
            throw new IllegalArgumentException("Java file path must not be null.");
        }

        CompilationUnit compilationUnit = javaParserAdapter.parse(javaFile);
        List<Endpoint> endpoints = new ArrayList<>();

        for (ClassOrInterfaceDeclaration type : javaParserAdapter.findTopLevelClasses(compilationUnit)) {
            List<String> classPaths = extractPathsFromAnnotations(type.getAnnotations(), true);
            List<String> classMethods = extractHttpMethodsFromAnnotations(type.getAnnotations(), true);

            for (MethodDeclaration method : javaParserAdapter.findMethods(type)) {
                boolean hasMethodLevelMapping = method.getAnnotations().stream()
                        .map(AnnotationExpr::getNameAsString)
                        .anyMatch(SUPPORTED_MAPPING_ANNOTATIONS::contains);
                if (!hasMethodLevelMapping) {
                    continue;
                }

                List<String> mappingPaths = extractPathsFromAnnotations(method.getAnnotations(), false);
                List<String> mappingMethods = extractHttpMethodsFromAnnotations(method.getAnnotations(), false);

                List<String> combinedPaths = combinePaths(classPaths, mappingPaths);
                List<String> combinedMethods = combineMethods(classMethods, mappingMethods);
                if (combinedPaths.isEmpty()) {
                    combinedPaths = List.of("");
                }
                if (combinedMethods.isEmpty()) {
                    combinedMethods = List.of("ALL");
                }

                List<String> pathVariables = extractPathVariables(method);
                List<String> queryParams = extractQueryParams(method);
                String requestBody = extractRequestBody(method);
                String responseType = method.getType().asString();

                for (String path : combinedPaths) {
                    for (String httpMethod : combinedMethods) {
                        endpoints.add(new Endpoint(
                                httpMethod,
                                normalizePath(classPaths, path),
                                pathVariables,
                                queryParams,
                                requestBody,
                                responseType,
                                type.getNameAsString(),
                                method.getNameAsString(),
                                List.of()
                        ));
                    }
                }
            }
        }

        endpoints.sort(Endpoint.CANONICAL_ORDER);
        return endpoints;
    }

    private List<String> extractPathsFromAnnotations(List<AnnotationExpr> annotations, boolean isClassLevel) {
        List<String> paths = new ArrayList<>();
        for (AnnotationExpr annotation : annotations) {
            String annotationName = annotation.getNameAsString();
            if (!SUPPORTED_MAPPING_ANNOTATIONS.contains(annotationName)) {
                continue;
            }

            List<String> values = extractStringValues(annotation, "path", "value");
            if (!values.isEmpty()) {
                paths.addAll(values);
                continue;
            }

            if (annotationName.equals("RequestMapping") && isClassLevel) {
                paths.add("");
            }
        }
        return deDuplicate(paths);
    }

    private List<String> extractHttpMethodsFromAnnotations(List<AnnotationExpr> annotations, boolean isClassLevel) {
        List<String> httpMethods = new ArrayList<>();
        for (AnnotationExpr annotation : annotations) {
            String annotationName = annotation.getNameAsString();
            if (!SUPPORTED_MAPPING_ANNOTATIONS.contains(annotationName)) {
                continue;
            }

            if (annotationName.equals("GetMapping")) {
                httpMethods.add("GET");
                continue;
            }
            if (annotationName.equals("PostMapping")) {
                httpMethods.add("POST");
                continue;
            }
            if (annotationName.equals("PutMapping")) {
                httpMethods.add("PUT");
                continue;
            }
            if (annotationName.equals("PatchMapping")) {
                httpMethods.add("PATCH");
                continue;
            }
            if (annotationName.equals("DeleteMapping")) {
                httpMethods.add("DELETE");
                continue;
            }
            if (annotationName.equals("RequestMapping")) {
                List<String> methods = extractMethodValues(annotation);
                if (methods.isEmpty()) {
                    httpMethods.add("ALL");
                } else {
                    httpMethods.addAll(methods);
                }
            }
        }
        return deDuplicate(httpMethods);
    }

    private List<String> extractMethodValues(AnnotationExpr annotationExpr) {
        if (annotationExpr instanceof SingleMemberAnnotationExpr) {
            return List.of();
        }
        if (annotationExpr instanceof NormalAnnotationExpr normalAnnotationExpr) {
            for (MemberValuePair pair : normalAnnotationExpr.getPairs()) {
                if ("method".equals(pair.getNameAsString())) {
                    return parseEnumValues(pair.getValue());
                }
            }
        }
        return List.of();
    }

    private List<String> extractStringValues(AnnotationExpr annotationExpr, String... names) {
        List<String> values = new ArrayList<>();

        if (annotationExpr instanceof SingleMemberAnnotationExpr singleMember) {
            values.addAll(parseStringValues(singleMember.getMemberValue()));
            return values;
        }

        if (annotationExpr instanceof NormalAnnotationExpr normalAnnotationExpr) {
            for (MemberValuePair pair : normalAnnotationExpr.getPairs()) {
                String name = pair.getNameAsString();
                if (List.of(names).contains(name)) {
                    values.addAll(parseStringValues(pair.getValue()));
                }
            }
        }

        return values;
    }

    private List<String> parseStringValues(Expression expression) {
        List<String> values = new ArrayList<>();
        if (expression == null) {
            return values;
        }

        if (expression.isStringLiteralExpr()) {
            values.add(expression.asStringLiteralExpr().getValue());
            return values;
        }

        if (expression.isArrayInitializerExpr()) {
            ArrayInitializerExpr initializer = expression.asArrayInitializerExpr();
            for (Expression item : initializer.getValues()) {
                if (item.isStringLiteralExpr()) {
                    values.add(item.asStringLiteralExpr().getValue());
                }
            }
            return values;
        }

        if (expression.isNameExpr()) {
            values.add(expression.asNameExpr().getNameAsString());
        }

        return values;
    }

    private List<String> parseEnumValues(Expression expression) {
        List<String> values = new ArrayList<>();
        if (expression == null) {
            return values;
        }

        if (expression.isArrayInitializerExpr()) {
            for (Expression item : expression.asArrayInitializerExpr().getValues()) {
                values.add(item.toString().replace("RequestMethod.", ""));
            }
            return values;
        }

        if (expression.isFieldAccessExpr()) {
            values.add(expression.toString().replace("RequestMethod.", ""));
            return values;
        }

        return values;
    }

    private List<String> combinePaths(List<String> classPaths, List<String> methodPaths) {
        if (classPaths.isEmpty()) {
            return methodPaths;
        }
        if (methodPaths.isEmpty()) {
            return classPaths;
        }

        List<String> combined = new ArrayList<>();
        for (String classPath : classPaths) {
            String classPrefix = classPath == null || classPath.isBlank() ? "" : classPath;
            for (String methodPath : methodPaths) {
                if (methodPath == null || methodPath.isBlank()) {
                    combined.add(classPrefix);
                } else if (classPrefix.isBlank()) {
                    combined.add(methodPath);
                } else {
                    combined.add(joinPaths(classPrefix, methodPath));
                }
            }
        }
        return deDuplicate(combined);
    }

    private List<String> combineMethods(List<String> classMethods, List<String> methodMethods) {
        List<String> classMethodsFiltered = classMethods == null ? List.of() : classMethods.stream()
                .filter(method -> method != null && !"ALL".equals(method))
                .collect(Collectors.toList());
        List<String> methodMethodsFiltered = methodMethods == null ? List.of() : methodMethods.stream()
                .filter(method -> method != null && !"ALL".equals(method))
                .collect(Collectors.toList());

        if (!classMethodsFiltered.isEmpty() && !methodMethodsFiltered.isEmpty()) {
            Set<String> combined = new LinkedHashSet<>();
            combined.addAll(classMethodsFiltered);
            combined.addAll(methodMethodsFiltered);
            return new ArrayList<>(combined);
        }
        if (!classMethodsFiltered.isEmpty()) {
            return classMethodsFiltered;
        }
        if (!methodMethodsFiltered.isEmpty()) {
            return methodMethodsFiltered;
        }
        if (classMethods != null && classMethods.contains("ALL") && (methodMethods == null || methodMethods.isEmpty())) {
            return List.of("ALL");
        }
        if (methodMethods != null && methodMethods.contains("ALL") && (classMethods == null || classMethods.isEmpty())) {
            return List.of("ALL");
        }
        if (classMethods != null && classMethods.contains("ALL") && methodMethods != null && methodMethods.contains("ALL")) {
            return List.of("ALL");
        }
        return List.of();
    }

    private List<String> extractPathVariables(MethodDeclaration method) {
        List<String> names = new ArrayList<>();
        for (com.github.javaparser.ast.body.Parameter parameter : method.getParameters()) {
            for (AnnotationExpr annotation : parameter.getAnnotations()) {
                if (annotation.getNameAsString().equals("PathVariable")) {
                    names.add(parameter.getNameAsString());
                }
            }
        }
        return names;
    }

    private List<String> extractQueryParams(MethodDeclaration method) {
        List<String> names = new ArrayList<>();
        for (com.github.javaparser.ast.body.Parameter parameter : method.getParameters()) {
            for (AnnotationExpr annotation : parameter.getAnnotations()) {
                if (annotation.getNameAsString().equals("RequestParam")) {
                    names.add(parameter.getNameAsString());
                }
            }
        }
        return names;
    }

    private String extractRequestBody(MethodDeclaration method) {
        for (com.github.javaparser.ast.body.Parameter parameter : method.getParameters()) {
            for (AnnotationExpr annotation : parameter.getAnnotations()) {
                if (annotation.getNameAsString().equals("RequestBody")) {
                    return parameter.getType().asString();
                }
            }
        }
        return "none";
    }

    private String normalizePath(List<String> classPaths, String path) {
        if (path == null) {
            return classPaths == null || classPaths.isEmpty() ? "" : classPaths.get(0).trim();
        }
        return path.trim();
    }

    private String joinPaths(String left, String right) {
        String normalizedLeft = left == null ? "" : left.trim();
        String normalizedRight = right == null ? "" : right.trim();

        if (normalizedLeft.isBlank()) {
            return normalizedRight;
        }
        if (normalizedRight.isBlank()) {
            return normalizedLeft;
        }
        if (normalizedLeft.endsWith("/") && normalizedRight.startsWith("/")) {
            return normalizedLeft + normalizedRight.substring(1);
        }
        if (normalizedLeft.endsWith("/") || normalizedRight.startsWith("/")) {
            return normalizedLeft + normalizedRight;
        }
        return normalizedLeft + "/" + normalizedRight;
    }

    private List<String> deDuplicate(List<String> values) {
        return new ArrayList<>(new LinkedHashSet<>(values));
    }
}
