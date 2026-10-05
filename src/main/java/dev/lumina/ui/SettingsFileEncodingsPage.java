package dev.lumina.ui;

import dev.lumina.settings.FileEncodingsSettings;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.awt.Desktop;
import java.io.File;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Complete, dynamic IntelliJ IDEA Editor > File Encodings settings page.
 * Faithfully reproduces IntelliJ IDEA layout, dynamic charset configuration,
 * toolbar actions (+, -, edit), path/encoding table, properties defaults,
 * BOM helper labels, and synchronization with .idea/encodings.xml.
 */
public class SettingsFileEncodingsPage extends VBox {

    private final FileEncodingsSettings settings = FileEncodingsSettings.getInstance();

    // Baseline values for dirty tracking
    private String baselineGlobal;
    private String baselineProject;
    private String baselineProperties;
    private boolean baselineNativeToAscii;
    private String baselineUtf8Bom;
    private final List<FileEncodingsSettings.PathMapping> baselineMappings = new ArrayList<>();

    // Controls
    private final EncodingMenuButton globalEncodingBtn;
    private final EncodingMenuButton projectEncodingBtn;
    private final EncodingMenuButton propertiesEncodingBtn;
    private final CheckBox nativeToAsciiCheck;
    private final ComboBox<String> utf8BomCombo;

    private final Label helperPrefix = new Label();
    private final Hyperlink bomHyperlink = new Hyperlink("UTF-8 BOM ↗");
    private final Label helperSuffix = new Label();

    private final TableView<EncodingRow> encodingTable = new TableView<>();
    private final ObservableList<EncodingRow> encodingData = FXCollections.observableArrayList();

    private final Button addBtn = new Button();
    private final Button removeBtn = new Button();
    private final Button editBtn = new Button();

    private Runnable onModifiedListener;

    public SettingsFileEncodingsPage() {
        getStyleClass().add("settings-page");
        setStyle("-fx-background-color: #1E1F22;");
        setPadding(new Insets(14, 20, 20, 20));
        setSpacing(12);

        // ============================================================
        // 1. Global Encoding Row
        // ============================================================
        HBox globalRow = new HBox(8);
        globalRow.setAlignment(Pos.CENTER_LEFT);

        Label globalLabel = new Label("Global Encoding:");
        globalLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        globalLabel.setMinWidth(110);

        globalEncodingBtn = new EncodingMenuButton(null, settings.getGlobalEncoding());
        globalEncodingBtn.setPrefWidth(95);
        globalEncodingBtn.setOnEncodingChanged(enc -> notifyModified());

        globalRow.getChildren().addAll(globalLabel, globalEncodingBtn);

        // ============================================================
        // 2. Project Encoding Row
        // ============================================================
        HBox projectRow = new HBox(8);
        projectRow.setAlignment(Pos.CENTER_LEFT);

        Label projectLabel = new Label("Project Encoding:");
        projectLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        projectLabel.setMinWidth(110);

        projectEncodingBtn = new EncodingMenuButton(
                FileEncodingsSettings.getSystemDefaultEncodingLabel(),
                settings.getProjectEncoding()
        );
        projectEncodingBtn.setPrefWidth(220);
        projectEncodingBtn.setOnEncodingChanged(enc -> notifyModified());

        projectRow.getChildren().addAll(projectLabel, projectEncodingBtn);

        // ============================================================
        // 3. Toolbar & Path/Encoding Table
        // ============================================================
        HBox toolbar = buildToolbar();
        buildTable();

        VBox.setVgrow(encodingTable, Priority.NEVER);
        encodingTable.setPrefHeight(180);

        // ============================================================
        // 4. Middle Explanatory Description
        // ============================================================
        Label tableDesc = new Label(
                "Add the path to a file or directory and select the encoding IntelliJ IDEA should use.\n"
                + "Files and directories inherit the encoding from the parent directory or from the Project Encoding.\n"
                + "Built-in file encodings in JSP, HTML, and XML files override these settings."
        );
        tableDesc.setStyle("-fx-text-fill: #868A91; -fx-font-size: 12px; -fx-line-spacing: 3px;");
        tableDesc.setWrapText(true);
        VBox.setMargin(tableDesc, new Insets(2, 0, 4, 0));

        // ============================================================
        // 5. Default Encoding for Properties Files Row
        // ============================================================
        HBox propertiesRow = new HBox(8);
        propertiesRow.setAlignment(Pos.CENTER_LEFT);

        Label propertiesLabel = new Label("Default encoding for properties files:");
        propertiesLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        propertiesLabel.setMinWidth(225);

        propertiesEncodingBtn = new EncodingMenuButton(
                FileEncodingsSettings.getPropertiesDefaultEncodingLabel(),
                settings.getPropertiesFileEncoding()
        );
        propertiesEncodingBtn.setPrefWidth(220);
        propertiesEncodingBtn.setOnEncodingChanged(enc -> notifyModified());

        propertiesRow.getChildren().addAll(propertiesLabel, propertiesEncodingBtn);

        // ============================================================
        // 6. Transparent Native-to-ASCII CheckBox
        // ============================================================
        nativeToAsciiCheck = new CheckBox("Transparent native-to-ascii conversion");
        nativeToAsciiCheck.setSelected(settings.isTransparentNativeToAscii());
        nativeToAsciiCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        nativeToAsciiCheck.setOnAction(e -> notifyModified());
        VBox.setMargin(nativeToAsciiCheck, new Insets(2, 0, 2, 0));

        // ============================================================
        // 7. Create UTF-8 Files Row
        // ============================================================
        HBox utf8Row = new HBox(8);
        utf8Row.setAlignment(Pos.CENTER_LEFT);

        Label utf8Label = new Label("Create UTF-8 files:");
        utf8Label.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        utf8Label.setMinWidth(110);

        utf8BomCombo = new ComboBox<>();
        utf8BomCombo.getItems().addAll(
                FileEncodingsSettings.BOM_WITH_NO_BOM,
                FileEncodingsSettings.BOM_WITH_BOM,
                FileEncodingsSettings.BOM_WITH_BOM_ON_WINDOWS
        );
        utf8BomCombo.setValue(settings.getCreateUtf8FilesOption());
        utf8BomCombo.setPrefWidth(340);
        utf8BomCombo.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #4E5157; "
                + "-fx-border-radius: 4; -fx-background-radius: 4; -fx-font-size: 12px;");

        utf8BomCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            updateBomHelperText(newVal);
            notifyModified();
        });

        utf8Row.getChildren().addAll(utf8Label, utf8BomCombo);

        // ============================================================
        // 8. Dynamic BOM Helper Label
        // ============================================================
        HBox helperBox = new HBox(0);
        helperBox.setAlignment(Pos.CENTER_LEFT);

        helperPrefix.setStyle("-fx-text-fill: #868A91; -fx-font-size: 12px;");
        helperSuffix.setStyle("-fx-text-fill: #868A91; -fx-font-size: 12px;");

        bomHyperlink.setStyle("-fx-text-fill: #589DF6; -fx-padding: 0; -fx-border-width: 0; -fx-font-size: 12px; -fx-cursor: hand;");
        bomHyperlink.setOnAction(e -> {
            try {
                Desktop.getDesktop().browse(URI.create("https://en.wikipedia.org/wiki/Byte_order_mark#UTF-8"));
            } catch (Exception ignored) {}
        });

        helperBox.getChildren().addAll(helperPrefix, bomHyperlink, helperSuffix);
        VBox.setMargin(helperBox, new Insets(0, 0, 0, 118));
        updateBomHelperText(utf8BomCombo.getValue());

        // ============================================================
        // Assemble All
        // ============================================================
        getChildren().addAll(
                globalRow,
                projectRow,
                toolbar,
                encodingTable,
                tableDesc,
                propertiesRow,
                nativeToAsciiCheck,
                utf8Row,
                helperBox
        );

        // Load data from settings & baseline
        reloadFromSettings();
    }

    private void updateBomHelperText(String bomOption) {
        if (FileEncodingsSettings.BOM_WITH_BOM.equals(bomOption)) {
            helperPrefix.setText("IDEA will add ");
            helperSuffix.setText(" to every created file in UTF-8 encoding");
        } else if (FileEncodingsSettings.BOM_WITH_BOM_ON_WINDOWS.equals(bomOption)) {
            helperPrefix.setText("IDEA will add ");
            helperSuffix.setText(" to every created file in UTF-8 encoding on Windows");
        } else {
            helperPrefix.setText("IDEA will NOT add ");
            helperSuffix.setText(" to every created file in UTF-8 encoding");
        }
    }

    private HBox buildToolbar() {
        HBox bar = new HBox(2);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(6, 0, 4, 0));

        // + (Add)
        SVGPath plusSvg = new SVGPath();
        plusSvg.setContent("M 6 2 L 6 10 M 2 6 L 10 6");
        styleSvg(plusSvg);
        addBtn.setGraphic(plusSvg);
        styleToolBtn(addBtn, "Add (Alt+Insert)");
        addBtn.setOnAction(e -> onAdd());

        // - (Remove)
        SVGPath minusSvg = new SVGPath();
        minusSvg.setContent("M 2 6 L 10 6");
        styleSvg(minusSvg);
        removeBtn.setGraphic(minusSvg);
        styleToolBtn(removeBtn, "Remove (Delete)");
        removeBtn.setOnAction(e -> onRemove());

        // Edit pencil
        SVGPath editSvg = new SVGPath();
        editSvg.setContent("M 2 8.5 L 2 10 L 3.5 10 L 9 4.5 L 7.5 3 Z M 8 2 L 9.5 3.5");
        styleSvg(editSvg);
        editBtn.setGraphic(editSvg);
        styleToolBtn(editBtn, "Edit (Enter)");
        editBtn.setOnAction(e -> onEdit());

        // Disable buttons when table selection is empty
        removeBtn.disableProperty().bind(encodingTable.getSelectionModel().selectedItemProperty().isNull());
        editBtn.disableProperty().bind(encodingTable.getSelectionModel().selectedItemProperty().isNull());

        bar.getChildren().addAll(addBtn, removeBtn, editBtn);
        return bar;
    }

    private void styleSvg(SVGPath p) {
        p.setFill(Color.TRANSPARENT);
        p.setStroke(Color.web("#AFB1B6"));
        p.setStrokeWidth(1.4);
    }

    private void styleToolBtn(Button btn, String tooltip) {
        btn.setMinSize(24, 24);
        btn.setPrefSize(24, 24);
        btn.setMaxSize(24, 24);
        btn.setStyle("-fx-background-color: transparent; -fx-padding: 0; -fx-background-radius: 4; -fx-cursor: hand;");
        btn.setOnMouseEntered(e -> {
            if (!btn.isDisable()) {
                btn.setStyle("-fx-background-color: #393B40; -fx-padding: 0; -fx-background-radius: 4; -fx-cursor: hand;");
            }
        });
        btn.setOnMouseExited(e -> {
            if (!btn.isDisable()) {
                btn.setStyle("-fx-background-color: transparent; -fx-padding: 0; -fx-background-radius: 4; -fx-cursor: hand;");
            }
        });
        btn.setTooltip(new Tooltip(tooltip));
    }

    private void buildTable() {
        encodingTable.setEditable(true);
        encodingTable.setStyle("-fx-background-color: #1E1F22; -fx-control-inner-background: #1E1F22; "
                + "-fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4;");
        encodingTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Path column with icon
        TableColumn<EncodingRow, String> pathCol = new TableColumn<>("Path ^");
        pathCol.setCellValueFactory(cell -> cell.getValue().displayPathProperty());
        pathCol.setPrefWidth(380);
        pathCol.setCellFactory(col -> new TableCell<>() {
            private final HBox content = new HBox(8);
            private final SVGPath icon = new SVGPath();
            private final Label label = new Label();

            {
                content.setAlignment(Pos.CENTER_LEFT);
                label.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
                content.getChildren().addAll(icon, label);
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    EncodingRow row = getTableRow() != null ? getTableRow().getItem() : null;
                    if (row != null && row.isDirectory()) {
                        // IntelliJ Folder icon
                        icon.setContent("M 1 2.5 L 5 2.5 L 6.5 4 L 13 4 C 13.5 4 14 4.5 14 5 L 14 11.5 C 14 12 13.5 12.5 13 12.5 L 1 12.5 C 0.5 12.5 0 12 0 11.5 L 0 3.5 C 0 3 0.5 2.5 1 2.5 Z");
                        icon.setFill(Color.web("#35538C"));
                        icon.setStroke(Color.web("#6B9EE8"));
                        icon.setStrokeWidth(1.1);
                    } else {
                        // File icon
                        icon.setContent("M 2 1 L 9 1 L 13 5 L 13 13 C 13 13.5 12.5 14 12 14 L 2 14 C 1.5 14 1 13.5 1 13 L 1 2 C 1 1.5 1.5 1 2 1 Z M 8 1.5 L 8 5.5 L 12 5.5");
                        icon.setFill(Color.web("#2B2D30"));
                        icon.setStroke(Color.web("#868A91"));
                        icon.setStrokeWidth(1.1);
                    }
                    label.setText(item);
                    setGraphic(content);
                    setText(null);
                }
            }
        });

        // Encoding column with in-cell dropdown
        TableColumn<EncodingRow, String> encodingCol = new TableColumn<>("Encoding");
        encodingCol.setCellValueFactory(cell -> cell.getValue().encodingProperty());
        encodingCol.setPrefWidth(160);
        encodingCol.setCellFactory(col -> new TableCell<>() {
            private final Label encLabel = new Label();

            {
                encLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
                setCursor(Cursor.HAND);
                setOnMouseClicked(e -> {
                    if (!isEmpty()) {
                        EncodingRow row = getTableRow().getItem();
                        if (row != null) {
                            showEncodingPicker(this, row);
                        }
                    }
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    encLabel.setText(item);
                    setGraphic(encLabel);
                    setText(null);
                }
            }
        });

        encodingTable.getColumns().addAll(pathCol, encodingCol);
        encodingTable.setItems(encodingData);
    }

    private void showEncodingPicker(TableCell<EncodingRow, String> cell, EncodingRow row) {
        EncodingMenuButton picker = new EncodingMenuButton("<Default>", row.getEncoding());
        picker.setOnEncodingChanged(newEnc -> {
            row.setEncoding(newEnc);
            notifyModified();
        });

        Point2D p = cell.localToScreen(0, cell.getHeight() + 2);
        if (p != null) {
            picker.showPopup();
        }
    }

    private void onAdd() {
        ContextMenu addMenu = new ContextMenu();
        addMenu.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #4E5157; -fx-border-radius: 6;");

        MenuItem dirItem = new MenuItem("Directory…");
        dirItem.setOnAction(e -> {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("Select Directory");
            Path root = settings.getProjectRoot();
            if (root != null && Files.isDirectory(root)) {
                chooser.setInitialDirectory(root.toFile());
            }
            Window w = getScene() != null ? getScene().getWindow() : null;
            File selected = chooser.showDialog(w);
            if (selected != null) {
                addPathMapping(selected.toPath(), true);
            }
        });

        MenuItem fileItem = new MenuItem("File…");
        fileItem.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Select File");
            Path root = settings.getProjectRoot();
            if (root != null && Files.isDirectory(root)) {
                chooser.setInitialDirectory(root.toFile());
            }
            Window w = getScene() != null ? getScene().getWindow() : null;
            File selected = chooser.showOpenDialog(w);
            if (selected != null) {
                addPathMapping(selected.toPath(), false);
            }
        });

        addMenu.getItems().addAll(dirItem, fileItem);
        addMenu.show(addBtn, javafx.geometry.Side.BOTTOM, 0, 0);
    }

    private void addPathMapping(Path path, boolean isDir) {
        Path root = settings.getProjectRoot();
        String rawPath;
        String displayPath;

        if (root != null && path.toAbsolutePath().normalize().startsWith(root.toAbsolutePath().normalize())) {
            Path rel = root.toAbsolutePath().normalize().relativize(path.toAbsolutePath().normalize());
            rawPath = rel.toString().replace('\\', '/');
            displayPath = "...\\" + rel.toString().replace('/', '\\');
        } else {
            rawPath = path.toAbsolutePath().normalize().toString();
            displayPath = rawPath;
        }

        // Avoid duplicate paths
        for (EncodingRow r : encodingData) {
            if (r.getRawPath().equalsIgnoreCase(rawPath)) {
                encodingTable.getSelectionModel().select(r);
                return;
            }
        }

        EncodingRow newRow = new EncodingRow(rawPath, displayPath, "UTF-8", isDir);
        encodingData.add(newRow);
        encodingTable.getSelectionModel().select(newRow);
        encodingTable.scrollTo(newRow);
        notifyModified();
    }

    private void onRemove() {
        EncodingRow selected = encodingTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            encodingData.remove(selected);
            notifyModified();
        }
    }

    private void onEdit() {
        EncodingRow selected = encodingTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            EncodingMenuButton picker = new EncodingMenuButton("<Default>", selected.getEncoding());
            picker.setOnEncodingChanged(newEnc -> {
                selected.setEncoding(newEnc);
                notifyModified();
            });
            picker.showPopup();
        }
    }

    public void reloadFromSettings() {
        settings.load();

        globalEncodingBtn.setSelectedEncoding(settings.getGlobalEncoding());
        projectEncodingBtn.setSelectedEncoding(settings.getProjectEncoding());
        propertiesEncodingBtn.setSelectedEncoding(settings.getPropertiesFileEncoding());
        nativeToAsciiCheck.setSelected(settings.isTransparentNativeToAscii());
        utf8BomCombo.setValue(settings.getCreateUtf8FilesOption());
        updateBomHelperText(utf8BomCombo.getValue());

        encodingData.clear();
        Path root = settings.getProjectRoot();
        for (FileEncodingsSettings.PathMapping m : settings.getMappings()) {
            encodingData.add(new EncodingRow(
                    m.relativeOrAbsolutePath(),
                    m.getDisplayPath(root),
                    m.encoding(),
                    m.isDirectory()
            ));
        }

        recordBaseline();
    }

    private void recordBaseline() {
        baselineGlobal = globalEncodingBtn.getSelectedEncoding();
        baselineProject = projectEncodingBtn.getSelectedEncoding();
        baselineProperties = propertiesEncodingBtn.getSelectedEncoding();
        baselineNativeToAscii = nativeToAsciiCheck.isSelected();
        baselineUtf8Bom = utf8BomCombo.getValue();

        baselineMappings.clear();
        for (EncodingRow r : encodingData) {
            baselineMappings.add(new FileEncodingsSettings.PathMapping(r.getRawPath(), r.getEncoding(), r.isDirectory()));
        }
    }

    public boolean isModified() {
        if (!Objects.equals(baselineGlobal, globalEncodingBtn.getSelectedEncoding())) return true;
        if (!Objects.equals(baselineProject, projectEncodingBtn.getSelectedEncoding())) return true;
        if (!Objects.equals(baselineProperties, propertiesEncodingBtn.getSelectedEncoding())) return true;
        if (baselineNativeToAscii != nativeToAsciiCheck.isSelected()) return true;
        if (!Objects.equals(baselineUtf8Bom, utf8BomCombo.getValue())) return true;

        if (baselineMappings.size() != encodingData.size()) return true;
        for (int i = 0; i < encodingData.size(); i++) {
            EncodingRow r = encodingData.get(i);
            FileEncodingsSettings.PathMapping bm = baselineMappings.get(i);
            if (!r.getRawPath().equals(bm.relativeOrAbsolutePath())
                    || !r.getEncoding().equals(bm.encoding())
                    || r.isDirectory() != bm.isDirectory()) {
                return true;
            }
        }
        return false;
    }

    public void apply() {
        settings.setGlobalEncoding(globalEncodingBtn.getSelectedEncoding());
        settings.setProjectEncoding(projectEncodingBtn.getSelectedEncoding());
        settings.setPropertiesFileEncoding(propertiesEncodingBtn.getSelectedEncoding());
        settings.setTransparentNativeToAscii(nativeToAsciiCheck.isSelected());
        settings.setCreateUtf8FilesOption(utf8BomCombo.getValue());

        List<FileEncodingsSettings.PathMapping> list = new ArrayList<>();
        for (EncodingRow r : encodingData) {
            list.add(new FileEncodingsSettings.PathMapping(r.getRawPath(), r.getEncoding(), r.isDirectory()));
        }
        settings.setMappings(list);

        settings.save();
        recordBaseline();
        notifyModified();
    }

    public void reset() {
        reloadFromSettings();
        notifyModified();
    }

    public void setOnModifiedListener(Runnable listener) {
        this.onModifiedListener = listener;
    }

    private void notifyModified() {
        if (onModifiedListener != null) {
            onModifiedListener.run();
        }
    }

    /**
     * Observable table row model.
     */
    public static class EncodingRow {
        private final SimpleStringProperty rawPath;
        private final SimpleStringProperty displayPath;
        private final SimpleStringProperty encoding;
        private final SimpleBooleanProperty isDirectory;

        public EncodingRow(String rawPath, String displayPath, String encoding, boolean isDirectory) {
            this.rawPath = new SimpleStringProperty(rawPath);
            this.displayPath = new SimpleStringProperty(displayPath);
            this.encoding = new SimpleStringProperty(encoding);
            this.isDirectory = new SimpleBooleanProperty(isDirectory);
        }

        public SimpleStringProperty rawPathProperty() { return rawPath; }
        public SimpleStringProperty displayPathProperty() { return displayPath; }
        public SimpleStringProperty encodingProperty() { return encoding; }
        public SimpleBooleanProperty isDirectoryProperty() { return isDirectory; }

        public String getRawPath() { return rawPath.get(); }
        public String getDisplayPath() { return displayPath.get(); }
        public String getEncoding() { return encoding.get(); }
        public boolean isDirectory() { return isDirectory.get(); }

        public void setEncoding(String enc) { this.encoding.set(enc); }
    }
}