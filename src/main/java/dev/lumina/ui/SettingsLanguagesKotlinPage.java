package dev.lumina.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Hyperlink;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Languages & Frameworks > Kotlin settings overview page in Lumina IDE.
 * Displays navigation link to Kotlin Scripting.
 */
public class SettingsLanguagesKotlinPage extends VBox {

    private Runnable onNavigateToKotlinScripting;
    private final Hyperlink scriptingLink;

    public SettingsLanguagesKotlinPage() {
        this(null);
    }

    public SettingsLanguagesKotlinPage(Runnable onNavigateToKotlinScripting) {
        this.onNavigateToKotlinScripting = onNavigateToKotlinScripting;

        setSpacing(10);
        setPadding(new Insets(20, 24, 20, 24));
        setAlignment(Pos.TOP_LEFT);
        setStyle("-fx-background-color: #1E1F22;");
        VBox.setVgrow(this, Priority.ALWAYS);

        scriptingLink = new Hyperlink("Kotlin Scripting");
        scriptingLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 13px; -fx-border-color: transparent; -fx-padding: 0;");
        scriptingLink.setOnMouseEntered(e -> scriptingLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 13px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0;"));
        scriptingLink.setOnMouseExited(e -> scriptingLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 13px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0;"));
        scriptingLink.setOnAction(e -> {
            if (this.onNavigateToKotlinScripting != null) {
                this.onNavigateToKotlinScripting.run();
            }
        });

        getChildren().add(scriptingLink);
    }

    public void setOnNavigateToKotlinScripting(Runnable onNavigateToKotlinScripting) {
        this.onNavigateToKotlinScripting = onNavigateToKotlinScripting;
    }

    public Hyperlink getScriptingLink() {
        return scriptingLink;
    }
}
