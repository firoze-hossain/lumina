package dev.lumina.ui;

import dev.lumina.tools.CsvFormat;
import dev.lumina.tools.CsvFormatsSettings;
import dev.lumina.tools.CsvFormatsSettingsManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/**
 * Tools > CSV Formats settings page in Lumina IDE matching 1:1 design of the reference IDE.
 * Provides configurable delimiters, quotation rules, and live dual raw text & parsed table preview.
 */
public class SettingsToolsCsvFormatsPage extends VBox {

    public static final String SAMPLE_CSV_DATA =
            "customer_id,first_name,last_name,active,create_date\n" +
            "1,MARY,SMITH,true,2006-02-14\n" +
            "2,PATRICIA,JOHNSON,true,2006-02-14\n" +
            "3,LINDA,WILLIAMS,true,2006-02-14\n" +
            "4,BARBARA,JONES,false,2006-02-14\n" +
            "5,ELIZABETH,BROWN,false,2006-02-14\n" +
            "6,JENNIFER,DAVIS,true,2006-02-14\n" +
            "7,MARIA,MILLER,false,2006-02-14\n" +
            "8,SUSAN,WILSON,true,2006-02-14\n" +
            "9,MARGARET,MOORE,true,2006-02-14\n" +
            "10,DOROTHY,TAYLOR,true,2006-02-14\n" +
            "11,LISA,ANDERSON,true,2006-02-14\n" +
            "12,NANCY,THOMAS,true,2006-02-14\n" +
            "13,KAREN,JACKSON,false,2006-02-14\n" +
            "14,BETTY,WHITE,true,2006-02-14\n" +
            "15,HELEN,HARRIS,true,2006-02-14\n" +
            "16,SANDRA,MARTIN,true,2006-02-14\n" +
            "17,DONNA,THOMPSON,true,2006-02-14\n" +
            "18,CAROL,GARCIA,true,2006-02-14";

    private final CsvFormatsSettingsManager manager;
    private CsvFormatsSettings initialSettings;
    private CsvFormatsSettings currentSettings;
    private Runnable onModifiedListener;
    private boolean updating = false;

    // Left controls
    private ListView<String> formatsListView;
    private ComboBox<String> valueSeparatorCombo;
    private ComboBox<String> rowSeparatorCombo;
    private ComboBox<String> nullValueTextCombo;
    private ListView<String> quotationListView;
    private ComboBox<String> quoteValuesCombo;
    private CheckBox trimWhitespacesCheck;
    private CheckBox firstRowIsHeaderCheck;
    private CheckBox firstColumnIsHeaderCheck;

    // Right preview controls
    private TextArea rawEditorArea;
    private TableView<List<String>> previewTable;

    public SettingsToolsCsvFormatsPage() {
        this.manager = CsvFormatsSettingsManager.getInstance();
        buildUI();
        loadData();
    }

    private void buildUI() {
        setPadding(new Insets(16, 20, 16, 20));
        setSpacing(12);
        setStyle("-fx-background-color: #1E1F22;");

        SplitPane mainSplit = new SplitPane();
        mainSplit.setStyle("-fx-background-color: #1E1F22; -fx-box-border: transparent;");
        VBox.setVgrow(mainSplit, Priority.ALWAYS);

        // ==========================
        // LEFT PANE: Format & Options
        // ==========================
        VBox leftPane = new VBox(10);
        leftPane.setPadding(new Insets(0, 12, 0, 0));
        leftPane.setMinWidth(320);
        leftPane.setPrefWidth(350);
        leftPane.setStyle("-fx-background-color: #1E1F22;");

        // 1. Formats: header & toolbar
        HBox formatsHeader = new HBox(6);
        formatsHeader.setAlignment(Pos.CENTER_LEFT);
        Label formatsLabel = new Label("Formats:");
        formatsLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        Region formatsSpacer = new Region();
        HBox.setHgrow(formatsSpacer, Priority.ALWAYS);

        HBox formatsToolbar = createSmallToolbar(
                () -> addFormat(),
                () -> removeFormat()
        );
        formatsHeader.getChildren().addAll(formatsLabel, formatsSpacer, formatsToolbar);

        formatsListView = new ListView<>();
        formatsListView.setPrefHeight(90);
        formatsListView.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #393B40; -fx-border-radius: 4px;");
        formatsListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (!updating && newVal != null) {
                currentSettings.setSelectedFormatName(newVal);
                updateControlsForSelectedFormat();
                updateLivePreview();
                notifyModified();
            }
        });

        // 2. Format details
        GridPane detailsGrid = new GridPane();
        detailsGrid.setHgap(8);
        detailsGrid.setVgap(8);

        Label valSepLabel = new Label("Value separator:");
        valSepLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        valueSeparatorCombo = new ComboBox<>();
        valueSeparatorCombo.getItems().addAll("Comma", "Tab", "Semicolon", "Pipe", "Space");
        valueSeparatorCombo.setPrefWidth(160);
        styleCombo(valueSeparatorCombo);
        valueSeparatorCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (!updating && newVal != null) {
                CsvFormat sel = currentSettings.getSelectedFormat();
                if (sel != null) sel.setValueSeparator(newVal);
                updateLivePreview();
                notifyModified();
            }
        });

        Label rowSepLabel = new Label("Row separator:");
        rowSepLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        rowSeparatorCombo = new ComboBox<>();
        rowSeparatorCombo.getItems().addAll("Newline", "CRLF", "CR");
        rowSeparatorCombo.setPrefWidth(160);
        styleCombo(rowSeparatorCombo);
        rowSeparatorCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (!updating && newVal != null) {
                CsvFormat sel = currentSettings.getSelectedFormat();
                if (sel != null) sel.setRowSeparator(newVal);
                notifyModified();
            }
        });

        Label nullValLabel = new Label("Null value text:");
        nullValLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        nullValueTextCombo = new ComboBox<>();
        nullValueTextCombo.getItems().addAll("Empty string", "null", "\\N");
        nullValueTextCombo.setPrefWidth(160);
        styleCombo(nullValueTextCombo);
        nullValueTextCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (!updating && newVal != null) {
                CsvFormat sel = currentSettings.getSelectedFormat();
                if (sel != null) sel.setNullValueText(newVal);
                notifyModified();
            }
        });

        detailsGrid.add(valSepLabel, 0, 0);
        detailsGrid.add(valueSeparatorCombo, 1, 0);
        detailsGrid.add(rowSepLabel, 0, 1);
        detailsGrid.add(rowSeparatorCombo, 1, 1);
        detailsGrid.add(nullValLabel, 0, 2);
        detailsGrid.add(nullValueTextCombo, 1, 2);

        Hyperlink prefixSuffixLink = new Hyperlink("Add row prefix/suffix");
        prefixSuffixLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-padding: 0;");

        // 3. Quotation section
        HBox quoteHeader = new HBox(6);
        quoteHeader.setAlignment(Pos.CENTER_LEFT);
        Label quoteLabel = new Label("Quotation:");
        quoteLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        Region quoteSpacer = new Region();
        HBox.setHgrow(quoteSpacer, Priority.ALWAYS);
        HBox quoteToolbar = createSmallToolbar(null, null);
        quoteHeader.getChildren().addAll(quoteLabel, quoteSpacer, quoteToolbar);

        quotationListView = new ListView<>();
        quotationListView.setPrefHeight(60);
        quotationListView.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #393B40; -fx-border-radius: 4px;");

        HBox quoteValuesRow = new HBox(8);
        quoteValuesRow.setAlignment(Pos.CENTER_LEFT);
        Label quoteValuesLabel = new Label("Quote values:");
        quoteValuesLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        quoteValuesCombo = new ComboBox<>();
        quoteValuesCombo.getItems().addAll("When needed", "Always", "Never");
        quoteValuesCombo.setPrefWidth(160);
        styleCombo(quoteValuesCombo);
        quoteValuesCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (!updating && newVal != null) {
                CsvFormat sel = currentSettings.getSelectedFormat();
                if (sel != null) sel.setQuoteValues(newVal);
                notifyModified();
            }
        });
        quoteValuesRow.getChildren().addAll(quoteValuesLabel, quoteValuesCombo);

        trimWhitespacesCheck = new CheckBox("Trim whitespaces");
        styleCheck(trimWhitespacesCheck);
        trimWhitespacesCheck.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (!updating) {
                CsvFormat sel = currentSettings.getSelectedFormat();
                if (sel != null) sel.setTrimWhitespaces(newVal);
                updateLivePreview();
                notifyModified();
            }
        });

        firstRowIsHeaderCheck = new CheckBox("First row is header");
        styleCheck(firstRowIsHeaderCheck);
        firstRowIsHeaderCheck.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (!updating) {
                CsvFormat sel = currentSettings.getSelectedFormat();
                if (sel != null) sel.setFirstRowIsHeader(newVal);
                updateLivePreview();
                notifyModified();
            }
        });

        firstColumnIsHeaderCheck = new CheckBox("First column is header");
        styleCheck(firstColumnIsHeaderCheck);
        firstColumnIsHeaderCheck.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (!updating) {
                CsvFormat sel = currentSettings.getSelectedFormat();
                if (sel != null) sel.setFirstColumnIsHeader(newVal);
                notifyModified();
            }
        });

        leftPane.getChildren().addAll(
                formatsHeader, formatsListView, detailsGrid, prefixSuffixLink,
                quoteHeader, quotationListView, quoteValuesRow,
                trimWhitespacesCheck, firstRowIsHeaderCheck, firstColumnIsHeaderCheck
        );

        // ==========================
        // RIGHT PANE: Live Previews
        // ==========================
        VBox rightPane = new VBox(8);
        rightPane.setPadding(new Insets(0, 0, 0, 12));
        HBox.setHgrow(rightPane, Priority.ALWAYS);
        rightPane.setStyle("-fx-background-color: #1E1F22;");

        // Upper raw editor area with line numbers
        rawEditorArea = new TextArea(SAMPLE_CSV_DATA);
        rawEditorArea.setPrefHeight(230);
        rawEditorArea.setStyle(
                "-fx-control-inner-background: #1E1F22; " +
                "-fx-background-color: #1E1F22; " +
                "-fx-text-fill: #DFE1E5; " +
                "-fx-font-family: monospace; " +
                "-fx-font-size: 12px; " +
                "-fx-border-color: #393B40; " +
                "-fx-border-radius: 4px;"
        );
        rawEditorArea.textProperty().addListener((obs, oldVal, newVal) -> updateLivePreview());

        // Lower parsed TableView grid
        previewTable = new TableView<>();
        previewTable.setStyle(
                "-fx-background-color: #1E1F22; " +
                "-fx-border-color: #393B40; " +
                "-fx-border-radius: 4px;"
        );
        VBox.setVgrow(previewTable, Priority.ALWAYS);

        rightPane.getChildren().addAll(rawEditorArea, previewTable);

        mainSplit.getItems().addAll(leftPane, rightPane);
        mainSplit.setDividerPositions(0.38);

        getChildren().add(mainSplit);
    }

    private void styleCombo(ComboBox<String> combo) {
        combo.setStyle(
                "-fx-background-color: #2B2D30; " +
                "-fx-border-color: #393B40; " +
                "-fx-border-radius: 4px; " +
                "-fx-background-radius: 4px; " +
                "-fx-text-fill: #DFE1E5; " +
                "-fx-font-size: 13px;"
        );
    }

    private void styleCheck(CheckBox cb) {
        cb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
    }

    private HBox createSmallToolbar(Runnable onAdd, Runnable onRemove) {
        HBox tb = new HBox(2);
        Button addBtn = new Button("+");
        addBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-padding: 1 5 1 5;");
        if (onAdd != null) addBtn.setOnAction(e -> onAdd.run());

        Button remBtn = new Button("−");
        remBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-padding: 1 5 1 5;");
        if (onRemove != null) remBtn.setOnAction(e -> onRemove.run());

        tb.getChildren().addAll(addBtn, remBtn);
        return tb;
    }

    private void addFormat() {
        String name = "Custom " + (currentSettings.getFormats().size() + 1);
        CsvFormat fmt = new CsvFormat(name, "Comma");
        currentSettings.getFormats().add(fmt);
        formatsListView.getItems().add(name);
        formatsListView.getSelectionModel().select(name);
        notifyModified();
    }

    private void removeFormat() {
        String selected = formatsListView.getSelectionModel().getSelectedItem();
        if (selected != null && currentSettings.getFormats().size() > 1) {
            currentSettings.getFormats().removeIf(f -> f.getName().equals(selected));
            formatsListView.getItems().remove(selected);
            formatsListView.getSelectionModel().selectFirst();
            notifyModified();
        }
    }

    private void loadData() {
        updating = true;
        currentSettings = manager.getSettings();
        formatsListView.getItems().clear();
        for (CsvFormat f : currentSettings.getFormats()) {
            formatsListView.getItems().add(f.getName());
        }
        formatsListView.getSelectionModel().select(currentSettings.getSelectedFormatName());
        updateControlsForSelectedFormat();
        updateLivePreview();
        initialSettings = currentSettings.clone();
        updating = false;
    }

    private void updateControlsForSelectedFormat() {
        CsvFormat format = currentSettings.getSelectedFormat();
        if (format == null) return;
        valueSeparatorCombo.setValue(format.getValueSeparator());
        rowSeparatorCombo.setValue(format.getRowSeparator());
        nullValueTextCombo.setValue(format.getNullValueText());
        quotationListView.getItems().setAll(format.getQuotationRules());
        quoteValuesCombo.setValue(format.getQuoteValues());
        trimWhitespacesCheck.setSelected(format.isTrimWhitespaces());
        firstRowIsHeaderCheck.setSelected(format.isFirstRowIsHeader());
        firstColumnIsHeaderCheck.setSelected(format.isFirstColumnIsHeader());
    }

    private void updateLivePreview() {
        CsvFormat format = currentSettings.getSelectedFormat();
        String raw = rawEditorArea.getText();
        List<List<String>> parsed = manager.parseData(raw, format);

        previewTable.getColumns().clear();
        previewTable.getItems().clear();

        if (parsed.isEmpty()) return;

        boolean hasHeader = format != null && format.isFirstRowIsHeader();
        List<String> headerRow = hasHeader ? parsed.get(0) : null;
        int startIndex = hasHeader ? 1 : 0;

        int numCols = parsed.get(0).size();
        for (int c = 0; c < numCols; c++) {
            final int colIndex = c;
            String colName = (headerRow != null && c < headerRow.size()) ? headerRow.get(c) : "C" + (c + 1);
            TableColumn<List<String>, String> col = new TableColumn<>(colName);
            col.setCellValueFactory(data -> {
                List<String> row = data.getValue();
                String val = (colIndex < row.size()) ? row.get(colIndex) : "";
                return new SimpleStringProperty(val);
            });
            col.setPrefWidth(95);
            previewTable.getColumns().add(col);
        }

        for (int r = startIndex; r < parsed.size(); r++) {
            previewTable.getItems().add(parsed.get(r));
        }
    }

    public boolean isModified() {
        if (initialSettings == null || currentSettings == null) return false;
        return !Objects.equals(initialSettings, currentSettings);
    }

    public void apply() {
        manager.setSettings(currentSettings);
        initialSettings = currentSettings.clone();
        notifyModified();
    }

    public void reset() {
        loadData();
        notifyModified();
    }

    public void revertChanges() {
        reset();
    }

    public void setOnModifiedListener(Runnable listener) {
        this.onModifiedListener = listener;
    }

    private void notifyModified() {
        if (!updating && onModifiedListener != null) {
            onModifiedListener.run();
        }
    }

    public ListView<String> getFormatsListView() {
        return formatsListView;
    }

    public ComboBox<String> getValueSeparatorCombo() {
        return valueSeparatorCombo;
    }

    public TableView<List<String>> getPreviewTable() {
        return previewTable;
    }

    public CheckBox getTrimWhitespacesCheck() {
        return trimWhitespacesCheck;
    }

    public CheckBox getFirstRowIsHeaderCheck() {
        return firstRowIsHeaderCheck;
    }

    public CheckBox getFirstColumnIsHeaderCheck() {
        return firstColumnIsHeaderCheck;
    }

    public CsvFormat getSelectedFormat() {
        String sel = formatsListView != null ? formatsListView.getSelectionModel().getSelectedItem() : null;
        if (sel == null || currentSettings == null) return null;
        return currentSettings.getFormatByName(sel);
    }
}
