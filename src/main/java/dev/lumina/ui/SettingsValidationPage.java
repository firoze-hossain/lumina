package dev.lumina.ui;

import dev.lumina.build.CompilerExcludeEntry;
import dev.lumina.build.ValidationSettings;
import dev.lumina.build.ValidationSettingsManager;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Settings page for Build, Execution, Deployment > Compiler > Validation.
 * Matches 1:1 with reference screenshot media_1791449977870_5ea029a4.png:
 *  - Validate on build
 *  - Validators:
 *    - FreeMarker
 *    - Hibernate
 *    - JPA
 *    - Jasper
 *    - Spring Model
 *    - Web.xml
 *  - Exclude From Validation: table with Path, Recursiv... and toolbar (+, -)
 */
public class SettingsValidationPage extends VBox {

    private final ValidationSettingsManager manager = ValidationSettingsManager.getInstance();

    private final CheckBox validateOnBuildCheck = new CheckBox("Validate on build");

    // Validators
    private final CheckBox freeMarkerCheck = new CheckBox("FreeMarker");
    private final CheckBox hibernateCheck = new CheckBox("Hibernate");
    private final CheckBox jpaCheck = new CheckBox("JPA");
    private final CheckBox jasperCheck = new CheckBox("Jasper");
    private final CheckBox springModelCheck = new CheckBox("Spring Model");
    private final CheckBox webXmlCheck = new CheckBox("Web.xml");

    // Excludes table
    private final ObservableList<CompilerExcludeEntry> excludesList = FXCollections.observableArrayList();
    private final TableView<CompilerExcludeEntry> excludesTable = new TableView<>();
    private final Button addExcludeBtn = new Button("+");
    private final Button removeExcludeBtn = new Button("−");

    private ValidationSettings initialSettings;
    private Runnable onModifiedListener;
    private boolean updating = false;

    public SettingsValidationPage() {
        setPadding(new Insets(16, 24, 20, 24));
        setSpacing(12);
        setStyle("-fx-background-color: #1E1F22;");

        buildUI();
        loadData();
    }

    private void buildUI() {
        // 1. Top checkbox: Validate on build
        styleCheckBox(validateOnBuildCheck);
        validateOnBuildCheck.setOnAction(e -> fireModified());

        // 2. Validators section
        Label validatorsLabel = new Label("Validators:");
        validatorsLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-padding: 6 0 2 0;");

        styleCheckBox(freeMarkerCheck);
        styleCheckBox(hibernateCheck);
        styleCheckBox(jpaCheck);
        styleCheckBox(jasperCheck);
        styleCheckBox(springModelCheck);
        styleCheckBox(webXmlCheck);

        wireCheckBox(freeMarkerCheck);
        wireCheckBox(hibernateCheck);
        wireCheckBox(jpaCheck);
        wireCheckBox(jasperCheck);
        wireCheckBox(springModelCheck);
        wireCheckBox(webXmlCheck);

        VBox validatorsBox = new VBox(8,
                freeMarkerCheck,
                hibernateCheck,
                jpaCheck,
                jasperCheck,
                springModelCheck,
                webXmlCheck
        );
        validatorsBox.setPadding(new Insets(0, 0, 0, 16));

        // 3. Exclude From Validation section
        Label excludesLabel = new Label("Exclude From Validation:");
        excludesLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-padding: 10 0 2 0;");

        HBox toolbar = createMiniToolbar(addExcludeBtn, removeExcludeBtn);
        addExcludeBtn.setOnAction(e -> showAddExcludeMenu());
        removeExcludeBtn.setOnAction(e -> {
            CompilerExcludeEntry sel = excludesTable.getSelectionModel().getSelectedItem();
            if (sel != null) {
                excludesList.remove(sel);
                fireModified();
            }
        });

        setupExcludesTable();
        VBox tableBox = new VBox(0, toolbar, excludesTable);
        VBox.setVgrow(tableBox, Priority.ALWAYS);

        getChildren().addAll(validateOnBuildCheck, validatorsLabel, validatorsBox, excludesLabel, tableBox);
    }

    private void wireCheckBox(CheckBox cb) {
        cb.selectedProperty().addListener((obs, oldVal, newVal) -> fireModified());
    }

    private void setupExcludesTable() {
        excludesTable.setItems(excludesList);
        excludesTable.setEditable(true);
        excludesTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        excludesTable.setPrefHeight(180);
        excludesTable.setStyle("-fx-background-color: #1E1F22; -fx-border-color: #43454A; -fx-border-radius: 0 0 4 4; -fx-background-radius: 0 0 4 4;");

        Label placeholder = new Label("No excludes");
        placeholder.setStyle("-fx-text-fill: #707278; -fx-font-size: 12px;");
        excludesTable.setPlaceholder(placeholder);

        TableColumn<CompilerExcludeEntry, String> colPath = new TableColumn<>("Path");
        colPath.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getPath()));
        colPath.setPrefWidth(400);

        TableColumn<CompilerExcludeEntry, Boolean> colRec = new TableColumn<>("Recursiv...");
        colRec.setCellValueFactory(data -> {
            SimpleBooleanProperty prop = new SimpleBooleanProperty(data.getValue().isRecursive());
            prop.addListener((obs, o, n) -> {
                data.getValue().setRecursive(n);
                fireModified();
            });
            return prop;
        });
        colRec.setCellFactory(CheckBoxTableCell.forTableColumn(colRec));
        colRec.setEditable(true);
        colRec.setPrefWidth(90);

        excludesTable.getColumns().addAll(colPath, colRec);
    }

    private void showAddExcludeMenu() {
        ContextMenu menu = new ContextMenu();
        MenuItem addDir = new MenuItem("Exclude Directory...");
        addDir.setOnAction(e -> {
            DirectoryChooser dc = new DirectoryChooser();
            dc.setTitle("Exclude Directory from Validation");
            File f = dc.showDialog(getScene() != null ? getScene().getWindow() : null);
            if (f != null) {
                excludesList.add(new CompilerExcludeEntry(f.getAbsolutePath(), true));
                fireModified();
            }
        });

        MenuItem addFile = new MenuItem("Exclude File...");
        addFile.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Exclude File from Validation");
            File f = fc.showOpenDialog(getScene() != null ? getScene().getWindow() : null);
            if (f != null) {
                excludesList.add(new CompilerExcludeEntry(f.getAbsolutePath(), false));
                fireModified();
            }
        });

        menu.getItems().addAll(addDir, addFile);
        menu.show(addExcludeBtn, javafx.geometry.Side.BOTTOM, 0, 0);
    }

    private void styleCheckBox(CheckBox cb) {
        cb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
    }

    private HBox createMiniToolbar(Button... buttons) {
        HBox bar = new HBox(2);
        for (Button b : buttons) {
            b.setStyle("-fx-background-color: transparent; -fx-text-fill: #DFE1E5; -fx-font-size: 14px; -fx-cursor: hand; -fx-padding: 2 8;");
            bar.getChildren().add(b);
        }
        bar.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #43454A; -fx-border-width: 1 1 0 1; -fx-padding: 2 4;");
        return bar;
    }

    public void loadData() {
        updating = true;
        initialSettings = manager.getSettings();

        validateOnBuildCheck.setSelected(initialSettings.isValidateOnBuild());
        freeMarkerCheck.setSelected(initialSettings.isFreeMarker());
        hibernateCheck.setSelected(initialSettings.isHibernate());
        jpaCheck.setSelected(initialSettings.isJpa());
        jasperCheck.setSelected(initialSettings.isJasper());
        springModelCheck.setSelected(initialSettings.isSpringModel());
        webXmlCheck.setSelected(initialSettings.isWebXml());

        excludesList.clear();
        for (CompilerExcludeEntry e : initialSettings.getExcludes()) {
            excludesList.add(e.clone());
        }

        updating = false;
    }

    public ValidationSettings getCurrentSettings() {
        ValidationSettings s = new ValidationSettings();
        s.setValidateOnBuild(validateOnBuildCheck.isSelected());
        s.setFreeMarker(freeMarkerCheck.isSelected());
        s.setHibernate(hibernateCheck.isSelected());
        s.setJpa(jpaCheck.isSelected());
        s.setJasper(jasperCheck.isSelected());
        s.setSpringModel(springModelCheck.isSelected());
        s.setWebXml(webXmlCheck.isSelected());

        List<CompilerExcludeEntry> list = new ArrayList<>();
        for (CompilerExcludeEntry e : excludesList) {
            list.add(e.clone());
        }
        s.setExcludes(list);
        return s;
    }

    public boolean isModified() {
        if (initialSettings == null) return false;
        return !Objects.equals(initialSettings, getCurrentSettings());
    }

    public void apply() {
        ValidationSettings current = getCurrentSettings();
        manager.setSettings(current);
        initialSettings = current.clone();
        if (onModifiedListener != null) onModifiedListener.run();
    }

    public void reset() {
        loadData();
        if (onModifiedListener != null) onModifiedListener.run();
    }

    public void setOnModifiedListener(Runnable listener) {
        this.onModifiedListener = listener;
    }

    private void fireModified() {
        if (!updating && onModifiedListener != null) {
            onModifiedListener.run();
        }
    }

    public CheckBox getValidateOnBuildCheck() { return validateOnBuildCheck; }
    public CheckBox getFreeMarkerCheck() { return freeMarkerCheck; }
    public CheckBox getHibernateCheck() { return hibernateCheck; }
    public CheckBox getJpaCheck() { return jpaCheck; }
    public CheckBox getJasperCheck() { return jasperCheck; }
    public CheckBox getSpringModelCheck() { return springModelCheck; }
    public CheckBox getWebXmlCheck() { return webXmlCheck; }
    public ObservableList<CompilerExcludeEntry> getExcludesList() { return excludesList; }
}
