package dev.lumina.ui;

import dev.lumina.database.DatabaseDataEditorSettings;
import dev.lumina.database.DatabaseDataEditorSettingsManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Tools > Database > Data Editor and Viewer settings page in Lumina IDE matching 1:1 design of the reference IDE.
 */
public class SettingsDatabaseDataEditorViewerPage extends ScrollPane {

    private final DatabaseDataEditorSettingsManager manager;
    private DatabaseDataEditorSettings initialSettings;
    private Runnable onModifiedListener;
    private boolean updating = false;
    private final Consumer<String> onNavigate;

    // Limitations controls
    private CheckBox limitPageSizeCheck;
    private TextField pageSizeField;
    private TextField resultSetPrefetchSizeField;
    private TextField filterHistorySizeField;
    private TextField maxBytesPerValueField;
    private CheckBox showFirstRowsCheck;
    private TextField previewRowCountField;

    // Controls Customization
    private CheckBox enablePagingInEditorCheck;
    private ComboBox<String> gridPaginationPosCombo;
    private CheckBox showQuickActionsToolbarCheck;
    private CheckBox enableQuickActionsCustomCheck;

    // Data Presentation
    private Hyperlink appearanceLink;
    private ComboBox<String> transposeTablesCombo;
    private CheckBox detectBinaryTextCheck;
    private CheckBox detectBinaryUuidCheck;
    private CheckBox enableLocalFilterCheck;
    private CheckBox enableImmediateCompletionCheck;
    private TextField temporalTimeZoneField;

    // Custom Number Formats
    private TextField decimalSeparatorField;
    private CheckBox useGroupingSeparatorCheck;
    private TextField groupingSeparatorField;
    private TextField infinityTextField;
    private TextField nanTextField;
    private CheckBox useNumberPatternCheck;
    private TextField numberPatternField;
    private Label numberPatternPreview;

    // Custom Date/Time Formats
    private CheckBox useDatetimeCheck;
    private TextField datetimePatternField;
    private Label datetimePreview;

    private CheckBox useDatetimeTzCheck;
    private TextField datetimeTzPatternField;
    private Label datetimeTzPreview;

    private CheckBox useTimeCheck;
    private TextField timePatternField;
    private Label timePreview;

    private CheckBox useTimeTzCheck;
    private TextField timeTzPatternField;
    private Label timeTzPreview;

    private CheckBox useDateCheck;
    private TextField datePatternField;
    private Label datePreview;

    // Data Sorting
    private CheckBox sortViaOrderByCheck;
    private CheckBox sortNumericPkCheck;
    private RadioButton pkAscendingRadio;
    private RadioButton pkDescendingRadio;
    private ToggleGroup pkSortGroup;
    private RadioButton addColAltClickRadio;
    private RadioButton addColClickRadio;
    private ToggleGroup addColGroup;

    // Data Modification
    private CheckBox submitChangesImmediatelyCheck;
    private CheckBox enableEditingJoinQueriesCheck;
    private CheckBox showDmlPreviewJoinCheck;

    // URL Click Settings
    private CheckBox openSecureLinksCheck;
    private CheckBox openStandardLinksCheck;
    private CheckBox openLocalFileLinksCheck;
    private CheckBox assumeHttpCheck;

    public SettingsDatabaseDataEditorViewerPage() {
        this(null);
    }

    public SettingsDatabaseDataEditorViewerPage(Consumer<String> onNavigate) {
        this.manager = DatabaseDataEditorSettingsManager.getInstance();
        this.onNavigate = onNavigate;
        buildUI();
        loadData();
    }

    private void buildUI() {
        setFitToWidth(true);
        setStyle("-fx-background: #1E1F22; -fx-background-color: #1E1F22;");

        VBox content = new VBox(20);
        content.setPadding(new Insets(20, 24, 20, 24));
        content.setStyle("-fx-background-color: #1E1F22;");

        // 1. Limitations
        VBox limitationsSection = buildLimitationsSection();

        // 2. Controls Customization
        VBox controlsSection = buildControlsCustomizationSection();

        // 3. Data Presentation
        VBox presentationSection = buildDataPresentationSection();

        // 4. Custom Number Formats
        VBox numberFormatsSection = buildCustomNumberFormatsSection();

        // 5. Custom Date/Time Formats
        VBox dateTimeFormatsSection = buildCustomDateTimeFormatsSection();

        // 6. Data Sorting
        VBox sortingSection = buildDataSortingSection();

        // 7. Data Modification
        VBox modificationSection = buildDataModificationSection();

        // 8. URL Click Settings
        VBox urlSection = buildUrlClickSection();

        content.getChildren().addAll(
                limitationsSection,
                controlsSection,
                presentationSection,
                numberFormatsSection,
                dateTimeFormatsSection,
                sortingSection,
                modificationSection,
                urlSection
        );

        setContent(content);
    }

    private VBox buildLimitationsSection() {
        VBox section = new VBox(8);
        section.getChildren().add(createSectionHeader("Limitations"));

        // Limit page size to
        limitPageSizeCheck = createCheckBox("Limit page size to:");
        pageSizeField = createNumberTextField(60);
        HBox limitPageRow = new HBox(8, limitPageSizeCheck, pageSizeField);
        limitPageRow.setAlignment(Pos.CENTER_LEFT);

        // Result set prefetch size
        Label prefetchLabel = createFieldLabel("Result set prefetch size:");
        prefetchLabel.setPrefWidth(260);
        resultSetPrefetchSizeField = createNumberTextField(60);
        HBox prefetchRow = new HBox(8, prefetchLabel, resultSetPrefetchSizeField);
        prefetchRow.setAlignment(Pos.CENTER_LEFT);

        // Filter history size
        Label filterLabel = createFieldLabel("Filter history size:");
        filterLabel.setPrefWidth(260);
        filterHistorySizeField = createNumberTextField(60);
        HBox filterRow = new HBox(8, filterLabel, filterHistorySizeField);
        filterRow.setAlignment(Pos.CENTER_LEFT);

        // Maximum number of bytes loaded per value
        Label maxBytesLabel = createFieldLabel("Maximum number of bytes loaded per value:");
        maxBytesLabel.setPrefWidth(260);
        maxBytesPerValueField = createNumberTextField(80);
        HBox maxBytesRow = new HBox(8, maxBytesLabel, maxBytesPerValueField);
        maxBytesRow.setAlignment(Pos.CENTER_LEFT);

        // Show first N data rows in preview
        showFirstRowsCheck = createCheckBox("Show first");
        previewRowCountField = createNumberTextField(50);
        Label previewSuffix = createFieldLabel("data rows in preview");
        HBox previewRowsRow = new HBox(8, showFirstRowsCheck, previewRowCountField, previewSuffix);
        previewRowsRow.setAlignment(Pos.CENTER_LEFT);

        section.getChildren().addAll(limitPageRow, prefetchRow, filterRow, maxBytesRow, previewRowsRow);
        return section;
    }

    private VBox buildControlsCustomizationSection() {
        VBox section = new VBox(8);
        section.getChildren().add(createSectionHeader("Controls Customization"));

        enablePagingInEditorCheck = createCheckBox("Enable paging in in-editor results by default");

        Label gridPosLabel = createFieldLabel("Position of the grid pagination control:");
        gridPaginationPosCombo = new ComboBox<>();
        gridPaginationPosCombo.getItems().addAll("Grid bottom (floating)", "Grid top", "Grid bottom", "Hidden");
        gridPaginationPosCombo.setPrefWidth(200);
        styleComboBox(gridPaginationPosCombo);
        gridPaginationPosCombo.valueProperty().addListener((obs, oldV, newV) -> {
            if (!updating) notifyModified();
        });
        HBox gridPosRow = new HBox(8, gridPosLabel, gridPaginationPosCombo);
        gridPosRow.setAlignment(Pos.CENTER_LEFT);

        showQuickActionsToolbarCheck = createCheckBox("Show the quick actions popup toolbar for cells");
        enableQuickActionsCustomCheck = createCheckBox("Enable customization of the quick actions popup toolbar");
        VBox.setMargin(enableQuickActionsCustomCheck, new Insets(0, 0, 0, 20));

        section.getChildren().addAll(enablePagingInEditorCheck, gridPosRow, showQuickActionsToolbarCheck, enableQuickActionsCustomCheck);
        return section;
    }

    private VBox buildDataPresentationSection() {
        VBox section = new VBox(8);
        section.getChildren().add(createSectionHeader("Data Presentation"));

        Label movedLabel = createFieldLabel("Appearance settings were moved to:");
        appearanceLink = new Hyperlink("Appearance & Behavior | Data Editor and Viewer");
        appearanceLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 13px; -fx-padding: 0;");
        appearanceLink.setOnAction(e -> {
            if (onNavigate != null) {
                onNavigate.accept("Appearance & Behavior");
            }
        });
        HBox movedRow = new HBox(6, movedLabel, appearanceLink);
        movedRow.setAlignment(Pos.CENTER_LEFT);

        Label transposeLabel = createFieldLabel("Automatically transpose tables:");
        transposeLabel.setPrefWidth(240);
        transposeTablesCombo = new ComboBox<>();
        transposeTablesCombo.getItems().addAll("Never", "Always", "When width exceeds screen");
        transposeTablesCombo.setPrefWidth(180);
        styleComboBox(transposeTablesCombo);
        transposeTablesCombo.valueProperty().addListener((obs, oldV, newV) -> {
            if (!updating) notifyModified();
        });
        HBox transposeRow = new HBox(8, transposeLabel, transposeTablesCombo);
        transposeRow.setAlignment(Pos.CENTER_LEFT);

        Label binLabel = createFieldLabel("Automatically detect binary values:");
        detectBinaryTextCheck = createCheckBox("Text");
        detectBinaryUuidCheck = createCheckBox("UUID");
        HBox binRow = new HBox(12, binLabel, detectBinaryTextCheck, detectBinaryUuidCheck);
        binRow.setAlignment(Pos.CENTER_LEFT);

        enableLocalFilterCheck = createCheckBox("Enable local filter by default");
        enableImmediateCompletionCheck = createCheckBox("Enable immediate completion in grid text cells");

        Label tzLabel = createFieldLabel("Display temporal data in time zone:");
        tzLabel.setPrefWidth(240);
        temporalTimeZoneField = createTextField(200);
        HBox tzRow = new HBox(8, tzLabel, temporalTimeZoneField);
        tzRow.setAlignment(Pos.CENTER_LEFT);

        section.getChildren().addAll(movedRow, transposeRow, binRow, enableLocalFilterCheck, enableImmediateCompletionCheck, tzRow);
        return section;
    }

    private VBox buildCustomNumberFormatsSection() {
        VBox section = new VBox(8);

        HBox headerRow = new HBox(12);
        headerRow.setAlignment(Pos.CENTER_LEFT);
        headerRow.getChildren().add(createSectionHeader("Custom Number Formats"));
        Hyperlink numPatternsLink = new Hyperlink("Number patterns ↗");
        numPatternsLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-padding: 0;");
        headerRow.getChildren().add(numPatternsLink);
        section.getChildren().add(headerRow);

        Label decSepLabel = createFieldLabel("Decimal separator:");
        decSepLabel.setPrefWidth(150);
        decimalSeparatorField = createTextField(50);
        HBox decSepRow = new HBox(8, decSepLabel, decimalSeparatorField);
        decSepRow.setAlignment(Pos.CENTER_LEFT);

        useGroupingSeparatorCheck = createCheckBox("Grouping separator:");
        useGroupingSeparatorCheck.setPrefWidth(150);
        groupingSeparatorField = createTextField(60);
        HBox grpSepRow = new HBox(8, useGroupingSeparatorCheck, groupingSeparatorField);
        grpSepRow.setAlignment(Pos.CENTER_LEFT);

        Label infLabel = createFieldLabel("Infinity:");
        infLabel.setPrefWidth(150);
        infinityTextField = createTextField(100);
        HBox infRow = new HBox(8, infLabel, infinityTextField);
        infRow.setAlignment(Pos.CENTER_LEFT);

        Label nanLabel = createFieldLabel("NaN:");
        nanLabel.setPrefWidth(150);
        nanTextField = createTextField(100);
        HBox nanRow = new HBox(8, nanLabel, nanTextField);
        nanRow.setAlignment(Pos.CENTER_LEFT);

        useNumberPatternCheck = createCheckBox("Number pattern");
        useNumberPatternCheck.setPrefWidth(150);
        numberPatternField = createTextField(120);
        numberPatternPreview = new Label("123456789.123456");
        numberPatternPreview.setStyle("-fx-text-fill: #8C8C8C; -fx-font-size: 13px;");
        HBox numPatRow = new HBox(8, useNumberPatternCheck, numberPatternField, numberPatternPreview);
        numPatRow.setAlignment(Pos.CENTER_LEFT);

        section.getChildren().addAll(decSepRow, grpSepRow, infRow, nanRow, numPatRow);
        return section;
    }

    private VBox buildCustomDateTimeFormatsSection() {
        VBox section = new VBox(8);

        HBox headerRow = new HBox(12);
        headerRow.setAlignment(Pos.CENTER_LEFT);
        headerRow.getChildren().add(createSectionHeader("Custom Date/Time Formats"));
        Hyperlink datePatternsLink = new Hyperlink("Date patterns ↗");
        datePatternsLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-padding: 0;");
        headerRow.getChildren().add(datePatternsLink);
        section.getChildren().add(headerRow);

        ZonedDateTime now = ZonedDateTime.now();

        // Datetime/timestamp
        useDatetimeCheck = createCheckBox("Datetime/timestamp");
        useDatetimeCheck.setPrefWidth(220);
        datetimePatternField = createTextField(160);
        datetimePreview = new Label(formatQuietly(now, "yyyy-MM-dd HH:mm:ss"));
        datetimePreview.setStyle("-fx-text-fill: #8C8C8C; -fx-font-size: 13px;");
        datetimePatternField.textProperty().addListener((obs, o, n) -> datetimePreview.setText(formatQuietly(now, n)));
        HBox dtRow = new HBox(8, useDatetimeCheck, datetimePatternField, datetimePreview);
        dtRow.setAlignment(Pos.CENTER_LEFT);

        // Datetime/timestamp with time zone
        useDatetimeTzCheck = createCheckBox("Datetime/timestamp with time zone");
        useDatetimeTzCheck.setPrefWidth(220);
        datetimeTzPatternField = createTextField(160);
        datetimeTzPreview = new Label(formatQuietly(now, "yyyy-MM-dd HH:mm:ss Z"));
        datetimeTzPreview.setStyle("-fx-text-fill: #8C8C8C; -fx-font-size: 13px;");
        datetimeTzPatternField.textProperty().addListener((obs, o, n) -> datetimeTzPreview.setText(formatQuietly(now, n)));
        HBox dtTzRow = new HBox(8, useDatetimeTzCheck, datetimeTzPatternField, datetimeTzPreview);
        dtTzRow.setAlignment(Pos.CENTER_LEFT);

        // Time
        useTimeCheck = createCheckBox("Time");
        useTimeCheck.setPrefWidth(220);
        timePatternField = createTextField(160);
        timePreview = new Label(formatQuietly(now, "HH:mm:ss"));
        timePreview.setStyle("-fx-text-fill: #8C8C8C; -fx-font-size: 13px;");
        timePatternField.textProperty().addListener((obs, o, n) -> timePreview.setText(formatQuietly(now, n)));
        HBox tmRow = new HBox(8, useTimeCheck, timePatternField, timePreview);
        tmRow.setAlignment(Pos.CENTER_LEFT);

        // Time with time zone
        useTimeTzCheck = createCheckBox("Time with time zone");
        useTimeTzCheck.setPrefWidth(220);
        timeTzPatternField = createTextField(160);
        timeTzPreview = new Label(formatQuietly(now, "HH:mm:ss Z"));
        timeTzPreview.setStyle("-fx-text-fill: #8C8C8C; -fx-font-size: 13px;");
        timeTzPatternField.textProperty().addListener((obs, o, n) -> timeTzPreview.setText(formatQuietly(now, n)));
        HBox tmTzRow = new HBox(8, useTimeTzCheck, timeTzPatternField, timeTzPreview);
        tmTzRow.setAlignment(Pos.CENTER_LEFT);

        // Date
        useDateCheck = createCheckBox("Date");
        useDateCheck.setPrefWidth(220);
        datePatternField = createTextField(160);
        datePreview = new Label(formatQuietly(now, "yyyy-MM-dd"));
        datePreview.setStyle("-fx-text-fill: #8C8C8C; -fx-font-size: 13px;");
        datePatternField.textProperty().addListener((obs, o, n) -> datePreview.setText(formatQuietly(now, n)));
        HBox dateRow = new HBox(8, useDateCheck, datePatternField, datePreview);
        dateRow.setAlignment(Pos.CENTER_LEFT);

        section.getChildren().addAll(dtRow, dtTzRow, tmRow, tmTzRow, dateRow);
        return section;
    }

    private VBox buildDataSortingSection() {
        VBox section = new VBox(8);
        section.getChildren().add(createSectionHeader("Data Sorting"));

        sortViaOrderByCheck = createCheckBox("Sort via ORDER BY");

        sortNumericPkCheck = createCheckBox("Sort tables by numeric primary key:");
        pkSortGroup = new ToggleGroup();
        pkAscendingRadio = new RadioButton("Ascending");
        pkAscendingRadio.setToggleGroup(pkSortGroup);
        pkAscendingRadio.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        pkDescendingRadio = new RadioButton("Descending");
        pkDescendingRadio.setToggleGroup(pkSortGroup);
        pkDescendingRadio.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        pkSortGroup.selectedToggleProperty().addListener((obs, o, n) -> {
            if (!updating) notifyModified();
        });
        HBox pkSortRow = new HBox(12, sortNumericPkCheck, pkAscendingRadio, pkDescendingRadio);
        pkSortRow.setAlignment(Pos.CENTER_LEFT);

        Label addColLabel = createFieldLabel("Add columns to sorting:");
        addColGroup = new ToggleGroup();
        addColAltClickRadio = new RadioButton("⌥Click");
        addColAltClickRadio.setToggleGroup(addColGroup);
        addColAltClickRadio.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        addColClickRadio = new RadioButton("Click");
        addColClickRadio.setToggleGroup(addColGroup);
        addColClickRadio.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        addColGroup.selectedToggleProperty().addListener((obs, o, n) -> {
            if (!updating) notifyModified();
        });
        HBox addColRow = new HBox(12, addColLabel, addColAltClickRadio, addColClickRadio);
        addColRow.setAlignment(Pos.CENTER_LEFT);

        section.getChildren().addAll(sortViaOrderByCheck, pkSortRow, addColRow);
        return section;
    }

    private VBox buildDataModificationSection() {
        VBox section = new VBox(8);
        section.getChildren().add(createSectionHeader("Data Modification"));

        submitChangesImmediatelyCheck = createCheckBox("Submit changes immediately");
        enableEditingJoinQueriesCheck = createCheckBox("Enable editing for queries with JOIN clauses");
        showDmlPreviewJoinCheck = createCheckBox("Show DML preview before submitting changes for queries with JOIN clauses");

        section.getChildren().addAll(submitChangesImmediatelyCheck, enableEditingJoinQueriesCheck, showDmlPreviewJoinCheck);
        return section;
    }

    private VBox buildUrlClickSection() {
        VBox section = new VBox(8);
        section.getChildren().add(createSectionHeader("URL Click Settings"));

        Label allowLabel = createFieldLabel("Allow opening:");
        openSecureLinksCheck = createCheckBox("Secure links (HTTPS)");
        openStandardLinksCheck = createCheckBox("Standard links (HTTP)");
        openLocalFileLinksCheck = createCheckBox("Local file links");
        assumeHttpCheck = createCheckBox("If no protocol is specified, assume HTTP for URLs");

        VBox linksBox = new VBox(6, openSecureLinksCheck, openStandardLinksCheck, openLocalFileLinksCheck, assumeHttpCheck);
        linksBox.setPadding(new Insets(0, 0, 0, 16));

        section.getChildren().addAll(allowLabel, linksBox);
        return section;
    }

    private Label createSectionHeader(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold;");
        return l;
    }

    private Label createFieldLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        return l;
    }

    private CheckBox createCheckBox(String text) {
        CheckBox cb = new CheckBox(text);
        cb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        cb.selectedProperty().addListener((obs, o, n) -> {
            if (!updating) notifyModified();
        });
        return cb;
    }

    private TextField createTextField(double width) {
        TextField tf = new TextField();
        tf.setPrefWidth(width);
        tf.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 3 7 3 7;");
        tf.textProperty().addListener((obs, o, n) -> {
            if (!updating) notifyModified();
        });
        return tf;
    }

    private TextField createNumberTextField(double width) {
        TextField tf = createTextField(width);
        tf.setTextFormatter(new TextFormatter<>(change -> {
            if (change.getText().matches("\\d*")) {
                return change;
            }
            return null;
        }));
        return tf;
    }

    private void styleComboBox(ComboBox<String> cb) {
        cb.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4;");
    }

    private String formatQuietly(ZonedDateTime dt, String pattern) {
        if (pattern == null || pattern.isBlank()) return "";
        try {
            return dt.format(DateTimeFormatter.ofPattern(pattern));
        } catch (Exception e) {
            return "Invalid pattern";
        }
    }

    private void loadData() {
        updating = true;
        try {
            initialSettings = manager.getSettings();

            // Limitations
            limitPageSizeCheck.setSelected(initialSettings.isLimitPageSize());
            pageSizeField.setText(String.valueOf(initialSettings.getPageSize()));
            resultSetPrefetchSizeField.setText(String.valueOf(initialSettings.getResultSetPrefetchSize()));
            filterHistorySizeField.setText(String.valueOf(initialSettings.getFilterHistorySize()));
            maxBytesPerValueField.setText(String.valueOf(initialSettings.getMaxBytesPerValue()));
            showFirstRowsCheck.setSelected(initialSettings.isShowFirstDataRowsInPreview());
            previewRowCountField.setText(String.valueOf(initialSettings.getPreviewRowCount()));

            // Controls
            enablePagingInEditorCheck.setSelected(initialSettings.isEnablePagingInEditorResults());
            gridPaginationPosCombo.setValue(initialSettings.getGridPaginationControlPosition());
            showQuickActionsToolbarCheck.setSelected(initialSettings.isShowQuickActionsPopupToolbar());
            enableQuickActionsCustomCheck.setSelected(initialSettings.isEnableQuickActionsCustomization());

            // Presentation
            transposeTablesCombo.setValue(initialSettings.getAutomaticallyTransposeTables());
            detectBinaryTextCheck.setSelected(initialSettings.isDetectBinaryText());
            detectBinaryUuidCheck.setSelected(initialSettings.isDetectBinaryUuid());
            enableLocalFilterCheck.setSelected(initialSettings.isEnableLocalFilterByDefault());
            enableImmediateCompletionCheck.setSelected(initialSettings.isEnableImmediateCompletion());
            temporalTimeZoneField.setText(initialSettings.getDisplayTemporalDataTimeZone());

            // Numbers
            decimalSeparatorField.setText(initialSettings.getDecimalSeparator());
            useGroupingSeparatorCheck.setSelected(initialSettings.isUseGroupingSeparator());
            groupingSeparatorField.setText(initialSettings.getGroupingSeparator());
            infinityTextField.setText(initialSettings.getInfinityText());
            nanTextField.setText(initialSettings.getNanText());
            useNumberPatternCheck.setSelected(initialSettings.isUseNumberPattern());
            numberPatternField.setText(initialSettings.getNumberPattern());

            // Dates
            useDatetimeCheck.setSelected(initialSettings.isUseDatetimeTimestamp());
            datetimePatternField.setText(initialSettings.getDatetimeTimestampPattern());
            useDatetimeTzCheck.setSelected(initialSettings.isUseDatetimeWithTz());
            datetimeTzPatternField.setText(initialSettings.getDatetimeWithTzPattern());
            useTimeCheck.setSelected(initialSettings.isUseTime());
            timePatternField.setText(initialSettings.getTimePattern());
            useTimeTzCheck.setSelected(initialSettings.isUseTimeWithTz());
            timeTzPatternField.setText(initialSettings.getTimeWithTzPattern());
            useDateCheck.setSelected(initialSettings.isUseDate());
            datePatternField.setText(initialSettings.getDatePattern());

            // Sorting
            sortViaOrderByCheck.setSelected(initialSettings.isSortViaOrderBy());
            sortNumericPkCheck.setSelected(initialSettings.isSortTablesByNumericPrimaryKey());
            if ("Descending".equalsIgnoreCase(initialSettings.getNumericPrimaryKeyDirection())) {
                pkDescendingRadio.setSelected(true);
            } else {
                pkAscendingRadio.setSelected(true);
            }
            if ("Click".equalsIgnoreCase(initialSettings.getAddColumnsToSorting())) {
                addColClickRadio.setSelected(true);
            } else {
                addColAltClickRadio.setSelected(true);
            }

            // Modification
            submitChangesImmediatelyCheck.setSelected(initialSettings.isSubmitChangesImmediately());
            enableEditingJoinQueriesCheck.setSelected(initialSettings.isEnableEditingForJoinQueries());
            showDmlPreviewJoinCheck.setSelected(initialSettings.isShowDmlPreviewForJoinQueries());

            // URL
            openSecureLinksCheck.setSelected(initialSettings.isAllowOpeningSecureLinks());
            openStandardLinksCheck.setSelected(initialSettings.isAllowOpeningStandardLinks());
            openLocalFileLinksCheck.setSelected(initialSettings.isAllowOpeningLocalFileLinks());
            assumeHttpCheck.setSelected(initialSettings.isAssumeHttpIfNoProtocol());
        } finally {
            updating = false;
        }
    }

    private DatabaseDataEditorSettings getCurrentSettingsFromUI() {
        DatabaseDataEditorSettings s = new DatabaseDataEditorSettings();

        // Limitations
        s.setLimitPageSize(limitPageSizeCheck.isSelected());
        s.setPageSize(parseInt(pageSizeField.getText(), 500));
        s.setResultSetPrefetchSize(parseInt(resultSetPrefetchSizeField.getText(), 100));
        s.setFilterHistorySize(parseInt(filterHistorySizeField.getText(), 10));
        s.setMaxBytesPerValue(parseInt(maxBytesPerValueField.getText(), 204800));
        s.setShowFirstDataRowsInPreview(showFirstRowsCheck.isSelected());
        s.setPreviewRowCount(parseInt(previewRowCountField.getText(), 10));

        // Controls
        s.setEnablePagingInEditorResults(enablePagingInEditorCheck.isSelected());
        s.setGridPaginationControlPosition(gridPaginationPosCombo.getValue());
        s.setShowQuickActionsPopupToolbar(showQuickActionsToolbarCheck.isSelected());
        s.setEnableQuickActionsCustomization(enableQuickActionsCustomCheck.isSelected());

        // Presentation
        s.setAutomaticallyTransposeTables(transposeTablesCombo.getValue());
        s.setDetectBinaryText(detectBinaryTextCheck.isSelected());
        s.setDetectBinaryUuid(detectBinaryUuidCheck.isSelected());
        s.setEnableLocalFilterByDefault(enableLocalFilterCheck.isSelected());
        s.setEnableImmediateCompletion(enableImmediateCompletionCheck.isSelected());
        s.setDisplayTemporalDataTimeZone(temporalTimeZoneField.getText());

        // Numbers
        s.setDecimalSeparator(decimalSeparatorField.getText());
        s.setUseGroupingSeparator(useGroupingSeparatorCheck.isSelected());
        s.setGroupingSeparator(groupingSeparatorField.getText());
        s.setInfinityText(infinityTextField.getText());
        s.setNanText(nanTextField.getText());
        s.setUseNumberPattern(useNumberPatternCheck.isSelected());
        s.setNumberPattern(numberPatternField.getText());

        // Dates
        s.setUseDatetimeTimestamp(useDatetimeCheck.isSelected());
        s.setDatetimeTimestampPattern(datetimePatternField.getText());
        s.setUseDatetimeWithTz(useDatetimeTzCheck.isSelected());
        s.setDatetimeWithTzPattern(datetimeTzPatternField.getText());
        s.setUseTime(useTimeCheck.isSelected());
        s.setTimePattern(timePatternField.getText());
        s.setUseTimeWithTz(useTimeTzCheck.isSelected());
        s.setTimeWithTzPattern(timeTzPatternField.getText());
        s.setUseDate(useDateCheck.isSelected());
        s.setDatePattern(datePatternField.getText());

        // Sorting
        s.setSortViaOrderBy(sortViaOrderByCheck.isSelected());
        s.setSortTablesByNumericPrimaryKey(sortNumericPkCheck.isSelected());
        s.setNumericPrimaryKeyDirection(pkDescendingRadio.isSelected() ? "Descending" : "Ascending");
        s.setAddColumnsToSorting(addColClickRadio.isSelected() ? "Click" : "⌥Click");

        // Modification
        s.setSubmitChangesImmediately(submitChangesImmediatelyCheck.isSelected());
        s.setEnableEditingForJoinQueries(enableEditingJoinQueriesCheck.isSelected());
        s.setShowDmlPreviewForJoinQueries(showDmlPreviewJoinCheck.isSelected());

        // URL
        s.setAllowOpeningSecureLinks(openSecureLinksCheck.isSelected());
        s.setAllowOpeningStandardLinks(openStandardLinksCheck.isSelected());
        s.setAllowOpeningLocalFileLinks(openLocalFileLinksCheck.isSelected());
        s.setAssumeHttpIfNoProtocol(assumeHttpCheck.isSelected());

        return s;
    }

    private int parseInt(String text, int defaultVal) {
        if (text == null || text.isBlank()) return defaultVal;
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            return defaultVal;
        }
    }

    public boolean isModified() {
        if (initialSettings == null) return false;
        return !Objects.equals(initialSettings, getCurrentSettingsFromUI());
    }

    public void apply() {
        DatabaseDataEditorSettings updated = getCurrentSettingsFromUI();
        manager.setSettings(updated);
        initialSettings = updated.clone();
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

    // Getters for testing and automation
    public CheckBox getLimitPageSizeCheck() { return limitPageSizeCheck; }
    public TextField getPageSizeField() { return pageSizeField; }
    public CheckBox getEnablePagingInEditorCheck() { return enablePagingInEditorCheck; }
    public ComboBox<String> getGridPaginationPosCombo() { return gridPaginationPosCombo; }
    public CheckBox getShowQuickActionsToolbarCheck() { return showQuickActionsToolbarCheck; }
    public CheckBox getEnableLocalFilterCheck() { return enableLocalFilterCheck; }
    public TextField getDecimalSeparatorField() { return decimalSeparatorField; }
    public CheckBox getSortViaOrderByCheck() { return sortViaOrderByCheck; }
    public CheckBox getSubmitChangesImmediatelyCheck() { return submitChangesImmediatelyCheck; }
    public CheckBox getOpenSecureLinksCheck() { return openSecureLinksCheck; }
}
