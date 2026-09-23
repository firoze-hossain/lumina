package dev.lumina.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;

/**
 * GitHub Copilot MCP Log tool window: provides a tool window header with sub-tabs,
 * options menu (with move and hide actions), clear logs, filter, server picker,
 * edit config, and a log list.
 */
public final class McpLogPanel extends VBox {

    private final ToolWindowHeader header = new ToolWindowHeader("GitHub Copilot: MCP");
    private final ListView<String> log = new ListView<>();
    private final HBox toolbar;

    public McpLogPanel(Runnable onEditConfig) {
        getStyleClass().add("mcp-log-panel");
        setStyle("-fx-background-color: #1E1F22;");

        header.addTab("Log", false, () -> {}, null);

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

        toolbar = new HBox(10, clear, filterLabel, filter,
                serverLabel, server, editConfig);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(6, 10, 6, 10));
        toolbar.getStyleClass().add("mcp-log-toolbar");
        toolbar.setStyle("-fx-background-color: #1E1F22; -fx-border-color: #393B40; -fx-border-width: 0 0 1 0;");

        header.setOnToolbarToggle(visible -> {
            toolbar.setVisible(visible);
            toolbar.setManaged(visible);
        });

        log.getStyleClass().add("mcp-log-list");
        log.setStyle("-fx-background-color: #1E1F22; -fx-control-inner-background: #1E1F22;");
        VBox.setVgrow(log, Priority.ALWAYS);

        getChildren().addAll(header, toolbar, log);

        // Keyboard filter for Shift+Escape to hide panel
        addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.isShiftDown() && e.getCode() == KeyCode.ESCAPE) {
                if (header.getHideButton().getOnAction() != null) {
                    header.getHideButton().fire();
                    e.consume();
                }
            }
        });
    }

    public ToolWindowHeader getHeader() {
        return header;
    }

    public void setOnHideToolWindow(Runnable onHide) {
        header.setOnHide(onHide);
    }

    public void setOnMaximizeToolWindow(Runnable onMaximize) {
        header.setOnMaximize(onMaximize);
    }

    public void setOnMoveToToolWindow(Consumer<String> onMoveTo) {
        header.setOnMoveTo(onMoveTo);
    }

    public void setActiveMoveToPosition(String position) {
        header.setActiveMoveToPosition(position);
    }

    public String getActiveMoveToPosition() {
        return header.getActiveMoveToPosition();
    }
}
