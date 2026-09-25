package dev.lumina.settings;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Dynamic configuration model for Editor > General > Gutter Icons.
 * Manages all gutter icon categories, entries, and their enabled states without hardcoding.
 * Supports runtime registration from plugins, persistent storage in ~/.lumina/lumina.properties,
 * and live change listeners.
 */
public class GutterIconsSettings {

    private static class Holder {
        private static final GutterIconsSettings INSTANCE = new GutterIconsSettings();
    }

    public static GutterIconsSettings getInstance() {
        return Holder.INSTANCE;
    }

    public record GutterIconDescriptor(
            String id,
            String category,
            String name,
            boolean defaultEnabled,
            String iconType
    ) {}

    private static final List<GutterIconDescriptor> BUILTIN_DESCRIPTORS = new ArrayList<>();

    static {
        // 1. AOP Pointcut Language
        registerBuiltin("aop.pointcut.java.kotlin", "AOP Pointcut Language", "AOP (Java/Kotlin)", true, "AOP");

        // 2. Common
        registerBuiltin("common.color.preview", "Common", "Color preview", true, "COLOR_PREVIEW");
        registerBuiltin("common.doc.comments.in.place", "Common", "Documentation comments in-place rendering", true, "DOC_COMMENTS");
        registerBuiltin("common.run.line.marker", "Common", "Run line marker", true, "RUN_MARKER");

        // 3. Compose Multiplatform
        registerBuiltin("compose.color.picker", "Compose Multiplatform", "Compose color picker", true, "NONE");

        // 4. Database Tools and SQL
        registerBuiltin("database.recursive.call", "Database Tools and SQL", "Recursive call", true, "RECURSIVE_CALL");

        // 5. Dev Containers
        registerBuiltin("devcontainers.add.modified.settings", "Dev Containers", "Add Modified Settings from IDE", true, "BOX_CONTAINER");

        // 6. EditorConfig
        registerBuiltin("editorconfig.code.preview", "EditorConfig", "Code preview", true, "NONE");

        // 7. GitHub
        registerBuiltin("github.action.color", "GitHub", "Action color", true, "NONE");
        registerBuiltin("github.action.icon", "GitHub", "Action icon", true, "NONE");

        // 8. Go
        registerBuiltin("go.datasource.db.connection", "Go", "Data source from the database connection", true, "DATABASE_TABLE");
        registerBuiltin("go.datasource.go.package", "Go", "Data source from the Go package", true, "DATABASE_PACKAGE");
        registerBuiltin("go.implementations", "Go", "Go to Implementations", true, "GOTO_IMPLEMENTATIONS");
        registerBuiltin("go.interfaces", "Go", "Go to Interfaces", true, "GOTO_INTERFACES");
        registerBuiltin("go.run.generate", "Go", "Run go generate on comment", true, "PLAY");

        // 9. HTTP Client
        registerBuiltin("http.client.execute.request", "HTTP Client", "Execute request", true, "EXCHANGE_ARROWS");
        registerBuiltin("http.client.line.markers", "HTTP Client", "HTTP Client line markers", true, "ARROW_RIGHT");
        registerBuiltin("http.client.response.diff", "HTTP Client", "HTTP response diff", true, "DIFF_ARROWS");
        registerBuiltin("http.client.retrofit.open", "HTTP Client", "Retrofit open in httpClient", true, "HTTP_CLIENT");
        registerBuiltin("http.client.run.request", "HTTP Client", "Run HTTP Request", true, "PLAY");
        registerBuiltin("http.client.certificate.passphrase", "HTTP Client", "Set certificate passphrase", true, "KEY_CERTIFICATE");

        // 10. Jakarta EE Platform
        registerBuiltin("jakarta.ee.app.config.file", "Jakarta EE Platform", "JavaEE:Application configuration file", true, "EE_BADGE");

        // 11. Jakarta EE: Contexts and Dependency Injection (CDI)
        registerBuiltin("jakarta.cdi.injection.points", "Jakarta EE: Contexts and Dependency Injection (CDI)", "Injection points", true, "INJECTION_POINT");
        registerBuiltin("jakarta.cdi.producers.disposer", "Jakarta EE: Contexts and Dependency Injection (CDI)", "Producers for Disposer methods", true, "DISPOSER");

        // 12. Jakarta EE: Persistence (JPA)
        registerBuiltin("jakarta.jpa.config.files", "Jakarta EE: Persistence (JPA)", "JPA configuration files", true, "JPA_CONFIG");
        registerBuiltin("jakarta.jpa.entity.mappings", "Jakarta EE: Persistence (JPA)", "JPA Entity mappings", true, "JPA_MAPPINGS");

        // 13. Jakarta EE: RESTful Web Services (JAX-RS)
        registerBuiltin("jakarta.jaxrs.open.http.client", "Jakarta EE: RESTful Web Services (JAX-RS)", "Open in HTTP Client JAX-RS RequestMapping", true, "GLOBE_LINK");

        // 14. Java
        registerBuiltin("java.external.annotations", "Java", "External annotations", true, "AT_SIGN");
        registerBuiltin("java.icon.preview", "Java", "Icon preview", true, "NONE");
        registerBuiltin("java.implemented.method", "Java", "Implemented method", true, "IMPLEMENTED_METHOD");
        registerBuiltin("java.implementing.method", "Java", "Implementing method", true, "IMPLEMENTING_METHOD");
        registerBuiltin("java.inferred.contract.annotations", "Java", "Inferred contract annotations", false, "AT_SIGN");
        registerBuiltin("java.inferred.nullability.annotations", "Java", "Inferred nullability annotations", false, "AT_SIGN");
        registerBuiltin("java.lambda", "Java", "Lambda", true, "LAMBDA");
        registerBuiltin("java.overridden.method", "Java", "Overridden method", true, "OVERRIDDEN_METHOD");
        registerBuiltin("java.overriding.method", "Java", "Overriding method", true, "OVERRIDING_METHOD");
        registerBuiltin("java.recursive.call", "Java", "Recursive call", true, "RECURSIVE_CALL");
        registerBuiltin("java.service", "Java", "Service", true, "SERVICE_STAR");
        registerBuiltin("java.sibling.inherited.method", "Java", "Sibling inherited method", true, "SIBLING_INHERITED");

        // 15. JavaScript and TypeScript
        registerBuiltin("js.ts.implemented", "JavaScript and TypeScript", "Implemented", true, "IMPLEMENTED_METHOD");
        registerBuiltin("js.ts.implementing", "JavaScript and TypeScript", "Implementing", true, "IMPLEMENTING_METHOD");
        registerBuiltin("js.ts.source", "JavaScript and TypeScript", "JavaScript source", true, "JS_BADGE");
        registerBuiltin("js.ts.overridden", "JavaScript and TypeScript", "Overridden", true, "OVERRIDDEN_METHOD");
        registerBuiltin("js.ts.overriding", "JavaScript and TypeScript", "Overriding", true, "OVERRIDING_METHOD");
        registerBuiltin("js.ts.recursive.call", "JavaScript and TypeScript", "Recursive call", true, "RECURSIVE_CALL");

        // 16. JetBrains AI Assistant
        registerBuiltin("ai.assistant.code.fragment.formatted", "JetBrains AI Assistant", "This code fragment is being formatted", true, "NONE");

        // 17. Kotlin
        registerBuiltin("kotlin.dsl.markers", "Kotlin", "DSL markers", true, "AT_SIGN");
        registerBuiltin("kotlin.implemented.declaration", "Kotlin", "Implemented declaration", true, "IMPLEMENTED_METHOD");
        registerBuiltin("kotlin.implementing.declaration", "Kotlin", "Implementing declaration", true, "IMPLEMENTING_METHOD");
        registerBuiltin("kotlin.multiplatform.actual", "Kotlin", "Multiplatform actual declaration", true, "ACTUAL_DECLARATION");
        registerBuiltin("kotlin.multiplatform.expect", "Kotlin", "Multiplatform expect declaration", true, "EXPECT_DECLARATION");
        registerBuiltin("kotlin.overridden.declaration", "Kotlin", "Overridden declaration", true, "OVERRIDDEN_METHOD");
        registerBuiltin("kotlin.overriding.declaration", "Kotlin", "Overriding declaration", true, "OVERRIDING_METHOD");
        registerBuiltin("kotlin.recursive.call", "Kotlin", "Recursive call", true, "RECURSIVE_CALL");

        // 18. Kubernetes
        registerBuiltin("k8s.helm.repository.actions", "Kubernetes", "Helm repository actions", true, "HELM_WHEEL");
        registerBuiltin("k8s.label.navigation", "Kubernetes", "Kubernetes label navigation", true, "K8S_WHEEL");
        registerBuiltin("k8s.overridden.chart.values", "Kubernetes", "Overridden chart values", true, "CHART_VALUES");

        // 19. Markdown
        registerBuiltin("markdown.configure.html.image", "Markdown", "Configure HTML image", true, "NONE");
        registerBuiltin("markdown.configure.markdown.image", "Markdown", "Configure Markdown image", true, "NONE");
        registerBuiltin("markdown.configure.plantuml.image", "Markdown", "Configure Markdown image", true, "NONE");
        registerBuiltin("markdown.install.plantuml", "Markdown", "Install PlantUML", true, "NONE");

        // 20. Mercurial
        registerBuiltin("mercurial.vcs.ignored.dirs", "Mercurial", "Version control ignored directories", true, "FOLDER");

        // 21. Micronaut
        registerBuiltin("micronaut.application.events", "Micronaut", "Application events", true, "HEADPHONES");
        registerBuiltin("micronaut.cacheable.operations", "Micronaut", "Cacheable operations", true, "CACHE_SERVER");
        registerBuiltin("micronaut.cdi", "Micronaut", "Contexts and dependency injection", true, "INJECTION_POINT");
        registerBuiltin("micronaut.datasource.properties", "Micronaut", "Datasource from .properties file", true, "MICRONAUT_MU");
        registerBuiltin("micronaut.datasource.yaml", "Micronaut", "Datasource from YAML file", true, "MICRONAUT_MU");
        registerBuiltin("micronaut.http.mappings", "Micronaut", "HTTP mappings", true, "HTTP_LINK");
        registerBuiltin("micronaut.management.endpoints", "Micronaut", "Management endpoints mappings", true, "ENDPOINT");
        registerBuiltin("micronaut.mongodb.mapping", "Micronaut", "Micronaut Data MongoDB mapping", true, "LEAF_DB");
        registerBuiltin("micronaut.mq.methods", "Micronaut", "Micronaut MQ methods", true, "MESSAGE_QUEUE");
        registerBuiltin("micronaut.websocket.mappings", "Micronaut", "WebSocket mappings", true, "WEBSOCKET");

        // 22. PHP
        registerBuiltin("php.extends.implements.overrides", "PHP", "Extends/Implements/Overrides", true, "NONE");
        registerBuiltin("php.guzzle.http.request", "PHP", "Guzzle HTTP request", true, "NONE");
        registerBuiltin("php.meta.declarations.exists", "PHP", "Meta declarations exists", true, "NONE");
        registerBuiltin("php.recursive.call", "PHP", "Recursive call", true, "RECURSIVE_CALL");

        // 23. Protocol Buffers
        registerBuiltin("protobuf.element.has.implementations", "Protocol Buffers", "Element has implementations", true, "NONE");
        registerBuiltin("protobuf.navigate.declaration.1", "Protocol Buffers", "Navigate to Protocol Buffers declaration", true, "NONE");
        registerBuiltin("protobuf.navigate.declaration.2", "Protocol Buffers", "Navigate to Protocol Buffers declaration", true, "NONE");

        // 24. Python
        registerBuiltin("python.web.structure", "Python", "Python web structure", true, "GLOBE");

        // 25. Quarkus
        registerBuiltin("quarkus.cacheable.operations", "Quarkus", "Cacheable operations", true, "CACHE_SERVER");
        registerBuiltin("quarkus.datasource.properties", "Quarkus", "Datasource from .properties file", true, "BLUE_STAR");
        registerBuiltin("quarkus.datasource.yaml", "Quarkus", "Datasource from YAML file", true, "BLUE_STAR");
        registerBuiltin("quarkus.scheduled.tasks", "Quarkus", "Scheduled tasks", true, "CLOCK_TIMER");

        // 26. Ruby
        registerBuiltin("ruby.action.cable", "Ruby", "Action cable", true, "FILE_TEXT");
        registerBuiltin("ruby.action.cable.client", "Ruby", "Action cable client", true, "FILE_CODE");
        registerBuiltin("ruby.factorybot.implicit.ref", "Ruby", "FactoryBot implicit reference", true, "RUBY_DIAMOND");
        registerBuiltin("ruby.factorybot.partial.decl", "Ruby", "FactoryBot partial declaration", true, "RUBY_DIAMOND");
        registerBuiltin("ruby.inherited.module", "Ruby", "Inherited module", true, "MODULE_I");
        registerBuiltin("ruby.overridden.declaration", "Ruby", "Overridden declaration", true, "OVERRIDDEN_METHOD");
        registerBuiltin("ruby.overriding.declaration", "Ruby", "Overriding declaration", true, "OVERRIDING_METHOD");
        registerBuiltin("ruby.partial.declaration", "Ruby", "Partial declaration", true, "RUBY_DIAMOND");
        registerBuiltin("ruby.rails.action", "Ruby", "Rails action", true, "RAILS_ACTION");
        registerBuiltin("ruby.rails.model", "Ruby", "Rails model", true, "RAILS_MODEL");
        registerBuiltin("ruby.rails.schema", "Ruby", "Rails schema", true, "RAILS_SCHEMA");
        registerBuiltin("ruby.rails.view", "Ruby", "Rails view", true, "RAILS_VIEW");
        registerBuiltin("ruby.rbs.overloaded.method", "Ruby", "RBS overloaded method", true, "RBS_DIAMOND");
        registerBuiltin("ruby.rbs.partial.declaration", "Ruby", "RBS partial declaration", true, "RBS_DIAMOND");

        // 27. Rust
        registerBuiltin("rust.generated.ts.declarations", "Rust", "Generated TypeScript declarations", true, "TS_BADGE");
        registerBuiltin("rust.implemented.item", "Rust", "Implemented item", true, "IMPLEMENTED_METHOD");
        registerBuiltin("rust.implementing.item", "Rust", "Implementing item", true, "IMPLEMENTING_METHOD");
        registerBuiltin("rust.open.documentation", "Rust", "Open documentation", true, "BOOK_DOC");
        registerBuiltin("rust.open.documentation.toml", "Rust", "Open documentation (TOML)", true, "BOOK_DOC");
        registerBuiltin("rust.overriding.item", "Rust", "Overriding item", true, "OVERRIDING_METHOD");
        registerBuiltin("rust.recursive.call", "Rust", "Recursive call", true, "RECURSIVE_CALL");

        // 28. Scala
        registerBuiltin("scala.companion", "Scala", "Companion", true, "CIRCLE_C");
        registerBuiltin("scala.implemented.member", "Scala", "Implemented member", true, "IMPLEMENTED_METHOD");
        registerBuiltin("scala.implementing.member", "Scala", "Implementing member", true, "IMPLEMENTING_METHOD");
        registerBuiltin("scala.implements.sam", "Scala", "Implements SAM", true, "AT_SIGN");
        registerBuiltin("scala.overridden.member", "Scala", "Overridden member", true, "OVERRIDDEN_METHOD");
        registerBuiltin("scala.overriding.member", "Scala", "Overriding member", true, "OVERRIDING_METHOD");
        registerBuiltin("scala.recursion.type", "Scala", "Recursion type", true, "RECURSIVE_CALL");

        // 29. Spring
        registerBuiltin("spring.aop.xml", "Spring", "AOP (XML)", true, "AOP");
        registerBuiltin("spring.application.events", "Spring", "Application events", true, "HEADPHONES");
        registerBuiltin("spring.autowired", "Spring", "Autowired", true, "AUTOWIRED");
        registerBuiltin("spring.bean", "Spring", "Bean", true, "SPRING_BEAN");
        registerBuiltin("spring.cacheable.operations", "Spring", "Cacheable operations with the same names", true, "CACHE_SERVER");
        registerBuiltin("spring.config.xml", "Spring", "Configuration (XML)", true, "XML_TAG");
        registerBuiltin("spring.model.dependencies.graph", "Spring", "Model dependencies graph (XML)", true, "GRAPH_ICON");
        registerBuiltin("spring.properties", "Spring", "Properties", true, "PROPERTIES_P");
        registerBuiltin("spring.scheduled.tasks", "Spring", "Scheduled tasks", true, "CLOCK_TIMER");
        registerBuiltin("spring.factories.registration", "Spring", "spring.factories registration", true, "SPRING_LEAF");
        registerBuiltin("spring.test.config", "Spring", "Test configuration", true, "TEST_LEAF");
        registerBuiltin("spring.testing.beans", "Spring", "Testing beans", true, "TESTING_BEANS");

        // 30. Spring Boot
        registerBuiltin("springboot.config.properties", "Spring Boot", "Configuration properties", true, "SPRINGBOOT_LEAF");
        registerBuiltin("springboot.datasource.properties", "Spring Boot", "Datasource from .properties file", true, "DATASOURCE_PROPERTIES");
        registerBuiltin("springboot.datasource.yaml", "Spring Boot", "Datasource from YAML file", true, "DATASOURCE_YAML");
        registerBuiltin("springboot.runtime.beans", "Spring Boot", "Runtime beans", true, "RUNTIME_BEANS");
        registerBuiltin("springboot.runtime.beans.xml", "Spring Boot", "Runtime beans (XML)", true, "RUNTIME_BEANS_XML");
        registerBuiltin("springboot.runtime.conditions", "Spring Boot", "Runtime conditions", true, "RUNTIME_CONDITIONS");
        registerBuiltin("springboot.aot.repository.methods", "Spring Boot", "Spring AOT repository methods", true, "SPRING_AOT");

        // 31. Spring Cloud
        registerBuiltin("springcloud.stream.bindings", "Spring Cloud", "Spring Cloud Stream bindings", true, "CLOUD_STREAM");

        // 32. Spring Data
        registerBuiltin("springdata.repositories", "Spring Data", "Repositories", true, "SPRINGDATA_REPO");
        registerBuiltin("springdata.run.query.console", "Spring Data", "Run a query in a console", true, "QUERY_CONSOLE");
        registerBuiltin("springdata.run.mongo.query.console", "Spring Data", "Run Mongo query in console", true, "MONGO_CONSOLE");
        registerBuiltin("springdata.jdbc.mapping", "Spring Data", "Spring Data JDBC mapping", true, "DATABASE_CYLINDER");
        registerBuiltin("springdata.mongodb.mapping", "Spring Data", "Spring Data MongoDB mapping", true, "LEAF_DB");
        registerBuiltin("springdata.projections", "Spring Data", "Spring Data projections", true, "PROJECTIONS");

        // 33. Spring Messaging
        registerBuiltin("springmessaging.queue.receiver.methods", "Spring Messaging", "Message queue receiver methods", true, "MESSAGE_QUEUE");

        // 34. Spring Web
        registerBuiltin("springweb.related.views", "Spring Web", "Related views", true, "SPRING_WEB_VIEWS");
        registerBuiltin("springweb.request.mappings", "Spring Web", "Request mappings", true, "SPRING_REQUEST_MAPPINGS");
    }

    private static void registerBuiltin(String id, String category, String name, boolean defaultEnabled, String iconType) {
        BUILTIN_DESCRIPTORS.add(new GutterIconDescriptor(id, category, name, defaultEnabled, iconType));
    }

    // Dynamic registry for builtin + extension plugin contributed descriptors
    private final Map<String, GutterIconDescriptor> descriptorsById = new LinkedHashMap<>();
    private final List<GutterIconDescriptor> allDescriptors = new ArrayList<>();

    // State
    private boolean showGutterIcons = true;
    private final Set<String> disabledIconIds = new LinkedHashSet<>();

    private final List<Consumer<GutterIconsSettings>> listeners = new CopyOnWriteArrayList<>();

    public GutterIconsSettings() {
        for (GutterIconDescriptor d : BUILTIN_DESCRIPTORS) {
            descriptorsById.put(d.id(), d);
            allDescriptors.add(d);
        }
        initDefaults();
        load();
    }

    /**
     * Allows plugins or modules to dynamically contribute new gutter icon types.
     */
    public synchronized void registerDescriptor(GutterIconDescriptor descriptor) {
        if (descriptor == null || descriptor.id() == null) return;
        if (!descriptorsById.containsKey(descriptor.id())) {
            descriptorsById.put(descriptor.id(), descriptor);
            allDescriptors.add(descriptor);
            if (!descriptor.defaultEnabled()) {
                disabledIconIds.add(descriptor.id());
            }
        }
    }

    public List<GutterIconDescriptor> getAllDescriptors() {
        return Collections.unmodifiableList(new ArrayList<>(allDescriptors));
    }

    public Map<String, List<GutterIconDescriptor>> getDescriptorsByCategory() {
        Map<String, List<GutterIconDescriptor>> map = new LinkedHashMap<>();
        for (GutterIconDescriptor d : allDescriptors) {
            map.computeIfAbsent(d.category(), k -> new ArrayList<>()).add(d);
        }
        return map;
    }

    public void initDefaults() {
        showGutterIcons = true;
        disabledIconIds.clear();
        for (GutterIconDescriptor d : allDescriptors) {
            if (!d.defaultEnabled()) {
                disabledIconIds.add(d.id());
            }
        }
    }

    public boolean isShowGutterIcons() {
        return showGutterIcons;
    }

    public void setShowGutterIcons(boolean showGutterIcons) {
        this.showGutterIcons = showGutterIcons;
    }

    public Set<String> getDisabledIconIds() {
        return Collections.unmodifiableSet(disabledIconIds);
    }

    /**
     * Checks if a gutter icon should be rendered in the editor.
     * Takes the master switch (showGutterIcons) and individual icon toggle into account.
     */
    public boolean isIconEnabled(String id) {
        if (!showGutterIcons) return false;
        return isIconConfiguredEnabled(id);
    }

    /**
     * Checks if an icon is configured enabled (regardless of whether the master switch is on).
     */
    public boolean isIconConfiguredEnabled(String id) {
        return !disabledIconIds.contains(id);
    }

    public void setIconEnabled(String id, boolean enabled) {
        if (enabled) {
            disabledIconIds.remove(id);
        } else {
            disabledIconIds.add(id);
        }
    }

    public void addListener(Consumer<GutterIconsSettings> listener) {
        listeners.add(listener);
    }

    public void removeListener(Consumer<GutterIconsSettings> listener) {
        listeners.remove(listener);
    }

    public void fireChanged() {
        for (Consumer<GutterIconsSettings> l : listeners) {
            try {
                l.accept(this);
            } catch (Throwable ignored) {}
        }
    }

    public GutterIconsSettings copy() {
        GutterIconsSettings c = new GutterIconsSettings();
        c.copyFrom(this);
        return c;
    }

    public void copyFrom(GutterIconsSettings other) {
        this.showGutterIcons = other.showGutterIcons;
        this.disabledIconIds.clear();
        this.disabledIconIds.addAll(other.disabledIconIds);
    }

    public boolean isModified(GutterIconsSettings other) {
        return this.showGutterIcons != other.showGutterIcons
                || !this.disabledIconIds.equals(other.disabledIconIds);
    }

    // ---------------------------------------------------------------- Persistence

    private boolean getBool(String key, boolean def) {
        String val = dev.lumina.util.Settings.get("editor.gutter.icons." + key);
        return val != null ? Boolean.parseBoolean(val) : def;
    }

    private void putBool(String key, boolean val) {
        dev.lumina.util.Settings.put("editor.gutter.icons." + key, String.valueOf(val));
    }

    private String getStr(String key, String def) {
        String val = dev.lumina.util.Settings.get("editor.gutter.icons." + key);
        return val != null ? val : def;
    }

    private void putStr(String key, String val) {
        dev.lumina.util.Settings.put("editor.gutter.icons." + key, val != null ? val : "");
    }

    public synchronized void load() {
        this.showGutterIcons = getBool("show", true);
        String disabledVal = getStr("disabled", null);
        if (disabledVal != null) {
            this.disabledIconIds.clear();
            if (!disabledVal.trim().isEmpty()) {
                String[] parts = disabledVal.split(",");
                for (String p : parts) {
                    String trimmed = p.trim();
                    if (!trimmed.isEmpty()) {
                        this.disabledIconIds.add(trimmed);
                    }
                }
            }
        }
    }

    public synchronized void save() {
        putBool("show", showGutterIcons);
        putStr("disabled", String.join(",", disabledIconIds));
        fireChanged();
    }
}
