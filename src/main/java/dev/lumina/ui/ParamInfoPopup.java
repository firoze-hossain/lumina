package dev.lumina.ui;

import dev.lumina.semantics.Docs;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Popup;

import java.util.List;

/**
 * M4 — parameter info: one line per overload with the argument under the
 * caret emphasized, IntelliJ-style. The index is updated as the caret moves
 * between arguments.
 */
public class ParamInfoPopup {

    private final Popup popup = new Popup();
    private final VBox box = new VBox(3);
    private List<Docs.Signature> signatures = List.of();
    private int openParenOffset = -1;

    public ParamInfoPopup() {
        popup.setAutoHide(true);
        popup.setHideOnEscape(true);
        box.getStyleClass().add("param-popup");
        popup.getContent().add(box);
    }

    public void show(Node owner, List<Docs.Signature> signatures,
                     int argIndex, int openParenOffset, Bounds caretScreen) {
        this.signatures = signatures;
        this.openParenOffset = openParenOffset;
        render(argIndex);
        popup.show(owner, caretScreen.getMinX(),
                caretScreen.getMinY() - box.getHeight() - 30);
        popup.setY(caretScreen.getMinY() - box.getHeight() - 8);
    }

    /** The '(' this popup belongs to; used to decide update-vs-hide. */
    public int openParenOffset() {
        return openParenOffset;
    }

    public void updateArgIndex(int argIndex) {
        if (popup.isShowing()) render(argIndex);
    }

    private void render(int argIndex) {
        box.getChildren().clear();
        for (Docs.Signature sig : signatures) {
            TextFlow flow = new TextFlow();
            flow.getStyleClass().add("param-row");
            if (sig.params().isEmpty()) {
                flow.getChildren().add(text(sig.name() + "()  : "
                        + sig.returnType(), false));
            } else {
                flow.getChildren().add(text(sig.name() + "(", false));
                for (int i = 0; i < sig.params().size(); i++) {
                    flow.getChildren().add(text(sig.params().get(i),
                            i == argIndex));
                    if (i < sig.params().size() - 1) {
                        flow.getChildren().add(text(", ", false));
                    }
                }
                flow.getChildren().add(text(")  : " + sig.returnType(), false));
            }
            box.getChildren().add(flow);
        }
    }

    private static Text text(String content, boolean current) {
        Text t = new Text(content);
        t.getStyleClass().add(current ? "param-current" : "param-text");
        return t;
    }

    public void hide() {
        popup.hide();
    }

    public boolean isShowing() {
        return popup.isShowing();
    }
}