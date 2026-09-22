package dev.lumina.ui;

import dev.lumina.git.SubversionSettingsManager;
import dev.lumina.git.SubversionSettingsManager.SshAuthMode;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.stage.FileChooser;

import java.io.File;

/**
 * Version Control > Subversion > SSH settings page matching IntelliJ IDEA Image 1.
 */
public class SettingsVcsSubversionSshPage extends VBox {

    private final SubversionSettingsManager manager = SubversionSettingsManager.getInstance();

    // Top fields
    private final TextField sshExecutableField = new TextField();
    private final Button sshExecutableBrowseBtn = new Button("...");
    private final TextField userNameField = new TextField();
    private final Spinner<Integer> portSpinner = new Spinner<>(1, 65535, 22);

    // Radios
    private final RadioButton passwordRadio = new RadioButton("Password");
    private final RadioButton privateKeyRadio = new RadioButton("Private key");
    private final RadioButton subversionConfigRadio = new RadioButton("Subversion config");

    // Private key fields
    private final TextField privateKeyPathField = new TextField();
    private final Button privateKeyBrowseBtn = new Button();

    // Subversion config fields
    private final TextField sshTunnelField = new TextField();
    private final Button updateTunnelBtn = new Button("Update");
    private final TextField svnSshEnvField = new TextField();

    public SettingsVcsSubversionSshPage() {
        getStyleClass().add("settings-page");
        setPadding(new Insets(14, 20, 24, 20));
        setSpacing(12);
        setStyle("-fx-background-color: #1E1F22; -fx-font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;");

        // 1. Executable Row
        HBox execRow = buildExecRow();

        // 2. User Name Row
        HBox userRow = buildUserRow();

        // 3. Port Row
        HBox portRow = buildPortRow();

        // 4. Auth Radios & Indented Options
        VBox authBox = buildAuthSection();

        getChildren().addAll(execRow, userRow, portRow, authBox);

        manager.addListener(this::syncFromManager);
        syncFromManager();
    }

    private HBox buildExecRow() {
        Label label = createFieldLabel("SSH executable:");

        sshExecutableField.setText(manager.getSshExecutablePath());
        styleTextField(sshExecutableField, 380);
        HBox.setHgrow(sshExecutableField, Priority.ALWAYS);
        sshExecutableField.textProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) manager.setSshExecutablePath(newV);
        });

        sshExecutableBrowseBtn.setStyle("-fx-background-color: #1E1F22; -fx-text-fill: #DFE1E5; -fx-border-color: #5A5D63; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 4 10 4 10; -fx-font-size: 12px; -fx-cursor: hand;");
        sshExecutableBrowseBtn.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Select SSH Executable");
            File f = fc.showOpenDialog(getScene().getWindow());
            if (f != null) {
                sshExecutableField.setText(f.getAbsolutePath());
                manager.setSshExecutablePath(f.getAbsolutePath());
            }
        });

        HBox row = new HBox(8, label, sshExecutableField, sshExecutableBrowseBtn);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private HBox buildUserRow() {
        Label label = createFieldLabel("User name:");

        userNameField.setText(manager.getSshUserName());
        styleTextField(userNameField, 380);
        HBox.setHgrow(userNameField, Priority.ALWAYS);
        userNameField.textProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) manager.setSshUserName(newV);
        });

        HBox row = new HBox(8, label, userNameField);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private HBox buildPortRow() {
        Label label = createFieldLabel("Port:");

        portSpinner.getValueFactory().setValue(manager.getSshPort());
        portSpinner.setPrefWidth(90);
        portSpinner.setEditable(true);
        portSpinner.setStyle("-fx-background-color: #1E1F22; -fx-font-size: 12px;");
        portSpinner.valueProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) manager.setSshPort(newV);
        });

        HBox row = new HBox(8, label, portSpinner);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private VBox buildAuthSection() {
        VBox box = new VBox(8);

        ToggleGroup group = new ToggleGroup();
        passwordRadio.setToggleGroup(group);
        privateKeyRadio.setToggleGroup(group);
        subversionConfigRadio.setToggleGroup(group);

        initRadio(passwordRadio, manager.getSshAuthMode() == SshAuthMode.PASSWORD);
        initRadio(privateKeyRadio, manager.getSshAuthMode() == SshAuthMode.PRIVATE_KEY);
        initRadio(subversionConfigRadio, manager.getSshAuthMode() == SshAuthMode.SUBVERSION_CONFIG);

        // Private Key indented row
        Label pathLabel = new Label("Path:");
        pathLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        pathLabel.setPrefWidth(90);

        privateKeyPathField.setText(manager.getSshPrivateKeyPath());
        styleTextField(privateKeyPathField, 380);
        HBox.setHgrow(privateKeyPathField, Priority.ALWAYS);
        privateKeyPathField.textProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) manager.setSshPrivateKeyPath(newV);
        });

        styleFolderBrowseBtn(privateKeyBrowseBtn, "Select Private Key File", f -> {
            privateKeyPathField.setText(f.getAbsolutePath());
            manager.setSshPrivateKeyPath(f.getAbsolutePath());
        });

        HBox privateKeyRow = new HBox(8, pathLabel, privateKeyPathField, privateKeyBrowseBtn);
        privateKeyRow.setAlignment(Pos.CENTER_LEFT);
        privateKeyRow.setPadding(new Insets(0, 0, 0, 24));

        privateKeyPathField.disableProperty().bind(privateKeyRadio.selectedProperty().not());
        privateKeyBrowseBtn.disableProperty().bind(privateKeyRadio.selectedProperty().not());

        // Subversion config indented rows
        Label tunnelLabel = new Label("SSH tunnel:");
        tunnelLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        tunnelLabel.setPrefWidth(90);

        sshTunnelField.setText(manager.getSshTunnel());
        styleTextField(sshTunnelField, 380);
        HBox.setHgrow(sshTunnelField, Priority.ALWAYS);
        sshTunnelField.textProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) manager.setSshTunnel(newV);
        });

        updateTunnelBtn.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #5A5D63; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 4 14 4 14; -fx-font-size: 12px; -fx-cursor: hand;");
        updateTunnelBtn.setOnAction(e -> {
            manager.updateSshTunnel();
            sshTunnelField.setText(manager.getSshTunnel());
            svnSshEnvField.setText(manager.getSvnSshEnv());
        });

        HBox tunnelRow = new HBox(8, tunnelLabel, sshTunnelField, updateTunnelBtn);
        tunnelRow.setAlignment(Pos.CENTER_LEFT);
        tunnelRow.setPadding(new Insets(0, 0, 0, 24));

        Label svnSshLabel = new Label("SVN_SSH:");
        svnSshLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        svnSshLabel.setPrefWidth(90);

        svnSshEnvField.setText(manager.getSvnSshEnv());
        styleTextField(svnSshEnvField, 380);
        HBox.setHgrow(svnSshEnvField, Priority.ALWAYS);
        svnSshEnvField.textProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) manager.setSvnSshEnv(newV);
        });

        HBox svnSshRow = new HBox(8, svnSshLabel, svnSshEnvField);
        svnSshRow.setAlignment(Pos.CENTER_LEFT);
        svnSshRow.setPadding(new Insets(0, 0, 0, 24));

        sshTunnelField.disableProperty().bind(subversionConfigRadio.selectedProperty().not());
        updateTunnelBtn.disableProperty().bind(subversionConfigRadio.selectedProperty().not());
        svnSshEnvField.disableProperty().bind(subversionConfigRadio.selectedProperty().not());

        group.selectedToggleProperty().addListener((obs, oldV, newV) -> {
            if (newV == passwordRadio) {
                manager.setSshAuthMode(SshAuthMode.PASSWORD);
            } else if (newV == privateKeyRadio) {
                manager.setSshAuthMode(SshAuthMode.PRIVATE_KEY);
            } else {
                manager.setSshAuthMode(SshAuthMode.SUBVERSION_CONFIG);
            }
        });

        box.getChildren().addAll(
                passwordRadio,
                privateKeyRadio,
                privateKeyRow,
                subversionConfigRadio,
                tunnelRow,
                svnSshRow
        );
        return box;
    }

    private Label createFieldLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        label.setPrefWidth(110);
        return label;
    }

    private void initRadio(RadioButton rb, boolean initial) {
        rb.setSelected(initial);
        rb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-cursor: hand;");
    }

    private void styleTextField(TextField tf, double width) {
        tf.setPrefWidth(width);
        tf.setStyle("-fx-background-color: #1E1F22; -fx-text-fill: #DFE1E5; -fx-border-color: #5A5D63; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 4 8 4 8; -fx-font-size: 12px;");
    }

    private void styleFolderBrowseBtn(Button btn, String title, java.util.function.Consumer<File> onFileChosen) {
        SVGPath folderIcon = new SVGPath();
        folderIcon.setContent("M 2 3 L 5 3 L 6.5 5 L 12 5 L 12 11 L 2 11 Z");
        folderIcon.setFill(Color.TRANSPARENT);
        folderIcon.setStroke(Color.web("#848BA3"));
        folderIcon.setStrokeWidth(1.2);
        btn.setGraphic(folderIcon);
        btn.setStyle("-fx-background-color: #1E1F22; -fx-border-color: #5A5D63; -fx-border-radius: 4; -fx-padding: 4 8 4 8; -fx-cursor: hand;");
        btn.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle(title);
            File f = fc.showOpenDialog(getScene().getWindow());
            if (f != null) {
                onFileChosen.accept(f);
            }
        });
    }

    private void syncFromManager() {
        sshExecutableField.setText(manager.getSshExecutablePath());
        userNameField.setText(manager.getSshUserName());
        portSpinner.getValueFactory().setValue(manager.getSshPort());

        switch (manager.getSshAuthMode()) {
            case PASSWORD -> passwordRadio.setSelected(true);
            case PRIVATE_KEY -> privateKeyRadio.setSelected(true);
            default -> subversionConfigRadio.setSelected(true);
        }

        privateKeyPathField.setText(manager.getSshPrivateKeyPath());
        sshTunnelField.setText(manager.getSshTunnel());
        svnSshEnvField.setText(manager.getSvnSshEnv());
    }
}
