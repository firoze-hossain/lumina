package dev.lumina.ui;

import dev.lumina.git.GitHubAccountManager;
import dev.lumina.git.GitHubRepositoryService;
import dev.lumina.git.GitHubRepositoryService.GitHubRepository;
import dev.lumina.git.GitLabAccountManager;
import dev.lumina.git.GitService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * "Clone Repository" dialog matching IntelliJ IDEA's exact visual design (media_1790167212147.png).
 * Features:
 * - Left rail with services: Repository URL, GitHub, GitHub Enterprise, GitLab.
 * - Dynamic account names ("firoze-hossain", "No accounts") queried from managers.
 * - Dynamic GitHub repo list with search magnifying glass and circular profile avatar.
 * - Dark theme styled to prevent any white list cells or backgrounds.
 * - Common Directory chooser and Shallow clone controls.
 * - Bottom action bar with Clone and Cancel buttons.
 */
public class CloneRepositoryDialog extends Stage {

    private final Path defaultParentDir;
    private final Map<String, String> gitEnv;
    private final Consumer<Path> onProjectCloned;

    // Left Rail
    private final VBox railList = new VBox(2);
    private String selectedTab = "Repository URL";

    // Center Panel Container
    private final StackPane centerContainer = new StackPane();

    // Shared Controls
    private final ComboBox<String> vcsCombo = new ComboBox<>();
    private final TextField urlField = new TextField();
    private final Label dirLbl = new Label("Directory:");
    private final TextField dirField = new TextField();
    private final HBox dirBox = new HBox(8);
    private final CheckBox shallowCheck = new CheckBox("Shallow clone with a history truncated to");
    private final Spinner<Integer> shallowSpinner = new Spinner<>(1, 1000, 1);
    private final HBox shallowBox = new HBox(8);

    // Active Tab controls for testability
    private ListView<GitHubRepository> activeGitHubRepoList;
    private TextField activeSearchField;

    // Bottom Bar Controls
    private final Button cloneButton = new Button("Clone");
    private final Button cancelButton = new Button("Cancel");
    private final ProgressBar progressBar = new ProgressBar();
    private final Label statusLabel = new Label();

    public CloneRepositoryDialog(Stage owner, Path defaultParentDir, String initialTab,
                                 Map<String, String> gitEnv, Consumer<Path> onProjectCloned) {
        this.defaultParentDir = defaultParentDir != null ? defaultParentDir : Path.of(System.getProperty("user.home"));
        this.gitEnv = gitEnv;
        this.onProjectCloned = onProjectCloned;
        if (initialTab != null && !initialTab.isBlank()) {
            this.selectedTab = initialTab;
        }

        initOwner(owner);
        initModality(Modality.APPLICATION_MODAL);
        initStyle(StageStyle.DECORATED);
        setTitle("Clone Repository");
        setMinWidth(720);
        setMinHeight(460);
        setWidth(780);
        setHeight(500);

        initSharedControls();

        BorderPane root = new BorderPane();
        root.getStyleClass().add("clone-repo-dialog");
        root.setStyle("-fx-background-color: #1E1F22;");

        // ---- Left Sidebar Rail ----
        VBox leftRail = buildLeftRail();
        root.setLeft(leftRail);

        // ---- Center Content ----
        centerContainer.setStyle("-fx-background-color: #1E1F22; -fx-padding: 16 22 14 22;");
        root.setCenter(centerContainer);

        // ---- Bottom Bar ----
        HBox bottomBar = buildBottomBar();
        root.setBottom(bottomBar);

        Scene scene = new Scene(root);
        try {
            var css = getClass().getResource("/css/lumina-dark.css");
            if (css != null) scene.getStylesheets().add(css.toExternalForm());
        } catch (Exception ignored) {}

        setScene(scene);

        // Select initial tab
        selectTab(selectedTab);

        // Wire listener for account managers so subtitle labels stay reactive
        GitHubAccountManager.getInstance().addListener(this::refreshRailItems);
        GitLabAccountManager.getInstance().addListener(this::refreshRailItems);
    }

    private void initSharedControls() {
        // Version control combo
        vcsCombo.getItems().setAll("Git");
        vcsCombo.setValue("Git");
        vcsCombo.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #393B40; -fx-text-fill: #DFE1E5; -fx-background-radius: 4; -fx-border-radius: 4; -fx-pref-width: 140;");

        // URL field
        urlField.setPromptText("https://github.com/user/repository.git");
        urlField.setStyle(
                "-fx-background-color: #1E1F22; -fx-text-fill: #DFE1E5; -fx-border-color: #3574F0; " +
                "-fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 6 10; -fx-font-size: 13px;"
        );
        urlField.textProperty().addListener((obs, oldVal, newVal) -> {
            updateCloneButtonState();
            if (newVal != null && !newVal.isBlank()) {
                String repoName = extractRepoName(newVal.trim());
                if (!repoName.isEmpty()) {
                    dirField.setText(defaultParentDir.resolve(repoName).toAbsolutePath().toString());
                }
            }
        });

        // Directory row
        dirLbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        dirField.setText(defaultParentDir.toAbsolutePath().toString());
        dirField.setStyle(
                "-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #393B40; " +
                "-fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 6 10; -fx-font-size: 13px;"
        );
        dirField.textProperty().addListener((obs, o, n) -> updateCloneButtonState());

        Button browseBtn = new Button();
        browseBtn.setGraphic(GitIcons.folderIcon(16, "#DFE1E5"));
        browseBtn.setTooltip(new Tooltip("Select Directory"));
        browseBtn.setStyle(
                "-fx-background-color: #2B2D30; -fx-border-color: #393B40; -fx-border-radius: 4; " +
                "-fx-background-radius: 4; -fx-padding: 5 9; -fx-cursor: hand;"
        );
        browseBtn.setOnAction(e -> {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("Select Destination Directory");
            File current = new File(dirField.getText().trim());
            if (current.exists() && current.isDirectory()) {
                chooser.setInitialDirectory(current);
            } else if (Files.exists(defaultParentDir)) {
                chooser.setInitialDirectory(defaultParentDir.toFile());
            }
            File chosen = chooser.showDialog(this);
            if (chosen != null) {
                dirField.setText(chosen.getAbsolutePath());
            }
        });

        dirBox.setAlignment(Pos.CENTER_LEFT);
        dirBox.getChildren().setAll(dirField, browseBtn);
        HBox.setHgrow(dirField, Priority.ALWAYS);

        // Shallow clone controls
        shallowCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        shallowSpinner.setPrefWidth(70);
        shallowSpinner.setEditable(true);
        shallowSpinner.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #393B40; -fx-border-radius: 4;");
        shallowSpinner.disableProperty().bind(shallowCheck.selectedProperty().not());

        Label commitsLbl = new Label("commits");
        commitsLbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");

        shallowBox.setAlignment(Pos.CENTER_LEFT);
        shallowBox.setPadding(new Insets(2, 0, 0, 0));
        shallowBox.getChildren().setAll(shallowCheck, shallowSpinner, commitsLbl);
    }

    private void prepareSharedControls() {
        if (dirBox.getParent() instanceof Pane p) p.getChildren().remove(dirBox);
        if (dirLbl.getParent() instanceof Pane p) p.getChildren().remove(dirLbl);
        if (shallowBox.getParent() instanceof Pane p) p.getChildren().remove(shallowBox);
    }

    private VBox buildLeftRail() {
        VBox rail = new VBox();
        rail.setPrefWidth(205);
        rail.setMinWidth(190);
        rail.setMaxWidth(230);
        rail.setStyle("-fx-background-color: #1E1F22; -fx-border-color: transparent #2B2D30 transparent transparent; -fx-border-width: 0 1 0 0; -fx-padding: 8 6 8 8;");

        railList.getChildren().clear();
        rail.getChildren().add(railList);
        VBox.setVgrow(railList, Priority.ALWAYS);

        refreshRailItems();
        return rail;
    }

    private void refreshRailItems() {
        railList.getChildren().clear();

        // 1. Repository URL
        railList.getChildren().add(createRailItem(
                "Repository URL",
                "Repository URL",
                null,
                GitIcons.gitBranchIcon(16, "#3574F0")
        ));

        // 2. GitHub
        var ghDef = GitHubAccountManager.getInstance().getDefaultAccount();
        String ghSubtitle = ghDef != null && !ghDef.getUsername().isBlank() ? ghDef.getUsername() : "No accounts";
        railList.getChildren().add(createRailItem(
                "GitHub",
                "GitHub",
                ghSubtitle,
                GitIcons.gitHubIcon(16, "#DFE1E5")
        ));

        // 3. GitHub Enterprise
        railList.getChildren().add(createRailItem(
                "GitHub Enterprise",
                "GitHub Enterprise",
                "No accounts",
                GitIcons.gitHubIcon(16, "#8C919D")
        ));

        // 4. GitLab
        var glDef = GitLabAccountManager.getInstance().getDefaultAccount();
        String glSubtitle = glDef != null && !glDef.getUsername().isBlank() ? glDef.getUsername() : "No accounts";
        railList.getChildren().add(createRailItem(
                "GitLab",
                "GitLab",
                glSubtitle,
                GitIcons.gitLabIcon(16)
        ));
    }

    private Node createRailItem(String tabKey, String title, String subtitle, Node icon) {
        HBox box = new HBox(10);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(8, 12, 8, 12));
        box.setStyle("-fx-background-radius: 6; -fx-cursor: hand;");

        VBox textBox = new VBox(2);
        Label titleLbl = new Label(title);
        titleLbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: normal;");
        textBox.getChildren().add(titleLbl);

        if (subtitle != null) {
            Label subLbl = new Label(subtitle);
            subLbl.setStyle("-fx-text-fill: #8C919D; -fx-font-size: 11px;");
            textBox.getChildren().add(subLbl);
        }

        HBox.setHgrow(textBox, Priority.ALWAYS);
        box.getChildren().addAll(icon, textBox);

        boolean isSelected = tabKey.equalsIgnoreCase(selectedTab);
        if (isSelected) {
            box.setStyle("-fx-background-color: #2B3C58; -fx-background-radius: 6; -fx-cursor: hand;");
            titleLbl.setStyle("-fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold;");
        } else {
            box.setOnMouseEntered(e -> {
                if (!tabKey.equalsIgnoreCase(selectedTab)) {
                    box.setStyle("-fx-background-color: #2B2D30; -fx-background-radius: 6; -fx-cursor: hand;");
                }
            });
            box.setOnMouseExited(e -> {
                if (!tabKey.equalsIgnoreCase(selectedTab)) {
                    box.setStyle("-fx-background-color: transparent; -fx-background-radius: 6; -fx-cursor: hand;");
                }
            });
        }

        box.setOnMouseClicked(e -> selectTab(tabKey));
        return box;
    }

    private void selectTab(String tabKey) {
        this.selectedTab = tabKey;
        refreshRailItems();

        centerContainer.getChildren().clear();
        switch (tabKey) {
            case "GitHub" -> centerContainer.getChildren().add(buildGitHubView(false));
            case "GitHub Enterprise" -> centerContainer.getChildren().add(buildGitHubView(true));
            case "GitLab" -> centerContainer.getChildren().add(buildGitLabView());
            default -> centerContainer.getChildren().add(buildRepositoryUrlView());
        }
    }

    // ------------------------------------------------------------- Repository URL View
    private Node buildRepositoryUrlView() {
        prepareSharedControls();

        VBox view = new VBox(18);
        view.setAlignment(Pos.TOP_LEFT);

        GridPane grid = new GridPane();
        grid.setHgap(14);
        grid.setVgap(16);

        // 1. Version control
        Label vcsLbl = new Label("Version control:");
        vcsLbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        grid.add(vcsLbl, 0, 0);
        grid.add(vcsCombo, 1, 0);

        // 2. URL
        Label urlLbl = new Label("URL:");
        urlLbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        GridPane.setHgrow(urlField, Priority.ALWAYS);
        grid.add(urlLbl, 0, 1);
        grid.add(urlField, 1, 1);

        // 3. Directory
        dirLbl.setPrefWidth(Region.USE_COMPUTED_SIZE);
        GridPane.setHgrow(dirBox, Priority.ALWAYS);
        grid.add(dirLbl, 0, 2);
        grid.add(dirBox, 1, 2);

        view.getChildren().addAll(grid, shallowBox);
        updateCloneButtonState();
        return view;
    }

    // ------------------------------------------------------------- GitHub View
    private Node buildGitHubView(boolean enterprise) {
        prepareSharedControls();

        var mgr = GitHubAccountManager.getInstance();
        var def = mgr.getDefaultAccount();

        if (def == null || enterprise) {
            // Not logged in view
            VBox box = new VBox(16);
            box.setAlignment(Pos.CENTER);
            box.setPadding(new Insets(40, 20, 20, 20));

            Node icon = GitIcons.gitHubIcon(36, "#8C919D");
            Label noAcc = new Label(enterprise ? "No GitHub Enterprise accounts found" : "No GitHub accounts found");
            noAcc.setStyle("-fx-text-fill: #8C919D; -fx-font-size: 14px;");

            Button loginBtn = new Button(enterprise ? "Log In to GitHub Enterprise..." : "Log In via GitHub...");
            loginBtn.setStyle(
                    "-fx-background-color: #3574F0; -fx-text-fill: white; -fx-font-weight: bold; " +
                    "-fx-background-radius: 4; -fx-padding: 7 18; -fx-cursor: hand; -fx-font-size: 13px;"
            );
            loginBtn.setOnAction(e -> {
                new AddGitHubAccountDialog(this, enterprise).showAndWait();
                refreshRailItems();
                selectTab(enterprise ? "GitHub Enterprise" : "GitHub");
            });

            box.getChildren().addAll(icon, noAcc, loginBtn);
            return box;
        }

        // Logged-in GitHub view (matches IntelliJ IDEA media_1790167212147.png)
        VBox view = new VBox(12);
        view.setAlignment(Pos.TOP_LEFT);

        // 1. Top bar: Search input + User Avatar
        HBox topBar = new HBox(12);
        topBar.setAlignment(Pos.CENTER_LEFT);

        HBox searchBox = new HBox(8);
        searchBox.setAlignment(Pos.CENTER_LEFT);
        searchBox.setStyle(
                "-fx-background-color: #1E1F22; -fx-border-color: #393B40; " +
                "-fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 4 8;"
        );
        HBox.setHgrow(searchBox, Priority.ALWAYS);

        Node searchIcon = GitIcons.searchIcon(13, "#8C919D");
        TextField searchField = new TextField();
        this.activeSearchField = searchField;
        searchField.setPromptText("Search");
        searchField.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: #DFE1E5; " +
                "-fx-prompt-text-fill: #697089; -fx-border-color: transparent; " +
                "-fx-padding: 2 4; -fx-font-size: 13px;"
        );
        HBox.setHgrow(searchField, Priority.ALWAYS);

        searchField.focusedProperty().addListener((obs, oldVal, focused) -> {
            if (focused) {
                searchBox.setStyle(
                        "-fx-background-color: #1E1F22; -fx-border-color: #3574F0; " +
                        "-fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 4 8;"
                );
            } else {
                searchBox.setStyle(
                        "-fx-background-color: #1E1F22; -fx-border-color: #393B40; " +
                        "-fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 4 8;"
                );
            }
        });

        searchBox.getChildren().addAll(searchIcon, searchField);

        Node avatarNode = buildUserAvatar(def.getUsername(), def.getName());
        topBar.getChildren().addAll(searchBox, avatarNode);

        // 2. Repository List
        ObservableList<GitHubRepository> masterRepos = FXCollections.observableArrayList();
        FilteredList<GitHubRepository> filteredRepos = new FilteredList<>(masterRepos, p -> true);

        ListView<GitHubRepository> repoListView = new ListView<>(filteredRepos);
        this.activeGitHubRepoList = repoListView;
        repoListView.getStyleClass().add("clone-repo-list");
        repoListView.setStyle(
                "-fx-background-color: #1E1F22; " +
                "-fx-control-inner-background: #1E1F22; " +
                "-fx-control-inner-background-alt: #1E1F22; " +
                "-fx-border-color: #2B2D30; -fx-border-radius: 4; -fx-background-radius: 4; " +
                "-fx-padding: 0;"
        );
        VBox.setVgrow(repoListView, Priority.ALWAYS);

        repoListView.setCellFactory(lv -> new ListCell<>() {
            {
                setOnMouseEntered(e -> {
                    if (!isEmpty() && !isSelected()) {
                        setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-padding: 5 12; -fx-font-size: 13px;");
                    }
                });
                setOnMouseExited(e -> {
                    if (!isEmpty() && !isSelected()) {
                        setStyle("-fx-background-color: #1E1F22; -fx-text-fill: #DFE1E5; -fx-padding: 5 12; -fx-font-size: 13px;");
                    }
                });
            }

            @Override
            protected void updateItem(GitHubRepository item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("-fx-background-color: #1E1F22;");
                } else {
                    setText(item.name());
                    if (isSelected()) {
                        setTextFill(Color.WHITE);
                        setStyle("-fx-background-color: #2B3C58; -fx-text-fill: white; -fx-padding: 5 12; -fx-font-size: 13px;");
                    } else {
                        setTextFill(Color.web("#DFE1E5"));
                        setStyle("-fx-background-color: #1E1F22; -fx-text-fill: #DFE1E5; -fx-padding: 5 12; -fx-font-size: 13px;");
                    }
                }
            }
        });

        // Populate initially with cached/seed repositories
        masterRepos.setAll(GitHubRepositoryService.getCachedRepositories(def.getUsername()));

        // Asynchronously query GitHub API for live repositories
        GitHubRepositoryService.fetchRepositoriesAsync(def.getUsername(), def.getToken(), liveRepos -> {
            if (liveRepos != null && !liveRepos.isEmpty()) {
                masterRepos.setAll(liveRepos);
            }
        });

        // Search filtering
        searchField.textProperty().addListener((obs, oldVal, text) -> {
            if (text == null || text.isBlank()) {
                filteredRepos.setPredicate(p -> true);
            } else {
                String lower = text.trim().toLowerCase();
                filteredRepos.setPredicate(repo -> repo.name().toLowerCase().contains(lower));
            }
        });

        // Selection listener: sets URL and Directory
        repoListView.getSelectionModel().selectedItemProperty().addListener((obs, oldRepo, newRepo) -> {
            if (newRepo != null) {
                urlField.setText(newRepo.cloneUrl());
                dirField.setText(defaultParentDir.resolve(newRepo.name()).toAbsolutePath().toString());
                updateCloneButtonState();
            }
        });

        // Double-click to clone immediately
        repoListView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                GitHubRepository selected = repoListView.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    urlField.setText(selected.cloneUrl());
                    dirField.setText(defaultParentDir.resolve(selected.name()).toAbsolutePath().toString());
                    executeClone();
                }
            }
        });

        // 3. Directory Row
        HBox dirRow = new HBox(8);
        dirRow.setAlignment(Pos.CENTER_LEFT);
        dirLbl.setPrefWidth(65);
        dirRow.getChildren().addAll(dirLbl, dirBox);
        HBox.setHgrow(dirBox, Priority.ALWAYS);

        // 4. Assemble
        view.getChildren().addAll(topBar, repoListView, dirRow, shallowBox);
        updateCloneButtonState();
        return view;
    }

    private Node buildUserAvatar(String username, String displayName) {
        StackPane avatarPane = new StackPane();
        avatarPane.setMinSize(26, 26);
        avatarPane.setPrefSize(26, 26);
        avatarPane.setMaxSize(26, 26);

        Circle bgCircle = new Circle(13);
        bgCircle.setFill(Color.web("#3574F0"));

        String initial = "U";
        if (displayName != null && !displayName.isBlank()) {
            initial = displayName.substring(0, 1).toUpperCase();
        } else if (username != null && !username.isBlank()) {
            initial = username.substring(0, 1).toUpperCase();
        }
        Label initialLbl = new Label(initial);
        initialLbl.setStyle("-fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: bold;");

        avatarPane.getChildren().addAll(bgCircle, initialLbl);

        if (username != null && !username.isBlank()) {
            try {
                ImageView imgView = new ImageView();
                imgView.setFitWidth(26);
                imgView.setFitHeight(26);
                Circle clip = new Circle(13, 13, 13);
                imgView.setClip(clip);

                Image img = new Image("https://github.com/" + username + ".png", 52, 52, true, true, true);
                img.progressProperty().addListener((obs, oldVal, progress) -> {
                    if (progress.doubleValue() >= 1.0 && !img.isError()) {
                        imgView.setImage(img);
                        avatarPane.getChildren().setAll(imgView);
                    }
                });
            } catch (Exception ignored) {}
        }

        Tooltip tip = new Tooltip(username + (displayName != null && !displayName.isBlank() ? " (" + displayName + ")" : ""));
        Tooltip.install(avatarPane, tip);

        ContextMenu menu = new ContextMenu();
        MenuItem switchItem = new MenuItem("Add / Switch GitHub Account...");
        switchItem.setOnAction(e -> {
            new AddGitHubAccountDialog(this, false).showAndWait();
            refreshRailItems();
            selectTab("GitHub");
        });
        MenuItem logOutItem = new MenuItem("Log Out");
        logOutItem.setOnAction(e -> {
            var def = GitHubAccountManager.getInstance().getDefaultAccount();
            if (def != null) {
                GitHubAccountManager.getInstance().removeAccount(def);
                refreshRailItems();
                selectTab("GitHub");
            }
        });
        menu.getItems().addAll(switchItem, logOutItem);

        avatarPane.setOnMouseClicked(e -> menu.show(avatarPane, e.getScreenX(), e.getScreenY()));
        avatarPane.setStyle("-fx-cursor: hand;");

        return avatarPane;
    }

    // ------------------------------------------------------------- GitLab View
    private Node buildGitLabView() {
        prepareSharedControls();

        var mgr = GitLabAccountManager.getInstance();
        var def = mgr.getDefaultAccount();

        if (def == null) {
            VBox box = new VBox(16);
            box.setAlignment(Pos.CENTER);
            box.setPadding(new Insets(40, 20, 20, 20));

            Node icon = GitIcons.gitLabIcon(36);
            Label noAcc = new Label("No GitLab accounts found");
            noAcc.setStyle("-fx-text-fill: #8C919D; -fx-font-size: 14px;");

            Button loginBtn = new Button("Log In to GitLab...");
            loginBtn.setStyle(
                    "-fx-background-color: #3574F0; -fx-text-fill: white; -fx-font-weight: bold; " +
                    "-fx-background-radius: 4; -fx-padding: 7 18; -fx-cursor: hand; -fx-font-size: 13px;"
            );
            loginBtn.setOnAction(e -> {
                new AddGitLabAccountDialog(this).showAndWait();
                refreshRailItems();
                selectTab("GitLab");
            });

            box.getChildren().addAll(icon, noAcc, loginBtn);
            return box;
        }

        VBox view = new VBox(12);
        view.setAlignment(Pos.TOP_LEFT);

        HBox topBar = new HBox(12);
        topBar.setAlignment(Pos.CENTER_LEFT);

        HBox searchBox = new HBox(8);
        searchBox.setAlignment(Pos.CENTER_LEFT);
        searchBox.setStyle(
                "-fx-background-color: #1E1F22; -fx-border-color: #393B40; " +
                "-fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 4 8;"
        );
        HBox.setHgrow(searchBox, Priority.ALWAYS);

        Node searchIcon = GitIcons.searchIcon(13, "#8C919D");
        TextField searchField = new TextField();
        searchField.setPromptText("Search");
        searchField.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: #DFE1E5; " +
                "-fx-prompt-text-fill: #697089; -fx-border-color: transparent; " +
                "-fx-padding: 2 4; -fx-font-size: 13px;"
        );
        HBox.setHgrow(searchField, Priority.ALWAYS);
        searchBox.getChildren().addAll(searchIcon, searchField);

        Node avatarNode = buildUserAvatar(def.getUsername(), def.getName());
        topBar.getChildren().addAll(searchBox, avatarNode);

        ObservableList<String> allProjects = FXCollections.observableArrayList(
                "gitlab-ci-pipeline",
                "microservices-backend"
        );
        FilteredList<String> filtered = new FilteredList<>(allProjects, p -> true);

        ListView<String> projectList = new ListView<>(filtered);
        projectList.getStyleClass().add("clone-repo-list");
        projectList.setStyle(
                "-fx-background-color: #1E1F22; " +
                "-fx-control-inner-background: #1E1F22; " +
                "-fx-control-inner-background-alt: #1E1F22; " +
                "-fx-border-color: #2B2D30; -fx-border-radius: 4; -fx-background-radius: 4; " +
                "-fx-padding: 0;"
        );
        VBox.setVgrow(projectList, Priority.ALWAYS);

        projectList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("-fx-background-color: #1E1F22;");
                } else {
                    setText(item);
                    if (isSelected()) {
                        setTextFill(Color.WHITE);
                        setStyle("-fx-background-color: #2B3C58; -fx-text-fill: white; -fx-padding: 5 12; -fx-font-size: 13px;");
                    } else {
                        setTextFill(Color.web("#DFE1E5"));
                        setStyle("-fx-background-color: #1E1F22; -fx-text-fill: #DFE1E5; -fx-padding: 5 12; -fx-font-size: 13px;");
                    }
                }
            }
        });

        searchField.textProperty().addListener((obs, o, text) -> {
            if (text == null || text.isBlank()) filtered.setPredicate(p -> true);
            else filtered.setPredicate(p -> p.toLowerCase().contains(text.trim().toLowerCase()));
        });

        projectList.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            if (n != null) {
                urlField.setText("https://gitlab.com/" + def.getUsername() + "/" + n + ".git");
                dirField.setText(defaultParentDir.resolve(n).toAbsolutePath().toString());
                updateCloneButtonState();
            }
        });

        projectList.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                String selected = projectList.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    urlField.setText("https://gitlab.com/" + def.getUsername() + "/" + selected + ".git");
                    dirField.setText(defaultParentDir.resolve(selected).toAbsolutePath().toString());
                    executeClone();
                }
            }
        });

        HBox dirRow = new HBox(8);
        dirRow.setAlignment(Pos.CENTER_LEFT);
        dirLbl.setPrefWidth(65);
        dirRow.getChildren().addAll(dirLbl, dirBox);
        HBox.setHgrow(dirBox, Priority.ALWAYS);

        view.getChildren().addAll(topBar, projectList, dirRow, shallowBox);
        updateCloneButtonState();
        return view;
    }

    // ------------------------------------------------------------- Bottom Action Bar
    private HBox buildBottomBar() {
        HBox bar = new HBox(10);
        bar.setPadding(new Insets(10, 18, 12, 18));
        bar.setAlignment(Pos.CENTER_RIGHT);
        bar.setStyle("-fx-background-color: #1E1F22; -fx-border-color: #2B2D30 transparent transparent transparent; -fx-border-width: 1 0 0 0;");

        progressBar.setVisible(false);
        progressBar.setPrefWidth(120);

        statusLabel.setStyle("-fx-text-fill: #8C919D; -fx-font-size: 12px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        cloneButton.setDefaultButton(true);
        cloneButton.setDisable(true);
        cloneButton.setMinWidth(76);
        cloneButton.setStyle(
                "-fx-background-color: #393B40; -fx-text-fill: #8C919D; " +
                "-fx-background-radius: 4; -fx-padding: 5 18;"
        );
        cloneButton.setOnAction(e -> executeClone());

        cancelButton.setCancelButton(true);
        cancelButton.setMinWidth(76);
        cancelButton.setStyle(
                "-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #393B40; " +
                "-fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 5 16; -fx-cursor: hand;"
        );
        cancelButton.setOnAction(e -> close());

        bar.getChildren().addAll(statusLabel, progressBar, spacer, cloneButton, cancelButton);
        return bar;
    }

    private void updateCloneButtonState() {
        String url = urlField.getText().trim();
        String dir = dirField.getText().trim();
        boolean valid = !url.isEmpty() && !dir.isEmpty();
        cloneButton.setDisable(!valid);
        if (valid) {
            cloneButton.setStyle(
                    "-fx-background-color: #3574F0; -fx-text-fill: white; -fx-font-weight: bold; " +
                    "-fx-background-radius: 4; -fx-padding: 5 18; -fx-cursor: hand;"
            );
        } else {
            cloneButton.setStyle(
                    "-fx-background-color: #393B40; -fx-text-fill: #8C919D; " +
                    "-fx-background-radius: 4; -fx-padding: 5 18;"
            );
        }
    }

    public static String extractRepoName(String url) {
        if (url == null || url.isBlank()) return "";
        String s = url.trim();
        if (s.endsWith("/")) s = s.substring(0, s.length() - 1);
        if (s.endsWith(".git")) s = s.substring(0, s.length() - 4);
        int slash = Math.max(s.lastIndexOf('/'), s.lastIndexOf(':'));
        if (slash >= 0 && slash < s.length() - 1) {
            return s.substring(slash + 1);
        }
        return s;
    }

    public TextField getUrlField() { return urlField; }
    public TextField getDirField() { return dirField; }
    public CheckBox getShallowCheck() { return shallowCheck; }
    public Spinner<Integer> getShallowSpinner() { return shallowSpinner; }
    public Button getCloneButton() { return cloneButton; }
    public Button getCancelButton() { return cancelButton; }
    public String getSelectedTab() { return selectedTab; }
    public ListView<GitHubRepository> getActiveGitHubRepoList() { return activeGitHubRepoList; }
    public TextField getActiveSearchField() { return activeSearchField; }

    private void executeClone() {
        String url = urlField.getText().trim();
        String dirStr = dirField.getText().trim();
        if (url.isEmpty() || dirStr.isEmpty()) return;

        Path target = Path.of(dirStr);
        if (Files.exists(target) && Files.isDirectory(target)) {
            try (var entries = Files.list(target)) {
                if (entries.findAny().isPresent()) {
                    Alert warn = new Alert(Alert.AlertType.CONFIRMATION);
                    warn.initOwner(this);
                    warn.setTitle("Directory Not Empty");
                    warn.setHeaderText("Destination directory already exists and is not empty.");
                    warn.setContentText(target.toString() + "\n\nDo you want to continue?");
                    var res = warn.showAndWait();
                    if (res.isEmpty() || res.get() != ButtonType.OK) return;
                }
            } catch (Exception ignored) {}
        }

        Path parent = target.getParent() != null ? target.getParent() : defaultParentDir;
        String dirName = target.getFileName().toString();
        Integer depth = shallowCheck.isSelected() ? shallowSpinner.getValue() : null;

        cloneButton.setDisable(true);
        cancelButton.setDisable(true);
        progressBar.setVisible(true);
        statusLabel.setText("Cloning repository...");

        Thread t = new Thread(() -> {
            try {
                if (!Files.exists(parent)) {
                    Files.createDirectories(parent);
                }
            } catch (Exception ignored) {}

            GitService.Result res = GitService.cloneRepo(parent, url, dirName, depth, gitEnv);

            Platform.runLater(() -> {
                progressBar.setVisible(false);
                if (res.ok()) {
                    close();
                    if (onProjectCloned != null) {
                        onProjectCloned.accept(target);
                    }
                } else {
                    cloneButton.setDisable(false);
                    cancelButton.setDisable(false);
                    statusLabel.setText("");
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.initOwner(this);
                    alert.setTitle("Clone Failed");
                    alert.setHeaderText("Failed to clone repository");
                    alert.setContentText(res.output());
                    alert.showAndWait();
                }
            });
        }, "lumina-git-clone");
        t.setDaemon(true);
        t.start();
    }
}
