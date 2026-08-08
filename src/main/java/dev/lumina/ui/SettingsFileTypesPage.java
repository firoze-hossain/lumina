// SettingsFileTypesPage.java
package dev.lumina.ui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * IntelliJ-style Editor > File Types settings page.
 * Complete implementation matching all screenshots.
 */
public class SettingsFileTypesPage extends VBox {

    private final ListView<String> fileTypeList = new ListView<>();
    private final ListView<String> patternList = new ListView<>();
    private final ListView<String> hashbangList = new ListView<>();
    private final ListView<String> ignoredList = new ListView<>();
    private final ToggleButton recognizedBtn = new ToggleButton("Recognized File Types");
    private final ToggleButton ignoredBtn = new ToggleButton("Ignored Files and Folders");
    private final ToggleGroup viewGroup = new ToggleGroup();
    private final StackPane contentPane = new StackPane();

    // Sample data
    private final ObservableList<String> fileTypes = FXCollections.observableArrayList(
            "GitLab CI Expression language",
            ".aiignore (Ailgnore)",
            ".dockerignore (DockerIgnore)",
            ".gitignore (Gitignore)",
            ".hignore (Hglignore)",
            ".ignore (IgnoreLang)",
            "Angular HTML Template",
            "Angular HTML Template (17+)",
            "Angular HTML Template (18.1+)",
            "Angular HTML Template (20+)",
            "Angular SVG Template",
            "Angular SVG Template (17+)",
            "Angular SVG Template (18.1+)",
            "Angular SVG Template (20+)",
            "Archive",
            "AspectJ (syntax highlighting only)",
            "C#",
            "C/C++",
            "Cascading style sheet"
    );

    private final ObservableList<String> ignoredPatterns = FXCollections.observableArrayList(
            "*.pyc", "*.pyo", "*.rbc", "*.yarb", "~", ".DS_Store",
            ".git", ".hg", ".mypy_cache", ".pytest_cache", ".ruff_cache",
            ".svn", "CVS", "__pycache__", ".svn", "vssver.scc", "vssver2.scc"
    );

    public SettingsFileTypesPage() {
        getStyleClass().add("settings-page");
        setPadding(new Insets(12, 20, 20, 20));
        setSpacing(14);

        // ============================================================
        // View toggle buttons
        // ============================================================
        HBox toggleRow = new HBox(0);
        toggleRow.setAlignment(Pos.CENTER_LEFT);

        recognizedBtn.setToggleGroup(viewGroup);
        ignoredBtn.setToggleGroup(viewGroup);
        recognizedBtn.getStyleClass().addAll("segment", "segment-first");
        ignoredBtn.getStyleClass().addAll("segment", "segment-last");
        recognizedBtn.setSelected(true);

        recognizedBtn.setOnAction(e -> showRecognizedView());
        ignoredBtn.setOnAction(e -> showIgnoredView());

        toggleRow.getChildren().addAll(recognizedBtn, ignoredBtn);

        // ============================================================
        // Content pane (switches between views)
        // ============================================================
        contentPane.setPadding(new Insets(8, 0, 0, 0));

        // Build both views
        VBox recognizedView = buildRecognizedView();
        VBox ignoredView = buildIgnoredView();

        contentPane.getChildren().addAll(recognizedView, ignoredView);
        showRecognizedView();

        getChildren().addAll(toggleRow, contentPane);
    }

    private VBox buildRecognizedView() {
        VBox view = new VBox(10);
        view.setPadding(new Insets(0, 0, 0, 0));

        // ---- Main layout: File Types | Patterns ----
        HBox mainLayout = new HBox(16);

        // File Types list (left)
        VBox fileTypeBox = new VBox(6);
        fileTypeBox.setPrefWidth(280);
        fileTypeBox.setMinWidth(240);

        Label fileTypeLabel = new Label("Recognized File Types:");
        fileTypeLabel.getStyleClass().add("settings-label");

        fileTypeList.getStyleClass().add("settings-list");
        fileTypeList.setPrefHeight(280);
        fileTypeList.setItems(fileTypes);
        fileTypeList.getSelectionModel().select("GitLab CI Expression language");

        fileTypeList.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            if (selected != null) {
                updatePatterns(selected);
            }
        });

        // Buttons for file types
        HBox fileTypeButtons = new HBox(6);
        fileTypeButtons.setAlignment(Pos.CENTER_LEFT);
        Button addFileTypeBtn = new Button("+");
        addFileTypeBtn.getStyleClass().add("property-button");
        addFileTypeBtn.setOnAction(e -> showNewFileTypeDialog());
        Button removeFileTypeBtn = new Button("-");
        removeFileTypeBtn.getStyleClass().add("property-button");
        removeFileTypeBtn.setOnAction(e -> {
            String selected = fileTypeList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                fileTypes.remove(selected);
            }
        });

        fileTypeButtons.getChildren().addAll(addFileTypeBtn, removeFileTypeBtn);

        fileTypeBox.getChildren().addAll(fileTypeLabel, fileTypeList, fileTypeButtons);

        // Patterns (right)
        VBox patternBox = new VBox(6);
        patternBox.setPrefWidth(350);
        patternBox.setMinWidth(300);
        HBox.setHgrow(patternBox, Priority.ALWAYS);

        Label patternLabel = new Label("File name patterns:");
        patternLabel.getStyleClass().add("settings-label");

        patternList.getStyleClass().add("settings-list");
        patternList.setPrefHeight(120);
        patternList.getItems().add(".gitlabciexpression");

        // Buttons for patterns
        HBox patternButtons = new HBox(6);
        patternButtons.setAlignment(Pos.CENTER_LEFT);
        Button addPatternBtn = new Button("+");
        addPatternBtn.getStyleClass().add("property-button");
        addPatternBtn.setOnAction(e -> showAddPatternDialog());
        Button removePatternBtn = new Button("-");
        removePatternBtn.getStyleClass().add("property-button");
        removePatternBtn.setOnAction(e -> {
            String selected = patternList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                patternList.getItems().remove(selected);
            }
        });

        patternButtons.getChildren().addAll(addPatternBtn, removePatternBtn);

        // HashBang patterns
        Label hashbangLabel = new Label("HashBang patterns:");
        hashbangLabel.getStyleClass().add("settings-label");
        hashbangLabel.setPadding(new Insets(8, 0, 2, 0));

        hashbangList.getStyleClass().add("settings-list");
        hashbangList.setPrefHeight(80);
        hashbangList.getItems().add("No registered file patterns");

        patternBox.getChildren().addAll(
                patternLabel, patternList, patternButtons,
                hashbangLabel, hashbangList
        );

        mainLayout.getChildren().addAll(fileTypeBox, patternBox);

        // Update initial patterns
        updatePatterns("GitLab CI Expression language");

        view.getChildren().add(mainLayout);
        return view;
    }

    private VBox buildIgnoredView() {
        VBox view = new VBox(10);
        view.setPadding(new Insets(0, 0, 0, 0));

        Label headerLabel = new Label("Ignored Files and Folders");
        headerLabel.getStyleClass().add("settings-section");

        ignoredList.getStyleClass().add("settings-list");
        ignoredList.setPrefHeight(280);
        ignoredList.setItems(ignoredPatterns);

        // Buttons for ignored patterns
        HBox ignoredButtons = new HBox(6);
        ignoredButtons.setAlignment(Pos.CENTER_LEFT);
        Button addIgnoredBtn = new Button("+");
        addIgnoredBtn.getStyleClass().add("property-button");
        addIgnoredBtn.setOnAction(e -> showAddIgnoredPatternDialog());
        Button removeIgnoredBtn = new Button("-");
        removeIgnoredBtn.getStyleClass().add("property-button");
        removeIgnoredBtn.setOnAction(e -> {
            String selected = ignoredList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                ignoredPatterns.remove(selected);
            }
        });

        ignoredButtons.getChildren().addAll(addIgnoredBtn, removeIgnoredBtn);

        view.getChildren().addAll(headerLabel, ignoredList, ignoredButtons);
        return view;
    }

    private void showRecognizedView() {
        for (javafx.scene.Node node : contentPane.getChildren()) {
            VBox view = (VBox) node;
            view.setVisible(view == contentPane.getChildren().get(0));
            view.setManaged(view == contentPane.getChildren().get(0));
        }
    }

    private void showIgnoredView() {
        for (javafx.scene.Node node : contentPane.getChildren()) {
            VBox view = (VBox) node;
            view.setVisible(view == contentPane.getChildren().get(1));
            view.setManaged(view == contentPane.getChildren().get(1));
        }
    }

    private void updatePatterns(String fileType) {
        patternList.getItems().clear();
        hashbangList.getItems().clear();

        if (fileType.contains("GitLab")) {
            patternList.getItems().add(".gitlabciexpression");
            hashbangList.getItems().add("No registered file patterns");
        } else if (fileType.contains("gitignore")) {
            patternList.getItems().add(".gitignore");
            hashbangList.getItems().add("No registered file patterns");
        } else if (fileType.contains("Angular")) {
            patternList.getItems().addAll("*.html", "*.svg");
            hashbangList.getItems().add("No registered file patterns");
        } else if (fileType.contains("Cascading")) {
            patternList.getItems().add("*.css");
            hashbangList.getItems().add("No registered file patterns");
        } else if (fileType.contains("C#")) {
            patternList.getItems().add("*.cs");
            hashbangList.getItems().add("No registered file patterns");
        } else if (fileType.contains("C/C++")) {
            patternList.getItems().addAll("*.c", "*.cpp", "*.h", "*.hpp");
            hashbangList.getItems().add("No registered file patterns");
        } else if (fileType.contains("Archive")) {
            patternList.getItems().addAll("*.zip", "*.jar", "*.war", "*.ear");
            hashbangList.getItems().add("No registered file patterns");
        } else {
            patternList.getItems().add("*.pattern");
            hashbangList.getItems().add("No registered file patterns");
        }
    }

    private void showNewFileTypeDialog() {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("New File Type");

        VBox content = new VBox(14);
        content.setPadding(new Insets(20));

        // Name
        HBox nameRow = new HBox(8);
        nameRow.setAlignment(Pos.CENTER_LEFT);
        Label nameLabel = new Label("Name:");
        nameLabel.getStyleClass().add("settings-label");
        TextField nameField = new TextField();
        nameField.getStyleClass().add("text-field");
        nameField.setPrefWidth(250);
        nameRow.getChildren().addAll(nameLabel, nameField);

        // Description
        HBox descRow = new HBox(8);
        descRow.setAlignment(Pos.CENTER_LEFT);
        Label descLabel = new Label("Description:");
        descLabel.getStyleClass().add("settings-label");
        TextField descField = new TextField();
        descField.getStyleClass().add("text-field");
        descField.setPrefWidth(250);
        descRow.getChildren().addAll(descLabel, descField);

        // Syntax Highlighting section
        Label syntaxLabel = new Label("Syntax Highlighting");
        syntaxLabel.getStyleClass().add("settings-section");

        // Line comment
        CheckBox lineCommentCheck = new CheckBox("Line comment:");
        lineCommentCheck.getStyleClass().add("settings-check");
        TextField lineCommentField = new TextField("//");
        lineCommentField.getStyleClass().add("text-field");
        lineCommentField.setPrefWidth(80);
        CheckBox onlyAtLineStart = new CheckBox("Only at line start");
        onlyAtLineStart.getStyleClass().add("settings-check");
        HBox lineCommentRow = new HBox(8, lineCommentCheck, lineCommentField, onlyAtLineStart);
        lineCommentRow.setAlignment(Pos.CENTER_LEFT);

        // Block comment
        CheckBox blockCommentCheck = new CheckBox("Block comment start:");
        blockCommentCheck.getStyleClass().add("settings-check");
        TextField blockStartField = new TextField("/*");
        blockStartField.getStyleClass().add("text-field");
        blockStartField.setPrefWidth(60);
        Label blockEndLabel = new Label("Block comment end:");
        blockEndLabel.getStyleClass().add("settings-label");
        TextField blockEndField = new TextField("*/");
        blockEndField.getStyleClass().add("text-field");
        blockEndField.setPrefWidth(60);
        HBox blockCommentRow = new HBox(8, blockCommentCheck, blockStartField, blockEndLabel, blockEndField);
        blockCommentRow.setAlignment(Pos.CENTER_LEFT);

        // Hex prefix
        CheckBox hexPrefixCheck = new CheckBox("Hex prefix:");
        hexPrefixCheck.getStyleClass().add("settings-check");
        TextField hexPrefixField = new TextField("0x");
        hexPrefixField.getStyleClass().add("text-field");
        hexPrefixField.setPrefWidth(60);
        Label numberPostfixLabel = new Label("Number postfixes:");
        numberPostfixLabel.getStyleClass().add("settings-label");
        TextField numberPostfixField = new TextField("l, f, d, L, F, D");
        numberPostfixField.getStyleClass().add("text-field");
        numberPostfixField.setPrefWidth(150);
        HBox hexRow = new HBox(8, hexPrefixCheck, hexPrefixField, numberPostfixLabel, numberPostfixField);
        hexRow.setAlignment(Pos.CENTER_LEFT);

        // Support checkboxes
        CheckBox supportBraces = new CheckBox("Support paired braces");
        supportBraces.setSelected(true);
        supportBraces.getStyleClass().add("settings-check");
        CheckBox supportBrackets = new CheckBox("Support paired brackets");
        supportBrackets.setSelected(true);
        supportBrackets.getStyleClass().add("settings-check");
        CheckBox supportParens = new CheckBox("Support paired parens");
        supportParens.setSelected(true);
        supportParens.getStyleClass().add("settings-check");
        CheckBox supportStringEscapes = new CheckBox("Support string escapes");
        supportStringEscapes.setSelected(true);
        supportStringEscapes.getStyleClass().add("settings-check");

        HBox supportRow = new HBox(16, supportBraces, supportBrackets, supportParens, supportStringEscapes);
        supportRow.setAlignment(Pos.CENTER_LEFT);
        supportRow.setPadding(new Insets(4, 0, 4, 0));

        // Keywords - using TextArea instead of TextField for multi-line
        Label keywordLabel = new Label("Keywords:");
        keywordLabel.getStyleClass().add("settings-label");
        TextArea keywordField = new TextArea("abstract, assert, break, case, catch, class, const, continue, default, do, else, enum, extends, final, finally, for, if, implements, import, instanceof, interface, native, new, package, private, protected, public, return, static, strictfp, super, switch, synchronized, this, throw, throws, transient, try, void, volatile, while");
        keywordField.getStyleClass().add("text-field");
        keywordField.setPrefWidth(400);
        keywordField.setPrefHeight(60);
        keywordField.setWrapText(true);
        keywordField.setStyle("-fx-background-color: #1F2230; -fx-text-fill: #D8DBE6; -fx-border-color: #2C3042; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 6 10 6 10;");

        // Ignore case
        CheckBox ignoreCase = new CheckBox("Ignore case");
        ignoreCase.getStyleClass().add("settings-check");

        // Buttons
        HBox buttons = new HBox(10);
        buttons.setAlignment(Pos.CENTER_RIGHT);
        Button okBtn = new Button("OK");
        okBtn.getStyleClass().add("dialog-primary");
        okBtn.setOnAction(e -> {
            String name = nameField.getText().trim();
            if (!name.isEmpty()) {
                fileTypes.add(name);
                fileTypeList.getSelectionModel().select(name);
                dialog.close();
            }
        });
        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().add("dialog-secondary");
        cancelBtn.setOnAction(e -> dialog.close());

        buttons.getChildren().addAll(okBtn, cancelBtn);

        content.getChildren().addAll(
                nameRow, descRow,
                syntaxLabel,
                lineCommentRow,
                blockCommentRow,
                hexRow,
                supportRow,
                keywordLabel, keywordField,
                ignoreCase,
                buttons
        );

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setPrefViewportHeight(450);
        scroll.getStyleClass().add("settings-scroll");

        Scene scene = new Scene(scroll, 620, 520);
        scene.getStylesheets().add(
                getClass().getResource("/css/lumina-dark.css").toExternalForm());
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    private void showAddPatternDialog() {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Add Wildcard");

        VBox content = new VBox(12);
        content.setPadding(new Insets(20));

        Label header = new Label("Enter new wildcard ('*' and '?' allowed):");
        header.getStyleClass().add("settings-label");

        TextField patternField = new TextField();
        patternField.getStyleClass().add("text-field");
        patternField.setPrefWidth(300);

        HBox buttons = new HBox(10);
        buttons.setAlignment(Pos.CENTER_RIGHT);
        Button okBtn = new Button("OK");
        okBtn.getStyleClass().add("dialog-primary");
        okBtn.setOnAction(e -> {
            String pattern = patternField.getText().trim();
            if (!pattern.isEmpty()) {
                patternList.getItems().add(pattern);
                dialog.close();
            }
        });
        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().add("dialog-secondary");
        cancelBtn.setOnAction(e -> dialog.close());

        buttons.getChildren().addAll(okBtn, cancelBtn);

        content.getChildren().addAll(header, patternField, buttons);

        Scene scene = new Scene(content, 360, 150);
        scene.getStylesheets().add(
                getClass().getResource("/css/lumina-dark.css").toExternalForm());
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    private void showAddIgnoredPatternDialog() {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Add Ignored Pattern");

        VBox content = new VBox(12);
        content.setPadding(new Insets(20));

        Label header = new Label("Enter pattern to ignore:");
        header.getStyleClass().add("settings-label");

        TextField patternField = new TextField();
        patternField.getStyleClass().add("text-field");
        patternField.setPrefWidth(300);

        HBox buttons = new HBox(10);
        buttons.setAlignment(Pos.CENTER_RIGHT);
        Button okBtn = new Button("OK");
        okBtn.getStyleClass().add("dialog-primary");
        okBtn.setOnAction(e -> {
            String pattern = patternField.getText().trim();
            if (!pattern.isEmpty()) {
                ignoredPatterns.add(pattern);
                dialog.close();
            }
        });
        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().add("dialog-secondary");
        cancelBtn.setOnAction(e -> dialog.close());

        buttons.getChildren().addAll(okBtn, cancelBtn);

        content.getChildren().addAll(header, patternField, buttons);

        Scene scene = new Scene(content, 360, 150);
        scene.getStylesheets().add(
                getClass().getResource("/css/lumina-dark.css").toExternalForm());
        dialog.setScene(scene);
        dialog.showAndWait();
    }
}