package dev.lumina.templates;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Central manager for File and Code Templates in Lumina IDE, mirroring IntelliJ IDEA's FileTemplateManager.
 * Manages Schemes, Built-in templates, custom user additions, overrides, and Velocity macro rendering.
 */
public class FileTemplateManager {

    private static final FileTemplateManager INSTANCE = new FileTemplateManager();

    private final Map<String, List<FileTemplate>> templatesByScheme = new LinkedHashMap<>();
    private final List<FileTemplate> defaultFactoryTemplates = new ArrayList<>();
    private String currentScheme = "Default";
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public static final String GENERAL_FILE_TEMPLATES_DESCRIPTION =
            "In file templates, you can use text, code, comments, and predefined variables. A list of predefined variables is available below. When you use these variables in templates, they expand into corresponding values later in the editor.\n\n"
                    + "It is also possible to specify custom variables. Custom variables use the following format: ${VARIABLE_NAME}, where VARIABLE_NAME is a name for your variable (for example, ${MY_CUSTOM_FUNCTION_NAME}). Before the IDE creates a new file with custom variables, you see a dialog where you can define values for custom variables in the editor.";

    public static FileTemplateManager getInstance() {
        return INSTANCE;
    }

    private FileTemplateManager() {
        initializeFactoryDefaults();
        loadSchemes();
    }

    public List<String> getAvailableSchemes() {
        return List.of("Default", "Project");
    }

    public String getCurrentScheme() {
        return currentScheme;
    }

    public void setCurrentScheme(String scheme) {
        if (scheme != null && !scheme.isBlank()) {
            this.currentScheme = scheme;
            ensureSchemeLoaded(scheme);
        }
    }

    /**
     * Gets all templates belonging to the specified category in the current scheme.
     */
    public List<FileTemplate> getTemplates(FileTemplateCategory category) {
        List<FileTemplate> list = getTemplatesForScheme(currentScheme);
        List<FileTemplate> result = new ArrayList<>();
        for (FileTemplate t : list) {
            if (t.getCategory() == category) {
                result.add(t);
            }
        }
        return result;
    }

    public List<FileTemplate> getAllTemplates() {
        return Collections.unmodifiableList(getTemplatesForScheme(currentScheme));
    }

    public FileTemplate findTemplateByName(String name, FileTemplateCategory category) {
        for (FileTemplate t : getTemplates(category)) {
            if (t.getName().equalsIgnoreCase(name)) {
                return t;
            }
        }
        return null;
    }

    /**
     * Adds a newly created custom template to the current scheme.
     */
    public void addTemplate(FileTemplate template) {
        if (template == null) return;
        List<FileTemplate> list = getTemplatesForScheme(currentScheme);
        list.add(template);
        save();
    }

    /**
     * Removes a custom template from the current scheme.
     */
    public boolean removeTemplate(FileTemplate template) {
        if (template == null || template.isBuiltin()) {
            return false;
        }
        List<FileTemplate> list = getTemplatesForScheme(currentScheme);
        boolean removed = list.remove(template);
        if (removed) {
            save();
        }
        return removed;
    }

    /**
     * Reverts a modified template to its factory default content.
     */
    public void revertTemplate(FileTemplate template) {
        if (template == null) return;
        if (template.isBuiltin()) {
            template.revertToDefault();
            save();
        }
    }

    /**
     * Duplicates an existing template as a custom copy.
     */
    public FileTemplate duplicateTemplate(FileTemplate source, String newName) {
        if (source == null) return null;
        FileTemplate copy = source.copy(newName);
        addTemplate(copy);
        return copy;
    }

    /**
     * Checks if any template in the current scheme has unsaved or custom modifications.
     */
    public boolean hasAnyModifiedTemplates() {
        for (FileTemplate t : getTemplatesForScheme(currentScheme)) {
            if (t.isModified()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Renders a file template expanding Apache Velocity directives (#if, #parse) and macros.
     */
    public String renderTemplate(FileTemplate template, Map<String, Object> customVars) {
        if (template == null) return "";
        String text = template.getText();
        return renderText(text, customVars);
    }

    public String renderText(String rawTemplate, Map<String, Object> customVars) {
        if (rawTemplate == null || rawTemplate.isEmpty()) return "";

        Map<String, String> context = getStandardVariables();
        if (customVars != null) {
            for (Map.Entry<String, Object> e : customVars.entrySet()) {
                context.put(e.getKey(), e.getValue() == null ? "" : String.valueOf(e.getValue()));
            }
        }

        String result = rawTemplate;

        // 1. Process #parse("IncludeName.ext") directives
        Pattern parsePattern = Pattern.compile("#parse\\([\"']([^\"']+)[\"']\\)");
        Matcher parseMatcher = parsePattern.matcher(result);
        StringBuffer parsedBuffer = new StringBuffer();
        while (parseMatcher.find()) {
            String includeTarget = parseMatcher.group(1).trim();
            String cleanName = includeTarget.replaceAll("\\.[a-zA-Z0-9]+$", "");
            FileTemplate includeTemplate = findTemplateByName(cleanName, FileTemplateCategory.INCLUDES);
            String replacement = "";
            if (includeTemplate != null) {
                replacement = renderText(includeTemplate.getText(), customVars);
            }
            parseMatcher.appendReplacement(parsedBuffer, Matcher.quoteReplacement(replacement));
        }
        parseMatcher.appendTail(parsedBuffer);
        result = parsedBuffer.toString();

        // 2. Process #if (${PACKAGE_NAME} && ${PACKAGE_NAME} != "") ... #end
        Pattern ifPackagePattern = Pattern.compile(
                "#if\\s*\\(\\s*\\$\\{PACKAGE_NAME\\}\\s*&&\\s*\\$\\{PACKAGE_NAME\\}\\s*!=\\s*\"\"\\s*\\)(.*?)(?:#else(.*?))?#end",
                Pattern.DOTALL
        );
        Matcher ifMatcher = ifPackagePattern.matcher(result);
        StringBuffer ifBuffer = new StringBuffer();
        String pkg = context.getOrDefault("PACKAGE_NAME", "");
        while (ifMatcher.find()) {
            String ifBody = ifMatcher.group(1);
            String elseBody = ifMatcher.group(2);
            String chosen = (pkg != null && !pkg.isBlank()) ? ifBody : (elseBody != null ? elseBody : "");
            ifMatcher.appendReplacement(ifBuffer, Matcher.quoteReplacement(chosen));
        }
        ifMatcher.appendTail(ifBuffer);
        result = ifBuffer.toString();

        // 3. Process variable replacements ${VAR_NAME} and $VAR_NAME
        for (Map.Entry<String, String> entry : context.entrySet()) {
            String key = entry.getKey();
            String val = entry.getValue() == null ? "" : entry.getValue();
            result = result.replace("${" + key + "}", val);
            result = result.replace("$" + key, val);
        }

        // Clean any leftover $END$ marker
        result = result.replace("$END$", "");

        return result;
    }

    public Map<String, String> getStandardVariables() {
        Map<String, String> vars = new LinkedHashMap<>();
        LocalDateTime now = LocalDateTime.now();
        vars.put("PACKAGE_NAME", "");
        vars.put("NAME", "SampleEntity");
        vars.put("USER", System.getProperty("user.name", "developer"));
        vars.put("DATE", now.format(DateTimeFormatter.ofPattern("M/d/yyyy")));
        vars.put("TIME", now.format(DateTimeFormatter.ofPattern("h:mm a")));
        vars.put("YEAR", String.valueOf(now.getYear()));
        vars.put("MONTH", String.format("%02d", now.getMonthValue()));
        vars.put("MONTH_NAME_SHORT", now.format(DateTimeFormatter.ofPattern("MMM")));
        vars.put("MONTH_NAME_FULL", now.format(DateTimeFormatter.ofPattern("MMMM")));
        vars.put("DAY", String.format("%02d", now.getDayOfMonth()));
        vars.put("HOUR", String.format("%02d", now.getHour()));
        vars.put("MINUTE", String.format("%02d", now.getMinute()));
        vars.put("PROJECT_NAME", "lumina");
        return vars;
    }

    /**
     * Persists customized templates to disk in JSON format.
     */
    public synchronized void save() {
        Path storeFile = getStorageFilePath(currentScheme);
        try {
            Files.createDirectories(storeFile.getParent());
            List<FileTemplateDto> dtos = new ArrayList<>();
            for (FileTemplate t : getTemplatesForScheme(currentScheme)) {
                if (t.isModified() || !t.isBuiltin()) {
                    FileTemplateDto dto = new FileTemplateDto();
                    dto.id = t.getId();
                    dto.name = t.getName();
                    dto.extension = t.getExtension();
                    dto.fileNameTemplate = t.getFileNameTemplate();
                    dto.category = t.getCategory().name();
                    dto.text = t.getText();
                    dto.builtin = t.isBuiltin();
                    dto.reformatCode = t.isReformatCode();
                    dto.liveTemplatesEnabled = t.isLiveTemplatesEnabled();
                    dto.iconKey = t.getIconKey();
                    dto.description = t.getDescription();
                    dto.group = t.getGroup();
                    dtos.add(dto);
                }
            }
            try (Writer writer = Files.newBufferedWriter(storeFile)) {
                gson.toJson(dtos, writer);
            }
        } catch (Throwable ignored) {
        }
    }

    private synchronized void loadSchemes() {
        ensureSchemeLoaded("Default");
        ensureSchemeLoaded("Project");
    }

    private synchronized void ensureSchemeLoaded(String scheme) {
        if (templatesByScheme.containsKey(scheme)) {
            return;
        }

        List<FileTemplate> schemeList = new ArrayList<>();
        // 1. Copy factory defaults
        for (FileTemplate factory : defaultFactoryTemplates) {
            FileTemplate copy = new FileTemplate(
                    factory.getName(), factory.getExtension(), factory.getFileNameTemplate(),
                    factory.getCategory(), factory.getDefaultText(), factory.getDescription(),
                    factory.getIconKey(), factory.getGroup(), true
            );
            copy.setVariables(factory.getVariables());
            copy.setReformatCode(factory.isReformatCode());
            copy.setLiveTemplatesEnabled(factory.isLiveTemplatesEnabled());
            schemeList.add(copy);
        }

        // 2. Overlay saved overrides from file
        Path storeFile = getStorageFilePath(scheme);
        if (Files.isRegularFile(storeFile)) {
            try (Reader reader = Files.newBufferedReader(storeFile)) {
                List<FileTemplateDto> dtos = gson.fromJson(reader, new TypeToken<List<FileTemplateDto>>() {}.getType());
                if (dtos != null) {
                    for (FileTemplateDto dto : dtos) {
                        FileTemplateCategory cat;
                        try {
                            cat = FileTemplateCategory.valueOf(dto.category);
                        } catch (Exception ex) {
                            cat = FileTemplateCategory.FILES;
                        }

                        FileTemplate existing = null;
                        for (FileTemplate t : schemeList) {
                            if (t.getName().equalsIgnoreCase(dto.name) && t.getCategory() == cat) {
                                existing = t;
                                break;
                            }
                        }

                        if (existing != null) {
                            existing.setText(dto.text);
                            existing.setReformatCode(dto.reformatCode);
                            existing.setLiveTemplatesEnabled(dto.liveTemplatesEnabled);
                        } else if (!dto.builtin) {
                            FileTemplate custom = new FileTemplate(
                                    dto.name, dto.extension, dto.fileNameTemplate, cat,
                                    dto.text, dto.description, dto.iconKey,
                                    dto.group != null ? dto.group : "", false
                            );
                            custom.setId(dto.id);
                            custom.setText(dto.text);
                            custom.setReformatCode(dto.reformatCode);
                            custom.setLiveTemplatesEnabled(dto.liveTemplatesEnabled);
                            schemeList.add(custom);
                        }
                    }
                }
            } catch (Throwable ignored) {
            }
        }

        templatesByScheme.put(scheme, schemeList);
    }

    private List<FileTemplate> getTemplatesForScheme(String scheme) {
        ensureSchemeLoaded(scheme);
        return templatesByScheme.get(scheme);
    }

    private Path getStorageFilePath(String scheme) {
        return Path.of(System.getProperty("user.home"), ".lumina", "fileTemplates", scheme.toLowerCase() + "_templates.json");
    }

    // =========================================================================
    // BUILT-IN TEMPLATES REGISTRY (EXACT TO INTELLIJ IDEA SCREENSHOTS)
    // =========================================================================

    private void initializeFactoryDefaults() {
        // --- 1. FILES CATEGORY ---
        registerFile("AnnotationType", "java",
                "#if (${PACKAGE_NAME} && ${PACKAGE_NAME} != \"\")package ${PACKAGE_NAME};\n\n#end\n#parse(\"File Header.java\")\npublic @interface ${NAME} {\n}\n",
                "Applies to new Java annotations that are created by invoking New | Java Class | Annotation in the Project tool window.\n\n"
                        + "This built-in template is editable. Along with Java expressions and comments, you can also use the predefined variables (listed below) that will then be expanded like macros into corresponding values.\n\n"
                        + "It is also possible to specify custom variables. Custom variables use the following format: ${VARIABLE_NAME}, where VARIABLE_NAME is a name for your variable (for example, ${MY_CUSTOM_FUNCTION_NAME}).",
                "java");

        registerFile("AsyncAPI", "yaml",
                "asyncapi: 2.6.0\ninfo:\n  title: ${NAME}\n  version: 1.0.0\nchannels: {}\n",
                "Applies to new AsyncAPI specification documents.",
                "yaml");

        registerFile("Class", "java",
                "#if (${PACKAGE_NAME} && ${PACKAGE_NAME} != \"\")package ${PACKAGE_NAME};\n\n#end\n#parse(\"File Header.java\")\npublic class ${NAME} {\n}\n",
                "Applies to new Java classes created by invoking New | Java Class | Class.",
                "java");

        registerFile("CSS File", "css",
                "/* CSS stylesheet */\n",
                "Applies to new CSS cascading style sheets.",
                "css");

        registerFile("Docker Compose", "yml",
                "version: '3.8'\nservices:\n  app:\n    build: .\n    ports:\n      - \"8080:8080\"\n",
                "Docker Compose container multi-service specification file.",
                "docker");

        registerFile("Dockerfile", "dockerfile",
                "FROM openjdk:21-jdk-slim\nWORKDIR /app\nCOPY . .\nEXPOSE 8080\nCMD [\"java\", \"-jar\", \"app.jar\"]\n",
                "Dockerfile container build script.",
                "docker");

        registerFile("Enum", "java",
                "#if (${PACKAGE_NAME} && ${PACKAGE_NAME} != \"\")package ${PACKAGE_NAME};\n\n#end\n#parse(\"File Header.java\")\npublic enum ${NAME} {\n}\n",
                "Applies to new Java enums that are created by invoking New | Java Class | Enum in the Project tool window.\n\n"
                        + "This built-in template is editable. Along with Java expressions and comments, you can also use the predefined variables (listed below) that will then be expanded like macros into corresponding values.\n\n"
                        + "It is also possible to specify custom variables. Custom variables use the following format: ${VARIABLE_NAME}, where VARIABLE_NAME is a name for your variable.",
                "java");

        registerFile("Exception", "java",
                "#if (${PACKAGE_NAME} && ${PACKAGE_NAME} != \"\")package ${PACKAGE_NAME};\n\n#end\n#parse(\"File Header.java\")\npublic class ${NAME} extends Exception {\n    public ${NAME}() {\n        super();\n    }\n    public ${NAME}(String message) {\n        super(message);\n    }\n}\n",
                "Applies to new Java exceptions extending java.lang.Exception.",
                "java");

        registerFile("FxmlFile", "fxml",
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n\n<?import javafx.scene.layout.VBox?>\n<?import javafx.scene.control.Label?>\n\n<VBox xmlns=\"http://javafx.com/javafx\" xmlns:fx=\"http://javafx.com/fxml\" fx:controller=\"${NAME}Controller\">\n    <Label text=\"${NAME}\"/>\n</VBox>\n",
                "JavaFX FXML UI markup template.",
                "fxml");

        registerFile("Gant Script", "gant",
                "target(default: 'The default target') {\n    echo(message: 'Executing Gant task')\n}\n",
                "Gant Groovy-based Ant build script.",
                "groovy");

        registerFile("GitHub Action", "yml",
                "name: ${NAME}\non: [push]\njobs:\n  build:\n    runs-on: ubuntu-latest\n    steps:\n      - uses: actions/checkout@v4\n",
                "GitHub Action workflow definition.",
                "github");

        registerFile("GitHub Workflow", "yml",
                "name: ${NAME}\non:\n  push:\n    branches: [ main ]\n  pull_request:\n    branches: [ main ]\njobs:\n  test:\n    runs-on: ubuntu-latest\n    steps:\n      - uses: actions/checkout@v4\n",
                "GitHub Actions CI/CD pipeline workflow.",
                "github");

        registerFile("Gradle Build Script", "gradle",
                "plugins {\n    id 'java'\n}\n\nrepositories {\n    mavenCentral()\n}\n\ndependencies {\n    testImplementation platform('org.junit:junit-bom:5.10.0')\n    testImplementation 'org.junit.jupiter:junit-jupiter'\n}\n\ntest {\n    useJUnitPlatform()\n}\n",
                "Gradle Groovy DSL build script.",
                "gradle");

        registerFile("Gradle Build Script with wrapper", "gradle",
                "plugins {\n    id 'java'\n}\n\nwrapper {\n    gradleVersion = '8.5'\n}\n",
                "Gradle Groovy DSL build script with preconfigured wrapper task.",
                "gradle");

        registerFile("Groovy Annotation", "groovy",
                "#if (${PACKAGE_NAME} && ${PACKAGE_NAME} != \"\")package ${PACKAGE_NAME};\n\n#end\n#parse(\"File Header.java\")\n@interface ${NAME} {\n}\n",
                "A built-in template for creating a Groovy annotation.\n\nApache Velocity template language is used",
                "groovy");

        registerFile("Groovy Class", "groovy",
                "#if (${PACKAGE_NAME} && ${PACKAGE_NAME} != \"\")package ${PACKAGE_NAME};\n\n#end\n#parse(\"File Header.java\")\nclass ${NAME} {\n}\n",
                "A built-in template for creating a Groovy class.\n\nApache Velocity template language is used",
                "groovy");

        registerFile("Groovy DSL Script", "gdsl",
                "def ctx = context(scope: scriptScope())\ncontributor(ctx) {\n}\n",
                "Groovy DSL support script (GDSL).",
                "groovy");

        registerFile("Groovy Enum", "groovy",
                "#if (${PACKAGE_NAME} && ${PACKAGE_NAME} != \"\")package ${PACKAGE_NAME};\n\n#end\n#parse(\"File Header.java\")\nenum ${NAME} {\n}\n",
                "A built-in template for creating a Groovy enum.",
                "groovy");

        registerFile("Groovy Interface", "groovy",
                "#if (${PACKAGE_NAME} && ${PACKAGE_NAME} != \"\")package ${PACKAGE_NAME};\n\n#end\n#parse(\"File Header.java\")\ninterface ${NAME} {\n}\n",
                "A built-in template for creating a Groovy interface.",
                "groovy");

        registerFile("Groovy Script", "groovy",
                "#parse(\"File Header.java\")\n\nprintln \"Hello from ${NAME}!\"\n",
                "Executable Groovy script file.",
                "groovy");

        registerFile("Groovy Trait", "groovy",
                "#if (${PACKAGE_NAME} && ${PACKAGE_NAME} != \"\")package ${PACKAGE_NAME};\n\n#end\n#parse(\"File Header.java\")\ntrait ${NAME} {\n}\n",
                "A built-in template for creating a Groovy trait.",
                "groovy");

        registerFile("HTML File", "html",
                "<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n    <meta charset=\"UTF-8\">\n    <title>#[[$Title$]]#</title>\n</head>\n<body>\n    #[[$END$]]#\n</body>\n</html>\n",
                "Standard HTML5 document template.",
                "html");

        registerFile("HTTP Private Environment File", "json",
                "{\n  \"dev\": {\n    \"host\": \"localhost:8080\"\n  }\n}\n",
                "Private environment variables file for HTTP client requests.",
                "json");

        registerFile("HTTP Public Environment File", "json",
                "{\n  \"dev\": {\n    \"host\": \"localhost:8080\"\n  }\n}\n",
                "Public environment variables file for HTTP client requests.",
                "json");

        registerFile("HTTP Request", "http",
                "### GET sample request\nGET https://httpbin.org/get\nAccept: application/json\n",
                "Interactive IntelliJ-style HTTP client request file.",
                "generic");

        registerFile("HTTP Request Scratch", "http",
                "### Scratch request\nGET http://localhost:8080/api\n",
                "Scratchpad HTTP request file.",
                "generic");

        registerFile("Interface", "java",
                "#if (${PACKAGE_NAME} && ${PACKAGE_NAME} != \"\")package ${PACKAGE_NAME};\n\n#end\n#parse(\"File Header.java\")\npublic interface ${NAME} {\n}\n",
                "Applies to new Java interfaces created by invoking New | Java Class | Interface.",
                "java");

        registerFile("JavaFXApplication", "java",
                "#if (${PACKAGE_NAME} && ${PACKAGE_NAME} != \"\")package ${PACKAGE_NAME};\n\n#end\nimport javafx.application.Application;\nimport javafx.stage.Stage;\nimport javafx.scene.Scene;\nimport javafx.scene.layout.StackPane;\nimport javafx.scene.control.Label;\n\npublic class ${NAME} extends Application {\n    @Override\n    public void start(Stage primaryStage) {\n        Label label = new Label(\"Hello Lumina!\");\n        StackPane root = new StackPane(label);\n        Scene scene = new Scene(root, 640, 480);\n        primaryStage.setTitle(\"${NAME}\");\n        primaryStage.setScene(scene);\n        primaryStage.show();\n    }\n    public static void main(String[] args) {\n        launch(args);\n    }\n}\n",
                "JavaFX Application main entry class.",
                "java");

        registerFile("JavaScript File", "js",
                "/**\n * ${NAME}\n */\n",
                "JavaScript source file.",
                "js");

        registerFile("JAX-RS Client", "java",
                "#if (${PACKAGE_NAME} && ${PACKAGE_NAME} != \"\")package ${PACKAGE_NAME};\n\n#end\nimport jakarta.ws.rs.client.Client;\nimport jakarta.ws.rs.client.ClientBuilder;\n\npublic class ${NAME} {\n    private final Client client = ClientBuilder.newClient();\n}\n",
                "JAX-RS REST Web Client class.",
                "java");

        registerFile("JAX-RS Resource", "java",
                "#if (${PACKAGE_NAME} && ${PACKAGE_NAME} != \"\")package ${PACKAGE_NAME};\n\n#end\nimport jakarta.ws.rs.GET;\nimport jakarta.ws.rs.Path;\nimport jakarta.ws.rs.Produces;\nimport jakarta.ws.rs.core.MediaType;\n\n@Path(\"/api\")\npublic class ${NAME} {\n    @GET\n    @Produces(MediaType.APPLICATION_JSON)\n    public String get() {\n        return \"{}\";\n    }\n}\n",
                "JAX-RS REST resource controller class.",
                "java");

        registerFile("Jsp File", "jsp",
                "<%@ page contentType=\"text/html;charset=UTF-8\" language=\"java\" %>\n<html>\n<head>\n    <title>${NAME}</title>\n</head>\n<body>\n</body>\n</html>\n",
                "JavaServer Pages (JSP) template.",
                "html");

        registerFile("package.json", "json",
                "{\n  \"name\": \"${NAME}\",\n  \"version\": \"1.0.0\",\n  \"description\": \"\",\n  \"main\": \"index.js\",\n  \"scripts\": {\n    \"test\": \"echo \\\"Error: no test specified\\\" && exit 1\"\n  },\n  \"keywords\": [],\n  \"author\": \"${USER}\",\n  \"license\": \"ISC\"\n}\n",
                "Node.js package manifest.",
                "json");

        registerFile("PostCSS File", "pcss",
                "/* PostCSS Stylesheet */\n",
                "PostCSS stylesheet document.",
                "css");

        registerFile("Record", "java",
                "#if (${PACKAGE_NAME} && ${PACKAGE_NAME} != \"\")package ${PACKAGE_NAME};\n\n#end\n#parse(\"File Header.java\")\npublic record ${NAME}() {\n}\n",
                "Applies to new Java records created by invoking New | Java Class | Record.",
                "java");

        registerFile("Sass File", "sass",
                "// Sass Stylesheet\n",
                "Sass indented syntax stylesheet.",
                "css");

        registerFile("SCSS File", "scss",
                "// SCSS Stylesheet\n",
                "SCSS stylesheet document.",
                "css");

        registerFile("SimpleSourceFile", "java",
                "void main() {\n    System.out.println(\"Hello, World!\");\n}\n",
                "Single-file Java source program using unnamed classes and instance main methods (JEP 445 / JEP 463).",
                "java");

        registerFile("Swagger", "yaml",
                "openapi: 3.0.3\ninfo:\n  title: ${NAME}\n  version: 1.0.0\npaths: {}\n",
                "OpenAPI / Swagger API specification document.",
                "yaml");

        registerFile("Tag Library Descriptor", "tld",
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<taglib xmlns=\"https://jakarta.ee/xml/ns/jakartaee\" version=\"3.0\">\n    <tlib-version>1.0</tlib-version>\n    <short-name>${NAME}</short-name>\n</taglib>\n",
                "JSP Tag Library Descriptor (TLD) file.",
                "xml");

        registerFile("tsconfig.json", "json",
                "{\n  \"compilerOptions\": {\n    \"target\": \"es2022\",\n    \"module\": \"commonjs\",\n    \"strict\": true,\n    \"esModuleInterop\": true,\n    \"skipLibCheck\": true,\n    \"forceConsistentCasingInFileNames\": true\n  }\n}\n",
                "TypeScript project configuration file.",
                "json");

        registerFile("TypeScript File", "ts",
                "export class ${NAME} {\n}\n",
                "TypeScript module file.",
                "ts");

        registerFile("TypeScript JSX File", "tsx",
                "import React from 'react';\n\nexport const ${NAME}: React.FC = () => {\n    return <div>${NAME}</div>;\n};\n",
                "React TypeScript JSX component file.",
                "ts");

        registerFile("Vue Class API Component", "vue",
                "<template>\n  <div>{{ message }}</div>\n</template>\n\n<script lang=\"ts\">\nimport { Component, Vue } from 'vue-property-decorator';\n\n@Component\nexport default class ${NAME} extends Vue {\n  message = '${NAME}';\n}\n</script>\n",
                "Vue.js Class-style Component file.",
                "vue");

        registerFile("Vue Composition API Component", "vue",
                "<template>\n  <div>{{ message }}</div>\n</template>\n\n<script setup lang=\"ts\">\nimport { ref } from 'vue';\nconst message = ref('${NAME}');\n</script>\n",
                "Vue.js Composition API script setup component file.",
                "vue");

        registerFile("Vue Options API Component", "vue",
                "<template>\n  <div>{{ message }}</div>\n</template>\n\n<script lang=\"ts\">\nimport { defineComponent } from 'vue';\n\nexport default defineComponent({\n  name: '${NAME}',\n  data() {\n    return { message: '${NAME}' };\n  }\n});\n</script>\n",
                "Vue.js Options API component file.",
                "vue");

        registerFile("XML Properties File", "xml",
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<!DOCTYPE properties SYSTEM \"http://java.sun.com/dtd/properties.dtd\">\n<properties>\n    <entry key=\"version\">1.0</entry>\n</properties>\n",
                "XML-based Java properties file.",
                "xml");

        // --- 2. INCLUDES CATEGORY ---
        registerInclude("File Header", "java",
                "/**\n * Created by ${USER} on ${DATE}.\n */",
                "Contains a code fragment that can be included into file templates (the Templates tab) with the help of the #parse directive.\n"
                        + "This built-in template is editable. Along with static text, code, and comments, you can also use the predefined variables that will then be expanded like macros into the corresponding values.\n\n"
                        + "Predefined variables take the following values:\n"
                        + "${PACKAGE_NAME}     Name of the package in which the new file is created\n"
                        + "${USER}             Current user system login name\n"
                        + "${DATE}             Current system date\n"
                        + "${TIME}             Current system time\n"
                        + "${YEAR}             Current year\n"
                        + "${MONTH}            Current month\n"
                        + "${MONTH_NAME_SHORT} First 3 letters of the month name\n"
                        + "${MONTH_NAME_FULL}  Full name of a month\n"
                        + "${DAY}              Current day of the month\n"
                        + "${HOUR}             Current hour\n"
                        + "${MINUTE}           Current minute\n"
                        + "${PROJECT_NAME}     Name of the current project",
                "java");

        registerInclude("File Footer", "java",
                "// End of ${NAME}\n",
                "Code fragment included at the bottom of generated files via #parse(\"File Footer.java\").",
                "java");

        registerInclude("ActionScript File Header", "as",
                "/**\n * Created by ${USER} on ${DATE}.\n */",
                "ActionScript file header banner.",
                "generic");

        registerInclude("PHP File Header", "php",
                "/**\n * Created by ${USER} on ${DATE}.\n */",
                "PHP documentation file header banner.",
                "generic");

        registerInclude("Python File Header", "py",
                "# Created by ${USER} on ${DATE}.\n",
                "Python script header banner.",
                "generic");

        // --- 3. CODE CATEGORY (Matching IntelliJ IDEA Screenshots) ---
        Map<String, String> catchVars = new LinkedHashMap<>();
        catchVars.put("EXCEPTION", "Name of the Exception variable specified as a catch parameter");
        catchVars.put("EXCEPTION_TYPE", "Type of the catch parameter");
        registerCode("Catch Statement Body",
                "throw new RuntimeException(${EXCEPTION});",
                "Fills the body of a catch block when it is generated, e.g. when the Code | Surround with... function is applied.\n"
                        + "This built-in template is editable. Along with Java expressions and comments, you can also use the predefined variables that will be then expanded into the corresponding values.",
                "java", catchVars);

        Map<String, String> catchDeclVars = new LinkedHashMap<>();
        catchDeclVars.put("EXCEPTION_TYPE", "Type of the catch parameter");
        catchDeclVars.put("EXCEPTION", "Name of the Exception variable specified as a catch parameter");
        registerCode("Catch Statement Declaration",
                "catch (final ${EXCEPTION_TYPE} ${EXCEPTION}) {\n    $END$\n}",
                "Surround-with catch statement declaration structure.",
                "java", catchDeclVars);

        registerCode("compose-desktop-build.gradle",
                "plugins {\n    id 'org.jetbrains.compose'\n}\n",
                "Compose Desktop build script template.",
                "gradle", null);

        registerCode("compose-desktop-main",
                "import androidx.compose.ui.window.Window\nimport androidx.compose.ui.window.application\n\nfun main() = application {\n    Window(onCloseRequest = ::exitApplication) {\n    }\n}\n",
                "Compose Desktop main entry point template.",
                "generic", null);

        registerCode("compose-desktop-run-configuration",
                "<component name=\"ProjectRunConfigurationManager\">\n</component>\n",
                "Compose Desktop run configuration XML template.",
                "html", null);

        registerCode("compose-gradle",
                "plugins {\n    kotlin(\"jvm\")\n}\n",
                "Compose Gradle settings template.",
                "gradle", null);

        registerCode("compose-gradle-wrapper",
                "distributionUrl=https\\://services.gradle.org/distributions/gradle-8.5-bin.zip\n",
                "Compose Gradle wrapper properties template.",
                "gradle", null);

        registerCode("compose-settings.gradle",
                "pluginManagement {\n    repositories {\n        google()\n        gradlePluginPortal()\n        mavenCentral()\n    }\n}\n",
                "Compose settings.gradle repository management template.",
                "gradle", null);

        registerCode("Dynamic Method Body",
                "// TODO: Dynamic method\n$END$",
                "Dynamic method body generation template.",
                "java", null);

        registerCode("Groovy JUnit SetUp Method",
                "void setUp() {\n    super.setUp()\n    $END$\n}",
                "Groovy JUnit setUp fixture method.",
                "groovy", null);

        registerCode("Groovy JUnit TearDown Method",
                "void tearDown() {\n    $END$\n    super.tearDown()\n}",
                "Groovy JUnit tearDown fixture method.",
                "groovy", null);

        registerCode("Groovy JUnit Test Case",
                "void test${NAME}() {\n    $END$\n}",
                "Groovy JUnit test case method.",
                "groovy", null);

        registerCode("Groovy JUnit Test Method",
                "void test${NAME}() {\n    $END$\n}",
                "Groovy JUnit test method.",
                "groovy", null);

        registerCode("Groovy New Method Body",
                "// TODO: Groovy method body\n$END$",
                "Groovy new method implementation body.",
                "groovy", null);

        registerCode("I18nized Concatenation",
                "${MESSAGE}",
                "Internationalized string concatenation template.",
                "generic", null);

        registerCode("I18nized Expression",
                "${KEY}",
                "Internationalized resource bundle expression template.",
                "generic", null);

        registerCode("I18nized JSP Expression",
                "<fmt:message key=\"${KEY}\"/>",
                "Internationalized JSP fmt tag message expression.",
                "html", null);

        registerCode("Method Body",
                "// TODO: Method body\n$END$",
                "Fills the body of a newly generated method declaration.",
                "java", null);

        registerCode("Constructor Body",
                "super();\n$END$",
                "Fills the body of an auto-generated class constructor.",
                "java", null);

        registerCode("Getter Body",
                "return this.${FIELD_NAME};",
                "Fills the body of an auto-generated getter method.",
                "java", null);

        registerCode("Setter Body",
                "this.${FIELD_NAME} = ${FIELD_NAME};",
                "Fills the body of an auto-generated setter method.",
                "java", null);

        registerCode("Equals and HashCode Body",
                "return java.util.Objects.equals(this, obj);",
                "Fills the body of auto-generated equals() and hashCode().",
                "java", null);

        registerCode("ToString Body",
                "return \"${NAME}{\" + \"}\";",
                "Fills the body of an auto-generated toString() method.",
                "java", null);

        registerCode("Implemented Method",
                "// TODO: Implement method from interface\n$END$",
                "Generated body for unimplemented interface methods.",
                "java", null);

        registerCode("Overridden Method",
                "super.${METHOD_NAME}();\n$END$",
                "Generated body for overridden superclass methods.",
                "java", null);

        registerCode("React Function Component",
                "import React from 'react';\n\nexport const ${NAME} = () => {\n    return <div>${NAME}</div>;\n};\n",
                "React functional component definition.",
                "js", null);

        registerCode("Retrofit",
                "import retrofit2.http.GET;\n\npublic interface ${NAME} {\n    @GET(\"/\")\n}\n",
                "Retrofit HTTP API interface specification.",
                "java", null);

        registerCode("Spock cleanup Method",
                "def cleanup() {\n    $END$\n}",
                "Spock framework test cleanup fixture method.",
                "groovy", null);

        registerCode("Spock Test Method",
                "def \"${NAME}\"() {\n    expect:\n    $END$\n}",
                "Spock feature test method specification.",
                "groovy", null);

        registerCode("Spock_SetUp_Method",
                "def setup() {\n    $END$\n}",
                "Spock setup method fixture.",
                "groovy", null);

        registerCode("Switch Default Branch",
                "default -> throw new IllegalStateException(\"Unexpected value: \" + ${EXPRESSION});",
                "Default case branch in modern Java enhanced switch statements.",
                "java", null);

        registerCode("TestNG AfterClass Method",
                "@org.testng.annotations.AfterClass\npublic void tearDown() {\n    $END$\n}",
                "TestNG class cleanup method.",
                "java", null);

        registerCode("TestNG BeforeClass Method",
                "@org.testng.annotations.BeforeClass\npublic void setUp() {\n    $END$\n}",
                "TestNG class setup method.",
                "java", null);

        registerCode("TestNG Parameters Method",
                "@org.testng.annotations.DataProvider(name = \"${NAME}\")\npublic Object[][] provideData() {\n    return new Object[][] {};\n}",
                "TestNG parameterized data provider method.",
                "java", null);

        registerCode("TestNG SetUp Method",
                "@org.testng.annotations.BeforeMethod\npublic void setUp() {\n    $END$\n}",
                "TestNG test method setup fixture.",
                "java", null);

        registerCode("TestNG TearDown Method",
                "@org.testng.annotations.AfterMethod\npublic void tearDown() {\n    $END$\n}",
                "TestNG test method cleanup fixture.",
                "java", null);

        registerCode("TestNG Test Class",
                "public class ${NAME} {\n    @org.testng.annotations.Test\n    public void test() {\n    }\n}",
                "TestNG unit test class with initial test method.",
                "java", null);

        Map<String, String> testngVars = new LinkedHashMap<>();
        testngVars.put("NAME", "Name of the created method");
        testngVars.put("BODY", "Generated method body");
        registerCode("TestNG Test Method",
                "@org.testng.annotations.Test\npublic void test${NAME}() {\n    ${BODY}\n}",
                "Creates a test method in a TestNG test class.",
                "java", testngVars);

        registerCode("Typed React Class Component",
                "import React from 'react';\n\ninterface ${NAME}Props {}\n\nexport class ${NAME} extends React.Component<${NAME}Props> {\n    render() {\n        return <div>${NAME}</div>;\n    }\n}",
                "TypeScript React class-based component template.",
                "ts", null);

        registerCode("Typed React Function Component",
                "import React from 'react';\n\ninterface ${NAME}Props {}\n\nexport const ${NAME}: React.FC<${NAME}Props> = (props) => {\n    return <div>${NAME}</div>;\n};\n",
                "TypeScript React functional component template.",
                "ts", null);

        registerCode("TypeScript Implemented Method",
                "${METHOD_NAME}(): void {\n    // TODO: implement\n}",
                "TypeScript implemented method body from interface.",
                "ts", null);

        // --- 4. OTHER CATEGORY (Hierarchical groups matching IntelliJ screenshot 4) ---
        // Application
        registerOther("application.properties", "properties",
                "# Application profile configuration\nserver.port=8080\n",
                "Application deployment and configuration profile template.",
                "generic", "Application", null);
        registerOther("application.xml", "xml",
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<application xmlns=\"https://jakarta.ee/xml/ns/jakartaee\" version=\"9\">\n</application>\n",
                "Java EE / Jakarta EE enterprise application descriptor.",
                "xml", "Deployment descriptors", null);

        // CDI
        registerOther("beans.xml", "xml",
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<beans xmlns=\"https://jakarta.ee/xml/ns/jakartaee\" bean-discovery-mode=\"annotated\">\n</beans>\n",
                "Contexts and Dependency Injection (CDI) descriptor file.",
                "xml", "CDI", null);

        // JAX-RS
        registerOther("JAX-RS Client", "java",
                "#if (${PACKAGE_NAME} && ${PACKAGE_NAME} != \"\")package ${PACKAGE_NAME};\n\n#end\nimport jakarta.ws.rs.client.Client;\nimport jakarta.ws.rs.client.ClientBuilder;\n\npublic class ${NAME} {\n    private final Client client = ClientBuilder.newClient();\n}\n",
                "JAX-RS REST Web Client definition.",
                "java", "JAX-RS", null);
        registerOther("JAX-RS Resource", "java",
                "#if (${PACKAGE_NAME} && ${PACKAGE_NAME} != \"\")package ${PACKAGE_NAME};\n\n#end\nimport jakarta.ws.rs.GET;\nimport jakarta.ws.rs.Path;\nimport jakarta.ws.rs.Produces;\nimport jakarta.ws.rs.core.MediaType;\n\n@Path(\"/api\")\npublic class ${NAME} {\n    @GET\n    @Produces(MediaType.APPLICATION_JSON)\n    public String get() {\n        return \"{}\";\n    }\n}\n",
                "JAX-RS REST endpoint resource class.",
                "java", "JAX-RS", null);

        // JBoss/WildFly Server
        registerOther("jboss-web.xml", "xml",
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<jboss-web>\n</jboss-web>\n",
                "JBoss / WildFly server web application configuration descriptor.",
                "xml", "JBoss/WildFly Server", null);
        registerOther("jboss-deployment-structure.xml", "xml",
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<jboss-deployment-structure>\n</jboss-deployment-structure>\n",
                "JBoss / WildFly module deployment structure descriptor.",
                "xml", "JBoss/WildFly Server", null);

        // JPA
        registerOther("persistence.xml", "xml",
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<persistence xmlns=\"https://jakarta.ee/xml/ns/persistence\" version=\"3.0\">\n    <persistence-unit name=\"default\">\n    </persistence-unit>\n</persistence>\n",
                "Jakarta Persistence (JPA) configuration descriptor.",
                "xml", "JPA", null);
        registerOther("orm.xml", "xml",
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<entity-mappings xmlns=\"https://jakarta.ee/xml/ns/persistence/orm\" version=\"3.0\">\n</entity-mappings>\n",
                "Jakarta Persistence XML entity-mappings descriptor.",
                "xml", "JPA", null);

        // Java Enterprise
        registerOther("ejb-jar.xml", "xml",
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<ejb-jar xmlns=\"https://jakarta.ee/xml/ns/jakartaee\" version=\"4.0\">\n</ejb-jar>\n",
                "Enterprise JavaBeans (EJB) deployment descriptor.",
                "xml", "Java Enterprise", null);

        // JavaFX
        registerOther("JavaFX Application", "java",
                "#if (${PACKAGE_NAME} && ${PACKAGE_NAME} != \"\")package ${PACKAGE_NAME};\n\n#end\nimport javafx.application.Application;\nimport javafx.stage.Stage;\nimport javafx.scene.Scene;\nimport javafx.scene.layout.StackPane;\nimport javafx.scene.control.Label;\n\npublic class ${NAME} extends Application {\n    @Override\n    public void start(Stage stage) {\n        Label label = new Label(\"Hello Lumina!\");\n        StackPane root = new StackPane(label);\n        Scene scene = new Scene(root, 640, 480);\n        stage.setTitle(\"${NAME}\");\n        stage.setScene(scene);\n        stage.show();\n    }\n}\n",
                "JavaFX Application main entry point.",
                "java", "JavaFX", null);
        registerOther("FxmlFile", "fxml",
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n\n<?import javafx.scene.layout.AnchorPane?>\n\n<AnchorPane xmlns=\"http://javafx.com/javafx\" xmlns:fx=\"http://javafx.com/fxml\" fx:controller=\"${NAME}Controller\">\n</AnchorPane>\n",
                "JavaFX FXML user interface definition.",
                "xml", "JavaFX", null);

        // Maven
        registerOther("pom.xml", "xml",
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<project xmlns=\"http://maven.apache.org/POM/4.0.0\"\n         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n    <modelVersion>4.0.0</modelVersion>\n    <groupId>${GROUP_ID}</groupId>\n    <artifactId>${ARTIFACT_ID}</artifactId>\n    <version>1.0-SNAPSHOT</version>\n</project>\n",
                "Apache Maven Project Object Model (POM) descriptor.",
                "xml", "Maven", null);
        registerOther("extensions.xml", "xml",
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<extensions xmlns=\"http://maven.apache.org/EXTENSIONS/1.0.0\">\n</extensions>\n",
                "Maven build extension configuration.",
                "xml", "Maven", null);
        registerOther("settings.xml", "xml",
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<settings xmlns=\"http://maven.apache.org/SETTINGS/1.0.0\">\n</settings>\n",
                "Maven local/remote configuration settings.",
                "xml", "Maven", null);

        // Spring
        registerOther("Spring Config", "xml",
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<beans xmlns=\"http://www.springframework.org/schema/beans\"\n       xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n       xsi:schemaLocation=\"http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd\">\n</beans>\n",
                "Spring bean application context XML definition.",
                "xml", "Spring", null);
        registerOther("application.properties", "properties",
                "spring.application.name=${NAME}\n",
                "Spring Boot application configuration properties.",
                "generic", "Spring", null);

        // Tomcat Server
        registerOther("context.xml", "xml",
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<Context reloadable=\"true\">\n</Context>\n",
                "Apache Tomcat web application context definition.",
                "xml", "Tomcat Server", null);

        // Web
        registerOther("web.xml", "xml",
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<web-app xmlns=\"https://jakarta.ee/xml/ns/jakartaee\"\n         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n         xsi:schemaLocation=\"https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/web-app_6_0.xsd\"\n         version=\"6.0\">\n</web-app>\n",
                "Java Enterprise Web Application deployment descriptor.",
                "xml", "Web", null);
        registerOther("faces-config.xml", "xml",
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<faces-config xmlns=\"https://jakarta.ee/xml/ns/jakartaee\" version=\"4.0\">\n</faces-config>\n",
                "Jakarta Faces (JSF) application configuration.",
                "xml", "Web", null);
        registerOther("Jsp File", "jsp",
                "<%@ page contentType=\"text/html;charset=UTF-8\" language=\"java\" %>\n<html>\n<head>\n    <title>${NAME}</title>\n</head>\n<body>\n</body>\n</html>\n",
                "JavaServer Pages (JSP) web template.",
                "html", "Web", null);
        registerOther("Tag Library Descriptor", "tld",
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<taglib xmlns=\"https://jakarta.ee/xml/ns/jakartaee\" version=\"3.0\">\n    <tlib-version>1.0</tlib-version>\n    <short-name>${NAME}</short-name>\n</taglib>\n",
                "JSP Tag Library Descriptor (TLD) file.",
                "xml", "Web", null);
    }

    private void registerFile(String name, String ext, String defaultText, String description, String iconKey) {
        FileTemplate t = new FileTemplate(name, ext, FileTemplateCategory.FILES, defaultText, description, iconKey);
        t.setVariables(getStandardVariables());
        defaultFactoryTemplates.add(t);
    }

    private void registerInclude(String name, String ext, String defaultText, String description, String iconKey) {
        FileTemplate t = new FileTemplate(name, ext, FileTemplateCategory.INCLUDES, defaultText, description, iconKey);
        t.setVariables(getStandardVariables());
        defaultFactoryTemplates.add(t);
    }

    private void registerCode(String name, String defaultText, String description, String iconKey, Map<String, String> variables) {
        FileTemplate t = new FileTemplate(name, "", FileTemplateCategory.CODE, defaultText, description, iconKey);
        if (variables != null) {
            t.setVariables(variables);
        } else {
            t.setVariables(getStandardVariables());
        }
        defaultFactoryTemplates.add(t);
    }

    private void registerOther(String name, String ext, String defaultText, String description, String iconKey, String group, Map<String, String> variables) {
        FileTemplate t = new FileTemplate(name, ext, "", FileTemplateCategory.OTHER, defaultText, description, iconKey, group, true);
        if (variables != null) {
            t.setVariables(variables);
        } else {
            t.setVariables(getStandardVariables());
        }
        defaultFactoryTemplates.add(t);
    }

    private static class FileTemplateDto {
        String id;
        String name;
        String extension;
        String fileNameTemplate;
        String category;
        String group;
        String text;
        boolean builtin;
        boolean reformatCode;
        boolean liveTemplatesEnabled;
        String iconKey;
        String description;
    }
}

