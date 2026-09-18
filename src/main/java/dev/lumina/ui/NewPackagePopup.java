package dev.lumina.ui;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Popup;
import javafx.stage.Window;

import java.util.function.Consumer;

/**
 * IntelliJ IDEA-style "New Package" floating popup:
 * Centered modal dialog card with a "New Package" header and a text input field
 * pre-populated with the current package path and trailing dot (e.g. "org.example.spring_boot_depency.").
 * Pressing Enter confirms and creates the package; Escape or clicking outside cancels.
 */
public class NewPackagePopup {

    private final Popup popup = new Popup();
    private final Label titleLabel = new Label("New Package");
    private final TextField packageField = new TextField();
    private Consumer<String> onConfirm;

    public NewPackagePopup() {
        popup.setAutoFix(true);
        popup.setAutoHide(true);
        popup.setHideOnEscape(true);

        titleLabel.getStyleClass().add("new-package-title");
        titleLabel.setAlignment(Pos.CENTER);
        titleLabel.setMaxWidth(Double.MAX_VALUE);

        packageField.getStyleClass().add("new-package-field");
        packageField.setPromptText("Package name");

        VBox box = new VBox(10, titleLabel, packageField);
        box.getStyleClass().add("new-package-popup");
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(12, 16, 14, 16));
        box.setPrefWidth(360);
        box.setMaxWidth(420);

        DropShadow shadow = new DropShadow();
        shadow.setColor(Color.rgb(0, 0, 0, 0.45));
        shadow.setRadius(16);
        shadow.setOffsetY(4);
        box.setEffect(shadow);

        popup.getContent().add(box);

        packageField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                confirm();
                e.consume();
            } else if (e.getCode() == KeyCode.ESCAPE) {
                popup.hide();
                e.consume();
            }
        });
    }

    private void confirm() {
        String text = packageField.getText() == null ? "" : packageField.getText().trim();
        popup.hide();
        if (!text.isEmpty() && onConfirm != null) {
            onConfirm.accept(text);
        }
    }

    public void show(Node owner, String initialPackage, Consumer<String> onConfirm) {
        this.onConfirm = onConfirm;
        String initial = initialPackage != null ? initialPackage : "";
        packageField.setText(initial);

        Window win = null;
        if (owner != null && owner.getScene() != null) {
            win = owner.getScene().getWindow();
        }

        if (win != null) {
            double x = win.getX() + Math.max(0, (win.getWidth() - 360) / 2.0);
            double y = win.getY() + Math.max(80, (win.getHeight() - 100) / 2.0 - 50);
            popup.show(win, x, y);
        } else if (owner != null) {
            popup.show(owner, 200, 200);
        }

        Platform.runLater(() -> {
            packageField.requestFocus();
            packageField.positionCaret(initial.length());
        });
    }

    public void hide() {
        popup.hide();
    }

    public boolean isShowing() {
        return popup.isShowing();
    }

    public TextField getPackageField() {
        return packageField;
    }
}
