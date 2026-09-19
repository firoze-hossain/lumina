package dev.lumina.ui;

import dev.lumina.semantics.Docs;
import javafx.animation.PauseTransition;
import javafx.collections.ObservableList;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Popup;
import javafx.stage.Screen;
import javafx.util.Duration;

import java.util.List;

/**
 * IntelliJ IDEA-style Quick Documentation / Quick Info hover popup:
 * - Top header with circle type badge (C/I/M/F) and container FQCN
 * - Syntax-colored method/type signature with indented parameters
 * - Clean Javadoc section with scrollable pane
 * - Bottom footer with folder icon, module name, and dock/action icons
 * - Smooth mouse hover persistence (allows moving mouse into popup to scroll/copy)
 */
public class QuickDocPopup {

    private final Popup popup = new Popup();
    private final VBox root = new VBox();
    private final HBox header = new HBox(6);
    private final Label badge = new Label();
    private final Label containerLabel = new Label();
    private final VBox signatureBox = new VBox();
    private final VBox javadocBox = new VBox(4);
    private final Label javadocLabel = new Label();
    private final ScrollPane javadocScroll = new ScrollPane(javadocLabel);
    private final Region separator = new Region();
    private final HBox footer = new HBox(6);
    private final Label moduleIcon = new Label("📁");
    private final Label moduleLabel = new Label();
    private final Label pinIcon = new Label("📌");
    private final Label menuIcon = new Label("⋮");

    private boolean isMouseOver = false;
    private final PauseTransition hideDelay = new PauseTransition(Duration.millis(250));

    public QuickDocPopup() {
        popup.setAutoHide(false);
        popup.setHideOnEscape(true);

        root.getStyleClass().add("quick-doc-popup");
        root.setMaxWidth(560);
        root.setMinWidth(320);

        // Header: (I) / (C) badge + container FQCN
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("quick-doc-header");

        badge.getStyleClass().add("quick-doc-badge");
        badge.setAlignment(Pos.CENTER);

        containerLabel.getStyleClass().add("quick-doc-container");
        header.getChildren().addAll(badge, containerLabel);

        // Signature
        signatureBox.getStyleClass().add("quick-doc-sig-box");

        // Javadoc separator & scroll
        separator.getStyleClass().add("quick-doc-separator");
        separator.setMinHeight(1);
        separator.setPrefHeight(1);
        separator.setMaxHeight(1);

        javadocLabel.getStyleClass().add("quick-doc-javadoc");
        javadocLabel.setWrapText(true);
        javadocLabel.setMaxWidth(530);

        javadocScroll.getStyleClass().add("quick-doc-scroll");
        javadocScroll.setFitToWidth(true);
        javadocScroll.setMaxHeight(180);
        javadocScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        javadocBox.getChildren().addAll(separator, javadocScroll);

        // Footer: 📁 moduleName ... 📌 ⋮
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.getStyleClass().add("quick-doc-footer");

        moduleIcon.getStyleClass().add("quick-doc-module-icon");
        moduleLabel.getStyleClass().add("quick-doc-module-label");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        pinIcon.getStyleClass().add("quick-doc-action-icon");
        menuIcon.getStyleClass().add("quick-doc-action-icon");

        footer.getChildren().addAll(moduleIcon, moduleLabel, spacer, pinIcon, menuIcon);

        root.getChildren().addAll(header, signatureBox, javadocBox, footer);
        popup.getContent().add(root);

        // Mouse hover persistence: allow moving mouse into popup
        root.setOnMouseEntered(e -> {
            isMouseOver = true;
            hideDelay.stop();
        });
        root.setOnMouseExited(e -> {
            isMouseOver = false;
            scheduleHide();
        });
        hideDelay.setOnFinished(e -> {
            if (!isMouseOver) {
                hide();
            }
        });
    }

    public boolean isMouseOver() {
        return isMouseOver;
    }

    public void scheduleHide() {
        if (!isMouseOver) {
            hideDelay.playFromStart();
        }
    }

    public void cancelHide() {
        hideDelay.stop();
    }

    public void hide() {
        hideDelay.stop();
        isMouseOver = false;
        popup.hide();
    }

    public boolean isShowing() {
        return popup.isShowing();
    }

    /**
     * Shows the quick documentation card anchored relative to the symbol's screen bounds.
     */
    public void show(Node owner, Docs.SymbolDoc doc, Bounds screenBounds) {
        if (doc == null || owner == null || screenBounds == null) return;
        render(doc);

        hideDelay.stop();
        isMouseOver = false;

        // Compute positioning with screen boundary detection
        double targetX = screenBounds.getMinX();
        double targetY = screenBounds.getMaxY() + 4;

        // Apply owner window bounds check
        ObservableList<Screen> screens = Screen.getScreensForRectangle(
                screenBounds.getMinX(), screenBounds.getMinY(),
                screenBounds.getWidth(), screenBounds.getHeight());
        Rectangle2D screen = screens.isEmpty()
                ? Screen.getPrimary().getVisualBounds()
                : screens.get(0).getVisualBounds();

        double estimatedWidth = 440;
        double estimatedHeight = 180;
        if (targetX + estimatedWidth > screen.getMaxX() - 10) {
            targetX = Math.max(screen.getMinX() + 10, screen.getMaxX() - estimatedWidth - 10);
        }
        if (targetY + estimatedHeight > screen.getMaxY() - 10) {
            targetY = Math.max(screen.getMinY() + 10, screenBounds.getMinY() - estimatedHeight - 6);
        }

        popup.show(owner, targetX, targetY);
    }

    private void render(Docs.SymbolDoc doc) {
        // 1. Badge & Container
        String kind = doc.kind() != null ? doc.kind().toLowerCase() : "method";
        badge.getStyleClass().removeAll("badge-interface", "badge-class", "badge-record", "badge-enum", "badge-method", "badge-field");
        switch (kind) {
            case "interface" -> {
                badge.setText("I");
                badge.getStyleClass().add("badge-interface");
            }
            case "record" -> {
                badge.setText("R");
                badge.getStyleClass().add("badge-record");
            }
            case "enum" -> {
                badge.setText("E");
                badge.getStyleClass().add("badge-enum");
            }
            case "field", "variable" -> {
                badge.setText("F");
                badge.getStyleClass().add("badge-field");
            }
            case "method" -> {
                badge.setText("M");
                badge.getStyleClass().add("badge-method");
            }
            default -> {
                badge.setText("C");
                badge.getStyleClass().add("badge-class");
            }
        }

        containerLabel.setText(doc.containerFqcn() != null ? doc.containerFqcn() : "");

        // 2. Syntax-highlighted signature TextFlow
        signatureBox.getChildren().clear();
        TextFlow sigFlow = new TextFlow();
        sigFlow.getStyleClass().add("quick-doc-sig-flow");

        if (doc.returnType() != null && !doc.returnType().isBlank()) {
            Text retText = new Text(doc.returnType() + " ");
            retText.getStyleClass().add("quick-doc-sig-type");
            sigFlow.getChildren().add(retText);
        }

        Text nameText = new Text(doc.name());
        nameText.getStyleClass().add("quick-doc-sig-name");
        sigFlow.getChildren().add(nameText);

        if (doc.params() != null) {
            if (doc.params().isEmpty()) {
                Text parens = new Text("()");
                parens.getStyleClass().add("quick-doc-sig-punct");
                sigFlow.getChildren().add(parens);
            } else {
                Text openParen = new Text("(\n");
                openParen.getStyleClass().add("quick-doc-sig-punct");
                sigFlow.getChildren().add(openParen);

                List<String> params = doc.params();
                for (int i = 0; i < params.size(); i++) {
                    String p = params.get(i);
                    Text indent = new Text("    ");
                    indent.getStyleClass().add("quick-doc-sig-punct");
                    sigFlow.getChildren().add(indent);

                    int spaceIdx = p.indexOf(' ');
                    if (spaceIdx > 0) {
                        String pType = p.substring(0, spaceIdx);
                        String pName = p.substring(spaceIdx + 1);

                        Text paramType = new Text(pType + " ");
                        paramType.getStyleClass().add("quick-doc-sig-param-type");

                        Text paramName = new Text(pName);
                        paramName.getStyleClass().add("quick-doc-sig-param-name");

                        sigFlow.getChildren().addAll(paramType, paramName);
                    } else {
                        Text paramRaw = new Text(p);
                        paramRaw.getStyleClass().add("quick-doc-sig-param-name");
                        sigFlow.getChildren().add(paramRaw);
                    }

                    if (i < params.size() - 1) {
                        Text comma = new Text(",");
                        comma.getStyleClass().add("quick-doc-sig-punct");
                        sigFlow.getChildren().add(comma);
                    }
                    Text newline = new Text("\n");
                    newline.getStyleClass().add("quick-doc-sig-punct");
                    sigFlow.getChildren().add(newline);
                }

                Text closeParen = new Text(")");
                closeParen.getStyleClass().add("quick-doc-sig-punct");
                sigFlow.getChildren().add(closeParen);
            }
        }
        signatureBox.getChildren().add(sigFlow);

        // 3. Javadoc section
        if (doc.javadoc() != null && !doc.javadoc().isBlank()) {
            javadocLabel.setText(doc.javadoc());
            javadocBox.setVisible(true);
            javadocBox.setManaged(true);
        } else {
            javadocLabel.setText("");
            javadocBox.setVisible(false);
            javadocBox.setManaged(false);
        }

        // 4. Footer module info
        String mod = doc.moduleName();
        if (mod != null && !mod.isBlank()) {
            moduleLabel.setText(mod);
            footer.setVisible(true);
            footer.setManaged(true);
        } else {
            moduleLabel.setText("");
            footer.setVisible(false);
            footer.setManaged(false);
        }
    }
}
