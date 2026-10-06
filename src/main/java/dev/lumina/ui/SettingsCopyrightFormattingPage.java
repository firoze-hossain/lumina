// SettingsCopyrightFormattingPage.java
package dev.lumina.ui;

import dev.lumina.copyright.CopyrightFormattingOptions;
import dev.lumina.copyright.CopyrightManager;
import dev.lumina.copyright.CopyrightProfile;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/**
 * Settings page for Editor > Copyright > Formatting (default formatting),
 * faithfully matching IntelliJ IDEA's design (screenshot media_1791264903448.png).
 */
public class SettingsCopyrightFormattingPage extends VBox {

    private static final String[] PREVIEW_LINES = {
            "Copyright (c) 2026. Lorem ipsum dolor sit amet, consectetur adipiscing elit.",
            "Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.",
            "Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.",
            "Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.",
            "Vestibulum commodo. Ut rhoncus gravida arcu."
    };

    // Comment Type controls
    private final RadioButton useBlockCommentRadio = new RadioButton("Use block comment");
    private final CheckBox prefixEachLineCheck = new CheckBox("Prefix each line");
    private final RadioButton useLineCommentRadio = new RadioButton("Use line comment");
    private final ToggleGroup commentTypeGroup = new ToggleGroup();

    // Relative Location controls
    private final RadioButton beforeOtherCommentsRadio = new RadioButton("Before other comments");
    private final RadioButton afterOtherCommentsRadio = new RadioButton("After other comments");
    private final ToggleGroup relativeLocationGroup = new ToggleGroup();

    // Borders controls
    private final CheckBox separatorBeforeCheck = new CheckBox("Separator before");
    private final TextField separatorBeforeLengthField = new TextField("80");
    private final CheckBox separatorAfterCheck = new CheckBox("Separator after");
    private final TextField separatorAfterLengthField = new TextField("80");
    private final TextField separatorCharField = new TextField();
    private final CheckBox boxCheck = new CheckBox("Box");
    private final CheckBox blankLineBeforeCheck = new CheckBox("Add blank line before");
    private final CheckBox blankLineAfterCheck = new CheckBox("Add blank line after");

    // Code Preview
    private final TextArea previewArea = new TextArea();

    // State
    private CopyrightFormattingOptions workingOptions = new CopyrightFormattingOptions();
    private CopyrightFormattingOptions originalOptions = new CopyrightFormattingOptions();
    private boolean isUpdating = false;
    private Runnable onModifiedListener;

    public SettingsCopyrightFormattingPage() {
        getStyleClass().add("settings-page");
        setPadding(new Insets(16, 20, 16, 20));
        setSpacing(14);
        setStyle("-fx-background-color: #1E1F22;");
        VBox.setVgrow(this, Priority.ALWAYS);

        initLayout();
        loadDataFromManager();
    }

    private void initLayout() {
        HBox topColumns = new HBox(40);
        topColumns.setAlignment(Pos.TOP_LEFT);

        // ---- Column 1: Comment Type & Relative Location ----
        VBox col1 = new VBox(16);
        col1.setPrefWidth(320);

        // Group 1: Comment Type
        VBox commentTypeBox = new VBox(8);
        Node commentTypeHeader = createSectionHeader("Comment Type");

        useBlockCommentRadio.setToggleGroup(commentTypeGroup);
        useLineCommentRadio.setToggleGroup(commentTypeGroup);
        useBlockCommentRadio.setStyle(radioStyle());
        useLineCommentRadio.setStyle(radioStyle());
        prefixEachLineCheck.setStyle(checkStyle());
        VBox.setMargin(prefixEachLineCheck, new Insets(0, 0, 0, 22));

        commentTypeGroup.selectedToggleProperty().addListener((obs, old, isSel) -> {
            if (!isUpdating) {
                workingOptions.setCommentType(
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
            if (!isUpdating) {
                workingOptions.setPrefixEachLine(isSel);
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
        VBox locationBox = new VBox(8);
        Node locationHeader = createSectionHeader("Relative Location");

        beforeOtherCommentsRadio.setToggleGroup(relativeLocationGroup);
        afterOtherCommentsRadio.setToggleGroup(relativeLocationGroup);
        beforeOtherCommentsRadio.setStyle(radioStyle());
        afterOtherCommentsRadio.setStyle(radioStyle());

        relativeLocationGroup.selectedToggleProperty().addListener((obs, old, isSel) -> {
            if (!isUpdating) {
                workingOptions.setRelativeLocation(
                        beforeOtherCommentsRadio.isSelected()
                                ? CopyrightFormattingOptions.RelativeLocation.BEFORE_OTHER_COMMENTS
                                : CopyrightFormattingOptions.RelativeLocation.AFTER_OTHER_COMMENTS
                );
                notifyModified();
            }
        });

        locationBox.getChildren().addAll(locationHeader, beforeOtherCommentsRadio, afterOtherCommentsRadio);

        col1.getChildren().addAll(commentTypeBox, locationBox);

        // ---- Column 2: Borders ----
        VBox col2 = new VBox(8);
        col2.setPrefWidth(340);
        Node bordersHeader = createSectionHeader("Borders");

        // Separator before
        HBox sepBeforeRow = new HBox(10);
        sepBeforeRow.setAlignment(Pos.CENTER_LEFT);
        separatorBeforeCheck.setStyle(checkStyle());
        Label lenLabel1 = new Label("Length:");
        lenLabel1.setStyle("-fx-text-fill: #868A91; -fx-font-size: 12px;");
        separatorBeforeLengthField.setStyle(inputStyle());
        separatorBeforeLengthField.setPrefWidth(55);
        separatorBeforeLengthField.setDisable(true);
        sepBeforeRow.getChildren().addAll(separatorBeforeCheck, lenLabel1, separatorBeforeLengthField);

        separatorBeforeCheck.selectedProperty().addListener((obs, old, isSel) -> {
            if (!isUpdating) {
                workingOptions.setSeparatorBefore(isSel);
                separatorBeforeLengthField.setDisable(!isSel);
                updatePreview();
                notifyModified();
            }
        });
        separatorBeforeLengthField.textProperty().addListener((obs, old, val) -> {
            if (!isUpdating) {
                try {
                    workingOptions.setSeparatorBeforeLength(Integer.parseInt(val.trim()));
                    updatePreview();
                    notifyModified();
                } catch (NumberFormatException ignored) {}
            }
        });

        // Separator after
        HBox sepAfterRow = new HBox(10);
        sepAfterRow.setAlignment(Pos.CENTER_LEFT);
        separatorAfterCheck.setStyle(checkStyle());
        Label lenLabel2 = new Label("Length:");
        lenLabel2.setStyle("-fx-text-fill: #868A91; -fx-font-size: 12px;");
        separatorAfterLengthField.setStyle(inputStyle());
        separatorAfterLengthField.setPrefWidth(55);
        separatorAfterLengthField.setDisable(true);
        sepAfterRow.getChildren().addAll(separatorAfterCheck, lenLabel2, separatorAfterLengthField);

        separatorAfterCheck.selectedProperty().addListener((obs, old, isSel) -> {
            if (!isUpdating) {
                workingOptions.setSeparatorAfter(isSel);
                separatorAfterLengthField.setDisable(!isSel);
                updatePreview();
                notifyModified();
            }
        });
        separatorAfterLengthField.textProperty().addListener((obs, old, val) -> {
            if (!isUpdating) {
                try {
                    workingOptions.setSeparatorAfterLength(Integer.parseInt(val.trim()));
                    updatePreview();
                    notifyModified();
                } catch (NumberFormatException ignored) {}
            }
        });

        // Separator char
        HBox sepCharRow = new HBox(10);
        sepCharRow.setAlignment(Pos.CENTER_LEFT);
        Label sepCharLabel = new Label("Separator:");
        sepCharLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        separatorCharField.setStyle(inputStyle());
        separatorCharField.setPrefWidth(55);
        separatorCharField.textProperty().addListener((obs, old, val) -> {
            if (!isUpdating) {
                workingOptions.setSeparatorChar(val != null ? val : "");
                updatePreview();
                notifyModified();
            }
        });
        sepCharRow.getChildren().addAll(sepCharLabel, separatorCharField);

        boxCheck.setStyle(checkStyle());
        boxCheck.selectedProperty().addListener((obs, old, isSel) -> {
            if (!isUpdating) {
                workingOptions.setBox(isSel);
                updatePreview();
                notifyModified();
            }
        });

        blankLineBeforeCheck.setStyle(checkStyle());
        blankLineBeforeCheck.selectedProperty().addListener((obs, old, isSel) -> {
            if (!isUpdating) {
                workingOptions.setBlankLineBefore(isSel);
                updatePreview();
                notifyModified();
            }
        });

        blankLineAfterCheck.setStyle(checkStyle());
        blankLineAfterCheck.selectedProperty().addListener((obs, old, isSel) -> {
            if (!isUpdating) {
                workingOptions.setBlankLineAfter(isSel);
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

        topColumns.getChildren().addAll(col1, col2);

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

        getChildren().addAll(topColumns, previewArea);
    }

    private void updatePreview() {
        String formatted = workingOptions.formatPreview(getNoticeLinesForPreview(), "/*", " */", " * ", "// ");
        previewArea.setText(formatted);
    }

    private String[] getNoticeLinesForPreview() {
        String defaultProfile = CopyrightManager.getInstance().getDefaultProjectCopyright();
        if (defaultProfile != null && !defaultProfile.equalsIgnoreCase("No copyright")) {
            CopyrightProfile profile = CopyrightManager.getInstance().findProfileByName(defaultProfile);
            if (profile != null && profile.getNotice() != null && !profile.getNotice().trim().isEmpty()) {
                String evaluated = CopyrightManager.getInstance().evaluateNotice(profile.getName(), "Sample.java", "lumina");
                if (evaluated != null && !evaluated.trim().isEmpty()) {
                    return evaluated.split("\r?\n");
                }
            }
        }
        return PREVIEW_LINES;
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
        workingOptions = manager.getDefaultFormatting().copy();
        originalOptions = workingOptions.copy();

        isUpdating = true;
        if (workingOptions.getCommentType() == CopyrightFormattingOptions.CommentType.BLOCK) {
            useBlockCommentRadio.setSelected(true);
            prefixEachLineCheck.setDisable(false);
        } else {
            useLineCommentRadio.setSelected(true);
            prefixEachLineCheck.setDisable(true);
        }
        prefixEachLineCheck.setSelected(workingOptions.isPrefixEachLine());

        if (workingOptions.getRelativeLocation() == CopyrightFormattingOptions.RelativeLocation.BEFORE_OTHER_COMMENTS) {
            beforeOtherCommentsRadio.setSelected(true);
        } else {
            afterOtherCommentsRadio.setSelected(true);
        }

        separatorBeforeCheck.setSelected(workingOptions.isSeparatorBefore());
        separatorBeforeLengthField.setText(String.valueOf(workingOptions.getSeparatorBeforeLength()));
        separatorBeforeLengthField.setDisable(!workingOptions.isSeparatorBefore());

        separatorAfterCheck.setSelected(workingOptions.isSeparatorAfter());
        separatorAfterLengthField.setText(String.valueOf(workingOptions.getSeparatorAfterLength()));
        separatorAfterLengthField.setDisable(!workingOptions.isSeparatorAfter());

        separatorCharField.setText(workingOptions.getSeparatorChar());
        boxCheck.setSelected(workingOptions.isBox());
        blankLineBeforeCheck.setSelected(workingOptions.isBlankLineBefore());
        blankLineAfterCheck.setSelected(workingOptions.isBlankLineAfter());

        isUpdating = false;
        updatePreview();
    }

    public boolean isModified() {
        return !workingOptions.isEquivalentTo(originalOptions);
    }

    public void apply() {
        CopyrightManager.getInstance().setDefaultFormatting(workingOptions.copy());
        CopyrightManager.getInstance().save();
        originalOptions = workingOptions.copy();
        notifyModified();
    }

    public void reset() {
        loadDataFromManager();
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

    public CopyrightFormattingOptions getWorkingOptions() {
        return workingOptions;
    }

    public TextArea getPreviewArea() {
        return previewArea;
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
}