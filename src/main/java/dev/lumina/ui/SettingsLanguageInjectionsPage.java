package dev.lumina.ui;

import dev.lumina.injections.LanguageInjection;
import dev.lumina.injections.LanguageInjectionRegistry;
import dev.lumina.injections.LanguageInjectionScope;
import dev.lumina.injections.LanguageInjectionType;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.util.*;

/**
 * Modern, dynamic Language Injections settings page matching the reference IDE design.
 * Features:
 * - Top action toolbar with '+' dropdown (all 16 injection types), remove, edit, enable, disable,
 *   move to scope, and duplicate actions.
 * - Multi-column TableView (Name with checkbox & host icon, Injected Language with target icon, Scope).
 * - Real-time places status bar ("294 injections (720 of 722 places enabled)").
 * - Space bar toggle, Enter/double-click edit, Delete key remove.
 * - Dynamic Add/Edit modal dialog with prefix, suffix, and pattern fields.
 * - Full dirty-tracking and apply/reset lifecycle.
 */
public class SettingsLanguageInjectionsPage extends VBox {

    private final LanguageInjectionRegistry registry = LanguageInjectionRegistry.getInstance();
    private final ObservableList<LanguageInjection> masterList = FXCollections.observableArrayList();
    private final FilteredList<LanguageInjection> filteredList = new FilteredList<>(masterList, p -> true);
    private final SortedList<LanguageInjection> sortedList = new SortedList<>(filteredList);

    // Toolbar controls
    private final MenuButton addButton = new MenuButton("+");
    private final Button removeButton = new Button("—");
    private final Button editButton = new Button("✎");
    private final Button enableButton = new Button("☑");
    private final Button disableButton = new Button("⊟");
    private final Button moveToProjectButton = new Button();
    private final Button moveToIdeButton = new Button();
    private final Button duplicateButton = new Button("⎘");
    private final TextField searchField = new TextField();

    // TableView
    private final TableView<LanguageInjection> tableView = new TableView<>();
    private final TableColumn<LanguageInjection, LanguageInjection> nameCol = new TableColumn<>("Name");
    private final TableColumn<LanguageInjection, LanguageInjection> langCol = new TableColumn<>("Language");
    private final TableColumn<LanguageInjection, LanguageInjection> scopeCol = new TableColumn<>("Scope");

    // Bottom summary label
    private final Label summaryLabel = new Label();

    private Runnable onModifiedListener;
    private final BooleanProperty modifiedProperty = new SimpleBooleanProperty(false);

    public SettingsLanguageInjectionsPage() {
        setStyle("-fx-background-color: #1E1F22;");
        setPadding(new Insets(6, 12, 10, 12));
        setSpacing(6);
        VBox.setVgrow(this, Priority.ALWAYS);

        buildUI();
        loadData();

        registry.setOnModifiedListener(() -> {
            modifiedProperty.set(registry.isModified());
            updateSummaryLabel();
            if (onModifiedListener != null) {
                onModifiedListener.run();
            }
        });
    }

    private void buildUI() {
        getChildren().clear();

        // 1. Toolbar
        HBox toolbar = buildToolbar();

        // 2. TableView
        buildTableView();
        VBox.setVgrow(tableView, Priority.ALWAYS);

        // 3. Bottom Summary
        HBox summaryBar = buildSummaryBar();

        getChildren().addAll(toolbar, tableView, summaryBar);
    }

    // =========================================================================
    // Toolbar
    // =========================================================================

    private HBox buildToolbar() {
        HBox bar = new HBox(4);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(2, 0, 4, 0));

        // '+' Dropdown with all 16 injection types
        addButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 2 6; -fx-cursor: hand;");
        addButton.setTooltip(new Tooltip("Add Injection"));

        for (LanguageInjectionType type : LanguageInjectionType.ALL_TYPES) {
            MenuItem item = new MenuItem(type.displayName());
            Label iconLbl = new Label(type.iconSymbol());
            iconLbl.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #589DF6; -fx-min-width: 20px;");
            item.setGraphic(iconLbl);
            item.setOnAction(e -> openAddDialog(type));
            addButton.getItems().add(item);
        }

        // '—' Remove button
        removeButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #868A91; -fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 2 6; -fx-cursor: hand;");
        removeButton.setTooltip(new Tooltip("Remove Selected Injection (Delete)"));
        removeButton.setOnAction(e -> removeSelected());

        // '✎' Edit button
        editButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #868A91; -fx-font-size: 13px; -fx-padding: 2 6; -fx-cursor: hand;");
        editButton.setTooltip(new Tooltip("Edit Selected Injection (Enter)"));
        editButton.setOnAction(e -> editSelected());

        // '☑' Enable button
        enableButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #868A91; -fx-font-size: 12px; -fx-padding: 2 6; -fx-cursor: hand;");
        enableButton.setTooltip(new Tooltip("Enable Selected"));
        enableButton.setOnAction(e -> setSelectedEnabled(true));

        // '⊟' Disable button
        disableButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #868A91; -fx-font-size: 12px; -fx-padding: 2 6; -fx-cursor: hand;");
        disableButton.setTooltip(new Tooltip("Disable Selected"));
        disableButton.setOnAction(e -> setSelectedEnabled(false));

        // '⤓' Move to Project Scope
        moveToProjectButton.setGraphic(createDownTrayIcon());
        moveToProjectButton.setStyle("-fx-background-color: transparent; -fx-padding: 2 6; -fx-cursor: hand;");
        moveToProjectButton.setTooltip(new Tooltip("Move to Project Scope"));
        moveToProjectButton.setOnAction(e -> changeSelectedScope(LanguageInjectionScope.PROJECT));

        // '⤒' Move to IDE Scope
        moveToIdeButton.setGraphic(createUpTrayIcon());
        moveToIdeButton.setStyle("-fx-background-color: transparent; -fx-padding: 2 6; -fx-cursor: hand;");
        moveToIdeButton.setTooltip(new Tooltip("Move to IDE Scope"));
        moveToIdeButton.setOnAction(e -> changeSelectedScope(LanguageInjectionScope.IDE));

        // '⎘' Duplicate button
        duplicateButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #868A91; -fx-font-size: 13px; -fx-padding: 2 6; -fx-cursor: hand;");
        duplicateButton.setTooltip(new Tooltip("Duplicate Injection"));
        duplicateButton.setOnAction(e -> duplicateSelected());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Search Field
        searchField.setPromptText("Filter injections...");
        searchField.setPrefWidth(220);
        searchField.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-prompt-text-fill: #6F737A; -fx-background-radius: 4; -fx-border-color: #3E4147; -fx-border-radius: 4; -fx-padding: 3 8; -fx-font-size: 12px;");
        searchField.textProperty().addListener((obs, old, text) -> {
            String filter = text != null ? text.trim().toLowerCase() : "";
            filteredList.setPredicate(inj -> {
                if (filter.isEmpty()) return true;
                return inj.getDisplayName().toLowerCase().contains(filter)
                        || inj.getInjectedLanguage().toLowerCase().contains(filter)
                        || inj.getScope().getDisplayName().toLowerCase().contains(filter);
            });
            updateSummaryLabel();
        });

        bar.getChildren().addAll(
                addButton, removeButton, editButton, enableButton, disableButton,
                moveToProjectButton, moveToIdeButton, duplicateButton,
                spacer, searchField
        );

        return bar;
    }

    private Node createDownTrayIcon() {
        SVGPath svg = new SVGPath();
        svg.setContent("M 2 8 L 2 11 L 12 11 L 12 8 M 7 2 L 7 8 M 4 6 L 7 9 L 10 6");
        svg.setStroke(Color.web("#868A91"));
        svg.setFill(null);
        svg.setStrokeWidth(1.2);
        return svg;
    }

    private Node createUpTrayIcon() {
        SVGPath svg = new SVGPath();
        svg.setContent("M 2 8 L 2 11 L 12 11 L 12 8 M 7 9 L 7 2 M 4 5 L 7 2 L 10 5");
        svg.setStroke(Color.web("#868A91"));
        svg.setFill(null);
        svg.setStrokeWidth(1.2);
        return svg;
    }

    // =========================================================================
    // TableView Setup
    // =========================================================================

    private void buildTableView() {
        tableView.setFixedCellSize(26.0);
        tableView.setStyle(
                "-fx-background-color: #1E1F22; " +
                "-fx-base: #1E1F22; " +
                "-fx-control-inner-background: #1E1F22; " +
                "-fx-table-cell-border-color: transparent; " +
                "-fx-background-insets: 0; " +
                "-fx-border-color: #323438; " +
                "-fx-border-width: 1;"
        );
        tableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        // Column 1: Name
        nameCol.setText("Name");
        nameCol.setPrefWidth(550);
        nameCol.setSortable(true);
        nameCol.setComparator((a, b) -> a.getDisplayName().compareToIgnoreCase(b.getDisplayName()));
        nameCol.setCellValueFactory(param -> new javafx.beans.property.SimpleObjectProperty<>(param.getValue()));
        nameCol.setCellFactory(col -> new NameTableCell());

        // Column 2: Language
        langCol.setText("Language");
        langCol.setPrefWidth(220);
        langCol.setSortable(true);
        langCol.setComparator((a, b) -> a.getInjectedLanguage().compareToIgnoreCase(b.getInjectedLanguage()));
        langCol.setCellValueFactory(param -> new javafx.beans.property.SimpleObjectProperty<>(param.getValue()));
        langCol.setCellFactory(col -> new LanguageTableCell());

        // Column 3: Scope
        scopeCol.setText("Scope");
        scopeCol.setPrefWidth(120);
        scopeCol.setSortable(true);
        scopeCol.setComparator((a, b) -> a.getScope().getDisplayName().compareToIgnoreCase(b.getScope().getDisplayName()));
        scopeCol.setCellValueFactory(param -> new javafx.beans.property.SimpleObjectProperty<>(param.getValue()));
        scopeCol.setCellFactory(col -> new ScopeTableCell());

        tableView.getColumns().setAll(List.of(nameCol, langCol, scopeCol));

        // Default sort by Name ascending
        sortedList.comparatorProperty().bind(tableView.comparatorProperty());
        tableView.setItems(sortedList);
        tableView.getSortOrder().add(nameCol);

        // Key bindings
        tableView.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.SPACE) {
                toggleSelectedRows();
                event.consume();
            } else if (event.getCode() == KeyCode.ENTER) {
                editSelected();
                event.consume();
            } else if (event.getCode() == KeyCode.DELETE || event.getCode() == KeyCode.BACK_SPACE) {
                removeSelected();
                event.consume();
            }
        });

        // Double-click row to edit & styling
        tableView.setRowFactory(tv -> {
            TableRow<LanguageInjection> row = new TableRow<>() {
                @Override
                protected void updateItem(LanguageInjection item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setStyle("-fx-background-color: #1E1F22;");
                    } else if (isSelected()) {
                        setStyle("-fx-background-color: #2E436E;");
                    } else {
                        setStyle("-fx-background-color: #1E1F22;");
                    }
                }
            };
            row.selectedProperty().addListener((obs, oldV, isSel) -> {
                if (row.getItem() != null) {
                    row.setStyle(isSel ? "-fx-background-color: #2E436E;" : "-fx-background-color: #1E1F22;");
                }
            });
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty()) && event.getButton() == MouseButton.PRIMARY) {
                    editSelected();
                }
            });
            return row;
        });

        // Selection listener to update toolbar states
        tableView.getSelectionModel().selectedItemProperty().addListener((obs, old, item) -> updateToolbarState());
    }

    private void updateToolbarState() {
        boolean hasSel = !tableView.getSelectionModel().getSelectedItems().isEmpty();
        removeButton.setDisable(!hasSel);
        editButton.setDisable(!hasSel);
        enableButton.setDisable(!hasSel);
        disableButton.setDisable(!hasSel);
        moveToProjectButton.setDisable(!hasSel);
        moveToIdeButton.setDisable(!hasSel);
        duplicateButton.setDisable(!hasSel);

        String activeStyle = "-fx-background-color: transparent; -fx-text-fill: #DFE1E5; -fx-padding: 2 6; -fx-cursor: hand;";
        String disabledStyle = "-fx-background-color: transparent; -fx-text-fill: #55575E; -fx-padding: 2 6;";

        removeButton.setStyle(hasSel ? activeStyle : disabledStyle);
        editButton.setStyle(hasSel ? activeStyle : disabledStyle);
        enableButton.setStyle(hasSel ? activeStyle : disabledStyle);
        disableButton.setStyle(hasSel ? activeStyle : disabledStyle);
        duplicateButton.setStyle(hasSel ? activeStyle : disabledStyle);
    }

    // =========================================================================
    // Table Cells
    // =========================================================================

    private class NameTableCell extends TableCell<LanguageInjection, LanguageInjection> {
        private final CheckBox checkBox = new CheckBox();
        private final Label hostBadge = new Label();
        private final Label nameLabel = new Label();
        private final Label pkgLabel = new Label();
        private final HBox rootBox = new HBox(6, checkBox, hostBadge, nameLabel, pkgLabel);

        NameTableCell() {
            rootBox.setAlignment(Pos.CENTER_LEFT);
            rootBox.setPadding(new Insets(1, 4, 1, 4));

            nameLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
            pkgLabel.setStyle("-fx-text-fill: #868A91; -fx-font-size: 11px;");

            checkBox.setOnAction(e -> {
                LanguageInjection inj = getItem();
                if (inj != null) {
                    registry.setInjectionEnabled(inj.getId(), checkBox.isSelected());
                    inj.setEnabled(checkBox.isSelected());
                    updateSummaryLabel();
                }
            });
        }

        @Override
        protected void updateItem(LanguageInjection item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
                setText(null);
            } else {
                checkBox.setSelected(item.isEnabled());

                // Host language badge/icon
                String host = item.getHostLanguage().toLowerCase();
                configureHostBadge(hostBadge, host);

                // Formatted display name (prefix, name, package)
                String full = item.getDisplayName();
                int parenIdx = full.indexOf('(');
                if (parenIdx > 0) {
                    nameLabel.setText(full.substring(0, parenIdx).trim());
                    pkgLabel.setText(full.substring(parenIdx).trim());
                    pkgLabel.setVisible(true);
                    pkgLabel.setManaged(true);
                } else {
                    nameLabel.setText(full);
                    pkgLabel.setText("");
                    pkgLabel.setVisible(false);
                    pkgLabel.setManaged(false);
                }

                boolean sel = isSelected() || (getTableRow() != null && getTableRow().isSelected());
                if (sel) {
                    nameLabel.setStyle("-fx-text-fill: #FFFFFF; -fx-font-size: 12px;");
                    pkgLabel.setStyle("-fx-text-fill: #B0B5C0; -fx-font-size: 11px;");
                } else {
                    nameLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
                    pkgLabel.setStyle("-fx-text-fill: #868A91; -fx-font-size: 11px;");
                }

                setGraphic(rootBox);
                setText(null);
            }
        }
    }

    private static void configureHostBadge(Label badge, String host) {
        badge.setText(host.length() > 6 ? host.substring(0, 5) : host);
        badge.setFont(Font.font("Monospace", FontWeight.BOLD, 10));
        badge.setPadding(new Insets(1, 4, 1, 4));
        badge.setStyle(switch (host) {
            case "java" -> "-fx-background-color: #2D3D58; -fx-text-fill: #589DF6; -fx-background-radius: 3;";
            case "go" -> "-fx-background-color: #1F3F4A; -fx-text-fill: #00ACD7; -fx-background-radius: 3;";
            case "groovy" -> "-fx-background-color: #234433; -fx-text-fill: #62B543; -fx-background-radius: 3;";
            case "kotlin" -> "-fx-background-color: #432E59; -fx-text-fill: #B150E2; -fx-background-radius: 3;";
            case "javascript", "js" -> "-fx-background-color: #4D4523; -fx-text-fill: #F7DF1E; -fx-background-radius: 3;";
            case "xml", "html" -> "-fx-background-color: #4A3324; -fx-text-fill: #E36209; -fx-background-radius: 3;";
            case "python" -> "-fx-background-color: #2D3A4B; -fx-text-fill: #3776AB; -fx-background-radius: 3;";
            case "php" -> "-fx-background-color: #38334C; -fx-text-fill: #8892BF; -fx-background-radius: 3;";
            case "ruby" -> "-fx-background-color: #4C2525; -fx-text-fill: #CC342D; -fx-background-radius: 3;";
            case "sql" -> "-fx-background-color: #2A3C4D; -fx-text-fill: #336791; -fx-background-radius: 3;";
            default -> "-fx-background-color: #323438; -fx-text-fill: #868A91; -fx-background-radius: 3;";
        });
    }

    private static class LanguageTableCell extends TableCell<LanguageInjection, LanguageInjection> {
        private final Label iconLabel = new Label();
        private final Label nameLabel = new Label();
        private final HBox box = new HBox(6, iconLabel, nameLabel);

        LanguageTableCell() {
            box.setAlignment(Pos.CENTER_LEFT);
            box.setPadding(new Insets(1, 4, 1, 4));
            nameLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        }

        @Override
        protected void updateItem(LanguageInjection item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
                setText(null);
            } else {
                String lang = item.getInjectedLanguage();
                nameLabel.setText(lang);

                // Language Icon / Symbol badge
                String symbol = switch (lang.toLowerCase()) {
                    case "sql", "postgresql", "sqlite", "microsoft sql server" -> "SQL";
                    case "regexp", "jsregexp", "jsunicoderegexp", "phpregexp" -> ".*";
                    case "xml" -> "</>";
                    case "xpath", "xpath2" -> "XP";
                    case "html" -> "<>";
                    case "css", "jquery-css" -> "#";
                    case "javascript" -> "JS";
                    case "kotlin" -> "KT";
                    case "ruby" -> "RB";
                    case "erb" -> "<%>";
                    case "injectable php" -> "PHP";
                    case "json", "json5", "jsonpath", "micronaut-mongodb-json" -> "{}";
                    case "groovy" -> "GR";
                    case "hibernate ql", "jpa ql" -> "QL";
                    case "spring el" -> "EL";
                    case "apache spark" -> "✦";
                    case "properties", "spring-resource-reference" -> "PR";
                    default -> "•";
                };

                iconLabel.setText(symbol);
                iconLabel.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #868A91; -fx-min-width: 22px;");

                boolean sel = isSelected() || (getTableRow() != null && getTableRow().isSelected());
                nameLabel.setStyle(sel ? "-fx-text-fill: #FFFFFF; -fx-font-size: 12px;" : "-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");

                setGraphic(box);
                setText(null);
            }
        }
    }

    private static class ScopeTableCell extends TableCell<LanguageInjection, LanguageInjection> {
        private final Label label = new Label();

        ScopeTableCell() {
            setPadding(new Insets(1, 4, 1, 4));
            setAlignment(Pos.CENTER_LEFT);
        }

        @Override
        protected void updateItem(LanguageInjection item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
                setText(null);
            } else {
                LanguageInjectionScope scope = item.getScope();
                label.setText(scope.getDisplayName());
                boolean sel = isSelected() || (getTableRow() != null && getTableRow().isSelected());
                if (sel) {
                    label.setStyle("-fx-text-fill: #FFFFFF; -fx-font-size: 12px;");
                } else if (scope == LanguageInjectionScope.IDE) {
                    label.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
                } else if (scope == LanguageInjectionScope.PROJECT) {
                    label.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-font-weight: bold;");
                } else {
                    label.setStyle("-fx-text-fill: #868A91; -fx-font-size: 12px;");
                }
                setGraphic(label);
                setText(null);
            }
        }
    }

    // =========================================================================
    // Summary Bar
    // =========================================================================

    private HBox buildSummaryBar() {
        HBox box = new HBox();
        box.setAlignment(Pos.CENTER_RIGHT);
        box.setPadding(new Insets(4, 8, 2, 8));

        summaryLabel.setStyle("-fx-text-fill: #868A91; -fx-font-size: 11px;");
        updateSummaryLabel();

        box.getChildren().add(summaryLabel);
        return box;
    }

    private void updateSummaryLabel() {
        int totalInjections = registry.totalInjectionsCount();
        int totalPlaces = registry.totalPlacesCount();
        int enabledPlaces = registry.enabledPlacesCount();
        summaryLabel.setText(totalInjections + " injections (" + enabledPlaces + " of " + totalPlaces + " places enabled)");
    }

    // =========================================================================
    // User Actions
    // =========================================================================

    private void loadData() {
        masterList.setAll(registry.getWorkingInjections());
        updateSummaryLabel();
        updateToolbarState();
        if (!masterList.isEmpty()) {
            tableView.getSelectionModel().select(0);
            tableView.scrollTo(0);
        }
    }

    private void toggleSelectedRows() {
        List<LanguageInjection> selected = new ArrayList<>(tableView.getSelectionModel().getSelectedItems());
        if (selected.isEmpty()) return;

        // Toggle state based on first item
        boolean newState = !selected.get(0).isEnabled();
        for (LanguageInjection inj : selected) {
            registry.setInjectionEnabled(inj.getId(), newState);
            inj.setEnabled(newState);
        }
        tableView.refresh();
        updateSummaryLabel();
    }

    private void setSelectedEnabled(boolean enabled) {
        List<LanguageInjection> selected = new ArrayList<>(tableView.getSelectionModel().getSelectedItems());
        for (LanguageInjection inj : selected) {
            registry.setInjectionEnabled(inj.getId(), enabled);
            inj.setEnabled(enabled);
        }
        tableView.refresh();
        updateSummaryLabel();
    }

    private void changeSelectedScope(LanguageInjectionScope scope) {
        List<LanguageInjection> selected = new ArrayList<>(tableView.getSelectionModel().getSelectedItems());
        for (LanguageInjection inj : selected) {
            registry.moveToScope(inj.getId(), scope);
            inj.setScope(scope);
        }
        tableView.refresh();
    }

    private void removeSelected() {
        List<LanguageInjection> selected = new ArrayList<>(tableView.getSelectionModel().getSelectedItems());
        for (LanguageInjection inj : selected) {
            registry.removeInjection(inj.getId());
        }
        loadData();
    }

    private void duplicateSelected() {
        LanguageInjection selected = tableView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            LanguageInjection copy = registry.duplicateInjection(selected);
            loadData();
            tableView.getSelectionModel().select(copy);
            tableView.scrollTo(copy);
        }
    }

    private void editSelected() {
        LanguageInjection selected = tableView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            openEditDialog(selected);
        }
    }

    // =========================================================================
    // Add / Edit Modal Dialogs
    // =========================================================================

    private void openAddDialog(LanguageInjectionType type) {
        Dialog<LanguageInjection> dialog = new Dialog<>();
        dialog.setTitle("Add Language Injection");
        dialog.setHeaderText("Configure new " + type.displayName());
        applyDialogDarkTheme(dialog);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(16));

        TextField nameField = new TextField(type.hostLanguage() + ": custom injection");
        nameField.setPrefWidth(320);

        ComboBox<String> langCombo = new ComboBox<>(FXCollections.observableArrayList(
                "SQL", "RegExp", "JSON", "XML", "XPath", "Groovy", "Hibernate QL",
                "JPA QL", "Spring EL", "HTML", "CSS", "Properties", "PostgreSQL",
                "http-header-reference", "http-method-reference", "encoding-reference"
        ));
        langCombo.setValue(type.id().contains("sql") ? "SQL" : (type.id().contains("xml") ? "XML" : "RegExp"));

        TextField patternField = new TextField();
        patternField.setPromptText("Class / method pattern (e.g. java.lang.String.matches)");

        TextField prefixField = new TextField();
        prefixField.setPromptText("Prefix (optional)");

        TextField suffixField = new TextField();
        suffixField.setPromptText("Suffix (optional)");

        ComboBox<LanguageInjectionScope> scopeCombo = new ComboBox<>(FXCollections.observableArrayList(
                LanguageInjectionScope.IDE, LanguageInjectionScope.PROJECT
        ));
        scopeCombo.setValue(LanguageInjectionScope.IDE);

        Spinner<Integer> placesSpinner = new Spinner<>(1, 100, 1);

        grid.add(new Label("Display Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Injected Language:"), 0, 1);
        grid.add(langCombo, 1, 1);
        grid.add(new Label("Target Pattern:"), 0, 2);
        grid.add(patternField, 1, 2);
        grid.add(new Label("Prefix:"), 0, 3);
        grid.add(prefixField, 1, 3);
        grid.add(new Label("Suffix:"), 0, 4);
        grid.add(suffixField, 1, 4);
        grid.add(new Label("Scope:"), 0, 5);
        grid.add(scopeCombo, 1, 5);
        grid.add(new Label("Places Count:"), 0, 6);
        grid.add(placesSpinner, 1, 6);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(button -> {
            if (button == ButtonType.OK) {
                String id = "custom." + System.currentTimeMillis();
                LanguageInjection inj = new LanguageInjection(
                        id,
                        nameField.getText().trim(),
                        type.hostLanguage(),
                        langCombo.getValue(),
                        scopeCombo.getValue(),
                        true,
                        placesSpinner.getValue(),
                        placesSpinner.getValue(),
                        type.id(),
                        prefixField.getText().trim(),
                        suffixField.getText().trim(),
                        patternField.getText().trim(),
                        "User Custom"
                );
                return inj;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(inj -> {
            registry.addInjection(inj);
            loadData();
            tableView.getSelectionModel().select(inj);
            tableView.scrollTo(inj);
        });
    }

    private void openEditDialog(LanguageInjection target) {
        Dialog<LanguageInjection> dialog = new Dialog<>();
        dialog.setTitle("Edit Language Injection");
        dialog.setHeaderText("Edit " + target.getDisplayName());
        applyDialogDarkTheme(dialog);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(16));

        TextField nameField = new TextField(target.getDisplayName());
        nameField.setPrefWidth(320);

        ComboBox<String> langCombo = new ComboBox<>(FXCollections.observableArrayList(
                "SQL", "RegExp", "JSON", "XML", "XPath", "Groovy", "Hibernate QL",
                "JPA QL", "Spring EL", "HTML", "CSS", "Properties", "PostgreSQL",
                "http-header-reference", "http-method-reference", "encoding-reference",
                "JSON5", "JSONPath", "Micronaut-MongoDB-JSON", "Apache Spark"
        ));
        langCombo.setValue(target.getInjectedLanguage());

        TextField patternField = new TextField(target.getPattern());
        TextField prefixField = new TextField(target.getPrefix());
        TextField suffixField = new TextField(target.getSuffix());

        ComboBox<LanguageInjectionScope> scopeCombo = new ComboBox<>(FXCollections.observableArrayList(
                LanguageInjectionScope.BUILT_IN, LanguageInjectionScope.IDE, LanguageInjectionScope.PROJECT
        ));
        scopeCombo.setValue(target.getScope());

        Spinner<Integer> placesSpinner = new Spinner<>(1, 100, target.getPlacesCount());

        grid.add(new Label("Display Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Injected Language:"), 0, 1);
        grid.add(langCombo, 1, 1);
        grid.add(new Label("Target Pattern:"), 0, 2);
        grid.add(patternField, 1, 2);
        grid.add(new Label("Prefix:"), 0, 3);
        grid.add(prefixField, 1, 3);
        grid.add(new Label("Suffix:"), 0, 4);
        grid.add(suffixField, 1, 4);
        grid.add(new Label("Scope:"), 0, 5);
        grid.add(scopeCombo, 1, 5);
        grid.add(new Label("Places Count:"), 0, 6);
        grid.add(placesSpinner, 1, 6);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(button -> {
            if (button == ButtonType.OK) {
                LanguageInjection copy = target.copy();
                copy.setDisplayName(nameField.getText().trim());
                copy.setInjectedLanguage(langCombo.getValue());
                copy.setPattern(patternField.getText().trim());
                copy.setPrefix(prefixField.getText().trim());
                copy.setSuffix(suffixField.getText().trim());
                copy.setScope(scopeCombo.getValue());
                copy.setPlacesCount(placesSpinner.getValue());
                return copy;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(updated -> {
            registry.updateInjection(updated);
            loadData();
        });
    }

    private void applyDialogDarkTheme(Dialog<?> dialog) {
        DialogPane pane = dialog.getDialogPane();
        pane.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5;");
    }

    // =========================================================================
    // Settings Lifecycle (Apply / Reset / isModified)
    // =========================================================================

    public boolean isModified() {
        return registry.isModified();
    }

    public void apply() {
        registry.apply();
        modifiedProperty.set(false);
    }

    public void reset() {
        registry.reset();
        modifiedProperty.set(false);
        loadData();
    }

    public void setOnModifiedListener(Runnable listener) {
        this.onModifiedListener = listener;
    }

    public BooleanProperty modifiedProperty() {
        return modifiedProperty;
    }

    public TableView<LanguageInjection> getTableView() {
        return tableView;
    }

    public TextField getSearchField() {
        return searchField;
    }
}
