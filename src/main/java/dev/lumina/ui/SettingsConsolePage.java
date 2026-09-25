package dev.lumina.ui;

import dev.lumina.settings.ConsoleSettings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Pixel-perfect IntelliJ IDEA-style Editor > General > Console settings page.
 * Completely dynamic, backed by ConsoleSettings, supporting dirty tracking,
 * pattern addition/removal/editing with custom dialogs, and real-time persistence.
 */
public class SettingsConsolePage extends VBox {

    private final CheckBox useSoftWrapsCheck = new CheckBox("Use soft wraps in console");
    private final TextField historySizeField = new TextField("300");

    private final CheckBox overrideCycleBufferCheck = new CheckBox("Override console cycle buffer size (1024 KB)");
    private final TextField cycleBufferSizeField = new TextField("1024");
    private final Label kbLabel = new Label("KB");

    private final ComboBox<String> defaultEncodingCombo = new ComboBox<>();

    // Folding patterns
    private final ObservableList<String> foldingPatternsData = FXCollections.observableArrayList();
    private final ListView<String> foldingPatternsList = new ListView<>(foldingPatternsData);
    private final Button addFoldBtn = new Button("+");
    private final Button removeFoldBtn = new Button("\u2014"); // Em dash '—'
    private final Button editFoldBtn = new Button("\u270E"); // Pencil '✎'

    // Exception patterns
    private final ObservableList<String> exceptionPatternsData = FXCollections.observableArrayList();
    private final ListView<String> exceptionPatternsList = new ListView<>(exceptionPatternsData);
    private final Button addExcBtn = new Button("+");
    private final Button removeExcBtn = new Button("\u2014");
    private final Button editExcBtn = new Button("\u270E");

    // Java Stack Trace
    private final CheckBox foldStackTraceCheck = new CheckBox("Fold stack trace longer than");
    private final TextField stackTraceLinesField = new TextField("8");
    private final Label linesLabel = new Label("lines");

    private Runnable onModifiedListener;
    private boolean suppressEvents = false;

    public SettingsConsolePage() {
        getStyleClass().add("settings-page");
        setStyle("-fx-background-color: #1E1F22;");
        setPadding(new Insets(14, 24, 28, 24));
        setSpacing(10);

        buildUi();
        setupListeners();
        loadFromSettings(ConsoleSettings.getInstance());
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

    private void styleTextField(TextField tf, double width) {
        tf.setPrefWidth(width);
        tf.setPrefHeight(25);
        tf.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4; -fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-padding: 3 6 3 6;");
    }

    private Node buildSectionSeparator(String title) {
        HBox box = new HBox(10);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(14, 0, 4, 0));

        Label label = new Label(title);
        label.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold;");

        Region line = new Region();
        line.setStyle("-fx-background-color: #393B40; -fx-min-height: 1px; -fx-pref-height: 1px; -fx-max-height: 1px;");
        HBox.setHgrow(line, Priority.ALWAYS);

        box.getChildren().addAll(label, line);
        return box;
    }

    private void buildUi() {
        // 1. Use soft wraps
        styleCheckBox(useSoftWrapsCheck);

        // 2. History size row
        Label historyLabel = new Label("Console commands history size:");
        historyLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        styleTextField(historySizeField, 60);

        HBox historyRow = new HBox(8, historyLabel, historySizeField);
        historyRow.setAlignment(Pos.CENTER_LEFT);

        // 3. Cycle buffer size row
        styleCheckBox(overrideCycleBufferCheck);
        styleTextField(cycleBufferSizeField, 65);
        kbLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");

        HBox bufferRow = new HBox(8, overrideCycleBufferCheck, cycleBufferSizeField, kbLabel);
        bufferRow.setAlignment(Pos.CENTER_LEFT);

        overrideCycleBufferCheck.selectedProperty().addListener((obs, o, n) -> cycleBufferSizeField.setDisable(!n));

        // 4. Default Encoding row
        Label encodingLabel = new Label("Default Encoding:");
        encodingLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");

        populateEncodings();
        defaultEncodingCombo.setPrefWidth(220);
        defaultEncodingCombo.setPrefHeight(25);
        defaultEncodingCombo.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4; -fx-text-fill: #DFE1E5; -fx-font-size: 12px;");

        HBox encodingRow = new HBox(8, encodingLabel, defaultEncodingCombo);
        encodingRow.setAlignment(Pos.CENTER_LEFT);

        // 5. Fold console lines that contain
        Label foldTitle = new Label("Fold console lines that contain:");
        foldTitle.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");

        VBox foldBox = buildTableBox(foldingPatternsList, addFoldBtn, removeFoldBtn, editFoldBtn, 160, true);

        // 6. Exceptions
        Label excTitle = new Label("Exceptions:");
        excTitle.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");

        VBox excBox = buildTableBox(exceptionPatternsList, addExcBtn, removeExcBtn, editExcBtn, 100, false);

        // 7. Java Stack Trace
        Node stackTraceSeparator = buildSectionSeparator("Java Stack Trace");

        styleCheckBox(foldStackTraceCheck);
        styleTextField(stackTraceLinesField, 55);
        linesLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");

        HBox stackTraceRow = new HBox(8, foldStackTraceCheck, stackTraceLinesField, linesLabel);
        stackTraceRow.setAlignment(Pos.CENTER_LEFT);
        foldStackTraceCheck.selectedProperty().addListener((obs, o, n) -> stackTraceLinesField.setDisable(!n));

        getChildren().addAll(
                useSoftWrapsCheck,
                historyRow,
                bufferRow,
                encodingRow,
                foldTitle,
                foldBox,
                excTitle,
                excBox,
                stackTraceSeparator,
                stackTraceRow
        );
    }

    private VBox buildTableBox(ListView<String> listView, Button addBtn, Button removeBtn, Button editBtn, double height, boolean isFoldPattern) {
        // Toolbar above the list table
        styleToolbarButton(addBtn);
        styleToolbarButton(removeBtn);
        styleToolbarButton(editBtn);

        removeBtn.setDisable(true);
        editBtn.setDisable(true);

        HBox toolbar = new HBox(4, addBtn, removeBtn, editBtn);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(3, 6, 3, 6));
        toolbar.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #393B40; -fx-border-width: 1 1 0 1; -fx-background-radius: 4 4 0 0; -fx-border-radius: 4 4 0 0;");

        // ListView setup
        listView.setPrefHeight(height);
        listView.setStyle("-fx-background-color: #1E1F22; -fx-border-color: #393B40; -fx-border-width: 1; -fx-background-radius: 0 0 4 4; -fx-border-radius: 0 0 4 4;");
        listView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    setText(item);
                    setTextFill(Color.web("#DFE1E5"));
                    setStyle(isSelected() ? "-fx-background-color: #2E436E; -fx-text-fill: #FFFFFF;" : "-fx-background-color: transparent; -fx-text-fill: #DFE1E5;");
                }
            }
        });

        listView.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            boolean hasSelection = n != null;
            removeBtn.setDisable(!hasSelection);
            editBtn.setDisable(!hasSelection);
        });

        listView.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                String sel = listView.getSelectionModel().getSelectedItem();
                if (sel != null) {
                    editItem(listView, sel, isFoldPattern);
                }
            }
        });

        addBtn.setOnAction(e -> addItem(listView, isFoldPattern));
        removeBtn.setOnAction(e -> {
            int idx = listView.getSelectionModel().getSelectedIndex();
            if (idx >= 0) {
                listView.getItems().remove(idx);
                notifyModified();
            }
        });
        editBtn.setOnAction(e -> {
            String sel = listView.getSelectionModel().getSelectedItem();
            if (sel != null) {
                editItem(listView, sel, isFoldPattern);
            }
        });

        VBox container = new VBox(0, toolbar, listView);
        return container;
    }

    private void styleToolbarButton(Button btn) {
        btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-padding: 2 7 2 7; -fx-cursor: hand;");
        btn.setOnMouseEntered(e -> {
            if (!btn.isDisable()) btn.setStyle("-fx-background-color: #393B40; -fx-text-fill: #FFFFFF; -fx-font-size: 13px; -fx-padding: 2 7 2 7; -fx-background-radius: 3; -fx-cursor: hand;");
        });
        btn.setOnMouseExited(e -> {
            if (!btn.isDisable()) btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-padding: 2 7 2 7; -fx-cursor: hand;");
        });
        btn.disabledProperty().addListener((obs, o, n) -> {
            if (n) {
                btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #55575E; -fx-font-size: 13px; -fx-padding: 2 7 2 7;");
            } else {
                btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-padding: 2 7 2 7; -fx-cursor: hand;");
            }
        });
    }

    private void addItem(ListView<String> listView, boolean isFoldPattern) {
        String prompt = isFoldPattern
                ? "Enter a substring of a console line you'd like to see folded:"
                : "Enter a substring of a console line you don't want to fold:";

        showPatternDialog("Folding Pattern", prompt, "").ifPresent(pattern -> {
            String trimmed = pattern.trim();
            if (!trimmed.isEmpty() && !listView.getItems().contains(trimmed)) {
                listView.getItems().add(trimmed);
                listView.getSelectionModel().select(trimmed);
                notifyModified();
            }
        });
    }

    private void editItem(ListView<String> listView, String currentVal, boolean isFoldPattern) {
        String prompt = isFoldPattern
                ? "Enter a substring of a console line you'd like to see folded:"
                : "Enter a substring of a console line you don't want to fold:";

        showPatternDialog("Folding Pattern", prompt, currentVal).ifPresent(pattern -> {
            String trimmed = pattern.trim();
            if (!trimmed.isEmpty()) {
                int idx = listView.getItems().indexOf(currentVal);
                if (idx >= 0) {
                    listView.getItems().set(idx, trimmed);
                    listView.getSelectionModel().select(trimmed);
                    notifyModified();
                }
            }
        });
    }

    /**
     * Renders a pixel-perfect modal dialog matching images 3 and 4:
     * Dark IntelliJ theme, blue circle with '?' icon, prompt label, input field, and OK/Cancel buttons.
     */
    private Optional<String> showPatternDialog(String title, String promptText, String initialText) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initStyle(StageStyle.UTILITY);
        dialog.setTitle(title);

        VBox root = new VBox(12);
        root.setStyle("-fx-background-color: #1E1F22; -fx-border-color: #4E5157; -fx-border-width: 1;");
        root.setPadding(new Insets(16, 18, 14, 18));
        root.setPrefWidth(420);

        // Question mark icon in blue circle
        StackPane iconPane = new StackPane();
        Circle circle = new Circle(10, Color.web("#3574F0"));
        Label qLabel = new Label("?");
        qLabel.setStyle("-fx-text-fill: #FFFFFF; -fx-font-weight: bold; -fx-font-size: 12px;");
        iconPane.getChildren().addAll(circle, qLabel);

        Label prompt = new Label(promptText);
        prompt.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        prompt.setWrapText(true);

        HBox header = new HBox(10, iconPane, prompt);
        header.setAlignment(Pos.CENTER_LEFT);

        TextField input = new TextField(initialText);
        input.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #3574F0; -fx-border-radius: 4; -fx-background-radius: 4; -fx-text-fill: #FFFFFF; -fx-font-size: 12px; -fx-padding: 5 8 5 8;");
        HBox.setHgrow(input, Priority.ALWAYS);

        Button okBtn = new Button("OK");
        okBtn.setDefaultButton(true);
        okBtn.setStyle("-fx-background-color: #3574F0; -fx-text-fill: #FFFFFF; -fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 5 18 5 18; -fx-background-radius: 4; -fx-cursor: hand;");

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setCancelButton(true);
        cancelBtn.setStyle("-fx-background-color: #393B40; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4; -fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-padding: 5 16 5 16; -fx-cursor: hand;");

        final String[] result = {null};

        okBtn.setOnAction(e -> {
            result[0] = input.getText();
            dialog.close();
        });
        cancelBtn.setOnAction(e -> dialog.close());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox buttons = new HBox(8, spacer, okBtn, cancelBtn);
        buttons.setAlignment(Pos.CENTER_RIGHT);
        buttons.setPadding(new Insets(6, 0, 0, 0));

        root.getChildren().addAll(header, input, buttons);

        Scene scene = new Scene(root);
        dialog.setScene(scene);
        dialog.showAndWait();

        return Optional.ofNullable(result[0]);
    }

    private void populateEncodings() {
        defaultEncodingCombo.getItems().clear();

        defaultEncodingCombo.getItems().add("<System Default: UTF-8>");
        defaultEncodingCombo.getItems().add("ISO-8859-1");
        defaultEncodingCombo.getItems().add("UTF-8");
        defaultEncodingCombo.getItems().add("UTF-16");
        defaultEncodingCombo.getItems().add("US-ASCII");
        defaultEncodingCombo.getItems().add("Big5");
        defaultEncodingCombo.getItems().add("Big5-HKSCS");
        defaultEncodingCombo.getItems().add("CESU-8");
        defaultEncodingCombo.getItems().add("EUC-JP");
        defaultEncodingCombo.getItems().add("EUC-KR");
        defaultEncodingCombo.getItems().add("GB18030");
        defaultEncodingCombo.getItems().add("GB2312");
        defaultEncodingCombo.getItems().add("GBK");
        defaultEncodingCombo.getItems().add("Shift_JIS");
        defaultEncodingCombo.getItems().add("windows-1250");
        defaultEncodingCombo.getItems().add("windows-1251");
        defaultEncodingCombo.getItems().add("windows-1252");

        defaultEncodingCombo.setValue("<System Default: UTF-8>");
    }

    private void setupListeners() {
        useSoftWrapsCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        historySizeField.textProperty().addListener((obs, o, n) -> notifyModified());
        overrideCycleBufferCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        cycleBufferSizeField.textProperty().addListener((obs, o, n) -> notifyModified());
        defaultEncodingCombo.valueProperty().addListener((obs, o, n) -> notifyModified());

        foldingPatternsData.addListener((javafx.collections.ListChangeListener<String>) c -> notifyModified());
        exceptionPatternsData.addListener((javafx.collections.ListChangeListener<String>) c -> notifyModified());

        foldStackTraceCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        stackTraceLinesField.textProperty().addListener((obs, o, n) -> notifyModified());
    }

    public void loadFromSettings(ConsoleSettings s) {
        suppressEvents = true;
        try {
            useSoftWrapsCheck.setSelected(s.isUseSoftWraps());
            historySizeField.setText(String.valueOf(s.getHistorySize()));

            overrideCycleBufferCheck.setSelected(s.isOverrideCycleBufferSize());
            cycleBufferSizeField.setText(String.valueOf(s.getCycleBufferSizeKb()));
            cycleBufferSizeField.setDisable(!s.isOverrideCycleBufferSize());

            defaultEncodingCombo.setValue(s.getDefaultEncoding());

            foldingPatternsData.setAll(s.getFoldingPatterns());
            exceptionPatternsData.setAll(s.getExceptionPatterns());

            foldStackTraceCheck.setSelected(s.isFoldStackTraceLongerThan());
            stackTraceLinesField.setText(String.valueOf(s.getStackTraceLinesLimit()));
            stackTraceLinesField.setDisable(!s.isFoldStackTraceLongerThan());
        } finally {
            suppressEvents = false;
        }
    }

    public void saveToSettings(ConsoleSettings s) {
        s.setUseSoftWraps(useSoftWrapsCheck.isSelected());
        try {
            s.setHistorySize(Integer.parseInt(historySizeField.getText().trim()));
        } catch (NumberFormatException ignored) {}

        s.setOverrideCycleBufferSize(overrideCycleBufferCheck.isSelected());
        try {
            s.setCycleBufferSizeKb(Integer.parseInt(cycleBufferSizeField.getText().trim()));
        } catch (NumberFormatException ignored) {}

        if (defaultEncodingCombo.getValue() != null) {
            s.setDefaultEncoding(defaultEncodingCombo.getValue());
        }

        s.setFoldingPatterns(new ArrayList<>(foldingPatternsData));
        s.setExceptionPatterns(new ArrayList<>(exceptionPatternsData));

        s.setFoldStackTraceLongerThan(foldStackTraceCheck.isSelected());
        try {
            s.setStackTraceLinesLimit(Integer.parseInt(stackTraceLinesField.getText().trim()));
        } catch (NumberFormatException ignored) {}
    }

    public boolean isModified() {
        ConsoleSettings current = new ConsoleSettings();
        saveToSettings(current);
        return current.isModified(ConsoleSettings.getInstance());
    }

    public void apply() {
        saveToSettings(ConsoleSettings.getInstance());
        ConsoleSettings.getInstance().save();
    }

    public void reset() {
        loadFromSettings(ConsoleSettings.getInstance());
    }
}