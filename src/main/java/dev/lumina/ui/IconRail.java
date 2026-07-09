package dev.lumina.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;

/** Slim vertical tool-window bar on the far left, like IntelliJ's new UI. */
public class IconRail extends VBox {

    private final ToggleButton projectToggle;
    private final ToggleButton bottomToggle;

    public IconRail(Consumer<Boolean> onProjectToggle,
                    Consumer<Boolean> onBottomToggle,
                    Runnable onShowTerminal,
                    Runnable onRun,
                    Runnable onNewProject) {
        getStyleClass().add("icon-rail");
        setAlignment(Pos.TOP_CENTER);
        setSpacing(6);
        setPadding(new Insets(10, 4, 10, 4));

        projectToggle = railToggle("\uD83D\uDCC1", "Project (toggle)", true);
        projectToggle.selectedProperty().addListener(
                (obs, old, v) -> onProjectToggle.accept(v));

        bottomToggle = railToggle("\u2B9F", "Run / Terminal panel (toggle)", true);
        bottomToggle.selectedProperty().addListener(
                (obs, old, v) -> onBottomToggle.accept(v));

        Button terminal = railButton("\u276F_", "Terminal");
        terminal.setOnAction(e -> onShowTerminal.run());

        Button run = railButton("\u25B6", "Run (Ctrl/Cmd+R)");
        run.getStyleClass().add("rail-run");
        run.setOnAction(e -> onRun.run());

        Button newProject = railButton("\uFF0B", "New Project\u2026");
        newProject.setOnAction(e -> onNewProject.run());

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        getChildren().addAll(projectToggle, bottomToggle, terminal, spacer, run, newProject);
    }

    public void setBottomSelected(boolean selected) {
        bottomToggle.setSelected(selected);
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
