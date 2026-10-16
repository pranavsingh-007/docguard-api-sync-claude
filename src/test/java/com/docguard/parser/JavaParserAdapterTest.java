package com.docguard.parser;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class JavaParserAdapterTest {

    @Test
    void parsesValidJavaFileAndFindsClassesAndMethods() throws IOException {
        Path sourceFile = Files.createTempFile("docguard-parser-", ".java");
        Files.writeString(sourceFile, "class ExampleController {\n" +
                "  public void example() {}\n" +
                "}\n");

        JavaParserAdapter adapter = new JavaParserAdapter();
        CompilationUnit unit = adapter.parse(sourceFile);
        List<ClassOrInterfaceDeclaration> classes = adapter.findTopLevelClasses(unit);
        List<MethodDeclaration> methods = adapter.findMethods(classes.get(0));

        assertNotNull(unit);
        assertEquals(1, classes.size());
        assertEquals(1, methods.size());
        assertEquals("example", methods.get(0).getNameAsString());
    }

    @Test
    void throwsForInvalidJavaSource() throws IOException {
        Path sourceFile = Files.createTempFile("docguard-invalid-", ".java");
        Files.writeString(sourceFile, "class Broken {\n void broken( {\n }\n");

        JavaParserAdapter adapter = new JavaParserAdapter();

        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> adapter.parse(sourceFile));
    }
}
