package dev.lumina.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;

/** Slim vertical tool-window bar on the far left, like IntelliJ's new UI. */
public class IconRail extends VBox {

    private final ToggleButton projectToggle;
    private final ToggleButton consoleToggle;

    public IconRail(Consumer<Boolean> onProjectToggle,
                    Consumer<Boolean> onConsoleToggle,
                    Runnable onRun,
                    Runnable onNewProject) {
        getStyleClass().add("icon-rail");
        setAlignment(Pos.TOP_CENTER);
        setSpacing(6);
        setPadding(new Insets(10, 4, 10, 4));

        projectToggle = railToggle("\uD83D\uDCC1", "Project (toggle)", true);
        projectToggle.selectedProperty().addListener(
                (obs, old, v) -> onProjectToggle.accept(v));

        consoleToggle = railToggle("\u276F_", "Console (toggle)", true);
        consoleToggle.selectedProperty().addListener(
                (obs, old, v) -> onConsoleToggle.accept(v));

        Button run = railButton("\u25B6", "Run current file (Ctrl/Cmd+R)");
        run.getStyleClass().add("rail-run");
        run.setOnAction(e -> onRun.run());

        Button newProject = railButton("\uFF0B", "New Project\u2026");
        newProject.setOnAction(e -> onNewProject.run());

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        getChildren().addAll(projectToggle, consoleToggle, spacer, run, newProject);
    }

    public void setConsoleSelected(boolean selected) {
        consoleToggle.setSelected(selected);
    }

    private ToggleButton railToggle(String glyph, String tip, boolean selected) {
        ToggleButton b = new ToggleButton(glyph);
        b.getStyleClass().add("rail-button");
        b.setTooltip(new Tooltip(tip));
        b.setSelected(selected);
        return b;
    }

    private Button railButton(String glyph, String tip) {
        Button b = new Button(glyph);
        b.getStyleClass().add("rail-button");
        b.setTooltip(new Tooltip(tip));
        return b;
    }
}
