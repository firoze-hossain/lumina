package dev.lumina.settings;

import dev.lumina.util.Settings;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Dynamic persistent configuration and model for Editor > Code Editing settings.
 * Backed by ~/.lumina/lumina.properties, supporting dynamic listeners,
 * real-time UI synchronization, and live editor updates.
 */
public final class CodeEditingSettings {

    private static final CodeEditingSettings INSTANCE = new CodeEditingSettings();

    public static CodeEditingSettings getInstance() {
        return INSTANCE;
    }

    public enum RefactoringOption {
        IN_EDITOR("In the editor"),
        IN_MODAL_DIALOGS("In modal dialogs");

        private final String label;

        RefactoringOption(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }

        public static RefactoringOption fromLabel(String label) {
            for (RefactoringOption o : values()) {
                if (o.label.equalsIgnoreCase(label) || o.name().equalsIgnoreCase(label)) {
                    return o;
                }
            }
            return IN_EDITOR;
        }
    }

    @FunctionalInterface
    public interface Listener {
        void onSettingsChanged(CodeEditingSettings settings);
    }

    private final List<Listener> listeners = new CopyOnWriteArrayList<>();

    // Highlight on Caret Movement
    private boolean matchedBrace = true;
    private boolean currentScope = false;
    private boolean usagesOfElementAtCaret = true;

    // Quick Documentation
    private boolean showQuickDocOnHover = true;

    // Refactorings
    private RefactoringOption refactoringOption = RefactoringOption.IN_EDITOR;
    private boolean preselectCurrentSymbolForRename = true;
    private boolean showInlineDialogForLocalVariables = true;

    // Error Highlighting
    private int errorStripeMarkMinHeight = 2; // pixels
    private int autoreparseDelay = 300; // milliseconds
    private String nextErrorActionGoesThrough = "The problems with the highest priority"; // or "All problems"
    private boolean suppressWithSuppressWarnings = true;

    // Editor Tooltips
    private int tooltipDelay = 500; // milliseconds

    public CodeEditingSettings() {
        initDefaults();
        load();
    }

    public void initDefaults() {
        matchedBrace = true;
        currentScope = false;
        usagesOfElementAtCaret = true;

        showQuickDocOnHover = true;

        refactoringOption = RefactoringOption.IN_EDITOR;
        preselectCurrentSymbolForRename = true;
        showInlineDialogForLocalVariables = true;

        errorStripeMarkMinHeight = 2;
        autoreparseDelay = 300;
        nextErrorActionGoesThrough = "The problems with the highest priority";
        suppressWithSuppressWarnings = true;

        tooltipDelay = 500;
    }

    public void load() {
        String val;

        val = Settings.get("editor.codeediting.matched.brace");
        if (val != null) matchedBrace = Boolean.parseBoolean(val);

        val = Settings.get("editor.codeediting.current.scope");
        if (val != null) currentScope = Boolean.parseBoolean(val);

        val = Settings.get("editor.codeediting.usages.at.caret");
        if (val != null) usagesOfElementAtCaret = Boolean.parseBoolean(val);

        val = Settings.get("editor.codeediting.quick.doc.hover");
        if (val != null) showQuickDocOnHover = Boolean.parseBoolean(val);

        val = Settings.get("editor.codeediting.refactoring.option");
        if (val != null) refactoringOption = RefactoringOption.fromLabel(val);

        val = Settings.get("editor.codeediting.preselect.rename.symbol");
        if (val != null) preselectCurrentSymbolForRename = Boolean.parseBoolean(val);

        val = Settings.get("editor.codeediting.inline.dialog.local.vars");
        if (val != null) showInlineDialogForLocalVariables = Boolean.parseBoolean(val);

        val = Settings.get("editor.codeediting.error.stripe.min.height");
        if (val != null) {
            try { errorStripeMarkMinHeight = Integer.parseInt(val); } catch (NumberFormatException ignored) {}
        }

        val = Settings.get("editor.codeediting.autoreparse.delay");
        if (val != null) {
            try { autoreparseDelay = Integer.parseInt(val); } catch (NumberFormatException ignored) {}
        }

        val = Settings.get("editor.codeediting.next.error.action");
        if (val != null && !val.isBlank()) nextErrorActionGoesThrough = val;

        val = Settings.get("editor.codeediting.suppress.warnings");
        if (val != null) suppressWithSuppressWarnings = Boolean.parseBoolean(val);

        val = Settings.get("editor.codeediting.tooltip.delay");
        if (val != null) {
            try { tooltipDelay = Integer.parseInt(val); } catch (NumberFormatException ignored) {}
        }
    }

    public void save() {
        Settings.put("editor.codeediting.matched.brace", String.valueOf(matchedBrace));
        Settings.put("editor.codeediting.current.scope", String.valueOf(currentScope));
        Settings.put("editor.codeediting.usages.at.caret", String.valueOf(usagesOfElementAtCaret));
        Settings.put("editor.codeediting.quick.doc.hover", String.valueOf(showQuickDocOnHover));
        Settings.put("editor.codeediting.refactoring.option", refactoringOption.name());
        Settings.put("editor.codeediting.preselect.rename.symbol", String.valueOf(preselectCurrentSymbolForRename));
        Settings.put("editor.codeediting.inline.dialog.local.vars", String.valueOf(showInlineDialogForLocalVariables));
        Settings.put("editor.codeediting.error.stripe.min.height", String.valueOf(errorStripeMarkMinHeight));
        Settings.put("editor.codeediting.autoreparse.delay", String.valueOf(autoreparseDelay));
        Settings.put("editor.codeediting.next.error.action", nextErrorActionGoesThrough);
        Settings.put("editor.codeediting.suppress.warnings", String.valueOf(suppressWithSuppressWarnings));
        Settings.put("editor.codeediting.tooltip.delay", String.valueOf(tooltipDelay));

        notifyListeners();
    }

    public CodeEditingSettings copy() {
        CodeEditingSettings clone = new CodeEditingSettings();
        clone.applyFrom(this);
        return clone;
    }

    public void applyFrom(CodeEditingSettings other) {
        if (other == null) return;
        this.matchedBrace = other.matchedBrace;
        this.currentScope = other.currentScope;
        this.usagesOfElementAtCaret = other.usagesOfElementAtCaret;
        this.showQuickDocOnHover = other.showQuickDocHover();
        this.refactoringOption = other.refactoringOption;
        this.preselectCurrentSymbolForRename = other.preselectCurrentSymbolForRename;
        this.showInlineDialogForLocalVariables = other.showInlineDialogForLocalVariables;
        this.errorStripeMarkMinHeight = other.errorStripeMarkMinHeight;
        this.autoreparseDelay = other.autoreparseDelay;
        this.nextErrorActionGoesThrough = other.nextErrorActionGoesThrough;
        this.suppressWithSuppressWarnings = other.suppressWithSuppressWarnings;
        this.tooltipDelay = other.tooltipDelay;
    }

    public boolean isModified(CodeEditingSettings other) {
        if (other == null) return true;
        return this.matchedBrace != other.matchedBrace
                || this.currentScope != other.currentScope
                || this.usagesOfElementAtCaret != other.usagesOfElementAtCaret
                || this.showQuickDocOnHover != other.showQuickDocOnHover
                || this.refactoringOption != other.refactoringOption
                || this.preselectCurrentSymbolForRename != other.preselectCurrentSymbolForRename
                || this.showInlineDialogForLocalVariables != other.showInlineDialogForLocalVariables
                || this.errorStripeMarkMinHeight != other.errorStripeMarkMinHeight
                || this.autoreparseDelay != other.autoreparseDelay
                || !Objects.equals(this.nextErrorActionGoesThrough, other.nextErrorActionGoesThrough)
                || this.suppressWithSuppressWarnings != other.suppressWithSuppressWarnings
                || this.tooltipDelay != other.tooltipDelay;
    }

    public void addListener(Listener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    private void notifyListeners() {
        for (Listener l : listeners) {
            try {
                l.onSettingsChanged(this);
            } catch (Exception ignored) {}
        }
    }

    // Getters and Setters
    public boolean isMatchedBrace() { return matchedBrace; }
    public void setMatchedBrace(boolean val) { this.matchedBrace = val; }

    public boolean isCurrentScope() { return currentScope; }
    public void setCurrentScope(boolean val) { this.currentScope = val; }

    public boolean isUsagesOfElementAtCaret() { return usagesOfElementAtCaret; }
    public void setUsagesOfElementAtCaret(boolean val) { this.usagesOfElementAtCaret = val; }

    public boolean isShowQuickDocOnHover() { return showQuickDocOnHover; }
    public boolean showQuickDocHover() { return showQuickDocOnHover; }
    public void setShowQuickDocOnHover(boolean val) { this.showQuickDocOnHover = val; }

    public RefactoringOption getRefactoringOption() { return refactoringOption; }
    public void setRefactoringOption(RefactoringOption val) { this.refactoringOption = val; }

    public boolean isPreselectCurrentSymbolForRename() { return preselectCurrentSymbolForRename; }
    public void setPreselectCurrentSymbolForRename(boolean val) { this.preselectCurrentSymbolForRename = val; }

    public boolean isShowInlineDialogForLocalVariables() { return showInlineDialogForLocalVariables; }
    public void setShowInlineDialogForLocalVariables(boolean val) { this.showInlineDialogForLocalVariables = val; }

    public int getErrorStripeMarkMinHeight() { return errorStripeMarkMinHeight; }
    public void setErrorStripeMarkMinHeight(int val) { this.errorStripeMarkMinHeight = val; }

    public int getAutoreparseDelay() { return autoreparseDelay; }
    public void setAutoreparseDelay(int val) { this.autoreparseDelay = val; }

    public String getNextErrorActionGoesThrough() { return nextErrorActionGoesThrough; }
    public void setNextErrorActionGoesThrough(String val) { this.nextErrorActionGoesThrough = val; }

    public boolean isSuppressWithSuppressWarnings() { return suppressWithSuppressWarnings; }
    public void setSuppressWithSuppressWarnings(boolean val) { this.suppressWithSuppressWarnings = val; }

    public int getTooltipDelay() { return tooltipDelay; }
    public void setTooltipDelay(int val) { this.tooltipDelay = val; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CodeEditingSettings that = (CodeEditingSettings) o;
        return !isModified(that);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                matchedBrace, currentScope, usagesOfElementAtCaret, showQuickDocOnHover,
                refactoringOption, preselectCurrentSymbolForRename, showInlineDialogForLocalVariables,
                errorStripeMarkMinHeight, autoreparseDelay, nextErrorActionGoesThrough,
                suppressWithSuppressWarnings, tooltipDelay
        );
    }
}
