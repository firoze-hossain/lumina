package dev.lumina.ui;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;

import java.util.function.BiConsumer;

/**
 * IntelliJ's "New Java Class" popup: a name field on top, a list of kinds
 * below (Class selected by default), arrow keys move the kind selection
 * without leaving the name field, Enter confirms, Esc cancels.
 */
public class NewJavaClassPopup {

    public enum Kind {
        CLASS("Class", "C", "#4FA8F0"),
        INTERFACE("Interface", "I", "#8FCE8F"),
        RECORD("Record", "R", "#C77DBB"),
        ENUM("Enum", "E", "#E8B450"),
        ANNOTATION("Annotation", "@", "#8FCE8F"),
        EXCEPTION("Exception", "!", "#E5534B"),
        COMPACT_SOURCE_FILE("Compact source file", "J", "#56A8F5");

        final String label;
        final String glyph;
        final String color;

        Kind(String label, String glyph, String color) {
            this.label = label;
            this.glyph = glyph;
            this.color = color;
        }
    }

    private final Popup popup = new Popup();
    private final TextField nameField = new TextField();
    private final ListView<Kind> kindList = new ListView<>();
    private BiConsumer<String, Kind> onCreate;

    public NewJavaClassPopup() {
        popup.setAutoFix(true);
        popup.setAutoHide(true);
        popup.setHideOnEscape(true);

        Label title = new Label("New Java Class");
        title.getStyleClass().add("new-class-title");

        nameField.setPromptText("Name");
        nameField.getStyleClass().add("new-class-name");

        kindList.setItems(FXCollections.observableArrayList(Kind.values()));
        kindList.getStyleClass().add("new-class-kinds");
        kindList.setCellFactory(lv -> kindCell());
        kindList.setPrefHeight(Kind.values().length * 30 + 6);
        kindList.setFocusTraversable(false);
        kindList.setOnMouseClicked(e -> confirm());

        VBox box = new VBox(8, title, nameField, kindList);
        box.getStyleClass().add("new-class-popup");
        box.setPadding(new Insets(14));
        box.setPrefWidth(340);
        popup.getContent().add(box);

        nameField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.DOWN) {
                move(1);
                e.consume();
            } else if (e.getCode() == KeyCode.UP) {
                move(-1);
                e.consume();
            } else if (e.getCode() == KeyCode.ENTER) {
                confirm();
                e.consume();
            } else if (e.getCode() == KeyCode.ESCAPE) {
                popup.hide();
                e.consume();
            }
        });
    }

    private void move(int delta) {
        int size = kindList.getItems().size();
        int index = kindList.getSelectionModel().getSelectedIndex();
        int next = Math.max(0, Math.min(size - 1, (index < 0 ? 0 : index) + delta));
        kindList.getSelectionModel().select(next);
        kindList.scrollTo(next);
    }

    private void confirm() {
        String name = nameField.getText() == null ? "" : nameField.getText().trim();
        if (name.isEmpty()) return;
        Kind kind = kindList.getSelectionModel().getSelectedItem();
        if (kind == null) kind = Kind.CLASS;
        popup.hide();
        if (onCreate != null) onCreate.accept(name, kind);
    }

    public void show(Node owner, double screenX, double screenY,
                     BiConsumer<String, Kind> onCreate) {
        this.onCreate = onCreate;
        nameField.clear();
        kindList.getSelectionModel().select(Kind.CLASS);
        popup.show(owner, screenX, screenY);
        Platform.runLater(nameField::requestFocus);
    }

    private ListCell<Kind> kindCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(Kind kind, boolean empty) {
                super.updateItem(kind, empty);
                if (empty || kind == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                Label icon = new Label(kind.glyph);
                icon.getStyleClass().add("new-class-icon");
                icon.setStyle("-fx-text-fill: " + kind.color + ";"
                        + " -fx-border-color: " + kind.color + ";");
                Label label = new Label(kind.label);
                label.getStyleClass().add("new-class-label");
                HBox row = new HBox(10, icon, label);
                row.setAlignment(Pos.CENTER_LEFT);
                setGraphic(row);
                setText(null);
            }
        };
    }
}