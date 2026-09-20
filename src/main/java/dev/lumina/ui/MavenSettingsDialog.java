package dev.lumina.ui;

import dev.lumina.project.MavenProjectModel.MavenProject;
import dev.lumina.util.Settings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.nio.file.Path;
import java.util.function.Consumer;

/**
 * IntelliJ IDEA-style Maven Settings dialog (Image 3).
 * Allows viewing/configuring Maven Home, settings.xml, local repo, and default options.
 */
public class MavenSettingsDialog {

    public static final String KEY_MAVEN_HOME = "maven.home.path";
    public static final String KEY_USER_SETTINGS = "maven.user.settings";
    public static final String KEY_LOCAL_REPO = "maven.local.repo";

    private final Stage stage = new Stage();
    private final TextField mavenHomeField = new TextField();
    private final TextField userSettingsField = new TextField();
    private final TextField localRepoField = new TextField();
    private final CheckBox skipTestsCheck = new CheckBox("Skip tests (-DskipTests)");
    private final CheckBox offlineCheck = new CheckBox("Work offline (-o)");

    public MavenSettingsDialog(Stage owner, MavenProject project, Consumer<Path> onOpenPom,
                               boolean currentSkipTests, boolean currentOffline,
                               Consumer<Boolean> onSkipTestsChanged, Consumer<Boolean> onOfflineChanged) {
        if (owner != null) {
            stage.initOwner(owner);
        }
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Maven Settings");

        Label title = new Label("Maven Settings");
        title.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 14px; -fx-font-weight: bold;");

        // Defaults
        String defaultMavenHome = System.getenv("M2_HOME");
        if (defaultMavenHome == null || defaultMavenHome.isBlank()) {
            defaultMavenHome = System.getenv("MAVEN_HOME");
        }
        if (defaultMavenHome == null || defaultMavenHome.isBlank()) {
            defaultMavenHome = "/usr/share/maven";
        }
        String defaultUserSettings = System.getProperty("user.home") + "/.m2/settings.xml";
        String defaultLocalRepo = System.getProperty("user.home") + "/.m2/repository";

        mavenHomeField.setText(Settings.get(KEY_MAVEN_HOME) != null ? Settings.get(KEY_MAVEN_HOME) : defaultMavenHome);
        userSettingsField.setText(Settings.get(KEY_USER_SETTINGS) != null ? Settings.get(KEY_USER_SETTINGS) : defaultUserSettings);
        localRepoField.setText(Settings.get(KEY_LOCAL_REPO) != null ? Settings.get(KEY_LOCAL_REPO) : defaultLocalRepo);

        styleField(mavenHomeField);
        styleField(userSettingsField);
        styleField(localRepoField);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10, 0, 10, 0));

        ColumnConstraints col1 = new ColumnConstraints(110);
        ColumnConstraints col2 = new ColumnConstraints(300, 360, Double.MAX_VALUE);
        col2.setHgrow(Priority.ALWAYS);
        ColumnConstraints col3 = new ColumnConstraints(70);
        grid.getColumnConstraints().addAll(col1, col2, col3);

        Button browseHome = new Button("Browse…");
        browseHome.setOnAction(e -> {
            DirectoryChooser dc = new DirectoryChooser();
            dc.setTitle("Select Maven Home Directory");
            File chosen = dc.showDialog(stage);
            if (chosen != null) mavenHomeField.setText(chosen.getAbsolutePath());
        });

        Button browseSettings = new Button("Browse…");
        browseSettings.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Select settings.xml");
            File chosen = fc.showOpenDialog(stage);
            if (chosen != null) userSettingsField.setText(chosen.getAbsolutePath());
        });

        Button browseRepo = new Button("Browse…");
        browseRepo.setOnAction(e -> {
            DirectoryChooser dc = new DirectoryChooser();
            dc.setTitle("Select Local Repository Directory");
            File chosen = dc.showDialog(stage);
            if (chosen != null) localRepoField.setText(chosen.getAbsolutePath());
        });

        addRow(grid, 0, "Maven home:", mavenHomeField, browseHome);
        addRow(grid, 1, "User settings:", userSettingsField, browseSettings);
        addRow(grid, 2, "Local repo:", localRepoField, browseRepo);

        skipTestsCheck.setSelected(currentSkipTests);
        skipTestsCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        offlineCheck.setSelected(currentOffline);
        offlineCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");

        HBox optionsBox = new HBox(16, skipTestsCheck, offlineCheck);
        optionsBox.setPadding(new Insets(4, 0, 8, 0));

        Separator sep = new Separator();

        Button openPomBtn = new Button("Open pom.xml in Editor");
        if (project == null || project.getPomPath() == null) {
            openPomBtn.setDisable(true);
        } else {
            openPomBtn.setOnAction(e -> {
                if (onOpenPom != null) onOpenPom.accept(project.getPomPath());
                stage.close();
            });
        }

        Button okBtn = new Button("OK");
        okBtn.setDefaultButton(true);
        okBtn.setOnAction(e -> {
            save();
            if (onSkipTestsChanged != null) onSkipTestsChanged.accept(skipTestsCheck.isSelected());
            if (onOfflineChanged != null) onOfflineChanged.accept(offlineCheck.isSelected());
            stage.close();
        });

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setCancelButton(true);
        cancelBtn.setOnAction(e -> stage.close());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox buttonBar = new HBox(8, openPomBtn, spacer, cancelBtn, okBtn);
        buttonBar.setAlignment(Pos.CENTER_RIGHT);
        buttonBar.setPadding(new Insets(8, 0, 0, 0));

        VBox root = new VBox(12, title, grid, optionsBox, sep, buttonBar);
        root.setPadding(new Insets(16, 20, 16, 20));
        root.setStyle("-fx-background-color: #1E1F22; -fx-border-color: #393B40; -fx-border-width: 1;");

        Scene scene = new Scene(root, 580, 280);
        scene.getStylesheets().add(getClass().getResource("/css/lumina-dark.css").toExternalForm());
        scene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) stage.close();
        });

        stage.setScene(scene);
    }

    private void styleField(TextField tf) {
        tf.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4;");
    }

    private void addRow(GridPane grid, int row, String labelText, TextField tf, Button btn) {
        Label lbl = new Label(labelText);
        lbl.setStyle("-fx-text-fill: #BCBEC4; -fx-font-size: 12px;");
        grid.add(lbl, 0, row);
        grid.add(tf, 1, row);
        grid.add(btn, 2, row);
    }

    private void save() {
        Settings.put(KEY_MAVEN_HOME, mavenHomeField.getText().trim());
        Settings.put(KEY_USER_SETTINGS, userSettingsField.getText().trim());
        Settings.put(KEY_LOCAL_REPO, localRepoField.getText().trim());
    }

    public void show() {
        stage.centerOnScreen();
        stage.showAndWait();
    }
}
