package dev.lumina.ui;

import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;

/**
 * M4 — the quick-documentation popup (Ctrl+Q): a signature header and the
 * extracted Javadoc body, dismissed by Esc, click, or caret movement.
 */
public class DocPopup {

    private final Popup popup = new Popup();
    private final Label title = new Label();
    private final Label body = new Label();

    public DocPopup() {
        popup.setAutoHide(true);
        popup.setHideOnEscape(true);

        title.getStyleClass().add("doc-title");
        title.setWrapText(true);
        title.setMaxWidth(520);

        body.getStyleClass().add("doc-body");
        body.setWrapText(true);
        body.setMaxWidth(520);

        ScrollPane scroll = new ScrollPane(body);
        scroll.setFitToWidth(true);
        scroll.setMaxHeight(260);
        scroll.getStyleClass().add("doc-scroll");

        VBox box = new VBox(6, title, scroll);
        box.getStyleClass().add("doc-popup");
        popup.getContent().add(box);
    }

    public void show(Node owner, String signature, String documentation,
                     Bounds caretScreen) {
        title.setText(signature == null ? "" : signature);
        body.setText(documentation == null || documentation.isBlank()
                ? "No documentation found." : documentation);
        popup.show(owner, caretScreen.getMinX(), caretScreen.getMaxY() + 4);
    }

    public void hide() {
        popup.hide();
    }

    public boolean isShowing() {
        return popup.isShowing();
    }
}