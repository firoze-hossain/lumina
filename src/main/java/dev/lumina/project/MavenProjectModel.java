package dev.lumina.project;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Data model and dynamic parser for Maven projects, matching IntelliJ IDEA's Maven tool window.
 * Decoupled from JavaFX so it can be tested headlessly.
 */
public final class MavenProjectModel {

    public static final List<String> LIFECYCLE_PHASES = List.of(
            "clean", "validate", "compile", "test", "package",
            "verify", "install", "site", "deploy"
    );

    public record PluginItem(
            String prefix,
            String groupId,
            String artifactId,
            String version,
            List<String> goals
    ) {
        public String displayString() {
            return prefix + " (" + groupId + ":" + artifactId + ":" + version + ")";
        }
    }

    public record DependencyItem(
            String groupId,
            String artifactId,
            String version,
            String scope,
            List<DependencyItem> transitiveDependencies
    ) {
        public String displayString() {
            String base = groupId + ":" + artifactId + ":" + version;
            if (scope != null && !scope.isBlank() && !"compile".equalsIgnoreCase(scope)) {
                return base + " (" + scope.toLowerCase() + ")";
            }
            return base;
        }
    }

    public record RepositoryItem(
            String id,
            String url,
            boolean isLocal
    ) {
        public String displayString() {
            return id + " (" + url + ")";
        }
    }

    public static class MavenProject {
        private String name;
        private String groupId;
        private String artifactId;
        private String version;
        private String packaging = "jar";
        private Path rootPath;
        private Path pomPath;
        private final Map<String, String> properties = new LinkedHashMap<>();
        private final List<PluginItem> plugins = new ArrayList<>();
        private final List<DependencyItem> dependencies = new ArrayList<>();
        private final List<RepositoryItem> repositories = new ArrayList<>();
        private final List<MavenProject> modules = new ArrayList<>();

        public MavenProject(Path rootPath, Path pomPath) {
            this.rootPath = rootPath;
            this.pomPath = pomPath;
        }

        public String getName() { return name != null && !name.isBlank() ? name : (artifactId != null ? artifactId : "Maven Project"); }
        public void setName(String name) { this.name = name; }
        public String getGroupId() { return groupId; }
        public void setGroupId(String groupId) { this.groupId = groupId; }
        public String getArtifactId() { return artifactId; }
        public void setArtifactId(String artifactId) { this.artifactId = artifactId; }
        public String getVersion() { return version; }
        public void setVersion(String version) { this.version = version; }
        public String getPackaging() { return packaging; }
        public void setPackaging(String packaging) { this.packaging = packaging; }
        public Path getRootPath() { return rootPath; }
        public Path getPomPath() { return pomPath; }
        public Map<String, String> getProperties() { return properties; }
        public List<String> getLifecyclePhases() { return LIFECYCLE_PHASES; }
        public List<PluginItem> getPlugins() { return plugins; }
        public List<DependencyItem> getDependencies() { return dependencies; }
        public List<RepositoryItem> getRepositories() { return repositories; }
        public List<MavenProject> getModules() { return modules; }
    }

    private MavenProjectModel() {}

    /**
     * Inspects a project root directory and dynamically parses its {@code pom.xml}.
     * Returns {@code null} if no {@code pom.xml} is present.
     */
    public static MavenProject parseProject(Path rootPath) {
        if (rootPath == null) return null;
        Path pomPath = rootPath.resolve("pom.xml");
        if (!Files.isRegularFile(pomPath)) return null;

        MavenProject project = new MavenProject(rootPath, pomPath);
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
            dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            DocumentBuilder db = dbf.newDocumentBuilder();

            try (InputStream in = Files.newInputStream(pomPath)) {
                Document doc = db.parse(in);
                doc.getDocumentElement().normalize();

                Element root = doc.getDocumentElement();

                // 1. Basic project coordinates
                project.setName(getTagValue(root, "name"));
                project.setGroupId(getTagValue(root, "groupId"));
                project.setArtifactId(getTagValue(root, "artifactId"));
                project.setVersion(getTagValue(root, "version"));
                String packaging = getTagValue(root, "packaging");
                if (packaging != null && !packaging.isBlank()) {
                    project.setPackaging(packaging.trim());
                }

                // If parent coordinates exist and project coordinates are missing
                Element parent = getChildElement(root, "parent");
                if (parent != null) {
                    if (project.getGroupId() == null) project.setGroupId(getTagValue(parent, "groupId"));
                    if (project.getVersion() == null) project.setVersion(getTagValue(parent, "version"));
                }

                // 2. Properties
                Element propsElem = getChildElement(root, "properties");
                if (propsElem != null) {
                    NodeList pNodes = propsElem.getChildNodes();
                    for (int i = 0; i < pNodes.getLength(); i++) {
                        if (pNodes.item(i) instanceof Element pe) {
                            project.getProperties().put(pe.getTagName(), pe.getTextContent().trim());
                        }
                    }
                }
                if (project.getGroupId() != null) project.getProperties().put("project.groupId", project.getGroupId());
                if (project.getArtifactId() != null) project.getProperties().put("project.artifactId", project.getArtifactId());
                if (project.getVersion() != null) project.getProperties().put("project.version", project.getVersion());

                // 3. Dependencies
                Element depsElem = getChildElement(root, "dependencies");
                if (depsElem != null) {
                    NodeList dNodes = depsElem.getElementsByTagName("dependency");
                    for (int i = 0; i < dNodes.getLength(); i++) {
                        if (dNodes.item(i) instanceof Element de) {
                            String g = resolveProperty(getTagValue(de, "groupId"), project.getProperties());
                            String a = resolveProperty(getTagValue(de, "artifactId"), project.getProperties());
                            String v = resolveProperty(getTagValue(de, "version"), project.getProperties());
                            String s = resolveProperty(getTagValue(de, "scope"), project.getProperties());
                            if (g != null && a != null) {
                                if (v == null) v = "";
                                List<DependencyItem> transitives = resolveTransitiveDependencies(g, a, v);
                                project.getDependencies().add(new DependencyItem(g, a, v, s, transitives));
                            }
                        }
                    }
                }

                // 4. Build plugins: default core plugins + pom.xml plugins
                Map<String, PluginItem> pluginMap = createDefaultPlugins();

                Element buildElem = getChildElement(root, "build");
                if (buildElem != null) {
                    Element pluginsElem = getChildElement(buildElem, "plugins");
                    if (pluginsElem != null) {
                        NodeList plNodes = pluginsElem.getElementsByTagName("plugin");
                        for (int i = 0; i < plNodes.getLength(); i++) {
                            if (plNodes.item(i) instanceof Element pe) {
                                String g = resolveProperty(getTagValue(pe, "groupId"), project.getProperties());
                                String a = resolveProperty(getTagValue(pe, "artifactId"), project.getProperties());
                                String v = resolveProperty(getTagValue(pe, "version"), project.getProperties());
                                if (g == null || g.isBlank()) g = "org.apache.maven.plugins";
                                if (a != null && !a.isBlank()) {
                                    String prefix = derivePluginPrefix(a);
                                    List<String> goals = derivePluginGoals(prefix, a);
                                    pluginMap.put(prefix, new PluginItem(prefix, g, a, v != null ? v : "LATEST", goals));
                                }
                            }
                        }
                    }
                }

                // Sort plugins alphabetically by prefix matching Image 2
                List<PluginItem> sortedPlugins = new ArrayList<>(pluginMap.values());
                sortedPlugins.sort(Comparator.comparing(PluginItem::prefix));
                project.getPlugins().addAll(sortedPlugins);

                // 5. Repositories
                // Standard local repository
                project.getRepositories().add(new RepositoryItem("local", "~/.m2/repository", true));
                project.getRepositories().add(new RepositoryItem("central", "https://repo.maven.apache.org/maven2", false));

                Element reposElem = getChildElement(root, "repositories");
                if (reposElem != null) {
                    NodeList rNodes = reposElem.getElementsByTagName("repository");
                    for (int i = 0; i < rNodes.getLength(); i++) {
                        if (rNodes.item(i) instanceof Element re) {
                            String id = getTagValue(re, "id");
                            String url = getTagValue(re, "url");
                            if (id != null && url != null && !"central".equalsIgnoreCase(id)) {
                                project.getRepositories().add(new RepositoryItem(id, url, false));
                            }
                        }
                    }
                }

                // 6. Child modules
                Element modulesElem = getChildElement(root, "modules");
                if (modulesElem != null) {
                    NodeList mNodes = modulesElem.getElementsByTagName("module");
                    for (int i = 0; i < mNodes.getLength(); i++) {
                        if (mNodes.item(i) instanceof Element me) {
                            String subDir = me.getTextContent().trim();
                            if (!subDir.isBlank()) {
                                Path subPath = rootPath.resolve(subDir);
                                MavenProject subProject = parseProject(subPath);
                                if (subProject != null) {
                                    project.getModules().add(subProject);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {}

        return project;
    }

    private static Map<String, PluginItem> createDefaultPlugins() {
        Map<String, PluginItem> map = new LinkedHashMap<>();
        map.put("clean", new PluginItem("clean", "org.apache.maven.plugins", "maven-clean-plugin", "3.2.0",
                List.of("clean:clean", "clean:help")));
        map.put("compiler", new PluginItem("compiler", "org.apache.maven.plugins", "maven-compiler-plugin", "3.13.0",
                List.of("compiler:compile", "compiler:testCompile", "compiler:help")));
        map.put("deploy", new PluginItem("deploy", "org.apache.maven.plugins", "maven-deploy-plugin", "3.1.2",
                List.of("deploy:deploy", "deploy:help")));
        map.put("install", new PluginItem("install", "org.apache.maven.plugins", "maven-install-plugin", "3.1.2",
                List.of("install:install", "install:help")));
        map.put("jar", new PluginItem("jar", "org.apache.maven.plugins", "maven-jar-plugin", "3.4.1",
                List.of("jar:jar", "jar:test-jar")));
        map.put("resources", new PluginItem("resources", "org.apache.maven.plugins", "maven-resources-plugin", "3.3.1",
                List.of("resources:resources", "resources:testResources")));
        map.put("site", new PluginItem("site", "org.apache.maven.plugins", "maven-site-plugin", "3.12.1",
                List.of("site:site", "site:deploy")));
        map.put("surefire", new PluginItem("surefire", "org.apache.maven.plugins", "maven-surefire-plugin", "3.2.5",
                List.of("surefire:test")));
        return map;
    }

    public static String derivePluginPrefix(String artifactId) {
        if (artifactId == null) return "plugin";
        if (artifactId.startsWith("maven-") && artifactId.endsWith("-plugin")) {
            return artifactId.substring("maven-".length(), artifactId.length() - "-plugin".length());
        }
        if (artifactId.endsWith("-maven-plugin")) {
            return artifactId.substring(0, artifactId.length() - "-maven-plugin".length());
        }
        if (artifactId.endsWith("-plugin")) {
            return artifactId.substring(0, artifactId.length() - "-plugin".length());
        }
        return artifactId;
    }

    public static List<String> derivePluginGoals(String prefix, String artifactId) {
        List<String> goals = new ArrayList<>();
        switch (prefix) {
            case "javafx" -> goals.addAll(List.of("javafx:run", "javafx:compile", "javafx:jlink"));
            case "spring-boot" -> goals.addAll(List.of("spring-boot:run", "spring-boot:repackage", "spring-boot:start", "spring-boot:stop"));
            case "compiler" -> goals.addAll(List.of("compiler:compile", "compiler:testCompile", "compiler:help"));
            case "surefire" -> goals.addAll(List.of("surefire:test"));
            case "clean" -> goals.addAll(List.of("clean:clean", "clean:help"));
            case "install" -> goals.addAll(List.of("install:install", "install:help"));
            case "deploy" -> goals.addAll(List.of("deploy:deploy", "deploy:help"));
            case "jar" -> goals.addAll(List.of("jar:jar", "jar:test-jar"));
            case "resources" -> goals.addAll(List.of("resources:resources", "resources:testResources"));
            case "site" -> goals.addAll(List.of("site:site", "site:deploy"));
            case "dependency" -> goals.addAll(List.of("dependency:tree", "dependency:sources", "dependency:resolve", "dependency:analyze"));
            case "exec" -> goals.addAll(List.of("exec:java", "exec:exec"));
            default -> goals.addAll(List.of(prefix + ":" + prefix, prefix + ":help"));
        }
        return goals;
    }

    private static List<DependencyItem> resolveTransitiveDependencies(String groupId, String artifactId, String version) {
        if (groupId == null || artifactId == null || version == null || version.isBlank()) {
            return List.of();
        }
        try {
            String m2 = System.getProperty("user.home", ".") + "/.m2/repository/";
            String rel = groupId.replace('.', '/') + "/" + artifactId + "/" + version + "/" + artifactId + "-" + version + ".pom";
            Path pomFile = Path.of(m2 + rel);
            if (!Files.isRegularFile(pomFile)) return List.of();

            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            DocumentBuilder db = dbf.newDocumentBuilder();
            try (InputStream in = Files.newInputStream(pomFile)) {
                Document doc = db.parse(in);
                Element root = doc.getDocumentElement();
                Element depsElem = getChildElement(root, "dependencies");
                if (depsElem == null) return List.of();

                Map<String, String> props = new HashMap<>();
                Element propsElem = getChildElement(root, "properties");
                if (propsElem != null) {
                    NodeList pNodes = propsElem.getChildNodes();
                    for (int i = 0; i < pNodes.getLength(); i++) {
                        if (pNodes.item(i) instanceof Element pe) {
                            props.put(pe.getTagName(), pe.getTextContent().trim());
                        }
                    }
                }

                List<DependencyItem> transitives = new ArrayList<>();
                NodeList dNodes = depsElem.getElementsByTagName("dependency");
                for (int i = 0; i < dNodes.getLength(); i++) {
                    if (dNodes.item(i) instanceof Element de) {
                        String g = resolveProperty(getTagValue(de, "groupId"), props);
                        String a = resolveProperty(getTagValue(de, "artifactId"), props);
                        String v = resolveProperty(getTagValue(de, "version"), props);
                        String s = resolveProperty(getTagValue(de, "scope"), props);
                        if (g != null && a != null) {
                            transitives.add(new DependencyItem(g, a, v != null ? v : version, s, List.of()));
                        }
                    }
                }
                return transitives;
            }
        } catch (Throwable ignored) {
            return List.of();
        }
    }

    private static String resolveProperty(String val, Map<String, String> props) {
        if (val == null) return null;
        val = val.trim();
        if (val.startsWith("${") && val.endsWith("}")) {
            String key = val.substring(2, val.length() - 1);
            return props.getOrDefault(key, val);
        }
        return val;
    }

    private static String getTagValue(Element parent, String tagName) {
        if (parent == null) return null;
        NodeList list = parent.getElementsByTagName(tagName);
        for (int i = 0; i < list.getLength(); i++) {
            Node n = list.item(i);
            if (n.getParentNode() == parent) {
                return n.getTextContent().trim();
            }
        }
        return null;
    }

    private static Element getChildElement(Element parent, String tagName) {
        if (parent == null) return null;
        NodeList list = parent.getChildNodes();
        for (int i = 0; i < list.getLength(); i++) {
            if (list.item(i) instanceof Element e && e.getTagName().equals(tagName)) {
                return e;
            }
        }
        return null;
    }
}
