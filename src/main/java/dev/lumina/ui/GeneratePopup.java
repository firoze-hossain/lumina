package dev.lumina.ui;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;

import java.util.List;

/**
 * IntelliJ IDEA-style Generate popup (Alt+Insert):
 * Header "Generate" with list of code generation options:
 * - Spring Component...
 * - Constructor
 * - Logger
 * - Getter
 * - Setter
 * - Getter and Setter
 * - equals() and hashCode()
 * - toString()
 * - Override Methods... Ctrl+O
 * - Delegate Methods...
 * - Test...
 * - Copyright
 */
public class GeneratePopup {

    public record GenerateItem(String id, String label, String shortcut, String iconGlyph, Runnable action) {
        public static GenerateItem of(String id, String label, String shortcut, String iconGlyph, Runnable action) {
            return new GenerateItem(id, label, shortcut, iconGlyph, action);
        }
    }

    private final Popup popup = new Popup();
    private final ListView<GenerateItem> listView = new ListView<>();

    public GeneratePopup() {
        popup.setAutoHide(true);
        popup.setHideOnEscape(true);

        Label title = new Label("Generate");
        title.getStyleClass().add("generate-title");
        title.setAlignment(Pos.CENTER);
        title.setMaxWidth(Double.MAX_VALUE);

        listView.getStyleClass().add("generate-list");
        listView.setCellFactory(lv -> new GenerateCell());
        listView.setFocusTraversable(true);

        listView.setOnMouseClicked(e -> {
            GenerateItem sel = listView.getSelectionModel().getSelectedItem();
            if (sel != null && sel.action() != null) {
                popup.hide();
                sel.action().run();
            }
        });

        listView.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.ENTER) {
                GenerateItem sel = listView.getSelectionModel().getSelectedItem();
                if (sel != null && sel.action() != null) {
                    popup.hide();
                    sel.action().run();
                }
                e.consume();
            } else if (e.getCode() == KeyCode.ESCAPE) {
                popup.hide();
                e.consume();
            }
        });

        VBox box = new VBox(6, title, listView);
        box.getStyleClass().add("generate-popup");
        box.setPrefWidth(260);
        popup.getContent().add(box);
    }

    public void show(Node owner, Bounds anchor, List<GenerateItem> items) {
        if (items.isEmpty()) return;

        listView.setItems(FXCollections.observableArrayList(items));
        int rowHeight = 26;
        listView.setPrefHeight(items.size() * rowHeight + 10);
        listView.getSelectionModel().select(0);

        double screenX = anchor.getMinX();
        double screenY = anchor.getMaxY() + 4;
        popup.show(owner, screenX, screenY);

        Platform.runLater(listView::requestFocus);
    }

    public void hide() {
        popup.hide();
    }

    public boolean isShowing() {
        return popup.isShowing();
    }

    private static final class GenerateCell extends ListCell<GenerateItem> {
        @Override
        protected void updateItem(GenerateItem item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
                setGraphic(null);
                return;
            }

            HBox box = new HBox(8);
            box.setAlignment(Pos.CENTER_LEFT);
            box.setPadding(new Insets(3, 8, 3, 8));

            if (item.iconGlyph() != null && !item.iconGlyph().isEmpty()) {
                Label icon = new Label(item.iconGlyph());
                icon.getStyleClass().add("generate-item-icon");
                box.getChildren().add(icon);
            }

            Label lbl = new Label(item.label());
            lbl.getStyleClass().add("generate-item-label");
            lbl.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(lbl, Priority.ALWAYS);
            box.getChildren().add(lbl);

            if (item.shortcut() != null && !item.shortcut().isEmpty()) {
                Label sc = new Label(item.shortcut());
                sc.getStyleClass().add("generate-item-shortcut");
                box.getChildren().add(sc);
            }

            setGraphic(box);
            setText(null);
        }
    }
}
