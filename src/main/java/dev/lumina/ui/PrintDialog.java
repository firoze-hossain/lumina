package dev.lumina.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.File;
import java.nio.file.Path;

/**
 * IntelliJ-style Print dialog.
 * Exactly as shown in the screenshot.
 */
public class PrintDialog {

    private final Stage stage;

    // File selection
    private final RadioButton currentFile = new RadioButton("File InvalidateCachesDialog.java");
    private final RadioButton selectedText = new RadioButton("Selected text");
    private final RadioButton allFiles = new RadioButton("All files in directory ");
    private final CheckBox includeSubdirectories = new CheckBox("Include subdirectories");
    private final TextField directoryField = new TextField();

    // Settings
    private final ComboBox<String> paperSize = new ComboBox<>();
    private final ComboBox<String> fontFamily = new ComboBox<>();
    private final Spinner<Integer> fontSize = new Spinner<>(8, 24, 13);
    private final CheckBox showLineNumbers = new CheckBox("Show line numbers");
    private final CheckBox showBorder = new CheckBox("Show border");

    // Orientation
    private final RadioButton portrait = new RadioButton("Portrait");
    private final RadioButton landscape = new RadioButton("Landscape");

    // Style
    private final CheckBox colorPrinting = new CheckBox("Color printing");
    private final CheckBox syntaxPrinting = new CheckBox("Syntax printing");
    private final CheckBox printAsGraphics = new CheckBox("Print as graphics");

    public PrintDialog(Stage owner, Path currentFilePath) {
        this.stage = new Stage();

        stage.initOwner(owner);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initStyle(StageStyle.DECORATED);
        stage.setTitle("Print");

        BorderPane root = new BorderPane();
        root.getStyleClass().addAll("app-root", "print-dialog");

        // ---- Content ----
        VBox content = new VBox(16);
        content.setPadding(new Insets(20, 24, 16, 24));

        // ---- File section ----
        Label fileLabel = new Label("File");
        fileLabel.getStyleClass().add("print-section-title");

        ToggleGroup fileGroup = new ToggleGroup();
        currentFile.setToggleGroup(fileGroup);
        currentFile.setSelected(true);
        selectedText.setToggleGroup(fileGroup);
        allFiles.setToggleGroup(fileGroup);

        // Set the current file name
        String fileName = currentFilePath != null
                ? currentFilePath.getFileName().toString()
                : "InvalidateCachesDialog.java";
        currentFile.setText("File " + fileName);

        // Directory field for "All files in directory"
        String dirPath = currentFilePath != null
                ? currentFilePath.getParent().toString()
                : "/home/firoze/projects/others/lumina/src/main/java/dev/lumina/ui";
        directoryField.setText(dirPath);
        directoryField.setEditable(false);
        directoryField.getStyleClass().add("text-field");

        HBox allFilesRow = new HBox(6);
        allFilesRow.setAlignment(Pos.CENTER_LEFT);
        allFilesRow.getChildren().addAll(allFiles, directoryField);

        HBox includeRow = new HBox(6);
        includeRow.setAlignment(Pos.CENTER_LEFT);
        includeSubdirectories.setSelected(true);
        includeRow.getChildren().add(includeSubdirectories);
        includeRow.setPadding(new Insets(0, 0, 0, 20));

        VBox fileBox = new VBox(6,
                currentFile,
                selectedText,
                allFilesRow,
                includeRow
        );

        // ---- Settings section ----
        Label settingsLabel = new Label("Settings");
        settingsLabel.getStyleClass().add("print-section-title");

        Label headerFooter = new Label("Header and Footer");
        headerFooter.getStyleClass().add("print-sublabel");
        Label advanced = new Label("Advanced");
        advanced.getStyleClass().add("print-sublabel");

        // Paper size
        HBox paperRow = new HBox(12);
        paperRow.setAlignment(Pos.CENTER_LEFT);
        Label paperLabel = new Label("Paper size");
        paperLabel.getStyleClass().add("print-label");
        paperSize.getItems().addAll("A4 (210 x 297 mm)", "A5 (148 x 210 mm)", "Letter (216 x 279 mm)", "Legal (216 x 356 mm)");
        paperSize.getSelectionModel().selectFirst();
        paperSize.getStyleClass().add("settings-combo");
        paperSize.setPrefWidth(200);
        paperRow.getChildren().addAll(paperLabel, paperSize);

        // Font
        HBox fontRow = new HBox(12);
        fontRow.setAlignment(Pos.CENTER_LEFT);
        Label fontLabel = new Label("Font");
        fontLabel.getStyleClass().add("print-label");
        fontFamily.getItems().addAll("JetBrains Mono", "Consolas", "Menlo", "Monaco", "Courier New", "Segoe UI");
        fontFamily.getSelectionModel().selectFirst();
        fontFamily.getStyleClass().add("settings-combo");
        fontFamily.setPrefWidth(160);
        Label sizeLabel = new Label("Size");
        sizeLabel.getStyleClass().add("print-label");
        fontSize.setPrefWidth(70);
        fontSize.getStyleClass().add("print-spinner");
        fontRow.getChildren().addAll(fontLabel, fontFamily, sizeLabel, fontSize);

        // Checkboxes
        showLineNumbers.setSelected(true);
        showLineNumbers.getStyleClass().add("settings-check");
        showBorder.setSelected(true);
        showBorder.getStyleClass().add("settings-check");

        HBox checkboxRow = new HBox(24);
        checkboxRow.setAlignment(Pos.CENTER_LEFT);
        checkboxRow.getChildren().addAll(showLineNumbers, showBorder);

        VBox settingsBox = new VBox(8,
                settingsLabel,
                headerFooter,
                advanced,
                paperRow,
                fontRow,
                checkboxRow
        );

        // ---- Orientation section ----
        Label orientationLabel = new Label("Orientation");
        orientationLabel.getStyleClass().add("print-section-title");

        ToggleGroup orientationGroup = new ToggleGroup();
        portrait.setToggleGroup(orientationGroup);
        portrait.setSelected(true);
        landscape.setToggleGroup(orientationGroup);

        HBox orientationBox = new HBox(20);
        orientationBox.setAlignment(Pos.CENTER_LEFT);
        orientationBox.getChildren().addAll(portrait, landscape);

        // ---- Style section ----
        Label styleLabel = new Label("Style");
        styleLabel.getStyleClass().add("print-section-title");

        colorPrinting.getStyleClass().add("settings-check");
        syntaxPrinting.setSelected(true);
        syntaxPrinting.getStyleClass().add("settings-check");
        printAsGraphics.setSelected(true);
        printAsGraphics.getStyleClass().add("settings-check");

        VBox styleBox = new VBox(6,
                styleLabel,
                colorPrinting,
                syntaxPrinting,
                printAsGraphics
        );

        // ---- Assemble ----
        content.getChildren().addAll(
                fileLabel,
                fileBox,
                settingsBox,
                orientationLabel,
                orientationBox,
                styleBox
        );

        // ---- Buttons ----
        HBox buttons = buildButtonBar();

        root.setCenter(content);
        root.setBottom(buttons);

        Scene scene = new Scene(root, 540, 520);
        scene.getStylesheets().add(
                getClass().getResource("/css/lumina-dark.css").toExternalForm());
        stage.setScene(scene);
    }

    public void show() {
        stage.showAndWait();
    }

    // --------------------------------------------------- button bar

    private HBox buildButtonBar() {
        Button printBtn = new Button("Print");
        printBtn.getStyleClass().add("dialog-primary");
        printBtn.setDefaultButton(true);
        printBtn.setOnAction(e -> {
            performPrint();
            stage.close();
        });

        Button cancel = new Button("Cancel");
        cancel.getStyleClass().add("dialog-secondary");
        cancel.setOnAction(e -> stage.close());

        Button apply = new Button("Apply");
        apply.getStyleClass().add("dialog-secondary");
        apply.setOnAction(e -> applySettings());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox bar = new HBox(10, spacer, apply, cancel, printBtn);
        bar.setAlignment(Pos.CENTER_RIGHT);
        bar.setPadding(new Insets(12, 20, 14, 20));
        bar.getStyleClass().add("dialog-footer");
        return bar;
    }

    // --------------------------------------------------- actions

    private void performPrint() {
        System.out.println("Printing with settings:");

        // File selection
        if (currentFile.isSelected()) {
            System.out.println("  File: " + currentFile.getText().replace("File ", ""));
        } else if (selectedText.isSelected()) {
            System.out.println("  Selected text");
        } else if (allFiles.isSelected()) {
            System.out.println("  All files in: " + directoryField.getText());
            System.out.println("  Include subdirectories: " + includeSubdirectories.isSelected());
        }

        // Settings
        System.out.println("  Paper size: " + paperSize.getValue());
        System.out.println("  Font: " + fontFamily.getValue() + " " + fontSize.getValue());
        System.out.println("  Show line numbers: " + showLineNumbers.isSelected());
        System.out.println("  Show border: " + showBorder.isSelected());

        // Orientation
        System.out.println("  Orientation: " + (portrait.isSelected() ? "Portrait" : "Landscape"));

        // Style
        System.out.println("  Color printing: " + colorPrinting.isSelected());
        System.out.println("  Syntax printing: " + syntaxPrinting.isSelected());
        System.out.println("  Print as graphics: " + printAsGraphics.isSelected());
    }

    private void applySettings() {
        System.out.println("Settings applied (not implemented yet)");
    }

    // --------------------------------------------------- getters for settings

    public String getPaperSize() {
        return paperSize.getValue();
    }

    public String getFontFamily() {
        return fontFamily.getValue();
    }

    public int getFontSize() {
        return fontSize.getValue();
    }

    public boolean isShowLineNumbers() {
        return showLineNumbers.isSelected();
    }

    public boolean isShowBorder() {
        return showBorder.isSelected();
    }

    public boolean isPortrait() {
        return portrait.isSelected();
    }

    public boolean isColorPrinting() {
        return colorPrinting.isSelected();
    }

    public boolean isSyntaxPrinting() {
        return syntaxPrinting.isSelected();
    }

    public boolean isPrintAsGraphics() {
        return printAsGraphics.isSelected();
    }
}