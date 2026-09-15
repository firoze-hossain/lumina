package dev.lumina.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * GitHub Copilot's MCP Log tool window, laid out like IntelliJ's: Clear
 * Logs / a filter box / a server picker / Edit Config, over a scrolling
 * log list. There's no real Copilot or MCP integration in Lumina, so the
 * log genuinely stays empty and the filter/server picker have nothing to
 * act on \u2014 shown for the layout, not pretending to be wired to anything.
 */
public final class McpLogPanel extends VBox {

    private final ListView<String> log = new ListView<>();

    public McpLogPanel(Runnable onEditConfig) {
        getStyleClass().add("mcp-log-panel");

        Button clear = new Button("Clear Logs");
        clear.getStyleClass().add("dialog-secondary");
        clear.setOnAction(e -> log.getItems().clear());

        Label filterLabel = new Label("Filter:");
        TextField filter = new TextField();
        filter.setPromptText("Search logs\u2026");
        filter.setPrefWidth(180);

        Label serverLabel = new Label("Server:");
        ComboBox<String> server = new ComboBox<>();
        server.getItems().add("All Servers");
        server.setValue("All Servers");

        Button editConfig = new Button("Edit Config");
        editConfig.getStyleClass().add("dialog-secondary");
        editConfig.setOnAction(e -> onEditConfig.run());

        HBox toolbar = new HBox(10, clear, filterLabel, filter,
                serverLabel, server, editConfig);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(8, 10, 8, 10));
        toolbar.getStyleClass().add("mcp-log-toolbar");

        log.getStyleClass().add("mcp-log-list");
        VBox.setVgrow(log, Priority.ALWAYS);

        getChildren().addAll(toolbar, log);
    }
}
