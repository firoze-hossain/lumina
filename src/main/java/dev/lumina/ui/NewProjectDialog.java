package dev.lumina.ui;

import dev.lumina.project.ProjectSpec;
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
import java.util.List;
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

    private static final List<GeneratorEntry> GENERATORS = List.of(
            new GeneratorEntry("Java", ProjectSpec.Generator.JAVA, true),
            new GeneratorEntry("Spring Boot", ProjectSpec.Generator.SPRING_BOOT, true),
            new GeneratorEntry("Maven Archetype", null, false),
            new GeneratorEntry("JavaFX", null, false),
            new GeneratorEntry("Quarkus", null, false),
            new GeneratorEntry("Micronaut", null, false),
            new GeneratorEntry("Jakarta EE", null, false));

    private final Stage stage = new Stage();
    private final Consumer<ProjectSpec> onCreate;

    // form controls
    private final TextField nameField = new TextField("demo");
    private final TextField locationField = new TextField(
            System.getProperty("user.home") + File.separator + "Development");
    private final Label locationHint = new Label();
    private final CheckBox gitCheck = new CheckBox("Create Git repository");
    private final ToggleGroup buildGroup = new ToggleGroup();
    private final TextField groupField = new TextField("com.example");
    private final TextField artifactField = new TextField("demo");
    private final TextField packageField = new TextField("com.example.demo");
    private final ComboBox<String> javaVersionBox =
            new ComboBox<>(javafx.collections.FXCollections.observableArrayList("25", "21", "17"));
    private final TextField dependenciesField = new TextField("web");
    private final Label errorLabel = new Label();

    private HBox dependenciesRow;
    private boolean packageEdited;
    private GeneratorEntry selected = GENERATORS.get(0);

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
        Label header = new Label("Generators");
        header.getStyleClass().add("panel-header");
        header.setPadding(new Insets(14, 16, 8, 16));

        ListView<GeneratorEntry> list = new ListView<>(
                javafx.collections.FXCollections.observableArrayList(GENERATORS));
        list.getStyleClass().add("generator-list");
        list.setCellFactory(lv -> new ListCell<>() {
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
        });
        list.getSelectionModel().select(0);
        list.getSelectionModel().selectedItemProperty().addListener((obs, old, entry) -> {
            if (entry != null) {
                selected = entry;
                updateForGenerator();
            }
        });
        VBox.setVgrow(list, Priority.ALWAYS);

        VBox box = new VBox(header, list);
        box.getStyleClass().add("generator-panel");
        box.setPrefWidth(210);
        return box;
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

        grid.add(formLabel("Build system:"), 0, row);
        grid.add(segmented(buildGroup, true, "Maven", "Gradle"), 1, row++);

        grid.add(formLabel("Group:"), 0, row);
        grid.add(groupField, 1, row++);

        grid.add(formLabel("Artifact:"), 0, row);
        grid.add(artifactField, 1, row++);

        grid.add(formLabel("Package name:"), 0, row);
        grid.add(packageField, 1, row++);

        grid.add(formLabel("JDK:"), 0, row);
        Label jdkLabel = new Label("\u2615 " + System.getProperty("java.version")
                + " \u2014 " + System.getProperty("java.vendor"));
        jdkLabel.getStyleClass().add("form-static");
        grid.add(jdkLabel, 1, row++);

        grid.add(formLabel("Java:"), 0, row);
        javaVersionBox.getSelectionModel().select("21");
        grid.add(javaVersionBox, 1, row++);

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
        dependenciesRow.setVisible(spring);
        javaVersionBox.getSelectionModel().select(spring ? "21" : "25");
        errorLabel.setText(selected.enabled() ? ""
                : selected.label() + " support arrives in a later phase.");
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
