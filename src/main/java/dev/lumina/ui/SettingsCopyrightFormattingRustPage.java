// SettingsCopyrightFormattingRustPage.java
package dev.lumina.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public class SettingsCopyrightFormattingRustPage extends VBox {

    public SettingsCopyrightFormattingRustPage() {
        getStyleClass().add("settings-page");
        setPadding(new Insets(12, 16, 16, 16));
        setSpacing(12);

        ToggleGroup mainSettingGroup = new ToggleGroup();

        RadioButton noCopyright = new RadioButton("No copyright");
        noCopyright.setToggleGroup(mainSettingGroup);
        noCopyright.getStyleClass().add("settings-radio");

        RadioButton useDefault = new RadioButton("Use default settings");
        useDefault.setToggleGroup(mainSettingGroup);
        useDefault.setSelected(true);
        useDefault.getStyleClass().add("settings-radio");

        RadioButton useCustom = new RadioButton("Use custom formatting options");
        useCustom.setToggleGroup(mainSettingGroup);
        useCustom.getStyleClass().add("settings-radio");

        VBox topRadioBox = new VBox(8, noCopyright, useDefault, useCustom);

        // --- Comment Type ---
        VBox commentTypeContent = new VBox(6);
        commentTypeContent.setPadding(new Insets(6, 0, 0, 0));

        ToggleGroup commentTypeGroup = new ToggleGroup();
        RadioButton useBlockComment = new RadioButton("Use block comment");
        useBlockComment.setToggleGroup(commentTypeGroup);
        useBlockComment.setSelected(true);
        useBlockComment.getStyleClass().add("settings-radio");

        CheckBox prefixEachLine = new CheckBox("Prefix each line");
        prefixEachLine.setSelected(true);
        prefixEachLine.getStyleClass().add("settings-check");
        VBox.setMargin(prefixEachLine, new Insets(0, 0, 0, 20));

        RadioButton useLineComment = new RadioButton("Use line comment");
        useLineComment.setToggleGroup(commentTypeGroup);
        useLineComment.getStyleClass().add("settings-radio");

        commentTypeContent.getChildren().addAll(useBlockComment, prefixEachLine, useLineComment);
        VBox commentTypeGroupPane = createTitledSection("Comment Type", commentTypeContent);

        // --- Relative Location ---
        VBox relativeLocationContent = new VBox(6);
        relativeLocationContent.setPadding(new Insets(6, 0, 0, 0));

        ToggleGroup locationGroup = new ToggleGroup();
        RadioButton beforeOther = new RadioButton("Before other comments");
        beforeOther.setToggleGroup(locationGroup);
        beforeOther.setSelected(true);
        beforeOther.getStyleClass().add("settings-radio");

        RadioButton afterOther = new RadioButton("After other comments");
        afterOther.setToggleGroup(locationGroup);
        afterOther.getStyleClass().add("settings-radio");

        relativeLocationContent.getChildren().addAll(beforeOther, afterOther);
        VBox relativeLocationGroupPane = createTitledSection("Relative Location", relativeLocationContent);

        VBox leftColumn = new VBox(16, commentTypeGroupPane, relativeLocationGroupPane);

        // --- Borders ---
        VBox bordersContent = new VBox(8);
        bordersContent.setPadding(new Insets(6, 0, 0, 0));

        HBox sepBeforeRow = new HBox(8);
        sepBeforeRow.setAlignment(Pos.CENTER_LEFT);
        CheckBox sepBefore = new CheckBox("Separator before");
        sepBefore.getStyleClass().add("settings-check");
        Label lenLabel1 = new Label("Length:");
        TextField lenField1 = new TextField("80");
        lenField1.setPrefWidth(50);
        sepBeforeRow.getChildren().addAll(sepBefore, lenLabel1, lenField1);

        HBox sepAfterRow = new HBox(8);
        sepAfterRow.setAlignment(Pos.CENTER_LEFT);
        CheckBox sepAfter = new CheckBox("Separator after");
        sepAfter.getStyleClass().add("settings-check");
        Label lenLabel2 = new Label("Length:");
        TextField lenField2 = new TextField("80");
        lenField2.setPrefWidth(50);
        sepAfterRow.getChildren().addAll(sepAfter, lenLabel2, lenField2);

        HBox sepCharRow = new HBox(8);
        sepCharRow.setAlignment(Pos.CENTER_LEFT);
        Label sepLabel = new Label("Separator:");
        TextField sepField = new TextField("");
        sepField.setPrefWidth(40);
        sepCharRow.getChildren().addAll(sepLabel, sepField);

        CheckBox boxCheck = new CheckBox("Box");
        boxCheck.getStyleClass().add("settings-check");

        CheckBox addBlankBefore = new CheckBox("Add blank line before");
        addBlankBefore.getStyleClass().add("settings-check");

        CheckBox addBlankAfter = new CheckBox("Add blank line after");
        addBlankAfter.setSelected(true);
        addBlankAfter.getStyleClass().add("settings-check");

        bordersContent.getChildren().addAll(
                sepBeforeRow,
                sepAfterRow,
                sepCharRow,
                boxCheck,
                addBlankBefore,
                addBlankAfter
        );

        VBox bordersGroupPane = createTitledSection("Borders", bordersContent);

        GridPane grid = new GridPane();
        grid.setHgap(24);
        grid.setVgap(12);

        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(50);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPercentWidth(50);
        grid.getColumnConstraints().addAll(col1, col2);

        grid.add(leftColumn, 0, 0);
        grid.add(bordersGroupPane, 1, 0);

        // --- Preview Box ---
        TextArea previewArea = new TextArea(
                "/*\n" +
                " * Copyright (c) 2026. Lorem ipsum dolor sit amet, consectetur adipiscing elit.\n" +
                " * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.\n" +
                " * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.\n" +
                " * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.\n" +
                " * Vestibulum commodo. Ut rhoncus gravida arcu.\n" +
                " */"
        );
        previewArea.setEditable(false);
        previewArea.setWrapText(false);
        previewArea.setPrefRowCount(8);
        previewArea.setStyle(
                "-fx-control-inner-background: #1e1f22; " +
                "-fx-background-color: #1e1f22; " +
                "-fx-text-fill: #a9b7c6; " +
                "-fx-border-color: #323232; " +
                "-fx-border-radius: 4px; " +
                "-fx-font-family: 'JetBrains Mono', 'Monospaced', monospace; " +
                "-fx-font-size: 12px;"
        );

        VBox.setVgrow(previewArea, Priority.ALWAYS);
        getChildren().addAll(topRadioBox, grid, previewArea);
    }

    private VBox createTitledSection(String title, Region content) {
        Label label = new Label(title);
        label.setStyle("-fx-text-fill: #8C8C8C; -fx-font-size: 11px;");
        Separator separator = new Separator();
        HBox.setHgrow(separator, Priority.ALWAYS);
        HBox header = new HBox(6, label, separator);
        header.setAlignment(Pos.CENTER_LEFT);
        return new VBox(4, header, content);
    }
}