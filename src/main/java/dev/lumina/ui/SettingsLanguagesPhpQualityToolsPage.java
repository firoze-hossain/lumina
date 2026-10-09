package dev.lumina.ui;

import dev.lumina.php.PhpQualityToolsSettings;
import dev.lumina.php.PhpSettingsManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.Objects;

/**
 * Settings page for Languages & Frameworks > PHP > Quality Tools.
 * Faithfully matches Image 2.
 */
public class SettingsLanguagesPhpQualityToolsPage extends VBox {

    private final PhpSettingsManager manager = PhpSettingsManager.getInstance();

    private RadioButton phpcbfRadio;
    private RadioButton csFixerRadio;
    private RadioButton laravelPintRadio;
    private RadioButton noneRadio;
    private ToggleGroup formattersGroup;

    private String initialFormatter = "none";
    private Runnable onModified;

    public SettingsLanguagesPhpQualityToolsPage() {
        setSpacing(14);
        setPadding(new Insets(16, 20, 20, 20));
        setStyle("-fx-background-color: #1E1F22;");
        VBox.setVgrow(this, Priority.ALWAYS);

        buildUI();
        loadFromManager();
    }

    public void setOnModified(Runnable onModified) {
        this.onModified = onModified;
    }

    private void notifyModified() {
        if (onModified != null) {
            onModified.run();
        }
    }

    private void buildUI() {
        HBox header = createSectionHeader("External Formatters");

        formattersGroup = new ToggleGroup();

        phpcbfRadio = new RadioButton("PHP Code Beautifier and Fixer");
        phpcbfRadio.setToggleGroup(formattersGroup);
        phpcbfRadio.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        csFixerRadio = new RadioButton("PHP CS Fixer");
        csFixerRadio.setToggleGroup(formattersGroup);
        csFixerRadio.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        laravelPintRadio = new RadioButton("Laravel Pint");
        laravelPintRadio.setToggleGroup(formattersGroup);
        laravelPintRadio.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        noneRadio = new RadioButton("No External Formatter");
        noneRadio.setToggleGroup(formattersGroup);
        noneRadio.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        formattersGroup.selectedToggleProperty().addListener((obs, ov, nv) -> notifyModified());

        VBox radioBox = new VBox(10, phpcbfRadio, csFixerRadio, laravelPintRadio, noneRadio);
        radioBox.setPadding(new Insets(4, 0, 0, 16));

        getChildren().addAll(header, radioBox);
    }

    private HBox createSectionHeader(String titleText) {
        HBox box = new HBox(8);
        box.setAlignment(Pos.CENTER_LEFT);

        Label label = new Label(titleText);
        label.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold;");

        Region line = new Region();
        line.setPrefHeight(1);
        line.setMaxHeight(1);
        line.setStyle("-fx-background-color: #393B40;");
        HBox.setHgrow(line, Priority.ALWAYS);

        box.getChildren().addAll(label, line);
        return box;
    }

    private String getSelectedFormatter() {
        if (phpcbfRadio.isSelected()) return "phpcbf";
        if (csFixerRadio.isSelected()) return "php_cs_fixer";
        if (laravelPintRadio.isSelected()) return "laravel_pint";
        return "none";
    }

    public void loadFromManager() {
        PhpQualityToolsSettings qs = manager.getQualityToolsSettings();
        String fmt = qs.getExternalFormatter();
        if ("phpcbf".equalsIgnoreCase(fmt)) {
            phpcbfRadio.setSelected(true);
        } else if ("php_cs_fixer".equalsIgnoreCase(fmt)) {
            csFixerRadio.setSelected(true);
        } else if ("laravel_pint".equalsIgnoreCase(fmt)) {
            laravelPintRadio.setSelected(true);
        } else {
            noneRadio.setSelected(true);
        }
        initialFormatter = getSelectedFormatter();
    }

    public boolean isModified() {
        return !Objects.equals(getSelectedFormatter(), initialFormatter);
    }

    public void apply() {
        PhpQualityToolsSettings qs = manager.getQualityToolsSettings();
        qs.setExternalFormatter(getSelectedFormatter());
        manager.setQualityToolsSettings(qs);
        initialFormatter = getSelectedFormatter();
    }

    public void reset() {
        loadFromManager();
    }

    public void revertChanges() {
        reset();
    }
}
