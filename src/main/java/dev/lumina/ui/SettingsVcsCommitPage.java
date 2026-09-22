package dev.lumina.ui;

import dev.lumina.git.VcsCommitSettings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;

import java.util.ArrayList;
import java.util.List;

/**
 * Version Control > Commit Settings Page matching IntelliJ IDEA Image 4.
 * Features:
 * - Clear initial commit message option
 * - Commit message inspections table with selection highlight & Severity selector (Warning, Info, Error, Weak Warning)
 * - Commit Checks (Reformat code, Rearrange code, Optimize imports, Cleanup, Update copyright, Check malicious dependencies, Run rustfmt, Go fmt)
 * - Advanced Commit Checks (Analyze code, Check TODO, Run Configuration, Run advanced checks after commit is done)
 */
public class SettingsVcsCommitPage extends VBox {

    private final VcsCommitSettings settings = VcsCommitSettings.getInstance();

    private final CheckBox clearInitialMsgCheck = new CheckBox("Clear initial commit message");

    // Commit Checks
    private final CheckBox reformatCodeCheck = new CheckBox("Reformat code");
    private final CheckBox rearrangeCodeCheck = new CheckBox("Rearrange code");
    private final CheckBox optimizeImportsCheck = new CheckBox("Optimize imports");
    private final CheckBox cleanupCheck = new CheckBox("Cleanup");
    private final Hyperlink cleanupProfileLink = new Hyperlink("Choose profile");
    private final CheckBox updateCopyrightCheck = new CheckBox("Update copyright");
    private final CheckBox checkMaliciousDepsCheck = new CheckBox("Check malicious dependencies");
    private final CheckBox runRustfmtCheck = new CheckBox("Run rustfmt");
    private final CheckBox goFmtCheck = new CheckBox("Go fmt");

    // Advanced Commit Checks
    private final CheckBox analyzeCodeCheck = new CheckBox("Analyze code");
    private final Hyperlink analyzeProfileLink = new Hyperlink("Choose profile");
    private final CheckBox checkTodoCheck = new CheckBox("Check TODO");
    private final Hyperlink checkTodoConfigureLink = new Hyperlink("Configure");
    private final CheckBox runConfigCheck = new CheckBox("Run Configuration");
    private final Hyperlink runConfigChooseLink = new Hyperlink("Choose configuration");
    private final CheckBox runAdvancedAfterCommitCheck = new CheckBox("Run advanced checks after a commit is done");

    // Inspections
    private static class InspectionRow {
        final String name;
        final CheckBox enabledCheck;
        final java.util.function.BooleanSupplier getter;
        final java.util.function.Consumer<Boolean> setter;
        final java.util.function.Supplier<VcsCommitSettings.InspectionSeverity> sevGetter;
        final java.util.function.Consumer<VcsCommitSettings.InspectionSeverity> sevSetter;
        HBox rowView;

        InspectionRow(String name,
                      java.util.function.BooleanSupplier getter,
                      java.util.function.Consumer<Boolean> setter,
                      java.util.function.Supplier<VcsCommitSettings.InspectionSeverity> sevGetter,
                      java.util.function.Consumer<VcsCommitSettings.InspectionSeverity> sevSetter) {
            this.name = name;
            this.getter = getter;
            this.setter = setter;
            this.sevGetter = sevGetter;
            this.sevSetter = sevSetter;
            this.enabledCheck = new CheckBox();
        }
    }

    private final List<InspectionRow> inspectionRows = new ArrayList<>();
    private InspectionRow selectedInspection = null;
    private final ComboBox<VcsCommitSettings.InspectionSeverity> severityCombo = new ComboBox<>();
    private final HBox severityBox = new HBox(6);

    public SettingsVcsCommitPage() {
        getStyleClass().add("settings-page");
        setPadding(new Insets(16, 20, 16, 20));
        setSpacing(12);
        setStyle("-fx-background-color: #1E1F22; -fx-font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;");

        // 1. Clear initial commit message
        styleCheck(clearInitialMsgCheck);
        clearInitialMsgCheck.setSelected(settings.isClearInitialCommitMessage());
        clearInitialMsgCheck.setOnAction(e -> settings.setClearInitialCommitMessage(clearInitialMsgCheck.isSelected()));

        // 2. Commit message inspections
        Label inspectionsTitle = new Label("Commit message inspections:");
        inspectionsTitle.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-padding: 4 0 2 0;");

        VBox inspectionsBox = buildInspectionsBox();

        // 3. Commit Checks section
        HBox commitChecksHeader = createSectionHeader("Commit Checks");
        VBox commitChecksBox = buildCommitChecksBox();

        // 4. Advanced Commit Checks section
        HBox advancedChecksHeader = createSectionHeader("Advanced Commit Checks");
        VBox advancedChecksBox = buildAdvancedChecksBox();

        getChildren().addAll(
                clearInitialMsgCheck,
                inspectionsTitle,
                inspectionsBox,
                commitChecksHeader,
                commitChecksBox,
                advancedChecksHeader,
                advancedChecksBox
        );

        settings.addListener(this::syncFromSettings);
    }

    private VBox buildInspectionsBox() {
        inspectionRows.clear();
        inspectionRows.add(new InspectionRow(
                "Blank line between subject and body",
                settings::isInspectionBlankLine,
                settings::setInspectionBlankLine,
                settings::getInspectionBlankLineSeverity,
                settings::setInspectionBlankLineSeverity
        ));
        inspectionRows.add(new InspectionRow(
                "Limit body line",
                settings::isInspectionLimitBodyLine,
                settings::setInspectionLimitBodyLine,
                settings::getInspectionLimitBodyLineSeverity,
                settings::setInspectionLimitBodyLineSeverity
        ));
        inspectionRows.add(new InspectionRow(
                "Limit subject line",
                settings::isInspectionLimitSubjectLine,
                settings::setInspectionLimitSubjectLine,
                settings::getInspectionLimitSubjectLineSeverity,
                settings::setInspectionLimitSubjectLineSeverity
        ));
        inspectionRows.add(new InspectionRow(
                "Spelling",
                settings::isInspectionSpelling,
                settings::setInspectionSpelling,
                settings::getInspectionSpellingSeverity,
                settings::setInspectionSpellingSeverity
        ));

        // Severity combo
        Label sevLbl = new Label("Severity:");
        sevLbl.setStyle("-fx-text-fill: #848BA3; -fx-font-size: 12px;");

        severityCombo.getItems().setAll(VcsCommitSettings.InspectionSeverity.values());
        severityCombo.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(VcsCommitSettings.InspectionSeverity s) {
                return s != null ? s.getDisplayName() : "";
            }
            @Override
            public VcsCommitSettings.InspectionSeverity fromString(String string) {
                return null;
            }
        });
        severityCombo.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(VcsCommitSettings.InspectionSeverity item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item.getDisplayName());
                    setGraphic(createSeverityIcon(item));
                    setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
                }
            }
        });
        severityCombo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(VcsCommitSettings.InspectionSeverity item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item.getDisplayName());
                    setGraphic(createSeverityIcon(item));
                    setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
                }
            }
        });
        severityCombo.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-font-size: 12px;");
        severityCombo.setPrefWidth(120);
        severityCombo.setOnAction(e -> {
            if (selectedInspection != null && severityCombo.getValue() != null) {
                selectedInspection.sevSetter.accept(severityCombo.getValue());
            }
        });

        severityBox.setAlignment(Pos.CENTER_RIGHT);
        severityBox.setSpacing(6);
        severityBox.getChildren().addAll(sevLbl, severityCombo);

        VBox rowsContainer = new VBox();
        rowsContainer.setStyle("-fx-background-color: #1E1F22; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-border-width: 1;");

        for (int i = 0; i < inspectionRows.size(); i++) {
            InspectionRow row = inspectionRows.get(i);
            row.enabledCheck.setSelected(row.getter.getAsBoolean());
            row.enabledCheck.setOnAction(e -> row.setter.accept(row.enabledCheck.isSelected()));

            Label nameLbl = new Label(row.name);
            nameLbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            HBox rowBox = new HBox(8, nameLbl, spacer, row.enabledCheck);
            rowBox.setAlignment(Pos.CENTER_LEFT);
            rowBox.setPadding(new Insets(5, 10, 5, 10));
            rowBox.setStyle("-fx-cursor: hand; -fx-background-color: transparent;");

            final int idx = i;
            rowBox.setOnMouseClicked(e -> selectInspectionRow(idx, rowsContainer));

            row.rowView = rowBox;
            rowsContainer.getChildren().add(rowBox);
        }

        // Default selection to first item matching Image 4
        selectInspectionRow(0, rowsContainer);

        HBox wrapper = new HBox(rowsContainer, severityBox);
        wrapper.setAlignment(Pos.TOP_LEFT);
        wrapper.setSpacing(12);
        HBox.setHgrow(rowsContainer, Priority.ALWAYS);

        VBox container = new VBox(wrapper);
        container.setPadding(new Insets(0, 0, 0, 0));
        return container;
    }

    private void selectInspectionRow(int index, VBox rowsContainer) {
        for (int i = 0; i < inspectionRows.size(); i++) {
            InspectionRow r = inspectionRows.get(i);
            if (i == index) {
                selectedInspection = r;
                r.rowView.setStyle("-fx-background-color: #2E436E; -fx-cursor: hand;");
                severityCombo.setValue(r.sevGetter.get());
            } else {
                r.rowView.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");
            }
        }
    }

    private Node createSeverityIcon(VcsCommitSettings.InspectionSeverity severity) {
        SVGPath path = new SVGPath();
        if (severity == VcsCommitSettings.InspectionSeverity.WARNING) {
            path.setContent("M 6 1 L 11 10 L 1 10 Z M 6 4 L 6 7 M 6 8.5 L 6 9");
            path.setFill(Color.web("#EDA200"));
            path.setStroke(Color.web("#EDA200"));
            path.setStrokeWidth(0.6);
        } else if (severity == VcsCommitSettings.InspectionSeverity.ERROR) {
            path.setContent("M 6 1 C 3.2 1 1 3.2 1 6 C 1 8.8 3.2 11 6 11 C 8.8 11 11 8.8 11 6 C 11 3.2 8.8 1 6 1 Z M 3.5 3.5 L 8.5 8.5 M 8.5 3.5 L 3.5 8.5");
            path.setFill(Color.web("#ED6C63"));
            path.setStroke(Color.web("#ED6C63"));
            path.setStrokeWidth(0.6);
        } else {
            path.setContent("M 6 1 C 3.2 1 1 3.2 1 6 C 1 8.8 3.2 11 6 11 C 8.8 11 11 8.8 11 6 C 11 3.2 8.8 1 6 1 Z M 6 4 L 6 8 M 6 3 L 6 3.5");
            path.setFill(Color.web("#589DF6"));
            path.setStroke(Color.web("#589DF6"));
            path.setStrokeWidth(0.6);
        }
        return path;
    }

    private VBox buildCommitChecksBox() {
        VBox box = new VBox(6);
        box.setPadding(new Insets(0, 0, 0, 16));

        styleCheck(reformatCodeCheck);
        reformatCodeCheck.setSelected(settings.isCheckReformatCode());
        reformatCodeCheck.setOnAction(e -> settings.setCheckReformatCode(reformatCodeCheck.isSelected()));

        styleCheck(rearrangeCodeCheck);
        rearrangeCodeCheck.setSelected(settings.isCheckRearrangeCode());
        rearrangeCodeCheck.setOnAction(e -> settings.setCheckRearrangeCode(rearrangeCodeCheck.isSelected()));

        styleCheck(optimizeImportsCheck);
        optimizeImportsCheck.setSelected(settings.isCheckOptimizeImports());
        optimizeImportsCheck.setOnAction(e -> settings.setCheckOptimizeImports(optimizeImportsCheck.isSelected()));

        styleCheck(cleanupCheck);
        cleanupCheck.setSelected(settings.isCheckCleanup());
        cleanupCheck.setOnAction(e -> settings.setCheckCleanup(cleanupCheck.isSelected()));
        styleLink(cleanupProfileLink);
        cleanupProfileLink.setOnAction(e -> showProfileDialog("Cleanup Profile", settings.getCleanupProfile(), settings::setCleanupProfile));
        HBox cleanupRow = new HBox(6, cleanupCheck, cleanupProfileLink);
        cleanupRow.setAlignment(Pos.CENTER_LEFT);

        styleCheck(updateCopyrightCheck);
        updateCopyrightCheck.setSelected(settings.isCheckUpdateCopyright());
        updateCopyrightCheck.setOnAction(e -> settings.setCheckUpdateCopyright(updateCopyrightCheck.isSelected()));

        styleCheck(checkMaliciousDepsCheck);
        checkMaliciousDepsCheck.setSelected(settings.isCheckMaliciousDependencies());
        checkMaliciousDepsCheck.setOnAction(e -> settings.setCheckMaliciousDependencies(checkMaliciousDepsCheck.isSelected()));

        styleCheck(runRustfmtCheck);
        runRustfmtCheck.setSelected(settings.isCheckRunRustfmt());
        runRustfmtCheck.setOnAction(e -> settings.setCheckRunRustfmt(runRustfmtCheck.isSelected()));

        styleCheck(goFmtCheck);
        goFmtCheck.setSelected(settings.isCheckGoFmt());
        goFmtCheck.setOnAction(e -> settings.setCheckGoFmt(goFmtCheck.isSelected()));

        box.getChildren().addAll(
                reformatCodeCheck,
                rearrangeCodeCheck,
                optimizeImportsCheck,
                cleanupRow,
                updateCopyrightCheck,
                checkMaliciousDepsCheck,
                runRustfmtCheck,
                goFmtCheck
        );
        return box;
    }

    private VBox buildAdvancedChecksBox() {
        VBox box = new VBox(6);
        box.setPadding(new Insets(0, 0, 0, 16));

        styleCheck(analyzeCodeCheck);
        analyzeCodeCheck.setSelected(settings.isCheckAnalyzeCode());
        analyzeCodeCheck.setOnAction(e -> settings.setCheckAnalyzeCode(analyzeCodeCheck.isSelected()));
        styleLink(analyzeProfileLink);
        analyzeProfileLink.setOnAction(e -> showProfileDialog("Analyze Code Profile", settings.getAnalysisProfile(), settings::setAnalysisProfile));
        HBox analyzeRow = new HBox(6, analyzeCodeCheck, analyzeProfileLink);
        analyzeRow.setAlignment(Pos.CENTER_LEFT);

        styleCheck(checkTodoCheck);
        checkTodoCheck.setSelected(settings.isCheckTodo());
        checkTodoCheck.setOnAction(e -> settings.setCheckTodo(checkTodoCheck.isSelected()));
        styleLink(checkTodoConfigureLink);
        HBox todoRow = new HBox(6, checkTodoCheck, checkTodoConfigureLink);
        todoRow.setAlignment(Pos.CENTER_LEFT);

        styleCheck(runConfigCheck);
        runConfigCheck.setSelected(settings.isCheckRunConfiguration());
        runConfigCheck.setOnAction(e -> settings.setCheckRunConfiguration(runConfigCheck.isSelected()));
        styleLink(runConfigChooseLink);
        runConfigChooseLink.setOnAction(e -> showProfileDialog("Run Configuration", settings.getRunConfigurationName(), settings::setRunConfigurationName));
        HBox runConfigRow = new HBox(6, runConfigCheck, runConfigChooseLink);
        runConfigRow.setAlignment(Pos.CENTER_LEFT);

        styleCheck(runAdvancedAfterCommitCheck);
        runAdvancedAfterCommitCheck.setSelected(settings.isRunAdvancedChecksAfterCommit());
        runAdvancedAfterCommitCheck.setOnAction(e -> settings.setRunAdvancedChecksAfterCommit(runAdvancedAfterCommitCheck.isSelected()));

        Label advDesc = new Label("Failed checks will not prevent a commit");
        advDesc.setStyle("-fx-text-fill: #848BA3; -fx-font-size: 11px; -fx-padding: 0 0 0 20;");

        VBox advBox = new VBox(2, runAdvancedAfterCommitCheck, advDesc);

        box.getChildren().addAll(analyzeRow, todoRow, runConfigRow, advBox);
        return box;
    }

    private void showProfileDialog(String title, String currentVal, java.util.function.Consumer<String> onSave) {
        TextInputDialog dialog = new TextInputDialog(currentVal);
        dialog.setTitle(title);
        dialog.setHeaderText("Specify " + title + ":");
        dialog.showAndWait().ifPresent(val -> {
            if (!val.isBlank()) onSave.accept(val.trim());
        });
    }

    private void syncFromSettings() {
        clearInitialMsgCheck.setSelected(settings.isClearInitialCommitMessage());

        for (InspectionRow row : inspectionRows) {
            row.enabledCheck.setSelected(row.getter.getAsBoolean());
        }
        if (selectedInspection != null) {
            severityCombo.setValue(selectedInspection.sevGetter.get());
        }

        reformatCodeCheck.setSelected(settings.isCheckReformatCode());
        rearrangeCodeCheck.setSelected(settings.isCheckRearrangeCode());
        optimizeImportsCheck.setSelected(settings.isCheckOptimizeImports());
        cleanupCheck.setSelected(settings.isCheckCleanup());
        updateCopyrightCheck.setSelected(settings.isCheckUpdateCopyright());
        checkMaliciousDepsCheck.setSelected(settings.isCheckMaliciousDependencies());
        runRustfmtCheck.setSelected(settings.isCheckRunRustfmt());
        goFmtCheck.setSelected(settings.isCheckGoFmt());

        analyzeCodeCheck.setSelected(settings.isCheckAnalyzeCode());
        checkTodoCheck.setSelected(settings.isCheckTodo());
        runConfigCheck.setSelected(settings.isCheckRunConfiguration());
        runAdvancedAfterCommitCheck.setSelected(settings.isRunAdvancedChecksAfterCommit());
    }

    private static HBox createSectionHeader(String title) {
        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(8, 0, 4, 0));

        Label lbl = new Label(title);
        lbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-font-weight: bold;");

        Region line = new Region();
        line.setPrefHeight(1);
        line.setMaxHeight(1);
        line.setStyle("-fx-background-color: #393B40;");
        HBox.setHgrow(line, Priority.ALWAYS);

        header.getChildren().addAll(lbl, line);
        return header;
    }

    private static void styleCheck(CheckBox cb) {
        cb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-cursor: hand;");
    }

    private static void styleLink(Hyperlink link) {
        link.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-padding: 0; -fx-border-color: transparent; -fx-underline: false;");
        link.setOnMouseEntered(e -> link.setStyle("-fx-text-fill: #70AAFF; -fx-font-size: 12px; -fx-padding: 0; -fx-border-color: transparent; -fx-underline: true;"));
        link.setOnMouseExited(e -> link.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-padding: 0; -fx-border-color: transparent; -fx-underline: false;"));
    }
}
