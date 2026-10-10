package dev.lumina.ui;

import dev.lumina.schemas.DefaultXmlSchemasSettings;
import dev.lumina.schemas.SchemasAndDtdsSettingsManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.Objects;

/**
 * Languages & Frameworks > Schemas and DTDs > Default XML Schemas settings page in Lumina IDE.
 * Matches Image 4:
 *  - Default HTML language level:
 *    - ( ) HTML 4 ("http://www.w3.org/TR/html4/loose.dtd")
 *    - (o) HTML 5
 *    - ( ) Other doctype: [ TextField ]
 *  - XML Schema version:
 *    - (o) XML Schema 1.0
 *    - ( ) XML Schema 1.1
 */
public class SettingsLanguagesDefaultXmlSchemasPage extends VBox {

    private final SchemasAndDtdsSettingsManager manager = SchemasAndDtdsSettingsManager.getInstance();
    private Runnable onModifiedListener;

    // HTML Language Level
    private ToggleGroup htmlToggleGroup;
    private RadioButton html4Radio;
    private RadioButton html5Radio;
    private RadioButton otherDoctypeRadio;
    private TextField otherDoctypeField;

    // XML Schema Version
    private ToggleGroup xmlToggleGroup;
    private RadioButton xmlSchema10Radio;
    private RadioButton xmlSchema11Radio;

    private DefaultXmlSchemasSettings initialSettings;

    public SettingsLanguagesDefaultXmlSchemasPage() {
        setSpacing(14);
        setPadding(new Insets(20, 24, 20, 24));
        setStyle("-fx-background-color: #1E1F22;");
        VBox.setVgrow(this, Priority.ALWAYS);

        buildContent();
        takeSnapshot();
    }

    private void buildContent() {
        DefaultXmlSchemasSettings current = manager.getDefaultXmlSchemasSettings();

        // 1. Default HTML language level section
        Label htmlTitle = new Label("Default HTML language level");
        htmlTitle.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        htmlToggleGroup = new ToggleGroup();

        html4Radio = new RadioButton("HTML 4 (\"http://www.w3.org/TR/html4/loose.dtd\")");
        html4Radio.setToggleGroup(htmlToggleGroup);
        styleRadio(html4Radio);

        html5Radio = new RadioButton("HTML 5");
        html5Radio.setToggleGroup(htmlToggleGroup);
        styleRadio(html5Radio);

        HBox otherRow = new HBox(8);
        otherRow.setAlignment(Pos.CENTER_LEFT);

        otherDoctypeRadio = new RadioButton("Other doctype:");
        otherDoctypeRadio.setToggleGroup(htmlToggleGroup);
        styleRadio(otherDoctypeRadio);

        otherDoctypeField = new TextField(current.getOtherDoctype());
        otherDoctypeField.setPrefWidth(260);
        otherDoctypeField.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #43454A; -fx-border-radius: 4; -fx-padding: 3 8;");
        otherDoctypeField.textProperty().addListener((obs, oldV, newV) -> fireModified());

        otherRow.getChildren().addAll(otherDoctypeRadio, otherDoctypeField);

        if (DefaultXmlSchemasSettings.HTML_4.equals(current.getHtmlLanguageLevel())) {
            html4Radio.setSelected(true);
        } else if (DefaultXmlSchemasSettings.HTML_OTHER.equals(current.getHtmlLanguageLevel())) {
            otherDoctypeRadio.setSelected(true);
        } else {
            html5Radio.setSelected(true);
        }

        updateOtherFieldState();
        htmlToggleGroup.selectedToggleProperty().addListener((obs, oldV, newV) -> {
            updateOtherFieldState();
            fireModified();
        });

        VBox htmlBox = new VBox(8, htmlTitle, html4Radio, html5Radio, otherRow);

        // 2. XML Schema version section
        HBox xmlHeader = createSectionHeader("XML Schema version");

        xmlToggleGroup = new ToggleGroup();

        xmlSchema10Radio = new RadioButton("XML Schema 1.0");
        xmlSchema10Radio.setToggleGroup(xmlToggleGroup);
        styleRadio(xmlSchema10Radio);

        xmlSchema11Radio = new RadioButton("XML Schema 1.1");
        xmlSchema11Radio.setToggleGroup(xmlToggleGroup);
        styleRadio(xmlSchema11Radio);

        if (DefaultXmlSchemasSettings.XML_SCHEMA_1_1.equals(current.getXmlSchemaVersion())) {
            xmlSchema11Radio.setSelected(true);
        } else {
            xmlSchema10Radio.setSelected(true);
        }

        xmlToggleGroup.selectedToggleProperty().addListener((obs, oldV, newV) -> fireModified());

        VBox xmlBox = new VBox(8, xmlHeader, xmlSchema10Radio, xmlSchema11Radio);

        getChildren().addAll(htmlBox, xmlBox);
    }

    private void updateOtherFieldState() {
        boolean otherSelected = otherDoctypeRadio.isSelected();
        otherDoctypeField.setDisable(!otherSelected);
        otherDoctypeField.setOpacity(otherSelected ? 1.0 : 0.6);
    }

    private HBox createSectionHeader(String title) {
        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(10, 0, 2, 0));

        Label label = new Label(title);
        label.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: #393B40; -fx-opacity: 0.5;");
        HBox.setHgrow(sep, Priority.ALWAYS);

        header.getChildren().addAll(label, sep);
        return header;
    }

    private void styleRadio(RadioButton rb) {
        rb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
    }

    public void setOnModifiedListener(Runnable listener) {
        this.onModifiedListener = listener;
    }

    private void fireModified() {
        if (onModifiedListener != null) {
            onModifiedListener.run();
        }
    }

    public void takeSnapshot() {
        this.initialSettings = getCurrentUiSettings();
    }

    public DefaultXmlSchemasSettings getCurrentUiSettings() {
        String level = DefaultXmlSchemasSettings.HTML_5;
        if (html4Radio.isSelected()) {
            level = DefaultXmlSchemasSettings.HTML_4;
        } else if (otherDoctypeRadio.isSelected()) {
            level = DefaultXmlSchemasSettings.HTML_OTHER;
        }

        String xmlVer = xmlSchema11Radio.isSelected() ?
                DefaultXmlSchemasSettings.XML_SCHEMA_1_1 :
                DefaultXmlSchemasSettings.XML_SCHEMA_1_0;

        return new DefaultXmlSchemasSettings(level, otherDoctypeField.getText(), xmlVer);
    }

    public boolean isModified() {
        if (initialSettings == null) return false;
        return !Objects.equals(initialSettings, getCurrentUiSettings());
    }

    public void apply() {
        manager.setDefaultXmlSchemasSettings(getCurrentUiSettings());
        takeSnapshot();
        fireModified();
    }

    public void reset() {
        DefaultXmlSchemasSettings saved = manager.getDefaultXmlSchemasSettings();
        applySettingsToUi(saved);
        takeSnapshot();
        fireModified();
    }

    public void revertChanges() {
        if (initialSettings != null) {
            applySettingsToUi(initialSettings);
            fireModified();
        }
    }

    public void resetDefaults() {
        applySettingsToUi(new DefaultXmlSchemasSettings());
        fireModified();
    }

    private void applySettingsToUi(DefaultXmlSchemasSettings s) {
        if (s == null) return;
        if (DefaultXmlSchemasSettings.HTML_4.equals(s.getHtmlLanguageLevel())) {
            html4Radio.setSelected(true);
        } else if (DefaultXmlSchemasSettings.HTML_OTHER.equals(s.getHtmlLanguageLevel())) {
            otherDoctypeRadio.setSelected(true);
        } else {
            html5Radio.setSelected(true);
        }
        otherDoctypeField.setText(s.getOtherDoctype());
        updateOtherFieldState();

        if (DefaultXmlSchemasSettings.XML_SCHEMA_1_1.equals(s.getXmlSchemaVersion())) {
            xmlSchema11Radio.setSelected(true);
        } else {
            xmlSchema10Radio.setSelected(true);
        }
    }

    // Direct UI accessors for tests
    public RadioButton getHtml4Radio() {
        return html4Radio;
    }

    public RadioButton getHtml5Radio() {
        return html5Radio;
    }

    public RadioButton getOtherDoctypeRadio() {
        return otherDoctypeRadio;
    }

    public TextField getOtherDoctypeField() {
        return otherDoctypeField;
    }

    public RadioButton getXmlSchema10Radio() {
        return xmlSchema10Radio;
    }

    public RadioButton getXmlSchema11Radio() {
        return xmlSchema11Radio;
    }
}
