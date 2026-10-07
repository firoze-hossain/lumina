package dev.lumina.ui;

import dev.lumina.settings.EmmetSettings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;

import java.util.List;

/**
 * Settings page for Editor > Emmet > HTML.
 * Matches IntelliJ IDEA layout with XML/HTML Emmet options, BEM configuration, and default filters.
 */
public class SettingsEmmetHtmlPage extends VBox {

    private final EmmetSettings workingSettings;
    private Runnable onModifiedListener;

    // Top checkboxes
    private final CheckBox enableXmlHtmlEmmetCheck = new CheckBox("Enable XML/HTML Emmet");
    private final CheckBox enableAbbrevPreviewCheck = new CheckBox("Enable abbreviation preview");
    private final CheckBox enableAutoUrlCheck = new CheckBox("Enable automatic URL recognition while wrapping text with <a> tag");
    private final CheckBox addEditPointCheck = new CheckBox("Add edit point at the end of template");

    // BEM fields
    private final TextField elemSeparatorField = new TextField();
    private final TextField modSeparatorField = new TextField();
    private final TextField shortPrefixField = new TextField();

    // Filters
    private final CheckBox filterXslCheck = new CheckBox("XSL tuning");
    private final CheckBox filterCommentCheck = new CheckBox("Comment tags");
    private final CheckBox filterEscapeCheck = new CheckBox("Escape");
    private final CheckBox filterSingleLineCheck = new CheckBox("Single line");
    private final CheckBox filterBemCheck = new CheckBox("BEM");
    private final CheckBox filterTrimCheck = new CheckBox("Trim line markers");

    private final VBox dependentControlsBox = new VBox(16);

    public SettingsEmmetHtmlPage() {
        this.workingSettings = EmmetSettings.getInstance().copy();
        getStyleClass().add("settings-page");
        setPadding(new Insets(20, 24, 20, 24));
        setSpacing(16);
        setStyle("-fx-background-color: #1E1F22;");

        buildUI();
        loadData();
    }

    public void setOnModifiedListener(Runnable listener) {
        this.onModifiedListener = listener;
    }

    private void fireModified() {
        if (onModifiedListener != null) {
            onModifiedListener.run();
        }
    }

    private void buildUI() {
        // Top 4 options
        enableXmlHtmlEmmetCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        enableAbbrevPreviewCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        enableAutoUrlCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        addEditPointCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        enableXmlHtmlEmmetCheck.setOnAction(e -> {
            workingSettings.setEnableXmlHtmlEmmet(enableXmlHtmlEmmetCheck.isSelected());
            updateEnableState();
            fireModified();
        });

        enableAbbrevPreviewCheck.setOnAction(e -> {
            workingSettings.setEnableAbbreviationPreview(enableAbbrevPreviewCheck.isSelected());
            fireModified();
        });

        enableAutoUrlCheck.setOnAction(e -> {
            workingSettings.setEnableAutoUrlRecognition(enableAutoUrlCheck.isSelected());
            fireModified();
        });

        addEditPointCheck.setOnAction(e -> {
            workingSettings.setAddEditPointAtEndOfTemplate(addEditPointCheck.isSelected());
            fireModified();
        });

        VBox topSubBox = new VBox(8, enableAbbrevPreviewCheck, enableAutoUrlCheck, addEditPointCheck);

        // BEM Section
        Label bemHeader = new Label("BEM");
        bemHeader.setStyle("-fx-text-fill: #DFE1E5; -fx-font-weight: bold; -fx-font-size: 13px;");

        GridPane bemGrid = new GridPane();
        bemGrid.setHgap(12);
        bemGrid.setVgap(8);
        bemGrid.setPadding(new Insets(4, 0, 4, 16));

        Label elemSepLabel = new Label("Element separator in class names:");
        elemSepLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        styleSmallTextField(elemSeparatorField);
        elemSeparatorField.textProperty().addListener((obs, oldV, newV) -> {
            workingSettings.setBemElementSeparator(newV);
            fireModified();
        });

        Label modSepLabel = new Label("Modifier separator in class names:");
        modSepLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        styleSmallTextField(modSeparatorField);
        modSeparatorField.textProperty().addListener((obs, oldV, newV) -> {
            workingSettings.setBemModifierSeparator(newV);
            fireModified();
        });

        Label shortPrefixLabel = new Label("Short element prefix:");
        shortPrefixLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        styleSmallTextField(shortPrefixField);
        shortPrefixField.textProperty().addListener((obs, oldV, newV) -> {
            workingSettings.setBemShortElementPrefix(newV);
            fireModified();
        });

        bemGrid.add(elemSepLabel, 0, 0);
        bemGrid.add(elemSeparatorField, 1, 0);
        bemGrid.add(modSepLabel, 0, 1);
        bemGrid.add(modSeparatorField, 1, 1);
        bemGrid.add(shortPrefixLabel, 0, 2);
        bemGrid.add(shortPrefixField, 1, 2);

        VBox bemSection = new VBox(6, bemHeader, bemGrid);

        // Filters Section
        Label filtersHeader = new Label("Filters enabled by default");
        filtersHeader.setStyle("-fx-text-fill: #DFE1E5; -fx-font-weight: bold; -fx-font-size: 13px;");

        for (CheckBox cb : List.of(filterXslCheck, filterCommentCheck, filterEscapeCheck, filterSingleLineCheck, filterBemCheck, filterTrimCheck)) {
            cb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        }

        filterXslCheck.setOnAction(e -> {
            workingSettings.setFilterXslTuning(filterXslCheck.isSelected());
            fireModified();
        });
        filterCommentCheck.setOnAction(e -> {
            workingSettings.setFilterCommentTags(filterCommentCheck.isSelected());
            fireModified();
        });
        filterEscapeCheck.setOnAction(e -> {
            workingSettings.setFilterEscape(filterEscapeCheck.isSelected());
            fireModified();
        });
        filterSingleLineCheck.setOnAction(e -> {
            workingSettings.setFilterSingleLine(filterSingleLineCheck.isSelected());
            fireModified();
        });
        filterBemCheck.setOnAction(e -> {
            workingSettings.setFilterBem(filterBemCheck.isSelected());
            fireModified();
        });
        filterTrimCheck.setOnAction(e -> {
            workingSettings.setFilterTrimLineMarkers(filterTrimCheck.isSelected());
            fireModified();
        });

        VBox filtersBox = new VBox(6,
                filterXslCheck,
                filterCommentCheck,
                filterEscapeCheck,
                filterSingleLineCheck,
                filterBemCheck,
                filterTrimCheck
        );
        filtersBox.setPadding(new Insets(4, 0, 4, 16));

        VBox filtersSection = new VBox(6, filtersHeader, filtersBox);

        // Group dependent controls
        dependentControlsBox.getChildren().addAll(topSubBox, bemSection, filtersSection);

        getChildren().addAll(enableXmlHtmlEmmetCheck, dependentControlsBox);
    }

    private void styleSmallTextField(TextField tf) {
        tf.setPrefWidth(60);
        tf.setMaxWidth(60);
        tf.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #393B40; -fx-border-radius: 3; -fx-background-radius: 3; -fx-padding: 3 6; -fx-font-size: 12px;");
    }

    private void updateEnableState() {
        boolean enabled = enableXmlHtmlEmmetCheck.isSelected();
        dependentControlsBox.setDisable(!enabled);
    }

    private void loadData() {
        enableXmlHtmlEmmetCheck.setSelected(workingSettings.isEnableXmlHtmlEmmet());
        enableAbbrevPreviewCheck.setSelected(workingSettings.isEnableAbbreviationPreview());
        enableAutoUrlCheck.setSelected(workingSettings.isEnableAutoUrlRecognition());
        addEditPointCheck.setSelected(workingSettings.isAddEditPointAtEndOfTemplate());

        elemSeparatorField.setText(workingSettings.getBemElementSeparator());
        modSeparatorField.setText(workingSettings.getBemModifierSeparator());
        shortPrefixField.setText(workingSettings.getBemShortElementPrefix());

        filterXslCheck.setSelected(workingSettings.isFilterXslTuning());
        filterCommentCheck.setSelected(workingSettings.isFilterCommentTags());
        filterEscapeCheck.setSelected(workingSettings.isFilterEscape());
        filterSingleLineCheck.setSelected(workingSettings.isFilterSingleLine());
        filterBemCheck.setSelected(workingSettings.isFilterBem());
        filterTrimCheck.setSelected(workingSettings.isFilterTrimLineMarkers());

        updateEnableState();
    }

    public void apply() {
        EmmetSettings.getInstance().applyFrom(workingSettings);
        EmmetSettings.getInstance().save();
    }

    public void reset() {
        workingSettings.applyFrom(EmmetSettings.getInstance());
        loadData();
    }

    public boolean isModified() {
        return workingSettings.isModified(EmmetSettings.getInstance());
    }
}
