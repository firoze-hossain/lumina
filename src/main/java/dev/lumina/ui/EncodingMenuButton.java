package dev.lumina.ui;

import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.stage.Popup;
import javafx.stage.Screen;

import java.nio.charset.Charset;
import java.util.*;
import java.util.function.Consumer;

/**
 * Custom IntelliJ IDEA-style Encoding Dropdown Button with favorites and 'More >' scrollable flyout submenu.
 * Matches IntelliJ IDEA UI in media_1791170640825.png and media_1791170674584.png.
 */
public class EncodingMenuButton extends HBox {

    public static final List<String> FAVORITE_CHARSETS = List.of(
            "ISO-8859-1",
            "US-ASCII",
            "UTF-16",
            "UTF-8",
            "windows-1252"
    );

    private static final List<String> ALL_AVAILABLE_CHARSETS;

    static {
        Set<String> set = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        set.addAll(Charset.availableCharsets().keySet());
        ALL_AVAILABLE_CHARSETS = new ArrayList<>(set);
    }

    private final Label textLabel = new Label();
    private final SVGPath arrow = new SVGPath();
    private final String defaultOption;
    private String selectedEncoding;

    private Consumer<String> onEncodingChanged;

    private final Popup mainPopup = new Popup();
    private final Popup subPopup = new Popup();

    public EncodingMenuButton(String defaultOption, String initialValue) {
        this.defaultOption = defaultOption;
        this.selectedEncoding = initialValue != null ? initialValue : (defaultOption != null ? defaultOption : "UTF-8");

        setAlignment(Pos.CENTER_LEFT);
        setSpacing(6);
        setPadding(new Insets(3, 8, 3, 8));
        setCursor(Cursor.HAND);
        setMinHeight(26);
        setPrefHeight(26);

        textLabel.setText(selectedEncoding);
        textLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        HBox.setHgrow(textLabel, Priority.ALWAYS);

        // Downward chevron
        arrow.setContent("M 1 2 L 4.5 5.5 L 8 2");
        arrow.setStroke(Color.web("#868A91"));
        arrow.setStrokeWidth(1.4);
        arrow.setFill(Color.TRANSPARENT);

        getChildren().addAll(textLabel, arrow);

        updateStyle(false, false);

        setOnMouseEntered(e -> updateStyle(true, mainPopup.isShowing()));
        setOnMouseExited(e -> updateStyle(false, mainPopup.isShowing()));
        setOnMouseClicked(e -> togglePopup());

        initPopups();
    }

    private void updateStyle(boolean hover, boolean active) {
        String bg = hover ? "#35373B" : "#2B2D30";
        String border = active ? "#3574F0" : (hover ? "#5C616B" : "#4E5157");
        setStyle("-fx-background-color: " + bg + "; -fx-border-color: " + border + "; "
                + "-fx-border-radius: 4; -fx-background-radius: 4;");
    }

    public void setSelectedEncoding(String encoding) {
        if (encoding == null) return;
        this.selectedEncoding = encoding;
        textLabel.setText(encoding);
    }

    public String getSelectedEncoding() {
        return selectedEncoding;
    }

    public void setOnEncodingChanged(Consumer<String> onEncodingChanged) {
        this.onEncodingChanged = onEncodingChanged;
    }

    private void initPopups() {
        mainPopup.setAutoHide(true);
        mainPopup.setHideOnEscape(true);
        subPopup.setAutoHide(false); // managed relative to main popup

        mainPopup.setOnHidden(e -> {
            subPopup.hide();
            updateStyle(false, false);
        });

        mainPopup.getContent().add(buildMainContent());
        subPopup.getContent().add(buildSubContent());

        mainPopup.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                closeAll();
            }
        });
        subPopup.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                closeAll();
            }
        });
    }

    private Pane buildMainContent() {
        VBox root = new VBox(0);
        root.setPadding(new Insets(4, 0, 4, 0));
        root.setMinWidth(140);
        root.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #4E5157; -fx-border-width: 1; "
                + "-fx-border-radius: 6; -fx-background-radius: 6; "
                + "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.5), 10, 0, 0, 4);");

        // Optional default item
        if (defaultOption != null && !defaultOption.isBlank()) {
            HBox defaultRow = createMenuItem(defaultOption, defaultOption.equals(selectedEncoding), () -> select(defaultOption));
            root.getChildren().add(defaultRow);

            Region sep = new Region();
            sep.setMinHeight(1);
            sep.setPrefHeight(1);
            sep.setStyle("-fx-background-color: #393B40; -fx-padding: 0;");
            VBox.setMargin(sep, new Insets(3, 0, 3, 0));
            root.getChildren().add(sep);
        }

        // Favorites
        for (String fav : FAVORITE_CHARSETS) {
            boolean isSel = fav.equalsIgnoreCase(selectedEncoding);
            HBox row = createMenuItem(fav, isSel, () -> select(fav));
            root.getChildren().add(row);
        }

        Region sep2 = new Region();
        sep2.setMinHeight(1);
        sep2.setPrefHeight(1);
        sep2.setStyle("-fx-background-color: #393B40; -fx-padding: 0;");
        VBox.setMargin(sep2, new Insets(3, 0, 3, 0));
        root.getChildren().add(sep2);

        // 'More >' row
        HBox moreRow = new HBox(8);
        moreRow.setAlignment(Pos.CENTER_LEFT);
        moreRow.setPadding(new Insets(4, 12, 4, 12));
        moreRow.setCursor(Cursor.HAND);

        Label moreLabel = new Label("More");
        moreLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        HBox.setHgrow(moreLabel, Priority.ALWAYS);

        SVGPath chevron = new SVGPath();
        chevron.setContent("M 2 1 L 5.5 4.5 L 2 8");
        chevron.setStroke(Color.web("#868A91"));
        chevron.setStrokeWidth(1.4);
        chevron.setFill(Color.TRANSPARENT);

        moreRow.getChildren().addAll(moreLabel, chevron);

        moreRow.setOnMouseEntered(e -> {
            moreRow.setStyle("-fx-background-color: #3574F0;");
            moreLabel.setStyle("-fx-text-fill: #FFFFFF; -fx-font-size: 12px;");
            chevron.setStroke(Color.web("#FFFFFF"));
            showSubmenu(moreRow);
        });
        moreRow.setOnMouseExited(e -> {
            if (!subPopup.isShowing()) {
                moreRow.setStyle("-fx-background-color: transparent;");
                moreLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
                chevron.setStroke(Color.web("#868A91"));
            }
        });
        moreRow.setOnMouseClicked(e -> showSubmenu(moreRow));

        root.getChildren().add(moreRow);

        return root;
    }

    private Region buildSubContent() {
        VBox list = new VBox(0);
        list.setPadding(new Insets(4, 0, 4, 0));
        list.setStyle("-fx-background-color: #2B2D30;");

        for (String cs : ALL_AVAILABLE_CHARSETS) {
            boolean isSel = cs.equalsIgnoreCase(selectedEncoding);
            HBox row = createMenuItem(cs, isSel, () -> select(cs));
            list.getChildren().add(row);
        }

        ScrollPane scroll = new ScrollPane(list);
        scroll.setFitToWidth(true);
        scroll.setPrefViewportHeight(280);
        scroll.setMaxHeight(320);
        scroll.setPrefWidth(170);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setStyle("-fx-background-color: #2B2D30; -fx-background: #2B2D30; -fx-border-color: #4E5157; "
                + "-fx-border-width: 1; -fx-border-radius: 6; -fx-background-radius: 6; "
                + "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.5), 10, 0, 0, 4);");

        // Keep open while mouse is inside submenu
        scroll.addEventFilter(MouseEvent.MOUSE_ENTERED, e -> {});
        scroll.addEventFilter(MouseEvent.MOUSE_EXITED, e -> {
            Point2D mouse = new Point2D(e.getScreenX(), e.getScreenY());
            Bounds mainB = mainPopup.getContent().get(0).localToScreen(mainPopup.getContent().get(0).getBoundsInLocal());
            if (mainB == null || !mainB.contains(mouse)) {
                subPopup.hide();
            }
        });

        return scroll;
    }

    private HBox createMenuItem(String text, boolean isSelected, Runnable onAction) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(4, 12, 4, 12));
        row.setCursor(Cursor.HAND);

        Label label = new Label(text);
        label.setStyle("-fx-font-size: 12px; -fx-text-fill: " + (isSelected ? "#FFFFFF" : "#DFE1E5") + ";");
        HBox.setHgrow(label, Priority.ALWAYS);

        row.setStyle(isSelected ? "-fx-background-color: #3574F0;" : "-fx-background-color: transparent;");
        row.getChildren().add(label);

        row.setOnMouseEntered(e -> {
            row.setStyle("-fx-background-color: #3574F0;");
            label.setStyle("-fx-font-size: 12px; -fx-text-fill: #FFFFFF;");
            if (subPopup.isShowing()) {
                subPopup.hide();
            }
        });

        row.setOnMouseExited(e -> {
            if (isSelected) {
                row.setStyle("-fx-background-color: #3574F0;");
                label.setStyle("-fx-font-size: 12px; -fx-text-fill: #FFFFFF;");
            } else {
                row.setStyle("-fx-background-color: transparent;");
                label.setStyle("-fx-font-size: 12px; -fx-text-fill: #DFE1E5;");
            }
        });

        row.setOnMouseClicked(e -> {
            e.consume();
            onAction.run();
        });

        return row;
    }

    private void showSubmenu(HBox moreRow) {
        Point2D screenPos = moreRow.localToScreen(moreRow.getWidth() + 2, 0);
        if (screenPos == null) return;

        double targetX = screenPos.getX();
        double targetY = screenPos.getY();

        var screens = Screen.getScreensForRectangle(targetX, targetY, 200, 300);
        if (!screens.isEmpty()) {
            Rectangle2D sb = screens.get(0).getVisualBounds();
            if (targetX + 180 > sb.getMaxX()) {
                Point2D leftPos = moreRow.localToScreen(-175, 0);
                if (leftPos != null) targetX = leftPos.getX();
            }
            if (targetY + 300 > sb.getMaxY()) {
                targetY = sb.getMaxY() - 310;
            }
        }

        subPopup.show(moreRow, targetX, targetY);
    }

    private void select(String encoding) {
        setSelectedEncoding(encoding);
        closeAll();
        if (onEncodingChanged != null) {
            onEncodingChanged.accept(encoding);
        }
    }

    public void togglePopup() {
        if (mainPopup.isShowing()) {
            closeAll();
        } else {
            showPopup();
        }
    }

    public void showPopup() {
        Point2D screenPos = localToScreen(0, getHeight() + 2);
        if (screenPos == null) return;

        // Rebuild main popup content to reflect current selected encoding
        mainPopup.getContent().setAll(buildMainContent());

        double targetX = screenPos.getX();
        double targetY = screenPos.getY();

        var screens = Screen.getScreensForRectangle(targetX, targetY, 200, 200);
        if (!screens.isEmpty()) {
            Rectangle2D sb = screens.get(0).getVisualBounds();
            if (targetY + 220 > sb.getMaxY()) {
                Point2D above = localToScreen(0, -220);
                if (above != null) targetY = above.getY();
            }
        }

        updateStyle(true, true);
        mainPopup.show(this, targetX, targetY);
    }

    public void closeAll() {
        subPopup.hide();
        mainPopup.hide();
        updateStyle(false, false);
    }
}
