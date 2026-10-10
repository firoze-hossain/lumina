package dev.lumina.ui;

import dev.lumina.tools.CodeWithMeSettings;
import dev.lumina.tools.CodeWithMeSettingsManager;
import java.util.Objects;
import java.util.function.Consumer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * Tools > Code With Me settings page in Lumina IDE matching 1:1 design of the reference IDE.
 */
public class SettingsToolsCodeWithMePage extends VBox {

    private final CodeWithMeSettingsManager manager;
    private CodeWithMeSettings initialSettings;
    private Runnable onModifiedListener;
    private boolean updating = false;

    private TextField userNameField;
    private Button useSystemNameButton;
    private TextField lobbyServerUrlField;

    private Runnable onNavigateToGuestColors;

    public SettingsToolsCodeWithMePage() {
        this(null);
    }

    public SettingsToolsCodeWithMePage(Runnable onNavigateToGuestColors) {
        this.manager = CodeWithMeSettingsManager.getInstance();
        this.onNavigateToGuestColors = onNavigateToGuestColors;
        buildUI();
        loadData();
    }

    public void setOnNavigateToGuestColors(Runnable onNavigateToGuestColors) {
        this.onNavigateToGuestColors = onNavigateToGuestColors;
    }

    private void buildUI() {
        setPadding(new Insets(20, 24, 20, 24));
        setSpacing(14);
        setStyle("-fx-background-color: #1E1F22;");

        final double labelWidth = 130;

        // 1. User name row
        VBox userNameContainer = new VBox(4);
        HBox userNameRow = new HBox(8);
        userNameRow.setAlignment(Pos.CENTER_LEFT);

        Label userLabel = new Label("User name:");
        userLabel.setMinWidth(labelWidth);
        userLabel.setPrefWidth(labelWidth);
        userLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        userNameField = new TextField();
        userNameField.setPrefWidth(520);
        userNameField.setStyle(
                "-fx-background-color: #2B2D30; " +
                "-fx-border-color: #393B40; " +
                "-fx-border-radius: 4px; " +
                "-fx-background-radius: 4px; " +
                "-fx-text-fill: #DFE1E5; " +
                "-fx-font-size: 13px; " +
                "-fx-padding: 4 8 4 8;"
        );
        userNameField.textProperty().addListener((obs, oldVal, newVal) -> notifyModified());

        useSystemNameButton = new Button("Use System Name");
        useSystemNameButton.setStyle(
                "-fx-background-color: #2B2D30; " +
                "-fx-border-color: #393B40; " +
                "-fx-border-radius: 4px; " +
                "-fx-background-radius: 4px; " +
                "-fx-text-fill: #DFE1E5; " +
                "-fx-font-size: 13px; " +
                "-fx-padding: 4 10 4 10;"
        );
        useSystemNameButton.setOnAction(e -> {
            userNameField.setText(manager.getSystemUserName());
            notifyModified();
        });

        userNameRow.getChildren().addAll(userLabel, userNameField, useSystemNameButton);

        Label sessionSubtext = new Label("New user name will be applied for all new sessions after restart");
        sessionSubtext.setStyle("-fx-text-fill: #8C8C8C; -fx-font-size: 12px; -fx-padding: 2 0 0 " + (labelWidth + 8) + ";");

        Hyperlink cursorColorsLink = new Hyperlink("Change guests' cursor colors");
        cursorColorsLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-padding: 0 0 0 " + (labelWidth + 8) + "; -fx-underline: false;");
        cursorColorsLink.setOnMouseEntered(e -> cursorColorsLink.setStyle("-fx-text-fill: #70AAFF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-padding: 0 0 0 " + (labelWidth + 8) + "; -fx-underline: true;"));
        cursorColorsLink.setOnMouseExited(e -> cursorColorsLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-padding: 0 0 0 " + (labelWidth + 8) + "; -fx-underline: false;"));
        cursorColorsLink.setOnAction(e -> {
            if (onNavigateToGuestColors != null) {
                onNavigateToGuestColors.run();
            }
        });

        userNameContainer.getChildren().addAll(userNameRow, sessionSubtext, cursorColorsLink);

        // 2. Lobby server URL row
        HBox lobbyRow = new HBox(8);
        lobbyRow.setAlignment(Pos.CENTER_LEFT);
        VBox.setMargin(lobbyRow, new Insets(10, 0, 0, 0));

        Label lobbyLabel = new Label("Lobby server URL:");
        lobbyLabel.setMinWidth(labelWidth);
        lobbyLabel.setPrefWidth(labelWidth);
        lobbyLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        lobbyServerUrlField = new TextField();
        lobbyServerUrlField.setPrefWidth(650);
        lobbyServerUrlField.setPromptText("Leave this field blank to use the nearest Lumina server");
        lobbyServerUrlField.setStyle(
                "-fx-background-color: #2B2D30; " +
                "-fx-border-color: #393B40; " +
                "-fx-border-radius: 4px; " +
                "-fx-background-radius: 4px; " +
                "-fx-text-fill: #DFE1E5; " +
                "-fx-prompt-text-fill: #6F737A; " +
                "-fx-font-size: 13px; " +
                "-fx-padding: 4 8 4 8;"
        );
        lobbyServerUrlField.textProperty().addListener((obs, oldVal, newVal) -> notifyModified());

        lobbyRow.getChildren().addAll(lobbyLabel, lobbyServerUrlField);

        // 3. Informational collaborative description
        Label descLabel = new Label("Work remotely with your teammates on a shared project. Invite them into your collaborative development session, edit code together, follow each other, and see everyone's changes appear in real time.");
        descLabel.setWrapText(true);
        descLabel.setMaxWidth(850);
        descLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-line-spacing: 3px; -fx-padding: 16 0 0 0;");

        // 4. Explore link
        Hyperlink exploreLink = new Hyperlink("Explore Code With Me");
        exploreLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 13px; -fx-border-color: transparent; -fx-padding: 2 0 0 0; -fx-underline: false;");
        exploreLink.setOnMouseEntered(e -> exploreLink.setStyle("-fx-text-fill: #70AAFF; -fx-font-size: 13px; -fx-border-color: transparent; -fx-padding: 2 0 0 0; -fx-underline: true;"));
        exploreLink.setOnMouseExited(e -> exploreLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 13px; -fx-border-color: transparent; -fx-padding: 2 0 0 0; -fx-underline: false;"));

        getChildren().addAll(userNameContainer, lobbyRow, descLabel, exploreLink);
    }

    private void loadData() {
        updating = true;
        CodeWithMeSettings s = manager.getSettings();
        userNameField.setText(s.getUserName());
        lobbyServerUrlField.setText(s.getLobbyServerUrl());
        initialSettings = getCurrentSettingsFromUI();
        updating = false;
    }

    public CodeWithMeSettings getCurrentSettingsFromUI() {
        CodeWithMeSettings s = new CodeWithMeSettings();
        s.setUserName(userNameField.getText());
        s.setLobbyServerUrl(lobbyServerUrlField.getText());
        return s;
    }

    public boolean isModified() {
        if (initialSettings == null) return false;
        return !Objects.equals(initialSettings, getCurrentSettingsFromUI());
    }

    public void apply() {
        CodeWithMeSettings updated = getCurrentSettingsFromUI();
        manager.setSettings(updated);
        initialSettings = updated.clone();
        notifyModified();
    }

    public void reset() {
        loadData();
        notifyModified();
    }

    public void revertChanges() {
        reset();
    }

    public void setOnModifiedListener(Runnable listener) {
        this.onModifiedListener = listener;
    }

    private void notifyModified() {
        if (!updating && onModifiedListener != null) {
            onModifiedListener.run();
        }
    }

    public TextField getUserNameField() {
        return userNameField;
    }

    public Button getUseSystemNameButton() {
        return useSystemNameButton;
    }

    public TextField getLobbyServerUrlField() {
        return lobbyServerUrlField;
    }
}
