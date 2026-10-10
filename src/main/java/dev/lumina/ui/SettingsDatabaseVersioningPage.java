package dev.lumina.ui;

import dev.lumina.database.versioning.DatabaseVersioningSettings;
import dev.lumina.database.versioning.DatabaseVersioningSettingsManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Tools > Database Versioning settings page in Lumina IDE matching 1:1 design of the reference IDE.
 */
public class SettingsDatabaseVersioningPage extends VBox {

    public static final List<String> SUBPAGES = List.of(
            "Type Mappings",
            "Diff Changes",
            "Liquibase",
            "Hibernate Envers",
            "Flyway"
    );

    private final DatabaseVersioningSettingsManager manager;
    private DatabaseVersioningSettings initialSettings;
    private Runnable onModifiedListener;
    private boolean updating = false;
    private final Consumer<String> onNavigate;

    private ComboBox<String> physicalNamingStrategyCombo;
    private ToggleGroup sequenceNamingGroup;
    private RadioButton seqAutoDetectRadio;
    private RadioButton seqSingleRadio;
    private RadioButton seqPerEntityRadio;

    private TextField maxDbIdentifierLengthField;
    private CheckBox createIndexForAssociationFkCheck;

    private ToggleGroup pkConstraintGroup;
    private RadioButton pkUnnamedRadio;
    private RadioButton pkNamedRadio;
    private TextField pkPrefixField;
    private Label pkPatternLabel;
    private TextField pkSuffixField;

    public SettingsDatabaseVersioningPage() {
        this(null);
    }

    public SettingsDatabaseVersioningPage(Consumer<String> onNavigate) {
        this.manager = DatabaseVersioningSettingsManager.getInstance();
        this.onNavigate = onNavigate;
        buildUI();
        loadData();
    }

    private void buildUI() {
        setPadding(new Insets(20, 24, 20, 24));
        setSpacing(16);
        setStyle("-fx-background-color: #1E1F22;");

        // 1. Physical naming strategy
        Label physicalLabel = createFieldLabel("Physical naming strategy:");
        physicalLabel.setPrefWidth(200);
        physicalNamingStrategyCombo = new ComboBox<>();
        physicalNamingStrategyCombo.getItems().addAll(
                "Use persistent unit strategy",
                "Spring Boot physical naming strategy",
                "Default naming strategy"
        );
        physicalNamingStrategyCombo.setPrefWidth(260);
        styleComboBox(physicalNamingStrategyCombo);
        physicalNamingStrategyCombo.valueProperty().addListener((obs, o, n) -> {
            if (!updating) notifyModified();
        });
        HBox physicalRow = new HBox(12, physicalLabel, physicalNamingStrategyCombo);
        physicalRow.setAlignment(Pos.CENTER_LEFT);

        // 2. Sequence naming strategy
        Label seqLabel = createFieldLabel("Sequence naming strategy:");
        seqLabel.setPrefWidth(200);

        sequenceNamingGroup = new ToggleGroup();
        seqAutoDetectRadio = createRadioButton("Auto detect", sequenceNamingGroup);
        seqSingleRadio = createRadioButton("Single sequence", sequenceNamingGroup);
        seqPerEntityRadio = createRadioButton("Sequence per entity", sequenceNamingGroup);

        HBox seqRadioBox = new HBox(12, seqAutoDetectRadio, seqSingleRadio, seqPerEntityRadio);
        seqRadioBox.setAlignment(Pos.CENTER_LEFT);

        Label hibernateNote = new Label("For Hibernate 6+ only");
        hibernateNote.setStyle("-fx-text-fill: #8C8C8C; -fx-font-size: 11px;");

        VBox seqContentBox = new VBox(4, seqRadioBox, hibernateNote);
        HBox seqRow = new HBox(12, seqLabel, seqContentBox);
        seqRow.setAlignment(Pos.TOP_LEFT);

        sequenceNamingGroup.selectedToggleProperty().addListener((obs, o, n) -> {
            if (!updating) notifyModified();
        });

        // 3. Max DB identifier length
        Label maxLenLabel = createFieldLabel("Max DB identifier length:");
        maxLenLabel.setPrefWidth(200);
        maxDbIdentifierLengthField = createTextField(700);
        maxDbIdentifierLengthField.setTextFormatter(new TextFormatter<>(change -> {
            if (change.getText().matches("\\d*")) {
                return change;
            }
            return null;
        }));
        HBox maxLenRow = new HBox(12, maxLenLabel, maxDbIdentifierLengthField);
        maxLenRow.setAlignment(Pos.CENTER_LEFT);

        // 4. Create index for association foreign key constraint
        createIndexForAssociationFkCheck = createCheckBox("Create index for association foreign key constraint");

        // 5. Primary key constraint name
        Label pkLabel = createFieldLabel("Primary key constraint name:");
        pkLabel.setPrefWidth(200);

        pkConstraintGroup = new ToggleGroup();
        pkUnnamedRadio = createRadioButton("Unnamed", pkConstraintGroup);
        pkNamedRadio = createRadioButton("Named", pkConstraintGroup);

        pkPrefixField = createTextField(50);
        pkPatternLabel = new Label("_${TABLE_NAME}_");
        pkPatternLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        pkSuffixField = createTextField(50);

        HBox pkNamedBox = new HBox(6, pkNamedRadio, pkPrefixField, pkPatternLabel, pkSuffixField);
        pkNamedBox.setAlignment(Pos.CENTER_LEFT);

        HBox pkRow = new HBox(12, pkLabel, pkUnnamedRadio, pkNamedBox);
        pkRow.setAlignment(Pos.CENTER_LEFT);

        pkConstraintGroup.selectedToggleProperty().addListener((obs, o, n) -> {
            boolean named = pkNamedRadio.isSelected();
            pkPrefixField.setDisable(!named);
            pkPatternLabel.setDisable(!named);
            pkSuffixField.setDisable(!named);
            if (!updating) notifyModified();
        });

        // 6. Navigation links
        VBox linksBox = new VBox(6);
        linksBox.setPadding(new Insets(10, 0, 0, 0));
        for (String sub : SUBPAGES) {
            Hyperlink link = new Hyperlink(sub);
            link.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 13px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0;");
            link.setOnMouseEntered(e -> link.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 13px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0;"));
            link.setOnMouseExited(e -> link.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 13px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0;"));
            link.setOnAction(e -> {
                if (onNavigate != null) {
                    onNavigate.accept(sub);
                }
            });
            linksBox.getChildren().add(link);
        }

        getChildren().addAll(
                physicalRow,
                seqRow,
                maxLenRow,
                createIndexForAssociationFkCheck,
                pkRow,
                linksBox
        );
    }

    private Label createFieldLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        return l;
    }

    private CheckBox createCheckBox(String text) {
        CheckBox cb = new CheckBox(text);
        cb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        cb.selectedProperty().addListener((obs, o, n) -> {
            if (!updating) notifyModified();
        });
        return cb;
    }

    private RadioButton createRadioButton(String text, ToggleGroup group) {
        RadioButton rb = new RadioButton(text);
        rb.setToggleGroup(group);
        rb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        return rb;
    }

    private TextField createTextField(double width) {
        TextField tf = new TextField();
        tf.setPrefWidth(width);
        tf.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 3 7 3 7;");
        tf.textProperty().addListener((obs, o, n) -> {
            if (!updating) notifyModified();
        });
        return tf;
    }

    private void styleComboBox(ComboBox<String> cb) {
        cb.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4;");
    }

    private void loadData() {
        updating = true;
        try {
            initialSettings = manager.getSettings();

            physicalNamingStrategyCombo.setValue(initialSettings.getPhysicalNamingStrategy());

            String seq = initialSettings.getSequenceNamingStrategy();
            if ("Single sequence".equalsIgnoreCase(seq)) {
                seqSingleRadio.setSelected(true);
            } else if ("Sequence per entity".equalsIgnoreCase(seq)) {
                seqPerEntityRadio.setSelected(true);
            } else {
                seqAutoDetectRadio.setSelected(true);
            }

            maxDbIdentifierLengthField.setText(String.valueOf(initialSettings.getMaxDbIdentifierLength()));
            createIndexForAssociationFkCheck.setSelected(initialSettings.isCreateIndexForAssociationFk());

            if (initialSettings.isPrimaryKeyConstraintNamed()) {
                pkNamedRadio.setSelected(true);
            } else {
                pkUnnamedRadio.setSelected(true);
            }
            pkPrefixField.setText(initialSettings.getPkConstraintPrefix());
            pkSuffixField.setText(initialSettings.getPkConstraintSuffix());

            boolean named = pkNamedRadio.isSelected();
            pkPrefixField.setDisable(!named);
            pkPatternLabel.setDisable(!named);
            pkSuffixField.setDisable(!named);
        } finally {
            updating = false;
        }
    }

    private DatabaseVersioningSettings getCurrentSettingsFromUI() {
        DatabaseVersioningSettings s = new DatabaseVersioningSettings();

        s.setPhysicalNamingStrategy(physicalNamingStrategyCombo.getValue());

        if (seqSingleRadio.isSelected()) {
            s.setSequenceNamingStrategy("Single sequence");
        } else if (seqPerEntityRadio.isSelected()) {
            s.setSequenceNamingStrategy("Sequence per entity");
        } else {
            s.setSequenceNamingStrategy("Auto detect");
        }

        try {
            s.setMaxDbIdentifierLength(Integer.parseInt(maxDbIdentifierLengthField.getText().trim()));
        } catch (NumberFormatException e) {
            s.setMaxDbIdentifierLength(63);
        }

        s.setCreateIndexForAssociationFk(createIndexForAssociationFkCheck.isSelected());
        s.setPrimaryKeyConstraintNamed(pkNamedRadio.isSelected());
        s.setPkConstraintPrefix(pkPrefixField.getText());
        s.setPkConstraintSuffix(pkSuffixField.getText());

        return s;
    }

    public boolean isModified() {
        if (initialSettings == null) return false;
        return !Objects.equals(initialSettings, getCurrentSettingsFromUI());
    }

    public void apply() {
        DatabaseVersioningSettings updated = getCurrentSettingsFromUI();
        manager.setSettings(updated);
        initialSettings = updated.clone();
        notifyModified();
    }

    public void reset() {
        loadData();
        notifyModified();
    }

    public void revertChanges() {
        reset();
    }

    public void setOnModifiedListener(Runnable listener) {
        this.onModifiedListener = listener;
    }

    private void notifyModified() {
        if (!updating && onModifiedListener != null) {
            onModifiedListener.run();
        }
    }

    // Getters for testing
    public ComboBox<String> getPhysicalNamingStrategyCombo() { return physicalNamingStrategyCombo; }
    public RadioButton getSeqAutoDetectRadio() { return seqAutoDetectRadio; }
    public RadioButton getSeqSingleRadio() { return seqSingleRadio; }
    public RadioButton getSeqPerEntityRadio() { return seqPerEntityRadio; }
    public TextField getMaxDbIdentifierLengthField() { return maxDbIdentifierLengthField; }
    public CheckBox getCreateIndexForAssociationFkCheck() { return createIndexForAssociationFkCheck; }
    public RadioButton getPkUnnamedRadio() { return pkUnnamedRadio; }
    public RadioButton getPkNamedRadio() { return pkNamedRadio; }
    public TextField getPkPrefixField() { return pkPrefixField; }
    public TextField getPkSuffixField() { return pkSuffixField; }
}
