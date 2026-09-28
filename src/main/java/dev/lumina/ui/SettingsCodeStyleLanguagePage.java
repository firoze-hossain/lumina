package dev.lumina.ui;

import dev.lumina.settings.CodeStyleSettings;
import dev.lumina.settings.CodeStyleSettings.CodeStyleScheme;
import dev.lumina.settings.CodeStyleSettings.LanguageCodeStyleProvider;
import dev.lumina.settings.CodeStyleSettings.LanguageCodeStyleSettings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Editor > Code Style > [Language] settings page (e.g. Java, Kotlin, Python, etc.).
 * Matches 1:1 with reference screenshots:
 * - media_1790562058172.png
 * - media_1790562074199.png
 *
 * Features:
 * - Top header with Scheme selector and "Set from..." link.
 * - Dynamic tab bar: Tabs and Indents, Spaces, Wrapping and Braces, Blank Lines, JavaDoc, Imports, Arrangement, Code Generation, Java EE Names.
 * - Left pane: Complete Tabs and Indents configuration controls (Use tab character, Smart tabs, Tab size, Indent, Continuation indent, Keep indents on empty lines, Label indent, Absolute label indent, Do not indent top level class members, Use indents relative to expression start).
 * - Right pane: Real-time Live Code Preview with dotted indent guides, syntax highlighting, and instant formatting synchronization.
 */
public class SettingsCodeStyleLanguagePage extends VBox {

    private final String languageId;
    private final CodeStyleHeaderBar headerBar;
    private final Hyperlink setFromLink = new Hyperlink("Set from...");

    // Tabs
    private final HBox tabBar = new HBox(4);
    private final ToggleGroup tabGroup = new ToggleGroup();
    private String activeTab = "Tabs and Indents";

    // Split layout
    private final SplitPane splitPane = new SplitPane();

    // Left pane controls (Tabs and Indents)
    private final CheckBox useTabCharCheck = new CheckBox("Use tab character");
    private final CheckBox smartTabsCheck = new CheckBox("Smart tabs");
    private final TextField tabSizeField = new TextField("4");
    private final TextField indentField = new TextField("4");
    private final TextField continuationIndentField = new TextField("8");
    private final CheckBox keepIndentsEmptyLinesCheck = new CheckBox("Keep indents on empty lines");
    private final TextField labelIndentField = new TextField("0");
    private final CheckBox absoluteLabelIndentCheck = new CheckBox("Absolute label indent");
    private final CheckBox doNotIndentTopLevelMembersCheck = new CheckBox("Do not indent top level class members");
    private final CheckBox useIndentsRelativeToExprCheck = new CheckBox("Use indents relative to expression start");

    // Right pane: Live Preview
    private final ScrollPane previewScrollPane = new ScrollPane();
    private final VBox previewLinesBox = new VBox(1);

    private Runnable onModifiedListener;
    private boolean suppressEvents = false;

    // Baseline snapshot for isModified
    private boolean originalUseTabChar;
    private boolean originalSmartTabs;
    private int originalTabSize;
    private int originalIndent;
    private int originalContinuationIndent;
    private boolean originalKeepIndentsEmptyLines;
    private int originalLabelIndent;
    private boolean originalAbsoluteLabelIndent;
    private boolean originalDoNotIndentTopLevelMembers;
    private boolean originalUseIndentsRelativeToExpr;

    public SettingsCodeStyleLanguagePage(String languageId) {
        this.languageId = languageId != null ? languageId : "Java";

        getStyleClass().add("settings-page");
        setStyle("-fx-background-color: #1E1F22;");
        setPadding(new Insets(12, 16, 16, 16));
        setSpacing(10);

        headerBar = new CodeStyleHeaderBar();
        headerBar.setOnSchemeChanged(scheme -> {
            loadFromCurrentScheme();
            notifyModified();
        });
        headerBar.setOnSettingsModified(this::notifyModified);

        buildUi();
        setupListeners();
        loadFromCurrentScheme();
        updatePreview();
    }

    private void buildUi() {
        // --- 1. Top Bar with Scheme and "Set from..." link ---
        HBox topBar = new HBox();
        topBar.setAlignment(Pos.CENTER_LEFT);

        HBox.setHgrow(headerBar, Priority.ALWAYS);

        setFromLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false;");
        setFromLink.setOnMouseEntered(e -> setFromLink.setUnderline(true));
        setFromLink.setOnMouseExited(e -> setFromLink.setUnderline(false));

        ContextMenu setFromMenu = new ContextMenu();
        setFromMenu.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #393B40;");

        MenuItem googleStyle = new MenuItem("Google Style Guide");
        googleStyle.setOnAction(e -> applyPredefinedGoogleStyle());

        MenuItem platformDefault = new MenuItem("Platform Default Style");
        platformDefault.setOnAction(e -> applyPredefinedDefaultStyle());

        Menu setFromLangMenu = new Menu("Set from language...");
        for (LanguageCodeStyleProvider p : LanguageCodeStyleProvider.getAllProviders()) {
            if (!p.getLanguageId().equals(this.languageId)) {
                MenuItem item = new MenuItem(p.getDisplayName());
                item.setOnAction(ev -> copyFromOtherLanguage(p.getLanguageId()));
                setFromLangMenu.getItems().add(item);
            }
        }

        setFromMenu.getItems().addAll(googleStyle, platformDefault, new SeparatorMenuItem(), setFromLangMenu);
        setFromLink.setOnAction(e -> setFromMenu.show(setFromLink, Side.BOTTOM, 0, 2));

        topBar.getChildren().addAll(headerBar, setFromLink);
        getChildren().add(topBar);

        // --- 2. Tab Bar ---
        buildTabBar();
        getChildren().add(tabBar);

        // --- 3. Split Pane (Controls on Left, Live Preview on Right) ---
        VBox leftPane = buildTabsAndIndentsPane();
        VBox rightPane = buildPreviewPane();

        splitPane.getItems().addAll(leftPane, rightPane);
        splitPane.setDividerPositions(0.40);
        splitPane.setStyle("-fx-background-color: #1E1F22; -fx-box-border: transparent;");
        VBox.setVgrow(splitPane, Priority.ALWAYS);

        getChildren().add(splitPane);
    }

    private void buildTabBar() {
        tabBar.setAlignment(Pos.CENTER_LEFT);
        tabBar.setPadding(new Insets(2, 0, 6, 0));

        LanguageCodeStyleProvider provider = LanguageCodeStyleProvider.getProvider(languageId);
        List<String> tabs = provider != null ? provider.getSupportedTabs() : List.of("Tabs and Indents", "Spaces", "Wrapping and Braces", "Blank Lines");

        for (String tabTitle : tabs) {
            ToggleButton btn = new ToggleButton(tabTitle);
            btn.setToggleGroup(tabGroup);
            styleTabButton(btn);

            if (tabTitle.equals(activeTab)) {
                btn.setSelected(true);
            }

            btn.setOnAction(e -> {
                activeTab = tabTitle;
                updatePreview();
            });

            tabBar.getChildren().add(btn);
        }
    }

    private void styleTabButton(ToggleButton btn) {
        btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #848BA3; -fx-font-size: 12px; -fx-padding: 4 10 4 10; -fx-cursor: hand; -fx-border-color: transparent;");
        btn.selectedProperty().addListener((obs, old, isSelected) -> {
            if (isSelected) {
                btn.setStyle("-fx-background-color: #35538F; -fx-text-fill: #FFFFFF; -fx-font-weight: bold; -fx-font-size: 12px; -fx-padding: 4 10 4 10; -fx-background-radius: 4;");
            } else {
                btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #848BA3; -fx-font-size: 12px; -fx-padding: 4 10 4 10; -fx-cursor: hand; -fx-border-color: transparent;");
            }
        });
        if (btn.isSelected()) {
            btn.setStyle("-fx-background-color: #35538F; -fx-text-fill: #FFFFFF; -fx-font-weight: bold; -fx-font-size: 12px; -fx-padding: 4 10 4 10; -fx-background-radius: 4;");
        }
    }

    private VBox buildTabsAndIndentsPane() {
        VBox pane = new VBox(12);
        pane.setPadding(new Insets(8, 16, 12, 4));
        pane.setStyle("-fx-background-color: #1E1F22;");

        // 1. Use tab character & smart tabs
        useTabCharCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        smartTabsCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-padding: 0 0 0 20;");
        smartTabsCheck.disableProperty().bind(useTabCharCheck.selectedProperty().not());

        pane.getChildren().addAll(useTabCharCheck, smartTabsCheck);

        // Spacer
        Region spacer1 = new Region();
        spacer1.setPrefHeight(4);
        pane.getChildren().add(spacer1);

        // 2. Tab size, Indent, Continuation indent
        pane.getChildren().add(createNumberRow("Tab size:", tabSizeField));
        pane.getChildren().add(createNumberRow("Indent:", indentField));
        pane.getChildren().add(createNumberRow("Continuation indent:", continuationIndentField));

        // Spacer
        Region spacer2 = new Region();
        spacer2.setPrefHeight(4);
        pane.getChildren().add(spacer2);

        // 3. Keep indents on empty lines
        keepIndentsEmptyLinesCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        pane.getChildren().add(keepIndentsEmptyLinesCheck);

        // Spacer
        Region spacer3 = new Region();
        spacer3.setPrefHeight(4);
        pane.getChildren().add(spacer3);

        // 4. Label indent & Absolute label indent
        pane.getChildren().add(createNumberRow("Label indent:", labelIndentField));
        absoluteLabelIndentCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-padding: 0 0 0 20;");
        pane.getChildren().add(absoluteLabelIndentCheck);

        // Spacer
        Region spacer4 = new Region();
        spacer4.setPrefHeight(4);
        pane.getChildren().add(spacer4);

        // 5. Additional indentation rules
        doNotIndentTopLevelMembersCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        useIndentsRelativeToExprCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        pane.getChildren().addAll(doNotIndentTopLevelMembersCheck, useIndentsRelativeToExprCheck);

        return pane;
    }

    private HBox createNumberRow(String labelText, TextField field) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);

        Label lbl = new Label(labelText);
        lbl.setPrefWidth(140);
        lbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        field.setPrefWidth(60);
        field.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 3 6 3 6; -fx-font-size: 12px;");

        row.getChildren().addAll(lbl, field);
        return row;
    }

    private VBox buildPreviewPane() {
        VBox rightPane = new VBox();
        rightPane.setStyle("-fx-background-color: #1E1F22; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4;");
        rightPane.setPadding(new Insets(6));

        previewLinesBox.setStyle("-fx-background-color: #1E1F22;");
        previewLinesBox.setPadding(new Insets(10, 14, 14, 14));

        previewScrollPane.setContent(previewLinesBox);
        previewScrollPane.setFitToWidth(true);
        previewScrollPane.setStyle("-fx-background: #1E1F22; -fx-background-color: transparent; -fx-border-color: transparent;");
        VBox.setVgrow(previewScrollPane, Priority.ALWAYS);

        rightPane.getChildren().add(previewScrollPane);
        return rightPane;
    }

    private void setupListeners() {
        useTabCharCheck.selectedProperty().addListener((obs, o, n) -> {
            updatePreview();
            notifyModified();
        });
        smartTabsCheck.selectedProperty().addListener((obs, o, n) -> {
            updatePreview();
            notifyModified();
        });
        tabSizeField.textProperty().addListener((obs, o, n) -> {
            updatePreview();
            notifyModified();
        });
        indentField.textProperty().addListener((obs, o, n) -> {
            updatePreview();
            notifyModified();
        });
        continuationIndentField.textProperty().addListener((obs, o, n) -> {
            updatePreview();
            notifyModified();
        });
        keepIndentsEmptyLinesCheck.selectedProperty().addListener((obs, o, n) -> {
            updatePreview();
            notifyModified();
        });
        labelIndentField.textProperty().addListener((obs, o, n) -> {
            updatePreview();
            notifyModified();
        });
        absoluteLabelIndentCheck.selectedProperty().addListener((obs, o, n) -> {
            updatePreview();
            notifyModified();
        });
        doNotIndentTopLevelMembersCheck.selectedProperty().addListener((obs, o, n) -> {
            updatePreview();
            notifyModified();
        });
        useIndentsRelativeToExprCheck.selectedProperty().addListener((obs, o, n) -> {
            updatePreview();
            notifyModified();
        });
    }

    public void loadFromCurrentScheme() {
        CodeStyleScheme scheme = CodeStyleSettings.getInstance().getActiveScheme();
        if (scheme == null) return;
        LanguageCodeStyleSettings settings = scheme.getLanguageSettings(this.languageId);
        if (settings == null) return;

        suppressEvents = true;
        try {
            useTabCharCheck.setSelected(settings.isUseTabCharacter());
            smartTabsCheck.setSelected(settings.isSmartTabs());
            tabSizeField.setText(String.valueOf(settings.getTabSize()));
            indentField.setText(String.valueOf(settings.getIndent()));
            continuationIndentField.setText(String.valueOf(settings.getContinuationIndent()));
            keepIndentsEmptyLinesCheck.setSelected(settings.isKeepIndentsOnEmptyLines());
            labelIndentField.setText(String.valueOf(settings.getLabelIndent()));
            absoluteLabelIndentCheck.setSelected(settings.isAbsoluteLabelIndent());
            doNotIndentTopLevelMembersCheck.setSelected(settings.isDoNotIndentTopLevelMembers());
            useIndentsRelativeToExprCheck.setSelected(settings.isUseIndentsRelativeToExpressionStart());

            // Save snapshot
            originalUseTabChar = settings.isUseTabCharacter();
            originalSmartTabs = settings.isSmartTabs();
            originalTabSize = settings.getTabSize();
            originalIndent = settings.getIndent();
            originalContinuationIndent = settings.getContinuationIndent();
            originalKeepIndentsEmptyLines = settings.isKeepIndentsOnEmptyLines();
            originalLabelIndent = settings.getLabelIndent();
            originalAbsoluteLabelIndent = settings.isAbsoluteLabelIndent();
            originalDoNotIndentTopLevelMembers = settings.isDoNotIndentTopLevelMembers();
            originalUseIndentsRelativeToExpr = settings.isUseIndentsRelativeToExpressionStart();

            headerBar.updateWarningBanner();
        } finally {
            suppressEvents = false;
        }
    }

    public void apply() {
        CodeStyleScheme scheme = CodeStyleSettings.getInstance().getActiveScheme();
        if (scheme == null) return;
        LanguageCodeStyleSettings settings = scheme.getLanguageSettings(this.languageId);
        if (settings == null) return;

        settings.setUseTabCharacter(useTabCharCheck.isSelected());
        settings.setSmartTabs(smartTabsCheck.isSelected());
        try { settings.setTabSize(Integer.parseInt(tabSizeField.getText().trim())); } catch (Exception ignored) {}
        try { settings.setIndent(Integer.parseInt(indentField.getText().trim())); } catch (Exception ignored) {}
        try { settings.setContinuationIndent(Integer.parseInt(continuationIndentField.getText().trim())); } catch (Exception ignored) {}
        settings.setKeepIndentsOnEmptyLines(keepIndentsEmptyLinesCheck.isSelected());
        try { settings.setLabelIndent(Integer.parseInt(labelIndentField.getText().trim())); } catch (Exception ignored) {}
        settings.setAbsoluteLabelIndent(absoluteLabelIndentCheck.isSelected());
        settings.setDoNotIndentTopLevelMembers(doNotIndentTopLevelMembersCheck.isSelected());
        settings.setUseIndentsRelativeToExpressionStart(useIndentsRelativeToExprCheck.isSelected());

        CodeStyleSettings.getInstance().saveSettings();

        // Update baseline
        originalUseTabChar = settings.isUseTabCharacter();
        originalSmartTabs = settings.isSmartTabs();
        originalTabSize = settings.getTabSize();
        originalIndent = settings.getIndent();
        originalContinuationIndent = settings.getContinuationIndent();
        originalKeepIndentsEmptyLines = settings.isKeepIndentsOnEmptyLines();
        originalLabelIndent = settings.getLabelIndent();
        originalAbsoluteLabelIndent = settings.isAbsoluteLabelIndent();
        originalDoNotIndentTopLevelMembers = settings.isDoNotIndentTopLevelMembers();
        originalUseIndentsRelativeToExpr = settings.isUseIndentsRelativeToExpressionStart();
    }

    public void reset() {
        loadFromCurrentScheme();
        updatePreview();
    }

    public boolean isModified() {
        if (useTabCharCheck.isSelected() != originalUseTabChar) return true;
        if (smartTabsCheck.isSelected() != originalSmartTabs) return true;
        try { if (Integer.parseInt(tabSizeField.getText().trim()) != originalTabSize) return true; } catch (Exception e) { return true; }
        try { if (Integer.parseInt(indentField.getText().trim()) != originalIndent) return true; } catch (Exception e) { return true; }
        try { if (Integer.parseInt(continuationIndentField.getText().trim()) != originalContinuationIndent) return true; } catch (Exception e) { return true; }
        if (keepIndentsEmptyLinesCheck.isSelected() != originalKeepIndentsEmptyLines) return true;
        try { if (Integer.parseInt(labelIndentField.getText().trim()) != originalLabelIndent) return true; } catch (Exception e) { return true; }
        if (absoluteLabelIndentCheck.isSelected() != originalAbsoluteLabelIndent) return true;
        if (doNotIndentTopLevelMembersCheck.isSelected() != originalDoNotIndentTopLevelMembers) return true;
        if (useIndentsRelativeToExprCheck.isSelected() != originalUseIndentsRelativeToExpr) return true;
        return false;
    }

    private void applyPredefinedGoogleStyle() {
        suppressEvents = true;
        try {
            useTabCharCheck.setSelected(false);
            tabSizeField.setText("2");
            indentField.setText("2");
            continuationIndentField.setText("4");
            labelIndentField.setText("0");
        } finally {
            suppressEvents = false;
        }
        updatePreview();
        notifyModified();
    }

    private void applyPredefinedDefaultStyle() {
        LanguageCodeStyleProvider p = LanguageCodeStyleProvider.getProvider(languageId);
        if (p == null) return;
        LanguageCodeStyleSettings def = p.createDefaultSettings();

        suppressEvents = true;
        try {
            useTabCharCheck.setSelected(def.isUseTabCharacter());
            smartTabsCheck.setSelected(def.isSmartTabs());
            tabSizeField.setText(String.valueOf(def.getTabSize()));
            indentField.setText(String.valueOf(def.getIndent()));
            continuationIndentField.setText(String.valueOf(def.getContinuationIndent()));
            keepIndentsEmptyLinesCheck.setSelected(def.isKeepIndentsOnEmptyLines());
            labelIndentField.setText(String.valueOf(def.getLabelIndent()));
            absoluteLabelIndentCheck.setSelected(def.isAbsoluteLabelIndent());
            doNotIndentTopLevelMembersCheck.setSelected(def.isDoNotIndentTopLevelMembers());
            useIndentsRelativeToExprCheck.setSelected(def.isUseIndentsRelativeToExpressionStart());
        } finally {
            suppressEvents = false;
        }
        updatePreview();
        notifyModified();
    }

    private void copyFromOtherLanguage(String otherLangId) {
        CodeStyleScheme scheme = CodeStyleSettings.getInstance().getActiveScheme();
        if (scheme == null) return;
        LanguageCodeStyleSettings other = scheme.getLanguageSettings(otherLangId);
        if (other == null) return;

        suppressEvents = true;
        try {
            useTabCharCheck.setSelected(other.isUseTabCharacter());
            smartTabsCheck.setSelected(other.isSmartTabs());
            tabSizeField.setText(String.valueOf(other.getTabSize()));
            indentField.setText(String.valueOf(other.getIndent()));
            continuationIndentField.setText(String.valueOf(other.getContinuationIndent()));
            keepIndentsEmptyLinesCheck.setSelected(other.isKeepIndentsOnEmptyLines());
            labelIndentField.setText(String.valueOf(other.getLabelIndent()));
            absoluteLabelIndentCheck.setSelected(other.isAbsoluteLabelIndent());
            doNotIndentTopLevelMembersCheck.setSelected(other.isDoNotIndentTopLevelMembers());
            useIndentsRelativeToExprCheck.setSelected(other.isUseIndentsRelativeToExpressionStart());
        } finally {
            suppressEvents = false;
        }
        updatePreview();
        notifyModified();
    }

    /**
     * Re-formats and re-renders the live preview pane in real-time.
     */
    public void updatePreview() {
        LanguageCodeStyleProvider provider = LanguageCodeStyleProvider.getProvider(languageId);
        String rawSample = provider != null ? provider.getSampleCode() : "// No preview available";

        LanguageCodeStyleSettings previewSettings = new LanguageCodeStyleSettings(this.languageId);
        previewSettings.setUseTabCharacter(useTabCharCheck.isSelected());
        previewSettings.setSmartTabs(smartTabsCheck.isSelected());
        try { previewSettings.setTabSize(Integer.parseInt(tabSizeField.getText().trim())); } catch (Exception ignored) {}
        try { previewSettings.setIndent(Integer.parseInt(indentField.getText().trim())); } catch (Exception ignored) {}
        try { previewSettings.setContinuationIndent(Integer.parseInt(continuationIndentField.getText().trim())); } catch (Exception ignored) {}
        previewSettings.setKeepIndentsOnEmptyLines(keepIndentsEmptyLinesCheck.isSelected());
        try { previewSettings.setLabelIndent(Integer.parseInt(labelIndentField.getText().trim())); } catch (Exception ignored) {}
        previewSettings.setAbsoluteLabelIndent(absoluteLabelIndentCheck.isSelected());
        previewSettings.setDoNotIndentTopLevelMembers(doNotIndentTopLevelMembersCheck.isSelected());
        previewSettings.setUseIndentsRelativeToExpressionStart(useIndentsRelativeToExprCheck.isSelected());

        String formattedCode = CodeStyleSettings.formatCodeSample(rawSample, previewSettings);

        previewLinesBox.getChildren().clear();
        String[] lines = formattedCode.split("\n", -1);

        for (String line : lines) {
            TextFlow flow = buildLineTextFlow(line, previewSettings);
            previewLinesBox.getChildren().add(flow);
        }
    }

    private TextFlow buildLineTextFlow(String line, LanguageCodeStyleSettings settings) {
        TextFlow flow = new TextFlow();
        flow.setPrefWidth(Region.USE_COMPUTED_SIZE);

        int leadingWhitespace = 0;
        while (leadingWhitespace < line.length() && (line.charAt(leadingWhitespace) == ' ' || line.charAt(leadingWhitespace) == '\t')) {
            leadingWhitespace++;
        }

        String indentPrefix = line.substring(0, leadingWhitespace);
        String codeText = line.substring(leadingWhitespace);

        // Render indent guides (subtle · dots for spaces, → for tabs)
        if (!indentPrefix.isEmpty()) {
            StringBuilder guideBuilder = new StringBuilder();
            for (char c : indentPrefix.toCharArray()) {
                if (c == '\t') {
                    guideBuilder.append("→   ");
                } else {
                    guideBuilder.append("·");
                }
            }
            Text indentGuideText = new Text(guideBuilder.toString());
            indentGuideText.setFont(Font.font("monospace", 12));
            indentGuideText.setFill(Color.web("#393B40")); // subtle indent guide color matching IDE
            flow.getChildren().add(indentGuideText);
        }

        // Render syntax-highlighted code text
        highlightAndAppend(flow, codeText);

        return flow;
    }

    private void highlightAndAppend(TextFlow flow, String code) {
        if (code.isEmpty()) return;

        // Simple token matcher for keywords, types, numbers, strings
        Pattern tokenPattern = Pattern.compile(
                "\\b(public|private|protected|class|interface|enum|record|void|int|long|boolean|char|float|double|" +
                "try|catch|finally|throw|throws|if|else|do|while|for|switch|case|default|break|continue|return|" +
                "new|package|import|extends|implements|static|final|fun|val|var|def|type|func|struct|fn|let|mut|" +
                "async|await|const|export|from)\\b|" +
                "(\"[^\"]*\")|" +
                "(\\b\\d+\\b)|" +
                "(\\b[A-Z][a-zA-Z0-9_]*\\b)|" +
                "(//.*|/\\*.*\\*/)"
        );

        Matcher matcher = tokenPattern.matcher(code);
        int lastEnd = 0;

        while (matcher.find()) {
            if (matcher.start() > lastEnd) {
                Text plain = new Text(code.substring(lastEnd, matcher.start()));
                plain.setFont(Font.font("monospace", 12));
                plain.setFill(Color.web("#BCBEC4"));
                flow.getChildren().add(plain);
            }

            Text token = new Text(matcher.group());
            token.setFont(Font.font("monospace", 12));

            if (matcher.group(1) != null) {
                // Keyword (orange/peach)
                token.setFill(Color.web("#CF8E6D"));
            } else if (matcher.group(2) != null) {
                // String (green)
                token.setFill(Color.web("#6AAB73"));
            } else if (matcher.group(3) != null) {
                // Number (cyan/blue)
                token.setFill(Color.web("#2AACB8"));
            } else if (matcher.group(4) != null) {
                // Class/Type (yellow/gold)
                token.setFill(Color.web("#C0B271"));
            } else if (matcher.group(5) != null) {
                // Comment (gray)
                token.setFill(Color.web("#7A7E85"));
            } else {
                token.setFill(Color.web("#BCBEC4"));
            }

            flow.getChildren().add(token);
            lastEnd = matcher.end();
        }

        if (lastEnd < code.length()) {
            Text plain = new Text(code.substring(lastEnd));
            plain.setFont(Font.font("monospace", 12));
            plain.setFill(Color.web("#BCBEC4"));
            flow.getChildren().add(plain);
        }
    }

    private void notifyModified() {
        if (!suppressEvents && onModifiedListener != null) {
            onModifiedListener.run();
        }
    }

    public void setOnModifiedListener(Runnable listener) {
        this.onModifiedListener = listener;
    }

    public CodeStyleHeaderBar getHeaderBar() {
        return headerBar;
    }

    public String getLanguageId() {
        return languageId;
    }
}
