package dev.lumina.database;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Settings configuration model for Tools > Database > Other in Lumina IDE.
 */
public class DatabaseOtherSettings implements Cloneable {

    private boolean confirmCancellationForDialogsModifySchema = true;
    private boolean showPreviewOfValidScript = true;
    private boolean suggestDumpingDdl = true;
    private String generateContextTemplates = "Append to existing console";
    private boolean rememberWhetherFilterIsOn = true;
    private List<DatabaseVirtualForeignKey> virtualForeignKeys = new ArrayList<>();
    private String defaultResolveModeForConsoles = "Playground";
    private String statementDelimiter = "";

    public DatabaseOtherSettings() {
        initDefaults();
    }

    private void initDefaults() {
        virtualForeignKeys.clear();
        virtualForeignKeys.add(new DatabaseVirtualForeignKey("(.*)_(?i)id", "$1\\.(?i)id"));
    }

    public boolean isConfirmCancellationForDialogsModifySchema() {
        return confirmCancellationForDialogsModifySchema;
    }

    public void setConfirmCancellationForDialogsModifySchema(boolean confirmCancellationForDialogsModifySchema) {
        this.confirmCancellationForDialogsModifySchema = confirmCancellationForDialogsModifySchema;
    }

    public boolean isShowPreviewOfValidScript() {
        return showPreviewOfValidScript;
    }

    public void setShowPreviewOfValidScript(boolean showPreviewOfValidScript) {
        this.showPreviewOfValidScript = showPreviewOfValidScript;
    }

    public boolean isSuggestDumpingDdl() {
        return suggestDumpingDdl;
    }

    public void setSuggestDumpingDdl(boolean suggestDumpingDdl) {
        this.suggestDumpingDdl = suggestDumpingDdl;
    }

    public String getGenerateContextTemplates() {
        return generateContextTemplates;
    }

    public void setGenerateContextTemplates(String generateContextTemplates) {
        this.generateContextTemplates = generateContextTemplates != null ? generateContextTemplates : "Append to existing console";
    }

    public boolean isRememberWhetherFilterIsOn() {
        return rememberWhetherFilterIsOn;
    }

    public void setRememberWhetherFilterIsOn(boolean rememberWhetherFilterIsOn) {
        this.rememberWhetherFilterIsOn = rememberWhetherFilterIsOn;
    }

    public List<DatabaseVirtualForeignKey> getVirtualForeignKeys() {
        return virtualForeignKeys;
    }

    public void setVirtualForeignKeys(List<DatabaseVirtualForeignKey> virtualForeignKeys) {
        this.virtualForeignKeys = virtualForeignKeys != null ? new ArrayList<>(virtualForeignKeys) : new ArrayList<>();
    }

    public String getDefaultResolveModeForConsoles() {
        return defaultResolveModeForConsoles;
    }

    public void setDefaultResolveModeForConsoles(String defaultResolveModeForConsoles) {
        this.defaultResolveModeForConsoles = defaultResolveModeForConsoles != null ? defaultResolveModeForConsoles : "Playground";
    }

    public String getStatementDelimiter() {
        return statementDelimiter;
    }

    public void setStatementDelimiter(String statementDelimiter) {
        this.statementDelimiter = statementDelimiter != null ? statementDelimiter : "";
    }

    @Override
    public DatabaseOtherSettings clone() {
        try {
            DatabaseOtherSettings copy = (DatabaseOtherSettings) super.clone();
            copy.virtualForeignKeys = new ArrayList<>();
            for (DatabaseVirtualForeignKey k : this.virtualForeignKeys) {
                copy.virtualForeignKeys.add(k.clone());
            }
            return copy;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DatabaseOtherSettings that = (DatabaseOtherSettings) o;
        return confirmCancellationForDialogsModifySchema == that.confirmCancellationForDialogsModifySchema &&
                showPreviewOfValidScript == that.showPreviewOfValidScript &&
                suggestDumpingDdl == that.suggestDumpingDdl &&
                rememberWhetherFilterIsOn == that.rememberWhetherFilterIsOn &&
                Objects.equals(generateContextTemplates, that.generateContextTemplates) &&
                Objects.equals(virtualForeignKeys, that.virtualForeignKeys) &&
                Objects.equals(defaultResolveModeForConsoles, that.defaultResolveModeForConsoles) &&
                Objects.equals(statementDelimiter, that.statementDelimiter);
    }

    @Override
    public int hashCode() {
        return Objects.hash(confirmCancellationForDialogsModifySchema, showPreviewOfValidScript,
                suggestDumpingDdl, generateContextTemplates, rememberWhetherFilterIsOn,
                virtualForeignKeys, defaultResolveModeForConsoles, statementDelimiter);
    }
}
