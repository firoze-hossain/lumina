package dev.lumina.ui;

import javafx.geometry.Insets;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.function.Consumer;

/**
 * Tools > GitHub Copilot category overview page in Lumina IDE matching 1:1 design of reference IDE.
 */
public class SettingsToolsGitHubCopilotOverviewPage extends VBox {

    public static final List<String> CATEGORIES = List.of(
            "General",
            "Chat",
            "Sandbox",
            "Completions",
            "Customizations",
            "Keymap",
            "Model Context Protocol (MCP)",
            "Network"
    );

    private final Consumer<String> onNavigate;

    public SettingsToolsGitHubCopilotOverviewPage(Consumer<String> onNavigate) {
        this.onNavigate = onNavigate;
        buildUI();
    }

    private void buildUI() {
        setPadding(new Insets(20, 24, 20, 24));
        setSpacing(20);
        setStyle("-fx-background-color: #1E1F22;");

        Label descLabel = new Label("Configure GitHub Copilot settings. Choose a category below to customize your experience.");
        descLabel.setStyle("-fx-text-fill: #868A91; -fx-font-size: 13px;");

        VBox categoriesBox = new VBox(10);
        Label catHeader = new Label("Settings Categories");
        catHeader.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold;");

        categoriesBox.getChildren().add(catHeader);

        for (String cat : CATEGORIES) {
            Hyperlink link = new Hyperlink(cat);
            link.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 13px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0;");
            link.setOnMouseEntered(e -> link.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 13px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0;"));
            link.setOnMouseExited(e -> link.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 13px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0;"));
            link.setOnAction(e -> {
                if (onNavigate != null) {
                    onNavigate.accept(cat);
                }
            });
            categoriesBox.getChildren().add(link);
        }

        getChildren().addAll(descLabel, categoriesBox);
    }
}
