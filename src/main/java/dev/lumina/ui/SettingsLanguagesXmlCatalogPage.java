package dev.lumina.ui;

import dev.lumina.schemas.XmlCatalogSettings;
import dev.lumina.schemas.XmlCatalogSettingsManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.Objects;

/**
 * Settings page for Languages & Frameworks > Schemas and DTDs > XML Catalog.
 * Matches reference screenshot media_1791600352140_3eefd9d9.png:
 *  - Catalog property file: [ TextField with 📁 browse button ]
 */
public class SettingsLanguagesXmlCatalogPage extends VBox {

    private final XmlCatalogSettingsManager manager = XmlCatalogSettingsManager.getInstance();

    private final TextField catalogPropertyField = new TextField();
    private final Button browseBtn = new Button("📁");

    private XmlCatalogSettings initialSettings;
    private Runnable onModifiedListener;
    private boolean updating = false;

    public SettingsLanguagesXmlCatalogPage() {
        setSpacing(14);
        setPadding(new Insets(16, 24, 20, 24));
        setStyle("-fx-background-color: #1E1F22;");
        VBox.setVgrow(this, Priority.ALWAYS);

        buildUI();
        loadData();
    }

    private void buildUI() {
        Label catalogLabel = new Label("Catalog property file:");
        catalogLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        catalogLabel.setPrefWidth(140);
        catalogLabel.setMinWidth(140);

        catalogPropertyField.setPromptText("Catalog property file");
        catalogPropertyField.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-prompt-text-fill: #707278; " +
                "-fx-border-color: #43454A; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 5 8; -fx-font-size: 13px;");
        HBox.setHgrow(catalogPropertyField, Priority.ALWAYS);
        catalogPropertyField.textProperty().addListener((obs, oldV, newV) -> fireModified());

        browseBtn.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #8C8C8C; -fx-border-color: #43454A; " +
                "-fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 4 8; -fx-cursor: hand; -fx-font-size: 12px;");
        browseBtn.setTooltip(new Tooltip("Browse for XML catalog property file"));
        browseBtn.setOnAction(e -> chooseCatalogFile());

        HBox inputRow = new HBox(6, catalogPropertyField, browseBtn);
        inputRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(inputRow, Priority.ALWAYS);

        HBox row = new HBox(12, catalogLabel, inputRow);
        row.setAlignment(Pos.CENTER_LEFT);

        getChildren().add(row);
    }

    private void chooseCatalogFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select XML Catalog File");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Catalog Files (*.xml, *.properties, *.dtd)", "*.xml", "*.properties", "*.dtd"),
                new FileChooser.ExtensionFilter("All Files (*.*)", "*.*")
        );

        String current = catalogPropertyField.getText();
        if (current != null && !current.isBlank()) {
            File f = new File(current);
            File parent = f.isDirectory() ? f : f.getParentFile();
            if (parent != null && parent.isDirectory()) {
                chooser.setInitialDirectory(parent);
            }
        } else {
            File userDir = new File(System.getProperty("user.dir", "."));
            if (userDir.isDirectory()) {
                chooser.setInitialDirectory(userDir);
            }
        }

        File selected = chooser.showOpenDialog(getScene() != null ? getScene().getWindow() : null);
        if (selected != null) {
            catalogPropertyField.setText(selected.getAbsolutePath());
            fireModified();
        }
    }

    public void loadData() {
        updating = true;
        initialSettings = manager.getSettings();
        catalogPropertyField.setText(initialSettings.getCatalogPropertyFile());
        updating = false;
    }

    public XmlCatalogSettings getCurrentSettings() {
        XmlCatalogSettings s = new XmlCatalogSettings();
        s.setCatalogPropertyFile(catalogPropertyField.getText());
        return s;
    }

    public boolean isModified() {
        if (initialSettings == null) return false;
        return !Objects.equals(initialSettings, getCurrentSettings());
    }

    public void apply() {
        XmlCatalogSettings current = getCurrentSettings();
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

    public TextField getCatalogPropertyField() {
        return catalogPropertyField;
    }

    public Button getBrowseBtn() {
        return browseBtn;
    }
}
