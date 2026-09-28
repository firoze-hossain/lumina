package dev.lumina.ui;

import dev.lumina.settings.CodeStyleSettings;
import dev.lumina.settings.CodeStyleSettings.CodeStyleScheme;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.stage.FileChooser;

import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Common Scheme Header bar and warning banner for all Editor > Code Style pages.
 * Matches 1:1 with reference screenshots:
 * - media_1790561994663.png / media_1790562004310.png:
 *   Scheme: [ ComboBox (Stored in Project / Stored in IDE) ] [ ⚙ Gear button ]
 *   ⚠ Settings may be overridden by Indents Detection Disable
 */
public class CodeStyleHeaderBar extends VBox {

    private final HBox schemeRow = new HBox(10);
    private final ComboBox<String> schemeCombo = new ComboBox<>();
    private final Button gearButton = new Button();
    private final ContextMenu actionsMenu = new ContextMenu();
    private final MenuItem duplicateItem = new MenuItem("Duplicate...");
    private final MenuItem renameItem = new MenuItem("Rename...");
    private final MenuItem restoreDefaultsItem = new MenuItem("Restore Defaults");
    private final Menu exportMenu = new Menu("Export");
    private final MenuItem exportXmlItem = new MenuItem("Code Style XML (.xml)");
    private final MenuItem exportEditorConfigItem = new MenuItem("EditorConfig (.editorconfig)");
    private final MenuItem importItem = new MenuItem("Import Scheme...");

    // Warning Banner
    private final HBox warningBox = new HBox(8);
    private final Label warningLabel = new Label("Settings may be overridden by Indents Detection");
    private final Hyperlink disableIndentsLink = new Hyperlink("Disable");

    private Consumer<CodeStyleScheme> onSchemeChanged;
    private Runnable onSettingsModified;
    private boolean suppressEvents = false;

    public CodeStyleHeaderBar() {
        setSpacing(8);
        setPadding(new Insets(2, 0, 8, 0));

        buildUi();
        setupListeners();
        refreshSchemes();
    }

    private void buildUi() {
        // --- 1. Scheme Row ---
        schemeRow.setAlignment(Pos.CENTER_LEFT);

        Label schemeLabel = new Label("Scheme:");
        schemeLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        schemeCombo.setPrefWidth(220);
        schemeCombo.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4; -fx-font-size: 12px;");

        // Custom Cell Factory for Grouped Categories ("Stored in Project", "Stored in IDE")
        schemeCombo.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("-fx-background-color: #2B2D30;");
                } else if (item.startsWith("---HEADER---")) {
                    String title = item.substring("---HEADER---".length());
                    Label lbl = new Label(title);
                    lbl.setStyle("-fx-text-fill: #848BA3; -fx-font-size: 11px; -fx-padding: 4 6 2 6; -fx-font-weight: bold;");
                    setText(null);
                    setGraphic(lbl);
                    setStyle("-fx-background-color: #2B2D30;");
                    setDisable(true);
                } else {
                    setText(item);
                    setGraphic(null);
                    setStyle("-fx-text-fill: #DFE1E5; -fx-background-color: #2B2D30; -fx-padding: 3 12 3 16;");
                    setDisable(false);
                }
            }
        });

        // Gear icon button for Scheme Actions
        SVGPath gearSvg = new SVGPath();
        gearSvg.setContent("M12 8a4 4 0 100 8 4 4 0 000-8zm-1.5 9.4A6.002 6.002 0 016.6 13.5l-1.9.4a1 1 0 01-1.1-.7l-1-2.4a1 1 0 01.3-1.2l1.6-1.1a5.96 5.96 0 010-2l-1.6-1.1a1 1 0 01-.3-1.2l1-2.4a1 1 0 011.1-.7l1.9.4a6.002 6.002 0 013.9-3.9l-.4-1.9a1 1 0 01.7-1.1l2.4-1a1 1 0 011.2.3l1.1 1.6a5.96 5.96 0 012 0l1.1-1.6a1 1 0 011.2-.3l2.4 1a1 1 0 01.7 1.1l-.4 1.9a6.002 6.002 0 013.9 3.9l1.9-.4a1 1 0 011.1.7l1 2.4a1 1 0 01-.3 1.2l-1.6 1.1a5.96 5.96 0 010 2l1.6 1.1a1 1 0 01.3 1.2l-1 2.4a1 1 0 01-1.1.7l-1.9-.4a6.002 6.002 0 01-3.9 3.9l.4 1.9a1 1 0 01-.7 1.1l-2.4 1a1 1 0 01-1.2-.3l-1.1-1.6a5.96 5.96 0 01-2 0l-1.1 1.6a1 1 0 01-1.2.3l-2.4-1a1 1 0 01-.7-1.1l.4-1.9z");
        gearSvg.setFill(Color.web("#848BA3"));
        gearSvg.setScaleX(0.7);
        gearSvg.setScaleY(0.7);

        gearButton.setGraphic(gearSvg);
        gearButton.setTooltip(new Tooltip("Show Scheme Actions"));
        gearButton.setStyle("-fx-background-color: transparent; -fx-padding: 3 6 3 6; -fx-cursor: hand;");

        exportMenu.getItems().addAll(exportXmlItem, exportEditorConfigItem);
        actionsMenu.getItems().addAll(duplicateItem, renameItem, restoreDefaultsItem, exportMenu, importItem);
        actionsMenu.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #393B40;");

        schemeRow.getChildren().addAll(schemeLabel, schemeCombo, gearButton);

        // --- 2. Warning Banner ---
        warningBox.setAlignment(Pos.CENTER_LEFT);
        warningBox.setPadding(new Insets(4, 0, 4, 0));

        Label warnIcon = new Label("⚠");
        warnIcon.setStyle("-fx-text-fill: #EDA200; -fx-font-size: 14px; -fx-font-weight: bold;");

        warningLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");

        disableIndentsLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-padding: 0; -fx-border-color: transparent; -fx-underline: false;");
        disableIndentsLink.setOnMouseEntered(e -> disableIndentsLink.setUnderline(true));
        disableIndentsLink.setOnMouseExited(e -> disableIndentsLink.setUnderline(false));

        warningBox.getChildren().addAll(warnIcon, warningLabel, disableIndentsLink);

        getChildren().addAll(schemeRow, warningBox);
        updateWarningBanner();
    }

    public void addRightAction(Node node) {
        if (node != null) {
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            schemeRow.getChildren().addAll(spacer, node);
        }
    }

    private void setupListeners() {
        schemeCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (!suppressEvents && newVal != null && !newVal.startsWith("---HEADER---")) {
                CodeStyleSettings.getInstance().setActiveSchemeName(newVal);
                updateWarningBanner();
                if (onSchemeChanged != null) {
                    onSchemeChanged.accept(CodeStyleSettings.getInstance().getActiveScheme());
                }
            }
        });

        gearButton.setOnAction(e -> {
            boolean isCustom = !"Default".equals(schemeCombo.getValue()) && !"Project".equals(schemeCombo.getValue());
            renameItem.setDisable(!isCustom);
            actionsMenu.show(gearButton, Side.BOTTOM, 0, 2);
        });

        duplicateItem.setOnAction(e -> handleDuplicateScheme());
        renameItem.setOnAction(e -> handleRenameScheme());
        restoreDefaultsItem.setOnAction(e -> {
            CodeStyleSettings.getInstance().restoreDefaults();
            refreshSchemes();
            if (onSettingsModified != null) onSettingsModified.run();
        });

        exportXmlItem.setOnAction(e -> handleExportXml());
        exportEditorConfigItem.setOnAction(e -> handleExportEditorConfig());
        importItem.setOnAction(e -> handleImportScheme());

        disableIndentsLink.setOnAction(e -> {
            CodeStyleScheme active = CodeStyleSettings.getInstance().getActiveScheme();
            if (active != null) {
                boolean current = active.isDetectAndUseExistingFileIndents();
                active.setDetectAndUseExistingFileIndents(!current);
                CodeStyleSettings.getInstance().saveSettings();
                updateWarningBanner();
                if (onSettingsModified != null) onSettingsModified.run();
            }
        });
    }

    public void updateWarningBanner() {
        CodeStyleScheme active = CodeStyleSettings.getInstance().getActiveScheme();
        if (active != null && active.isDetectAndUseExistingFileIndents()) {
            warningBox.setVisible(true);
            warningBox.setManaged(true);
            disableIndentsLink.setText("Disable");
        } else {
            warningBox.setVisible(false);
            warningBox.setManaged(false);
        }
    }

    public void refreshSchemes() {
        suppressEvents = true;
        try {
            schemeCombo.getItems().clear();
            schemeCombo.getItems().add("---HEADER---Stored in Project");
            schemeCombo.getItems().add("Project");

            schemeCombo.getItems().add("---HEADER---Stored in IDE");
            schemeCombo.getItems().add("Default");

            for (CodeStyleScheme s : CodeStyleSettings.getInstance().getSchemes()) {
                if (!"Project".equals(s.getName()) && !"Default".equals(s.getName())) {
                    schemeCombo.getItems().add(s.getName());
                }
            }

            CodeStyleScheme active = CodeStyleSettings.getInstance().getActiveScheme();
            if (active != null) {
                schemeCombo.setValue(active.getName());
            } else {
                schemeCombo.setValue("Default");
            }
            updateWarningBanner();
        } finally {
            suppressEvents = false;
        }
    }

    private void handleDuplicateScheme() {
        String current = schemeCombo.getValue();
        if (current == null || current.startsWith("---HEADER---")) current = "Default";

        TextInputDialog dialog = new TextInputDialog(current + " copy");
        dialog.setTitle("Duplicate Scheme");
        dialog.setHeaderText("Enter a new scheme name:");
        dialog.setContentText("Name:");
        dialog.getDialogPane().setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5;");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(name -> {
            if (!name.isBlank()) {
                CodeStyleSettings.getInstance().duplicateScheme(schemeCombo.getValue(), name.trim());
                refreshSchemes();
                if (onSchemeChanged != null) {
                    onSchemeChanged.accept(CodeStyleSettings.getInstance().getActiveScheme());
                }
                if (onSettingsModified != null) onSettingsModified.run();
            }
        });
    }

    private void handleRenameScheme() {
        String current = schemeCombo.getValue();
        if ("Default".equals(current) || "Project".equals(current)) return;

        TextInputDialog dialog = new TextInputDialog(current);
        dialog.setTitle("Rename Scheme");
        dialog.setHeaderText("Enter a new scheme name:");
        dialog.setContentText("Name:");
        dialog.getDialogPane().setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5;");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(name -> {
            if (!name.isBlank() && !name.equals(current)) {
                CodeStyleScheme s = CodeStyleSettings.getInstance().getScheme(current);
                if (s != null) {
                    s.setName(name.trim());
                    CodeStyleSettings.getInstance().removeScheme(current);
                    CodeStyleSettings.getInstance().getSchemes().add(s);
                    CodeStyleSettings.getInstance().setActiveSchemeName(name.trim());
                    refreshSchemes();
                    if (onSchemeChanged != null) onSchemeChanged.accept(s);
                    if (onSettingsModified != null) onSettingsModified.run();
                }
            }
        });
    }

    private void handleExportXml() {
        CodeStyleScheme active = CodeStyleSettings.getInstance().getActiveScheme();
        if (active == null) return;

        FileChooser fc = new FileChooser();
        fc.setTitle("Export Code Style Scheme");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("XML Files (*.xml)", "*.xml"));
        fc.setInitialFileName(active.getName() + ".xml");
        File file = fc.showSaveDialog(getScene() != null ? getScene().getWindow() : null);
        if (file != null) {
            try {
                String xml = CodeStyleSettings.getInstance().exportToXml(active);
                Files.writeString(file.toPath(), xml);
            } catch (Exception ex) {
                showError("Export Failed", ex.getMessage());
            }
        }
    }

    private void handleExportEditorConfig() {
        CodeStyleScheme active = CodeStyleSettings.getInstance().getActiveScheme();
        if (active == null) return;

        FileChooser fc = new FileChooser();
        fc.setTitle("Export .editorconfig");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("EditorConfig Files (*.editorconfig)", "*.editorconfig"));
        fc.setInitialFileName(".editorconfig");
        File file = fc.showSaveDialog(getScene() != null ? getScene().getWindow() : null);
        if (file != null) {
            try {
                String ec = CodeStyleSettings.getInstance().exportToEditorConfig(active);
                Files.writeString(file.toPath(), ec);
            } catch (Exception ex) {
                showError("Export Failed", ex.getMessage());
            }
        }
    }

    private void handleImportScheme() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Import Code Style Scheme");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("XML Files (*.xml)", "*.xml"));
        File file = fc.showOpenDialog(getScene() != null ? getScene().getWindow() : null);
        if (file != null) {
            try {
                String content = Files.readString(file.toPath());
                String schemeName = file.getName().replace(".xml", "");
                CodeStyleScheme imported = CodeStyleSettings.getInstance().duplicateScheme("Default", schemeName);
                refreshSchemes();
                if (onSchemeChanged != null) onSchemeChanged.accept(imported);
                if (onSettingsModified != null) onSettingsModified.run();
            } catch (Exception ex) {
                showError("Import Failed", ex.getMessage());
            }
        }
    }

    private void showError(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        alert.setTitle(title);
        alert.showAndWait();
    }

    public void setOnSchemeChanged(Consumer<CodeStyleScheme> onSchemeChanged) {
        this.onSchemeChanged = onSchemeChanged;
    }

    public void setOnSettingsModified(Runnable onSettingsModified) {
        this.onSettingsModified = onSettingsModified;
    }

    public ComboBox<String> getSchemeCombo() {
        return schemeCombo;
    }
}
