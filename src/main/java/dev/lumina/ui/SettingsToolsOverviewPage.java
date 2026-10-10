package dev.lumina.ui;

import java.util.List;
import java.util.function.Consumer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Tools Overview settings page in Lumina IDE matching 1:1 design of the reference IDE.
 * Displays category title, description, and dynamic clickable links to all registered tool pages.
 */
public class SettingsToolsOverviewPage extends VBox {

    public static final List<String> DEFAULT_TOOL_LINKS = List.of(
            "Actions on Save",
            "Black",
            "Bundler",
            "Claude Code [Beta]",
            "Code With Me",
            "CSV Formats",
            "Database",
            "Database Versioning",
            "Diagrams",
            "Diff & Merge",
            "External Tools",
            "Features Suggester",
            "Features Trainer",
            "GitHub Copilot",
            "HTTP Client",
            "JPA Entity Declaration",
            "JPA Reverse Engineering",
            "Jupyter",
            "Kotlin Notebook",
            "MCP Server",
            "Python External Documentation",
            "Python Integrated Tools",
            "Python Plots",
            "Qodana",
            "Remote SSH External Tools",
            "Rsync",
            "RuboCop",
            "Shared Indexes",
            "SSH Configurations",
            "SSH Terminal",
            "Startup Tasks",
            "Tasks",
            "Terminal",
            "Web Browsers and Preview",
            "XPath Viewer"
    );

    private final Consumer<String> onNavigate;

    public SettingsToolsOverviewPage(Consumer<String> onNavigate) {
        this.onNavigate = onNavigate;
        buildUI();
    }

    private void buildUI() {
        setPadding(new Insets(20, 24, 20, 24));
        setSpacing(14);
        setStyle("-fx-background-color: #1E1F22;");

        // 1. Title
        Label titleLabel = new Label("Tools");
        titleLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 16px; -fx-font-weight: bold;");

        // 2. Subtitle description
        Label descLabel = new Label("Configure integration with third-party applications, specify the SSH Terminal connection settings, manage server certificates and tasks, configure diagrams layout, etc.");
        descLabel.setWrapText(true);
        descLabel.setMaxWidth(850);
        descLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-line-spacing: 3px;");

        // 3. Links list
        VBox linksContainer = new VBox(4);
        linksContainer.setAlignment(Pos.TOP_LEFT);
        linksContainer.setPadding(new Insets(4, 0, 16, 0));

        for (String toolName : DEFAULT_TOOL_LINKS) {
            Hyperlink link = new Hyperlink(toolName);
            link.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 13px; -fx-border-color: transparent; -fx-padding: 2 0 2 0; -fx-underline: false;");
            link.setOnMouseEntered(e -> link.setStyle("-fx-text-fill: #70AAFF; -fx-font-size: 13px; -fx-border-color: transparent; -fx-padding: 2 0 2 0; -fx-underline: true;"));
            link.setOnMouseExited(e -> link.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 13px; -fx-border-color: transparent; -fx-padding: 2 0 2 0; -fx-underline: false;"));
            link.setOnAction(e -> {
                if (onNavigate != null) {
                    onNavigate.accept(toolName);
                }
            });
            linksContainer.getChildren().add(link);
        }

        ScrollPane scrollPane = new ScrollPane(linksContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent; -fx-padding: 0;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        getChildren().addAll(titleLabel, descLabel, scrollPane);
    }
}
