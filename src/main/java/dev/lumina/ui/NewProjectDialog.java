package dev.lumina.ui;

import dev.lumina.project.ProjectSpec;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

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

    private static final List<GeneratorEntry> NEW_PROJECT_ENTRIES = List.of(
            new GeneratorEntry("Java", ProjectSpec.Generator.JAVA, true),
            new GeneratorEntry("Kotlin", null, false),
            new GeneratorEntry("Groovy", null, false),
            new GeneratorEntry("Rust", null, false),
            new GeneratorEntry("Empty Project", null, false));

    private static final List<GeneratorEntry> GENERATOR_ENTRIES = List.of(
            new GeneratorEntry("Maven Archetype", null, false),
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
            new ComboBox<>(javafx.collections.FXCollections.observableArrayList("25", "21", "17"));
    private final CheckBox mavenWrapperCheck = new CheckBox("Use Maven wrapper");
    private final ComboBox<String> mavenVersionBox = new ComboBox<>(
            javafx.collections.FXCollections.observableArrayList("3.9.5", "3.8.8"));
    private final CheckBox gradleWrapperCheck = new CheckBox("Use Gradle wrapper");
    private final ComboBox<String> gradleVersionBox = new ComboBox<>(
            javafx.collections.FXCollections.observableArrayList("9.2", "9.1", "8.3", "7.6"));
    private final CheckBox saveSettingsCheck = new CheckBox("Use these settings for future projects");
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

        Scene scene = new Scene(root, 800, 640);
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

        // language (Java only in this phase, shown as a segmented control)
        grid.add(formLabel("Language:"), 0, row);
        grid.add(segmented(new ToggleGroup(), true, "Java"), 1, row++);

        HBox buildControl = segmented(buildGroup, true, "Maven", "Gradle");
        grid.add(formLabel("Build system:"), 0, row);
        grid.add(buildControl, 1, row++);

        grid.add(formLabel("Group:"), 0, row);
        grid.add(groupField, 1, row++);

        grid.add(formLabel("Artifact:"), 0, row);
        grid.add(artifactField, 1, row++);

        grid.add(formLabel("Package name:"), 0, row);
        grid.add(packageField, 1, row++);

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

        grid.add(formLabel("Java:"), 0, row);
        javaVersionBox.getSelectionModel().select("21");
        grid.add(javaVersionBox, 1, row++);

        grid.add(formLabel("Build options:"), 0, row);
        advancedBox.setSpacing(10);
        updateAdvancedOptions();
        grid.add(advancedBox, 1, row++);

        grid.add(saveSettingsCheck, 1, row++);

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
            updateHints();
        });
        locationField.textProperty().addListener((obs, old, v) -> updateHints());
        groupField.textProperty().addListener((obs, old, v) -> updateHints());
        artifactField.textProperty().addListener((obs, old, v) -> updateHints());
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

    // --------------------------------------------------------------- logic

    private void updateForGenerator() {
        boolean spring = selected.generator() == ProjectSpec.Generator.SPRING_BOOT;
        if (dependenciesRow != null) {
            dependenciesRow.setVisible(spring);
        }
        javaVersionBox.getSelectionModel().select(spring ? "21" : "25");
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
                    .resolve(nameField.getText()).toString());
        } catch (java.nio.file.InvalidPathException ex) {
            locationHint.setText("Invalid location path");
        }
        if (!packageEdited) {
            String pkg = (sanitize(groupField.getText()) + "." + sanitize(artifactField.getText()))
                    .replaceAll("^\\.|\\.$", "");
            packageField.setText(pkg);
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
        if (artifactField.getText().trim().isEmpty()) {
            errorLabel.setText("Artifact is required.");
            return;
        }

        ToggleButton buildToggle = (ToggleButton) buildGroup.getSelectedToggle();
        ProjectSpec.BuildSystem build = buildToggle != null
                && "Gradle".equals(buildToggle.getText())
                ? ProjectSpec.BuildSystem.GRADLE
                : ProjectSpec.BuildSystem.MAVEN;

        ProjectSpec spec = new ProjectSpec(
                selected.generator(),
                name,
                Path.of(location),
                gitCheck.isSelected(),
                build,
                groupField.getText().trim(),
                artifactField.getText().trim(),
                packageField.getText().trim(),
                javaVersionBox.getValue(),
                dependenciesField.getText().trim());

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
