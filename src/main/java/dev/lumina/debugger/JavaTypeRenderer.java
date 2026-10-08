package dev.lumina.debugger;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Custom type renderer model matching IntelliJ IDEA (Image 5).
 */
public class JavaTypeRenderer implements Cloneable {

    public enum NodeRendererType {
        DEFAULT,
        EXPRESSION
    }

    public enum ExpandRendererType {
        DEFAULT,
        EXPRESSION,
        EXPRESSION_LIST
    }

    public static class ChildExpressionRule implements Cloneable {
        private String name = "";
        private String expression = "";
        private boolean onDemand = false;

        public ChildExpressionRule() {
        }

        public ChildExpressionRule(String name, String expression, boolean onDemand) {
            this.name = name != null ? name : "";
            this.expression = expression != null ? expression : "";
            this.onDemand = onDemand;
        }

        public ChildExpressionRule(ChildExpressionRule other) {
            if (other != null) {
                this.name = other.name;
                this.expression = other.expression;
                this.onDemand = other.onDemand;
            }
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name != null ? name : "";
        }

        public String getExpression() {
            return expression;
        }

        public void setExpression(String expression) {
            this.expression = expression != null ? expression : "";
        }

        public boolean isOnDemand() {
            return onDemand;
        }

        public void setOnDemand(boolean onDemand) {
            this.onDemand = onDemand;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ChildExpressionRule that)) return false;
            return onDemand == that.onDemand &&
                    Objects.equals(name, that.name) &&
                    Objects.equals(expression, that.expression);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, expression, onDemand);
        }

        @Override
        public ChildExpressionRule clone() {
            return new ChildExpressionRule(this);
        }
    }

    private String id;
    private boolean enabled = true;
    private String name = "unnamed";
    private String targetClassName = "java.lang.Object";

    // When rendering a node
    private boolean showTypeAndObjectId = true;
    private NodeRendererType nodeRendererType = NodeRendererType.DEFAULT;
    private String nodeExpression = "";
    private boolean nodeOnDemand = false;

    // When expanding a node
    private ExpandRendererType expandRendererType = ExpandRendererType.DEFAULT;
    private String expandExpression = "";
    private String testCanExpandExpression = "";
    private List<ChildExpressionRule> childExpressions = new ArrayList<>();
    private boolean appendDefaultChildren = false;

    public JavaTypeRenderer() {
        this.id = UUID.randomUUID().toString();
    }

    public JavaTypeRenderer(String name, String targetClassName) {
        this.id = UUID.randomUUID().toString();
        this.name = name != null ? name : "unnamed";
        this.targetClassName = targetClassName != null ? targetClassName : "java.lang.Object";
    }

    public JavaTypeRenderer(JavaTypeRenderer other) {
        if (other != null) {
            this.id = other.id != null ? other.id : UUID.randomUUID().toString();
            this.enabled = other.enabled;
            this.name = other.name;
            this.targetClassName = other.targetClassName;
            this.showTypeAndObjectId = other.showTypeAndObjectId;
            this.nodeRendererType = other.nodeRendererType != null ? other.nodeRendererType : NodeRendererType.DEFAULT;
            this.nodeExpression = other.nodeExpression;
            this.nodeOnDemand = other.nodeOnDemand;
            this.expandRendererType = other.expandRendererType != null ? other.expandRendererType : ExpandRendererType.DEFAULT;
            this.expandExpression = other.expandExpression;
            this.testCanExpandExpression = other.testCanExpandExpression;
            this.childExpressions = new ArrayList<>();
            for (ChildExpressionRule c : other.childExpressions) {
                this.childExpressions.add(c.clone());
            }
            this.appendDefaultChildren = other.appendDefaultChildren;
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name != null ? name : "unnamed";
    }

    public String getTargetClassName() {
        return targetClassName;
    }

    public void setTargetClassName(String targetClassName) {
        this.targetClassName = targetClassName != null ? targetClassName : "java.lang.Object";
    }

    public boolean isShowTypeAndObjectId() {
        return showTypeAndObjectId;
    }

    public void setShowTypeAndObjectId(boolean showTypeAndObjectId) {
        this.showTypeAndObjectId = showTypeAndObjectId;
    }

    public NodeRendererType getNodeRendererType() {
        return nodeRendererType;
    }

    public void setNodeRendererType(NodeRendererType nodeRendererType) {
        this.nodeRendererType = nodeRendererType != null ? nodeRendererType : NodeRendererType.DEFAULT;
    }

    public String getNodeExpression() {
        return nodeExpression;
    }

    public void setNodeExpression(String nodeExpression) {
        this.nodeExpression = nodeExpression != null ? nodeExpression : "";
    }

    public boolean isNodeOnDemand() {
        return nodeOnDemand;
    }

    public void setNodeOnDemand(boolean nodeOnDemand) {
        this.nodeOnDemand = nodeOnDemand;
    }

    public ExpandRendererType getExpandRendererType() {
        return expandRendererType;
    }

    public void setExpandRendererType(ExpandRendererType expandRendererType) {
        this.expandRendererType = expandRendererType != null ? expandRendererType : ExpandRendererType.DEFAULT;
    }

    public String getExpandExpression() {
        return expandExpression;
    }

    public void setExpandExpression(String expandExpression) {
        this.expandExpression = expandExpression != null ? expandExpression : "";
    }

    public String getTestCanExpandExpression() {
        return testCanExpandExpression;
    }

    public void setTestCanExpandExpression(String testCanExpandExpression) {
        this.testCanExpandExpression = testCanExpandExpression != null ? testCanExpandExpression : "";
    }

    public List<ChildExpressionRule> getChildExpressions() {
        return childExpressions;
    }

    public void setChildExpressions(List<ChildExpressionRule> childExpressions) {
        this.childExpressions = childExpressions != null ? new ArrayList<>(childExpressions) : new ArrayList<>();
    }

    public boolean isAppendDefaultChildren() {
        return appendDefaultChildren;
    }

    public void setAppendDefaultChildren(boolean appendDefaultChildren) {
        this.appendDefaultChildren = appendDefaultChildren;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof JavaTypeRenderer that)) return false;
        return enabled == that.enabled &&
                showTypeAndObjectId == that.showTypeAndObjectId &&
                nodeOnDemand == that.nodeOnDemand &&
                appendDefaultChildren == that.appendDefaultChildren &&
                Objects.equals(name, that.name) &&
                Objects.equals(targetClassName, that.targetClassName) &&
                nodeRendererType == that.nodeRendererType &&
                Objects.equals(nodeExpression, that.nodeExpression) &&
                expandRendererType == that.expandRendererType &&
                Objects.equals(expandExpression, that.expandExpression) &&
                Objects.equals(testCanExpandExpression, that.testCanExpandExpression) &&
                Objects.equals(childExpressions, that.childExpressions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(enabled, name, targetClassName, showTypeAndObjectId,
                nodeRendererType, nodeExpression, nodeOnDemand, expandRendererType,
                expandExpression, testCanExpandExpression, childExpressions, appendDefaultChildren);
    }

    @Override
    public JavaTypeRenderer clone() {
        return new JavaTypeRenderer(this);
    }
}
