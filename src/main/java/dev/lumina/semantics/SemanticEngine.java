package dev.lumina.semantics;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.Range;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.resolution.declarations.ResolvedFieldDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedValueDeclaration;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JarTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * M1 — the semantic engine. Wraps JavaParser + symbol solver to answer,
 * exactly, "where is this declared?" and "who uses this?" across project
 * sources, the JDK, and every dependency JAR. All methods are safe to call
 * from background threads; every resolution failure returns an empty result
 * so callers can fall back to the older text heuristics.
 */
public final class SemanticEngine {

    // ------------------------------------------------------------- results

    public enum Kind { PROJECT, LIBRARY, DECLARATION, NONE }

    public record Location(Path file, int line) {
    }

    public record Resolution(Kind kind, Location location, String libraryFqcn) {
        public static Resolution project(Location loc) {
            return new Resolution(Kind.PROJECT, loc, null);
        }
        public static Resolution library(String fqcn) {
            return new Resolution(Kind.LIBRARY, null, fqcn);
        }
        public static Resolution declaration(Location loc) {
            return new Resolution(Kind.DECLARATION, loc, null);
        }
        public static Resolution none() {
            return new Resolution(Kind.NONE, null, null);
        }
    }

    public record Usage(Path file, int line, int startCol, int endCol,
                        String preview, boolean declaration) {
    }

    // -------------------------------------------------------------- state

    private final Path projectRoot;
    private final List<Path> sourceRoots;
    private final JavaParser parser;
    private final int jarCount;
    private final Map<String, List<String>> typeIndex;   // simple -> fqcns
    private final ClassLoader dependencyLoader;

    private static final int CACHE_SIZE = 64;
    private final Map<Path, CachedUnit> cache =
            java.util.Collections.synchronizedMap(
                    new LinkedHashMap<>(CACHE_SIZE, 0.75f, true) {
                        @Override
                        protected boolean removeEldestEntry(
                                Map.Entry<Path, CachedUnit> eldest) {
                            return size() > CACHE_SIZE;
                        }
                    });

    private record CachedUnit(int contentHash, CompilationUnit unit) {
    }

    private SemanticEngine(Path projectRoot, List<Path> sourceRoots,
                           JavaParser parser, int jarCount,
                           Map<String, List<String>> typeIndex,
                           ClassLoader dependencyLoader) {
        this.projectRoot = projectRoot;
        this.sourceRoots = sourceRoots;
        this.parser = parser;
        this.jarCount = jarCount;
        this.typeIndex = typeIndex;
        this.dependencyLoader = dependencyLoader;
    }

    public int jarCount() {
        return jarCount;
    }

    public List<Path> sourceRoots() {
        return sourceRoots;
    }

    /**
     * Build an engine for a project. classpath is the File.pathSeparator
     * string produced by dependency:build-classpath (may be null; the engine
     * still resolves project sources and the JDK).
     */
    public static SemanticEngine create(Path projectRoot, String classpath,
                                        Consumer<String> log) {
        try {
            CombinedTypeSolver solver = new CombinedTypeSolver();
            solver.add(new ReflectionTypeSolver());   // JDK classes

            ParserConfiguration srcConfig = new ParserConfiguration()
                    .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21);

            List<Path> roots = new ArrayList<>();
            for (String rel : new String[]{"src/main/java", "src/test/java"}) {
                Path root = projectRoot.resolve(rel);
                if (Files.isDirectory(root)) {
                    roots.add(root);
                    solver.add(new JavaParserTypeSolver(root, srcConfig));
                }
            }

            int jars = 0;
            List<Path> jarPaths = new ArrayList<>();
            if (classpath != null) {
                for (String part : classpath.split(Pattern.quote(File.pathSeparator))) {
                    if (!part.endsWith(".jar")) continue;
                    Path jar = Path.of(part);
                    if (!Files.isRegularFile(jar)) continue;
                    try {
                        solver.add(new JarTypeSolver(jar));
                        jarPaths.add(jar);
                        jars++;
                    } catch (Exception ignored) {
                        // unreadable jar: skip, resolution degrades gracefully
                    }
                }
            }

            // M2: index of project types (simple name -> FQCNs) for
            // type-name completion with auto-import.
            Map<String, List<String>> typeIndex = new java.util.HashMap<>();
            for (Path root : roots) {
                try (Stream<Path> walk = Files.walk(root)) {
                    walk.filter(p -> p.toString().endsWith(".java")).forEach(p -> {
                        String rel = root.relativize(p).toString()
                                .replace(File.separatorChar, '/');
                        String fqcn = rel.substring(0, rel.length() - 5)
                                .replace('/', '.');
                        String simple = fqcn.substring(fqcn.lastIndexOf('.') + 1);
                        typeIndex.computeIfAbsent(simple,
                                k -> new ArrayList<>()).add(fqcn);
                    });
                } catch (Exception ignored) {
                }
            }

            // M2: classloader over dependency jars + compiled output, used to
            // reflect members of library types (the completion "PSI" for JARs).
            List<java.net.URL> urls = new ArrayList<>();
            for (Path jar : jarPaths) {
                try {
                    urls.add(jar.toUri().toURL());
                } catch (Exception ignored) {
                }
            }
            for (String out : new String[]{"target/classes",
                    "build/classes/java/main"}) {
                Path dir = projectRoot.resolve(out);
                if (Files.isDirectory(dir)) {
                    try {
                        urls.add(dir.toUri().toURL());
                    } catch (Exception ignored) {
                    }
                }
            }
            ClassLoader loader = new java.net.URLClassLoader(
                    urls.toArray(new java.net.URL[0]),
                    ClassLoader.getPlatformClassLoader());

            ParserConfiguration config = new ParserConfiguration()
                    .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21)
                    .setSymbolResolver(new JavaSymbolSolver(solver));

            return new SemanticEngine(projectRoot, roots,
                    new JavaParser(config), jars, typeIndex, loader);
        } catch (Throwable t) {
            if (log != null) log.accept("Semantic engine failed to start: " + t);
            return null;
        }
    }

    // ------------------------------------------------------------- parsing

    /** Parse (or reuse from cache) a file; text may be the unsaved editor buffer. */
    private CompilationUnit parse(Path file, String text) {
        try {
            String source = text != null ? text : Files.readString(file);
            int hash = source.hashCode();
            CachedUnit cached = cache.get(file);
            if (cached != null && cached.contentHash() == hash) {
                return cached.unit();
            }
            ParseResult<CompilationUnit> result;
            synchronized (parser) {
                result = parser.parse(source);
            }
            CompilationUnit unit = result.getResult().orElse(null);
            if (unit != null) {
                cache.put(file, new CachedUnit(hash, unit));
            }
            return unit;
        } catch (Throwable t) {
            return null;
        }
    }

    // ------------------------------------------------------ resolve-at-caret

    /**
     * Resolve the symbol at a 1-based (line, column) caret position.
     * PROJECT: jump target inside the project. LIBRARY: fqcn to decompile /
     * open from a sources jar. DECLARATION: caret already sits on the
     * declaration itself. NONE: could not resolve; caller should fall back.
     */
    public Resolution resolveAt(Path file, String text, int line, int column) {
        CompilationUnit cu = parse(file, text);
        if (cu == null) return Resolution.none();

        SimpleName target = nameAt(cu, line, column);
        if (target == null) return Resolution.none();
        Node parent = target.getParentNode().orElse(null);
        if (parent == null) return Resolution.none();

        try {
            // caret ON a declaration name -> tell the caller to show usages
            if ((parent instanceof MethodDeclaration md && md.getName() == target)
                    || (parent instanceof ClassOrInterfaceDeclaration cd && cd.getName() == target)
                    || (parent instanceof EnumDeclaration ed && ed.getName() == target)
                    || (parent instanceof RecordDeclaration rd && rd.getName() == target)
                    || (parent instanceof VariableDeclarator vd && vd.getName() == target)
                    || (parent instanceof Parameter pp && pp.getName() == target)) {
                return Resolution.declaration(new Location(file, line));
            }

            if (parent instanceof MethodCallExpr call && call.getName() == target) {
                ResolvedMethodDeclaration m = call.resolve();
                return memberLocation(m.declaringType().getQualifiedName(),
                        m.getName(), m.getNumberOfParams(), true);
            }

            if (parent instanceof ClassOrInterfaceType type && type.getName() == target) {
                ResolvedType rt = type.resolve();
                if (rt.isReferenceType()) {
                    return typeLocation(rt.asReferenceType().getQualifiedName());
                }
                return Resolution.none();
            }

            if (parent instanceof FieldAccessExpr access && access.getName() == target) {
                ResolvedValueDeclaration v = access.resolve();
                if (v instanceof ResolvedFieldDeclaration f) {
                    return memberLocation(f.declaringType().getQualifiedName(),
                            f.getName(), -1, false);
                }
                return Resolution.none();
            }

            if (parent instanceof NameExpr name && name.getName() == target) {
                ResolvedValueDeclaration v = name.resolve();
                if (v instanceof ResolvedFieldDeclaration f) {
                    return memberLocation(f.declaringType().getQualifiedName(),
                            f.getName(), -1, false);
                }
                // local variable or parameter: find it in this same file
                Location local = localDeclaration(cu, file, target);
                return local != null ? Resolution.project(local) : Resolution.none();
            }
        } catch (Throwable unresolved) {
            return Resolution.none();
        }
        return Resolution.none();
    }

    /** Smallest SimpleName whose range contains the caret. */
    private SimpleName nameAt(CompilationUnit cu, int line, int column) {
        SimpleName best = null;
        int bestWidth = Integer.MAX_VALUE;
        for (SimpleName name : cu.findAll(SimpleName.class)) {
            Optional<Range> r = name.getRange();
            if (r.isEmpty()) continue;
            Range range = r.get();
            if (range.begin.line != line || range.end.line != line) continue;
            if (column < range.begin.column || column > range.end.column + 1) continue;
            int width = range.end.column - range.begin.column;
            if (width < bestWidth) {
                bestWidth = width;
                best = name;
            }
        }
        return best;
    }

    /** Nearest in-scope local/parameter declaration for a usage. */
    private Location localDeclaration(CompilationUnit cu, Path file, SimpleName usage) {
        String name = usage.getIdentifier();
        int usageLine = usage.getRange().map(r -> r.begin.line).orElse(-1);
        Location best = null;
        int bestLine = -1;

        for (VariableDeclarator vd : cu.findAll(VariableDeclarator.class)) {
            if (!vd.getNameAsString().equals(name)) continue;
            int declLine = vd.getRange().map(r -> r.begin.line).orElse(-1);
            if (declLine < 0 || declLine > usageLine) continue;
            Node scope = vd.getParentNode().flatMap(Node::getParentNode).orElse(null);
            if (scope != null && isAncestor(scope, usage) && declLine > bestLine) {
                bestLine = declLine;
                best = new Location(file, declLine);
            }
        }
        for (Parameter p : cu.findAll(Parameter.class)) {
            if (!p.getNameAsString().equals(name)) continue;
            int declLine = p.getRange().map(r -> r.begin.line).orElse(-1);
            Node owner = p.getParentNode().orElse(null);
            if (owner != null && isAncestor(owner, usage) && declLine > bestLine) {
                bestLine = declLine;
                best = new Location(file, declLine);
            }
        }
        return best;
    }

    private static boolean isAncestor(Node maybeAncestor, Node node) {
        Node current = node;
        while (current != null) {
            if (current == maybeAncestor) return true;
            current = current.getParentNode().orElse(null);
        }
        return false;
    }

    // -------------------------------------------------- locating declarations

    /** Locate a method/field declaration by its declaring type's FQCN. */
    private Resolution memberLocation(String typeQn, String member,
                                      int paramCount, boolean isMethod) {
        Path source = sourceFileFor(typeQn);
        if (source == null) {
            return Resolution.library(libraryEntryName(typeQn));
        }
        CompilationUnit cu = parse(source, null);
        if (cu == null) return Resolution.project(new Location(source, 1));

        if (isMethod) {
            MethodDeclaration exact = null;
            MethodDeclaration byName = null;
            for (MethodDeclaration m : cu.findAll(MethodDeclaration.class)) {
                if (!m.getNameAsString().equals(member)) continue;
                if (byName == null) byName = m;
                if (m.getParameters().size() == paramCount) {
                    exact = m;
                    break;
                }
            }
            MethodDeclaration found = exact != null ? exact : byName;
            if (found != null) {
                return Resolution.project(new Location(source,
                        found.getRange().map(r -> r.begin.line).orElse(1)));
            }
            for (ConstructorDeclaration c : cu.findAll(ConstructorDeclaration.class)) {
                if (c.getNameAsString().equals(member)) {
                    return Resolution.project(new Location(source,
                            c.getRange().map(r -> r.begin.line).orElse(1)));
                }
            }
        } else {
            for (FieldDeclaration f : cu.findAll(FieldDeclaration.class)) {
                for (VariableDeclarator v : f.getVariables()) {
                    if (v.getNameAsString().equals(member)) {
                        return Resolution.project(new Location(source,
                                v.getRange().map(r -> r.begin.line).orElse(1)));
                    }
                }
            }
        }
        return Resolution.project(new Location(source, 1));
    }

    /** Locate a type declaration by FQCN. */
    private Resolution typeLocation(String typeQn) {
        Path source = sourceFileFor(typeQn);
        if (source == null) {
            return Resolution.library(libraryEntryName(typeQn));
        }
        String simple = typeQn.substring(typeQn.lastIndexOf('.') + 1);
        CompilationUnit cu = parse(source, null);
        if (cu != null) {
            for (ClassOrInterfaceDeclaration d : cu.findAll(ClassOrInterfaceDeclaration.class)) {
                if (d.getNameAsString().equals(simple)) {
                    return Resolution.project(new Location(source,
                            d.getRange().map(r -> r.begin.line).orElse(1)));
                }
            }
            for (EnumDeclaration d : cu.findAll(EnumDeclaration.class)) {
                if (d.getNameAsString().equals(simple)) {
                    return Resolution.project(new Location(source,
                            d.getRange().map(r -> r.begin.line).orElse(1)));
                }
            }
            for (RecordDeclaration d : cu.findAll(RecordDeclaration.class)) {
                if (d.getNameAsString().equals(simple)) {
                    return Resolution.project(new Location(source,
                            d.getRange().map(r -> r.begin.line).orElse(1)));
                }
            }
        }
        return Resolution.project(new Location(source, 1));
    }

    /**
     * Map a qualified type name to its .java file under a source root.
     * Handles nested classes (a.b.Outer.Inner -> a/b/Outer.java) by trying
     * progressively shorter prefixes.
     */
    private Path sourceFileFor(String qualifiedName) {
        String[] parts = qualifiedName.split("\\.");
        for (int end = parts.length; end >= 1; end--) {
            StringBuilder rel = new StringBuilder();
            for (int i = 0; i < end; i++) {
                if (i > 0) rel.append('/');
                rel.append(parts[i]);
            }
            rel.append(".java");
            for (Path root : sourceRoots) {
                Path candidate = root.resolve(rel.toString());
                if (Files.isRegularFile(candidate)) return candidate;
            }
        }
        return null;
    }

    /** Trim nested-class segments so sources-jar lookup finds Outer.java. */
    private static String libraryEntryName(String qualifiedName) {
        String[] parts = qualifiedName.split("\\.");
        StringBuilder qn = new StringBuilder();
        for (String part : parts) {
            if (!qn.isEmpty()) qn.append('.');
            qn.append(part);
            if (!part.isEmpty() && Character.isUpperCase(part.charAt(0))) {
                break;   // first class segment = the top-level type
            }
        }
        return qn.toString();
    }

    // ------------------------------------------------------------ find usages

    /**
     * Semantically-exact usages of the symbol at the caret. Empty list means
     * the symbol couldn't be resolved and the caller should fall back to the
     * textual scan.
     */
    public List<Usage> findUsages(Path file, String text, int line, int column) {
        CompilationUnit cu = parse(file, text);
        if (cu == null) return List.of();
        SimpleName target = nameAt(cu, line, column);
        if (target == null) return List.of();
        Node parent = target.getParentNode().orElse(null);
        if (parent == null) return List.of();

        try {
            // methods: usage or declaration
            if (parent instanceof MethodCallExpr call && call.getName() == target) {
                ResolvedMethodDeclaration m = call.resolve();
                return methodUsages(m.declaringType().getQualifiedName(),
                        m.getName(), m.getNumberOfParams());
            }
            if (parent instanceof MethodDeclaration md && md.getName() == target) {
                ResolvedMethodDeclaration m = md.resolve();
                return methodUsages(m.declaringType().getQualifiedName(),
                        m.getName(), m.getNumberOfParams());
            }
            // types
            if (parent instanceof ClassOrInterfaceType type && type.getName() == target) {
                ResolvedType rt = type.resolve();
                if (rt.isReferenceType()) {
                    return typeUsages(rt.asReferenceType().getQualifiedName());
                }
            }
            if (parent instanceof ClassOrInterfaceDeclaration cd && cd.getName() == target) {
                ResolvedReferenceTypeDeclaration d = cd.resolve();
                return typeUsages(d.getQualifiedName());
            }
            // fields
            if (parent instanceof FieldAccessExpr access && access.getName() == target) {
                ResolvedValueDeclaration v = access.resolve();
                if (v instanceof ResolvedFieldDeclaration f) {
                    return fieldUsages(f.declaringType().getQualifiedName(), f.getName());
                }
            }
            if (parent instanceof NameExpr name && name.getName() == target) {
                ResolvedValueDeclaration v = name.resolve();
                if (v instanceof ResolvedFieldDeclaration f) {
                    return fieldUsages(f.declaringType().getQualifiedName(), f.getName());
                }
                return localUsages(cu, file, target);
            }
            if (parent instanceof VariableDeclarator vd && vd.getName() == target) {
                ResolvedValueDeclaration v = vd.resolve();
                if (v instanceof ResolvedFieldDeclaration f) {
                    return fieldUsages(f.declaringType().getQualifiedName(), f.getName());
                }
                return localUsages(cu, file, target);
            }
            if (parent instanceof Parameter p && p.getName() == target) {
                return localUsages(cu, file, target);
            }
        } catch (Throwable unresolved) {
            return List.of();
        }
        return List.of();
    }

    private List<Usage> methodUsages(String typeQn, String method, int paramCount) {
        List<Usage> hits = new ArrayList<>();
        addDeclarationHit(hits, memberLocation(typeQn, method, paramCount, true), method);
        scanProject(method, (path, unit, lines) -> {
            for (MethodCallExpr call : unit.findAll(MethodCallExpr.class)) {
                if (!call.getNameAsString().equals(method)) continue;
                try {
                    ResolvedMethodDeclaration m = call.resolve();
                    if (m.declaringType().getQualifiedName().equals(typeQn)
                            && m.getNumberOfParams() == paramCount) {
                        addHit(hits, path, call.getName().getRange(), lines, false);
                    }
                } catch (Throwable ignored) {
                }
            }
        });
        return finish(hits);
    }

    private List<Usage> fieldUsages(String typeQn, String field) {
        List<Usage> hits = new ArrayList<>();
        addDeclarationHit(hits, memberLocation(typeQn, field, -1, false), field);
        scanProject(field, (path, unit, lines) -> {
            for (NameExpr name : unit.findAll(NameExpr.class)) {
                if (!name.getNameAsString().equals(field)) continue;
                try {
                    ResolvedValueDeclaration v = name.resolve();
                    if (v instanceof ResolvedFieldDeclaration f
                            && f.declaringType().getQualifiedName().equals(typeQn)) {
                        addHit(hits, path, name.getRange(), lines, false);
                    }
                } catch (Throwable ignored) {
                }
            }
            for (FieldAccessExpr access : unit.findAll(FieldAccessExpr.class)) {
                if (!access.getNameAsString().equals(field)) continue;
                try {
                    ResolvedValueDeclaration v = access.resolve();
                    if (v instanceof ResolvedFieldDeclaration f
                            && f.declaringType().getQualifiedName().equals(typeQn)) {
                        addHit(hits, path, access.getName().getRange(), lines, false);
                    }
                } catch (Throwable ignored) {
                }
            }
        });
        return finish(hits);
    }

    private List<Usage> typeUsages(String typeQn) {
        List<Usage> hits = new ArrayList<>();
        addDeclarationHit(hits, typeLocation(typeQn),
                typeQn.substring(typeQn.lastIndexOf('.') + 1));
        String simple = typeQn.substring(typeQn.lastIndexOf('.') + 1);
        Path declFile = sourceFileFor(typeQn);
        if (declFile != null) {
            CompilationUnit declUnit = parse(declFile, null);
            if (declUnit != null) {
                List<String> declLines = readText(declFile).lines().toList();
                for (ConstructorDeclaration c
                        : declUnit.findAll(ConstructorDeclaration.class)) {
                    if (c.getNameAsString().equals(simple)) {
                        addHit(hits, declFile, c.getName().getRange(),
                                declLines, false);
                    }
                }
            }
        }
        scanProject(simple, (path, unit, lines) -> {
            for (ClassOrInterfaceType type : unit.findAll(ClassOrInterfaceType.class)) {
                if (!type.getNameAsString().equals(simple)) continue;
                try {
                    ResolvedType rt = type.resolve();
                    if (rt.isReferenceType() && rt.asReferenceType()
                            .getQualifiedName().equals(typeQn)) {
                        addHit(hits, path, type.getName().getRange(), lines, false);
                    }
                } catch (Throwable ignored) {
                }
            }
        });
        return finish(hits);
    }

    /** Local variable/parameter usages: same file, enclosing callable only. */
    private List<Usage> localUsages(CompilationUnit cu, Path file, SimpleName target) {
        String name = target.getIdentifier();
        Node scope = target;
        while (scope != null
                && !(scope instanceof MethodDeclaration)
                && !(scope instanceof ConstructorDeclaration)) {
            scope = scope.getParentNode().orElse(null);
        }
        if (scope == null) return List.of();

        List<String> lines;
        try {
            lines = Files.readAllLines(file);
        } catch (Exception e) {
            lines = List.of();
        }
        List<Usage> hits = new ArrayList<>();
        for (SimpleName candidate : scope.findAll(SimpleName.class)) {
            if (!candidate.getIdentifier().equals(name)) continue;
            boolean isDecl = candidate.getParentNode()
                    .map(p -> p instanceof VariableDeclarator || p instanceof Parameter)
                    .orElse(false);
            addHit(hits, file, candidate.getRange(), lines, isDecl);
        }
        return finish(hits);
    }

    // ------------------------------------------------------------- scanning

    private interface UnitVisitor {
        void visit(Path path, CompilationUnit unit, List<String> lines);
    }

    private static final int MAX_FILES = 400;

    /** Parse and visit project files that textually contain the identifier. */
    private void scanProject(String identifier, UnitVisitor visitor) {
        Pattern quick = Pattern.compile("\\b" + Pattern.quote(identifier) + "\\b");
        int visited = 0;
        for (Path root : sourceRoots) {
            try (Stream<Path> walk = Files.walk(root)) {
                List<Path> files = walk
                        .filter(p -> p.toString().endsWith(".java"))
                        .toList();
                for (Path path : files) {
                    if (visited >= MAX_FILES) return;
                    String content;
                    try {
                        content = Files.readString(path);
                    } catch (Exception e) {
                        continue;
                    }
                    if (!quick.matcher(content).find()) continue;
                    CompilationUnit unit = parse(path, content);
                    if (unit == null) continue;
                    visited++;
                    visitor.visit(path, unit, content.lines().toList());
                }
            } catch (Exception ignored) {
            }
        }
    }

    private void addDeclarationHit(List<Usage> hits, Resolution declaration) {
        addDeclarationHit(hits, declaration, null);
    }

    private void addDeclarationHit(List<Usage> hits, Resolution declaration,
                                   String name) {
        if (declaration.kind() == Kind.PROJECT && declaration.location() != null) {
            Location loc = declaration.location();
            String preview = "";
            String rawLine = "";
            try {
                List<String> lines = Files.readAllLines(loc.file());
                if (loc.line() - 1 < lines.size()) {
                    rawLine = lines.get(loc.line() - 1);
                    preview = rawLine.strip();
                }
            } catch (Exception ignored) {
            }
            int startCol = 0;
            int endCol = 0;
            if (name != null && !rawLine.isEmpty()) {
                java.util.regex.Matcher m = java.util.regex.Pattern
                        .compile("\\b" + java.util.regex.Pattern.quote(name)
                                + "\\b")
                        .matcher(rawLine);
                if (m.find()) {
                    startCol = m.start() + 1;
                    endCol = m.end();
                }
            }
            hits.add(new Usage(loc.file(), loc.line(), startCol, endCol,
                    truncate(preview), true));
        }
    }

    private static void addHit(List<Usage> hits, Path path,
                               Optional<Range> range, List<String> lines,
                               boolean declaration) {
        if (range.isEmpty() || hits.size() >= 500) return;
        Range r = range.get();
        int line = r.begin.line;
        String preview = line - 1 < lines.size() ? lines.get(line - 1).strip() : "";
        hits.add(new Usage(path, line, r.begin.column, r.end.column,
                truncate(preview), declaration));
    }

    private static List<Usage> finish(List<Usage> hits) {
        // declaration first, then stable file/line order, no duplicates
        return hits.stream()
                .distinct()
                .sorted((a, b) -> {
                    int decl = Boolean.compare(b.declaration(), a.declaration());
                    if (decl != 0) return decl;
                    int file = a.file().compareTo(b.file());
                    return file != 0 ? file : Integer.compare(a.line(), b.line());
                })
                .toList();
    }

    private static String truncate(String s) {
        return s.length() > 90 ? s.substring(0, 90) + "\u2026" : s;
    }

    // ========================================================== M2 completion

    /**
     * Completions for "receiver.prefix|". Resolves the receiver — this, a
     * type name for statics, or a variable's declared type — then lists its
     * members from project source (with inherited members up the extends
     * chain) or by reflecting over the dependency-jar classloader.
     */
    public List<Completion.Item> memberCompletions(Path file, String text,
                                                   int caretLine,
                                                   String receiver,
                                                   String prefix) {
        try {
            boolean staticOnly = false;
            String fqcn = null;
            if (receiver.equals("this")) {
                fqcn = fqcnForFile(file);
            } else if (Character.isUpperCase(receiver.charAt(0))) {
                fqcn = resolveTypeName(receiver, text);
                staticOnly = fqcn != null;
            }
            if (fqcn == null) {
                String typeName = declaredTypeOf(file, text, caretLine, receiver);
                if (typeName != null) {
                    staticOnly = false;
                    fqcn = typeName.contains(".") ? typeName
                            : resolveTypeName(typeName, text);
                }
            }
            if (fqcn == null) return List.of();
            boolean includePrivate = receiver.equals("this")
                    || fqcn.equals(fqcnForFile(file));
            return finishItems(membersOf(fqcn, staticOnly, includePrivate,
                    prefix, 0));
        } catch (Throwable t) {
            return List.of();
        }
    }

    /**
     * Completions for a bare identifier: locals and parameters in scope,
     * fields and methods of this file's classes, and type names (project
     * index + common JDK types) that auto-import on selection.
     */
    public List<Completion.Item> scopeCompletions(Path file, String text,
                                                  int caretLine, String prefix) {
        List<Completion.Item> items = new ArrayList<>();
        try {
            CompilationUnit cu = parse(file, blankLine(text, caretLine));
            if (cu != null) {
                for (Parameter p : cu.findAll(Parameter.class)) {
                    Node owner = p.getParentNode().orElse(null);
                    if (owner == null || !rangeContainsLine(owner, caretLine)) {
                        continue;
                    }
                    String name = p.getNameAsString();
                    if (!Completion.matches(prefix, name)) continue;
                    items.add(new Completion.Item(name, name, name,
                            ": " + simpleType(p.getType().asString()),
                            Completion.Kind.VARIABLE, null, 0));
                }
                for (VariableDeclarator v : cu.findAll(VariableDeclarator.class)) {
                    String name = v.getNameAsString();
                    if (!Completion.matches(prefix, name)) continue;
                    boolean isField = v.getParentNode()
                            .map(p -> p instanceof FieldDeclaration)
                            .orElse(false);
                    if (isField) {
                        Node cls = v.getParentNode()
                                .flatMap(Node::getParentNode).orElse(null);
                        if (cls != null && rangeContainsLine(cls, caretLine)) {
                            items.add(new Completion.Item(name, name, name,
                                    ": " + simpleType(v.getType().asString()),
                                    Completion.Kind.FIELD, null, 0));
                        }
                    } else {
                        int declLine = beginLine(v);
                        Node scope = v.getParentNode()
                                .flatMap(Node::getParentNode).orElse(null);
                        if (declLine > 0 && declLine <= caretLine
                                && scope != null
                                && rangeContainsLine(scope, caretLine)) {
                            items.add(new Completion.Item(name, name, name,
                                    ": " + simpleType(v.getType().asString()),
                                    Completion.Kind.VARIABLE, null, 0));
                        }
                    }
                }
                for (MethodDeclaration m : cu.findAll(MethodDeclaration.class)) {
                    String name = m.getNameAsString();
                    if (!Completion.matches(prefix, name)) continue;
                    items.add(methodItem(name, params(m),
                            simpleType(m.getType().asString()),
                            m.getParameters().isEmpty()));
                }
            }
            if (!prefix.isEmpty()) {
                String filePkg = filePackage(text);
                for (Map.Entry<String, List<String>> e : typeIndex.entrySet()) {
                    if (!Completion.matches(prefix, e.getKey())) continue;
                    String fq = e.getValue().stream()
                            .filter(q -> packageOf(q).equals(filePkg))
                            .findFirst()
                            .orElse(e.getValue().get(0));
                    items.add(new Completion.Item(e.getKey(), e.getKey(),
                            e.getKey(), packageOf(fq),
                            Completion.Kind.CLASS, fq, 0));
                }
                for (int i = 0; i < JDK_TYPES.length; i += 2) {
                    String simple = JDK_TYPES[i];
                    if (!Completion.matches(prefix, simple)) continue;
                    items.add(new Completion.Item(simple, simple, simple,
                            packageOf(JDK_TYPES[i + 1]),
                            Completion.Kind.CLASS, JDK_TYPES[i + 1], 0));
                }
            }
        } catch (Throwable t) {
            // partial results are fine
        }
        return finishItems(items);
    }

    // ------------------------------------------------------- member listing

    private List<Completion.Item> membersOf(String fqcn, boolean staticOnly,
                                            boolean includePrivate,
                                            String prefix, int depth) {
        List<Completion.Item> items = new ArrayList<>();
        if (depth > 4 || "java.lang.Object".equals(fqcn)) return items;

        Path source = sourceFileFor(fqcn);
        if (source != null) {
            CompilationUnit cu = parse(source, null);
            if (cu != null) {
                String simple = fqcn.substring(fqcn.lastIndexOf('.') + 1);
                ClassOrInterfaceDeclaration owner = null;
                for (ClassOrInterfaceDeclaration d
                        : cu.findAll(ClassOrInterfaceDeclaration.class)) {
                    if (d.getNameAsString().equals(simple)) {
                        owner = d;
                        break;
                    }
                }
                Node scope = owner != null ? owner : cu;
                for (MethodDeclaration m : scope.findAll(MethodDeclaration.class)) {
                    if (m.isPrivate() && !includePrivate) continue;
                    if (staticOnly && !m.isStatic()) continue;
                    String name = m.getNameAsString();
                    if (!Completion.matches(prefix, name)) continue;
                    items.add(methodItem(name, params(m),
                            simpleType(m.getType().asString()),
                            m.getParameters().isEmpty()));
                }
                for (FieldDeclaration f : scope.findAll(FieldDeclaration.class)) {
                    if (f.isPrivate() && !includePrivate) continue;
                    if (staticOnly && !f.isStatic()) continue;
                    for (VariableDeclarator v : f.getVariables()) {
                        String name = v.getNameAsString();
                        if (!Completion.matches(prefix, name)) continue;
                        items.add(new Completion.Item(name, name, name,
                                ": " + simpleType(v.getType().asString()),
                                Completion.Kind.FIELD, null, 0));
                    }
                }
                if (owner != null) {
                    String ownText = readText(source);
                    for (ClassOrInterfaceType ext : owner.getExtendedTypes()) {
                        String superFqcn = resolveTypeName(
                                ext.getNameAsString(), ownText);
                        if (superFqcn != null) {
                            items.addAll(membersOf(superFqcn, staticOnly,
                                    false, prefix, depth + 1));
                        }
                    }
                }
                return items;
            }
        }

        // library type: reflect over the dependency classloader
        try {
            Class<?> type = Class.forName(fqcn, false, dependencyLoader);
            for (java.lang.reflect.Method m : type.getMethods()) {
                if (m.isSynthetic() || m.isBridge()) continue;
                boolean isStatic = java.lang.reflect.Modifier
                        .isStatic(m.getModifiers());
                if (staticOnly && !isStatic) continue;
                String name = m.getName();
                if (!Completion.matches(prefix, name)) continue;
                StringBuilder ps = new StringBuilder();
                for (Class<?> pt : m.getParameterTypes()) {
                    if (!ps.isEmpty()) ps.append(", ");
                    ps.append(pt.getSimpleName());
                }
                items.add(methodItem(name, ps.toString(),
                        m.getReturnType().getSimpleName(),
                        m.getParameterCount() == 0));
            }
            for (java.lang.reflect.Field f : type.getFields()) {
                if (staticOnly && !java.lang.reflect.Modifier
                        .isStatic(f.getModifiers())) {
                    continue;
                }
                String name = f.getName();
                if (!Completion.matches(prefix, name)) continue;
                items.add(new Completion.Item(name, name, name,
                        ": " + f.getType().getSimpleName(),
                        Completion.Kind.FIELD, null, 0));
            }
        } catch (Throwable ignored) {
        }
        return items;
    }

    // -------------------------------------------------- completion helpers

    private static Completion.Item methodItem(String name, String params,
                                              String returnType,
                                              boolean noParams) {
        return new Completion.Item(name, name + "(" + params + ")",
                name + "()", ": " + returnType,
                Completion.Kind.METHOD, null, noParams ? 0 : 1);
    }

    private String params(MethodDeclaration m) {
        StringBuilder sb = new StringBuilder();
        for (Parameter p : m.getParameters()) {
            if (!sb.isEmpty()) sb.append(", ");
            sb.append(simpleType(p.getType().asString()));
        }
        return sb.toString();
    }

    /** Declared type of a variable/parameter/field visible at the caret. */
    private String declaredTypeOf(Path file, String text, int caretLine,
                                  String receiver) {
        CompilationUnit cu = parse(file, blankLine(text, caretLine));
        if (cu == null) return null;
        String best = null;
        int bestLine = -1;
        for (Parameter p : cu.findAll(Parameter.class)) {
            if (!p.getNameAsString().equals(receiver)) continue;
            Node owner = p.getParentNode().orElse(null);
            int line = beginLine(p);
            if (owner != null && rangeContainsLine(owner, caretLine)
                    && line > bestLine) {
                bestLine = line;
                best = p.getType().asString();
            }
        }
        for (VariableDeclarator v : cu.findAll(VariableDeclarator.class)) {
            if (!v.getNameAsString().equals(receiver)) continue;
            boolean isField = v.getParentNode()
                    .map(p -> p instanceof FieldDeclaration).orElse(false);
            int line = beginLine(v);
            if (isField) {
                Node cls = v.getParentNode()
                        .flatMap(Node::getParentNode).orElse(null);
                if (cls != null && rangeContainsLine(cls, caretLine)
                        && best == null) {
                    best = v.getType().asString();   // locals shadow fields
                }
            } else {
                Node scope = v.getParentNode()
                        .flatMap(Node::getParentNode).orElse(null);
                if (line > 0 && line <= caretLine && scope != null
                        && rangeContainsLine(scope, caretLine)
                        && line > bestLine) {
                    bestLine = line;
                    best = v.getType().asString();
                }
            }
        }
        if (best == null) return null;
        String stripped = best.replaceAll("<.*>", "").replace("[]", "").trim();
        return stripped.isEmpty() || stripped.equals("var") ? null : stripped;
    }

    /** Resolve a simple type name to an FQCN via imports, index, java.lang. */
    private String resolveTypeName(String simple, String sourceText) {
        if (simple.contains(".")) return simple;
        String filePkg = filePackage(sourceText);
        for (String raw : sourceText.split("\n")) {
            String line = raw.strip();
            if (line.startsWith("import ") && line.endsWith("." + simple + ";")) {
                return line.substring(7, line.length() - 1).trim();
            }
        }
        for (String raw : sourceText.split("\n")) {
            String line = raw.strip();
            if (line.startsWith("import ") && line.endsWith(".*;")) {
                String candidate = line.substring(7, line.length() - 3)
                        + "." + simple;
                if (sourceFileFor(candidate) != null || loadable(candidate)) {
                    return candidate;
                }
            }
        }
        if (!filePkg.isEmpty()) {
            String candidate = filePkg + "." + simple;
            if (sourceFileFor(candidate) != null) return candidate;
        }
        List<String> indexed = typeIndex.get(simple);
        if (indexed != null && !indexed.isEmpty()) {
            return indexed.stream()
                    .filter(q -> packageOf(q).equals(filePkg))
                    .findFirst()
                    .orElse(indexed.get(0));
        }
        if (loadable("java.lang." + simple)) return "java.lang." + simple;
        return null;
    }

    private boolean loadable(String fqcn) {
        try {
            Class.forName(fqcn, false, dependencyLoader);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    private String fqcnForFile(Path file) {
        Path abs = file.toAbsolutePath().normalize();
        for (Path root : sourceRoots) {
            Path base = root.toAbsolutePath().normalize();
            if (abs.startsWith(base)) {
                String rel = base.relativize(abs).toString()
                        .replace(File.separatorChar, '/');
                if (rel.endsWith(".java")) {
                    return rel.substring(0, rel.length() - 5).replace('/', '.');
                }
            }
        }
        return null;
    }

    private static String filePackage(String sourceText) {
        for (String raw : sourceText.split("\n")) {
            String line = raw.strip();
            if (line.startsWith("package ") && line.endsWith(";")) {
                return line.substring(8, line.length() - 1).trim();
            }
            if (line.startsWith("import ") || line.contains("class ")) break;
        }
        return "";
    }

    private static String packageOf(String fqcn) {
        int dot = fqcn.lastIndexOf('.');
        return dot < 0 ? "" : fqcn.substring(0, dot);
    }

    private static String simpleType(String type) {
        String stripped = type.replaceAll("<.*>", "");
        int dot = stripped.lastIndexOf('.');
        return dot < 0 ? stripped : stripped.substring(dot + 1);
    }

    /** Replace one 1-based line with spaces so incomplete code still parses. */
    private static String blankLine(String text, int line) {
        String[] lines = text.split("\n", -1);
        if (line - 1 < 0 || line - 1 >= lines.length) return text;
        lines[line - 1] = " ".repeat(lines[line - 1].length());
        return String.join("\n", lines);
    }

    private static boolean rangeContainsLine(Node node, int line) {
        return node.getRange()
                .map(r -> r.begin.line <= line && line <= r.end.line)
                .orElse(false);
    }

    private static int beginLine(Node node) {
        return node.getRange().map(r -> r.begin.line).orElse(-1);
    }

    private String readText(Path file) {
        try {
            return Files.readString(file);
        } catch (Exception e) {
            return "";
        }
    }

    /** De-duplicate by label, order by kind then name, cap the list. */
    private static List<Completion.Item> finishItems(List<Completion.Item> items) {
        Map<String, Completion.Item> unique = new LinkedHashMap<>();
        for (Completion.Item item : items) {
            unique.putIfAbsent(item.kind() + "|" + item.label(), item);
        }
        return unique.values().stream()
                .sorted((a, b) -> {
                    int kind = Integer.compare(a.kind().ordinal(),
                            b.kind().ordinal());
                    return kind != 0 ? kind
                            : a.name().compareToIgnoreCase(b.name());
                })
                .limit(200)
                .toList();
    }

    // ============================================================== M4 docs

    /**
     * Where the documentation for the symbol at the caret lives: a project
     * file + declaration line, or a library FQCN to read from its
     * sources jar. Includes a display signature. Null when unresolvable.
     */
    public Docs.DocTarget docTargetAt(Path file, String text, int line,
                                      int column) {
        CompilationUnit cu = parse(file, text);
        if (cu == null) return null;
        SimpleName target = nameAt(cu, line, column);
        if (target == null) return null;
        Node parent = target.getParentNode().orElse(null);
        if (parent == null) return null;
        try {
            if (parent instanceof MethodCallExpr call && call.getName() == target) {
                ResolvedMethodDeclaration m = call.resolve();
                return memberDocTarget(m.declaringType().getQualifiedName(),
                        m.getName(), m.getNumberOfParams());
            }
            if (parent instanceof ClassOrInterfaceType type
                    && type.getName() == target) {
                ResolvedType rt = type.resolve();
                if (rt.isReferenceType()) {
                    return typeDocTarget(rt.asReferenceType().getQualifiedName());
                }
                return null;
            }
            if (parent instanceof NameExpr name && name.getName() == target) {
                ResolvedValueDeclaration v = name.resolve();
                if (v instanceof ResolvedFieldDeclaration f) {
                    return memberDocTarget(f.declaringType().getQualifiedName(),
                            f.getName(), -1);
                }
                Location local = localDeclaration(cu, file, target);
                if (local != null) {
                    String typeName = declaredTypeOf(file, text, line,
                            target.getIdentifier());
                    String sig = (typeName != null ? typeName + " " : "")
                            + target.getIdentifier();
                    return new Docs.DocTarget(sig, local.file(), local.line(),
                            null, target.getIdentifier(), -1);
                }
                return null;
            }
            if (parent instanceof FieldAccessExpr access
                    && access.getName() == target) {
                ResolvedValueDeclaration v = access.resolve();
                if (v instanceof ResolvedFieldDeclaration f) {
                    return memberDocTarget(f.declaringType().getQualifiedName(),
                            f.getName(), -1);
                }
                return null;
            }
            // caret on a declaration name in this very file
            if (parent instanceof MethodDeclaration md && md.getName() == target) {
                return new Docs.DocTarget(signatureOf(md), file, beginLine(md),
                        null, md.getNameAsString(), md.getParameters().size());
            }
            if (parent instanceof ClassOrInterfaceDeclaration cd
                    && cd.getName() == target) {
                String own = fqcnForFile(file);
                return new Docs.DocTarget(own != null ? own : cd.getNameAsString(),
                        file, beginLine(cd), null, cd.getNameAsString(), -2);
            }
        } catch (Throwable unresolved) {
            return null;
        }
        return null;
    }

    private Docs.DocTarget memberDocTarget(String typeQn, String member,
                                           int paramCount) {
        Path source = sourceFileFor(typeQn);
        if (source == null) {
            return new Docs.DocTarget(
                    reflectionSignature(typeQn, member, paramCount),
                    null, -1, libraryEntryName(typeQn), member, paramCount);
        }
        CompilationUnit cu = parse(source, null);
        int line = -1;
        String sig = typeQn + (member != null ? "." + member : "");
        if (cu != null && member != null) {
            for (MethodDeclaration m : cu.findAll(MethodDeclaration.class)) {
                if (!m.getNameAsString().equals(member)) continue;
                if (paramCount >= 0
                        && m.getParameters().size() != paramCount) {
                    continue;
                }
                line = beginLine(m);
                sig = signatureOf(m);
                break;
            }
            if (line < 0) {   // maybe a field
                for (FieldDeclaration fd : cu.findAll(FieldDeclaration.class)) {
                    for (VariableDeclarator v : fd.getVariables()) {
                        if (v.getNameAsString().equals(member)) {
                            line = beginLine(v);
                            sig = simpleType(v.getType().asString())
                                    + " " + member;
                        }
                    }
                }
            }
        }
        return new Docs.DocTarget(sig, source, Math.max(1, line), null,
                member, paramCount);
    }

    private Docs.DocTarget typeDocTarget(String typeQn) {
        Path source = sourceFileFor(typeQn);
        String simple = typeQn.substring(typeQn.lastIndexOf('.') + 1);
        if (source == null) {
            return new Docs.DocTarget(typeQn, null, -1,
                    libraryEntryName(typeQn), simple, -2);
        }
        Resolution loc = typeLocation(typeQn);
        int line = loc.location() != null ? loc.location().line() : 1;
        return new Docs.DocTarget(typeQn, source, line, null, simple, -2);
    }

    private String signatureOf(MethodDeclaration m) {
        StringBuilder ps = new StringBuilder();
        for (Parameter p : m.getParameters()) {
            if (!ps.isEmpty()) ps.append(", ");
            ps.append(simpleType(p.getType().asString())).append(' ')
                    .append(p.getNameAsString());
        }
        return simpleType(m.getType().asString()) + " " + m.getNameAsString()
                + "(" + ps + ")";
    }

    private String reflectionSignature(String typeQn, String member,
                                       int paramCount) {
        try {
            Class<?> type = Class.forName(typeQn, false, dependencyLoader);
            if (member == null) return typeQn;
            for (java.lang.reflect.Method m : type.getMethods()) {
                if (!m.getName().equals(member)) continue;
                if (paramCount >= 0 && m.getParameterCount() != paramCount) {
                    continue;
                }
                StringBuilder ps = new StringBuilder();
                for (Class<?> pt : m.getParameterTypes()) {
                    if (!ps.isEmpty()) ps.append(", ");
                    ps.append(pt.getSimpleName());
                }
                return m.getReturnType().getSimpleName() + " " + member
                        + "(" + ps + ")";
            }
            for (java.lang.reflect.Field f : type.getFields()) {
                if (f.getName().equals(member)) {
                    return f.getType().getSimpleName() + " " + member;
                }
            }
        } catch (Throwable ignored) {
        }
        return typeQn + (member != null ? "." + member : "");
    }

    // ======================================================== M4 param info

    /** Overload signatures for receiver.method( — the param-info popup. */
    public List<Docs.Signature> signaturesFor(Path file, String text,
                                              int caretLine, String receiver,
                                              String methodName) {
        try {
            String fqcn = receiverType(file, text, caretLine, receiver);
            if (fqcn == null) return List.of();
            List<Docs.Signature> out = new ArrayList<>();
            collectSignatures(fqcn, methodName, out, 0);
            return out;
        } catch (Throwable t) {
            return List.of();
        }
    }

    private String receiverType(Path file, String text, int caretLine,
                                String receiver) {
        if (receiver == null || receiver.isEmpty()
                || receiver.equals("this")) {
            return fqcnForFile(file);
        }
        if (Character.isUpperCase(receiver.charAt(0))) {
            String fqcn = resolveTypeName(receiver, text);
            if (fqcn != null) return fqcn;
        }
        String typeName = declaredTypeOf(file, text, caretLine, receiver);
        if (typeName == null) return null;
        return typeName.contains(".") ? typeName
                : resolveTypeName(typeName, text);
    }

    private void collectSignatures(String fqcn, String methodName,
                                   List<Docs.Signature> out, int depth) {
        if (depth > 4 || out.size() >= 12
                || "java.lang.Object".equals(fqcn)) {
            return;
        }
        Path source = sourceFileFor(fqcn);
        if (source != null) {
            CompilationUnit cu = parse(source, null);
            if (cu == null) return;
            String simple = fqcn.substring(fqcn.lastIndexOf('.') + 1);
            ClassOrInterfaceDeclaration owner = null;
            for (ClassOrInterfaceDeclaration d
                    : cu.findAll(ClassOrInterfaceDeclaration.class)) {
                if (d.getNameAsString().equals(simple)) {
                    owner = d;
                    break;
                }
            }
            Node scope = owner != null ? owner : cu;
            for (MethodDeclaration m : scope.findAll(MethodDeclaration.class)) {
                if (!m.getNameAsString().equals(methodName)) continue;
                List<String> ps = new ArrayList<>();
                for (Parameter p : m.getParameters()) {
                    ps.add(simpleType(p.getType().asString()) + " "
                            + p.getNameAsString());
                }
                out.add(new Docs.Signature(
                        simpleType(m.getType().asString()), methodName, ps));
            }
            if (owner != null) {
                String ownText = readText(source);
                for (ClassOrInterfaceType ext : owner.getExtendedTypes()) {
                    String superFqcn = resolveTypeName(
                            ext.getNameAsString(), ownText);
                    if (superFqcn != null) {
                        collectSignatures(superFqcn, methodName, out,
                                depth + 1);
                    }
                }
            }
            return;
        }
        try {
            Class<?> type = Class.forName(fqcn, false, dependencyLoader);
            for (java.lang.reflect.Method m : type.getMethods()) {
                if (!m.getName().equals(methodName)) continue;
                if (m.isSynthetic() || m.isBridge()) continue;
                List<String> ps = new ArrayList<>();
                for (Class<?> pt : m.getParameterTypes()) {
                    ps.add(pt.getSimpleName());
                }
                out.add(new Docs.Signature(
                        m.getReturnType().getSimpleName(), methodName, ps));
                if (out.size() >= 12) return;
            }
        } catch (Throwable ignored) {
        }
    }

    // ======================================================= M5 refactoring

    /**
     * Analyze a whole-line selection for Extract Method: which outer
     * variables become parameters, whether one inside-declared variable
     * must be returned, and where the new method goes.
     */
    public dev.lumina.refactor.Refactor.MethodPlan planExtractMethod(
            Path file, String text, int selStartLine, int selEndLine) {
        try {
            CompilationUnit cu = parse(file, text);
            if (cu == null) {
                return dev.lumina.refactor.Refactor.MethodPlan.invalid(
                        "the file has syntax errors \u2014 fix them first");
            }
            MethodDeclaration enclosing = null;
            for (MethodDeclaration m : cu.findAll(MethodDeclaration.class)) {
                if (rangeContainsLine(m, selStartLine)
                        && rangeContainsLine(m, selEndLine)) {
                    if (enclosing == null
                            || spanOf(m) < spanOf(enclosing)) {
                        enclosing = m;
                    }
                }
            }
            if (enclosing == null) {
                return dev.lumina.refactor.Refactor.MethodPlan.invalid(
                        "the selection is not inside a single method");
            }
            int methodStart = beginLine(enclosing);
            int methodEnd = endLine(enclosing);
            if (selStartLine <= methodStart || selEndLine >= methodEnd) {
                return dev.lumina.refactor.Refactor.MethodPlan.invalid(
                        "select whole statements inside the method body");
            }

            java.util.Set<String> usedInside = new java.util.LinkedHashSet<>();
            java.util.Set<String> usedAfter = new java.util.LinkedHashSet<>();
            for (NameExpr n : enclosing.findAll(NameExpr.class)) {
                int line = beginLine(n);
                if (line >= selStartLine && line <= selEndLine) {
                    usedInside.add(n.getNameAsString());
                } else if (line > selEndLine) {
                    usedAfter.add(n.getNameAsString());
                }
            }
            java.util.Map<String, String> declaredInside =
                    new java.util.LinkedHashMap<>();
            java.util.Map<String, String> declaredBefore =
                    new java.util.LinkedHashMap<>();
            for (Parameter p : enclosing.getParameters()) {
                declaredBefore.put(p.getNameAsString(), p.getType().asString());
            }
            for (VariableDeclarator v
                    : enclosing.findAll(VariableDeclarator.class)) {
                int line = beginLine(v);
                if (line >= selStartLine && line <= selEndLine) {
                    declaredInside.put(v.getNameAsString(),
                            v.getType().asString());
                } else if (line < selStartLine && line > 0) {
                    declaredBefore.put(v.getNameAsString(),
                            v.getType().asString());
                }
            }

            List<String> paramDecls = new ArrayList<>();
            List<String> paramNames = new ArrayList<>();
            for (Map.Entry<String, String> e : declaredBefore.entrySet()) {
                if (!usedInside.contains(e.getKey())
                        || declaredInside.containsKey(e.getKey())) {
                    continue;
                }
                if ("var".equals(e.getValue())) {
                    return dev.lumina.refactor.Refactor.MethodPlan.invalid(
                            "cannot infer a parameter type for 'var' variable '"
                                    + e.getKey() + "'");
                }
                paramDecls.add(e.getValue() + " " + e.getKey());
                paramNames.add(e.getKey());
            }

            List<String> returned = declaredInside.keySet().stream()
                    .filter(usedAfter::contains)
                    .toList();
            if (returned.size() > 1) {
                return dev.lumina.refactor.Refactor.MethodPlan.invalid(
                        "the selection declares several variables used later: "
                                + returned);
            }
            for (AssignExpr assign : enclosing.findAll(AssignExpr.class)) {
                int line = beginLine(assign);
                if (line < selStartLine || line > selEndLine) continue;
                if (assign.getTarget() instanceof NameExpr targetName) {
                    String name = targetName.getNameAsString();
                    if (declaredBefore.containsKey(name)
                            && !declaredInside.containsKey(name)
                            && usedAfter.contains(name)) {
                        return dev.lumina.refactor.Refactor.MethodPlan.invalid(
                                "the selection modifies '" + name
                                        + "', which is used after it");
                    }
                }
            }
            String returnVar = returned.isEmpty() ? null : returned.get(0);
            String returnType = returnVar == null ? "void"
                    : declaredInside.get(returnVar);
            if ("var".equals(returnType)) {
                return dev.lumina.refactor.Refactor.MethodPlan.invalid(
                        "cannot infer the return type of 'var' variable '"
                                + returnVar + "'");
            }
            return new dev.lumina.refactor.Refactor.MethodPlan(true, null,
                    paramDecls, paramNames, returnType, returnVar,
                    methodEnd, isStaticMethod(enclosing));
        } catch (Throwable t) {
            return dev.lumina.refactor.Refactor.MethodPlan.invalid(
                    "analysis failed: " + t.getClass().getSimpleName());
        }
    }

    private static int endLine(Node node) {
        return node.getRange().map(r -> r.end.line).orElse(-1);
    }

    private static int spanOf(Node node) {
        return node.getRange()
                .map(r -> r.end.line - r.begin.line)
                .orElse(Integer.MAX_VALUE);
    }

    private static boolean isStaticMethod(MethodDeclaration m) {
        try {
            return m.isStatic();
        } catch (Throwable t) {
            return false;
        }
    }

    private static final String[] JDK_TYPES = {
            "List", "java.util.List", "ArrayList", "java.util.ArrayList",
            "Map", "java.util.Map", "HashMap", "java.util.HashMap",
            "Set", "java.util.Set", "HashSet", "java.util.HashSet",
            "Optional", "java.util.Optional", "Arrays", "java.util.Arrays",
            "Collections", "java.util.Collections", "Objects", "java.util.Objects",
            "UUID", "java.util.UUID", "Stream", "java.util.stream.Stream",
            "Collectors", "java.util.stream.Collectors",
            "Files", "java.nio.file.Files", "Path", "java.nio.file.Path",
            "Paths", "java.nio.file.Paths",
            "String", "java.lang.String", "StringBuilder", "java.lang.StringBuilder",
            "Integer", "java.lang.Integer", "Long", "java.lang.Long",
            "Double", "java.lang.Double", "Boolean", "java.lang.Boolean",
            "Math", "java.lang.Math", "System", "java.lang.System",
            "Exception", "java.lang.Exception",
            "RuntimeException", "java.lang.RuntimeException",
            "Thread", "java.lang.Thread",
            "BigDecimal", "java.math.BigDecimal",
            "LocalDate", "java.time.LocalDate",
            "LocalDateTime", "java.time.LocalDateTime",
    };
}