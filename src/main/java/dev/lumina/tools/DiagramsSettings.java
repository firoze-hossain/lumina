package dev.lumina.tools;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Model representing Tools > Diagrams settings in Lumina IDE.
 */
public class DiagramsSettings implements Cloneable {

    private String defaultScope = "All";
    private String nodeItemStyle = "With types aligned";
    private int shortenNodeItemsLength = 60;
    private boolean showGridByDefault = true;
    private boolean enableAnimations = true;
    private boolean enableNodeItemsSyntaxHighlighting = true;

    private String defaultLayout = "Hierarchic Compact";
    private String layoutOnCategorySwitch = "With light layouter";
    private int layoutAnimationDuration = 1500;
    private boolean fitContentAfterLayout = true;
    private boolean doRelayoutWhenNewElementsAdded = true;

    private Map<String, Boolean> categorySelections = new LinkedHashMap<>();

    public DiagramsSettings() {
        initDefaults();
    }

    private void initDefaults() {
        categorySelections.clear();
        // Show Difference
        categorySelections.put("Show Difference", true);
        categorySelections.put("Show Difference/Details", true);

        // Java Classes
        categorySelections.put("Java Classes", false);
        categorySelections.put("Java Classes/Fields", false);
        categorySelections.put("Java Classes/Constructors", false);
        categorySelections.put("Java Classes/Methods", false);
        categorySelections.put("Java Classes/Properties", false);
        categorySelections.put("Java Classes/Inner Classes", false);

        // Dependencies
        categorySelections.put("Dependencies", false);
        categorySelections.put("Dependencies/Show One To One", true);
        categorySelections.put("Dependencies/Show One To Many", true);
        categorySelections.put("Dependencies/Show Usages In Code", false);
        categorySelections.put("Dependencies/Show Links In Documentation", false);
        categorySelections.put("Dependencies/Show Cyclic", false);
        categorySelections.put("Dependencies/Others", true);

        // Project Modules
        categorySelections.put("Project Modules", false);
        categorySelections.put("Project Modules/Libraries", false);
        categorySelections.put("Project Modules/Test dependencies", false);
        categorySelections.put("Project Modules/Show Paths: Root -> Selection", false);
        categorySelections.put("Project Modules/Show Neighbors of Selected Nodes", false);
        categorySelections.put("Project Modules/Show Borders", true);

        // CDI Dependencies
        categorySelections.put("CDI Dependencies", true);
        categorySelections.put("CDI Dependencies/@Inject", true);
        categorySelections.put("CDI Dependencies/@Produces", true);
        categorySelections.put("CDI Dependencies/@Decorator", true);

        // Spring Beans Dependencies
        categorySelections.put("Spring Beans Dependencies", false);
        categorySelections.put("Spring Beans Dependencies/Show Neighbors of Selected Nodes", false);
        categorySelections.put("Spring Beans Dependencies/Local context", true);
        categorySelections.put("Spring Beans Dependencies/Show beans from libraries", false);
        categorySelections.put("Spring Beans Dependencies/Properties", true);
        categorySelections.put("Spring Beans Dependencies/Show Borders", true);

        // Spring Context Dependencies
        categorySelections.put("Spring Context Dependencies", false);
        categorySelections.put("Spring Context Dependencies/Show library models", false);
        categorySelections.put("Spring Context Dependencies/Show filesets as groups", true);
        categorySelections.put("Spring Context Dependencies/Show Borders", true);

        // Ruby Class Diagram
        categorySelections.put("Ruby Class Diagram", false);
        categorySelections.put("Ruby Class Diagram/Fields", false);
        categorySelections.put("Ruby Class Diagram/Methods", false);
        categorySelections.put("Ruby Class Diagram/Constants", false);

        // Rails Model Dependency Diagram
        categorySelections.put("Rails Model Dependency Diagram", false);
        categorySelections.put("Rails Model Dependency Diagram/Association fields", false);
        categorySelections.put("Rails Model Dependency Diagram/Migration fields", true);
        categorySelections.put("Rails Model Dependency Diagram/System DB fields", false);
        categorySelections.put("Rails Model Dependency Diagram/Own fields", false);

        // Spring Integration
        categorySelections.put("Spring Integration", true);
        categorySelections.put("Spring Integration/Show Labels", true);

        // Python Class Diagram
        categorySelections.put("Python Class Diagram", false);
        categorySelections.put("Python Class Diagram/Methods", false);
        categorySelections.put("Python Class Diagram/Inner Classes", false);
        categorySelections.put("Python Class Diagram/Fields", false);

        // SQLAlchemy Model Dependency Diagram
        categorySelections.put("SQLAlchemy Model Dependency Diagram", false);
        categorySelections.put("SQLAlchemy Model Dependency Diagram/Fields", false);

        // Maven dependencies
        categorySelections.put("Maven dependencies", false);
        categorySelections.put("Maven dependencies/Show 'groupId':'artifactId':'version'", false);
        categorySelections.put("Maven dependencies/Show Conflicts/Duplicates", false);
        categorySelections.put("Maven dependencies/Show Paths: Root -> Selection", false);
        categorySelections.put("Maven dependencies/Show Neighbors of Selected Nodes", false);
        categorySelections.put("Maven dependencies/Show Borders", true);

        // Gradle Dependencies
        categorySelections.put("Gradle Dependencies", false);
        categorySelections.put("Gradle Dependencies/Show 'groupId':'artifactId':'version'", false);
        categorySelections.put("Gradle Dependencies/Show Conflicts/Duplicates", false);
        categorySelections.put("Gradle Dependencies/Show Paths: Root -> Selection", false);
        categorySelections.put("Gradle Dependencies/Show Neighbors of Selected Nodes", false);
        categorySelections.put("Gradle Dependencies/Show Borders", true);

        // Database Schema Diagram
        categorySelections.put("Database Schema Diagram", true);
        categorySelections.put("Database Schema Diagram/Key columns", true);
        categorySelections.put("Database Schema Diagram/Columns", true);
        categorySelections.put("Database Schema Diagram/Virtual foreign keys", true);
        categorySelections.put("Database Schema Diagram/Comments", true);

        // Graphical Explain Plan
        categorySelections.put("Graphical Explain Plan", true);
        categorySelections.put("Graphical Explain Plan/Attributes", true);

        // JPA ER Diagram
        categorySelections.put("JPA ER Diagram", true);
        categorySelections.put("JPA ER Diagram/Properties", true);
        categorySelections.put("JPA ER Diagram/Embeddables", true);
        categorySelections.put("JPA ER Diagram/Superclasses", true);

        // PHP Class Diagrams
        categorySelections.put("PHP Class Diagrams", false);
        categorySelections.put("PHP Class Diagrams/Fields", false);
        categorySelections.put("PHP Class Diagrams/Constants", false);
        categorySelections.put("PHP Class Diagrams/Constructors", false);
        categorySelections.put("PHP Class Diagrams/Methods", false);

        // Services diagram
        categorySelections.put("Services diagram", false);
        categorySelections.put("Services diagram/From tests", false);
        categorySelections.put("Services diagram/From libraries", false);
        categorySelections.put("Services diagram/Show Neighbors of Selected Nodes", false);
        categorySelections.put("Services diagram/Show Borders", true);
    }

    public String getDefaultScope() {
        return defaultScope;
    }

    public void setDefaultScope(String defaultScope) {
        this.defaultScope = defaultScope != null ? defaultScope : "All";
    }

    public String getNodeItemStyle() {
        return nodeItemStyle;
    }

    public void setNodeItemStyle(String nodeItemStyle) {
        this.nodeItemStyle = nodeItemStyle != null ? nodeItemStyle : "With types aligned";
    }

    public int getShortenNodeItemsLength() {
        return shortenNodeItemsLength;
    }

    public void setShortenNodeItemsLength(int shortenNodeItemsLength) {
        this.shortenNodeItemsLength = shortenNodeItemsLength;
    }

    public boolean isShowGridByDefault() {
        return showGridByDefault;
    }

    public void setShowGridByDefault(boolean showGridByDefault) {
        this.showGridByDefault = showGridByDefault;
    }

    public boolean isEnableAnimations() {
        return enableAnimations;
    }

    public void setEnableAnimations(boolean enableAnimations) {
        this.enableAnimations = enableAnimations;
    }

    public boolean isEnableNodeItemsSyntaxHighlighting() {
        return enableNodeItemsSyntaxHighlighting;
    }

    public void setEnableNodeItemsSyntaxHighlighting(boolean enableNodeItemsSyntaxHighlighting) {
        this.enableNodeItemsSyntaxHighlighting = enableNodeItemsSyntaxHighlighting;
    }

    public String getDefaultLayout() {
        return defaultLayout;
    }

    public void setDefaultLayout(String defaultLayout) {
        this.defaultLayout = defaultLayout != null ? defaultLayout : "Hierarchic Compact";
    }

    public String getLayoutOnCategorySwitch() {
        return layoutOnCategorySwitch;
    }

    public void setLayoutOnCategorySwitch(String layoutOnCategorySwitch) {
        this.layoutOnCategorySwitch = layoutOnCategorySwitch != null ? layoutOnCategorySwitch : "With light layouter";
    }

    public int getLayoutAnimationDuration() {
        return layoutAnimationDuration;
    }

    public void setLayoutAnimationDuration(int layoutAnimationDuration) {
        this.layoutAnimationDuration = layoutAnimationDuration;
    }

    public boolean isFitContentAfterLayout() {
        return fitContentAfterLayout;
    }

    public void setFitContentAfterLayout(boolean fitContentAfterLayout) {
        this.fitContentAfterLayout = fitContentAfterLayout;
    }

    public boolean isDoRelayoutWhenNewElementsAdded() {
        return doRelayoutWhenNewElementsAdded;
    }

    public void setDoRelayoutWhenNewElementsAdded(boolean doRelayoutWhenNewElementsAdded) {
        this.doRelayoutWhenNewElementsAdded = doRelayoutWhenNewElementsAdded;
    }

    public Map<String, Boolean> getCategorySelections() {
        return categorySelections;
    }

    public void setCategorySelections(Map<String, Boolean> categorySelections) {
        this.categorySelections = categorySelections != null ? new LinkedHashMap<>(categorySelections) : new LinkedHashMap<>();
    }

    @Override
    public DiagramsSettings clone() {
        try {
            DiagramsSettings copy = (DiagramsSettings) super.clone();
            copy.categorySelections = new LinkedHashMap<>(this.categorySelections);
            return copy;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DiagramsSettings that = (DiagramsSettings) o;
        return shortenNodeItemsLength == that.shortenNodeItemsLength &&
                showGridByDefault == that.showGridByDefault &&
                enableAnimations == that.enableAnimations &&
                enableNodeItemsSyntaxHighlighting == that.enableNodeItemsSyntaxHighlighting &&
                layoutAnimationDuration == that.layoutAnimationDuration &&
                fitContentAfterLayout == that.fitContentAfterLayout &&
                doRelayoutWhenNewElementsAdded == that.doRelayoutWhenNewElementsAdded &&
                Objects.equals(defaultScope, that.defaultScope) &&
                Objects.equals(nodeItemStyle, that.nodeItemStyle) &&
                Objects.equals(defaultLayout, that.defaultLayout) &&
                Objects.equals(layoutOnCategorySwitch, that.layoutOnCategorySwitch) &&
                Objects.equals(categorySelections, that.categorySelections);
    }

    @Override
    public int hashCode() {
        return Objects.hash(defaultScope, nodeItemStyle, shortenNodeItemsLength,
                showGridByDefault, enableAnimations, enableNodeItemsSyntaxHighlighting,
                defaultLayout, layoutOnCategorySwitch, layoutAnimationDuration,
                fitContentAfterLayout, doRelayoutWhenNewElementsAdded, categorySelections);
    }
}
