package dev.lumina.database.versioning;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Settings configuration model for Tools > Database Versioning > Diff Changes in Lumina IDE.
 */
public class DatabaseDiffChangesSettings implements Cloneable {

    private List<DiffChangeRule> rules = new ArrayList<>();
    private List<DiffChangeExcludedItem> excludedChanges = new ArrayList<>();

    public DatabaseDiffChangesSettings() {
        initDefaults();
    }

    private void initDefaults() {
        rules.clear();

        // Column
        rules.add(new DiffChangeRule("Column", "Add Column", "Primary", "SAFE", "#6AAB73"));
        rules.add(new DiffChangeRule("Column", "Drop Column", "Primary", "DANGER", "#E06C75"));
        rules.add(new DiffChangeRule("Column", "Add Not Null Constraint", "Primary", "WARNING", "#E5C07B"));
        rules.add(new DiffChangeRule("Column", "Drop Not Null Constraint", "Primary", "SAFE", "#6AAB73"));
        rules.add(new DiffChangeRule("Column", "Add Auto Increment", "Primary", "SAFE", "#6AAB73"));
        rules.add(new DiffChangeRule("Column", "Add Default Value", "Primary", "SAFE", "#6AAB73"));
        rules.add(new DiffChangeRule("Column", "Modify Data Type", "Primary", "DANGER", "#E06C75"));

        // Comment
        rules.add(new DiffChangeRule("Comment", "Set Table Remarks", "Disregard", "SAFE", "#8C8C8C"));
        rules.add(new DiffChangeRule("Comment", "Set Column Remarks", "Disregard", "SAFE", "#8C8C8C"));

        // Foreign Key
        rules.add(new DiffChangeRule("Foreign Key", "Add Foreign Key Constraint", "Primary", "SAFE", "#6AAB73"));
        rules.add(new DiffChangeRule("Foreign Key", "Drop Foreign Key Constraint", "Primary", "DANGER", "#E06C75"));

        // Index
        rules.add(new DiffChangeRule("Index", "Create Index", "Primary", "SAFE", "#6AAB73"));
        rules.add(new DiffChangeRule("Index", "Drop Index", "Mark Ignored", "SAFE", "#8C8C8C"));

        // Primary Key
        rules.add(new DiffChangeRule("Primary Key", "Add Primary Key", "Primary", "SAFE", "#6AAB73"));
        rules.add(new DiffChangeRule("Primary Key", "Drop Primary Key", "Primary", "DANGER", "#E06C75"));

        // Sequence
        rules.add(new DiffChangeRule("Sequence", "Create Sequence", "Primary", "SAFE", "#6AAB73"));
        rules.add(new DiffChangeRule("Sequence", "Drop Sequence", "Primary", "DANGER", "#E06C75"));

        // Table
        rules.add(new DiffChangeRule("Table", "Create Table", "Primary", "SAFE", "#6AAB73"));
        rules.add(new DiffChangeRule("Table", "Drop Table", "Primary", "DANGER", "#E06C75"));

        // Unique Constraint
        rules.add(new DiffChangeRule("Unique Constraint", "Add Unique Constraint", "Primary", "SAFE", "#6AAB73"));
        rules.add(new DiffChangeRule("Unique Constraint", "Drop Unique Constraint", "Mark Ignored", "SAFE", "#8C8C8C"));

        excludedChanges.clear();
    }

    public List<DiffChangeRule> getRules() {
        return rules;
    }

    public void setRules(List<DiffChangeRule> rules) {
        this.rules = rules != null ? new ArrayList<>(rules) : new ArrayList<>();
    }

    public List<DiffChangeExcludedItem> getExcludedChanges() {
        return excludedChanges;
    }

    public void setExcludedChanges(List<DiffChangeExcludedItem> excludedChanges) {
        this.excludedChanges = excludedChanges != null ? new ArrayList<>(excludedChanges) : new ArrayList<>();
    }

    public DiffChangeRule findRule(String category, String action) {
        if (category == null || action == null) return null;
        for (DiffChangeRule r : rules) {
            if (category.equalsIgnoreCase(r.getCategory()) && action.equalsIgnoreCase(r.getAction())) {
                return r;
            }
        }
        return null;
    }

    @Override
    public DatabaseDiffChangesSettings clone() {
        try {
            DatabaseDiffChangesSettings copy = (DatabaseDiffChangesSettings) super.clone();
            copy.rules = new ArrayList<>();
            for (DiffChangeRule r : this.rules) {
                copy.rules.add(r.clone());
            }
            copy.excludedChanges = new ArrayList<>();
            for (DiffChangeExcludedItem item : this.excludedChanges) {
                copy.excludedChanges.add(item.clone());
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
        DatabaseDiffChangesSettings that = (DatabaseDiffChangesSettings) o;
        return Objects.equals(rules, that.rules) &&
                Objects.equals(excludedChanges, that.excludedChanges);
    }

    @Override
    public int hashCode() {
        return Objects.hash(rules, excludedChanges);
    }
}
