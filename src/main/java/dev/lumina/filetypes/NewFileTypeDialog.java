package dev.lumina.filetypes;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.util.Optional;

/**
 * Dialog for creating or editing a custom File Type with syntax highlighting rules,
 * precisely matching IntelliJ IDEA's "New File Type" dialog (media_1791255950965.png).
 */
public class NewFileTypeDialog {

    private final Stage stage;
    private final TextField nameField = new TextField();
    private final TextField descField = new TextField();

    // Syntax highlighting controls
    private final TextField lineCommentField = new TextField("//");
    private final CheckBox lineCommentAtStartCheck = new CheckBox("at start of line only");
    private final TextField blockCommentStartField = new TextField("/*");
    private final TextField blockCommentEndField = new TextField("*/");
    private final TextField hexPrefixField = new TextField("0x");
    private final TextField numberPostfixesField = new TextField();

    private final CheckBox pairedBracesCheck = new CheckBox("Support paired braces");
    private final CheckBox pairedBracketsCheck = new CheckBox("Support paired brackets");
    private final CheckBox pairedParensCheck = new CheckBox("Support paired parens");
    private final CheckBox pairedStringEscapesCheck = new CheckBox("Support paired string escapes");

    private final ToggleButton group1Btn = new ToggleButton("1");
    private final ToggleButton group2Btn = new ToggleButton("2");
    private final ToggleButton group3Btn = new ToggleButton("3");
    private final ToggleButton group4Btn = new ToggleButton("4");
    private final ToggleGroup groupToggle = new ToggleGroup();
    private final CheckBox ignoreCaseCheck = new CheckBox("Ignore case");
    private final TextArea keywordsArea = new TextArea();

    // 4 keyword buffers
    private String kw1 = "";
    private String kw2 = "";
    private String kw3 = "";
    private String kw4 = "";
    private int currentGroup = 1;

    private FileType result = null;

    public NewFileTypeDialog(Window owner, FileType existing) {
        stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        if (owner != null) {
            stage.initOwner(owner);
        }
        stage.setTitle(existing == null ? "New File Type" : "Edit File Type");
        stage.setResizable(true);

        VBox root = new VBox(14);
        root.setPadding(new Insets(18, 20, 18, 20));
        root.setStyle("-fx-background-color: #2B2D30;");
        root.setPrefWidth(680);
        root.setPrefHeight(600);

        // ---- Top: Name & Description ----
        GridPane topGrid = new GridPane();
        topGrid.setHgap(12);
        topGrid.setVgap(10);

        Label nameLabel = new Label("Name:");
        nameLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        nameField.setStyle(inputStyle());
        GridPane.setHgrow(nameField, Priority.ALWAYS);

        Label descLabel = new Label("Description:");
        descLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        descField.setStyle(inputStyle());
        GridPane.setHgrow(descField, Priority.ALWAYS);

        topGrid.add(nameLabel, 0, 0);
        topGrid.add(nameField, 1, 0);
        topGrid.add(descLabel, 0, 1);
        topGrid.add(descField, 1, 1);

        // ---- Syntax Highlighting Box ----
        VBox syntaxSection = new VBox(12);
        syntaxSection.setStyle("-fx-border-color: #393B40; -fx-border-radius: 6; -fx-padding: 14; -fx-background-color: #2B2D30;");
        VBox.setVgrow(syntaxSection, Priority.ALWAYS);

        Label syntaxTitle = new Label("Syntax Highlighting");
        syntaxTitle.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold;");

        HBox columns = new HBox(20);
        VBox.setVgrow(columns, Priority.ALWAYS);

        // Left Column (Delimiters & Brackets)
        VBox leftCol = new VBox(10);
        leftCol.setPrefWidth(280);

        GridPane delimsGrid = new GridPane();
        delimsGrid.setHgap(8);
        delimsGrid.setVgap(8);

        Label lineCommentLbl = new Label("Line comment:");
        lineCommentLbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        lineCommentField.setStyle(inputStyle());
        lineCommentField.setPrefWidth(80);

        lineCommentAtStartCheck.setStyle(checkStyle());

        Label blockStartLbl = new Label("Block comment start:");
        blockStartLbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        blockCommentStartField.setStyle(inputStyle());
        blockCommentStartField.setPrefWidth(80);

        Label blockEndLbl = new Label("Block comment end:");
        blockEndLbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        blockCommentEndField.setStyle(inputStyle());
        blockCommentEndField.setPrefWidth(80);

        Label hexPrefixLbl = new Label("Hex prefix:");
        hexPrefixLbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        hexPrefixField.setStyle(inputStyle());
        hexPrefixField.setPrefWidth(80);

        Label numPostfixLbl = new Label("Number postfixes:");
        numPostfixLbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        numberPostfixesField.setStyle(inputStyle());
        numberPostfixesField.setPrefWidth(80);

        delimsGrid.add(lineCommentLbl, 0, 0);
        delimsGrid.add(lineCommentField, 1, 0);
        delimsGrid.add(lineCommentAtStartCheck, 0, 1, 2, 1);
        delimsGrid.add(blockStartLbl, 0, 2);
        delimsGrid.add(blockCommentStartField, 1, 2);
        delimsGrid.add(blockEndLbl, 0, 3);
        delimsGrid.add(blockCommentEndField, 1, 3);
        delimsGrid.add(hexPrefixLbl, 0, 4);
        delimsGrid.add(hexPrefixField, 1, 4);
        delimsGrid.add(numPostfixLbl, 0, 5);
        delimsGrid.add(numberPostfixesField, 1, 5);

        pairedBracesCheck.setStyle(checkStyle());
        pairedBracketsCheck.setStyle(checkStyle());
        pairedParensCheck.setStyle(checkStyle());
        pairedStringEscapesCheck.setStyle(checkStyle());
        pairedBracesCheck.setSelected(true);
        pairedBracketsCheck.setSelected(true);
        pairedParensCheck.setSelected(true);
        pairedStringEscapesCheck.setSelected(true);

        VBox bracketsBox = new VBox(6, pairedBracesCheck, pairedBracketsCheck, pairedParensCheck, pairedStringEscapesCheck);
        bracketsBox.setPadding(new Insets(6, 0, 0, 0));

        leftCol.getChildren().addAll(delimsGrid, bracketsBox);

        // Right Column (Keywords 4 groups)
        VBox rightCol = new VBox(8);
        HBox.setHgrow(rightCol, Priority.ALWAYS);

        HBox kwTopBar = new HBox(8);
        kwTopBar.setAlignment(Pos.CENTER_LEFT);

        group1Btn.setToggleGroup(groupToggle);
        group2Btn.setToggleGroup(groupToggle);
        group3Btn.setToggleGroup(groupToggle);
        group4Btn.setToggleGroup(groupToggle);
        styleTabButton(group1Btn);
        styleTabButton(group2Btn);
        styleTabButton(group3Btn);
        styleTabButton(group4Btn);
        group1Btn.setSelected(true);

        HBox tabGroup = new HBox(2, group1Btn, group2Btn, group3Btn, group4Btn);

        ignoreCaseCheck.setStyle(checkStyle());
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        kwTopBar.getChildren().addAll(tabGroup, spacer, ignoreCaseCheck);

        keywordsArea.setStyle(
                "-fx-control-inner-background: #1E1F22; -fx-background-color: #1E1F22; " +
                "-fx-text-fill: #DFE1E5; -fx-font-family: 'JetBrains Mono', Consolas, monospace; " +
                "-fx-font-size: 12px; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4;"
        );
        keywordsArea.setPromptText("Enter keywords separated by spaces or newlines...");
        VBox.setVgrow(keywordsArea, Priority.ALWAYS);

        // Group switching logic
        group1Btn.setOnAction(e -> switchGroup(1));
        group2Btn.setOnAction(e -> switchGroup(2));
        group3Btn.setOnAction(e -> switchGroup(3));
        group4Btn.setOnAction(e -> switchGroup(4));

        rightCol.getChildren().addAll(kwTopBar, keywordsArea);

        columns.getChildren().addAll(leftCol, rightCol);
        syntaxSection.getChildren().addAll(syntaxTitle, columns);

        // ---- Bottom: OK & Cancel ----
        HBox buttonBar = new HBox(8);
        buttonBar.setAlignment(Pos.CENTER_RIGHT);

        Button okBtn = new Button("OK");
        okBtn.setDefaultButton(true);
        okBtn.setStyle(
                "-fx-background-color: #3574F0; -fx-text-fill: white; -fx-font-size: 12px; " +
                "-fx-font-weight: bold; -fx-padding: 5 18; -fx-background-radius: 4; -fx-cursor: hand;"
        );

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setCancelButton(true);
        cancelBtn.setStyle(
                "-fx-background-color: #393B40; -fx-text-fill: #DFE1E5; -fx-font-size: 12px; " +
                "-fx-padding: 5 16; -fx-background-radius: 4; -fx-cursor: hand;"
        );

        okBtn.setOnAction(e -> {
            saveCurrentGroupBuffer();
            String name = nameField.getText().trim();
            if (name.isEmpty()) {
                nameField.setStyle(inputStyle() + " -fx-border-color: #E5534B;");
                return;
            }

            FileType ft = existing != null ? existing.copy() : new FileType();
            ft.setName(name);
            ft.setDescription(descField.getText().trim());
            ft.setBuiltin(false);
            ft.setIconKind("generic");

            ft.setLineComment(lineCommentField.getText());
            ft.setLineCommentAtStartOnly(lineCommentAtStartCheck.isSelected());
            ft.setBlockCommentStart(blockCommentStartField.getText());
            ft.setBlockCommentEnd(blockCommentEndField.getText());
            ft.setHexPrefix(hexPrefixField.getText());
            ft.setNumberPostfixes(numberPostfixesField.getText());

            ft.setSupportPairedBraces(pairedBracesCheck.isSelected());
            ft.setSupportPairedBrackets(pairedBracketsCheck.isSelected());
            ft.setSupportPairedParens(pairedParensCheck.isSelected());
            ft.setSupportPairedStringEscapes(pairedStringEscapesCheck.isSelected());

            ft.setIgnoreCase(ignoreCaseCheck.isSelected());
            ft.setKeywordsGroup1(kw1);
            ft.setKeywordsGroup2(kw2);
            ft.setKeywordsGroup3(kw3);
            ft.setKeywordsGroup4(kw4);

            result = ft;
            stage.close();
        });

        cancelBtn.setOnAction(e -> stage.close());

        buttonBar.getChildren().addAll(okBtn, cancelBtn);

        root.getChildren().addAll(topGrid, syntaxSection, buttonBar);

        // Populate existing values if editing
        if (existing != null) {
            nameField.setText(existing.getName());
            descField.setText(existing.getDescription());
            lineCommentField.setText(existing.getLineComment());
            lineCommentAtStartCheck.setSelected(existing.isLineCommentAtStartOnly());
            blockCommentStartField.setText(existing.getBlockCommentStart());
            blockCommentEndField.setText(existing.getBlockCommentEnd());
            hexPrefixField.setText(existing.getHexPrefix());
            numberPostfixesField.setText(existing.getNumberPostfixes());

            pairedBracesCheck.setSelected(existing.isSupportPairedBraces());
            pairedBracketsCheck.setSelected(existing.isSupportPairedBrackets());
            pairedParensCheck.setSelected(existing.isSupportPairedParens());
            pairedStringEscapesCheck.setSelected(existing.isSupportPairedStringEscapes());

            ignoreCaseCheck.setSelected(existing.isIgnoreCase());
            kw1 = existing.getKeywordsGroup1();
            kw2 = existing.getKeywordsGroup2();
            kw3 = existing.getKeywordsGroup3();
            kw4 = existing.getKeywordsGroup4();
            keywordsArea.setText(kw1);
        }

        Scene scene = new Scene(root);
        stage.setScene(scene);
    }

    private void switchGroup(int targetGroup) {
        saveCurrentGroupBuffer();
        currentGroup = targetGroup;
        switch (targetGroup) {
            case 1: keywordsArea.setText(kw1); break;
            case 2: keywordsArea.setText(kw2); break;
            case 3: keywordsArea.setText(kw3); break;
            case 4: keywordsArea.setText(kw4); break;
        }
    }

    private void saveCurrentGroupBuffer() {
        String currentText = keywordsArea.getText();
        switch (currentGroup) {
            case 1: kw1 = currentText; break;
            case 2: kw2 = currentText; break;
            case 3: kw3 = currentText; break;
            case 4: kw4 = currentText; break;
        }
    }

    private void styleTabButton(ToggleButton btn) {
        btn.setStyle(
                "-fx-background-color: #393B40; -fx-text-fill: #DFE1E5; " +
                "-fx-font-size: 11px; -fx-padding: 3 10; -fx-background-radius: 4; -fx-cursor: hand;"
        );
        btn.selectedProperty().addListener((obs, old, isSel) -> {
            if (isSel) {
                btn.setStyle(
                        "-fx-background-color: #3574F0; -fx-text-fill: white; " +
                        "-fx-font-size: 11px; -fx-padding: 3 10; -fx-background-radius: 4; -fx-cursor: hand; -fx-font-weight: bold;"
                );
            } else {
                btn.setStyle(
                        "-fx-background-color: #393B40; -fx-text-fill: #DFE1E5; " +
                        "-fx-font-size: 11px; -fx-padding: 3 10; -fx-background-radius: 4; -fx-cursor: hand;"
                );
            }
        });
    }

    private static String inputStyle() {
        return "-fx-background-color: #1E1F22; -fx-text-fill: #DFE1E5; " +
                "-fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4; " +
                "-fx-padding: 4 8; -fx-font-size: 12px;";
    }

    private static String checkStyle() {
        return "-fx-text-fill: #DFE1E5; -fx-font-size: 12px;";
    }

    public static Optional<FileType> show(Window owner, FileType existing) {
        NewFileTypeDialog dialog = new NewFileTypeDialog(owner, existing);
        dialog.stage.showAndWait();
        return Optional.ofNullable(dialog.result);
    }
}
