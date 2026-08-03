// SettingsInspectionsPage.java
package dev.lumina.ui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.control.cell.CheckBoxListCell;

import java.util.HashMap;
import java.util.Map;

/**
 * IntelliJ-style Editor > Inspections settings page.
 * Complete implementation matching all screenshots.
 */
public class SettingsInspectionsPage extends VBox {

    private final ListView<String> categoryList = new ListView<>();
    private final ListView<InspectionItem> inspectionList = new ListView<>();
    private final Map<String, ObservableList<InspectionItem>> categoryInspections = new HashMap<>();
    private final ComboBox<String> scopeCombo = new ComboBox<>();
    private final ComboBox<String> severityCombo = new ComboBox<>();
    private final ComboBox<String> highlightingCombo = new ComboBox<>();
    private final CheckBox disableNewInspections = new CheckBox("Disable new inspections by default");

    public SettingsInspectionsPage() {
        getStyleClass().add("settings-page");
        setPadding(new Insets(8, 0, 8, 0));
        setSpacing(14);

        // ============================================================
        // Profile section
        // ============================================================
        HBox profileRow = new HBox(8);
        profileRow.setPadding(new Insets(4, 0, 8, 20));
        profileRow.setAlignment(Pos.CENTER_LEFT);

        Label profileLabel = new Label("Profile:");
        profileLabel.getStyleClass().add("settings-label");
        ComboBox<String> profileCombo = new ComboBox<>();
        profileCombo.getItems().addAll("Project Default Project", "Default", "Project");
        profileCombo.getSelectionModel().selectFirst();
        profileCombo.getStyleClass().add("settings-combo");
        profileCombo.setPrefWidth(200);
        profileRow.getChildren().addAll(profileLabel, profileCombo);

        // ============================================================
        // Main layout: Category list + Inspection list
        // ============================================================
        Label categoriesLabel = new Label("Categories");
        categoriesLabel.getStyleClass().add("settings-section");

        // Category list on the left
        categoryList.getStyleClass().add("settings-list");
        categoryList.setPrefHeight(300);
        categoryList.setPrefWidth(200);
        categoryList.getItems().addAll(
            "User defined",
            "Angular",
            "AOP",
            "Application servers",
            "Bean Validation",
            "CDI (Contexts and Dependency Injection)",
            "Code Coverage",
            "Code metrics",
            "Compose Multiplatform Preview",
            "Cron",
            "CSS",
            "Dev Container",
            "Docker-compose",
            "Dockerfile",
            "EditorConfig",
            "EL",
            "FreeMarker",
            "General",
            "GitHub actions",
            "GitLab CI/CD",
            "Gradle",
            "Gradle Declarative",
            "Groovy",
            "Hibernate",
            "HTML",
            "HTTP Client",
            "Inappropriate gRPC request scheme",
            "Internationalization",
            "Jakarta Data",
            "Java",
            "Java EE",
            "JavaFX",
            "JavaScript and TypeScript",
            "JPA",
            "JSON and JSON5",
            "JSONPath",
            "JSP",
            "JUnit",
            "JVM languages",
            "Kotlin",
            "Ktor",
            "Kubernetes",
            "Language injection",
            "Less",
            "Liquibase",
            "Manifest",
            "Markdown",
            "Maven",
            "Micronaut",
            "MongoDB",
            "MySQL",
            "OpenAPI specifications",
            "Oracle",
            "Pattern validation",
            "PostCSS",
            "PostgreSQL",
            "Proofreading",
            "Properties files",
            "Protocol Buffers",
            "Qodana",
            "Quarkus",
            "RegExp",
            "RELAX NG",
            "RESTful Web Service (JAX-RS)",
            "Rust",
            "Sass/SCSS",
            "Security",
            "Shell script",
            "Spring",
            "Spring Data",
            "Spring Modulith",
            "SQL",
            "SQL server",
            "Thymeleaf",
            "TOML",
            "Velocity",
            "Version control",
            "Vue",
            "XML",
            "XPath",
            "XSLT",
            "YAML"
        );
        categoryList.getSelectionModel().select("Java");

        // Inspection list on the right
        inspectionList.getStyleClass().add("settings-list");
        inspectionList.setPrefHeight(300);
        inspectionList.setPrefWidth(350);
        inspectionList.setCellFactory(CheckBoxListCell.forListView(
            item -> item.selectedProperty()
        ));

        // Populate inspections for each category
        populateInspections();

        // Update inspection list when category changes
        categoryList.getSelectionModel().selectedItemProperty().addListener((obs, old, category) -> {
            if (category != null) {
                inspectionList.setItems(categoryInspections.getOrDefault(category, FXCollections.observableArrayList()));
            }
        });

        // Category and inspection split
        HBox listSplit = new HBox(16);
        listSplit.setPadding(new Insets(4, 0, 8, 0));
        VBox catBox = new VBox(6, new Label("Categories:"), categoryList);
        VBox inspBox = new VBox(6, new Label("Inspections:"), inspectionList);
        HBox.setHgrow(inspBox, Priority.ALWAYS);
        listSplit.getChildren().addAll(catBox, inspBox);

        // ============================================================
        // Bottom section: Disable new inspections + Scope/Severity/Highlighting
        // ============================================================
        disableNewInspections.getStyleClass().add("settings-check");
        disableNewInspections.setPadding(new Insets(4, 0, 8, 0));

        HBox bottomRow = new HBox(20);
        bottomRow.setPadding(new Insets(8, 0, 8, 0));
        bottomRow.setAlignment(Pos.CENTER_LEFT);

        // Scope
        Label scopeLabel = new Label("Scope:");
        scopeLabel.getStyleClass().add("settings-label");
        scopeCombo.getItems().addAll("In All Scopes", "Current Scope", "Project Scope");
        scopeCombo.getSelectionModel().selectFirst();
        scopeCombo.getStyleClass().add("settings-combo");
        scopeCombo.setPrefWidth(140);

        // Severity
        Label severityLabel = new Label("Severity:");
        severityLabel.getStyleClass().add("settings-label");
        severityCombo.getItems().addAll("Mixed", "Warning", "Error", "Info", "Weak Warning");
        severityCombo.getSelectionModel().selectFirst();
        severityCombo.getStyleClass().add("settings-combo");
        severityCombo.setPrefWidth(140);

        // Highlighting
        Label highlightingLabel = new Label("Highlighting in editor:");
        highlightingLabel.getStyleClass().add("settings-label");
        highlightingCombo.getItems().addAll("Mixed", "Warning", "Error", "Info", "Weak Warning");
        highlightingCombo.getSelectionModel().selectFirst();
        highlightingCombo.getStyleClass().add("settings-combo");
        highlightingCombo.setPrefWidth(140);

        bottomRow.getChildren().addAll(
            scopeLabel, scopeCombo,
            severityLabel, severityCombo,
            highlightingLabel, highlightingCombo
        );

        // ============================================================
        // Assemble all sections
        // ============================================================
        getChildren().addAll(
            profileRow,
            categoriesLabel,
            listSplit,
            disableNewInspections,
            bottomRow
        );

        // Set initial selection
        categoryList.getSelectionModel().select("Java");
    }

    private void populateInspections() {
        // Java inspections
        ObservableList<InspectionItem> javaInspections = FXCollections.observableArrayList(
            new InspectionItem("Abstraction issues", true),
            new InspectionItem("Assignment issues", true),
            new InspectionItem("Bitwise operation issues", true),
            new InspectionItem("Class metrics", true),
            new InspectionItem("Class structure", true),
            new InspectionItem("Cloning issues", true),
            new InspectionItem("Code maturity", true),
            new InspectionItem("Code style issues", true),
            new InspectionItem("Compiler issues", true),
            new InspectionItem("Concurrency annotation issues", true),
            new InspectionItem("Control flow issues", true),
            new InspectionItem("Data flow", true),
            new InspectionItem("Declaration redundancy", true),
            new InspectionItem("Dependency issues", true),
            new InspectionItem("Encapsulation", true),
            new InspectionItem("Error handling", true),
            new InspectionItem("Finalization", true),
            new InspectionItem("Imports", true),
            new InspectionItem("Inheritance issues", true),
            new InspectionItem("Initialization", true),
            new InspectionItem("Internationalization", true),
            new InspectionItem("Java language level issues", true),
            new InspectionItem("Java language level migration aids", true),
            new InspectionItem("JavaBeans issues", true),
            new InspectionItem("Javadoc", true),
            new InspectionItem("JUnit", true),
            new InspectionItem("Logging", true),
            new InspectionItem("Lombok", true),
            new InspectionItem("Memory", true),
            new InspectionItem("Method metrics", true),
            new InspectionItem("Modularization issues", true),
            new InspectionItem("Naming conventions", true),
            new InspectionItem("Numeric issues", true),
            new InspectionItem("Packaging issues", true),
            new InspectionItem("Performance", true),
            new InspectionItem("Portability", true),
            new InspectionItem("Probable bugs", true),
            new InspectionItem("Properties files", true),
            new InspectionItem("Reflective access", true),
            new InspectionItem("Resource management", true),
            new InspectionItem("Security", true),
            new InspectionItem("Serialization issues", true),
            new InspectionItem("Test frameworks", true),
            new InspectionItem("TestNG", true),
            new InspectionItem("Threading issues", true),
            new InspectionItem("toString() issues", true),
            new InspectionItem("Verbose or redundant code constructs", true),
            new InspectionItem("Visibility", true)
        );
        categoryInspections.put("Java", javaInspections);

        // Java EE inspections
        ObservableList<InspectionItem> javaEEInspections = FXCollections.observableArrayList(
            new InspectionItem("Application configuration file", true),
            new InspectionItem("Contexts and Dependency Injection (CDI)", true),
            new InspectionItem("Persistence (JPA)", true),
            new InspectionItem("RESTful Web Services (JAX-RS)", true)
        );
        categoryInspections.put("Java EE", javaEEInspections);

        // JavaScript and TypeScript inspections
        ObservableList<InspectionItem> jsInspections = FXCollections.observableArrayList(
            new InspectionItem("Implemented", true),
            new InspectionItem("Implementing", true),
            new InspectionItem("JavaScript source", true),
            new InspectionItem("Overridden", true),
            new InspectionItem("Overriding", true),
            new InspectionItem("Recursive call", true),
            new InspectionItem("TypeScript", true)
        );
        categoryInspections.put("JavaScript and TypeScript", jsInspections);

        // JPA inspections
        ObservableList<InspectionItem> jpaInspections = FXCollections.observableArrayList(
            new InspectionItem("JPA Queries", true),
            new InspectionItem("Persistence", true),
            new InspectionItem("Entity", true),
            new InspectionItem("Mapping", true)
        );
        categoryInspections.put("JPA", jpaInspections);

        // HTML inspections
        ObservableList<InspectionItem> htmlInspections = FXCollections.observableArrayList(
            new InspectionItem("HTML", true),
            new InspectionItem("HTML 5", true),
            new InspectionItem("HTML 5 Accessibility", true),
            new InspectionItem("HTML 5 Mobile", true),
            new InspectionItem("HTML 5 Performance", true),
            new InspectionItem("HTML 5 SEO", true)
        );
        categoryInspections.put("HTML", htmlInspections);

        // Spring inspections
        ObservableList<InspectionItem> springInspections = FXCollections.observableArrayList(
            new InspectionItem("Spring", true),
            new InspectionItem("Spring Boot", true),
            new InspectionItem("Spring Data", true),
            new InspectionItem("Spring Modulith", true),
            new InspectionItem("Spring Cloud Stream bindings", true),
            new InspectionItem("Repositories", true),
            new InspectionItem("Request mappings", true)
        );
        categoryInspections.put("Spring", springInspections);

        // SQL inspections
        ObservableList<InspectionItem> sqlInspections = FXCollections.observableArrayList(
            new InspectionItem("SQL", true),
            new InspectionItem("SQL server", true),
            new InspectionItem("PostgreSQL", true),
            new InspectionItem("MySQL", true),
            new InspectionItem("Oracle", true),
            new InspectionItem("MongoDB", true)
        );
        categoryInspections.put("SQL", sqlInspections);

        // Kotlin inspections
        ObservableList<InspectionItem> kotlinInspections = FXCollections.observableArrayList(
            new InspectionItem("DSL markers", true),
            new InspectionItem("Implemented declaration", true),
            new InspectionItem("Multiplatform actual declaration", true),
            new InspectionItem("Multiplatform expect declaration", true),
            new InspectionItem("Overridden declaration", true),
            new InspectionItem("Overriding declaration", true),
            new InspectionItem("Recursive call", true)
        );
        categoryInspections.put("Kotlin", kotlinInspections);

        // Rust inspections
        ObservableList<InspectionItem> rustInspections = FXCollections.observableArrayList(
            new InspectionItem("Rust", true),
            new InspectionItem("One-line methods", true),
            new InspectionItem("Move errors", true)
        );
        categoryInspections.put("Rust", rustInspections);

        // Markdown inspections
        ObservableList<InspectionItem> markdownInspections = FXCollections.observableArrayList(
            new InspectionItem("Markdown", true),
            new InspectionItem("Collapse front matter", true),
            new InspectionItem("Collapse links", true),
            new InspectionItem("Collapse tables", true),
            new InspectionItem("Collapse code fences", true),
            new InspectionItem("Collapse table of contents", true)
        );
        categoryInspections.put("Markdown", markdownInspections);

        // JSON inspections
        ObservableList<InspectionItem> jsonInspections = FXCollections.observableArrayList(
            new InspectionItem("JSON", true),
            new InspectionItem("JSON and JSON5", true),
            new InspectionItem("JSONPath", true)
        );
        categoryInspections.put("JSON and JSON5", jsonInspections);

        // XML inspections
        ObservableList<InspectionItem> xmlInspections = FXCollections.observableArrayList(
            new InspectionItem("XML", true),
            new InspectionItem("XML tags", true),
            new InspectionItem("XML entities", true),
            new InspectionItem("XPath", true),
            new InspectionItem("XSLT", true)
        );
        categoryInspections.put("XML", xmlInspections);

        // YAML inspections
        ObservableList<InspectionItem> yamlInspections = FXCollections.observableArrayList(
            new InspectionItem("YAML", true),
            new InspectionItem("YAML", true)
        );
        categoryInspections.put("YAML", yamlInspections);

        // General inspections
        ObservableList<InspectionItem> generalInspections = FXCollections.observableArrayList(
            new InspectionItem("General", true),
            new InspectionItem("Proofreading", true),
            new InspectionItem("Properties files", true),
            new InspectionItem("Version control", true)
        );
        categoryInspections.put("General", generalInspections);

        // Maven inspections
        ObservableList<InspectionItem> mavenInspections = FXCollections.observableArrayList(
            new InspectionItem("Maven", true),
            new InspectionItem("Maven pom.xml", true)
        );
        categoryInspections.put("Maven", mavenInspections);

        // Gradle inspections
        ObservableList<InspectionItem> gradleInspections = FXCollections.observableArrayList(
            new InspectionItem("Gradle", true),
            new InspectionItem("Gradle Declarative", true),
            new InspectionItem("Gradle build", true)
        );
        categoryInspections.put("Gradle", gradleInspections);

        // Kubernetes inspections
        ObservableList<InspectionItem> k8sInspections = FXCollections.observableArrayList(
            new InspectionItem("Kubernetes", true),
            new InspectionItem("Helm repository actions", true),
            new InspectionItem("Kubernetes label navigation", true)
        );
        categoryInspections.put("Kubernetes", k8sInspections);

        // Micronaut inspections
        ObservableList<InspectionItem> micronautInspections = FXCollections.observableArrayList(
            new InspectionItem("Micronaut", true),
            new InspectionItem("Application events", true),
            new InspectionItem("Cacheable operations", true),
            new InspectionItem("Contexts and dependency injection", true),
            new InspectionItem("Datasource from YAML file", true),
            new InspectionItem("HTTP mappings", true),
            new InspectionItem("Management endpoints mappings", true),
            new InspectionItem("Micronaut Data MongoDB mapping", true),
            new InspectionItem("Micronaut MQ methods", true),
            new InspectionItem("WebSocket mappings", true)
        );
        categoryInspections.put("Micronaut", micronautInspections);

        // Quarkus inspections
        ObservableList<InspectionItem> quarkusInspections = FXCollections.observableArrayList(
            new InspectionItem("Quarkus", true),
            new InspectionItem("Cacheable operations", true),
            new InspectionItem("Datasource from YAML file", true),
            new InspectionItem("Scheduled tasks", true)
        );
        categoryInspections.put("Quarkus", quarkusInspections);

        // JUnit inspections
        ObservableList<InspectionItem> junitInspections = FXCollections.observableArrayList(
            new InspectionItem("JUnit", true),
            new InspectionItem("JUnit 4", true),
            new InspectionItem("JUnit 5", true)
        );
        categoryInspections.put("JUnit", junitInspections);

        // JVM languages inspections
        ObservableList<InspectionItem> jvmInspections = FXCollections.observableArrayList(
            new InspectionItem("JVM languages", true),
            new InspectionItem("Groovy", true)
        );
        categoryInspections.put("JVM languages", jvmInspections);

        // FreeMarker inspections
        ObservableList<InspectionItem> freemarkerInspections = FXCollections.observableArrayList(
            new InspectionItem("FreeMarker", true),
            new InspectionItem("EL", true)
        );
        categoryInspections.put("FreeMarker", freemarkerInspections);

        // CSS inspections
        ObservableList<InspectionItem> cssInspections = FXCollections.observableArrayList(
            new InspectionItem("CSS", true),
            new InspectionItem("Less", true),
            new InspectionItem("Sass/SCSS", true),
            new InspectionItem("PostCSS", true)
        );
        categoryInspections.put("CSS", cssInspections);

        // HTTP Client inspections
        ObservableList<InspectionItem> httpInspections = FXCollections.observableArrayList(
            new InspectionItem("HTTP Client", true),
            new InspectionItem("Inappropriate gRPC request scheme", true)
        );
        categoryInspections.put("HTTP Client", httpInspections);

        // Hibernate inspections
        ObservableList<InspectionItem> hibernateInspections = FXCollections.observableArrayList(
            new InspectionItem("Hibernate", true),
            new InspectionItem("JPA", true)
        );
        categoryInspections.put("Hibernate", hibernateInspections);

        // Docker inspections
        ObservableList<InspectionItem> dockerInspections = FXCollections.observableArrayList(
            new InspectionItem("Docker-compose", true),
            new InspectionItem("Dockerfile", true),
            new InspectionItem("Dev Container", true)
        );
        categoryInspections.put("Docker-compose", dockerInspections);

        // EditorConfig inspections
        ObservableList<InspectionItem> editorConfigInspections = FXCollections.observableArrayList(
            new InspectionItem("EditorConfig", true)
        );
        categoryInspections.put("EditorConfig", editorConfigInspections);

        // GitHub actions inspections
        ObservableList<InspectionItem> githubActionsInspections = FXCollections.observableArrayList(
            new InspectionItem("GitHub actions", true),
            new InspectionItem("GitLab CI/CD", true)
        );
        categoryInspections.put("GitHub actions", githubActionsInspections);

        // Angular inspections
        ObservableList<InspectionItem> angularInspections = FXCollections.observableArrayList(
            new InspectionItem("Angular", true)
        );
        categoryInspections.put("Angular", angularInspections);

        // Bean Validation inspections
        ObservableList<InspectionItem> beanValidationInspections = FXCollections.observableArrayList(
            new InspectionItem("Bean Validation", true),
            new InspectionItem("Hibernate", true)
        );
        categoryInspections.put("Bean Validation", beanValidationInspections);

        // CDI inspections
        ObservableList<InspectionItem> cdiInspections = FXCollections.observableArrayList(
            new InspectionItem("CDI (Contexts and Dependency Injection)", true),
            new InspectionItem("Injection points", true),
            new InspectionItem("Producers for Disposer methods", true)
        );
        categoryInspections.put("CDI (Contexts and Dependency Injection)", cdiInspections);

        // Code Coverage inspections
        ObservableList<InspectionItem> codeCoverageInspections = FXCollections.observableArrayList(
            new InspectionItem("Code Coverage", true),
            new InspectionItem("Code metrics", true)
        );
        categoryInspections.put("Code Coverage", codeCoverageInspections);

        // Code metrics inspections
        ObservableList<InspectionItem> codeMetricsInspections = FXCollections.observableArrayList(
            new InspectionItem("Code metrics", true),
            new InspectionItem("Class metrics", true),
            new InspectionItem("Method metrics", true)
        );
        categoryInspections.put("Code metrics", codeMetricsInspections);

        // Compose Multiplatform Preview inspections
        ObservableList<InspectionItem> composeInspections = FXCollections.observableArrayList(
            new InspectionItem("Compose Multiplatform Preview", true)
        );
        categoryInspections.put("Compose Multiplatform Preview", composeInspections);

        // Cron inspections
        ObservableList<InspectionItem> cronInspections = FXCollections.observableArrayList(
            new InspectionItem("Cron", true)
        );
        categoryInspections.put("Cron", cronInspections);

        // Internationalization inspections
        ObservableList<InspectionItem> i18nInspections = FXCollections.observableArrayList(
            new InspectionItem("Internationalization", true),
            new InspectionItem("I18n strings", true)
        );
        categoryInspections.put("Internationalization", i18nInspections);

        // Jakarta Data inspections
        ObservableList<InspectionItem> jakartaDataInspections = FXCollections.observableArrayList(
            new InspectionItem("Jakarta Data", true),
            new InspectionItem("Jakarta EE", true)
        );
        categoryInspections.put("Jakarta Data", jakartaDataInspections);

        // JavaFX inspections
        ObservableList<InspectionItem> javafxInspections = FXCollections.observableArrayList(
            new InspectionItem("JavaFX", true),
            new InspectionItem("JavaFX redundant property values", true),
            new InspectionItem("JavaFX unused imports", true),
            new InspectionItem("Event handler method signature problems", true),
            new InspectionItem("Unnecessary default tag", true),
            new InspectionItem("Unresolved f:uid attribute reference", true),
            new InspectionItem("Unresolved style class reference", true),
            new InspectionItem("The value from properties file is incompatible with the attribute type", true)
        );
        categoryInspections.put("JavaFX", javafxInspections);

        // Ktor inspections
        ObservableList<InspectionItem> ktorInspections = FXCollections.observableArrayList(
            new InspectionItem("Ktor", true)
        );
        categoryInspections.put("Ktor", ktorInspections);

        // Language injection inspections
        ObservableList<InspectionItem> langInjectionInspections = FXCollections.observableArrayList(
            new InspectionItem("Language injection", true),
            new InspectionItem("Language Injections", true)
        );
        categoryInspections.put("Language injection", langInjectionInspections);

        // Liquibase inspections
        ObservableList<InspectionItem> liquibaseInspections = FXCollections.observableArrayList(
            new InspectionItem("Liquibase", true)
        );
        categoryInspections.put("Liquibase", liquibaseInspections);

        // Manifest inspections
        ObservableList<InspectionItem> manifestInspections = FXCollections.observableArrayList(
            new InspectionItem("Manifest", true)
        );
        categoryInspections.put("Manifest", manifestInspections);

        // OpenAPI inspections
        ObservableList<InspectionItem> openapiInspections = FXCollections.observableArrayList(
            new InspectionItem("OpenAPI specifications", true)
        );
        categoryInspections.put("OpenAPI specifications", openapiInspections);

        // Pattern validation inspections
        ObservableList<InspectionItem> patternInspections = FXCollections.observableArrayList(
            new InspectionItem("Pattern validation", true)
        );
        categoryInspections.put("Pattern validation", patternInspections);

        // Protocol Buffers inspections
        ObservableList<InspectionItem> protobufInspections = FXCollections.observableArrayList(
            new InspectionItem("Protocol Buffers", true)
        );
        categoryInspections.put("Protocol Buffers", protobufInspections);

        // Qodana inspections
        ObservableList<InspectionItem> qodanaInspections = FXCollections.observableArrayList(
            new InspectionItem("Qodana", true)
        );
        categoryInspections.put("Qodana", qodanaInspections);

        // RegExp inspections
        ObservableList<InspectionItem> regexpInspections = FXCollections.observableArrayList(
            new InspectionItem("RegExp", true),
            new InspectionItem("RELAX NG", true)
        );
        categoryInspections.put("RegExp", regexpInspections);

        // RESTful Web Service inspections
        ObservableList<InspectionItem> restInspections = FXCollections.observableArrayList(
            new InspectionItem("RESTful Web Service (JAX-RS)", true),
            new InspectionItem("Open in HTTP Client JAX-RS RequestMapping", true)
        );
        categoryInspections.put("RESTful Web Service (JAX-RS)", restInspections);

        // Security inspections
        ObservableList<InspectionItem> securityInspections = FXCollections.observableArrayList(
            new InspectionItem("Security", true)
        );
        categoryInspections.put("Security", securityInspections);

        // Shell script inspections
        ObservableList<InspectionItem> shellInspections = FXCollections.observableArrayList(
            new InspectionItem("Shell script", true)
        );
        categoryInspections.put("Shell script", shellInspections);

        // Spring Data inspections
        ObservableList<InspectionItem> springDataInspections = FXCollections.observableArrayList(
            new InspectionItem("Spring Data", true),
            new InspectionItem("Spring Data JDBC mapping", true),
            new InspectionItem("Spring Data MongoDB mapping", true),
            new InspectionItem("Spring Data projections", true)
        );
        categoryInspections.put("Spring Data", springDataInspections);

        // Spring Modulith inspections
        ObservableList<InspectionItem> springModulithInspections = FXCollections.observableArrayList(
            new InspectionItem("Spring Modulith", true)
        );
        categoryInspections.put("Spring Modulith", springModulithInspections);

        // SQL server inspections
        ObservableList<InspectionItem> sqlServerInspections = FXCollections.observableArrayList(
            new InspectionItem("SQL server", true)
        );
        categoryInspections.put("SQL server", sqlServerInspections);

        // Thymeleaf inspections
        ObservableList<InspectionItem> thymeleafInspections = FXCollections.observableArrayList(
            new InspectionItem("Thymeleaf", true)
        );
        categoryInspections.put("Thymeleaf", thymeleafInspections);

        // TOML inspections
        ObservableList<InspectionItem> tomlInspections = FXCollections.observableArrayList(
            new InspectionItem("TOML", true)
        );
        categoryInspections.put("TOML", tomlInspections);

        // Velocity inspections
        ObservableList<InspectionItem> velocityInspections = FXCollections.observableArrayList(
            new InspectionItem("Velocity", true)
        );
        categoryInspections.put("Velocity", velocityInspections);

        // Vue inspections
        ObservableList<InspectionItem> vueInspections = FXCollections.observableArrayList(
            new InspectionItem("Vue", true)
        );
        categoryInspections.put("Vue", vueInspections);

        // XPath inspections
        ObservableList<InspectionItem> xpathInspections = FXCollections.observableArrayList(
            new InspectionItem("XPath", true)
        );
        categoryInspections.put("XPath", xpathInspections);

        // XSLT inspections
        ObservableList<InspectionItem> xsltInspections = FXCollections.observableArrayList(
            new InspectionItem("XSLT", true)
        );
        categoryInspections.put("XSLT", xsltInspections);

        // Application servers inspections
        ObservableList<InspectionItem> appServerInspections = FXCollections.observableArrayList(
            new InspectionItem("Application servers", true)
        );
        categoryInspections.put("Application servers", appServerInspections);

        // AOP inspections
        ObservableList<InspectionItem> aopInspections = FXCollections.observableArrayList(
            new InspectionItem("AOP", true),
            new InspectionItem("AOP Pointcut Language", true)
        );
        categoryInspections.put("AOP", aopInspections);

        // User defined inspections (for the root category)
        ObservableList<InspectionItem> userDefinedInspections = FXCollections.observableArrayList(
            new InspectionItem("User defined", true)
        );
        categoryInspections.put("User defined", userDefinedInspections);

        // JSP inspections
        ObservableList<InspectionItem> jspInspections = FXCollections.observableArrayList(
            new InspectionItem("JSP", true),
            new InspectionItem("JSP", true)
        );
        categoryInspections.put("JSP", jspInspections);

        // JSONPath inspections
        ObservableList<InspectionItem> jsonPathInspections = FXCollections.observableArrayList(
            new InspectionItem("JSONPath", true)
        );
        categoryInspections.put("JSONPath", jsonPathInspections);

        // JVM languages inspections
        categoryInspections.put("JVM languages", jvmInspections);
    }

    // ============================================================
    // InspectionItem class
    // ============================================================
    public static class InspectionItem {
        private final String name;
        private final javafx.beans.property.BooleanProperty selected;

        public InspectionItem(String name, boolean selected) {
            this.name = name;
            this.selected = new javafx.beans.property.SimpleBooleanProperty(selected);
        }

        public String getName() {
            return name;
        }

        public javafx.beans.property.BooleanProperty selectedProperty() {
            return selected;
        }

        public boolean isSelected() {
            return selected.get();
        }

        public void setSelected(boolean selected) {
            this.selected.set(selected);
        }

        @Override
        public String toString() {
            return name;
        }
    }
}