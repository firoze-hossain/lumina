package dev.lumina.ui;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.function.IntConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A lightweight outline of the open Java file: type name, fields and
 * methods, each clickable to jump to its line. This is a regex-based
 * approximation of IntelliJ's PSI-backed Structure view (no full parser
 * behind it), which is fine for a quick "what's in this file" glance but
 * won't catch every edge case a real Java grammar would.
 */
public final class StructurePanel extends VBox {

    private final Label fileLabel = new Label();
    private final TreeView<Entry> tree = new TreeView<>();
    private IntConsumer onJumpToLine = line -> { };

    private record Entry(String text, int line, boolean isRoot) {
        @Override public String toString() { return text; }
    }

    private static final Pattern TYPE = Pattern.compile(
            "\\b(?:public|private|protected)?\\s*(?:static\\s+)?(?:final\\s+)?"
                    + "(class|interface|enum|record)\\s+([A-Za-z_][A-Za-z0-9_]*)");
    private static final Pattern FIELD = Pattern.compile(
            "^\\s*(?:@\\w+(?:\\([^)]*\\))?\\s*)*"
                    + "(?:public|private|protected)\\s+(?:static\\s+)?(?:final\\s+)?"
                    + "[\\w<>\\[\\],. ?]+\\s+([A-Za-z_][A-Za-z0-9_]*)\\s*(?:=|;)");
    private static final Pattern METHOD = Pattern.compile(
            "^\\s*(?:@\\w+(?:\\([^)]*\\))?\\s*)*"
                    + "(?:public|private|protected)\\s+(?:static\\s+)?(?:final\\s+)?"
                    + "(?:<[^>]*>\\s+)?[\\w<>\\[\\],. ?]+\\s+"
                    + "([A-Za-z_][A-Za-z0-9_]*)\\s*\\([^;{}]*\\)\\s*"
                    + "(?:throws\\s+[\\w., ]+)?\\s*\\{");

    public StructurePanel() {
        getStyleClass().add("structure-panel");
        setSpacing(4);
        setPadding(new Insets(8));
        fileLabel.getStyleClass().add("side-subtle");
        tree.setShowRoot(true);
        VBox.setVgrow(tree, Priority.ALWAYS);
        tree.getSelectionModel().selectedItemProperty().addListener((obs, old, item) -> {
            if (item != null && item.getValue() != null && !item.getValue().isRoot()) {
                onJumpToLine.accept(item.getValue().line());
            }
        });
        getChildren().addAll(fileLabel, tree);
    }

    public void setOnJumpToLine(IntConsumer onJumpToLine) {
        this.onJumpToLine = onJumpToLine;
    }

    public void showOutline(String fileName, String source) {
        fileLabel.setText(fileName == null ? "" : fileName);
        if (source == null) {
            tree.setRoot(null);
            return;
        }
        String[] lines = source.split("\n", -1);

        String typeName = fileName;
        Matcher tm = TYPE.matcher(source);
        if (tm.find()) typeName = tm.group(2);

        TreeItem<Entry> root = new TreeItem<>(new Entry(typeName, 0, true));
        TreeItem<Entry> fields = new TreeItem<>(new Entry("Fields", 0, true));
        TreeItem<Entry> methods = new TreeItem<>(new Entry("Methods", 0, true));

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            Matcher fm = FIELD.matcher(line);
            if (fm.find()) {
                fields.getChildren().add(new TreeItem<>(new Entry(fm.group(1), i + 1, false)));
                continue;
            }
            Matcher mm = METHOD.matcher(line);
            if (mm.find()) {
                methods.getChildren().add(new TreeItem<>(new Entry(mm.group(1) + "()", i + 1, false)));
            }
        }
        if (!fields.getChildren().isEmpty()) root.getChildren().add(fields);
        if (!methods.getChildren().isEmpty()) root.getChildren().add(methods);
        root.setExpanded(true);
        fields.setExpanded(true);
        methods.setExpanded(true);
        tree.setRoot(root);
    }
}