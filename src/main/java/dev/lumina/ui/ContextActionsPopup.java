package dev.lumina.ui;

import dev.lumina.codegen.JavaCodeGenerator;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Separator;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Popup;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * IntelliJ IDEA-style Context Actions popup (Alt+Enter / More actions...):
 * Displays light-bulb quick-fixes and refactorings with an adjacent
 * Live Code Preview popup showing exact generated code with line numbers.
 */
public class ContextActionsPopup {

    public record ActionItem(
            String id,
            String label,
            boolean isFix,
            boolean isSeparator,
            boolean hasSubmenu,
            JavaCodeGenerator.Preview preview,
            Runnable action
    ) {
        public static ActionItem fix(String id, String label, JavaCodeGenerator.Preview preview, Runnable action) {
            return new ActionItem(id, label, true, false, false, preview, action);
        }

        public static ActionItem item(String id, String label, Runnable action) {
            return new ActionItem(id, label, false, false, false, null, action);
        }

        public static ActionItem itemWithMenu(String id, String label, Runnable action) {
            return new ActionItem(id, label, false, false, true, null, action);
        }

        public static ActionItem separator() {
            return new ActionItem(null, null, false, true, false, null, null);
        }
    }

    private final Popup popup = new Popup();
    private final Popup previewPopup = new Popup();
    private final ListView<ActionItem> listView = new ListView<>();
    private final VBox previewBox = new VBox(4);
    private boolean showPreview = true;
    private Node ownerNode;

    public ContextActionsPopup() {
        popup.setAutoHide(true);
        popup.setHideOnEscape(true);
        previewPopup.setAutoHide(false);

        listView.getStyleClass().add("context-actions-list");
        listView.setFocusTraversable(true);
        listView.setCellFactory(lv -> new ActionCell());

        listView.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            updatePreview(newV);
        });

        listView.setOnMouseClicked(e -> {
            ActionItem sel = listView.getSelectionModel().getSelectedItem();
            if (sel != null && sel.action() != null && !sel.isSeparator()) {
                hide();
                sel.action().run();
            }
        });

        listView.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.ENTER) {
                ActionItem sel = listView.getSelectionModel().getSelectedItem();
                if (sel != null && sel.action() != null && !sel.isSeparator()) {
                    hide();
                    sel.action().run();
                }
                e.consume();
            } else if (e.getCode() == KeyCode.ESCAPE) {
                hide();
                e.consume();
            } else if (e.getCode() == KeyCode.Q && e.isControlDown()) {
                showPreview = !showPreview;
                ActionItem sel = listView.getSelectionModel().getSelectedItem();
                updatePreview(sel);
                e.consume();
            }
        });

        popup.setOnHidden(e -> previewPopup.hide());

        Label tip = new Label("Press Ctrl+Q to toggle preview");
        tip.getStyleClass().add("context-actions-tip");

        VBox menuBox = new VBox(listView, tip);
        menuBox.getStyleClass().add("context-actions-popup");
        menuBox.setPrefWidth(320);
        menuBox.setMaxWidth(380);
        popup.getContent().add(menuBox);

        previewBox.getStyleClass().add("context-preview-popup");
        previewBox.setPadding(new Insets(8, 12, 8, 12));
        previewPopup.getContent().add(previewBox);
    }

    public void show(Node owner, Bounds anchor, List<ActionItem> items) {
        this.ownerNode = owner;
        if (items.isEmpty()) return;

        listView.setItems(FXCollections.observableArrayList(items));
        int rowHeight = 26;
        int listHeight = Math.min(380, items.size() * rowHeight + 10);
        listView.setPrefHeight(listHeight);

        // Select first non-separator item
        for (int i = 0; i < items.size(); i++) {
            if (!items.get(i).isSeparator()) {
                listView.getSelectionModel().select(i);
                break;
            }
        }

        double screenX = anchor.getMinX();
        double screenY = anchor.getMaxY() + 4;
        popup.show(owner, screenX, screenY);

        Platform.runLater(() -> {
            listView.requestFocus();
            updatePreview(listView.getSelectionModel().getSelectedItem());
        });
    }

    public void hide() {
        if (popup.isShowing()) popup.hide();
        if (previewPopup.isShowing()) previewPopup.hide();
    }

    public boolean isShowing() {
        return popup.isShowing();
    }

    private void updatePreview(ActionItem item) {
        if (!showPreview || item == null || item.preview() == null || !popup.isShowing()) {
            previewPopup.hide();
            return;
        }

        previewBox.getChildren().clear();
        JavaCodeGenerator.Preview p = item.preview();
        String[] lines = p.code().split("\n");
        int lineNum = p.startLine();

        for (String line : lines) {
            HBox lineBox = new HBox(10);
            lineBox.setAlignment(Pos.CENTER_LEFT);

            Label numLbl = new Label(String.valueOf(lineNum++));
            numLbl.getStyleClass().add("context-preview-line-num");
            numLbl.setMinWidth(26);
            numLbl.setAlignment(Pos.CENTER_RIGHT);

            TextFlow codeFlow = renderSyntaxTokens(line);
            lineBox.getChildren().addAll(numLbl, codeFlow);
            previewBox.getChildren().add(lineBox);
        }

        double menuX = popup.getX();
        double menuY = popup.getY();
        double previewWidth = 330;
        double previewX = Math.max(10, menuX - previewWidth - 8);

        if (!previewPopup.isShowing()) {
            previewPopup.show(ownerNode, previewX, menuY);
        } else {
            previewPopup.setX(previewX);
            previewPopup.setY(menuY);
        }
    }

    private TextFlow renderSyntaxTokens(String line) {
        TextFlow tf = new TextFlow();
        tf.getStyleClass().add("context-preview-code");

        String[] tokens = line.split("(?<=\\s)|(?=\\s)|(?<=[(),;{}])|(?=[(),;{}])");
        for (String tok : tokens) {
            Text text = new Text(tok);
            if (tok.matches("public|private|protected|void|return|class|static|final")) {
                text.setFill(Color.web("#CF8E6D")); // keyword
            } else if (tok.matches("String|Long|Integer|Boolean|Object|Double|Float|List|Map|Set")) {
                text.setFill(Color.web("#56A8F5")); // type
            } else if (tok.matches("[a-z][A-Za-z0-9_]*(?=\\()|get[A-Z][A-Za-z0-9_]*|set[A-Z][A-Za-z0-9_]*")) {
                text.setFill(Color.web("#56A8F5")); // method
            } else if (tok.equals("this")) {
                text.setFill(Color.web("#CF8E6D"));
            } else if (tok.matches("[A-Za-z0-9_]+")) {
                text.setFill(Color.web("#BCBEC4")); // identifier
            } else {
                text.setFill(Color.web("#BCBEC4")); // punctuation
            }
            tf.getChildren().add(text);
        }
        return tf;
    }

    private final class ActionCell extends ListCell<ActionItem> {
        @Override
        protected void updateItem(ActionItem item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
                setGraphic(null);
                setStyle("");
                return;
            }

            if (item.isSeparator()) {
                Separator sep = new Separator();
                sep.getStyleClass().add("context-menu-sep");
                setGraphic(sep);
                setText(null);
                setDisable(true);
                return;
            }

            setDisable(false);
            HBox box = new HBox(8);
            box.setAlignment(Pos.CENTER_LEFT);
            box.setPadding(new Insets(2, 6, 2, 6));

            if (item.isFix()) {
                Label bulb = new Label("\uD83D\uDCA1"); // 💡
                bulb.getStyleClass().add("context-item-bulb");
                box.getChildren().add(bulb);
            } else {
                Region spacer = new Region();
                spacer.setPrefWidth(16);
                box.getChildren().add(spacer);
            }

            Label lbl = new Label(item.label());
            lbl.getStyleClass().add("context-item-label");
            lbl.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(lbl, Priority.ALWAYS);
            box.getChildren().add(lbl);

            if (item.hasSubmenu()) {
                Label arrow = new Label(">");
                arrow.getStyleClass().add("context-item-arrow");
                box.getChildren().add(arrow);
            }

            setGraphic(box);
            setText(null);
        }
    }
}
