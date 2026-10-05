package dev.lumina.settings;

import dev.lumina.LuminaApp;
import dev.lumina.util.Settings;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Dynamic persistent configuration model for Editor > File Encodings settings.
 * Synchronizes with IntelliJ IDEA's .idea/encodings.xml and Lumina preferences.
 */
public final class FileEncodingsSettings {

    private static final FileEncodingsSettings INSTANCE = new FileEncodingsSettings();

    public static FileEncodingsSettings getInstance() {
        return INSTANCE;
    }

    public static final String DEFAULT_GLOBAL_ENCODING = "UTF-8";
    public static final String BOM_WITH_NO_BOM = "with NO BOM";
    public static final String BOM_WITH_BOM = "with BOM";
    public static final String BOM_WITH_BOM_ON_WINDOWS = "with BOM only on Windows";

    public record PathMapping(String relativeOrAbsolutePath, String encoding, boolean isDirectory) {
        public String getDisplayPath(Path projectRoot) {
            String p = relativeOrAbsolutePath.replace('/', File.separatorChar).replace('\\', File.separatorChar);
            if (projectRoot != null) {
                try {
                    Path abs = projectRoot.resolve(p).normalize();
                    if (abs.startsWith(projectRoot.normalize())) {
                        Path rel = projectRoot.normalize().relativize(abs);
                        return "...\\" + rel.toString().replace('/', '\\');
                    }
                } catch (Exception ignored) {}
            }
            if (p.startsWith("...\\") || p.startsWith(".../")) {
                return p;
            }
            if (!p.contains(":") && !p.startsWith(File.separator)) {
                return "...\\" + p.replace('/', '\\');
            }
            return p;
        }

        public String toIdeaUrl(Path projectRoot) {
            String p = relativeOrAbsolutePath.replace('\\', '/');
            if (p.startsWith(".../")) {
                p = p.substring(4);
            } else if (p.startsWith("...\\")) {
                p = p.substring(4);
            }
            if (!p.contains(":") && !p.startsWith("/")) {
                return "file://$PROJECT_DIR$/" + p;
            }
            if (projectRoot != null) {
                try {
                    Path abs = Path.of(p).toAbsolutePath().normalize();
                    Path root = projectRoot.toAbsolutePath().normalize();
                    if (abs.startsWith(root)) {
                        Path rel = root.relativize(abs);
                        return "file://$PROJECT_DIR$/" + rel.toString().replace('\\', '/');
                    }
                } catch (Exception ignored) {}
            }
            return "file://" + p;
        }
    }

    private String globalEncoding = DEFAULT_GLOBAL_ENCODING;
    private String projectEncoding = getSystemDefaultEncodingLabel();
    private String propertiesFileEncoding = getPropertiesDefaultEncodingLabel();
    private boolean transparentNativeToAscii = false;
    private String createUtf8FilesOption = BOM_WITH_NO_BOM;
    private final List<PathMapping> mappings = new ArrayList<>();

    private final List<Consumer<FileEncodingsSettings>> listeners = new CopyOnWriteArrayList<>();

    private FileEncodingsSettings() {
        load();
    }

    public static String getSystemDefaultEncodingLabel() {
        return "<System Default: " + Charset.defaultCharset().displayName() + ">";
    }

    public static String getPropertiesDefaultEncodingLabel() {
        return "<Properties Default: UTF-8>";
    }

    public Path getProjectRoot() {
        if (!LuminaApp.ACTIVE_INSTANCES.isEmpty()) {
            LuminaApp app = LuminaApp.ACTIVE_INSTANCES.get(0);
            if (app != null && app.getProjectRoot() != null) {
                return app.getProjectRoot();
            }
        }
        String last = Settings.get(Settings.LAST_PROJECT);
        if (last != null && !last.isBlank()) {
            try {
                Path p = Path.of(last).toAbsolutePath().normalize();
                if (Files.exists(p)) return p;
            } catch (Exception ignored) {}
        }
        return Path.of(".").toAbsolutePath().normalize();
    }

    public synchronized void load() {
        String savedGlobal = Settings.get("encoding.global");
        if (savedGlobal != null && !savedGlobal.isBlank()) {
            globalEncoding = savedGlobal.trim();
        } else {
            globalEncoding = DEFAULT_GLOBAL_ENCODING;
        }

        String savedProject = Settings.get("encoding.project");
        if (savedProject != null && !savedProject.isBlank()) {
            projectEncoding = savedProject.trim();
        } else {
            projectEncoding = getSystemDefaultEncodingLabel();
        }

        String savedProps = Settings.get("encoding.properties");
        if (savedProps != null && !savedProps.isBlank()) {
            propertiesFileEncoding = savedProps.trim();
        } else {
            propertiesFileEncoding = getPropertiesDefaultEncodingLabel();
        }

        String savedNative = Settings.get("encoding.native2ascii");
        if (savedNative != null && !savedNative.isBlank()) {
            transparentNativeToAscii = Boolean.parseBoolean(savedNative.trim());
        } else {
            transparentNativeToAscii = false;
        }

        String savedBom = Settings.get("encoding.createUtf8BOM");
        if (savedBom != null && !savedBom.isBlank()) {
            createUtf8FilesOption = savedBom.trim();
        } else {
            createUtf8FilesOption = BOM_WITH_NO_BOM;
        }

        mappings.clear();

        // Load project mappings from .idea/encodings.xml
        Path root = getProjectRoot();
        if (root != null) {
            Path encXml = root.resolve(".idea").resolve("encodings.xml");
            if (Files.isRegularFile(encXml)) {
                loadFromIdeaXml(encXml, root);
            }
        }

        // If no mappings loaded, add default src/main/java and src/main/resources if present
        if (mappings.isEmpty() && root != null) {
            Path javaSrc = root.resolve("src/main/java");
            Path resSrc = root.resolve("src/main/resources");
            if (Files.exists(javaSrc) || Files.exists(resSrc)) {
                if (Files.exists(javaSrc)) {
                    mappings.add(new PathMapping("src/main/java", "UTF-8", true));
                }
                if (Files.exists(resSrc)) {
                    mappings.add(new PathMapping("src/main/resources", "UTF-8", true));
                }
            }
        }
    }

    private void loadFromIdeaXml(Path encXml, Path root) {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(encXml.toFile());
            doc.getDocumentElement().normalize();

            NodeList components = doc.getElementsByTagName("component");
            for (int i = 0; i < components.getLength(); i++) {
                Element comp = (Element) components.item(i);
                if ("Encoding".equals(comp.getAttribute("name"))) {
                    if (comp.hasAttribute("defaultCharsetForProperties")) {
                        String propEnc = comp.getAttribute("defaultCharsetForProperties");
                        if (!propEnc.isBlank()) {
                            propertiesFileEncoding = propEnc.trim();
                        }
                    }
                    if (comp.hasAttribute("native2AsciiForPropertiesFiles")) {
                        transparentNativeToAscii = "true".equalsIgnoreCase(comp.getAttribute("native2AsciiForPropertiesFiles"));
                    }

                    NodeList files = comp.getElementsByTagName("file");
                    for (int j = 0; j < files.getLength(); j++) {
                        Element f = (Element) files.item(j);
                        String url = f.getAttribute("url");
                        String charset = f.getAttribute("charset");
                        if (charset.isBlank()) charset = "UTF-8";

                        if ("PROJECT".equalsIgnoreCase(url)) {
                            projectEncoding = charset;
                        } else if (!url.isBlank()) {
                            String relPath = url;
                            if (url.startsWith("file://$PROJECT_DIR$/")) {
                                relPath = url.substring("file://$PROJECT_DIR$/".length());
                            } else if (url.startsWith("file://")) {
                                relPath = url.substring("file://".length());
                            }
                            boolean isDir = true;
                            try {
                                Path resolved = root.resolve(relPath);
                                if (Files.exists(resolved) && !Files.isDirectory(resolved)) {
                                    isDir = false;
                                }
                            } catch (Exception ignored) {}
                            mappings.add(new PathMapping(relPath, charset, isDir));
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Keep existing defaults on parse failure
        }
    }

    public synchronized void save() {
        Settings.put("encoding.global", globalEncoding);
        Settings.put("encoding.project", projectEncoding);
        Settings.put("encoding.properties", propertiesFileEncoding);
        Settings.put("encoding.native2ascii", String.valueOf(transparentNativeToAscii));
        Settings.put("encoding.createUtf8BOM", createUtf8FilesOption);

        Path root = getProjectRoot();
        if (root != null) {
            try {
                Path ideaDir = root.resolve(".idea");
                Files.createDirectories(ideaDir);
                Path encXml = ideaDir.resolve("encodings.xml");

                saveToIdeaXml(encXml, root);
            } catch (Exception ignored) {}
        }

        notifyListeners();
    }

    private void saveToIdeaXml(Path encXml, Path root) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.newDocument();

        Element projectElem = doc.createElement("project");
        projectElem.setAttribute("version", "4");
        doc.appendChild(projectElem);

        Element compElem = doc.createElement("component");
        compElem.setAttribute("name", "Encoding");

        if (propertiesFileEncoding != null && !propertiesFileEncoding.startsWith("<")) {
            compElem.setAttribute("defaultCharsetForProperties", propertiesFileEncoding);
        }
        if (transparentNativeToAscii) {
            compElem.setAttribute("native2AsciiForPropertiesFiles", "true");
        }

        // If projectEncoding is not system default, persist url="PROJECT"
        if (projectEncoding != null && !projectEncoding.startsWith("<")) {
            Element projFile = doc.createElement("file");
            projFile.setAttribute("url", "PROJECT");
            projFile.setAttribute("charset", projectEncoding);
            compElem.appendChild(projFile);
        }

        for (PathMapping m : mappings) {
            Element f = doc.createElement("file");
            f.setAttribute("url", m.toIdeaUrl(root));
            f.setAttribute("charset", m.encoding());
            compElem.appendChild(f);
        }

        projectElem.appendChild(compElem);

        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.METHOD, "xml");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

        StringWriter sw = new StringWriter();
        transformer.transform(new DOMSource(doc), new StreamResult(sw));
        Files.writeString(encXml, sw.toString(), StandardCharsets.UTF_8);
    }

    public synchronized Charset getEffectiveCharset(Path path) {
        if (path == null) return getProjectOrGlobalCharset();

        Path abs = path.toAbsolutePath().normalize();
        Path root = getProjectRoot();

        // 1. Direct path match or directory match
        for (PathMapping m : mappings) {
            try {
                Path mPath = (root != null) ? root.resolve(m.relativeOrAbsolutePath()).toAbsolutePath().normalize()
                        : Path.of(m.relativeOrAbsolutePath()).toAbsolutePath().normalize();
                if (abs.equals(mPath) || (m.isDirectory() && abs.startsWith(mPath))) {
                    try {
                        return Charset.forName(m.encoding());
                    } catch (Exception ignored) {}
                }
            } catch (Exception ignored) {}
        }

        // 2. Properties files check
        if (path.getFileName() != null && path.getFileName().toString().endsWith(".properties")) {
            if (propertiesFileEncoding != null && !propertiesFileEncoding.startsWith("<")) {
                try {
                    return Charset.forName(propertiesFileEncoding);
                } catch (Exception ignored) {}
            }
        }

        // 3. Project or Global
        return getProjectOrGlobalCharset();
    }

    public Charset getProjectOrGlobalCharset() {
        if (projectEncoding != null && !projectEncoding.startsWith("<")) {
            try {
                return Charset.forName(projectEncoding);
            } catch (Exception ignored) {}
        }
        if (globalEncoding != null && !globalEncoding.isBlank()) {
            try {
                return Charset.forName(globalEncoding);
            } catch (Exception ignored) {}
        }
        return StandardCharsets.UTF_8;
    }

    public void addListener(Consumer<FileEncodingsSettings> listener) {
        listeners.add(listener);
    }

    public void removeListener(Consumer<FileEncodingsSettings> listener) {
        listeners.remove(listener);
    }

    private void notifyListeners() {
        for (Consumer<FileEncodingsSettings> l : listeners) {
            try {
                l.accept(this);
            } catch (Exception ignored) {}
        }
    }

    // Getters and Setters
    public String getGlobalEncoding() { return globalEncoding; }
    public void setGlobalEncoding(String globalEncoding) { this.globalEncoding = globalEncoding; }

    public String getProjectEncoding() { return projectEncoding; }
    public void setProjectEncoding(String projectEncoding) { this.projectEncoding = projectEncoding; }

    public String getPropertiesFileEncoding() { return propertiesFileEncoding; }
    public void setPropertiesFileEncoding(String propertiesFileEncoding) { this.propertiesFileEncoding = propertiesFileEncoding; }

    public boolean isTransparentNativeToAscii() { return transparentNativeToAscii; }
    public void setTransparentNativeToAscii(boolean transparentNativeToAscii) { this.transparentNativeToAscii = transparentNativeToAscii; }

    public String getCreateUtf8FilesOption() { return createUtf8FilesOption; }
    public void setCreateUtf8FilesOption(String createUtf8FilesOption) { this.createUtf8FilesOption = createUtf8FilesOption; }

    public List<PathMapping> getMappings() { return new ArrayList<>(mappings); }
    public void setMappings(List<PathMapping> newMappings) {
        mappings.clear();
        if (newMappings != null) mappings.addAll(newMappings);
    }
}
