package dev.lumina.ui;

import dev.lumina.build.MavenSettings;
import dev.lumina.build.MavenSettingsManager;
import javafx.geometry.Insets;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

/**
 * Settings subpage for Build, Execution, Deployment > Build Tools > Maven > Running Tests.
 * Matches 1:1 with standard IDE design:
 * Pass to JUnit process following maven-surefire-plugin and maven-failsafe-plugin settings:
 *  - argLine
 *  - systemPropertyVariables
 *  - environmentVariables
 */
public class SettingsMavenRunningTestsPage extends VBox {

    private final MavenSettingsManager manager = MavenSettingsManager.getInstance();

    private final CheckBox passArgLineCheck = new CheckBox("argLine");
    private final CheckBox passSystemPropertyVariablesCheck = new CheckBox("systemPropertyVariables");
    private final CheckBox passEnvironmentVariablesCheck = new CheckBox("environmentVariables");

    private MavenSettings initialSettings;
    private Runnable onModifiedListener;
    private boolean updating = false;

    public SettingsMavenRunningTestsPage() {
        setPadding(new Insets(20, 24, 20, 24));
        setSpacing(14);
        setStyle("-fx-background-color: #1E1F22;");

        buildUI();
        loadData();
    }

    private void buildUI() {
        // Header text: Pass to JUnit process following maven-surefire-plugin and maven-failsafe-plugin settings:
        Text text1 = new Text("Pass to JUnit process following ");
        text1.setFill(javafx.scene.paint.Color.web("#DFE1E5"));
        text1.setFont(Font.font("System", FontWeight.NORMAL, 13));

        Text textSurefire = new Text("maven-surefire-plugin");
        textSurefire.setFill(javafx.scene.paint.Color.web("#DFE1E5"));
        textSurefire.setFont(Font.font("System", FontWeight.BOLD, 13));

        Text textAnd = new Text(" and ");
        textAnd.setFill(javafx.scene.paint.Color.web("#DFE1E5"));
        textAnd.setFont(Font.font("System", FontWeight.NORMAL, 13));

        Text textFailsafe = new Text("maven-failsafe-plugin");
        textFailsafe.setFill(javafx.scene.paint.Color.web("#DFE1E5"));
        textFailsafe.setFont(Font.font("System", FontWeight.BOLD, 13));

        Text textSettings = new Text(" settings:");
        textSettings.setFill(javafx.scene.paint.Color.web("#DFE1E5"));
        textSettings.setFont(Font.font("System", FontWeight.NORMAL, 13));

        TextFlow headerFlow = new TextFlow(text1, textSurefire, textAnd, textFailsafe, textSettings);
        headerFlow.setPadding(new Insets(0, 0, 4, 0));

        // Italic checkboxes
        styleItalicCheckBox(passArgLineCheck);
        styleItalicCheckBox(passSystemPropertyVariablesCheck);
        styleItalicCheckBox(passEnvironmentVariablesCheck);

        VBox checksBox = new VBox(10, passArgLineCheck, passSystemPropertyVariablesCheck, passEnvironmentVariablesCheck);
        checksBox.setPadding(new Insets(4, 0, 0, 0));

        getChildren().addAll(headerFlow, checksBox);

        passArgLineCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        passSystemPropertyVariablesCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        passEnvironmentVariablesCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
    }

    private void styleItalicCheckBox(CheckBox cb) {
        cb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-style: italic; -fx-font-size: 13px;");
    }

    public void loadData() {
        updating = true;
        initialSettings = manager.getSettings();

        passArgLineCheck.setSelected(initialSettings.isPassArgLine());
        passSystemPropertyVariablesCheck.setSelected(initialSettings.isPassSystemPropertyVariables());
        passEnvironmentVariablesCheck.setSelected(initialSettings.isPassEnvironmentVariables());

        updating = false;
    }

    public boolean isModified() {
        if (initialSettings == null) return false;
        MavenSettings cur = getCurrentSettings();
        return initialSettings.isPassArgLine() != cur.isPassArgLine()
                || initialSettings.isPassSystemPropertyVariables() != cur.isPassSystemPropertyVariables()
                || initialSettings.isPassEnvironmentVariables() != cur.isPassEnvironmentVariables();
    }

    public void apply() {
        MavenSettings cur = getCurrentSettings();
        MavenSettings s = manager.getSettings();
        s.setPassArgLine(cur.isPassArgLine());
        s.setPassSystemPropertyVariables(cur.isPassSystemPropertyVariables());
        s.setPassEnvironmentVariables(cur.isPassEnvironmentVariables());
        manager.setSettings(s);
        initialSettings = s.clone();
    }

    public void reset() {
        loadData();
    }

    public MavenSettings getCurrentSettings() {
        MavenSettings s = (initialSettings != null) ? initialSettings.clone() : new MavenSettings();
        s.setPassArgLine(passArgLineCheck.isSelected());
        s.setPassSystemPropertyVariables(passSystemPropertyVariablesCheck.isSelected());
        s.setPassEnvironmentVariables(passEnvironmentVariablesCheck.isSelected());
        return s;
    }

    public CheckBox getPassArgLineCheck() {
        return passArgLineCheck;
    }

    public CheckBox getPassSystemPropertyVariablesCheck() {
        return passSystemPropertyVariablesCheck;
    }

    public CheckBox getPassEnvironmentVariablesCheck() {
        return passEnvironmentVariablesCheck;
    }

    public void setOnModifiedListener(Runnable listener) {
        this.onModifiedListener = listener;
    }

    private void notifyModified() {
        if (!updating && onModifiedListener != null) {
            onModifiedListener.run();
        }
    }
}
