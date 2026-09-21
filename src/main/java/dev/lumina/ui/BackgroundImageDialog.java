package dev.lumina.ui;

import java.io.File;
import java.util.prefs.Preferences;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

/**
 * IntelliJ IDEA-identical Background Image dialog with:
 * - Dynamic image selection and recent history
 * - Opacity slider & spinner (0 - 100)
 * - Orientation / flip buttons (split horizontal & vertical)
 * - Scope: This project only checkbox
 * - Target tabs: Editor and Tools | Empty Frame
 * - Placement / Scale mode controls (Center, Fill/Stretch, Tile)
 * - 3x3 interactive Anchor Grid selector
 * - Live interactive preview panel showing:
 *     - Syntax-highlighted code with tool-window divider for "Editor and Tools"
 *     - Centered shortcut table for "Empty Frame"
 *     - Live rendering of the background image with real-time opacity, scaling, anchor, and flip
 * - Bottom action buttons: Cancel, Clear and Close, and dynamic Set button
 */
public class BackgroundImageDialog {

    private final Stage stage;
    private final Preferences prefs = Preferences.userNodeForPackage(BackgroundImageDialog.class);

    // Target tracking: true = Editor and Tools, false = Empty Frame
    private boolean isEditorTarget = true;

    // Settings for Editor and Tools
    private String editorPath = "";
    private int editorOpacity = 15;
    private int editorScaleMode = 1; // 0 = Center, 1 = Fill/Stretch, 2 = Tile
    private int editorAnchorRow = 1;  // 0 = Top, 1 = Center, 2 = Bottom
    private int editorAnchorCol = 1;  // 0 = Left, 1 = Center, 2 = Right
    private boolean editorFlipH = false;
    private boolean editorFlipV = false;
    private boolean editorProjectOnly = false;

    // Settings for Empty Frame
    private String framePath = "";
    private int frameOpacity = 15;
    private int frameScaleMode = 1;
    private int frameAnchorRow = 1;
    private int frameAnchorCol = 1;
    private boolean frameFlipH = false;
    private boolean frameFlipV = false;
    private boolean frameProjectOnly = false;

    // Top Controls
    private final ComboBox<String> imageCombo;
    private final Slider opacitySlider;
    private final Spinner<Integer> opacitySpinner;
    private final ToggleButton splitHorizBtn;
    private final ToggleButton splitVertBtn;
    private final CheckBox thisProjectOnly;
    private final ToggleButton editorAndToolsBtn;
    private final ToggleButton emptyFrameBtn;
    private final StackPane[] scaleBoxes = new StackPane[3];
    private final StackPane[][] anchorCells = new StackPane[3][3];

    // Preview Panel
    private final Canvas previewCanvas = new Canvas(578, 270);
    private final StackPane previewOverlay = new StackPane();
    private final VBox editorOverlayBox = new VBox();
    private final VBox emptyFrameOverlayBox = new VBox();
    private Image currentLoadedImage = null;
    private String currentLoadedPath = "";

    // Action Buttons
    private final Button setButton;

    public BackgroundImageDialog(Window owner) {
        stage = new Stage();
        stage.initOwner(owner);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Background Image");
        stage.setResizable(true);

        loadSavedPreferences();

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5;");

        // ============================================================
        // Top Controls Area
        // ============================================================
        VBox topControlsBox = new VBox(10);
        topControlsBox.setPadding(new Insets(14, 18, 10, 18));

        // 1. Image Row
        HBox imageRow = new HBox(8);
        imageRow.setAlignment(Pos.CENTER_LEFT);

        Label imageLabel = new Label("Image:");
        imageLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-min-width: 50px;");

        imageCombo = new ComboBox<>();
        imageCombo.setEditable(true);
        HBox.setHgrow(imageCombo, Priority.ALWAYS);
        imageCombo.setStyle("-fx-background-color: #1E1F22; -fx-border-color: #3574F0; -fx-border-radius: 4; -fx-background-radius: 4;");
        if (imageCombo.getEditor() != null) {
            imageCombo.getEditor().setStyle("-fx-background-color: #1E1F22; -fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-prompt-text-fill: #6F737A;");
        }

        Button browseBtn = new Button("...");
        browseBtn.setStyle("-fx-background-color: #393B40; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4; -fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-cursor: hand; -fx-padding: 3 8 3 8;");
        browseBtn.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Select Background Image");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.svg", "*.bmp"));
            File file = chooser.showOpenDialog(stage);
            if (file != null) {
                String path = file.getAbsolutePath();
                if (!imageCombo.getItems().contains(path)) {
                    imageCombo.getItems().add(0, path);
                }
                imageCombo.getSelectionModel().select(path);
                if (imageCombo.getEditor() != null) {
                    imageCombo.getEditor().setText(path);
                }
                onImagePathChanged(path);
            }
        });

        imageRow.getChildren().addAll(imageLabel, imageCombo, browseBtn);

        // 2. Opacity Row
        HBox opacityRow = new HBox(8);
        opacityRow.setAlignment(Pos.CENTER_LEFT);

        Label opacityLabel = new Label("Opacity:");
        opacityLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-min-width: 50px;");

        opacitySlider = new Slider(0, 100, isEditorTarget ? editorOpacity : frameOpacity);
        HBox.setHgrow(opacitySlider, Priority.ALWAYS);
        opacitySlider.setStyle("-fx-control-inner-background: #1E1F22;");

        opacitySpinner = new Spinner<>(0, 100, isEditorTarget ? editorOpacity : frameOpacity);
        opacitySpinner.setPrefWidth(70);
        opacitySpinner.setEditable(true);
        opacitySpinner.setStyle("-fx-background-color: #1E1F22; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4;");
        if (opacitySpinner.getEditor() != null) {
            opacitySpinner.getEditor().setStyle("-fx-background-color: #1E1F22; -fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-alignment: center;");
        }

        opacitySlider.valueProperty().addListener((obs, oldV, newV) -> {
            int val = newV.intValue();
            if (!opacitySpinner.getValue().equals(val)) {
                opacitySpinner.getValueFactory().setValue(val);
            }
            if (isEditorTarget) editorOpacity = val;
            else frameOpacity = val;
            updatePreview();
        });
        opacitySpinner.valueProperty().addListener((obs, oldV, newV) -> {
            if (newV != null && (int) opacitySlider.getValue() != newV) {
                opacitySlider.setValue(newV);
            }
        });

        opacityRow.getChildren().addAll(opacityLabel, opacitySlider, opacitySpinner);

        // 3. Middle Control Strip (Left: Toggles, Checkbox, Tabs; Right: Scale Boxes & Anchor Grid)
        HBox middleStrip = new HBox(14);
        middleStrip.setAlignment(Pos.CENTER_LEFT);

        // Left Sub-Column
        VBox leftSubCol = new VBox(8);
        leftSubCol.setAlignment(Pos.CENTER_LEFT);

        HBox flipRow = new HBox(6);
        flipRow.setAlignment(Pos.CENTER_LEFT);

        splitVertBtn = new ToggleButton("◫");
        splitVertBtn.setStyle("-fx-background-color: #1E1F22; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-padding: 3 8 3 8; -fx-cursor: hand;");
        splitVertBtn.setOnAction(e -> {
            boolean active = splitVertBtn.isSelected();
            if (isEditorTarget) editorFlipV = active;
            else frameFlipV = active;
            updateFlipButtonStyles();
            updatePreview();
        });

        splitHorizBtn = new ToggleButton("⊟");
        splitHorizBtn.setStyle("-fx-background-color: #1E1F22; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-padding: 3 8 3 8; -fx-cursor: hand;");
        splitHorizBtn.setOnAction(e -> {
            boolean active = splitHorizBtn.isSelected();
            if (isEditorTarget) editorFlipH = active;
            else frameFlipH = active;
            updateFlipButtonStyles();
            updatePreview();
        });

        flipRow.getChildren().addAll(splitVertBtn, splitHorizBtn);

        thisProjectOnly = new CheckBox("This project only");
        thisProjectOnly.setSelected(isEditorTarget ? editorProjectOnly : frameProjectOnly);
        thisProjectOnly.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        thisProjectOnly.setOnAction(e -> {
            if (isEditorTarget) editorProjectOnly = thisProjectOnly.isSelected();
            else frameProjectOnly = thisProjectOnly.isSelected();
        });

        // Segmented target buttons
        HBox segmentedTabs = new HBox(0);
        segmentedTabs.setStyle("-fx-background-color: #1E1F22; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4;");
        ToggleGroup targetGroup = new ToggleGroup();

        editorAndToolsBtn = new ToggleButton("Editor and Tools");
        editorAndToolsBtn.setToggleGroup(targetGroup);
        editorAndToolsBtn.setSelected(true);

        emptyFrameBtn = new ToggleButton("Empty Frame");
        emptyFrameBtn.setToggleGroup(targetGroup);

        targetGroup.selectedToggleProperty().addListener((obs, oldT, newT) -> {
            if (newT == null) {
                if (oldT != null) oldT.setSelected(true);
                return;
            }
            switchTarget(newT == editorAndToolsBtn);
        });

        segmentedTabs.getChildren().addAll(editorAndToolsBtn, emptyFrameBtn);
        leftSubCol.getChildren().addAll(flipRow, thisProjectOnly, segmentedTabs);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Right Framed Container (Scale Options + Anchor Grid)
        HBox framedVisualSelectors = new HBox(10);
        framedVisualSelectors.setAlignment(Pos.CENTER);
        framedVisualSelectors.setStyle("-fx-background-color: #1E1F22; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 4 8 4 8;");

        // Scale Mode Buttons (3 items)
        HBox scaleBoxRow = new HBox(4);
        scaleBoxRow.setAlignment(Pos.CENTER);

        // 1) Center icon
        scaleBoxes[0] = createScaleBox(0, createCenterIcon());
        // 2) Fill / Stretch icon (concentric expanding glow boxes)
        scaleBoxes[1] = createScaleBox(1, createFillIcon());
        // 3) Tile icon (dense square pattern)
        scaleBoxes[2] = createScaleBox(2, createTileIcon());

        updateScaleBoxStyles();
        scaleBoxRow.getChildren().addAll(scaleBoxes[0], scaleBoxes[1], scaleBoxes[2]);

        // 3x3 Anchor Grid
        GridPane anchorGrid = new GridPane();
        anchorGrid.setHgap(2);
        anchorGrid.setVgap(2);
        anchorGrid.setStyle("-fx-background-color: #14161E; -fx-padding: 2; -fx-border-color: #393B40; -fx-border-radius: 3; -fx-background-radius: 3;");

        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                final int row = r;
                final int col = c;
                StackPane cell = new StackPane();
                cell.setPrefSize(18, 18);
                cell.setCursor(javafx.scene.Cursor.HAND);
                cell.setOnMouseClicked(e -> {
                    if (isEditorTarget) {
                        editorAnchorRow = row;
                        editorAnchorCol = col;
                    } else {
                        frameAnchorRow = row;
                        frameAnchorCol = col;
                    }
                    updateAnchorGridStyles();
                    updatePreview();
                });
                anchorCells[r][c] = cell;
                anchorGrid.add(cell, c, r);
            }
        }
        updateAnchorGridStyles();

        framedVisualSelectors.getChildren().addAll(scaleBoxRow, anchorGrid);
        middleStrip.getChildren().addAll(leftSubCol, spacer, framedVisualSelectors);

        topControlsBox.getChildren().addAll(imageRow, opacityRow, middleStrip);
        root.setTop(topControlsBox);

        // ============================================================
        // Center: Live Preview Container
        // ============================================================
        StackPane previewContainer = new StackPane();
        previewContainer.setStyle("-fx-background-color: #1E1F22; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4;");
        previewContainer.setPadding(Insets.EMPTY);

        // Bind canvas size to preview container size
        previewCanvas.widthProperty().bind(previewContainer.widthProperty().subtract(2));
        previewCanvas.heightProperty().bind(previewContainer.heightProperty().subtract(2));

        previewCanvas.widthProperty().addListener(e -> updatePreview());
        previewCanvas.heightProperty().addListener(e -> updatePreview());

        // Build overlays
        buildEditorOverlay();
        buildEmptyFrameOverlay();

        previewOverlay.getChildren().setAll(editorOverlayBox);

        previewContainer.getChildren().addAll(previewCanvas, previewOverlay);
        VBox.setVgrow(previewContainer, Priority.ALWAYS);

        VBox centerArea = new VBox(previewContainer);
        centerArea.setPadding(new Insets(2, 18, 8, 18));
        root.setCenter(centerArea);

        // ============================================================
        // Bottom Action Bar
        // ============================================================
        VBox bottomBox = new VBox(10);
        bottomBox.setPadding(new Insets(6, 18, 14, 18));

        Separator separator = new Separator();
        separator.setStyle("-fx-background-color: #393B40;");

        HBox buttonBar = new HBox(10);
        buttonBar.setAlignment(Pos.CENTER_RIGHT);

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle("-fx-background-color: #393B40; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4; -fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-cursor: hand; -fx-padding: 5 14 5 14;");
        cancelBtn.setOnAction(e -> stage.close());

        Button clearAndCloseBtn = new Button("Clear and Close");
        clearAndCloseBtn.setStyle("-fx-background-color: #393B40; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4; -fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-cursor: hand; -fx-padding: 5 14 5 14;");
        clearAndCloseBtn.setOnAction(e -> {
            clearSettings();
            stage.close();
        });

        setButton = new Button("Set for Editor and Tools");
        setButton.setDefaultButton(true);

        setButton.setOnAction(e -> {
            saveCurrentSettings();
            stage.close();
        });

        Region btnSpacer = new Region();
        HBox.setHgrow(btnSpacer, Priority.ALWAYS);

        buttonBar.getChildren().addAll(btnSpacer, cancelBtn, clearAndCloseBtn, setButton);
        bottomBox.getChildren().addAll(separator, buttonBar);
        root.setBottom(bottomBox);

        // Initial image sync
        String currentPath = isEditorTarget ? editorPath : framePath;
        if (!currentPath.isEmpty()) {
            imageCombo.getItems().add(currentPath);
            imageCombo.getSelectionModel().select(currentPath);
            if (imageCombo.getEditor() != null) {
                imageCombo.getEditor().setText(currentPath);
            }
            onImagePathChanged(currentPath);
        }

        if (imageCombo.getEditor() != null) {
            imageCombo.getEditor().textProperty().addListener((obs, oldV, newV) -> onImagePathChanged(newV));
        }
        imageCombo.valueProperty().addListener((obs, oldV, newV) -> onImagePathChanged(newV));

        updateTargetStyles();
        updateSetButtonState();

        Scene scene = new Scene(root, 620, 560);
        stage.setScene(scene);
        stage.setMinWidth(540);
        stage.setMinHeight(500);
    }

    // ============================================================
    // Target Switching & State Management
    // ============================================================

    private void switchTarget(boolean toEditor) {
        isEditorTarget = toEditor;
        updateTargetStyles();

        // Update control values from target state
        String path = isEditorTarget ? editorPath : framePath;
        if (imageCombo.getEditor() != null) {
            imageCombo.getEditor().setText(path);
        }
        imageCombo.getSelectionModel().select(path);

        int opacity = isEditorTarget ? editorOpacity : frameOpacity;
        opacitySlider.setValue(opacity);
        opacitySpinner.getValueFactory().setValue(opacity);

        thisProjectOnly.setSelected(isEditorTarget ? editorProjectOnly : frameProjectOnly);
        splitHorizBtn.setSelected(isEditorTarget ? editorFlipH : frameFlipH);
        splitVertBtn.setSelected(isEditorTarget ? editorFlipV : frameFlipV);
        updateFlipButtonStyles();

        updateScaleBoxStyles();
        updateAnchorGridStyles();

        // Switch live overlay
        previewOverlay.getChildren().setAll(isEditorTarget ? editorOverlayBox : emptyFrameOverlayBox);
        setButton.setText(isEditorTarget ? "Set for Editor and Tools" : "Set for Empty Frame");

        onImagePathChanged(path);
    }

    private void updateTargetStyles() {
        if (isEditorTarget) {
            editorAndToolsBtn.setStyle("-fx-background-color: #393B40; -fx-text-fill: #DFE1E5; -fx-font-size: 11px; -fx-padding: 4 10 4 10; -fx-background-radius: 4 0 0 4; -fx-cursor: hand;");
            emptyFrameBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #848BA3; -fx-font-size: 11px; -fx-padding: 4 10 4 10; -fx-background-radius: 0 4 4 0; -fx-cursor: hand;");
        } else {
            editorAndToolsBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #848BA3; -fx-font-size: 11px; -fx-padding: 4 10 4 10; -fx-background-radius: 4 0 0 4; -fx-cursor: hand;");
            emptyFrameBtn.setStyle("-fx-background-color: #393B40; -fx-text-fill: #DFE1E5; -fx-font-size: 11px; -fx-padding: 4 10 4 10; -fx-background-radius: 0 4 4 0; -fx-cursor: hand;");
        }
    }

    private void updateFlipButtonStyles() {
        boolean vert = isEditorTarget ? editorFlipV : frameFlipV;
        boolean horiz = isEditorTarget ? editorFlipH : frameFlipH;

        splitVertBtn.setStyle(vert
                ? "-fx-background-color: #264C72; -fx-border-color: #3574F0; -fx-border-radius: 4; -fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-padding: 3 8 3 8; -fx-cursor: hand;"
                : "-fx-background-color: #1E1F22; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-padding: 3 8 3 8; -fx-cursor: hand;");

        splitHorizBtn.setStyle(horiz
                ? "-fx-background-color: #264C72; -fx-border-color: #3574F0; -fx-border-radius: 4; -fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-padding: 3 8 3 8; -fx-cursor: hand;"
                : "-fx-background-color: #1E1F22; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-padding: 3 8 3 8; -fx-cursor: hand;");
    }

    private void onImagePathChanged(String path) {
        if (isEditorTarget) editorPath = path != null ? path.trim() : "";
        else framePath = path != null ? path.trim() : "";

        String targetPath = isEditorTarget ? editorPath : framePath;
        if (targetPath != null && !targetPath.isEmpty()) {
            if (!targetPath.equals(currentLoadedPath)) {
                try {
                    File file = new File(targetPath);
                    if (file.exists() && file.isFile()) {
                        currentLoadedImage = new Image(file.toURI().toString());
                        currentLoadedPath = targetPath;
                    } else {
                        currentLoadedImage = null;
                        currentLoadedPath = "";
                    }
                } catch (Exception e) {
                    currentLoadedImage = null;
                    currentLoadedPath = "";
                }
            }
        } else {
            currentLoadedImage = null;
            currentLoadedPath = "";
        }

        updateSetButtonState();
        updatePreview();
    }

    private void updateSetButtonState() {
        String path = isEditorTarget ? editorPath : framePath;
        boolean hasImage = path != null && !path.isEmpty();
        setButton.setDisable(!hasImage);
        if (hasImage) {
            setButton.setStyle("-fx-background-color: #3574F0; -fx-border-color: #3574F0; -fx-border-radius: 4; -fx-background-radius: 4; -fx-text-fill: #FFFFFF; -fx-font-size: 12px; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 5 14 5 14;");
        } else {
            setButton.setStyle("-fx-background-color: #2E3034; -fx-border-color: #3E4146; -fx-border-radius: 4; -fx-background-radius: 4; -fx-text-fill: #5E626B; -fx-font-size: 12px; -fx-font-weight: bold; -fx-cursor: default; -fx-padding: 5 14 5 14;");
        }
    }

    // ============================================================
    // Live Preview Rendering Engine
    // ============================================================

    private void updatePreview() {
        GraphicsContext gc = previewCanvas.getGraphicsContext2D();
        double w = previewCanvas.getWidth();
        double h = previewCanvas.getHeight();
        if (w <= 0 || h <= 0) return;

        gc.clearRect(0, 0, w, h);

        if (currentLoadedImage == null || currentLoadedImage.isError()) {
            return;
        }

        double imgW = currentLoadedImage.getWidth();
        double imgH = currentLoadedImage.getHeight();
        if (imgW <= 0 || imgH <= 0) return;

        int opacity = isEditorTarget ? editorOpacity : frameOpacity;
        int scaleMode = isEditorTarget ? editorScaleMode : frameScaleMode;
        int anchorRow = isEditorTarget ? editorAnchorRow : frameAnchorRow;
        int anchorCol = isEditorTarget ? editorAnchorCol : frameAnchorCol;
        boolean flipH = isEditorTarget ? editorFlipH : frameFlipH;
        boolean flipV = isEditorTarget ? editorFlipV : frameFlipV;

        gc.save();
        gc.setGlobalAlpha(Math.max(0.0, Math.min(1.0, opacity / 100.0)));

        // Coordinate transforms for flip
        if (flipH || flipV) {
            gc.translate(flipH ? w : 0, flipV ? h : 0);
            gc.scale(flipH ? -1 : 1, flipV ? -1 : 1);
        }

        if (scaleMode == 1) {
            // Fill / Stretch: cover preview area
            gc.drawImage(currentLoadedImage, 0, 0, w, h);
        } else if (scaleMode == 0) {
            // Center / Natural Size positioned by anchor
            double drawX;
            if (anchorCol == 0) drawX = 0;
            else if (anchorCol == 2) drawX = w - imgW;
            else drawX = (w - imgW) / 2.0;

            double drawY;
            if (anchorRow == 0) drawY = 0;
            else if (anchorRow == 2) drawY = h - imgH;
            else drawY = (h - imgH) / 2.0;

            gc.drawImage(currentLoadedImage, drawX, drawY);
        } else if (scaleMode == 2) {
            // Tile: repeating grid
            double tileW = Math.min(imgW, w);
            double tileH = Math.min(imgH, h);
            if (tileW <= 0) tileW = 100;
            if (tileH <= 0) tileH = 100;

            for (double x = 0; x < w; x += tileW) {
                for (double y = 0; y < h; y += tileH) {
                    gc.drawImage(currentLoadedImage, x, y, tileW, tileH);
                }
            }
        }

        gc.restore();
    }

    // ============================================================
    // Overlays: Code Editor & Empty Frame
    // ============================================================

    private void buildEditorOverlay() {
        editorOverlayBox.getChildren().clear();
        editorOverlayBox.setAlignment(Pos.TOP_LEFT);

        HBox splitContainer = new HBox();
        splitContainer.setAlignment(Pos.TOP_LEFT);
        HBox.setHgrow(splitContainer, Priority.ALWAYS);
        VBox.setVgrow(splitContainer, Priority.ALWAYS);

        // Left Code Area
        VBox codeArea = new VBox(2);
        codeArea.setPadding(new Insets(12, 14, 12, 14));
        HBox.setHgrow(codeArea, Priority.ALWAYS);

        codeArea.getChildren().addAll(
                createCodeLine(token("#result", "#C77DBB"), token(" = ", "#BCBEC4"), token("1", "#2AACB8"), token(" + ", "#BCBEC4"), token("2", "#2AACB8")),
                createCodeLine(token("#{", "#CF8E6D"), token("6.0221415E+23D", "#2AACB8"), token(" instanceof ", "#CF8E6D"), token("T(Double)", "#2AACB8")),
                createCodeLine(token("intArray", "#BCBEC4"), token("[", "#BCBEC4"), token("idx", "#BCBEC4"), token("]-- ", "#BCBEC4"), token("le ", "#CF8E6D"), token("0xFF", "#2AACB8")),
                createCodeLine(token("{", "#BCBEC4"), token("1", "#2AACB8"), token(", ", "#BCBEC4"), token("2", "#2AACB8"), token(", ", "#BCBEC4"), token("3", "#2AACB8"), token("}", "#BCBEC4")),
                createCodeLine(token("(", "#BCBEC4"), token("true", "#CF8E6D"), token(" and ", "#CF8E6D"), token("false", "#CF8E6D"), token(") || ", "#BCBEC4"), token("variable ", "#BCBEC4"), token("not null ", "#CF8E6D"), token("? ", "#BCBEC4"), token("1.0f", "#2AACB8"), token(" : ", "#BCBEC4"), token("0.0f", "#2AACB8")),
                createCodeLine(token("Members.?[", "#BCBEC4"), token("Nationality", "#BCBEC4"), token(" == ", "#BCBEC4"), token("'Serbian'", "#6AAB73"), token("]", "#BCBEC4")),
                createCodeLine(token("'5.00'", "#6AAB73"), token(" matches ", "#CF8E6D"), token("'^\\-?\\\\d+(\\\\.\\\\d{2})?$'", "#6AAB73")),
                createCodeLine(token("new ", "#CF8E6D"), token("java.lang.String(", "#BCBEC4"), token("'stringLiteral'", "#6AAB73"), token(")", "#BCBEC4")),
                createCodeLine(token("${", "#C77DBB"), token("myPropertyKey", "#C77DBB"), token("}", "#C77DBB")),
                createCodeLine(token("@myBean.", "#BCBEC4"), token("instanceMethod", "#56A8F5"), token("(", "#BCBEC4"), token("1", "#2AACB8"), token(", ", "#BCBEC4"), token("2", "#2AACB8"), token(")", "#BCBEC4")),
                createCodeLine(token("T(String).", "#BCBEC4"), token("CASE_INSENSITIVE_ORDER", "#C77DBB"))
        );

        // Right Tools Sidebar representation
        VBox toolSidebar = new VBox();
        toolSidebar.setPrefWidth(160);
        toolSidebar.setStyle("-fx-background-color: rgba(26, 28, 31, 0.4); -fx-border-color: transparent transparent transparent #393B40; -fx-border-width: 0 0 0 1;");

        splitContainer.getChildren().addAll(codeArea, toolSidebar);
        editorOverlayBox.getChildren().add(splitContainer);
    }

    private TextFlow createCodeLine(Text... tokens) {
        TextFlow flow = new TextFlow(tokens);
        flow.setStyle("-fx-font-family: 'JetBrains Mono', 'Menlo', monospace; -fx-font-size: 11px;");
        return flow;
    }

    private Text token(String text, String hexColor) {
        Text t = new Text(text);
        t.setFill(Color.web(hexColor));
        return t;
    }

    private void buildEmptyFrameOverlay() {
        emptyFrameOverlayBox.getChildren().clear();
        emptyFrameOverlayBox.setAlignment(Pos.CENTER);
        emptyFrameOverlayBox.setPadding(new Insets(24, 20, 20, 20));

        VBox shortcutContainer = new VBox(6);
        shortcutContainer.setAlignment(Pos.CENTER);
        shortcutContainer.setMaxWidth(380);

        shortcutContainer.getChildren().addAll(
                createShortcutRow("Search Everywhere", "Double ⇧"),
                createShortcutRow("Project View", "⌘1"),
                createShortcutRow("Go to File", "⇧⌘O"),
                createShortcutRow("Recent Files", "⌘E"),
                createShortcutRow("Navigation Bar", "⌘↑")
        );

        Label dropFilesHint = new Label("Drop files here to open them");
        dropFilesHint.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-padding: 16 0 0 0;");

        emptyFrameOverlayBox.getChildren().addAll(shortcutContainer, dropFilesHint);
    }

    private HBox createShortcutRow(String action, String key) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER);

        Label actionLbl = new Label(action);
        actionLbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold; -fx-alignment: center-right;");
        actionLbl.setPrefWidth(180);

        Label keyLbl = new Label(key);
        keyLbl.setStyle("-fx-text-fill: #6F737A; -fx-font-size: 13px; -fx-alignment: center-left;");
        keyLbl.setPrefWidth(120);

        row.getChildren().addAll(actionLbl, keyLbl);
        return row;
    }

    // ============================================================
    // Icon Builders & Style Helpers
    // ============================================================

    private StackPane createScaleBox(int index, Region icon) {
        StackPane box = new StackPane(icon);
        box.setPrefSize(38, 56);
        box.setCursor(javafx.scene.Cursor.HAND);
        box.setOnMouseClicked(e -> {
            if (isEditorTarget) editorScaleMode = index;
            else frameScaleMode = index;
            updateScaleBoxStyles();
            updatePreview();
        });
        return box;
    }

    private Region createCenterIcon() {
        StackPane pane = new StackPane();
        pane.setPrefSize(24, 24);
        Rectangle inner = new Rectangle(8, 8);
        inner.setFill(Color.TRANSPARENT);
        inner.setStroke(Color.web("#DFE1E5"));
        inner.setStrokeWidth(1.5);
        pane.getChildren().add(inner);
        return pane;
    }

    private Region createFillIcon() {
        StackPane pane = new StackPane();
        pane.setPrefSize(24, 30);
        Rectangle outer = new Rectangle(20, 26);
        outer.setFill(Color.TRANSPARENT);
        outer.setStroke(Color.web("#848BA3"));
        outer.setStrokeWidth(1.2);

        Rectangle mid = new Rectangle(14, 18);
        mid.setFill(Color.TRANSPARENT);
        mid.setStroke(Color.web("#B9BECF"));
        mid.setStrokeWidth(1.2);

        Rectangle inner = new Rectangle(8, 10);
        inner.setFill(Color.web("#DFE1E5"));

        pane.getChildren().addAll(outer, mid, inner);
        return pane;
    }

    private Region createTileIcon() {
        GridPane grid = new GridPane();
        grid.setHgap(2);
        grid.setVgap(2);
        grid.setAlignment(Pos.CENTER);
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                Rectangle sq = new Rectangle(5, 5);
                sq.setFill(Color.web("#DFE1E5"));
                grid.add(sq, c, r);
            }
        }
        return grid;
    }

    private void updateScaleBoxStyles() {
        int activeMode = isEditorTarget ? editorScaleMode : frameScaleMode;
        for (int i = 0; i < scaleBoxes.length; i++) {
            if (i == activeMode) {
                scaleBoxes[i].setStyle("-fx-background-color: #264C72; -fx-border-color: #3574F0; -fx-border-radius: 4; -fx-background-radius: 4;");
            } else {
                scaleBoxes[i].setStyle("-fx-background-color: #14161E; -fx-border-color: #2C3042; -fx-border-radius: 4; -fx-background-radius: 4;");
            }
        }
    }

    private void updateAnchorGridStyles() {
        int activeRow = isEditorTarget ? editorAnchorRow : frameAnchorRow;
        int activeCol = isEditorTarget ? editorAnchorCol : frameAnchorCol;

        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                if (r == activeRow && c == activeCol) {
                    anchorCells[r][c].setStyle("-fx-background-color: #3574F0; -fx-border-color: #589DF6; -fx-background-radius: 2; -fx-border-radius: 2;");
                } else {
                    anchorCells[r][c].setStyle("-fx-background-color: #26282E; -fx-border-color: #33363D; -fx-background-radius: 2; -fx-border-radius: 2;");
                }
            }
        }
    }

    // ============================================================
    // Persistence
    // ============================================================

    private void loadSavedPreferences() {
        editorPath = prefs.get("bg_editor_path", prefs.get("bg_image_path", ""));
        editorOpacity = prefs.getInt("bg_editor_opacity", prefs.getInt("bg_opacity", 15));
        editorScaleMode = prefs.getInt("bg_editor_scale", prefs.getInt("bg_scale_mode", 1));
        editorAnchorRow = prefs.getInt("bg_editor_anchor_row", prefs.getInt("bg_anchor_row", 1));
        editorAnchorCol = prefs.getInt("bg_editor_anchor_col", prefs.getInt("bg_anchor_col", 1));
        editorFlipH = prefs.getBoolean("bg_editor_flip_h", false);
        editorFlipV = prefs.getBoolean("bg_editor_flip_v", false);
        editorProjectOnly = prefs.getBoolean("bg_editor_project_only", prefs.getBoolean("bg_project_only", false));

        framePath = prefs.get("bg_frame_path", "");
        frameOpacity = prefs.getInt("bg_frame_opacity", 15);
        frameScaleMode = prefs.getInt("bg_frame_scale", 1);
        frameAnchorRow = prefs.getInt("bg_frame_anchor_row", 1);
        frameAnchorCol = prefs.getInt("bg_frame_anchor_col", 1);
        frameFlipH = prefs.getBoolean("bg_frame_flip_h", false);
        frameFlipV = prefs.getBoolean("bg_frame_flip_v", false);
        frameProjectOnly = prefs.getBoolean("bg_frame_project_only", false);
    }

    private void saveCurrentSettings() {
        if (isEditorTarget) {
            String path = imageCombo.getEditor() != null ? imageCombo.getEditor().getText() : imageCombo.getValue();
            if (path != null && !path.trim().isEmpty()) {
                editorPath = path.trim();
                prefs.put("bg_editor_path", editorPath);
                prefs.put("bg_image_path", editorPath); // legacy key
            }
            prefs.putInt("bg_editor_opacity", editorOpacity);
            prefs.putInt("bg_opacity", editorOpacity);
            prefs.putInt("bg_editor_scale", editorScaleMode);
            prefs.putInt("bg_scale_mode", editorScaleMode);
            prefs.putInt("bg_editor_anchor_row", editorAnchorRow);
            prefs.putInt("bg_anchor_row", editorAnchorRow);
            prefs.putInt("bg_editor_anchor_col", editorAnchorCol);
            prefs.putInt("bg_anchor_col", editorAnchorCol);
            prefs.putBoolean("bg_editor_flip_h", editorFlipH);
            prefs.putBoolean("bg_editor_flip_v", editorFlipV);
            prefs.putBoolean("bg_editor_project_only", editorProjectOnly);
            prefs.putBoolean("bg_project_only", editorProjectOnly);
        } else {
            String path = imageCombo.getEditor() != null ? imageCombo.getEditor().getText() : imageCombo.getValue();
            if (path != null && !path.trim().isEmpty()) {
                framePath = path.trim();
                prefs.put("bg_frame_path", framePath);
            }
            prefs.putInt("bg_frame_opacity", frameOpacity);
            prefs.putInt("bg_frame_scale", frameScaleMode);
            prefs.putInt("bg_frame_anchor_row", frameAnchorRow);
            prefs.putInt("bg_frame_anchor_col", frameAnchorCol);
            prefs.putBoolean("bg_frame_flip_h", frameFlipH);
            prefs.putBoolean("bg_frame_flip_v", frameFlipV);
            prefs.putBoolean("bg_frame_project_only", frameProjectOnly);
        }
    }

    private void clearSettings() {
        if (isEditorTarget) {
            editorPath = "";
            editorOpacity = 15;
            prefs.remove("bg_editor_path");
            prefs.remove("bg_image_path");
            prefs.putInt("bg_editor_opacity", 15);
            prefs.putInt("bg_opacity", 15);
        } else {
            framePath = "";
            frameOpacity = 15;
            prefs.remove("bg_frame_path");
            prefs.putInt("bg_frame_opacity", 15);
        }
        currentLoadedImage = null;
        currentLoadedPath = "";
        if (imageCombo.getEditor() != null) {
            imageCombo.getEditor().setText("");
        }
        imageCombo.getSelectionModel().clearSelection();
        updateSetButtonState();
        updatePreview();
    }

    public void showAndWait() {
        stage.showAndWait();
    }
}
