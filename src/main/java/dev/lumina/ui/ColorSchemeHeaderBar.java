package dev.lumina.ui;

import dev.lumina.settings.EditorColorSchemeSettings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.SVGPath;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Common Scheme Header bar for all Editor > Color Scheme pages.
 * Matches 1:1 with the reference screenshots:
 * Scheme: [ ComboBox ] [Gear icon (Show Scheme Actions)] [Change IDE Theme...] [?]
 */
public class ColorSchemeHeaderBar extends HBox {

    private final ComboBox<String> schemeCombo = new ComboBox<>();
    private final Button gearButton = new Button();
    private final Hyperlink changeThemeLink = new Hyperlink("Change IDE Theme...");
    private final Button helpButton = new Button("?");

    private final ContextMenu actionsMenu = new ContextMenu();
    private final MenuItem duplicateItem = new MenuItem("Duplicate...");
    private final MenuItem restoreDefaultsItem = new MenuItem("Restore Defaults");
    private final Menu exportMenu = new Menu("Export");
    private final MenuItem exportIclsItem = new MenuItem("Editor color scheme (.icls)");
    private final Menu importMenu = new Menu("Import Scheme");
    private final MenuItem importLuminaItem = new MenuItem("Lumina color scheme (.icls) or settings (.jar)");
    private final MenuItem importEclipseItem = new MenuItem("Eclipse color theme (XML)");

    private Consumer<String> onSchemeChanged;
    private Runnable onNavigateToTheme;
    private boolean suppressEvents = false;

    public ColorSchemeHeaderBar() {
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(10);
        setPadding(new Insets(2, 0, 8, 0));

        buildUi();
        setupListeners();
        refreshSchemes();
    }

    private void buildUi() {
        Label schemeLabel = new Label("Scheme:");
        schemeLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        schemeCombo.setPrefWidth(260);
        schemeCombo.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4; -fx-font-size: 12px;");

        // Custom list cell styling with separator before classic themes
        schemeCombo.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else if ("---".equals(item)) {
                    setText(null);
                    Separator sep = new Separator();
                    sep.setStyle("-fx-background-color: #393B40;");
                    setGraphic(sep);
                    setDisable(true);
                } else {
                    setText(item);
                    setStyle("-fx-text-fill: #DFE1E5; -fx-background-color: #2B2D30;");
                    setDisable(false);
                }
            }
        });

        // Gear icon button for Show Scheme Actions
        SVGPath gearSvg = new SVGPath();
        gearSvg.setContent("M12 8a4 4 0 100 8 4 4 0 000-8zm-1.5 9.4A6.002 6.002 0 016.6 13.5l-1.9.4a1 1 0 01-1.1-.7l-1-2.4a1 1 0 01.3-1.2l1.6-1.1a5.96 5.96 0 010-2l-1.6-1.1a1 1 0 01-.3-1.2l1-2.4a1 1 0 011.1-.7l1.9.4a6.002 6.002 0 013.9-3.9l-.4-1.9a1 1 0 01.7-1.1l2.4-1a1 1 0 011.2.3l1.1 1.6a5.96 5.96 0 012 0l1.1-1.6a1 1 0 011.2-.3l2.4 1a1 1 0 01.7 1.1l-.4 1.9a6.002 6.002 0 013.9 3.9l1.9-.4a1 1 0 011.1.7l1 2.4a1 1 0 01-.3 1.2l-1.6 1.1a5.96 5.96 0 010 2l1.6 1.1a1 1 0 01.3 1.2l-1 2.4a1 1 0 01-1.1.7l-1.9-.4a6.002 6.002 0 01-3.9 3.9l.4 1.9a1 1 0 01-.7 1.1l-2.4 1a1 1 0 01-1.2-.3l-1.1-1.6a5.96 5.96 0 01-2 0l-1.1 1.6a1 1 0 01-1.2.3l-2.4-1a1 1 0 01-.7-1.1l.4-1.9z");
        gearSvg.setFill(Color.web("#848BA3"));
        gearSvg.setScaleX(0.7);
        gearSvg.setScaleY(0.7);

        gearButton.setGraphic(gearSvg);
        gearButton.setTooltip(new Tooltip("Show Scheme Actions"));
        gearButton.setStyle("-fx-background-color: transparent; -fx-padding: 3 6 3 6; -fx-cursor: hand;");

        // Build Action Context Menu
        exportMenu.getItems().add(exportIclsItem);
        importMenu.getItems().addAll(importLuminaItem, importEclipseItem);
        actionsMenu.getItems().addAll(duplicateItem, restoreDefaultsItem, exportMenu, importMenu);
        actionsMenu.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #393B40;");

        // Change IDE Theme link
        changeThemeLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-padding: 0; -fx-border-color: transparent; -fx-underline: false;");
        changeThemeLink.setOnMouseEntered(e -> changeThemeLink.setUnderline(true));
        changeThemeLink.setOnMouseExited(e -> changeThemeLink.setUnderline(false));

        // Help circle button (?)
        helpButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #848BA3; -fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 0 4 0 4; -fx-cursor: hand;");
        helpButton.setTooltip(new Tooltip("Help"));

        getChildren().addAll(schemeLabel, schemeCombo, gearButton, changeThemeLink, helpButton);
    }

    private void setupListeners() {
        schemeCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (!suppressEvents && newVal != null && !"---".equals(newVal)) {
                EditorColorSchemeSettings.getInstance().setActiveSchemeName(newVal);
                updateRestoreDefaultsState();
                if (onSchemeChanged != null) {
                    onSchemeChanged.accept(newVal);
                }
            }
        });

        gearButton.setOnAction(e -> {
            updateRestoreDefaultsState();
            actionsMenu.show(gearButton, Side.BOTTOM, 0, 0);
        });

        duplicateItem.setOnAction(e -> handleDuplicateScheme());
        restoreDefaultsItem.setOnAction(e -> handleRestoreDefaults());

        changeThemeLink.setOnAction(e -> {
            if (onNavigateToTheme != null) {
                onNavigateToTheme.run();
            }
        });
    }

    private void updateRestoreDefaultsState() {
        String active = EditorColorSchemeSettings.getInstance().getActiveSchemeName();
        boolean isModified = EditorColorSchemeSettings.getInstance().isSchemeModified(active);
        restoreDefaultsItem.setDisable(!isModified);
    }

    private void handleDuplicateScheme() {
        String current = EditorColorSchemeSettings.getInstance().getActiveSchemeName();
        TextInputDialog dialog = new TextInputDialog(current + " Copy");
        dialog.setTitle("Duplicate Scheme");
        dialog.setHeaderText("Duplicate color scheme '" + current + "'");
        dialog.setContentText("New scheme name:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(name -> {
            String trimmed = name.trim();
            if (!trimmed.isEmpty()) {
                boolean success = EditorColorSchemeSettings.getInstance().duplicateScheme(current, trimmed);
                if (success) {
                    refreshSchemes();
                    schemeCombo.setValue(trimmed);
                } else {
                    Alert alert = new Alert(Alert.AlertType.WARNING, "Scheme name already exists or is invalid.", ButtonType.OK);
                    alert.showAndWait();
                }
            }
        });
    }

    private void handleRestoreDefaults() {
        String active = EditorColorSchemeSettings.getInstance().getActiveSchemeName();
        EditorColorSchemeSettings.getInstance().restoreDefaults(active);
        updateRestoreDefaultsState();
        if (onSchemeChanged != null) {
            onSchemeChanged.accept(active);
        }
    }

    public void refreshSchemes() {
        suppressEvents = true;
        try {
            EditorColorSchemeSettings s = EditorColorSchemeSettings.getInstance();
            schemeCombo.getItems().clear();

            schemeCombo.getItems().add("Islands Dark Theme default");
            schemeCombo.getItems().add("Light");
            schemeCombo.getItems().add("High Contrast");
            schemeCombo.getItems().add("---");
            schemeCombo.getItems().add("Classic Light");
            schemeCombo.getItems().add("Darcula");
            schemeCombo.getItems().add("Darcula Contrast");
            schemeCombo.getItems().add("Dark");

            for (String custom : s.getCustomSchemes()) {
                if (!schemeCombo.getItems().contains(custom)) {
                    schemeCombo.getItems().add(custom);
                }
            }

            schemeCombo.setValue(s.getActiveSchemeName());
            updateRestoreDefaultsState();
        } finally {
            suppressEvents = false;
        }
    }

    public ComboBox<String> getSchemeCombo() {
        return schemeCombo;
    }

    public Button getGearButton() {
        return gearButton;
    }

    public Hyperlink getChangeThemeLink() {
        return changeThemeLink;
    }

    public MenuItem getDuplicateItem() {
        return duplicateItem;
    }

    public MenuItem getRestoreDefaultsItem() {
        return restoreDefaultsItem;
    }

    public void setOnSchemeChanged(Consumer<String> listener) {
        this.onSchemeChanged = listener;
    }

    public void setOnNavigateToTheme(Runnable listener) {
        this.onNavigateToTheme = listener;
    }
}
