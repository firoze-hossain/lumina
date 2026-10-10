package dev.lumina.ui;

import dev.lumina.tools.TasksGeneralSettings;
import dev.lumina.tools.TasksGeneralSettingsManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.Objects;

/**
 * Settings UI page for Tools > Tasks in Lumina IDE.
 * 1:1 visual match with reference layout.
 */
public class SettingsToolsTasksPage extends VBox {

    private final TextField changelistFormatField;
    private final TextField branchFormatField;
    private final CheckBox lowercasedCheck;
    private final TextField replaceSpacesField;
    private final TextField historyLengthField;
    private final TextField timeoutField;
    private final CheckBox showWidgetNoTasksCheck;
    private final CheckBox saveContextCommitCheck;

    // Issue Cache
    private final CheckBox enableCacheCheck;
    private final TextField updateIssuesCountField;
    private final TextField cacheIntervalField;

    private Runnable onModified;
    private boolean suppressEvents = false;

    public SettingsToolsTasksPage() {
        setSpacing(12);
        setPadding(new Insets(16, 20, 20, 20));
        setStyle("-fx-background-color: #1E1F22;");

        // Row 1: Changelist name format:
        Label changelistLabel = new Label("Changelist name format:");
        changelistLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-min-width: 190px;");

        changelistFormatField = new TextField();
        changelistFormatField.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4; -fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-padding: 4 8;");
        HBox.setHgrow(changelistFormatField, Priority.ALWAYS);

        Button addChangelistMacroBtn = createMacroButton(changelistFormatField, "${id}", "${summary}", "${project}");
        HBox changelistRow = new HBox(8, changelistLabel, changelistFormatField, addChangelistMacroBtn);
        changelistRow.setAlignment(Pos.CENTER_LEFT);

        // Row 2: Feature branch name format:
        Label branchLabel = new Label("Feature branch name format:");
        branchLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-min-width: 190px;");

        branchFormatField = new TextField();
        branchFormatField.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4; -fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-padding: 4 8;");
        HBox.setHgrow(branchFormatField, Priority.ALWAYS);

        Button addBranchMacroBtn = createMacroButton(branchFormatField, "${id}", "${summary}", "${type}");
        HBox branchRow = new HBox(8, branchLabel, branchFormatField, addBranchMacroBtn);
        branchRow.setAlignment(Pos.CENTER_LEFT);

        // Row 3: Lowercased & Replace spaces with
        lowercasedCheck = new CheckBox("Lowercased");
        lowercasedCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        Region branchSpacer = new Region();
        branchSpacer.setPrefWidth(120);

        Label replaceSpacesLabel = new Label("Replace spaces with");
        replaceSpacesLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        replaceSpacesField = new TextField("-");
        replaceSpacesField.setPrefWidth(45);
        replaceSpacesField.setMaxWidth(45);
        replaceSpacesField.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4; -fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-padding: 4 6; -fx-alignment: center;");

        HBox lowercasedRow = new HBox(12, lowercasedCheck, branchSpacer, replaceSpacesLabel, replaceSpacesField);
        lowercasedRow.setAlignment(Pos.CENTER_LEFT);
        lowercasedRow.setPadding(new Insets(0, 0, 0, 198));

        // Row 4: Task history length:
        Label historyLabel = new Label("Task history length:");
        historyLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-min-width: 190px;");

        historyLengthField = new TextField("50");
        historyLengthField.setPrefWidth(60);
        historyLengthField.setMaxWidth(70);
        historyLengthField.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4; -fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-padding: 4 8;");

        HBox historyRow = new HBox(8, historyLabel, historyLengthField);
        historyRow.setAlignment(Pos.CENTER_LEFT);

        // Row 5: Connection timeout:
        Label timeoutLabel = new Label("Connection timeout:");
        timeoutLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-min-width: 190px;");

        timeoutField = new TextField("5000");
        timeoutField.setPrefWidth(60);
        timeoutField.setMaxWidth(70);
        timeoutField.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4; -fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-padding: 4 8;");

        Label msLabel = new Label("milliseconds");
        msLabel.setStyle("-fx-text-fill: #848BA3; -fx-font-size: 12px;");

        HBox timeoutRow = new HBox(8, timeoutLabel, timeoutField, msLabel);
        timeoutRow.setAlignment(Pos.CENTER_LEFT);

        // Row 6: Show task widget if there are no active tasks
        showWidgetNoTasksCheck = new CheckBox("Show task widget if there are no active tasks");
        showWidgetNoTasksCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        // Row 7: Save context on commit
        saveContextCommitCheck = new CheckBox("Save context on commit");
        saveContextCommitCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        // Section: Issue Cache
        HBox issueCacheHeader = createSectionHeader("Issue Cache");

        enableCacheCheck = new CheckBox("Enable cache");
        enableCacheCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        Label updateLabel = new Label("Update");
        updateLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        updateIssuesCountField = new TextField("100");
        updateIssuesCountField.setPrefWidth(55);
        updateIssuesCountField.setMaxWidth(60);
        updateIssuesCountField.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4; -fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-padding: 4 6; -fx-alignment: center;");
        updateIssuesCountField.disableProperty().bind(enableCacheCheck.selectedProperty().not());

        Label issuesEveryLabel = new Label("issues every");
        issuesEveryLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        cacheIntervalField = new TextField("20");
        cacheIntervalField.setPrefWidth(50);
        cacheIntervalField.setMaxWidth(55);
        cacheIntervalField.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4; -fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-padding: 4 6; -fx-alignment: center;");
        cacheIntervalField.disableProperty().bind(enableCacheCheck.selectedProperty().not());

        Label minutesLabel = new Label("minutes");
        minutesLabel.setStyle("-fx-text-fill: #848BA3; -fx-font-size: 13px;");

        HBox cacheParamsRow = new HBox(8, enableCacheCheck, updateLabel, updateIssuesCountField, issuesEveryLabel, cacheIntervalField, minutesLabel);
        cacheParamsRow.setAlignment(Pos.CENTER_LEFT);
        cacheParamsRow.setPadding(new Insets(2, 0, 0, 16));

        getChildren().addAll(
                changelistRow,
                branchRow,
                lowercasedRow,
                historyRow,
                timeoutRow,
                showWidgetNoTasksCheck,
                saveContextCommitCheck,
                issueCacheHeader,
                cacheParamsRow
        );

        setupListeners();
        loadSettings();
    }

    private Button createMacroButton(TextField targetField, String... macros) {
        Button btn = new Button("+");
        btn.setTooltip(new Tooltip("Insert macro variable"));
        btn.setStyle("-fx-background-color: #393B40; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4; -fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-padding: 4 8; -fx-cursor: hand;");

        ContextMenu menu = new ContextMenu();
        for (String m : macros) {
            MenuItem item = new MenuItem(m);
            item.setOnAction(e -> {
                targetField.replaceSelection(m);
                notifyModified();
            });
            menu.getItems().add(item);
        }

        btn.setOnAction(e -> menu.show(btn, javafx.geometry.Side.BOTTOM, 0, 0));
        return btn;
    }

    private HBox createSectionHeader(String title) {
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold;");

        Region line = new Region();
        line.setStyle("-fx-background-color: #393B40; -fx-min-height: 1; -fx-max-height: 1;");
        HBox.setHgrow(line, Priority.ALWAYS);

        HBox box = new HBox(12, titleLabel, line);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(8, 0, 4, 0));
        return box;
    }

    private void setupListeners() {
        changelistFormatField.textProperty().addListener((o, ov, nv) -> notifyModified());
        branchFormatField.textProperty().addListener((o, ov, nv) -> notifyModified());
        lowercasedCheck.selectedProperty().addListener((o, ov, nv) -> notifyModified());
        replaceSpacesField.textProperty().addListener((o, ov, nv) -> notifyModified());
        historyLengthField.textProperty().addListener((o, ov, nv) -> notifyModified());
        timeoutField.textProperty().addListener((o, ov, nv) -> notifyModified());
        showWidgetNoTasksCheck.selectedProperty().addListener((o, ov, nv) -> notifyModified());
        saveContextCommitCheck.selectedProperty().addListener((o, ov, nv) -> notifyModified());
        enableCacheCheck.selectedProperty().addListener((o, ov, nv) -> notifyModified());
        updateIssuesCountField.textProperty().addListener((o, ov, nv) -> notifyModified());
        cacheIntervalField.textProperty().addListener((o, ov, nv) -> notifyModified());
    }

    public void setOnModified(Runnable onModified) {
        this.onModified = onModified;
    }

    private void notifyModified() {
        if (!suppressEvents && onModified != null) {
            onModified.run();
        }
    }

    public void loadSettings() {
        suppressEvents = true;
        try {
            TasksGeneralSettings s = TasksGeneralSettingsManager.getInstance().getSettings();
            changelistFormatField.setText(s.getChangelistNameFormat());
            branchFormatField.setText(s.getFeatureBranchNameFormat());
            lowercasedCheck.setSelected(s.isLowercased());
            replaceSpacesField.setText(s.getReplaceSpacesWith());
            historyLengthField.setText(String.valueOf(s.getTaskHistoryLength()));
            timeoutField.setText(String.valueOf(s.getConnectionTimeoutMs()));
            showWidgetNoTasksCheck.setSelected(s.isShowTaskWidgetIfNoActiveTasks());
            saveContextCommitCheck.setSelected(s.isSaveContextOnCommit());
            enableCacheCheck.setSelected(s.isEnableCache());
            updateIssuesCountField.setText(String.valueOf(s.getUpdateIssuesCount()));
            cacheIntervalField.setText(String.valueOf(s.getCacheIntervalMinutes()));
        } finally {
            suppressEvents = false;
        }
    }

    public boolean isModified() {
        TasksGeneralSettings current = TasksGeneralSettingsManager.getInstance().getSettings();
        int hl = 50;
        try { hl = Integer.parseInt(historyLengthField.getText().trim()); } catch (NumberFormatException ignored) {}
        int to = 5000;
        try { to = Integer.parseInt(timeoutField.getText().trim()); } catch (NumberFormatException ignored) {}
        int ui = 100;
        try { ui = Integer.parseInt(updateIssuesCountField.getText().trim()); } catch (NumberFormatException ignored) {}
        int ci = 20;
        try { ci = Integer.parseInt(cacheIntervalField.getText().trim()); } catch (NumberFormatException ignored) {}

        return !Objects.equals(changelistFormatField.getText().trim(), current.getChangelistNameFormat()) ||
                !Objects.equals(branchFormatField.getText().trim(), current.getFeatureBranchNameFormat()) ||
                lowercasedCheck.isSelected() != current.isLowercased() ||
                !Objects.equals(replaceSpacesField.getText().trim(), current.getReplaceSpacesWith()) ||
                hl != current.getTaskHistoryLength() ||
                to != current.getConnectionTimeoutMs() ||
                showWidgetNoTasksCheck.isSelected() != current.isShowTaskWidgetIfNoActiveTasks() ||
                saveContextCommitCheck.isSelected() != current.isSaveContextOnCommit() ||
                enableCacheCheck.isSelected() != current.isEnableCache() ||
                ui != current.getUpdateIssuesCount() ||
                ci != current.getCacheIntervalMinutes();
    }

    public void apply() {
        TasksGeneralSettings s = new TasksGeneralSettings();
        s.setChangelistNameFormat(changelistFormatField.getText().trim());
        s.setFeatureBranchNameFormat(branchFormatField.getText().trim());
        s.setLowercased(lowercasedCheck.isSelected());
        s.setReplaceSpacesWith(replaceSpacesField.getText().trim());
        try { s.setTaskHistoryLength(Integer.parseInt(historyLengthField.getText().trim())); } catch (NumberFormatException ignored) {}
        try { s.setConnectionTimeoutMs(Integer.parseInt(timeoutField.getText().trim())); } catch (NumberFormatException ignored) {}
        s.setShowTaskWidgetIfNoActiveTasks(showWidgetNoTasksCheck.isSelected());
        s.setSaveContextOnCommit(saveContextCommitCheck.isSelected());
        s.setEnableCache(enableCacheCheck.isSelected());
        try { s.setUpdateIssuesCount(Integer.parseInt(updateIssuesCountField.getText().trim())); } catch (NumberFormatException ignored) {}
        try { s.setCacheIntervalMinutes(Integer.parseInt(cacheIntervalField.getText().trim())); } catch (NumberFormatException ignored) {}
        TasksGeneralSettingsManager.getInstance().setSettings(s);
    }

    public void reset() {
        loadSettings();
    }

    public void revertChanges() {
        reset();
    }

    public TextField getChangelistFormatField() {
        return changelistFormatField;
    }

    public TextField getBranchFormatField() {
        return branchFormatField;
    }

    public CheckBox getLowercasedCheck() {
        return lowercasedCheck;
    }

    public CheckBox getEnableCacheCheck() {
        return enableCacheCheck;
    }
}
