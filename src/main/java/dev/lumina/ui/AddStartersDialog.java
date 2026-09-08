package dev.lumina.ui;

import dev.lumina.project.SpringInitializrMetadata;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * IntelliJ Ultimate's "Add Starters" \u2014 the same Spring Initializr
 * dependency picker as the New Project wizard's page 2, but opened from an
 * already-open pom.xml/build.gradle to add more starters to an existing
 * project. Only ever appends new {@code <dependency>}/{@code implementation}
 * entries; it never touches or removes anything already in the file, so a
 * hand-added or unrecognized dependency is always left exactly as it was.
 */
public final class AddStartersDialog {

    private record Dep(String id, String name, String description) {
    }

    private record Category(String name, List<Dep> deps) {
    }

    /** Maven coordinates plus how the dependency should be scoped. */
    private record Coordinate(String groupId, String artifactId, Scope scope) {
    }

    private enum Scope { COMPILE, RUNTIME, TEST, OPTIONAL, DEVELOPMENT_ONLY }

    // Best-confidence mapping for the common/important starters; anything
    // not listed here falls back to org.springframework.boot:spring-boot-
    // starter-<id> at compile scope, which is correct for the large
    // majority of Spring Initializr's catalog.
    private static final Map<String, Coordinate> COORDINATES = new LinkedHashMap<>();
    static {
        put("devtools", "org.springframework.boot", "spring-boot-devtools", Scope.DEVELOPMENT_ONLY);
        put("configuration-processor", "org.springframework.boot",
                "spring-boot-configuration-processor", Scope.OPTIONAL);
        put("lombok", "org.projectlombok", "lombok", Scope.OPTIONAL);
        put("docker-compose", "org.springframework.boot",
                "spring-boot-docker-compose", Scope.DEVELOPMENT_ONLY);
        put("modulith", "org.springframework.modulith", "spring-modulith-starter-core", Scope.COMPILE);
        put("postgresql", "org.postgresql", "postgresql", Scope.RUNTIME);
        put("mysql", "com.mysql", "mysql-connector-j", Scope.RUNTIME);
        put("h2", "com.h2database", "h2", Scope.RUNTIME);
        put("liquibase", "org.liquibase", "liquibase-core", Scope.RUNTIME);
        put("flyway", "org.flywaydb", "flyway-core", Scope.RUNTIME);
        put("kafka", "org.springframework.kafka", "spring-kafka", Scope.COMPILE);
        put("prometheus", "io.micrometer", "micrometer-registry-prometheus", Scope.RUNTIME);
        put("testcontainers", "org.testcontainers", "junit-jupiter", Scope.TEST);
        put("restdocs", "org.springframework.restdocs", "spring-restdocs-mockmvc", Scope.TEST);
        put("cloud-eureka", "org.springframework.cloud",
                "spring-cloud-starter-netflix-eureka-client", Scope.COMPILE);
        put("cloud-config-client", "org.springframework.cloud", "spring-cloud-starter-config", Scope.COMPILE);
        put("cloud-gateway", "org.springframework.cloud", "spring-cloud-starter-gateway", Scope.COMPILE);
        put("cloud-resilience4j", "org.springframework.cloud",
                "spring-cloud-starter-circuitbreaker-resilience4j", Scope.COMPILE);
        put("cloud-openfeign", "org.springframework.cloud", "spring-cloud-starter-openfeign", Scope.COMPILE);
    }

    private static void put(String id, String group, String artifact, Scope scope) {
        COORDINATES.put(id, new Coordinate(group, artifact, scope));
    }

    private static final List<String> FALLBACK_VERSIONS =
            List.of("4.1.1", "4.1.0", "4.0.6", "3.5.8", "3.4.12");

    // Same descriptions as the New Project wizard's offline fallback; kept
    // in sync manually since this dialog is deliberately self-contained.
    private static final List<Category> FALLBACK_CATALOG = List.of(
            new Category("Developer Tools", List.of(
                    new Dep("devtools", "Spring Boot DevTools",
                            "Provides fast application restarts, LiveReload, and configurations "
                                    + "for enhanced development experience."),
                    new Dep("lombok", "Lombok",
                            "Java annotation library which helps to reduce boilerplate code."),
                    new Dep("configuration-processor", "Spring Configuration Processor",
                            "Generate metadata for contextual help and code completion when "
                                    + "working with custom configuration keys."),
                    new Dep("docker-compose", "Docker Compose Support",
                            "Provides Docker Compose support for enhanced development experience."),
                    new Dep("modulith", "Spring Modulith",
                            "Support for building modular monolithic applications."))),
            new Category("Web", List.of(
                    new Dep("web", "Spring Web",
                            "Build web, including RESTful, applications using Spring MVC."),
                    new Dep("webflux", "Spring Reactive Web",
                            "Build reactive web applications with Spring WebFlux and Netty."),
                    new Dep("websocket", "WebSocket",
                            "Build Servlet-based WebSocket applications with SockJS and STOMP."),
                    new Dep("hateoas", "Spring HATEOAS",
                            "Eases the creation of RESTful APIs that follow the HATEOAS principle."))),
            new Category("Security", List.of(
                    new Dep("security", "Spring Security",
                            "Highly customizable authentication and access-control framework."),
                    new Dep("oauth2-client", "OAuth2 Client",
                            "Spring Security's OAuth2/OpenID Connect client support."),
                    new Dep("oauth2-resource-server", "OAuth2 Resource Server",
                            "Spring Security's OAuth2 resource server support."))),
            new Category("SQL", List.of(
                    new Dep("data-jpa", "Spring Data JPA",
                            "Persist data in SQL stores with Java Persistence API using Spring Data."),
                    new Dep("data-jdbc", "Spring Data JDBC", "Persist data with plain JDBC."),
                    new Dep("jdbc", "JDBC API",
                            "Database Connectivity API for accessing a database."),
                    new Dep("postgresql", "PostgreSQL Driver",
                            "A JDBC and R2DBC driver for PostgreSQL."),
                    new Dep("mysql", "MySQL Driver", "MySQL JDBC driver."),
                    new Dep("h2", "H2 Database",
                            "A fast in-memory database supporting JDBC and embedded mode."),
                    new Dep("liquibase", "Liquibase Migration",
                            "Liquibase database migration and source control library."),
                    new Dep("flyway", "Flyway Migration",
                            "Version control for your database schema."))),
            new Category("NoSQL", List.of(
                    new Dep("data-mongodb", "Spring Data MongoDB", "Store data in MongoDB."),
                    new Dep("data-redis", "Spring Data Redis", "Thread-safe Redis client."))),
            new Category("Messaging", List.of(
                    new Dep("amqp", "Spring for RabbitMQ", "Send and receive messages using AMQP."),
                    new Dep("kafka", "Spring for Apache Kafka",
                            "Publish, subscribe, store, and process streams of records."))),
            new Category("I/O", List.of(
                    new Dep("validation", "Validation", "Bean Validation with Hibernate validator."),
                    new Dep("cache", "Spring Cache Abstraction", "Cache-related operations."),
                    new Dep("mail", "Java Mail Sender", "Send email using Java Mail."))),
            new Category("Ops", List.of(
                    new Dep("actuator", "Spring Boot Actuator",
                            "Endpoints that let you monitor and manage your application."))),
            new Category("Testing", List.of(
                    new Dep("testcontainers", "Testcontainers",
                            "Lightweight, throwaway instances of common databases for testing."),
                    new Dep("restdocs", "Spring REST Docs",
                            "Document RESTful services with auto-generated snippets."))));

    private final Stage dialog = new Stage();
    private final ComboBox<String> versionBox = new ComboBox<>();
    private final TreeView<Object> tree = new TreeView<>();
    private final Label descTitle = new Label();
    private final Label descBody = new Label();
    private final VBox addedBox = new VBox(4);
    private final Label addedPlaceholder = new Label("No dependencies added");
    private final Set<String> selected;
    private final Set<String> alreadyPresent;
    private List<Category> categories = FALLBACK_CATALOG;

    public AddStartersDialog(Stage owner, Path buildFile, boolean gradle,
                             String detectedBootVersion, Set<String> alreadyPresentIds,
                             Runnable onApplied) {
        this.alreadyPresent = new LinkedHashSet<>(alreadyPresentIds);
        this.selected = new LinkedHashSet<>(alreadyPresentIds);

        dialog.initOwner(owner);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Add Starters");

        Label serverLabel = new Label("Server URL:");
        Label serverValue = new Label("start.spring.io");
        serverValue.getStyleClass().add("maven-link");
        HBox serverRow = new HBox(8, serverLabel, serverValue);
        serverRow.setAlignment(Pos.CENTER_LEFT);

        versionBox.setItems(FXCollections.observableArrayList(FALLBACK_VERSIONS));
        versionBox.getSelectionModel().select(
                detectedBootVersion != null && !detectedBootVersion.isBlank()
                        ? detectedBootVersion : FALLBACK_VERSIONS.get(0));
        versionBox.setDisable(true);   // matches the existing project; not switchable here
        Label versionLabel = new Label("Spring Boot:");
        HBox versionRow = new HBox(8, versionLabel, versionBox);
        versionRow.setAlignment(Pos.CENTER_LEFT);

        Label depsLabel = new Label("Dependencies:");
        depsLabel.getStyleClass().add("panel-header");
        javafx.scene.control.TextField search = new javafx.scene.control.TextField();
        search.setPromptText("Search dependencies\u2026");
        search.getStyleClass().add("dep-search");
        Label glyph = new Label("\uD83D\uDD0D");
        glyph.getStyleClass().add("dep-search-glyph");
        HBox searchRow = new HBox(6, glyph, search);
        searchRow.getStyleClass().add("dep-search-row");
        HBox.setHgrow(search, Priority.ALWAYS);

        tree.setShowRoot(false);
        tree.getStyleClass().add("dep-tree");
        tree.setCellFactory(tv -> cell());
        rebuild("");
        VBox.setVgrow(tree, Priority.ALWAYS);
        search.textProperty().addListener((o, old, v) -> rebuild(v == null ? "" : v.trim()));

        VBox left = new VBox(14, serverRow, versionRow, depsLabel, searchRow, tree);
        left.getStyleClass().add("dep-left");
        left.setPrefWidth(560);

        descTitle.getStyleClass().add("dep-description-title");
        descTitle.setWrapText(true);
        descBody.getStyleClass().add("dep-description-body");
        descBody.setWrapText(true);
        VBox descBox = new VBox(6, descTitle, descBody);
        descBox.getStyleClass().add("dep-description-box");

        Label addedTitle = new Label("Added dependencies:");
        addedTitle.getStyleClass().add("panel-header");
        addedPlaceholder.getStyleClass().add("dep-added-placeholder");
        addedBox.getStyleClass().add("dep-added-box");
        ScrollPane addedScroll = new ScrollPane(addedBox);
        addedScroll.setFitToWidth(true);
        addedScroll.getStyleClass().add("dep-added-scroll");
        VBox.setVgrow(addedScroll, Priority.ALWAYS);
        refreshAdded();

        VBox right = new VBox(16, descBox, addedTitle, addedScroll);
        right.getStyleClass().add("dep-right");
        right.setPrefWidth(300);

        HBox layout = new HBox(24, left, right);
        HBox.setHgrow(left, Priority.ALWAYS);
        layout.setPadding(new Insets(20, 24, 12, 24));

        Button cancel = new Button("Cancel");
        cancel.getStyleClass().add("dialog-secondary");
        cancel.setOnAction(e -> dialog.close());
        Button ok = new Button("OK");
        ok.getStyleClass().add("dialog-primary");
        ok.setDefaultButton(true);
        ok.setOnAction(e -> {
            Set<String> added = new LinkedHashSet<>(selected);
            added.removeAll(alreadyPresent);
            if (!added.isEmpty()) {
                applyToFile(buildFile, gradle, added);
            }
            dialog.close();
            if (!added.isEmpty() && onApplied != null) onApplied.run();
        });
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox buttons = new HBox(10, spacer, cancel, ok);
        buttons.setAlignment(Pos.CENTER_RIGHT);
        buttons.setPadding(new Insets(12, 20, 14, 20));
        buttons.getStyleClass().add("dialog-footer");

        BorderPane root = new BorderPane();
        root.getStyleClass().addAll("app-root", "new-project-dialog");
        root.setCenter(layout);
        root.setBottom(buttons);

        Scene scene = new Scene(root, 900, 660);
        if (owner.getScene() != null) {
            scene.getStylesheets().addAll(owner.getScene().getStylesheets());
        }
        dialog.setScene(scene);

        ensureLiveCatalog();
    }

    public void show() {
        dialog.showAndWait();
    }

    // ----------------------------------------------------------------- tree

    private TreeCell<Object> cell() {
        return new TreeCell<>() {
            @Override
            protected void updateItem(Object item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeAll("dep-category-cell", "dep-item-cell");
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                if (item instanceof String categoryName) {
                    setText(categoryName);
                    setGraphic(null);
                    getStyleClass().add("dep-category-cell");
                    return;
                }
                Dep dep = (Dep) item;
                CheckBox box = new CheckBox(dep.name());
                box.getStyleClass().add("dep-checkbox");
                boolean present = alreadyPresent.contains(dep.id());
                box.setSelected(selected.contains(dep.id()));
                box.setDisable(present);
                if (present) {
                    Label already = new Label("already added");
                    already.getStyleClass().add("dep-already-present");
                    HBox row = new HBox(8, box, already);
                    row.setAlignment(Pos.CENTER_LEFT);
                    setGraphic(row);
                } else {
                    box.selectedProperty().addListener((o, was, is) -> {
                        if (is) selected.add(dep.id()); else selected.remove(dep.id());
                        refreshAdded();
                    });
                    setGraphic(box);
                }
                setOnMouseEntered(e -> {
                    descTitle.setText(dep.name());
                    descBody.setText(dep.description());
                });
                getStyleClass().add("dep-item-cell");
                setText(null);
            }
        };
    }

    private void rebuild(String filter) {
        String needle = filter.toLowerCase();
        TreeItem<Object> root = new TreeItem<>("root");
        for (Category category : categories) {
            List<Dep> matches = needle.isEmpty() ? category.deps()
                    : category.deps().stream()
                            .filter(d -> d.name().toLowerCase().contains(needle))
                            .toList();
            if (matches.isEmpty()) continue;
            TreeItem<Object> categoryItem = new TreeItem<>(category.name());
            categoryItem.setExpanded(!needle.isEmpty()
                    || category.name().equals("Developer Tools"));
            for (Dep dep : matches) categoryItem.getChildren().add(new TreeItem<>(dep));
            root.getChildren().add(categoryItem);
        }
        tree.setRoot(root);
    }

    private void refreshAdded() {
        addedBox.getChildren().clear();
        if (selected.isEmpty()) {
            addedBox.getChildren().add(addedPlaceholder);
            return;
        }
        for (String id : selected) {
            Dep dep = find(id);
            Label text = new Label(dep != null ? dep.name() : id);
            text.getStyleClass().add("dep-added-label");
            HBox.setHgrow(text, Priority.ALWAYS);
            HBox row = new HBox(6, text);
            if (!alreadyPresent.contains(id)) {
                Button remove = new Button("\u00D7");
                remove.getStyleClass().add("dep-added-remove");
                remove.setOnAction(e -> {
                    selected.remove(id);
                    refreshAdded();
                    tree.refresh();
                });
                row.getChildren().add(remove);
            } else {
                Label already = new Label("already added");
                already.getStyleClass().add("dep-already-present");
                row.getChildren().add(already);
            }
            row.getStyleClass().add("dep-added-row");
            row.setAlignment(Pos.CENTER_LEFT);
            addedBox.getChildren().add(row);
        }
    }

    private Dep find(String id) {
        for (Category c : categories) {
            for (Dep d : c.deps()) if (d.id().equals(id)) return d;
        }
        return null;
    }

    // ---------------------------------------------------------- live catalog

    private void ensureLiveCatalog() {
        Thread worker = new Thread(() -> {
            try {
                SpringInitializrMetadata.Metadata metadata = SpringInitializrMetadata.fetch();
                List<Category> live = new ArrayList<>();
                for (SpringInitializrMetadata.Category c : metadata.categories()) {
                    List<Dep> deps = new ArrayList<>();
                    for (SpringInitializrMetadata.Dependency d : c.dependencies()) {
                        deps.add(new Dep(d.id(), d.name(), d.description()));
                    }
                    live.add(new Category(c.name(), deps));
                }
                Platform.runLater(() -> {
                    categories = live;
                    rebuild("");
                });
            } catch (Exception ignored) {
                // offline fallback catalog stays in place
            }
        }, "lumina-add-starters-metadata");
        worker.setDaemon(true);
        worker.start();
    }

    // -------------------------------------------------------- file editing

    /**
     * Appends new dependency entries to the build file. Only ever inserts;
     * never rewrites or removes existing content, so anything not in our
     * catalog (hand-added or unrecognized) is left completely untouched.
     */
    private void applyToFile(Path buildFile, boolean gradle, Set<String> added) {
        try {
            String text = Files.readString(buildFile);
            String updated = gradle ? insertGradle(text, added) : insertMaven(text, added);
            if (updated != null) {
                Files.writeString(buildFile, updated);
            }
        } catch (IOException ignored) {
            // the editor still has the unmodified file open; nothing was lost
        }
    }

    private Coordinate coordinateFor(String id) {
        Coordinate explicit = COORDINATES.get(id);
        if (explicit != null) return explicit;
        return new Coordinate("org.springframework.boot",
                "spring-boot-starter-" + id, Scope.COMPILE);
    }

    private String insertMaven(String text, Set<String> added) {
        int close = text.lastIndexOf("</dependencies>");
        if (close < 0) return null;
        StringBuilder block = new StringBuilder();
        for (String id : added) {
            Coordinate c = coordinateFor(id);
            block.append("\n        <dependency>\n")
                    .append("            <groupId>").append(c.groupId()).append("</groupId>\n")
                    .append("            <artifactId>").append(c.artifactId()).append("</artifactId>\n");
            switch (c.scope()) {
                case RUNTIME -> block.append("            <scope>runtime</scope>\n");
                case TEST -> block.append("            <scope>test</scope>\n");
                case OPTIONAL, DEVELOPMENT_ONLY ->
                        block.append("            <optional>true</optional>\n");
                case COMPILE -> { /* no scope tag needed */ }
            }
            block.append("        </dependency>\n");
        }
        return text.substring(0, close) + block + text.substring(close);
    }

    private String insertGradle(String text, Set<String> added) {
        int close = findGradleDependenciesClose(text);
        if (close < 0) return null;
        StringBuilder block = new StringBuilder();
        for (String id : added) {
            Coordinate c = coordinateFor(id);
            String configuration = switch (c.scope()) {
                case RUNTIME -> "runtimeOnly";
                case TEST -> "testImplementation";
                case DEVELOPMENT_ONLY -> "developmentOnly";
                case OPTIONAL -> "compileOnly";
                case COMPILE -> "implementation";
            };
            block.append("    ").append(configuration).append(" '")
                    .append(c.groupId()).append(':').append(c.artifactId()).append("'\n");
            if (c.scope() == Scope.OPTIONAL) {
                block.append("    annotationProcessor '")
                        .append(c.groupId()).append(':').append(c.artifactId()).append("'\n");
            }
        }
        return text.substring(0, close) + block + text.substring(close);
    }

    /** Offset of the closing '}' of the top-level dependencies { } block. */
    private static int findGradleDependenciesClose(String text) {
        int start = text.indexOf("dependencies {");
        if (start < 0) start = text.indexOf("dependencies{");
        if (start < 0) return -1;
        int open = text.indexOf('{', start);
        int depth = 1;
        int i = open + 1;
        while (i < text.length() && depth > 0) {
            char c = text.charAt(i);
            if (c == '{') depth++;
            else if (c == '}') {
                depth--;
                if (depth == 0) return i;
            }
            i++;
        }
        return -1;
    }
}
