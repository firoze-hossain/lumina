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

    public record Usage(Path file, int line, String preview, boolean declaration) {
    }

    // -------------------------------------------------------------- state

    private final Path projectRoot;
    private final List<Path> sourceRoots;
    private final JavaParser parser;
    private final int jarCount;

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
                           JavaParser parser, int jarCount) {
        this.projectRoot = projectRoot;
        this.sourceRoots = sourceRoots;
        this.parser = parser;
        this.jarCount = jarCount;
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
            if (classpath != null) {
                for (String part : classpath.split(Pattern.quote(File.pathSeparator))) {
                    if (!part.endsWith(".jar")) continue;
                    Path jar = Path.of(part);
                    if (!Files.isRegularFile(jar)) continue;
                    try {
                        solver.add(new JarTypeSolver(jar));
                        jars++;
                    } catch (Exception ignored) {
                        // unreadable jar: skip, resolution degrades gracefully
                    }
                }
            }

            ParserConfiguration config = new ParserConfiguration()
                    .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21)
                    .setSymbolResolver(new JavaSymbolSolver(solver));

            return new SemanticEngine(projectRoot, roots,
                    new JavaParser(config), jars);
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
        addDeclarationHit(hits, memberLocation(typeQn, method, paramCount, true));
        scanProject(method, (path, unit, lines) -> {
            for (MethodCallExpr call : unit.findAll(MethodCallExpr.class)) {
                if (!call.getNameAsString().equals(method)) continue;
                try {
                    ResolvedMethodDeclaration m = call.resolve();
                    if (m.declaringType().getQualifiedName().equals(typeQn)
                            && m.getNumberOfParams() == paramCount) {
                        addHit(hits, path, call.getRange(), lines, false);
                    }
                } catch (Throwable ignored) {
                }
            }
        });
        return finish(hits);
    }

    private List<Usage> fieldUsages(String typeQn, String field) {
        List<Usage> hits = new ArrayList<>();
        addDeclarationHit(hits, memberLocation(typeQn, field, -1, false));
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
                        addHit(hits, path, access.getRange(), lines, false);
                    }
                } catch (Throwable ignored) {
                }
            }
        });
        return finish(hits);
    }

    private List<Usage> typeUsages(String typeQn) {
        List<Usage> hits = new ArrayList<>();
        addDeclarationHit(hits, typeLocation(typeQn));
        String simple = typeQn.substring(typeQn.lastIndexOf('.') + 1);
        scanProject(simple, (path, unit, lines) -> {
            for (ClassOrInterfaceType type : unit.findAll(ClassOrInterfaceType.class)) {
                if (!type.getNameAsString().equals(simple)) continue;
                try {
                    ResolvedType rt = type.resolve();
                    if (rt.isReferenceType() && rt.asReferenceType()
                            .getQualifiedName().equals(typeQn)) {
                        addHit(hits, path, type.getRange(), lines, false);
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
        if (declaration.kind() == Kind.PROJECT && declaration.location() != null) {
            Location loc = declaration.location();
            String preview = "";
            try {
                List<String> lines = Files.readAllLines(loc.file());
                if (loc.line() - 1 < lines.size()) {
                    preview = lines.get(loc.line() - 1).strip();
                }
            } catch (Exception ignored) {
            }
            hits.add(new Usage(loc.file(), loc.line(), truncate(preview), true));
        }
    }

    private static void addHit(List<Usage> hits, Path path,
                               Optional<Range> range, List<String> lines,
                               boolean declaration) {
        if (range.isEmpty() || hits.size() >= 500) return;
        int line = range.get().begin.line;
        String preview = line - 1 < lines.size() ? lines.get(line - 1).strip() : "";
        hits.add(new Usage(path, line, truncate(preview), declaration));
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
}