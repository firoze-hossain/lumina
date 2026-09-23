package dev.lumina.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * The Services tool window: provides a standard tool window header with options
 * and hide controls, a list of service connections on the left, and a detail pane on the right.
 */
public final class ServicesPanel extends VBox {

    private final ToolWindowHeader header = new ToolWindowHeader("Services");

    public ServicesPanel() {
        getStyleClass().add("services-panel");
        setStyle("-fx-background-color: #1E1F22;");

        Button add = toolbarButton("+", "Add Service");
        Button toggleView = toolbarButton("\u25CE", "Show Services Details");
        HBox listToolbar = new HBox(4, add, toggleView);
        listToolbar.getStyleClass().add("services-list-toolbar");
        listToolbar.setPadding(new Insets(4, 6, 4, 6));

        ListView<String> list = new ListView<>();
        list.getItems().add("Docker");
        list.setCellFactory(v -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                setGraphic(empty ? null : dockerIcon());
            }
        });
        list.getStyleClass().add("services-list");
        VBox.setVgrow(list, Priority.ALWAYS);

        VBox left = new VBox(listToolbar, list);
        left.getStyleClass().add("services-list-pane");
        left.setPrefWidth(220);

        Label prompt = new Label("Double-click on the server node to connect");
        prompt.getStyleClass().add("right-tool-empty");
        StackPane detail = new StackPane(prompt);
        detail.getStyleClass().add("services-detail-pane");
        HBox.setHgrow(detail, Priority.ALWAYS);

        list.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                prompt.setText("Double-click on the server node to connect");
            }
        });

        HBox body = new HBox(left, detail);
        body.setStyle("-fx-background-color: #1E1F22;");
        VBox.setVgrow(body, Priority.ALWAYS);

        getChildren().addAll(header, body);
    }

    public void setOnHideToolWindow(Runnable onHide) {
        header.setOnHide(onHide);
    }

    public void setOnMaximizeToolWindow(Runnable onMaximize) {
        header.setOnMaximize(onMaximize);
    }

    public ToolWindowHeader getHeader() {
        return header;
    }

    private static Button toolbarButton(String glyph, String tip) {
        Button b = new Button(glyph);
        b.getStyleClass().add("console-button");
        b.setTooltip(new Tooltip(tip));
        return b;
    }

    private static javafx.scene.Node dockerIcon() {
        javafx.scene.shape.Circle c = new javafx.scene.shape.Circle(6);
        c.setFill(javafx.scene.paint.Color.web("#3592C4"));
        StackPane pane = new StackPane(c);
        pane.setPrefSize(14, 14);
        pane.setAlignment(Pos.CENTER);
        return pane;
    }
}
