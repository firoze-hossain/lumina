package dev.lumina.gutter;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Popup;

import java.nio.file.Path;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * IntelliJ IDEA-style "Choose Implementation" floating popup.
 * Displays when a gutter icon has multiple navigation targets.
 */
public class ImplementationChooserPopup {

    private final Popup popup = new Popup();
    private final VBox root = new VBox(6);
    private final Label titleLabel = new Label();
    private final ObservableList<GutterMarker.NavigationTarget> items = FXCollections.observableArrayList();
    private final ListView<GutterMarker.NavigationTarget> listView = new ListView<>(items);
    private final BiConsumer<Path, Integer> onNavigate;

    public ImplementationChooserPopup(BiConsumer<Path, Integer> onNavigate) {
        this.onNavigate = onNavigate;

        popup.setAutoFix(true);
        popup.setAutoHide(true);
        popup.setHideOnEscape(true);

        root.getStyleClass().add("impl-chooser-popup");
        root.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #43454A; -fx-border-radius: 6; "
                + "-fx-background-radius: 6; -fx-padding: 8 10 10 10; "
                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 12, 0, 0, 4);");
        root.setPrefWidth(520);
        root.setMaxWidth(620);

        // Header
        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(2, 4, 4, 4));

        titleLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-font-weight: bold;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label closeBtn = new Label("✕");
        closeBtn.setStyle("-fx-text-fill: #80848B; -fx-cursor: hand; -fx-font-size: 11px;");
        closeBtn.setOnMouseClicked(e -> popup.hide());
        closeBtn.setOnMouseEntered(e -> closeBtn.setStyle("-fx-text-fill: #DFE1E5; -fx-cursor: hand; -fx-font-size: 11px;"));
        closeBtn.setOnMouseExited(e -> closeBtn.setStyle("-fx-text-fill: #80848B; -fx-cursor: hand; -fx-font-size: 11px;"));

        header.getChildren().addAll(titleLabel, spacer, closeBtn);

        // List
        listView.setStyle("-fx-background-color: transparent; -fx-border-color: #393B40; -fx-border-radius: 4;");
        listView.setPrefHeight(180);
        listView.setCellFactory(lv -> new ImplementationCell());

        listView.setOnMouseClicked(e -> {
            GutterMarker.NavigationTarget selected = listView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                popup.hide();
                if (onNavigate != null) {
                    onNavigate.accept(selected.file(), selected.line());
                }
            }
        });

        listView.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                GutterMarker.NavigationTarget selected = listView.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    popup.hide();
                    if (onNavigate != null) {
                        onNavigate.accept(selected.file(), selected.line());
                    }
                }
            } else if (e.getCode() == KeyCode.ESCAPE) {
                popup.hide();
            }
        });

        root.getChildren().addAll(header, listView);
        popup.getContent().add(root);
    }

    public void show(Node anchor, List<GutterMarker.NavigationTarget> targets) {
        show(anchor, "Choose Implementation (" + (targets != null ? targets.size() : 0) + " found)", targets);
    }

    public void show(Node anchor, String title, List<GutterMarker.NavigationTarget> targets) {
        if (targets == null || targets.isEmpty()) return;
        titleLabel.setText(title != null && !title.isBlank() ? title : "Choose Implementation (" + targets.size() + " found)");
        items.setAll(targets);
        listView.setPrefHeight(Math.min(260, Math.max(80, targets.size() * 32 + 10)));
        listView.getSelectionModel().selectFirst();

        Bounds b = anchor.localToScreen(anchor.getBoundsInLocal());
        if (b != null) {
            popup.show(anchor, b.getMaxX() + 6, b.getMinY());
        } else {
            popup.show(anchor, 100, 100);
        }
        listView.requestFocus();
    }

    public void hide() {
        if (popup.isShowing()) {
            popup.hide();
        }
    }

    private static class ImplementationCell extends ListCell<GutterMarker.NavigationTarget> {
        @Override
        protected void updateItem(GutterMarker.NavigationTarget item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
                setText(null);
                setStyle("-fx-background-color: transparent;");
                return;
            }

            HBox row = new HBox(8);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(3, 6, 3, 6));

            // Class icon badge (C)
            StackPane badge = new StackPane();
            badge.setPrefSize(14, 14);
            Circle c = new Circle(6.0, Color.TRANSPARENT);
            c.setStroke(Color.web("#56A8F5"));
            c.setStrokeWidth(1.2);
            Text letter = new Text("C");
            letter.setFont(Font.font("Segoe UI", FontWeight.BOLD, 7.5));
            letter.setFill(Color.web("#56A8F5"));
            badge.getChildren().addAll(c, letter);

            Label nameLbl = new Label(item.targetName());
            nameLbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-weight: bold; -fx-font-size: 11.5px;");

            Label pkgLbl = new Label("(" + item.packageName() + ")");
            pkgLbl.setStyle("-fx-text-fill: #80848B; -fx-font-size: 11px;");

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Label lineLbl = new Label(":" + item.line());
            lineLbl.setStyle("-fx-text-fill: #56A8F5; -fx-font-size: 11px;");

            row.getChildren().addAll(badge, nameLbl, pkgLbl, spacer, lineLbl);
            setGraphic(row);
            setText(null);
            setStyle("-fx-background-color: transparent; -fx-cursor: hand;");
        }
    }
}
