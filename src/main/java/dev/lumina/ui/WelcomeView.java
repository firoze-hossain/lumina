package dev.lumina.ui;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/** Center placeholder shown when no editor tabs are open. */
public class WelcomeView extends VBox {

    public WelcomeView(Runnable onNewProject, Runnable onNewFile,
                       Runnable onOpenFile, Runnable onOpenFolder) {
        getStyleClass().add("welcome-view");
        setAlignment(Pos.CENTER);
        setSpacing(10);

        Label glyph = new Label("\u2726");            // four-pointed star
        glyph.getStyleClass().add("welcome-glyph");

        Label name = new Label("Lumina");
        name.getStyleClass().add("welcome-title");

        Label tagline = new Label("Write Java in a warmer light.");
        tagline.getStyleClass().add("welcome-tagline");

        Button newProject = welcomeButton("New project\u2026", "Create Java or Spring Boot", onNewProject);
        newProject.getStyleClass().add("welcome-primary");
        Button newFile = welcomeButton("New file", "\u2318N / Ctrl+N", onNewFile);
        Button openFile = welcomeButton("Open file", "\u2318O / Ctrl+O", onOpenFile);
        Button openFolder = welcomeButton("Open folder", "\u21E7\u2318O / Ctrl+Shift+O", onOpenFolder);

        HBox actions = new HBox(12, newProject, newFile, openFile, openFolder);
        actions.setAlignment(Pos.CENTER);
        actions.setPadding(new javafx.geometry.Insets(24, 0, 0, 0));

        getChildren().addAll(glyph, name, tagline, actions);
    }

    private Button welcomeButton(String text, String hint, Runnable action) {
        Button b = new Button(text);
        b.getStyleClass().add("welcome-button");
        b.setTooltip(new javafx.scene.control.Tooltip(hint));
        b.setOnAction(e -> action.run());
        return b;
    }
}
