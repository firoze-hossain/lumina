package dev.lumina.gutter;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.type.ClassOrInterfaceType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Dynamic background service for detecting and computing gutter line markers:
 * - Implemented method ((I) ↑)
 * - Implemented in method ((I) ↓)
 * - Interface / Class declaration markers
 * - Inferred annotations (@)
 * - Spring Bean / Autowired dependencies (↙)
 *
 * Scans project source roots dynamically without hardcoding.
 */
public class GutterMarkerService {

    private static final Set<String> SPRING_BEAN_ANNOTATIONS = Set.of(
            "Service", "Component", "Repository", "RestController", "Controller", "Configuration"
    );

    private static final Set<String> PRIMITIVES = Set.of(
            "int", "long", "boolean", "double", "float", "char", "byte", "short", "void"
    );

    public record TypeInfo(
            String name,
            String packageName,
            Path file,
            int line,
            boolean isInterface,
            String signature,
            List<String> extendedTypes,
            List<String> implementedTypes
    ) {}

    public record MethodTarget(int line, String signature) {}

    private final Path projectRoot;
    private final List<Path> sourceRoots = new ArrayList<>();
    private final JavaParser parser;

    // Project-wide dynamic indexes (simpleName -> TypeInfo)
    private final Map<String, TypeInfo> typeIndex = new ConcurrentHashMap<>();
    // interface simpleName -> list of implementing TypeInfos
    private final Map<String, List<TypeInfo>> interfaceImplementations = new ConcurrentHashMap<>();

    // Recent test outcomes (className#methodName -> boolean passed)
    private static final Map<String, Boolean> TEST_OUTCOME_CACHE = new ConcurrentHashMap<>();

    public static void recordTestOutcome(String className, String methodName, boolean passed) {
        if (className != null && methodName != null) {
            String simple = className.contains(".") ? className.substring(className.lastIndexOf('.') + 1) : className;
            TEST_OUTCOME_CACHE.put(simple + "#" + methodName, passed);
            TEST_OUTCOME_CACHE.put(className + "#" + methodName, passed);
        }
    }

    public static Boolean getTestOutcome(String className, String methodName) {
        if (className == null || methodName == null) return null;
        String simple = className.contains(".") ? className.substring(className.lastIndexOf('.') + 1) : className;
        Boolean b = TEST_OUTCOME_CACHE.get(simple + "#" + methodName);
        if (b != null) return b;
        return TEST_OUTCOME_CACHE.get(className + "#" + methodName);
    }

    public static void clearTestOutcomes() {
        TEST_OUTCOME_CACHE.clear();
    }

    public GutterMarkerService(Path projectRoot, List<Path> sourceRoots) {
        this.projectRoot = projectRoot;
        if (sourceRoots != null) {
            this.sourceRoots.addAll(sourceRoots);
        } else if (projectRoot != null) {
            Path src = projectRoot.resolve("src");
            if (Files.isDirectory(src)) {
                this.sourceRoots.add(src);
            } else {
                this.sourceRoots.add(projectRoot);
            }
        }

        ParserConfiguration config = new ParserConfiguration();
        config.setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21);
        this.parser = new JavaParser(config);

        reindexProject();
    }

    /** Re-indexes all project Java files in background to build type hierarchy. */
    public void reindexProject() {
        if (projectRoot == null && sourceRoots.isEmpty()) return;
        typeIndex.clear();
        interfaceImplementations.clear();

        List<Path> javaFiles = new ArrayList<>();
        for (Path root : sourceRoots) {
            if (!Files.isDirectory(root)) continue;
            try (Stream<Path> s = Files.walk(root)) {
                s.filter(p -> p.toString().endsWith(".java")).forEach(javaFiles::add);
            } catch (IOException ignored) {}
        }

        for (Path file : javaFiles) {
            try {
                indexSingleFileFast(file);
            } catch (Throwable ignored) {}
        }
    }

    private void indexSingleFileFast(Path file) {
        try {
            String content = Files.readString(file);
            indexContent(file, content);
        } catch (Throwable ignored) {}
    }

    public void indexContent(Path file, String content) {
        ParseResult<CompilationUnit> result = parser.parse(content);
        if (!result.isSuccessful() || result.getResult().isEmpty()) return;
        CompilationUnit cu = result.getResult().get();

        String pkg = cu.getPackageDeclaration()
                .map(pd -> pd.getName().asString())
                .orElse("");

        for (ClassOrInterfaceDeclaration decl : cu.findAll(ClassOrInterfaceDeclaration.class)) {
            String name = decl.getNameAsString();
            int line = decl.getName().getBegin().map(p -> p.line).orElseGet(() -> decl.getBegin().map(p -> p.line).orElse(1));
            boolean isIface = decl.isInterface();
            String sig = (isIface ? "interface " : "class ") + name;

            List<String> ext = new ArrayList<>();
            for (ClassOrInterfaceType t : decl.getExtendedTypes()) {
                ext.add(t.getNameAsString());
            }

            List<String> impl = new ArrayList<>();
            for (ClassOrInterfaceType t : decl.getImplementedTypes()) {
                impl.add(t.getNameAsString());
            }

            TypeInfo ti = new TypeInfo(name, pkg, file, line, isIface, sig, ext, impl);
            typeIndex.put(name, ti);

            for (String ifaceName : impl) {
                interfaceImplementations.computeIfAbsent(ifaceName, k -> new ArrayList<>()).add(ti);
            }
        }
    }

    /** Analyzes an open Java file and produces all gutter markers for that file. */
    public Map<Integer, List<GutterMarker>> analyzeFile(Path file, String content) {
        Map<Integer, List<GutterMarker>> lineMarkers = new HashMap<>();
        if (content == null || content.isBlank()) return lineMarkers;

        ParseResult<CompilationUnit> result = parser.parse(content);
        if (!result.isSuccessful() || result.getResult().isEmpty()) return lineMarkers;
        CompilationUnit cu = result.getResult().get();

        for (ClassOrInterfaceDeclaration decl : cu.findAll(ClassOrInterfaceDeclaration.class)) {
            int declLine = decl.getName().getBegin().map(p -> p.line).orElseGet(() -> decl.getBegin().map(p -> p.line).orElse(1));

            if (decl.isInterface()) {
                analyzeInterface(decl, declLine, lineMarkers);
            } else {
                analyzeClass(decl, declLine, lineMarkers);
            }
        }

        return lineMarkers;
    }

    private void analyzeInterface(ClassOrInterfaceDeclaration decl, int declLine, Map<Integer, List<GutterMarker>> lineMarkers) {
        String ifaceName = decl.getNameAsString();
        List<TypeInfo> impls = interfaceImplementations.getOrDefault(ifaceName, Collections.emptyList());

        if (!impls.isEmpty()) {
            List<GutterMarker.NavigationTarget> targets = new ArrayList<>();
            for (TypeInfo ti : impls) {
                targets.add(new GutterMarker.NavigationTarget(
                        ti.file, ti.line, ti.name, ti.packageName, ti.signature, ti.name + " (" + ti.packageName + ")"
                ));
            }
            String title = impls.size() == 1
                    ? "Is implemented by " + impls.get(0).name + " (" + impls.get(0).packageName + ")"
                    : "Has implementations in " + impls.size() + " classes";
            addMarker(lineMarkers, declLine, new GutterMarker(
                    declLine, GutterMarker.MarkerType.IMPLEMENTED_INTERFACE, title, "Press Ctrl+Alt+B to navigate", null, targets
            ));
        }

        // Methods in interface
        for (MethodDeclaration m : decl.getMethods()) {
            int mLine = m.getName().getBegin().map(p -> p.line).orElseGet(() -> m.getBegin().map(p -> p.line).orElse(-1));
            if (mLine == -1) continue;

            List<GutterMarker.NavigationTarget> methodTargets = new ArrayList<>();
            for (TypeInfo ti : impls) {
                MethodTarget mt = findMatchingMethodInFile(ti.file, m.getNameAsString(), m.getParameters().size());
                if (mt != null) {
                    methodTargets.add(new GutterMarker.NavigationTarget(
                            ti.file, mt.line, ti.name, ti.packageName, mt.signature, ti.name + " (" + ti.packageName + ")"
                    ));
                }
            }

            if (!methodTargets.isEmpty()) {
                String title = methodTargets.size() == 1
                        ? "Is implemented in " + methodTargets.get(0).targetName() + " (" + methodTargets.get(0).packageName() + ")"
                        : "Has implementations in " + methodTargets.size() + " classes";
                addMarker(lineMarkers, mLine, new GutterMarker(
                        mLine, GutterMarker.MarkerType.IMPLEMENTED_METHOD, title, "Press Ctrl+Alt+B to navigate", null, methodTargets
                ));
            }
        }
    }

    private void analyzeClass(ClassOrInterfaceDeclaration decl, int declLine, Map<Integer, List<GutterMarker>> lineMarkers) {
        // 1. Class implements interface(s)
        List<GutterMarker.NavigationTarget> classTargets = new ArrayList<>();
        for (ClassOrInterfaceType ifaceType : decl.getImplementedTypes()) {
            TypeInfo ti = typeIndex.get(ifaceType.getNameAsString());
            if (ti != null) {
                classTargets.add(new GutterMarker.NavigationTarget(
                        ti.file, ti.line, ti.name, ti.packageName, ti.signature, ti.name + " (" + ti.packageName + ")"
                ));
            }
        }
        if (!classTargets.isEmpty()) {
            String title = "Implements " + classTargets.get(0).targetName() + " (" + classTargets.get(0).packageName() + ")";
            addMarker(lineMarkers, declLine, new GutterMarker(
                    declLine, GutterMarker.MarkerType.IMPLEMENTS_INTERFACE, title, "Press Ctrl+U to navigate", null, classTargets
            ));
        }

        // 2. Methods
        for (MethodDeclaration m : decl.getMethods()) {
            int mLine = m.getName().getBegin().map(p -> p.line).orElseGet(() -> m.getBegin().map(p -> p.line).orElse(-1));
            if (mLine == -1) continue;

            boolean isOverride = m.isAnnotationPresent("Override");
            List<GutterMarker.NavigationTarget> superTargets = new ArrayList<>();
            String superTypeName = null;
            String superPkg = null;
            boolean isInterfaceMethod = false;

            // Check implemented interfaces
            for (ClassOrInterfaceType ifaceType : decl.getImplementedTypes()) {
                TypeInfo ifaceInfo = typeIndex.get(ifaceType.getNameAsString());
                if (ifaceInfo != null) {
                    MethodTarget mt = findMatchingMethodInFile(ifaceInfo.file, m.getNameAsString(), m.getParameters().size());
                    if (mt != null) {
                        superTargets.add(new GutterMarker.NavigationTarget(
                                ifaceInfo.file, mt.line, ifaceInfo.name, ifaceInfo.packageName, mt.signature, ifaceInfo.name + " (" + ifaceInfo.packageName + ")"
                        ));
                        superTypeName = ifaceInfo.name;
                        superPkg = ifaceInfo.packageName;
                        isInterfaceMethod = true;
                        break;
                    }
                }
            }

            // Check extended superclass if no interface match found
            if (superTargets.isEmpty() && !decl.getExtendedTypes().isEmpty()) {
                for (ClassOrInterfaceType superType : decl.getExtendedTypes()) {
                    TypeInfo superInfo = typeIndex.get(superType.getNameAsString());
                    if (superInfo != null) {
                        MethodTarget mt = findMatchingMethodInFile(superInfo.file, m.getNameAsString(), m.getParameters().size());
                        if (mt != null) {
                            superTargets.add(new GutterMarker.NavigationTarget(
                                    superInfo.file, mt.line, superInfo.name, superInfo.packageName, mt.signature, superInfo.name + " (" + superInfo.packageName + ")"
                            ));
                            superTypeName = superInfo.name;
                            superPkg = superInfo.packageName;
                            break;
                        }
                    }
                }
            }

            if (!superTargets.isEmpty()) {
                String title = isInterfaceMethod
                        ? "Implements method in " + superTypeName + " (" + superPkg + ")"
                        : "Overrides method in " + superTypeName + " (" + superPkg + ")";
                GutterMarker.MarkerType mType = isInterfaceMethod
                        ? GutterMarker.MarkerType.IMPLEMENTS_METHOD
                        : GutterMarker.MarkerType.OVERRIDES_METHOD;
                addMarker(lineMarkers, mLine, new GutterMarker(
                        mLine, mType, title, "Press Ctrl+U to navigate", null, superTargets
                ));
            }

            // Inferred annotations marker (@):
            // Displayed on methods having annotations (like @Transactional, @Override, etc.)
            if (!m.getAnnotations().isEmpty() || isOverride) {
                StringBuilder sigPreview = new StringBuilder();
                for (var ann : m.getAnnotations()) {
                    if (ann.getNameAsString().equals("Override")) continue;
                    sigPreview.append(ann.toString()).append("\n");
                }
                sigPreview.append(m.getTypeAsString()).append(" ").append(m.getNameAsString()).append("(");
                var params = m.getParameters();
                for (int p = 0; p < params.size(); p++) {
                    if (p > 0) sigPreview.append(", ");
                    String pType = params.get(p).getTypeAsString();
                    if (!PRIMITIVES.contains(pType)) {
                        sigPreview.append("@NotNull ");
                    }
                    sigPreview.append(pType).append(" ").append(params.get(p).getNameAsString());
                }
                sigPreview.append(")");

                String title = "Inferred annotations available. Full signature:";
                addMarker(lineMarkers, mLine, new GutterMarker(
                        mLine, GutterMarker.MarkerType.INFERRED_ANNOTATION, title, null, sigPreview.toString(), Collections.emptyList()
                ));
            }
        }

        // 3. Fields (Spring Bean Dependency Injection)
        boolean isSpringBean = isSpringBean(decl);
        boolean hasRequiredArgs = decl.isAnnotationPresent("RequiredArgsConstructor") || decl.isAnnotationPresent("AllArgsConstructor");
        for (FieldDeclaration f : decl.getFields()) {
            int fLine = f.getVariables().isEmpty() ? f.getBegin().map(p -> p.line).orElse(-1)
                    : f.getVariable(0).getName().getBegin().map(p -> p.line).orElseGet(() -> f.getBegin().map(p -> p.line).orElse(-1));
            if (fLine == -1) continue;
            boolean isAutowired = f.isAnnotationPresent("Autowired") || f.isAnnotationPresent("Inject");
            boolean isFinal = f.isFinal();

            if (isAutowired || (isSpringBean && hasRequiredArgs && isFinal)) {
                for (VariableDeclarator var : f.getVariables()) {
                    String fieldType = var.getTypeAsString();
                    TypeInfo ti = typeIndex.get(fieldType);
                    List<GutterMarker.NavigationTarget> targets = ti != null
                            ? List.of(new GutterMarker.NavigationTarget(ti.file, ti.line, ti.name, ti.packageName, ti.signature, ti.name))
                            : Collections.emptyList();
                    String title = "Navigate to autowired dependencies for '" + var.getNameAsString() + "'";
                    addMarker(lineMarkers, fLine, new GutterMarker(
                            fLine, GutterMarker.MarkerType.BEAN_INJECTION, title, null, null, targets
                    ));
                }
            }
        }

        // 4. Test Class and Test Method run markers
        boolean isTestClass = decl.getNameAsString().endsWith("Test") || decl.getNameAsString().endsWith("Tests") || decl.getNameAsString().endsWith("TestCase");
        boolean hasTestMethods = false;

        for (MethodDeclaration m : decl.getMethods()) {
            boolean isTest = isTestMethod(m, isTestClass);
            if (isTest) {
                hasTestMethods = true;
                int mLine = m.getName().getBegin().map(p -> p.line).orElseGet(() -> m.getBegin().map(p -> p.line).orElse(-1));
                if (mLine != -1) {
                    Boolean passed = getTestOutcome(decl.getNameAsString(), m.getNameAsString());
                    GutterMarker.MarkerType markerType = passed == null
                            ? GutterMarker.MarkerType.TEST_METHOD
                            : (passed ? GutterMarker.MarkerType.TEST_METHOD_PASSED : GutterMarker.MarkerType.TEST_METHOD_FAILED);
                    String title = "Run '" + decl.getNameAsString() + "." + m.getNameAsString() + "()'";
                    List<GutterMarker.NavigationTarget> targets = List.of(new GutterMarker.NavigationTarget(
                            null, mLine, m.getNameAsString(), null, decl.getNameAsString(), "Test method"
                    ));
                    addMarker(lineMarkers, mLine, new GutterMarker(
                            mLine, markerType, title, "Click to run or debug test", null, targets
                    ));
                }
            }
        }

        if (isTestClass || hasTestMethods) {
            String title = "Run '" + decl.getNameAsString() + "'";
            List<GutterMarker.NavigationTarget> targets = List.of(new GutterMarker.NavigationTarget(
                    null, declLine, decl.getNameAsString(), null, decl.getNameAsString(), "Test class"
            ));
            addMarker(lineMarkers, declLine, new GutterMarker(
                    declLine, GutterMarker.MarkerType.TEST_CLASS, title, "Click to run or debug all tests", null, targets
            ));
        }
    }

    private static boolean isTestMethod(MethodDeclaration m, boolean inTestClass) {
        for (var ann : m.getAnnotations()) {
            String name = ann.getNameAsString();
            if ("Test".equals(name) || "ParameterizedTest".equals(name) || "RepeatedTest".equals(name)
                    || "TestFactory".equals(name) || "TestTemplate".equals(name)) {
                return true;
            }
        }
        return inTestClass && m.getNameAsString().startsWith("test") && m.getTypeAsString().equals("void");
    }

    private boolean isSpringBean(ClassOrInterfaceDeclaration decl) {
        for (var ann : decl.getAnnotations()) {
            if (SPRING_BEAN_ANNOTATIONS.contains(ann.getNameAsString())) {
                return true;
            }
        }
        return false;
    }

    private MethodTarget findMatchingMethodInFile(Path file, String methodName, int paramCount) {
        if (file == null || !Files.exists(file)) return null;
        try {
            String content = Files.readString(file);
            ParseResult<CompilationUnit> res = parser.parse(content);
            if (!res.isSuccessful() || res.getResult().isEmpty()) return null;
            CompilationUnit cu = res.getResult().get();

            MethodDeclaration bestMatch = null;
            for (MethodDeclaration m : cu.findAll(MethodDeclaration.class)) {
                if (m.getNameAsString().equals(methodName)) {
                    if (m.getParameters().size() == paramCount) {
                        bestMatch = m;
                        break;
                    }
                    if (bestMatch == null) {
                        bestMatch = m;
                    }
                }
            }

            if (bestMatch != null) {
                final MethodDeclaration match = bestMatch;
                int line = match.getName().getBegin().map(p -> p.line).orElseGet(() -> match.getBegin().map(p -> p.line).orElse(1));
                String sig = match.getDeclarationAsString(false, false, false);
                return new MethodTarget(line, sig);
            }
        } catch (Throwable ignored) {}
        return null;
    }

    private static void addMarker(Map<Integer, List<GutterMarker>> lineMarkers, int line, GutterMarker marker) {
        lineMarkers.computeIfAbsent(line, k -> new ArrayList<>()).add(marker);
    }
}
