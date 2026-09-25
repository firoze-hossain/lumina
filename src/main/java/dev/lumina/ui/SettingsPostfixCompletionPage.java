package dev.lumina.ui;

import dev.lumina.settings.PostfixCompletionSettings;
import dev.lumina.settings.PostfixCompletionSettings.TemplateItem;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.util.*;

/**
 * Modern dynamic Editor > General > Postfix Completion settings page.
 * Completely dynamic, backed by PostfixCompletionSettings, supporting tree with checkboxes,
 * template creation/editing toolbar, live before/after syntax preview, dirty tracking, and apply/reset.
 */
public class SettingsPostfixCompletionPage extends VBox {

    // Top Controls
    private final CheckBox enablePostfixCheck = new CheckBox("Enable postfix completion");
    private final CheckBox showAsCommandCheck = new CheckBox("Show postfix completions as command completions");
    private final ComboBox<String> expandCombo = new ComboBox<>();

    // Toolbar Buttons
    private final Button addButton = new Button("+");
    private final Button removeButton = new Button("—");
    private final Button editButton = new Button("✎");
    private final Button duplicateButton = new Button("⎘");

    // Left Tree
    private final TreeView<TreeData> treeView = new TreeView<>();
    private final TreeItem<TreeData> treeRoot = new TreeItem<>(new TreeData(null, null, false));
    private final Map<String, TreeItem<TreeData>> languageNodes = new LinkedHashMap<>();
    private final Map<String, TreeItem<TreeData>> templateNodes = new LinkedHashMap<>();

    // Right Preview
    private final VBox rightPane = new VBox(10);
    private final Label descHeaderLabel = new Label();
    private final Label descSubLabel1 = new Label();
    private final Label descSubLabel2 = new Label();
    private final VBox beforeBox = new VBox(4);
    private final VBox afterBox = new VBox(4);
    private final VBox beforeCodePane = new VBox(2);
    private final VBox afterCodePane = new VBox(2);

    private Runnable onModifiedListener;
    private boolean suppressEvents = false;

    // Data holder for tree items
    public static class TreeData {
        public final String language;
        public final TemplateItem template;
        public final boolean isLanguageGroup;

        public TreeData(String language, TemplateItem template, boolean isLanguageGroup) {
            this.language = language;
            this.template = template;
            this.isLanguageGroup = isLanguageGroup;
        }
    }

    public SettingsPostfixCompletionPage() {
        getStyleClass().add("settings-page");
        setStyle("-fx-background-color: #1E1F22;");
        setPadding(new Insets(14, 24, 20, 24));
        setSpacing(10);

        buildUi();
        setupListeners();
        loadFromSettings(PostfixCompletionSettings.getInstance());
    }

    public void setOnModifiedListener(Runnable listener) {
        this.onModifiedListener = listener;
    }

    private void notifyModified() {
        if (!suppressEvents && onModifiedListener != null) {
            onModifiedListener.run();
        }
    }

    private void styleCheckBox(CheckBox cb) {
        cb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
    }

    private void buildUi() {
        // --- 1. Top Section ---
        styleCheckBox(enablePostfixCheck);
        styleCheckBox(showAsCommandCheck);

        VBox checksBox = new VBox(6, enablePostfixCheck, showAsCommandCheck);

        Label expandLabel = new Label("Expand templates with");
        expandLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");

        expandCombo.getItems().addAll("Tab", "Space", "Enter");
        expandCombo.setValue("Tab");
        expandCombo.setPrefWidth(100);
        expandCombo.setPrefHeight(25);
        expandCombo.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4; -fx-text-fill: #DFE1E5; -fx-font-size: 12px;");

        HBox expandRow = new HBox(8, expandLabel, expandCombo);
        expandRow.setAlignment(Pos.CENTER_LEFT);
        expandRow.setPadding(new Insets(0, 0, 4, 0));

        // --- 2. Main Two-Column Split Layout ---
        BorderPane splitContainer = new BorderPane();
        splitContainer.setStyle("-fx-background-color: #1E1F22;");
        VBox.setVgrow(splitContainer, Priority.ALWAYS);

        // Left: Toolbar + TreeView
        HBox toolbar = buildToolbar();

        treeView.setShowRoot(false);
        treeView.setRoot(treeRoot);
        treeView.getStyleClass().add("postfix-tree");
        treeView.setStyle("-fx-background-color: #1E1F22; -fx-control-inner-background: #1E1F22; -fx-control-inner-background-alt: #1E1F22; -fx-background: #1E1F22; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4;");
        treeView.setCellFactory(tv -> new PostfixTreeCell());
        VBox.setVgrow(treeView, Priority.ALWAYS);

        VBox leftPane = new VBox(toolbar, treeView);
        leftPane.setPrefWidth(380);
        leftPane.setMinWidth(320);

        // Right: Descriptions and Code Previews
        buildRightPreviewPane();
        rightPane.setPadding(new Insets(0, 0, 0, 16));
        HBox.setHgrow(rightPane, Priority.ALWAYS);

        HBox mainSplit = new HBox(leftPane, rightPane);
        HBox.setHgrow(rightPane, Priority.ALWAYS);
        VBox.setVgrow(mainSplit, Priority.ALWAYS);

        // Master checkbox disabling
        enablePostfixCheck.selectedProperty().addListener((obs, o, enabled) -> {
            showAsCommandCheck.setDisable(!enabled);
            expandRow.setDisable(!enabled);
            mainSplit.setDisable(!enabled);
            notifyModified();
        });

        getChildren().addAll(checksBox, expandRow, mainSplit);
    }

    private HBox buildToolbar() {
        HBox bar = new HBox(2);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(0, 0, 4, 0));

        String btnStyle = "-fx-background-color: transparent; -fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-min-width: 24px; -fx-min-height: 24px; -fx-max-width: 24px; -fx-max-height: 24px; -fx-cursor: hand; -fx-padding: 0;";

        addButton.setStyle(btnStyle);
        removeButton.setStyle(btnStyle);
        editButton.setStyle(btnStyle);
        duplicateButton.setStyle(btnStyle);

        addButton.setTooltip(new Tooltip("Add Template"));
        removeButton.setTooltip(new Tooltip("Remove Template"));
        editButton.setTooltip(new Tooltip("Edit Template"));
        duplicateButton.setTooltip(new Tooltip("Duplicate Template"));

        removeButton.setDisable(true);
        editButton.setDisable(true);
        duplicateButton.setDisable(true);

        // Add Button Context Menu with supported languages
        ContextMenu addMenu = new ContextMenu();
        addMenu.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #393B40;");

        List<String> menuLangs = List.of(
                "Rust",
                "SQL",
                "Python",
                "JavaScript and TypeScript",
                "Java",
                "Go",
                "TypeScript",
                "PHP",
                "Groovy"
        );

        int idx = 1;
        for (String lang : menuLangs) {
            String label = idx + "  " + lang;
            MenuItem item = new MenuItem(label);
            item.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
            item.setOnAction(e -> showAddCustomTemplateDialog(lang.equals("JavaScript and TypeScript") ? "JavaScript" : lang));
            addMenu.getItems().add(item);
            idx++;
        }

        addButton.setOnAction(e -> addMenu.show(addButton, javafx.geometry.Side.BOTTOM, 0, 0));

        removeButton.setOnAction(e -> {
            TreeItem<TreeData> sel = treeView.getSelectionModel().getSelectedItem();
            if (sel != null && sel.getValue() != null && sel.getValue().template != null && sel.getValue().template.isCustom()) {
                PostfixCompletionSettings.getInstance().removeTemplate(sel.getValue().template.getId());
                sel.getParent().getChildren().remove(sel);
                notifyModified();
            }
        });

        editButton.setOnAction(e -> {
            TreeItem<TreeData> sel = treeView.getSelectionModel().getSelectedItem();
            if (sel != null && sel.getValue() != null && sel.getValue().template != null) {
                showEditTemplateDialog(sel.getValue().template);
            }
        });

        duplicateButton.setOnAction(e -> {
            TreeItem<TreeData> sel = treeView.getSelectionModel().getSelectedItem();
            if (sel != null && sel.getValue() != null && sel.getValue().template != null) {
                showDuplicateTemplateDialog(sel.getValue().template);
            }
        });

        bar.getChildren().addAll(addButton, removeButton, editButton, duplicateButton);
        return bar;
    }

    private void showAddCustomTemplateDialog(String language) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Create New Postfix Template");
        dialog.setHeaderText("Add custom postfix template for " + language);

        DialogPane pane = dialog.getDialogPane();
        pane.setStyle("-fx-background-color: #1E1F22;");
        try {
            pane.getStylesheets().add(getClass().getResource("/css/lumina-dark.css").toExternalForm());
        } catch (Exception ignored) {}

        Label keyLabel = new Label("Key:");
        keyLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        TextField keyField = new TextField();
        keyField.setPromptText("e.g. log, test");
        keyField.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #4E5157; -fx-border-radius: 4;");

        Label descLabel = new Label("Description:");
        descLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        TextField descField = new TextField();
        descField.setPromptText("Template description");
        descField.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #4E5157; -fx-border-radius: 4;");

        Label templateLabel = new Label("Template expression (use 'expr' for target):");
        templateLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        TextArea templateArea = new TextArea();
        templateArea.setPromptText("e.g. System.out.println(expr);");
        templateArea.setPrefRowCount(4);
        templateArea.setStyle("-fx-control-inner-background: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #4E5157;");

        VBox content = new VBox(8, keyLabel, keyField, descLabel, descField, templateLabel, templateArea);
        pane.setContent(content);
        pane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK && !keyField.getText().trim().isEmpty()) {
                String key = keyField.getText().trim();
                String desc = descField.getText().trim();
                String code = templateArea.getText().trim();
                if (code.isEmpty()) code = key + "(expr);";

                String id = "custom." + language.toLowerCase().replace(" ", "_") + "." + key + "_" + System.currentTimeMillis();
                TemplateItem item = new TemplateItem(id, language, key, code, "[expr]." + key, code, desc, true, true);

                TreeItem<TreeData> parentNode = languageNodes.get(language);
                if (parentNode == null) {
                    parentNode = languageNodes.get("Java");
                }
                if (parentNode != null) {
                    TreeItem<TreeData> child = new TreeItem<>(new TreeData(language, item, false));
                    parentNode.getChildren().add(child);
                    parentNode.setExpanded(true);
                    treeView.getSelectionModel().select(child);
                }
                notifyModified();
            }
        });
    }

    private void showEditTemplateDialog(TemplateItem item) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Edit Postfix Template");
        dialog.setHeaderText("Edit postfix template for " + item.getLanguage());

        DialogPane pane = dialog.getDialogPane();
        pane.setStyle("-fx-background-color: #1E1F22;");
        try {
            pane.getStylesheets().add(getClass().getResource("/css/lumina-dark.css").toExternalForm());
        } catch (Exception ignored) {}

        Label keyLabel = new Label("Key:");
        keyLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        TextField keyField = new TextField(item.getKey());
        keyField.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #4E5157; -fx-border-radius: 4;");
        if (!item.isCustom()) keyField.setDisable(true);

        Label descLabel = new Label("Description:");
        descLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        TextField descField = new TextField(item.getDescription());
        descField.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #4E5157; -fx-border-radius: 4;");

        Label templateLabel = new Label("Template expression (use 'expr' for target):");
        templateLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        TextArea templateArea = new TextArea(item.getAfterSample());
        templateArea.setPrefRowCount(4);
        templateArea.setStyle("-fx-control-inner-background: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #4E5157;");

        VBox content = new VBox(8, keyLabel, keyField, descLabel, descField, templateLabel, templateArea);
        pane.setContent(content);
        pane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                // If custom, update fields
                notifyModified();
                updatePreview(treeView.getSelectionModel().getSelectedItem());
            }
        });
    }

    private void showDuplicateTemplateDialog(TemplateItem item) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Duplicate Postfix Template");
        dialog.setHeaderText("Duplicate postfix template for " + item.getLanguage());

        DialogPane pane = dialog.getDialogPane();
        pane.setStyle("-fx-background-color: #1E1F22;");
        try {
            pane.getStylesheets().add(getClass().getResource("/css/lumina-dark.css").toExternalForm());
        } catch (Exception ignored) {}

        Label keyLabel = new Label("Key:");
        keyLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        TextField keyField = new TextField(item.getKey() + "_copy");
        keyField.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #4E5157; -fx-border-radius: 4;");

        Label descLabel = new Label("Description:");
        descLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        TextField descField = new TextField(item.getDescription());
        descField.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #4E5157; -fx-border-radius: 4;");

        Label templateLabel = new Label("Template expression (use 'expr' for target):");
        templateLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        TextArea templateArea = new TextArea(item.getExpansion() != null ? item.getExpansion() : item.getAfterSample());
        templateArea.setPrefRowCount(4);
        templateArea.setStyle("-fx-control-inner-background: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #4E5157;");

        VBox content = new VBox(8, keyLabel, keyField, descLabel, descField, templateLabel, templateArea);
        pane.setContent(content);
        pane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK && !keyField.getText().trim().isEmpty()) {
                String key = keyField.getText().trim();
                String desc = descField.getText().trim();
                String code = templateArea.getText().trim();
                if (code.isEmpty()) code = key + "(expr);";

                String id = "custom." + item.getLanguage().toLowerCase().replace(" ", "_") + "." + key + "_" + System.currentTimeMillis();
                TemplateItem newItem = new TemplateItem(id, item.getLanguage(), key, code, item.getBeforeSample(), item.getAfterSample(), desc, code, true, true);

                TreeItem<TreeData> parentNode = languageNodes.get(item.getLanguage());
                if (parentNode != null) {
                    TreeItem<TreeData> child = new TreeItem<>(new TreeData(item.getLanguage(), newItem, false));
                    parentNode.getChildren().add(child);
                    parentNode.setExpanded(true);
                    treeView.getSelectionModel().select(child);
                }
                notifyModified();
            }
        });
    }

    private void buildRightPreviewPane() {
        descHeaderLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-line-spacing: 3px;");
        descHeaderLabel.setWrapText(true);

        descSubLabel1.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-line-spacing: 3px;");
        descSubLabel1.setWrapText(true);

        descSubLabel2.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-line-spacing: 3px;");
        descSubLabel2.setWrapText(true);

        VBox descGroup = new VBox(3, descHeaderLabel, descSubLabel1, descSubLabel2);
        descGroup.setMinHeight(60);

        Label beforeTitle = new Label("Before:");
        beforeTitle.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");

        beforeCodePane.getStyleClass().add("postfix-code-pane");
        beforeCodePane.setStyle("-fx-background-color: #1E1F22; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 8 10 8 10;");
        beforeCodePane.setMinHeight(120);
        VBox.setVgrow(beforeCodePane, Priority.ALWAYS);

        beforeBox.getChildren().addAll(beforeTitle, beforeCodePane);
        VBox.setVgrow(beforeBox, Priority.ALWAYS);

        Label afterTitle = new Label("After:");
        afterTitle.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");

        afterCodePane.getStyleClass().add("postfix-code-pane");
        afterCodePane.setStyle("-fx-background-color: #1E1F22; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 8 10 8 10;");
        afterCodePane.setMinHeight(120);
        VBox.setVgrow(afterCodePane, Priority.ALWAYS);

        afterBox.getChildren().addAll(afterTitle, afterCodePane);
        VBox.setVgrow(afterBox, Priority.ALWAYS);

        rightPane.getChildren().addAll(descGroup, beforeBox, afterBox);
    }

    private void renderCodeSnippet(VBox targetPane, String codeText, boolean highlightRectangle) {
        targetPane.getChildren().clear();
        if (codeText == null) codeText = "";
        String[] lines = codeText.split("\n", -1);

        int lineNum = 1;
        for (String rawLine : lines) {
            HBox lineRow = new HBox(8);
            lineRow.setAlignment(Pos.CENTER_LEFT);

            Label numLabel = new Label(String.valueOf(lineNum++));
            numLabel.setPrefWidth(22);
            numLabel.setAlignment(Pos.CENTER_RIGHT);
            numLabel.setStyle("-fx-text-fill: #5A5D6B; -fx-font-family: 'SF Mono', Menlo, Monaco, Consolas, monospace; -fx-font-size: 12px;");

            TextFlow codeFlow = new TextFlow();
            if (highlightRectangle && rawLine.contains("[") && rawLine.contains("]")) {
                int start = rawLine.indexOf('[');
                int end = rawLine.indexOf(']');
                String prefix = rawLine.substring(0, start);
                String boxed = rawLine.substring(start + 1, end);
                String suffix = rawLine.substring(end + 1);

                Text tPrefix = new Text(prefix.replace(" ", "\u00A0"));
                tPrefix.setStyle("-fx-fill: #DFE1E5; -fx-font-family: 'SF Mono', Menlo, Monaco, Consolas, monospace; -fx-font-size: 12px;");

                Label boxLabel = new Label(boxed);
                boxLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-family: 'SF Mono', Menlo, Monaco, Consolas, monospace; -fx-font-size: 12px; -fx-border-color: #8C9099; -fx-border-style: solid; -fx-border-width: 1px; -fx-border-radius: 2px; -fx-padding: 0 3 0 3; -fx-background-color: #26282E;");

                Text tSuffix = new Text(suffix.replace(" ", "\u00A0"));
                tSuffix.setStyle("-fx-fill: #DFE1E5; -fx-font-family: 'SF Mono', Menlo, Monaco, Consolas, monospace; -fx-font-size: 12px;");

                codeFlow.getChildren().addAll(tPrefix, boxLabel, tSuffix);
            } else {
                Text t = new Text(rawLine.replace(" ", "\u00A0"));
                t.setStyle("-fx-fill: #DFE1E5; -fx-font-family: 'SF Mono', Menlo, Monaco, Consolas, monospace; -fx-font-size: 12px;");
                codeFlow.getChildren().add(t);
            }

            lineRow.getChildren().addAll(numLabel, codeFlow);
            targetPane.getChildren().add(lineRow);
        }
    }

    private void updatePreview(TreeItem<TreeData> selected) {
        if (selected == null || selected.getValue() == null) {
            showLanguageDefaultPreview("Java");
            return;
        }

        TreeData data = selected.getValue();
        if (data.isLanguageGroup) {
            showLanguageDefaultPreview(data.language);
            removeButton.setDisable(true);
            editButton.setDisable(true);
            duplicateButton.setDisable(true);
        } else if (data.template != null) {
            TemplateItem t = data.template;
            descHeaderLabel.setText(t.getDescription());
            descSubLabel1.setText("");
            descSubLabel2.setText("");

            renderCodeSnippet(beforeCodePane, t.getBeforeSample(), true);
            renderCodeSnippet(afterCodePane, t.getAfterSample(), false);

            removeButton.setDisable(!t.isCustom());
            editButton.setDisable(false);
            duplicateButton.setDisable(false);
        }
    }

    private void showLanguageDefaultPreview(String language) {
        descHeaderLabel.setText("You have selected the postfix completion language.");
        descSubLabel1.setText("By clicking the checkbox, you can enable/disable all postfix templates for the language.");
        descSubLabel2.setText("To enable/disable a postfix template select it inside the group.");

        renderCodeSnippet(beforeCodePane,
                "The sample code featuring selected template will be shown here.\n[Flashing rectangle] shows the place where the intention is applicable.",
                true);
        renderCodeSnippet(afterCodePane,
                "Postfix completion invocation result will be shown here.",
                false);
    }

    private void setupListeners() {
        showAsCommandCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        expandCombo.valueProperty().addListener((obs, o, n) -> notifyModified());

        treeView.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            updatePreview(selected);
        });
    }

    public void loadFromSettings(PostfixCompletionSettings s) {
        suppressEvents = true;
        try {
            enablePostfixCheck.setSelected(s.isEnablePostfixCompletion());
            showAsCommandCheck.setSelected(s.isShowAsCommandCompletions());
            expandCombo.setValue(s.getExpandShortcut());

            treeRoot.getChildren().clear();
            languageNodes.clear();
            templateNodes.clear();

            for (String lang : PostfixCompletionSettings.ALL_LANGUAGES) {
                TreeItem<TreeData> langNode = new TreeItem<>(new TreeData(lang, null, true));
                languageNodes.put(lang, langNode);

                List<TemplateItem> items = s.getTemplatesForLanguage(lang);
                for (TemplateItem t : items) {
                    TreeItem<TreeData> itemNode = new TreeItem<>(new TreeData(lang, t, false));
                    templateNodes.put(t.getId(), itemNode);
                    langNode.getChildren().add(itemNode);
                }

                treeRoot.getChildren().add(langNode);
            }

            // Default selection: select Java group or first
            TreeItem<TreeData> javaGroup = languageNodes.get("Java");
            if (javaGroup != null) {
                javaGroup.setExpanded(true);
                treeView.getSelectionModel().select(javaGroup);
                updatePreview(javaGroup);
            }
        } finally {
            suppressEvents = false;
        }
    }

    public void saveToSettings(PostfixCompletionSettings s) {
        s.setEnablePostfixCompletion(enablePostfixCheck.isSelected());
        s.setShowAsCommandCompletions(showAsCommandCheck.isSelected());
        if (expandCombo.getValue() != null) {
            s.setExpandShortcut(expandCombo.getValue());
        }

        for (Map.Entry<String, TreeItem<TreeData>> entry : languageNodes.entrySet()) {
            String lang = entry.getKey();
            TreeItem<TreeData> node = entry.getValue();

            // Check if all or any child is checked
            boolean anyEnabled = false;
            for (TreeItem<TreeData> child : node.getChildren()) {
                if (child.getValue() != null && child.getValue().template != null) {
                    TemplateItem t = child.getValue().template;
                    s.getAllTemplates().get(t.getId()).setEnabled(t.isEnabled());
                    if (t.isEnabled()) anyEnabled = true;
                }
            }
            s.setLanguageEnabled(lang, anyEnabled);
        }
    }

    public boolean isModified() {
        PostfixCompletionSettings current = new PostfixCompletionSettings();
        saveToSettings(current);
        return current.isModified(PostfixCompletionSettings.getInstance());
    }

    public void apply() {
        saveToSettings(PostfixCompletionSettings.getInstance());
        PostfixCompletionSettings.getInstance().save();
    }

    public void reset() {
        loadFromSettings(PostfixCompletionSettings.getInstance());
    }

    // Custom CheckBox TreeCell
    private class PostfixTreeCell extends TreeCell<TreeData> {
        private final CheckBox checkBox = new CheckBox();
        private final Label primaryLabel = new Label();
        private final Label secondaryLabel = new Label();
        private final HBox cellBox = new HBox(6);

        public PostfixTreeCell() {
            cellBox.setAlignment(Pos.CENTER_LEFT);
            setStyle("-fx-background-color: transparent; -fx-padding: 3 6 3 4;");

            checkBox.setFocusTraversable(false);
            checkBox.setStyle("-fx-cursor: hand;");

            cellBox.getChildren().addAll(checkBox, primaryLabel, secondaryLabel);

            selectedProperty().addListener((obs, wasSel, isSel) -> updateRowVisuals(isSel, isHover()));
            hoverProperty().addListener((obs, wasHov, isHov) -> updateRowVisuals(isSelected(), isHov));

            checkBox.setOnAction(e -> {
                TreeItem<TreeData> item = getTreeItem();
                if (item != null && item.getValue() != null) {
                    boolean checked = checkBox.isSelected();
                    if (item.getValue().isLanguageGroup) {
                        for (TreeItem<TreeData> child : item.getChildren()) {
                            if (child.getValue() != null && child.getValue().template != null) {
                                child.getValue().template.setEnabled(checked);
                            }
                        }
                        treeView.refresh();
                    } else if (item.getValue().template != null) {
                        item.getValue().template.setEnabled(checked);
                    }
                    notifyModified();
                }
            });
        }

        private void updateRowVisuals(boolean selected, boolean hover) {
            TreeData data = getItem();
            boolean isGroup = data != null && data.isLanguageGroup;
            if (selected) {
                setStyle("-fx-background-color: #2E436E; -fx-padding: 3 6 3 4;");
                primaryLabel.setStyle("-fx-text-fill: #FFFFFF; -fx-font-size: 12px;" + (isGroup ? " -fx-font-weight: bold;" : ""));
                secondaryLabel.setStyle("-fx-text-fill: #B0C4DE; -fx-font-size: 12px;");
            } else if (hover) {
                setStyle("-fx-background-color: #2B2D30; -fx-padding: 3 6 3 4;");
                primaryLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;" + (isGroup ? " -fx-font-weight: bold;" : ""));
                secondaryLabel.setStyle("-fx-text-fill: #848BA3; -fx-font-size: 12px;");
            } else {
                setStyle("-fx-background-color: transparent; -fx-padding: 3 6 3 4;");
                primaryLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;" + (isGroup ? " -fx-font-weight: bold;" : ""));
                secondaryLabel.setStyle("-fx-text-fill: #848BA3; -fx-font-size: 12px;");
            }
        }

        @Override
        protected void updateItem(TreeData data, boolean empty) {
            super.updateItem(data, empty);
            if (empty || data == null) {
                setText(null);
                setGraphic(null);
                setStyle("-fx-background-color: transparent; -fx-padding: 3 6 3 4;");
            } else {
                if (data.isLanguageGroup) {
                    primaryLabel.setText(data.language);
                    primaryLabel.setMinWidth(Region.USE_COMPUTED_SIZE);
                    secondaryLabel.setText("");

                    // Checkbox state: true if any enabled
                    TreeItem<TreeData> treeItem = getTreeItem();
                    boolean noneChecked = true;
                    if (treeItem != null && !treeItem.getChildren().isEmpty()) {
                        for (TreeItem<TreeData> c : treeItem.getChildren()) {
                            if (c.getValue() != null && c.getValue().template != null && c.getValue().template.isEnabled()) {
                                noneChecked = false;
                                break;
                            }
                        }
                    }
                    checkBox.setSelected(!noneChecked);
                } else if (data.template != null) {
                    primaryLabel.setText(data.template.getKey());
                    primaryLabel.setMinWidth(55);
                    secondaryLabel.setText(data.template.getExample());
                    checkBox.setSelected(data.template.isEnabled());
                }
                updateRowVisuals(isSelected(), isHover());
                setText(null);
                setGraphic(cellBox);
            }
        }
    }

    // Accessors for testing
    CheckBox getEnablePostfixCheck() { return enablePostfixCheck; }
    CheckBox getShowAsCommandCheck() { return showAsCommandCheck; }
    ComboBox<String> getExpandCombo() { return expandCombo; }
    TreeView<TreeData> getTreeView() { return treeView; }
    Button getAddButton() { return addButton; }
    Button getRemoveButton() { return removeButton; }
    Button getEditButton() { return editButton; }
    Button getDuplicateButton() { return duplicateButton; }
    Map<String, TreeItem<TreeData>> getLanguageNodes() { return languageNodes; }
}