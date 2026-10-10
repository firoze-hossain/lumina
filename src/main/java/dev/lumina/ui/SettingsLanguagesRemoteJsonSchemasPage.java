package dev.lumina.ui;

import dev.lumina.schemas.RemoteJsonSchemasSettings;
import dev.lumina.schemas.RemoteJsonSchemasSettingsManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.awt.Desktop;
import java.net.URI;
import java.util.Objects;

/**
 * Settings page for Languages & Frameworks > Schemas and DTDs > Remote JSON Schemas.
 * Matches reference screenshot media_1791600340922_4fe71f33.png:
 *  - [x] Allow downloading JSON Schemas from remote sources
 *    - [x] Use schemastore.org JSON Schema catalog
 *      Schemas will be downloaded and assigned using the SchemaStore API ↗
 *    - [ ] Always download the most recent version of schemas
 *      Schemas will always be downloaded from the SchemaStore, even if some of them are bundled with the IDE
 */
public class SettingsLanguagesRemoteJsonSchemasPage extends VBox {

    private final RemoteJsonSchemasSettingsManager manager = RemoteJsonSchemasSettingsManager.getInstance();

    private final CheckBox allowDownloadCheck = new CheckBox("Allow downloading JSON Schemas from remote sources");
    private final CheckBox useSchemaStoreCheck = new CheckBox("Use schemastore.org JSON Schema catalog");
    private final Hyperlink schemaStoreApiLink = new Hyperlink("SchemaStore API ↗");
    private final CheckBox alwaysDownloadMostRecentCheck = new CheckBox("Always download the most recent version of schemas");

    private final VBox subOptionsBox = new VBox(10);

    private RemoteJsonSchemasSettings initialSettings;
    private Runnable onModifiedListener;
    private boolean updating = false;

    public SettingsLanguagesRemoteJsonSchemasPage() {
        setSpacing(14);
        setPadding(new Insets(16, 24, 20, 24));
        setStyle("-fx-background-color: #1E1F22;");
        VBox.setVgrow(this, Priority.ALWAYS);

        buildUI();
        loadData();
    }

    private void buildUI() {
        styleCheckBox(allowDownloadCheck);
        allowDownloadCheck.selectedProperty().addListener((obs, oldV, newV) -> {
            updateEnabledStates();
            fireModified();
        });

        styleCheckBox(useSchemaStoreCheck);
        useSchemaStoreCheck.selectedProperty().addListener((obs, oldV, newV) -> fireModified());

        Label schemaStoreApiDesc = new Label("Schemas will be downloaded and assigned using the ");
        schemaStoreApiDesc.setStyle("-fx-text-fill: #8C8C8C; -fx-font-size: 11px;");

        schemaStoreApiLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 11px; -fx-border-color: transparent; -fx-padding: 0;");
        schemaStoreApiLink.setOnAction(e -> {
            try {
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    Desktop.getDesktop().browse(new URI("https://www.schemastore.org/api/json/catalog.json"));
                }
            } catch (Exception ignored) {}
        });

        HBox schemaStoreDescRow = new HBox(0, schemaStoreApiDesc, schemaStoreApiLink);
        schemaStoreDescRow.setAlignment(Pos.CENTER_LEFT);
        schemaStoreDescRow.setPadding(new Insets(0, 0, 4, 22));

        VBox schemaStoreGroup = new VBox(2, useSchemaStoreCheck, schemaStoreDescRow);

        styleCheckBox(alwaysDownloadMostRecentCheck);
        alwaysDownloadMostRecentCheck.selectedProperty().addListener((obs, oldV, newV) -> fireModified());

        Label alwaysDownloadDesc = new Label("Schemas will always be downloaded from the SchemaStore, even if some of them are bundled with the IDE");
        alwaysDownloadDesc.setStyle("-fx-text-fill: #8C8C8C; -fx-font-size: 11px; -fx-padding: 0 0 0 22;");

        VBox alwaysDownloadGroup = new VBox(2, alwaysDownloadMostRecentCheck, alwaysDownloadDesc);

        subOptionsBox.setPadding(new Insets(2, 0, 0, 18));
        subOptionsBox.getChildren().addAll(schemaStoreGroup, alwaysDownloadGroup);

        getChildren().addAll(allowDownloadCheck, subOptionsBox);
    }

    private void updateEnabledStates() {
        boolean enabled = allowDownloadCheck.isSelected();
        subOptionsBox.setDisable(!enabled);
    }

    private void styleCheckBox(CheckBox cb) {
        cb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
    }

    public void loadData() {
        updating = true;
        initialSettings = manager.getSettings();

        allowDownloadCheck.setSelected(initialSettings.isAllowDownloadRemoteSchemas());
        useSchemaStoreCheck.setSelected(initialSettings.isUseSchemaStoreCatalog());
        alwaysDownloadMostRecentCheck.setSelected(initialSettings.isAlwaysDownloadMostRecentVersion());

        updateEnabledStates();
        updating = false;
    }

    public RemoteJsonSchemasSettings getCurrentSettings() {
        RemoteJsonSchemasSettings s = new RemoteJsonSchemasSettings();
        s.setAllowDownloadRemoteSchemas(allowDownloadCheck.isSelected());
        s.setUseSchemaStoreCatalog(useSchemaStoreCheck.isSelected());
        s.setAlwaysDownloadMostRecentVersion(alwaysDownloadMostRecentCheck.isSelected());
        if (initialSettings != null) {
            s.setSchemaStoreCatalogUrl(initialSettings.getSchemaStoreCatalogUrl());
        }
        return s;
    }

    public boolean isModified() {
        if (initialSettings == null) return false;
        return !Objects.equals(initialSettings, getCurrentSettings());
    }

    public void apply() {
        RemoteJsonSchemasSettings current = getCurrentSettings();
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

    public CheckBox getAllowDownloadCheck() {
        return allowDownloadCheck;
    }

    public CheckBox getUseSchemaStoreCheck() {
        return useSchemaStoreCheck;
    }

    public CheckBox getAlwaysDownloadMostRecentCheck() {
        return alwaysDownloadMostRecentCheck;
    }

    public Hyperlink getSchemaStoreApiLink() {
        return schemaStoreApiLink;
    }
}
