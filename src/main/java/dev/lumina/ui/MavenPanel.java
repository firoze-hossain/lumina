package dev.lumina.ui;

import dev.lumina.run.RunConfiguration;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.BorderPane;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

/**
 * IntelliJ-style build tool window. Lists lifecycle goals/tasks for the
 * open project; double-click runs the goal in the Run console.
 */
public class MavenPanel extends BorderPane {

    private static final List<String> MAVEN_GOALS = List.of(
            "clean", "validate", "compile", "test", "package",
            "verify", "install", "dependency:tree", "spring-boot:run");

    private static final List<String> GRADLE_TASKS = List.of(
            "clean", "classes", "test", "build", "run", "bootRun", "dependencies");

    private final Label header = new Label("MAVEN");
    private final Label projectLabel = new Label("No project");
    private final ListView<String> goals = new ListView<>();
    private final Consumer<String> onRunGoal;

    public MavenPanel(Consumer<String> onRunGoal) {
        this.onRunGoal = onRunGoal;
        getStyleClass().add("side-panel");

        header.getStyleClass().add("panel-header");
        header.setPadding(new Insets(8, 12, 2, 12));
        projectLabel.getStyleClass().add("side-subtle");
        projectLabel.setPadding(new Insets(0, 12, 8, 12));

        goals.getStyleClass().add("goal-list");
        goals.setPlaceholder(styledPlaceholder(
                "Open a Maven or Gradle project\nto see its goals"));
        goals.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                String goal = goals.getSelectionModel().getSelectedItem();
                if (goal != null) onRunGoal.accept(goal);
            }
        });

        javafx.scene.layout.VBox top = new javafx.scene.layout.VBox(header, projectLabel);
        setTop(top);
        setCenter(goals);
        setMinWidth(180);
    }

    /** Refresh contents for the given project root (null clears the panel). */
    public void setProject(Path root) {
        if (root == null) {
            header.setText("MAVEN");
            projectLabel.setText("No project");
            goals.getItems().clear();
            return;
        }
        if (RunConfiguration.isMavenProject(root)) {
            header.setText("MAVEN");
            projectLabel.setText("m " + root.getFileName());
            goals.getItems().setAll(MAVEN_GOALS);
        } else if (RunConfiguration.isGradleProject(root)) {
            header.setText("GRADLE");
            projectLabel.setText("g " + root.getFileName());
            goals.getItems().setAll(GRADLE_TASKS);
        } else {
            header.setText("MAVEN");
            projectLabel.setText(root.getFileName() + " (no build file)");
            goals.getItems().clear();
        }
    }

    private Label styledPlaceholder(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("explorer-empty");
        return l;
    }
}
