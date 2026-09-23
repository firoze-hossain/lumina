package dev.lumina.ui;

import dev.lumina.diagnostics.JavaDiagnostics;
import dev.lumina.semantics.Docs;
import javafx.animation.PauseTransition;
import javafx.collections.ObservableList;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
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
import java.util.function.Consumer;

/**
 * IntelliJ IDEA-style Quick Documentation / Quick Info hover popup:
 * - Top inspection warning banner for unused methods / diagnostics (with Safe delete & Alt+Enter actions)
 * - Header with circle type badge (C/I/M/F) and container FQCN
 * - Syntax-colored method/type signature with annotations and indented parameters
 * - Clean Javadoc section with scrollable pane
 * - Bottom footer with folder icon, module name, and dock/action icons
 * - Smooth mouse hover persistence (allows moving mouse into popup to scroll/copy)
 */
public class QuickDocPopup {

    private final Popup popup = new Popup();
    private final VBox root = new VBox();

    // Top Inspection Box (shown when hovering over an unused method or diagnostic)
    private final VBox inspectionBox = new VBox(6);
    private final HBox inspectionTitleRow = new HBox(6);
    private final Label inspectionWarningIcon = new Label("\u26A0");
    private final Label inspectionTitle = new Label();
    private final Label inspectionMenuIcon = new Label("\u22EE");
    private final HBox inspectionActionRow = new HBox(12);
    private final Label safeDeleteLink = new Label();
    private final Label safeDeleteShortcut = new Label("Alt+Shift+Enter");
    private final Label moreActionsLink = new Label("More actions\u2026");
    private final Label moreActionsShortcut = new Label("Alt+Enter");
    private final Region inspectionDivider = new Region();

    // Header: Badge & FQCN
    private final HBox header = new HBox(6);
    private final Label badge = new Label();
    private final Label containerLabel = new Label();

    // Signature & Javadoc
    private final VBox signatureBox = new VBox();
    private final HBox specifiedByBox = new HBox(4);
    private final VBox javadocBox = new VBox(4);
    private final Label javadocLabel = new Label();
    private final ScrollPane javadocScroll = new ScrollPane(javadocLabel);
    private final Region separator = new Region();

    // Footer
    private final HBox footer = new HBox(6);
    private final Label moduleIcon = new Label("📁");
    private final Label moduleLabel = new Label();
    private final Label pinIcon = new Label("📌");
    private final Label menuIcon = new Label("⋮");

    private boolean isMouseOver = false;
    private final PauseTransition hideDelay = new PauseTransition(Duration.millis(250));

    private Consumer<JavaDiagnostics.Diag> onSafeDelete;
    private Consumer<JavaDiagnostics.Diag> onMoreActions;
    private JavaDiagnostics.Diag currentDiag;

    public QuickDocPopup() {
        popup.setAutoHide(false);
        popup.setHideOnEscape(true);

        root.getStyleClass().add("quick-doc-popup");
        root.setMaxWidth(580);
        root.setMinWidth(340);

        // 1. Inspection warning banner
        inspectionBox.getStyleClass().add("quick-doc-inspection-box");

        inspectionTitleRow.setAlignment(Pos.CENTER_LEFT);
        inspectionWarningIcon.getStyleClass().add("quick-doc-warning-icon");
        inspectionTitle.getStyleClass().add("quick-doc-inspection-title");
        inspectionTitle.setWrapText(true);
        inspectionTitle.setMaxWidth(480);
        HBox.setHgrow(inspectionTitle, Priority.ALWAYS);

        inspectionMenuIcon.getStyleClass().add("quick-doc-menu-btn");
        inspectionTitleRow.getChildren().addAll(inspectionWarningIcon, inspectionTitle, inspectionMenuIcon);

        inspectionActionRow.setAlignment(Pos.CENTER_LEFT);

        safeDeleteLink.getStyleClass().add("quick-doc-action-link");
        safeDeleteShortcut.getStyleClass().add("quick-doc-shortcut-hint");
        HBox safeDeleteBox = new HBox(6, safeDeleteLink, safeDeleteShortcut);
        safeDeleteBox.setAlignment(Pos.CENTER_LEFT);
        safeDeleteBox.setCursor(javafx.scene.Cursor.HAND);
        safeDeleteBox.setOnMouseClicked(e -> {
            e.consume();
            JavaDiagnostics.Diag d = currentDiag;
            hide();
            if (onSafeDelete != null && d != null) {
                onSafeDelete.accept(d);
            }
        });

        Region actionSpacer = new Region();
        HBox.setHgrow(actionSpacer, Priority.ALWAYS);

        moreActionsLink.getStyleClass().add("quick-doc-action-link");
        moreActionsShortcut.getStyleClass().add("quick-doc-shortcut-hint");
        HBox moreActionsBox = new HBox(6, moreActionsLink, moreActionsShortcut);
        moreActionsBox.setAlignment(Pos.CENTER_LEFT);
        moreActionsBox.setCursor(javafx.scene.Cursor.HAND);
        moreActionsBox.setOnMouseClicked(e -> {
            e.consume();
            JavaDiagnostics.Diag d = currentDiag;
            hide();
            if (onMoreActions != null && d != null) {
                onMoreActions.accept(d);
            }
        });

        inspectionActionRow.getChildren().addAll(safeDeleteBox, actionSpacer, moreActionsBox);

        inspectionDivider.getStyleClass().add("quick-doc-separator");
        inspectionDivider.setMinHeight(1);
        inspectionDivider.setPrefHeight(1);
        inspectionDivider.setMaxHeight(1);

        inspectionBox.getChildren().addAll(inspectionTitleRow, inspectionActionRow, inspectionDivider);
        inspectionBox.setVisible(false);
        inspectionBox.setManaged(false);

        // 2. Header: (I) / (C) badge + container FQCN
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("quick-doc-header");

        badge.getStyleClass().add("quick-doc-badge");
        badge.setAlignment(Pos.CENTER);

        containerLabel.getStyleClass().add("quick-doc-container");
        header.getChildren().addAll(badge, containerLabel);

        // 3. Signature
        signatureBox.getStyleClass().add("quick-doc-sig-box");

        // 4. Javadoc separator & scroll
        separator.getStyleClass().add("quick-doc-separator");
        separator.setMinHeight(1);
        separator.setPrefHeight(1);
        separator.setMaxHeight(1);

        javadocLabel.getStyleClass().add("quick-doc-javadoc");
        javadocLabel.setWrapText(true);
        javadocLabel.setMaxWidth(550);

        javadocScroll.getStyleClass().add("quick-doc-scroll");
        javadocScroll.setFitToWidth(true);
        javadocScroll.setMaxHeight(180);
        javadocScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        javadocBox.getChildren().addAll(separator, javadocScroll);

        // 5. Footer: 📁 moduleName ... 📌 ⋮
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.getStyleClass().add("quick-doc-footer");

        moduleIcon.getStyleClass().add("quick-doc-module-icon");
        moduleLabel.getStyleClass().add("quick-doc-module-label");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        pinIcon.getStyleClass().add("quick-doc-action-icon");
        menuIcon.getStyleClass().add("quick-doc-action-icon");

        specifiedByBox.setAlignment(Pos.CENTER_LEFT);
        specifiedByBox.setPadding(new Insets(6, 12, 4, 12));
        specifiedByBox.setVisible(false);
        specifiedByBox.setManaged(false);

        root.getChildren().addAll(inspectionBox, header, signatureBox, specifiedByBox, javadocBox, footer);
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

    public void setOnSafeDelete(Consumer<JavaDiagnostics.Diag> handler) {
        this.onSafeDelete = handler;
    }

    public void setOnMoreActions(Consumer<JavaDiagnostics.Diag> handler) {
        this.onMoreActions = handler;
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

    public void show(Node owner, Docs.SymbolDoc doc, Bounds screenBounds) {
        show(owner, doc, screenBounds, null);
    }

    /**
     * Shows the quick documentation card anchored relative to the symbol's screen bounds,
     * optionally including the top inspection warning banner if a diagnostic is present.
     */
    public void show(Node owner, Docs.SymbolDoc doc, Bounds screenBounds, JavaDiagnostics.Diag diag) {
        if (doc == null || owner == null || screenBounds == null) return;
        render(doc, diag);

        hideDelay.stop();
        isMouseOver = false;

        // Compute positioning with screen boundary detection
        double targetX = screenBounds.getMinX();
        double targetY = screenBounds.getMaxY() + 4;

        ObservableList<Screen> screens = Screen.getScreensForRectangle(
                screenBounds.getMinX(), screenBounds.getMinY(),
                screenBounds.getWidth(), screenBounds.getHeight());
        Rectangle2D screen = screens.isEmpty()
                ? Screen.getPrimary().getVisualBounds()
                : screens.get(0).getVisualBounds();

        double estimatedWidth = 460;
        double estimatedHeight = diag != null ? 240 : 180;
        if (targetX + estimatedWidth > screen.getMaxX() - 10) {
            targetX = Math.max(screen.getMinX() + 10, screen.getMaxX() - estimatedWidth - 10);
        }
        if (targetY + estimatedHeight > screen.getMaxY() - 10) {
            targetY = Math.max(screen.getMinY() + 10, screenBounds.getMinY() - estimatedHeight - 6);
        }

        popup.show(owner, targetX, targetY);
    }

    private void render(Docs.SymbolDoc doc, JavaDiagnostics.Diag diag) {
        this.currentDiag = diag;

        // 1. Inspection Banner
        if (diag != null && diag.title() != null && !diag.title().isBlank()) {
            inspectionTitle.setText(diag.title());
            String actionLabel = "Safe delete";
            if (diag.quickFix() != null && diag.quickFix().startsWith("unused-method:")) {
                String sig = diag.quickFix().substring("unused-method:".length());
                actionLabel = "Safe delete '" + sig + "'";
            } else if (diag.quickFix() != null && diag.quickFix().startsWith("unused-field:")) {
                String name = diag.quickFix().substring("unused-field:".length());
                actionLabel = "Safe delete '" + name + "'";
            }
            safeDeleteLink.setText(actionLabel);
            inspectionBox.setVisible(true);
            inspectionBox.setManaged(true);
        } else {
            inspectionBox.setVisible(false);
            inspectionBox.setManaged(false);
        }

        // 2. Badge & Container
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

        // 3. Syntax-highlighted signature TextFlow
        signatureBox.getChildren().clear();
        TextFlow sigFlow = new TextFlow();
        sigFlow.getStyleClass().add("quick-doc-sig-flow");

        // Render method annotations (e.g. @Query(...))
        if (doc.annotations() != null && !doc.annotations().isEmpty()) {
            for (String ann : doc.annotations()) {
                appendAnnotation(sigFlow, ann);
                Text nl = new Text("\n");
                nl.getStyleClass().add("quick-doc-sig-punct");
                sigFlow.getChildren().add(nl);
            }
        }

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

                    // Check for parameter annotations: e.g. @Param("searchTerm") String searchTerm
                    if (p.startsWith("@")) {
                        int lastSpace = p.lastIndexOf(' ');
                        if (lastSpace > 0) {
                            String beforeName = p.substring(0, lastSpace);
                            String pName = p.substring(lastSpace + 1);

                            int typeStart = beforeName.lastIndexOf(' ');
                            if (typeStart > 0) {
                                String annPart = beforeName.substring(0, typeStart).trim();
                                String pType = beforeName.substring(typeStart + 1).trim();

                                appendAnnotation(sigFlow, annPart);
                                Text space = new Text(" ");
                                sigFlow.getChildren().add(space);

                                Text paramType = new Text(pType + " ");
                                paramType.getStyleClass().add("quick-doc-sig-param-type");

                                Text paramName = new Text(pName);
                                paramName.getStyleClass().add("quick-doc-sig-param-name");

                                sigFlow.getChildren().addAll(paramType, paramName);
                            } else {
                                appendAnnotation(sigFlow, beforeName);
                                Text space = new Text(" ");
                                sigFlow.getChildren().add(space);
                                Text paramName = new Text(pName);
                                paramName.getStyleClass().add("quick-doc-sig-param-name");
                                sigFlow.getChildren().add(paramName);
                            }
                        } else {
                            Text paramRaw = new Text(p);
                            paramRaw.getStyleClass().add("quick-doc-sig-param-name");
                            sigFlow.getChildren().add(paramRaw);
                        }
                    } else {
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

        // 3b. Specified by / Overrides row
        if (doc.specifiedBy() != null && !doc.specifiedBy().isBlank()) {
            specifiedByBox.getChildren().clear();
            String s = doc.specifiedBy();
            if (s.contains(" in interface ")) {
                int inIdx = s.indexOf(" in interface ");
                String pfx = s.substring(0, inIdx);
                String iface = s.substring(inIdx + " in interface ".length());
                String pfxTitle = pfx.startsWith("Specified by: ") ? "Specified by: " : "Overrides: ";
                String mName = pfx.substring(pfxTitle.length());

                Label pfxLbl = new Label(pfxTitle);
                pfxLbl.setStyle("-fx-text-fill: #80848B; -fx-font-size: 11.5px;");

                Label mPill = new Label(mName);
                mPill.setStyle("-fx-background-color: #393B40; -fx-text-fill: #56A8F5; -fx-background-radius: 3; -fx-padding: 1 5 1 5; -fx-font-size: 11px;");

                Label inLbl = new Label(" in interface ");
                inLbl.setStyle("-fx-text-fill: #80848B; -fx-font-size: 11.5px;");

                Label ifacePill = new Label(iface);
                ifacePill.setStyle("-fx-background-color: #393B40; -fx-text-fill: #56A8F5; -fx-background-radius: 3; -fx-padding: 1 5 1 5; -fx-font-size: 11px;");

                specifiedByBox.getChildren().addAll(pfxLbl, mPill, inLbl, ifacePill);
            } else if (s.contains(" in class ")) {
                int inIdx = s.indexOf(" in class ");
                String pfx = s.substring(0, inIdx);
                String cls = s.substring(inIdx + " in class ".length());
                String pfxTitle = pfx.startsWith("Overrides: ") ? "Overrides: " : "Specified by: ";
                String mName = pfx.substring(pfxTitle.length());

                Label pfxLbl = new Label(pfxTitle);
                pfxLbl.setStyle("-fx-text-fill: #80848B; -fx-font-size: 11.5px;");

                Label mPill = new Label(mName);
                mPill.setStyle("-fx-background-color: #393B40; -fx-text-fill: #56A8F5; -fx-background-radius: 3; -fx-padding: 1 5 1 5; -fx-font-size: 11px;");

                Label inLbl = new Label(" in class ");
                inLbl.setStyle("-fx-text-fill: #80848B; -fx-font-size: 11.5px;");

                Label clsPill = new Label(cls);
                clsPill.setStyle("-fx-background-color: #393B40; -fx-text-fill: #56A8F5; -fx-background-radius: 3; -fx-padding: 1 5 1 5; -fx-font-size: 11px;");

                specifiedByBox.getChildren().addAll(pfxLbl, mPill, inLbl, clsPill);
            } else {
                Label lbl = new Label(s);
                lbl.setStyle("-fx-text-fill: #80848B; -fx-font-size: 11.5px;");
                specifiedByBox.getChildren().add(lbl);
            }
            specifiedByBox.setVisible(true);
            specifiedByBox.setManaged(true);
        } else {
            specifiedByBox.setVisible(false);
            specifiedByBox.setManaged(false);
        }

        // 4. Javadoc section
        if (doc.javadoc() != null && !doc.javadoc().isBlank()) {
            javadocLabel.setText(doc.javadoc());
            javadocBox.setVisible(true);
            javadocBox.setManaged(true);
        } else {
            javadocLabel.setText("");
            javadocBox.setVisible(false);
            javadocBox.setManaged(false);
        }

        // 5. Footer module info
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

    private void appendAnnotation(TextFlow flow, String ann) {
        int parenIdx = ann.indexOf('(');
        if (parenIdx > 0 && ann.endsWith(")")) {
            String annName = ann.substring(0, parenIdx);
            String inside = ann.substring(parenIdx + 1, ann.length() - 1);

            Text annText = new Text(annName);
            annText.getStyleClass().add("quick-doc-sig-annotation");

            Text openParen = new Text("(");
            openParen.getStyleClass().add("quick-doc-sig-punct");

            flow.getChildren().addAll(annText, openParen);

            if (inside.startsWith("\"") && inside.endsWith("\"")) {
                Text strText = new Text(inside);
                strText.getStyleClass().add("quick-doc-sig-string");
                flow.getChildren().add(strText);
            } else {
                Text valText = new Text(inside);
                valText.getStyleClass().add("quick-doc-sig-punct");
                flow.getChildren().add(valText);
            }

            Text closeParen = new Text(")");
            closeParen.getStyleClass().add("quick-doc-sig-punct");
            flow.getChildren().add(closeParen);
        } else {
            Text annText = new Text(ann);
            annText.getStyleClass().add("quick-doc-sig-annotation");
            flow.getChildren().add(annText);
        }
    }
}
