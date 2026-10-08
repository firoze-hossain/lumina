package dev.lumina.ui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.stage.Window;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Dialog for choosing an annotation or pattern to exclude from test coverage in Lumina IDE.
 * Faithfully matches reference IDE design and behavior.
 * Supports "Search by Name" with live filtering and "Project" hierarchy browsing.
 */
public class ChooseExcludeAnnotationDialog extends Dialog<String> {

    public static class AnnotationItem {
        private final String simpleName;
        private final String packageName;
        private final boolean inProject;
        private final boolean property;

        public AnnotationItem(String simpleName, String packageName, boolean inProject, boolean property) {
            this.simpleName = simpleName;
            this.packageName = packageName;
            this.inProject = inProject;
            this.property = property;
        }

        public String getSimpleName() {
            return simpleName;
        }

        public String getPackageName() {
            return packageName;
        }

        public boolean isInProject() {
            return inProject;
        }

        public boolean isProperty() {
            return property;
        }

        public String getQualifiedName() {
            return packageName == null || packageName.isEmpty() ? simpleName : packageName + "." + simpleName;
        }

        @Override
        public String toString() {
            if (packageName == null || packageName.isEmpty()) {
                return simpleName;
            }
            return simpleName + " of " + packageName;
        }
    }

    private final Button searchByNameBtn = new Button("Search by Name");
    private final Button projectBtn = new Button("Project");
    private final Label matchesLabel = new Label("No matches found in project");

    private final TextField searchField = new TextField();
    private final ListView<AnnotationItem> annotationListView = new ListView<>();
    private final TreeView<String> projectTreeView = new TreeView<>();

    private final StackPane contentStack = new StackPane();
    private final VBox searchByNameView = new VBox(6);
    private final VBox projectView = new VBox(6);

    private final ObservableList<AnnotationItem> allAnnotations = FXCollections.observableArrayList();
    private final ObservableList<AnnotationItem> filteredAnnotations = FXCollections.observableArrayList();

    public ChooseExcludeAnnotationDialog() {
        this(null);
    }

    public ChooseExcludeAnnotationDialog(Window owner) {
        setTitle("Choose Exclude Annotation");
        if (owner != null) {
            initOwner(owner);
        }

        DialogPane dialogPane = getDialogPane();
        dialogPane.setPrefSize(520, 440);
        dialogPane.setMinSize(460, 380);
        dialogPane.setStyle("-fx-background-color: #1E1F22; -fx-border-color: #43454A; -fx-padding: 10;");
        if (getClass().getResource("/css/lumina-dark.css") != null) {
            dialogPane.getStylesheets().add(getClass().getResource("/css/lumina-dark.css").toExternalForm());
        }

        initAnnotationCatalog();
        scanWorkspaceAnnotations();

        setupHeaderTabs();
        setupSearchByNameView();
        setupProjectView();
        setupDialogButtons();

        contentStack.getChildren().addAll(projectView, searchByNameView);
        VBox.setVgrow(contentStack, Priority.ALWAYS);

        VBox root = new VBox(10);
        root.getChildren().addAll(createTopBar(), contentStack);
        dialogPane.setContent(root);

        switchToSearchTab();
        filter("");

        setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                return resolveResult();
            }
            return null;
        });
    }

    private HBox createTopBar() {
        HBox topBar = new HBox(8);
        topBar.setAlignment(Pos.CENTER_LEFT);

        HBox tabSegment = new HBox(2, searchByNameBtn, projectBtn);
        tabSegment.setAlignment(Pos.CENTER_LEFT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        matchesLabel.setStyle("-fx-text-fill: #8C9099; -fx-font-size: 12px;");

        topBar.getChildren().addAll(tabSegment, spacer, matchesLabel);
        return topBar;
    }

    private void setupHeaderTabs() {
        searchByNameBtn.setOnAction(e -> switchToSearchTab());
        projectBtn.setOnAction(e -> switchToProjectTab());
    }

    public void switchToSearchTab() {
        searchByNameBtn.setStyle("-fx-background-color: #3574F0; -fx-text-fill: #FFFFFF; -fx-font-weight: bold; " +
                "-fx-background-radius: 4; -fx-padding: 4 12; -fx-font-size: 12px; -fx-cursor: hand;");
        projectBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #8C9099; -fx-font-weight: normal; " +
                "-fx-padding: 4 12; -fx-font-size: 12px; -fx-cursor: hand;");

        searchByNameView.setVisible(true);
        projectView.setVisible(false);
        searchByNameView.toFront();
        searchField.requestFocus();
    }

    public void switchToProjectTab() {
        searchByNameBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #8C9099; -fx-font-weight: normal; " +
                "-fx-padding: 4 12; -fx-font-size: 12px; -fx-cursor: hand;");
        projectBtn.setStyle("-fx-background-color: #3574F0; -fx-text-fill: #FFFFFF; -fx-font-weight: bold; " +
                "-fx-background-radius: 4; -fx-padding: 4 12; -fx-font-size: 12px; -fx-cursor: hand;");

        searchByNameView.setVisible(false);
        projectView.setVisible(true);
        projectView.toFront();
        projectTreeView.requestFocus();
    }

    private void setupSearchByNameView() {
        // Search Input Bar with magnifying glass
        HBox searchBox = new HBox(6);
        searchBox.setAlignment(Pos.CENTER_LEFT);
        searchBox.setPadding(new Insets(3, 8, 3, 8));
        searchBox.setStyle("-fx-background-color: #1E1F22; -fx-border-color: #3574F0; -fx-border-radius: 4; -fx-background-radius: 4;");

        Label searchIcon = new Label("🔍");
        searchIcon.setStyle("-fx-text-fill: #8C9099; -fx-font-size: 11px;");

        searchField.setStyle("-fx-background-color: transparent; -fx-text-fill: #DFE1E5; -fx-prompt-text-fill: #707278; -fx-font-size: 13px; -fx-padding: 2;");
        HBox.setHgrow(searchField, Priority.ALWAYS);

        searchField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                searchBox.setStyle("-fx-background-color: #1E1F22; -fx-border-color: #3574F0; -fx-border-radius: 4; -fx-background-radius: 4;");
            } else {
                searchBox.setStyle("-fx-background-color: #1E1F22; -fx-border-color: #43454A; -fx-border-radius: 4; -fx-background-radius: 4;");
            }
        });

        searchField.textProperty().addListener((obs, oldVal, newVal) -> filter(newVal));
        searchField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                confirmAndClose();
            } else if (e.getCode() == KeyCode.DOWN) {
                annotationListView.requestFocus();
                if (annotationListView.getSelectionModel().getSelectedIndex() < 0 && !filteredAnnotations.isEmpty()) {
                    annotationListView.getSelectionModel().select(0);
                }
            }
        });

        searchBox.getChildren().addAll(searchIcon, searchField);

        // Annotation List View
        annotationListView.setItems(filteredAnnotations);
        annotationListView.setStyle("-fx-background-color: #1E1F22; -fx-control-inner-background: #1E1F22; " +
                "-fx-border-color: #43454A; -fx-border-radius: 4; -fx-background-radius: 4;");
        VBox.setVgrow(annotationListView, Priority.ALWAYS);

        annotationListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(AnnotationItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    Label icon = new Label("@");
                    if (item.isProperty()) {
                        icon.setStyle("-fx-background-color: #3B2E58; -fx-text-fill: #B388FF; -fx-font-size: 11px; " +
                                "-fx-font-weight: bold; -fx-padding: 1 4; -fx-background-radius: 3;");
                    } else {
                        icon.setStyle("-fx-background-color: #233529; -fx-text-fill: #59A869; -fx-font-size: 11px; " +
                                "-fx-font-weight: bold; -fx-padding: 1 4; -fx-background-radius: 3;");
                    }

                    Label nameLbl = new Label(item.getSimpleName());
                    nameLbl.setStyle("-fx-text-fill: " + (isSelected() ? "#FFFFFF" : "#DFE1E5") + "; -fx-font-weight: bold; -fx-font-size: 13px;");

                    Label pkgLbl = new Label(" of " + item.getPackageName());
                    pkgLbl.setStyle("-fx-text-fill: " + (isSelected() ? "#C0C8D8" : "#8C9099") + "; -fx-font-size: 13px;");

                    HBox row = new HBox(6, icon, nameLbl, pkgLbl);
                    row.setAlignment(Pos.CENTER_LEFT);
                    setGraphic(row);
                    setText(null);

                    if (isSelected()) {
                        setStyle("-fx-background-color: #2E436E; -fx-padding: 3 6;");
                    } else {
                        setStyle("-fx-background-color: transparent; -fx-padding: 3 6;");
                    }
                }
            }
        });

        annotationListView.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2) {
                confirmAndClose();
            }
        });

        annotationListView.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                confirmAndClose();
            }
        });

        searchByNameView.getChildren().addAll(searchBox, annotationListView);
        VBox.setVgrow(searchByNameView, Priority.ALWAYS);
    }

    private void setupProjectView() {
        projectTreeView.setShowRoot(false);
        projectTreeView.setStyle("-fx-background-color: #1E1F22; -fx-control-inner-background: #1E1F22; " +
                "-fx-border-color: #43454A; -fx-border-radius: 4; -fx-background-radius: 4;");
        VBox.setVgrow(projectTreeView, Priority.ALWAYS);

        TreeItem<String> root = new TreeItem<>("Root");

        // 1. lumina [lumina-ide] module node
        String userDir = System.getProperty("user.dir", ".");
        String projectName = new File(userDir).getName();
        String rootLabel = projectName + " [" + projectName + "-ide] ~" + formatPath(userDir);
        TreeItem<String> projectNode = new TreeItem<>(rootLabel);
        Label projIcon = new Label("📁");
        projIcon.setStyle("-fx-font-size: 11px;");
        projectNode.setGraphic(projIcon);
        projectNode.setExpanded(true);

        TreeItem<String> srcNode = new TreeItem<>("src");
        srcNode.setGraphic(new Label("📁"));
        TreeItem<String> mainNode = new TreeItem<>("main/java");
        mainNode.setGraphic(new Label("📁"));
        srcNode.getChildren().add(mainNode);
        projectNode.getChildren().add(srcNode);

        // Add any project discovered annotations under projectNode
        for (AnnotationItem item : allAnnotations) {
            if (item.isInProject()) {
                TreeItem<String> annItem = new TreeItem<>("@" + item.getSimpleName());
                Label annIcon = new Label("@");
                annIcon.setStyle("-fx-text-fill: #59A869; -fx-font-weight: bold; -fx-font-size: 11px;");
                annItem.setGraphic(annIcon);
                mainNode.getChildren().add(annItem);
            }
        }

        // 2. External Libraries node
        TreeItem<String> extLibNode = new TreeItem<>("External Libraries");
        Label libIcon = new Label("📚");
        libIcon.setStyle("-fx-font-size: 11px;");
        extLibNode.setGraphic(libIcon);
        extLibNode.setExpanded(false);

        TreeItem<String> javaJdkNode = new TreeItem<>("< Java 25 >");
        javaJdkNode.setGraphic(new Label("☕"));
        extLibNode.getChildren().add(javaJdkNode);

        String[] sampleLibs = {
                "Maven: org.checkerframework:checker-qual:3.42.0",
                "Maven: org.junit.jupiter:junit-jupiter-api:5.10.0",
                "Maven: com.google.guava:guava:33.0.0-jre",
                "Maven: com.github.javaparser:javaparser-core:3.25.8",
                "Maven: org.projectlombok:lombok:1.18.34"
        };
        for (String lib : sampleLibs) {
            TreeItem<String> libItem = new TreeItem<>(lib);
            libItem.setGraphic(new Label("📦"));
            extLibNode.getChildren().add(libItem);
        }

        // 3. Scratches and Consoles node
        TreeItem<String> scratchesNode = new TreeItem<>("Scratches and Consoles");
        Label scratchIcon = new Label("📄");
        scratchIcon.setStyle("-fx-font-size: 11px;");
        scratchesNode.setGraphic(scratchIcon);
        scratchesNode.getChildren().add(new TreeItem<>("Scratches"));
        scratchesNode.getChildren().add(new TreeItem<>("Extensions"));

        root.getChildren().addAll(projectNode, extLibNode, scratchesNode);
        projectTreeView.setRoot(root);

        projectTreeView.setCellFactory(tv -> new TreeCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    setText(item);
                    if (isSelected()) {
                        setStyle("-fx-background-color: #2E436E; -fx-text-fill: #FFFFFF; -fx-padding: 3 6;");
                    } else {
                        setStyle("-fx-background-color: transparent; -fx-text-fill: #DFE1E5; -fx-padding: 3 6;");
                    }
                }
            }
        });

        projectTreeView.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2) {
                confirmAndClose();
            }
        });

        projectView.getChildren().add(projectTreeView);
        VBox.setVgrow(projectView, Priority.ALWAYS);
    }

    private String formatPath(String fullPath) {
        String home = System.getProperty("user.home", "");
        if (!home.isEmpty() && fullPath.startsWith(home)) {
            return fullPath.substring(home.length());
        }
        return fullPath;
    }

    private void setupDialogButtons() {
        DialogPane pane = getDialogPane();
        pane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Button okBtn = (Button) pane.lookupButton(ButtonType.OK);
        if (okBtn != null) {
            okBtn.setText("OK");
            okBtn.setDefaultButton(true);
            okBtn.setStyle("-fx-background-color: #3574F0; -fx-text-fill: #FFFFFF; -fx-font-weight: bold; " +
                    "-fx-background-radius: 4; -fx-padding: 5 18; -fx-font-size: 13px; -fx-cursor: hand;");
        }

        Button cancelBtn = (Button) pane.lookupButton(ButtonType.CANCEL);
        if (cancelBtn != null) {
            cancelBtn.setText("Cancel");
            cancelBtn.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #43454A; " +
                    "-fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 5 18; -fx-font-size: 13px; -fx-cursor: hand;");
        }
    }

    public void filter(String query) {
        filteredAnnotations.clear();
        String q = query == null ? "" : query.trim();

        if (q.isEmpty()) {
            filteredAnnotations.addAll(allAnnotations);
        } else {
            String patternString = q.replace(".", "\\.").replace("*", ".*").replace("?", ".");
            Pattern pat = null;
            try {
                pat = Pattern.compile(patternString, Pattern.CASE_INSENSITIVE);
            } catch (Exception ignored) {}

            for (AnnotationItem item : allAnnotations) {
                boolean matches = false;
                if (pat != null) {
                    matches = pat.matcher(item.getSimpleName()).find() || pat.matcher(item.getQualifiedName()).find();
                } else {
                    matches = item.getSimpleName().toLowerCase().contains(q.toLowerCase())
                            || item.getQualifiedName().toLowerCase().contains(q.toLowerCase());
                }
                if (matches) {
                    filteredAnnotations.add(item);
                }
            }
        }

        long projectMatches = filteredAnnotations.stream().filter(AnnotationItem::isInProject).count();
        if (projectMatches == 0) {
            matchesLabel.setText("No matches found in project");
        } else if (projectMatches == 1) {
            matchesLabel.setText("1 match found in project");
        } else {
            matchesLabel.setText(projectMatches + " matches found in project");
        }

        if (!filteredAnnotations.isEmpty()) {
            annotationListView.getSelectionModel().select(0);
        }
    }

    private String resolveResult() {
        String text = searchField.getText() != null ? searchField.getText().trim() : "";
        if (searchByNameView.isVisible()) {
            AnnotationItem selected = annotationListView.getSelectionModel().getSelectedItem();
            if (selected != null && (text.isEmpty() || text.equalsIgnoreCase(selected.getSimpleName()))) {
                return selected.getSimpleName();
            }
            if (!text.isEmpty()) {
                return text;
            }
            if (selected != null) {
                return selected.getSimpleName();
            }
        } else {
            TreeItem<String> sel = projectTreeView.getSelectionModel().getSelectedItem();
            if (sel != null && sel.getValue() != null) {
                String val = sel.getValue();
                if (val.startsWith("@")) {
                    return val.substring(1);
                }
                return val;
            }
        }
        return text.isEmpty() ? null : text;
    }

    private void confirmAndClose() {
        String res = resolveResult();
        if (res != null && !res.isBlank()) {
            setResult(res);
            close();
        }
    }

    private void initAnnotationCatalog() {
        // Built-in catalog faithfully matching reference IDE annotations (from Image 4)
        List<AnnotationItem> catalog = List.of(
                new AnnotationItem("A", "org.checkerframework.checker.units.qual", false, false),
                new AnnotationItem("Acceleration", "org.checkerframework.checker.units.qual", false, false),
                new AnnotationItem("AccessibleLateinitPropertyLiteral", "kotlin.internal", false, true),
                new AnnotationItem("AfterAll", "org.junit.jupiter.api", false, false),
                new AnnotationItem("AfterEach", "org.junit.jupiter.api", false, false),
                new AnnotationItem("AggregateWith", "org.junit.jupiter.params.aggregator", false, false),
                new AnnotationItem("AllFieldsConstructor", "com.github.javaparser.ast", false, false),
                new AnnotationItem("AllowConcurrentEvents", "com.google.common.eventbus", false, false),
                new AnnotationItem("AlwaysSafe", "org.checkerframework.checker.guieffect.qual", false, false),
                new AnnotationItem("Angle", "org.checkerframework.checker.units.qual", false, false),
                new AnnotationItem("AnnotatedFor", "org.checkerframework.framework.qual", false, false),
                new AnnotationItem("Area", "org.checkerframework.checker.units.qual", false, false),
                new AnnotationItem("AssertFalse", "jakarta.validation.constraints", false, false),
                new AnnotationItem("AssertTrue", "jakarta.validation.constraints", false, false),
                new AnnotationItem("Async", "org.springframework.scheduling.annotation", false, false),
                new AnnotationItem("Autowired", "org.springframework.beans.factory.annotation", false, false),
                new AnnotationItem("BeforeAll", "org.junit.jupiter.api", false, false),
                new AnnotationItem("BeforeEach", "org.junit.jupiter.api", false, false),
                new AnnotationItem("Benchmark", "org.openjdk.jmh.annotations", false, false),
                new AnnotationItem("Builder", "lombok", false, false),
                new AnnotationItem("Data", "lombok", false, false),
                new AnnotationItem("Default", "org.checkerframework.checker.nullness.qual", false, false),
                new AnnotationItem("Deprecated", "java.lang", false, false),
                new AnnotationItem("Documented", "java.lang.annotation", false, false),
                new AnnotationItem("Generated", "javax.annotation.processing", false, false),
                new AnnotationItem("Getter", "lombok", false, false),
                new AnnotationItem("Id", "jakarta.persistence", false, false),
                new AnnotationItem("Inherited", "java.lang.annotation", false, false),
                new AnnotationItem("NonNull", "org.checkerframework.checker.nullness.qual", false, false),
                new AnnotationItem("Nullable", "org.checkerframework.checker.nullness.qual", false, false),
                new AnnotationItem("Override", "java.lang", false, false),
                new AnnotationItem("Param", "org.junit.jupiter.params.provider", false, false),
                new AnnotationItem("ParameterizedTest", "org.junit.jupiter.params", false, false),
                new AnnotationItem("Retention", "java.lang.annotation", false, false),
                new AnnotationItem("SafeVarargs", "java.lang", false, false),
                new AnnotationItem("Setter", "lombok", false, false),
                new AnnotationItem("SneakyThrows", "lombok", false, false),
                new AnnotationItem("SuppressWarnings", "java.lang", false, false),
                new AnnotationItem("Target", "java.lang.annotation", false, false),
                new AnnotationItem("Test", "org.junit.jupiter.api", false, false),
                new AnnotationItem("Value", "lombok", false, false),
                new AnnotationItem("VisibleForTesting", "com.google.common.annotations", false, false)
        );

        allAnnotations.addAll(catalog);
    }

    private void scanWorkspaceAnnotations() {
        String baseDir = System.getProperty("user.dir", ".");
        File srcDir = new File(baseDir, "src");
        if (!srcDir.exists() || !srcDir.isDirectory()) {
            return;
        }

        Pattern pkgPat = Pattern.compile("^\\s*package\\s+([a-zA-Z0-9_.]+);", Pattern.MULTILINE);
        Pattern annPat = Pattern.compile("public\\s+@interface\\s+([a-zA-Z0-9_]+)");

        try (Stream<Path> stream = Files.walk(srcDir.toPath())) {
            stream.filter(p -> p.toString().endsWith(".java")).forEach(path -> {
                try {
                    String content = Files.readString(path);
                    Matcher mAnn = annPat.matcher(content);
                    while (mAnn.find()) {
                        String name = mAnn.group(1);
                        Matcher mPkg = pkgPat.matcher(content);
                        String pkg = mPkg.find() ? mPkg.group(1) : "dev.lumina";
                        boolean exists = allAnnotations.stream()
                                .anyMatch(a -> a.getSimpleName().equals(name) && a.getPackageName().equals(pkg));
                        if (!exists) {
                            allAnnotations.add(new AnnotationItem(name, pkg, true, false));
                        }
                    }
                } catch (IOException ignored) {}
            });
        } catch (Exception ignored) {}
    }

    public TextField getSearchField() {
        return searchField;
    }

    public ListView<AnnotationItem> getAnnotationListView() {
        return annotationListView;
    }

    public TreeView<String> getProjectTreeView() {
        return projectTreeView;
    }

    public Label getMatchesLabel() {
        return matchesLabel;
    }

    public Button getSearchByNameBtn() {
        return searchByNameBtn;
    }

    public Button getProjectBtn() {
        return projectBtn;
    }

    public ObservableList<AnnotationItem> getAllAnnotations() {
        return allAnnotations;
    }

    public ObservableList<AnnotationItem> getFilteredAnnotations() {
        return filteredAnnotations;
    }
}
