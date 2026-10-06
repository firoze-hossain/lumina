// SettingsCopyrightFormattingLanguagePage.java
package dev.lumina.ui;

import dev.lumina.copyright.CopyrightFormattingOptions;
import dev.lumina.copyright.CopyrightManager;
import dev.lumina.copyright.CopyrightProfile;
import dev.lumina.copyright.LanguageFormattingOverride;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/**
 * Reusable and customizable settings page for Editor > Copyright > Formatting > [Language],
 * faithfully matching IntelliJ IDEA's design (screenshots media_1791264931119.png to media_1791265032530.png).
 */
public class SettingsCopyrightFormattingLanguagePage extends VBox {

    private static final String[] PREVIEW_LINES = {
            "Copyright (c) 2026. Lorem ipsum dolor sit amet, consectetur adipiscing elit.",
            "Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.",
            "Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.",
            "Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.",
            "Vestibulum commodo. Ut rhoncus gravida arcu."
    };

    private final String language;

    // Top mode radio buttons
    private final RadioButton noCopyrightRadio = new RadioButton("No copyright");
    private final RadioButton useDefaultRadio = new RadioButton("Use default settings");
    private final RadioButton useCustomRadio = new RadioButton("Use custom formatting options");
    private final ToggleGroup modeGroup = new ToggleGroup();

    // Containers for sub-groups
    private final VBox commentTypeBox = new VBox(8);
    private final VBox locationBox = new VBox(8);
    private final VBox col2 = new VBox(8);

    // Comment Type controls
    private final RadioButton useBlockCommentRadio = new RadioButton("Use block comment");
    private final CheckBox prefixEachLineCheck = new CheckBox("Prefix each line");
    private final RadioButton useLineCommentRadio = new RadioButton("Use line comment");
    private final ToggleGroup commentTypeGroup = new ToggleGroup();

    // Relative Location controls
    private final RadioButton beforeOtherCommentsRadio = new RadioButton("Before other comments");
    private final RadioButton afterOtherCommentsRadio = new RadioButton("After other comments");
    private final ToggleGroup relativeLocationGroup = new ToggleGroup();

    // Location in File (for Java, HTML, XML, DTD, etc.)
    private VBox locationInFileBox = null;
    private final RadioButton beforePackageRadio = new RadioButton("Before package");
    private final RadioButton beforeImportsRadio = new RadioButton("Before imports");
    private final RadioButton beforeClassRadio = new RadioButton("Before class");
    private final RadioButton beforeDoctypeRadio = new RadioButton("Before doctype");
    private final RadioButton beforeRootTagRadio = new RadioButton("Before root tag");
    private final ToggleGroup locationInFileGroup = new ToggleGroup();

    // Borders controls
    private final CheckBox separatorBeforeCheck = new CheckBox("Separator before");
    private final TextField separatorBeforeLengthField = new TextField("80");
    private final CheckBox separatorAfterCheck = new CheckBox("Separator after");
    private final TextField separatorAfterLengthField = new TextField("80");
    private final TextField separatorCharField = new TextField();
    private final CheckBox boxCheck = new CheckBox("Box");
    private final CheckBox blankLineBeforeCheck = new CheckBox("Add blank line before");
    private final CheckBox blankLineAfterCheck = new CheckBox("Add blank line after");
    private final HBox optionsBox = new HBox(40);

    // Code Preview
    private final TextArea previewArea = new TextArea();

    // State
    private LanguageFormattingOverride workingOverride;
    private LanguageFormattingOverride originalOverride;
    private boolean isUpdating = false;
    private Runnable onModifiedListener;

    public SettingsCopyrightFormattingLanguagePage(String language) {
        this.language = language != null ? language : "Java";
        getStyleClass().add("settings-page");
        setPadding(new Insets(16, 20, 16, 20));
        setSpacing(14);
        setStyle("-fx-background-color: #1E1F22;");
        VBox.setVgrow(this, Priority.ALWAYS);

        initLayout();
        loadDataFromManager();
    }

    private void initLayout() {
        // ============================================================
        // Top Radio Buttons
        // ============================================================
        noCopyrightRadio.setToggleGroup(modeGroup);
        useDefaultRadio.setToggleGroup(modeGroup);
        useCustomRadio.setToggleGroup(modeGroup);
        noCopyrightRadio.setStyle(radioStyle());
        useDefaultRadio.setStyle(radioStyle());
        useCustomRadio.setStyle(radioStyle());

        VBox topModeBox = new VBox(6, noCopyrightRadio, useDefaultRadio, useCustomRadio);

        modeGroup.selectedToggleProperty().addListener((obs, old, isSel) -> {
            if (!isUpdating) {
                if (noCopyrightRadio.isSelected()) {
                    workingOverride.setMode(LanguageFormattingOverride.Mode.NO_COPYRIGHT);
                } else if (useCustomRadio.isSelected()) {
                    workingOverride.setMode(LanguageFormattingOverride.Mode.USE_CUSTOM);
                } else {
                    workingOverride.setMode(LanguageFormattingOverride.Mode.USE_DEFAULT);
                }
                updateControlStates();
                updatePreview();
                notifyModified();
            }
        });

        // ============================================================
        // Two columns: Comment Type / Location & Borders
        // ============================================================
        optionsBox.setAlignment(Pos.TOP_LEFT);

        VBox col1 = new VBox(16);
        col1.setPrefWidth(320);

        // Group 1: Comment Type
        commentTypeBox.getChildren().clear();
        Node commentTypeHeader = createSectionHeader("Comment Type");

        useBlockCommentRadio.setToggleGroup(commentTypeGroup);
        useLineCommentRadio.setToggleGroup(commentTypeGroup);
        useBlockCommentRadio.setStyle(radioStyle());
        useLineCommentRadio.setStyle(radioStyle());
        prefixEachLineCheck.setStyle(checkStyle());
        VBox.setMargin(prefixEachLineCheck, new Insets(0, 0, 0, 22));

        commentTypeGroup.selectedToggleProperty().addListener((obs, old, isSel) -> {
            if (!isUpdating && useCustomRadio.isSelected()) {
                workingOverride.getCustomOptions().setCommentType(
                        useBlockCommentRadio.isSelected()
                                ? CopyrightFormattingOptions.CommentType.BLOCK
                                : CopyrightFormattingOptions.CommentType.LINE
                );
                prefixEachLineCheck.setDisable(!useBlockCommentRadio.isSelected());
                updatePreview();
                notifyModified();
            }
        });

        prefixEachLineCheck.selectedProperty().addListener((obs, old, isSel) -> {
            if (!isUpdating && useCustomRadio.isSelected()) {
                workingOverride.getCustomOptions().setPrefixEachLine(isSel);
                updatePreview();
                notifyModified();
            }
        });

        commentTypeBox.getChildren().addAll(
                commentTypeHeader,
                useBlockCommentRadio,
                prefixEachLineCheck,
                useLineCommentRadio
        );

        // Group 2: Relative Location
        locationBox.getChildren().clear();
        Node locationHeader = createSectionHeader("Relative Location");

        beforeOtherCommentsRadio.setToggleGroup(relativeLocationGroup);
        afterOtherCommentsRadio.setToggleGroup(relativeLocationGroup);
        beforeOtherCommentsRadio.setStyle(radioStyle());
        afterOtherCommentsRadio.setStyle(radioStyle());

        relativeLocationGroup.selectedToggleProperty().addListener((obs, old, isSel) -> {
            if (!isUpdating && useCustomRadio.isSelected()) {
                workingOverride.getCustomOptions().setRelativeLocation(
                        beforeOtherCommentsRadio.isSelected()
                                ? CopyrightFormattingOptions.RelativeLocation.BEFORE_OTHER_COMMENTS
                                : CopyrightFormattingOptions.RelativeLocation.AFTER_OTHER_COMMENTS
                );
                notifyModified();
            }
        });

        locationBox.getChildren().addAll(locationHeader, beforeOtherCommentsRadio, afterOtherCommentsRadio);
        col1.getChildren().addAll(commentTypeBox, locationBox);

        // Group 3: Location in File (Java, HTML, XML, DTD, etc.)
        if (isJavaLanguage(language)) {
            locationInFileBox = new VBox(8);
            Node fileLocHeader = createSectionHeader("Location in File");

            beforePackageRadio.setToggleGroup(locationInFileGroup);
            beforeImportsRadio.setToggleGroup(locationInFileGroup);
            beforeClassRadio.setToggleGroup(locationInFileGroup);
            beforePackageRadio.setStyle(radioStyle());
            beforeImportsRadio.setStyle(radioStyle());
            beforeClassRadio.setStyle(radioStyle());

            locationInFileGroup.selectedToggleProperty().addListener((obs, old, isSel) -> {
                if (!isUpdating && !noCopyrightRadio.isSelected()) {
                    if (beforeImportsRadio.isSelected()) {
                        workingOverride.getCustomOptions().setLocationInFile(CopyrightFormattingOptions.LocationInFile.BEFORE_IMPORTS);
                    } else if (beforeClassRadio.isSelected()) {
                        workingOverride.getCustomOptions().setLocationInFile(CopyrightFormattingOptions.LocationInFile.BEFORE_CLASS);
                    } else {
                        workingOverride.getCustomOptions().setLocationInFile(CopyrightFormattingOptions.LocationInFile.BEFORE_PACKAGE);
                    }
                    notifyModified();
                }
            });

            locationInFileBox.getChildren().addAll(fileLocHeader, beforePackageRadio, beforeImportsRadio, beforeClassRadio);
            col1.getChildren().add(locationInFileBox);
        } else if (isXmlOrHtmlLanguage(language)) {
            locationInFileBox = new VBox(8);
            Node fileLocHeader = createSectionHeader("Location in File");

            beforeDoctypeRadio.setToggleGroup(locationInFileGroup);
            beforeRootTagRadio.setToggleGroup(locationInFileGroup);
            beforeDoctypeRadio.setStyle(radioStyle());
            beforeRootTagRadio.setStyle(radioStyle());

            locationInFileGroup.selectedToggleProperty().addListener((obs, old, isSel) -> {
                if (!isUpdating && !noCopyrightRadio.isSelected()) {
                    workingOverride.getCustomOptions().setLocationInFile(
                            beforeDoctypeRadio.isSelected()
                                    ? CopyrightFormattingOptions.LocationInFile.BEFORE_DOCTYPE
                                    : CopyrightFormattingOptions.LocationInFile.BEFORE_ROOT_TAG
                    );
                    notifyModified();
                }
            });

            locationInFileBox.getChildren().addAll(fileLocHeader, beforeDoctypeRadio, beforeRootTagRadio);
            col1.getChildren().add(locationInFileBox);
        }

        // ---- Column 2: Borders ----
        col2.getChildren().clear();
        col2.setPrefWidth(340);
        Node bordersHeader = createSectionHeader("Borders");

        HBox sepBeforeRow = new HBox(10);
        sepBeforeRow.setAlignment(Pos.CENTER_LEFT);
        separatorBeforeCheck.setStyle(checkStyle());
        Label lenLabel1 = new Label("Length:");
        lenLabel1.setStyle("-fx-text-fill: #868A91; -fx-font-size: 12px;");
        separatorBeforeLengthField.setStyle(inputStyle());
        separatorBeforeLengthField.setPrefWidth(55);
        sepBeforeRow.getChildren().addAll(separatorBeforeCheck, lenLabel1, separatorBeforeLengthField);

        separatorBeforeCheck.selectedProperty().addListener((obs, old, isSel) -> {
            if (!isUpdating && useCustomRadio.isSelected()) {
                workingOverride.getCustomOptions().setSeparatorBefore(isSel);
                separatorBeforeLengthField.setDisable(!isSel);
                updatePreview();
                notifyModified();
            }
        });
        separatorBeforeLengthField.textProperty().addListener((obs, old, val) -> {
            if (!isUpdating && useCustomRadio.isSelected()) {
                try {
                    workingOverride.getCustomOptions().setSeparatorBeforeLength(Integer.parseInt(val.trim()));
                    updatePreview();
                    notifyModified();
                } catch (NumberFormatException ignored) {}
            }
        });

        HBox sepAfterRow = new HBox(10);
        sepAfterRow.setAlignment(Pos.CENTER_LEFT);
        separatorAfterCheck.setStyle(checkStyle());
        Label lenLabel2 = new Label("Length:");
        lenLabel2.setStyle("-fx-text-fill: #868A91; -fx-font-size: 12px;");
        separatorAfterLengthField.setStyle(inputStyle());
        separatorAfterLengthField.setPrefWidth(55);
        sepAfterRow.getChildren().addAll(separatorAfterCheck, lenLabel2, separatorAfterLengthField);

        separatorAfterCheck.selectedProperty().addListener((obs, old, isSel) -> {
            if (!isUpdating && useCustomRadio.isSelected()) {
                workingOverride.getCustomOptions().setSeparatorAfter(isSel);
                separatorAfterLengthField.setDisable(!isSel);
                updatePreview();
                notifyModified();
            }
        });
        separatorAfterLengthField.textProperty().addListener((obs, old, val) -> {
            if (!isUpdating && useCustomRadio.isSelected()) {
                try {
                    workingOverride.getCustomOptions().setSeparatorAfterLength(Integer.parseInt(val.trim()));
                    updatePreview();
                    notifyModified();
                } catch (NumberFormatException ignored) {}
            }
        });

        HBox sepCharRow = new HBox(10);
        sepCharRow.setAlignment(Pos.CENTER_LEFT);
        Label sepCharLabel = new Label("Separator:");
        sepCharLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        separatorCharField.setStyle(inputStyle());
        separatorCharField.setPrefWidth(55);
        separatorCharField.textProperty().addListener((obs, old, val) -> {
            if (!isUpdating && useCustomRadio.isSelected()) {
                workingOverride.getCustomOptions().setSeparatorChar(val != null ? val : "");
                updatePreview();
                notifyModified();
            }
        });
        sepCharRow.getChildren().addAll(sepCharLabel, separatorCharField);

        boxCheck.setStyle(checkStyle());
        boxCheck.selectedProperty().addListener((obs, old, isSel) -> {
            if (!isUpdating && useCustomRadio.isSelected()) {
                workingOverride.getCustomOptions().setBox(isSel);
                updatePreview();
                notifyModified();
            }
        });

        blankLineBeforeCheck.setStyle(checkStyle());
        blankLineBeforeCheck.selectedProperty().addListener((obs, old, isSel) -> {
            if (!isUpdating && useCustomRadio.isSelected()) {
                workingOverride.getCustomOptions().setBlankLineBefore(isSel);
                updatePreview();
                notifyModified();
            }
        });

        blankLineAfterCheck.setStyle(checkStyle());
        blankLineAfterCheck.selectedProperty().addListener((obs, old, isSel) -> {
            if (!isUpdating && useCustomRadio.isSelected()) {
                workingOverride.getCustomOptions().setBlankLineAfter(isSel);
                updatePreview();
                notifyModified();
            }
        });

        col2.getChildren().addAll(
                bordersHeader,
                sepBeforeRow,
                sepAfterRow,
                sepCharRow,
                boxCheck,
                blankLineBeforeCheck,
                blankLineAfterCheck
        );

        optionsBox.getChildren().addAll(col1, col2);

        // ---- Bottom: Code Preview ----
        previewArea.setStyle(
                "-fx-control-inner-background: #1E1F22; -fx-background-color: #1E1F22; " +
                "-fx-text-fill: #DFE1E5; -fx-font-family: 'JetBrains Mono', Consolas, monospace; " +
                "-fx-font-size: 12px; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4; " +
                "-fx-padding: 8;"
        );
        previewArea.setEditable(false);
        previewArea.setWrapText(false);
        previewArea.setPrefHeight(160);
        VBox.setVgrow(previewArea, Priority.ALWAYS);

        getChildren().addAll(topModeBox, optionsBox, previewArea);
    }

    private void updateControlStates() {
        boolean isNoCopyright = noCopyrightRadio.isSelected();
        boolean custom = useCustomRadio.isSelected();

        if (locationInFileBox == null) {
            optionsBox.setDisable(!custom);
        } else {
            optionsBox.setDisable(false);
        }

        commentTypeBox.setDisable(!custom);
        locationBox.setDisable(!custom);
        col2.setDisable(!custom);

        useBlockCommentRadio.setDisable(!custom);
        useLineCommentRadio.setDisable(!custom);
        prefixEachLineCheck.setDisable(!custom || !useBlockCommentRadio.isSelected());

        beforeOtherCommentsRadio.setDisable(!custom);
        afterOtherCommentsRadio.setDisable(!custom);

        if (locationInFileBox != null) {
            // Location in File remains enabled when "Use default settings" is selected, matching Image 1!
            locationInFileBox.setDisable(isNoCopyright);
            if (isJavaLanguage(language)) {
                beforePackageRadio.setDisable(isNoCopyright);
                beforeImportsRadio.setDisable(isNoCopyright);
                beforeClassRadio.setDisable(isNoCopyright);
            } else {
                beforeDoctypeRadio.setDisable(isNoCopyright);
                beforeRootTagRadio.setDisable(isNoCopyright);
            }
        }

        separatorBeforeCheck.setDisable(!custom);
        separatorBeforeLengthField.setDisable(!custom || !separatorBeforeCheck.isSelected());
        separatorAfterCheck.setDisable(!custom);
        separatorAfterLengthField.setDisable(!custom || !separatorAfterCheck.isSelected());
        separatorCharField.setDisable(!custom);
        boxCheck.setDisable(!custom);
        blankLineBeforeCheck.setDisable(!custom);
        blankLineAfterCheck.setDisable(!custom);

        if (!custom) {
            // Populate controls with default values for visual reflection
            CopyrightFormattingOptions def = CopyrightManager.getInstance().getDefaultFormatting();
            isUpdating = true;
            if (def.getCommentType() == CopyrightFormattingOptions.CommentType.BLOCK) {
                useBlockCommentRadio.setSelected(true);
            } else {
                useLineCommentRadio.setSelected(true);
            }
            prefixEachLineCheck.setSelected(def.isPrefixEachLine());
            if (def.getRelativeLocation() == CopyrightFormattingOptions.RelativeLocation.BEFORE_OTHER_COMMENTS) {
                beforeOtherCommentsRadio.setSelected(true);
            } else {
                afterOtherCommentsRadio.setSelected(true);
            }
            separatorBeforeCheck.setSelected(def.isSeparatorBefore());
            separatorBeforeLengthField.setText(String.valueOf(def.getSeparatorBeforeLength()));
            separatorAfterCheck.setSelected(def.isSeparatorAfter());
            separatorAfterLengthField.setText(String.valueOf(def.getSeparatorAfterLength()));
            separatorCharField.setText(def.getSeparatorChar());
            boxCheck.setSelected(def.isBox());
            blankLineBeforeCheck.setSelected(def.isBlankLineBefore());
            blankLineAfterCheck.setSelected(def.isBlankLineAfter());
            isUpdating = false;
        }
    }

    private void updatePreview() {
        if (noCopyrightRadio.isSelected()) {
            previewArea.setText("// No copyright formatted for " + language);
            return;
        }

        CopyrightFormattingOptions opts = useCustomRadio.isSelected()
                ? workingOverride.getCustomOptions()
                : CopyrightManager.getInstance().getDefaultFormatting();

        String blockStart = "/*";
        String blockEnd = " */";
        String linePrefix = " * ";
        String lineComment = "// ";

        if ("DTD".equalsIgnoreCase(language)) {
            blockStart = "<!--";
            blockEnd = " -->";
            linePrefix = " - ";
            lineComment = "<!-- ";
        } else if (isXmlOrHtmlLanguage(language)) {
            blockStart = "<!--";
            blockEnd = "-->";
            linePrefix = " ~ ";
            lineComment = "<!-- ";
        } else if ("Shell Script".equalsIgnoreCase(language) || "Properties".equalsIgnoreCase(language)) {
            blockStart = "##";
            blockEnd = "##";
            linePrefix = "# ";
            lineComment = "# ";
        } else if ("SQL".equalsIgnoreCase(language)) {
            blockStart = "/*";
            blockEnd = " */";
            linePrefix = " * ";
            lineComment = "-- ";
        }

        String formatted = opts.formatPreview(getNoticeLinesForPreview(), blockStart, blockEnd, linePrefix, lineComment);
        previewArea.setText(formatted);
    }

    private String[] getNoticeLinesForPreview() {
        String defaultProfile = CopyrightManager.getInstance().getDefaultProjectCopyright();
        if (defaultProfile != null && !defaultProfile.equalsIgnoreCase("No copyright")) {
            CopyrightProfile profile = CopyrightManager.getInstance().findProfileByName(defaultProfile);
            if (profile != null && profile.getNotice() != null && !profile.getNotice().trim().isEmpty()) {
                String evaluated = CopyrightManager.getInstance().evaluateNotice(profile.getName(), "Sample." + getFileExtension(language), "lumina");
                if (evaluated != null && !evaluated.trim().isEmpty()) {
                    return evaluated.split("\r?\n");
                }
            }
        }
        return PREVIEW_LINES;
    }

    private String getFileExtension(String lang) {
        if (lang == null) return "txt";
        return switch (lang.trim().toLowerCase()) {
            case "java" -> "java";
            case "kotlin" -> "kt";
            case "groovy" -> "groovy";
            case "javascript" -> "js";
            case "typescript" -> "ts";
            case "css" -> "css";
            case "less" -> "less";
            case "sass" -> "sass";
            case "scss" -> "scss";
            case "postcss" -> "pcss";
            case "html" -> "html";
            case "xhtml" -> "xhtml";
            case "xml" -> "xml";
            case "dtd" -> "dtd";
            case "svg" -> "svg";
            case "vue", "vue template" -> "vue";
            case "jsp" -> "jsp";
            case "jspx" -> "jspx";
            case "properties" -> "properties";
            case "rust" -> "rs";
            case "shell script" -> "sh";
            case "sql" -> "sql";
            default -> "txt";
        };
    }

    private boolean isJavaLanguage(String lang) {
        return lang != null && "Java".equalsIgnoreCase(lang.trim());
    }

    private boolean isXmlOrHtmlLanguage(String lang) {
        if (lang == null) return false;
        String l = lang.toLowerCase();
        return l.contains("html") || l.contains("xml") || l.contains("dtd") || l.contains("xhtml")
                || l.contains("svg") || l.contains("vue") || l.contains("jsp");
    }

    private Node createSectionHeader(String text) {
        HBox box = new HBox(8);
        box.setAlignment(Pos.CENTER_LEFT);
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: #868A91; -fx-font-size: 12px;");
        Region line = new Region();
        line.setPrefHeight(1);
        line.setMinHeight(1);
        line.setMaxHeight(1);
        line.setStyle("-fx-background-color: #393B40;");
        HBox.setHgrow(line, Priority.ALWAYS);
        box.getChildren().addAll(label, line);
        return box;
    }

    private static String radioStyle() {
        return "-fx-text-fill: #DFE1E5; -fx-font-size: 12px;";
    }

    private static String checkStyle() {
        return "-fx-text-fill: #DFE1E5; -fx-font-size: 12px;";
    }

    private static String inputStyle() {
        return "-fx-background-color: #1E1F22; -fx-text-fill: #DFE1E5; " +
                "-fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4; " +
                "-fx-padding: 3 6; -fx-font-size: 12px;";
    }

    private void loadDataFromManager() {
        CopyrightManager manager = CopyrightManager.getInstance();
        workingOverride = manager.getLanguageOverride(language).copy();
        originalOverride = workingOverride.copy();

        isUpdating = true;
        if (workingOverride.getMode() == LanguageFormattingOverride.Mode.NO_COPYRIGHT) {
            noCopyrightRadio.setSelected(true);
        } else if (workingOverride.getMode() == LanguageFormattingOverride.Mode.USE_CUSTOM) {
            useCustomRadio.setSelected(true);
        } else {
            useDefaultRadio.setSelected(true);
        }

        CopyrightFormattingOptions custom = workingOverride.getCustomOptions();
        if (custom.getCommentType() == CopyrightFormattingOptions.CommentType.BLOCK) {
            useBlockCommentRadio.setSelected(true);
        } else {
            useLineCommentRadio.setSelected(true);
        }
        prefixEachLineCheck.setSelected(custom.isPrefixEachLine());

        if (custom.getRelativeLocation() == CopyrightFormattingOptions.RelativeLocation.BEFORE_OTHER_COMMENTS) {
            beforeOtherCommentsRadio.setSelected(true);
        } else {
            afterOtherCommentsRadio.setSelected(true);
        }

        if (locationInFileBox != null) {
            CopyrightFormattingOptions.LocationInFile loc = custom.getLocationInFile();
            if (isJavaLanguage(language)) {
                if (loc == CopyrightFormattingOptions.LocationInFile.BEFORE_IMPORTS) {
                    beforeImportsRadio.setSelected(true);
                } else if (loc == CopyrightFormattingOptions.LocationInFile.BEFORE_CLASS) {
                    beforeClassRadio.setSelected(true);
                } else {
                    beforePackageRadio.setSelected(true);
                }
            } else {
                if (loc == CopyrightFormattingOptions.LocationInFile.BEFORE_ROOT_TAG) {
                    beforeRootTagRadio.setSelected(true);
                } else {
                    beforeDoctypeRadio.setSelected(true);
                }
            }
        }

        separatorBeforeCheck.setSelected(custom.isSeparatorBefore());
        separatorBeforeLengthField.setText(String.valueOf(custom.getSeparatorBeforeLength()));
        separatorAfterCheck.setSelected(custom.isSeparatorAfter());
        separatorAfterLengthField.setText(String.valueOf(custom.getSeparatorAfterLength()));
        separatorCharField.setText(custom.getSeparatorChar());
        boxCheck.setSelected(custom.isBox());
        blankLineBeforeCheck.setSelected(custom.isBlankLineBefore());
        blankLineAfterCheck.setSelected(custom.isBlankLineAfter());

        isUpdating = false;
        updateControlStates();
        updatePreview();
    }

    public boolean isModified() {
        return !workingOverride.isEquivalentTo(originalOverride);
    }

    public void apply() {
        CopyrightManager.getInstance().setLanguageOverride(language, workingOverride.copy());
        CopyrightManager.getInstance().save();
        originalOverride = workingOverride.copy();
        notifyModified();
    }

    public void reset() {
        loadDataFromManager();
        notifyModified();
    }

    public void setOnModifiedListener(Runnable listener) {
        this.onModifiedListener = listener;
    }

    public void notifyModified() {
        if (onModifiedListener != null) {
            onModifiedListener.run();
        }
    }

    public String getLanguage() {
        return language;
    }

    public LanguageFormattingOverride getWorkingOverride() {
        return workingOverride;
    }

    public RadioButton getNoCopyrightRadio() {
        return noCopyrightRadio;
    }

    public RadioButton getUseDefaultRadio() {
        return useDefaultRadio;
    }

    public RadioButton getUseCustomRadio() {
        return useCustomRadio;
    }

    public TextArea getPreviewArea() {
        return previewArea;
    }

    public TextArea getPreviewCodeArea() {
        return previewArea;
    }

    public HBox getOptionsBox() {
        return optionsBox;
    }

    public HBox getOptionsSection() {
        return optionsBox;
    }

    public RadioButton getBlockCommentRadio() {
        return useBlockCommentRadio;
    }

    public RadioButton getLineCommentRadio() {
        return useLineCommentRadio;
    }

    public CheckBox getPrefixEachLineCheck() {
        return prefixEachLineCheck;
    }

    public RadioButton getBeforeOtherCommentsRadio() {
        return beforeOtherCommentsRadio;
    }

    public RadioButton getAfterOtherCommentsRadio() {
        return afterOtherCommentsRadio;
    }

    public VBox getLocationInFileBox() {
        return locationInFileBox;
    }

    public VBox getLocationInFileGroup() {
        return locationInFileBox;
    }

    public RadioButton getBeforeDoctypeRadio() {
        return beforeDoctypeRadio;
    }

    public RadioButton getBeforeRootTagRadio() {
        return beforeRootTagRadio;
    }

    public CheckBox getSeparatorBeforeCheck() {
        return separatorBeforeCheck;
    }

    public TextField getSeparatorBeforeLengthField() {
        return separatorBeforeLengthField;
    }

    public CheckBox getSeparatorAfterCheck() {
        return separatorAfterCheck;
    }

    public TextField getSeparatorAfterLengthField() {
        return separatorAfterLengthField;
    }

    public TextField getSeparatorCharField() {
        return separatorCharField;
    }

    public CheckBox getBoxCheck() {
        return boxCheck;
    }

    public CheckBox getBlankLineBeforeCheck() {
        return blankLineBeforeCheck;
    }

    public CheckBox getBlankLineAfterCheck() {
        return blankLineAfterCheck;
    }

    public RadioButton getBeforePackageRadio() {
        return beforePackageRadio;
    }

    public RadioButton getBeforeImportsRadio() {
        return beforeImportsRadio;
    }

    public RadioButton getBeforeClassRadio() {
        return beforeClassRadio;
    }

    public VBox getCommentTypeBox() {
        return commentTypeBox;
    }

    public VBox getRelativeLocationBox() {
        return locationBox;
    }

    public VBox getBordersBox() {
        return col2;
    }
}
