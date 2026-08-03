// SettingsGutterIconsPage.java
package dev.lumina.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/**
 * IntelliJ-style Editor > General > Gutter Icons settings page.
 * Complete implementation matching all five screenshots.
 */
public class SettingsGutterIconsPage extends VBox {

    public SettingsGutterIconsPage() {
        getStyleClass().add("settings-page");
        setPadding(new Insets(8, 0, 8, 0));
        setSpacing(14);

        // ============================================================
        // Show gutter icons - main checkbox
        // ============================================================
        CheckBox showGutterIcons = new CheckBox("Show gutter icons");
        showGutterIcons.setSelected(true);
        showGutterIcons.getStyleClass().add("settings-check");
        showGutterIcons.setPadding(new Insets(0, 0, 8, 0));

        // ============================================================
        // AOP (Java/Kotlin) section
        // ============================================================
        Label aopLabel = new Label("AOP (Java/Kotlin)");
        aopLabel.getStyleClass().add("settings-section");

        CheckBox aop = new CheckBox("AOP (Java/Kotlin)");
        aop.setSelected(true);
        aop.getStyleClass().add("settings-check");

        VBox aopBox = new VBox(4);
        aopBox.setPadding(new Insets(4, 0, 8, 20));
        aopBox.getChildren().add(aop);

        // ============================================================
        // Common section (first group of checkboxes from image 1)
        // ============================================================
        Label commonLabel = new Label("Common");
        commonLabel.getStyleClass().add("settings-section");

        VBox commonBox = new VBox(4);
        commonBox.setPadding(new Insets(4, 0, 8, 20));

        String[] commonItems = {
            "Color preview",
            "Documentation comments in-place rendering",
            "Run line marker",
            "Compose color picker",
            "Recursive call",
            "Add Modified Settings from IDE",
            "Code preview",
            "Action color",
            "Action icon",
            "Execute request",
            "HTTP Client line markers",
            "HTTP response diff",
            "Retrofit open in httpClient",
            "Run HTTP Request",
            "Set certificate passphrase"
        };

        for (String item : commonItems) {
            CheckBox cb = new CheckBox(item);
            cb.setSelected(true);
            cb.getStyleClass().add("settings-check");
            commonBox.getChildren().add(cb);
        }

        // ============================================================
        // Jakarta EE: Application configuration file
        // ============================================================
        Label jakartaAppLabel = new Label("JavaEE:Application configuration file");
        jakartaAppLabel.getStyleClass().add("settings-section");

        CheckBox jakartaApp = new CheckBox("JavaEE:Application configuration file");
        jakartaApp.setSelected(true);
        jakartaApp.getStyleClass().add("settings-check");

        VBox jakartaAppBox = new VBox(4);
        jakartaAppBox.setPadding(new Insets(4, 0, 8, 20));
        jakartaAppBox.getChildren().add(jakartaApp);

        // ============================================================
        // Jakarta EE: Contexts and Dependency Injection (CDI)
        // ============================================================
        Label cdiLabel = new Label("Jakarta EE: Contexts and Dependency Injection (CDI)");
        cdiLabel.getStyleClass().add("settings-section");

        VBox cdiBox = new VBox(4);
        cdiBox.setPadding(new Insets(4, 0, 8, 20));

        String[] cdiItems = {
            "Injection points",
            "Producers for Disposer methods"
        };

        for (String item : cdiItems) {
            CheckBox cb = new CheckBox(item);
            cb.setSelected(true);
            cb.getStyleClass().add("settings-check");
            cdiBox.getChildren().add(cb);
        }

        // ============================================================
        // Jakarta EE: Persistence (JPA)
        // ============================================================
        Label jpaLabel = new Label("Jakarta EE: Persistence (JPA)");
        jpaLabel.getStyleClass().add("settings-section");

        CheckBox jpa = new CheckBox("Jakarta EE: Persistence (JPA)");
        jpa.setSelected(true);
        jpa.getStyleClass().add("settings-check");

        VBox jpaBox = new VBox(4);
        jpaBox.setPadding(new Insets(4, 0, 8, 20));
        jpaBox.getChildren().add(jpa);

        // ============================================================
        // Jakarta EE: RESTful Web Services (JAX-RS)
        // ============================================================
        Label jaxrsLabel = new Label("Jakarta EE: RESTful Web Services (JAX-RS)");
        jaxrsLabel.getStyleClass().add("settings-section");

        VBox jaxrsBox = new VBox(4);
        jaxrsBox.setPadding(new Insets(4, 0, 8, 20));

        CheckBox openHttpClient = new CheckBox("Open in HTTP Client JAX-RS RequestMapping");
        openHttpClient.setSelected(true);
        openHttpClient.getStyleClass().add("settings-check");

        CheckBox externalAnnotations = new CheckBox("External annotations");
        externalAnnotations.setSelected(true);
        externalAnnotations.getStyleClass().add("settings-check");

        CheckBox iconPreview = new CheckBox("Icon preview");
        iconPreview.setSelected(true);
        iconPreview.getStyleClass().add("settings-check");

        CheckBox implementedMethod = new CheckBox("Implemented method");
        implementedMethod.setSelected(true);
        implementedMethod.getStyleClass().add("settings-check");

        jaxrsBox.getChildren().addAll(openHttpClient, externalAnnotations, iconPreview, implementedMethod);

        // ============================================================
        // Java section
        // ============================================================
        Label javaLabel = new Label("Java");
        javaLabel.getStyleClass().add("settings-section");

        VBox javaBox = new VBox(4);
        javaBox.setPadding(new Insets(4, 0, 8, 20));

        String[] javaItems = {
            "External annotations",
            "Icon preview",
            "Implemented method",
            "Implementing method",
            "Inferred contract annotations",
            "Inferred nullability annotations",
            "Lambda",
            "Overridden method",
            "Overriding method",
            "Recursive call",
            "Service",
            "Sibling inherited method"
        };

        for (String item : javaItems) {
            CheckBox cb = new CheckBox(item);
            cb.setSelected(true);
            cb.getStyleClass().add("settings-check");
            javaBox.getChildren().add(cb);
        }

        // ============================================================
        // JavaScript and TypeScript section
        // ============================================================
        Label jsLabel = new Label("JavaScript and TypeScript");
        jsLabel.getStyleClass().add("settings-section");

        VBox jsBox = new VBox(4);
        jsBox.setPadding(new Insets(4, 0, 8, 20));

        String[] jsItems = {
            "Implemented",
            "Implementing",
            "JavaScript source",
            "Overridden",
            "Overriding",
            "Recursive call"
        };

        for (String item : jsItems) {
            CheckBox cb = new CheckBox(item);
            cb.setSelected(true);
            cb.getStyleClass().add("settings-check");
            jsBox.getChildren().add(cb);
        }

        // ============================================================
        // JetBrains AI Assistant section
        // ============================================================
        Label aiLabel = new Label("JetBrains AI Assistant");
        aiLabel.getStyleClass().add("settings-section");

        CheckBox formatting = new CheckBox("This code fragment is being formatted");
        formatting.setSelected(true);
        formatting.getStyleClass().add("settings-check");

        VBox aiBox = new VBox(4);
        aiBox.setPadding(new Insets(4, 0, 8, 20));
        aiBox.getChildren().add(formatting);

        // ============================================================
        // Kotlin section
        // ============================================================
        Label kotlinLabel = new Label("Kotlin");
        kotlinLabel.getStyleClass().add("settings-section");

        VBox kotlinBox = new VBox(4);
        kotlinBox.setPadding(new Insets(4, 0, 8, 20));

        String[] kotlinItems = {
            "DSL markers",
            "Implemented declaration",
            "Multiplatform actual declaration",
            "Multiplatform expect declaration",
            "Overridden declaration",
            "Overriding declaration",
            "Recursive call"
        };

        for (String item : kotlinItems) {
            CheckBox cb = new CheckBox(item);
            cb.setSelected(true);
            cb.getStyleClass().add("settings-check");
            kotlinBox.getChildren().add(cb);
        }

        // ============================================================
        // Kubernetes section
        // ============================================================
        Label k8sLabel = new Label("Kubernetes");
        k8sLabel.getStyleClass().add("settings-section");

        VBox k8sBox = new VBox(4);
        k8sBox.setPadding(new Insets(4, 0, 8, 20));

        String[] k8sItems = {
            "Helm repository actions",
            "Kubernetes label navigation",
            "Override chart values"
        };

        for (String item : k8sItems) {
            CheckBox cb = new CheckBox(item);
            cb.setSelected(true);
            cb.getStyleClass().add("settings-check");
            k8sBox.getChildren().add(cb);
        }

        // ============================================================
        // Markdown section
        // ============================================================
        Label mdLabel = new Label("Markdown");
        mdLabel.getStyleClass().add("settings-section");

        VBox mdBox = new VBox(4);
        mdBox.setPadding(new Insets(4, 0, 8, 20));

        String[] mdItems = {
            "Configure HTML image",
            "Configure Markdown image",
            "Install PlantUML"
        };

        for (String item : mdItems) {
            CheckBox cb = new CheckBox(item);
            cb.setSelected(true);
            cb.getStyleClass().add("settings-check");
            mdBox.getChildren().add(cb);
        }

        // ============================================================
        // Micronaut section
        // ============================================================
        Label micronautLabel = new Label("Micronaut");
        micronautLabel.getStyleClass().add("settings-section");

        VBox micronautBox = new VBox(4);
        micronautBox.setPadding(new Insets(4, 0, 8, 20));

        String[] micronautItems = {
            "Application events",
            "Cacheable operations",
            "Contexts and dependency injection",
            "Datasource from .yaml file",
            "Datasource from YAML file",
            "HTTP mappings",
            "Management endpoints mappings",
            "Micronaut Data MongoDB mapping",
            "Micronaut MQ methods",
            "WebSocket mappings"
        };

        for (String item : micronautItems) {
            CheckBox cb = new CheckBox(item);
            cb.setSelected(true);
            cb.getStyleClass().add("settings-check");
            micronautBox.getChildren().add(cb);
        }

        // ============================================================
        // Protocol Buffers section
        // ============================================================
        Label pbLabel = new Label("Protocol Buffers");
        pbLabel.getStyleClass().add("settings-section");

        CheckBox pbNavigate = new CheckBox("Navigate to Protocol Buffers declaration");
        pbNavigate.setSelected(true);
        pbNavigate.getStyleClass().add("settings-check");

        VBox pbBox = new VBox(4);
        pbBox.setPadding(new Insets(4, 0, 8, 20));
        pbBox.getChildren().add(pbNavigate);

        // ============================================================
        // Quarkus section
        // ============================================================
        Label quarkusLabel = new Label("Quarkus");
        quarkusLabel.getStyleClass().add("settings-section");

        VBox quarkusBox = new VBox(4);
        quarkusBox.setPadding(new Insets(4, 0, 8, 20));

        String[] quarkusItems = {
            "Cacheable operations",
            "Datasource from .yaml file",
            "Datasource from YAML file",
            "Scheduled tasks"
        };

        for (String item : quarkusItems) {
            CheckBox cb = new CheckBox(item);
            cb.setSelected(true);
            cb.getStyleClass().add("settings-check");
            quarkusBox.getChildren().add(cb);
        }

        // ============================================================
        // Rust section
        // ============================================================
        Label rustLabel = new Label("Rust");
        rustLabel.getStyleClass().add("settings-section");

        VBox rustBox = new VBox(4);
        rustBox.setPadding(new Insets(4, 0, 8, 20));

        String[] rustItems = {
            "Generated TypeScript declarations",
            "Implemented item",
            "Implementing item",
            "Open documentation",
            "Open documentation (YAML)"
        };

        for (String item : rustItems) {
            CheckBox cb = new CheckBox(item);
            cb.setSelected(true);
            cb.getStyleClass().add("settings-check");
            rustBox.getChildren().add(cb);
        }

        // ============================================================
        // Spring section
        // ============================================================
        Label springLabel = new Label("Spring");
        springLabel.getStyleClass().add("settings-section");

        VBox springBox = new VBox(4);
        springBox.setPadding(new Insets(4, 0, 8, 20));

        String[] springItems = {
            "AOP (XML)",
            "Application events",
            "Autowired",
            "Bean",
            "Cacheable operations with the same names",
            "Configuration (XML)",
            "Model dependencies graph (XML)",
            "Properties",
            "Scheduled tasks",
            "spring.factories registration",
            "Test configuration",
            "Testing beans",
            "Configuration properties",
            "Datasource from .properties file",
            "Datasource from YAML file",
            "Runtime beans",
            "Runtime beans (XML)",
            "Runtime conditions",
            "Spring AOT repository methods",
            "Spring Cloud Stream bindings",
            "Repositories",
            "Run a query in a console",
            "Run MongoDB query in console",
            "Spring Data JDBC mapping",
            "Spring Data MongoDB mapping",
            "Spring Data projections",
            "Message queue receiver methods",
            "Related views",
            "Request mappings"
        };

        for (String item : springItems) {
            CheckBox cb = new CheckBox(item);
            cb.setSelected(true);
            cb.getStyleClass().add("settings-check");
            springBox.getChildren().add(cb);
        }

        // ============================================================
        // Version control ignored directories
        // ============================================================
        Label vcLabel = new Label("Version Control");
        vcLabel.getStyleClass().add("settings-section");

        CheckBox vcIgnored = new CheckBox("Version control ignored directories");
        vcIgnored.setSelected(true);
        vcIgnored.getStyleClass().add("settings-check");

        VBox vcBox = new VBox(4);
        vcBox.setPadding(new Insets(4, 0, 8, 20));
        vcBox.getChildren().add(vcIgnored);

        // ============================================================
        // Assemble all sections
        // ============================================================
        getChildren().addAll(
            showGutterIcons,
            aopLabel, aopBox,
            commonLabel, commonBox,
            jakartaAppLabel, jakartaAppBox,
            cdiLabel, cdiBox,
            jpaLabel, jpaBox,
            jaxrsLabel, jaxrsBox,
            javaLabel, javaBox,
            jsLabel, jsBox,
            aiLabel, aiBox,
            kotlinLabel, kotlinBox,
            k8sLabel, k8sBox,
            mdLabel, mdBox,
            micronautLabel, micronautBox,
            pbLabel, pbBox,
            quarkusLabel, quarkusBox,
            rustLabel, rustBox,
            springLabel, springBox,
            vcLabel, vcBox
        );
    }
}