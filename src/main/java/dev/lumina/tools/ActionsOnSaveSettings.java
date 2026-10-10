package dev.lumina.tools;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Settings configuration model for all Actions on Save in Lumina IDE.
 */
public class ActionsOnSaveSettings implements Cloneable {

    private List<ActionOnSaveItem> items = new ArrayList<>();

    public ActionsOnSaveSettings() {
        initDefaultActions();
    }

    public static ActionsOnSaveSettings createDefault() {
        return new ActionsOnSaveSettings();
    }

    private void initDefaultActions() {
        items.clear();

        // 1. Reformat code
        ActionOnSaveItem reformat = new ActionOnSaveItem("reformat.code", "Reformat code", true, "Any save");
        reformat.setScopeOptions(List.of("Go files", "All file types", "Java files", "Kotlin files", "Rust files"));
        reformat.setSelectedScope("Go files");
        reformat.setModeOptions(List.of("Whole file", "Changed lines"));
        reformat.setSelectedMode("Whole file");
        items.add(reformat);

        // 2. Optimize imports
        ActionOnSaveItem optimizeImports = new ActionOnSaveItem("optimize.imports", "Optimize imports", false, "Any save");
        optimizeImports.setScopeOptions(List.of("All file types", "Java files", "Kotlin files", "Go files"));
        optimizeImports.setSelectedScope("All file types");
        items.add(optimizeImports);

        // 3. Rearrange code
        ActionOnSaveItem rearrange = new ActionOnSaveItem("rearrange.code", "Rearrange code", false, "Any save");
        items.add(rearrange);

        // 4. Run code cleanup
        ActionOnSaveItem cleanup = new ActionOnSaveItem("code.cleanup", "Run code cleanup", false, "Any save");
        cleanup.setSubtext("Applies fixes from the code cleanup inspections");
        cleanup.setProfileOptions(List.of("Project Profile", "IDE Profile"));
        cleanup.setSelectedProfile("Project Profile");
        items.add(cleanup);

        // 5. Update copyright notice
        ActionOnSaveItem copyright = new ActionOnSaveItem("copyright", "Update copyright notice", false, "Any save");
        copyright.setSubtext("No copyright is configured");
        items.add(copyright);

        // 6. Run Black
        ActionOnSaveItem black = new ActionOnSaveItem("black", "Run Black", false, "Any save");
        black.setSubtext("Black formatter package is not installed on the current interpreter");
        black.setWarning(true);
        black.setConfigureTarget("Black");
        items.add(black);

        // 7. Run stylelint --fix
        ActionOnSaveItem stylelint = new ActionOnSaveItem("stylelint", "Run stylelint --fix", false, "Any save");
        stylelint.setSubtext("Stylelint integration disabled");
        items.add(stylelint);

        // 8. Run eslint --fix
        ActionOnSaveItem eslint = new ActionOnSaveItem("eslint", "Run eslint --fix", false, "Any save");
        eslint.setSubtext("ESLint integration disabled");
        items.add(eslint);

        // 9. Run Prettier
        ActionOnSaveItem prettier = new ActionOnSaveItem("prettier", "Run Prettier", false, "Any save");
        prettier.setSubtext("Prettier integration disabled");
        items.add(prettier);

        // 10. Upload to default server
        ActionOnSaveItem upload = new ActionOnSaveItem("upload.server", "Upload to default server", false, "Any save");
        upload.setSubtext("Default server is not configured");
        items.add(upload);

        // 11. Build project
        ActionOnSaveItem build = new ActionOnSaveItem("build.project", "Build project", false, "Any save and external change");
        build.setSubtext("Not triggered while running/debugging");
        items.add(build);
    }

    public List<ActionOnSaveItem> getItems() {
        return items;
    }

    public void setItems(List<ActionOnSaveItem> items) {
        this.items = items != null ? new ArrayList<>(items) : new ArrayList<>();
    }

    public ActionOnSaveItem findItemById(String id) {
        if (id == null) return null;
        for (ActionOnSaveItem item : items) {
            if (id.equals(item.getId())) {
                return item;
            }
        }
        return null;
    }

    @Override
    public ActionsOnSaveSettings clone() {
        try {
            ActionsOnSaveSettings copy = (ActionsOnSaveSettings) super.clone();
            copy.items = new ArrayList<>();
            for (ActionOnSaveItem item : this.items) {
                copy.items.add(item.clone());
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
        ActionsOnSaveSettings that = (ActionsOnSaveSettings) o;
        return Objects.equals(items, that.items);
    }

    @Override
    public int hashCode() {
        return Objects.hash(items);
    }
}
