package dev.lumina.ui;

import dev.lumina.project.MavenProjectModel.MavenProject;
import dev.lumina.project.MavenProjectModel.PluginItem;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * IntelliJ IDEA-exact "Run Anything" popup dialog for Maven (Image 4).
 * Prefills "mvn ", displays "Maven Goals" section header, and provides real-time
 * filtered suggestions of all lifecycle and plugin goals.
 */
public class RunAnythingDialog {

    private final Stage stage = new Stage();
    private final TextField queryField = new TextField();
    private final ObservableList<String> visibleGoals = FXCollections.observableArrayList();
    private final ListView<String> goalsList = new ListView<>(visibleGoals);
    private final List<String> allGoals = new ArrayList<>();
    private final Consumer<String> onRunGoal;

    public RunAnythingDialog(Stage owner, MavenProject project, Consumer<String> onRunGoal) {
        this.onRunGoal = onRunGoal;

        if (owner != null) {
            stage.initOwner(owner);
        }
        stage.initModality(Modality.NONE);
        stage.initStyle(StageStyle.UNDECORATED);

        // Gather all goals dynamically (Lifecycle + Plugins)
        populateGoals(project);

        // 1. Top Header Bar: "Run Anything" and "Project ▾  \"
        Label titleLabel = new Label("Run Anything");
        titleLabel.getStyleClass().add("run-anything-title");

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);

        String projectName = (project != null && project.getName() != null) ? project.getName() : "Project";
        Label scopeLabel = new Label(projectName + " ▾  \\");
        scopeLabel.getStyleClass().add("run-anything-scope");

        HBox headerBar = new HBox(8, titleLabel, headerSpacer, scopeLabel);
        headerBar.setAlignment(Pos.CENTER_LEFT);
        headerBar.setPadding(new Insets(10, 14, 8, 14));

        // 2. Search Box with blue italic 'm' icon and prefilled "mvn "
        Node mavenIcon = createMavenGlyph();
        queryField.setText("mvn ");
        queryField.getStyleClass().add("run-anything-input");
        HBox.setHgrow(queryField, Priority.ALWAYS);

        HBox inputBox = new HBox(8, mavenIcon, queryField);
        inputBox.setAlignment(Pos.CENTER_LEFT);
        inputBox.getStyleClass().add("run-anything-input-box");
        inputBox.setPadding(new Insets(4, 10, 4, 10));

        VBox searchArea = new VBox(6, inputBox);
        searchArea.setPadding(new Insets(2, 14, 8, 14));

        // 3. Section Header: "Maven Goals"
        Label sectionHeader = new Label("Maven Goals");
        sectionHeader.getStyleClass().add("run-anything-section-header");
        sectionHeader.setPadding(new Insets(6, 14, 4, 14));

        Separator separator = new Separator();
        separator.getStyleClass().add("run-anything-separator");

        // 4. Goals Suggestions List
        goalsList.getStyleClass().add("run-anything-list");
        goalsList.setPrefHeight(300);
        goalsList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Node mIcon = createMavenGlyph();
                    Label textLabel = new Label("mvn " + item);
                    textLabel.getStyleClass().add("run-anything-goal-text");
                    HBox row = new HBox(10, mIcon, textLabel);
                    row.setAlignment(Pos.CENTER_LEFT);
                    setGraphic(row);
                    setText(null);
                }
            }
        });

        Runnable executeSelected = () -> {
            String selected = goalsList.getSelectionModel().getSelectedItem();
            String rawText = queryField.getText() != null ? queryField.getText().trim() : "";
            stage.close();

            if (selected != null) {
                run(selected);
            } else if (!rawText.isEmpty()) {
                if (rawText.startsWith("mvn ")) {
                    rawText = rawText.substring(4).trim();
                }
                if (!rawText.isEmpty()) {
                    run(rawText);
                }
            }
        };

        // Wire search input events
        queryField.textProperty().addListener((obs, oldVal, newVal) -> filterGoals(newVal));
        queryField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.DOWN) {
                goalsList.requestFocus();
                if (goalsList.getSelectionModel().getSelectedItem() == null && !visibleGoals.isEmpty()) {
                    goalsList.getSelectionModel().selectFirst();
                }
            } else if (e.getCode() == KeyCode.UP) {
                goalsList.requestFocus();
                if (!visibleGoals.isEmpty()) {
                    goalsList.getSelectionModel().selectLast();
                }
            } else if (e.getCode() == KeyCode.ENTER) {
                executeSelected.run();
            } else if (e.getCode() == KeyCode.ESCAPE) {
                stage.close();
            }
        });

        // Wire list events
        goalsList.setOnMouseClicked(e -> {
            if (e.getClickCount() == 1 || e.getClickCount() == 2) {
                if (goalsList.getSelectionModel().getSelectedItem() != null) {
                    executeSelected.run();
                }
            }
        });
        goalsList.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                executeSelected.run();
            } else if (e.getCode() == KeyCode.ESCAPE) {
                stage.close();
            } else if (e.getCode() == KeyCode.UP && goalsList.getSelectionModel().getSelectedIndex() == 0) {
                queryField.requestFocus();
                queryField.positionCaret(queryField.getText().length());
            }
        });

        VBox content = new VBox(0, headerBar, searchArea, separator, sectionHeader, goalsList);
        content.getStyleClass().add("run-anything-dialog");
        content.setEffect(new DropShadow(16, 0, 8, Color.color(0, 0, 0, 0.45)));

        Scene scene = new Scene(content, 540, 420);
        scene.setFill(Color.TRANSPARENT);
        scene.getStylesheets().add(getClass().getResource("/css/lumina-dark.css").toExternalForm());
        stage.setScene(scene);

        // Auto close on Esc or focus loss
        scene.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, ev -> {
            if (ev.getCode() == KeyCode.ESCAPE) {
                ev.consume();
                stage.close();
            }
        });
        stage.focusedProperty().addListener((obs, was, focused) -> {
            if (!focused) stage.close();
        });

        stage.setOnShown(e -> {
            queryField.requestFocus();
            queryField.positionCaret(queryField.getText().length());
        });

        filterGoals(queryField.getText());
    }

    private void run(String goal) {
        if (onRunGoal != null && goal != null && !goal.isBlank()) {
            onRunGoal.accept(goal.trim());
        }
    }

    private void populateGoals(MavenProject project) {
        Set<String> set = new LinkedHashSet<>();
        // Default / project lifecycle phases in order
        if (project != null && project.getLifecyclePhases() != null && !project.getLifecyclePhases().isEmpty()) {
            set.addAll(project.getLifecyclePhases());
        } else {
            set.addAll(List.of("clean", "compile", "deploy", "install", "package", "site", "test", "validate", "verify"));
        }

        // Plugin goals from project or defaults
        if (project != null && project.getPlugins() != null) {
            for (PluginItem plugin : project.getPlugins()) {
                String prefix = plugin.prefix();
                for (String g : plugin.goals()) {
                    set.add(prefix + ":" + g);
                }
            }
        } else {
            // Core standard plugin goals
            set.addAll(List.of(
                    "clean:clean", "clean:help",
                    "compiler:compile", "compiler:help", "compiler:testCompile",
                    "deploy:deploy", "deploy:deploy-file", "deploy:help",
                    "install:help", "install:install", "install:install-file",
                    "jar:jar", "jar:help",
                    "resources:resources", "resources:testResources",
                    "site:site",
                    "surefire:test"
            ));
        }

        allGoals.addAll(set);
    }

    private void filterGoals(String raw) {
        String filter = raw != null ? raw.trim() : "";
        if (filter.startsWith("mvn ")) {
            filter = filter.substring(4).trim();
        } else if (filter.equals("mvn")) {
            filter = "";
        }
        String lower = filter.toLowerCase();

        visibleGoals.clear();
        for (String goal : allGoals) {
            if (lower.isEmpty() || goal.toLowerCase().contains(lower)) {
                visibleGoals.add(goal);
            }
        }

        if (!visibleGoals.isEmpty()) {
            goalsList.getSelectionModel().selectFirst();
        }
    }

    public void show() {
        if (stage.getOwner() != null) {
            stage.setX(stage.getOwner().getX() + (stage.getOwner().getWidth() - 540) / 2);
            stage.setY(stage.getOwner().getY() + 100);
        } else {
            stage.centerOnScreen();
        }
        stage.show();
        Platform.runLater(() -> {
            queryField.requestFocus();
            queryField.positionCaret(queryField.getText().length());
        });
    }

    public static Node createMavenGlyph() {
        Label m = new Label("m");
        m.getStyleClass().add("maven-blue-glyph");
        m.setStyle("-fx-font-family: 'Georgia', 'DejaVu Serif', serif; -fx-font-size: 13px; -fx-font-weight: bold; -fx-font-style: italic; -fx-text-fill: #3574F0;");
        return m;
    }
}
