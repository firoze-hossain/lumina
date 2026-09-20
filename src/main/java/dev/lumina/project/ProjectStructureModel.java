package dev.lumina.project;

import com.google.gson.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Data model and dynamic detection/persistence service for Project Structure,
 * matching IntelliJ IDEA's Project Structure dialog.
 */
public final class ProjectStructureModel {

    public enum FolderType {
        SOURCE("Source Folders", "#548AF7", "📁"),
        TEST_SOURCE("Test Source Folders", "#59A869", "📁"),
        RESOURCE("Resource Folders", "#9876AA", "📁"),
        TEST_RESOURCE("Test Resource Folders", "#BBB529", "📁"),
        EXCLUDED("Excluded Folders", "#ED5565", "📁");

        private final String displayLabel;
        private final String hexColor;
        private final String glyph;

        FolderType(String displayLabel, String hexColor, String glyph) {
            this.displayLabel = displayLabel;
            this.hexColor = hexColor;
            this.glyph = glyph;
        }

        public String displayLabel() { return displayLabel; }
        public String hexColor() { return hexColor; }
        public String glyph() { return glyph; }
    }

    public static class DependencyItem {
        private String name;
        private String scope;
        private boolean isModule;

        public DependencyItem(String name, String scope, boolean isModule) {
            this.name = name;
            this.scope = scope;
            this.isModule = isModule;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getScope() { return scope; }
        public void setScope(String scope) { this.scope = scope; }
        public boolean isModule() { return isModule; }
        public void setModule(boolean module) { isModule = module; }

        @Override
        public String toString() { return name + " (" + scope + ")"; }
    }

    public static class FacetModel {
        private String name;
        private String type;
        private Map<String, String> configuration = new LinkedHashMap<>();

        public FacetModel(String name, String type) {
            this.name = name;
            this.type = type;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public Map<String, String> getConfiguration() { return configuration; }
    }

    public static class LibraryModel {
        private String name;
        private List<String> classesPaths = new ArrayList<>();
        private List<String> sourcesPaths = new ArrayList<>();
        private List<String> javadocPaths = new ArrayList<>();

        public LibraryModel(String name) {
            this.name = name;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public List<String> getClassesPaths() { return classesPaths; }
        public List<String> getSourcesPaths() { return sourcesPaths; }
        public List<String> getJavadocPaths() { return javadocPaths; }
    }

    public static class ArtifactModel {
        private String name;
        private String type;
        private String outputPath;

        public ArtifactModel(String name, String type, String outputPath) {
            this.name = name;
            this.type = type;
            this.outputPath = outputPath;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getOutputPath() { return outputPath; }
        public void setOutputPath(String outputPath) { this.outputPath = outputPath; }
    }

    public static class ModuleModel {
        private String name;
        private String type = "JAVA_MODULE";
        private Path contentRoot;
        private ProjectSdk.SdkItem moduleSdk;
        private String languageLevel = "Project default";
        private final Set<String> sourceFolders = new LinkedHashSet<>();
        private final Set<String> testSourceFolders = new LinkedHashSet<>();
        private final Set<String> resourceFolders = new LinkedHashSet<>();
        private final Set<String> testResourceFolders = new LinkedHashSet<>();
        private final Set<String> excludedFolders = new LinkedHashSet<>();
        private String excludePatterns = "*.iml;*.hprof;*.pyc;__pycache__";
        private boolean inheritCompilerOutput = true;
        private String outputPath = "";
        private String testOutputPath = "";
        private final List<DependencyItem> dependencies = new ArrayList<>();

        public ModuleModel(String name, Path contentRoot) {
            this.name = name;
            this.contentRoot = contentRoot;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public Path getContentRoot() { return contentRoot; }
        public void setContentRoot(Path contentRoot) { this.contentRoot = contentRoot; }
        public ProjectSdk.SdkItem getModuleSdk() { return moduleSdk; }
        public void setModuleSdk(ProjectSdk.SdkItem moduleSdk) { this.moduleSdk = moduleSdk; }
        public String getLanguageLevel() { return languageLevel; }
        public void setLanguageLevel(String languageLevel) { this.languageLevel = languageLevel; }

        public Set<String> getSourceFolders() { return sourceFolders; }
        public Set<String> getTestSourceFolders() { return testSourceFolders; }
        public Set<String> getResourceFolders() { return resourceFolders; }
        public Set<String> getTestResourceFolders() { return testResourceFolders; }
        public Set<String> getExcludedFolders() { return excludedFolders; }

        public String getExcludePatterns() { return excludePatterns; }
        public void setExcludePatterns(String excludePatterns) { this.excludePatterns = excludePatterns; }
        public boolean isInheritCompilerOutput() { return inheritCompilerOutput; }
        public void setInheritCompilerOutput(boolean inheritCompilerOutput) { this.inheritCompilerOutput = inheritCompilerOutput; }
        public String getOutputPath() { return outputPath; }
        public void setOutputPath(String outputPath) { this.outputPath = outputPath; }
        public String getTestOutputPath() { return testOutputPath; }
        public void setTestOutputPath(String testOutputPath) { this.testOutputPath = testOutputPath; }
        public List<DependencyItem> getDependencies() { return dependencies; }

        public void markFolder(String relativePath, FolderType targetType) {
            String norm = normalizePath(relativePath);
            sourceFolders.remove(norm);
            testSourceFolders.remove(norm);
            resourceFolders.remove(norm);
            testResourceFolders.remove(norm);
            excludedFolders.remove(norm);

            if (targetType != null) {
                switch (targetType) {
                    case SOURCE -> sourceFolders.add(norm);
                    case TEST_SOURCE -> testSourceFolders.add(norm);
                    case RESOURCE -> resourceFolders.add(norm);
                    case TEST_RESOURCE -> testResourceFolders.add(norm);
                    case EXCLUDED -> excludedFolders.add(norm);
                }
            }
        }

        public void unmarkFolder(String relativePath) {
            markFolder(relativePath, null);
        }

        public FolderType getFolderType(String relativePath) {
            String norm = normalizePath(relativePath);
            if (sourceFolders.contains(norm)) return FolderType.SOURCE;
            if (testSourceFolders.contains(norm)) return FolderType.TEST_SOURCE;
            if (resourceFolders.contains(norm)) return FolderType.RESOURCE;
            if (testResourceFolders.contains(norm)) return FolderType.TEST_RESOURCE;
            if (excludedFolders.contains(norm)) return FolderType.EXCLUDED;
            return null;
        }

        private String normalizePath(String path) {
            if (path == null) return "";
            String s = path.trim().replace('\\', '/');
            while (s.startsWith("/")) s = s.substring(1);
            while (s.endsWith("/")) s = s.substring(0, s.length() - 1);
            return s;
        }
    }

    // Project structure fields
    private String projectName;
    private Path projectRoot;
    private ProjectSdk.SdkItem projectSdk;
    private String languageLevel = "SDK default";
    private String compilerOutput = "";
    private final List<ModuleModel> modules = new ArrayList<>();
    private final List<LibraryModel> libraries = new ArrayList<>();
    private final List<FacetModel> facets = new ArrayList<>();
    private final List<ArtifactModel> artifacts = new ArrayList<>();

    public ProjectStructureModel(String projectName, Path projectRoot) {
        this.projectName = projectName;
        this.projectRoot = projectRoot;
    }

    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }
    public Path getProjectRoot() { return projectRoot; }
    public void setProjectRoot(Path projectRoot) { this.projectRoot = projectRoot; }
    public ProjectSdk.SdkItem getProjectSdk() { return projectSdk; }
    public void setProjectSdk(ProjectSdk.SdkItem projectSdk) { this.projectSdk = projectSdk; }
    public String getLanguageLevel() { return languageLevel; }
    public void setLanguageLevel(String languageLevel) { this.languageLevel = languageLevel; }
    public String getCompilerOutput() { return compilerOutput; }
    public void setCompilerOutput(String compilerOutput) { this.compilerOutput = compilerOutput; }
    public List<ModuleModel> getModules() { return modules; }
    public List<LibraryModel> getLibraries() { return libraries; }
    public List<FacetModel> getFacets() { return facets; }
    public List<ArtifactModel> getArtifacts() { return artifacts; }

    public ModuleModel getPrimaryModule() {
        if (!modules.isEmpty()) return modules.getFirst();
        ModuleModel m = new ModuleModel(projectName != null ? projectName : "untitled", projectRoot);
        modules.add(m);
        return m;
    }

    // ------------------------------------------------------------- Service

    public static final class Service {

        public static ProjectStructureModel load(Path root) {
            String name = root != null && root.getFileName() != null
                    ? root.getFileName().toString() : "untitled";
            ProjectStructureModel model = new ProjectStructureModel(name, root);
            List<ProjectSdk.SdkItem> allSdks = ProjectSdk.discoverAllSdks();

            ModuleModel primary = new ModuleModel(name, root);
            model.getModules().add(primary);

            if (root == null || !Files.isDirectory(root)) {
                return model;
            }

            boolean isMaven = Files.isRegularFile(root.resolve("pom.xml"));
            boolean isGradle = Files.isRegularFile(root.resolve("build.gradle"))
                    || Files.isRegularFile(root.resolve("build.gradle.kts"));
            boolean isPython = hasPythonFiles(root);
            boolean isPhp = Files.isRegularFile(root.resolve("composer.json")) || hasPhpFiles(root);
            boolean isNode = Files.isRegularFile(root.resolve("package.json"));

            // Default compiler output
            model.setCompilerOutput(root.resolve("out/production/" + name).toString());

            if (isMaven) {
                inspectMaven(root, model, primary, allSdks);
            } else if (isGradle) {
                inspectGradle(root, model, primary, allSdks);
            } else if (isPython) {
                inspectPython(root, model, primary, allSdks);
            } else if (isPhp) {
                inspectPhp(root, model, primary, allSdks);
            } else if (isNode) {
                inspectNode(root, model, primary, allSdks);
            } else {
                inspectGeneric(root, model, primary, allSdks);
            }

            // Load saved .idea/misc.xml and .idea/modules.xml if present
            readIdeaMetadata(root, model, primary, allSdks);

            // Load saved .lumina/project-structure.json if present
            readLuminaMetadata(root, model, primary, allSdks);

            // Setup default artifact
            if (model.getArtifacts().isEmpty() && (isMaven || isGradle || "JAVA_MODULE".equals(primary.getType()))) {
                model.getArtifacts().add(new ArtifactModel(name + ":jar", "JAR", root.resolve("out/artifacts/" + name + "_jar").toString()));
            }

            return model;
        }

        private static boolean hasPythonFiles(Path root) {
            if (Files.isRegularFile(root.resolve("requirements.txt"))
                    || Files.isRegularFile(root.resolve("pyproject.toml"))
                    || Files.isRegularFile(root.resolve("setup.py"))
                    || Files.isRegularFile(root.resolve("Pipfile"))) {
                return true;
            }
            try (var s = Files.list(root)) {
                return s.anyMatch(p -> p.toString().endsWith(".py"));
            } catch (Exception e) {
                return false;
            }
        }

        private static boolean hasPhpFiles(Path root) {
            try (var s = Files.list(root)) {
                return s.anyMatch(p -> p.toString().endsWith(".php"));
            } catch (Exception e) {
                return false;
            }
        }

        private static void inspectMaven(Path root, ProjectStructureModel model, ModuleModel module, List<ProjectSdk.SdkItem> allSdks) {
            module.setType("JAVA_MODULE");
            model.setProjectSdk(findFirstSdkOfType(allSdks, ProjectSdk.SdkType.JDK));
            model.setCompilerOutput(root.resolve("target/classes").toString());
            module.setOutputPath(root.resolve("target/classes").toString());
            module.setTestOutputPath(root.resolve("target/test-classes").toString());

            addFolderIfExists(root, module, "src/main/java", FolderType.SOURCE);
            addFolderIfExists(root, module, "src/main/kotlin", FolderType.SOURCE);
            addFolderIfExists(root, module, "src/main/resources", FolderType.RESOURCE);
            addFolderIfExists(root, module, "src/test/java", FolderType.TEST_SOURCE);
            addFolderIfExists(root, module, "src/test/kotlin", FolderType.TEST_SOURCE);
            addFolderIfExists(root, module, "src/test/resources", FolderType.TEST_RESOURCE);
            addFolderIfExists(root, module, "target", FolderType.EXCLUDED);
            addFolderIfExists(root, module, ".idea", FolderType.EXCLUDED);

            try {
                String pom = Files.readString(root.resolve("pom.xml"));
                Matcher artMatch = Pattern.compile("<artifactId>([^<]+)</artifactId>").matcher(pom);
                if (artMatch.find()) {
                    String a = artMatch.group(1).trim();
                    model.setProjectName(a);
                    module.setName(a);
                }
                Matcher sourceMatch = Pattern.compile("<maven\\.compiler\\.source>([^<]+)</maven\\.compiler\\.source>").matcher(pom);
                if (sourceMatch.find()) {
                    model.setLanguageLevel(sourceMatch.group(1).trim());
                    module.setLanguageLevel(sourceMatch.group(1).trim());
                } else {
                    Matcher relMatch = Pattern.compile("<maven\\.compiler\\.release>([^<]+)</maven\\.compiler\\.release>").matcher(pom);
                    if (relMatch.find()) {
                        model.setLanguageLevel(relMatch.group(1).trim());
                        module.setLanguageLevel(relMatch.group(1).trim());
                    }
                }

                // Dependencies
                Matcher depMatch = Pattern.compile("<dependency>([\\s\\S]*?)</dependency>").matcher(pom);
                while (depMatch.find()) {
                    String block = depMatch.group(1);
                    String g = extractTag(block, "groupId");
                    String a = extractTag(block, "artifactId");
                    String v = extractTag(block, "version");
                    String s = extractTag(block, "scope");
                    if (s == null || s.isBlank()) s = "Compile";
                    else s = Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase();

                    String label = (g != null && !g.isBlank() ? g + ":" : "") + (a != null ? a : "dep") + (v != null && !v.isBlank() ? ":" + v : "");
                    module.getDependencies().add(new DependencyItem(label, s, false));
                    model.getLibraries().add(new LibraryModel("Maven: " + label));

                    if ("spring-boot-starter".equals(a) || (g != null && g.contains("springframework.boot"))) {
                        if (model.getFacets().stream().noneMatch(f -> "Spring Boot".equals(f.getName()))) {
                            model.getFacets().add(new FacetModel("Spring Boot", "Spring"));
                        }
                    }
                    if ("javafx-controls".equals(a) || (g != null && g.contains("openjfx"))) {
                        if (model.getFacets().stream().noneMatch(f -> "JavaFX".equals(f.getName()))) {
                            model.getFacets().add(new FacetModel("JavaFX", "JavaFX"));
                        }
                    }
                    if ("kotlin-stdlib".equals(a) || (g != null && g.contains("jetbrains.kotlin"))) {
                        if (model.getFacets().stream().noneMatch(f -> "Kotlin".equals(f.getName()))) {
                            model.getFacets().add(new FacetModel("Kotlin", "Kotlin"));
                        }
                    }
                }
            } catch (Exception ignored) {}
        }

        private static void inspectGradle(Path root, ProjectStructureModel model, ModuleModel module, List<ProjectSdk.SdkItem> allSdks) {
            module.setType("JAVA_MODULE");
            model.setProjectSdk(findFirstSdkOfType(allSdks, ProjectSdk.SdkType.JDK));
            model.setCompilerOutput(root.resolve("build/classes/java/main").toString());
            module.setOutputPath(root.resolve("build/classes/java/main").toString());
            module.setTestOutputPath(root.resolve("build/classes/java/test").toString());

            addFolderIfExists(root, module, "src/main/java", FolderType.SOURCE);
            addFolderIfExists(root, module, "src/main/kotlin", FolderType.SOURCE);
            addFolderIfExists(root, module, "src/main/resources", FolderType.RESOURCE);
            addFolderIfExists(root, module, "src/test/java", FolderType.TEST_SOURCE);
            addFolderIfExists(root, module, "src/test/kotlin", FolderType.TEST_SOURCE);
            addFolderIfExists(root, module, "src/test/resources", FolderType.TEST_RESOURCE);
            addFolderIfExists(root, module, "build", FolderType.EXCLUDED);
            addFolderIfExists(root, module, ".gradle", FolderType.EXCLUDED);
            addFolderIfExists(root, module, ".idea", FolderType.EXCLUDED);

            // Read settings.gradle or build.gradle
            Path settings = root.resolve("settings.gradle");
            if (!Files.isRegularFile(settings)) settings = root.resolve("settings.gradle.kts");
            if (Files.isRegularFile(settings)) {
                try {
                    String content = Files.readString(settings);
                    Matcher m = Pattern.compile("rootProject\\.name\\s*=\\s*['\"]([^'\"]+)['\"]").matcher(content);
                    if (m.find()) {
                        model.setProjectName(m.group(1));
                        module.setName(m.group(1));
                    }
                } catch (Exception ignored) {}
            }

            Path buildGradle = root.resolve("build.gradle");
            if (!Files.isRegularFile(buildGradle)) buildGradle = root.resolve("build.gradle.kts");
            if (Files.isRegularFile(buildGradle)) {
                try {
                    String content = Files.readString(buildGradle);
                    Matcher toolchain = Pattern.compile("(?:languageVersion\\.set\\(JavaLanguageVersion\\.of\\((\\d+)\\)\\)|jvmToolchain\\((\\d+)\\))").matcher(content);
                    if (toolchain.find()) {
                        String ver = toolchain.group(1) != null ? toolchain.group(1) : toolchain.group(2);
                        model.setLanguageLevel(ver);
                        module.setLanguageLevel(ver);
                    }
                    if (content.contains("org.springframework.boot")) {
                        model.getFacets().add(new FacetModel("Spring Boot", "Spring"));
                    }
                    if (content.contains("javafx")) {
                        model.getFacets().add(new FacetModel("JavaFX", "JavaFX"));
                    }
                    if (content.contains("kotlin(\"jvm\")") || content.contains("org.jetbrains.kotlin")) {
                        model.getFacets().add(new FacetModel("Kotlin", "Kotlin"));
                    }
                } catch (Exception ignored) {}
            }
        }

        private static void inspectPython(Path root, ProjectStructureModel model, ModuleModel module, List<ProjectSdk.SdkItem> allSdks) {
            module.setType("PYTHON_MODULE");
            model.setProjectSdk(findFirstSdkOfType(allSdks, ProjectSdk.SdkType.PYTHON));
            model.setLanguageLevel("Python 3.12");
            module.setLanguageLevel("Python 3.12");

            addFolderIfExists(root, module, "src", FolderType.SOURCE);
            addFolderIfExists(root, module, "tests", FolderType.TEST_SOURCE);
            addFolderIfExists(root, module, "test", FolderType.TEST_SOURCE);
            addFolderIfExists(root, module, "venv", FolderType.EXCLUDED);
            addFolderIfExists(root, module, ".venv", FolderType.EXCLUDED);
            addFolderIfExists(root, module, "env", FolderType.EXCLUDED);
            addFolderIfExists(root, module, "__pycache__", FolderType.EXCLUDED);
            addFolderIfExists(root, module, ".pytest_cache", FolderType.EXCLUDED);
            addFolderIfExists(root, module, ".idea", FolderType.EXCLUDED);

            model.getFacets().add(new FacetModel("Python", "Python"));

            Path reqs = root.resolve("requirements.txt");
            if (Files.isRegularFile(reqs)) {
                try {
                    for (String line : Files.readAllLines(reqs)) {
                        String tr = line.trim();
                        if (!tr.isBlank() && !tr.startsWith("#")) {
                            module.getDependencies().add(new DependencyItem(tr, "Compile", false));
                        }
                    }
                } catch (Exception ignored) {}
            }
        }

        private static void inspectPhp(Path root, ProjectStructureModel model, ModuleModel module, List<ProjectSdk.SdkItem> allSdks) {
            module.setType("PHP_MODULE");
            model.setProjectSdk(findFirstSdkOfType(allSdks, ProjectSdk.SdkType.PHP));
            model.setLanguageLevel("8.3");
            module.setLanguageLevel("8.3");

            addFolderIfExists(root, module, "src", FolderType.SOURCE);
            addFolderIfExists(root, module, "app", FolderType.SOURCE);
            addFolderIfExists(root, module, "tests", FolderType.TEST_SOURCE);
            addFolderIfExists(root, module, "vendor", FolderType.EXCLUDED);
            addFolderIfExists(root, module, ".idea", FolderType.EXCLUDED);

            model.getFacets().add(new FacetModel("PHP", "PHP"));

            Path comp = root.resolve("composer.json");
            if (Files.isRegularFile(comp)) {
                try {
                    String jsonStr = Files.readString(comp);
                    JsonObject obj = JsonParser.parseString(jsonStr).getAsJsonObject();
                    if (obj.has("require")) {
                        JsonObject req = obj.getAsJsonObject("require");
                        for (String k : req.keySet()) {
                            module.getDependencies().add(new DependencyItem(k + ": " + req.get(k).getAsString(), "Compile", false));
                        }
                    }
                } catch (Exception ignored) {}
            }
        }

        private static void inspectNode(Path root, ProjectStructureModel model, ModuleModel module, List<ProjectSdk.SdkItem> allSdks) {
            module.setType("WEB_MODULE");
            model.setProjectSdk(findFirstSdkOfType(allSdks, ProjectSdk.SdkType.NODE));

            addFolderIfExists(root, module, "src", FolderType.SOURCE);
            addFolderIfExists(root, module, "public", FolderType.RESOURCE);
            addFolderIfExists(root, module, "node_modules", FolderType.EXCLUDED);
            addFolderIfExists(root, module, "dist", FolderType.EXCLUDED);
            addFolderIfExists(root, module, "build", FolderType.EXCLUDED);
            addFolderIfExists(root, module, ".idea", FolderType.EXCLUDED);

            model.getFacets().add(new FacetModel("Node.js", "Web"));
        }

        private static void inspectGeneric(Path root, ProjectStructureModel model, ModuleModel module, List<ProjectSdk.SdkItem> allSdks) {
            module.setType("JAVA_MODULE");
            model.setProjectSdk(findFirstSdkOfType(allSdks, ProjectSdk.SdkType.JDK));

            addFolderIfExists(root, module, "src/main/java", FolderType.SOURCE);
            addFolderIfExists(root, module, "src/main/resources", FolderType.RESOURCE);
            addFolderIfExists(root, module, "src/test/java", FolderType.TEST_SOURCE);
            addFolderIfExists(root, module, "src", FolderType.SOURCE);
            addFolderIfExists(root, module, "target", FolderType.EXCLUDED);
            addFolderIfExists(root, module, "build", FolderType.EXCLUDED);
            addFolderIfExists(root, module, ".idea", FolderType.EXCLUDED);
        }

        private static void addFolderIfExists(Path root, ModuleModel module, String rel, FolderType type) {
            if (Files.isDirectory(root.resolve(rel))) {
                module.markFolder(rel, type);
            }
        }

        private static String extractTag(String xml, String tag) {
            Matcher m = Pattern.compile("<" + tag + ">([^<]+)</" + tag + ">").matcher(xml);
            return m.find() ? m.group(1).trim() : null;
        }

        private static ProjectSdk.SdkItem findFirstSdkOfType(List<ProjectSdk.SdkItem> sdks, ProjectSdk.SdkType type) {
            for (ProjectSdk.SdkItem item : sdks) {
                if (item.type() == type) return item;
            }
            return !sdks.isEmpty() ? sdks.getFirst() : null;
        }

        private static void readIdeaMetadata(Path root, ProjectStructureModel model, ModuleModel module, List<ProjectSdk.SdkItem> allSdks) {
            Path misc = root.resolve(".idea/misc.xml");
            if (Files.isRegularFile(misc)) {
                try {
                    String content = Files.readString(misc);
                    Matcher sdkMatch = Pattern.compile("project-jdk-name=[\"']([^\"']+)[\"']").matcher(content);
                    if (sdkMatch.find()) {
                        String sdkName = sdkMatch.group(1);
                        boolean found = false;
                        for (ProjectSdk.SdkItem item : allSdks) {
                            if (item.name().equals(sdkName)) {
                                model.setProjectSdk(item);
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            for (ProjectSdk.SdkItem item : allSdks) {
                                if (item.name().contains(sdkName)) {
                                    model.setProjectSdk(item);
                                    found = true;
                                    break;
                                }
                            }
                        }
                        if (!found && sdkName != null && !sdkName.isBlank() && !"<No SDK>".equals(sdkName)) {
                            model.setProjectSdk(new ProjectSdk.SdkItem(
                                    "sdk-" + sdkName.toLowerCase().replace(' ', '-'),
                                    sdkName,
                                    ProjectSdk.SdkType.JDK,
                                    "",
                                    "",
                                    false,
                                    false,
                                    List.of()
                            ));
                        }
                    }
                    Matcher langMatch = Pattern.compile("languageLevel=[\"']JDK_([^\"']+)[\"']").matcher(content);
                    if (langMatch.find()) {
                        String lvl = langMatch.group(1);
                        model.setLanguageLevel(lvl);
                        module.setLanguageLevel(lvl);
                    }
                    Matcher outMatch = Pattern.compile("<output url=[\"']([^\"']+)[\"']").matcher(content);
                    if (outMatch.find()) {
                        String out = outMatch.group(1).replace("$PROJECT_DIR$", root.toString()).replace("file://", "");
                        model.setCompilerOutput(out);
                    }
                } catch (Exception ignored) {}
            }
        }

        private static void readLuminaMetadata(Path root, ProjectStructureModel model, ModuleModel module, List<ProjectSdk.SdkItem> allSdks) {
            Path luminaFile = root.resolve(".lumina/project-structure.json");
            if (Files.isRegularFile(luminaFile)) {
                try {
                    String jsonStr = Files.readString(luminaFile);
                    JsonObject obj = JsonParser.parseString(jsonStr).getAsJsonObject();
                    if (obj.has("projectName")) model.setProjectName(obj.get("projectName").getAsString());
                    if (obj.has("languageLevel")) model.setLanguageLevel(obj.get("languageLevel").getAsString());
                    if (obj.has("compilerOutput")) model.setCompilerOutput(obj.get("compilerOutput").getAsString());
                    if (obj.has("sdkName")) {
                        String sdkName = obj.get("sdkName").getAsString();
                        boolean found = false;
                        for (ProjectSdk.SdkItem item : allSdks) {
                            if (item.name().equals(sdkName)) {
                                model.setProjectSdk(item);
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            for (ProjectSdk.SdkItem item : allSdks) {
                                if (item.name().contains(sdkName)) {
                                    model.setProjectSdk(item);
                                    found = true;
                                    break;
                                }
                            }
                        }
                        if (!found && sdkName != null && !sdkName.isBlank() && !"<No SDK>".equals(sdkName)) {
                            model.setProjectSdk(new ProjectSdk.SdkItem(
                                    "sdk-" + sdkName.toLowerCase().replace(' ', '-'),
                                    sdkName,
                                    ProjectSdk.SdkType.JDK,
                                    "",
                                    "",
                                    false,
                                    false,
                                    List.of()
                            ));
                        }
                    }
                    if (obj.has("sources")) {
                        for (JsonElement e : obj.getAsJsonArray("sources")) {
                            module.markFolder(e.getAsString(), FolderType.SOURCE);
                        }
                    }
                    if (obj.has("tests")) {
                        for (JsonElement e : obj.getAsJsonArray("tests")) {
                            module.markFolder(e.getAsString(), FolderType.TEST_SOURCE);
                        }
                    }
                    if (obj.has("resources")) {
                        for (JsonElement e : obj.getAsJsonArray("resources")) {
                            module.markFolder(e.getAsString(), FolderType.RESOURCE);
                        }
                    }
                    if (obj.has("testResources")) {
                        for (JsonElement e : obj.getAsJsonArray("testResources")) {
                            module.markFolder(e.getAsString(), FolderType.TEST_RESOURCE);
                        }
                    }
                    if (obj.has("excluded")) {
                        for (JsonElement e : obj.getAsJsonArray("excluded")) {
                            module.markFolder(e.getAsString(), FolderType.EXCLUDED);
                        }
                    }
                } catch (Exception ignored) {}
            }
        }

        public static void save(ProjectStructureModel model) {
            Path root = model.getProjectRoot();
            if (root == null) return;

            try {
                // 1. Save to .idea/misc.xml
                Path ideaDir = root.resolve(".idea");
                Files.createDirectories(ideaDir);

                String sdkName = model.getProjectSdk() != null ? model.getProjectSdk().name() : "";
                String sdkType = model.getProjectSdk() != null ? model.getProjectSdk().type().ideaTypeId() : "JavaSDK";
                String lvl = model.getLanguageLevel() != null ? model.getLanguageLevel().replaceAll("[^0-9a-zA-Z_.]", "") : "25";
                if (lvl.isBlank() || lvl.equalsIgnoreCase("SDKdefault")) lvl = "25";

                String outputUrl = model.getCompilerOutput();
                if (outputUrl == null || outputUrl.isBlank()) {
                    outputUrl = "$PROJECT_DIR$/out";
                } else if (outputUrl.startsWith(root.toString())) {
                    outputUrl = "$PROJECT_DIR$" + outputUrl.substring(root.toString().length());
                }

                Files.writeString(ideaDir.resolve("misc.xml"), """
                        <?xml version="1.0" encoding="UTF-8"?>
                        <project version="4">
                          <component name="ProjectRootManager" version="2" languageLevel="JDK_%s" default="true" project-jdk-name="%s" project-jdk-type="%s">
                            <output url="file://%s" />
                          </component>
                        </project>
                        """.formatted(lvl, sdkName, sdkType, outputUrl));

                // 2. Save to .idea/modules.xml and .iml
                ModuleModel primary = model.getPrimaryModule();
                String moduleName = primary.getName();
                Files.writeString(ideaDir.resolve("modules.xml"), """
                        <?xml version="1.0" encoding="UTF-8"?>
                        <project version="4">
                          <component name="ProjectModuleManager">
                            <modules>
                              <module fileurl="file://$PROJECT_DIR$/.idea/%s.iml" filepath="$PROJECT_DIR$/.idea/%s.iml" />
                            </modules>
                          </component>
                        </project>
                        """.formatted(moduleName, moduleName));

                StringBuilder imlContent = new StringBuilder();
                imlContent.append("""
                        <?xml version="1.0" encoding="UTF-8"?>
                        <module type="%s" version="4">
                          <component name="NewModuleRootManager" inherit-compiler-output="%b">
                            <exclude-output />
                            <content url="file://$MODULE_DIR$/..">
                        """.formatted(primary.getType(), primary.isInheritCompilerOutput()));

                for (String sf : primary.getSourceFolders()) {
                    imlContent.append("      <sourceFolder url=\"file://$MODULE_DIR$/../").append(sf).append("\" isTestSource=\"false\" />\n");
                }
                for (String tf : primary.getTestSourceFolders()) {
                    imlContent.append("      <sourceFolder url=\"file://$MODULE_DIR$/../").append(tf).append("\" isTestSource=\"true\" />\n");
                }
                for (String rf : primary.getResourceFolders()) {
                    imlContent.append("      <sourceFolder url=\"file://$MODULE_DIR$/../").append(rf).append("\" type=\"java-resource\" />\n");
                }
                for (String trf : primary.getTestResourceFolders()) {
                    imlContent.append("      <sourceFolder url=\"file://$MODULE_DIR$/../").append(trf).append("\" type=\"java-test-resource\" />\n");
                }
                for (String ef : primary.getExcludedFolders()) {
                    imlContent.append("      <excludeFolder url=\"file://$MODULE_DIR$/../").append(ef).append("\" />\n");
                }

                imlContent.append("""
                            </content>
                            <orderEntry type="inheritedJdk" />
                            <orderEntry type="sourceFolder" forTests="false" />
                          </component>
                        </module>
                        """);

                Files.writeString(ideaDir.resolve(moduleName + ".iml"), imlContent.toString());

                // 3. Save internal JSON in .lumina/project-structure.json
                Path luminaDir = root.resolve(".lumina");
                Files.createDirectories(luminaDir);

                JsonObject json = new JsonObject();
                json.addProperty("projectName", model.getProjectName());
                if (model.getProjectSdk() != null) json.addProperty("sdkName", model.getProjectSdk().name());
                json.addProperty("languageLevel", model.getLanguageLevel());
                json.addProperty("compilerOutput", model.getCompilerOutput());

                JsonArray sourcesArr = new JsonArray();
                primary.getSourceFolders().forEach(sourcesArr::add);
                json.add("sources", sourcesArr);

                JsonArray testsArr = new JsonArray();
                primary.getTestSourceFolders().forEach(testsArr::add);
                json.add("tests", testsArr);

                JsonArray resArr = new JsonArray();
                primary.getResourceFolders().forEach(resArr::add);
                json.add("resources", resArr);

                JsonArray testResArr = new JsonArray();
                primary.getTestResourceFolders().forEach(testResArr::add);
                json.add("testResources", testResArr);

                JsonArray exclArr = new JsonArray();
                primary.getExcludedFolders().forEach(exclArr::add);
                json.add("excluded", exclArr);

                Files.writeString(luminaDir.resolve("project-structure.json"),
                        new GsonBuilder().setPrettyPrinting().create().toJson(json));

            } catch (Exception ignored) {}
        }
    }
}
