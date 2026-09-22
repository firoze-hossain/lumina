package dev.lumina.ui;

import dev.lumina.git.SubversionNetworkOptionsManager;
import dev.lumina.git.SubversionNetworkOptionsManager.FileSource;
import dev.lumina.git.SubversionNetworkOptionsManager.NetworkGroup;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Modal dialog for editing Subversion network options matching IntelliJ IDEA Image 3.
 */
public class EditSubversionNetworkOptionsDialog extends Stage {

    private final SubversionNetworkOptionsManager userManager = new SubversionNetworkOptionsManager();
    private final SubversionNetworkOptionsManager systemManager = new SubversionNetworkOptionsManager();
    private FileSource currentSource = FileSource.USER_FILE;

    // Top Tabs
    private final Button userFileTabBtn = new Button("User file");
    private final Button systemFileTabBtn = new Button("System file");

    // Left List
    private final ListView<String> groupsListView = new ListView<>();
    private final Button addGroupBtn = new Button("+");
    private final Button removeGroupBtn = new Button("−");
    private final Button copyGroupBtn = new Button();

    // Right Fields - HTTP proxy
    private final TextArea urlPatternsArea = new TextArea();
    private final TextArea exceptionsArea = new TextArea();
    private final TextField serverField = new TextField();
    private final TextField userField = new TextField();
    private final TextField portField = new TextField();
    private final PasswordField passwordField = new PasswordField();
    private final TextField timeoutField = new TextField();

    // Right Fields - SSL
    private final TextField caCertFilesField = new TextField();
    private final TextField clientCertFileField = new TextField();
    private final Button clientCertBrowseBtn = new Button();
    private final PasswordField clientCertPassphraseField = new PasswordField();
    private final CheckBox trustDefaultCasCheck = new CheckBox("Trust default CAs");

    // Right Fields - Repositories
    private final Button testConnectionBtn = new Button("Test connection");

    private NetworkGroup currentlyEditingGroup = null;

    public EditSubversionNetworkOptionsDialog(Window owner) {
        initOwner(owner);
        initModality(Modality.APPLICATION_MODAL);
        setTitle("Edit Subversion Options Related to the Network Layers");

        // Load configs from disk
        userManager.loadFromFile(SubversionNetworkOptionsManager.getUserServersPath());
        systemManager.loadFromFile(SubversionNetworkOptionsManager.getSystemServersPath());

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #1E1F22; -fx-font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;");
        root.setPadding(new Insets(12, 16, 12, 16));

        // 1. Top Bar: Tabs
        HBox topBar = buildTopBar();
        root.setTop(topBar);

        // 2. Center: Split into left groups list & right form
        HBox center = buildCenter();
        root.setCenter(center);

        // 3. Bottom Bar: Help + OK / Cancel
        HBox bottomBar = buildBottomBar();
        root.setBottom(bottomBar);

        Scene scene = new Scene(root, 760, 620);
        var darkCss = getClass().getResource("/css/lumina-dark.css");
        if (darkCss != null) {
            scene.getStylesheets().add(darkCss.toExternalForm());
        }

        String inlineDarkCss = """
            .list-view {
                -fx-background-color: transparent;
                -fx-control-inner-background: #1E1F22;
                -fx-background: #1E1F22;
                -fx-border-color: transparent;
            }
            .list-view .virtual-flow, .list-view .clipped-container {
                -fx-background-color: transparent;
            }
            .list-cell {
                -fx-background-color: transparent;
                -fx-text-fill: #DFE1E5;
            }
            .list-cell:empty {
                -fx-background-color: transparent;
            }
            .scroll-pane {
                -fx-background-color: transparent;
                -fx-background: transparent;
            }
            .scroll-pane > .viewport {
                -fx-background-color: transparent;
            }
            .scroll-bar:vertical, .scroll-bar:horizontal {
                -fx-background-color: transparent;
            }
            .scroll-bar:vertical .track, .scroll-bar:horizontal .track {
                -fx-background-color: transparent;
            }
            .scroll-bar:vertical .thumb, .scroll-bar:horizontal .thumb {
                -fx-background-color: #393B40;
                -fx-background-radius: 4;
            }
            .scroll-bar:vertical .thumb:hover, .scroll-bar:horizontal .thumb:hover {
                -fx-background-color: #4E5157;
            }
            .scroll-bar:vertical .increment-button, .scroll-bar:vertical .decrement-button,
            .scroll-bar:horizontal .increment-button, .scroll-bar:horizontal .decrement-button {
                -fx-background-color: transparent;
                -fx-padding: 0;
            }
            .text-area {
                -fx-control-inner-background: #1E1F22;
                -fx-text-fill: #DFE1E5;
            }
            .text-area .content {
                -fx-background-color: #1E1F22;
            }
            .text-area .scroll-pane {
                -fx-background-color: #1E1F22;
            }
        """;
        scene.getStylesheets().add("data:text/css," + java.net.URLEncoder.encode(inlineDarkCss, java.nio.charset.StandardCharsets.UTF_8).replace("+", "%20"));

        setScene(scene);

        // Initial populate
        updateTabStyles();
        populateGroupsList();
    }

    private HBox buildTopBar() {
        userFileTabBtn.setOnAction(e -> switchFileSource(FileSource.USER_FILE));
        systemFileTabBtn.setOnAction(e -> switchFileSource(FileSource.SYSTEM_FILE));

        HBox bar = new HBox(8, userFileTabBtn, systemFileTabBtn);
        bar.setPadding(new Insets(0, 0, 10, 0));
        bar.setAlignment(Pos.CENTER_LEFT);
        return bar;
    }

    private void updateTabStyles() {
        String activeStyle = "-fx-background-color: #3574F0; -fx-text-fill: #FFFFFF; -fx-border-color: #3574F0; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 4 14 4 14; -fx-font-size: 12px; -fx-cursor: hand;";
        String inactiveStyle = "-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 4 14 4 14; -fx-font-size: 12px; -fx-cursor: hand;";

        if (currentSource == FileSource.USER_FILE) {
            userFileTabBtn.setStyle(activeStyle);
            systemFileTabBtn.setStyle(inactiveStyle);
        } else {
            userFileTabBtn.setStyle(inactiveStyle);
            systemFileTabBtn.setStyle(activeStyle);
        }
    }

    private void switchFileSource(FileSource source) {
        if (currentSource != source) {
            saveCurrentGroupForm();
            currentSource = source;
            updateTabStyles();
            populateGroupsList();
        }
    }

    private SubversionNetworkOptionsManager currentManager() {
        return currentSource == FileSource.USER_FILE ? userManager : systemManager;
    }

    private HBox buildCenter() {
        // Left Column: Toolbar + ListView
        VBox leftBox = new VBox(6);
        leftBox.setPrefWidth(150);

        HBox toolbar = new HBox(4);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        styleToolbarBtn(addGroupBtn);
        addGroupBtn.setOnAction(e -> onAddGroup());

        styleToolbarBtn(removeGroupBtn);
        removeGroupBtn.setOnAction(e -> onRemoveGroup());

        SVGPath copyIcon = new SVGPath();
        copyIcon.setContent("M 2 2 L 6 2 L 6 3 L 3 3 L 3 7 L 2 7 Z M 4 4 L 9 4 L 9 9 L 4 9 Z");
        copyIcon.setFill(Color.web("#848BA3"));
        copyGroupBtn.setGraphic(copyIcon);
        styleToolbarBtn(copyGroupBtn);
        copyGroupBtn.setOnAction(e -> onCopyGroup());

        toolbar.getChildren().addAll(addGroupBtn, removeGroupBtn, copyGroupBtn);

        groupsListView.setStyle("-fx-background-color: transparent; -fx-control-inner-background: #1E1F22; -fx-background: #1E1F22; -fx-border-color: transparent; -fx-padding: 2 0 0 0;");
        groupsListView.setPrefHeight(450);
        groupsListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("-fx-background-color: transparent; -fx-padding: 0;");
                } else {
                    setText(item);
                    setAlignment(Pos.CENTER_LEFT);
                    setPadding(new Insets(5, 12, 5, 12));
                    if (isSelected()) {
                        setStyle("-fx-background-color: #393B40; -fx-background-radius: 4; -fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-background-color: transparent; -fx-background-radius: 4; -fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
                    }
                }
            }

            @Override
            public void updateSelected(boolean selected) {
                super.updateSelected(selected);
                if (!isEmpty() && getItem() != null) {
                    if (selected) {
                        setStyle("-fx-background-color: #393B40; -fx-background-radius: 4; -fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-background-color: transparent; -fx-background-radius: 4; -fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
                    }
                }
            }
        });
        groupsListView.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            if (oldV != null) saveGroupForm(oldV);
            if (newV != null) loadGroupForm(newV);
            removeGroupBtn.setDisable("global".equalsIgnoreCase(newV));
        });

        leftBox.getChildren().addAll(toolbar, groupsListView);

        // Right Column: Form sections in ScrollPane
        VBox rightForm = new VBox(10);
        rightForm.setPadding(new Insets(0, 0, 0, 16));

        // 1. HTTP Proxy
        VBox httpSection = buildHttpProxySection();

        // 2. SSL
        VBox sslSection = buildSslSection();

        // 3. Repositories
        VBox repoSection = buildRepositoriesSection();

        rightForm.getChildren().addAll(httpSection, sslSection, repoSection);

        ScrollPane scroll = new ScrollPane(rightForm);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");
        HBox.setHgrow(scroll, Priority.ALWAYS);

        HBox centerBox = new HBox(leftBox, scroll);
        return centerBox;
    }

    private VBox buildHttpProxySection() {
        VBox box = new VBox(6);
        box.getChildren().add(createSectionHeader("HTTP proxy settings"));

        // URL patterns
        Label urlLbl = createFormLabel("URL patterns:");
        styleTextArea(urlPatternsArea, 48);
        HBox urlRow = new HBox(8, urlLbl, urlPatternsArea);
        urlRow.setAlignment(Pos.TOP_LEFT);

        // Exceptions
        Label excLbl = createFormLabel("Exceptions:");
        styleTextArea(exceptionsArea, 48);
        HBox excRow = new HBox(8, excLbl, exceptionsArea);
        excRow.setAlignment(Pos.TOP_LEFT);

        // Server / User
        Label srvLbl = createFormLabel("Server:");
        styleTextField(serverField, 140);
        Label userLbl = createFormLabel("User:");
        userLbl.setPrefWidth(50);
        styleTextField(userField, 140);
        HBox srvUserRow = new HBox(8, srvLbl, serverField, userLbl, userField);
        srvUserRow.setAlignment(Pos.CENTER_LEFT);

        // Port / Password
        Label portLbl = createFormLabel("Port:");
        styleTextField(portField, 140);
        Label passLbl = createFormLabel("Password:");
        passLbl.setPrefWidth(50);
        stylePasswordField(passwordField, 140);
        HBox portPassRow = new HBox(8, portLbl, portField, passLbl, passwordField);
        portPassRow.setAlignment(Pos.CENTER_LEFT);

        // Timeout
        Label timeoutLbl = createFormLabel("Connection timeout:");
        styleTextField(timeoutField, 140);
        Label secLbl = new Label("seconds");
        secLbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        HBox timeoutRow = new HBox(8, timeoutLbl, timeoutField, secLbl);
        timeoutRow.setAlignment(Pos.CENTER_LEFT);

        box.getChildren().addAll(urlRow, excRow, srvUserRow, portPassRow, timeoutRow);
        return box;
    }

    private VBox buildSslSection() {
        VBox box = new VBox(6);
        box.getChildren().add(createSectionHeader("SSL settings"));

        // CA certs
        Label caLbl = new Label("Comma-separated paths to CAs certificate files:");
        caLbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        caLbl.setPrefWidth(260);
        styleTextField(caCertFilesField, 230);
        HBox.setHgrow(caCertFilesField, Priority.ALWAYS);
        HBox caRow = new HBox(8, caLbl, caCertFilesField);
        caRow.setAlignment(Pos.CENTER_LEFT);

        // SSL Client Cert
        Label clientCertLbl = createFormLabel("SSL client certificate file:");
        clientCertLbl.setPrefWidth(260);
        styleTextField(clientCertFileField, 200);
        HBox.setHgrow(clientCertFileField, Priority.ALWAYS);

        styleFolderBrowseBtn(clientCertBrowseBtn, "Select SSL Client Certificate", f -> {
            clientCertFileField.setText(f.getAbsolutePath());
        });
        HBox clientCertRow = new HBox(8, clientCertLbl, clientCertFileField, clientCertBrowseBtn);
        clientCertRow.setAlignment(Pos.CENTER_LEFT);

        // Passphrase & Trust CAs
        Label passLbl = createFormLabel("SSL client certificate passphrase:");
        passLbl.setPrefWidth(260);
        stylePasswordField(clientCertPassphraseField, 130);
        trustDefaultCasCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-cursor: hand;");
        HBox passRow = new HBox(8, passLbl, clientCertPassphraseField, trustDefaultCasCheck);
        passRow.setAlignment(Pos.CENTER_LEFT);

        box.getChildren().addAll(caRow, clientCertRow, passRow);
        return box;
    }

    private VBox buildRepositoriesSection() {
        VBox box = new VBox(6);
        box.getChildren().add(createSectionHeader("Repositories"));

        StackPane repoPlaceholder = new StackPane();
        repoPlaceholder.setStyle("-fx-background-color: #1E1F22; -fx-border-color: #393B40; -fx-border-radius: 4;");
        repoPlaceholder.setPrefHeight(90);

        Label emptyLbl = new Label("Nothing to show");
        emptyLbl.setStyle("-fx-text-fill: #848BA3; -fx-font-size: 12px;");
        repoPlaceholder.getChildren().add(emptyLbl);

        testConnectionBtn.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #848BA3; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 4 14 4 14; -fx-font-size: 12px;");
        testConnectionBtn.setDisable(true);

        HBox btnRow = new HBox(testConnectionBtn);
        btnRow.setAlignment(Pos.CENTER_RIGHT);

        box.getChildren().addAll(repoPlaceholder, btnRow);
        return box;
    }

    private HBox buildBottomBar() {
        Button helpBtn = new Button("?");
        helpBtn.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #5A5D63; -fx-border-radius: 12; -fx-background-radius: 12; -fx-pref-width: 24; -fx-pref-height: 24; -fx-font-size: 12px; -fx-cursor: hand;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button okBtn = new Button("OK");
        okBtn.setStyle("-fx-background-color: #3574F0; -fx-text-fill: #FFFFFF; -fx-border-color: #3574F0; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 4 20 4 20; -fx-font-size: 12px; -fx-cursor: hand;");
        okBtn.setOnAction(e -> onOk());

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #5A5D63; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 4 14 4 14; -fx-font-size: 12px; -fx-cursor: hand;");
        cancelBtn.setOnAction(e -> close());

        HBox bar = new HBox(8, helpBtn, spacer, okBtn, cancelBtn);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(10, 0, 0, 0));
        return bar;
    }

    private void onAddGroup() {
        TextInputDialog d = new TextInputDialog();
        d.initOwner(this);
        d.setTitle("Add Subversion Group");
        d.setHeaderText("Create new network options group");
        d.setContentText("Group name:");
        Optional<String> res = d.showAndWait();
        res.ifPresent(name -> {
            String trimmed = name.trim();
            if (!trimmed.isBlank() && currentManager().addGroup(trimmed)) {
                groupsListView.getItems().add(trimmed);
                groupsListView.getSelectionModel().select(trimmed);
            }
        });
    }

    private void onRemoveGroup() {
        String selected = groupsListView.getSelectionModel().getSelectedItem();
        if (selected != null && !"global".equalsIgnoreCase(selected)) {
            currentManager().removeGroup(selected);
            groupsListView.getItems().remove(selected);
            groupsListView.getSelectionModel().select("global");
        }
    }

    private void onCopyGroup() {
        String selected = groupsListView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            NetworkGroup src = currentManager().getGroup(selected);
            if (src != null) {
                String copyName = selected + "_copy";
                int i = 1;
                while (currentManager().getGroups().containsKey(copyName)) {
                    copyName = selected + "_copy" + (++i);
                }
                NetworkGroup cloned = src.copy(copyName);
                currentManager().getGroups().put(copyName, cloned);
                groupsListView.getItems().add(copyName);
                groupsListView.getSelectionModel().select(copyName);
            }
        }
    }

    private void onOk() {
        saveCurrentGroupForm();
        try {
            userManager.saveToFile(SubversionNetworkOptionsManager.getUserServersPath());
        } catch (IOException ignored) {}
        close();
    }

    private void populateGroupsList() {
        groupsListView.getItems().clear();
        groupsListView.getItems().addAll(currentManager().getGroups().keySet());
        groupsListView.getSelectionModel().select("global");
    }

    private void saveCurrentGroupForm() {
        String sel = groupsListView.getSelectionModel().getSelectedItem();
        if (sel != null) {
            saveGroupForm(sel);
        }
    }

    private void saveGroupForm(String groupName) {
        NetworkGroup grp = currentManager().getGroup(groupName);
        if (grp != null) {
            grp.setUrlPatterns(urlPatternsArea.getText());
            grp.setExceptions(exceptionsArea.getText());
            grp.setServer(serverField.getText());
            grp.setUser(userField.getText());
            grp.setPort(portField.getText());
            grp.setPassword(passwordField.getText());
            grp.setTimeout(timeoutField.getText());
            grp.setCaCertFiles(caCertFilesField.getText());
            grp.setClientCertFile(clientCertFileField.getText());
            grp.setClientCertPassphrase(clientCertPassphraseField.getText());
            grp.setTrustDefaultCas(trustDefaultCasCheck.isSelected());
        }
    }

    private void loadGroupForm(String groupName) {
        NetworkGroup grp = currentManager().getGroup(groupName);
        currentlyEditingGroup = grp;
        if (grp != null) {
            urlPatternsArea.setText(grp.getUrlPatterns());
            exceptionsArea.setText(grp.getExceptions());
            serverField.setText(grp.getServer());
            userField.setText(grp.getUser());
            portField.setText(grp.getPort());
            passwordField.setText(grp.getPassword());
            timeoutField.setText(grp.getTimeout());
            caCertFilesField.setText(grp.getCaCertFiles());
            clientCertFileField.setText(grp.getClientCertFile());
            clientCertPassphraseField.setText(grp.getClientCertPassphrase());
            trustDefaultCasCheck.setSelected(grp.isTrustDefaultCas());

            boolean isGlobal = "global".equalsIgnoreCase(groupName);
            urlPatternsArea.setDisable(isGlobal);
        }
    }

    private Label createFormLabel(String text) {
        Label lbl = new Label(text);
        lbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        lbl.setPrefWidth(120);
        return lbl;
    }

    private HBox createSectionHeader(String title) {
        Label lbl = new Label(title);
        lbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-font-weight: bold;");

        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: #393B40; -fx-border-color: #393B40;");
        HBox.setHgrow(sep, Priority.ALWAYS);

        HBox bar = new HBox(8, lbl, sep);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(4, 0, 2, 0));
        return bar;
    }

    private void styleToolbarBtn(Button btn) {
        btn.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #393B40; -fx-border-radius: 3; -fx-background-radius: 3; -fx-padding: 2 8 2 8; -fx-font-size: 12px; -fx-cursor: hand;");
    }

    private void styleTextField(TextField tf, double width) {
        tf.setPrefWidth(width);
        tf.setStyle("-fx-background-color: #1E1F22; -fx-text-fill: #DFE1E5; -fx-border-color: #5A5D63; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 3 6 3 6; -fx-font-size: 12px;");
    }

    private void stylePasswordField(PasswordField pf, double width) {
        pf.setPrefWidth(width);
        pf.setStyle("-fx-background-color: #1E1F22; -fx-text-fill: #DFE1E5; -fx-border-color: #5A5D63; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 3 6 3 6; -fx-font-size: 12px;");
    }

    private void styleTextArea(TextArea ta, double height) {
        ta.setPrefHeight(height);
        ta.setWrapText(true);
        ta.setStyle("-fx-control-inner-background: #1E1F22; -fx-text-fill: #DFE1E5; -fx-border-color: #5A5D63; -fx-border-radius: 4; -fx-font-size: 12px;");
        HBox.setHgrow(ta, Priority.ALWAYS);
    }

    private void styleFolderBrowseBtn(Button btn, String title, java.util.function.Consumer<File> onFileChosen) {
        SVGPath folderIcon = new SVGPath();
        folderIcon.setContent("M 2 3 L 5 3 L 6.5 5 L 12 5 L 12 11 L 2 11 Z");
        folderIcon.setFill(Color.TRANSPARENT);
        folderIcon.setStroke(Color.web("#848BA3"));
        folderIcon.setStrokeWidth(1.2);
        btn.setGraphic(folderIcon);
        btn.setStyle("-fx-background-color: #1E1F22; -fx-border-color: #5A5D63; -fx-border-radius: 4; -fx-padding: 3 6 3 6; -fx-cursor: hand;");
        btn.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle(title);
            File f = fc.showOpenDialog(this);
            if (f != null) {
                onFileChosen.accept(f);
            }
        });
    }
}
