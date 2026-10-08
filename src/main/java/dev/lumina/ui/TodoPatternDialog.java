package dev.lumina.ui;

import dev.lumina.todo.TodoIconType;
import dev.lumina.todo.TodoPattern;
import dev.lumina.todo.TodoSettingsManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.SVGPath;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.util.UUID;

/**
 * Modal dialog for creating or editing a TODO pattern in Lumina IDE.
 * Faithfully matches reference IDE design and controls (Image 1: media_1791419876080_f381f3ed.png).
 * Features live regex validation, lightbulb prompt icon, icon selector dropdown,
 * "Use color scheme TODO default colors" toggle with subordinate controls (Bold, Italic,
 * Foreground, Background, Error stripe mark, Effects combo box).
 */
public class TodoPatternDialog {

    private final Stage stage;
    private final TextField patternField = new TextField();
    private final ComboBox<TodoIconType> iconCombo = new ComboBox<>();
    private final CheckBox caseSensitiveBox = new CheckBox("Case sensitive");
    private final CheckBox useDefaultColorsBox = new CheckBox("Use color scheme TODO default colors");

    // Subordinate color scheme customization controls
    private final VBox customColorsGroup = new VBox(8);
    private final CheckBox boldBox = new CheckBox("Bold");
    private final CheckBox italicBox = new CheckBox("Italic");

    private final CheckBox foregroundBox = new CheckBox("Foreground");
    private final ColorPicker foregroundColorPicker = new ColorPicker(Color.web("#00A8EC"));

    private final CheckBox backgroundBox = new CheckBox("Background");
    private final ColorPicker backgroundColorPicker = new ColorPicker(Color.web("#2B3856"));

    private final CheckBox errorStripeBox = new CheckBox("Error stripe mark");
    private final ColorPicker errorStripeColorPicker = new ColorPicker(Color.web("#00A8EC"));

    private final CheckBox effectsBox = new CheckBox("Effects");
    private final ColorPicker effectsColorPicker = new ColorPicker(Color.web("#DFE1E5"));
    private final ComboBox<String> effectTypeCombo = new ComboBox<>();

    private final Label errorLabel = new Label();
    private final Button okBtn = new Button("OK");
    private final Button cancelBtn = new Button("Cancel");

    private TodoPattern result = null;
    private final String patternId;
    private final boolean builtIn;

    public TodoPatternDialog(Window owner, TodoPattern initial) {
        stage = new Stage();
        if (owner != null) {
            stage.initOwner(owner);
        }
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle(initial == null ? "Add Pattern" : "Edit Pattern");
        stage.setResizable(false);

        if (initial != null) {
            this.patternId = initial.getId();
            this.builtIn = initial.isBuiltIn();
            patternField.setText(initial.getPattern());
            caseSensitiveBox.setSelected(initial.isCaseSensitive());
            iconCombo.setValue(initial.getIconType());
            useDefaultColorsBox.setSelected(initial.isUseDefaultColors());
            boldBox.setSelected(initial.isBold());
            italicBox.setSelected(initial.isItalic());

            foregroundBox.setSelected(initial.isHasForeground());
            if (initial.getForegroundColor() != null) {
                try {
                    foregroundColorPicker.setValue(Color.web(initial.getForegroundColor()));
                } catch (Exception ignored) {}
            }

            backgroundBox.setSelected(initial.isHasBackground());
            if (initial.getBackgroundColor() != null) {
                try {
                    backgroundColorPicker.setValue(Color.web(initial.getBackgroundColor()));
                } catch (Exception ignored) {}
            }

            errorStripeBox.setSelected(initial.isHasErrorStripe());
            if (initial.getErrorStripeColor() != null) {
                try {
                    errorStripeColorPicker.setValue(Color.web(initial.getErrorStripeColor()));
                } catch (Exception ignored) {}
            }

            effectsBox.setSelected(initial.isHasEffects());
            if (initial.getEffectsColor() != null) {
                try {
                    effectsColorPicker.setValue(Color.web(initial.getEffectsColor()));
                } catch (Exception ignored) {}
            }
            effectTypeCombo.setValue(initial.getEffectType() != null ? initial.getEffectType() : "Bordered");
        } else {
            this.patternId = UUID.randomUUID().toString();
            this.builtIn = false;
            iconCombo.setValue(TodoIconType.TODO);
            useDefaultColorsBox.setSelected(true);
            effectTypeCombo.setValue("Bordered");
        }

        buildUi();
        setupListeners();
        updateSubordinateStates();
    }

    private void buildUi() {
        VBox root = new VBox(10);
        root.setStyle("-fx-background-color: #2B2D30; -fx-padding: 14 18 14 18;");
        root.setPrefWidth(350);

        // --- 1. Pattern Row ---
        Label patternLabel = new Label("Pattern:");
        patternLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        patternLabel.setPrefWidth(65);

        // Yellow lightbulb icon inside or next to field
        SVGPath bulbSvg = new SVGPath();
        bulbSvg.setContent("M9 21c0 .55.45 1 1 1h4c.55 0 1-.45 1-1v-1H9v1zm3-19C8.14 2 5 5.14 5 9c0 2.38 1.19 4.47 3 5.74V17c0 .55.45 1 1 1h6c.55 0 1-.45 1-1v-2.26c1.81-1.27 3-3.36 3-5.74 0-3.86-3.14-7-7-7z");
        bulbSvg.setFill(Color.web("#E5A93C"));
        bulbSvg.setScaleX(0.6);
        bulbSvg.setScaleY(0.6);

        patternField.setStyle("-fx-background-color: #1E1F22; -fx-text-fill: #DFE1E5; -fx-border-color: #3574F0; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 4 6 4 6; -fx-font-size: 12px;");
        HBox.setHgrow(patternField, Priority.ALWAYS);

        HBox fieldWithIcon = new HBox(4, bulbSvg, patternField);
        fieldWithIcon.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(fieldWithIcon, Priority.ALWAYS);

        HBox patternRow = new HBox(8, patternLabel, fieldWithIcon);
        patternRow.setAlignment(Pos.CENTER_LEFT);

        errorLabel.setStyle("-fx-text-fill: #FA5252; -fx-font-size: 11px;");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

        // --- 2. Icon Row ---
        Label iconLabel = new Label("Icon:");
        iconLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        iconLabel.setPrefWidth(65);

        iconCombo.getItems().setAll(TodoIconType.values());
        iconCombo.setPrefWidth(110);
        iconCombo.setStyle("-fx-background-color: #1E1F22; -fx-text-fill: #DFE1E5; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4; -fx-font-size: 12px;");
        iconCombo.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(TodoIconType item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item.getDisplayName());
                    setGraphic(renderIconGraphic(item));
                    setStyle("-fx-text-fill: #DFE1E5; -fx-background-color: #1E1F22;");
                }
            }
        });
        iconCombo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(TodoIconType item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item.getDisplayName());
                    setGraphic(renderIconGraphic(item));
                    setStyle("-fx-text-fill: #DFE1E5;");
                }
            }
        });

        HBox iconRow = new HBox(8, iconLabel, iconCombo);
        iconRow.setAlignment(Pos.CENTER_LEFT);

        // --- 3. Case sensitive ---
        styleCheckBox(caseSensitiveBox);

        // --- 4. Use color scheme TODO default colors ---
        styleCheckBox(useDefaultColorsBox);

        // --- 5. Formatting Group ---
        styleCheckBox(boldBox);
        styleCheckBox(italicBox);
        HBox fontAttrsRow = new HBox(16, boldBox, italicBox);
        fontAttrsRow.setPadding(new Insets(2, 0, 2, 8));

        styleCheckBox(foregroundBox);
        styleColorPicker(foregroundColorPicker);
        HBox foregroundRow = createColorRow(foregroundBox, foregroundColorPicker);

        styleCheckBox(backgroundBox);
        styleColorPicker(backgroundColorPicker);
        HBox backgroundRow = createColorRow(backgroundBox, backgroundColorPicker);

        styleCheckBox(errorStripeBox);
        styleColorPicker(errorStripeColorPicker);
        HBox errorStripeRow = createColorRow(errorStripeBox, errorStripeColorPicker);

        styleCheckBox(effectsBox);
        styleColorPicker(effectsColorPicker);
        HBox effectsRow = createColorRow(effectsBox, effectsColorPicker);

        effectTypeCombo.getItems().setAll("Bordered", "Underscored", "Underwaved", "Dotted line", "Strikeout");
        effectTypeCombo.setPrefWidth(140);
        effectTypeCombo.setStyle("-fx-background-color: #1E1F22; -fx-text-fill: #DFE1E5; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4; -fx-font-size: 11px;");
        VBox.setMargin(effectTypeCombo, new Insets(0, 0, 0, 30));

        customColorsGroup.setSpacing(6);
        customColorsGroup.setPadding(new Insets(2, 0, 4, 18));
        customColorsGroup.getChildren().addAll(
                fontAttrsRow,
                foregroundRow,
                backgroundRow,
                errorStripeRow,
                effectsRow,
                effectTypeCombo
        );

        // --- 6. Button Bar ---
        okBtn.setStyle("-fx-background-color: #3574F0; -fx-text-fill: white; -fx-font-size: 12px; -fx-padding: 5 18; -fx-background-radius: 4; -fx-cursor: hand;");
        cancelBtn.setStyle("-fx-background-color: #393B40; -fx-text-fill: #DFE1E5; -fx-border-color: #4E5157; -fx-font-size: 12px; -fx-padding: 5 18; -fx-background-radius: 4; -fx-cursor: hand;");

        HBox buttonBar = new HBox(8);
        buttonBar.setAlignment(Pos.CENTER_RIGHT);
        buttonBar.setPadding(new Insets(10, 0, 0, 0));
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        buttonBar.getChildren().addAll(spacer, okBtn, cancelBtn);

        root.getChildren().addAll(
                patternRow,
                errorLabel,
                iconRow,
                caseSensitiveBox,
                useDefaultColorsBox,
                customColorsGroup,
                buttonBar
        );

        Scene scene = new Scene(root);
        scene.setFill(Color.web("#2B2D30"));

        try {
            var css = getClass().getResource("/css/lumina-dark.css");
            if (css != null) {
                scene.getStylesheets().add(css.toExternalForm());
            }
        } catch (Exception ignored) {}

        String darkDialogCss = """
            .combo-box-popup .list-view {
                -fx-background-color: #1E1F22;
                -fx-control-inner-background: #1E1F22;
                -fx-border-color: #393B40;
            }
            .combo-box-popup .list-cell {
                -fx-background-color: #1E1F22;
                -fx-text-fill: #DFE1E5;
            }
            .combo-box-popup .list-cell:hover {
                -fx-background-color: #26282E;
            }
            .combo-box-popup .list-cell:selected {
                -fx-background-color: #2E436E;
                -fx-text-fill: #FFFFFF;
            }
            .check-box .box {
                -fx-background-color: #2B2D30;
                -fx-border-color: #4E5157;
                -fx-border-radius: 3;
                -fx-background-radius: 3;
            }
            .check-box:selected .box {
                -fx-background-color: #3574F0;
                -fx-border-color: #3574F0;
            }
            .check-box:selected .mark {
                -fx-background-color: white;
            }
        """;
        try {
            scene.getStylesheets().add("data:text/css," + java.net.URLEncoder.encode(darkDialogCss, java.nio.charset.StandardCharsets.UTF_8).replace("+", "%20"));
        } catch (Exception ignored) {}

        stage.setScene(scene);
    }

    private Node renderIconGraphic(TodoIconType type) {
        if (type == TodoIconType.FIXME) {
            Circle c = new Circle(4);
            c.setFill(Color.web("#FF6B68"));
            return c;
        } else if (type == TodoIconType.TODO) {
            Circle c = new Circle(4);
            c.setFill(Color.web("#3574F0"));
            return c;
        } else {
            Circle c = new Circle(4);
            c.setFill(Color.web("#8C919D"));
            return c;
        }
    }

    private void styleCheckBox(CheckBox cb) {
        cb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
    }

    private void styleColorPicker(ColorPicker cp) {
        cp.setPrefWidth(65);
        cp.setPrefHeight(22);
        cp.setStyle("-fx-color-label-visible: false; -fx-background-color: #1E1F22; -fx-border-color: #4E5157; -fx-border-radius: 3; -fx-background-radius: 3;");
    }

    private HBox createColorRow(CheckBox cb, ColorPicker cp) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        cb.setPrefWidth(125);
        row.getChildren().addAll(cb, cp);
        return row;
    }

    private void setupListeners() {
        useDefaultColorsBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            updateSubordinateStates();
        });

        foregroundBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            foregroundColorPicker.setDisable(!newVal || useDefaultColorsBox.isSelected());
        });

        backgroundBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            backgroundColorPicker.setDisable(!newVal || useDefaultColorsBox.isSelected());
        });

        errorStripeBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            errorStripeColorPicker.setDisable(!newVal || useDefaultColorsBox.isSelected());
        });

        effectsBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            boolean active = newVal && !useDefaultColorsBox.isSelected();
            effectsColorPicker.setDisable(!active);
            effectTypeCombo.setDisable(!active);
        });

        okBtn.setOnAction(e -> handleOk());
        cancelBtn.setOnAction(e -> stage.close());

        patternField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                handleOk();
            } else if (e.getCode() == KeyCode.ESCAPE) {
                stage.close();
            }
        });
    }

    private void updateSubordinateStates() {
        boolean useDefault = useDefaultColorsBox.isSelected();
        customColorsGroup.setDisable(useDefault);

        if (!useDefault) {
            foregroundColorPicker.setDisable(!foregroundBox.isSelected());
            backgroundColorPicker.setDisable(!backgroundBox.isSelected());
            errorStripeColorPicker.setDisable(!errorStripeBox.isSelected());
            boolean eff = effectsBox.isSelected();
            effectsColorPicker.setDisable(!eff);
            effectTypeCombo.setDisable(!eff);
        }
    }

    private void handleOk() {
        String regex = patternField.getText().trim();
        if (regex.isEmpty()) {
            showError("Pattern cannot be empty");
            return;
        }
        if (!TodoSettingsManager.validatePatternRegex(regex)) {
            showError("Invalid regular expression syntax");
            return;
        }

        result = new TodoPattern(
                patternId,
                regex,
                caseSensitiveBox.isSelected(),
                iconCombo.getValue(),
                foregroundBox.isSelected() ? toHex(foregroundColorPicker.getValue()) : null,
                backgroundBox.isSelected() ? toHex(backgroundColorPicker.getValue()) : null,
                boldBox.isSelected(),
                italicBox.isSelected(),
                builtIn,
                true
        );

        result.setUseDefaultColors(useDefaultColorsBox.isSelected());
        result.setHasForeground(foregroundBox.isSelected());
        result.setHasBackground(backgroundBox.isSelected());
        result.setHasErrorStripe(errorStripeBox.isSelected());
        if (errorStripeBox.isSelected()) {
            result.setErrorStripeColor(toHex(errorStripeColorPicker.getValue()));
        }
        result.setHasEffects(effectsBox.isSelected());
        if (effectsBox.isSelected()) {
            result.setEffectsColor(toHex(effectsColorPicker.getValue()));
            result.setEffectType(effectTypeCombo.getValue());
        }

        stage.close();
    }

    private static String toHex(Color color) {
        if (color == null) return null;
        return String.format("#%02X%02X%02X",
                (int) (color.getRed() * 255),
                (int) (color.getGreen() * 255),
                (int) (color.getBlue() * 255));
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    public TodoPattern showDialog() {
        stage.showAndWait();
        return result;
    }

    // Accessors for testing
    public TextField getPatternField() {
        return patternField;
    }

    public CheckBox getCaseSensitiveBox() {
        return caseSensitiveBox;
    }

    public CheckBox getUseDefaultColorsBox() {
        return useDefaultColorsBox;
    }

    public VBox getCustomColorsGroup() {
        return customColorsGroup;
    }

    public ComboBox<TodoIconType> getIconCombo() {
        return iconCombo;
    }

    public Button getOkBtn() {
        return okBtn;
    }

    public Button getCancelBtn() {
        return cancelBtn;
    }
}
