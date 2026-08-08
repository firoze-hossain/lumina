// SettingsCopyrightFormattingPage.java
package dev.lumina.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/**
 * IntelliJ-style Editor > Copyright > Formatting settings page.
 * Complete implementation matching the screenshot.
 */
public class SettingsCopyrightFormattingPage extends VBox {

    private final ListView<String> languageList = new ListView<>();
    private final VBox contentArea = new VBox(14);

    public SettingsCopyrightFormattingPage() {
        getStyleClass().add("settings-page");
        setPadding(new Insets(12, 20, 20, 20));
        setSpacing(14);

        // ============================================================
        // Main layout: Language list | Content
        // ============================================================
        HBox mainLayout = new HBox(16);
        mainLayout.setPadding(new Insets(8, 0, 0, 0));

        // ---- Language list (left) ----
        VBox listBox = new VBox(6);
        listBox.setPrefWidth(180);
        listBox.setMinWidth(160);

        Label listLabel = new Label("Languages:");
        listLabel.getStyleClass().add("settings-label");

        languageList.getStyleClass().add("settings-list");
        languageList.setPrefHeight(300);
        languageList.getItems().addAll(
            "CSS", "DTD", "Groovy", "HTML", "Java", "JavaScript", "JSP",
            "JSPX", "Kotlin", "Less", "PostCSS", "Properties", "Rust",
            "Sass", "SCSS", "Shell Script", "SPI", "SQL", "SVG",
            "TypeScript", "Vue template", "XHTML", "XML"
        );
        languageList.getSelectionModel().select("Java");

        languageList.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            if (selected != null) {
                updateContent(selected);
            }
        });

        listBox.getChildren().addAll(listLabel, languageList);

        // ---- Content area (right) ----
        contentArea.setPrefWidth(450);
        contentArea.setMinWidth(350);
        HBox.setHgrow(contentArea, Priority.ALWAYS);

        // Show initial content
        updateContent("Java");

        mainLayout.getChildren().addAll(listBox, contentArea);

        getChildren().addAll(mainLayout);
    }

    private void updateContent(String language) {
        contentArea.getChildren().clear();

        // ============================================================
        // Comment Type section
        // ============================================================
        Label commentTypeLabel = new Label("Comment Type");
        commentTypeLabel.getStyleClass().add("settings-section");

        RadioButton blockComment = new RadioButton("Use block comment");
        blockComment.setSelected(true);
        blockComment.getStyleClass().add("settings-radio");

        CheckBox prefixEachLine = new CheckBox("Prefix each line");
        prefixEachLine.setSelected(true);
        prefixEachLine.getStyleClass().add("settings-check");
        prefixEachLine.setPadding(new Insets(0, 0, 0, 20));

        RadioButton lineComment = new RadioButton("Use line comment");
        lineComment.getStyleClass().add("settings-radio");

        ToggleGroup commentGroup = new ToggleGroup();
        blockComment.setToggleGroup(commentGroup);
        lineComment.setToggleGroup(commentGroup);

        VBox commentBox = new VBox(4);
        commentBox.setPadding(new Insets(4, 0, 8, 20));
        commentBox.getChildren().addAll(blockComment, prefixEachLine, lineComment);

        // ============================================================
        // Relative Location section
        // ============================================================
        Label locationLabel = new Label("Relative Location");
        locationLabel.getStyleClass().add("settings-section");

        RadioButton beforeOther = new RadioButton("Before other comments");
        beforeOther.setSelected(true);
        beforeOther.getStyleClass().add("settings-radio");

        RadioButton afterOther = new RadioButton("After other comments");
        afterOther.getStyleClass().add("settings-radio");

        ToggleGroup locationGroup = new ToggleGroup();
        beforeOther.setToggleGroup(locationGroup);
        afterOther.setToggleGroup(locationGroup);

        VBox locationBox = new VBox(4);
        locationBox.setPadding(new Insets(4, 0, 8, 20));
        locationBox.getChildren().addAll(beforeOther, afterOther);

        // ============================================================
        // Borders section
        // ============================================================
        Label bordersLabel = new Label("Borders");
        bordersLabel.getStyleClass().add("settings-section");

        // Separator before
        HBox beforeRow = new HBox(8);
        beforeRow.setPadding(new Insets(4, 0, 4, 20));
        beforeRow.setAlignment(Pos.CENTER_LEFT);
        CheckBox separatorBefore = new CheckBox("Separator before");
        separatorBefore.setSelected(true);
        separatorBefore.getStyleClass().add("settings-check");
        Label lengthLabel1 = new Label("Length:");
        lengthLabel1.getStyleClass().add("settings-label");
        Spinner<Integer> lengthSpinner1 = new Spinner<>(10, 200, 80, 5);
        lengthSpinner1.setPrefWidth(70);
        lengthSpinner1.getStyleClass().add("settings-spinner");
        beforeRow.getChildren().addAll(separatorBefore, lengthLabel1, lengthSpinner1);

        // Separator after
        HBox afterRow = new HBox(8);
        afterRow.setPadding(new Insets(4, 0, 4, 20));
        afterRow.setAlignment(Pos.CENTER_LEFT);
        CheckBox separatorAfter = new CheckBox("Separator after");
        separatorAfter.setSelected(true);
        separatorAfter.getStyleClass().add("settings-check");
        Label lengthLabel2 = new Label("Length:");
        lengthLabel2.getStyleClass().add("settings-label");
        Spinner<Integer> lengthSpinner2 = new Spinner<>(10, 200, 80, 5);
        lengthSpinner2.setPrefWidth(70);
        lengthSpinner2.getStyleClass().add("settings-spinner");
        afterRow.getChildren().addAll(separatorAfter, lengthLabel2, lengthSpinner2);

        // Separator style
        HBox separatorRow = new HBox(12);
        separatorRow.setPadding(new Insets(4, 0, 4, 20));
        separatorRow.setAlignment(Pos.CENTER_LEFT);
        Label separatorLabel = new Label("Separator:");
        separatorLabel.getStyleClass().add("settings-label");
        ComboBox<String> separatorCombo = new ComboBox<>();
        separatorCombo.getItems().addAll("Box", "Line", "Dashed", "Dotted");
        separatorCombo.getSelectionModel().selectFirst();
        separatorCombo.getStyleClass().add("settings-combo");
        separatorCombo.setPrefWidth(120);
        separatorRow.getChildren().addAll(separatorLabel, separatorCombo);

        // Blank lines
        HBox blankRow = new HBox(12);
        blankRow.setPadding(new Insets(4, 0, 4, 20));
        blankRow.setAlignment(Pos.CENTER_LEFT);
        CheckBox blankBefore = new CheckBox("Add blank line before");
        blankBefore.setSelected(true);
        blankBefore.getStyleClass().add("settings-check");
        CheckBox blankAfter = new CheckBox("Add blank line after");
        blankAfter.setSelected(true);
        blankAfter.getStyleClass().add("settings-check");
        blankRow.getChildren().addAll(blankBefore, blankAfter);

        VBox bordersBox = new VBox(4);
        bordersBox.setPadding(new Insets(4, 0, 8, 0));
        bordersBox.getChildren().addAll(beforeRow, afterRow, separatorRow, blankRow);

        // ============================================================
        // Preview section
        // ============================================================
        Label previewLabel = new Label("Preview");
        previewLabel.getStyleClass().add("settings-section");

        TextArea previewArea = new TextArea(
            "* Copyright (c) 2026. Lorem ipsum dolor sit amet, consectetur adipiscing elit\n" +
            "* Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam\n" +
            "* Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.\n" +
            "* Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis\n" +
            "* Vestibulum commodo. Ut rhoncus gravida arcu."
        );
        previewArea.setEditable(false);
        previewArea.setWrapText(true);
        previewArea.setPrefHeight(120);
        previewArea.setStyle("-fx-background-color: #1F2230; -fx-border-color: #2C3042; -fx-border-radius: 6; -fx-background-radius: 6; -fx-text-fill: #7A7E85; -fx-font-family: 'JetBrains Mono'; -fx-font-size: 12px;");

        VBox previewBox = new VBox(4);
        previewBox.setPadding(new Insets(4, 0, 8, 20));
        previewBox.getChildren().add(previewArea);

        // ============================================================
        // Assemble
        // ============================================================
        contentArea.getChildren().addAll(
            commentTypeLabel, commentBox,
            locationLabel, locationBox,
            bordersLabel, bordersBox,
            previewLabel, previewBox
        );
    }
}