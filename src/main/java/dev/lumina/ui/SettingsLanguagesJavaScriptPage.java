package dev.lumina.ui;

import dev.lumina.javascript.JavaScriptLanguageVersion;
import dev.lumina.javascript.JavaScriptSettingsManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * 1:1 dynamic replica of IntelliJ IDEA Languages & Frameworks > JavaScript settings page.
 * Displays JavaScript language version, proposals description, and link to code completion.
 */
public class SettingsLanguagesJavaScriptPage extends VBox {

    private final JavaScriptSettingsManager manager = JavaScriptSettingsManager.getInstance();
    private Runnable onModifiedListener;
    private Runnable onNavigateToCodeCompletion;

    private ComboBox<String> languageVersionCombo;
    private Button optionsButton;
    private Label descriptionLabel;
    private Hyperlink codeCompletionLink;

    private JavaScriptLanguageVersion initialVersion;

    public SettingsLanguagesJavaScriptPage() {
        setSpacing(20);
        setPadding(new Insets(20, 24, 20, 24));
        setStyle("-fx-background-color: #1E1F22;");
        VBox.setVgrow(this, Priority.ALWAYS);

        buildContent();
        takeSnapshot();
    }

    private void buildContent() {
        // Row 1: Language version combo + description
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);

        Label label = new Label("JavaScript language version");
        label.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        label.setPrefWidth(200);

        languageVersionCombo = new ComboBox<>();
        languageVersionCombo.getItems().addAll(JavaScriptLanguageVersion.getAllDisplayNames());
        languageVersionCombo.setValue(manager.getLanguageVersion().getDisplayName());
        languageVersionCombo.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4; -fx-font-size: 13px;");
        languageVersionCombo.setPrefWidth(160);

        optionsButton = new Button("...");
        optionsButton.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4; -fx-font-size: 13px; -fx-padding: 4 10 4 10; -fx-cursor: hand;");
        optionsButton.setOnMouseEntered(e -> optionsButton.setStyle("-fx-background-color: #393B40; -fx-text-fill: #FFFFFF; -fx-border-color: #589DF6; -fx-border-radius: 4; -fx-background-radius: 4; -fx-font-size: 13px; -fx-padding: 4 10 4 10; -fx-cursor: hand;"));
        optionsButton.setOnMouseExited(e -> optionsButton.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4; -fx-font-size: 13px; -fx-padding: 4 10 4 10; -fx-cursor: hand;"));

        descriptionLabel = new Label(manager.getLanguageVersion().getDescription());
        descriptionLabel.setStyle("-fx-text-fill: #8C9099; -fx-font-size: 13px; -fx-padding: 0 0 0 12;");

        languageVersionCombo.valueProperty().addListener((obs, oldV, newV) -> {
            JavaScriptLanguageVersion v = JavaScriptLanguageVersion.fromDisplayName(newV);
            descriptionLabel.setText(v.getDescription());
            fireModified();
        });

        row.getChildren().addAll(label, languageVersionCombo, optionsButton, descriptionLabel);

        // Row 2: Configure code completion settings hyperlink
        codeCompletionLink = new Hyperlink("Configure code completion settings");
        codeCompletionLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 13px; -fx-border-color: transparent; -fx-padding: 0; -fx-underline: false;");
        codeCompletionLink.setOnMouseEntered(e -> codeCompletionLink.setStyle("-fx-text-fill: #70AAFF; -fx-font-size: 13px; -fx-border-color: transparent; -fx-padding: 0; -fx-underline: true;"));
        codeCompletionLink.setOnMouseExited(e -> codeCompletionLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 13px; -fx-border-color: transparent; -fx-padding: 0; -fx-underline: false;"));
        codeCompletionLink.setOnAction(e -> {
            if (onNavigateToCodeCompletion != null) {
                onNavigateToCodeCompletion.run();
            }
        });

        getChildren().addAll(row, codeCompletionLink);
    }

    public void setOnNavigateToCodeCompletion(Runnable listener) {
        this.onNavigateToCodeCompletion = listener;
    }

    public void setOnModifiedListener(Runnable listener) {
        this.onModifiedListener = listener;
    }

    private void fireModified() {
        if (onModifiedListener != null) {
            onModifiedListener.run();
        }
    }

    private void takeSnapshot() {
        this.initialVersion = JavaScriptLanguageVersion.fromDisplayName(languageVersionCombo.getValue());
    }

    public boolean isModified() {
        JavaScriptLanguageVersion current = JavaScriptLanguageVersion.fromDisplayName(languageVersionCombo.getValue());
        return current != initialVersion;
    }

    public void apply() {
        manager.setLanguageVersion(JavaScriptLanguageVersion.fromDisplayName(languageVersionCombo.getValue()));
        takeSnapshot();
        fireModified();
    }

    public void reset() {
        languageVersionCombo.setValue(initialVersion != null ? initialVersion.getDisplayName() : manager.getLanguageVersion().getDisplayName());
        descriptionLabel.setText(initialVersion != null ? initialVersion.getDescription() : manager.getLanguageVersion().getDescription());
        fireModified();
    }

    public void revertChanges() {
        reset();
    }

    public ComboBox<String> getLanguageVersionCombo() {
        return languageVersionCombo;
    }

    public Label getDescriptionLabel() {
        return descriptionLabel;
    }

    public Hyperlink getCodeCompletionLink() {
        return codeCompletionLink;
    }
}
