package dev.lumina.ui;

import dev.lumina.semantics.Completion;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.stage.Popup;

import java.util.List;
import java.util.function.Consumer;

/**
 * M2 — the completion popup: a lightweight, non-focus-stealing list shown at
 * the caret. The editor keeps keyboard focus and routes Up/Down/Enter/Tab/Esc
 * here while it is visible.
 */
public class CompletionPopup {

    private final Popup popup = new Popup();
    private final ListView<Completion.Item> list = new ListView<>();
    private final Consumer<Completion.Item> onAccept;

    public CompletionPopup(Consumer<Completion.Item> onAccept) {
        this.onAccept = onAccept;
        popup.setAutoFix(true);
        popup.setAutoHide(true);
        popup.setHideOnEscape(false);   // the editor handles Esc itself

        list.getStyleClass().add("completion-list");
        list.setCellFactory(lv -> new ItemCell());
        list.setPrefWidth(460);
        list.setMaxHeight(240);
        list.setFocusTraversable(false);
        list.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) acceptSelected();
        });
        popup.getContent().add(list);
    }

    /** Show (or refresh) the popup anchored under the caret bounds. */
    public void show(Node owner, List<Completion.Item> items, Bounds caretScreen) {
        list.getItems().setAll(items);
        list.getSelectionModel().select(0);
        list.scrollTo(0);
        list.setPrefHeight(Math.min(items.size(), 9) * 26 + 8);
        if (!popup.isShowing()) {
            popup.show(owner, caretScreen.getMinX(), caretScreen.getMaxY() + 2);
        } else {
            popup.setX(caretScreen.getMinX());
            popup.setY(caretScreen.getMaxY() + 2);
        }
    }

    public boolean isShowing() {
        return popup.isShowing();
    }

    public void hide() {
        popup.hide();
    }

    public void moveSelection(int delta) {
        int size = list.getItems().size();
        if (size == 0) return;
        int index = list.getSelectionModel().getSelectedIndex();
        int next = Math.max(0, Math.min(size - 1, index + delta));
        list.getSelectionModel().select(next);
        list.scrollTo(Math.max(0, next - 4));
    }

    public void acceptSelected() {
        Completion.Item selected = list.getSelectionModel().getSelectedItem();
        popup.hide();
        if (selected != null && onAccept != null) {
            onAccept.accept(selected);
        }
    }

    // -------------------------------------------------------------- render

    private static final class ItemCell extends ListCell<Completion.Item> {
        @Override
        protected void updateItem(Completion.Item item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
                setText(null);
                return;
            }
            Label icon = new Label(glyphFor(item.kind()));
            icon.getStyleClass().addAll("completion-icon",
                    "completion-icon-" + item.kind().name().toLowerCase());
            icon.setMinWidth(20);
            icon.setAlignment(Pos.CENTER);

            Label label = new Label(item.label());
            label.getStyleClass().add("completion-label");

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Label detail = new Label(item.detail());
            detail.getStyleClass().add("completion-detail");

            HBox row = new HBox(8, icon, label, spacer, detail);
            row.setAlignment(Pos.CENTER_LEFT);
            setGraphic(row);
            setText(null);
        }

        private static String glyphFor(Completion.Kind kind) {
            return switch (kind) {
                case METHOD -> "m";
                case FIELD -> "f";
                case VARIABLE -> "v";
                case CLASS -> "C";
                case KEYWORD -> "k";
                case TEMPLATE -> "t";
            };
        }
    }
}