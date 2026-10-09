package dev.lumina.ui;

import dev.lumina.php.PhpDebugSettings;
import dev.lumina.php.PhpSettingsManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Settings page for Languages & Frameworks > PHP > Debug > DBGp Proxy.
 * Faithfully matches Image 4.
 */
public class SettingsLanguagesPhpDebugDbgpProxyPage extends VBox {

    private final PhpSettingsManager manager = PhpSettingsManager.getInstance();

    private TextField ideKeyField;
    private TextField hostField;
    private TextField portField;

    private String initialIdeKey = "";
    private String initialHost = "";
    private String initialPort = "9001";

    private Runnable onModifiedListener;

    public SettingsLanguagesPhpDebugDbgpProxyPage() {
        setSpacing(16);
        setPadding(new Insets(16, 22, 20, 22));
        setStyle("-fx-background-color: #1E1F22;");

        buildForm();
        loadFromManager();
        takeSnapshot();
    }

    public void setOnModifiedListener(Runnable listener) {
        this.onModifiedListener = listener;
    }

    private void notifyModified() {
        if (onModifiedListener != null) {
            onModifiedListener.run();
        }
    }

    private void buildForm() {
        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(10);
        grid.setAlignment(Pos.TOP_LEFT);

        // 1. IDE key
        Label ideKeyLabel = createLabel("IDE key:");
        ideKeyField = createTextField("", 450);
        GridPane.setHgrow(ideKeyField, Priority.ALWAYS);
        grid.add(ideKeyLabel, 0, 0);
        grid.add(ideKeyField, 1, 0);

        // 2. Host
        Label hostLabel = createLabel("Host:");
        hostField = createTextField("", 450);
        GridPane.setHgrow(hostField, Priority.ALWAYS);
        grid.add(hostLabel, 0, 1);
        grid.add(hostField, 1, 1);

        // 3. Port
        Label portLabel = createLabel("Port:");
        portField = createTextField("9001", 90);
        portField.setMaxWidth(90);
        grid.add(portLabel, 0, 2);
        grid.add(portField, 1, 2);

        getChildren().add(grid);
    }

    private Label createLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        label.setMinWidth(70);
        return label;
    }

    private TextField createTextField(String text, double prefWidth) {
        TextField tf = new TextField(text);
        if (prefWidth > 0) {
            tf.setPrefWidth(prefWidth);
        }
        tf.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-padding: 4 8; -fx-font-size: 13px;");
        tf.textProperty().addListener((obs, ov, nv) -> notifyModified());
        return tf;
    }

    public void loadFromManager() {
        PhpDebugSettings s = manager.getDebugSettings();
        ideKeyField.setText(s.getDbgpIdeKey());
        hostField.setText(s.getDbgpHost());
        portField.setText(s.getDbgpPort());
    }

    public void takeSnapshot() {
        this.initialIdeKey = ideKeyField.getText();
        this.initialHost = hostField.getText();
        this.initialPort = portField.getText();
    }

    public boolean isModified() {
        return !ideKeyField.getText().equals(initialIdeKey)
                || !hostField.getText().equals(initialHost)
                || !portField.getText().equals(initialPort);
    }

    public void apply() {
        PhpDebugSettings s = manager.getDebugSettings();
        s.setDbgpIdeKey(ideKeyField.getText());
        s.setDbgpHost(hostField.getText());
        s.setDbgpPort(portField.getText());
        manager.setDebugSettings(s);
        takeSnapshot();
        notifyModified();
    }

    public void reset() {
        loadFromManager();
        takeSnapshot();
        notifyModified();
    }

    public void revertChanges() {
        ideKeyField.setText(initialIdeKey);
        hostField.setText(initialHost);
        portField.setText(initialPort);
        notifyModified();
    }

    // Getters for testing
    public TextField getIdeKeyField() {
        return ideKeyField;
    }

    public TextField getHostField() {
        return hostField;
    }

    public TextField getPortField() {
        return portField;
    }
}
