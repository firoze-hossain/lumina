package dev.lumina.ui;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CodeGuidesScannerTest {

    private final String sampleJavaCode = """
            package dev.lumina.codegen;

            public final class JavaCodeGenerator {

                public static Preview previewGetter(String source, String fieldName) {
                    ClassModel model = parseModel(source);
                    return new Preview(1, "code");
                }

                public static Preview previewSetter(String source, String fieldName) {
                    ClassModel model = parseModel(source);
                    return new Preview(1, "code");
                }

                public static Preview previewAddConstructorParam(String source, String fieldName) {
                    ClassModel model = parseModel(source);
                    FieldInfo field = model.findField(fieldName);

                    if (model.constructors.isEmpty()) {
                        int line = model.findFirstMethodOrConstructorLine();
                        return new Preview(line, "code");
                    } else {
                        ConstructorInfo firstCtor = model.constructors.get(0);
                        return new Preview(firstCtor.startLine(), "code");
                    }
                }

                public static Preview previewRemoveField(String source, String fieldName) {
                    return new Preview(1, "removed");
                }
            }
            """;

    @Test
    void testMethodSeparatorsDetection() {
        CodeGuidesScanner.GuidesResult result = CodeGuidesScanner.scan(sampleJavaCode, "JavaCodeGenerator.java");
        assertNotNull(result);
        assertFalse(result.methodSeparatorLines().isEmpty(), "Method separators must be detected");

        // Methods are declared at lines 4, 9, 14, 26 (0-based)
        List<Integer> methodLines = result.methodSeparatorLines();
        assertTrue(methodLines.contains(4), "previewGetter should have method separator");
        assertTrue(methodLines.contains(9), "previewSetter should have method separator");
        assertTrue(methodLines.contains(14), "previewAddConstructorParam should have method separator");
        assertTrue(methodLines.contains(27), "previewRemoveField should have method separator");
    }

    @Test
    void testBlockScopesAndIndentColumns() {
        CodeGuidesScanner.GuidesResult result = CodeGuidesScanner.scan(sampleJavaCode, "JavaCodeGenerator.java");
        List<CodeGuidesScanner.BlockScope> scopes = result.blockScopes();
        assertNotNull(scopes);
        assertFalse(scopes.isEmpty());

        // Class scope at column 0
        CodeGuidesScanner.BlockScope classScope = scopes.stream()
                .filter(s -> "JavaCodeGenerator".equals(s.name()))
                .findFirst()
                .orElse(null);
        assertNotNull(classScope);
        assertEquals(0, classScope.indentColumn());

        // Method previewGetter scope at column 4
        CodeGuidesScanner.BlockScope methodScope = scopes.stream()
                .filter(s -> "previewGetter".equals(s.name()))
                .findFirst()
                .orElse(null);
        assertNotNull(methodScope);
        assertEquals(4, methodScope.indentColumn());
        assertTrue(methodScope.isMethod());

        // 'if' block scope inside previewAddConstructorParam at column 8
        CodeGuidesScanner.BlockScope ifScope = scopes.stream()
                .filter(s -> "if".equals(s.name()))
                .findFirst()
                .orElse(null);
        assertNotNull(ifScope);
        assertEquals(8, ifScope.indentColumn());

        // 'else' block scope at column 8
        CodeGuidesScanner.BlockScope elseScope = scopes.stream()
                .filter(s -> "else".equals(s.name()))
                .findFirst()
                .orElse(null);
        assertNotNull(elseScope);
        assertEquals(8, elseScope.indentColumn());
    }

    @Test
    void testActiveScopeResolution() {
        CodeGuidesScanner.GuidesResult result = CodeGuidesScanner.scan(sampleJavaCode, "JavaCodeGenerator.java");
        List<CodeGuidesScanner.BlockScope> scopes = result.blockScopes();

        // Line 19 is inside: "int line = model.findFirstMethodOrConstructorLine();" (inside 'if')
        CodeGuidesScanner.BlockScope activeInIf = CodeGuidesScanner.findActiveScope(scopes, 19);
        assertNotNull(activeInIf);
        assertEquals("if", activeInIf.name());
        assertEquals(8, activeInIf.indentColumn());

        // Line 16 is: "FieldInfo field = model.findField(fieldName);" (inside previewAddConstructorParam, outside if)
        CodeGuidesScanner.BlockScope activeInMethod = CodeGuidesScanner.findActiveScope(scopes, 16);
        assertNotNull(activeInMethod);
        assertEquals("previewAddConstructorParam", activeInMethod.name());
        assertEquals(4, activeInMethod.indentColumn());

        // Line 2 is: "public final class JavaCodeGenerator {" (class scope)
        CodeGuidesScanner.BlockScope activeInClass = CodeGuidesScanner.findActiveScope(scopes, 2);
        assertNotNull(activeInClass);
        assertEquals("JavaCodeGenerator", activeInClass.name());
    }

    @Test
    void testFallbackBracesScanner() {
        String nonJavaCode = """
                function outer() {
                    const a = 1;
                    if (a > 0) {
                        console.log(a);
                    }
                }
                """;
        CodeGuidesScanner.GuidesResult result = CodeGuidesScanner.scan(nonJavaCode, "script.js");
        assertNotNull(result);
        assertFalse(result.blockScopes().isEmpty());

        // Outer scope at column 0
        assertEquals(0, result.blockScopes().get(0).indentColumn());

        // Inner 'if' scope at column 4
        CodeGuidesScanner.BlockScope ifScope = result.blockScopes().stream()
                .filter(s -> s.indentColumn() == 4)
                .findFirst()
                .orElse(null);
        assertNotNull(ifScope);
        assertEquals("if", ifScope.name());
    }

    @Test
    void testAnnotatedMethodSeparatorPlacement() {
        String annotatedCode = """
                package com.example.service;

                public class BrandServiceImpl {

                    // Creates a brand
                    @Override
                    @Transactional
                    public BrandResponse createBrand(BrandRequest request) {
                        if (request == null) {
                            throw new IllegalArgumentException();
                        }
                        return new BrandResponse();
                    }
                }
                """;

        CodeGuidesScanner.GuidesResult result = CodeGuidesScanner.scan(annotatedCode, "BrandServiceImpl.java");
        assertNotNull(result);
        List<Integer> methodSeps = result.methodSeparatorLines();
        assertFalse(methodSeps.isEmpty(), "Must detect method separator");

        // Line 4 is: "// Creates a brand" (0-based)
        // Separator line must be at line 4 (above comments and annotations), NEVER at line 6 or 7
        assertEquals(4, methodSeps.get(0).intValue(),
                "Method separator must be placed above comments and annotations, not between annotations");
    }

    @Test
    void testMethodBodyAndIfBlockScopesExactBounds() {
        String annotatedCode = """
                package com.example.service;

                public class BrandServiceImpl {

                    @Override
                    @Transactional
                    public BrandResponse createBrand(BrandRequest request) {
                        if (request == null) {
                            throw new IllegalArgumentException();
                        }
                        return new BrandResponse();
                    }
                }
                """;

        CodeGuidesScanner.GuidesResult result = CodeGuidesScanner.scan(annotatedCode, "BrandServiceImpl.java");
        assertNotNull(result);
        List<CodeGuidesScanner.BlockScope> scopes = result.blockScopes();

        // Find createBrand scope
        CodeGuidesScanner.BlockScope methodScope = scopes.stream()
                .filter(s -> "createBrand".equals(s.name()))
                .findFirst()
                .orElse(null);
        assertNotNull(methodScope);
        // Line 6 is "public BrandResponse createBrand(...) {" (0-based)
        // Line 11 is "    }" (0-based)
        assertEquals(6, methodScope.startLine(), "Method block must start at the line with opening brace {");
        assertEquals(11, methodScope.endLine(), "Method block must end at the line with closing brace }");
        assertEquals(4, methodScope.indentColumn(), "Method indent column must be 4");

        // Find if block scope
        CodeGuidesScanner.BlockScope ifScope = scopes.stream()
                .filter(s -> "if".equals(s.name()))
                .findFirst()
                .orElse(null);
        assertNotNull(ifScope);
        // Line 7 is "        if (request == null) {" (0-based)
        // Line 9 is "        }" (0-based)
        assertEquals(7, ifScope.startLine(), "If block must start at the line with opening brace {");
        assertEquals(9, ifScope.endLine(), "If block must end at the line with closing brace }");
        assertEquals(8, ifScope.indentColumn(), "If block indent column must be 8");
    }
}
