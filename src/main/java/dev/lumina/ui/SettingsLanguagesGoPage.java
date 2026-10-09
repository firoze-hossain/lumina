package dev.lumina.ui;

import dev.lumina.go.GoSettings;
import dev.lumina.go.GoSettingsManager;
import javafx.geometry.Insets;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Settings page for Languages & Frameworks > Go.
 * Faithfully matches Image 2.
 */
public class SettingsLanguagesGoPage extends VBox {

    private final GoSettingsManager manager = GoSettingsManager.getInstance();

    private CheckBox suggestParamsCheck;
    private CheckBox suggestVariantsCheck;
    private CheckBox indentOnEnterCheck;
    private CheckBox showDocCheck;
    private CheckBox detectClipboardCheck;
    private CheckBox askSharingCheck;

    private ToggleGroup dirRenamedGroup;
    private RadioButton dirShowOptRadio;
    private RadioButton dirRenamePkgRadio;
    private RadioButton dirDoNotRenameRadio;

    private ToggleGroup pkgRenamedGroup;
    private RadioButton pkgShowOptRadio;
    private RadioButton pkgRenameDirRadio;
    private RadioButton pkgDoNotRenameRadio;

    private ToggleGroup fileRenamedGroup;
    private RadioButton fileShowOptRadio;
    private RadioButton fileRenameCorrespRadio;
    private RadioButton fileDoNotRenameRadio;

    private ToggleGroup jsonPastedGroup;
    private RadioButton jsonShowOptRadio;
    private RadioButton jsonConvertRadio;
    private RadioButton jsonInsertAsIsRadio;

    private ToggleGroup tagRenamedGroup;
    private RadioButton tagShowOptRadio;
    private RadioButton tagRenameTagRadio;
    private RadioButton tagDoNotRenameRadio;

    private GoSettings initialSettings = new GoSettings();
    private Runnable onModified;

    public SettingsLanguagesGoPage() {
        setSpacing(14);
        setPadding(new Insets(16, 20, 20, 20));
        setStyle("-fx-background-color: #1E1F22;");
        VBox.setVgrow(this, Priority.ALWAYS);

        buildUI();
        loadFromManager();
    }

    public void setOnModified(Runnable onModified) {
        this.onModified = onModified;
    }

    private void notifyModified() {
        if (onModified != null) {
            onModified.run();
        }
    }

    private void buildUI() {
        // Top checkboxes
        suggestParamsCheck = createCheckBox("Suggest parameters name in completion", true);
        suggestVariantsCheck = createCheckBox("Suggest variants that require additional imports as you type", true);
        indentOnEnterCheck = createCheckBox("Indent on Enter in raw strings", false);
        showDocCheck = createCheckBox("Show documentation in parameter info", false);
        detectClipboardCheck = createCheckBox("Detect go packages from clipboard", false);
        askSharingCheck = createCheckBox("Ask before sharing in Go Playground", true);

        VBox topChecksBox = new VBox(8,
                suggestParamsCheck,
                suggestVariantsCheck,
                indentOnEnterCheck,
                showDocCheck,
                detectClipboardCheck,
                askSharingCheck
        );

        // Radio group 1: When directory is renamed
        dirRenamedGroup = new ToggleGroup();
        dirShowOptRadio = createRadio("Show options", dirRenamedGroup);
        dirRenamePkgRadio = createRadio("Rename package", dirRenamedGroup);
        dirDoNotRenameRadio = createRadio("Do not rename package", dirRenamedGroup);
        VBox dirBox = createRadioSection("When directory is renamed", dirShowOptRadio, dirRenamePkgRadio, dirDoNotRenameRadio);

        // Radio group 2: When package is renamed
        pkgRenamedGroup = new ToggleGroup();
        pkgShowOptRadio = createRadio("Show options", pkgRenamedGroup);
        pkgRenameDirRadio = createRadio("Rename directory", pkgRenamedGroup);
        pkgDoNotRenameRadio = createRadio("Do not rename directory", pkgRenamedGroup);
        VBox pkgBox = createRadioSection("When package is renamed", pkgShowOptRadio, pkgRenameDirRadio, pkgDoNotRenameRadio);

        // Radio group 3: When file is renamed
        fileRenamedGroup = new ToggleGroup();
        fileShowOptRadio = createRadio("Show options", fileRenamedGroup);
        fileRenameCorrespRadio = createRadio("Rename corresponding test or production file", fileRenamedGroup);
        fileDoNotRenameRadio = createRadio("Do not rename corresponding test or production file", fileRenamedGroup);
        VBox fileBox = createRadioSection("When file is renamed", fileShowOptRadio, fileRenameCorrespRadio, fileDoNotRenameRadio);

        // Radio group 4: When JSON is pasted
        jsonPastedGroup = new ToggleGroup();
        jsonShowOptRadio = createRadio("Show options", jsonPastedGroup);
        jsonConvertRadio = createRadio("Convert JSON to a Go type", jsonPastedGroup);
        jsonInsertAsIsRadio = createRadio("Insert JSON as-is", jsonPastedGroup);
        VBox jsonBox = createRadioSection("When JSON is pasted", jsonShowOptRadio, jsonConvertRadio, jsonInsertAsIsRadio);

        // Radio group 5: When tag is renamed
        tagRenamedGroup = new ToggleGroup();
        tagShowOptRadio = createRadio("Show options", tagRenamedGroup);
        tagRenameTagRadio = createRadio("Rename a tag", tagRenamedGroup);
        tagDoNotRenameRadio = createRadio("Do not rename tag", tagRenamedGroup);
        VBox tagBox = createRadioSection("When tag is renamed", tagShowOptRadio, tagRenameTagRadio, tagDoNotRenameRadio);

        getChildren().addAll(topChecksBox, dirBox, pkgBox, fileBox, jsonBox, tagBox);
    }

    private CheckBox createCheckBox(String text, boolean defaultValue) {
        CheckBox cb = new CheckBox(text);
        cb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        cb.setSelected(defaultValue);
        cb.selectedProperty().addListener((obs, ov, nv) -> notifyModified());
        return cb;
    }

    private RadioButton createRadio(String text, ToggleGroup group) {
        RadioButton rb = new RadioButton(text);
        rb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        rb.setToggleGroup(group);
        rb.selectedProperty().addListener((obs, ov, nv) -> notifyModified());
        return rb;
    }

    private VBox createRadioSection(String title, RadioButton... radios) {
        Label lbl = new Label(title);
        lbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        VBox radioList = new VBox(4);
        radioList.setPadding(new Insets(2, 0, 0, 16));
        radioList.getChildren().addAll(radios);

        return new VBox(4, lbl, radioList);
    }

    public void loadFromManager() {
        GoSettings s = manager.getSettings();
        suggestParamsCheck.setSelected(s.isSuggestParametersNameInCompletion());
        suggestVariantsCheck.setSelected(s.isSuggestVariantsRequireAdditionalImports());
        indentOnEnterCheck.setSelected(s.isIndentOnEnterInRawStrings());
        showDocCheck.setSelected(s.isShowDocInParameterInfo());
        detectClipboardCheck.setSelected(s.isDetectGoPackagesFromClipboard());
        askSharingCheck.setSelected(s.isAskBeforeSharingInGoPlayground());

        selectRadioByValue(s.getWhenDirectoryRenamed(), dirShowOptRadio, dirRenamePkgRadio, dirDoNotRenameRadio);
        selectRadioByValue(s.getWhenPackageRenamed(), pkgShowOptRadio, pkgRenameDirRadio, pkgDoNotRenameRadio);
        selectRadioByValue(s.getWhenFileRenamed(), fileShowOptRadio, fileRenameCorrespRadio, fileDoNotRenameRadio);
        selectRadioByValue(s.getWhenJsonPasted(), jsonShowOptRadio, jsonConvertRadio, jsonInsertAsIsRadio);
        selectRadioByValue(s.getWhenTagRenamed(), tagShowOptRadio, tagRenameTagRadio, tagDoNotRenameRadio);

        initialSettings = s.copy();
    }

    private void selectRadioByValue(String val, RadioButton opt1, RadioButton opt2, RadioButton opt3) {
        if (opt2.getText().equalsIgnoreCase(val)) {
            opt2.setSelected(true);
        } else if (opt3.getText().equalsIgnoreCase(val)) {
            opt3.setSelected(true);
        } else {
            opt1.setSelected(true);
        }
    }

    private String getSelectedRadioValue(RadioButton opt1, RadioButton opt2, RadioButton opt3) {
        if (opt2.isSelected()) return opt2.getText();
        if (opt3.isSelected()) return opt3.getText();
        return opt1.getText();
    }

    private GoSettings buildCurrentSettings() {
        GoSettings s = manager.getSettings();
        s.setSuggestParametersNameInCompletion(suggestParamsCheck.isSelected());
        s.setSuggestVariantsRequireAdditionalImports(suggestVariantsCheck.isSelected());
        s.setIndentOnEnterInRawStrings(indentOnEnterCheck.isSelected());
        s.setShowDocInParameterInfo(showDocCheck.isSelected());
        s.setDetectGoPackagesFromClipboard(detectClipboardCheck.isSelected());
        s.setAskBeforeSharingInGoPlayground(askSharingCheck.isSelected());

        s.setWhenDirectoryRenamed(getSelectedRadioValue(dirShowOptRadio, dirRenamePkgRadio, dirDoNotRenameRadio));
        s.setWhenPackageRenamed(getSelectedRadioValue(pkgShowOptRadio, pkgRenameDirRadio, pkgDoNotRenameRadio));
        s.setWhenFileRenamed(getSelectedRadioValue(fileShowOptRadio, fileRenameCorrespRadio, fileDoNotRenameRadio));
        s.setWhenJsonPasted(getSelectedRadioValue(jsonShowOptRadio, jsonConvertRadio, jsonInsertAsIsRadio));
        s.setWhenTagRenamed(getSelectedRadioValue(tagShowOptRadio, tagRenameTagRadio, tagDoNotRenameRadio));

        return s;
    }

    public boolean isModified() {
        GoSettings c = buildCurrentSettings();
        return c.isSuggestParametersNameInCompletion() != initialSettings.isSuggestParametersNameInCompletion() ||
                c.isSuggestVariantsRequireAdditionalImports() != initialSettings.isSuggestVariantsRequireAdditionalImports() ||
                c.isIndentOnEnterInRawStrings() != initialSettings.isIndentOnEnterInRawStrings() ||
                c.isShowDocInParameterInfo() != initialSettings.isShowDocInParameterInfo() ||
                c.isDetectGoPackagesFromClipboard() != initialSettings.isDetectGoPackagesFromClipboard() ||
                c.isAskBeforeSharingInGoPlayground() != initialSettings.isAskBeforeSharingInGoPlayground() ||
                !c.getWhenDirectoryRenamed().equals(initialSettings.getWhenDirectoryRenamed()) ||
                !c.getWhenPackageRenamed().equals(initialSettings.getWhenPackageRenamed()) ||
                !c.getWhenFileRenamed().equals(initialSettings.getWhenFileRenamed()) ||
                !c.getWhenJsonPasted().equals(initialSettings.getWhenJsonPasted()) ||
                !c.getWhenTagRenamed().equals(initialSettings.getWhenTagRenamed());
    }

    public void apply() {
        GoSettings s = buildCurrentSettings();
        manager.setSettings(s);
        initialSettings = s.copy();
    }

    public void reset() {
        loadFromManager();
    }

    public void revertChanges() {
        reset();
    }
}
