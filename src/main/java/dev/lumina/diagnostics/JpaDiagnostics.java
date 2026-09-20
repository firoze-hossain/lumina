package dev.lumina.diagnostics;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import dev.lumina.semantics.LibrarySourceService;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Inspections for JPA entity rules matching IntelliJ IDEA:
 * - Persistent entity should have primary key (@Id / @EmbeddedId / @IdClass).
 * - Dynamically resolves superclass hierarchies (e.g. @MappedSuperclass BaseEntity)
 *   so child entities extending superclasses with a primary key do not trigger false positives.
 */
public final class JpaDiagnostics {

    private static final Pattern ENTITY_ANN = Pattern.compile(
            "@(?:(?:jakarta|javax)\\.persistence\\.)?Entity\\b");
    private static final Pattern CLASS_DECL = Pattern.compile(
            "(?:public\\s+)?class\\s+([A-Za-z0-9_]+)");
    private static final Pattern ID_ANN = Pattern.compile(
            "@(?:(?:jakarta|javax)\\.persistence\\.)?(?:Id|EmbeddedId|IdClass)\\b");
    private static final Pattern EXTENDS_PATTERN = Pattern.compile(
            "\\bclass\\s+[A-Za-z0-9_]+(?:<[^>]+>)?\\s+extends\\s+([A-Za-z0-9_.]+)(?:<[^>]+>)?");
    private static final Pattern PACKAGE_PATTERN = Pattern.compile(
            "^\\s*package\\s+([A-Za-z0-9_.]+)\\s*;", Pattern.MULTILINE);
    private static final Pattern IMPORT_PATTERN = Pattern.compile(
            "^\\s*import\\s+(?:static\\s+)?([A-Za-z0-9_.*]+)\\s*;", Pattern.MULTILINE);

    private JpaDiagnostics() {}

    /** Backward-compatible overload without roots. */
    public static List<JavaDiagnostics.Diag> checkPersistentEntities(Path file, String text, String moduleName) {
        return checkPersistentEntities(file, text, List.of(), moduleName);
    }

    /**
     * Inspects Java source code for persistent entity violations.
     * When an @Entity class does not declare or inherit an @Id, @EmbeddedId, or @IdClass,
     * emits an ERROR diagnostic on the class identifier with an "Add Id attribute" quick-fix.
     */
    public static List<JavaDiagnostics.Diag> checkPersistentEntities(Path file, String text, List<Path> roots, String moduleName) {
        if (text == null || !ENTITY_ANN.matcher(text).find()) {
            return List.of();
        }

        // Check if primary key is declared directly or inherited from superclasses
        boolean hasId = hasPrimaryKey(text, file, roots, 0, new HashSet<>());

        if (hasId) {
            return List.of();
        }

        List<JavaDiagnostics.Diag> diags = new ArrayList<>();
        Matcher classMatcher = CLASS_DECL.matcher(text);
        if (classMatcher.find()) {
            String className = classMatcher.group(1);
            int start = classMatcher.start(1);
            int end = classMatcher.end(1);

            int line = 1;
            for (int i = 0; i < start; i++) {
                if (text.charAt(i) == '\n') line++;
            }

            String title = "Persistent entity '" + className + "' should have primary key";
            String message = "Persistent entity should have primary key";
            String quickFix = "add-id-attribute:" + className;

            diags.add(new JavaDiagnostics.Diag(
                    JavaDiagnostics.Severity.ERROR,
                    line,
                    start,
                    end,
                    message,
                    quickFix,
                    title,
                    null,
                    null,
                    null,
                    moduleName
            ));
        }
        return diags;
    }

    /**
     * Dynamically checks if the class text, or any superclass in its inheritance hierarchy,
     * contains a primary key definition (@Id, @EmbeddedId, @IdClass).
     */
    public static boolean hasPrimaryKey(String text, Path file, List<Path> roots, int depth, Set<String> visited) {
        if (text == null || text.isBlank() || depth > 10) {
            return false;
        }

        // 1. Direct declaration check
        if (ID_ANN.matcher(text).find()) {
            return true;
        }

        // 2. Discover extended superclasses
        List<String> superTypes = extractSuperClasses(text);
        if (superTypes.isEmpty()) {
            return false;
        }

        for (String superName : superTypes) {
            if (!visited.add(superName)) {
                continue; // Avoid cyclic inheritance
            }

            // A) Check project source files
            Path superFile = resolveSuperClassFile(superName, text, file, roots);
            if (superFile != null && Files.isRegularFile(superFile)) {
                try {
                    String superText = Files.readString(superFile);
                    if (hasPrimaryKey(superText, superFile, roots, depth + 1, visited)) {
                        return true;
                    }
                } catch (IOException ignored) {}
            }

            // B) Check library sources if not in project
            String fqcn = resolveFqcn(text, superName);
            if (fqcn != null) {
                LibrarySourceService libService = LibrarySourceService.getInstance();
                Path cachedLib = libService.getCachedSourcePath(fqcn);
                if (Files.isRegularFile(cachedLib)) {
                    try {
                        String libText = Files.readString(cachedLib);
                        if (hasPrimaryKey(libText, cachedLib, roots, depth + 1, visited)) {
                            return true;
                        }
                    } catch (IOException ignored) {}
                }
                String libSource = libService.findSourceInMavenRepo(fqcn);
                if (libSource != null && hasPrimaryKey(libSource, null, roots, depth + 1, visited)) {
                    return true;
                }
            }
        }

        return false;
    }

    private static List<String> extractSuperClasses(String text) {
        List<String> supers = new ArrayList<>();
        try {
            CompilationUnit cu = StaticJavaParser.parse(text);
            for (ClassOrInterfaceDeclaration cid : cu.findAll(ClassOrInterfaceDeclaration.class)) {
                for (ClassOrInterfaceType extendedType : cid.getExtendedTypes()) {
                    String name = extendedType.getNameAsString();
                    if (!name.isBlank() && !supers.contains(name)) {
                        supers.add(name);
                    }
                }
            }
        } catch (Throwable fallback) {
            Matcher m = EXTENDS_PATTERN.matcher(text);
            while (m.find()) {
                String raw = m.group(1).trim();
                String simple = raw.contains(".") ? raw.substring(raw.lastIndexOf('.') + 1) : raw;
                if (!simple.isBlank() && !supers.contains(simple)) {
                    supers.add(simple);
                }
            }
        }
        return supers;
    }

    /**
     * Resolves the Path to the superclass source file within project roots or relative to the current file.
     */
    public static Path resolveSuperClassFile(String superClassName, String currentText, Path currentFile, List<Path> roots) {
        if (superClassName == null || superClassName.isBlank()) return null;

        String fqcn = resolveFqcn(currentText, superClassName);

        // 1. Check in provided roots (src/main/java, src/test/java, etc.)
        if (roots != null && !roots.isEmpty()) {
            if (fqcn != null) {
                String relPath = fqcn.replace('.', File.separatorChar) + ".java";
                for (Path root : roots) {
                    Path candidate = root.resolve(relPath);
                    if (Files.isRegularFile(candidate)) {
                        return candidate;
                    }
                }
            }
            // Check wildcard imports in roots
            List<String> wildcardPackages = extractWildcardImports(currentText);
            for (String wp : wildcardPackages) {
                String relPath = wp.replace('.', File.separatorChar) + File.separatorChar + superClassName + ".java";
                for (Path root : roots) {
                    Path candidate = root.resolve(relPath);
                    if (Files.isRegularFile(candidate)) {
                        return candidate;
                    }
                }
            }
            // Search roots for any matching file name
            for (Path root : roots) {
                if (!Files.isDirectory(root)) continue;
                try (Stream<Path> walk = Files.walk(root, 15)) {
                    Optional<Path> found = walk.filter(p -> p.getFileName().toString().equals(superClassName + ".java"))
                            .findFirst();
                    if (found.isPresent()) return found.get();
                } catch (Exception ignored) {}
            }
        }

        // 2. Check sibling directory if currentFile is provided
        if (currentFile != null) {
            Path sibling = currentFile.resolveSibling(superClassName + ".java");
            if (Files.isRegularFile(sibling)) {
                return sibling;
            }

            // Walk up to find project root / src directory
            Path parent = currentFile.getParent();
            Path projectDir = null;
            while (parent != null) {
                if (Files.exists(parent.resolve("pom.xml")) || Files.exists(parent.resolve("build.gradle"))
                        || Files.isDirectory(parent.resolve("src"))) {
                    projectDir = parent;
                    break;
                }
                parent = parent.getParent();
            }

            if (projectDir != null) {
                Path srcMain = projectDir.resolve("src/main/java");
                if (Files.isDirectory(srcMain)) {
                    if (fqcn != null) {
                        Path candidate = srcMain.resolve(fqcn.replace('.', File.separatorChar) + ".java");
                        if (Files.isRegularFile(candidate)) return candidate;
                    }
                    try (Stream<Path> walk = Files.walk(srcMain, 15)) {
                        Optional<Path> found = walk.filter(p -> p.getFileName().toString().equals(superClassName + ".java"))
                                .findFirst();
                        if (found.isPresent()) return found.get();
                    } catch (Exception ignored) {}
                }
            }
        }

        return null;
    }

    private static String resolveFqcn(String text, String simpleName) {
        if (simpleName.contains(".")) return simpleName;
        if (text == null) return null;

        // Check explicit imports
        Matcher m = IMPORT_PATTERN.matcher(text);
        while (m.find()) {
            String imp = m.group(1);
            if (imp.endsWith("." + simpleName)) {
                return imp;
            }
        }

        // Check current package
        Matcher pkgMatcher = PACKAGE_PATTERN.matcher(text);
        if (pkgMatcher.find()) {
            return pkgMatcher.group(1) + "." + simpleName;
        }

        return simpleName;
    }

    private static List<String> extractWildcardImports(String text) {
        List<String> wildcards = new ArrayList<>();
        if (text == null) return wildcards;
        Matcher m = IMPORT_PATTERN.matcher(text);
        while (m.find()) {
            String imp = m.group(1);
            if (imp.endsWith(".*")) {
                wildcards.add(imp.substring(0, imp.length() - 2));
            }
        }
        return wildcards;
    }
}
