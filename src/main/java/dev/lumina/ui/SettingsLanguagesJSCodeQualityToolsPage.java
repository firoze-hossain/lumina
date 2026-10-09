package dev.lumina.ui;

import javafx.geometry.Insets;
import javafx.scene.control.Hyperlink;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;

/**
 * Languages & Frameworks > JavaScript > Code Quality Tools overview page in Lumina IDE.
 * Displays clickable links to ESLint and JSHint.
 */
public class SettingsLanguagesJSCodeQualityToolsPage extends VBox {

    public SettingsLanguagesJSCodeQualityToolsPage(Consumer<String> onNavigate) {
        setPadding(new Insets(20, 24, 20, 24));
        setSpacing(12);
        setStyle("-fx-background-color: #1E1F22;");
        VBox.setVgrow(this, Priority.ALWAYS);

        for (String child : new String[]{"ESLint", "JSHint"}) {
            Hyperlink link = new Hyperlink(child);
            link.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 13px; -fx-border-color: transparent; -fx-padding: 2 0 2 0; -fx-underline: false;");
            link.setOnMouseEntered(e -> link.setStyle("-fx-text-fill: #70AAFF; -fx-font-size: 13px; -fx-border-color: transparent; -fx-padding: 2 0 2 0; -fx-underline: true;"));
            link.setOnMouseExited(e -> link.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 13px; -fx-border-color: transparent; -fx-padding: 2 0 2 0; -fx-underline: false;"));
            link.setOnAction(e -> {
                if (onNavigate != null) {
                    onNavigate.accept(child);
                }
            });
            getChildren().add(link);
        }
    }
}
