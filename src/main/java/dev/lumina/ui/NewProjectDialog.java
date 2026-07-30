package dev.lumina.ui;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import dev.lumina.project.ProjectSpec;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
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

    private record JdkEntry(String label, String detail, boolean enabled, boolean action) {
        @Override
        public String toString() {
            return label;
        }
    }

    private record CatalogEntry(String name, String type, String location) {
    }

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
            new GeneratorEntry("Kotlin", null, false),
            new GeneratorEntry("Groovy", null, false),
            new GeneratorEntry("Rust", null, false),
            new GeneratorEntry("Empty Project", null, false));

    private static final List<GeneratorEntry> GENERATOR_ENTRIES = List.of(
            new GeneratorEntry("Maven Archetype", ProjectSpec.Generator.MAVEN_ARCHETYPE, true),
            new GeneratorEntry("Spring Boot", ProjectSpec.Generator.SPRING_BOOT, true),
            new GeneratorEntry("JavaFX", null, false),
            new GeneratorEntry("Quarkus", null, false),
            new GeneratorEntry("Micronaut", null, false),
            new GeneratorEntry("Jakarta EE", null, false),
            new GeneratorEntry("Ktor", null, false),
            new GeneratorEntry("HTML", null, false),
            new GeneratorEntry("React", null, false),
            new GeneratorEntry("Express", null, false),
            new GeneratorEntry("Angular CLI", null, false),
            new GeneratorEntry("Vue.js", null, false),
            new GeneratorEntry("Vite", null, false),
            new GeneratorEntry("Nuxt", null, false));

    private static final List<JdkEntry> JDK_ENTRIES = List.of(
            new JdkEntry("Registered JDKs", "", false, false),
            new JdkEntry("Oracle OpenJDK 25.0.2", "Registered", true, false),
            new JdkEntry("Alibaba Dragonwell 21.0.10", "Registered", true, false),
            new JdkEntry("dragonwell-ex-21 Alibaba Dragonwell 21.0.10", "Registered", true, false),
            new JdkEntry("Download JDK...", "", true, true),
            new JdkEntry("Add JDK from Disk...", "", true, true),
            new JdkEntry("Detected JDKs", "", false, false),
            new JdkEntry("Oracle OpenJDK 25.0.2", "/usr/lib/jvm/jdk-25.0.2-oracle-x64", true, false));

    private final Stage stage = new Stage();
    private final Consumer<ProjectSpec> onCreate;

    // form controls
    private final TextField nameField = new TextField("demo");
    private final TextField locationField = new TextField(
            System.getProperty("user.home") + File.separator + "Development");
    private final Label locationHint = new Label();
    private final CheckBox gitCheck = new CheckBox("Create Git repository");
    private final ToggleGroup buildGroup = new ToggleGroup();
    private final ComboBox<JdkEntry> jdkCombo = new ComboBox<>();
    private final TextField groupField = new TextField("com.example");
    private final TextField artifactField = new TextField("demo");
    private final TextField packageField = new TextField("com.example.demo");
    private final ComboBox<String> javaVersionBox =
            new ComboBox<>(FXCollections.observableArrayList("25", "21", "17"));
    private final CheckBox mavenWrapperCheck = new CheckBox("Use Maven wrapper");
    private final ComboBox<String> mavenVersionBox = new ComboBox<>(
            FXCollections.observableArrayList("3.9.5", "3.8.8"));
    private final CheckBox gradleWrapperCheck = new CheckBox("Use Gradle wrapper");
    private final ComboBox<String> gradleVersionBox = new ComboBox<>(
            FXCollections.observableArrayList("9.2", "9.1", "8.3", "7.6"));
    private final CheckBox saveSettingsCheck = new CheckBox("Use these settings for future projects");
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
    private final List<Node> standardOnlyNodes = new ArrayList<>();
    private final List<Node> springOnlyNodes = new ArrayList<>();

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

    private final TextField dependenciesField = new TextField("web");
    private final Label errorLabel = new Label();
    private final VBox advancedBox = new VBox(10);

    private HBox dependenciesRow;
    private boolean packageEdited;
    private GeneratorEntry selected = NEW_PROJECT_ENTRIES.get(0);
    private JdkEntry selectedJdk = JDK_ENTRIES.get(1);

    public NewProjectDialog(Stage owner, Consumer<ProjectSpec> onCreate) {
        this.onCreate = onCreate;

        stage.initOwner(owner);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("New Project");

        BorderPane root = new BorderPane();
        root.getStyleClass().addAll("app-root", "new-project-dialog");
        root.setLeft(buildGeneratorList());
        root.setCenter(buildForm());
        root.setBottom(buildButtons());

        Scene scene = new Scene(root, 960, 760);
        scene.getStylesheets().add(
                getClass().getResource("/css/lumina-dark.css").toExternalForm());
        stage.setScene(scene);
    }

    public void show() {
        updateForGenerator();
        stage.showAndWait();
    }

    // -------------------------------------------------------- generator list

    private VBox buildGeneratorList() {
        Label newProjectHeader = new Label("New Project");
        newProjectHeader.getStyleClass().add("panel-header");
        newProjectHeader.setPadding(new Insets(14, 16, 8, 16));

        ListView<GeneratorEntry> newProjectList = new ListView<>(
                javafx.collections.FXCollections.observableArrayList(NEW_PROJECT_ENTRIES));
        newProjectList.getStyleClass().addAll("generator-list", "project-list");
        newProjectList.setCellFactory(this::createGeneratorCell);

        Label generatorsHeader = new Label("Generators");
        generatorsHeader.getStyleClass().add("panel-header");
        generatorsHeader.setPadding(new Insets(14, 16, 8, 16));

        ListView<GeneratorEntry> generatorList = new ListView<>(
                javafx.collections.FXCollections.observableArrayList(GENERATOR_ENTRIES));
        generatorList.getStyleClass().addAll("generator-list", "project-list");
        generatorList.setCellFactory(this::createGeneratorCell);

        newProjectList.getSelectionModel().selectedItemProperty().addListener((obs, old, entry) -> {
            if (entry != null) {
                selected = entry;
                generatorList.getSelectionModel().clearSelection();
                updateForGenerator();
            }
        });
        generatorList.getSelectionModel().selectedItemProperty().addListener((obs, old, entry) -> {
            if (entry != null) {
                selected = entry;
                newProjectList.getSelectionModel().clearSelection();
                updateForGenerator();
            }
        });

        Platform.runLater(() -> newProjectList.getSelectionModel().select(0));
        VBox.setVgrow(newProjectList, Priority.ALWAYS);
        VBox.setVgrow(generatorList, Priority.ALWAYS);

        Button plugins = new Button("More via plugins...");
        plugins.getStyleClass().add("plugin-link");
        plugins.setMaxWidth(Double.MAX_VALUE);
        plugins.setOnAction(e -> showPluginManager());

        VBox box = new VBox(newProjectHeader, newProjectList,
                generatorsHeader, generatorList, plugins);
        box.getStyleClass().add("generator-panel");
        box.setPrefWidth(240);
        box.setSpacing(8);
        box.setPadding(new javafx.geometry.Insets(0, 12, 12, 12));
        return box;
    }

    private ListCell<GeneratorEntry> createGeneratorCell(ListView<GeneratorEntry> view) {
        return new ListCell<>() {
            @Override
            protected void updateItem(GeneratorEntry item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    getStyleClass().remove("generator-disabled");
                    return;
                }
                setText((item.enabled() ? "" : "\u25CB ") + item.label()
                        + (item.enabled() ? "" : "  (soon)"));
                if (!item.enabled() && !getStyleClass().contains("generator-disabled")) {
                    getStyleClass().add("generator-disabled");
                } else if (item.enabled()) {
                    getStyleClass().remove("generator-disabled");
                }
            }
        };
    }

    // ------------------------------------------------------------------ form

    private ScrollPane buildForm() {
        GridPane grid = new GridPane();
        grid.setHgap(14);
        grid.setVgap(14);
        grid.setPadding(new Insets(24, 28, 12, 28));

        ColumnConstraints c0 = new ColumnConstraints();
        c0.setMinWidth(110);
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(c0, c1);

        int row = 0;

        grid.add(formLabel("Name:"), 0, row);
        grid.add(nameField, 1, row++);

        Button browse = new Button("\uD83D\uDCC2");
        browse.getStyleClass().add("console-button");
        browse.setOnAction(e -> {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("Project Location");
            File current = new File(locationField.getText());
            if (current.isDirectory()) chooser.setInitialDirectory(current);
            File dir = chooser.showDialog(stage);
            if (dir != null) locationField.setText(dir.getAbsolutePath());
        });
        HBox locationRow = new HBox(8, locationField, browse);
        HBox.setHgrow(locationField, Priority.ALWAYS);
        grid.add(formLabel("Location:"), 0, row);
        grid.add(locationRow, 1, row++);

        locationHint.getStyleClass().add("form-hint");
        grid.add(locationHint, 1, row++);
        grid.add(gitCheck, 1, row++);

        serverUrlLabel.getStyleClass().add("form-static");
        serverSettingsButton.getStyleClass().add("console-button");
        serverSettingsButton.setOnAction(e -> showServerSettings());
        serverRow.getChildren().setAll(serverUrlLabel, serverSettingsButton);
        serverRow.setAlignment(Pos.CENTER_LEFT);
        Label serverLabel = formLabel("Server URL:");
        grid.add(serverLabel, 0, row);
        grid.add(serverRow, 1, row++);
        springOnlyNodes.addAll(List.of(serverLabel, serverRow));

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
        Label languageLabel = formLabel("Language:");
        grid.add(languageLabel, 0, row);
        grid.add(languageRow, 1, row++);
        springOnlyNodes.addAll(List.of(languageLabel, languageRow));

        typeGroup.getToggles().addAll(typeGradleGroovy, typeGradleKotlin, typeMaven);
        typeGradleGroovy.setToggleGroup(typeGroup);
        typeGradleKotlin.setToggleGroup(typeGroup);
        typeMaven.setToggleGroup(typeGroup);
        typeGradleGroovy.getStyleClass().addAll("segment", "segment-first");
        typeGradleKotlin.getStyleClass().addAll("segment");
        typeMaven.getStyleClass().addAll("segment", "segment-last");
        typeMaven.setSelected(true);
        typeRow.getChildren().setAll(typeGradleGroovy, typeGradleKotlin, typeMaven);
        typeRow.getStyleClass().add("segmented");
        Label typeLabel = formLabel("Type:");
        grid.add(typeLabel, 0, row);
        grid.add(typeRow, 1, row++);
        springOnlyNodes.addAll(List.of(typeLabel, typeRow));

        packagingGroup.getToggles().addAll(packagingJar, packagingWar);
        packagingJar.setToggleGroup(packagingGroup);
        packagingWar.setToggleGroup(packagingGroup);
        packagingJar.getStyleClass().addAll("segment", "segment-first");
        packagingWar.getStyleClass().addAll("segment", "segment-last");
        packagingJar.setSelected(true);
        packagingRow.getChildren().setAll(packagingJar, packagingWar);
        packagingRow.getStyleClass().add("segmented");
        Label packagingLabel = formLabel("Packaging:");
        grid.add(packagingLabel, 0, row);
        grid.add(packagingRow, 1, row++);
        springOnlyNodes.addAll(List.of(packagingLabel, packagingRow));

        configGroup.getToggles().addAll(configProperties, configYaml);
        configProperties.setToggleGroup(configGroup);
        configYaml.setToggleGroup(configGroup);
        configProperties.getStyleClass().addAll("segment", "segment-first");
        configYaml.getStyleClass().addAll("segment", "segment-last");
        configProperties.setSelected(true);
        configRow.getChildren().setAll(configProperties, configYaml);
        configRow.getStyleClass().add("segmented");
        Label configLabel = formLabel("Configuration:");
        grid.add(configLabel, 0, row);
        grid.add(configRow, 1, row++);
        springOnlyNodes.addAll(List.of(configLabel, configRow));

        setNodesVisible(springOnlyNodes, false);

        buildSystemRow.getChildren().setAll(segmented(buildGroup, true, "Maven", "Gradle"));
        Label buildSystemLabel = formLabel("Build system:");
        grid.add(buildSystemLabel, 0, row);
        grid.add(buildSystemRow, 1, row++);
        standardOnlyNodes.addAll(List.of(buildSystemLabel, buildSystemRow));

        Label groupLabel = formLabel("Group:");
        grid.add(groupLabel, 0, row);
        grid.add(groupField, 1, row++);
        standardOnlyNodes.addAll(List.of(groupLabel, groupField));

        Label artifactLabel = formLabel("Artifact:");
        grid.add(artifactLabel, 0, row);
        grid.add(artifactField, 1, row++);
        standardOnlyNodes.addAll(List.of(artifactLabel, artifactField));

        Label packageLabel = formLabel("Package name:");
        grid.add(packageLabel, 0, row);
        grid.add(packageField, 1, row++);
        standardOnlyNodes.addAll(List.of(packageLabel, packageField));

        grid.add(formLabel("JDK:"), 0, row);
        jdkCombo.setItems(javafx.collections.FXCollections.observableArrayList(JDK_ENTRIES));
        jdkCombo.setCellFactory(listView -> createJdkCell());
        jdkCombo.setButtonCell(createJdkButtonCell());
        if (selectedJdk != null) {
            jdkCombo.getSelectionModel().select(selectedJdk);
        }
        jdkCombo.getSelectionModel().selectedItemProperty().addListener((obs, old, entry) -> {
            if (entry == null || entry == selectedJdk) return;
            if (!entry.enabled()) {
                jdkCombo.getSelectionModel().select(selectedJdk);
                return;
            }
            if (entry.action()) {
                handleJdkAction(entry);
                jdkCombo.getSelectionModel().select(selectedJdk);
                return;
            }
            selectedJdk = entry;
        });
        grid.add(jdkCombo, 1, row++);

        buildMavenArchetypeForm();
        grid.add(mavenArchetypeBox, 0, row++, 2, 1);

        Label javaLabel = formLabel("Java:");
        grid.add(javaLabel, 0, row);
        javaVersionBox.getSelectionModel().select("21");
        grid.add(javaVersionBox, 1, row++);
        standardOnlyNodes.addAll(List.of(javaLabel, javaVersionBox));

        Label buildOptionsLabel = formLabel("Build options:");
        grid.add(buildOptionsLabel, 0, row);
        advancedBox.setSpacing(10);
        updateAdvancedOptions();
        grid.add(advancedBox, 1, row++);
        standardOnlyNodes.addAll(List.of(buildOptionsLabel, advancedBox));

        grid.add(saveSettingsCheck, 1, row++);
        standardOnlyNodes.add(saveSettingsCheck);

        Label depsLabel = formLabel("Dependencies:");
        dependenciesField.setPromptText("comma separated, e.g. web,data-jpa,lombok");
        dependenciesRow = new HBox(dependenciesField);
        HBox.setHgrow(dependenciesField, Priority.ALWAYS);
        grid.add(depsLabel, 0, row);
        grid.add(dependenciesRow, 1, row++);
        dependenciesRow.visibleProperty().addListener((obs, old, v) -> {
            depsLabel.setVisible(v);
            depsLabel.setManaged(v);
            dependenciesRow.setManaged(v);
        });

        buildGroup.selectedToggleProperty().addListener((obs, old, n) -> updateAdvancedOptions());

        // live bindings
        nameField.textProperty().addListener((obs, old, v) -> {
            artifactField.setText(sanitize(v));
            mavenArtifactField.setText(sanitize(v));
            updateHints();
        });
        locationField.textProperty().addListener((obs, old, v) -> updateHints());
        groupField.textProperty().addListener((obs, old, v) -> updateHints());
        artifactField.textProperty().addListener((obs, old, v) -> updateHints());
        mavenArtifactField.textProperty().addListener((obs, old, v) -> updateHints());
        packageField.setOnKeyTyped(e -> packageEdited = true);
        updateHints();

        ScrollPane scroll = new ScrollPane(grid);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("form-scroll");
        return scroll;
    }

    private HBox buildButtons() {
        errorLabel.getStyleClass().add("form-error");

        Button cancel = new Button("Cancel");
        cancel.getStyleClass().add("dialog-secondary");
        cancel.setOnAction(e -> stage.close());

        Button create = new Button("Create");
        create.getStyleClass().add("dialog-primary");
        create.setDefaultButton(true);
        create.setOnAction(e -> tryCreate());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox box = new HBox(10, errorLabel, spacer, cancel, create);
        box.setAlignment(Pos.CENTER_RIGHT);
        box.setPadding(new Insets(12, 20, 14, 20));
        box.getStyleClass().add("dialog-footer");
        return box;
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

        fields.add(formLabel("Catalog:  ⓘ"), 0, 0);
        fields.add(catalog, 1, 0);
        fields.add(formLabel("Archetype:  ⓘ"), 0, 1);
        fields.add(archetype, 1, 1);
        fields.add(formLabel("Version:"), 0, 2);
        fields.add(archetypeVersionBox, 1, 2);

        configurePropertiesTable();
        Button addProperty = new Button("+");
        Button removeProperty = new Button("−");
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

        Label advancedTitle = new Label("⌄  Advanced Settings");
        advancedTitle.getStyleClass().add("maven-section-title");
        GridPane advanced = new GridPane();
        advanced.setHgap(12);
        advanced.setVgap(10);
        advanced.getColumnConstraints().addAll(new ColumnConstraints(96), new ColumnConstraints());
        advanced.add(formLabel("GroupId:  ⓘ"), 0, 0);
        advanced.add(mavenGroupField, 1, 0);
        advanced.add(formLabel("ArtifactId:  ⓘ"), 0, 1);
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
        boolean spring = selected.generator() == ProjectSpec.Generator.SPRING_BOOT;
        boolean mavenArchetype = selected.generator() == ProjectSpec.Generator.MAVEN_ARCHETYPE;
        if (dependenciesRow != null) {
            dependenciesRow.setVisible(spring);
            dependenciesRow.setManaged(spring);
        }
        setNodesVisible(springOnlyNodes, spring);
        setNodesVisible(standardOnlyNodes, !mavenArchetype);
        mavenArchetypeBox.setVisible(mavenArchetype);
        mavenArchetypeBox.setManaged(mavenArchetype);

        if (!mavenArchetype) javaVersionBox.getSelectionModel().select(spring ? "21" : "25");
        errorLabel.setText(selected.enabled() ? ""
                : selected.label() + " support arrives in a later phase.");
        updateAdvancedOptions();
    }

    private void updateAdvancedOptions() {
        advancedBox.getChildren().clear();
        ToggleButton buildToggle = (ToggleButton) buildGroup.getSelectedToggle();
        boolean gradle = buildToggle != null && "Gradle".equals(buildToggle.getText());

        if (gradle) {
            HBox wrapperRow = new HBox(12, gradleWrapperCheck, gradleVersionBox);
            wrapperRow.setAlignment(Pos.CENTER_LEFT);
            gradleWrapperCheck.setSelected(true);
            advancedBox.getChildren().addAll(new Label("Gradle wrapper:"), wrapperRow);
        } else {
            HBox wrapperRow = new HBox(12, mavenWrapperCheck, mavenVersionBox);
            wrapperRow.setAlignment(Pos.CENTER_LEFT);
            mavenWrapperCheck.setSelected(true);
            advancedBox.getChildren().addAll(new Label("Maven wrapper:"), wrapperRow);
        }
    }

    private void showPluginManager() {
        Stage dialog = new Stage();
        dialog.initOwner(stage);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Install Plugin");

        BorderPane root = new BorderPane();
        root.getStyleClass().addAll("app-root", "new-project-dialog");

        Label header = new Label("Install Plugin");
        header.getStyleClass().add("panel-header");
        header.setPadding(new Insets(14, 16, 8, 16));
        root.setTop(header);

        ListView<String> list = new ListView<>(
                javafx.collections.FXCollections.observableArrayList(
                        "Go", "PHP", "Python", "Plugin DevKit", "Ruby", "Scala"));
        list.getSelectionModel().select(3);
        list.setStyle("-fx-background-color: #14161E;");
        root.setCenter(list);

        Button install = new Button("Install");
        install.getStyleClass().add("dialog-primary");
        install.setOnAction(e -> {
            String plugin = list.getSelectionModel().getSelectedItem();
            Alert info = new Alert(Alert.AlertType.INFORMATION);
            info.initOwner(dialog);
            info.setTitle("Install Plugin");
            info.setHeaderText(plugin + " installation");
            info.setContentText("Plugin installation is not available yet.");
            info.getDialogPane().getStylesheets().add(
                    getClass().getResource("/css/lumina-dark.css").toExternalForm());
            info.showAndWait();
        });

        Button manage = new Button("Manage plugins...");
        manage.getStyleClass().add("dialog-secondary");
        manage.setOnAction(e -> {
            Alert info = new Alert(Alert.AlertType.INFORMATION);
            info.initOwner(dialog);
            info.setTitle("Manage plugins");
            info.setHeaderText("Plugin manager coming soon");
            info.setContentText("Future versions will let you install and manage plugins "
                    + "from a marketplace.");
            info.getDialogPane().getStylesheets().add(
                    getClass().getResource("/css/lumina-dark.css").toExternalForm());
            info.showAndWait();
        });

        Button close = new Button("Close");
        close.getStyleClass().add("dialog-secondary");
        close.setOnAction(e -> dialog.close());

        HBox footer = new HBox(10, install, manage, close);
        footer.setPadding(new Insets(12, 16, 18, 16));
        footer.setAlignment(Pos.CENTER_RIGHT);
        root.setBottom(footer);

        Scene scene = new Scene(root, 360, 420);
        scene.getStylesheets().add(getClass().getResource("/css/lumina-dark.css").toExternalForm());
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    private void updateHints() {
        try {
            locationHint.setText("Project will be created in: "
                    + Path.of(locationField.getText().isBlank() ? "." : locationField.getText())
                    .resolve(selected.generator() == ProjectSpec.Generator.MAVEN_ARCHETYPE
                            ? mavenArtifactField.getText() : nameField.getText()).toString());
        } catch (java.nio.file.InvalidPathException ex) {
            locationHint.setText("Invalid location path");
        }
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

    private static void setNodesVisible(List<Node> nodes, boolean visible) {
        for (Node node : nodes) {
            node.setVisible(visible);
            node.setManaged(visible);
        }
    }

    private ListCell<JdkEntry> createJdkCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(JdkEntry item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                if (!item.enabled()) {
                    setText(item.label());
                    setStyle("-fx-text-fill: #565D75;");
                    return;
                }
                if (item.action()) {
                    setText(item.label());
                    setStyle("-fx-text-fill: #8FCE8F;");
                    return;
                }
                setText(item.label() + (item.detail().isBlank() ? "" : "  " + item.detail()));
                setStyle("-fx-text-fill: #D8DBE6;");
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
                setText(item.label());
            }
        };
    }

    private void handleJdkAction(JdkEntry entry) {
        if (entry.label().startsWith("Download JDK")) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.initOwner(stage);
            alert.setTitle("Download JDK");
            alert.setHeaderText("Download JDK");
            alert.setContentText("Downloading JDK is not implemented yet.");
            alert.getDialogPane().getStylesheets().add(
                    getClass().getResource("/css/lumina-dark.css").toExternalForm());
            alert.showAndWait();
        } else if (entry.label().startsWith("Add JDK")) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.initOwner(stage);
            alert.setTitle("Add JDK");
            alert.setHeaderText("Add JDK from Disk");
            alert.setContentText("Adding a JDK from disk is not implemented yet.");
            alert.getDialogPane().getStylesheets().add(
                    getClass().getResource("/css/lumina-dark.css").toExternalForm());
            alert.showAndWait();
        }
    }

    private void showServerSettings() {
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
        String artifact = (mavenArchetype ? mavenArtifactField : artifactField).getText().trim();
        if (artifact.isEmpty()) {
            errorLabel.setText("Artifact is required.");
            return;
        }

        ProjectSpec.Language language = ProjectSpec.Language.JAVA;
        if (langKotlin.isSelected()) language = ProjectSpec.Language.KOTLIN;
        else if (langGroovy.isSelected()) language = ProjectSpec.Language.GROOVY;

        ProjectSpec.BuildSystem build;
        if (mavenArchetype) {
            build = ProjectSpec.BuildSystem.MAVEN;
        } else if (selected.generator() == ProjectSpec.Generator.SPRING_BOOT) {
            if (typeGradleGroovy.isSelected() || typeGradleKotlin.isSelected()) {
                build = ProjectSpec.BuildSystem.GRADLE;
            } else {
                build = ProjectSpec.BuildSystem.MAVEN;
            }
        } else {
            ToggleButton buildToggle = (ToggleButton) buildGroup.getSelectedToggle();
            build = buildToggle != null && "Gradle".equals(buildToggle.getText())
                    ? ProjectSpec.BuildSystem.GRADLE
                    : ProjectSpec.BuildSystem.MAVEN;
        }

        ProjectSpec.Packaging packaging = packagingJar.isSelected()
                ? ProjectSpec.Packaging.JAR : ProjectSpec.Packaging.WAR;
        ProjectSpec.ConfigFormat configFormat = configYaml.isSelected()
                ? ProjectSpec.ConfigFormat.YAML : ProjectSpec.ConfigFormat.PROPERTIES;

        ProjectSpec spec = new ProjectSpec(
                selected.generator(),
                name,
                Path.of(location),
                gitCheck.isSelected(),
                build,
                language,
                packaging,
                configFormat,
                (mavenArchetype ? mavenGroupField : groupField).getText().trim(),
                artifact,
                mavenArchetype
                        ? (sanitize(mavenGroupField.getText()) + "." + sanitize(artifact))
                                .replaceAll("^\\.|\\.$", "")
                        : packageField.getText().trim(),
                javaVersionBox.getValue(),
                dependenciesField.getText().trim(),
                catalogCombo.getValue(),
                selectedArchetype(),
                archetypeVersionBox.getValue(),
                projectVersionField.getText().trim(),
                additionalProperties.stream()
                        .filter(property -> !property.getName().isBlank())
                        .map(property -> property.getName().trim() + "=" + property.getValue().trim())
                        .collect(java.util.stream.Collectors.joining(",")));

        stage.close();
        onCreate.accept(spec);
    }

    // -------------------------------------------------------------- helpers

    private Label formLabel(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("form-label");
        return l;
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

    private static String sanitize(String s) {
        return s == null ? "" : s.toLowerCase().replaceAll("[^a-z0-9.\\-]", "");
    }
}
