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

import java.io.DataInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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

    public record Resolution(Kind kind, Location location, String libraryFqcn, String member, int paramCount) {
        public Resolution(Kind kind, Location location, String libraryFqcn) {
            this(kind, location, libraryFqcn, null, -1);
        }
        public static Resolution project(Location loc) {
            return new Resolution(Kind.PROJECT, loc, null, null, -1);
        }
        public static Resolution library(String fqcn) {
            return new Resolution(Kind.LIBRARY, null, fqcn, null, -1);
        }
        public static Resolution library(String fqcn, String member, int paramCount) {
            return new Resolution(Kind.LIBRARY, null, fqcn, member, paramCount);
        }
        public static Resolution declaration(Location loc) {
            return new Resolution(Kind.DECLARATION, loc, null, null, -1);
        }
        public static Resolution none() {
            return new Resolution(Kind.NONE, null, null, null, -1);
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
    private final Map<String, List<String>> libraryTypeIndex;   // simple -> fqcns, from jars
    private final Map<String, List<String>> annotationIndex;   // simple -> fqcns, indexed from classpath + JDK + project
    private final Set<String> interfaceTypes;   // simple names of interfaces (JDK, libraries, project)
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
                           Map<String, List<String>> libraryTypeIndex,
                           Map<String, List<String>> annotationIndex,
                           Set<String> interfaceTypes,
                           ClassLoader dependencyLoader) {
        this.projectRoot = projectRoot;
        this.sourceRoots = sourceRoots;
        this.parser = parser;
        this.jarCount = jarCount;
        this.typeIndex = typeIndex;
        this.libraryTypeIndex = libraryTypeIndex;
        this.annotationIndex = annotationIndex;
        this.interfaceTypes = interfaceTypes != null ? interfaceTypes : Set.of();
        this.dependencyLoader = dependencyLoader;
    }

    public Map<String, List<String>> annotationIndex() {
        return annotationIndex;
    }

    public Map<String, List<String>> typeIndex() {
        return typeIndex;
    }

    public Map<String, List<String>> libraryTypeIndex() {
        return libraryTypeIndex;
    }

    public Set<String> interfaceTypes() {
        return interfaceTypes;
    }

    public boolean isInterface(String simpleName) {
        return interfaceTypes != null && interfaceTypes.contains(simpleName);
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
            ParserConfiguration.LanguageLevel languageLevel = detectLanguageLevel(projectRoot);
            CombinedTypeSolver solver = new CombinedTypeSolver();
            solver.add(new ReflectionTypeSolver());   // JDK classes

            ParserConfiguration srcConfig = new ParserConfiguration()
                    .setLanguageLevel(languageLevel);

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
            // type-name completion with auto-import, plus project @interface annotations.
            Map<String, List<String>> typeIndex = new java.util.HashMap<>();
            Map<String, List<String>> annotationIndex = new java.util.HashMap<>();
            Set<String> interfaceTypes = new java.util.HashSet<>(Set.of(
                    "List", "Set", "Map", "Collection", "Iterable", "Iterator", "Queue", "Deque",
                    "Comparable", "Comparator", "Runnable", "Callable", "Closeable", "AutoCloseable",
                    "Serializable", "Cloneable", "Supplier", "Consumer", "Function", "Predicate",
                    "BiConsumer", "BiFunction", "BiPredicate", "Stream",
                    "JpaRepository", "CrudRepository", "ListCrudRepository",
                    "PagingAndSortingRepository", "ListPagingAndSortingRepository",
                    "Repository", "ReactiveCrudRepository", "MongoRepository", "ReactiveMongoRepository"
            ));

            // Seed standard JDK annotations
            for (int i = 0; i < JDK_ANNOTATIONS.length; i += 2) {
                String simple = JDK_ANNOTATIONS[i];
                String fqcn = JDK_ANNOTATIONS[i + 1];
                annotationIndex.computeIfAbsent(simple, k -> new ArrayList<>(1)).add(fqcn);
            }

            Pattern ANNOTATION_DECL = Pattern.compile("@interface\\s+([A-Za-z0-9_]+)");
            Pattern INTERFACE_DECL = Pattern.compile("(?:public\\s+)?interface\\s+([A-Za-z0-9_]+)");
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

                        // Scan for project annotations and interfaces
                        try {
                            String content = Files.readString(p);
                            if (content.contains("@interface")) {
                                var matcher = ANNOTATION_DECL.matcher(content);
                                while (matcher.find()) {
                                    String annSimple = matcher.group(1);
                                    String pkg = "";
                                    int pkgIdx = content.indexOf("package ");
                                    if (pkgIdx >= 0) {
                                        int semi = content.indexOf(';', pkgIdx);
                                        if (semi > pkgIdx) {
                                            pkg = content.substring(pkgIdx + 8, semi).trim();
                                        }
                                    }
                                    String annFqcn = pkg.isEmpty() ? annSimple : pkg + "." + annSimple;
                                    List<String> list = annotationIndex.computeIfAbsent(
                                            annSimple, k -> new ArrayList<>(1));
                                    if (!list.contains(annFqcn)) {
                                        list.add(0, annFqcn);
                                    }
                                }
                            }
                            if (content.contains("interface ") && !content.contains("@interface")) {
                                var ifaceMatcher = INTERFACE_DECL.matcher(content);
                                while (ifaceMatcher.find()) {
                                    interfaceTypes.add(ifaceMatcher.group(1));
                                }
                            }
                        } catch (Exception ignored) {
                        }
                    });
                } catch (Exception ignored) {
                }
            }

            // Type-name completion must also offer library types (Spring,
            // JPA, etc.) — not just project classes and a curated JDK list.
            // Every dependency jar's top-level public class names are
            // indexed once here, along with any @interface annotation classes.
            Map<String, List<String>> libraryTypeIndex = new java.util.HashMap<>();
            int libraryClassesIndexed = 0;
            for (Path jar : jarPaths) {
                if (libraryClassesIndexed > 60_000) break;   // safety cap
                try (java.util.zip.ZipFile zip = new java.util.zip.ZipFile(jar.toFile())) {
                    java.util.Enumeration<? extends java.util.zip.ZipEntry> entries =
                            zip.entries();
                    while (entries.hasMoreElements()) {
                        java.util.zip.ZipEntry entry = entries.nextElement();
                        String name = entry.getName();
                        if (!name.endsWith(".class") || name.startsWith("META-INF/")) {
                            continue;
                        }
                        // Library top-level types index
                        if (name.indexOf('$') < 0) {
                            String fqcn = name.substring(0, name.length() - 6)
                                    .replace('/', '.');
                            int dot = fqcn.lastIndexOf('.');
                            String simple = dot < 0 ? fqcn : fqcn.substring(dot + 1);
                            if (!simple.isEmpty() && Character.isUpperCase(simple.charAt(0))) {
                                List<String> list = libraryTypeIndex.computeIfAbsent(
                                        simple, k -> new ArrayList<>(2));
                                if (list.size() < 3) list.add(fqcn);   // cap ambiguous names
                                libraryClassesIndexed++;
                            }
                        }
                        // Classpath bytecode parser for interfaces and annotations
                        long sz = entry.getSize();
                        if (sz > 0 && sz <= 8192) {
                            String className = name.substring(0, name.length() - 6);
                            int slash = className.lastIndexOf('/');
                            String pkg = slash < 0 ? "" : className.substring(0, slash).replace('/', '.');
                            String simpleWithDollar = slash < 0 ? className : className.substring(slash + 1);
                            int dollar = simpleWithDollar.lastIndexOf('$');
                            String simple = dollar < 0 ? simpleWithDollar : simpleWithDollar.substring(dollar + 1);
                            if (!simple.isEmpty() && Character.isJavaIdentifierStart(simple.charAt(0))
                                    && Character.isUpperCase(simple.charAt(0))) {
                                Completion.Kind kind = bytecodeKind(zip.getInputStream(entry));
                                if (kind == Completion.Kind.ANNOTATION) {
                                    String fqcn = (pkg.isEmpty() ? "" : pkg + ".") + simpleWithDollar.replace('$', '.');
                                    List<String> list = annotationIndex.computeIfAbsent(
                                            simple, k -> new ArrayList<>(2));
                                    if (list.size() < 4 && !list.contains(fqcn)) {
                                        list.add(fqcn);
                                    }
                                } else if (kind == Completion.Kind.INTERFACE) {
                                    interfaceTypes.add(simple);
                                }
                            }
                        }
                    }
                } catch (Exception ignored) {
                    // unreadable jar: its types just won't be suggested
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
                    .setLanguageLevel(languageLevel)
                    .setSymbolResolver(new JavaSymbolSolver(solver));

            if (log != null) {
                log.accept("Semantic engine: parsing as " + languageLevel);
            }
            return new SemanticEngine(projectRoot, roots,
                    new JavaParser(config), jars, typeIndex, libraryTypeIndex, annotationIndex, interfaceTypes, loader);
        } catch (Throwable t) {
            if (log != null) log.accept("Semantic engine failed to start: " + t);
            return null;
        }
    }

    /**
     * Reads the project's own configured Java version out of pom.xml
     * (java.version / maven.compiler.release / maven.compiler.source) or
     * build.gradle(.kts) (JavaLanguageVersion.of(N) / sourceCompatibility)
     * so the parser accepts exactly the language features that project
     * actually targets \u2014 var/records/sealed types on 17+, but not features
     * an older-targeted project couldn't use. Falls back to JAVA_21 (the
     * newest level this JavaParser version reliably supports) when nothing
     * is found, which stays permissive rather than rejecting valid code.
     */
    private static ParserConfiguration.LanguageLevel detectLanguageLevel(Path projectRoot) {
        try {
            Path pom = projectRoot.resolve("pom.xml");
            if (Files.isRegularFile(pom)) {
                String text = Files.readString(pom);
                java.util.regex.Matcher m = java.util.regex.Pattern.compile(
                        "<(?:java\\.version|maven\\.compiler\\.release"
                                + "|maven\\.compiler\\.source)>\\s*(1\\.)?(\\d+)\\s*<")
                        .matcher(text);
                if (m.find()) return levelFor(Integer.parseInt(m.group(2)));
            }
            for (String name : new String[]{"build.gradle", "build.gradle.kts"}) {
                Path gradle = projectRoot.resolve(name);
                if (!Files.isRegularFile(gradle)) continue;
                String text = Files.readString(gradle);
                java.util.regex.Matcher m = java.util.regex.Pattern
                        .compile("JavaLanguageVersion\\.of\\((\\d+)\\)").matcher(text);
                if (m.find()) return levelFor(Integer.parseInt(m.group(1)));
                m = java.util.regex.Pattern.compile(
                        "(?:sourceCompatibility|targetCompatibility)\\s*=\\s*"
                                + "['\"]?(?:JavaVersion\\.VERSION_)?(1\\.)?(\\d+)").matcher(text);
                if (m.find()) return levelFor(Integer.parseInt(m.group(2)));
            }
        } catch (Exception ignored) {
        }
        return ParserConfiguration.LanguageLevel.JAVA_21;
    }

    /** Rounds down to the nearest LTS level this JavaParser build reliably
     *  names, rather than guessing at less-common intermediate constants. */
    private static ParserConfiguration.LanguageLevel levelFor(int version) {
        if (version <= 8) return ParserConfiguration.LanguageLevel.JAVA_8;
        if (version <= 11) return ParserConfiguration.LanguageLevel.JAVA_11;
        if (version <= 17) return ParserConfiguration.LanguageLevel.JAVA_17;
        return ParserConfiguration.LanguageLevel.JAVA_21;
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
                try {
                    ResolvedMethodDeclaration m = call.resolve();
                    return memberLocation(m.declaringType().getQualifiedName(),
                            m.getName(), m.getNumberOfParams(), true);
                } catch (Throwable unresolved) {
                    String memberName = target.getIdentifier();
                    String receiverType = null;
                    if (call.getScope().isPresent()) {
                        com.github.javaparser.ast.expr.Expression scope = call.getScope().get();
                        if (scope.isNameExpr()) {
                            String recName = scope.asNameExpr().getNameAsString();
                            if (Character.isUpperCase(recName.charAt(0))) {
                                receiverType = resolveTypeName(recName, text);
                            } else {
                                String dt = declaredTypeOf(file, text, line, recName);
                                if (dt != null) {
                                    receiverType = dt.contains(".") ? dt : resolveTypeName(dt, text);
                                }
                            }
                        }
                    } else {
                        receiverType = fqcnForFile(file);
                    }
                    if (receiverType != null) {
                        LombokSupport.Location loc = LombokSupport.resolveLombokDeclaration(
                                receiverType, memberName, sourceRoots, text);
                        if (loc != null) {
                            return Resolution.project(new Location(loc.file(), loc.line()));
                        }
                    }
                    return Resolution.none();
                }
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
                try {
                    ResolvedValueDeclaration v = name.resolve();
                    if (v instanceof ResolvedFieldDeclaration f) {
                        return memberLocation(f.declaringType().getQualifiedName(),
                                f.getName(), -1, false);
                    }
                    // local variable or parameter: find it in this same file
                    Location local = localDeclaration(cu, file, target);
                    return local != null ? Resolution.project(local) : Resolution.none();
                } catch (Throwable unresolved) {
                    if ("log".equals(target.getIdentifier())) {
                        String currentClass = fqcnForFile(file);
                        if (currentClass != null) {
                            LombokSupport.Location loc = LombokSupport.resolveLombokDeclaration(
                                    currentClass, "log", sourceRoots, text);
                            if (loc != null) {
                                return Resolution.project(new Location(loc.file(), loc.line()));
                            }
                        }
                    }
                    return Resolution.none();
                }
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
            return Resolution.library(libraryEntryName(typeQn), member, paramCount);
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
        String simple = typeQn.substring(typeQn.lastIndexOf('.') + 1);
        if (source == null) {
            return Resolution.library(libraryEntryName(typeQn), simple, -2);
        }
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
                try {
                    ResolvedMethodDeclaration m = call.resolve();
                    return methodUsages(m.declaringType().getQualifiedName(),
                            m.getName(), m.getNumberOfParams());
                } catch (Throwable unresolved) {
                    String receiverType = null;
                    if (call.getScope().isPresent()) {
                        com.github.javaparser.ast.expr.Expression scope = call.getScope().get();
                        if (scope.isNameExpr()) {
                            String recName = scope.asNameExpr().getNameAsString();
                            if (Character.isUpperCase(recName.charAt(0))) {
                                receiverType = resolveTypeName(recName, text);
                            } else {
                                String dt = declaredTypeOf(file, text, line, recName);
                                if (dt != null) receiverType = dt.contains(".") ? dt : resolveTypeName(dt, text);
                            }
                        }
                    } else {
                        receiverType = fqcnForFile(file);
                    }
                    if (receiverType != null) {
                        return methodUsages(receiverType, target.getIdentifier(), call.getArguments().size());
                    }
                }
            }
            if (parent instanceof MethodDeclaration md && md.getName() == target) {
                try {
                    ResolvedMethodDeclaration m = md.resolve();
                    return methodUsages(m.declaringType().getQualifiedName(),
                            m.getName(), m.getNumberOfParams());
                } catch (Throwable unresolved) {
                    String typeQn = fqcnForFile(file);
                    return methodUsages(typeQn, md.getNameAsString(), md.getParameters().size());
                }
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
                boolean matched = false;
                try {
                    ResolvedMethodDeclaration m = call.resolve();
                    if (m.declaringType().getQualifiedName().equals(typeQn)
                            && (paramCount < 0 || m.getNumberOfParams() == paramCount)) {
                        matched = true;
                    }
                } catch (Throwable unresolved) {
                    String simpleType = typeQn.substring(typeQn.lastIndexOf('.') + 1);
                    if (call.getScope().isPresent()) {
                        com.github.javaparser.ast.expr.Expression scope = call.getScope().get();
                        if (scope.isNameExpr()) {
                            String recName = scope.asNameExpr().getNameAsString();
                            if (recName.equalsIgnoreCase(simpleType)
                                    || recName.toLowerCase().endsWith(simpleType.toLowerCase())) {
                                matched = true;
                            } else {
                                int callLine = call.getBegin().map(p -> p.line).orElse(1);
                                String dt = declaredTypeOf(path, String.join("\n", lines), callLine, recName);
                                if (dt != null && (dt.equals(simpleType) || dt.equals(typeQn))) {
                                    matched = true;
                                }
                            }
                        } else if (scope.isFieldAccessExpr()) {
                            String fieldName = scope.asFieldAccessExpr().getNameAsString();
                            if (fieldName.equalsIgnoreCase(simpleType)) {
                                matched = true;
                            }
                        }
                    } else {
                        if (fqcnForFile(path).equals(typeQn)) {
                            matched = true;
                        }
                    }
                }
                if (matched) {
                    addHit(hits, path, call.getName().getRange(), lines, false);
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
            if (fqcn == null && receiver.endsWith("Builder")) {
                fqcn = resolveTypeName(receiver, text);
                if (fqcn == null) {
                    String base = receiver.substring(0, receiver.length() - "Builder".length());
                    String baseFqcn = resolveTypeName(base, text);
                    if (baseFqcn != null) {
                        fqcn = baseFqcn + "Builder";
                    }
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
                items.addAll(jpaMethodNameSuggestions(cu, caretLine, prefix));
            }
            if (!prefix.isEmpty()) {
                String filePkg = filePackage(text);
                java.util.Set<String> offered = new java.util.HashSet<>();
                for (Map.Entry<String, List<String>> e : typeIndex.entrySet()) {
                    if (!Completion.matches(prefix, e.getKey())) continue;
                    String fq = e.getValue().stream()
                            .filter(q -> packageOf(q).equals(filePkg))
                            .findFirst()
                            .orElse(e.getValue().get(0));
                    Completion.Kind kind = isInterface(e.getKey())
                            ? Completion.Kind.INTERFACE : Completion.Kind.CLASS;
                    items.add(new Completion.Item(e.getKey(), e.getKey(),
                            e.getKey(), packageOf(fq),
                            kind, fq, 0));
                    offered.add(e.getKey());
                }
                // Library types (Spring, JPA, Jackson, whatever's on the
                // classpath) — project types of the same simple name win.
                for (Map.Entry<String, List<String>> e : libraryTypeIndex.entrySet()) {
                    if (offered.contains(e.getKey())) continue;
                    if (!Completion.matches(prefix, e.getKey())) continue;
                    String fq = e.getValue().get(0);
                    Completion.Kind kind = isInterface(e.getKey())
                            ? Completion.Kind.INTERFACE : Completion.Kind.CLASS;
                    items.add(new Completion.Item(e.getKey(), e.getKey(),
                            e.getKey(), packageOf(fq),
                            kind, fq, 0));
                    offered.add(e.getKey());
                }
                for (int i = 0; i < JDK_TYPES.length; i += 2) {
                    String simple = JDK_TYPES[i];
                    if (offered.contains(simple)) continue;
                    if (!Completion.matches(prefix, simple)) continue;
                    Completion.Kind kind = isInterface(simple)
                            ? Completion.Kind.INTERFACE : Completion.Kind.CLASS;
                    items.add(new Completion.Item(simple, simple, simple,
                            packageOf(JDK_TYPES[i + 1]),
                            kind, JDK_TYPES[i + 1], 0));
                }
            }
        } catch (Throwable t) {
            // partial results are fine
        }
        return finishItems(items);
    }

    // ============================================================== annotations

    public static final String[] JDK_ANNOTATIONS = new String[]{
            "Target", "java.lang.annotation.Target",
            "Retention", "java.lang.annotation.Retention",
            "Documented", "java.lang.annotation.Documented",
            "Inherited", "java.lang.annotation.Inherited",
            "Repeatable", "java.lang.annotation.Repeatable",
            "Native", "java.lang.annotation.Native",
            "Override", "java.lang.Override",
            "Deprecated", "java.lang.Deprecated",
            "SuppressWarnings", "java.lang.SuppressWarnings",
            "FunctionalInterface", "java.lang.FunctionalInterface",
            "SafeVarargs", "java.lang.SafeVarargs",
            "Transient", "java.beans.Transient",
            "Generated", "javax.annotation.processing.Generated"
    };

    private static final List<String> POPULAR_ANNOTATIONS = List.of(
            "Target", "Retention", "Documented", "Inherited", "Repeatable",
            "Override", "Deprecated", "SuppressWarnings", "FunctionalInterface", "SafeVarargs",
            "Entity", "Table", "Id", "GeneratedValue", "Column",
            "SpringBootApplication", "Configuration", "Bean", "Component", "Service",
            "Repository", "Controller", "RestController", "Autowired", "Qualifier", "Value",
            "RequestMapping", "GetMapping", "PostMapping", "PutMapping", "DeleteMapping",
            "PatchMapping", "RequestParam", "PathVariable", "RequestBody", "ResponseBody",
            "NotNull", "NotEmpty", "NotBlank", "Valid", "Validated", "Size", "Min", "Max",
            "Getter", "Setter", "ToString", "EqualsAndHashCode", "NoArgsConstructor",
            "AllArgsConstructor", "RequiredArgsConstructor", "Builder", "Data", "Slf4j",
            "Transactional", "ManyToOne", "OneToMany", "ManyToMany", "OneToOne",
            "JoinColumn", "JoinTable", "Transient", "Enumerated", "Temporal", "Version"
    );

    /**
     * Parse the JVM class file header and constant pool to determine whether
     * the class is an ANNOTATION (@), INTERFACE (I), or regular CLASS (C).
     * Zero external dependencies, runs in ~5µs.
     */
    public static Completion.Kind bytecodeKind(InputStream in) {
        if (in == null) return Completion.Kind.CLASS;
        try (DataInputStream dis = new DataInputStream(in)) {
            if (dis.readInt() != 0xCAFEBABE) return Completion.Kind.CLASS;
            dis.readUnsignedShort(); // minor
            dis.readUnsignedShort(); // major
            int cpCount = dis.readUnsignedShort();
            for (int i = 1; i < cpCount; i++) {
                int tag = dis.readUnsignedByte();
                switch (tag) {
                    case 1 -> dis.skipNBytes(dis.readUnsignedShort());
                    case 3, 4, 9, 10, 11, 12, 17, 18 -> dis.skipNBytes(4);
                    case 5, 6 -> {
                        dis.skipNBytes(8);
                        i++; // Long and Double take two cp slots
                    }
                    case 7, 8, 16, 19, 20 -> dis.skipNBytes(2);
                    case 15 -> dis.skipNBytes(3);
                    default -> { return Completion.Kind.CLASS; }
                }
            }
            int accessFlags = dis.readUnsignedShort();
            if ((accessFlags & 0x2000) != 0) return Completion.Kind.ANNOTATION;
            if ((accessFlags & 0x0200) != 0) return Completion.Kind.INTERFACE;
            return Completion.Kind.CLASS;
        } catch (Exception e) {
            return Completion.Kind.CLASS;
        }
    }

    public static Completion.Kind bytecodeKind(byte[] bytes) {
        if (bytes == null || bytes.length < 10) return Completion.Kind.CLASS;
        return bytecodeKind(new java.io.ByteArrayInputStream(bytes));
    }

    public static boolean isAnnotationClass(InputStream in) {
        return bytecodeKind(in) == Completion.Kind.ANNOTATION;
    }

    public static boolean isAnnotationClass(byte[] bytes) {
        if (bytes == null || bytes.length < 10) return false;
        return isAnnotationClass(new java.io.ByteArrayInputStream(bytes));
    }

    /**
     * Provide dynamic, dependency-aware annotation completion items.
     * When prefix is empty (user just typed '@'), presents popular available
     * annotations. When prefix is non-empty (e.g. "Ent"), filters matching
     * annotations with exact and prefix matches ranked first.
     */
    public List<Completion.Item> annotationCompletions(String prefix) {
        String p = prefix == null ? "" : prefix.trim();
        List<Completion.Item> items = new ArrayList<>();
        java.util.Set<String> seenFqcns = new java.util.HashSet<>();

        if (p.isEmpty()) {
            for (String pop : POPULAR_ANNOTATIONS) {
                List<String> fqcns = annotationIndex.get(pop);
                if (fqcns != null) {
                    for (String fqcn : fqcns) {
                        if (seenFqcns.add(fqcn)) {
                            items.add(new Completion.Item(pop, pop, pop,
                                    "(" + packageOf(fqcn) + ")",
                                    Completion.Kind.ANNOTATION, fqcn, 0));
                        }
                    }
                }
            }
            List<Map.Entry<String, List<String>>> remaining = new ArrayList<>(annotationIndex.entrySet());
            remaining.sort(Map.Entry.comparingByKey(String.CASE_INSENSITIVE_ORDER));
            for (Map.Entry<String, List<String>> e : remaining) {
                for (String fqcn : e.getValue()) {
                    if (seenFqcns.add(fqcn)) {
                        items.add(new Completion.Item(e.getKey(), e.getKey(), e.getKey(),
                                "(" + packageOf(fqcn) + ")",
                                Completion.Kind.ANNOTATION, fqcn, 0));
                    }
                }
            }
            return items.stream().limit(100).toList();
        }

        for (Map.Entry<String, List<String>> e : annotationIndex.entrySet()) {
            String simple = e.getKey();
            if (!Completion.matches(p, simple)) continue;
            for (String fqcn : e.getValue()) {
                if (seenFqcns.add(fqcn)) {
                    items.add(new Completion.Item(simple, simple, simple,
                            "(" + packageOf(fqcn) + ")",
                            Completion.Kind.ANNOTATION, fqcn, 0));
                }
            }
        }

        items.sort((a, b) -> {
            int rankA = annotationRank(p, a.name());
            int rankB = annotationRank(p, b.name());
            if (rankA != rankB) return Integer.compare(rankA, rankB);
            if (rankA == 1) {
                int lenCmp = Integer.compare(a.name().length(), b.name().length());
                if (lenCmp != 0) return lenCmp;
            }
            return a.name().compareToIgnoreCase(b.name());
        });

        return items.stream().limit(100).toList();
    }

    private static int annotationRank(String prefix, String name) {
        if (name.equalsIgnoreCase(prefix)) return 0;
        if (name.regionMatches(true, 0, prefix, 0, prefix.length())) return 1;
        if (Completion.matches(prefix, name)) return 2;
        return 3;
    }

    public static List<Completion.Item> fallbackAnnotationCompletions(String prefix) {
        String p = prefix == null ? "" : prefix.trim();
        List<Completion.Item> items = new ArrayList<>();
        for (int i = 0; i < JDK_ANNOTATIONS.length; i += 2) {
            String simple = JDK_ANNOTATIONS[i];
            String fqcn = JDK_ANNOTATIONS[i + 1];
            if (p.isEmpty() || Completion.matches(p, simple)) {
                items.add(new Completion.Item(simple, simple, simple,
                        "(" + packageOf(fqcn) + ")",
                        Completion.Kind.ANNOTATION, fqcn, 0));
            }
        }
        return items;
    }

    // =============================================== repository completions & entities

    public record EntityInfo(String simpleName, String fqcn, String idType, boolean hasId) {}

    /**
     * Resolves a simple class/interface name to its FQCN.
     * Looks up project types first, then classpath library types, then JDK types.
     */
    public String resolveFqcn(String simpleName) {
        if (simpleName == null || simpleName.isBlank()) return null;
        List<String> projectMatches = typeIndex.get(simpleName);
        if (projectMatches != null && !projectMatches.isEmpty()) {
            return projectMatches.get(0);
        }
        List<String> libMatches = libraryTypeIndex.get(simpleName);
        if (libMatches != null && !libMatches.isEmpty()) {
            return libMatches.get(0);
        }
        for (int i = 0; i < JDK_TYPES.length; i += 2) {
            if (JDK_TYPES[i].equals(simpleName)) {
                return JDK_TYPES[i + 1];
            }
        }
        return null;
    }

    /**
     * Scans project source files for JPA @Entity classes and their primary key @Id types.
     */
    public List<EntityInfo> findProjectEntities() {
        List<EntityInfo> entities = new ArrayList<>();
        Pattern ENTITY_ANN = Pattern.compile("@(?:jakarta\\.persistence\\.|javax\\.persistence\\.)?Entity\\b");
        Pattern CLASS_DECL = Pattern.compile("(?:public\\s+)?class\\s+([A-Za-z0-9_]+)");
        Pattern ID_FIELD = Pattern.compile("@(?:jakarta\\.persistence\\.|javax\\.persistence\\.)?(?:Id|EmbeddedId)\\s+(?:private|protected|public)?\\s*([A-Za-z0-9_<>]+)\\s+([A-Za-z0-9_]+)");
        Pattern ID_ANN = Pattern.compile("@(?:jakarta\\.persistence\\.|javax\\.persistence\\.)?(?:Id|EmbeddedId)\\b");

        for (Path root : sourceRoots) {
            try (Stream<Path> walk = Files.walk(root)) {
                walk.filter(p -> p.toString().endsWith(".java")).forEach(p -> {
                    try {
                        String content = Files.readString(p);
                        if (!ENTITY_ANN.matcher(content).find()) return;
                        var classMatcher = CLASS_DECL.matcher(content);
                        if (!classMatcher.find()) return;
                        String className = classMatcher.group(1);

                        String pkg = "";
                        int pkgIdx = content.indexOf("package ");
                        if (pkgIdx >= 0) {
                            int semi = content.indexOf(';', pkgIdx);
                            if (semi > pkgIdx) pkg = content.substring(pkgIdx + 8, semi).trim();
                        }
                        String fqcn = pkg.isEmpty() ? className : pkg + "." + className;

                        String idType = "Long";
                        boolean hasId = false;
                        var idMatcher = ID_FIELD.matcher(content);
                        if (idMatcher.find()) {
                            hasId = true;
                            String rawType = idMatcher.group(1).trim();
                            idType = switch (rawType) {
                                case "long" -> "Long";
                                case "int" -> "Integer";
                                case "short" -> "Short";
                                case "byte" -> "Byte";
                                case "float" -> "Float";
                                case "double" -> "Double";
                                default -> rawType;
                            };
                        } else if (ID_ANN.matcher(content).find()) {
                            hasId = true;
                        }
                        entities.add(new EntityInfo(className, fqcn, idType, hasId));
                    } catch (Exception ignored) {
                    }
                });
            } catch (Exception ignored) {
            }
        }
        return entities;
    }

    /**
     * Suggests Spring Data repository extensions (e.g. JpaRepository<Student, Long>,
     * CrudRepository<Student, Long>) matching the entity inferred from the interface name.
     */
    public List<Completion.Item> repositoryExtendsCompletions(String interfaceName, String prefix) {
        String p = prefix == null ? "" : prefix.trim();
        List<EntityInfo> entities = findProjectEntities();

        String entityCandidate = null;
        if (interfaceName != null && !interfaceName.isBlank()) {
            if (interfaceName.endsWith("EntityRepository")) {
                entityCandidate = interfaceName.substring(0, interfaceName.length() - 16);
            } else if (interfaceName.endsWith("Repository")) {
                entityCandidate = interfaceName.substring(0, interfaceName.length() - 10);
            } else if (interfaceName.endsWith("Repo")) {
                entityCandidate = interfaceName.substring(0, interfaceName.length() - 4);
            } else if (interfaceName.endsWith("Dao")) {
                entityCandidate = interfaceName.substring(0, interfaceName.length() - 3);
            } else {
                entityCandidate = interfaceName;
            }
        }

        EntityInfo chosenEntity = null;
        if (entityCandidate != null && !entityCandidate.isBlank()) {
            for (EntityInfo e : entities) {
                if (e.simpleName().equalsIgnoreCase(entityCandidate)) {
                    chosenEntity = e;
                    break;
                }
            }
            if (chosenEntity == null) {
                chosenEntity = new EntityInfo(entityCandidate, null, "Long", false);
            }
        } else if (!entities.isEmpty()) {
            chosenEntity = entities.get(0);
        } else {
            chosenEntity = new EntityInfo("Entity", null, "Long", false);
        }

        String entityName = chosenEntity.simpleName();
        String idType = chosenEntity.idType();

        List<Completion.Item> items = new ArrayList<>();

        record RepoDef(String simpleName, String fqcn, int priority) {}
        List<RepoDef> repoDefs = new ArrayList<>();
        repoDefs.add(new RepoDef("JpaRepository", "org.springframework.data.jpa.repository.JpaRepository", 1));
        repoDefs.add(new RepoDef("CrudRepository", "org.springframework.data.repository.CrudRepository", 2));
        repoDefs.add(new RepoDef("ListCrudRepository", "org.springframework.data.repository.ListCrudRepository", 3));
        repoDefs.add(new RepoDef("PagingAndSortingRepository", "org.springframework.data.repository.PagingAndSortingRepository", 4));
        repoDefs.add(new RepoDef("ListPagingAndSortingRepository", "org.springframework.data.repository.ListPagingAndSortingRepository", 5));
        repoDefs.add(new RepoDef("Repository", "org.springframework.data.repository.Repository", 6));

        if (libraryTypeIndex != null && libraryTypeIndex.containsKey("MongoRepository")) {
            repoDefs.add(new RepoDef("MongoRepository", "org.springframework.data.mongodb.repository.MongoRepository", 7));
        }

        for (RepoDef def : repoDefs) {
            String fullWithGenerics = def.simpleName + "<" + entityName + ", " + idType + ">";
            if (p.isEmpty() || Completion.matches(p, def.simpleName) || Completion.matches(p, fullWithGenerics)) {
                items.add(new Completion.Item(
                        def.simpleName,
                        fullWithGenerics,
                        fullWithGenerics,
                        " (" + packageOf(def.fqcn) + ")",
                        Completion.Kind.INTERFACE,
                        def.fqcn,
                        0
                ));
            }
        }

        return items;
    }

    public static List<Completion.Item> fallbackRepositoryCompletions(String interfaceName, String prefix) {
        String p = prefix == null ? "" : prefix.trim();
        String entityCandidate = "Entity";
        if (interfaceName != null && !interfaceName.isBlank()) {
            if (interfaceName.endsWith("EntityRepository")) {
                entityCandidate = interfaceName.substring(0, interfaceName.length() - 16);
            } else if (interfaceName.endsWith("Repository")) {
                entityCandidate = interfaceName.substring(0, interfaceName.length() - 10);
            } else if (interfaceName.endsWith("Repo")) {
                entityCandidate = interfaceName.substring(0, interfaceName.length() - 4);
            } else if (interfaceName.endsWith("Dao")) {
                entityCandidate = interfaceName.substring(0, interfaceName.length() - 3);
            } else {
                entityCandidate = interfaceName;
            }
        }
        if (entityCandidate.isEmpty()) entityCandidate = "Entity";
        String idType = "Long";

        List<Completion.Item> items = new ArrayList<>();
        String[] repos = {
            "JpaRepository", "org.springframework.data.jpa.repository.JpaRepository",
            "CrudRepository", "org.springframework.data.repository.CrudRepository",
            "ListCrudRepository", "org.springframework.data.repository.ListCrudRepository",
            "PagingAndSortingRepository", "org.springframework.data.repository.PagingAndSortingRepository"
        };
        for (int i = 0; i < repos.length; i += 2) {
            String simple = repos[i];
            String fqcn = repos[i + 1];
            String full = simple + "<" + entityCandidate + ", " + idType + ">";
            if (p.isEmpty() || Completion.matches(p, simple) || Completion.matches(p, full)) {
                items.add(new Completion.Item(simple, full, full, " (" + packageOf(fqcn) + ")",
                        Completion.Kind.INTERFACE, fqcn, 0));
            }
        }
        return items;
    }

    // ===================================================== go to implementation

    private static final java.util.Set<String> SPRING_BEAN_ANNOTATIONS = java.util.Set.of(
            "Service", "Component", "Repository", "Controller", "RestController");

    /**
     * IntelliJ's Ctrl+Alt+B: resolves the interface type at the caret (a
     * field/parameter's declared type, or the type name itself) and finds
     * every project class that both implements it and carries a Spring
     * stereotype annotation \u2014 the concrete bean, not just the contract.
     * Empty when the caret isn't on an interface reference, or when no
     * annotated implementation exists in the project.
     */
    public List<Location> findImplementations(Path file, String text, int line, int column) {
        try {
            CompilationUnit cu = parse(file, text);
            if (cu == null) return List.of();
            SimpleName target = nameAt(cu, line, column);
            if (target == null) return List.of();
            Node parent = target.getParentNode().orElse(null);
            if (parent == null) return List.of();

            String typeSimple = null;
            if (parent instanceof ClassOrInterfaceType type && type.getName() == target) {
                typeSimple = target.getIdentifier();
            } else if (parent instanceof NameExpr || parent instanceof VariableDeclarator
                    || parent instanceof Parameter) {
                String declaredType = declaredTypeOf(file, text, line, target.getIdentifier());
                if (declaredType != null) {
                    typeSimple = declaredType.contains(".")
                            ? declaredType.substring(declaredType.lastIndexOf('.') + 1)
                            : declaredType;
                }
            }
            if (typeSimple == null) return List.of();
            return findSpringImplementations(typeSimple);
        } catch (Throwable t) {
            return List.of();
        }
    }

    private List<Location> findSpringImplementations(String interfaceSimple) {
        List<String> candidates = typeIndex.get(interfaceSimple);
        if (candidates == null || candidates.isEmpty()) return List.of();
        Path interfaceSource = sourceFileFor(candidates.get(0));
        if (interfaceSource == null) return List.of();
        CompilationUnit ifaceCu = parse(interfaceSource, null);
        boolean isInterface = ifaceCu != null
                && ifaceCu.findAll(ClassOrInterfaceDeclaration.class).stream()
                        .anyMatch(d -> d.isInterface()
                                && d.getNameAsString().equals(interfaceSimple));
        if (!isInterface) return List.of();

        List<Location> out = new ArrayList<>();
        scanProject(interfaceSimple, (path, unit, lines) -> {
            for (ClassOrInterfaceDeclaration d
                    : unit.findAll(ClassOrInterfaceDeclaration.class)) {
                if (d.isInterface()) continue;
                boolean implementsIt = d.getImplementedTypes().stream()
                        .anyMatch(t -> t.getNameAsString().equals(interfaceSimple));
                if (!implementsIt) continue;
                boolean isBean = d.getAnnotations().stream()
                        .anyMatch(a -> SPRING_BEAN_ANNOTATIONS.contains(a.getNameAsString()));
                if (!isBean) continue;
                out.add(new Location(path, beginLine(d)));
            }
        });
        return out;
    }

    // ------------------------------------------------------- member listing

    private static final java.util.Set<String> JPA_REPOSITORY_MARKERS = java.util.Set.of(
            "JpaRepository", "CrudRepository", "PagingAndSortingRepository",
            "ListCrudRepository", "ListPagingAndSortingRepository",
            "ReactiveCrudRepository", "MongoRepository", "ReactiveMongoRepository");

    /**
     * Spring Data JPA's signature feature: typing a new method name inside
     * a repository interface (one extending JpaRepository&lt;Entity, Id&gt;
     * or a sibling) suggests derived query names \u2014 findByX, existsByX,
     * countByX, deleteByX \u2014 built from the entity's real declared field
     * names, resolved from project source. Empty outside a repository
     * interface or when the entity type can't be found in the project.
     */
    private List<Completion.Item> jpaMethodNameSuggestions(CompilationUnit cu,
                                                            int caretLine, String prefix) {
        List<Completion.Item> out = new ArrayList<>();
        if (prefix.isEmpty()) return out;
        try {
            for (ClassOrInterfaceDeclaration decl
                    : cu.findAll(ClassOrInterfaceDeclaration.class)) {
                if (!decl.isInterface() || !rangeContainsLine(decl, caretLine)) continue;
                for (ClassOrInterfaceType ext : decl.getExtendedTypes()) {
                    if (!JPA_REPOSITORY_MARKERS.contains(ext.getNameAsString())) continue;
                    var typeArgs = ext.getTypeArguments();
                    if (typeArgs.isEmpty() || typeArgs.get().isEmpty()) continue;
                    String entitySimple = typeArgs.get().get(0).asString();
                    List<String> stringFields = new ArrayList<>();
                    for (String field : entityFieldNames(entitySimple, stringFields)) {
                        String cap = Character.toUpperCase(field.charAt(0))
                                + field.substring(1);
                        String entityRef = "List<" + entitySimple + ">";
                        jpaItem(out, prefix, "findBy" + cap,
                                entityRef + " \u2014 Spring Data derived query");
                        jpaItem(out, prefix, "existsBy" + cap,
                                "boolean \u2014 Spring Data derived query");
                        jpaItem(out, prefix, "countBy" + cap,
                                "long \u2014 Spring Data derived query");
                        jpaItem(out, prefix, "deleteBy" + cap,
                                "void \u2014 Spring Data derived query");
                        if (stringFields.contains(field)) {
                            jpaItem(out, prefix, "findBy" + cap + "Containing",
                                    entityRef + " \u2014 LIKE %value%");
                            jpaItem(out, prefix, "findBy" + cap + "ContainingIgnoreCase",
                                    entityRef + " \u2014 LIKE %value%, case-insensitive");
                        }
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return out;
    }

    private static void jpaItem(List<Completion.Item> out, String prefix,
                                String name, String detail) {
        if (!Completion.matches(prefix, name)) return;
        out.add(new Completion.Item(name, name, name + "()", ": " + detail,
                Completion.Kind.METHOD, null, 1));
    }

    /** Non-static field names of a project-source entity, by simple name;
     *  also collects which of those are String-typed into stringFieldsOut. */
    private List<String> entityFieldNames(String simpleTypeName,
                                          List<String> stringFieldsOut) {
        List<String> fqcns = typeIndex.get(simpleTypeName);
        if (fqcns == null || fqcns.isEmpty()) return List.of();
        Path source = sourceFileFor(fqcns.get(0));
        if (source == null) return List.of();
        CompilationUnit entityCu = parse(source, null);
        if (entityCu == null) return List.of();
        List<String> fields = new ArrayList<>();
        for (FieldDeclaration fd : entityCu.findAll(FieldDeclaration.class)) {
            if (isStaticField(fd)) continue;
            for (VariableDeclarator v : fd.getVariables()) {
                fields.add(v.getNameAsString());
                if (v.getType().asString().equals("String")) {
                    stringFieldsOut.add(v.getNameAsString());
                }
            }
        }
        return fields;
    }

    private static boolean isStaticField(FieldDeclaration fd) {
        try {
            return fd.isStatic();
        } catch (Throwable t) {
            return false;
        }
    }

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
                    items.addAll(LombokSupport.getLombokCompletions(owner, prefix, staticOnly, sourceRoots));
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

        if (fqcn.endsWith("Builder")) {
            String outerFqcn = fqcn.contains(".")
                    ? fqcn.substring(0, fqcn.lastIndexOf('.'))
                    : fqcn.substring(0, fqcn.length() - "Builder".length());
            Path outerSource = sourceFileFor(outerFqcn);
            if (outerSource != null) {
                CompilationUnit cu = parse(outerSource, null);
                if (cu != null) {
                    String outerSimple = outerFqcn.substring(outerFqcn.lastIndexOf('.') + 1);
                    for (ClassOrInterfaceDeclaration d : cu.findAll(ClassOrInterfaceDeclaration.class)) {
                        if (d.getNameAsString().equals(outerSimple)) {
                            items.addAll(LombokSupport.getBuilderCompletions(d, prefix, sourceRoots));
                            return items;
                        }
                    }
                }
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

    private static String cleanType(String type) {
        if (type == null) return null;
        return type.replaceAll("\\b[a-z0-9_.]+\\.([A-Z][a-zA-Z0-9_]*)", "$1");
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

    /**
     * Resolves rich symbol documentation for hover quick info and quick doc popup,
     * including container type, clean formatted signature, parameters, and module info.
     */
    public Docs.SymbolDoc symbolDocAt(Path file, String text, int line, int column) {
        CompilationUnit cu = parse(file, text);
        if (cu == null) return null;
        SimpleName target = nameAt(cu, line, column);
        if (target == null) return null;
        Node parent = target.getParentNode().orElse(null);
        if (parent == null) return null;

        String moduleName = projectRoot != null ? projectRoot.getFileName().toString() : "";

        try {
            // Case 1: Method Declaration in current file
            if (parent instanceof MethodDeclaration md && md.getName() == target) {
                String containerFqcn = fqcnForFile(file);
                String kind = "method";
                Node p = md.getParentNode().orElse(null);
                if (p instanceof ClassOrInterfaceDeclaration cd) {
                    kind = cd.isInterface() ? "interface" : "class";
                }
                List<String> annotations = new ArrayList<>();
                for (var ann : md.getAnnotations()) {
                    annotations.add(ann.toString());
                }
                List<String> params = new ArrayList<>();
                for (Parameter param : md.getParameters()) {
                    StringBuilder paramStr = new StringBuilder();
                    for (var pa : param.getAnnotations()) {
                        paramStr.append(pa.toString()).append(" ");
                    }
                    paramStr.append(cleanType(param.getType().asString())).append(" ").append(param.getNameAsString());
                    params.add(paramStr.toString());
                }
                String javadoc = Docs.javadocAbove(text.lines().toList(), beginLine(md));
                return new Docs.SymbolDoc(kind, containerFqcn, cleanType(md.getType().asString()),
                        md.getNameAsString(), params, annotations, moduleName, javadoc, file, beginLine(md));
            }

            // Case 2: Class/Interface/Record/Enum Declaration in current file
            if (parent instanceof ClassOrInterfaceDeclaration cd && cd.getName() == target) {
                String containerFqcn = fqcnForFile(file);
                String kind = cd.isInterface() ? "interface" : "class";
                String javadoc = Docs.javadocAbove(text.lines().toList(), beginLine(cd));
                return new Docs.SymbolDoc(kind, containerFqcn, null, cd.getNameAsString(),
                        null, moduleName, javadoc, file, beginLine(cd));
            }
            if (parent instanceof EnumDeclaration ed && ed.getName() == target) {
                String containerFqcn = fqcnForFile(file);
                String javadoc = Docs.javadocAbove(text.lines().toList(), beginLine(ed));
                return new Docs.SymbolDoc("enum", containerFqcn, null, ed.getNameAsString(),
                        null, moduleName, javadoc, file, beginLine(ed));
            }
            if (parent instanceof RecordDeclaration rd && rd.getName() == target) {
                String containerFqcn = fqcnForFile(file);
                String javadoc = Docs.javadocAbove(text.lines().toList(), beginLine(rd));
                return new Docs.SymbolDoc("record", containerFqcn, null, rd.getNameAsString(),
                        null, moduleName, javadoc, file, beginLine(rd));
            }

            // Case 3: Method Call at usage / call site
            if (parent instanceof MethodCallExpr call && call.getName() == target) {
                String memberName = target.getIdentifier();
                // 3a: Try JavaParser resolve()
                try {
                    ResolvedMethodDeclaration m = call.resolve();
                    String declaringFqcn = m.declaringType().getQualifiedName();
                    Path declPath = sourceFileFor(declaringFqcn);
                    if (declPath != null) {
                        CompilationUnit targetCu = parse(declPath, null);
                        if (targetCu != null) {
                            for (MethodDeclaration md : targetCu.findAll(MethodDeclaration.class)) {
                                if (md.getNameAsString().equals(memberName)
                                        && (m.getNumberOfParams() < 0 || md.getParameters().size() == m.getNumberOfParams())) {
                                    String kind = "class";
                                    Node p = md.getParentNode().orElse(null);
                                    if (p instanceof ClassOrInterfaceDeclaration cd) {
                                        kind = cd.isInterface() ? "interface" : "class";
                                    }
                                    List<String> annotations = new ArrayList<>();
                                    for (var ann : md.getAnnotations()) {
                                        annotations.add(ann.toString());
                                    }
                                    List<String> params = new ArrayList<>();
                                    for (Parameter param : md.getParameters()) {
                                        StringBuilder paramStr = new StringBuilder();
                                        for (var pa : param.getAnnotations()) {
                                            paramStr.append(pa.toString()).append(" ");
                                        }
                                        paramStr.append(cleanType(param.getType().asString())).append(" ").append(param.getNameAsString());
                                        params.add(paramStr.toString());
                                    }
                                    List<String> srcLines = Files.readAllLines(declPath);
                                    String javadoc = Docs.javadocAbove(srcLines, beginLine(md));
                                    return new Docs.SymbolDoc(kind, declaringFqcn, cleanType(md.getType().asString()),
                                            memberName, params, annotations, moduleName, javadoc, declPath, beginLine(md));
                                }
                            }
                        }
                    }
                    // Library / reflection fallback
                    List<String> params = new ArrayList<>();
                    for (int i = 0; i < m.getNumberOfParams(); i++) {
                        params.add(m.getParam(i).getType().describe() + " " + m.getParam(i).getName());
                    }
                    return new Docs.SymbolDoc("method", declaringFqcn, m.getReturnType().describe(),
                            memberName, params, List.of(), moduleName, null, null, -1);
                } catch (Throwable unresolved) {
                    // 3b: Fallback for Spring Data repositories, Lombok, etc.
                    String receiverType = null;
                    if (call.getScope().isPresent()) {
                        com.github.javaparser.ast.expr.Expression scope = call.getScope().get();
                        if (scope.isNameExpr()) {
                            String recName = scope.asNameExpr().getNameAsString();
                            if (Character.isUpperCase(recName.charAt(0))) {
                                receiverType = resolveTypeName(recName, text);
                            } else {
                                String dt = declaredTypeOf(file, text, line, recName);
                                if (dt != null) {
                                    receiverType = dt.contains(".") ? dt : resolveTypeName(dt, text);
                                }
                            }
                        }
                    } else {
                        receiverType = fqcnForFile(file);
                    }
                    if (receiverType != null) {
                        Path declPath = sourceFileFor(receiverType);
                        if (declPath != null) {
                            CompilationUnit targetCu = parse(declPath, null);
                            if (targetCu != null) {
                                for (MethodDeclaration md : targetCu.findAll(MethodDeclaration.class)) {
                                    if (md.getNameAsString().equals(memberName)) {
                                        String kind = "interface";
                                        Node p = md.getParentNode().orElse(null);
                                        if (p instanceof ClassOrInterfaceDeclaration cd) {
                                            kind = cd.isInterface() ? "interface" : "class";
                                        }
                                        List<String> annotations = new ArrayList<>();
                                        for (var ann : md.getAnnotations()) {
                                            annotations.add(ann.toString());
                                        }
                                        List<String> params = new ArrayList<>();
                                        for (Parameter param : md.getParameters()) {
                                            StringBuilder paramStr = new StringBuilder();
                                            for (var pa : param.getAnnotations()) {
                                                paramStr.append(pa.toString()).append(" ");
                                            }
                                            paramStr.append(cleanType(param.getType().asString())).append(" ").append(param.getNameAsString());
                                            params.add(paramStr.toString());
                                        }
                                        List<String> srcLines = Files.readAllLines(declPath);
                                        String javadoc = Docs.javadocAbove(srcLines, beginLine(md));
                                        return new Docs.SymbolDoc(kind, receiverType, cleanType(md.getType().asString()),
                                                memberName, params, annotations, moduleName, javadoc, declPath, beginLine(md));
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Case 4: ClassOrInterfaceType
            if (parent instanceof ClassOrInterfaceType type && type.getName() == target) {
                try {
                    ResolvedType rt = type.resolve();
                    if (rt.isReferenceType()) {
                        String fqcn = rt.asReferenceType().getQualifiedName();
                        Path src = sourceFileFor(fqcn);
                        String kind = isInterface(target.getIdentifier()) ? "interface" : "class";
                        String javadoc = null;
                        int declLine = -1;
                        if (src != null) {
                            List<String> srcLines = Files.readAllLines(src);
                            declLine = 1;
                            javadoc = Docs.javadocAbove(srcLines, declLine);
                        }
                        return new Docs.SymbolDoc(kind, fqcn, null, target.getIdentifier(),
                                null, moduleName, javadoc, src, declLine);
                    }
                } catch (Throwable ignored) {
                    String simple = target.getIdentifier();
                    String fqcn = resolveTypeName(simple, text);
                    if (fqcn != null) {
                        Path src = sourceFileFor(fqcn);
                        return new Docs.SymbolDoc("class", fqcn, null, simple,
                                null, moduleName, null, src, src != null ? 1 : -1);
                    }
                }
            }

            // Case 5: Field or Variable
            if (parent instanceof VariableDeclarator vd && vd.getName() == target) {
                String typeName = simpleType(vd.getType().asString());
                String containerFqcn = fqcnForFile(file);
                return new Docs.SymbolDoc("field", containerFqcn, typeName, vd.getNameAsString(),
                        null, moduleName, null, file, beginLine(vd));
            }
            if (parent instanceof NameExpr name && name.getName() == target) {
                String typeName = declaredTypeOf(file, text, line, target.getIdentifier());
                String containerFqcn = fqcnForFile(file);
                return new Docs.SymbolDoc("variable", containerFqcn, typeName != null ? typeName : "",
                        target.getIdentifier(), null, moduleName, null, file, line);
            }
        } catch (Throwable ignored) {
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