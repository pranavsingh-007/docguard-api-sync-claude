package com.docguard.parser;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseProblemException;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.type.Type;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public final class JavaParserAdapter {
    private final JavaParser javaParser;

    public JavaParserAdapter() {
        this.javaParser = new JavaParser();
    }

    public CompilationUnit parse(Path javaFile) {
        if (javaFile == null || javaFile.toString().isBlank()) {
            throw new IllegalArgumentException("Java file path must not be null or blank.");
        }

        if (!Files.exists(javaFile)) {
            throw new IllegalArgumentException("Java file does not exist: " + javaFile);
        }

        try {
            ParseResult<CompilationUnit> parseResult = javaParser.parse(javaFile);
            if (!parseResult.isSuccessful()) {
                String message = parseResult.getProblems().stream()
                        .map(problem -> problem.getMessage())
                        .collect(Collectors.joining("; "));
                throw new IllegalArgumentException("Unable to parse Java source file: " + javaFile + " -> " + message);
            }
            return parseResult.getResult().orElseThrow(() ->
                    new IllegalArgumentException("Java parser returned no compilation unit for: " + javaFile));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read Java source file: " + javaFile, e);
        }
    }

    public List<ClassOrInterfaceDeclaration> findTopLevelClasses(CompilationUnit compilationUnit) {
        if (compilationUnit == null) {
            return List.of();
        }
        return compilationUnit.findAll(ClassOrInterfaceDeclaration.class).stream()
                .filter(node -> node.getParentNode().map(parent -> parent instanceof CompilationUnit).orElse(false))
                .collect(Collectors.toList());
    }

    public List<MethodDeclaration> findMethods(ClassOrInterfaceDeclaration typeDeclaration) {
        if (typeDeclaration == null) {
            return List.of();
        }
        return new ArrayList<>(typeDeclaration.getMethods());
    }

    public String getTypeName(Parameter parameter) {
        if (parameter == null) {
            return "unknown";
        }
        Type type = parameter.getType();
        return type == null ? "unknown" : type.asString();
    }
}
