package dev.lumina.ui;

import dev.lumina.stylesheets.TailwindCssSettings;
import dev.lumina.stylesheets.TailwindCssSettingsManager;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;

import java.awt.Desktop;
import java.io.File;
import java.net.URI;
import java.util.Objects;

/**
 * Settings page for Languages & Frameworks > Style Sheets > Tailwind CSS.
 * Matches reference screenshots media_1791600486101_c3595d17.png & media_1791600502988_86d834fe.png:
 *  - Language Server: [ @tailwindcss/language-server (Default)  0.14.22  v ] [ ... ]
 *  - Configuration:                                          See available options ↗
 *  - Full JSON Configuration Code Editor
 */
public class SettingsLanguagesStyleSheetsTailwindPage extends VBox {

    private final TailwindCssSettingsManager manager = TailwindCssSettingsManager.getInstance();

    private final ComboBox<String> languageServerCombo = new ComboBox<>();
    private final Button browseServerBtn = new Button("...");

    private final Hyperlink seeOptionsLink = new Hyperlink("See available options ↗");
    private final TextArea configurationEditor = new TextArea();

    private TailwindCssSettings initialSettings;
    private Runnable onModifiedListener;
    private boolean updating = false;

    public SettingsLanguagesStyleSheetsTailwindPage() {
        setSpacing(10);
        setPadding(new Insets(14, 20, 16, 20));
        setStyle("-fx-background-color: #1E1F22; -fx-font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;");
        VBox.setVgrow(this, Priority.ALWAYS);

        buildUI();
        loadData();
    }

    private void buildUI() {
        // --- 1. Language Server row ---
        Label serverLabel = new Label("Language Server:");
        serverLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        serverLabel.setPrefWidth(120);

        languageServerCombo.setItems(FXCollections.observableArrayList(
                "@tailwindcss/language-server (Default)",
                "Select..."
        ));
        languageServerCombo.setValue("@tailwindcss/language-server (Default)");
        languageServerCombo.setPrefWidth(380);
        languageServerCombo.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #43454A; -fx-border-radius: 4; " +
                "-fx-background-radius: 4; -fx-font-size: 13px; -fx-text-fill: #DFE1E5;");

        // Custom Cell to display server name on left and version on right
        languageServerCombo.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else if (item.startsWith("@tailwindcss/language-server")) {
                    HBox box = new HBox();
                    box.setAlignment(Pos.CENTER_LEFT);
                    Label nameLbl = new Label(item);
                    nameLbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

                    Region spacer = new Region();
                    HBox.setHgrow(spacer, Priority.ALWAYS);

                    Label verLbl = new Label(TailwindCssSettings.DEFAULT_VERSION);
                    verLbl.setStyle("-fx-text-fill: #8C8C8C; -fx-font-size: 12px; -fx-padding: 0 4 0 8;");

                    box.getChildren().addAll(nameLbl, spacer, verLbl);
                    setGraphic(box);
                    setText(null);
                } else {
                    setText(item);
                    setGraphic(null);
                    setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
                }
            }
        });

        languageServerCombo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else if (item.startsWith("@tailwindcss/language-server")) {
                    HBox box = new HBox();
                    box.setAlignment(Pos.CENTER_LEFT);
                    Label nameLbl = new Label(item);
                    nameLbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

                    Region spacer = new Region();
                    HBox.setHgrow(spacer, Priority.ALWAYS);

                    Label verLbl = new Label(TailwindCssSettings.DEFAULT_VERSION);
                    verLbl.setStyle("-fx-text-fill: #8C8C8C; -fx-font-size: 12px; -fx-padding: 0 4 0 8;");

                    box.getChildren().addAll(nameLbl, spacer, verLbl);
                    setGraphic(box);
                    setText(null);
                } else {
                    setText(item);
                    setGraphic(null);
                    setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
                }
            }
        });

        languageServerCombo.setOnAction(e -> {
            String selected = languageServerCombo.getValue();
            if ("Select...".equals(selected)) {
                chooseServerDir();
            } else {
                fireModified();
            }
        });

        browseServerBtn.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #8C8C8C; -fx-border-color: #43454A; " +
                "-fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 4 8; -fx-cursor: hand; -fx-font-size: 12px;");
        browseServerBtn.setOnAction(e -> chooseServerDir());

        HBox serverRow = new HBox(12, serverLabel, languageServerCombo, browseServerBtn);
        serverRow.setAlignment(Pos.CENTER_LEFT);

        // --- 2. Configuration header row ---
        Label configLabel = new Label("Configuration:");
        configLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        seeOptionsLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-padding: 0;");
        seeOptionsLink.setOnAction(e -> {
            try {
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    Desktop.getDesktop().browse(new URI("https://github.com/tailwindlabs/tailwindcss-intellisense"));
                }
            } catch (Exception ignored) {}
        });

        HBox configHeaderRow = new HBox(8, configLabel, spacer, seeOptionsLink);
        configHeaderRow.setAlignment(Pos.CENTER_LEFT);
        configHeaderRow.setPadding(new Insets(6, 0, 0, 0));

        // --- 3. JSON Configuration Editor ---
        configurationEditor.setStyle(
                "-fx-control-inner-background: #1E1F22; " +
                "-fx-background-color: #1E1F22; " +
                "-fx-text-fill: #DFE1E5; " +
                "-fx-font-family: 'Menlo', Monaco, Consolas, 'Courier New', monospace; " +
                "-fx-font-size: 13px; " +
                "-fx-border-color: #393B40; " +
                "-fx-border-radius: 3; " +
                "-fx-background-radius: 3; " +
                "-fx-highlight-fill: #264F78; " +
                "-fx-highlight-text-fill: #FFFFFF;"
        );
        configurationEditor.setWrapText(false);
        VBox.setVgrow(configurationEditor, Priority.ALWAYS);

        configurationEditor.textProperty().addListener((obs, oldV, newV) -> fireModified());

        getChildren().addAll(serverRow, configHeaderRow, configurationEditor);
    }

    private void chooseServerDir() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select Tailwind Language Server Package");
        File selected = chooser.showDialog(getScene() != null ? getScene().getWindow() : null);
        if (selected != null) {
            String path = selected.getAbsolutePath();
            if (!languageServerCombo.getItems().contains(path)) {
                languageServerCombo.getItems().add(path);
            }
            languageServerCombo.setValue(path);
            fireModified();
        } else {
            // Revert back if cancelled
            if ("Select...".equals(languageServerCombo.getValue())) {
                languageServerCombo.setValue(initialSettings != null ? initialSettings.getLanguageServer() : TailwindCssSettings.DEFAULT_SERVER);
            }
        }
    }

    public void loadData() {
        updating = true;
        initialSettings = manager.getSettings();

        String server = initialSettings.getLanguageServer();
        if (server != null && !server.isBlank()) {
            if (!languageServerCombo.getItems().contains(server)) {
                languageServerCombo.getItems().add(server);
            }
            languageServerCombo.setValue(server);
        } else {
            languageServerCombo.setValue(TailwindCssSettings.DEFAULT_SERVER);
        }

        configurationEditor.setText(initialSettings.getConfigurationJson());
        updating = false;
    }

    public TailwindCssSettings getCurrentSettings() {
        TailwindCssSettings s = new TailwindCssSettings();
        String server = languageServerCombo.getValue();
        s.setLanguageServer(server != null ? server : TailwindCssSettings.DEFAULT_SERVER);
        s.setConfigurationJson(configurationEditor.getText());
        if (initialSettings != null) {
            s.setLanguageServerVersion(initialSettings.getLanguageServerVersion());
            s.setCustomServerPath(initialSettings.getCustomServerPath());
        }
        return s;
    }

    public boolean isModified() {
        if (initialSettings == null) return false;
        return !Objects.equals(initialSettings, getCurrentSettings());
    }

    public void apply() {
        TailwindCssSettings current = getCurrentSettings();
        manager.setSettings(current);
        initialSettings = current.clone();
        if (onModifiedListener != null) onModifiedListener.run();
    }

    public void reset() {
        loadData();
        if (onModifiedListener != null) onModifiedListener.run();
    }

    public void revertChanges() {
        reset();
    }

    public void setOnModifiedListener(Runnable listener) {
        this.onModifiedListener = listener;
    }

    private void fireModified() {
        if (!updating && onModifiedListener != null) {
            onModifiedListener.run();
        }
    }

    // Getters for programmatic inspection and testing
    public ComboBox<String> getLanguageServerCombo() {
        return languageServerCombo;
    }

    public Button getBrowseServerBtn() {
        return browseServerBtn;
    }

    public Hyperlink getSeeOptionsLink() {
        return seeOptionsLink;
    }

    public TextArea getConfigurationEditor() {
        return configurationEditor;
    }
}
