package dev.lumina.ui;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import dev.lumina.project.ExpressMetadata;
import dev.lumina.project.GradleMetadata;
import dev.lumina.project.GroovyMetadata;
import dev.lumina.project.GoMetadata;
import dev.lumina.project.JdkMetadata;
import dev.lumina.project.JdkMetadata.JdkInstallation;
import dev.lumina.project.NodeMetadata;
import dev.lumina.project.ProjectSpec;
import dev.lumina.project.ReactMetadata;
import dev.lumina.project.ScalaMetadata;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.scene.Group;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextArea;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.SVGPath;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * IntelliJ-style "New Project" dialog:
 * generator list on the left, configuration form on the right.
 * Functional generators in this phase: Java and Spring Boot.
 */
public class NewProjectDialog {

    private record GeneratorEntry(String label, ProjectSpec.Generator generator, boolean enabled) {
        @Override
        public String toString() {
            return label;
        }
    }

    private record JdkEntry(String label, String detail, boolean enabled, boolean action, JdkInstallation installation) {
        public JdkEntry(String label, String detail, boolean enabled, boolean action) {
            this(label, detail, enabled, action, null);
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private record CatalogEntry(String name, String type, String location) {
    }

    private record RustTemplate(String label, String detail, String value, boolean builtIn) {
        public RustTemplate(String label, String detail, String value) {
            this(label, detail, value, !value.startsWith("Custom:"));
        }
        @Override
        public String toString() {
            return label + (detail.isBlank() ? "" : "  " + detail);
        }
    }

    /** One selectable Spring Initializr dependency. */
    private record SpringDep(String id, String label, String description) {
    }

    /** A collapsible category in the dependency tree, e.g. "Web", "SQL". */
    private record SpringDepCategory(String name, List<SpringDep> deps) {
    }

    /** One selectable JavaFX additional library matching IntelliJ IDEA's wizard. */
    private record JavaFXLib(String id, String name, String version, String description, String website) {
        public String label() {
            return name + " (" + version + ")";
        }
    }

    private static final List<JavaFXLib> JAVAFX_LIBRARIES = List.of(
            new JavaFXLib("bootstrapfx", "BootstrapFX", "0.4.0",
                    "Provides a CSS stylesheet that closely resembles the Twitter Bootstrap while being custom tailored for JavaFX's unique CSS flavor.",
                    "https://github.com/kordamp/bootstrapfx"),
            new JavaFXLib("controlsfx", "ControlsFX", "11.2.1",
                    "High quality UI controls and other tools to complement the core JavaFX distribution.",
                    "https://controlsfx.github.io/"),
            new JavaFXLib("formsfx", "FormsFX", "11.6.0",
                    "A framework for creating forms easily and effectively.",
                    "https://github.com/dlsc-software-consulting-gmbh/FormsFX"),
            new JavaFXLib("fxgl", "FXGL", "21.1",
                    "Java / JavaFX / Kotlin Game Development Framework.",
                    "https://github.com/AlmasB/FXGL"),
            new JavaFXLib("ikonli", "Ikonli", "12.3.1",
                    "Icon packs for Java applications.",
                    "https://kordamp.org/ikonli/"),
            new JavaFXLib("tilesfx", "TilesFX", "21.0.9",
                    "A JavaFX library containing tiles that can be used for dashboards.",
                    "https://github.com/HanSolo/tilesfx"),
            new JavaFXLib("validatorfx", "ValidatorFX", "0.6.1",
                    "A form validation library for JavaFX.",
                    "https://github.com/effad/ValidatorFX")
    );

    private static final List<String> FALLBACK_BOOT_VERSIONS =
            List.of("4.1.1", "4.1.0", "4.0.6", "3.5.8", "3.4.12");

    private static final List<SpringDepCategory> FALLBACK_DEP_CATALOG = List.of(
            new SpringDepCategory("Developer Tools", List.of(
                    new SpringDep("native", "GraalVM Native Support",
                            "Support for compiling Spring applications to native "
                                    + "executables using the GraalVM native-image compiler."),
                    new SpringDep("graphql-dgs-codegen", "GraphQL DGS Code Generation",
                            "Generates Java types from a GraphQL schema for use with "
                                    + "the Netflix DGS framework."),
                    new SpringDep("devtools", "Spring Boot DevTools",
                            "Provides fast application restarts, LiveReload, and "
                                    + "configurations for enhanced development experience."),
                    new SpringDep("lombok", "Lombok",
                            "Java annotation library which helps to reduce boilerplate "
                                    + "code such as getters, setters, and constructors."),
                    new SpringDep("configuration-processor", "Spring Configuration Processor",
                            "Generate metadata for developers to offer contextual help "
                                    + "and code completion when working with custom "
                                    + "configuration keys."),
                    new SpringDep("docker-compose", "Docker Compose Support",
                            "Provides Docker Compose support for enhanced development "
                                    + "experience."),
                    new SpringDep("modulith", "Spring Modulith",
                            "Support for building modular monolithic applications."))),
            new SpringDepCategory("Web", List.of(
                    new SpringDep("web", "Spring Web",
                            "Build web, including RESTful, applications using Spring "
                                    + "MVC. Uses Apache Tomcat as the default embedded container."),
                    new SpringDep("webflux", "Spring Reactive Web",
                            "Build reactive web applications with Spring WebFlux and "
                                    + "Netty."),
                    new SpringDep("graphql", "Spring for GraphQL",
                            "Build GraphQL applications with Spring for GraphQL and "
                                    + "GraphQL Java."),
                    new SpringDep("websocket", "WebSocket",
                            "Build Servlet-based WebSocket applications with SockJS "
                                    + "and STOMP."),
                    new SpringDep("web-services", "Spring Web Services",
                            "Facilitates contract-first SOAP development."),
                    new SpringDep("jersey", "Jersey",
                            "Alternative to Spring MVC with JAX-RS and better "
                                    + "multi-part form/file upload support."),
                    new SpringDep("rest-repositories", "Rest Repositories",
                            "Exposes Spring Data repositories over REST via "
                                    + "Spring Data REST."),
                    new SpringDep("hateoas", "Spring HATEOAS",
                            "Eases the creation of RESTful APIs that follow the "
                                    + "HATEOAS principle."))),
            new SpringDepCategory("Template Engines", List.of(
                    new SpringDep("thymeleaf", "Thymeleaf",
                            "A modern server-side Java template engine for both web "
                                    + "and standalone environments."),
                    new SpringDep("freemarker", "Apache Freemarker",
                            "A server-side Java template engine for both web and "
                                    + "standalone environments."),
                    new SpringDep("mustache", "Mustache",
                            "A logic-less templating engine, whose template syntax "
                                    + "is common to many programming languages."),
                    new SpringDep("groovy-templates", "Groovy Templates",
                            "A server-side Groovy template engine."))),
            new SpringDepCategory("Security", List.of(
                    new SpringDep("security", "Spring Security",
                            "Highly customizable authentication and access-control "
                                    + "framework for Spring applications."),
                    new SpringDep("oauth2-client", "OAuth2 Client",
                            "Spring Security's OAuth2/OpenID Connect client support."),
                    new SpringDep("oauth2-resource-server", "OAuth2 Resource Server",
                            "Spring Security's OAuth2 resource server support."),
                    new SpringDep("oauth2-authorization-server", "OAuth2 Authorization Server",
                            "Spring's experimental OAuth2 authorization server support."),
                    new SpringDep("ldap", "Spring LDAP",
                            "Makes it easier to build Spring-based applications that "
                                    + "use the Lightweight Directory Access Protocol."))),
            new SpringDepCategory("SQL", List.of(
                    new SpringDep("data-jpa", "Spring Data JPA",
                            "Persist data in SQL stores with Java Persistence API "
                                    + "using Spring Data and Hibernate."),
                    new SpringDep("data-jdbc", "Spring Data JDBC",
                            "Persist data in SQL stores with plain JDBC using Spring "
                                    + "Data."),
                    new SpringDep("data-r2dbc", "Spring Data R2DBC",
                            "Provides Reactive Relational Database Connectivity to "
                                    + "persist data in SQL stores using Spring Data."),
                    new SpringDep("jdbc", "JDBC API",
                            "Database Connectivity API that defines how a client may "
                                    + "access a database."),
                    new SpringDep("postgresql", "PostgreSQL Driver",
                            "A JDBC and R2DBC driver that allows Java programs to "
                                    + "connect to a PostgreSQL database."),
                    new SpringDep("mysql", "MySQL Driver",
                            "MySQL JDBC driver."),
                    new SpringDep("h2", "H2 Database",
                            "Provides a fast in-memory database that supports JDBC "
                                    + "API and embedded mode."),
                    new SpringDep("liquibase", "Liquibase Migration",
                            "Liquibase database migration and source control library."),
                    new SpringDep("flyway", "Flyway Migration",
                            "Version control for your database so you can migrate "
                                    + "with ease and confidence."))),
            new SpringDepCategory("NoSQL", List.of(
                    new SpringDep("data-mongodb", "Spring Data MongoDB",
                            "Store data in flexible, JSON-like documents using "
                                    + "Spring Data MongoDB."),
                    new SpringDep("data-redis", "Spring Data Redis",
                            "Advanced and thread-safe Java Redis client for "
                                    + "monitoring, pooling and pipelining."),
                    new SpringDep("data-elasticsearch", "Spring Data Elasticsearch",
                            "A distributed, RESTful search and analytics engine "
                                    + "via Spring Data Elasticsearch."),
                    new SpringDep("data-cassandra", "Spring Data Cassandra",
                            "A distributed NoSQL database via Spring Data "
                                    + "Cassandra."))),
            new SpringDepCategory("Messaging", List.of(
                    new SpringDep("amqp", "Spring for RabbitMQ",
                            "Gives your applications a common platform to send and "
                                    + "receive messages using AMQP."),
                    new SpringDep("kafka", "Spring for Apache Kafka",
                            "Publish, subscribe, store, and process streams of "
                                    + "records via Spring for Apache Kafka."),
                    new SpringDep("artemis", "Spring for Artemis",
                            "Gives your applications a common platform to send and "
                                    + "receive messages using Apache ActiveMQ Artemis."))),
            new SpringDepCategory("I/O", List.of(
                    new SpringDep("validation", "Validation",
                            "Bean Validation with Hibernate validator."),
                    new SpringDep("batch", "Spring Batch",
                            "Boot-strap your batch applications with helpful "
                                    + "autoconfiguration."),
                    new SpringDep("cache", "Spring Cache Abstraction",
                            "Provides cache-related operations, such as the update "
                                    + "of content in the cache."),
                    new SpringDep("mail", "Java Mail Sender",
                            "Send email using Java Mail and Spring Framework's "
                                    + "JavaMailSender."))),
            new SpringDepCategory("Ops", List.of(
                    new SpringDep("actuator", "Spring Boot Actuator",
                            "Supports built in (or custom) endpoints that let you "
                                    + "monitor and manage your application."))),
            new SpringDepCategory("Observability", List.of(
                    new SpringDep("prometheus", "Prometheus",
                            "Expose actuator metrics in a format that can be "
                                    + "scraped by a Prometheus server."),
                    new SpringDep("zipkin", "Distributed Tracing (Zipkin)",
                            "Enable and report request traces for distributed "
                                    + "tracing with Zipkin."))),
            new SpringDepCategory("Testing", List.of(
                    new SpringDep("testcontainers", "Testcontainers",
                            "Provide lightweight, throwaway instances of common "
                                    + "databases or anything else that can run in a "
                                    + "Docker container for testing."),
                    new SpringDep("restdocs", "Spring REST Docs",
                            "Document RESTful services by combining hand-written "
                                    + "documentation with auto-generated snippets "
                                    + "produced with Spring MVC Test."),
                    new SpringDep("cloud-contract-verifier", "Spring Cloud Contract Verifier",
                            "Moves TDD to the level of software architecture, "
                                    + "verifying that services adhere to a shared "
                                    + "contract."))),
            new SpringDepCategory("Spring Cloud", List.of(
                    new SpringDep("cloud-eureka", "Eureka Discovery Client",
                            "A REST based service for locating services for the "
                                    + "purpose of load balancing and failover."),
                    new SpringDep("cloud-config-client", "Config Client",
                            "Client that connects to a Spring Cloud Config Server "
                                    + "to fetch remote configuration properties."),
                    new SpringDep("cloud-gateway", "Gateway",
                            "Provides a simple, effective way to route to APIs and "
                                    + "provide cross-cutting concerns to them."),
                    new SpringDep("cloud-resilience4j", "Resilience4j",
                            "Circuit breaker, retry and rate limiting for Spring "
                                    + "Boot applications, backed by Resilience4j."),
                    new SpringDep("cloud-openfeign", "OpenFeign",
                            "Declarative REST client via Spring Cloud OpenFeign."))));

    private static class PropertyEntry {
        private final StringProperty name = new SimpleStringProperty("");
        private final StringProperty value = new SimpleStringProperty("");

        public StringProperty nameProperty() {
            return name;
        }

        public StringProperty valueProperty() {
            return value;
        }

        public String getName() {
            return name.get();
        }

        public void setName(String name) {
            this.name.set(name);
        }

        public String getValue() {
            return value.get();
        }

        public void setValue(String value) {
            this.value.set(value);
        }
    }

    private static final List<GeneratorEntry> NEW_PROJECT_ENTRIES = List.of(
            new GeneratorEntry("Java", ProjectSpec.Generator.JAVA, true),
            new GeneratorEntry("Kotlin", ProjectSpec.Generator.KOTLIN, true),
            new GeneratorEntry("Groovy", ProjectSpec.Generator.GROOVY, true),
            new GeneratorEntry("Scala", ProjectSpec.Generator.SCALA, true),
            new GeneratorEntry("Python", ProjectSpec.Generator.PYTHON, true),
            new GeneratorEntry("PHP", ProjectSpec.Generator.PHP, true),
            new GeneratorEntry("Ruby", ProjectSpec.Generator.RUBY, true),
            new GeneratorEntry("Rust", ProjectSpec.Generator.RUST, true),
            new GeneratorEntry("Go", ProjectSpec.Generator.GO, true),
            new GeneratorEntry("Empty Project", ProjectSpec.Generator.EMPTY_PROJECT, true));

    private static final List<GeneratorEntry> GENERATOR_ENTRIES = List.of(
            new GeneratorEntry("Maven Archetype", ProjectSpec.Generator.MAVEN_ARCHETYPE, true),
            new GeneratorEntry("Spring Boot", ProjectSpec.Generator.SPRING_BOOT, true),
            new GeneratorEntry("JavaFX", ProjectSpec.Generator.JAVAFX, true),
            new GeneratorEntry("Quarkus", ProjectSpec.Generator.QUARKUS, true),
            new GeneratorEntry("Micronaut", ProjectSpec.Generator.MICRONAUT, true),
            new GeneratorEntry("Jakarta EE", ProjectSpec.Generator.JAKARTA_EE, true),
            new GeneratorEntry("Ktor", ProjectSpec.Generator.KTOR, true),
            new GeneratorEntry("HTML", ProjectSpec.Generator.HTML, true),
            new GeneratorEntry("React", ProjectSpec.Generator.REACT, true),
            new GeneratorEntry("Express", ProjectSpec.Generator.EXPRESS, true),
            new GeneratorEntry("Angular CLI", ProjectSpec.Generator.ANGULAR_CLI, true),
            new GeneratorEntry("Vue.js", ProjectSpec.Generator.VUE, true),
            new GeneratorEntry("Vite", ProjectSpec.Generator.VITE, true),
            new GeneratorEntry("Nuxt", ProjectSpec.Generator.NUXT, true));

    private final Stage stage = new Stage();
    private final Consumer<ProjectSpec> onCreate;

    // form controls
    private final TextField nameField = new TextField("untitled");
    private final TextField locationField = new TextField(
            System.getProperty("user.home") + File.separator + "projects" + File.separator + "others");
    private final Label locationHint = new Label();
    private final CheckBox gitCheck = new CheckBox("Create Git repository");
    private final CheckBox sampleCodeCheck = new CheckBox("Add sample code");
    private final CheckBox angularStandaloneCheck = new CheckBox("Create new project with standalone components");
    private final CheckBox angularDefaultsCheck = new CheckBox("Use the default project setup");
    private final ToggleGroup buildGroup = new ToggleGroup();
    private final ComboBox<JdkEntry> jdkCombo = new ComboBox<>();
    private final TextField groupField = new TextField("com.example");
    private final TextField artifactField = new TextField("untitled");
    private final TextField packageField = new TextField("org.example.untitled");
    private final ComboBox<String> javaVersionBox =
            new ComboBox<>(FXCollections.observableArrayList("25", "21", "17"));
    private final ComboBox<String> groovySdkBox = new ComboBox<>(
            FXCollections.observableArrayList(GroovyMetadata.FALLBACK_VERSIONS));
    private final ComboBox<String> nodeRuntimeBox = new ComboBox<>(
            FXCollections.observableArrayList("node  /usr/bin/node                         22.23.1"));
    private final ComboBox<String> angularCliBox = new ComboBox<>(
            FXCollections.observableArrayList("npx --package @angular/cli ng                         22.1.1"));
    private final ComboBox<String> viteBox = new ComboBox<>(
            FXCollections.observableArrayList("npx create-vite                                                    9.1.2"));
    private final ComboBox<String> viteTemplateBox = new ComboBox<>(
            FXCollections.observableArrayList("React", "Vue", "Vanilla", "Svelte"));
    private final TextField webParametersField = new TextField();
    private final ComboBox<String> gradleVersionBox = new ComboBox<>();
    private final CheckBox saveSettingsCheck = new CheckBox("Use these settings for future projects");
    private final CheckBox multiModuleCheck = new CheckBox("Generate multi-module build");
    private final Label gradleDslLabel = formLabel("Gradle DSL:");
    private final ToggleGroup gradleDslGroup = new ToggleGroup();
    private final HBox gradleDslRow = new HBox();
    private final Label gradleDistributionLabel = formLabel("Gradle distribution:");
    private final ComboBox<String> gradleDistributionBox = new ComboBox<>(
            FXCollections.observableArrayList("Wrapper", "Local installation"));
    private final Label gradleVersionLabel = formLabel("Gradle version:");
    private final CheckBox gradleAutoSelectCheck = new CheckBox("Auto-select");
    private final HBox gradleVersionRow = new HBox(16);
    private final Label gradleLocationLabel = formLabel("Gradle location:");
    private final TextField gradleLocationField = new TextField();
    private final Button gradleLocationBrowseButton = new Button();
    private final HBox gradleLocationRow = new HBox(6);
    private final GridPane javaAdvGrid = new GridPane();
    private boolean updatingGradleVersion = false;
    private final ToggleButton langJava = new ToggleButton("Java");
    private final ToggleButton langKotlin = new ToggleButton("Kotlin");
    private final ToggleButton langGroovy = new ToggleButton("Groovy");
    private final ToggleGroup languageGroup = new ToggleGroup();

    private final ComboBox<String> catalogCombo = new ComboBox<>(FXCollections.observableArrayList(
            "Internal", "Default Local", "Maven Central"));
    private final Button manageCatalogsButton = new Button("Manage catalogs...");
    private final ComboBox<String> archetypeCombo = new ComboBox<>(FXCollections.observableArrayList(
            "maven-archetype-quickstart", "maven-archetype-webapp", "maven-archetype-site"));
    private final Button addArchetypeButton = new Button("Add...");
    private final ComboBox<String> archetypeVersionBox = new ComboBox<>(FXCollections.observableArrayList(
            "1.4", "1.0", "1.1", "1.3"));
    private final TextField mavenGroupField = new TextField("com.example");
    private final TextField mavenArtifactField = new TextField("demo");
    private final TextField projectVersionField = new TextField("1.0-SNAPSHOT");
    private final TableView<PropertyEntry> propertiesTable = new TableView<>();
    private final ObservableList<PropertyEntry> additionalProperties = FXCollections.observableArrayList();
    private final VBox mavenArchetypeBox = new VBox(14);
    private final VBox rustBox = new VBox(14);
    private final List<Node> standardOnlyNodes = new ArrayList<>();
    private final List<Node> webOnlyNodes = new ArrayList<>();
    private final List<Node> viteOnlyNodes = new ArrayList<>();
    private final List<Node> angularOnlyNodes = new ArrayList<>();
    private final List<Node> sampleCodeNodes = new ArrayList<>();
    private final List<Node> groovyOnlyNodes = new ArrayList<>();
    private final List<Node> emptyOnlyNodes = new ArrayList<>();
    private final List<Node> javafxOnlyNodes = new ArrayList<>();
    private final List<Node> javafxHiddenNodes = new ArrayList<>();
    private final List<Node> languageNodes = new ArrayList<>();
    private final Label emptyDescription = new Label("A basic project with free structure.");
    private final HBox kotlinInfoBox = new HBox(4);
    private final VBox generatorSpecificBox = new VBox();
    private final List<Node> springOnlyNodes = new ArrayList<>();
    private final List<Node> jdkNodes = new ArrayList<>();
    // Rust controls
    private final Label rustToolchainLabel = formLabel("Toolchain location:");
    private final ComboBox<String> rustToolchainBox = new ComboBox<>();
    private final Button rustToolchainBrowseBtn = new Button("…");
    private final HBox rustToolchainRow = new HBox(8);

    private final Label rustVersionTitleLabel = formLabel("Toolchain version:");
    private final Label rustVersionLabel = new Label();

    private final Label rustStdlibLabel = formLabel("Standard library:");
    private final TextField rustStdlibField = new TextField();
    private final Button rustStdlibBrowseBtn = new Button();
    private final HBox rustStdlibRow = new HBox(8);

    private final Label rustEnvironmentLabel = formLabel("Environment variables:");
    private final TextField rustEnvironmentField = new TextField();
    private final Button rustEnvironmentBtn = new Button();
    private final HBox rustEnvironmentRow = new HBox(8);

    private final HBox rustTemplateHeaderRow = new HBox(8);
    private final ListView<RustTemplate> rustTemplateList = new ListView<>();
    private final ObservableList<RustTemplate> rustTemplates = FXCollections.observableArrayList();
    private final HBox rustTemplateToolbar = new HBox(5);
    private final VBox rustTemplateContainer = new VBox();
    private final Hyperlink rustInstallCargoGenerateLink = new Hyperlink("Install cargo-generate using Cargo");
    private final Label rustCargoGenerateStatusLabel = new Label();
    private final HBox rustCargoGenerateBox = new HBox(8, rustInstallCargoGenerateLink, rustCargoGenerateStatusLabel);

    // Go controls
    private final Label goRootLabel = formLabel("GOROOT:");
    private final ComboBox<GoMetadata.GoSdk> goRootBox = new ComboBox<>();
    private final Button goAddSdkBtn = new Button("Add SDK…");
    private final HBox goRootRow = new HBox(8);
    private final CheckBox goVendoringCheck = new CheckBox("Enable vendoring support automatically");
    private final Label goVendoringHelp = new Label("?");
    private final HBox goVendoringRow = new HBox(4);
    private final Label goEnvironmentLabel = formLabel("Environment:");
    private final TextField goEnvironmentField = new TextField();
    private final Button goEnvironmentBtn = new Button();
    private final HBox goEnvironmentRow = new HBox(8);
    private final Label goEnvironmentSubtext = new Label("GOPROXY, GOPRIVATE, and other environment variables");
    private final CheckBox goSampleCodeCheck = new CheckBox("Add sample code");

    private final ToggleButton typeGradleGroovy = new ToggleButton("Gradle - Groovy");
    private final ToggleButton typeGradleKotlin = new ToggleButton("Gradle - Kotlin");
    private final ToggleButton typeMaven = new ToggleButton("Maven");
    private final ToggleGroup typeGroup = new ToggleGroup();

    private final ToggleButton packagingJar = new ToggleButton("Jar");
    private final ToggleButton packagingWar = new ToggleButton("War");
    private final ToggleGroup packagingGroup = new ToggleGroup();

    private final ToggleButton configProperties = new ToggleButton("Properties");
    private final ToggleButton configYaml = new ToggleButton("YAML");
    private final ToggleGroup configGroup = new ToggleGroup();

    private final Label serverUrlLabel = new Label("start.spring.io");
    private final Button serverSettingsButton = new Button("\u2699");
    private final HBox serverRow = new HBox(8);
    private final HBox typeRow = new HBox(8);
    private final HBox packagingRow = new HBox(8);
    private final HBox configRow = new HBox(8);
    private final HBox languageRow = new HBox(8);
    private final HBox buildSystemRow = new HBox(8);

    private final GridPane formGrid = new GridPane();
    private final ColumnConstraints formCol0 = new ColumnConstraints(110);
    private final ColumnConstraints formCol1 = new ColumnConstraints();
    private final Label serverLabel = formLabel("Server URL:");
    private final Label nameLabel = formLabel("Name:");
    private final Label locationLabel = formLabel("Location:");
    private final Button browseLocationButton = new Button();
    private final HBox locationRow = new HBox(8);
    private final Label languageLabel = formLabel("Language:");
    private final Label buildSystemLabel = formLabel("Build system:");
    private final Label packagingLabel = formLabel("Packaging:");
    private final Label configLabel = formLabel("Configuration:");
    private final Label jdkLabel = formLabel("JDK:");
    private final Label javaLabel = formLabel("Java:");
    private final Label groovySdkLabel = formLabel("Groovy SDK:");

    // Scala controls
    private final ToggleGroup scalaBuildGroup = new ToggleGroup();
    private final HBox scalaBuildSystemRow = new HBox(8);
    private final Label sbtLabel = formLabel("sbt:");
    private final ComboBox<String> sbtVersionBox = new ComboBox<>();
    private final CheckBox sbtDownloadSourcesCheck = new CheckBox("Download sources");
    private final HBox sbtRow = new HBox(12);
    private final Label scalaVersionLabel = formLabel("Scala:");
    private final ComboBox<String> scalaVersionBox = new ComboBox<>();
    private final CheckBox scalaDownloadSourcesCheck = new CheckBox("Download sources");
    private final HBox scalaVersionRow = new HBox(12);
    private final CheckBox scalaOptionalBracesCheck = new CheckBox("Use significant indentation syntax (Optional Braces)");
    private final Label scalaPackagePrefixLabel = formLabel("Package prefix:");
    private final TextField scalaPackagePrefixField = new TextField();
    private final TextField scalaModuleNameField = new TextField();

    // Python controls
    private final Label interpreterTypeLabel = formLabel("Interpreter type:");
    private final ToggleGroup pythonInterpreterGroup = new ToggleGroup();
    private final HBox pythonInterpreterRow = new HBox(8);
    private final Label pythonVersionLabel = formLabel("Python version:");
    private final ComboBox<dev.lumina.project.PythonMetadata.PythonInstallation> pythonVersionCombo = new ComboBox<>();
    private final Button pythonBrowseBtn = new Button();
    private final HBox pythonVersionRow = new HBox(8);
    private final Label pythonVenvHintLabel = new Label();

    private final ComboBox<String> uvPythonVersionCombo = new ComboBox<>();
    private final Label pathToUvLabel = formLabel("Path to uv:");
    private final TextField uvPathField = new TextField();
    private final Label uvPathCheckIcon = new Label("✓");
    private final Button uvBrowseBtn = new Button();
    private final HBox uvPathRow = new HBox(8);
    private final Label uvHintLabel = new Label();

    // Base conda controls
    private final HBox condaWarningBanner = new HBox(8);
    private final Label condaWarningIcon = new Label("⚠");
    private final Label condaWarningText = new Label("No conda executable found");
    private final Hyperlink condaInstallLink = new Hyperlink("Install Miniconda");
    private final Hyperlink condaSelectPathLink = new Hyperlink("Select path");
    private final Label condaErrorCallout = new Label("Executable is not detected");
    private final ProgressIndicator condaInstallSpinner = new ProgressIndicator();
    private final Label condaInstallStatus = new Label();
    private final Hyperlink condaCancelInstallLink = new Hyperlink("Cancel");
    private final Hyperlink condaOpenWebLink = new Hyperlink("Download in browser ↗");
    private volatile CompletableFuture<Path> activeCondaInstallFuture = null;
    private final Label pathToCondaLabel = formLabel("Path to conda:");
    private final TextField condaPathField = new TextField();
    private final Button condaBrowseBtn = new Button();
    private final HBox condaPathRow = new HBox(8);
    private final Label condaSubtextLabel = new Label("To create a new conda environment or choose an existing one, proceed with Custom environment");
    private final ComboBox<String> condaPythonVersionCombo = new ComboBox<>();

    // Custom environment controls
    private final Label customEnvLabel = formLabel("Environment:");
    private final ToggleGroup customEnvGroup = new ToggleGroup();
    private final RadioButton customEnvGenerateNewRadio = new RadioButton("Generate new");
    private final RadioButton customEnvSelectExistingRadio = new RadioButton("Select existing");
    private final HBox customEnvRadioRow = new HBox(16);

    private final Label customTypeLabel = formLabel("Type:");
    private final ComboBox<String> customTypeCombo = new ComboBox<>();

    private final Label customBasePythonLabel = formLabel("Base Python:");
    private final ComboBox<dev.lumina.project.PythonMetadata.PythonInstallation> customBasePythonCombo = new ComboBox<>();
    private final Button customBasePythonBrowseBtn = new Button();
    private final HBox customBasePythonRow = new HBox(8);

    private final Label customLocationLabel = formLabel("Location:");
    private final TextField customLocationField = new TextField();
    private final Label customLocationCheckIcon = new Label("✓");
    private final Button customLocationBrowseBtn = new Button();
    private final HBox customLocationRow = new HBox(8);
    private boolean customLocationFieldEdited = false;

    private final CheckBox inheritPackagesCheck = new CheckBox("Inherit packages from base interpreter");
    private final CheckBox makeAvailableCheck = new CheckBox("Make available to all projects");

    private final Label customPythonPathLabel = formLabel("Python path:");
    private final ComboBox<dev.lumina.project.PythonMetadata.PythonInstallation> customPythonPathCombo = new ComboBox<>();
    private final Button customPythonPathBrowseBtn = new Button();
    private final HBox customPythonPathRow = new HBox(8);

    // PHP controls
    private final CheckBox phpAddComposerJsonCheck = new CheckBox("Add 'composer.json'");

    // Ruby controls
    private final Label rubyInterpreterLabel = formLabel("Ruby Interpreter:");
    private final ComboBox<dev.lumina.project.RubyMetadata.RubyInstallation> rubyInterpreterCombo = new ComboBox<>();
    private final Button rubyAddInterpreterBtn = new Button();
    private final StackPane rubyInterpreterPane = new StackPane();
    private final CheckBox rubyAddSampleCodeCheck = new CheckBox("Add sample code");

    private final Label jakartaTemplateLabel = formLabel("Template:");
    private final Label jakartaServerLabel = formLabel("Application server:");
    private final HBox jakartaServerRow = new HBox(8);
    private final Label nodeLabel = formLabel("Node runtime:");
    private final Label angularCliLabel = formLabel("Angular CLI:");
    private final Label webParametersLabel = formLabel("Additional parameters:");
    private final HBox nodeRow = new HBox(8);
    private final HBox cliRow = new HBox(8);
    private final HBox viteRow = new HBox(8);
    private final Label viteLabel = formLabel("Vite:");
    private final Label viteTemplateLabel = formLabel("Template:");
    private final CheckBox viteTypescriptCheck = new CheckBox("Use TypeScript template");
    private final Label ktorEngineLabel = formLabel("Engine:");
    private final Label packageLabel = formLabel("Package name:");

    private final TextField dependenciesField = new TextField();
    private final Label errorLabel = new Label();

    private final List<Node> serverNodes = new ArrayList<>();
    private final List<Node> typeNodes = new ArrayList<>();
    private final List<Node> springConfigNodes = new ArrayList<>();
    private final List<Node> buildSystemNodes = new ArrayList<>();
    private final List<Node> packageNodes = new ArrayList<>();
    private Label typeLabel;
    private Node sidebar;

    // ---- Quarkus extensions page (page 2 of the wizard) ----
    private String quarkusServerUrl = dev.lumina.project.QuarkusMetadata.DEFAULT_SERVER_URL;
    private final ComboBox<dev.lumina.project.QuarkusMetadata.QuarkusStream> quarkusStreamBox = new ComboBox<>(
            FXCollections.observableArrayList(dev.lumina.project.QuarkusMetadata.FALLBACK_STREAMS));
    private final TextField quarkusSearchField = new TextField();
    private final Label quarkusCatalogStatus = new Label();
    private List<dev.lumina.project.QuarkusMetadata.QuarkusCategory> quarkusCategories = dev.lumina.project.QuarkusMetadata.FALLBACK_CATEGORIES;
    private static volatile dev.lumina.project.QuarkusMetadata.QuarkusCatalog cachedQuarkusCatalog;
    private static volatile boolean quarkusFetchFailed;
    private boolean quarkusFetchStarted;
    private final TreeView<Object> quarkusTree = new TreeView<>();
    private final Set<String> selectedQuarkusExtIds = new LinkedHashSet<>();
    private final Label quarkusDetailTitle = new Label();
    private final Label quarkusDetailDesc = new Label();
    private final Hyperlink quarkusGuideLink = new Hyperlink("Guide \u2197");
    private final VBox quarkusAddedBox = new VBox(4);
    private final Label quarkusNoExtensions = new Label("No extensions added");
    private BorderPane quarkusDepsPage;
    private boolean onQuarkusDepsPage;

    // ---- Jakarta EE dependency-picker page (page 2 of the wizard) ----
    private final ComboBox<String> jakartaTemplateBox = new ComboBox<>(FXCollections.observableArrayList(
            dev.lumina.project.JakartaMetadata.SUPPORTED_TEMPLATES));
    private final ComboBox<String> jakartaAppServerBox = new ComboBox<>(FXCollections.observableArrayList(
            "<No application server>", "GlassFish 7.x", "WildFly 27.x+", "Apache Tomcat 10.x+", "Payara 6.x"));
    private final Button newAppServerButton = new Button("New...");
    private final List<Node> jakartaOnlyNodes = new ArrayList<>();
    private final Set<String> selectedJakartaDepIds = new LinkedHashSet<>();
    private final ComboBox<String> jakartaVersionBox = new ComboBox<>(
            FXCollections.observableArrayList(dev.lumina.project.JakartaMetadata.SUPPORTED_VERSIONS));
    private final TextField jakartaSearchField = new TextField();
    private final TreeView<Object> jakartaTree = new TreeView<>();
    private final Label jakartaDetailTitle = new Label();
    private final Label jakartaDetailDesc = new Label();
    private final Hyperlink jakartaWebLink = new Hyperlink("Web site \u2197");
    private final Hyperlink jakartaSpecLink = new Hyperlink("Specification \u2197");
    private final VBox jakartaAddedBox = new VBox(4);
    private final Label jakartaNoDependencies = new Label("No dependencies selected");
    private BorderPane jakartaDepsPage;
    private boolean onJakartaDepsPage;

    // ---- Micronaut features page (page 2 of the wizard) ----
    private record MicronautAppType(String label, String value) {
        @Override
        public String toString() {
            return label;
        }
    }

    private final ToggleButton testJUnit = new ToggleButton("JUnit");
    private final ToggleButton testKotest = new ToggleButton("Kotest");
    private final ToggleButton testSpock = new ToggleButton("Spock");
    private final ToggleGroup testGroup = new ToggleGroup();
    private final HBox testFrameworkRow = new HBox(8);
    private Label testFrameworkLabel;

    private final ComboBox<MicronautAppType> micronautAppTypeBox = new ComboBox<>(
            FXCollections.observableArrayList(
                    new MicronautAppType("Application", "default"),
                    new MicronautAppType("CLI Application", "cli"),
                    new MicronautAppType("Function", "function"),
                    new MicronautAppType("gRPC Application", "grpc"),
                    new MicronautAppType("Messaging-Driven Application", "messaging")
            )
    );
    private Label micronautAppTypeLabel;
    private final List<Node> micronautStep1Nodes = new ArrayList<>();

    private String micronautServerUrl = dev.lumina.project.MicronautMetadata.DEFAULT_SERVER_URL;
    private String micronautVersion = dev.lumina.project.MicronautMetadata.DEFAULT_VERSION;
    private final Label micronautVersionHeaderLabel = new Label("Micronaut: " + dev.lumina.project.MicronautMetadata.DEFAULT_VERSION);
    private final TextField micronautSearchField = new TextField();
    private final Label micronautCatalogStatus = new Label();
    private List<dev.lumina.project.MicronautMetadata.MicronautCategory> micronautCategories = dev.lumina.project.MicronautMetadata.FALLBACK_CATEGORIES;
    private static volatile dev.lumina.project.MicronautMetadata.MicronautCatalog cachedMicronautCatalog;
    private static volatile boolean micronautFetchFailed;
    private boolean micronautFetchStarted;
    private final TreeView<Object> micronautTree = new TreeView<>();
    private final Set<String> selectedMicronautFeatureIds = new LinkedHashSet<>();
    private final Label micronautDetailTitle = new Label();
    private final Label micronautDetailDesc = new Label();
    private final VBox micronautAddedBox = new VBox(4);
    private final Label micronautNoFeatures = new Label("No features added");
    private BorderPane micronautFeaturesPage;
    private boolean onMicronautFeaturesPage;

    // ---- Ktor Generator fields & Step 2 Plugins page ----
    private String ktorServerUrl = dev.lumina.project.KtorMetadata.DEFAULT_SERVER_URL;
    private final ComboBox<String> ktorEngineBox = new ComboBox<>(FXCollections.observableArrayList(
            "Netty  Default", "CIO", "Tomcat", "Jetty"));
    private final CheckBox ktorAddSampleCodeCheck = new CheckBox("Add sample code");
    private Label ktorTutorialsLabel;
    private HBox ktorAdvancedToggle;
    private Label ktorAdvancedArrow;
    private VBox ktorAdvancedContainer;
    private boolean isKtorAdvancedExpanded = false;
    private final ToggleGroup ktorBuildGroup = new ToggleGroup();
    private final RadioButton ktorBuildGradle = new RadioButton("Gradle");
    private final RadioButton ktorBuildKotlin = new RadioButton("Kotlin");
    private final RadioButton ktorBuildMaven = new RadioButton("Maven");
    private final HBox ktorBuildSystemRow = new HBox();
    private final ComboBox<String> ktorVersionBox = new ComboBox<>(FXCollections.observableArrayList(
            "3.5.2  Default", "3.1.1", "3.0.3", "2.3.13"));
    private final ComboBox<String> ktorConfigInBox = new ComboBox<>(FXCollections.observableArrayList(
            "YAML File  Default", "HOCON file", "Code in application.kt"));
    private final List<Node> ktorStep1Nodes = new ArrayList<>();

    private BorderPane ktorPluginsPage;
    private boolean onKtorPluginsPage;
    private final TextField ktorSearchField = new TextField();
    private final Label ktorPluginsCountLabel = new Label("0 plugins added");
    private final Hyperlink ktorShowAddedLink = new Hyperlink("Show");
    private boolean ktorShowOnlyAdded = false;
    private final VBox ktorCardsBox = new VBox(4);
    private final Set<String> selectedKtorPluginIds = new LinkedHashSet<>();
    private final List<dev.lumina.project.KtorMetadata.KtorPlugin> ktorPluginsList = new ArrayList<>(dev.lumina.project.KtorMetadata.FALLBACK_PLUGINS);
    private dev.lumina.project.KtorMetadata.KtorPlugin selectedKtorPlugin;
    private Label ktorDetailName;
    private Hyperlink ktorDetailVendor;
    private Label ktorDetailVersion;
    private Hyperlink ktorDetailGithub;
    private Button ktorDetailActionBtn;
    private VBox ktorDetailContentBox;
    private boolean ktorFetchStarted;

    // ---- HTML generator fields (HTML5 Boilerplate / Bootstrap) ----
    private final ToggleGroup htmlProjectTypeGroup = new ToggleGroup();
    private final ComboBox<String> htmlVersionBox = new ComboBox<>();
    private final Button htmlRefreshButton = new Button("\u27F3");
    private boolean htmlInitialized = false;
    private volatile boolean htmlFetching = false;

    // ---- React generator fields (React / React Native / Next.js) ----
    private final ToggleGroup reactProjectTypeGroup = new ToggleGroup();
    private final ComboBox<String> reactNodeInterpreterBox = new ComboBox<>();
    private final Button reactNodeBrowseBtn = new Button("\u2026");
    private final ComboBox<String> reactCliBox = new ComboBox<>();
    private final Button reactCliBrowseBtn = new Button("\u2026");
    private final Label reactCliLabel = new Label("create-react-app:");
    private final CheckBox reactTsCheck = new CheckBox("Create TypeScript project");
    private final VBox reactAdvisoryBox = new VBox(6);
    private boolean reactInitialized = false;
    private String lastValidNodeInterpreter = "";

    // ---- Express generator fields ----
    private final ComboBox<String> expressNodeInterpreterBox = new ComboBox<>();
    private final Button expressNodeBrowseBtn = new Button("\u2026");
    private final ComboBox<String> expressCliBox = new ComboBox<>();
    private final Button expressCliBrowseBtn = new Button("\u2026");
    private final ComboBox<String> expressViewEngineBox = new ComboBox<>();
    private final ComboBox<String> expressStylesheetEngineBox = new ComboBox<>();
    private boolean expressInitialized = false;
    private String lastValidExpressNodeInterpreter = "";

    // ---- Spring Boot dependency-picker page (page 2 of the wizard) ----
    private final ComboBox<String> springBootVersionBox = new ComboBox<>(
            FXCollections.observableArrayList(FALLBACK_BOOT_VERSIONS));
    private final TextField depSearchField = new TextField();
    private final Label catalogStatus = new Label(
            "Loading current versions \u0026 dependencies from start.spring.io\u2026");
    /** Live categories once fetched; falls back to the bundled list until then. */
    private List<SpringDepCategory> depCategories = FALLBACK_DEP_CATALOG;
    private static volatile dev.lumina.project.SpringInitializrMetadata.Metadata
            cachedMetadata;
    private static volatile boolean metadataFetchFailed;
    private boolean metadataFetchStarted;
    private final TreeView<Object> depTree = new TreeView<>();
    private final Set<String> selectedDepIds = new LinkedHashSet<>();
    private final Label depDescriptionTitle = new Label();
    private final Label depDescriptionBody = new Label();
    private final VBox addedDepsBox = new VBox(4);
    private final Label addedDepsPlaceholder = new Label("No dependencies added");
    private final Button helpButton = new Button("?");
    private boolean onSpringDepsPage;
    private boolean onJavaFXDepsPage;
    private StackPane centerStack;
    private ScrollPane formScroll;
    private BorderPane springDepsPage;
    private BorderPane javafxDepsPage;
    private final Set<String> selectedJavaFXDepIds = new LinkedHashSet<>();
    private final ListView<JavaFXLib> javafxLibList = new ListView<>();
    private final Label javafxLibTitle = new Label();
    private final Label javafxLibDescription = new Label();
    private final Hyperlink javafxWebLink = new Hyperlink("Web site ↗");
    private final VBox javafxAddedDepsBox = new VBox(6);
    private final Label javafxAddedDepsPlaceholder = new Label("No dependencies added");

    private HBox dependenciesRow;
    private Button createButton;
    private Button cancelButton;
    private Button previousButton;
    private boolean packageEdited;
    private boolean artifactEdited;
    private GeneratorEntry selected = NEW_PROJECT_ENTRIES.get(0);
    private JdkEntry selectedJdk = null;

    private final HBox javaAdvancedToggle = new HBox(8);
    private final Label javaAdvancedArrow = new Label("\u25BE  Advanced Settings");
    private final VBox javaAdvancedContainer = new VBox(10);
    private boolean isJavaAdvancedExpanded = true;
    private final List<Node> advancedSettingsNodes = new ArrayList<>();

    public NewProjectDialog(Stage owner, Consumer<ProjectSpec> onCreate) {
        this.onCreate = onCreate;

        stage.initOwner(owner);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("New Project");

        populateJdkList(null);

        BorderPane root = new BorderPane();
        root.getStyleClass().addAll("app-root", "new-project-dialog");
        sidebar = buildGeneratorList();
        root.setLeft(sidebar);
        formScroll = buildForm();
        springDepsPage = buildSpringDependencyPage();
        springDepsPage.setVisible(false);
        springDepsPage.setManaged(false);
        javafxDepsPage = buildJavaFXDependencyPage();
        javafxDepsPage.setVisible(false);
        javafxDepsPage.setManaged(false);
        quarkusDepsPage = buildQuarkusDependencyPage();
        quarkusDepsPage.setVisible(false);
        quarkusDepsPage.setManaged(false);
        jakartaDepsPage = buildJakartaDependencyPage();
        jakartaDepsPage.setVisible(false);
        jakartaDepsPage.setManaged(false);
        micronautFeaturesPage = buildMicronautFeaturesPage();
        micronautFeaturesPage.setVisible(false);
        micronautFeaturesPage.setManaged(false);
        ktorPluginsPage = buildKtorPluginsPage();
        ktorPluginsPage.setVisible(false);
        ktorPluginsPage.setManaged(false);
        centerStack = new StackPane(formScroll, springDepsPage, javafxDepsPage, quarkusDepsPage, jakartaDepsPage, micronautFeaturesPage, ktorPluginsPage);
        root.setCenter(centerStack);
        root.setBottom(buildButtons());

        Scene scene = new Scene(root, 1060, 840);
        scene.getStylesheets().add(
                getClass().getResource("/css/lumina-dark.css").toExternalForm());
        stage.setScene(scene);
    }

    public void show() {
        updateForGenerator();
        stage.showAndWait();
    }

    private final List<GeneratorEntry> allSidebarEntries = new ArrayList<>();
    private final Map<GeneratorEntry, HBox> entryRowMap = new LinkedHashMap<>();
    private ScrollPane generatorScrollPane;
    private VBox generatorItemsBox;
    private Label newProjectHeaderLabel;
    private Label generatorsHeaderLabel;

    // -------------------------------------------------------- generator list

    private VBox buildGeneratorList() {
        allSidebarEntries.clear();
        allSidebarEntries.addAll(NEW_PROJECT_ENTRIES);
        allSidebarEntries.addAll(GENERATOR_ENTRIES);
        entryRowMap.clear();

        // Top bar with search icon matching IntelliJ New Project wizard
        HBox topBar = new HBox(8);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(10, 14, 4, 14));

        SVGPath searchIcon = new SVGPath();
        searchIcon.setContent("M 6,1 C 8.8,1 11,3.2 11,6 C 11,7.2 10.6,8.3 9.9,9.1 L 13.5,12.7 L 12.7,13.5 L 9.1,9.9 C 8.3,10.6 7.2,11 6,11 C 3.2,11 1,8.8 1,6 C 1,3.2 3.2,1 6,1 Z M 6,2.2 C 3.9,2.2 2.2,3.9 2.2,6 C 2.2,8.1 3.9,9.8 6,9.8 C 8.1,9.8 9.8,8.1 9.8,6 C 9.8,3.9 8.1,2.2 6,2.2 Z");
        searchIcon.setFill(Color.web("#8B92A6"));

        Button searchBtn = new Button();
        searchBtn.setGraphic(searchIcon);
        searchBtn.getStyleClass().add("sidebar-search-btn");
        searchBtn.setTooltip(new Tooltip("Search generators"));

        TextField searchField = new TextField();
        searchField.setPromptText("Search...");
        searchField.getStyleClass().add("search-field");
        searchField.setPrefWidth(160);
        searchField.setVisible(false);
        searchField.setManaged(false);

        searchBtn.setOnAction(e -> {
            boolean show = !searchField.isVisible();
            searchField.setVisible(show);
            searchField.setManaged(show);
            if (show) {
                searchField.requestFocus();
            } else {
                searchField.clear();
                filterGeneratorEntries("");
            }
        });

        searchField.textProperty().addListener((obs, old, text) -> filterGeneratorEntries(text));

        topBar.getChildren().addAll(searchBtn, searchField);

        generatorItemsBox = new VBox(2);
        generatorItemsBox.getStyleClass().add("generator-items-box");
        generatorItemsBox.setPadding(new Insets(2, 8, 12, 8));

        newProjectHeaderLabel = new Label("New Project");
        newProjectHeaderLabel.getStyleClass().add("panel-header");
        newProjectHeaderLabel.setPadding(new Insets(6, 10, 4, 10));
        generatorItemsBox.getChildren().add(newProjectHeaderLabel);

        for (GeneratorEntry entry : NEW_PROJECT_ENTRIES) {
            HBox row = createGeneratorRow(entry);
            generatorItemsBox.getChildren().add(row);
        }

        generatorsHeaderLabel = new Label("Generators");
        generatorsHeaderLabel.getStyleClass().add("panel-header");
        // Follows immediately after Empty Project with compact 14px top padding
        generatorsHeaderLabel.setPadding(new Insets(14, 10, 4, 10));
        generatorItemsBox.getChildren().add(generatorsHeaderLabel);

        for (GeneratorEntry entry : GENERATOR_ENTRIES) {
            HBox row = createGeneratorRow(entry);
            generatorItemsBox.getChildren().add(row);
        }

        Button plugins = new Button("More via plugins...");
        plugins.getStyleClass().add("plugin-link");
        plugins.setMaxWidth(Double.MAX_VALUE);
        plugins.setOnAction(e -> showPluginManager());
        VBox.setMargin(plugins, new Insets(14, 2, 8, 2));
        generatorItemsBox.getChildren().add(plugins);

        generatorScrollPane = new ScrollPane(generatorItemsBox);
        generatorScrollPane.setFitToWidth(true);
        generatorScrollPane.getStyleClass().add("generator-scroll");
        generatorScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        generatorScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        VBox.setVgrow(generatorScrollPane, Priority.ALWAYS);

        VBox sidebar = new VBox(topBar, generatorScrollPane);
        sidebar.getStyleClass().add("generator-panel");
        sidebar.setPrefWidth(240);
        sidebar.setMinWidth(220);

        sidebar.setFocusTraversable(true);
        sidebar.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.UP) {
                int idx = allSidebarEntries.indexOf(selected);
                if (idx > 0) {
                    selectSidebarEntry(allSidebarEntries.get(idx - 1));
                    ensureVisible(entryRowMap.get(selected));
                }
                e.consume();
            } else if (e.getCode() == KeyCode.DOWN) {
                int idx = allSidebarEntries.indexOf(selected);
                if (idx >= 0 && idx < allSidebarEntries.size() - 1) {
                    selectSidebarEntry(allSidebarEntries.get(idx + 1));
                    ensureVisible(entryRowMap.get(selected));
                }
                e.consume();
            }
        });

        Platform.runLater(() -> selectSidebarEntry(NEW_PROJECT_ENTRIES.get(0)));

        return sidebar;
    }

    private HBox createGeneratorRow(GeneratorEntry entry) {
        Node icon = GeneratorIcons.getIcon(entry.label());

        Label label = new Label(entry.label());
        label.getStyleClass().add("generator-row-label");
        HBox.setHgrow(label, Priority.ALWAYS);

        HBox row = new HBox(10, icon, label);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(5, 10, 5, 10));
        row.getStyleClass().add("generator-item");

        if (!entry.enabled()) {
            row.getStyleClass().add("generator-disabled");
            Label soon = new Label("(soon)");
            soon.getStyleClass().add("generator-soon-label");
            row.getChildren().add(soon);
        }

        row.setOnMouseClicked(e -> selectSidebarEntry(entry));
        entryRowMap.put(entry, row);
        return row;
    }

    private void selectSidebarEntry(GeneratorEntry entry) {
        if (entry == null) return;
        selected = entry;
        for (Map.Entry<GeneratorEntry, HBox> item : entryRowMap.entrySet()) {
            boolean isSel = item.getKey().equals(entry);
            if (isSel) {
                if (!item.getValue().getStyleClass().contains("generator-item-selected")) {
                    item.getValue().getStyleClass().add("generator-item-selected");
                }
            } else {
                item.getValue().getStyleClass().remove("generator-item-selected");
            }
        }
        updateForGenerator();
    }

    private void ensureVisible(Node node) {
        if (node == null || generatorScrollPane == null) return;
        Platform.runLater(() -> {
            Bounds nodeBounds = node.getBoundsInParent();
            Bounds viewportBounds = generatorScrollPane.getViewportBounds();
            if (viewportBounds == null || nodeBounds == null) return;
            double contentHeight = generatorScrollPane.getContent().getBoundsInLocal().getHeight();
            double viewHeight = viewportBounds.getHeight();
            if (contentHeight <= viewHeight) return;

            double nodeMinY = nodeBounds.getMinY();
            double nodeMaxY = nodeBounds.getMaxY();
            double currentScrollY = generatorScrollPane.getVvalue() * (contentHeight - viewHeight);

            if (nodeMinY < currentScrollY) {
                generatorScrollPane.setVvalue(nodeMinY / (contentHeight - viewHeight));
            } else if (nodeMaxY > currentScrollY + viewHeight) {
                generatorScrollPane.setVvalue((nodeMaxY - viewHeight) / (contentHeight - viewHeight));
            }
        });
    }

    private void filterGeneratorEntries(String query) {
        String q = query == null ? "" : query.trim().toLowerCase();
        boolean hasNewProjectMatches = false;
        boolean hasGeneratorMatches = false;

        for (GeneratorEntry entry : NEW_PROJECT_ENTRIES) {
            HBox row = entryRowMap.get(entry);
            if (row != null) {
                boolean match = q.isEmpty() || entry.label().toLowerCase().contains(q);
                row.setVisible(match);
                row.setManaged(match);
                if (match) hasNewProjectMatches = true;
            }
        }
        for (GeneratorEntry entry : GENERATOR_ENTRIES) {
            HBox row = entryRowMap.get(entry);
            if (row != null) {
                boolean match = q.isEmpty() || entry.label().toLowerCase().contains(q);
                row.setVisible(match);
                row.setManaged(match);
                if (match) hasGeneratorMatches = true;
            }
        }

        if (newProjectHeaderLabel != null) {
            newProjectHeaderLabel.setVisible(hasNewProjectMatches);
            newProjectHeaderLabel.setManaged(hasNewProjectMatches);
        }
        if (generatorsHeaderLabel != null) {
            generatorsHeaderLabel.setVisible(hasGeneratorMatches);
            generatorsHeaderLabel.setManaged(hasGeneratorMatches);
        }
    }

    // ------------------------------------------------------------------ form

    private ScrollPane buildForm() {
        formGrid.setHgap(14);
        formGrid.setVgap(10);
        formGrid.setPadding(new Insets(20, 24, 16, 24));

        formCol0.setMinWidth(110);
        formCol0.setPrefWidth(110);
        formCol1.setHgrow(Priority.ALWAYS);
        formGrid.getColumnConstraints().setAll(formCol0, formCol1);

        serverUrlLabel.getStyleClass().add("form-static");
        serverSettingsButton.getStyleClass().add("console-button");
        serverSettingsButton.setOnAction(e -> showServerSettings());
        serverRow.getChildren().setAll(serverUrlLabel, serverSettingsButton);
        serverRow.setAlignment(Pos.CENTER_LEFT);

        browseLocationButton.setGraphic(createBrowseFolderIcon());
        browseLocationButton.setText(null);
        browseLocationButton.getStyleClass().addAll("console-button", "browse-button");
        browseLocationButton.setPrefSize(28, 28);
        browseLocationButton.setMinSize(28, 28);
        browseLocationButton.setMaxSize(28, 28);
        browseLocationButton.setTooltip(new Tooltip("Select Project Location"));
        browseLocationButton.setOnAction(e -> {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("Project Location");
            File current = new File(locationField.getText());
            if (current.isDirectory()) chooser.setInitialDirectory(current);
            File dir = chooser.showDialog(stage);
            if (dir != null) locationField.setText(dir.getAbsolutePath());
        });
        locationRow.getChildren().setAll(locationField, browseLocationButton);
        locationRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(locationField, Priority.ALWAYS);

        locationHint.getStyleClass().add("form-hint");

        jakartaTemplateBox.getSelectionModel().select(dev.lumina.project.JakartaMetadata.TEMPLATE_REST);
        jakartaTemplateBox.setMaxWidth(Double.MAX_VALUE);
        jakartaAppServerBox.getSelectionModel().selectFirst();
        jakartaAppServerBox.setMaxWidth(Double.MAX_VALUE);
        newAppServerButton.getStyleClass().add("dialog-secondary");
        newAppServerButton.setOnAction(e -> showNewAppServerDialog());
        jakartaServerRow.getChildren().setAll(jakartaAppServerBox, newAppServerButton);
        jakartaServerRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(jakartaAppServerBox, Priority.ALWAYS);

        nodeRuntimeBox.getSelectionModel().selectFirst();
        angularCliBox.getSelectionModel().selectFirst();
        viteBox.getSelectionModel().selectFirst();
        viteTemplateBox.getSelectionModel().select("React");
        nodeRuntimeBox.setMaxWidth(Double.MAX_VALUE);
        angularCliBox.setMaxWidth(Double.MAX_VALUE);
        viteBox.setMaxWidth(Double.MAX_VALUE);
        viteTemplateBox.setMaxWidth(Double.MAX_VALUE);
        Button nodeMore = compactButton("\u2026");
        Button cliMore = compactButton("\u2026");
        Button viteMore = compactButton("\u2026");
        nodeRow.getChildren().setAll(nodeRuntimeBox, nodeMore);
        nodeRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(nodeRuntimeBox, Priority.ALWAYS);
        cliRow.getChildren().setAll(angularCliBox, cliMore);
        cliRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(angularCliBox, Priority.ALWAYS);
        viteRow.getChildren().setAll(viteBox, viteMore);
        viteRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(viteBox, Priority.ALWAYS);

        emptyDescription.getStyleClass().add("form-hint");

        languageGroup.getToggles().addAll(langJava, langKotlin, langGroovy);
        langJava.setToggleGroup(languageGroup);
        langKotlin.setToggleGroup(languageGroup);
        langGroovy.setToggleGroup(languageGroup);
        langJava.getStyleClass().addAll("segment", "segment-first");
        langKotlin.getStyleClass().addAll("segment");
        langGroovy.getStyleClass().addAll("segment", "segment-last");
        langJava.setSelected(true);
        languageRow.getChildren().setAll(langJava, langKotlin, langGroovy);
        languageRow.getStyleClass().add("segmented");

        typeGroup.getToggles().addAll(typeGradleGroovy, typeGradleKotlin, typeMaven);
        typeGradleGroovy.setToggleGroup(typeGroup);
        typeGradleKotlin.setToggleGroup(typeGroup);
        typeMaven.setToggleGroup(typeGroup);
        typeGradleGroovy.getStyleClass().addAll("segment", "segment-first");
        typeGradleKotlin.getStyleClass().addAll("segment");
        typeMaven.getStyleClass().addAll("segment", "segment-last");
        javafx.scene.control.Tooltip.install(typeGradleGroovy, new javafx.scene.control.Tooltip(
                "Generate a Gradle project descriptor written in Groovy."));
        javafx.scene.control.Tooltip.install(typeGradleKotlin, new javafx.scene.control.Tooltip(
                "Generate a Gradle project descriptor written in the Kotlin DSL."));
        javafx.scene.control.Tooltip.install(typeMaven, new javafx.scene.control.Tooltip(
                "Generate a Maven archive."));
        typeMaven.setSelected(true);
        typeRow.getChildren().setAll(typeGradleGroovy, typeGradleKotlin, typeMaven);
        typeRow.getStyleClass().add("segmented");
        typeLabel = formLabel("Type:");

        testGroup.getToggles().addAll(testJUnit, testKotest, testSpock);
        testJUnit.setToggleGroup(testGroup);
        testKotest.setToggleGroup(testGroup);
        testSpock.setToggleGroup(testGroup);
        testJUnit.getStyleClass().addAll("segment", "segment-first");
        testKotest.getStyleClass().addAll("segment");
        testSpock.getStyleClass().addAll("segment", "segment-last");
        testJUnit.setSelected(true);
        testFrameworkRow.getChildren().setAll(testJUnit, testKotest, testSpock);
        testFrameworkRow.getStyleClass().add("segmented");
        testFrameworkLabel = formLabel("Test framework:");

        packagingGroup.getToggles().addAll(packagingJar, packagingWar);
        packagingJar.setToggleGroup(packagingGroup);
        packagingWar.setToggleGroup(packagingGroup);
        packagingJar.getStyleClass().addAll("segment", "segment-first");
        packagingWar.getStyleClass().addAll("segment", "segment-last");
        packagingJar.setSelected(true);
        packagingRow.getChildren().setAll(packagingJar, packagingWar);
        packagingRow.getStyleClass().add("segmented");

        configGroup.getToggles().addAll(configProperties, configYaml);
        configProperties.setToggleGroup(configGroup);
        configYaml.setToggleGroup(configGroup);
        configProperties.getStyleClass().addAll("segment", "segment-first");
        configYaml.getStyleClass().addAll("segment", "segment-last");
        configProperties.setSelected(true);
        configRow.getChildren().setAll(configProperties, configYaml);
        configRow.getStyleClass().add("segmented");

        configureBuildOptions();

        micronautAppTypeBox.getSelectionModel().selectFirst();
        micronautAppTypeBox.setMaxWidth(Double.MAX_VALUE);
        micronautAppTypeBox.setOnAction(e -> {
            MicronautAppType appType = micronautAppTypeBox.getValue();
            if (appType != null) {
                loadMicronautFeaturesForAppType(appType.value());
            }
        });
        micronautAppTypeLabel = formLabel("Application type:");

        ktorEngineBox.getSelectionModel().selectFirst();
        ktorEngineBox.setMaxWidth(Double.MAX_VALUE);
        ktorAddSampleCodeCheck.setSelected(true);
        ktorTutorialsLabel = new Label("Start with Ktor Server and Client tutorials \u2197");
        ktorTutorialsLabel.setStyle("-fx-text-fill: #589DF6; -fx-cursor: hand; -fx-font-size: 12px;");
        ktorTutorialsLabel.setOnMouseClicked(e -> openBrowser("https://ktor.io/docs/server-create-a-new-project.html"));

        ktorAdvancedToggle = new HBox(8);
        ktorAdvancedToggle.setAlignment(Pos.CENTER_LEFT);
        ktorAdvancedArrow = new Label("\u25B8  Advanced Settings");
        ktorAdvancedArrow.setStyle("-fx-text-fill: #DFE1E5; -fx-font-weight: bold; -fx-cursor: hand; -fx-font-size: 12px;");
        Region advLine = new Region();
        advLine.setStyle("-fx-background-color: #393B40;");
        advLine.setPrefHeight(1);
        advLine.setMaxHeight(1);
        HBox.setHgrow(advLine, Priority.ALWAYS);
        ktorAdvancedToggle.getChildren().addAll(ktorAdvancedArrow, advLine);

        ktorAdvancedContainer = new VBox(12);
        ktorAdvancedContainer.setPadding(new Insets(6, 0, 6, 0));
        GridPane advGrid = new GridPane();
        advGrid.setHgap(16);
        advGrid.setVgap(12);

        Label ktorBuildLabel = formLabel("Build system:");
        ktorBuildGroup.getToggles().addAll(ktorBuildGradle, ktorBuildKotlin, ktorBuildMaven);
        ktorBuildGradle.setToggleGroup(ktorBuildGroup);
        ktorBuildKotlin.setToggleGroup(ktorBuildGroup);
        ktorBuildMaven.setToggleGroup(ktorBuildGroup);
        ktorBuildGradle.getStyleClass().addAll("segment", "segment-first");
        ktorBuildKotlin.getStyleClass().addAll("segment");
        ktorBuildMaven.getStyleClass().addAll("segment", "segment-last");
        ktorBuildGradle.setSelected(true);
        ktorBuildSystemRow.getChildren().setAll(ktorBuildGradle, ktorBuildKotlin, ktorBuildMaven);
        ktorBuildSystemRow.getStyleClass().add("segmented");
        advGrid.add(ktorBuildLabel, 0, 0);
        advGrid.add(ktorBuildSystemRow, 1, 0);

        Label ktorVerLabel = formLabel("Ktor version:");
        ktorVersionBox.getSelectionModel().selectFirst();
        ktorVersionBox.setMaxWidth(Double.MAX_VALUE);
        advGrid.add(ktorVerLabel, 0, 1);
        advGrid.add(ktorVersionBox, 1, 1);

        Label ktorCfgLabel = formLabel("Configuration in:");
        ktorConfigInBox.getSelectionModel().selectFirst();
        ktorConfigInBox.setMaxWidth(Double.MAX_VALUE);
        advGrid.add(ktorCfgLabel, 0, 2);
        advGrid.add(ktorConfigInBox, 1, 2);

        ktorAdvancedContainer.getChildren().add(advGrid);
        ktorAdvancedContainer.setVisible(false);
        ktorAdvancedContainer.setManaged(false);

        ktorAdvancedToggle.setOnMouseClicked(e -> {
            isKtorAdvancedExpanded = !isKtorAdvancedExpanded;
            ktorAdvancedArrow.setText(isKtorAdvancedExpanded ? "\u25BE  Advanced Settings" : "\u25B8  Advanced Settings");
            ktorAdvancedContainer.setVisible(isKtorAdvancedExpanded);
            ktorAdvancedContainer.setManaged(isKtorAdvancedExpanded);
        });

        jdkCombo.setCellFactory(listView -> createJdkCell());
        jdkCombo.setButtonCell(createJdkButtonCell());
        if (selectedJdk != null) {
            jdkCombo.getSelectionModel().select(selectedJdk);
        }
        jdkCombo.getSelectionModel().selectedItemProperty().addListener((obs, old, entry) -> {
            if (entry == null || entry == selectedJdk) return;
            if (!entry.enabled()) {
                Platform.runLater(() -> jdkCombo.getSelectionModel().select(selectedJdk));
                return;
            }
            if (entry.action()) {
                Platform.runLater(() -> {
                    jdkCombo.getSelectionModel().select(selectedJdk);
                    handleJdkAction(entry);
                });
                return;
            }
            selectedJdk = entry;
            syncGradleVersionWithJdk();
        });

        groovySdkBox.getItems().setAll(GroovyMetadata.fetchVersions(false));
        if (!groovySdkBox.getItems().isEmpty()) {
            groovySdkBox.getSelectionModel().selectFirst();
        }
        GroovyMetadata.fetchVersionsAsync(versions -> {
            if (versions != null && !versions.isEmpty()) {
                String cur = groovySdkBox.getValue();
                groovySdkBox.getItems().setAll(versions);
                if (cur != null && versions.contains(cur)) {
                    groovySdkBox.setValue(cur);
                } else {
                    groovySdkBox.getSelectionModel().selectFirst();
                }
            }
        });

        groovySdkBox.setOnAction(e -> {
            String selected = groovySdkBox.getValue();
            if (GroovyMetadata.SPECIFY_HOME_OPTION.equals(selected)) {
                DirectoryChooser dc = new DirectoryChooser();
                dc.setTitle("Select Groovy SDK Home Directory");
                File chosen = dc.showDialog(stage);
                if (chosen != null) {
                    String detected = GroovyMetadata.detectGroovyVersionFromHome(chosen.toPath());
                    if (detected != null && !detected.isBlank()) {
                        if (!groovySdkBox.getItems().contains(detected)) {
                            groovySdkBox.getItems().add(0, detected);
                        }
                        groovySdkBox.setValue(detected);
                    } else {
                        String customLabel = chosen.getName();
                        if (!groovySdkBox.getItems().contains(customLabel)) {
                            groovySdkBox.getItems().add(0, customLabel);
                        }
                        groovySdkBox.setValue(customLabel);
                    }
                } else {
                    groovySdkBox.getSelectionModel().selectFirst();
                }
            }
        });

        // Scala controls setup
        scalaBuildSystemRow.getChildren().setAll(segmented(scalaBuildGroup, false, "sbt", "Scala CLI"));
        for (Toggle t : scalaBuildGroup.getToggles()) {
            if ("sbt".equalsIgnoreCase(((ToggleButton) t).getText())) {
                t.setSelected(true);
                break;
            }
        }
        scalaBuildGroup.selectedToggleProperty().addListener((obs, old, n) -> {
            if (selected != null && selected.generator() == ProjectSpec.Generator.SCALA) {
                rebuildFormGrid();
            }
        });

        sbtVersionBox.getItems().setAll(ScalaMetadata.fetchSbtVersions(false));
        if (!sbtVersionBox.getItems().isEmpty()) {
            sbtVersionBox.getSelectionModel().selectFirst();
        }
        ScalaMetadata.fetchSbtVersionsAsync(versions -> {
            if (versions != null && !versions.isEmpty()) {
                String cur = sbtVersionBox.getValue();
                sbtVersionBox.getItems().setAll(versions);
                if (cur != null && versions.contains(cur)) {
                    sbtVersionBox.setValue(cur);
                } else {
                    sbtVersionBox.getSelectionModel().selectFirst();
                }
            }
        });
        sbtDownloadSourcesCheck.setSelected(false);
        sbtRow.setAlignment(Pos.CENTER_LEFT);
        sbtRow.getChildren().setAll(sbtVersionBox, sbtDownloadSourcesCheck);

        scalaVersionBox.getItems().setAll(ScalaMetadata.fetchScalaVersions(false));
        if (!scalaVersionBox.getItems().isEmpty()) {
            scalaVersionBox.getSelectionModel().selectFirst();
        }
        ScalaMetadata.fetchScalaVersionsAsync(versions -> {
            if (versions != null && !versions.isEmpty()) {
                String cur = scalaVersionBox.getValue();
                scalaVersionBox.getItems().setAll(versions);
                if (cur != null && versions.contains(cur)) {
                    scalaVersionBox.setValue(cur);
                } else {
                    scalaVersionBox.getSelectionModel().selectFirst();
                }
            }
        });
        scalaDownloadSourcesCheck.setSelected(true);
        scalaVersionRow.setAlignment(Pos.CENTER_LEFT);
        scalaVersionRow.getChildren().setAll(scalaVersionBox, scalaDownloadSourcesCheck);

        scalaOptionalBracesCheck.setSelected(false);
        scalaPackagePrefixField.setPromptText("Such as 'org.example.application'");
        scalaModuleNameField.setText(nameField.getText());
        nameField.textProperty().addListener((obs, old, v) -> {
            if (selected != null && selected.generator() == ProjectSpec.Generator.SCALA) {
                scalaModuleNameField.setText(v);
            }
        });

        // Python controls setup
        pythonInterpreterRow.getChildren().setAll(segmented(pythonInterpreterGroup, false, "Project venv", "uv", "Base conda", "Custom environment"));
        for (Toggle t : pythonInterpreterGroup.getToggles()) {
            if ("Project venv".equalsIgnoreCase(((ToggleButton) t).getText())) {
                t.setSelected(true);
                break;
            }
        }
        pythonInterpreterGroup.selectedToggleProperty().addListener((obs, old, n) -> {
            if (selected != null && selected.generator() == ProjectSpec.Generator.PYTHON) {
                rebuildFormGrid();
            }
        });

        setupPythonComboCellFactory(pythonVersionCombo);
        pythonVersionCombo.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(pythonVersionCombo, Priority.ALWAYS);

        setupPythonComboCellFactory(customBasePythonCombo);
        customBasePythonCombo.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(customBasePythonCombo, Priority.ALWAYS);

        setupPythonComboCellFactory(customPythonPathCombo);
        customPythonPathCombo.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(customPythonPathCombo, Priority.ALWAYS);

        List<dev.lumina.project.PythonMetadata.PythonInstallation> initPy = dev.lumina.project.PythonMetadata.fetchPythonInstallations(false);
        pythonVersionCombo.getItems().setAll(initPy);
        if (!pythonVersionCombo.getItems().isEmpty()) {
            pythonVersionCombo.getSelectionModel().selectFirst();
        }
        customBasePythonCombo.getItems().setAll(initPy);
        if (!customBasePythonCombo.getItems().isEmpty()) {
            customBasePythonCombo.getSelectionModel().selectFirst();
        }
        customPythonPathCombo.getItems().setAll(initPy);
        if (!customPythonPathCombo.getItems().isEmpty()) {
            customPythonPathCombo.getSelectionModel().selectFirst();
        }

        dev.lumina.project.PythonMetadata.fetchPythonInstallationsAsync(list -> {
            if (list != null && !list.isEmpty()) {
                dev.lumina.project.PythonMetadata.PythonInstallation cur = pythonVersionCombo.getValue();
                pythonVersionCombo.getItems().setAll(list);
                if (cur != null && list.contains(cur)) {
                    pythonVersionCombo.setValue(cur);
                } else {
                    pythonVersionCombo.getSelectionModel().selectFirst();
                }

                dev.lumina.project.PythonMetadata.PythonInstallation baseCur = customBasePythonCombo.getValue();
                customBasePythonCombo.getItems().setAll(list);
                if (baseCur != null && list.contains(baseCur)) {
                    customBasePythonCombo.setValue(baseCur);
                } else {
                    customBasePythonCombo.getSelectionModel().selectFirst();
                }

                dev.lumina.project.PythonMetadata.PythonInstallation pathCur = customPythonPathCombo.getValue();
                customPythonPathCombo.getItems().setAll(list);
                if (pathCur != null && list.contains(pathCur)) {
                    customPythonPathCombo.setValue(pathCur);
                } else {
                    customPythonPathCombo.getSelectionModel().selectFirst();
                }
            }
        });

        pythonBrowseBtn.setGraphic(createBrowseFolderIcon());
        pythonBrowseBtn.setText(null);
        pythonBrowseBtn.getStyleClass().addAll("console-button", "browse-button");
        pythonBrowseBtn.setPrefSize(28, 28);
        pythonBrowseBtn.setMinSize(28, 28);
        pythonBrowseBtn.setMaxSize(28, 28);
        pythonBrowseBtn.setTooltip(new Tooltip("Select Python Interpreter"));
        pythonBrowseBtn.setOnAction(e -> pickPythonExecutable());

        pythonVersionRow.setAlignment(Pos.CENTER_LEFT);
        pythonVersionRow.getChildren().setAll(pythonVersionCombo, pythonBrowseBtn);
        HBox.setHgrow(pythonVersionCombo, Priority.ALWAYS);

        pythonVenvHintLabel.setStyle("-fx-text-fill: #8C92A4; -fx-font-size: 11px;");
        pythonVenvHintLabel.setWrapText(true);

        uvPythonVersionCombo.getItems().setAll(dev.lumina.project.PythonMetadata.fetchUvPythonVersions());
        uvPythonVersionCombo.getSelectionModel().select("Default");
        uvPythonVersionCombo.setMaxWidth(160);

        String detectedUv = dev.lumina.project.PythonMetadata.detectUvPath();
        uvPathField.setText(detectedUv);
        HBox.setHgrow(uvPathField, Priority.ALWAYS);

        uvPathCheckIcon.setStyle("-fx-text-fill: #4BB543; -fx-font-size: 14px; -fx-font-weight: bold;");
        uvPathCheckIcon.setVisible(dev.lumina.project.PythonMetadata.isValidUv(detectedUv));
        uvPathField.textProperty().addListener((obs, old, v) -> {
            uvPathCheckIcon.setVisible(dev.lumina.project.PythonMetadata.isValidUv(v));
        });

        uvBrowseBtn.setGraphic(createBrowseFolderIcon());
        uvBrowseBtn.setText(null);
        uvBrowseBtn.getStyleClass().addAll("console-button", "browse-button");
        uvBrowseBtn.setPrefSize(28, 28);
        uvBrowseBtn.setMinSize(28, 28);
        uvBrowseBtn.setMaxSize(28, 28);
        uvBrowseBtn.setTooltip(new Tooltip("Select uv Executable"));
        uvBrowseBtn.setOnAction(e -> pickUvExecutable());

        uvPathRow.setAlignment(Pos.CENTER_LEFT);
        uvPathRow.getChildren().setAll(uvPathField, uvPathCheckIcon, uvBrowseBtn);

        uvHintLabel.setStyle("-fx-text-fill: #8C92A4; -fx-font-size: 11px;");
        uvHintLabel.setWrapText(true);

        // Base Conda setup
        condaWarningBanner.setAlignment(Pos.CENTER_LEFT);
        condaWarningBanner.setSpacing(8);
        condaWarningBanner.setStyle("-fx-background-color: #3D3222; -fx-background-radius: 4px; -fx-border-color: #5E4B28; -fx-border-radius: 4px; -fx-padding: 8px 12px;");
        condaWarningIcon.setStyle("-fx-text-fill: #E5A83B; -fx-font-size: 13px; -fx-font-weight: bold;");
        condaWarningText.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        Region bannerSpacer = new Region();
        HBox.setHgrow(bannerSpacer, Priority.ALWAYS);
        condaInstallLink.setStyle("-fx-text-fill: #548AF7; -fx-font-size: 12px; -fx-cursor: hand; -fx-underline: false; -fx-padding: 0;");
        condaInstallLink.setOnAction(e -> startMinicondaInstallation());
        condaSelectPathLink.setStyle("-fx-text-fill: #548AF7; -fx-font-size: 12px; -fx-cursor: hand; -fx-underline: false; -fx-padding: 0;");
        condaSelectPathLink.setOnAction(e -> pickCondaExecutable());
        HBox linksBox = new HBox(12, condaInstallLink, condaSelectPathLink);
        linksBox.setAlignment(Pos.CENTER_RIGHT);
        condaWarningBanner.getChildren().setAll(condaWarningIcon, condaWarningText, bannerSpacer, linksBox);

        condaErrorCallout.setStyle("-fx-background-color: #56282D; -fx-text-fill: #F5B5BA; -fx-border-color: #8A3940; -fx-border-radius: 4px; -fx-background-radius: 4px; -fx-padding: 3px 8px; -fx-font-size: 11px;");

        String detectedConda = dev.lumina.project.PythonMetadata.detectCondaPath();
        condaPathField.setText(detectedConda);
        HBox.setHgrow(condaPathField, Priority.ALWAYS);
        updateCondaValidation(detectedConda);
        condaPathField.textProperty().addListener((obs, old, v) -> updateCondaValidation(v));

        condaBrowseBtn.setGraphic(createBrowseFolderIcon());
        condaBrowseBtn.setText(null);
        condaBrowseBtn.getStyleClass().addAll("console-button", "browse-button");
        condaBrowseBtn.setPrefSize(28, 28);
        condaBrowseBtn.setMinSize(28, 28);
        condaBrowseBtn.setMaxSize(28, 28);
        condaBrowseBtn.setTooltip(new Tooltip("Select Conda Executable"));
        condaBrowseBtn.setOnAction(e -> pickCondaExecutable());

        condaPathRow.setAlignment(Pos.CENTER_LEFT);
        condaPathRow.getChildren().setAll(condaPathField, condaBrowseBtn);

        condaSubtextLabel.setStyle("-fx-text-fill: #8C92A4; -fx-font-size: 11px;");
        condaSubtextLabel.setWrapText(true);

        condaPythonVersionCombo.getItems().setAll(dev.lumina.project.PythonMetadata.fetchCondaPythonVersions());
        condaPythonVersionCombo.getSelectionModel().select("3.12");
        condaPythonVersionCombo.setMaxWidth(160);

        // Custom environment setup
        customEnvGenerateNewRadio.setToggleGroup(customEnvGroup);
        customEnvSelectExistingRadio.setToggleGroup(customEnvGroup);
        customEnvGenerateNewRadio.setSelected(true);
        customEnvRadioRow.setAlignment(Pos.CENTER_LEFT);
        customEnvRadioRow.getChildren().setAll(customEnvGenerateNewRadio, customEnvSelectExistingRadio);
        customEnvGroup.selectedToggleProperty().addListener((obs, old, n) -> {
            boolean isGenNew = customEnvGenerateNewRadio.isSelected();
            if (isGenNew) {
                customTypeCombo.getItems().setAll(dev.lumina.project.PythonMetadata.CUSTOM_ENV_GENERATE_NEW_TYPES);
                customTypeCombo.getSelectionModel().select("Virtualenv");
            } else {
                customTypeCombo.getItems().setAll(dev.lumina.project.PythonMetadata.CUSTOM_ENV_SELECT_EXISTING_TYPES);
                customTypeCombo.getSelectionModel().select("Python");
            }
            if (selected != null && selected.generator() == ProjectSpec.Generator.PYTHON) {
                rebuildFormGrid();
            }
        });

        customTypeCombo.getItems().setAll(dev.lumina.project.PythonMetadata.CUSTOM_ENV_GENERATE_NEW_TYPES);
        customTypeCombo.getSelectionModel().select("Virtualenv");
        customTypeCombo.setPrefWidth(160);
        javafx.util.Callback<ListView<String>, ListCell<String>> typeCellFactory = lv -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    HBox box = new HBox(8);
                    box.setAlignment(Pos.CENTER_LEFT);
                    Node icon = dev.lumina.ui.GeneratorIcons.getIcon(item);
                    Label label = new Label(item);
                    label.setStyle("-fx-text-fill: #DFE1E5;");
                    box.getChildren().addAll(icon, label);
                    setGraphic(box);
                    setText(null);
                }
            }
        };
        customTypeCombo.setCellFactory(typeCellFactory);
        customTypeCombo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    HBox box = new HBox(8);
                    box.setAlignment(Pos.CENTER_LEFT);
                    Node icon = dev.lumina.ui.GeneratorIcons.getIcon(item);
                    Label label = new Label(item);
                    label.setStyle("-fx-text-fill: #DFE1E5;");
                    box.getChildren().addAll(icon, label);
                    setGraphic(box);
                    setText(null);
                }
            }
        });
        customTypeCombo.valueProperty().addListener((obs, old, n) -> {
            if (selected != null && selected.generator() == ProjectSpec.Generator.PYTHON) {
                rebuildFormGrid();
            }
        });

        customBasePythonBrowseBtn.setGraphic(createBrowseFolderIcon());
        customBasePythonBrowseBtn.setText(null);
        customBasePythonBrowseBtn.getStyleClass().addAll("console-button", "browse-button");
        customBasePythonBrowseBtn.setPrefSize(28, 28);
        customBasePythonBrowseBtn.setMinSize(28, 28);
        customBasePythonBrowseBtn.setMaxSize(28, 28);
        customBasePythonBrowseBtn.setTooltip(new Tooltip("Select Base Python Interpreter"));
        customBasePythonBrowseBtn.setOnAction(e -> pickCustomBasePython());

        customBasePythonRow.setAlignment(Pos.CENTER_LEFT);
        customBasePythonRow.getChildren().setAll(customBasePythonCombo, customBasePythonBrowseBtn);

        customLocationCheckIcon.setStyle("-fx-text-fill: #4BB543; -fx-font-size: 14px; -fx-font-weight: bold;");
        customLocationCheckIcon.setVisible(true);
        customLocationField.textProperty().addListener((obs, old, v) -> {
            customLocationFieldEdited = true;
            customLocationCheckIcon.setVisible(v != null && !v.isBlank());
        });
        HBox.setHgrow(customLocationField, Priority.ALWAYS);

        customLocationBrowseBtn.setGraphic(createBrowseFolderIcon());
        customLocationBrowseBtn.setText(null);
        customLocationBrowseBtn.getStyleClass().addAll("console-button", "browse-button");
        customLocationBrowseBtn.setPrefSize(28, 28);
        customLocationBrowseBtn.setMinSize(28, 28);
        customLocationBrowseBtn.setMaxSize(28, 28);
        customLocationBrowseBtn.setTooltip(new Tooltip("Select Environment Location"));
        customLocationBrowseBtn.setOnAction(e -> pickCustomLocation());

        customLocationRow.setAlignment(Pos.CENTER_LEFT);
        customLocationRow.getChildren().setAll(customLocationField, customLocationCheckIcon, customLocationBrowseBtn);

        customPythonPathBrowseBtn.setGraphic(createBrowseFolderIcon());
        customPythonPathBrowseBtn.setText(null);
        customPythonPathBrowseBtn.getStyleClass().addAll("console-button", "browse-button");
        customPythonPathBrowseBtn.setPrefSize(28, 28);
        customPythonPathBrowseBtn.setMinSize(28, 28);
        customPythonPathBrowseBtn.setMaxSize(28, 28);
        customPythonPathBrowseBtn.setTooltip(new Tooltip("Select Python Interpreter"));
        customPythonPathBrowseBtn.setOnAction(e -> pickCustomPythonPath());

        customPythonPathRow.setAlignment(Pos.CENTER_LEFT);
        customPythonPathRow.getChildren().setAll(customPythonPathCombo, customPythonPathBrowseBtn);

        sampleCodeCheck.setSelected(true);
        Label kotlinPrefix = new Label("To create a Kotlin Multiplatform project,");
        kotlinPrefix.setStyle("-fx-text-fill: #8C92A4; -fx-font-size: 11px;");
        Label kotlinLink = new Label("click here \u2197");
        kotlinLink.setStyle("-fx-text-fill: #548AF7; -fx-font-size: 11px; -fx-cursor: hand;");
        kotlinLink.setOnMouseClicked(e -> {
            try {
                if (java.awt.Desktop.isDesktopSupported() && java.awt.Desktop.getDesktop().isSupported(java.awt.Desktop.Action.BROWSE)) {
                    java.awt.Desktop.getDesktop().browse(java.net.URI.create("https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html"));
                }
            } catch (Exception ignored) {}
        });
        kotlinInfoBox.setAlignment(Pos.CENTER_LEFT);
        kotlinInfoBox.getChildren().setAll(kotlinPrefix, kotlinLink);

        javaAdvancedToggle.setAlignment(Pos.CENTER_LEFT);
        javaAdvancedArrow.setStyle("-fx-text-fill: #DFE1E5; -fx-font-weight: bold; -fx-cursor: hand; -fx-font-size: 12px;");
        Region javaAdvLine = new Region();
        javaAdvLine.setStyle("-fx-background-color: #393B40;");
        javaAdvLine.setPrefHeight(1);
        javaAdvLine.setMaxHeight(1);
        HBox.setHgrow(javaAdvLine, Priority.ALWAYS);
        javaAdvancedToggle.getChildren().setAll(javaAdvancedArrow, javaAdvLine);
        javaAdvancedToggle.setPadding(new Insets(6, 0, 2, 0));

        javaAdvGrid.setHgap(14);
        javaAdvGrid.setVgap(10);
        ColumnConstraints advCol0 = new ColumnConstraints(130);
        advCol0.setMinWidth(130);
        advCol0.setPrefWidth(130);
        ColumnConstraints advCol1 = new ColumnConstraints();
        advCol1.setHgrow(Priority.ALWAYS);
        javaAdvGrid.getColumnConstraints().addAll(advCol0, advCol1);

        // Gradle DSL
        gradleDslRow.getChildren().setAll(segmented(gradleDslGroup, false, "Kotlin", "Groovy"));
        for (Toggle t : gradleDslGroup.getToggles()) {
            if ("Kotlin".equalsIgnoreCase(((ToggleButton) t).getText())) {
                t.setSelected(true);
                break;
            }
        }
        gradleDslGroup.selectedToggleProperty().addListener((obs, old, n) -> {
            if (selected != null && (selected.generator() == ProjectSpec.Generator.KOTLIN
                    || selected.generator() == ProjectSpec.Generator.JAVA
                    || selected.generator() == ProjectSpec.Generator.GROOVY)) {
                rebuildFormGrid();
            }
        });

        // Gradle distribution
        gradleDistributionBox.getSelectionModel().select("Wrapper");
        gradleDistributionBox.setMaxWidth(Double.MAX_VALUE);
        gradleDistributionBox.valueProperty().addListener((obs, old, n) -> updateJavaAdvancedGrid());

        // Gradle version & auto-select
        gradleVersionBox.setMinWidth(110);
        gradleVersionBox.setPrefWidth(110);
        gradleVersionBox.setMaxWidth(130);
        gradleVersionRow.setAlignment(Pos.CENTER_LEFT);
        gradleVersionRow.setSpacing(12);
        gradleVersionRow.getChildren().setAll(gradleVersionBox, gradleAutoSelectCheck);
        gradleAutoSelectCheck.setSelected(true);
        gradleAutoSelectCheck.selectedProperty().addListener((obs, old, sel) -> {
            if (sel) syncGradleVersionWithJdk();
        });
        gradleVersionBox.valueProperty().addListener((obs, old, n) -> {
            if (!updatingGradleVersion && n != null) {
                String autoVer = GradleMetadata.getLatestCompatibleVersion(getSelectedJdkMajorVersion());
                if (!n.equals(autoVer) && gradleAutoSelectCheck.isSelected()) {
                    gradleAutoSelectCheck.setSelected(false);
                }
            }
        });

        // Gradle location for local installation
        gradleLocationField.setPromptText("Path to local Gradle");
        HBox.setHgrow(gradleLocationField, Priority.ALWAYS);
        gradleLocationBrowseButton.getStyleClass().add("browse-button");
        gradleLocationBrowseButton.setGraphic(createBrowseFolderIcon());
        gradleLocationBrowseButton.setOnAction(e -> {
            DirectoryChooser dc = new DirectoryChooser();
            dc.setTitle("Select Gradle Installation Directory");
            File chosen = dc.showDialog(stage);
            if (chosen != null) {
                gradleLocationField.setText(chosen.getAbsolutePath());
            }
        });
        gradleLocationRow.setAlignment(Pos.CENTER_LEFT);
        gradleLocationRow.getChildren().setAll(gradleLocationField, gradleLocationBrowseButton);

        saveSettingsCheck.setSelected(true);

        // Load saved preferences if any
        try {
            java.util.prefs.Preferences prefs = java.util.prefs.Preferences.userNodeForPackage(NewProjectDialog.class);
            String savedDsl = prefs.get("gradle.dsl", "Kotlin");
            for (Toggle t : gradleDslGroup.getToggles()) {
                if (((ToggleButton) t).getText().equalsIgnoreCase(savedDsl)) {
                    t.setSelected(true);
                    break;
                }
            }
            String savedDist = prefs.get("gradle.distribution", "Wrapper");
            gradleDistributionBox.setValue(savedDist);
            String savedLoc = prefs.get("gradle.location", "");
            if (!savedLoc.isBlank()) {
                gradleLocationField.setText(savedLoc);
            }
            String savedGroup = prefs.get("gradle.group", null);
            if (savedGroup != null && !savedGroup.isBlank()) {
                groupField.setText(savedGroup);
            }
        } catch (Exception ignored) {}

        syncGradleVersionWithJdk();
        GradleMetadata.fetchReleasesAsync(releases -> syncGradleVersionWithJdk());

        updateJavaAdvancedGrid();
        javaAdvancedContainer.getChildren().setAll(javaAdvGrid);
        javaAdvancedContainer.setPadding(new Insets(4, 0, 4, 0));

        javaAdvancedToggle.setOnMouseClicked(e -> {
            isJavaAdvancedExpanded = !isJavaAdvancedExpanded;
            javaAdvancedArrow.setText(isJavaAdvancedExpanded ? "\u25BE  Advanced Settings" : "\u25B8  Advanced Settings");
            javaAdvancedContainer.setVisible(isJavaAdvancedExpanded);
            javaAdvancedContainer.setManaged(isJavaAdvancedExpanded);
        });

        buildMavenArchetypeForm();
        setupRustControls();
        setupGoControls();

        javaVersionBox.getSelectionModel().select("21");

        dependenciesField.setPromptText("comma separated, e.g. web,data-jpa,lombok");
        dependenciesRow = new HBox(dependenciesField);
        HBox.setHgrow(dependenciesField, Priority.ALWAYS);

        buildGroup.selectedToggleProperty().addListener((obs, old, n) -> {
            if (selected != null && (selected.generator() == ProjectSpec.Generator.JAVA
                    || selected.generator() == ProjectSpec.Generator.KOTLIN
                    || selected.generator() == ProjectSpec.Generator.GROOVY)) {
                rebuildFormGrid();
                updateJavaAdvancedGrid();
            }
        });

        // live bindings
        nameField.textProperty().addListener((obs, old, v) -> {
            if (!artifactEdited) {
                artifactField.setText(sanitize(v));
            }
            mavenArtifactField.setText(sanitize(v));
            updateHints();
        });
        artifactField.setOnKeyTyped(e -> artifactEdited = true);
        locationField.textProperty().addListener((obs, old, v) -> updateHints());
        groupField.textProperty().addListener((obs, old, v) -> updateHints());
        artifactField.textProperty().addListener((obs, old, v) -> updateHints());
        mavenArtifactField.textProperty().addListener((obs, old, v) -> updateHints());
        packageField.setOnKeyTyped(e -> packageEdited = true);
        updateHints();

        setupRubyControls();
        rebuildFormGrid();

        ScrollPane scroll = new ScrollPane(formGrid);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("form-scroll");
        return scroll;
    }

    private void rebuildFormGrid() {
        formGrid.getChildren().clear();
        formGrid.getRowConstraints().clear();
        formCol0.setMinWidth(110);
        formCol0.setPrefWidth(110);

        ProjectSpec.Generator generator = selected.generator();
        boolean spring = generator == ProjectSpec.Generator.SPRING_BOOT;
        boolean quarkus = generator == ProjectSpec.Generator.QUARKUS;
        boolean micronaut = generator == ProjectSpec.Generator.MICRONAUT;
        boolean jakarta = generator == ProjectSpec.Generator.JAKARTA_EE;
        boolean ktor = generator == ProjectSpec.Generator.KTOR;
        boolean mavenArchetype = generator == ProjectSpec.Generator.MAVEN_ARCHETYPE;
        boolean rust = generator == ProjectSpec.Generator.RUST;
        boolean kotlin = generator == ProjectSpec.Generator.KOTLIN;
        boolean groovy = generator == ProjectSpec.Generator.GROOVY;
        boolean scala = generator == ProjectSpec.Generator.SCALA;
        boolean python = generator == ProjectSpec.Generator.PYTHON;
        boolean php = generator == ProjectSpec.Generator.PHP;
        boolean ruby = generator == ProjectSpec.Generator.RUBY;
        boolean go = generator == ProjectSpec.Generator.GO;
        boolean empty = generator == ProjectSpec.Generator.EMPTY_PROJECT;
        boolean angular = generator == ProjectSpec.Generator.ANGULAR_CLI;
        boolean vite = generator == ProjectSpec.Generator.VITE;
        boolean javafx = generator == ProjectSpec.Generator.JAVAFX;
        boolean java = generator == ProjectSpec.Generator.JAVA;
        boolean web = angular || vite;
        boolean specific = switch (generator) {
            case HTML, REACT, EXPRESS, VUE, NUXT -> true;
            default -> false;
        };

        int row = 0;

        // 1. Server URL (for Spring Boot, Quarkus, Micronaut)
        if (spring || quarkus || micronaut) {
            if (micronaut) {
                serverUrlLabel.setText(micronautServerUrl.replaceFirst("^https?://", ""));
            } else if (quarkus) {
                serverUrlLabel.setText(quarkusServerUrl.replaceFirst("^https?://", ""));
            } else {
                serverUrlLabel.setText("start.spring.io");
            }
            serverLabel.setVisible(true);
            serverLabel.setManaged(true);
            serverRow.setVisible(true);
            serverRow.setManaged(true);
            formGrid.add(serverLabel, 0, row);
            formGrid.add(serverRow, 1, row++);
        }

        // 2. Name
        nameLabel.setVisible(true);
        nameLabel.setManaged(true);
        nameField.setVisible(true);
        nameField.setManaged(true);
        formGrid.add(nameLabel, 0, row);
        formGrid.add(nameField, 1, row++);

        // 3. Location
        locationLabel.setVisible(true);
        locationLabel.setManaged(true);
        locationRow.setVisible(true);
        locationRow.setManaged(true);
        formGrid.add(locationLabel, 0, row);
        formGrid.add(locationRow, 1, row++);

        // 4. Location hint + Git checkbox (col 1)
        VBox locationSub = new VBox(4);
        locationSub.setPadding(new Insets(2, 0, 4, 0));
        locationHint.setVisible(true);
        locationHint.setManaged(true);
        locationSub.getChildren().add(locationHint);
        boolean showGit = !web && !empty && !specific;
        if (showGit) {
            gitCheck.setVisible(true);
            gitCheck.setManaged(true);
            locationSub.getChildren().add(gitCheck);
        } else {
            gitCheck.setVisible(false);
            gitCheck.setManaged(false);
        }
        formGrid.add(locationSub, 1, row++);

        // 5. Generator-specific forms
        if (specific) {
            generatorSpecificBox.setVisible(true);
            generatorSpecificBox.setManaged(true);
            buildSpecificForm(generator);
            formGrid.add(generatorSpecificBox, 0, row++, 2, 1);
            return;
        }

        if (empty) {
            emptyDescription.setVisible(true);
            emptyDescription.setManaged(true);
            formGrid.add(emptyDescription, 1, row++);
            return;
        }

        if (php) {
            formGrid.add(phpAddComposerJsonCheck, 1, row++);
            return;
        }

        if (ruby) {
            formGrid.add(rubyInterpreterLabel, 0, row);
            formGrid.add(rubyInterpreterPane, 1, row++);
            formGrid.add(rubyAddSampleCodeCheck, 1, row++);
            return;
        }

        if (rust) {
            formCol0.setMinWidth(150);
            formCol0.setPrefWidth(150);

            formGrid.add(rustToolchainLabel, 0, row);
            formGrid.add(rustToolchainRow, 1, row++);

            formGrid.add(rustVersionTitleLabel, 0, row);
            formGrid.add(rustVersionLabel, 1, row++);

            formGrid.add(rustStdlibLabel, 0, row);
            formGrid.add(rustStdlibRow, 1, row++);

            formGrid.add(rustEnvironmentLabel, 0, row);
            formGrid.add(rustEnvironmentRow, 1, row++);

            formGrid.add(rustTemplateHeaderRow, 0, row++, 2, 1);
            formGrid.add(rustTemplateContainer, 0, row++, 2, 1);
            formGrid.add(rustCargoGenerateBox, 0, row++, 2, 1);
            return;
        }

        if (go) {
            formCol0.setMinWidth(110);
            formCol0.setPrefWidth(110);

            formGrid.add(goRootLabel, 0, row);
            formGrid.add(goRootRow, 1, row++);

            formGrid.add(goVendoringRow, 1, row++);

            formGrid.add(goEnvironmentLabel, 0, row);
            VBox envCol = new VBox(4, goEnvironmentRow, goEnvironmentSubtext);
            formGrid.add(envCol, 1, row++);

            formGrid.add(goSampleCodeCheck, 1, row++);
            return;
        }

        if (angular) {
            formGrid.add(nodeLabel, 0, row);
            formGrid.add(nodeRow, 1, row++);
            formGrid.add(angularCliLabel, 0, row);
            formGrid.add(cliRow, 1, row++);
            formGrid.add(webParametersLabel, 0, row);
            formGrid.add(webParametersField, 1, row++);
            formGrid.add(angularStandaloneCheck, 1, row++);
            formGrid.add(angularDefaultsCheck, 1, row++);
            return;
        }

        if (vite) {
            formGrid.add(nodeLabel, 0, row);
            formGrid.add(nodeRow, 1, row++);
            formGrid.add(viteLabel, 0, row);
            formGrid.add(viteRow, 1, row++);
            formGrid.add(viteTemplateLabel, 0, row);
            formGrid.add(viteTemplateBox, 1, row++);
            formGrid.add(viteTypescriptCheck, 1, row++);
            return;
        }

        if (mavenArchetype) {
            mavenArchetypeBox.setVisible(true);
            mavenArchetypeBox.setManaged(true);
            formGrid.add(mavenArchetypeBox, 0, row++, 2, 1);
            formGrid.add(jdkLabel, 0, row);
            formGrid.add(jdkCombo, 1, row++);
            return;
        }

        if (ktor) {
            formGrid.add(ktorEngineLabel, 0, row);
            formGrid.add(ktorEngineBox, 1, row++);
            formGrid.add(ktorAddSampleCodeCheck, 1, row++);
            formGrid.add(ktorTutorialsLabel, 1, row++);
            formGrid.add(ktorAdvancedToggle, 0, row++, 2, 1);
            formGrid.add(ktorAdvancedContainer, 0, row++, 2, 1);
            ktorAdvancedContainer.setVisible(isKtorAdvancedExpanded);
            ktorAdvancedContainer.setManaged(isKtorAdvancedExpanded);
            ktorAdvancedArrow.setText(isKtorAdvancedExpanded ? "\u25BE  Advanced Settings" : "\u25B8  Advanced Settings");
            return;
        }

        if (jakarta) {
            formGrid.add(jakartaTemplateLabel, 0, row);
            formGrid.add(jakartaTemplateBox, 1, row++);
            formGrid.add(jakartaServerLabel, 0, row);
            formGrid.add(jakartaServerRow, 1, row++);
            formGrid.add(languageLabel, 0, row);
            formGrid.add(languageRow, 1, row++);
            formGrid.add(buildSystemLabel, 0, row);
            formGrid.add(buildSystemRow, 1, row++);
            formGrid.add(jdkLabel, 0, row);
            formGrid.add(jdkCombo, 1, row++);
            formGrid.add(javaLabel, 0, row);
            formGrid.add(javaVersionBox, 1, row++);
            return;
        }

        if (spring) {
            formGrid.add(languageLabel, 0, row);
            formGrid.add(languageRow, 1, row++);
            typeLabel.setText("Type:");
            formGrid.add(typeLabel, 0, row);
            formGrid.add(typeRow, 1, row++);
            formGrid.add(jdkLabel, 0, row);
            formGrid.add(jdkCombo, 1, row++);
            formGrid.add(javaLabel, 0, row);
            formGrid.add(javaVersionBox, 1, row++);
            formGrid.add(packagingLabel, 0, row);
            formGrid.add(packagingRow, 1, row++);
            formGrid.add(configLabel, 0, row);
            formGrid.add(configRow, 1, row++);
            return;
        }

        if (quarkus) {
            langGroovy.setVisible(false);
            langGroovy.setManaged(false);
            if (langGroovy.isSelected()) langJava.setSelected(true);
            formGrid.add(languageLabel, 0, row);
            formGrid.add(languageRow, 1, row++);
            typeLabel.setText("Build system:");
            formGrid.add(typeLabel, 0, row);
            formGrid.add(typeRow, 1, row++);
            formGrid.add(jdkLabel, 0, row);
            formGrid.add(jdkCombo, 1, row++);
            formGrid.add(javaLabel, 0, row);
            formGrid.add(javaVersionBox, 1, row++);
            formGrid.add(sampleCodeCheck, 1, row++);
            return;
        }

        if (micronaut) {
            langGroovy.setVisible(true);
            langGroovy.setManaged(true);
            formGrid.add(languageLabel, 0, row);
            formGrid.add(languageRow, 1, row++);
            typeLabel.setText("Build system:");
            formGrid.add(typeLabel, 0, row);
            formGrid.add(typeRow, 1, row++);
            formGrid.add(testFrameworkLabel, 0, row);
            formGrid.add(testFrameworkRow, 1, row++);
            formGrid.add(micronautAppTypeLabel, 0, row);
            formGrid.add(micronautAppTypeBox, 1, row++);
            formGrid.add(jdkLabel, 0, row);
            formGrid.add(jdkCombo, 1, row++);
            formGrid.add(javaLabel, 0, row);
            formGrid.add(javaVersionBox, 1, row++);
            return;
        }

        if (javafx) {
            formGrid.add(languageLabel, 0, row);
            formGrid.add(languageRow, 1, row++);
            formGrid.add(buildSystemLabel, 0, row);
            formGrid.add(buildSystemRow, 1, row++);
            formGrid.add(jdkLabel, 0, row);
            formGrid.add(jdkCombo, 1, row++);
            formGrid.add(javaAdvancedToggle, 0, row++, 2, 1);
            formGrid.add(javaAdvancedContainer, 0, row++, 2, 1);
            javaAdvancedContainer.setVisible(isJavaAdvancedExpanded);
            javaAdvancedContainer.setManaged(isJavaAdvancedExpanded);
            javaAdvancedArrow.setText(isJavaAdvancedExpanded ? "\u25BE  Advanced Settings" : "\u25B8  Advanced Settings");
            return;
        }

        if (scala) {
            formGrid.add(buildSystemLabel, 0, row);
            formGrid.add(scalaBuildSystemRow, 1, row++);

            if (isSbtSelected()) {
                formGrid.add(jdkLabel, 0, row);
                formGrid.add(jdkCombo, 1, row++);

                formGrid.add(sbtLabel, 0, row);
                formGrid.add(sbtRow, 1, row++);

                if (scalaVersionBox.getParent() instanceof javafx.scene.layout.Pane p) {
                    p.getChildren().remove(scalaVersionBox);
                }
                scalaVersionRow.getChildren().setAll(scalaVersionBox, scalaDownloadSourcesCheck);
                formGrid.add(scalaVersionLabel, 0, row);
                formGrid.add(scalaVersionRow, 1, row++);

                formGrid.add(scalaOptionalBracesCheck, 1, row++);

                formGrid.add(scalaPackagePrefixLabel, 0, row);
                formGrid.add(scalaPackagePrefixField, 1, row++);

                formGrid.add(sampleCodeCheck, 1, row++);

                updateJavaAdvancedGrid();
                formGrid.add(javaAdvancedToggle, 0, row++, 2, 1);
                formGrid.add(javaAdvancedContainer, 0, row++, 2, 1);
                javaAdvancedContainer.setVisible(isJavaAdvancedExpanded);
                javaAdvancedContainer.setManaged(isJavaAdvancedExpanded);
                javaAdvancedArrow.setText(isJavaAdvancedExpanded ? "▾  Advanced Settings" : "▸  Advanced Settings");
            } else {
                if (scalaVersionBox.getParent() instanceof javafx.scene.layout.Pane p) {
                    p.getChildren().remove(scalaVersionBox);
                }
                formGrid.add(scalaVersionLabel, 0, row);
                formGrid.add(scalaVersionBox, 1, row++);

                formGrid.add(scalaOptionalBracesCheck, 1, row++);

                formGrid.add(sampleCodeCheck, 1, row++);
            }
            return;
        }

        if (python) {
            formGrid.add(interpreterTypeLabel, 0, row);
            formGrid.add(pythonInterpreterRow, 1, row++);

            ProjectSpec.PythonInterpreterType interpType = getSelectedPythonInterpreterType();
            if (interpType == ProjectSpec.PythonInterpreterType.UV) {
                formGrid.add(pythonVersionLabel, 0, row);
                formGrid.add(uvPythonVersionCombo, 1, row++);

                formGrid.add(pathToUvLabel, 0, row);
                formGrid.add(uvPathRow, 1, row++);

                formGrid.add(uvHintLabel, 1, row++);
            } else if (interpType == ProjectSpec.PythonInterpreterType.BASE_CONDA) {
                if (condaWarningBanner.isVisible()) {
                    formGrid.add(condaWarningBanner, 1, row++);
                }
                if (condaErrorCallout.isVisible()) {
                    formGrid.add(condaErrorCallout, 1, row++);
                }
                formGrid.add(pathToCondaLabel, 0, row);
                formGrid.add(condaPathRow, 1, row++);

                formGrid.add(condaSubtextLabel, 1, row++);
            } else if (interpType == ProjectSpec.PythonInterpreterType.CUSTOM_ENVIRONMENT) {
                formGrid.add(customEnvLabel, 0, row);
                formGrid.add(customEnvRadioRow, 1, row++);

                formGrid.add(customTypeLabel, 0, row);
                formGrid.add(customTypeCombo, 1, row++);

                boolean isGenNew = customEnvGenerateNewRadio.isSelected();
                String selectedType = customTypeCombo.getValue() != null ? customTypeCombo.getValue() : "Virtualenv";

                if (isGenNew) {
                    if ("Virtualenv".equalsIgnoreCase(selectedType)) {
                        formGrid.add(customBasePythonLabel, 0, row);
                        formGrid.add(customBasePythonRow, 1, row++);

                        formGrid.add(customLocationLabel, 0, row);
                        formGrid.add(customLocationRow, 1, row++);

                        VBox checkBoxes = new VBox(6, inheritPackagesCheck, makeAvailableCheck);
                        checkBoxes.setPadding(new Insets(4, 0, 4, 0));
                        formGrid.add(checkBoxes, 1, row++);
                    } else if ("Conda".equalsIgnoreCase(selectedType)) {
                        if (condaWarningBanner.isVisible()) {
                            formGrid.add(condaWarningBanner, 1, row++);
                        }
                        formGrid.add(pythonVersionLabel, 0, row);
                        formGrid.add(condaPythonVersionCombo, 1, row++);

                        if (condaErrorCallout.isVisible()) {
                            formGrid.add(condaErrorCallout, 1, row++);
                        }
                        formGrid.add(pathToCondaLabel, 0, row);
                        formGrid.add(condaPathRow, 1, row++);
                    } else if ("uv".equalsIgnoreCase(selectedType)) {
                        formGrid.add(pythonVersionLabel, 0, row);
                        formGrid.add(uvPythonVersionCombo, 1, row++);

                        formGrid.add(pathToUvLabel, 0, row);
                        formGrid.add(uvPathRow, 1, row++);
                    } else {
                        // Pipenv, Poetry, Hatch
                        formGrid.add(customBasePythonLabel, 0, row);
                        formGrid.add(customBasePythonRow, 1, row++);
                    }
                } else {
                    // Select existing
                    if ("Conda".equalsIgnoreCase(selectedType)) {
                        if (condaWarningBanner.isVisible()) {
                            formGrid.add(condaWarningBanner, 1, row++);
                        }
                        if (condaErrorCallout.isVisible()) {
                            formGrid.add(condaErrorCallout, 1, row++);
                        }
                        formGrid.add(pathToCondaLabel, 0, row);
                        formGrid.add(condaPathRow, 1, row++);
                    } else {
                        // Python
                        formGrid.add(customPythonPathLabel, 0, row);
                        formGrid.add(customPythonPathRow, 1, row++);
                    }
                }
            } else {
                // Project venv
                formGrid.add(pythonVersionLabel, 0, row);
                formGrid.add(pythonVersionRow, 1, row++);

                formGrid.add(pythonVenvHintLabel, 1, row++);
            }
            return;
        }

        // Standard Java / Kotlin / Groovy
        formGrid.add(buildSystemLabel, 0, row);
        formGrid.add(buildSystemRow, 1, row++);

        formGrid.add(jdkLabel, 0, row);
        formGrid.add(jdkCombo, 1, row++);

        if (groovy) {
            formGrid.add(groovySdkLabel, 0, row);
            formGrid.add(groovySdkBox, 1, row++);
        }

        if (isGradleSelected()) {
            formGrid.add(gradleDslLabel, 0, row);
            formGrid.add(gradleDslRow, 1, row++);
        }

        formGrid.add(sampleCodeCheck, 1, row++);

        boolean showMultiModule = kotlin && isGradleSelected() && isKotlinDslSelected();
        if (showMultiModule) {
            formGrid.add(createMultiModuleNode(), 1, row++);
        } else {
            multiModuleCheck.setSelected(false);
        }

        if (kotlin) {
            formGrid.add(kotlinInfoBox, 1, row++);
        }

        formGrid.add(javaAdvancedToggle, 0, row++, 2, 1);
        formGrid.add(javaAdvancedContainer, 0, row++, 2, 1);
        javaAdvancedContainer.setVisible(isJavaAdvancedExpanded);
        javaAdvancedContainer.setManaged(isJavaAdvancedExpanded);
        javaAdvancedArrow.setText(isJavaAdvancedExpanded ? "\u25BE  Advanced Settings" : "\u25B8  Advanced Settings");
    }

    // ------------------------------------------------ Spring Boot page 2

    /**
     * The Spring Initializr-style dependency picker: version dropdown,
     * searchable checkbox tree on the left, a live description plus the
     * running "Added dependencies" list on the right \u2014 the same layout
     * IntelliJ Ultimate uses for its Spring Boot wizard's second page.
     */
    private BorderPane buildSpringDependencyPage() {
        springBootVersionBox.getSelectionModel().select(0);
        springBootVersionBox.setPrefWidth(160);
        HBox versionRow = new HBox(10, formLabel("Spring Boot:"), springBootVersionBox);
        versionRow.setAlignment(Pos.CENTER_LEFT);

        Label depsLabel = new Label("Dependencies:");
        depsLabel.getStyleClass().add("panel-header");

        depSearchField.setPromptText("Search dependencies\u2026");
        depSearchField.getStyleClass().add("dep-search");
        Label searchGlyph = new Label("\uD83D\uDD0D");
        searchGlyph.getStyleClass().add("dep-search-glyph");
        HBox searchRow = new HBox(6, searchGlyph, depSearchField);
        searchRow.getStyleClass().add("dep-search-row");
        HBox.setHgrow(depSearchField, Priority.ALWAYS);

        depTree.setShowRoot(false);
        depTree.getStyleClass().add("dep-tree");
        depTree.setCellFactory(tv -> createDepCell());
        rebuildDepTree("");
        VBox.setVgrow(depTree, Priority.ALWAYS);

        depSearchField.textProperty().addListener((obs, old, value) ->
                rebuildDepTree(value == null ? "" : value.trim()));

        catalogStatus.getStyleClass().add("dep-catalog-status");
        catalogStatus.setWrapText(true);

        VBox left = new VBox(14, versionRow, depsLabel, searchRow, depTree, catalogStatus);
        left.getStyleClass().add("dep-left");
        left.setPrefWidth(560);

        depDescriptionTitle.getStyleClass().add("dep-description-title");
        depDescriptionTitle.setWrapText(true);
        depDescriptionBody.getStyleClass().add("dep-description-body");
        depDescriptionBody.setWrapText(true);
        VBox descriptionBox = new VBox(6, depDescriptionTitle, depDescriptionBody);
        descriptionBox.getStyleClass().add("dep-description-box");

        Label addedTitle = new Label("Added dependencies:");
        addedTitle.getStyleClass().add("panel-header");
        addedDepsPlaceholder.getStyleClass().add("dep-added-placeholder");
        addedDepsBox.getStyleClass().add("dep-added-box");
        ScrollPane addedScroll = new ScrollPane(addedDepsBox);
        addedScroll.setFitToWidth(true);
        addedScroll.getStyleClass().add("dep-added-scroll");
        VBox.setVgrow(addedScroll, Priority.ALWAYS);
        refreshAddedDeps();

        VBox right = new VBox(16, descriptionBox, addedTitle, addedScroll);
        right.getStyleClass().add("dep-right");
        right.setPrefWidth(300);

        HBox layout = new HBox(24, left, right);
        HBox.setHgrow(left, Priority.ALWAYS);
        layout.setPadding(new Insets(20, 24, 12, 24));

        BorderPane page = new BorderPane();
        page.setCenter(layout);
        ensureLiveCatalog();
        return page;
    }

    /**
     * Uses the metadata already fetched this JVM run, if any; otherwise
     * fetches start.spring.io/metadata/client once in the background \u2014
     * exactly what IntelliJ's wizard queries \u2014 and swaps the version
     * list and dependency tree to the live data on success. On any failure
     * (offline, timeout, unexpected format) the bundled fallback list stays
     * in place and the status line says so plainly.
     */
    private void ensureLiveCatalog() {
        if (cachedMetadata != null) {
            applyMetadata(cachedMetadata);
            return;
        }
        if (metadataFetchFailed) {
            catalogStatus.setText("Showing offline defaults \u2014 "
                    + "couldn't reach start.spring.io.");
            return;
        }
        if (metadataFetchStarted) {
            return;
        }
        metadataFetchStarted = true;
        Thread worker = new Thread(() -> {
            try {
                dev.lumina.project.SpringInitializrMetadata.Metadata metadata =
                        dev.lumina.project.SpringInitializrMetadata.fetch();
                cachedMetadata = metadata;
                Platform.runLater(() -> applyMetadata(metadata));
            } catch (Exception ex) {
                metadataFetchFailed = true;
                Platform.runLater(() -> catalogStatus.setText(
                        "Showing offline defaults \u2014 couldn't reach "
                                + "start.spring.io (" + ex.getClass().getSimpleName()
                                + ")."));
            }
        }, "lumina-spring-initializr-metadata");
        worker.setDaemon(true);
        worker.start();
    }

    /** Swap the version box and dependency tree to the live catalog. */
    private void applyMetadata(
            dev.lumina.project.SpringInitializrMetadata.Metadata metadata) {
        String previousVersion = springBootVersionBox.getValue();
        springBootVersionBox.setItems(
                FXCollections.observableArrayList(metadata.bootVersions()));
        String toSelect = !metadata.defaultBootVersion().isBlank()
                ? metadata.defaultBootVersion() : metadata.bootVersions().get(0);
        springBootVersionBox.getSelectionModel().select(toSelect);
        if (previousVersion != null
                && metadata.bootVersions().contains(previousVersion)) {
            springBootVersionBox.getSelectionModel().select(previousVersion);
        }

        List<SpringDepCategory> live = new ArrayList<>();
        for (dev.lumina.project.SpringInitializrMetadata.Category category
                : metadata.categories()) {
            List<SpringDep> deps = new ArrayList<>();
            for (dev.lumina.project.SpringInitializrMetadata.Dependency dep
                    : category.dependencies()) {
                deps.add(new SpringDep(dep.id(), dep.name(), dep.description()));
            }
            live.add(new SpringDepCategory(category.name(), deps));
        }
        depCategories = live;
        rebuildDepTree(depSearchField.getText() == null ? ""
                : depSearchField.getText().trim());
        catalogStatus.setText("");
    }

    private TreeCell<Object> createDepCell() {
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
                SpringDep dep = (SpringDep) item;
                CheckBox box = new CheckBox(dep.label());
                box.getStyleClass().add("dep-checkbox");
                box.setSelected(selectedDepIds.contains(dep.id()));
                box.selectedProperty().addListener((obs, was, isNow) -> {
                    if (isNow) selectedDepIds.add(dep.id());
                    else selectedDepIds.remove(dep.id());
                    syncDependenciesField();
                    refreshAddedDeps();
                });
                setOnMouseEntered(e -> showDepDescription(dep));
                getStyleClass().add("dep-item-cell");
                setGraphic(box);
                setText(null);
            }
        };
    }

    /** Rebuild the tree; a non-blank filter narrows to matching leaves and
     *  auto-expands their categories, mirroring IntelliJ's live search. */
    private void rebuildDepTree(String filter) {
        String needle = filter.toLowerCase();
        TreeItem<Object> root = new TreeItem<>("root");
        for (SpringDepCategory category : depCategories) {
            List<SpringDep> matches = needle.isEmpty() ? category.deps()
                    : category.deps().stream()
                            .filter(d -> d.label().toLowerCase().contains(needle))
                            .toList();
            if (matches.isEmpty()) continue;
            TreeItem<Object> categoryItem = new TreeItem<>(category.name());
            categoryItem.setExpanded(!needle.isEmpty()
                    || category.name().equals("Developer Tools"));
            for (SpringDep dep : matches) {
                categoryItem.getChildren().add(new TreeItem<>(dep));
            }
            root.getChildren().add(categoryItem);
        }
        depTree.setRoot(root);
    }

    private void showDepDescription(SpringDep dep) {
        depDescriptionTitle.setText(dep.label());
        depDescriptionBody.setText(dep.description());
    }

    /** Keeps the legacy comma-separated field in sync so tryCreate() needs
     *  no changes \u2014 it already reads dependenciesField.getText(). */
    private void syncDependenciesField() {
        dependenciesField.setText(String.join(",", selectedDepIds));
    }

    private void refreshAddedDeps() {
        addedDepsBox.getChildren().clear();
        if (selectedDepIds.isEmpty()) {
            addedDepsBox.getChildren().add(addedDepsPlaceholder);
            return;
        }
        for (String id : selectedDepIds) {
            SpringDep dep = findDep(id);
            String label = dep != null ? dep.label() : id;
            Label text = new Label(label);
            text.getStyleClass().add("dep-added-label");
            HBox.setHgrow(text, Priority.ALWAYS);
            Button remove = new Button("\u00D7");
            remove.getStyleClass().add("dep-added-remove");
            remove.setOnAction(e -> {
                selectedDepIds.remove(id);
                syncDependenciesField();
                refreshAddedDeps();
                depTree.refresh();
            });
            HBox row = new HBox(6, text, remove);
            row.getStyleClass().add("dep-added-row");
            row.setAlignment(Pos.CENTER_LEFT);
            addedDepsBox.getChildren().add(row);
        }
    }

    private SpringDep findDep(String id) {
        for (SpringDepCategory category : depCategories) {
            for (SpringDep dep : category.deps()) {
                if (dep.id().equals(id)) return dep;
            }
        }
        return null;
    }

    /** Page 1 \u2192 page 2, after validating the fields page 2 doesn't repeat. */
    private void goToSpringDepsPage() {
        String name = nameField.getText().trim();
        String location = locationField.getText().trim();
        String artifact = artifactField.getText().trim();
        if (name.isEmpty()) {
            errorLabel.setText("Project name is required.");
            return;
        }
        if (location.isEmpty()) {
            errorLabel.setText("Location is required.");
            return;
        }
        if (artifact.isEmpty()) {
            errorLabel.setText("Artifact is required.");
            return;
        }
        errorLabel.setText("");
        onSpringDepsPage = true;
        formScroll.setVisible(false);
        formScroll.setManaged(false);
        springDepsPage.setVisible(true);
        springDepsPage.setManaged(true);
        createButton.setText("Create");
        createButton.setOnAction(e -> tryCreate());
        cancelButton.setVisible(false);
        cancelButton.setManaged(false);
        previousButton.setVisible(true);
        previousButton.setManaged(true);
        previousButton.setOnAction(e -> backToSpringForm());
    }

    /** Page 2 \u2192 page 1, keeping every already-picked dependency. */
    private void backToSpringForm() {
        onSpringDepsPage = false;
        springDepsPage.setVisible(false);
        springDepsPage.setManaged(false);
        formScroll.setVisible(true);
        formScroll.setManaged(true);
        createButton.setText("Next");
        createButton.setOnAction(e -> goToSpringDepsPage());
        cancelButton.setVisible(true);
        cancelButton.setManaged(true);
        previousButton.setVisible(false);
        previousButton.setManaged(false);
    }

    // ----------------------------------------------------------- javafx deps

    private BorderPane buildJavaFXDependencyPage() {
        Label header = new Label("Additional libraries:");
        header.getStyleClass().add("panel-header");
        header.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #D8DBE6;");

        javafxLibList.setItems(FXCollections.observableArrayList(JAVAFX_LIBRARIES));
        javafxLibList.getStyleClass().addAll("dep-tree", "javafx-dep-list");
        javafxLibList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(JavaFXLib lib, boolean empty) {
                super.updateItem(lib, empty);
                if (empty || lib == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                CheckBox cb = new CheckBox();
                cb.getStyleClass().add("dep-checkbox");
                cb.setSelected(selectedJavaFXDepIds.contains(lib.id()));
                cb.setOnAction(e -> {
                    javafxLibList.getSelectionModel().select(lib);
                    if (cb.isSelected()) selectedJavaFXDepIds.add(lib.id());
                    else selectedJavaFXDepIds.remove(lib.id());
                    refreshAddedJavaFXDeps();
                });

                Label icon = new Label("\uD83D\uDCDA");
                icon.setStyle("-fx-font-size: 13px; -fx-opacity: 0.9;");

                Label nameLabel = new Label(lib.name());
                nameLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #E6E9F2; -fx-font-size: 12.5px;");

                Label verLabel = new Label("(" + lib.version() + ")");
                verLabel.setStyle("-fx-text-fill: #8A91A8; -fx-font-size: 12px;");

                HBox textHBox = new HBox(4, nameLabel, verLabel);
                textHBox.setAlignment(Pos.CENTER_LEFT);

                HBox row = new HBox(10, cb, icon, textHBox);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setPadding(new Insets(3, 6, 3, 6));

                setGraphic(row);
                setText(null);
            }
        });

        javafxLibList.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            if (sel != null) showJavaFXLibDetails(sel);
        });

        VBox.setVgrow(javafxLibList, Priority.ALWAYS);
        VBox left = new VBox(10, header, javafxLibList);
        left.getStyleClass().add("dep-left");
        left.setPrefWidth(460);

        // Right details
        javafxLibTitle.getStyleClass().add("dep-description-title");
        javafxLibTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #E6E9F2;");

        javafxLibDescription.getStyleClass().add("dep-description-body");
        javafxLibDescription.setWrapText(true);
        javafxLibDescription.setStyle("-fx-text-fill: #A9AFC3; -fx-font-size: 12.5px; -fx-line-spacing: 2;");

        javafxWebLink.getStyleClass().add("javafx-web-link");
        javafxWebLink.setStyle("-fx-text-fill: #589DF6; -fx-underline: false; -fx-font-size: 12.5px; -fx-padding: 0;");
        javafxWebLink.setOnAction(e -> {
            JavaFXLib current = javafxLibList.getSelectionModel().getSelectedItem();
            if (current != null && current.website() != null) {
                openBrowser(current.website());
            }
        });

        VBox descBox = new VBox(8, javafxLibTitle, javafxLibDescription, javafxWebLink);
        descBox.getStyleClass().add("dep-description-box");
        descBox.setMinHeight(160);

        Label addedHeader = new Label("Added dependencies:");
        addedHeader.getStyleClass().add("panel-header");
        addedHeader.setStyle("-fx-font-size: 12.5px; -fx-font-weight: bold; -fx-text-fill: #D8DBE6;");

        javafxAddedDepsPlaceholder.getStyleClass().add("dep-added-placeholder");
        javafxAddedDepsPlaceholder.setStyle("-fx-text-fill: #6B7290; -fx-font-size: 12.5px;");
        StackPane placeholderWrap = new StackPane(javafxAddedDepsPlaceholder);
        placeholderWrap.setAlignment(Pos.CENTER);

        javafxAddedDepsBox.getStyleClass().add("dep-added-box");
        javafxAddedDepsBox.setPadding(new Insets(8));

        ScrollPane addedScroll = new ScrollPane(javafxAddedDepsBox);
        addedScroll.setFitToWidth(true);
        addedScroll.getStyleClass().add("dep-added-scroll");
        VBox.setVgrow(addedScroll, Priority.ALWAYS);

        StackPane addedContainer = new StackPane(placeholderWrap, addedScroll);
        addedContainer.setStyle("-fx-background-color: #1A1D28; -fx-border-color: #333849; -fx-border-radius: 6; -fx-background-radius: 6;");
        VBox.setVgrow(addedContainer, Priority.ALWAYS);

        VBox right = new VBox(14, descBox, addedHeader, addedContainer);
        right.getStyleClass().add("dep-right");
        right.setPrefWidth(360);

        HBox layout = new HBox(24, left, right);
        HBox.setHgrow(left, Priority.ALWAYS);
        layout.setPadding(new Insets(20, 24, 16, 24));

        BorderPane page = new BorderPane();
        page.setCenter(layout);

        javafxLibList.getSelectionModel().select(0);
        showJavaFXLibDetails(JAVAFX_LIBRARIES.get(0));
        refreshAddedJavaFXDeps();

        return page;
    }

    private void showJavaFXLibDetails(JavaFXLib lib) {
        javafxLibTitle.setText(lib.name());
        javafxLibDescription.setText(lib.description());
        javafxWebLink.setVisible(lib.website() != null && !lib.website().isBlank());
    }

    private void refreshAddedJavaFXDeps() {
        javafxAddedDepsBox.getChildren().clear();
        boolean empty = selectedJavaFXDepIds.isEmpty();
        javafxAddedDepsPlaceholder.setVisible(empty);
        javafxAddedDepsPlaceholder.setManaged(empty);

        if (!empty) {
            for (JavaFXLib lib : JAVAFX_LIBRARIES) {
                if (!selectedJavaFXDepIds.contains(lib.id())) continue;
                Label name = new Label(lib.label());
                name.getStyleClass().add("dep-added-label");
                name.setStyle("-fx-text-fill: #D8DBE6; -fx-font-size: 12.5px;");

                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                Button remove = new Button("\u00D7");
                remove.getStyleClass().add("dep-added-remove");
                remove.setOnAction(e -> {
                    selectedJavaFXDepIds.remove(lib.id());
                    refreshAddedJavaFXDeps();
                    javafxLibList.refresh();
                });

                HBox row = new HBox(6, name, spacer, remove);
                row.setAlignment(Pos.CENTER_LEFT);
                row.getStyleClass().add("dep-added-row");
                row.setPadding(new Insets(5, 8, 5, 8));
                javafxAddedDepsBox.getChildren().add(row);
            }
        }
    }

    private void goToJavaFXDepsPage() {
        String name = nameField.getText().trim();
        String location = locationField.getText().trim();
        String artifact = artifactField.getText().trim();
        if (name.isEmpty()) {
            errorLabel.setText("Project name is required.");
            return;
        }
        if (location.isEmpty()) {
            errorLabel.setText("Location is required.");
            return;
        }
        if (artifact.isEmpty()) {
            errorLabel.setText("Artifact is required.");
            return;
        }
        errorLabel.setText("");
        onJavaFXDepsPage = true;
        formScroll.setVisible(false);
        formScroll.setManaged(false);
        javafxDepsPage.setVisible(true);
        javafxDepsPage.setManaged(true);
        createButton.setText("Create");
        createButton.setOnAction(e -> tryCreate());
        cancelButton.setVisible(false);
        cancelButton.setManaged(false);
        previousButton.setVisible(true);
        previousButton.setManaged(true);
        previousButton.setOnAction(e -> backToJavaFXForm());
    }

    private void backToJavaFXForm() {
        onJavaFXDepsPage = false;
        javafxDepsPage.setVisible(false);
        javafxDepsPage.setManaged(false);
        formScroll.setVisible(true);
        formScroll.setManaged(true);
        createButton.setText("Next");
        createButton.setOnAction(e -> goToJavaFXDepsPage());
        cancelButton.setVisible(true);
        cancelButton.setManaged(true);
        previousButton.setVisible(false);
        previousButton.setManaged(false);
    }

    // ----------------------------------------------------------- quarkus deps

    private BorderPane buildQuarkusDependencyPage() {
        BorderPane page = new BorderPane();
        page.getStyleClass().addAll("spring-deps-page", "quarkus-deps-page");
        page.setPadding(new Insets(16, 20, 16, 20));

        // Top bar
        HBox streamRow = new HBox(12);
        streamRow.setAlignment(Pos.CENTER_LEFT);
        Label quarkusLabel = new Label("Quarkus:");
        quarkusLabel.getStyleClass().add("form-label");
        quarkusLabel.setStyle("-fx-text-fill: #A0A5B5; -fx-font-size: 13px;");

        quarkusStreamBox.setPrefWidth(140);
        if (!quarkusStreamBox.getItems().isEmpty()) {
            quarkusStreamBox.getSelectionModel().selectFirst();
        }
        quarkusStreamBox.setOnAction(e -> {
            dev.lumina.project.QuarkusMetadata.QuarkusStream stream = quarkusStreamBox.getValue();
            if (stream != null) {
                loadQuarkusExtensionsForStream(stream.key());
            }
        });

        quarkusCatalogStatus.setStyle("-fx-text-fill: #72778A; -fx-font-size: 11px;");
        Region topSpacer = new Region();
        HBox.setHgrow(topSpacer, Priority.ALWAYS);

        streamRow.getChildren().addAll(quarkusLabel, quarkusStreamBox, topSpacer, quarkusCatalogStatus);

        Label extensionsHeader = new Label("Extensions:");
        extensionsHeader.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #D8DBE6;");
        VBox.setMargin(extensionsHeader, new Insets(14, 0, 8, 0));

        VBox topBox = new VBox(streamRow, extensionsHeader);
        page.setTop(topBox);

        // Center split: Left tree, Right details & added
        HBox center = new HBox(16);
        center.setPadding(new Insets(6, 0, 0, 0));

        // Left pane: Search field + TreeView
        VBox leftPane = new VBox(8);
        leftPane.setPrefWidth(430);
        leftPane.setMinWidth(360);

        HBox searchBox = new HBox(6);
        searchBox.setAlignment(Pos.CENTER_LEFT);
        searchBox.getStyleClass().add("search-box-container");
        searchBox.setPadding(new Insets(4, 8, 4, 8));
        searchBox.setStyle("-fx-background-color: #21242C; -fx-background-radius: 4; -fx-border-color: #363B4A; -fx-border-radius: 4;");

        SVGPath searchIcon = new SVGPath();
        searchIcon.setContent("M 6,1 C 8.8,1 11,3.2 11,6 C 11,7.2 10.6,8.3 9.9,9.1 L 13.5,12.7 L 12.7,13.5 L 9.1,9.9 C 8.3,10.6 7.2,11 6,11 C 3.2,11 1,8.8 1,6 C 1,3.2 3.2,1 6,1 Z M 6,2.2 C 3.9,2.2 2.2,3.9 2.2,6 C 2.2,8.1 3.9,9.8 6,9.8 C 8.1,9.8 9.8,8.1 9.8,6 C 9.8,3.9 8.1,2.2 6,2.2 Z");
        searchIcon.setFill(Color.web("#8B92A6"));

        quarkusSearchField.setPromptText("Search");
        quarkusSearchField.getStyleClass().add("dep-search-field");
        quarkusSearchField.setStyle("-fx-background-color: transparent; -fx-text-fill: #DFE1E5; -fx-prompt-text-fill: #72778A; -fx-border-color: transparent;");
        HBox.setHgrow(quarkusSearchField, Priority.ALWAYS);
        quarkusSearchField.textProperty().addListener((obs, old, text) -> rebuildQuarkusTree(text));

        searchBox.getChildren().addAll(searchIcon, quarkusSearchField);

        quarkusTree.setShowRoot(false);
        quarkusTree.getStyleClass().add("dep-tree");
        VBox.setVgrow(quarkusTree, Priority.ALWAYS);
        quarkusTree.setCellFactory(tv -> createQuarkusCell());

        quarkusTree.getSelectionModel().selectedItemProperty().addListener((obs, old, item) -> {
            if (item != null) {
                if (item.getValue() instanceof dev.lumina.project.QuarkusMetadata.QuarkusExtension ext) {
                    showQuarkusExtensionDetail(ext);
                } else if (item.getValue() instanceof String catName) {
                    showQuarkusCategoryDetail(catName);
                }
            }
        });

        leftPane.getChildren().addAll(searchBox, quarkusTree);

        // Right pane: Detail section + Added extensions section
        VBox rightPane = new VBox(16);
        HBox.setHgrow(rightPane, Priority.ALWAYS);

        // Top Details
        VBox detailBox = new VBox(8);
        detailBox.setPadding(new Insets(12, 14, 12, 14));
        detailBox.setStyle("-fx-background-color: #1E2129; -fx-background-radius: 6; -fx-border-color: #2D323E; -fx-border-radius: 6;");
        detailBox.setPrefHeight(180);
        detailBox.setMinHeight(140);

        quarkusDetailTitle.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #FFFFFF;");
        quarkusDetailTitle.setText("REST");

        quarkusDetailDesc.setWrapText(true);
        quarkusDetailDesc.setStyle("-fx-font-size: 12px; -fx-text-fill: #9DA3B4; -fx-line-spacing: 2px;");
        quarkusDetailDesc.setText("Build RESTful web services and APIs using Jakarta REST (formerly JAX-RS)");

        quarkusGuideLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-padding: 0;");
        quarkusGuideLink.setOnAction(e -> {
            String url = (String) quarkusGuideLink.getUserData();
            if (url != null && !url.isBlank()) {
                openBrowser(url);
            }
        });
        quarkusGuideLink.setUserData("https://quarkus.io/guides/rest");

        detailBox.getChildren().addAll(quarkusDetailTitle, quarkusDetailDesc, quarkusGuideLink);

        // Bottom Added extensions
        VBox addedContainer = new VBox(8);
        VBox.setVgrow(addedContainer, Priority.ALWAYS);

        Label addedLabel = new Label("Added extensions:");
        addedLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #D8DBE6;");

        ScrollPane addedScroll = new ScrollPane();
        addedScroll.setFitToWidth(true);
        addedScroll.getStyleClass().add("dep-added-scroll");
        VBox.setVgrow(addedScroll, Priority.ALWAYS);

        quarkusAddedBox.setPadding(new Insets(10));
        quarkusAddedBox.setStyle("-fx-background-color: #1A1D24; -fx-background-radius: 6; -fx-border-color: #2D323E; -fx-border-radius: 6;");
        quarkusAddedBox.setMinHeight(160);

        quarkusNoExtensions.setStyle("-fx-text-fill: #5E6476; -fx-font-size: 13px;");
        quarkusNoExtensions.setAlignment(Pos.CENTER);
        quarkusNoExtensions.setMaxWidth(Double.MAX_VALUE);
        quarkusNoExtensions.setPadding(new Insets(30, 0, 30, 0));

        quarkusAddedBox.getChildren().add(quarkusNoExtensions);
        addedScroll.setContent(quarkusAddedBox);

        addedContainer.getChildren().addAll(addedLabel, addedScroll);

        rightPane.getChildren().addAll(detailBox, addedContainer);

        center.getChildren().addAll(leftPane, rightPane);
        page.setCenter(center);

        rebuildQuarkusTree("");
        return page;
    }

    private TreeCell<Object> createQuarkusCell() {
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
                if (item instanceof String catName) {
                    setText(catName);
                    setGraphic(null);
                    getStyleClass().add("dep-category-cell");
                    return;
                }
                if (item instanceof dev.lumina.project.QuarkusMetadata.QuarkusExtension ext) {
                    CheckBox cb = new CheckBox(ext.name());
                    cb.getStyleClass().add("dep-checkbox");
                    cb.setSelected(selectedQuarkusExtIds.contains(ext.id()));
                    cb.setOnAction(e -> {
                        if (cb.isSelected()) {
                            selectedQuarkusExtIds.add(ext.id());
                        } else {
                            selectedQuarkusExtIds.remove(ext.id());
                        }
                        refreshAddedQuarkusExtensions();
                    });
                    setOnMouseEntered(e -> showQuarkusExtensionDetail(ext));
                    setOnMouseClicked(e -> showQuarkusExtensionDetail(ext));
                    getStyleClass().add("dep-item-cell");
                    setGraphic(cb);
                    setText(null);
                }
            }
        };
    }

    private void rebuildQuarkusTree(String filter) {
        String needle = filter == null ? "" : filter.trim().toLowerCase();
        TreeItem<Object> root = new TreeItem<>("root");
        for (dev.lumina.project.QuarkusMetadata.QuarkusCategory cat : quarkusCategories) {
            List<dev.lumina.project.QuarkusMetadata.QuarkusExtension> matches;
            if (needle.isEmpty()) {
                matches = cat.extensions();
            } else {
                matches = cat.extensions().stream()
                        .filter(ext -> ext.name().toLowerCase().contains(needle)
                                || ext.id().toLowerCase().contains(needle)
                                || ext.description().toLowerCase().contains(needle)
                                || (ext.keywords() != null && ext.keywords().stream().anyMatch(kw -> kw.toLowerCase().contains(needle))))
                        .toList();
            }
            if (matches.isEmpty()) continue;

            TreeItem<Object> catItem = new TreeItem<>(cat.name());
            catItem.setExpanded(!needle.isEmpty() || cat.name().equalsIgnoreCase("Web"));
            for (dev.lumina.project.QuarkusMetadata.QuarkusExtension ext : matches) {
                catItem.getChildren().add(new TreeItem<>(ext));
            }
            root.getChildren().add(catItem);
        }
        quarkusTree.setRoot(root);
    }

    private void showQuarkusExtensionDetail(dev.lumina.project.QuarkusMetadata.QuarkusExtension ext) {
        if (ext == null) return;
        quarkusDetailTitle.setText(ext.name());
        quarkusDetailDesc.setText(ext.description().isBlank()
                ? "No description available for " + ext.name() : ext.description());
        if (ext.guide() != null && !ext.guide().isBlank()) {
            quarkusGuideLink.setText("Guide \u2197");
            quarkusGuideLink.setUserData(ext.guide());
            quarkusGuideLink.setVisible(true);
            quarkusGuideLink.setManaged(true);
        } else {
            quarkusGuideLink.setVisible(false);
            quarkusGuideLink.setManaged(false);
        }
    }

    private void showQuarkusCategoryDetail(String categoryName) {
        quarkusDetailTitle.setText(categoryName);
        quarkusDetailDesc.setText("Quarkus extensions under category " + categoryName + ".");
        quarkusGuideLink.setVisible(false);
        quarkusGuideLink.setManaged(false);
    }

    private void refreshAddedQuarkusExtensions() {
        quarkusAddedBox.getChildren().clear();
        if (selectedQuarkusExtIds.isEmpty()) {
            quarkusAddedBox.getChildren().add(quarkusNoExtensions);
            return;
        }

        for (String id : selectedQuarkusExtIds) {
            dev.lumina.project.QuarkusMetadata.QuarkusExtension ext = findQuarkusExtension(id);
            String label = ext != null ? ext.name() : id;

            HBox row = new HBox(8);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(4, 8, 4, 8));
            row.setStyle("-fx-background-color: #212530; -fx-background-radius: 4;");

            Label name = new Label(label);
            name.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
            HBox.setHgrow(name, Priority.ALWAYS);

            Button remove = new Button("\u00D7");
            remove.getStyleClass().add("dep-added-remove");
            remove.setStyle("-fx-background-color: transparent; -fx-text-fill: #8B92A6; -fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 0 4 0 4;");
            remove.setOnAction(e -> {
                selectedQuarkusExtIds.remove(id);
                refreshAddedQuarkusExtensions();
                quarkusTree.refresh();
            });

            row.getChildren().addAll(name, remove);
            quarkusAddedBox.getChildren().add(row);
        }
    }

    private dev.lumina.project.QuarkusMetadata.QuarkusExtension findQuarkusExtension(String id) {
        for (dev.lumina.project.QuarkusMetadata.QuarkusCategory cat : quarkusCategories) {
            for (dev.lumina.project.QuarkusMetadata.QuarkusExtension ext : cat.extensions()) {
                if (ext.id().equals(id)) return ext;
            }
        }
        return null;
    }

    private void kickOffQuarkusMetadataFetch() {
        if (cachedQuarkusCatalog != null) {
            applyQuarkusCatalog(cachedQuarkusCatalog);
            return;
        }
        if (quarkusFetchFailed) {
            quarkusCatalogStatus.setText("Offline catalog");
            return;
        }
        if (quarkusFetchStarted) {
            return;
        }
        quarkusFetchStarted = true;
        quarkusCatalogStatus.setText("Connecting to " + quarkusServerUrl + " …");

        Thread worker = new Thread(() -> {
            try {
                dev.lumina.project.QuarkusMetadata.QuarkusCatalog catalog =
                        dev.lumina.project.QuarkusMetadata.fetchCatalog(quarkusServerUrl, null);
                cachedQuarkusCatalog = catalog;
                Platform.runLater(() -> applyQuarkusCatalog(catalog));
            } catch (Exception ex) {
                quarkusFetchFailed = true;
                Platform.runLater(() -> {
                    quarkusCatalogStatus.setText("Offline catalog (" + ex.getClass().getSimpleName() + ")");
                });
            }
        }, "lumina-quarkus-metadata");
        worker.setDaemon(true);
        worker.start();
    }

    private void applyQuarkusCatalog(dev.lumina.project.QuarkusMetadata.QuarkusCatalog catalog) {
        if (catalog.streams() != null && !catalog.streams().isEmpty()) {
            quarkusStreamBox.setItems(FXCollections.observableArrayList(catalog.streams()));
            dev.lumina.project.QuarkusMetadata.QuarkusStream toSelect = catalog.streams().stream()
                    .filter(dev.lumina.project.QuarkusMetadata.QuarkusStream::recommended)
                    .findFirst()
                    .orElse(catalog.streams().get(0));
            quarkusStreamBox.getSelectionModel().select(toSelect);
        }
        if (catalog.categories() != null && !catalog.categories().isEmpty()) {
            quarkusCategories = catalog.categories();
            rebuildQuarkusTree(quarkusSearchField.getText());
        }
        quarkusCatalogStatus.setText("");
    }

    private void loadQuarkusExtensionsForStream(String streamKey) {
        quarkusCatalogStatus.setText("Updating extensions …");
        Thread worker = new Thread(() -> {
            try {
                java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
                        .connectTimeout(java.time.Duration.ofSeconds(6))
                        .build();
                List<dev.lumina.project.QuarkusMetadata.QuarkusCategory> categories =
                        dev.lumina.project.QuarkusMetadata.fetchExtensions(client,
                                dev.lumina.project.QuarkusMetadata.normalizeServerUrl(quarkusServerUrl), streamKey);
                Platform.runLater(() -> {
                    if (categories != null && !categories.isEmpty()) {
                        quarkusCategories = categories;
                        rebuildQuarkusTree(quarkusSearchField.getText());
                    }
                    quarkusCatalogStatus.setText("");
                });
            } catch (Exception e) {
                Platform.runLater(() -> quarkusCatalogStatus.setText(""));
            }
        }, "lumina-quarkus-stream-fetch");
        worker.setDaemon(true);
        worker.start();
    }

    private void goToQuarkusDepsPage() {
        String name = nameField.getText().trim();
        String location = locationField.getText().trim();
        String artifact = artifactField.getText().trim();
        if (name.isEmpty()) {
            errorLabel.setText("Project name is required.");
            return;
        }
        if (location.isEmpty()) {
            errorLabel.setText("Location is required.");
            return;
        }
        if (artifact.isEmpty()) {
            errorLabel.setText("Artifact is required.");
            return;
        }
        errorLabel.setText("");
        onQuarkusDepsPage = true;
        if (sidebar != null) {
            sidebar.setVisible(false);
            sidebar.setManaged(false);
        }
        formScroll.setVisible(false);
        formScroll.setManaged(false);
        quarkusDepsPage.setVisible(true);
        quarkusDepsPage.setManaged(true);
        createButton.setText("Create");
        createButton.setOnAction(e -> tryCreate());
        cancelButton.setVisible(true);
        cancelButton.setManaged(true);
        previousButton.setVisible(true);
        previousButton.setManaged(true);
        previousButton.setOnAction(e -> backToQuarkusForm());

        kickOffQuarkusMetadataFetch();
    }

    private void backToQuarkusForm() {
        onQuarkusDepsPage = false;
        quarkusDepsPage.setVisible(false);
        quarkusDepsPage.setManaged(false);
        if (sidebar != null) {
            sidebar.setVisible(true);
            sidebar.setManaged(true);
        }
        formScroll.setVisible(true);
        formScroll.setManaged(true);
        createButton.setText("Next");
        createButton.setOnAction(e -> goToQuarkusDepsPage());
        previousButton.setVisible(false);
        previousButton.setManaged(false);
    }

    private void showNewAppServerDialog() {
        Stage dialog = new Stage();
        dialog.initOwner(stage);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("New Application Server");

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(14);
        grid.setPadding(new Insets(20));

        ComboBox<String> serverTypeBox = new ComboBox<>(FXCollections.observableArrayList(
                "GlassFish", "WildFly", "Tomcat", "Payara", "Open Liberty", "TomEE"
        ));
        serverTypeBox.getSelectionModel().selectFirst();
        serverTypeBox.setMaxWidth(Double.MAX_VALUE);

        TextField homeField = new TextField();
        homeField.setPromptText("Path to server home");
        Button browse = new Button("\u2026");
        browse.setOnAction(e -> {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("Application Server Home");
            File dir = chooser.showDialog(dialog);
            if (dir != null) homeField.setText(dir.getAbsolutePath());
        });
        HBox homeRow = new HBox(8, homeField, browse);
        HBox.setHgrow(homeField, Priority.ALWAYS);

        grid.add(formLabel("Server:"), 0, 0);
        grid.add(serverTypeBox, 1, 0);
        grid.add(formLabel("Home:"), 0, 1);
        grid.add(homeRow, 1, 1);

        Button ok = new Button("OK");
        ok.getStyleClass().add("dialog-primary");
        ok.setOnAction(e -> {
            String selectedType = serverTypeBox.getValue();
            if (selectedType != null && !selectedType.isBlank()) {
                String entry = selectedType + (homeField.getText().isBlank() ? "" : " (" + homeField.getText().trim() + ")");
                if (!jakartaAppServerBox.getItems().contains(entry)) {
                    jakartaAppServerBox.getItems().add(entry);
                }
                jakartaAppServerBox.getSelectionModel().select(entry);
            }
            dialog.close();
        });

        Button cancel = new Button("Cancel");
        cancel.getStyleClass().add("dialog-secondary");
        cancel.setOnAction(e -> dialog.close());

        HBox buttons = new HBox(10, ok, cancel);
        buttons.setAlignment(Pos.CENTER_RIGHT);
        buttons.setPadding(new Insets(12, 0, 0, 0));

        BorderPane root = new BorderPane(grid);
        root.setBottom(buttons);
        root.setPadding(new Insets(12));
        root.getStyleClass().addAll("app-root", "app-server-dialog");
        Scene scene = new Scene(root, 480, 200);
        scene.getStylesheets().add(getClass().getResource("/css/lumina-dark.css").toExternalForm());
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    private void goToJakartaDepsPage() {
        String name = nameField.getText().trim();
        String location = locationField.getText().trim();
        String artifact = artifactField.getText().trim();
        if (name.isEmpty()) {
            errorLabel.setText("Project name is required.");
            return;
        }
        if (location.isEmpty()) {
            errorLabel.setText("Location is required.");
            return;
        }
        if (artifact.isEmpty()) {
            errorLabel.setText("Artifact is required.");
            return;
        }
        errorLabel.setText("");
        onJakartaDepsPage = true;
        if (sidebar != null) {
            sidebar.setVisible(false);
            sidebar.setManaged(false);
        }
        formScroll.setVisible(false);
        formScroll.setManaged(false);
        jakartaDepsPage.setVisible(true);
        jakartaDepsPage.setManaged(true);
        createButton.setText("Create");
        createButton.setOnAction(e -> tryCreate());
        cancelButton.setVisible(true);
        cancelButton.setManaged(true);
        previousButton.setVisible(true);
        previousButton.setManaged(true);
        previousButton.setOnAction(e -> backToJakartaForm());

        if (selectedJakartaDepIds.isEmpty()) {
            selectedJakartaDepIds.addAll(
                    dev.lumina.project.JakartaMetadata.getDefaultDependenciesForTemplate(jakartaTemplateBox.getValue()));
        }
        refreshAddedJakartaDependencies();
        jakartaTree.refresh();
    }

    private void backToJakartaForm() {
        onJakartaDepsPage = false;
        jakartaDepsPage.setVisible(false);
        jakartaDepsPage.setManaged(false);
        if (sidebar != null) {
            sidebar.setVisible(true);
            sidebar.setManaged(true);
        }
        formScroll.setVisible(true);
        formScroll.setManaged(true);
        createButton.setText("Next");
        createButton.setOnAction(e -> goToJakartaDepsPage());
        previousButton.setVisible(false);
        previousButton.setManaged(false);
    }

    private BorderPane buildJakartaDependencyPage() {
        BorderPane page = new BorderPane();
        page.getStyleClass().addAll("spring-deps-page", "jakarta-deps-page");
        page.setPadding(new Insets(16, 20, 16, 20));

        // Top bar
        HBox versionRow = new HBox(12);
        versionRow.setAlignment(Pos.CENTER_LEFT);
        Label versionLabel = new Label("Version:");
        versionLabel.getStyleClass().add("form-label");
        versionLabel.setStyle("-fx-text-fill: #A0A5B5; -fx-font-size: 13px;");

        jakartaVersionBox.setPrefWidth(160);
        jakartaVersionBox.getSelectionModel().select(dev.lumina.project.JakartaMetadata.EE_11);
        jakartaVersionBox.setOnAction(e -> {
            jakartaTree.refresh();
            TreeItem<Object> selectedItem = jakartaTree.getSelectionModel().getSelectedItem();
            if (selectedItem != null && selectedItem.getValue() instanceof dev.lumina.project.JakartaMetadata.JakartaDep dep) {
                showJakartaDependencyDetail(dep);
            }
        });

        versionRow.getChildren().addAll(versionLabel, jakartaVersionBox);

        Label depsHeader = new Label("Dependencies:");
        depsHeader.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #D8DBE6;");
        VBox.setMargin(depsHeader, new Insets(14, 0, 8, 0));

        VBox topBox = new VBox(versionRow, depsHeader);
        page.setTop(topBox);

        // Center split
        HBox center = new HBox(16);
        center.setPadding(new Insets(6, 0, 0, 0));

        // Left pane: Search field + TreeView
        VBox leftPane = new VBox(8);
        leftPane.setPrefWidth(430);
        leftPane.setMinWidth(360);

        HBox searchBox = new HBox(6);
        searchBox.setAlignment(Pos.CENTER_LEFT);
        searchBox.getStyleClass().add("search-box-container");
        searchBox.setPadding(new Insets(4, 8, 4, 8));
        searchBox.setStyle("-fx-background-color: #21242C; -fx-background-radius: 4; -fx-border-color: #363B4A; -fx-border-radius: 4;");

        SVGPath searchIcon = new SVGPath();
        searchIcon.setContent("M 6,1 C 8.8,1 11,3.2 11,6 C 11,7.2 10.6,8.3 9.9,9.1 L 13.5,12.7 L 12.7,13.5 L 9.1,9.9 C 8.3,10.6 7.2,11 6,11 C 3.2,11 1,8.8 1,6 C 1,3.2 3.2,1 6,1 Z M 6,2.2 C 3.9,2.2 2.2,3.9 2.2,6 C 2.2,8.1 3.9,9.8 6,9.8 C 8.1,9.8 9.8,8.1 9.8,6 C 9.8,3.9 8.1,2.2 6,2.2 Z");
        searchIcon.setFill(Color.web("#8B92A6"));

        jakartaSearchField.setPromptText("Search");
        jakartaSearchField.getStyleClass().add("dep-search-field");
        jakartaSearchField.setStyle("-fx-background-color: transparent; -fx-text-fill: #DFE1E5; -fx-prompt-text-fill: #72778A; -fx-border-color: transparent;");
        HBox.setHgrow(jakartaSearchField, Priority.ALWAYS);
        jakartaSearchField.textProperty().addListener((obs, old, text) -> rebuildJakartaTree(text));

        searchBox.getChildren().addAll(searchIcon, jakartaSearchField);

        jakartaTree.setShowRoot(false);
        jakartaTree.getStyleClass().add("dep-tree");
        VBox.setVgrow(jakartaTree, Priority.ALWAYS);
        jakartaTree.setCellFactory(tv -> createJakartaCell());

        jakartaTree.getSelectionModel().selectedItemProperty().addListener((obs, old, item) -> {
            if (item != null) {
                if (item.getValue() instanceof dev.lumina.project.JakartaMetadata.JakartaDep dep) {
                    showJakartaDependencyDetail(dep);
                } else if (item.getValue() instanceof dev.lumina.project.JakartaMetadata.JakartaCategory cat) {
                    showJakartaCategoryDetail(cat);
                }
            }
        });

        leftPane.getChildren().addAll(searchBox, jakartaTree);

        // Right pane: Detail section + Added dependencies section
        VBox rightPane = new VBox(16);
        HBox.setHgrow(rightPane, Priority.ALWAYS);

        // Top Details
        VBox detailBox = new VBox(8);
        detailBox.setPadding(new Insets(12, 14, 12, 14));
        detailBox.setStyle("-fx-background-color: #1E2129; -fx-background-radius: 6; -fx-border-color: #2D323E; -fx-border-radius: 6;");
        detailBox.setPrefHeight(180);
        detailBox.setMinHeight(140);

        jakartaDetailTitle.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #FFFFFF;");
        jakartaDetailTitle.setText("Full Platform");

        jakartaDetailDesc.setWrapText(true);
        jakartaDetailDesc.setStyle("-fx-font-size: 12px; -fx-text-fill: #9DA3B4; -fx-line-spacing: 2px;");
        jakartaDetailDesc.setText("Includes most of the Jakarta EE specifications. Compatible servers: GlassFish 7.x, Wildfly 27.x");

        HBox linksBox = new HBox(12);
        jakartaWebLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-padding: 0;");
        jakartaWebLink.setOnAction(e -> {
            String url = (String) jakartaWebLink.getUserData();
            if (url != null && !url.isBlank()) openBrowser(url);
        });
        jakartaWebLink.setUserData("https://jakarta.ee");

        jakartaSpecLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-padding: 0;");
        jakartaSpecLink.setOnAction(e -> {
            String url = (String) jakartaSpecLink.getUserData();
            if (url != null && !url.isBlank()) openBrowser(url);
        });
        jakartaSpecLink.setUserData("https://jakarta.ee/specifications/platform/");

        linksBox.getChildren().addAll(jakartaWebLink, jakartaSpecLink);
        detailBox.getChildren().addAll(jakartaDetailTitle, jakartaDetailDesc, linksBox);

        // Bottom Added dependencies
        VBox addedContainer = new VBox(8);
        VBox.setVgrow(addedContainer, Priority.ALWAYS);

        Label addedLabel = new Label("Added dependencies:");
        addedLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #D8DBE6;");

        ScrollPane addedScroll = new ScrollPane();
        addedScroll.setFitToWidth(true);
        addedScroll.getStyleClass().add("dep-added-scroll");
        VBox.setVgrow(addedScroll, Priority.ALWAYS);

        jakartaAddedBox.setPadding(new Insets(10));
        jakartaAddedBox.setStyle("-fx-background-color: #1A1D24; -fx-background-radius: 6; -fx-border-color: #2D323E; -fx-border-radius: 6;");
        jakartaAddedBox.setMinHeight(160);

        jakartaNoDependencies.setStyle("-fx-text-fill: #5E6476; -fx-font-size: 13px;");
        jakartaNoDependencies.setAlignment(Pos.CENTER);
        jakartaNoDependencies.setMaxWidth(Double.MAX_VALUE);
        jakartaNoDependencies.setPadding(new Insets(30, 0, 30, 0));

        jakartaAddedBox.getChildren().add(jakartaNoDependencies);
        addedScroll.setContent(jakartaAddedBox);

        addedContainer.getChildren().addAll(addedLabel, addedScroll);
        rightPane.getChildren().addAll(detailBox, addedContainer);

        center.getChildren().addAll(leftPane, rightPane);
        page.setCenter(center);

        rebuildJakartaTree("");
        return page;
    }

    private TreeCell<Object> createJakartaCell() {
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
                if (item instanceof dev.lumina.project.JakartaMetadata.JakartaCategory cat) {
                    setText(cat.name());
                    setGraphic(null);
                    getStyleClass().add("dep-category-cell");
                    setStyle("-fx-font-weight: bold; -fx-text-fill: #D8DBE6; -fx-font-size: 13px;");
                    return;
                }
                if (item instanceof dev.lumina.project.JakartaMetadata.JakartaDep dep) {
                    String currentVer = dev.lumina.project.JakartaMetadata.getVersion(dep.id(), jakartaVersionBox.getValue());
                    HBox row = new HBox(8);
                    row.setAlignment(Pos.CENTER_LEFT);

                    CheckBox cb = new CheckBox();
                    cb.getStyleClass().add("dep-checkbox");
                    cb.setSelected(selectedJakartaDepIds.contains(dep.id()));
                    cb.setOnAction(e -> {
                        if (cb.isSelected()) {
                            selectedJakartaDepIds.add(dep.id());
                        } else {
                            selectedJakartaDepIds.remove(dep.id());
                        }
                        refreshAddedJakartaDependencies();
                    });

                    Node icon = createJakartaDepIcon(dep);

                    Label nameLabel = new Label(dep.name());
                    nameLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");

                    Label versionLabel = new Label(currentVer.isBlank() ? "" : "(" + currentVer + ")");
                    versionLabel.setStyle("-fx-text-fill: #72778A; -fx-font-size: 12px;");

                    row.getChildren().addAll(cb, icon, nameLabel, versionLabel);

                    setOnMouseEntered(e -> showJakartaDependencyDetail(dep));
                    setOnMouseClicked(e -> showJakartaDependencyDetail(dep));
                    getStyleClass().add("dep-item-cell");
                    setGraphic(row);
                    setText(null);
                }
            }
        };
    }

    private Node createJakartaDepIcon(dev.lumina.project.JakartaMetadata.JakartaDep dep) {
        SVGPath svg = new SVGPath();
        if ("web-profile".equals(dep.id())) {
            svg.setContent("M 6 1 A 5 5 0 1 0 6 11 A 5 5 0 1 0 6 1 M 1 6 L 11 6 M 6 1 C 4.5 3 4.5 9 6 11 M 6 1 C 7.5 3 7.5 9 6 11");
            svg.setStroke(Color.web("#589DF6"));
            svg.setFill(Color.TRANSPARENT);
        } else if ("Implementations".equals(dep.category())) {
            svg.setContent("M 2 2 L 6 2 L 6 11 L 2 11 Z M 6 2 L 10 2 L 10 11 L 6 11 Z");
            svg.setFill(Color.web("#B388FF"));
        } else {
            svg.setContent("M 2 1 L 7 1 L 10 4 L 10 11 L 2 11 Z M 7 1 L 7 4 L 10 4");
            svg.setFill(Color.web("#6897BB"));
        }
        return svg;
    }

    private void rebuildJakartaTree(String filter) {
        String needle = filter == null ? "" : filter.trim().toLowerCase();
        TreeItem<Object> root = new TreeItem<>("root");
        for (dev.lumina.project.JakartaMetadata.JakartaCategory cat : dev.lumina.project.JakartaMetadata.getCategories()) {
            List<dev.lumina.project.JakartaMetadata.JakartaDep> matches;
            if (needle.isEmpty()) {
                matches = cat.dependencies();
            } else {
                matches = cat.dependencies().stream()
                        .filter(dep -> dep.name().toLowerCase().contains(needle)
                                || dep.id().toLowerCase().contains(needle)
                                || dep.description().toLowerCase().contains(needle))
                        .toList();
            }
            if (matches.isEmpty()) continue;

            TreeItem<Object> catItem = new TreeItem<>(cat);
            catItem.setExpanded(true);
            for (dev.lumina.project.JakartaMetadata.JakartaDep dep : matches) {
                catItem.getChildren().add(new TreeItem<>(dep));
            }
            root.getChildren().add(catItem);
        }
        jakartaTree.setRoot(root);
    }

    private void showJakartaDependencyDetail(dev.lumina.project.JakartaMetadata.JakartaDep dep) {
        if (dep == null) return;
        jakartaDetailTitle.setText(dep.name());
        jakartaDetailDesc.setText(dep.description().isBlank()
                ? "No description available for " + dep.name() : dep.description());

        if (dep.website() != null && !dep.website().isBlank()) {
            jakartaWebLink.setText("Web site \u2197");
            jakartaWebLink.setUserData(dep.website());
            jakartaWebLink.setVisible(true);
            jakartaWebLink.setManaged(true);
        } else {
            jakartaWebLink.setVisible(false);
            jakartaWebLink.setManaged(false);
        }

        if (dep.specUrl() != null && !dep.specUrl().isBlank()) {
            jakartaSpecLink.setText("Specification \u2197");
            jakartaSpecLink.setUserData(dep.specUrl());
            jakartaSpecLink.setVisible(true);
            jakartaSpecLink.setManaged(true);
        } else {
            jakartaSpecLink.setVisible(false);
            jakartaSpecLink.setManaged(false);
        }
    }

    private void showJakartaCategoryDetail(dev.lumina.project.JakartaMetadata.JakartaCategory cat) {
        jakartaDetailTitle.setText(cat.name());
        jakartaDetailDesc.setText(cat.description());
        jakartaWebLink.setVisible(false);
        jakartaWebLink.setManaged(false);
        jakartaSpecLink.setVisible(false);
        jakartaSpecLink.setManaged(false);
    }

    private void refreshAddedJakartaDependencies() {
        jakartaAddedBox.getChildren().clear();
        if (selectedJakartaDepIds.isEmpty()) {
            jakartaAddedBox.getChildren().add(jakartaNoDependencies);
            return;
        }

        for (String id : selectedJakartaDepIds) {
            dev.lumina.project.JakartaMetadata.JakartaDep dep = dev.lumina.project.JakartaMetadata.getDependency(id);
            String label = dep != null ? dep.name() : id;

            HBox row = new HBox(8);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(4, 8, 4, 8));
            row.setStyle("-fx-background-color: #212530; -fx-background-radius: 4;");

            Label name = new Label(label);
            name.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
            HBox.setHgrow(name, Priority.ALWAYS);

            Button remove = new Button("\u00D7");
            remove.getStyleClass().add("dep-added-remove");
            remove.setStyle("-fx-background-color: transparent; -fx-text-fill: #8B92A6; -fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 0 4 0 4;");
            remove.setOnAction(e -> {
                selectedJakartaDepIds.remove(id);
                refreshAddedJakartaDependencies();
                jakartaTree.refresh();
            });

            row.getChildren().addAll(name, remove);
            jakartaAddedBox.getChildren().add(row);
        }
    }

    // ----------------------------------------------------------- micronaut features

    private BorderPane buildMicronautFeaturesPage() {
        BorderPane page = new BorderPane();
        page.getStyleClass().addAll("spring-deps-page", "micronaut-deps-page");
        page.setPadding(new Insets(16, 20, 16, 20));

        // Top bar
        HBox versionRow = new HBox(12);
        versionRow.setAlignment(Pos.CENTER_LEFT);
        micronautVersionHeaderLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #FFFFFF;");
        micronautCatalogStatus.setStyle("-fx-text-fill: #72778A; -fx-font-size: 11px;");
        Region topSpacer = new Region();
        HBox.setHgrow(topSpacer, Priority.ALWAYS);
        versionRow.getChildren().addAll(micronautVersionHeaderLabel, topSpacer, micronautCatalogStatus);

        Label featuresHeader = new Label("Features:");
        featuresHeader.setStyle("-fx-font-size: 13px; -fx-text-fill: #A0A5B5;");
        VBox.setMargin(featuresHeader, new Insets(10, 0, 8, 0));

        VBox topBox = new VBox(versionRow, featuresHeader);
        page.setTop(topBox);

        // Center split: Left tree, Right details & added
        HBox center = new HBox(16);
        center.setPadding(new Insets(6, 0, 0, 0));

        // Left pane: Search field + TreeView
        VBox leftPane = new VBox(8);
        leftPane.setPrefWidth(450);
        leftPane.setMinWidth(360);

        HBox searchBox = new HBox(6);
        searchBox.setAlignment(Pos.CENTER_LEFT);
        searchBox.getStyleClass().add("search-box-container");
        searchBox.setPadding(new Insets(4, 8, 4, 8));
        searchBox.setStyle("-fx-background-color: #21242C; -fx-background-radius: 4; -fx-border-color: #363B4A; -fx-border-radius: 4;");

        SVGPath searchIcon = new SVGPath();
        searchIcon.setContent("M 6,1 C 8.8,1 11,3.2 11,6 C 11,7.2 10.6,8.3 9.9,9.1 L 13.5,12.7 L 12.7,13.5 L 9.1,9.9 C 8.3,10.6 7.2,11 6,11 C 3.2,11 1,8.8 1,6 C 1,3.2 3.2,1 6,1 Z M 6,2.2 C 3.9,2.2 2.2,3.9 2.2,6 C 2.2,8.1 3.9,9.8 6,9.8 C 8.1,9.8 9.8,8.1 9.8,6 C 9.8,3.9 8.1,2.2 6,2.2 Z");
        searchIcon.setFill(Color.web("#8B92A6"));

        micronautSearchField.setPromptText("Search");
        micronautSearchField.getStyleClass().add("dep-search-field");
        micronautSearchField.setStyle("-fx-background-color: transparent; -fx-text-fill: #DFE1E5; -fx-prompt-text-fill: #72778A; -fx-border-color: transparent;");
        HBox.setHgrow(micronautSearchField, Priority.ALWAYS);
        micronautSearchField.textProperty().addListener((obs, old, text) -> rebuildMicronautTree(text));

        searchBox.getChildren().addAll(searchIcon, micronautSearchField);

        micronautTree.setShowRoot(false);
        micronautTree.getStyleClass().add("dep-tree");
        VBox.setVgrow(micronautTree, Priority.ALWAYS);
        micronautTree.setCellFactory(tv -> createMicronautCell());

        micronautTree.getSelectionModel().selectedItemProperty().addListener((obs, old, item) -> {
            if (item != null) {
                if (item.getValue() instanceof dev.lumina.project.MicronautMetadata.MicronautFeature feat) {
                    showMicronautFeatureDetail(feat);
                } else if (item.getValue() instanceof String catName) {
                    showMicronautCategoryDetail(catName);
                }
            }
        });

        leftPane.getChildren().addAll(searchBox, micronautTree);

        // Right pane: Detail section + Added features section
        VBox rightPane = new VBox(16);
        HBox.setHgrow(rightPane, Priority.ALWAYS);

        // Top Details
        VBox detailBox = new VBox(8);
        detailBox.setPadding(new Insets(12, 14, 12, 14));
        detailBox.setStyle("-fx-background-color: #1E2129; -fx-background-radius: 6; -fx-border-color: #2D323E; -fx-border-radius: 6;");
        detailBox.setPrefHeight(160);
        detailBox.setMinHeight(130);

        micronautDetailTitle.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #FFFFFF;");
        micronautDetailTitle.setText("Server");

        micronautDetailDesc.setWrapText(true);
        micronautDetailDesc.setStyle("-fx-font-size: 12px; -fx-text-fill: #9DA3B4; -fx-line-spacing: 2px;");
        micronautDetailDesc.setText("");

        detailBox.getChildren().addAll(micronautDetailTitle, micronautDetailDesc);

        // Bottom Added features
        VBox addedContainer = new VBox(8);
        VBox.setVgrow(addedContainer, Priority.ALWAYS);

        Label addedLabel = new Label("Added features:");
        addedLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #D8DBE6;");

        ScrollPane addedScroll = new ScrollPane();
        addedScroll.setFitToWidth(true);
        addedScroll.getStyleClass().add("dep-added-scroll");
        VBox.setVgrow(addedScroll, Priority.ALWAYS);

        micronautAddedBox.setPadding(new Insets(10));
        micronautAddedBox.setStyle("-fx-background-color: #1A1D24; -fx-background-radius: 6; -fx-border-color: #2D323E; -fx-border-radius: 6;");
        micronautAddedBox.setMinHeight(160);

        micronautNoFeatures.setStyle("-fx-text-fill: #5E6476; -fx-font-size: 13px;");
        micronautNoFeatures.setAlignment(Pos.CENTER);
        micronautNoFeatures.setMaxWidth(Double.MAX_VALUE);
        micronautNoFeatures.setPadding(new Insets(30, 0, 30, 0));

        micronautAddedBox.getChildren().add(micronautNoFeatures);
        addedScroll.setContent(micronautAddedBox);

        addedContainer.getChildren().addAll(addedLabel, addedScroll);

        rightPane.getChildren().addAll(detailBox, addedContainer);

        center.getChildren().addAll(leftPane, rightPane);
        page.setCenter(center);

        rebuildMicronautTree("");
        return page;
    }

    private TreeCell<Object> createMicronautCell() {
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
                if (item instanceof String catName) {
                    setText(catName);
                    setGraphic(null);
                    getStyleClass().add("dep-category-cell");
                    return;
                }
                if (item instanceof dev.lumina.project.MicronautMetadata.MicronautFeature feat) {
                    CheckBox cb = new CheckBox(feat.title());
                    cb.getStyleClass().add("dep-checkbox");
                    cb.setSelected(selectedMicronautFeatureIds.contains(feat.name()));
                    cb.setOnAction(e -> {
                        if (cb.isSelected()) {
                            selectedMicronautFeatureIds.add(feat.name());
                        } else {
                            selectedMicronautFeatureIds.remove(feat.name());
                        }
                        refreshAddedMicronautFeatures();
                    });
                    setOnMouseEntered(e -> showMicronautFeatureDetail(feat));
                    setOnMouseClicked(e -> showMicronautFeatureDetail(feat));
                    getStyleClass().add("dep-item-cell");
                    setGraphic(cb);
                    setText(null);
                }
            }
        };
    }

    private void rebuildMicronautTree(String filter) {
        String needle = filter == null ? "" : filter.trim().toLowerCase();
        TreeItem<Object> root = new TreeItem<>("root");
        for (dev.lumina.project.MicronautMetadata.MicronautCategory cat : micronautCategories) {
            List<dev.lumina.project.MicronautMetadata.MicronautFeature> matches;
            if (needle.isEmpty()) {
                matches = cat.features();
            } else {
                matches = cat.features().stream()
                        .filter(f -> f.title().toLowerCase().contains(needle)
                                || f.name().toLowerCase().contains(needle)
                                || f.description().toLowerCase().contains(needle))
                        .toList();
            }
            if (matches.isEmpty()) continue;

            TreeItem<Object> catItem = new TreeItem<>(cat.name());
            catItem.setExpanded(!needle.isEmpty() || cat.name().equalsIgnoreCase("Server"));
            for (dev.lumina.project.MicronautMetadata.MicronautFeature f : matches) {
                catItem.getChildren().add(new TreeItem<>(f));
            }
            root.getChildren().add(catItem);
        }
        micronautTree.setRoot(root);
    }

    private void showMicronautFeatureDetail(dev.lumina.project.MicronautMetadata.MicronautFeature feat) {
        if (feat == null) return;
        micronautDetailTitle.setText(feat.title());
        micronautDetailDesc.setText(feat.description().isBlank()
                ? "No description available for " + feat.title() : feat.description());
    }

    private void showMicronautCategoryDetail(String categoryName) {
        micronautDetailTitle.setText(categoryName);
        micronautDetailDesc.setText("");
    }

    private void refreshAddedMicronautFeatures() {
        micronautAddedBox.getChildren().clear();
        if (selectedMicronautFeatureIds.isEmpty()) {
            micronautAddedBox.getChildren().add(micronautNoFeatures);
            return;
        }

        for (String name : selectedMicronautFeatureIds) {
            dev.lumina.project.MicronautMetadata.MicronautFeature feat = findMicronautFeature(name);
            String label = feat != null ? feat.title() : name;

            HBox row = new HBox(8);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(4, 8, 4, 8));
            row.setStyle("-fx-background-color: #212530; -fx-background-radius: 4;");

            Label nameLbl = new Label(label);
            nameLbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
            HBox.setHgrow(nameLbl, Priority.ALWAYS);

            Button remove = new Button("\u00D7");
            remove.getStyleClass().add("dep-added-remove");
            remove.setStyle("-fx-background-color: transparent; -fx-text-fill: #8B92A6; -fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 0 4 0 4;");
            remove.setOnAction(e -> {
                selectedMicronautFeatureIds.remove(name);
                refreshAddedMicronautFeatures();
                micronautTree.refresh();
            });

            row.getChildren().addAll(nameLbl, remove);
            micronautAddedBox.getChildren().add(row);
        }
    }

    private dev.lumina.project.MicronautMetadata.MicronautFeature findMicronautFeature(String name) {
        for (dev.lumina.project.MicronautMetadata.MicronautCategory cat : micronautCategories) {
            for (dev.lumina.project.MicronautMetadata.MicronautFeature feat : cat.features()) {
                if (feat.name().equals(name)) return feat;
            }
        }
        return null;
    }

    private void kickOffMicronautMetadataFetch() {
        if (cachedMicronautCatalog != null) {
            applyMicronautCatalog(cachedMicronautCatalog);
            return;
        }
        if (micronautFetchStarted) return;
        micronautFetchStarted = true;
        micronautCatalogStatus.setText("Checking launch.micronaut.io …");

        Thread worker = new Thread(() -> {
            try {
                String appType = micronautAppTypeBox.getValue() != null
                        ? micronautAppTypeBox.getValue().value() : "default";
                dev.lumina.project.MicronautMetadata.MicronautCatalog catalog =
                        dev.lumina.project.MicronautMetadata.fetchCatalog(micronautServerUrl, appType);
                cachedMicronautCatalog = catalog;
                Platform.runLater(() -> applyMicronautCatalog(catalog));
            } catch (Exception e) {
                micronautFetchFailed = true;
                Platform.runLater(() -> {
                    micronautCatalogStatus.setText("");
                    applyMicronautCatalog(dev.lumina.project.MicronautMetadata.FALLBACK_CATALOG);
                });
            }
        }, "lumina-micronaut-metadata-fetch");
        worker.setDaemon(true);
        worker.start();
    }

    private void applyMicronautCatalog(dev.lumina.project.MicronautMetadata.MicronautCatalog catalog) {
        if (catalog.version() != null && !catalog.version().isBlank()) {
            micronautVersion = catalog.version();
            micronautVersionHeaderLabel.setText("Micronaut: " + micronautVersion);
        }
        if (catalog.categories() != null && !catalog.categories().isEmpty()) {
            micronautCategories = catalog.categories();
            rebuildMicronautTree(micronautSearchField.getText());
        }
        micronautCatalogStatus.setText("");
    }

    private void loadMicronautFeaturesForAppType(String appType) {
        micronautCatalogStatus.setText("Updating features …");
        Thread worker = new Thread(() -> {
            try {
                java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
                        .connectTimeout(java.time.Duration.ofSeconds(6))
                        .build();
                List<dev.lumina.project.MicronautMetadata.MicronautCategory> categories =
                        dev.lumina.project.MicronautMetadata.fetchFeatures(client,
                                dev.lumina.project.MicronautMetadata.normalizeServerUrl(micronautServerUrl), appType);
                Platform.runLater(() -> {
                    if (categories != null && !categories.isEmpty()) {
                        micronautCategories = categories;
                        rebuildMicronautTree(micronautSearchField.getText());
                    }
                    micronautCatalogStatus.setText("");
                });
            } catch (Exception e) {
                Platform.runLater(() -> micronautCatalogStatus.setText(""));
            }
        }, "lumina-micronaut-features-fetch");
        worker.setDaemon(true);
        worker.start();
    }

    private void goToMicronautFeaturesPage() {
        String name = nameField.getText().trim();
        String location = locationField.getText().trim();
        String artifact = artifactField.getText().trim();
        if (name.isEmpty()) {
            errorLabel.setText("Project name is required.");
            return;
        }
        if (location.isEmpty()) {
            errorLabel.setText("Location is required.");
            return;
        }
        if (artifact.isEmpty()) {
            errorLabel.setText("Artifact is required.");
            return;
        }
        errorLabel.setText("");
        onMicronautFeaturesPage = true;
        if (sidebar != null) {
            sidebar.setVisible(false);
            sidebar.setManaged(false);
        }
        formScroll.setVisible(false);
        formScroll.setManaged(false);
        micronautFeaturesPage.setVisible(true);
        micronautFeaturesPage.setManaged(true);
        createButton.setText("Create");
        createButton.setOnAction(e -> tryCreate());
        cancelButton.setVisible(true);
        cancelButton.setManaged(true);
        previousButton.setVisible(true);
        previousButton.setManaged(true);
        previousButton.setOnAction(e -> backToMicronautForm());

        kickOffMicronautMetadataFetch();
    }

    private void backToMicronautForm() {
        onMicronautFeaturesPage = false;
        micronautFeaturesPage.setVisible(false);
        micronautFeaturesPage.setManaged(false);
        if (sidebar != null) {
            sidebar.setVisible(true);
            sidebar.setManaged(true);
        }
        formScroll.setVisible(true);
        formScroll.setManaged(true);
        createButton.setText("Next");
        createButton.setOnAction(e -> goToMicronautFeaturesPage());
        previousButton.setVisible(false);
        previousButton.setManaged(false);
    }

    // -------------------------------------------------------- Ktor wizard page & methods

    private BorderPane buildKtorPluginsPage() {
        BorderPane page = new BorderPane();
        page.getStyleClass().add("ktor-plugins-page");

        HBox mainLayout = new HBox(0);
        mainLayout.setStyle("-fx-background-color: #1E1F22;");

        // Left Column (width 400px):
        VBox leftCol = new VBox(0);
        leftCol.setPrefWidth(400);
        leftCol.setMinWidth(350);
        leftCol.setMaxWidth(460);
        leftCol.setStyle("-fx-background-color: #1E1F22; -fx-border-color: #393B40; -fx-border-width: 0 1 0 0;");

        // Search bar at top
        HBox searchBox = new HBox(8);
        searchBox.setAlignment(Pos.CENTER_LEFT);
        searchBox.setPadding(new Insets(10, 14, 10, 14));
        searchBox.setStyle("-fx-background-color: #1E1F22; -fx-border-color: #393B40; -fx-border-width: 0 0 1 0;");

        Label searchIcon = new Label("🔍");
        searchIcon.setStyle("-fx-font-size: 13px; -fx-text-fill: #8B92A6;");
        ktorSearchField.setPromptText("Type / to see options");
        ktorSearchField.setStyle("-fx-background-color: transparent; -fx-text-fill: #DFE1E5; -fx-prompt-text-fill: #6F737A; -fx-border-color: transparent; -fx-font-size: 13px;");
        HBox.setHgrow(ktorSearchField, Priority.ALWAYS);

        searchBox.getChildren().addAll(searchIcon, ktorSearchField);

        // Status header row
        HBox statusRow = new HBox(8);
        statusRow.setAlignment(Pos.CENTER_LEFT);
        statusRow.setPadding(new Insets(8, 14, 8, 14));
        statusRow.setStyle("-fx-background-color: #1E1F22; -fx-border-color: #2B2D30; -fx-border-width: 0 0 1 0;");

        ktorPluginsCountLabel.setStyle("-fx-text-fill: #8B92A6; -fx-font-size: 12px;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        ktorShowAddedLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-underline: false;");
        ktorShowAddedLink.setOnAction(e -> {
            ktorShowOnlyAdded = !ktorShowOnlyAdded;
            ktorShowAddedLink.setText(ktorShowOnlyAdded ? "Show all" : "Show");
            rebuildKtorCards(ktorSearchField.getText());
        });

        statusRow.getChildren().addAll(ktorPluginsCountLabel, spacer, ktorShowAddedLink);

        // Cards ScrollPane
        ktorCardsBox.setPadding(new Insets(4, 0, 4, 0));
        ScrollPane scrollCards = new ScrollPane(ktorCardsBox);
        scrollCards.setFitToWidth(true);
        scrollCards.setStyle("-fx-background: #1E1F22; -fx-background-color: #1E1F22; -fx-border-color: transparent;");
        VBox.setVgrow(scrollCards, Priority.ALWAYS);

        // Bottom progress / indicator bar
        HBox bottomIndicatorBar = new HBox();
        bottomIndicatorBar.setAlignment(Pos.CENTER_RIGHT);
        bottomIndicatorBar.setPadding(new Insets(4, 14, 8, 14));
        Region bar = new Region();
        bar.setPrefSize(44, 4);
        bar.setStyle("-fx-background-color: #357444; -fx-background-radius: 2;");
        bottomIndicatorBar.getChildren().add(bar);

        leftCol.getChildren().addAll(searchBox, statusRow, scrollCards, bottomIndicatorBar);

        // Right Column:
        VBox rightCol = new VBox(0);
        rightCol.setStyle("-fx-background-color: #1E1F22;");
        HBox.setHgrow(rightCol, Priority.ALWAYS);

        // Right Header
        HBox detailHeader = new HBox(16);
        detailHeader.setAlignment(Pos.CENTER_LEFT);
        detailHeader.setPadding(new Insets(16, 24, 16, 24));
        detailHeader.setStyle("-fx-background-color: #1E1F22; -fx-border-color: #393B40; -fx-border-width: 0 0 1 0;");

        Node headerIcon = createLargeKtorIcon();

        VBox headerTitles = new VBox(4);
        ktorDetailName = new Label("AsyncAPI");
        ktorDetailName.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #DFE1E5;");

        HBox subHeaderRow = new HBox(12);
        subHeaderRow.setAlignment(Pos.CENTER_LEFT);
        ktorDetailVendor = new Hyperlink("AsyncAPI");
        ktorDetailVendor.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 13px; -fx-padding: 0;");
        ktorDetailVendor.setOnAction(e -> {
            if (selectedKtorPlugin != null && !selectedKtorPlugin.githubUrl().isBlank()) {
                openBrowser(selectedKtorPlugin.githubUrl());
            }
        });

        ktorDetailVersion = new Label("1.0.0");
        ktorDetailVersion.setStyle("-fx-text-fill: #8B92A6; -fx-font-size: 13px;");

        ktorDetailGithub = new Hyperlink("See plugin's Github \u2197");
        ktorDetailGithub.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 13px; -fx-padding: 0;");
        ktorDetailGithub.setOnAction(e -> {
            if (selectedKtorPlugin != null && !selectedKtorPlugin.githubUrl().isBlank()) {
                openBrowser(selectedKtorPlugin.githubUrl());
            }
        });

        subHeaderRow.getChildren().addAll(ktorDetailVendor, ktorDetailVersion, ktorDetailGithub);
        headerTitles.getChildren().addAll(ktorDetailName, subHeaderRow);

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);

        ktorDetailActionBtn = new Button("Add");
        ktorDetailActionBtn.setPrefWidth(88);
        updateKtorButtonAppearance(ktorDetailActionBtn, false);
        ktorDetailActionBtn.setOnAction(e -> {
            if (selectedKtorPlugin != null) {
                toggleKtorPlugin(selectedKtorPlugin.id());
            }
        });

        detailHeader.getChildren().addAll(headerIcon, headerTitles, headerSpacer, ktorDetailActionBtn);

        // Right Content ScrollPane
        ktorDetailContentBox = new VBox(16);
        ktorDetailContentBox.setPadding(new Insets(24, 28, 28, 28));
        ScrollPane detailScroll = new ScrollPane(ktorDetailContentBox);
        detailScroll.setFitToWidth(true);
        detailScroll.setStyle("-fx-background: #1E1F22; -fx-background-color: #1E1F22; -fx-border-color: transparent;");
        VBox.setVgrow(detailScroll, Priority.ALWAYS);

        rightCol.getChildren().addAll(detailHeader, detailScroll);

        mainLayout.getChildren().addAll(leftCol, rightCol);
        page.setCenter(mainLayout);

        ktorSearchField.textProperty().addListener((obs, oldV, newV) -> rebuildKtorCards(newV));

        rebuildKtorCards("");
        if (!ktorPluginsList.isEmpty()) {
            showKtorPluginDetail(ktorPluginsList.get(0));
        }

        return page;
    }

    private Node createLargeKtorIcon() {
        Polygon p1 = new Polygon(11.25, 1.5, 21, 11.25, 11.25, 11.25);
        p1.setFill(Color.web("#7F52FF"));

        Polygon p2 = new Polygon(1.5, 11.25, 11.25, 11.25, 11.25, 21);
        p2.setFill(Color.web("#C757BC"));

        Polygon p3 = new Polygon(11.25, 11.25, 21, 21, 1.5, 21);
        p3.setFill(Color.web("#E24A4A"));

        Group ktor = new Group(p1, p2, p3);
        StackPane sp = new StackPane(ktor);
        sp.setPrefSize(28, 28);
        return sp;
    }

    private void rebuildKtorCards(String filter) {
        ktorCardsBox.getChildren().clear();
        String needle = filter == null ? "" : filter.trim().toLowerCase();

        for (dev.lumina.project.KtorMetadata.KtorPlugin p : ktorPluginsList) {
            boolean isAdded = selectedKtorPluginIds.contains(p.id());
            if (ktorShowOnlyAdded && !isAdded) continue;

            if (!needle.isEmpty()) {
                boolean match = p.name().toLowerCase().contains(needle)
                        || p.id().toLowerCase().contains(needle)
                        || p.description().toLowerCase().contains(needle)
                        || p.category().toLowerCase().contains(needle);
                if (!match) continue;
            }

            HBox card = new HBox(12);
            card.setAlignment(Pos.CENTER_LEFT);
            card.setPadding(new Insets(10, 14, 10, 14));

            boolean isCurrent = selectedKtorPlugin != null && selectedKtorPlugin.id().equals(p.id());
            if (isCurrent) {
                card.setStyle("-fx-background-color: #2E436E; -fx-cursor: hand;");
            } else {
                card.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");
                card.setOnMouseEntered(e -> {
                    if (selectedKtorPlugin == null || !selectedKtorPlugin.id().equals(p.id())) {
                        card.setStyle("-fx-background-color: #26282E; -fx-cursor: hand;");
                    }
                });
                card.setOnMouseExited(e -> {
                    if (selectedKtorPlugin == null || !selectedKtorPlugin.id().equals(p.id())) {
                        card.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");
                    }
                });
            }

            Node icon = dev.lumina.ui.GeneratorIcons.getIcon("Ktor");

            VBox textCol = new VBox(3);
            Label title = new Label(p.name());
            title.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold;");
            Label desc = new Label(p.description());
            desc.setStyle("-fx-text-fill: #8B92A6; -fx-font-size: 11px;");
            desc.setMaxWidth(220);
            desc.setTextOverrun(OverrunStyle.ELLIPSIS);
            textCol.getChildren().addAll(title, desc);

            Region cardSpacer = new Region();
            HBox.setHgrow(cardSpacer, Priority.ALWAYS);

            Button btn = new Button();
            btn.setPrefWidth(72);
            updateKtorButtonAppearance(btn, isAdded);
            btn.setOnAction(e -> {
                toggleKtorPlugin(p.id());
                e.consume();
            });

            card.getChildren().addAll(icon, textCol, cardSpacer, btn);

            card.setOnMouseClicked(e -> {
                showKtorPluginDetail(p);
                rebuildKtorCards(ktorSearchField.getText());
            });

            ktorCardsBox.getChildren().add(card);
        }
    }

    private void updateKtorButtonAppearance(Button btn, boolean isAdded) {
        if (isAdded) {
            btn.setText("Remove");
            btn.setStyle("-fx-background-color: #1E1F22; -fx-border-color: #357444; -fx-border-radius: 4; -fx-background-radius: 4; -fx-text-fill: #438C56; -fx-font-size: 12px; -fx-font-weight: bold; -fx-cursor: hand;");
        } else {
            btn.setText("Add");
            btn.setStyle("-fx-background-color: #357444; -fx-border-color: #357444; -fx-border-radius: 4; -fx-background-radius: 4; -fx-text-fill: #FFFFFF; -fx-font-size: 12px; -fx-font-weight: bold; -fx-cursor: hand;");
        }
    }

    private void toggleKtorPlugin(String id) {
        if (selectedKtorPluginIds.contains(id)) {
            selectedKtorPluginIds.remove(id);
        } else {
            selectedKtorPluginIds.add(id);
        }
        int count = selectedKtorPluginIds.size();
        ktorPluginsCountLabel.setText(count + (count == 1 ? " plugin added" : " plugins added"));
        if (selectedKtorPlugin != null) {
            updateKtorButtonAppearance(ktorDetailActionBtn, selectedKtorPluginIds.contains(selectedKtorPlugin.id()));
        }
        rebuildKtorCards(ktorSearchField.getText());
    }

    private void showKtorPluginDetail(dev.lumina.project.KtorMetadata.KtorPlugin p) {
        if (p == null) return;
        selectedKtorPlugin = p;
        ktorDetailName.setText(p.name());
        ktorDetailVendor.setText(p.group());
        ktorDetailVersion.setText(p.version());
        updateKtorButtonAppearance(ktorDetailActionBtn, selectedKtorPluginIds.contains(p.id()));

        ktorDetailContentBox.getChildren().clear();

        Label descHeader = new Label("Description");
        descHeader.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #DFE1E5;");

        Label descBody = new Label(p.description());
        descBody.setWrapText(true);
        descBody.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 14px; -fx-line-spacing: 4px;");

        ktorDetailContentBox.getChildren().addAll(descHeader, descBody);

        if (!p.usageMarkdown().isBlank()) {
            Label usageHeader = new Label("Usage");
            usageHeader.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #DFE1E5; -fx-padding: 12 0 0 0;");
            ktorDetailContentBox.getChildren().add(usageHeader);

            renderKtorUsage(ktorDetailContentBox, p.usageMarkdown());
        }
    }

    private void renderKtorUsage(VBox container, String markdown) {
        String[] lines = markdown.split("\n");
        boolean inCode = false;
        StringBuilder codeBuilder = new StringBuilder();

        for (String line : lines) {
            if (line.trim().startsWith("```")) {
                if (inCode) {
                    container.getChildren().add(createCodeSnippetBox(codeBuilder.toString().trim()));
                    codeBuilder.setLength(0);
                    inCode = false;
                } else {
                    inCode = true;
                }
                continue;
            }
            if (inCode) {
                codeBuilder.append(line).append("\n");
            } else {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("###") || trimmed.startsWith("The `")) continue;
                if (trimmed.startsWith("- ")) {
                    HBox bulletRow = new HBox(8);
                    bulletRow.setAlignment(Pos.TOP_LEFT);
                    Label bullet = new Label("•");
                    bullet.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
                    Label text = new Label(trimmed.substring(2));
                    text.setWrapText(true);
                    text.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
                    bulletRow.getChildren().addAll(bullet, text);
                    container.getChildren().add(bulletRow);
                } else {
                    Label text = new Label(trimmed);
                    text.setWrapText(true);
                    text.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-line-spacing: 3px;");
                    container.getChildren().add(text);
                }
            }
        }
        if (inCode && !codeBuilder.isEmpty()) {
            container.getChildren().add(createCodeSnippetBox(codeBuilder.toString().trim()));
        }
    }

    private VBox createCodeSnippetBox(String code) {
        VBox box = new VBox(4);
        box.setPadding(new Insets(10, 14, 10, 14));
        box.setStyle("-fx-background-color: #141517; -fx-background-radius: 6; -fx-border-color: #2B2D30; -fx-border-radius: 6;");

        TextArea area = new TextArea(code);
        area.setEditable(false);
        area.setWrapText(false);
        area.setStyle("-fx-font-family: 'JetBrains Mono', monospace; -fx-font-size: 12px; -fx-text-fill: #DFE1E5; -fx-background-color: transparent; -fx-border-color: transparent;");
        area.setPrefRowCount(Math.min(10, Math.max(3, code.split("\n").length)));
        box.getChildren().add(area);
        return box;
    }

    private void kickOffKtorPluginsFetch() {
        if (ktorFetchStarted) return;
        ktorFetchStarted = true;
        Thread.ofVirtual().start(() -> {
            List<dev.lumina.project.KtorMetadata.KtorPlugin> fetched =
                    dev.lumina.project.KtorMetadata.fetchPlugins(ktorServerUrl);
            if (fetched != null && !fetched.isEmpty()) {
                Platform.runLater(() -> {
                    ktorPluginsList.clear();
                    ktorPluginsList.addAll(fetched);
                    rebuildKtorCards(ktorSearchField.getText());
                });
            }
        });
    }

    private void goToKtorPluginsPage() {
        String name = nameField.getText().trim();
        String location = locationField.getText().trim();
        String artifact = artifactField.getText().trim();
        if (name.isEmpty()) {
            errorLabel.setText("Project name is required.");
            return;
        }
        if (location.isEmpty()) {
            errorLabel.setText("Location is required.");
            return;
        }
        if (artifact.isEmpty()) {
            errorLabel.setText("Artifact is required.");
            return;
        }
        errorLabel.setText("");
        onKtorPluginsPage = true;
        if (sidebar != null) {
            sidebar.setVisible(false);
            sidebar.setManaged(false);
        }
        formScroll.setVisible(false);
        formScroll.setManaged(false);
        ktorPluginsPage.setVisible(true);
        ktorPluginsPage.setManaged(true);
        createButton.setText("Create");
        createButton.setOnAction(e -> tryCreate());
        cancelButton.setVisible(true);
        cancelButton.setManaged(true);
        previousButton.setVisible(true);
        previousButton.setManaged(true);
        previousButton.setOnAction(e -> backToKtorForm());

        kickOffKtorPluginsFetch();
    }

    private void backToKtorForm() {
        onKtorPluginsPage = false;
        ktorPluginsPage.setVisible(false);
        ktorPluginsPage.setManaged(false);
        if (sidebar != null) {
            sidebar.setVisible(true);
            sidebar.setManaged(true);
        }
        formScroll.setVisible(true);
        formScroll.setManaged(true);
        createButton.setText("Next");
        createButton.setOnAction(e -> goToKtorPluginsPage());
        previousButton.setVisible(false);
        previousButton.setManaged(false);
    }

    private static void openBrowser(String url) {
        try {
            if (java.awt.Desktop.isDesktopSupported()
                    && java.awt.Desktop.getDesktop().isSupported(java.awt.Desktop.Action.BROWSE)) {
                java.awt.Desktop.getDesktop().browse(java.net.URI.create(url));
            }
        } catch (Exception ignored) {
        }
    }

    // ------------------------------------------------------------- html logic

    private void initHtmlControls() {
        if (htmlInitialized) return;
        htmlInitialized = true;

        htmlVersionBox.setItems(FXCollections.observableArrayList(dev.lumina.project.HtmlMetadata.getH5bpVersions()));
        htmlVersionBox.getSelectionModel().selectFirst();
        htmlVersionBox.getStyleClass().add("choice-box");

        htmlRefreshButton.getStyleClass().addAll("console-button", "html-refresh-btn");
        Tooltip.install(htmlRefreshButton, new Tooltip("Check for new versions"));
        htmlRefreshButton.setOnAction(e -> refreshHtmlVersions(true));

        htmlProjectTypeGroup.selectedToggleProperty().addListener((obs, old, toggle) -> {
            if (toggle instanceof ToggleButton tb) {
                boolean isBootstrap = tb.getText().equalsIgnoreCase(dev.lumina.project.HtmlMetadata.TYPE_BOOTSTRAP);
                List<String> versions = isBootstrap
                        ? dev.lumina.project.HtmlMetadata.getBootstrapVersions()
                        : dev.lumina.project.HtmlMetadata.getH5bpVersions();
                htmlVersionBox.setItems(FXCollections.observableArrayList(versions));
                htmlVersionBox.getSelectionModel().selectFirst();
                refreshHtmlVersions(false);
            }
        });

        refreshHtmlVersions(false);
    }

    private String getSelectedHtmlProjectType() {
        Toggle toggle = htmlProjectTypeGroup.getSelectedToggle();
        if (toggle instanceof ToggleButton tb) {
            return tb.getText();
        }
        return dev.lumina.project.HtmlMetadata.TYPE_H5BP;
    }

    private void refreshHtmlVersions(boolean force) {
        if (htmlFetching) return;
        htmlFetching = true;
        htmlRefreshButton.setDisable(true);
        String currentType = getSelectedHtmlProjectType();

        Thread t = new Thread(() -> {
            try {
                List<String> versions = dev.lumina.project.HtmlMetadata.fetchVersions(currentType, force);
                Platform.runLater(() -> {
                    if (getSelectedHtmlProjectType().equalsIgnoreCase(currentType)) {
                        String currentSelected = htmlVersionBox.getValue();
                        htmlVersionBox.setItems(FXCollections.observableArrayList(versions));
                        if (currentSelected != null && versions.contains(currentSelected)) {
                            htmlVersionBox.getSelectionModel().select(currentSelected);
                        } else {
                            htmlVersionBox.getSelectionModel().selectFirst();
                        }
                    }
                });
            } finally {
                Platform.runLater(() -> {
                    htmlFetching = false;
                    htmlRefreshButton.setDisable(false);
                });
            }
        }, "lumina-html-version-fetcher");
        t.setDaemon(true);
        t.start();
    }

    // ------------------------------------------------------------ react logic

    private void initReactControls() {
        if (reactInitialized) return;
        reactInitialized = true;

        reactNodeInterpreterBox.getStyleClass().add("choice-box");
        reactNodeInterpreterBox.setPrefWidth(360);
        reactCliBox.getStyleClass().add("choice-box");
        reactCliBox.setPrefWidth(360);

        reactCliLabel.getStyleClass().add("form-label");

        reactNodeBrowseBtn.getStyleClass().addAll("console-button", "react-browse-btn");
        reactCliBrowseBtn.getStyleClass().addAll("console-button", "react-browse-btn");
        Tooltip.install(reactNodeBrowseBtn, new Tooltip("Select Node.js interpreter executable"));
        Tooltip.install(reactCliBrowseBtn, new Tooltip("Specify custom CLI version or package"));

        // FileChooser for Node interpreter
        reactNodeBrowseBtn.setOnAction(e -> pickNodeExecutable());

        // File/version chooser for CLI
        reactCliBrowseBtn.setOnAction(e -> pickCliVersion());

        // Node interpreter dropdown selection listener
        reactNodeInterpreterBox.valueProperty().addListener((obs, old, val) -> {
            if (val == null) return;
            if (NodeMetadata.ACTION_ADD.equals(val)) {
                Platform.runLater(this::pickNodeExecutable);
            } else if (NodeMetadata.ACTION_DOWNLOAD.equals(val)) {
                Platform.runLater(() -> {
                    reactNodeInterpreterBox.setValue(lastValidNodeInterpreter);
                    new DownloadNodeDialog(stage, installed -> {
                        String display = installed.formatDisplay();
                        if (!reactNodeInterpreterBox.getItems().contains(display)) {
                            reactNodeInterpreterBox.getItems().add(0, display);
                        }
                        reactNodeInterpreterBox.setValue(display);
                        lastValidNodeInterpreter = display;
                    }).show();
                });
            } else {
                lastValidNodeInterpreter = val;
            }
        });

        // CLI box selection listener
        reactCliBox.valueProperty().addListener((obs, old, val) -> {
            if (val == null) return;
            if (ReactMetadata.ACTION_SELECT.equals(val)) {
                Platform.runLater(this::pickCliVersion);
            }
        });

        // Populate Node interpreters
        refreshNodeInterpreters();

        // Populate initial CLI
        updateCliBoxForType(ReactMetadata.TYPE_REACT);

        // Build Advisory warning box matching screenshot
        buildReactAdvisory();

        // Project type change listener
        reactProjectTypeGroup.selectedToggleProperty().addListener((obs, old, toggle) -> {
            if (toggle instanceof ToggleButton tb) {
                String type = tb.getText();
                reactCliLabel.setText(ReactMetadata.getCliLabel(type));
                updateCliBoxForType(type);
                boolean isReact = ReactMetadata.TYPE_REACT.equalsIgnoreCase(type);
                boolean isReactNative = ReactMetadata.TYPE_REACT_NATIVE.equalsIgnoreCase(type);
                reactAdvisoryBox.setVisible(isReact);
                reactAdvisoryBox.setManaged(isReact);
                reactTsCheck.setVisible(!isReactNative);
                reactTsCheck.setManaged(!isReactNative);
            }
        });
    }

    private void refreshNodeInterpreters() {
        Thread t = new Thread(() -> {
            var interpreters = NodeMetadata.detectInterpreters(false);
            Platform.runLater(() -> {
                ObservableList<String> items = FXCollections.observableArrayList();
                for (var interp : interpreters) {
                    items.add(interp.formatDisplay());
                }
                items.add(NodeMetadata.ACTION_ADD);
                items.add(NodeMetadata.ACTION_DOWNLOAD);
                reactNodeInterpreterBox.setItems(items);
                if (!items.isEmpty()) {
                    reactNodeInterpreterBox.getSelectionModel().selectFirst();
                    lastValidNodeInterpreter = items.getFirst();
                }
            });
        }, "lumina-node-detector");
        t.setDaemon(true);
        t.start();
    }

    private void updateCliBoxForType(String type) {
        List<String> versions = ReactMetadata.getVersions(type);
        ObservableList<String> items = FXCollections.observableArrayList();
        if (ReactMetadata.TYPE_REACT_NATIVE.equalsIgnoreCase(type)) {
            // In IntelliJ IDEA, React Native displays primary version (e.g. 20.2.0) and Select...
            String primaryVer = versions.isEmpty() ? "20.2.0" : versions.getFirst();
            items.add(ReactMetadata.formatCliDisplay(type, primaryVer));
        } else if (ReactMetadata.TYPE_NEXT_JS.equalsIgnoreCase(type)) {
            // In IntelliJ IDEA, Next.js displays primary version (e.g. 16.3.5) and Select...
            String primaryVer = versions.isEmpty() ? "16.3.5" : versions.getFirst();
            items.add(ReactMetadata.formatCliDisplay(type, primaryVer));
        } else {
            for (String v : versions) {
                items.add(ReactMetadata.formatCliDisplay(type, v));
            }
        }
        items.add(ReactMetadata.ACTION_SELECT);
        reactCliBox.setItems(items);
        reactCliBox.getSelectionModel().selectFirst();

        // Async fetch latest from npm
        Thread t = new Thread(() -> {
            String latest = ReactMetadata.fetchLatestVersion(type, false);
            if (latest != null && !latest.isBlank()) {
                Platform.runLater(() -> {
                    if (getSelectedReactProjectType().equalsIgnoreCase(type)) {
                        String formatted = ReactMetadata.formatCliDisplay(type, latest);
                        if (!reactCliBox.getItems().contains(formatted)) {
                            reactCliBox.getItems().add(0, formatted);
                            reactCliBox.getSelectionModel().selectFirst();
                        }
                    }
                });
            }
        }, "lumina-npm-fetcher");
        t.setDaemon(true);
        t.start();
    }

    private void buildReactAdvisory() {
        reactAdvisoryBox.getChildren().clear();
        reactAdvisoryBox.setPadding(new Insets(6, 0, 0, 0));

        javafx.scene.text.Text text1 = new javafx.scene.text.Text("Using the ");
        text1.setFill(Color.web("#E06C75"));

        javafx.scene.text.Text textBold = new javafx.scene.text.Text("create-react-app");
        textBold.setFill(Color.web("#E06C75"));
        textBold.setStyle("-fx-font-weight: bold;");

        javafx.scene.text.Text text2 = new javafx.scene.text.Text(" is not the advised method for creating React applications. The preferred\napproach is to use a template with the ");
        text2.setFill(Color.web("#E06C75"));

        javafx.scene.text.Text textLink = new javafx.scene.text.Text("Vite bundler");
        textLink.setFill(Color.web("#61AFEF"));
        textLink.setUnderline(true);
        textLink.setCursor(javafx.scene.Cursor.HAND);
        textLink.setOnMouseClicked(e -> {
            for (GeneratorEntry entry : allSidebarEntries) {
                if (entry.generator() == ProjectSpec.Generator.VITE) {
                    selectSidebarEntry(entry);
                    break;
                }
            }
        });

        javafx.scene.text.Text text3 = new javafx.scene.text.Text(" when using React without a framework.");
        text3.setFill(Color.web("#E06C75"));

        javafx.scene.text.TextFlow textFlow = new javafx.scene.text.TextFlow(text1, textBold, text2, textLink, text3);
        textFlow.setLineSpacing(3);
        reactAdvisoryBox.getChildren().setAll(textFlow);
    }

    private void pickNodeExecutable() {
        javafx.stage.FileChooser chooser = new javafx.stage.FileChooser();
        chooser.setTitle("Select Node.js Interpreter");
        File initial = new File("/usr/local/bin");
        if (!initial.exists()) initial = new File("/opt/homebrew/bin");
        if (initial.exists()) chooser.setInitialDirectory(initial);
        File file = chooser.showOpenDialog(stage);
        if (file != null && file.canExecute()) {
            String ver = NodeMetadata.probeVersion(file.getAbsolutePath());
            var interp = new NodeMetadata.NodeInterpreter(file.getName(), file.getAbsolutePath(), ver != null ? ver : "custom", false);
            String display = interp.formatDisplay();
            if (!reactNodeInterpreterBox.getItems().contains(display)) {
                reactNodeInterpreterBox.getItems().add(0, display);
            }
            reactNodeInterpreterBox.setValue(display);
            lastValidNodeInterpreter = display;
        } else {
            reactNodeInterpreterBox.setValue(lastValidNodeInterpreter);
        }
    }

    private void pickCliVersion() {
        String type = getSelectedReactProjectType();
        String currentVer = getSelectedReactCliVersion();
        new SelectCliVersionDialog(stage, type, currentVer, selectedVer -> {
            String formatted = ReactMetadata.formatCliDisplay(type, selectedVer);
            if (!reactCliBox.getItems().contains(formatted)) {
                reactCliBox.getItems().add(0, formatted);
            }
            reactCliBox.setValue(formatted);
        }).show();
    }

    private String getSelectedReactProjectType() {
        Toggle t = reactProjectTypeGroup.getSelectedToggle();
        if (t instanceof ToggleButton tb) {
            return tb.getText();
        }
        return ReactMetadata.TYPE_REACT;
    }

    private String getSelectedReactNodeInterpreter() {
        String val = reactNodeInterpreterBox.getValue();
        if (val == null || val.isBlank() || NodeMetadata.ACTION_ADD.equals(val)
                || NodeMetadata.ACTION_DOWNLOAD.equals(val)) {
            return "/usr/local/bin/node";
        }
        String s = val.trim();
        if (s.startsWith("node")) {
            s = s.substring(4).trim();
        }
        String[] parts = s.split("\\s+");
        if (parts.length > 0 && !parts[0].isBlank()) {
            return parts[0].trim();
        }
        return "/usr/local/bin/node";
    }

    private String getSelectedReactCliVersion() {
        String val = reactCliBox.getValue();
        String type = getSelectedReactProjectType();
        String defaultVer = ReactMetadata.TYPE_REACT_NATIVE.equalsIgnoreCase(type) ? "20.2.0" : "5.1.0";
        if (val == null || val.isBlank() || ReactMetadata.ACTION_SELECT.equals(val)) {
            return defaultVer;
        }
        String[] parts = val.trim().split("\\s+");
        if (parts.length > 0) {
            return parts[parts.length - 1].trim();
        }
        return defaultVer;
    }

    // ------------------------------------------------------------ express logic

    private void initExpressControls() {
        if (expressInitialized) return;
        expressInitialized = true;

        expressNodeInterpreterBox.getStyleClass().add("choice-box");
        expressNodeInterpreterBox.setPrefWidth(360);
        expressCliBox.getStyleClass().add("choice-box");
        expressCliBox.setPrefWidth(360);
        expressViewEngineBox.getStyleClass().add("choice-box");
        expressViewEngineBox.setPrefWidth(240);
        expressStylesheetEngineBox.getStyleClass().add("choice-box");
        expressStylesheetEngineBox.setPrefWidth(240);

        expressNodeBrowseBtn.getStyleClass().addAll("console-button", "react-browse-btn");
        expressCliBrowseBtn.getStyleClass().addAll("console-button", "react-browse-btn");
        Tooltip.install(expressNodeBrowseBtn, new Tooltip("Select Node.js interpreter executable"));
        Tooltip.install(expressCliBrowseBtn, new Tooltip("Specify custom express-generator CLI version or package"));

        // FileChooser for Node interpreter
        expressNodeBrowseBtn.setOnAction(e -> pickExpressNodeExecutable());

        // File/version chooser for CLI
        expressCliBrowseBtn.setOnAction(e -> pickExpressCliVersion());

        // Node interpreter dropdown selection listener
        expressNodeInterpreterBox.valueProperty().addListener((obs, old, val) -> {
            if (val == null) return;
            if (NodeMetadata.ACTION_ADD.equals(val)) {
                Platform.runLater(this::pickExpressNodeExecutable);
            } else if (NodeMetadata.ACTION_DOWNLOAD.equals(val)) {
                Platform.runLater(() -> {
                    expressNodeInterpreterBox.setValue(lastValidExpressNodeInterpreter);
                    new DownloadNodeDialog(stage, installed -> {
                        String display = installed.formatDisplay();
                        if (!expressNodeInterpreterBox.getItems().contains(display)) {
                            expressNodeInterpreterBox.getItems().add(0, display);
                        }
                        expressNodeInterpreterBox.setValue(display);
                        lastValidExpressNodeInterpreter = display;
                    }).show();
                });
            } else {
                lastValidExpressNodeInterpreter = val;
            }
        });

        // CLI box selection listener
        expressCliBox.valueProperty().addListener((obs, old, val) -> {
            if (val == null) return;
            if (ExpressMetadata.ACTION_SELECT.equals(val)) {
                Platform.runLater(this::pickExpressCliVersion);
            }
        });

        // Populate View Engines dynamically from metadata
        expressViewEngineBox.getItems().clear();
        for (ExpressMetadata.ViewEngine ve : ExpressMetadata.getViewEngines()) {
            expressViewEngineBox.getItems().add(ve.name());
        }
        expressViewEngineBox.setValue(ExpressMetadata.getDefaultViewEngine().name());

        // Populate Stylesheet Engines dynamically from metadata
        expressStylesheetEngineBox.getItems().clear();
        for (ExpressMetadata.StylesheetEngine se : ExpressMetadata.getStylesheetEngines()) {
            expressStylesheetEngineBox.getItems().add(se.name());
        }
        expressStylesheetEngineBox.setValue(ExpressMetadata.getDefaultStylesheetEngine().name());

        // Populate CLI versions
        ObservableList<String> cliItems = FXCollections.observableArrayList();
        cliItems.add(ExpressMetadata.formatCliDisplay(ExpressMetadata.DEFAULT_VERSION));
        cliItems.add(ExpressMetadata.ACTION_SELECT);
        expressCliBox.setItems(cliItems);
        expressCliBox.getSelectionModel().selectFirst();

        // Async fetch latest version from npm registry
        Thread t = new Thread(() -> {
            String latest = ExpressMetadata.fetchLatestVersion(false);
            if (latest != null && !latest.isBlank() && !latest.equals(ExpressMetadata.DEFAULT_VERSION)) {
                Platform.runLater(() -> {
                    String formatted = ExpressMetadata.formatCliDisplay(latest);
                    if (!expressCliBox.getItems().contains(formatted)) {
                        expressCliBox.getItems().add(0, formatted);
                        expressCliBox.getSelectionModel().selectFirst();
                    }
                });
            }
        }, "lumina-express-npm-fetcher");
        t.setDaemon(true);
        t.start();
    }

    private void refreshExpressNodeInterpreters() {
        if (!reactNodeInterpreterBox.getItems().isEmpty()) {
            expressNodeInterpreterBox.setItems(FXCollections.observableArrayList(reactNodeInterpreterBox.getItems()));
            if (reactNodeInterpreterBox.getValue() != null) {
                expressNodeInterpreterBox.setValue(reactNodeInterpreterBox.getValue());
                lastValidExpressNodeInterpreter = reactNodeInterpreterBox.getValue();
            } else {
                expressNodeInterpreterBox.getSelectionModel().selectFirst();
                lastValidExpressNodeInterpreter = expressNodeInterpreterBox.getItems().getFirst();
            }
            return;
        }

        Thread t = new Thread(() -> {
            var interpreters = NodeMetadata.detectInterpreters(false);
            Platform.runLater(() -> {
                ObservableList<String> items = FXCollections.observableArrayList();
                for (var interp : interpreters) {
                    items.add(interp.formatDisplay());
                }
                items.add(NodeMetadata.ACTION_ADD);
                items.add(NodeMetadata.ACTION_DOWNLOAD);
                expressNodeInterpreterBox.setItems(items);
                if (!items.isEmpty()) {
                    expressNodeInterpreterBox.getSelectionModel().selectFirst();
                    lastValidExpressNodeInterpreter = items.getFirst();
                }
            });
        }, "lumina-express-node-detector");
        t.setDaemon(true);
        t.start();
    }

    private void pickExpressNodeExecutable() {
        javafx.stage.FileChooser chooser = new javafx.stage.FileChooser();
        chooser.setTitle("Select Node.js Interpreter");
        File initial = new File("/usr/local/bin");
        if (!initial.exists()) initial = new File("/opt/homebrew/bin");
        if (initial.exists()) chooser.setInitialDirectory(initial);
        File file = chooser.showOpenDialog(stage);
        if (file != null && file.canExecute()) {
            String ver = NodeMetadata.probeVersion(file.getAbsolutePath());
            var interp = new NodeMetadata.NodeInterpreter(file.getName(), file.getAbsolutePath(), ver != null ? ver : "custom", false);
            String display = interp.formatDisplay();
            if (!expressNodeInterpreterBox.getItems().contains(display)) {
                expressNodeInterpreterBox.getItems().add(0, display);
            }
            expressNodeInterpreterBox.setValue(display);
            lastValidExpressNodeInterpreter = display;
        } else {
            expressNodeInterpreterBox.setValue(lastValidExpressNodeInterpreter);
        }
    }

    private void pickExpressCliVersion() {
        String currentVer = getSelectedExpressCliVersion();
        new SelectCliVersionDialog(stage, ExpressMetadata.TYPE_EXPRESS, currentVer, selectedVer -> {
            String formatted = ExpressMetadata.formatCliDisplay(selectedVer);
            if (!expressCliBox.getItems().contains(formatted)) {
                expressCliBox.getItems().add(0, formatted);
            }
            expressCliBox.setValue(formatted);
        }).show();
    }

    private String getSelectedExpressNodeInterpreter() {
        String val = expressNodeInterpreterBox.getValue();
        if (val == null || val.isBlank() || NodeMetadata.ACTION_ADD.equals(val)
                || NodeMetadata.ACTION_DOWNLOAD.equals(val)) {
            return "/usr/local/bin/node";
        }
        String s = val.trim();
        if (s.startsWith("node")) {
            s = s.substring(4).trim();
        }
        String[] parts = s.split("\\s+");
        if (parts.length > 0 && !parts[0].isBlank()) {
            return parts[0].trim();
        }
        return "/usr/local/bin/node";
    }

    private String getSelectedExpressCliVersion() {
        String val = expressCliBox.getValue();
        if (val == null || val.isBlank() || ExpressMetadata.ACTION_SELECT.equals(val)) {
            return ExpressMetadata.DEFAULT_VERSION;
        }
        String[] parts = val.trim().split("\\s+");
        if (parts.length > 0) {
            return parts[parts.length - 1].trim();
        }
        return ExpressMetadata.DEFAULT_VERSION;
    }

    private String getSelectedExpressViewEngine() {
        String val = expressViewEngineBox.getValue();
        return val != null && !val.isBlank() ? val : ExpressMetadata.getDefaultViewEngine().name();
    }

    private String getSelectedExpressStylesheetEngine() {
        String val = expressStylesheetEngineBox.getValue();
        return val != null && !val.isBlank() ? val : ExpressMetadata.getDefaultStylesheetEngine().name();
    }

    private HBox buildButtons() {
        errorLabel.getStyleClass().add("form-error");

        helpButton.getStyleClass().add("wizard-help-button");
        helpButton.setOnAction(e -> showHelp());

        cancelButton = new Button("Cancel");
        cancelButton.getStyleClass().add("dialog-secondary");
        cancelButton.setOnAction(e -> stage.close());

        previousButton = new Button("Previous");
        previousButton.getStyleClass().add("dialog-secondary");
        previousButton.setOnAction(e -> backToSpringForm());
        previousButton.setVisible(false);
        previousButton.setManaged(false);

        createButton = new Button("Create");
        createButton.getStyleClass().add("dialog-primary");
        createButton.setDefaultButton(true);
        createButton.setOnAction(e -> tryCreate());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox box = new HBox(10, helpButton, cancelButton, errorLabel, spacer,
                previousButton, createButton);
        box.setAlignment(Pos.CENTER_RIGHT);
        box.setPadding(new Insets(12, 20, 14, 20));
        box.getStyleClass().add("dialog-footer");
        return box;
    }

    private void showHelp() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.initOwner(stage);
        alert.setTitle("New Project");
        alert.setHeaderText(selected.label());
        if (selected.generator() == ProjectSpec.Generator.QUARKUS) {
            alert.setContentText("Generates a Quarkus application via code.quarkus.io, with dynamic stream selection and full extension catalog.");
        } else if (selected.generator() == ProjectSpec.Generator.JAKARTA_EE) {
            alert.setContentText("Generates enterprise Java applications with Jakarta EE specifications and implementations, dynamically configuring Maven and Gradle build descriptors.");
        } else if (selected.generator() == ProjectSpec.Generator.SPRING_BOOT) {
            alert.setContentText("Generates a project via start.spring.io, the same service "
                    + "IntelliJ uses, with full access to starters and versions.");
        } else if (selected.generator() == ProjectSpec.Generator.GO) {
            alert.setContentText("Generates a Go project with Go modules, vendoring support, environment variables, and sample code matching IntelliJ IDEA / GoLand.");
        } else {
            alert.setContentText("Project configuration for " + selected.label());
        }
        alert.getDialogPane().getStylesheets().add(
                getClass().getResource("/css/lumina-dark.css").toExternalForm());
        alert.showAndWait();
    }

    private void buildMavenArchetypeForm() {
        Label intro = new Label("To create a general Maven project, go to the Java page.");
        intro.getStyleClass().add("form-hint");

        GridPane fields = new GridPane();
        fields.setHgap(12);
        fields.setVgap(14);
        ColumnConstraints labelColumn = new ColumnConstraints(96);
        ColumnConstraints valueColumn = new ColumnConstraints();
        valueColumn.setHgrow(Priority.ALWAYS);
        fields.getColumnConstraints().addAll(labelColumn, valueColumn);

        catalogCombo.getSelectionModel().select("Internal");
        catalogCombo.setPrefWidth(275);
        manageCatalogsButton.getStyleClass().add("maven-link");
        manageCatalogsButton.setOnAction(e -> showCatalogManager());
        HBox catalog = new HBox(8, catalogCombo, manageCatalogsButton);
        catalog.setAlignment(Pos.CENTER_LEFT);

        archetypeCombo.getItems().setAll(
                "org.apache.maven.archetypes:maven-archetype-archetype",
                "org.apache.maven.archetypes:maven-archetype-j2ee-simple",
                "org.apache.maven.archetypes:maven-archetype-plugin",
                "org.apache.maven.archetypes:maven-archetype-plugin-site",
                "org.apache.maven.archetypes:maven-archetype-portlet",
                "org.apache.maven.archetypes:maven-archetype-quickstart",
                "org.apache.maven.archetypes:maven-archetype-site",
                "org.apache.maven.archetypes:maven-archetype-site-simple",
                "org.apache.maven.archetypes:maven-archetype-webapp");
        archetypeCombo.setEditable(true);
        archetypeCombo.getSelectionModel().select("org.apache.maven.archetypes:maven-archetype-quickstart");
        HBox.setHgrow(archetypeCombo, Priority.ALWAYS);
        addArchetypeButton.getStyleClass().add("dialog-secondary");
        addArchetypeButton.setOnAction(e -> addArchetype());
        HBox archetype = new HBox(8, archetypeCombo, addArchetypeButton);
        archetype.setAlignment(Pos.CENTER_LEFT);

        archetypeVersionBox.getSelectionModel().select("1.4");
        archetypeVersionBox.setPrefWidth(110);

        fields.add(formLabel("Catalog:  \u24D8"), 0, 0);
        fields.add(catalog, 1, 0);
        fields.add(formLabel("Archetype:  \u24D8"), 0, 1);
        fields.add(archetype, 1, 1);
        fields.add(formLabel("Version:"), 0, 2);
        fields.add(archetypeVersionBox, 1, 2);

        configurePropertiesTable();
        Button addProperty = new Button("+");
        Button removeProperty = new Button("\u2212");
        addProperty.getStyleClass().add("property-button");
        removeProperty.getStyleClass().add("property-button");
        addProperty.setOnAction(e -> {
            PropertyEntry property = new PropertyEntry();
            additionalProperties.add(property);
            propertiesTable.getSelectionModel().select(property);
            propertiesTable.edit(additionalProperties.size() - 1, propertiesTable.getColumns().get(0));
        });
        removeProperty.setOnAction(e -> {
            PropertyEntry property = propertiesTable.getSelectionModel().getSelectedItem();
            if (property != null) additionalProperties.remove(property);
        });
        HBox propertyButtons = new HBox(5, addProperty, removeProperty);
        propertyButtons.getStyleClass().add("property-toolbar");

        Label propertiesTitle = new Label("Additional Properties");
        propertiesTitle.getStyleClass().add("maven-section-title");
        VBox properties = new VBox(5, propertiesTitle, propertyButtons, propertiesTable);
        properties.getStyleClass().add("maven-properties");

        Label advancedTitle = new Label("\u2304  Advanced Settings");
        advancedTitle.getStyleClass().add("maven-section-title");
        GridPane advanced = new GridPane();
        advanced.setHgap(12);
        advanced.setVgap(10);
        advanced.getColumnConstraints().addAll(new ColumnConstraints(96), new ColumnConstraints());
        advanced.add(formLabel("GroupId:  \u24D8"), 0, 0);
        advanced.add(mavenGroupField, 1, 0);
        advanced.add(formLabel("ArtifactId:  \u24D8"), 0, 1);
        advanced.add(mavenArtifactField, 1, 1);
        advanced.add(formLabel("Version:"), 0, 2);
        advanced.add(projectVersionField, 1, 2);
        for (Node field : List.of(mavenGroupField, mavenArtifactField, projectVersionField)) {
            ((TextField) field).setPrefWidth(275);
        }
        VBox advancedSection = new VBox(12, advancedTitle, advanced);
        advancedSection.getStyleClass().add("maven-advanced");

        mavenArchetypeBox.getChildren().setAll(intro, fields, properties, advancedSection);
        mavenArchetypeBox.setVisible(false);
        mavenArchetypeBox.setManaged(false);
    }

    private void configurePropertiesTable() {
        propertiesTable.setItems(additionalProperties);
        propertiesTable.setEditable(true);
        propertiesTable.getStyleClass().add("maven-properties-table");
        propertiesTable.setPrefHeight(145);
        propertiesTable.setPlaceholder(new Label("No properties"));

        TableColumn<PropertyEntry, String> name = new TableColumn<>("Name");
        name.setCellValueFactory(cell -> cell.getValue().nameProperty());
        name.setCellFactory(TextFieldTableCell.<PropertyEntry>forTableColumn());
        name.setOnEditCommit(e -> e.getRowValue().setName(e.getNewValue()));
        name.setPrefWidth(230);
        TableColumn<PropertyEntry, String> value = new TableColumn<>("Value");
        value.setCellValueFactory(cell -> cell.getValue().valueProperty());
        value.setCellFactory(TextFieldTableCell.<PropertyEntry>forTableColumn());
        value.setOnEditCommit(e -> e.getRowValue().setValue(e.getNewValue()));
        value.setPrefWidth(330);
        propertiesTable.getColumns().setAll(name, value);
    }

    private void setupRustControls() {
        List<String> toolchains = dev.lumina.project.RustMetadata.discoverToolchains();
        rustToolchainBox.setEditable(true);
        rustToolchainBox.getItems().setAll(toolchains);
        String defaultToolchain = dev.lumina.project.RustMetadata.defaultToolchainPath();
        if (!rustToolchainBox.getItems().contains(defaultToolchain) && !defaultToolchain.isBlank()) {
            rustToolchainBox.getItems().add(0, defaultToolchain);
        }
        rustToolchainBox.getSelectionModel().select(defaultToolchain);
        rustToolchainBox.getEditor().setText(defaultToolchain);
        rustToolchainBox.setPrefWidth(470);
        HBox.setHgrow(rustToolchainBox, Priority.ALWAYS);

        rustToolchainBrowseBtn.setGraphic(createBrowseFolderIcon());
        rustToolchainBrowseBtn.getStyleClass().add("console-button");
        rustToolchainBrowseBtn.setOnAction(e -> chooseRustToolchain());
        rustToolchainRow.getChildren().setAll(rustToolchainBox, rustToolchainBrowseBtn);
        rustToolchainRow.setAlignment(Pos.CENTER_LEFT);

        rustVersionLabel.getStyleClass().add("form-static");

        HBox.setHgrow(rustStdlibField, Priority.ALWAYS);
        rustStdlibBrowseBtn.setGraphic(createBrowseFolderIcon());
        rustStdlibBrowseBtn.getStyleClass().add("console-button");
        rustStdlibBrowseBtn.setOnAction(e -> chooseRustStdlib());
        rustStdlibRow.getChildren().setAll(rustStdlibField, rustStdlibBrowseBtn);
        rustStdlibRow.setAlignment(Pos.CENTER_LEFT);

        rustEnvironmentField.setPromptText("Environment variables");
        HBox.setHgrow(rustEnvironmentField, Priority.ALWAYS);
        rustEnvironmentBtn.setGraphic(GeneratorIcons.envVariablesIcon());
        rustEnvironmentBtn.getStyleClass().add("console-button");
        rustEnvironmentBtn.setOnAction(e -> {
            EnvironmentVariablesDialog.show(stage, rustEnvironmentField.getText().trim())
                    .ifPresent(rustEnvironmentField::setText);
        });
        rustEnvironmentRow.getChildren().setAll(rustEnvironmentField, rustEnvironmentBtn);
        rustEnvironmentRow.setAlignment(Pos.CENTER_LEFT);

        rustInstallCargoGenerateLink.getStyleClass().setAll("cargo-generate-link");
        rustCargoGenerateStatusLabel.setStyle("-fx-text-fill: #8C919D; -fx-font-size: 12px;");
        rustCargoGenerateBox.setAlignment(Pos.CENTER_LEFT);
        rustCargoGenerateBox.setPadding(new Insets(4, 0, 0, 0));
        rustInstallCargoGenerateLink.setOnAction(e -> {
            rustInstallCargoGenerateLink.setDisable(true);
            rustCargoGenerateStatusLabel.setText("Installing cargo-generate...");
            String p = rustToolchainBox.getEditor().getText().trim();
            if (p.isBlank() && rustToolchainBox.getValue() != null) {
                p = rustToolchainBox.getValue().trim();
            }
            final String toolchain = p;
            new Thread(() -> {
                int exitCode = dev.lumina.project.RustMetadata.installCargoGenerate(toolchain, msg -> {
                    javafx.application.Platform.runLater(() -> rustCargoGenerateStatusLabel.setText(msg));
                });
                boolean success = (exitCode == 0);
                javafx.application.Platform.runLater(() -> {
                    rustInstallCargoGenerateLink.setDisable(false);
                    if (success) {
                        rustCargoGenerateStatusLabel.setText("cargo-generate installed successfully.");
                        rustCargoGenerateBox.setVisible(false);
                        rustCargoGenerateBox.setManaged(false);
                    } else {
                        rustCargoGenerateStatusLabel.setText("Installation failed. Run 'cargo install cargo-generate' manually.");
                    }
                });
            }, "cargo-generate-installer").start();
        });

        Runnable updateCargoGenerateState = () -> {
            String p = rustToolchainBox.getEditor().getText().trim();
            if (p.isBlank() && rustToolchainBox.getValue() != null) {
                p = rustToolchainBox.getValue().trim();
            }
            final String toolchain = p;
            new Thread(() -> {
                boolean installed = dev.lumina.project.RustMetadata.isCargoGenerateInstalled(toolchain);
                javafx.application.Platform.runLater(() -> {
                    rustCargoGenerateBox.setVisible(!installed);
                    rustCargoGenerateBox.setManaged(!installed);
                    if (!installed) {
                        rustCargoGenerateStatusLabel.setText("");
                    }
                });
            }, "cargo-generate-detector").start();
        };

        Runnable updateRustInfo = () -> {
            String p = rustToolchainBox.getEditor().getText().trim();
            if (p.isBlank() && rustToolchainBox.getValue() != null) {
                p = rustToolchainBox.getValue().trim();
            }
            String ver = dev.lumina.project.RustMetadata.detectRustVersion(p);
            rustVersionLabel.setText(ver);
            String stdlib = dev.lumina.project.RustMetadata.detectStandardLibrary(p);
            if (!stdlib.isBlank()) {
                rustStdlibField.setText(stdlib);
            }
            updateCargoGenerateState.run();
        };
        rustToolchainBox.valueProperty().addListener((obs, oldV, newV) -> updateRustInfo.run());
        rustToolchainBox.getEditor().textProperty().addListener((obs, oldV, newV) -> updateRustInfo.run());
        updateRustInfo.run();

        // Project Template Header
        Label templatesTitle = new Label("Project Template");
        templatesTitle.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-font-weight: normal;");
        Region separator = new Region();
        separator.setMinHeight(1);
        separator.setPrefHeight(1);
        separator.setMaxHeight(1);
        separator.setStyle("-fx-background-color: #393B40;");
        HBox.setHgrow(separator, Priority.ALWAYS);
        rustTemplateHeaderRow.getChildren().setAll(templatesTitle, separator);
        rustTemplateHeaderRow.setAlignment(Pos.CENTER_LEFT);
        rustTemplateHeaderRow.setSpacing(8);
        rustTemplateHeaderRow.setPadding(new Insets(10, 0, 4, 0));

        // Project Templates list
        rustTemplates.clear();
        for (var t : dev.lumina.project.RustMetadata.defaultTemplates()) {
            rustTemplates.add(new RustTemplate(t.name(), t.url(), t.value(), t.builtIn()));
        }
        rustTemplateList.setItems(rustTemplates);
        rustTemplateList.getStyleClass().setAll("rust-template-list");
        rustTemplateList.setPrefHeight(130);
        rustTemplateList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(RustTemplate item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    HBox row = new HBox(8);
                    row.setAlignment(Pos.CENTER_LEFT);
                    Node icon = item.builtIn()
                            ? GeneratorIcons.rustIcon()
                            : GeneratorIcons.rustTemplateMacroIcon();
                    Label name = new Label(item.label());
                    name.getStyleClass().add("rust-template-name");
                    row.getChildren().addAll(icon, name);
                    if (item.detail() != null && !item.detail().isBlank()) {
                        Label detail = new Label(item.detail());
                        detail.getStyleClass().add("rust-template-detail");
                        row.getChildren().add(detail);
                    }
                    setText(null);
                    setGraphic(row);
                }
            }
        });
        if (!rustTemplates.isEmpty()) {
            rustTemplateList.getSelectionModel().select(0);
        }

        Button addTemplate = new Button("+");
        addTemplate.setOnAction(e -> showAddRustTemplate());
        Button removeTemplate = new Button("\u2212");
        removeTemplate.setOnAction(e -> {
            RustTemplate selectedTemplate = rustTemplateList.getSelectionModel().getSelectedItem();
            if (selectedTemplate != null && !selectedTemplate.builtIn()) {
                rustTemplates.remove(selectedTemplate);
            }
        });
        rustTemplateToolbar.getChildren().setAll(addTemplate, removeTemplate);
        rustTemplateToolbar.getStyleClass().setAll("rust-template-toolbar");

        rustTemplateContainer.getChildren().setAll(rustTemplateList, rustTemplateToolbar);
        rustTemplateContainer.setSpacing(0);
    }

    private void setupGoControls() {
        List<GoMetadata.GoSdk> sdks = GoMetadata.discoverGoRoots();
        goRootBox.getItems().setAll(sdks);
        goRootBox.setPromptText("<No SDK>");
        goRootBox.setPrefWidth(380);
        HBox.setHgrow(goRootBox, Priority.ALWAYS);

        goRootBox.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(GoMetadata.GoSdk item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.formatDisplay());
                }
            }
        });
        goRootBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(GoMetadata.GoSdk item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.formatDisplay());
                }
            }
        });

        ContextMenu addSdkMenu = new ContextMenu();
        MenuItem localItem = new MenuItem("Local…");
        localItem.setOnAction(e -> chooseLocalGoSdk());
        MenuItem downloadItem = new MenuItem("Download…");
        downloadItem.setOnAction(e -> downloadGoSdk());
        addSdkMenu.getItems().setAll(localItem, downloadItem);
        addSdkMenu.getStyleClass().add("catalog-menu");

        goAddSdkBtn.getStyleClass().add("console-button");
        goAddSdkBtn.setOnAction(e -> {
            addSdkMenu.show(goAddSdkBtn, javafx.geometry.Side.BOTTOM, 0, 0);
        });

        if (!sdks.isEmpty()) {
            goRootBox.getSelectionModel().select(0);
            goRootRow.getChildren().setAll(goRootBox, goAddSdkBtn);
        } else {
            goRootRow.getChildren().setAll(goAddSdkBtn);
        }
        goRootRow.setAlignment(Pos.CENTER_LEFT);

        goVendoringCheck.setSelected(true);
        goVendoringCheck.setStyle("-fx-font-size: 12px;");
        goVendoringHelp.setStyle("-fx-text-fill: #707890; -fx-font-size: 10px; -fx-cursor: hand; "
                + "-fx-border-color: #707890; -fx-border-radius: 8; -fx-min-width: 14px; "
                + "-fx-alignment: center; -fx-padding: 0 2 0 2;");
        Tooltip.install(goVendoringHelp, new Tooltip("Vendoring support: automatically run 'go mod vendor' or use vendor folder"));
        goVendoringRow.getChildren().setAll(goVendoringCheck, goVendoringHelp);
        goVendoringRow.setAlignment(Pos.CENTER_LEFT);
        goVendoringRow.setSpacing(4);

        goEnvironmentField.setPromptText("Environment variables");
        HBox.setHgrow(goEnvironmentField, Priority.ALWAYS);
        goEnvironmentBtn.setGraphic(GeneratorIcons.envVariablesIcon());
        goEnvironmentBtn.getStyleClass().add("console-button");
        goEnvironmentBtn.setOnAction(e -> {
            EnvironmentVariablesDialog.show(stage, goEnvironmentField.getText().trim())
                    .ifPresent(goEnvironmentField::setText);
        });
        goEnvironmentRow.getChildren().setAll(goEnvironmentField, goEnvironmentBtn);
        goEnvironmentRow.setAlignment(Pos.CENTER_LEFT);

        goEnvironmentSubtext.setStyle("-fx-text-fill: #8C919D; -fx-font-size: 11px;");

        goSampleCodeCheck.setSelected(true);
        goSampleCodeCheck.setStyle("-fx-font-size: 12px;");
    }

    private void chooseLocalGoSdk() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select GOROOT Directory");
        File chosen = chooser.showDialog(stage);
        if (chosen != null) {
            if (!GoMetadata.isValidGoRoot(chosen)) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.initOwner(stage);
                alert.setTitle("Invalid GOROOT");
                alert.setHeaderText("Invalid GOROOT Location");
                alert.setContentText("The chosen directory does not appear to contain a valid Go SDK (missing bin/go or VERSION).");
                alert.showAndWait();
                return;
            }
            String ver = GoMetadata.detectGoVersion(chosen.getAbsolutePath());
            GoMetadata.GoSdk sdk = new GoMetadata.GoSdk(
                    "Go " + (ver.startsWith("go") ? ver.substring(2) : ver),
                    ver,
                    chosen.getAbsolutePath(),
                    true
            );
            if (!goRootBox.getItems().contains(sdk)) {
                goRootBox.getItems().add(0, sdk);
            }
            goRootBox.getSelectionModel().select(sdk);
            goRootRow.getChildren().setAll(goRootBox, goAddSdkBtn);
        }
    }

    private void downloadGoSdk() {
        DownloadGoSdkDialog dialog = new DownloadGoSdkDialog(stage, sdk -> {
            if (sdk != null) {
                if (!goRootBox.getItems().contains(sdk)) {
                    goRootBox.getItems().add(0, sdk);
                }
                goRootBox.getSelectionModel().select(sdk);
                goRootRow.getChildren().setAll(goRootBox, goAddSdkBtn);
            }
        });
        dialog.show();
    }

    private void chooseRustToolchain() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Rust Toolchain Location");
        File chosen = chooser.showDialog(stage);
        if (chosen != null) {
            String path = chosen.getAbsolutePath();
            if (!rustToolchainBox.getItems().contains(path)) {
                rustToolchainBox.getItems().add(path);
            }
            rustToolchainBox.getSelectionModel().select(path);
            rustToolchainBox.getEditor().setText(path);
        }
    }

    private void chooseRustStdlib() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Rust Standard Library Location");
        File chosen = chooser.showDialog(stage);
        if (chosen != null) {
            rustStdlibField.setText(chosen.getAbsolutePath());
        }
    }

    private void showAddRustTemplate() {
        Stage dialog = new Stage();
        dialog.initOwner(stage);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Add Custom Template");

        TextField url = new TextField();
        TextField name = new TextField();

        javafx.scene.text.Text textBefore = new javafx.scene.text.Text("The template will be generated with ");
        textBefore.setFill(Color.web("#8C919D"));
        Hyperlink link = new Hyperlink("cargo-generate \u2197");
        link.setStyle("-fx-padding: 0; -fx-border-width: 0; -fx-text-fill: #589DF6;");
        link.setOnAction(e -> {
            try {
                java.awt.Desktop.getDesktop().browse(java.net.URI.create("https://github.com/cargo-generate/cargo-generate"));
            } catch (Exception ignored) {
            }
        });
        javafx.scene.text.Text textAfter = new javafx.scene.text.Text(". You can provide a link to any GitHub project.");
        textAfter.setFill(Color.web("#8C919D"));
        javafx.scene.text.TextFlow note = new javafx.scene.text.TextFlow(textBefore, link, textAfter);

        url.textProperty().addListener((obs, oldV, newV) -> {
            if (name.getText().isBlank() && newV != null && !newV.isBlank()) {
                String clean = newV.replaceAll(".*/", "").replaceAll("\\.git$", "");
                if (!clean.isBlank()) {
                    name.setText(clean);
                }
            }
        });

        Label help = new Label("?");
        help.setStyle("-fx-text-fill: #707890; -fx-font-size: 10px; -fx-cursor: hand; "
                + "-fx-border-color: #707890; -fx-border-radius: 8; -fx-min-width: 14px; "
                + "-fx-alignment: center; -fx-padding: 0 2 0 2;");
        javafx.scene.control.Tooltip.install(help, new javafx.scene.control.Tooltip("The name of the template"));

        HBox nameRow = new HBox(8, name, help);
        HBox.setHgrow(name, Priority.ALWAYS);
        nameRow.setAlignment(Pos.CENTER_LEFT);

        GridPane fields = new GridPane();
        fields.setHgap(12);
        fields.setVgap(12);
        fields.setPadding(new Insets(18));
        fields.getColumnConstraints().addAll(new ColumnConstraints(120), new ColumnConstraints(480));
        fields.add(formLabel("Template URL:"), 0, 0);
        fields.add(url, 1, 0);
        fields.add(note, 1, 1);
        fields.add(formLabel("Name:"), 0, 2);
        fields.add(nameRow, 1, 2);

        Button add = new Button("Add");
        add.getStyleClass().add("dialog-primary");
        Runnable doAdd = () -> {
            String templateUrl = url.getText().trim();
            String templateName = name.getText().trim();
            if (!templateUrl.isBlank() && !templateName.isBlank()) {
                RustTemplate template = new RustTemplate(templateName, templateUrl, "Custom:" + templateUrl, false);
                rustTemplates.add(template);
                rustTemplateList.getSelectionModel().select(template);
                dialog.close();
            }
        };
        add.setOnAction(e -> doAdd.run());
        url.setOnAction(e -> doAdd.run());
        name.setOnAction(e -> doAdd.run());

        Button cancel = new Button("Cancel");
        cancel.getStyleClass().add("dialog-secondary");
        cancel.setOnAction(e -> dialog.close());

        HBox footer = new HBox(10, add, cancel);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(12, 18, 18, 18));

        BorderPane root = new BorderPane(fields);
        root.setBottom(footer);
        root.getStyleClass().addAll("app-root", "catalog-manager");
        Scene scene = new Scene(root, 650, 230);
        scene.getStylesheets().add(getClass().getResource("/css/lumina-dark.css").toExternalForm());
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    private void addArchetype() {
        javafx.scene.control.TextInputDialog dialog = new javafx.scene.control.TextInputDialog();
        dialog.initOwner(stage);
        dialog.setTitle("Add Maven Archetype");
        dialog.setHeaderText("Add archetype coordinates");
        dialog.setContentText("Group ID:Artifact ID");
        dialog.showAndWait().map(String::trim).filter(value -> !value.isBlank()).ifPresent(value -> {
            if (!archetypeCombo.getItems().contains(value)) archetypeCombo.getItems().add(value);
            archetypeCombo.getSelectionModel().select(value);
        });
    }

    private void showCatalogManager() {
        Stage dialog = new Stage();
        dialog.initOwner(stage);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Manage Catalogs");

        TableView<CatalogEntry> catalogs = new TableView<>(FXCollections.observableArrayList(
                new CatalogEntry("Internal", "System", ""),
                new CatalogEntry("Default Local", "System", System.getProperty("user.home") + "/.m2/repository"),
                new CatalogEntry("Maven Central", "System", "https://repo.maven.apache.org/maven2")));
        catalogs.getStyleClass().add("maven-properties-table");
        TableColumn<CatalogEntry, String> name = new TableColumn<>("Name");
        name.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().name()));
        name.setPrefWidth(135);
        TableColumn<CatalogEntry, String> type = new TableColumn<>("Type");
        type.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().type()));
        type.setPrefWidth(70);
        TableColumn<CatalogEntry, String> location = new TableColumn<>("Location");
        location.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().location()));
        location.setPrefWidth(340);
        catalogs.getColumns().setAll(name, type, location);

        Button ok = new Button("OK");
        ok.getStyleClass().add("dialog-primary");
        ok.setOnAction(e -> dialog.close());
        Button cancel = new Button("Cancel");
        cancel.getStyleClass().add("dialog-secondary");
        cancel.setOnAction(e -> dialog.close());
        HBox footer = new HBox(10, ok, cancel);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(12));

        BorderPane root = new BorderPane(catalogs);
        root.setBottom(footer);
        root.getStyleClass().addAll("app-root", "catalog-manager");
        Scene scene = new Scene(root, 580, 330);
        scene.getStylesheets().add(getClass().getResource("/css/lumina-dark.css").toExternalForm());
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    // --------------------------------------------------------------- logic

    private void updateForGenerator() {
        // Switching which generator is selected always returns to page 1,
        // exactly like IntelliJ's wizard \u2014 mid-wizard state on the Spring
        // Boot dependency page only survives Previous/Next, not a new pick.
        if (formScroll != null && springDepsPage != null) {
            onSpringDepsPage = false;
            springDepsPage.setVisible(false);
            springDepsPage.setManaged(false);
            formScroll.setVisible(true);
            formScroll.setManaged(true);
            if (previousButton != null) {
                previousButton.setVisible(false);
                previousButton.setManaged(false);
            }
            if (cancelButton != null) {
                cancelButton.setVisible(true);
                cancelButton.setManaged(true);
            }
        }
        if (formScroll != null && javafxDepsPage != null) {
            onJavaFXDepsPage = false;
            javafxDepsPage.setVisible(false);
            javafxDepsPage.setManaged(false);
            formScroll.setVisible(true);
            formScroll.setManaged(true);
            if (previousButton != null) {
                previousButton.setVisible(false);
                previousButton.setManaged(false);
            }
            if (cancelButton != null) {
                cancelButton.setVisible(true);
                cancelButton.setManaged(true);
            }
        }
        if (quarkusDepsPage != null) {
            onQuarkusDepsPage = false;
            quarkusDepsPage.setVisible(false);
            quarkusDepsPage.setManaged(false);
        }
        if (jakartaDepsPage != null) {
            onJakartaDepsPage = false;
            jakartaDepsPage.setVisible(false);
            jakartaDepsPage.setManaged(false);
        }
        if (micronautFeaturesPage != null) {
            onMicronautFeaturesPage = false;
            micronautFeaturesPage.setVisible(false);
            micronautFeaturesPage.setManaged(false);
        }
        if (ktorPluginsPage != null) {
            onKtorPluginsPage = false;
            ktorPluginsPage.setVisible(false);
            ktorPluginsPage.setManaged(false);
        }
        if (sidebar != null) {
            sidebar.setVisible(true);
            sidebar.setManaged(true);
        }
        ProjectSpec.Generator generator = selected.generator();
        boolean spring = generator == ProjectSpec.Generator.SPRING_BOOT;
        boolean quarkus = generator == ProjectSpec.Generator.QUARKUS;
        boolean micronaut = generator == ProjectSpec.Generator.MICRONAUT;
        boolean jakarta = generator == ProjectSpec.Generator.JAKARTA_EE;
        boolean ktor = generator == ProjectSpec.Generator.KTOR;
        boolean mavenArchetype = generator == ProjectSpec.Generator.MAVEN_ARCHETYPE;
        boolean rust = generator == ProjectSpec.Generator.RUST;
        boolean kotlin = generator == ProjectSpec.Generator.KOTLIN;
        boolean groovy = generator == ProjectSpec.Generator.GROOVY;
        boolean scala = generator == ProjectSpec.Generator.SCALA;
        boolean empty = generator == ProjectSpec.Generator.EMPTY_PROJECT;
        boolean angular = generator == ProjectSpec.Generator.ANGULAR_CLI;
        boolean vite = generator == ProjectSpec.Generator.VITE;
        boolean javafx = generator == ProjectSpec.Generator.JAVAFX;
        boolean java = generator == ProjectSpec.Generator.JAVA;
        boolean web = angular || vite;
        boolean specific = switch (generator) {
            case HTML, REACT, EXPRESS, VUE, NUXT -> true;
            default -> false;
        };
        if (dependenciesRow != null) {
            // Spring Boot picks dependencies on its own wizard page now;
            // this legacy text field stays hidden and just backs it.
            dependenciesRow.setVisible(false);
            dependenciesRow.setManaged(false);
        }
        if (micronaut) {
            serverUrlLabel.setText(micronautServerUrl.replaceFirst("^https?://", ""));
        } else if (quarkus) {
            serverUrlLabel.setText(quarkusServerUrl.replaceFirst("^https?://", ""));
        } else if (spring) {
            serverUrlLabel.setText("start.spring.io");
        }
        langGroovy.setVisible(!quarkus);
        langGroovy.setManaged(!quarkus);
        if (quarkus && langGroovy.isSelected()) {
            langJava.setSelected(true);
        }
        if (typeLabel != null) {
            typeLabel.setText(quarkus || micronaut ? "Build system:" : "Type:");
        }
        if (ktor) {
            if (nameField.getText().trim().isEmpty() || "demo".equals(nameField.getText().trim())) {
                nameField.setText("ktor-sample");
            }
            if (groupField.getText().trim().isEmpty() || "org.example".equals(groupField.getText().trim())) {
                groupField.setText("com.example");
            }
            if (artifactField.getText().trim().isEmpty() || "demo".equals(artifactField.getText().trim())) {
                artifactField.setText("com.example.ktor-sample");
            }
        }
        boolean html = generator == ProjectSpec.Generator.HTML;
        boolean react = generator == ProjectSpec.Generator.REACT;
        gitCheck.setVisible(!web && !html && !react);
        gitCheck.setManaged(!web && !html && !react);
        generatorSpecificBox.setVisible(specific);
        generatorSpecificBox.setManaged(specific);
        if (specific) buildSpecificForm(generator);

        configureBuildOptions();

        if (kotlin) {
            for (Toggle t : gradleDslGroup.getToggles()) {
                if ("Kotlin".equalsIgnoreCase(((ToggleButton) t).getText())) {
                    t.setSelected(true);
                    break;
                }
            }
        } else if (groovy) {
            for (Toggle t : gradleDslGroup.getToggles()) {
                if ("Groovy".equalsIgnoreCase(((ToggleButton) t).getText())) {
                    t.setSelected(true);
                    break;
                }
            }
        } else if (scala) {
            sampleCodeCheck.setSelected(false);
            scalaModuleNameField.setText(nameField.getText());
        }

        if (createButton != null) {
            if (spring) {
                createButton.setText("Next");
                createButton.setOnAction(e -> goToSpringDepsPage());
            } else if (javafx) {
                createButton.setText("Next");
                createButton.setOnAction(e -> goToJavaFXDepsPage());
            } else if (quarkus) {
                createButton.setText("Next");
                createButton.setOnAction(e -> goToQuarkusDepsPage());
            } else if (micronaut) {
                createButton.setText("Next");
                createButton.setOnAction(e -> goToMicronautFeaturesPage());
            } else if (ktor) {
                createButton.setText("Next");
                createButton.setOnAction(e -> goToKtorPluginsPage());
            } else if (jakarta) {
                createButton.setText("Next");
                createButton.setOnAction(e -> goToJakartaDepsPage());
            } else {
                createButton.setText(specific && generator != ProjectSpec.Generator.HTML
                        && generator != ProjectSpec.Generator.REACT && generator != ProjectSpec.Generator.EXPRESS
                        && generator != ProjectSpec.Generator.VUE && generator != ProjectSpec.Generator.NUXT
                        ? "Next" : "Create");
                createButton.setOnAction(e -> tryCreate());
            }
        }
        mavenArchetypeBox.setVisible(mavenArchetype);
        mavenArchetypeBox.setManaged(mavenArchetype);
        rustBox.setVisible(false);
        rustBox.setManaged(false);
        emptyDescription.setVisible(empty);
        emptyDescription.setManaged(empty);

        if (!mavenArchetype && !rust) javaVersionBox.getSelectionModel().select((spring || quarkus || jakarta) ? "21" : "25");
        errorLabel.setText(selected.enabled() ? ""
                : selected.label() + " support arrives in a later phase.");

        rebuildFormGrid();
        updateHints();
        updateJavaAdvancedGrid();
    }

    private void configureBuildOptions() {
        ToggleButton selectedBuild = (ToggleButton) buildGroup.getSelectedToggle();
        String wasSelected = selectedBuild == null ? "" : selectedBuild.getText();
        buildGroup.getToggles().clear();
        buildSystemRow.getChildren().setAll(segmented(buildGroup, false, "Maven", "Gradle"));
        boolean matched = false;
        for (javafx.scene.control.Toggle toggle : buildGroup.getToggles()) {
            if (((ToggleButton) toggle).getText().equals(wasSelected)) {
                toggle.setSelected(true);
                matched = true;
                break;
            }
        }
        if (!matched) {
            String defaultChoice = (selected != null && (selected.generator() == ProjectSpec.Generator.KOTLIN || selected.generator() == ProjectSpec.Generator.GROOVY)) ? "Gradle" : "Maven";
            for (javafx.scene.control.Toggle toggle : buildGroup.getToggles()) {
                if (defaultChoice.equals(((ToggleButton) toggle).getText())) {
                    toggle.setSelected(true);
                    matched = true;
                    break;
                }
            }
            if (!matched && !buildGroup.getToggles().isEmpty()) {
                buildGroup.getToggles().get(0).setSelected(true);
            }
        }
    }

    /** Builds the compact page for generators that have their own external wizard. */
    private void buildSpecificForm(ProjectSpec.Generator generator) {
        GridPane form = new GridPane();
        form.setHgap(12);
        form.setVgap(14);
        ColumnConstraints labels = new ColumnConstraints(112);
        ColumnConstraints values = new ColumnConstraints();
        values.setHgrow(Priority.ALWAYS);
        form.getColumnConstraints().addAll(labels, values);
        int row = 0;

        if (generator == ProjectSpec.Generator.MICRONAUT) {
            String url = "launch.micronaut.io";
            HBox server = new HBox(14, blueText(url), compactButton("\u2699"));
            form.add(formLabel("Server URL:"), 0, row); form.add(server, 1, row++);
        }

        switch (generator) {
            case MICRONAUT -> {
                add(form, row++, "Language:", segments("Java", "Kotlin", "Groovy"));
                add(form, row++, "Build system:", segments("Gradle - Groovy", "Gradle - Kotlin", "Maven"));
                add(form, row++, "Test framework:", segments("JUnit", "Kotest", "Spock"));
                add(form, row++, "Group:  \u24D8", text("org.example"));
                add(form, row++, "Artifact:  \u24D8", text("demo"));
                add(form, row++, "Application type:", choice("Application", "CLI Application", "Function"));
                add(form, row++, "JDK:", jdkChoice());
                add(form, row++, "Java:", choice("21", "17"));
            }
            case KTOR -> {
                add(form, row++, "Server:", text(ktorServerUrl));
                add(form, row++, "Artifact:", text("com.example.ktor-sample"));
                add(form, row++, "Engine:", choice("Netty  Default", "CIO"));
                form.add(selectedCheck("Add sample code"), 1, row++);
                Label tutorials = new Label("Start with Ktor Server and Client tutorials \u2197");
                tutorials.getStyleClass().add("form-hint"); form.add(tutorials, 1, row++);
                Label advanced = new Label("\u2304  Advanced Settings"); advanced.getStyleClass().add("maven-section-title");
                form.add(advanced, 0, row++, 2, 1);
                add(form, row++, "Build system:", segments("Gradle", "Kotlin", "Maven"));
                add(form, row++, "Ktor version:", choice("3.5.1  Default", "3.4.0"));
                add(form, row++, "Configuration in:", choice("YAML File  Default", "HOCON File"));
            }
            case HTML -> {
                initHtmlControls();
                Toggle previous = htmlProjectTypeGroup.getSelectedToggle();
                String selectedName = previous instanceof ToggleButton tb ? tb.getText() : dev.lumina.project.HtmlMetadata.TYPE_H5BP;
                htmlProjectTypeGroup.getToggles().clear();
                HBox typeSegments = segmented(htmlProjectTypeGroup, false, dev.lumina.project.HtmlMetadata.TYPE_H5BP, dev.lumina.project.HtmlMetadata.TYPE_BOOTSTRAP);
                for (Toggle t : htmlProjectTypeGroup.getToggles()) {
                    if (t instanceof ToggleButton tb && tb.getText().equalsIgnoreCase(selectedName)) {
                        tb.setSelected(true);
                        break;
                    }
                }
                if (htmlProjectTypeGroup.getSelectedToggle() == null && !htmlProjectTypeGroup.getToggles().isEmpty()) {
                    ((ToggleButton) htmlProjectTypeGroup.getToggles().getFirst()).setSelected(true);
                }
                form.add(formLabel("Project type:"), 0, row);
                form.add(typeSegments, 1, row++);

                htmlVersionBox.setPrefWidth(240);
                HBox versionRow = new HBox(8, htmlVersionBox, htmlRefreshButton);
                versionRow.setAlignment(Pos.CENTER_LEFT);
                form.add(formLabel("Version:"), 0, row);
                form.add(versionRow, 1, row++);
            }
            case REACT -> {
                initReactControls();
                Toggle previous = reactProjectTypeGroup.getSelectedToggle();
                String selectedName = previous instanceof ToggleButton tb ? tb.getText() : ReactMetadata.TYPE_REACT;
                reactProjectTypeGroup.getToggles().clear();
                HBox typeSegments = segmented(reactProjectTypeGroup, false, ReactMetadata.TYPE_REACT, ReactMetadata.TYPE_REACT_NATIVE, ReactMetadata.TYPE_NEXT_JS);
                for (Toggle t : reactProjectTypeGroup.getToggles()) {
                    if (t instanceof ToggleButton tb && tb.getText().equalsIgnoreCase(selectedName)) {
                        tb.setSelected(true);
                        break;
                    }
                }
                if (reactProjectTypeGroup.getSelectedToggle() == null && !reactProjectTypeGroup.getToggles().isEmpty()) {
                    ((ToggleButton) reactProjectTypeGroup.getToggles().getFirst()).setSelected(true);
                }
                form.add(formLabel("Project type:"), 0, row);
                form.add(typeSegments, 1, row++);

                HBox nodeRow = new HBox(8, reactNodeInterpreterBox, reactNodeBrowseBtn);
                nodeRow.setAlignment(Pos.CENTER_LEFT);
                form.add(formLabel("Node interpreter:"), 0, row);
                form.add(nodeRow, 1, row++);

                HBox cliRow = new HBox(8, reactCliBox, reactCliBrowseBtn);
                cliRow.setAlignment(Pos.CENTER_LEFT);
                form.add(reactCliLabel, 0, row);
                form.add(cliRow, 1, row++);

                boolean isReactNative = ReactMetadata.TYPE_REACT_NATIVE.equalsIgnoreCase(getSelectedReactProjectType());
                reactTsCheck.setVisible(!isReactNative);
                reactTsCheck.setManaged(!isReactNative);
                form.add(reactTsCheck, 1, row++);

                boolean isReact = ReactMetadata.TYPE_REACT.equalsIgnoreCase(getSelectedReactProjectType());
                reactAdvisoryBox.setVisible(isReact);
                reactAdvisoryBox.setManaged(isReact);
                form.add(reactAdvisoryBox, 1, row++);
            }
            case EXPRESS -> {
                initExpressControls();
                refreshExpressNodeInterpreters();

                HBox nodeRow = new HBox(8, expressNodeInterpreterBox, expressNodeBrowseBtn);
                nodeRow.setAlignment(Pos.CENTER_LEFT);
                form.add(formLabel("Node interpreter:"), 0, row);
                form.add(nodeRow, 1, row++);

                HBox cliRow = new HBox(8, expressCliBox, expressCliBrowseBtn);
                cliRow.setAlignment(Pos.CENTER_LEFT);
                form.add(formLabel("express-generator:"), 0, row);
                form.add(cliRow, 1, row++);

                Label optionsLabel = new Label("Options");
                optionsLabel.setStyle("-fx-text-fill: #8C92A4; -fx-font-size: 11px; -fx-font-weight: bold;");
                Separator sep = new Separator(Orientation.HORIZONTAL);
                HBox.setHgrow(sep, Priority.ALWAYS);
                HBox optionsRow = new HBox(8, optionsLabel, sep);
                optionsRow.setAlignment(Pos.CENTER_LEFT);
                optionsRow.setPadding(new Insets(8, 0, 4, 0));
                form.add(optionsRow, 0, row++, 2, 1);

                form.add(formLabel("View engine:"), 0, row);
                form.add(expressViewEngineBox, 1, row++);

                form.add(formLabel("Stylesheet engine:"), 0, row);
                form.add(expressStylesheetEngineBox, 1, row++);
            }
            case VUE -> {
                add(form, row++, "Node runtime:", runtime("node  /usr/bin/node                         22.23.1"));
                add(form, row++, "Vue CLI:", runtime("npx create-vue                                                3.23.0"));
                form.add(selectedCheck("Use the default project setup"), 1, row++);
            }
            case NUXT -> {
                add(form, row++, "Node runtime:", runtime("node  /usr/bin/node                         22.23.1"));
                add(form, row++, "Nuxt CLI:", runtime("npx nuxi@latest                                               3.37.0"));
            }
            default -> { }
        }
        generatorSpecificBox.getChildren().setAll(form);
    }

    private void add(GridPane grid, int row, String label, Node value) {
        grid.add(formLabel(label), 0, row);
        grid.add(value, 1, row);
    }

    private TextField text(String value) {
        TextField field = new TextField(value);
        field.setPrefWidth(275);
        return field;
    }

    private ComboBox<String> choice(String... values) {
        ComboBox<String> box = new ComboBox<>(FXCollections.observableArrayList(values));
        box.getSelectionModel().selectFirst();
        box.setPrefWidth(398);
        return box;
    }

    private HBox runtime(String value) {
        ComboBox<String> box = choice(value);
        box.setMaxWidth(Double.MAX_VALUE);
        HBox row = new HBox(6, box, compactButton("\u2026"));
        HBox.setHgrow(box, Priority.ALWAYS);
        return row;
    }

    private HBox segments(String... labels) {
        return segmented(new ToggleGroup(), true, labels);
    }

    private CheckBox selectedCheck(String label) {
        CheckBox box = new CheckBox(label);
        box.setSelected(true);
        return box;
    }

    private ComboBox<JdkEntry> jdkChoice() {
        ComboBox<JdkEntry> box = new ComboBox<>(jdkCombo.getItems());
        box.setCellFactory(list -> createJdkCell());
        box.setButtonCell(createJdkButtonCell());
        if (selectedJdk != null) {
            box.getSelectionModel().select(selectedJdk);
        }
        box.setPrefWidth(398);
        return box;
    }

    private Label blueText(String value) {
        Label label = new Label(value);
        label.getStyleClass().add("maven-link");
        return label;
    }

    private void updateJavaAdvancedGrid() {
        javaAdvGrid.getChildren().clear();
        javaAdvGrid.getRowConstraints().clear();

        if (selected != null && selected.generator() == ProjectSpec.Generator.SCALA) {
            javaAdvGrid.add(formLabel("Module name:"), 0, 0);
            javaAdvGrid.add(scalaModuleNameField, 1, 0);
            return;
        }

        boolean isGradle = isGradleSelected();
        int row = 0;

        if (isGradle) {
            // 1. Gradle distribution: [ Wrapper / Local installation ]
            javaAdvGrid.add(gradleDistributionLabel, 0, row);
            javaAdvGrid.add(gradleDistributionBox, 1, row++);

            // 2. Gradle version (with Auto-select) OR Gradle location
            if ("Local installation".equals(gradleDistributionBox.getValue())) {
                if (gradleLocationField.getText().isBlank()) {
                    List<String> localInstalls = GradleMetadata.detectLocalInstallations();
                    if (!localInstalls.isEmpty()) {
                        gradleLocationField.setText(localInstalls.get(0));
                    }
                }
                javaAdvGrid.add(gradleLocationLabel, 0, row);
                javaAdvGrid.add(gradleLocationRow, 1, row++);
            } else {
                gradleVersionRow.getChildren().setAll(gradleVersionBox, gradleAutoSelectCheck);
                javaAdvGrid.add(gradleVersionLabel, 0, row);
                javaAdvGrid.add(gradleVersionRow, 1, row++);
            }

            // 3. [ ] Use these settings for future projects
            javaAdvGrid.add(saveSettingsCheck, 1, row++);
        }

        // GroupId & ArtifactId
        Node groupLabelWithHelp = formLabelWithHelp("GroupId:", "The group ID uniquely identifies your project across all projects (e.g., com.example).");
        Node artifactLabelWithHelp = formLabelWithHelp("ArtifactId:", "The artifact ID is the name of the jar or build artifact (e.g., demo).");
        javaAdvGrid.add(groupLabelWithHelp, 0, row);
        javaAdvGrid.add(groupField, 1, row++);
        javaAdvGrid.add(artifactLabelWithHelp, 0, row);
        javaAdvGrid.add(artifactField, 1, row++);
    }

    private void syncGradleVersionWithJdk() {
        int jdkMajor = getSelectedJdkMajorVersion();
        updatingGradleVersion = true;
        try {
            List<GradleMetadata.GradleRelease> releases = GradleMetadata.fetchReleases(false);
            List<GradleMetadata.GradleRelease> compatible = GradleMetadata.filterCompatibleVersions(releases, jdkMajor);
            List<String> versionStrings = compatible.stream().map(GradleMetadata.GradleRelease::version).toList();
            String currentSelection = gradleVersionBox.getValue();
            gradleVersionBox.getItems().setAll(versionStrings);

            String best = GradleMetadata.getLatestCompatibleVersion(jdkMajor);
            if (gradleAutoSelectCheck.isSelected() || currentSelection == null || !versionStrings.contains(currentSelection)) {
                if (versionStrings.contains(best)) {
                    gradleVersionBox.getSelectionModel().select(best);
                } else if (!versionStrings.isEmpty()) {
                    gradleVersionBox.getSelectionModel().select(versionStrings.get(versionStrings.size() - 1));
                }
            } else {
                gradleVersionBox.getSelectionModel().select(currentSelection);
            }
        } finally {
            updatingGradleVersion = false;
        }
    }

    private int getSelectedJdkMajorVersion() {
        if (selectedJdk != null) {
            if (selectedJdk.installation() != null) {
                return selectedJdk.installation().majorVersion();
            }
            if (selectedJdk.label() != null && !selectedJdk.label().isBlank()) {
                int parsed = JdkMetadata.parseMajorVersion(selectedJdk.label());
                if (parsed > 0) return parsed;
            }
        }
        JdkEntry val = jdkCombo.getValue();
        if (val != null) {
            if (val.installation() != null) {
                return val.installation().majorVersion();
            }
            if (val.label() != null && !val.label().isBlank()) {
                int parsed = JdkMetadata.parseMajorVersion(val.label());
                if (parsed > 0) return parsed;
            }
        }
        return 21;
    }

    private boolean isGradleSelected() {
        Toggle t = buildGroup.getSelectedToggle();
        return t instanceof ToggleButton tb && "Gradle".equalsIgnoreCase(tb.getText());
    }

    private boolean isKotlinDslSelected() {
        Toggle t = gradleDslGroup.getSelectedToggle();
        if (t instanceof ToggleButton tb) {
            return "Kotlin".equalsIgnoreCase(tb.getText());
        }
        return true;
    }

    private boolean isSbtSelected() {
        Toggle t = scalaBuildGroup.getSelectedToggle();
        return t == null || !(t instanceof ToggleButton tb) || "sbt".equalsIgnoreCase(tb.getText());
    }

    private boolean isUvSelected() {
        Toggle t = pythonInterpreterGroup.getSelectedToggle();
        return t instanceof ToggleButton tb && "uv".equalsIgnoreCase(tb.getText());
    }

    private ProjectSpec.PythonInterpreterType getSelectedPythonInterpreterType() {
        Toggle t = pythonInterpreterGroup.getSelectedToggle();
        if (t instanceof ToggleButton tb) {
            String txt = tb.getText().trim();
            if ("uv".equalsIgnoreCase(txt)) return ProjectSpec.PythonInterpreterType.UV;
            if ("Base conda".equalsIgnoreCase(txt)) return ProjectSpec.PythonInterpreterType.BASE_CONDA;
            if ("Custom environment".equalsIgnoreCase(txt)) return ProjectSpec.PythonInterpreterType.CUSTOM_ENVIRONMENT;
        }
        return ProjectSpec.PythonInterpreterType.PROJECT_VENV;
    }

    private void setupPythonComboCellFactory(ComboBox<dev.lumina.project.PythonMetadata.PythonInstallation> combo) {
        combo.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(dev.lumina.project.PythonMetadata.PythonInstallation item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    HBox cellBox = new HBox(8);
                    cellBox.setAlignment(Pos.CENTER_LEFT);
                    Node icon = dev.lumina.ui.GeneratorIcons.pythonIcon();
                    Label title = new Label(item.label());
                    title.setStyle("-fx-font-weight: 500; -fx-text-fill: #DFE1E5;");
                    Label pathLabel = new Label("(" + item.executable() + ") " + item.type());
                    pathLabel.setStyle("-fx-text-fill: #8C92A4; -fx-font-size: 11px;");
                    cellBox.getChildren().addAll(icon, title, pathLabel);
                    setGraphic(cellBox);
                    setText(null);
                }
            }
        });
        combo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(dev.lumina.project.PythonMetadata.PythonInstallation item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    HBox cellBox = new HBox(8);
                    cellBox.setAlignment(Pos.CENTER_LEFT);
                    Node icon = dev.lumina.ui.GeneratorIcons.pythonIcon();
                    Label title = new Label(item.label());
                    title.setStyle("-fx-font-weight: 500; -fx-text-fill: #DFE1E5;");
                    Label pathLabel = new Label("(" + item.executable() + ") " + item.type());
                    pathLabel.setStyle("-fx-text-fill: #8C92A4; -fx-font-size: 11px;");
                    cellBox.getChildren().addAll(icon, title, pathLabel);
                    setGraphic(cellBox);
                    setText(null);
                }
            }
        });
    }

    private void updateCondaValidation(String path) {
        boolean valid = dev.lumina.project.PythonMetadata.isValidConda(path);
        boolean wasBannerVisible = condaWarningBanner.isVisible();
        boolean wasCalloutVisible = condaErrorCallout.isVisible();
        if (valid) {
            condaWarningBanner.setVisible(false);
            condaWarningBanner.setManaged(false);
            condaErrorCallout.setVisible(false);
            condaErrorCallout.setManaged(false);
            condaPathField.setStyle("");
        } else {
            condaWarningBanner.setVisible(true);
            condaWarningBanner.setManaged(true);
            condaErrorCallout.setVisible(true);
            condaErrorCallout.setManaged(true);
            condaPathField.setStyle("-fx-border-color: #DE3423; -fx-border-radius: 4px;");
        }
        if ((wasBannerVisible != condaWarningBanner.isVisible() || wasCalloutVisible != condaErrorCallout.isVisible())
                && selected != null && selected.generator() == ProjectSpec.Generator.PYTHON) {
            rebuildFormGrid();
        }
    }

    private void startMinicondaInstallation() {
        if (activeCondaInstallFuture != null && !activeCondaInstallFuture.isDone()) {
            activeCondaInstallFuture.cancel(true);
        }

        condaInstallSpinner.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
        condaInstallSpinner.setPrefSize(16, 16);
        condaInstallSpinner.setMinSize(16, 16);
        condaInstallSpinner.setMaxSize(16, 16);
        condaInstallSpinner.setStyle("-fx-progress-color: #548AF7;");

        condaInstallStatus.setText("Connecting to Miniconda repository…");
        condaInstallStatus.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");

        condaCancelInstallLink.setStyle("-fx-text-fill: #548AF7; -fx-font-size: 12px; -fx-cursor: hand; -fx-underline: false; -fx-padding: 0;");
        condaCancelInstallLink.setOnAction(e -> cancelMinicondaInstallation());

        condaOpenWebLink.setStyle("-fx-text-fill: #548AF7; -fx-font-size: 12px; -fx-cursor: hand; -fx-underline: false; -fx-padding: 0;");
        condaOpenWebLink.setOnAction(e -> dev.lumina.project.PythonMetadata.openExternalUrl("https://docs.anaconda.com/miniconda/"));

        Region bannerSpacer = new Region();
        HBox.setHgrow(bannerSpacer, Priority.ALWAYS);
        HBox progressBox = new HBox(8, condaInstallSpinner, condaInstallStatus);
        progressBox.setAlignment(Pos.CENTER_LEFT);
        HBox actionsBox = new HBox(12, condaCancelInstallLink, condaOpenWebLink);
        actionsBox.setAlignment(Pos.CENTER_RIGHT);
        condaWarningBanner.getChildren().setAll(progressBox, bannerSpacer, actionsBox);

        activeCondaInstallFuture = dev.lumina.project.PythonMetadata.installMinicondaAsync(
                status -> javafx.application.Platform.runLater(() -> condaInstallStatus.setText(status)),
                progress -> javafx.application.Platform.runLater(() -> {
                    condaInstallSpinner.setProgress(progress);
                    condaInstallStatus.setText(String.format(java.util.Locale.US, "Downloading Miniconda (%.0f%%)…", progress * 100));
                })
        );

        activeCondaInstallFuture.thenAccept(binPath -> {
            javafx.application.Platform.runLater(() -> {
                activeCondaInstallFuture = null;
                condaPathField.setText(binPath.toAbsolutePath().toString());
                updateCondaValidation(binPath.toAbsolutePath().toString());
                restoreCondaBannerLinks();
            });
        }).exceptionally(ex -> {
            javafx.application.Platform.runLater(() -> {
                activeCondaInstallFuture = null;
                restoreCondaBannerLinks();
                String msg = ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();
                condaWarningText.setText("Installation failed: " + msg);
            });
            return null;
        });
    }

    private void cancelMinicondaInstallation() {
        if (activeCondaInstallFuture != null) {
            activeCondaInstallFuture.cancel(true);
            activeCondaInstallFuture = null;
        }
        restoreCondaBannerLinks();
    }

    private void restoreCondaBannerLinks() {
        Region bannerSpacer = new Region();
        HBox.setHgrow(bannerSpacer, Priority.ALWAYS);
        HBox linksBox = new HBox(12, condaInstallLink, condaSelectPathLink);
        linksBox.setAlignment(Pos.CENTER_RIGHT);
        condaWarningText.setText("No conda executable found");
        condaWarningBanner.getChildren().setAll(condaWarningIcon, condaWarningText, bannerSpacer, linksBox);
    }

    private String getSelectedPythonPath() {
        ProjectSpec.PythonInterpreterType type = getSelectedPythonInterpreterType();
        if (type == ProjectSpec.PythonInterpreterType.UV) return "";
        if (type == ProjectSpec.PythonInterpreterType.BASE_CONDA) {
            return condaPathField.getText().trim();
        }
        if (type == ProjectSpec.PythonInterpreterType.CUSTOM_ENVIRONMENT) {
            if (customEnvGenerateNewRadio.isSelected()) {
                String ctype = customTypeCombo.getValue();
                if ("Conda".equalsIgnoreCase(ctype)) {
                    return condaPathField.getText().trim();
                }
                dev.lumina.project.PythonMetadata.PythonInstallation inst = customBasePythonCombo.getValue();
                return inst != null && inst.executable() != null ? inst.executable() : "/usr/bin/python3";
            } else {
                String ctype = customTypeCombo.getValue();
                if ("Conda".equalsIgnoreCase(ctype)) {
                    return condaPathField.getText().trim();
                }
                dev.lumina.project.PythonMetadata.PythonInstallation inst = customPythonPathCombo.getValue();
                return inst != null && inst.executable() != null ? inst.executable() : "/usr/bin/python3";
            }
        }
        dev.lumina.project.PythonMetadata.PythonInstallation inst = pythonVersionCombo.getValue();
        return inst != null && inst.executable() != null ? inst.executable() : "/usr/bin/python3";
    }

    private String getSelectedPythonVersion() {
        ProjectSpec.PythonInterpreterType type = getSelectedPythonInterpreterType();
        if (type == ProjectSpec.PythonInterpreterType.UV) {
            return uvPythonVersionCombo.getValue() != null ? uvPythonVersionCombo.getValue() : "Default";
        }
        if (type == ProjectSpec.PythonInterpreterType.BASE_CONDA) {
            return condaPythonVersionCombo.getValue() != null ? condaPythonVersionCombo.getValue() : "3.12";
        }
        if (type == ProjectSpec.PythonInterpreterType.CUSTOM_ENVIRONMENT) {
            if (customEnvGenerateNewRadio.isSelected()) {
                String ctype = customTypeCombo.getValue();
                if ("Conda".equalsIgnoreCase(ctype)) {
                    return condaPythonVersionCombo.getValue() != null ? condaPythonVersionCombo.getValue() : "3.12";
                }
                dev.lumina.project.PythonMetadata.PythonInstallation inst = customBasePythonCombo.getValue();
                if (inst != null && inst.label() != null) {
                    return inst.label().replaceFirst("^Python\\s*", "");
                }
            } else {
                dev.lumina.project.PythonMetadata.PythonInstallation inst = customPythonPathCombo.getValue();
                if (inst != null && inst.label() != null) {
                    return inst.label().replaceFirst("^Python\\s*", "");
                }
            }
        }
        dev.lumina.project.PythonMetadata.PythonInstallation inst = pythonVersionCombo.getValue();
        if (inst != null && inst.label() != null) {
            return inst.label().replaceFirst("^Python\\s*", "");
        }
        return "3.12";
    }

    private void openMinicondaInstallPage() {
        dev.lumina.project.PythonMetadata.openExternalUrl("https://docs.anaconda.com/miniconda/");
    }

    private void pickCondaExecutable() {
        javafx.stage.FileChooser chooser = new javafx.stage.FileChooser();
        chooser.setTitle("Select Conda Executable");
        File file = chooser.showOpenDialog(stage);
        if (file != null) {
            condaPathField.setText(file.getAbsolutePath());
            updateCondaValidation(file.getAbsolutePath());
        }
    }

    private void pickCustomBasePython() {
        javafx.stage.FileChooser chooser = new javafx.stage.FileChooser();
        chooser.setTitle("Select Base Python Interpreter");
        File file = chooser.showOpenDialog(stage);
        if (file != null && file.canExecute()) {
            String path = file.getAbsolutePath();
            String label = file.getName();
            var inst = new dev.lumina.project.PythonMetadata.PythonInstallation(label, path, "custom", label + " (" + path + ") custom");
            if (!customBasePythonCombo.getItems().contains(inst)) {
                customBasePythonCombo.getItems().add(0, inst);
            }
            customBasePythonCombo.setValue(inst);
        }
    }

    private void pickCustomPythonPath() {
        javafx.stage.FileChooser chooser = new javafx.stage.FileChooser();
        chooser.setTitle("Select Python Interpreter");
        File file = chooser.showOpenDialog(stage);
        if (file != null && file.canExecute()) {
            String path = file.getAbsolutePath();
            String label = file.getName();
            var inst = new dev.lumina.project.PythonMetadata.PythonInstallation(label, path, "custom", label + " (" + path + ") custom");
            if (!customPythonPathCombo.getItems().contains(inst)) {
                customPythonPathCombo.getItems().add(0, inst);
            }
            customPythonPathCombo.setValue(inst);
        }
    }

    private void pickCustomLocation() {
        javafx.stage.DirectoryChooser chooser = new javafx.stage.DirectoryChooser();
        chooser.setTitle("Select Environment Location");
        File dir = chooser.showDialog(stage);
        if (dir != null) {
            customLocationField.setText(dir.getAbsolutePath());
            customLocationFieldEdited = true;
        }
    }

    private void pickPythonExecutable() {
        javafx.stage.FileChooser chooser = new javafx.stage.FileChooser();
        chooser.setTitle("Select Python Interpreter");
        File file = chooser.showOpenDialog(stage);
        if (file != null && file.canExecute()) {
            String path = file.getAbsolutePath();
            String label = file.getName();
            var inst = new dev.lumina.project.PythonMetadata.PythonInstallation(label, path, "custom", label + " (" + path + ") custom");
            if (!pythonVersionCombo.getItems().contains(inst)) {
                pythonVersionCombo.getItems().add(0, inst);
            }
            pythonVersionCombo.setValue(inst);
        }
    }

    private void pickUvExecutable() {
        javafx.stage.FileChooser chooser = new javafx.stage.FileChooser();
        chooser.setTitle("Select uv Executable");
        File file = chooser.showOpenDialog(stage);
        if (file != null && file.canExecute()) {
            uvPathField.setText(file.getAbsolutePath());
        }
    }

    private static Node createPlusIcon() {
        SVGPath plus = new SVGPath();
        plus.setContent("M 1 5.5 L 10 5.5 M 5.5 1 L 5.5 10");
        plus.setStroke(Color.web("#8C919D"));
        plus.setStrokeWidth(1.4);
        return plus;
    }

    private void setupRubyControls() {
        rubyInterpreterCombo.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(rubyInterpreterCombo, Priority.ALWAYS);

        rubyInterpreterCombo.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(dev.lumina.project.RubyMetadata.RubyInstallation item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    HBox box = new HBox(8);
                    box.setAlignment(Pos.CENTER_LEFT);
                    Node icon = dev.lumina.ui.GeneratorIcons.rubyIcon();
                    Label nameLabel = new Label(item.label());
                    nameLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
                    Label pathLabel = new Label("(" + item.executable() + ")");
                    pathLabel.setStyle("-fx-text-fill: #6C7387; -fx-font-size: 11px;");
                    Region sp = new Region();
                    HBox.setHgrow(sp, Priority.ALWAYS);
                    box.getChildren().addAll(icon, nameLabel, sp, pathLabel);
                    setGraphic(box);
                    setText(null);
                }
            }
        });

        rubyInterpreterCombo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(dev.lumina.project.RubyMetadata.RubyInstallation item, boolean empty) {
                super.updateItem(item, empty);
                HBox box = new HBox(8);
                box.setAlignment(Pos.CENTER_LEFT);
                box.setPadding(new Insets(0, 48, 0, 0));
                Node icon = dev.lumina.ui.GeneratorIcons.rubyIcon();
                if (item == null) {
                    Label label = new Label(dev.lumina.project.RubyMetadata.NO_INTERPRETER_LABEL);
                    label.setStyle("-fx-text-fill: #E06C75; -fx-font-size: 12px;");
                    box.getChildren().addAll(icon, label);
                } else {
                    Label label = new Label(item.label());
                    label.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
                    box.getChildren().addAll(icon, label);
                }
                setGraphic(box);
                setText(null);
            }
        });

        List<dev.lumina.project.RubyMetadata.RubyInstallation> rubies = dev.lumina.project.RubyMetadata.fetchRubyInstallations(false);
        rubyInterpreterCombo.getItems().setAll(rubies);
        if (!rubies.isEmpty()) {
            rubyInterpreterCombo.getSelectionModel().selectFirst();
        }

        rubyAddInterpreterBtn.setGraphic(createPlusIcon());
        rubyAddInterpreterBtn.setText(null);
        rubyAddInterpreterBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 3 6 3 6; -fx-min-width: 22px; -fx-min-height: 22px;");
        rubyAddInterpreterBtn.setTooltip(new Tooltip("Add Ruby Interpreter"));
        rubyAddInterpreterBtn.setOnAction(e -> pickRubyInterpreter());
        rubyAddInterpreterBtn.setOnMouseEntered(e -> {
            Node g = rubyAddInterpreterBtn.getGraphic();
            if (g instanceof SVGPath p) p.setStroke(Color.web("#DFE1E5"));
        });
        rubyAddInterpreterBtn.setOnMouseExited(e -> {
            Node g = rubyAddInterpreterBtn.getGraphic();
            if (g instanceof SVGPath p) p.setStroke(Color.web("#8C919D"));
        });

        Region divider = new Region();
        divider.setPrefWidth(1);
        divider.setMinWidth(1);
        divider.setMaxWidth(1);
        divider.setPrefHeight(14);
        divider.setMaxHeight(14);
        divider.setStyle("-fx-background-color: #393B40;");

        Region arrowSpacer = new Region();
        arrowSpacer.setPrefWidth(22);
        arrowSpacer.setMinWidth(22);
        arrowSpacer.setMaxWidth(22);
        arrowSpacer.setMouseTransparent(true);

        HBox rightControls = new HBox(4);
        rightControls.setAlignment(Pos.CENTER_RIGHT);
        rightControls.setPickOnBounds(false);
        rightControls.getChildren().addAll(rubyAddInterpreterBtn, divider, arrowSpacer);

        rubyInterpreterPane.getChildren().setAll(rubyInterpreterCombo, rightControls);
        HBox.setHgrow(rubyInterpreterPane, Priority.ALWAYS);
        rubyInterpreterPane.setMaxWidth(Double.MAX_VALUE);
    }

    private void pickRubyInterpreter() {
        javafx.stage.FileChooser chooser = new javafx.stage.FileChooser();
        chooser.setTitle("Select Ruby Interpreter Path");
        String initialPath = "/usr/bin";
        File initDir = new File(initialPath);
        if (!initDir.isDirectory()) {
            initDir = new File(System.getProperty("user.home", "."));
        }
        if (initDir.isDirectory()) {
            chooser.setInitialDirectory(initDir);
        }
        File file = chooser.showOpenDialog(stage);
        if (file != null && file.exists()) {
            var inst = dev.lumina.project.RubyMetadata.createInstallationFromPath(file.getAbsolutePath());
            if (!rubyInterpreterCombo.getItems().contains(inst)) {
                rubyInterpreterCombo.getItems().add(0, inst);
            }
            rubyInterpreterCombo.setValue(inst);
        }
    }

    private void showPluginManager() {
        try {
            PluginManagerDialog pm = new PluginManagerDialog(
                    stage,
                    () -> Platform.runLater(this::updateForGenerator)
            );
            pm.show();
        } catch (Exception e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.initOwner(stage);
            alert.setTitle("Plugin Manager Error");
            alert.setHeaderText("Could not open Plugin Manager");
            alert.setContentText("Error: " + e.getMessage());
            alert.getDialogPane().getStylesheets().add(
                    getClass().getResource("/css/lumina-dark.css").toExternalForm());
            alert.showAndWait();
        }
    }

    private void updateHints() {
        try {
            String fullPath = Path.of(locationField.getText().isBlank() ? "." : locationField.getText())
                    .resolve(selected.generator() == ProjectSpec.Generator.MAVEN_ARCHETYPE
                            ? mavenArtifactField.getText() : nameField.getText()).toString();
            String userHome = System.getProperty("user.home");
            if (userHome != null && fullPath.startsWith(userHome)) {
                fullPath = "~" + fullPath.substring(userHome.length());
            }
            locationHint.setText("Project will be created in: " + fullPath);
        } catch (java.nio.file.InvalidPathException ex) {
            locationHint.setText("Invalid location path");
        }
        try {
            String loc = locationField.getText().trim();
            if (loc.startsWith("~")) {
                loc = System.getProperty("user.home") + loc.substring(1);
            }
            String nm = nameField.getText().trim();
            if (nm.isBlank()) nm = "untitled1";
            Path projDir = Path.of(loc.isBlank() ? "." : loc).resolve(nm);
            pythonVenvHintLabel.setText("Python virtual environment will be created in the project root:\n" + projDir.resolve(".venv"));
            uvHintLabel.setText("uv environment will be created in the project root: " + projDir.resolve(".venv"));
            if (!customLocationFieldEdited) {
                customLocationField.setText(projDir.resolve(".venv").toString());
            }
        } catch (Exception ignored) {}
        if (!packageEdited) {
            String pkg = (sanitize(groupField.getText()) + "." + sanitize(artifactField.getText()))
                    .replaceAll("^\\.|\\.$", "");
            packageField.setText(pkg);
        }
    }

    private String selectedArchetype() {
        String typed = archetypeCombo.getEditor().getText().trim();
        return typed.isEmpty() ? archetypeCombo.getValue() : typed;
    }

    private RustTemplate selectedRustTemplate() {
        RustTemplate template = rustTemplateList.getSelectionModel().getSelectedItem();
        return template == null ? rustTemplates.getFirst() : template;
    }

    private static void setNodesVisible(List<Node> nodes, boolean visible) {
        for (Node node : nodes) {
            node.setVisible(visible);
            node.setManaged(visible);
        }
    }

    private void populateJdkList(JdkInstallation toSelect) {
        List<JdkInstallation> detected = JdkMetadata.detectInstallations(false);
        List<JdkEntry> entries = new ArrayList<>();
        if (!detected.isEmpty()) {
            entries.add(new JdkEntry("Detected JDKs", "", false, false));
            for (JdkInstallation inst : detected) {
                entries.add(new JdkEntry(inst.formatDisplay(), inst.homePath(), true, false, inst));
            }
        } else {
            entries.add(new JdkEntry("No JDK detected", "", false, false));
        }
        entries.add(new JdkEntry(JdkMetadata.ACTION_DOWNLOAD, "", true, true));
        entries.add(new JdkEntry(JdkMetadata.ACTION_ADD, "", true, true));

        jdkCombo.setItems(FXCollections.observableArrayList(entries));

        JdkEntry target = null;
        if (toSelect != null) {
            for (JdkEntry e : entries) {
                if (e.installation() != null && e.installation().homePath().equals(toSelect.homePath())) {
                    target = e;
                    break;
                }
            }
        }
        if (target == null && selectedJdk != null && selectedJdk.installation() != null) {
            for (JdkEntry e : entries) {
                if (e.installation() != null && e.installation().homePath().equals(selectedJdk.installation().homePath())) {
                    target = e;
                    break;
                }
            }
        }
        if (target == null) {
            for (JdkEntry e : entries) {
                if (e.installation() != null && e.enabled() && !e.action()) {
                    target = e;
                    break;
                }
            }
        }
        if (target != null) {
            selectedJdk = target;
            jdkCombo.getSelectionModel().select(target);
        }
    }

    private static Node createJdkFolderIcon() {
        SVGPath folder = new SVGPath();
        folder.setContent("M 2,3 L 6,3 L 7.5,4.8 L 14,4.8 C 14.5,4.8 15,5.3 15,5.8 L 15,12 C 15,12.5 14.5,13 14,13 L 2,13 C 1.5,13 1,12.5 1,12 L 1,4 C 1,3.5 1.5,3 2,3 Z");
        folder.setFill(Color.web("#E5C07B"));
        return folder;
    }

    private ListCell<JdkEntry> createJdkCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(JdkEntry item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("");
                    return;
                }
                if (!item.enabled()) {
                    setText(item.label());
                    setGraphic(null);
                    setStyle("-fx-text-fill: #6C7387; -fx-font-weight: bold; -fx-font-size: 11px; -fx-padding: 6 8 2 8;");
                    return;
                }
                if (item.action()) {
                    setText(item.label());
                    setGraphic(null);
                    setStyle("-fx-text-fill: #589DF6; -fx-cursor: hand; -fx-padding: 4 8 4 8;");
                    return;
                }
                HBox cellBox = new HBox(8);
                cellBox.setAlignment(Pos.CENTER_LEFT);
                Node icon = createJdkFolderIcon();
                Label label = new Label(item.label());
                label.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
                Label detail = new Label(item.detail());
                detail.setStyle("-fx-text-fill: #6C7387; -fx-font-size: 11px;");
                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);
                cellBox.getChildren().addAll(icon, label, spacer, detail);
                setText(null);
                setGraphic(cellBox);
                setStyle("-fx-padding: 3 8 3 8;");
            }
        };
    }

    private ListCell<JdkEntry> createJdkButtonCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(JdkEntry item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                if (item.action() || !item.enabled()) {
                    setText(item.label());
                    setGraphic(null);
                    return;
                }
                HBox box = new HBox(8);
                box.setAlignment(Pos.CENTER_LEFT);
                Label label = new Label(item.label());
                label.setStyle("-fx-text-fill: #DFE1E5;");
                box.getChildren().addAll(createJdkFolderIcon(), label);
                setText(null);
                setGraphic(box);
            }
        };
    }

    private void handleJdkAction(JdkEntry entry) {
        if (entry.label().startsWith("Download JDK")) {
            int initialVersion = 25;
            if (selectedJdk != null && selectedJdk.installation() != null) {
                initialVersion = selectedJdk.installation().majorVersion();
            }
            DownloadJdkDialog dialog = new DownloadJdkDialog(stage, initialVersion, installed -> {
                populateJdkList(installed);
            });
            dialog.show();
        } else if (entry.label().startsWith("Add JDK")) {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("Select JDK Home Directory");
            File initialDir = new File("/Library/Java/JavaVirtualMachines");
            if (!initialDir.isDirectory()) {
                initialDir = new File(System.getProperty("user.home"));
            }
            chooser.setInitialDirectory(initialDir);
            File dir = chooser.showDialog(stage);
            if (dir != null) {
                JdkInstallation inspected = JdkMetadata.inspectDirectory(dir.toPath(), false);
                if (inspected != null) {
                    populateJdkList(inspected);
                } else {
                    Alert alert = new Alert(Alert.AlertType.WARNING);
                    alert.initOwner(stage);
                    alert.setTitle("Invalid JDK Home");
                    alert.setHeaderText("The selected directory does not appear to be a valid JDK home.");
                    alert.setContentText("Directory: " + dir.getAbsolutePath() + "\n\nMake sure it contains 'bin/java' or a 'release' file.");
                    alert.getDialogPane().getStylesheets().add(
                            getClass().getResource("/css/lumina-dark.css").toExternalForm());
                    alert.showAndWait();
                }
            }
        }
    }

    private void showServerSettings() {
        if (selected.generator() == ProjectSpec.Generator.QUARKUS) {
            TextInputDialog dialog = new TextInputDialog(quarkusServerUrl);
            dialog.initOwner(stage);
            dialog.setTitle("Quarkus Server URL");
            dialog.setHeaderText("Specify custom code.quarkus.io server URL");
            dialog.setContentText("Server URL:");
            dialog.getDialogPane().getStylesheets().add(
                    getClass().getResource("/css/lumina-dark.css").toExternalForm());
            dialog.showAndWait().ifPresent(url -> {
                if (!url.isBlank()) {
                    quarkusServerUrl = dev.lumina.project.QuarkusMetadata.normalizeServerUrl(url);
                    String display = quarkusServerUrl.replaceFirst("^https?://", "");
                    serverUrlLabel.setText(display);
                    cachedQuarkusCatalog = null;
                    quarkusFetchFailed = false;
                    quarkusFetchStarted = false;
                    kickOffQuarkusMetadataFetch();
                }
            });
            return;
        }
        if (selected.generator() == ProjectSpec.Generator.MICRONAUT) {
            TextInputDialog dialog = new TextInputDialog(micronautServerUrl);
            dialog.initOwner(stage);
            dialog.setTitle("Micronaut Server URL");
            dialog.setHeaderText("Specify custom launch.micronaut.io server URL");
            dialog.setContentText("Server URL:");
            dialog.getDialogPane().getStylesheets().add(
                    getClass().getResource("/css/lumina-dark.css").toExternalForm());
            dialog.showAndWait().ifPresent(url -> {
                if (!url.isBlank()) {
                    micronautServerUrl = dev.lumina.project.MicronautMetadata.normalizeServerUrl(url);
                    String display = micronautServerUrl.replaceFirst("^https?://", "");
                    serverUrlLabel.setText(display);
                    cachedMicronautCatalog = null;
                    micronautFetchFailed = false;
                    micronautFetchStarted = false;
                    kickOffMicronautMetadataFetch();
                }
            });
            return;
        }
        if (selected.generator() == ProjectSpec.Generator.KTOR) {
            TextInputDialog dialog = new TextInputDialog(ktorServerUrl);
            dialog.initOwner(stage);
            dialog.setTitle("Ktor Server URL");
            dialog.setHeaderText("Specify custom start.ktor.io server URL");
            dialog.setContentText("Server URL:");
            dialog.getDialogPane().getStylesheets().add(
                    getClass().getResource("/css/lumina-dark.css").toExternalForm());
            dialog.showAndWait().ifPresent(url -> {
                if (!url.isBlank()) {
                    ktorServerUrl = dev.lumina.project.KtorMetadata.normalizeServerUrl(url);
                    ktorFetchStarted = false;
                    kickOffKtorPluginsFetch();
                }
            });
            return;
        }
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.initOwner(stage);
        alert.setTitle("Spring Initializr Server");
        alert.setHeaderText("Spring Initializr server settings");
        alert.setContentText("Server settings are not configurable yet.");
        alert.getDialogPane().getStylesheets().add(
                getClass().getResource("/css/lumina-dark.css").toExternalForm());
        alert.showAndWait();
    }

    private void tryCreate() {
        if (!selected.enabled()) {
            errorLabel.setText(selected.label() + " support arrives in a later phase.");
            return;
        }
        String name = nameField.getText().trim();
        String location = locationField.getText().trim();
        if (name.isEmpty()) {
            errorLabel.setText("Project name is required.");
            return;
        }
        if (location.isEmpty()) {
            errorLabel.setText("Location is required.");
            return;
        }
        boolean mavenArchetype = selected.generator() == ProjectSpec.Generator.MAVEN_ARCHETYPE;
        boolean rust = selected.generator() == ProjectSpec.Generator.RUST;
        boolean quarkus = selected.generator() == ProjectSpec.Generator.QUARKUS;
        boolean micronaut = selected.generator() == ProjectSpec.Generator.MICRONAUT;
        boolean jakarta = selected.generator() == ProjectSpec.Generator.JAKARTA_EE;
        boolean ktor = selected.generator() == ProjectSpec.Generator.KTOR;
        boolean html = selected.generator() == ProjectSpec.Generator.HTML;
        boolean react = selected.generator() == ProjectSpec.Generator.REACT;
        boolean isPython = selected.generator() == ProjectSpec.Generator.PYTHON;
        boolean isPhp = selected.generator() == ProjectSpec.Generator.PHP;
        boolean isRuby = selected.generator() == ProjectSpec.Generator.RUBY;
        String artifact = (mavenArchetype ? mavenArtifactField : artifactField).getText().trim();
        if (artifact.isEmpty()) {
            if (html || react || isPython || isPhp || isRuby) {
                artifact = sanitize(name);
                if (artifact.isEmpty()) artifact = "untitled1";
            } else {
                errorLabel.setText("Artifact is required.");
                return;
            }
        }

        ProjectSpec.Language language = ProjectSpec.Language.JAVA;
        if (selected.generator() == ProjectSpec.Generator.KOTLIN || ktor || langKotlin.isSelected()) language = ProjectSpec.Language.KOTLIN;
        else if (selected.generator() == ProjectSpec.Generator.GROOVY || langGroovy.isSelected()) language = ProjectSpec.Language.GROOVY;
        else if (selected.generator() == ProjectSpec.Generator.SCALA) language = ProjectSpec.Language.SCALA;
        else if (isPython) language = ProjectSpec.Language.PYTHON;
        else if (isPhp) language = ProjectSpec.Language.PHP;
        else if (isRuby) language = ProjectSpec.Language.RUBY;
        else if (rust) language = ProjectSpec.Language.RUST;

        ProjectSpec.BuildSystem build;
        if (selected.generator() == ProjectSpec.Generator.SCALA) {
            build = isSbtSelected() ? ProjectSpec.BuildSystem.SBT : ProjectSpec.BuildSystem.SCALA_CLI;
        } else if (isPython || isPhp || isRuby) {
            build = ProjectSpec.BuildSystem.MAVEN;
        } else if (mavenArchetype || rust) {
            build = ProjectSpec.BuildSystem.MAVEN;
        } else if (selected.generator() == ProjectSpec.Generator.SPRING_BOOT || quarkus || micronaut) {
            if (typeGradleGroovy.isSelected() || typeGradleKotlin.isSelected()) {
                build = ProjectSpec.BuildSystem.GRADLE;
            } else {
                build = ProjectSpec.BuildSystem.MAVEN;
            }
        } else if (ktor) {
            build = ktorBuildMaven.isSelected() ? ProjectSpec.BuildSystem.MAVEN : ProjectSpec.BuildSystem.GRADLE;
        } else {
            ToggleButton buildToggle = (ToggleButton) buildGroup.getSelectedToggle();
            String text = buildToggle != null ? buildToggle.getText() : "Maven";
            build = "Gradle".equals(text) ? ProjectSpec.BuildSystem.GRADLE : ("IntelliJ".equals(text) ? ProjectSpec.BuildSystem.INTELLIJ : ProjectSpec.BuildSystem.MAVEN);
        }

        String quarkusBuildTool = "MAVEN";
        if (typeGradleGroovy.isSelected()) quarkusBuildTool = "GRADLE";
        else if (typeGradleKotlin.isSelected()) quarkusBuildTool = "GRADLE_KOTLIN_DSL";

        String quarkusStreamKey = "";
        if (quarkusStreamBox.getValue() != null) {
            quarkusStreamKey = quarkusStreamBox.getValue().key();
        }

        String micronautBuild = "gradle";
        if (typeGradleKotlin.isSelected()) micronautBuild = "gradle_kotlin";
        else if (typeMaven.isSelected()) micronautBuild = "maven";

        String micronautTest = "JUNIT";
        if (testKotest.isSelected()) micronautTest = "KOTEST";
        else if (testSpock.isSelected()) micronautTest = "SPOCK";

        String micronautAppType = micronautAppTypeBox.getValue() != null
                ? micronautAppTypeBox.getValue().value() : "default";

        ProjectSpec.Packaging packaging = packagingJar.isSelected()
                ? ProjectSpec.Packaging.JAR : ProjectSpec.Packaging.WAR;
        ProjectSpec.ConfigFormat configFormat = configYaml.isSelected()
                ? ProjectSpec.ConfigFormat.YAML : ProjectSpec.ConfigFormat.PROPERTIES;

        boolean isJava = selected.generator() == ProjectSpec.Generator.JAVA;
        boolean isKotlin = selected.generator() == ProjectSpec.Generator.KOTLIN;
        boolean isGroovy = selected.generator() == ProjectSpec.Generator.GROOVY;
        boolean isScala = selected.generator() == ProjectSpec.Generator.SCALA;
        String javaVer;
        if (selectedJdk != null && selectedJdk.installation() != null &&
            (isJava || isKotlin || isGroovy || isScala)) {
            javaVer = String.valueOf(selectedJdk.installation().majorVersion());
        } else if (selectedJdk != null && !selectedJdk.label().isBlank() && (isJava || isKotlin || isGroovy || isScala)) {
            int parsed = JdkMetadata.parseMajorVersion(selectedJdk.label());
            javaVer = parsed > 0 ? String.valueOf(parsed) : "25";
        } else if (javaVersionBox.getValue() != null && !javaVersionBox.getValue().isBlank()) {
            javaVer = javaVersionBox.getValue();
        } else {
            javaVer = "25";
        }

        String pkg;
        if (isScala) {
            pkg = isSbtSelected() ? scalaPackagePrefixField.getText().trim() : "";
        } else if (mavenArchetype) {
            pkg = (sanitize(mavenGroupField.getText()) + "." + sanitize(artifact)).replaceAll("^\\.|\\.$", "");
        } else if (isJava || isKotlin || isGroovy) {
            String g = sanitize(groupField.getText().trim());
            pkg = g.isBlank() ? "" : g;
        } else if (selected.generator() == ProjectSpec.Generator.JAVAFX || quarkus || jakarta || micronaut || ktor || html || react || selected.generator() == ProjectSpec.Generator.EXPRESS) {
            pkg = (sanitize(groupField.getText()) + "." + sanitize(artifact)).replaceAll("^\\.|\\.$", "");
        } else {
            pkg = packageField.getText().trim();
        }

        ProjectSpec.GradleDsl gradleDsl = isKotlinDslSelected()
                ? ProjectSpec.GradleDsl.KOTLIN
                : ProjectSpec.GradleDsl.GROOVY;
        String gradleDist = gradleDistributionBox.getValue() != null
                ? gradleDistributionBox.getValue() : "Wrapper";
        String gradleVer = gradleVersionBox.getValue() != null && !gradleVersionBox.getValue().isBlank()
                ? gradleVersionBox.getValue() : "9.2.0";
        String gradleHome = gradleLocationField.getText().trim();

        if ((isJava || isKotlin || isGroovy) && build == ProjectSpec.BuildSystem.GRADLE && saveSettingsCheck.isSelected()) {
            try {
                java.util.prefs.Preferences prefs = java.util.prefs.Preferences.userNodeForPackage(NewProjectDialog.class);
                prefs.put("gradle.dsl", gradleDsl == ProjectSpec.GradleDsl.KOTLIN ? "Kotlin" : "Groovy");
                prefs.put("gradle.distribution", gradleDist);
                if (!gradleHome.isBlank()) {
                    prefs.put("gradle.location", gradleHome);
                }
                if (!groupField.getText().isBlank()) {
                    prefs.put("gradle.group", groupField.getText().trim());
                }
            } catch (Exception ignored) {}
        }

        ProjectSpec spec = new ProjectSpec(
                selected.generator(),
                name,
                Path.of(location),
                gitCheck.isSelected(),
                selected.generator() == ProjectSpec.Generator.GO ? ProjectSpec.BuildSystem.INTELLIJ : build,
                selected.generator() == ProjectSpec.Generator.GO ? ProjectSpec.Language.GO : language,
                packaging,
                configFormat,
                (mavenArchetype ? mavenGroupField : groupField).getText().trim(),
                artifact,
                pkg,
                javaVer,
                dependenciesField.getText().trim(),
                selected.generator() == ProjectSpec.Generator.SPRING_BOOT
                        ? emptyIfDefault(springBootVersionBox.getValue())
                        : "",
                catalogCombo.getValue(),
                selectedArchetype(),
                archetypeVersionBox.getValue(),
                projectVersionField.getText().trim(),
                additionalProperties.stream()
                        .filter(property -> !property.getName().isBlank())
                        .map(property -> property.getName().trim() + "=" + property.getValue().trim())
                        .collect(java.util.stream.Collectors.joining(",")),
                rustToolchainBox.getEditor().getText().trim(),
                rust ? selectedRustTemplate().value() : "",
                rustEnvironmentField.getText().trim(),
                selected.generator() == ProjectSpec.Generator.JAVAFX
                        ? String.join(",", selectedJavaFXDepIds)
                        : "",
                quarkusServerUrl,
                quarkusStreamKey,
                quarkus ? String.join(",", selectedQuarkusExtIds) : "",
                quarkusBuildTool,
                (selected.generator() == ProjectSpec.Generator.GO ? goSampleCodeCheck.isSelected() : sampleCodeCheck.isSelected()),
                jakartaVersionBox.getValue() != null ? jakartaVersionBox.getValue() : dev.lumina.project.JakartaMetadata.EE_11,
                jakartaTemplateBox.getValue() != null ? jakartaTemplateBox.getValue() : dev.lumina.project.JakartaMetadata.TEMPLATE_REST,
                jakarta ? String.join(",", selectedJakartaDepIds) : "",
                jakartaAppServerBox.getValue() != null ? jakartaAppServerBox.getValue() : "<No application server>",
                micronautServerUrl,
                micronautVersion,
                micronautTest,
                micronautAppType,
                micronaut ? String.join(",", selectedMicronautFeatureIds) : "",
                micronautBuild,
                ktorServerUrl,
                ktorEngineBox.getValue() != null ? ktorEngineBox.getValue() : "Netty",
                ktorAddSampleCodeCheck.isSelected(),
                ktorBuildKotlin.isSelected() ? "Kotlin" : (ktorBuildMaven.isSelected() ? "Maven" : "Gradle"),
                ktorVersionBox.getValue() != null ? ktorVersionBox.getValue() : "3.5.2",
                ktorConfigInBox.getValue() != null ? ktorConfigInBox.getValue() : "YAML File",
                ktor ? String.join(",", selectedKtorPluginIds) : "",
                getSelectedHtmlProjectType(),
                htmlVersionBox.getValue() != null ? htmlVersionBox.getValue() : "v9.0.1",
                getSelectedReactProjectType(),
                getSelectedReactNodeInterpreter(),
                getSelectedReactCliVersion(),
                reactTsCheck.isSelected(),
                getSelectedExpressNodeInterpreter(),
                getSelectedExpressCliVersion(),
                getSelectedExpressViewEngine(),
                getSelectedExpressStylesheetEngine(),
                gradleDsl,
                gradleDist,
                gradleVer,
                gradleHome,
                (selected != null && selected.generator() == ProjectSpec.Generator.KOTLIN && isGradleSelected() && isKotlinDslSelected()) && multiModuleCheck.isSelected(),
                groovySdkBox.getValue() != null && !groovySdkBox.getValue().isBlank() && !GroovyMetadata.SPECIFY_HOME_OPTION.equals(groovySdkBox.getValue().trim()) ? groovySdkBox.getValue().trim() : "5.1.1",
                sbtVersionBox.getValue() != null && !sbtVersionBox.getValue().isBlank() ? sbtVersionBox.getValue().trim() : "2.0.9",
                scalaVersionBox.getValue() != null && !scalaVersionBox.getValue().isBlank() ? scalaVersionBox.getValue().trim() : "3.9.0",
                isSbtSelected() && sbtDownloadSourcesCheck.isSelected(),
                isSbtSelected() && scalaDownloadSourcesCheck.isSelected(),
                scalaOptionalBracesCheck.isSelected(),
                isSbtSelected() && scalaPackagePrefixField.getText() != null ? scalaPackagePrefixField.getText().trim() : "",
                isSbtSelected() && scalaModuleNameField.getText() != null && !scalaModuleNameField.getText().isBlank() ? scalaModuleNameField.getText().trim() : name,
                getSelectedPythonInterpreterType(),
                getSelectedPythonPath(),
                getSelectedPythonVersion(),
                uvPathField.getText().trim(),
                condaPathField.getText().trim(),
                customEnvGenerateNewRadio.isSelected(),
                customTypeCombo.getValue() != null ? customTypeCombo.getValue() : "Virtualenv",
                customLocationField.getText().trim(),
                inheritPackagesCheck.isSelected(),
                makeAvailableCheck.isSelected(),
                phpAddComposerJsonCheck.isSelected(),
                rubyInterpreterCombo.getValue() != null ? rubyInterpreterCombo.getValue().executable() : "",
                rubyAddSampleCodeCheck.isSelected(),
                selected.generator() == ProjectSpec.Generator.GO && goRootBox.getValue() != null ? goRootBox.getValue().path() : "",
                goVendoringCheck.isSelected(),
                goEnvironmentField.getText().trim());

        Path targetDir = spec.projectDir();
        boolean requiresEmptySlot = selected.generator() == ProjectSpec.Generator.MAVEN_ARCHETYPE
                || selected.generator() == ProjectSpec.Generator.RUST;
        if (requiresEmptySlot && java.nio.file.Files.exists(targetDir)) {
            errorLabel.setText("A file or folder already exists at " + targetDir
                    + " \u2014 choose a different name or location.");
            return;
        }
        if (java.nio.file.Files.isDirectory(targetDir)) {
            try (var entries = java.nio.file.Files.list(targetDir)) {
                if (entries.findAny().isPresent()) {
                    errorLabel.setText("The folder " + targetDir
                            + " already exists and is not empty \u2014 "
                            + "choose a different name or location.");
                    return;
                }
            } catch (IOException ex) {
                errorLabel.setText("Can't read " + targetDir + ": " + ex.getMessage());
                return;
            }
        }

        stage.close();
        onCreate.accept(spec);
    }

    // -------------------------------------------------------------- helpers

    /** The version box always has a concrete selection; "" lets the
     *  generator fall back to start.spring.io's own default if needed. */
    private static String emptyIfDefault(String version) {
        return version == null ? "" : version;
    }

    private static Label formLabel(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("form-label");
        return l;
    }

    private static Node createBrowseFolderIcon() {
        SVGPath folder = new SVGPath();
        folder.setContent("M 1.5,3 C 1.5,2.4 1.9,2 2.5,2 L 5.8,2 C 6.2,2 6.6,2.2 6.8,2.6 L 8,4.2 L 13.5,4.2 C 14.1,4.2 14.5,4.6 14.5,5.2 L 14.5,12 C 14.5,12.6 14.1,13 13.5,13 L 2.5,13 C 1.9,13 1.5,12.6 1.5,12 Z");
        folder.setFill(Color.TRANSPARENT);
        folder.setStroke(Color.web("#8C919D"));
        folder.setStrokeWidth(1.2);
        return folder;
    }

    private Node formLabelWithHelp(String text, String helpText) {
        Label label = formLabel(text);
        Label help = new Label("?");
        help.setStyle("-fx-text-fill: #707890; -fx-font-size: 10px; -fx-cursor: hand; "
                + "-fx-border-color: #707890; -fx-border-radius: 8; -fx-min-width: 14px; "
                + "-fx-alignment: center; -fx-padding: 0 2 0 2;");
        javafx.scene.control.Tooltip tip = new javafx.scene.control.Tooltip(helpText);
        javafx.scene.control.Tooltip.install(help, tip);
        HBox box = new HBox(4, label, help);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private Node createMultiModuleNode() {
        Label help = new Label("?");
        help.setStyle("-fx-text-fill: #707890; -fx-font-size: 10px; -fx-cursor: hand; "
                + "-fx-border-color: #707890; -fx-border-radius: 8; -fx-min-width: 14px; "
                + "-fx-alignment: center; -fx-padding: 0 2 0 2;");
        javafx.scene.control.Tooltip tip = new javafx.scene.control.Tooltip("Create a project with a root directory and a subproject.");
        javafx.scene.control.Tooltip.install(help, tip);
        HBox box = new HBox(6, multiModuleCheck, help);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private HBox segmented(ToggleGroup group, boolean selectFirst, String... options) {
        HBox box = new HBox(0);
        box.getStyleClass().add("segmented");
        for (int i = 0; i < options.length; i++) {
            ToggleButton b = new ToggleButton(options[i]);
            b.setToggleGroup(group);
            b.getStyleClass().add("segment");
            if (i == 0) b.getStyleClass().add("segment-first");
            if (i == options.length - 1) b.getStyleClass().add("segment-last");
            if (i == 0 && selectFirst) b.setSelected(true);
            // never allow zero selection
            b.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_RELEASED, e -> {
                if (b.isSelected()) e.consume();
            });
            box.getChildren().add(b);
        }
        return box;
    }

    private Button compactButton(String text) {
        Button button = new Button(text);
        button.getStyleClass().add("console-button");
        return button;
    }

    private HBox wideRow(Node field, Node button) {
        HBox row = new HBox(6, field, button);
        HBox.setHgrow(field, Priority.ALWAYS);
        return row;
    }

    private static String sanitize(String s) {
        return s == null ? "" : s.toLowerCase().replaceAll("[^a-z0-9.\\-]", "");
    }
}